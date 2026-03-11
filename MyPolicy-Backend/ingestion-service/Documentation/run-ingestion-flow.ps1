# ================================================================================
# INGESTION SERVICE - FULL LIFECYCLE TEST FLOW
# ================================================================================
# Uploads CSV, transitions job through UPLOADED -> PROCESSING -> COMPLETED
# Prerequisites: Ingestion service on 8082, MongoDB running, curl.exe
# Usage: .\run-ingestion-flow.ps1
# ================================================================================

$baseUrl = "http://localhost:8082/api/v1/ingestion"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ingestionServiceDir = Split-Path -Parent (Split-Path -Parent $scriptDir)
$testFile = Join-Path $ingestionServiceDir "ingestion-service\Datasets\Auto_Insurance.csv"

# Fallback if path structure differs
if (-not (Test-Path $testFile)) {
    $testFile = Join-Path $ingestionServiceDir "ingestion-service\test-sample.xlsx"
}
if (-not (Test-Path $testFile)) {
    Write-Host "ERROR: No test file found. Expected Auto_Insurance.csv or test-sample.xlsx in ingestion-service/Datasets or ingestion-service/" -ForegroundColor Red
    exit 1
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Ingestion Service - Full Lifecycle Test" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# --------------------------------------------------------------------------------
# Step 1: Upload
# --------------------------------------------------------------------------------
Write-Host "[1] Uploading file: $testFile" -ForegroundColor Cyan
$uploadResp = curl.exe -s -X POST -F "file=@$testFile" -F "insurerId=HDFC_LIFE" -F "uploadedBy=test-user" "$baseUrl/upload"

try {
    $json = $uploadResp | ConvertFrom-Json
    $jobId = $json.jobId
    $status = $json.status
} catch {
    Write-Host "  FAIL: Upload failed. Response: $uploadResp" -ForegroundColor Red
    exit 1
}

if (-not $jobId) {
    Write-Host "  FAIL: No jobId in response. Response: $uploadResp" -ForegroundColor Red
    exit 1
}

Write-Host "  jobId: $jobId" -ForegroundColor Green
Write-Host "  status: $status" -ForegroundColor Green

# --------------------------------------------------------------------------------
# Step 2: Get Status (UPLOADED)
# --------------------------------------------------------------------------------
Write-Host "`n[2] GET status (expect UPLOADED)" -ForegroundColor Cyan
$statusResp = curl.exe -s "$baseUrl/status/$jobId"
$statusJson = $statusResp | ConvertFrom-Json
Write-Host "  status: $($statusJson.status), processedRecords: $($statusJson.processedRecords)" -ForegroundColor Green

# --------------------------------------------------------------------------------
# Step 3: PATCH status -> PROCESSING
# --------------------------------------------------------------------------------
Write-Host "`n[3] PATCH status -> PROCESSING" -ForegroundColor Cyan
$patch1 = curl.exe -s -w "%{http_code}" -o $null -X PATCH -H "Content-Type: application/json" -d '{"status":"PROCESSING"}' "$baseUrl/$jobId/status"
if ($patch1 -eq "204") {
    Write-Host "  OK (204 No Content)" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Expected 204, got $patch1" -ForegroundColor Red
}

# --------------------------------------------------------------------------------
# Step 4: PATCH progress
# --------------------------------------------------------------------------------
Write-Host "`n[4] PATCH progress (processedRecordsDelta: 10)" -ForegroundColor Cyan
$patch2 = curl.exe -s -w "%{http_code}" -o $null -X PATCH -H "Content-Type: application/json" -d '{"processedRecordsDelta":10}' "$baseUrl/$jobId/progress"
if ($patch2 -eq "204") {
    Write-Host "  OK (204 No Content)" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Expected 204, got $patch2" -ForegroundColor Red
}

# --------------------------------------------------------------------------------
# Step 5: Get Status (PROCESSING, processedRecords=10)
# --------------------------------------------------------------------------------
Write-Host "`n[5] GET status (expect PROCESSING, processedRecords=10)" -ForegroundColor Cyan
$statusResp2 = curl.exe -s "$baseUrl/status/$jobId"
$statusJson2 = $statusResp2 | ConvertFrom-Json
Write-Host "  status: $($statusJson2.status), processedRecords: $($statusJson2.processedRecords)" -ForegroundColor Green

# --------------------------------------------------------------------------------
# Step 6: PATCH status -> COMPLETED
# --------------------------------------------------------------------------------
Write-Host "`n[6] PATCH status -> COMPLETED" -ForegroundColor Cyan
$patch3 = curl.exe -s -w "%{http_code}" -o $null -X PATCH -H "Content-Type: application/json" -d '{"status":"COMPLETED"}' "$baseUrl/$jobId/status"
if ($patch3 -eq "204") {
    Write-Host "  OK (204 No Content)" -ForegroundColor Green
} else {
    Write-Host "  FAIL: Expected 204, got $patch3" -ForegroundColor Red
}

# --------------------------------------------------------------------------------
# Step 7: Final Status (COMPLETED)
# --------------------------------------------------------------------------------
Write-Host "`n[7] GET status (expect COMPLETED)" -ForegroundColor Cyan
$statusResp3 = curl.exe -s "$baseUrl/status/$jobId"
$statusJson3 = $statusResp3 | ConvertFrom-Json
Write-Host "  status: $($statusJson3.status), processedRecords: $($statusJson3.processedRecords)" -ForegroundColor Green

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Full lifecycle test completed." -ForegroundColor Cyan
Write-Host " jobId: $jobId" -ForegroundColor Gray
Write-Host "========================================`n" -ForegroundColor Cyan
