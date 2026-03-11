================================================================================
INGESTION SERVICE - DOCUMENTATION INDEX
================================================================================

This folder contains all documentation related to the Ingestion Service.

--------------------------------------------------------------------------------
FILES
--------------------------------------------------------------------------------

IMPLEMENTATION_DESCRIPTION.txt
  - Detailed description of how each function is implemented at code level
  - Covers: validation, storage, job creation, MongoDB tracking, APIs, state machine

POSTMAN_TEST_GUIDE.txt
  - Step-by-step Postman testing instructions
  - All endpoints with method, URL, body, expected responses
  - Suggested test flow and collection tips

test-upload.ps1
  - PowerShell script for end-to-end upload + status test
  - Requires: Ingestion service on 8082, MongoDB, Python + openpyxl
  - Run from ingestion-service folder or adjust $testFile path

run-postman-tests.ps1
  - Automated script that runs ALL tests from POSTMAN_TEST_GUIDE.txt
  - Verifies each endpoint: Upload, Get Status, Update Status, Update Progress
  - Reports PASS/FAIL for each check, exits with 1 if any fail
  - Run: .\run-postman-tests.ps1

--------------------------------------------------------------------------------
QUICK REFERENCE
--------------------------------------------------------------------------------

Base URL (Ingestion):  http://localhost:8082/api/v1/ingestion
Base URL (BFF):        http://localhost:8080/api/bff/upload

Endpoints:
  POST   /upload              - Upload Excel file
  GET    /status/{jobId}      - Get job metadata & progress
  PATCH  /{jobId}/progress    - Update processed count (internal)
  PATCH  /{jobId}/status      - Transition status (internal)
