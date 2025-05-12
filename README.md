# Correlation Log Analyzer

A simple Java tool to analyze [WSO2 API Manager](https://wso2.com/api-management/) correlation logs and extract latency metrics across large sets of log files.

It supports wildcard matching and multi-file processing and generates a sorted latency report in CSV format.

---

## Features

- Supports large log directories with thousands of files
- Collects correlation IDs from `REQUEST_HEAD` lines
- Matches `BACKEND LATENCY` and `ROUND-TRIP LATENCY` even across separate files
- Supports wildcard filtering for API paths
- Generates `latency-report.csv` sorted by round-trip latency (descending)
- Lightweight and dependency-free JAR

---

## Usage

### 1. **Download the Pre-built JAR**

Download the JAR from [here](https://github.com/niranRameshPeiris/correlation-log-analyzer/blob/main/target/CorrelationLogAnalyzer-1.0-jar-with-dependencies.jar)

Run it from your terminal:

```bash
java -jar CorrelationLogAnalyzer-1.0-jar-with-dependencies.jar
```

The program will prompt for:
- Path to the logs directory (e.g., /Niran/Documents/logs)
- HTTP method (e.g., POST, or * for all)
- API resource path pattern (e.g., /test/v1/*)

Output file:
- latency-report.csv

Sample Output file: 
```
CorrelationID|ResourcePath|BACKEND_LATENCY|ROUND_TRIP_LATENCY
395dcb7a-b599-4752-ad9a-914bebecca72|/test/v1/token|6008|6021
90a85bcb-ce36-42b8-bbbf-0bb06b94d93e|/test/v1/token|4009|4018
94527064-4c9b-4386-872e-64c9ee38d2a0|/test/v1/token|2007|2017
e3a028f2-90ae-4739-b44d-7d57326b949a|/test/v1/token|1011|1021
cec12450-b717-4032-8a08-4f90b145fc78|/test/v1/timeout|6|27
1cb7a2f2-be9a-411d-bdbc-4c371714d374|/test/v1/token|8|18
```

### 2. **Build from Source**

**Prerequisites:**
- Java 11+
- Maven

**Build Steps:**
- Clone this repository:
```
git clone https://github.com/niranRameshPeiris/correlation-log-analyzer.git
cd correlation-log-analyzer
```
- Build the JAR:
```
mvn clean package
```
- Run it:
```
java -jar target/CorrelationLogAnalyzer-1.0-jar-with-dependencies.jar
```
