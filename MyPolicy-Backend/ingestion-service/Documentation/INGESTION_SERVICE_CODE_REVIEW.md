# Ingestion Service — Code Review Documentation

This document details the purpose and responsibilities of each file in the ingestion-service for code review purposes.

---

## Project Structure

```
ingestion-service/
├── pom.xml
├── src/main/
│   ├── java/com/mypolicy/ingestion/
│   │   ├── IngestionServiceApplication.java
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── model/
│   │   ├── dto/
│   │   ├── config/
│   │   └── exception/
│   └── resources/
│       └── application.properties
└── Documentation/
```

---

## 1. Application Entry Point

### `IngestionServiceApplication.java`
- **Location:** `com.mypolicy.ingestion`
- **Purpose:** Spring Boot application main class. Bootstraps the service with `@SpringBootApplication` (component scanning, auto-configuration, embedded Tomcat).
- **Responsibilities:** Single `main()` method that runs `SpringApplication.run()`.

---

## 2. Controllers

### `IngestionController.java`
- **Location:** `com.mypolicy.ingestion.controller`
- **Path prefix:** `/api/v1/ingestion`
- **Purpose:** Internal REST API used by BFF and Processing Service. No API key required (JWT/auth handled by BFF).
- **Endpoints:**
  | Method | Path | Description |
  |--------|------|-------------|
  | POST | /upload | Upload CSV/Excel; accepts `file`, `insurerId`, `uploadedBy`, optional `fileType` |
  | GET | /status/{jobId} | Return job status for BFF/Processing |
  | PATCH | /{jobId}/progress | Processing Service updates processed record count |
  | PATCH | /{jobId}/status | Processing Service transitions job state |
- **Responsibilities:** Validates request params, delegates to `IngestionService`, handles `IllegalArgumentException` (re-throws) and `IOException` (wraps in `RuntimeException`), returns appropriate HTTP status codes.

### `PublicIngestionController.java`
- **Location:** `com.mypolicy.ingestion.controller`
- **Path prefix:** `/api/public/v1/ingestion`
- **Purpose:** Exposed customer-facing API for external insurers. Requires `X-API-Key` header (validated by `ApiKeyAuthFilter`).
- **Endpoints:**
  | Method | Path | Description |
  |--------|------|-------------|
  | POST | /upload | Same as internal; requires valid API key |
  | GET | /status/{jobId} | Same as internal; requires valid API key |
- **Responsibilities:** Thin controller; delegates to `IngestionService`. No business logic. Same error handling as internal controller.

---

## 3. Service Layer

### `IngestionService.java`
- **Location:** `com.mypolicy.ingestion.service`
- **Purpose:** Core business logic for file intake, validation, storage, and job lifecycle. Does **not** parse or transform policy data (that is done by Processing Service).
- **Key methods:**
  | Method | Description |
  |--------|-------------|
  | `uploadFile()` | Validates file → resolves fileType (param or filename `_correction`) → saves to disk → creates job in MongoDB → returns jobId |
  | `getJobStatus()` | Fetches job by ID, maps to `JobStatusResponse` |
  | `updateProgress()` | Increments `processedRecords` only when status is PROCESSING |
  | `updateStatus()` | Transitions job state; enforces state machine |
  | `setTotalRecords()` | Sets total record count (for Processing Service) |
- **Validation:** `validateFile()` — checks non-empty, size ≤ 50MB, allowed extensions (.csv, .xls, .xlsx).
- **State machine:** `validateStateTransition()` — UPLOADED → PROCESSING → COMPLETED | FAILED; no backward or skip transitions.
- **File type resolution:** `resolveFileType()` — uses `fileType` param if "correction", else detects from filename containing `_correction`.

---

## 4. Repository

### `IngestionJobRepository.java`
- **Location:** `com.mypolicy.ingestion.repository`
- **Purpose:** Spring Data MongoDB repository for `IngestionJob` documents.
- **Interface:** Extends `MongoRepository<IngestionJob, String>`.
- **Custom queries:**
  | Method | Description |
  |--------|-------------|
  | `findByStatus(IngestionStatus)` | Jobs by status (e.g. UPLOADED) |
  | `findByInsurerId(String)` | Jobs by insurer |
- **Note:** Standard CRUD (`findById`, `save`) used by service; custom methods available for future queries.

---

## 5. Model / Domain

### `IngestionJob.java`
- **Location:** `com.mypolicy.ingestion.model`
- **Purpose:** MongoDB document representing an ingestion job. Stores file metadata and processing state.
- **Collection:** `ingestion_jobs`
- **Fields:**
  | Field | Type | Description |
  |-------|------|-------------|
  | jobId | String | Primary key (UUID) |
  | insurerId | String | Insurer identifier |
  | filePath | String | Absolute path to stored file |
  | fileType | String | "normal" or "correction" |
  | status | IngestionStatus | @Indexed for queries |
  | totalRecords | int | Total rows (set by Processing) |
  | processedRecords | int | Rows processed so far |
  | uploadedBy | String | User/system that uploaded |
  | failureReason | String | Populated when status = FAILED |
  | createdAt, updatedAt | LocalDateTime | Timestamps; createdAt @Indexed |

### `IngestionStatus.java`
- **Location:** `com.mypolicy.ingestion.model`
- **Purpose:** Enum for job lifecycle state.
- **Values:** `UPLOADED`, `PROCESSING`, `COMPLETED`, `FAILED`.

### `ValidationError.java`
- **Location:** `com.mypolicy.ingestion.model`
- **Purpose:** DTO for row-level validation errors (e.g. field validation during parsing).
- **Fields:** `rowNumber`, `field`, `errorMessage`, `value`.
- **Note:** Defined but not currently used in ingestion-service (may be used by Processing Service or future validation features).

---

## 6. DTOs (Data Transfer Objects)

### `UploadResponse.java`
- **Location:** `com.mypolicy.ingestion.dto`
- **Purpose:** Response body for successful upload.
- **Fields:** `jobId`, `status` (IngestionStatus).

### `JobStatusResponse.java`
- **Location:** `com.mypolicy.ingestion.dto`
- **Purpose:** Response body for job status GET.
- **Fields:** `jobId`, `status`, `processedRecords`, `totalRecords`, `filePath`, `insurerId`, `fileType`, `createdAt`, `updatedAt`.

### `ProgressUpdateRequest.java`
- **Location:** `com.mypolicy.ingestion.dto`
- **Purpose:** Request body for PATCH progress.
- **Fields:** `processedRecordsDelta` (must be positive).
- **Validation:** Handled in service; could add `@Min(1)` for Bean Validation.

### `StatusUpdateRequest.java`
- **Location:** `com.mypolicy.ingestion.dto`
- **Purpose:** Request body for PATCH status.
- **Fields:** `status` (required), `failureReason` (optional, used when status=FAILED).

---

## 7. Configuration

### `ApiKeyAuthFilter.java`
- **Location:** `com.mypolicy.ingestion.config`
- **Purpose:** Servlet filter that validates `X-API-Key` header for public API.
- **Behavior:** Extends `OncePerRequestFilter`. Applies only to URIs starting with `/api/public/` (see `shouldNotFilter()`).
- **Logic:** If header missing or invalid → 401 JSON response; else continues filter chain.
- **Config:** Accepts comma-separated list of valid keys in constructor.

### `ApiKeyFilterConfig.java`
- **Location:** `com.mypolicy.ingestion.config`
- **Purpose:** Registers `ApiKeyAuthFilter` with Spring.
- **Bean:** `FilterRegistrationBean<ApiKeyAuthFilter>` — wires `ingestion.customer.api-keys` from properties, applies to `/api/public/v1/ingestion/*`, order 1.

### `PublicApiCorsConfig.java`
- **Location:** `com.mypolicy.ingestion.config`
- **Purpose:** CORS configuration for public API (cross-origin requests from customer apps).
- **Config:** Allowed origins (default `*`), methods (GET, POST, OPTIONS), headers (Authorization, X-API-Key, Content-Type), exposed headers (Location).
- **Path:** `/api/public/**`

---

## 8. Exception Handling

### `GlobalExceptionHandler.java`
- **Location:** `com.mypolicy.ingestion.exception`
- **Purpose:** Centralized exception handling via `@RestControllerAdvice`.
- **Mappings:**
  | Exception | HTTP Status | Response body |
  |-----------|-------------|---------------|
  | IllegalArgumentException | 400 Bad Request | `{"error": "<message>"}` |
  | IllegalStateException | 409 Conflict | `{"error": "<message>"}` |
  | MongoException | 503 Service Unavailable | `{"error": "Database error: ..."}` |
  | RuntimeException | 500 Internal Server Error | `{"error": "Internal server error"}` |

---

## 9. Configuration Files

### `application.properties`
- **Location:** `src/main/resources`
- **Key properties:**
  | Property | Default | Description |
  |----------|---------|-------------|
  | server.port | 8082 | HTTP port |
  | spring.data.mongodb.host/port/database | localhost, 27017, mypolicy_ingestion_db | MongoDB connection |
  | ingestion.customer.api-keys | mypolicy-customer-api-key-dev | Comma-separated API keys; overridable via INGESTION_CUSTOMER_API_KEYS |
  | ingestion.storage.path | storage/ingestion | Base path for uploaded files |
  | spring.servlet.multipart.max-file-size | 50MB | Max upload size |

### `pom.xml`
- **Dependencies:** spring-boot-starter-web, spring-boot-starter-data-mongodb, spring-boot-starter-validation, lombok, spring-boot-starter-test (scope test).
- **Parent:** Spring Boot 3.1.5, Java 17.

---

## 10. Flow Summary

| Flow | Entry | Path |
|------|-------|------|
| Internal upload | BFF / Processing | POST /api/v1/ingestion/upload → IngestionController → IngestionService |
| External upload | Customer app | POST /api/public/v1/ingestion/upload → ApiKeyAuthFilter → PublicIngestionController → IngestionService |
| Job status | BFF / Processing / Customer | GET /status/{jobId} (internal or public) → IngestionService |
| Progress update | Processing Service | PATCH /{jobId}/progress → IngestionController → IngestionService |
| Status update | Processing Service | PATCH /{jobId}/status → IngestionController → IngestionService |

---

## 11. Code Review Checklist

- [ ] **Controllers:** Thin; delegate to service; no business logic
- [ ] **Service:** All business logic; stateless; state machine enforced
- [ ] **Security:** API key filter only on /api/public/*; internal API unprotected (BFF handles auth)
- [ ] **Error handling:** Centralized; consistent JSON format
- [ ] **Validation:** File validation in service; request DTOs could add Bean Validation
- [ ] **Persistence:** MongoDB; document model; indexed fields for status and createdAt

---

*Document: Ingestion Service Code Review | MyPolicy Backend | Version 1.0*
