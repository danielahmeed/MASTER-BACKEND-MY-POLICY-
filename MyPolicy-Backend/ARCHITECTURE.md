# MyPolicy System - Complete Architecture Documentation

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Diagram](#architecture-diagram)
3. [Service Details](#service-details)
4. [Data Flow](#data-flow)
5. [API Endpoints](#api-endpoints)
6. [Database Schema](#database-schema)
7. [Deployment Guide](#deployment-guide)
8. [Testing Guide](#testing-guide)

---

## System Overview

**MyPolicy** is a comprehensive insurance policy aggregation platform that allows users to:
- Upload insurance policies from multiple insurers
- View all policies in a unified dashboard
- Get intelligent coverage insights and recommendations
- Identify coverage gaps
- Receive personalized advisory

### Key Features
✅ Multi-insurer policy aggregation
✅ Automated data processing with metadata-driven transformation
✅ Intelligent customer matching with fuzzy logic
✅ Unified portfolio view
✅ Coverage gap analysis
✅ AI-powered recommendations
✅ Secure authentication with JWT
✅ PII encryption

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend Application                          │
│                    (Web / Mobile / Admin)                        │
└────────────────────────────┬────────────────────────────────────┘
                             │ HTTPS
                             ▼
                    ┌────────────────┐
                    │  BFF Service   │ Port 8080
                    │  (API Gateway) │
                    │  - Auth        │
                    │  - Portfolio   │
                    │  - Insights    │
                    │  - Upload      │
                    └────────┬───────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Customer   │    │    Policy    │    │  Ingestion   │
│   Service    │    │   Service    │    │   Service    │
│   :8081      │    │   :8085      │    │   :8082      │
│              │    │              │    │              │
│ - Register   │    │ - Create     │    │ - Upload     │
│ - Login/JWT  │    │ - Retrieve   │    │ - Validate   │
│ - Profile    │    │ - Link       │    │ - Track      │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                    │
       ▼                   ▼                    ▼
  PostgreSQL          PostgreSQL            MongoDB
  customer_db         policy_db         ingestion_db

        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Metadata    │    │  Processing  │    │   Matching   │
│  Service     │    │   Service    │    │   Engine     │
│   :8083      │    │   :8084      │    │   :8086      │
│              │    │              │    │              │
│ - Mappings   │◄───│ - Transform  │───►│ - Fuzzy      │
│ - Rules      │    │ - Parse      │    │   Match      │
│ - Config     │    │ - Validate   │    │ - Link       │
└──────┬───────┘    └──────────────┘    └──────────────┘
       │
       ▼
  PostgreSQL
  metadata_db
```

---

## Service Details

### 1. BFF Service (Backend for Frontend) - Port 8080

**Purpose**: API Gateway and request aggregator for frontend applications

**Responsibilities**:
- Single entry point for all frontend requests
- Request aggregation (combine multiple service calls)
- Response transformation (frontend-optimized format)
- JWT authentication validation
- Error handling and fallback

**Key Endpoints**:
```
POST   /api/bff/auth/register          - User registration
POST   /api/bff/auth/login             - Authentication & JWT
GET    /api/bff/portfolio/{customerId} - Unified portfolio view
GET    /api/bff/insights/{customerId}  - Coverage analysis
POST   /api/bff/upload                 - File upload
GET    /api/bff/upload/status/{jobId}  - Upload status
```

**Technology Stack**:
- Spring Boot 3.1.5
- Spring Cloud OpenFeign
- Spring Security
- JWT (io.jsonwebtoken)

---

### 2. Customer Service - Port 8081

**Purpose**: User management and authentication

**Responsibilities**:
- Customer registration with PII encryption
- JWT-based authentication
- Password hashing (BCrypt)
- Customer profile management
- Customer search and retrieval

**Database**: PostgreSQL (`mypolicy_customer_db`)

**Schema**:
```sql
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) UNIQUE,      -- Encrypted
    mobile_number VARCHAR(20),      -- Encrypted
    pan_number VARCHAR(10),         -- Encrypted
    date_of_birth VARCHAR(255),     -- Encrypted
    address TEXT,                   -- Encrypted
    password_hash VARCHAR(255),
    status VARCHAR(20),             -- ACTIVE, INACTIVE, SUSPENDED
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

**Key Features**:
- AES-256 encryption for PII
- BCrypt password hashing
- JWT token generation
- Customer status management

---

### 3. Ingestion Service - Port 8082

**Purpose**: Handle file uploads from insurers

**Responsibilities**:
- Accept Excel/CSV file uploads
- Create ingestion jobs
- Track processing status
- Store validation errors
- File metadata management

**Database**: MongoDB (`mypolicy_ingestion_db`)

**Schema**:
```javascript
{
  _id: ObjectId,
  jobId: String,
  customerId: String,
  fileName: String,
  fileType: String,        // EXCEL, CSV
  filePath: String,
  insurerId: String,
  status: String,          // UPLOADED, PROCESSING, COMPLETED, FAILED
  totalRecords: Number,
  processedRecords: Number,
  validationErrors: [
    {
      row: Number,
      field: String,
      message: String
    }
  ],
  uploadedAt: Date,
  completedAt: Date
}
```

**Supported Formats**:
- Excel (.xlsx, .xls)
- CSV (.csv)

---

### 4. Metadata Service - Port 8083

**Purpose**: Store and manage field mapping rules for different insurers

**Responsibilities**:
- Store insurer-specific field mappings
- Manage policy type configurations
- Provide mapping rules to Processing Service
- Support multiple policy types per insurer

**Database**: PostgreSQL (`mypolicy_metadata_db`)

**Schema**:
```sql
CREATE TABLE insurer_configurations (
    config_id UUID PRIMARY KEY,
    insurer_id VARCHAR(50) UNIQUE,
    insurer_name VARCHAR(255),
    field_mappings JSONB,           -- Flexible mapping storage
    active BOOLEAN DEFAULT true,
    updated_at TIMESTAMP
);
```

**Field Mapping Structure**:
```json
{
  "TERM_LIFE": [
    {
      "sourceField": "Mob_No",
      "targetField": "mobileNumber",
      "dataType": "STRING",
      "required": true,
      "transformFunction": null
    },
    {
      "sourceField": "Policy_Num",
      "targetField": "policyNumber",
      "dataType": "STRING",
      "required": true,
      "transformFunction": null
    }
  ]
}
```

---

### 5. Processing Service - Port 8084

**Purpose**: Read uploaded files, apply metadata rules, and standardize data

**Responsibilities**:
- Fetch metadata rules from Metadata Service
- Read Excel/CSV files using Apache POI
- Transform data using field mappings
- Validate data integrity
- Send standardized records to Matching Engine

**Technology Stack**:
- Spring Boot 3.1.5
- Apache POI (Excel processing)
- Spring Cloud OpenFeign

**Processing Flow**:
```
1. Receive file path, insurerId, policyType
2. Call Metadata Service → Get field mappings
3. Read file row by row
4. Apply transformations
5. Validate required fields
6. Create standardized records
7. Send to Matching Engine
```

---

### 6. Policy Service - Port 8085

**Purpose**: Store and manage insurance policies

**Responsibilities**:
- Policy creation and storage
- Link policies to customers
- Policy retrieval by customer
- Policy status management
- Support multiple policy types

**Database**: PostgreSQL (`mypolicy_policy_db`)

**Schema**:
```sql
CREATE TABLE policies (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(255),
    insurer_id VARCHAR(100),
    policy_number VARCHAR(100) UNIQUE,
    policy_type VARCHAR(50),        -- TERM_LIFE, HEALTH, MOTOR, etc.
    plan_name VARCHAR(255),
    premium_amount DECIMAL(15,2),
    sum_assured DECIMAL(15,2),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20),             -- ACTIVE, EXPIRED, LAPSED, etc.
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_customer_id ON policies(customer_id);
CREATE INDEX idx_policy_number ON policies(policy_number);
```

---

### 7. Matching Engine - Port 8086

**Purpose**: Match processed policy data with existing customers using fuzzy logic

**Responsibilities**:
- Customer search and matching
- Fuzzy name matching (Levenshtein distance)
- Exact matching on PAN/Email/Mobile
- Policy creation via Policy Service
- Handle duplicate detection

**Technology Stack**:
- Spring Boot 3.1.5
- Apache Commons Text (Levenshtein distance)
- Spring Cloud OpenFeign

**Matching Algorithm**:
```
1. Receive standardized policy record
2. Extract customer identifiers (name, mobile, email, PAN)
3. Search Customer Service:
   a. Exact match on PAN (highest priority)
   b. Exact match on Email
   c. Exact match on Mobile
   d. Fuzzy match on Name (Levenshtein distance ≤ 3)
4. If match found → Link policy to existing customer
5. If no match → Create new customer record
6. Create policy in Policy Service
```

---

## Data Flow

### Flow 1: User Registration
```
User → BFF → Customer Service → PostgreSQL
                ↓
            JWT Token
                ↓
            User (Frontend)
```

### Flow 2: User Login
```
User → BFF → Customer Service
                ↓
         Validate Password
                ↓
         Generate JWT Token
                ↓
            User (Frontend)
```

### Flow 3: Portfolio View (Aggregation)
```
User → BFF
        ↓
    [Parallel Calls]
        ├─→ Customer Service → Get customer details
        └─→ Policy Service → Get all policies
        ↓
    Aggregate Data
        ↓
    Calculate Totals
        ↓
    Return Unified Response
```

### Flow 4: Coverage Insights
```
User → BFF → InsightsService
                ↓
    [Parallel Calls]
        ├─→ Customer Service
        └─→ Policy Service
        ↓
    Analyze Coverage
        ↓
    Identify Gaps
        ↓
    Generate Recommendations
        ↓
    Calculate Score
        ↓
    Return Insights
```

### Flow 5: File Upload & Processing (Complete Pipeline)
```
User → BFF → Ingestion Service
                ↓
            Save to MongoDB
                ↓
            Return Job ID
                ↓
        Processing Service (Triggered)
                ↓
        Fetch Metadata Rules
                ↓
        Read Excel File
                ↓
        Transform Data
                ↓
        Matching Engine
                ↓
    [Customer Matching]
        ├─→ Find/Create Customer
        └─→ Create Policy
                ↓
            Complete
```

---

## API Endpoints

### BFF Service (Port 8080)

#### Authentication
```http
POST /api/bff/auth/register
POST /api/bff/auth/login
```

#### Portfolio
```http
GET /api/bff/portfolio/{customerId}
```

#### Insights
```http
GET /api/bff/insights/{customerId}
```

#### File Upload
```http
POST /api/bff/upload
GET /api/bff/upload/status/{jobId}
```

### Customer Service (Port 8081)
```http
POST /api/v1/customers/register
POST /api/v1/customers/login
GET  /api/v1/customers/{customerId}
```

### Ingestion Service (Port 8082)
```http
POST /api/v1/ingestion/upload
GET  /api/v1/ingestion/status/{jobId}
```

### Metadata Service (Port 8083)
```http
POST /api/v1/metadata/config
GET  /api/v1/metadata/config/{insurerId}
```

### Processing Service (Port 8084)
```http
POST /api/v1/processing/trigger
```

### Policy Service (Port 8085)
```http
POST /api/v1/policies
GET  /api/v1/policies/customer/{customerId}
GET  /api/v1/policies/{id}
```

---

## Database Schema

### PostgreSQL Databases

#### 1. mypolicy_customer_db
```sql
-- Customers table
CREATE TABLE customers (
    customer_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    mobile_number VARCHAR(20) NOT NULL,
    pan_number VARCHAR(10),
    date_of_birth VARCHAR(255),
    address TEXT,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email ON customers(email);
CREATE INDEX idx_mobile ON customers(mobile_number);
CREATE INDEX idx_pan ON customers(pan_number);
```

#### 2. mypolicy_metadata_db
```sql
-- Insurer configurations table
CREATE TABLE insurer_configurations (
    config_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insurer_id VARCHAR(50) UNIQUE NOT NULL,
    insurer_name VARCHAR(255) NOT NULL,
    field_mappings JSONB NOT NULL,
    active BOOLEAN DEFAULT true,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_insurer_id ON insurer_configurations(insurer_id);
```

#### 3. mypolicy_policy_db
```sql
-- Policies table
CREATE TABLE policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id VARCHAR(255) NOT NULL,
    insurer_id VARCHAR(100) NOT NULL,
    policy_number VARCHAR(100) UNIQUE NOT NULL,
    policy_type VARCHAR(50) NOT NULL,
    plan_name VARCHAR(255),
    premium_amount DECIMAL(15,2) NOT NULL,
    sum_assured DECIMAL(15,2) NOT NULL,
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_id ON policies(customer_id);
CREATE INDEX idx_policy_number ON policies(policy_number);
CREATE INDEX idx_insurer_id ON policies(insurer_id);
```

### MongoDB Database

#### mypolicy_ingestion_db
```javascript
// Collection: ingestion_jobs
{
  _id: ObjectId,
  jobId: String,              // Unique job identifier
  customerId: String,         // User who uploaded
  fileName: String,           // Original filename
  fileType: String,           // EXCEL or CSV
  filePath: String,           // Storage path
  insurerId: String,          // Insurer identifier
  status: String,             // UPLOADED, PROCESSING, COMPLETED, FAILED
  totalRecords: Number,       // Total rows in file
  processedRecords: Number,   // Successfully processed
  validationErrors: [         // Validation issues
    {
      row: Number,
      field: String,
      message: String
    }
  ],
  uploadedAt: Date,
  completedAt: Date
}

// Indexes
db.ingestion_jobs.createIndex({ jobId: 1 }, { unique: true })
db.ingestion_jobs.createIndex({ customerId: 1 })
db.ingestion_jobs.createIndex({ status: 1 })
```

---

## Deployment Guide

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- MongoDB 6.0+

### Database Setup

#### PostgreSQL
```bash
# Create databases
createdb mypolicy_customer_db
createdb mypolicy_metadata_db
createdb mypolicy_policy_db

# Create user (optional)
psql -c "CREATE USER mypolicy WITH PASSWORD 'password';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE mypolicy_customer_db TO mypolicy;"
psql -c "GRANT ALL PRIVILEGES ON DATABASE mypolicy_metadata_db TO mypolicy;"
psql -c "GRANT ALL PRIVILEGES ON DATABASE mypolicy_policy_db TO mypolicy;"
```

#### MongoDB
```bash
# Start MongoDB
mongod --dbpath /data/db

# Create database (auto-created on first use)
mongosh
> use mypolicy_ingestion_db
```

### Service Startup Order

```bash
# 1. Start core services first
cd customer-service && mvn spring-boot:run &
cd policy-service && mvn spring-boot:run &
cd ingestion-service && mvn spring-boot:run &
cd metadata-service && mvn spring-boot:run &

# 2. Start processing services
cd processing-service && mvn spring-boot:run &
cd matching-engine && mvn spring-boot:run &

# 3. Start BFF (API Gateway) last
cd bff-service && mvn spring-boot:run &
```

### Verify Services
```bash
# Check all services are running
curl http://localhost:8080/actuator/health  # BFF
curl http://localhost:8081/actuator/health  # Customer
curl http://localhost:8082/actuator/health  # Ingestion
curl http://localhost:8083/actuator/health  # Metadata
curl http://localhost:8084/actuator/health  # Processing
curl http://localhost:8085/actuator/health  # Policy
curl http://localhost:8086/actuator/health  # Matching
```

---

## Testing Guide

### 1. Register a User
```bash
curl -X POST http://localhost:8080/api/bff/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "panNumber": "ABCDE1234F",
    "dateOfBirth": "1990-01-01",
    "address": "123 Main St, Mumbai",
    "password": "SecurePass123"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/bff/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePass123"
  }'

# Save the JWT token from response
export JWT_TOKEN="<token_from_response>"
```

### 3. Configure Metadata
```bash
curl -X POST "http://localhost:8083/api/v1/metadata/config?insurerId=HDFC_LIFE&insurerName=HDFC Life" \
  -H "Content-Type: application/json" \
  -d '{
    "TERM_LIFE": [
      {
        "sourceField": "Mob_No",
        "targetField": "mobileNumber",
        "dataType": "STRING",
        "required": true
      },
      {
        "sourceField": "Policy_Num",
        "targetField": "policyNumber",
        "dataType": "STRING",
        "required": true
      },
      {
        "sourceField": "Premium",
        "targetField": "premiumAmount",
        "dataType": "DECIMAL",
        "required": true
      },
      {
        "sourceField": "Sum_Assured",
        "targetField": "sumAssured",
        "dataType": "DECIMAL",
        "required": true
      }
    ]
  }'
```

### 4. Upload File
```bash
curl -X POST http://localhost:8080/api/bff/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@hdfc_policies.xlsx" \
  -F "customerId=<customer_id>" \
  -F "insurerId=HDFC_LIFE"
```

### 5. Get Portfolio
```bash
curl -X GET "http://localhost:8080/api/bff/portfolio/<customer_id>" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### 6. Get Coverage Insights
```bash
curl -X GET "http://localhost:8080/api/bff/insights/<customer_id>" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

---

## Security Considerations

### 1. Authentication
- JWT tokens with 24-hour expiration
- BCrypt password hashing (strength: 10)
- Secure token storage on client side

### 2. Data Encryption
- PII fields encrypted at rest (AES-256)
- HTTPS for all communications
- Database connection encryption

### 3. Authorization
- Role-based access control (future)
- Customer can only access own data
- Admin endpoints protected

### 4. Input Validation
- Request validation at BFF layer
- SQL injection prevention (JPA)
- File upload size limits
- Content type validation

---

## Monitoring & Logging

### Application Logs
```
logging.level.com.mypolicy=DEBUG
```

### Health Checks
```
GET /actuator/health
GET /actuator/info
```

### Metrics (Future)
- Prometheus integration
- Grafana dashboards
- Request rate monitoring
- Error rate tracking

---

## Future Enhancements

1. **Message Queue**: Kafka/RabbitMQ for async processing
2. **API Gateway**: Spring Cloud Gateway
3. **Service Discovery**: Eureka Server
4. **Circuit Breaker**: Resilience4j
5. **Distributed Tracing**: Zipkin/Jaeger
6. **Caching**: Redis for frequently accessed data
7. **Rate Limiting**: API throttling
8. **Notification Service**: Email/SMS alerts
9. **Audit Logging**: Track all data changes
10. **Advanced Analytics**: ML-based recommendations

---

## Support

For issues or questions:
- Email: support@mypolicy.com
- Documentation: /docs
- API Reference: /api-docs
