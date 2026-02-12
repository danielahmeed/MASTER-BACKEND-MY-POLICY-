# MyPolicy System - Mermaid Sequence Diagrams

## Table of Contents
1. [User Registration Flow](#1-user-registration-flow)
2. [User Login Flow](#2-user-login-flow)
3. [Portfolio View Flow](#3-portfolio-view-flow)
4. [Coverage Insights Flow](#4-coverage-insights-flow)
5. [File Upload & Processing Flow](#5-file-upload--processing-flow)
6. [Complete End-to-End Flow](#6-complete-end-to-end-flow)

---

## 1. User Registration Flow

```mermaid
sequenceDiagram
    participant User
    participant BFF as BFF Service<br/>(Port 8080)
    participant Customer as Customer Service<br/>(Port 8081)
    participant DB as PostgreSQL<br/>(customer_db)

    User->>BFF: POST /api/bff/auth/register<br/>{firstName, lastName, email, password, ...}
    activate BFF
    
    BFF->>Customer: POST /api/v1/customers/register
    activate Customer
    
    Customer->>Customer: Hash password (BCrypt)
    Customer->>Customer: Encrypt PII (AES-256)<br/>(email, mobile, PAN, DOB)
    Customer->>Customer: Generate UUID
    
    Customer->>DB: INSERT INTO customers
    activate DB
    DB-->>Customer: Success
    deactivate DB
    
    Customer-->>BFF: CustomerResponse<br/>{customerId, firstName, lastName, ...}
    deactivate Customer
    
    BFF-->>User: 200 OK<br/>Customer created
    deactivate BFF
```

---

## 2. User Login Flow

```mermaid
sequenceDiagram
    participant User
    participant BFF as BFF Service<br/>(Port 8080)
    participant Customer as Customer Service<br/>(Port 8081)
    participant JWT as JwtService
    participant DB as PostgreSQL<br/>(customer_db)

    User->>BFF: POST /api/bff/auth/login<br/>{email, password}
    activate BFF
    
    BFF->>Customer: POST /api/v1/customers/login
    activate Customer
    
    Customer->>DB: SELECT * FROM customers<br/>WHERE email = ?
    activate DB
    DB-->>Customer: Customer record
    deactivate DB
    
    Customer->>Customer: Validate password<br/>(BCrypt compare)
    
    alt Password Valid
        Customer->>JWT: generateToken(email)
        activate JWT
        JWT-->>Customer: JWT Token (24h expiration)
        deactivate JWT
        
        Customer-->>BFF: AuthResponse<br/>{token, customer}
        BFF-->>User: 200 OK<br/>{token, customer}
    else Password Invalid
        Customer-->>BFF: 401 Unauthorized
        BFF-->>User: 401 Unauthorized
    end
    
    deactivate Customer
    deactivate BFF
```

---

## 3. Portfolio View Flow (Aggregation)

```mermaid
sequenceDiagram
    participant User
    participant BFF as BFF Service<br/>(Port 8080)
    participant Customer as Customer Service<br/>(Port 8081)
    participant Policy as Policy Service<br/>(Port 8085)
    participant CustDB as PostgreSQL<br/>(customer_db)
    participant PolDB as PostgreSQL<br/>(policy_db)

    User->>BFF: GET /api/bff/portfolio/{customerId}<br/>Authorization: Bearer {JWT}
    activate BFF
    
    BFF->>BFF: Validate JWT Token
    
    par Parallel Service Calls
        BFF->>Customer: GET /api/v1/customers/{customerId}
        activate Customer
        Customer->>CustDB: SELECT * FROM customers
        activate CustDB
        CustDB-->>Customer: Customer data
        deactivate CustDB
        Customer-->>BFF: CustomerDTO
        deactivate Customer
    and
        BFF->>Policy: GET /api/v1/policies/customer/{customerId}
        activate Policy
        Policy->>PolDB: SELECT * FROM policies<br/>WHERE customer_id = ?
        activate PolDB
        PolDB-->>Policy: List of policies
        deactivate PolDB
        Policy-->>BFF: List<PolicyDTO>
        deactivate Policy
    end
    
    BFF->>BFF: Aggregate Data<br/>- Calculate total premium<br/>- Calculate total coverage<br/>- Count policies
    
    BFF-->>User: 200 OK<br/>PortfolioResponse<br/>{customer, policies, totals}
    deactivate BFF
```

---

## 4. Coverage Insights Flow

```mermaid
sequenceDiagram
    participant User
    participant BFF as BFF Service<br/>(Port 8080)
    participant Insights as InsightsService
    participant Customer as Customer Service<br/>(Port 8081)
    participant Policy as Policy Service<br/>(Port 8085)

    User->>BFF: GET /api/bff/insights/{customerId}<br/>Authorization: Bearer {JWT}
    activate BFF
    
    BFF->>BFF: Validate JWT Token
    
    BFF->>Insights: analyzeCoverage(customerId)
    activate Insights
    
    par Fetch Data
        Insights->>Customer: GET /api/v1/customers/{customerId}
        activate Customer
        Customer-->>Insights: CustomerDTO
        deactivate Customer
    and
        Insights->>Policy: GET /api/v1/policies/customer/{customerId}
        activate Policy
        Policy-->>Insights: List<PolicyDTO>
        deactivate Policy
    end
    
    Insights->>Insights: Group policies by type
    Insights->>Insights: Calculate coverage by type
    Insights->>Insights: Compare with recommended amounts
    Insights->>Insights: Identify gaps (HIGH/MEDIUM/LOW)
    Insights->>Insights: Generate recommendations
    Insights->>Insights: Calculate coverage score (0-100)
    Insights->>Insights: Generate advisory text
    
    Insights-->>BFF: CoverageInsights<br/>{gaps, recommendations, score}
    deactivate Insights
    
    BFF-->>User: 200 OK<br/>Coverage analysis with recommendations
    deactivate BFF
```

---

## 5. File Upload & Processing Flow

```mermaid
sequenceDiagram
    participant User
    participant BFF as BFF Service<br/>(Port 8080)
    participant Ingestion as Ingestion Service<br/>(Port 8082)
    participant Processing as Processing Service<br/>(Port 8084)
    participant Metadata as Metadata Service<br/>(Port 8083)
    participant Matching as Matching Engine<br/>(Port 8086)
    participant Customer as Customer Service<br/>(Port 8081)
    participant Policy as Policy Service<br/>(Port 8085)
    participant Mongo as MongoDB<br/>(ingestion_db)

    User->>BFF: POST /api/bff/upload<br/>{file, customerId, insurerId}
    activate BFF
    
    BFF->>Ingestion: POST /api/v1/ingestion/upload
    activate Ingestion
    
    Ingestion->>Ingestion: Validate file type<br/>(Excel/CSV)
    Ingestion->>Ingestion: Save file to storage
    Ingestion->>Ingestion: Generate jobId
    
    Ingestion->>Mongo: INSERT ingestion_job<br/>{jobId, status: UPLOADED}
    activate Mongo
    Mongo-->>Ingestion: Success
    deactivate Mongo
    
    Ingestion-->>BFF: JobResponse<br/>{jobId, status: UPLOADED}
    BFF-->>User: 200 OK<br/>{jobId}
    deactivate BFF
    deactivate Ingestion
    
    Note over Processing: Async Processing Triggered
    
    activate Processing
    Processing->>Metadata: GET /api/v1/metadata/config/{insurerId}
    activate Metadata
    Metadata-->>Processing: InsurerConfiguration<br/>{fieldMappings}
    deactivate Metadata
    
    Processing->>Processing: Read Excel file (Apache POI)
    Processing->>Processing: Apply field mappings
    Processing->>Processing: Transform data
    Processing->>Processing: Validate records
    
    loop For each record
        Processing->>Matching: Process record
        activate Matching
        
        Matching->>Customer: Search customer<br/>(PAN/Email/Mobile/Name)
        activate Customer
        Customer-->>Matching: Customer match result
        deactivate Customer
        
        alt Customer Found
            Matching->>Matching: Use existing customerId
        else Customer Not Found
            Matching->>Customer: Create new customer
            activate Customer
            Customer-->>Matching: New customerId
            deactivate Customer
        end
        
        Matching->>Policy: POST /api/v1/policies<br/>{customerId, policyData}
        activate Policy
        Policy-->>Matching: Policy created
        deactivate Policy
        
        Matching-->>Processing: Record processed
        deactivate Matching
    end
    
    Processing->>Mongo: UPDATE ingestion_job<br/>{status: COMPLETED}
    activate Mongo
    Mongo-->>Processing: Success
    deactivate Mongo
    
    deactivate Processing
```

---

## 6. Complete End-to-End Flow

```mermaid
sequenceDiagram
    participant User
    participant BFF as BFF Service
    participant Customer as Customer Service
    participant Ingestion as Ingestion Service
    participant Processing as Processing Service
    participant Metadata as Metadata Service
    participant Matching as Matching Engine
    participant Policy as Policy Service

    rect rgb(200, 220, 240)
        Note over User,Customer: Phase 1: User Registration & Login
        User->>BFF: Register
        BFF->>Customer: Create customer
        Customer-->>BFF: Customer created
        BFF-->>User: Success
        
        User->>BFF: Login
        BFF->>Customer: Validate credentials
        Customer-->>BFF: JWT Token
        BFF-->>User: Token
    end

    rect rgb(220, 240, 200)
        Note over User,Policy: Phase 2: File Upload & Processing
        User->>BFF: Upload Excel file
        BFF->>Ingestion: Store file
        Ingestion-->>BFF: JobId
        BFF-->>User: Upload successful
        
        Processing->>Metadata: Get field mappings
        Metadata-->>Processing: Mapping rules
        Processing->>Processing: Transform data
        Processing->>Matching: Process records
        Matching->>Customer: Find/Create customer
        Matching->>Policy: Create policies
    end

    rect rgb(240, 220, 200)
        Note over User,Policy: Phase 3: View Portfolio & Insights
        User->>BFF: Get portfolio
        BFF->>Customer: Get customer
        BFF->>Policy: Get policies
        BFF->>BFF: Aggregate
        BFF-->>User: Unified portfolio
        
        User->>BFF: Get insights
        BFF->>BFF: Analyze coverage
        BFF->>BFF: Generate recommendations
        BFF-->>User: Coverage insights
    end
```

---

## 7. Metadata Configuration Flow

```mermaid
sequenceDiagram
    participant Admin
    participant Metadata as Metadata Service<br/>(Port 8083)
    participant DB as PostgreSQL<br/>(metadata_db)

    Admin->>Metadata: POST /api/v1/metadata/config<br/>?insurerId=HDFC_LIFE
    activate Metadata
    
    Note over Admin,Metadata: Request Body:<br/>{<br/>  "TERM_LIFE": [<br/>    {sourceField, targetField, dataType}<br/>  ]<br/>}
    
    Metadata->>Metadata: Validate mapping structure
    Metadata->>Metadata: Generate configId (UUID)
    Metadata->>Metadata: Set timestamp
    
    Metadata->>DB: INSERT/UPDATE insurer_configurations<br/>(JSONB field_mappings)
    activate DB
    DB-->>Metadata: Success
    deactivate DB
    
    Metadata-->>Admin: 200 OK<br/>Configuration saved
    deactivate Metadata
```

---

## 8. Customer Matching Flow (Detailed)

```mermaid
sequenceDiagram
    participant Matching as Matching Engine
    participant Customer as Customer Service
    participant DB as PostgreSQL<br/>(customer_db)

    Matching->>Matching: Extract identifiers<br/>(name, mobile, email, PAN)
    
    alt Exact Match on PAN
        Matching->>Customer: Search by PAN
        activate Customer
        Customer->>DB: SELECT WHERE pan_number = ?
        activate DB
        DB-->>Customer: Customer found
        deactivate DB
        Customer-->>Matching: CustomerId
        deactivate Customer
        Note over Matching: Use existing customer
    else Exact Match on Email
        Matching->>Customer: Search by Email
        activate Customer
        Customer->>DB: SELECT WHERE email = ?
        activate DB
        DB-->>Customer: Customer found
        deactivate DB
        Customer-->>Matching: CustomerId
        deactivate Customer
        Note over Matching: Use existing customer
    else Exact Match on Mobile
        Matching->>Customer: Search by Mobile
        activate Customer
        Customer->>DB: SELECT WHERE mobile_number = ?
        activate DB
        DB-->>Customer: Customer found
        deactivate DB
        Customer-->>Matching: CustomerId
        deactivate Customer
        Note over Matching: Use existing customer
    else Fuzzy Match on Name
        Matching->>Customer: Search by Name
        activate Customer
        Customer->>DB: SELECT * FROM customers
        activate DB
        DB-->>Customer: All customers
        deactivate DB
        Customer-->>Matching: Customer list
        deactivate Customer
        
        Matching->>Matching: Calculate Levenshtein distance<br/>(threshold: ≤ 3)
        
        alt Match Found
            Note over Matching: Use matched customer
        else No Match
            Matching->>Customer: Create new customer
            activate Customer
            Customer->>DB: INSERT INTO customers
            activate DB
            DB-->>Customer: New customerId
            deactivate DB
            Customer-->>Matching: CustomerId
            deactivate Customer
        end
    end
```

---

## Usage Instructions

### To render these diagrams:

1. **GitHub/GitLab**: Paste directly in markdown files
2. **Mermaid Live Editor**: https://mermaid.live/
3. **VS Code**: Install "Markdown Preview Mermaid Support" extension
4. **Documentation Tools**: Most support Mermaid natively

### To customize:

- Change colors: `rect rgb(R, G, B)`
- Add notes: `Note over Service1,Service2: Text`
- Add loops: `loop Condition ... end`
- Add alternatives: `alt Condition ... else ... end`

---

## Key Symbols

- `→` : Synchronous request
- `-->>` : Response
- `activate/deactivate` : Service active period
- `par ... and ... end` : Parallel execution
- `alt ... else ... end` : Conditional flow
- `loop ... end` : Iteration

---

## Color Coding

- **Blue** (rgb(200, 220, 240)): Authentication & User Management
- **Green** (rgb(220, 240, 200)): File Processing & Data Transformation
- **Orange** (rgb(240, 220, 200)): Analytics & Insights

---

## Related Documentation

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Complete architecture
- [API_REFERENCE.md](./bff-service/API_REFERENCE.md) - API documentation
- [SEQUENCE_COMPLIANCE.md](./SEQUENCE_COMPLIANCE.md) - Original sequence alignment
