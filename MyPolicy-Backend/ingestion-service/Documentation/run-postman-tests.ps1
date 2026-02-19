# ================================================================================
# INGESTION SERVICE - AUTOMATED API TESTS
# ================================================================================
# Runs all tests from POSTMAN_TEST_GUIDE.txt and reports PASS/FAIL
# Prerequisites: Ingestion service on 8082, MongoDB running, curl.exe, Python+openpyxl
# ================================================================================

$baseUrl = "http://localhost:8082/api/v1/ingestion"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent (Split-Path -Parent $scriptDir)
$ingestionServiceDir = Join-Path $projectRoot "ingestion-service"
$testFile = Join-Path $ingestionServiceDir "test-sample.xlsx"
$passed = 0
$failed = 0
$failedTests = @()

function Write-TestResult {
    param([string]$Name, [bool]$Ok, [string]$Detail = "")
    if ($Ok) {
        Write-Host "  [PASS] $Name" -ForegroundColor Green
        if ($Detail) { Write-Host "        $Detail" -ForegroundColor Gray }
        $script:passed++
    } else {
        Write-Host "  [FAIL] $Name" -ForegroundColor Red
        if ($Detail) { Write-Host "        $Detail" -ForegroundColor Red }
        $script:failed++
        $script:failedTests += $Name
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Ingestion Service - API Test Suite" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Ensure test file exists
if (-not (Test-Path $testFile)) {
    Write-Host "Creating test Excel file..." -ForegroundColor Yellow
    Push-Location $ingestionServiceDir
    python -c "from openpyxl import Workbook; wb=Workbook(); ws=wb.active; ws['A1'],ws['B1'],ws['C1'],ws['D1']='Policy_Num','Mob_No','Premium','Sum_Assured'; ws['A2'],ws['B2'],ws['C2'],ws['D2']='POL001','9876543210',15000,1000000; wb.save('test-sample.xlsx')"
    Pop-Location
}

# --------------------------------------------------------------------------------
# TEST 1: Upload File (POST /upload)
# --------------------------------------------------------------------------------
Write-Host "Test 1: POST /upload" -ForegroundColor White
try {
    $uploadResp = curl.exe -s -w "`n%{http_code}" -X POST -F "file=@$testFile" -F "insurerId=ICICI_PRU" -F "uploadedBy=user-001" "$baseUrl/upload"
    $lines = $uploadResp -split "`n"
    $httpCode = [int]$lines[-1]
    $body = ($lines[0..($lines.Count-2)] -join "`n").Trim()
    
    if ($httpCode -eq 201) {
        $json = $body | ConvertFrom-Json
        $jobId = $json.jobId
        $status = $json.status
        
        $hasJobId = -not [string]::IsNullOrEmpty($jobId)
        $hasStatus = $status -eq "UPLOADED"
        Write-TestResult "Upload returns 201" ($httpCode -eq 201) "HTTP $httpCode"
        Write-TestResult "Response contains jobId" $hasJobId "jobId=$jobId"
        Write-TestResult "Response status is UPLOADED" $hasStatus "status=$status"
        
        if (-not $hasJobId) {
            Write-Host "  [SKIP] Remaining tests (no jobId)" -ForegroundColor Yellow
            $jobId = $null
        }
    } else {
        Write-TestResult "Upload returns 201" $false "Got HTTP $httpCode : $body"
        $jobId = $null
    }
} catch {
    Write-TestResult "Upload request" $false $_.Exception.Message
    $jobId = $null
}

if (-not $jobId) {
    $color = if ($failed -gt 0) { "Red" } else { "Green" }
    Write-Host "`n--- Summary: $passed passed, $failed failed ---`n" -ForegroundColor $color
    if ($failed -gt 0) { exit 1 } else { exit 0 }
}

# --------------------------------------------------------------------------------
# TEST 2: Get Job Status (GET /status/{jobId})
# --------------------------------------------------------------------------------
Write-Host "`nTest 2: GET /status/{jobId}" -ForegroundColor White
try {
    $statusResp = curl.exe -s -w "`n%{http_code}" "$baseUrl/status/$jobId"
    $lines = $statusResp -split "`n"
    $httpCode = [int]$lines[-1]
    $body = ($lines[0..($lines.Count-2)] -join "`n").Trim()
    
    Write-TestResult "Get status returns 200" ($httpCode -eq 200) "HTTP $httpCode"
    
    if ($httpCode -eq 200) {
        $json = $body | ConvertFrom-Json
        Write-TestResult "Response has jobId" ($json.jobId -eq $jobId) "jobId match"
        Write-TestResult "Response has status" ($null -ne $json.status) "status=$($json.status)"
        Write-TestResult "Response has filePath" (-not [string]::IsNullOrEmpty($json.filePath)) "filePath present"
        Write-TestResult "Response has insurerId" ($json.insurerId -eq "ICICI_PRU") "insurerId=$($json.insurerId)"
        Write-TestResult "Response has processedRecords" ($null -ne $json.processedRecords) "processedRecords=$($json.processedRecords)"
        Write-TestResult "Response has totalRecords" ($null -ne $json.totalRecords) "totalRecords=$($json.totalRecords)"
    } else {
        Write-TestResult "Get status body" $false "Got: $body"
    }
} catch {
    Write-TestResult "Get status request" $false $_.Exception.Message
}

# --------------------------------------------------------------------------------
# TEST 3: Update Status to PROCESSING (PATCH /{jobId}/status)
# --------------------------------------------------------------------------------
Write-Host "`nTest 3: PATCH /{jobId}/status (UPLOADED -> PROCESSING)" -ForegroundColor White
try {
    $patchResp = curl.exe -s -w "`n%{http_code}" -X PATCH -H "Content-Type: application/json" -d "{\"status\":\"PROCESSING\"}" "$baseUrl/$jobId/status"
    $lines = $patchResp -split "`n"
    $httpCode = [int]$lines[-1]
    Write-TestResult "Update status returns 204" ($httpCode -eq 204) "HTTP $httpCode"
} catch {
    Write-TestResult "Update status request" $false $_.Exception.Message
}

# --------------------------------------------------------------------------------
# TEST 4: Update Progress (PATCH /{jobId}/progress)
# --------------------------------------------------------------------------------
Write-Host "`nTest 4: PATCH /{jobId}/progress" -ForegroundColor White
try {
    $progressResp = curl.exe -s -w "`n%{http_code}" -X PATCH -H "Content-Type: application/json" -d "{\"processedRecordsDelta\":10}" "$baseUrl/$jobId/progress"
    $lines = $progressResp -split "`n"
    $httpCode = [int]$lines[-1]
    Write-TestResult "Update progress returns 204" ($httpCode -eq 204) "HTTP $httpCode"
} catch {
    Write-TestResult "Update progress request" $false $_.Exception.Message
}

# --------------------------------------------------------------------------------
# TEST 5: Get Status again - verify processedRecords
# --------------------------------------------------------------------------------
Write-Host "`nTest 5: GET /status/{jobId} (verify progress)" -ForegroundColor White
try {
    $statusResp2 = curl.exe -s -w "`n%{http_code}" "$baseUrl/status/$jobId"
    $lines = $statusResp2 -split "`n"
    $httpCode = [int]$lines[-1]
    $body = ($lines[0..($lines.Count-2)] -join "`n").Trim()
    
    if ($httpCode -eq 200) {
        $json = $body | ConvertFrom-Json
        Write-TestResult "processedRecords updated to 10" ($json.processedRecords -eq 10) "processedRecords=$($json.processedRecords)"
        Write-TestResult "status is PROCESSING" ($json.status -eq "PROCESSING") "status=$($json.status)"
    } else {
        Write-TestResult "Get status (progress check)" $false "HTTP $httpCode"
    }
} catch {
    Write-TestResult "Get status (progress check)" $false $_.Exception.Message
}

# --------------------------------------------------------------------------------
# TEST 6: Update Status to COMPLETED (PATCH /{jobId}/status)
# --------------------------------------------------------------------------------
Write-Host "`nTest 6: PATCH /{jobId}/status (PROCESSING -> COMPLETED)" -ForegroundColor White
try {
    $patchResp2 = curl.exe -s -w "`n%{http_code}" -X PATCH -H "Content-Type: application/json" -d "{\"status\":\"COMPLETED\"}" "$baseUrl/$jobId/status"
    $lines = $patchResp2 -split "`n"
    $httpCode = [int]$lines[-1]
    Write-TestResult "Update status to COMPLETED returns 204" ($httpCode -eq 204) "HTTP $httpCode"
} catch {
    Write-TestResult "Update status to COMPLETED" $false $_.Exception.Message
}

# --------------------------------------------------------------------------------
# TEST 7: Get Status - verify final COMPLETED state
# --------------------------------------------------------------------------------
Write-Host "`nTest 7: GET /status/{jobId} (verify final state)" -ForegroundColor White
try {
    $statusResp3 = curl.exe -s -w "`n%{http_code}" "$baseUrl/status/$jobId"
    $lines = $statusResp3 -split "`n"
    $httpCode = [int]$lines[-1]
    $body = ($lines[0..($lines.Count-2)] -join "`n").Trim()
    
    if ($httpCode -eq 200) {
        $json = $body | ConvertFrom-Json
        Write-TestResult "Final status is COMPLETED" ($json.status -eq "COMPLETED") "status=$($json.status)"
        Write-TestResult "processedRecords is 10" ($json.processedRecords -eq 10) "processedRecords=$($json.processedRecords)"
    } else {
        Write-TestResult "Get final status" $false "HTTP $httpCode"
    }
} catch {
    Write-TestResult "Get final status" $false $_.Exception.Message
}

# --------------------------------------------------------------------------------
# Summary
# --------------------------------------------------------------------------------
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Passed: $passed" -ForegroundColor Green
Write-Host " Failed: $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })
if ($failedTests.Count -gt 0) {
    Write-Host " Failed tests: $($failedTests -join ', ')" -ForegroundColor Red
}
Write-Host "========================================`n" -ForegroundColor Cyan

if ($failed -gt 0) { exit 1 } else { exit 0 }
