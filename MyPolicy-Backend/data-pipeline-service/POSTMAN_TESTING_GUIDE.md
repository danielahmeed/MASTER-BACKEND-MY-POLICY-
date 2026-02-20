# Data Pipeline Service - Postman Testing Guide

## Service Information

- **Base URL**: `http://localhost:8082`
- **Service**: Data Pipeline Service (Consolidated)
- **Port**: 8082

---

## Prerequisites

1. PostgreSQL running on `localhost:5432`
   - Database: `mypolicy_db`
   - Username: `postgres`
   - Password: `postgres123`

2. MongoDB running on `localhost:27017`
   - Database: `ingestion_db`

3. A valid JWT token (for authenticated endpoints)

---

## API Endpoints

### 1. Metadata Module

#### 1.1 Health Check

```
GET http://localhost:8082/api/v1/metadata/health
```

**Response:**

```
Metadata Module: OK
```

#### 1.2 Create/Update Insurer Configuration

```
POST http://localhost:8082/api/v1/metadata/config?insurerId=HDFC_LIFE&insurerName=HDFC Life Insurance
Content-Type: application/json

{
  "TERM_LIFE": [
    {
      "clientFieldName": "policyNumber",
      "insurerFieldName": "Policy_No",
      "dataType": "STRING",
      "required": true,
      "defaultValue": null
    },
    {
      "clientFieldName": "customerName",
      "insurerFieldName": "Insured_Name",
      "dataType": "STRING",
      "required": true,
      "defaultValue": null
    },
    {
      "clientFieldName": "premiumAmount",
      "insurerFieldName": "Premium_Amt",
      "dataType": "DECIMAL",
      "required": true,
      "defaultValue": "0.00"
    }
  ]
}
```

#### 1.3 Get Insurer Configuration

```
GET http://localhost:8082/api/v1/metadata/config/HDFC_LIFE
```

---

### 2. Ingestion Module

#### 2.1 Upload File (Requires JWT)

```
POST http://localhost:8082/api/v1/ingestion/upload
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: multipart/form-data

Form Data:
- file: [Select your Excel/CSV file]
- insurerId: HDFC_LIFE
```

**Example Response:**

```json
{
  "success": true,
  "message": "File uploaded successfully",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "policies.xlsx",
    "status": "UPLOADED",
    "uploadedAt": "2026-02-20T10:30:00"
  },
  "timestamp": "2026-02-20T10:30:00"
}
```

**Note**: To get a JWT token, you need to authenticate with the Auth Service first.

#### 2.2 Get Job Status

```
GET http://localhost:8082/api/v1/ingestion/status/{jobId}
```

**Example:**

```
GET http://localhost:8082/api/v1/ingestion/status/550e8400-e29b-41d4-a716-446655440000
```

**Response:**

```json
{
  "success": true,
  "message": "Job status retrieved successfully",
  "data": {
    "jobId": "550e8400-e29b-41d4-a716-446655440000",
    "fileName": "policies.xlsx",
    "status": "UPLOADED",
    "totalRecords": 100,
    "processedRecords": 0,
    "uploadedAt": "2026-02-20T10:30:00",
    "uploadedBy": "admin@mypolicy.com"
  },
  "timestamp": "2026-02-20T10:31:00"
}
```

#### 2.3 Update Progress (Internal API)

```
PATCH http://localhost:8082/api/v1/ingestion/{jobId}/progress
Content-Type: application/json

{
  "processedRecords": 50
}
```

#### 2.4 Update Status (Internal API)

```
PATCH http://localhost:8082/api/v1/ingestion/{jobId}/status
Content-Type: application/json

{
  "status": "PROCESSING"
}
```

---

### 3. Processing Module

#### 3.1 Health Check

```
GET http://localhost:8082/api/v1/processing/health
```

#### 3.2 Trigger Processing

```
POST http://localhost:8082/api/v1/processing/trigger?jobId=550e8400-e29b-41d4-a716-446655440000&policyType=TERM_LIFE
```

**Response:**

```
Processing started for jobId: 550e8400-e29b-41d4-a716-446655440000
```

---

## Postman Collection Setup

### Step 1: Create Environment Variables

1. In Postman, create a new Environment
2. Add variables:
   - `baseUrl`: `http://localhost:8082`
   - `jwtToken`: `your_jwt_token_here`
   - `jobId`: (will be set from upload response)

### Step 2: Import Collection Structure

Create folders in Postman:

1. **Metadata APIs**
   - Health Check
   - Create Configuration
   - Get Configuration

2. **Ingestion APIs**
   - Upload File
   - Get Status
   - Update Progress
   - Update Status

3. **Processing APIs**
   - Health Check
   - Trigger Processing

### Step 3: Test Flow

1. **Setup Metadata** (First Time Only)

   ```
   POST {{baseUrl}}/api/v1/metadata/config?insurerId=HDFC_LIFE&insurerName=HDFC Life
   ```

2. **Upload File**

   ```
   POST {{baseUrl}}/api/v1/ingestion/upload
   Authorization: Bearer {{jwtToken}}
   ```

   - Save the `jobId` from response

3. **Check Status**

   ```
   GET {{baseUrl}}/api/v1/ingestion/status/{{jobId}}
   ```

4. **Trigger Processing**

   ```
   POST {{baseUrl}}/api/v1/processing/trigger?jobId={{jobId}}&policyType=TERM_LIFE
   ```

5. **Monitor Progress**
   ```
   GET {{baseUrl}}/api/v1/ingestion/status/{{jobId}}
   ```

---

## Sample Test Files

### Sample CSV File (policies.csv)

```csv
Policy_No,Insured_Name,Premium_Amt,Start_Date,End_Date
POL001,John Doe,25000,2026-01-01,2027-01-01
POL002,Jane Smith,30000,2026-01-15,2027-01-15
POL003,Bob Johnson,28000,2026-02-01,2027-02-01
```

---

## Common Errors & Solutions

### 1. 401 Unauthorized

- **Cause**: Missing or invalid JWT token
- **Solution**: Get a valid token from Auth Service first

### 2. 400 Bad Request - Validation Error

- **Cause**: Invalid file format or missing required fields
- **Solution**:
  - Ensure file is .xlsx, .xls, or .csv
  - Check file size < 50MB
  - Verify insurerId is configured in metadata

### 3. 404 Not Found - Job Not Found

- **Cause**: Invalid jobId
- **Solution**: Verify jobId from upload response

### 4. 500 Internal Server Error

- **Cause**: Database connection issues or server errors
- **Solution**: Check service logs and database connectivity

---

## Mock JWT Token (for testing without Auth Service)

If you want to test without the Auth Service, you can temporarily disable JWT validation or use this structure:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBteXBvbGljeS5jb20iLCJuYW1lIjoiQWRtaW4gVXNlciIsImlhdCI6MTUxNjIzOTAyMn0.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

Note: This is a sample token and needs to be generated with your secret key.

---

## Next Steps

1. Start PostgreSQL and MongoDB
2. Run the application: `mvn spring-boot:run`
3. Test endpoints in order: Health → Metadata → Upload → Status → Processing
4. Check logs for any errors: `logs/data-pipeline-service.log`
