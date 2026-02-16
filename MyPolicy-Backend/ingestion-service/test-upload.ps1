# End-to-end test for Ingestion Service
# Prerequisites: Ingestion service running on 8082, MongoDB running

$baseUrl = "http://localhost:8082/api/v1/ingestion"
$testFile = Join-Path $PSScriptRoot "test-sample.xlsx"

if (-not (Test-Path $testFile)) {
    Write-Host "Creating test Excel file (requires Python + openpyxl)..."
    Push-Location $PSScriptRoot
    python -c "from openpyxl import Workbook; wb=Workbook(); ws=wb.active; ws['A1'],ws['B1'],ws['C1'],ws['D1']='Policy_Num','Mob_No','Premium','Sum_Assured'; ws['A2'],ws['B2'],ws['C2'],ws['D2']='POL001','9876543210',15000,1000000; wb.save('test-sample.xlsx')"
    Pop-Location
}

Write-Host "`n1. Uploading file..." -ForegroundColor Cyan
$uploadResponse = curl.exe -s -X POST -F "file=@$testFile" -F "insurerId=ICICI_PRU" -F "uploadedBy=user-001" "$baseUrl/upload"
Write-Host $uploadResponse

$jobId = ($uploadResponse | ConvertFrom-Json).jobId
if (-not $jobId) {
    Write-Host "Upload failed!" -ForegroundColor Red
    exit 1
}

Write-Host "`n2. Fetching job status for jobId: $jobId" -ForegroundColor Cyan
$statusResponse = curl.exe -s "$baseUrl/status/$jobId"
Write-Host $statusResponse

Write-Host "`nDone. Ingestion service is working." -ForegroundColor Green
