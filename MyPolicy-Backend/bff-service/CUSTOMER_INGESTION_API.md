# Customer Ingestion API - Unified External API

This is the **unified exposed API** for external customers to upload CSV/Excel files for ingestion. Customers receive an API key to authenticate their requests.

## Base URL

```
http://localhost:8080/api/public/v1/ingestion
```

In production, replace with your deployed BFF URL (e.g. `https://api.mypolicy.com/api/public/v1/ingestion`).

---

## Authentication

All endpoints require the **X-API-Key** header. Contact MyPolicy to obtain your API key.

```http
X-API-Key: your-api-key-here
```

---

## Endpoints

### Upload File

Upload a CSV or Excel file for processing. Accepted formats: `.csv`, `.xls`, `.xlsx` (max 50MB).

```http
POST /api/public/v1/ingestion/upload
Content-Type: multipart/form-data
X-API-Key: your-api-key-here

Form Data:
- file: [CSV or Excel file]
- insurerId: HDFC_LIFE          # Your organization/insurer identifier
- uploadedBy: user-123          # Your user or system ID
```

**Response (201 Created):**

```json
{
  "jobId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "UPLOADED"
}
```

**Example (curl):**

```bash
curl -X POST "http://localhost:8080/api/public/v1/ingestion/upload" \
  -H "X-API-Key: mypolicy-customer-api-key-dev" \
  -F "file=@policies.csv" \
  -F "insurerId=HDFC_LIFE" \
  -F "uploadedBy=user-001"
```

---

### Get Job Status

Check the status of an ingestion job.

```http
GET /api/public/v1/ingestion/status/{jobId}
X-API-Key: your-api-key-here
```

**Response (200 OK):**

```json
{
  "jobId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "PROCESSING",
  "processedRecords": 45,
  "totalRecords": 100,
  "filePath": "/path/to/file",
  "insurerId": "HDFC_LIFE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:31:00"
}
```

**Job Status Values:**
- `UPLOADED` - File received, queued for processing
- `PROCESSING` - Being processed
- `COMPLETED` - Successfully processed
- `FAILED` - Processing failed (check `failureReason` if available)

**Example (curl):**

```bash
curl -X GET "http://localhost:8080/api/public/v1/ingestion/status/a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  -H "X-API-Key: mypolicy-customer-api-key-dev"
```

---

## Error Responses

### 401 Unauthorized (Missing API Key)

```json
{
  "error": "Missing X-API-Key header"
}
```

### 401 Unauthorized (Invalid API Key)

```json
{
  "error": "Invalid API key"
}
```

### 400 Bad Request (Validation Error)

```json
{
  "error": "File is empty or missing"
}
```

```json
{
  "error": "Invalid file type. Allowed: .xls, .xlsx, .csv"
}
```

### 500 Internal Server Error

```json
{
  "error": "Internal server error"
}
```

---

## CORS

The API supports CORS for browser-based uploads. Configure allowed origins via:

```
ingestion.customer.cors.allowed-origins=https://customer1.com,https://customer2.com
```

Default: `*` (all origins) for development.

---

## Configuration

| Property | Description | Default |
|----------|-------------|---------|
| `ingestion.customer.api-keys` | Comma-separated valid API keys | `mypolicy-customer-api-key-dev` (dev only) |
| `INGESTION_CUSTOMER_API_KEYS` | Env override for API keys | - |
| `ingestion.customer.cors.allowed-origins` | CORS allowed origins | `*` |
