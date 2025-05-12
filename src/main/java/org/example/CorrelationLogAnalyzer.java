/**
 * Correlation Log Analyzer Tool
 *
 * Author: Niran Peiris
 * Created: May 2025
 * Description: Parses WSO2 API Manager correlation logs to extract and report latency metrics.
 */
package org.example;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.*;

public class CorrelationLogAnalyzer {

    static class LatencyInfo {
        String correlationId;
        String resourcePath;
        Integer backendLatency;
        Integer roundTripLatency;

        LatencyInfo(String correlationId, String resourcePath) {
            this.correlationId = correlationId;
            this.resourcePath = resourcePath;
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter path to logs directory: ");
        String logsDirectory = scanner.nextLine().trim();
        if (!Files.isDirectory(Paths.get(logsDirectory))) {
            System.err.println("Invalid directory path: " + logsDirectory);
            return;
        }

        System.out.print("Enter HTTP method (e.g., GET, POST or * for all): ");
        String httpMethod = scanner.nextLine().trim().toUpperCase();
        if (!httpMethod.matches("GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|\\*")) {
            System.err.println("Invalid HTTP method: " + httpMethod);
            return;
        }

        System.out.print("Enter API resource path pattern (wildcard supported, e.g., /test/v1/*): ");
        String resourcePattern = scanner.nextLine().trim();
        if (!resourcePattern.startsWith("/")) {
            System.err.println("Invalid resource path. It should start with '/'");
            return;
        }

        Pattern resourceRegex = Pattern.compile(wildcardToRegex(resourcePattern));
        Map<String, LatencyInfo> latencyMap = new ConcurrentHashMap<>();

        List<Path> logFiles;
        try (Stream<Path> paths = Files.walk(Paths.get(logsDirectory))) {
            logFiles = paths.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        // Pass 1: collect all correlation IDs
        for (Path path : logFiles) {
            collectCorrelationIds(path.toFile(), httpMethod, resourceRegex, latencyMap);
        }

        // Pass 2: collect latency data
        for (Path path : logFiles) {
            collectLatencies(path.toFile(), latencyMap);
        }

        List<LatencyInfo> results = new ArrayList<>(latencyMap.values());
        results.removeIf(info -> info.roundTripLatency == null);
        results.sort(Comparator.comparingInt((LatencyInfo info) -> info.roundTripLatency).reversed());

        try (PrintWriter writer = new PrintWriter(new FileWriter("latency-report.csv"))) {
            writer.println("CorrelationID|ResourcePath|BACKEND_LATENCY|ROUND_TRIP_LATENCY");
            for (LatencyInfo info : results) {
                writer.printf("%s|%s|%s|%s%n",
                        info.correlationId,
                        info.resourcePath,
                        info.backendLatency != null ? info.backendLatency : "",
                        info.roundTripLatency);
            }
        }

        System.out.println("Latency report generated: latency-report.csv");
    }

    private static void collectCorrelationIds(File file, String httpMethod, Pattern resourceRegex, Map<String, LatencyInfo> latencyMap) {
        boolean matchAllMethods = httpMethod.equals("*");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 9) {
                    String correlationId = parts[1];
                    String component = parts[2];
                    String method = parts[6];
                    String resource = parts[7];
                    String action = parts[8].trim();

                    if (component.contains("HTTPS-Listener") &&
                            (matchAllMethods || httpMethod.equals(method)) &&
                            resourceRegex.matcher(resource).matches() &&
                            "REQUEST_HEAD".equals(action)) {

                        System.out.println("Matched Correlation ID: " + correlationId + " | Resource: " + resource);
                        latencyMap.putIfAbsent(correlationId, new LatencyInfo(correlationId, resource));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
        }
    }

    private static void collectLatencies(File file, Map<String, LatencyInfo> latencyMap) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 3) continue;

                String correlationId = parts[1];
                String component = parts[2];

                if (latencyMap.containsKey(correlationId)) {
                    if (parts.length >= 9) {
                        String action = parts[8].trim();
                        if (component.contains("HTTPS-Listener") && "ROUND-TRIP LATENCY".equals(action)) {
                            System.out.println("Found ROUND-TRIP LATENCY for ID: " + correlationId);
                            latencyMap.get(correlationId).roundTripLatency = extractLatency(parts);
                        }
                    }
                    if (parts.length >= 7) {
                        String action = parts[6].trim();
                        if (component.contains("HTTP-Sender") && "BACKEND LATENCY".equals(action)) {
                            System.out.println("Found BACKEND LATENCY for ID: " + correlationId);
                            latencyMap.get(correlationId).backendLatency = extractLatency(parts);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
        }
    }

    private static Integer extractLatency(String[] parts) {
        try {
            return Integer.parseInt(parts[3]);
        } catch (Exception e) {
            System.err.println("Failed to extract latency from value: " + parts[3]);
            return null;
        }
    }

    private static String wildcardToRegex(String pattern) {
        return "^" + pattern.replace("**", ".*").replace("*", "[^/]*") + "$";
    }
}



