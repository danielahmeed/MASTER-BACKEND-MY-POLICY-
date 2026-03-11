# Ingestion Service – Complete Testing Guide

This guide explains how to test **all Ingestion Service functionalities** across every phase where it participates in the system sequence (Phase 3, Phase 4, Phase 7).

---

## Phase-to-Test Mapping

| Phase | Scenario | APIs Used | Tester |
|-------|----------|-----------|--------|
| **Phase 3** | File upload & job creation | POST /upload | BFF → Ingestion |
| **Phase 4** | Processing pipeline integration | GET /status, PATCH /progress, PATCH /status | Processing Service (simulated) |
| **Phase 7** | UI polling for upload progress | GET /status | BFF → Ingestion |

---

## Prerequisites

| Requirement | Check |
|-------------|-------|
| **MongoDB** | `mongosh --eval "db.version()"` or MongoDB Compass |
| **Ingestion Service** | `http://localhost:8082` (or `curl.exe http://localhost:8082/actuator/health`) |
| **BFF Service** (for Phase 3 & 7) | `http://localhost:8080` |
| **curl** | Built into Windows 10+ |
| **PowerShell** | For running scripts |

---

## Test Setup

### 1. Start MongoDB
```bash
# If not running as a service, start MongoDB manually
# mongod --dbpath <path>
```

### 2. Start Ingestion Service
```powershell
cd MyPolicy-Backend/ingestion-service
mvn spring-boot:run
```
Wait for: `Started IngestionServiceApplication`

### 3. Start BFF Service (for Phase 3 & 7 tests)
```powershell
cd MyPolicy-Backend/bff-service
mvn spring-boot:run
```
Wait for: `Started` on port 8080

### 4. Test Files Location
- `ingestion-service/Datasets/Auto_Insurance.csv`
- `ingestion-service/Datasets/Health_Insurance.csv`
- `ingestion-service/Datasets/Life_Insurance.csv`
- `ingestion-service/test-sample.xlsx` (if exists; create via `run-postman-tests.ps1`)

---

## Phase 3: File Upload & Job Creation

Tests the flow: User → BFF → Ingestion → MongoDB.

### 3.1 Direct Ingestion API (no BFF)

```powershell
cd MyPolicy-Backend/ingestion-service
curl.exe -X POST -F "file=@Datasets/Auto_Insurance.csv" -F "insurerId=HDFC_LIFE" -F "uploadedBy=test-user" http://localhost:8082/api/v1/ingestion/upload
```

**Expected (201):**
```json
{"jobId":"xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx","status":"UPLOADED"}
```

**Verify:**
- [ ] HTTP 201
- [ ] `jobId` is a valid UUID
- [ ] `status` is `"UPLOADED"`
- [ ] File exists at `storage/ingestion/{jobId}.csv`
- [ ] MongoDB `mypolicy_ingestion_db.ingestion_jobs` has new document

### 3.2 Via BFF (Phase 3 sequence)

```powershell
curl.exe -X POST -F "file=@ingestion-service/Datasets/Auto_Insurance.csv" -F "insurerId=HDFC_LIFE" -F "uploadedBy=test-user" http://localhost:8080/api/bff/upload
```

**Expected (200):** Same JSON shape as 3.1 (`jobId`, `status`).

**Verify:**
- [ ] HTTP 200 (BFF returns 200; Ingestion returns 201 internally)
- [ ] Response has `jobId` and `status`
- [ ] Ingestion Service logs show the upload

---

## Phase 4: Processing Pipeline Integration

Simulates Processing Service: GET status → PATCH status → PATCH progress → PATCH status COMPLETED.

Use a `jobId` from Phase 3.

### 4.1 GET Status (for filePath & insurerId)

```powershell
$jobId = "YOUR_JOB_ID_HERE"
curl.exe http://localhost:8082/api/v1/ingestion/status/$jobId
```

**Expected (200):**
```json
{
  "jobId": "...",
  "status": "UPLOADED",
  "processedRecords": 0,
  "totalRecords": 0,
  "filePath": "C:\\...\\storage\\ingestion\\{jobId}.csv",
  "insurerId": "HDFC_LIFE",
  "createdAt": "...",
  "updatedAt": "..."
}
```

**Verify:**
- [ ] `filePath` points to the uploaded file
- [ ] `insurerId` matches upload
- [ ] File at `filePath` is readable

### 4.2 PATCH Status → PROCESSING

```powershell
curl.exe -X PATCH -H "Content-Type: application/json" -d '{"status":"PROCESSING"}' "http://localhost:8082/api/v1/ingestion/$jobId/status"
```

**Expected:** HTTP 204 No Content

### 4.3 PATCH Progress

```powershell
curl.exe -X PATCH -H "Content-Type: application/json" -d '{"processedRecordsDelta":10}' "http://localhost:8082/api/v1/ingestion/$jobId/progress"
```

**Expected:** HTTP 204 No Content

### 4.4 Verify Progress

```powershell
curl.exe "http://localhost:8082/api/v1/ingestion/status/$jobId"
```

**Expected:** `status` = `"PROCESSING"`, `processedRecords` = `10`

### 4.5 PATCH Status → COMPLETED

```powershell
curl.exe -X PATCH -H "Content-Type: application/json" -d '{"status":"COMPLETED"}' "http://localhost:8082/api/v1/ingestion/$jobId/status"
```

**Expected:** HTTP 204 No Content

### 4.6 Verify Final State

```powershell
curl.exe "http://localhost:8082/api/v1/ingestion/status/$jobId"
```

**Expected:** `status` = `"COMPLETED"`, `processedRecords` = `10`

---

## Phase 7: UI Polling (Check Upload Status)

Tests: User → BFF → Ingestion → MongoDB for progress display.

### 7.1 Via BFF (Phase 7 sequence)

```powershell
$jobId = "YOUR_JOB_ID_HERE"
curl.exe "http://localhost:8080/api/bff/upload/status/$jobId"
```

**Expected (200):** Same structure as Ingestion GET /status (jobId, status, processedRecords, totalRecords, filePath, insurerId, createdAt, updatedAt).

**Verify:**
- [ ] BFF returns Ingestion response unchanged
- [ ] `processedRecords` and `totalRecords` match Ingestion
- [ ] `status` is correct for current job state

---

## Automated Scripts

### Option A: Full Lifecycle (Direct Ingestion)

```powershell
cd MyPolicy-Backend/Documentation/Ingestion Service
.\run-ingestion-flow.ps1
```

**Covers:** Phase 3 (direct), Phase 4 (all steps), file storage, MongoDB updates.

**Requires:** Ingestion on 8082, MongoDB, `Auto_Insurance.csv` (or `test-sample.xlsx`).

### Option B: Comprehensive Test Suite (PASS/FAIL)

```powershell
cd MyPolicy-Backend/Documentation/Ingestion Service
.\run-postman-tests.ps1
```

**Covers:** Upload, GET status, PATCH status, PATCH progress, final status with assertions.

**Requires:** Ingestion on 8082, MongoDB, curl, Python+openpyxl (for Excel generation).

---

## Full Manual Test Sequence (PowerShell)

Run from `ingestion-service` directory:

```powershell
$base = "http://localhost:8082/api/v1/ingestion"

# 1. Upload (Phase 3)
$upload = curl.exe -s -X POST -F "file=@Datasets/Auto_Insurance.csv" -F "insurerId=HDFC_LIFE" -F "uploadedBy=test-user" "$base/upload"
$jobId = ($upload | ConvertFrom-Json).jobId
Write-Host "jobId: $jobId"

# 2. GET status (Phase 4 start / Phase 7)
curl.exe -s "$base/status/$jobId"

# 3. To PROCESSING (Phase 4)
curl.exe -s -o $null -w "%{http_code}" -X PATCH -H "Content-Type: application/json" -d '{"status":"PROCESSING"}' "$base/$jobId/status"

# 4. Progress (Phase 4)
curl.exe -s -o $null -w "%{http_code}" -X PATCH -H "Content-Type: application/json" -d '{"processedRecordsDelta":10}' "$base/$jobId/progress"

# 5. Verify (Phase 7 equivalent)
curl.exe -s "$base/status/$jobId"

# 6. COMPLETED (Phase 4)
curl.exe -s -o $null -w "%{http_code}" -X PATCH -H "Content-Type: application/json" -d '{"status":"COMPLETED"}' "$base/$jobId/status"

# 7. Final status
curl.exe -s "$base/status/$jobId"
```

---

## BFF Integration Test (Phase 3 + Phase 7)

With BFF running on 8080:

```powershell
cd MyPolicy-Backend/ingestion-service

# Phase 3: Upload via BFF
$upload = curl.exe -s -X POST -F "file=@Datasets/Auto_Insurance.csv" -F "insurerId=TEST" -F "uploadedBy=qa" "http://localhost:8080/api/bff/upload"
$jobId = ($upload | ConvertFrom-Json).jobId

# Phase 7: Poll status via BFF
curl.exe -s "http://localhost:8080/api/bff/upload/status/$jobId"
```

---

## Negative Tests & Edge Cases

### Invalid file type
```powershell
curl.exe -X POST -F "file=@somefile.txt" -F "insurerId=X" -F "uploadedBy=u" http://localhost:8082/api/v1/ingestion/upload
# Expected: 400, "Invalid file type. Allowed: .xls, .xlsx, .csv"
```

### Job not found
```powershell
curl.exe http://localhost:8082/api/v1/ingestion/status/00000000-0000-0000-0000-000000000000
# Expected: 400, "Job not found: ..."
```

### Invalid state transition (UPLOADED → COMPLETED)
```powershell
# After upload, skip PROCESSING:
curl.exe -X PATCH -H "Content-Type: application/json" -d '{"status":"COMPLETED"}' "http://localhost:8082/api/v1/ingestion/$jobId/status"
# Expected: 409, "Invalid transition: UPLOADED -> COMPLETED. Allowed: PROCESSING"
```

### Progress on non-PROCESSING job
```powershell
# Call PATCH progress while status is still UPLOADED
curl.exe -X PATCH -H "Content-Type: application/json" -d '{"processedRecordsDelta":5}' "http://localhost:8082/api/v1/ingestion/$jobId/progress"
# Expected: 409, "Cannot update progress: job must be in PROCESSING state"
```

---

## Validation Checklist

| # | Test | Phase | Pass |
|---|------|-------|------|
| 1 | POST /upload (direct) returns 201, jobId, UPLOADED | 3 | ☐ |
| 2 | File stored at storage/ingestion/{jobId}.{ext} | 3 | ☐ |
| 3 | MongoDB ingestion_jobs has document | 3 | ☐ |
| 4 | POST /upload via BFF returns jobId | 3 | ☐ |
| 5 | GET /status returns filePath, insurerId | 4 | ☐ |
| 6 | PATCH status → PROCESSING returns 204 | 4 | ☐ |
| 7 | PATCH progress returns 204 | 4 | ☐ |
| 8 | GET status shows processedRecords updated | 4 | ☐ |
| 9 | PATCH status → COMPLETED returns 204 | 4 | ☐ |
| 10 | GET status shows COMPLETED | 4 | ☐ |
| 11 | GET /status via BFF returns same data | 7 | ☐ |
| 12 | Invalid file type returns 400 | - | ☐ |
| 13 | Invalid transition returns 409 | - | ☐ |

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Connection refused 8082 | Start Ingestion: `mvn spring-boot:run` in ingestion-service |
| Connection refused 8080 | Start BFF: `mvn spring-boot:run` in bff-service |
| Port 8082 in use | `Get-NetTCPConnection -LocalPort 8082 \| % { Stop-Process -Id $_.OwningProcess -Force }` |
| PowerShell curl JSON error | Use single quotes: `-d '{"status":"PROCESSING"}'` |
| File not found | Run from `ingestion-service` or use full path: `@C:\...\Datasets\Auto_Insurance.csv` |
