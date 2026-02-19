# Ingestion Service – Step-by-Step Testing Guide

A Spring Boot testing guide for the Ingestion Service. You will build and run the application, exercise its REST APIs, and validate the full job lifecycle.

---

## Spring Boot Overview

The Ingestion Service is a **Spring Boot 3** application that:

- Uses **embedded Tomcat** (port 8082)
- Connects to **MongoDB** for job persistence
- Exposes REST endpoints via **Spring MVC** (`@RestController`)
- Accepts multipart file uploads (Excel `.xls`/`.xlsx` or CSV `.csv`)

Configuration is in `application.properties` under `ingestion-service/src/main/resources/`.

---

## Prerequisites

| Requirement | Details |
|-------------|---------|
| **Java** | JDK 17+ (the project uses Java 25) |
| **Maven** | For `mvn spring-boot:run` and `mvn compile` |
| **MongoDB** | Running on `localhost:27017` |
| **curl** | Built into Windows 10+; used for API testing |

Verify MongoDB:

```bash
mongosh --eval "db.version()"
# or: mongo --eval "db.version()"
```

---

## Step 1: Build the Spring Boot Application

From the project root:

```bash
cd MyPolicy-Backend/ingestion-service
mvn clean compile
```

Ensure the build succeeds before running the app.

---

## Step 2: Run the Ingestion Service

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

**Expected output:**

- Maven compiles and starts the app
- Spring Boot banner appears
- `Tomcat initialized with port(s): 8082 (http)`
- `Started IngestionServiceApplication`
- MongoDB connection established

**Port conflict:** If you see *"Port 8082 was already in use"*, either:

1. Stop the existing process on 8082 (the service may already be running), or  
2. Set `server.port=8083` in `application.properties` to use another port

Keep this terminal open; the service must stay running for the tests.

---

## Step 3: Upload a File (Excel or CSV)

Open a **new terminal**; leave the first one running the Spring Boot app.

### Option A: CSV from `Datasets/`

```bash
cd MyPolicy-Backend/ingestion-service
curl.exe -X POST -F "file=@Datasets/Auto_Insurance.csv" -F "insurerId=HDFC_LIFE" -F "uploadedBy=test-user" http://localhost:8082/api/v1/ingestion/upload
```

You can swap `Auto_Insurance.csv` for `Health_Insurance.csv`, `Life_Insurance.csv`, or `Customer_data.csv`.

### Option B: Excel test file

```bash
curl.exe -X POST -F "file=@test-sample.xlsx" -F "insurerId=ICICI_PRU" -F "uploadedBy=user-001" http://localhost:8082/api/v1/ingestion/upload
```

**Expected response (HTTP 201):**

```json
{"jobId":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx","status":"UPLOADED"}
```

**Copy the `jobId`** — you will need it in the following steps.

---

## Step 4: Get Job Status

Replace `YOUR_JOB_ID` with the value from Step 3.

```bash
curl.exe http://localhost:8082/api/v1/ingestion/status/YOUR_JOB_ID
```

**Expected response (HTTP 200):**

```json
{
  "jobId": "YOUR_JOB_ID",
  "status": "UPLOADED",
  "processedRecords": 0,
  "totalRecords": 0,
  "filePath": "C:\\...\\storage\\ingestion\\YOUR_JOB_ID.csv",
  "insurerId": "HDFC_LIFE",
  "createdAt": "2026-02-12T...",
  "updatedAt": "2026-02-12T..."
}
```

Check that `filePath` points to a file under `storage/ingestion` and `insurerId` matches your upload.

---

## Step 5: Update Status to PROCESSING

Simulate the Processing Service moving the job to `PROCESSING`:

```bash
curl.exe -X PATCH -H "Content-Type: application/json" -d "{\"status\":\"PROCESSING\"}" http://localhost:8082/api/v1/ingestion/YOUR_JOB_ID/status
```

**Expected:** HTTP 204 No Content

---

## Step 6: Update Progress

Simulate record processing:

```bash
curl.exe -X PATCH -H "Content-Type: application/json" -d "{\"processedRecordsDelta\":10}" http://localhost:8082/api/v1/ingestion/YOUR_JOB_ID/progress
```

**Expected:** HTTP 204 No Content

---

## Step 7: Verify Progress

```bash
curl.exe http://localhost:8082/api/v1/ingestion/status/YOUR_JOB_ID
```

**Expected:** `processedRecords` is `10`, `status` is `"PROCESSING"`.

---

## Step 8: Complete the Job

```bash
curl.exe -X PATCH -H "Content-Type: application/json" -d "{\"status\":\"COMPLETED\"}" http://localhost:8082/api/v1/ingestion/YOUR_JOB_ID/status
```

**Expected:** HTTP 204 No Content

---

## Step 9: Final Status Check

```bash
curl.exe http://localhost:8082/api/v1/ingestion/status/YOUR_JOB_ID
```

**Expected:** `status` is `"COMPLETED"`, `processedRecords` is `10`.

---

## Quick Reference: Full curl Flow (PowerShell)

```powershell
$jobId = "PASTE_JOB_ID_HERE"
$base = "http://localhost:8082/api/v1/ingestion"

# Upload
curl.exe -X POST -F "file=@Datasets/Auto_Insurance.csv" -F "insurerId=TEST" -F "uploadedBy=you" "$base/upload"

# Status
curl.exe "$base/status/$jobId"

# To PROCESSING
curl.exe -X PATCH -H "Content-Type: application/json" -d '{\"status\":\"PROCESSING\"}' "$base/$jobId/status"

# Progress
curl.exe -X PATCH -H "Content-Type: application/json" -d '{\"processedRecordsDelta\":10}' "$base/$jobId/progress"

# Verify
curl.exe "$base/status/$jobId"

# To COMPLETED
curl.exe -X PATCH -H "Content-Type: application/json" -d '{\"status\":\"COMPLETED\"}' "$base/$jobId/status"

# Final status
curl.exe "$base/status/$jobId"
```

---

## Automated Test Script

Run the full flow with one command:

```powershell
cd MyPolicy-Backend/Documentation/Ingestion Service
.\run-postman-tests.ps1
```

The script will:

- Create `test-sample.xlsx` if missing (requires Python + openpyxl)
- Call Upload → Status → PATCH status → PATCH progress → Status → COMPLETED
- Report PASS/FAIL for each check

Requires: Ingestion service on 8082, MongoDB, curl, and Python+openpyxl for Excel generation.

---

## Spring Boot Configuration (Reference)

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 8082 | Embedded Tomcat port |
| `spring.data.mongodb.host` | localhost | MongoDB host |
| `spring.data.mongodb.port` | 27017 | MongoDB port |
| `spring.data.mongodb.database` | mypolicy_ingestion_db | MongoDB database |
| `ingestion.storage.path` | storage/ingestion | File storage base path |
| `spring.servlet.multipart.max-file-size` | 50MB | Max upload size |

---

## State Machine

Valid transitions for job `status`:

```
UPLOADED → PROCESSING → COMPLETED
                     → FAILED
```

You cannot skip `PROCESSING` or transition backwards from `COMPLETED`/`FAILED`.

---

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| Connection refused on 8082 | Service not running | Run `mvn spring-boot:run` in `ingestion-service` |
| Port 8082 already in use | Another instance running | Stop the existing process or change `server.port` |
| Job not found | Invalid or wrong `jobId` | Use the exact `jobId` from the upload response |
| 400 Invalid file type | Wrong extension | Use `.xls`, `.xlsx`, or `.csv` |
| 400 File is empty or missing | Incorrect file path | Ensure path is correct (e.g. `@Datasets/Auto_Insurance.csv`) |
| 409 Invalid transition | Wrong state transition | Follow UPLOADED → PROCESSING → COMPLETED/FAILED |
