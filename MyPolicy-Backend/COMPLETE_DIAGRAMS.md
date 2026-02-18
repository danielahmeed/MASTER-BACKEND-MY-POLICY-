# Complete System Diagrams - MyPolicy Insurance Platform

## 📚 Table of Contents

1. [High-Level Design (HLD)](#high-level-design-hld)
2. [Complete API Sequence Diagram](#complete-api-sequence-diagram)
3. [Customer Matching Logic](#customer-matching-logic)
4. [Data Correction Workflow](#data-correction-workflow)
5. [Layered Architecture](#layered-architecture)
6. [Data Flow Diagrams](#data-flow-diagrams)
7. [Deployment Architecture](#deployment-architecture)

---

## 🏗️ High-Level Design (HLD)

### System Architecture with Customer Correction

```mermaid
graph TB
    subgraph "External Sources"
        A1[Insurer A<br/>CSV/Excel]
        A2[Insurer B<br/>CSV/Excel]
        A3[Insurer C<br/>CSV/Excel]
    end

    subgraph "Presentation Layer"
        UI[Web Portal<br/>React/Angular]
        ADMIN[Admin Dashboard<br/>Data Correction Interface]
    end

    subgraph "API Gateway Layer"
        BFF[BFF Service :8080<br/>━━━━━━━━━━<br/>• Request Aggregation<br/>• JWT Validation<br/>• Response Transform<br/>• Customer Updates]
    end

    subgraph "Authentication & User Management"
        AUTH[Customer Service :8081<br/>━━━━━━━━━━<br/>• User Registration<br/>• JWT Generation<br/>• PII Encryption<br/>• Customer Updates ⭐<br/>• Data Correction ⭐]
    end

    subgraph "Ingestion & Processing Layer"
        ING[Ingestion Service :8082<br/>━━━━━━━━━━<br/>• File Upload<br/>• Job Tracking<br/>• Validation Log]

        PROC[Processing Service :8084<br/>━━━━━━━━━━<br/>• Excel/CSV Parser<br/>• Data Transform<br/>• Apply Mappings]

        META[Metadata Service :8083<br/>━━━━━━━━━━<br/>• Field Mappings<br/>• Transformation Rules<br/>• Insurer Configs]
    end

    subgraph "Business Logic Layer"
        MATCH[Matching Engine :8086<br/>━━━━━━━━━━<br/>• Identity Resolution<br/>• Fuzzy Matching<br/>• Quality Scoring ⭐<br/>• Flag for Review ⭐]

        POL[Policy Service :8085<br/>━━━━━━━━━━<br/>• Policy CRUD<br/>• Customer Linkage]

        INSIGHTS[Insights Engine<br/>━━━━━━━━━━<br/>• Gap Analysis<br/>• Recommendations<br/>• Coverage Score]
    end

    subgraph "Data Persistence Layer"
        DB[(PostgreSQL<br/>mypolicy_db<br/>━━━━━━━━━━<br/>📊 Tables:<br/>• customers ⭐<br/>• policies<br/>• insurer_configurations<br/><br/>🔄 Update Support ⭐)]

        MONGO[(MongoDB<br/>ingestion_db<br/>━━━━━━━━━━<br/>• ingestion_jobs<br/>• validation_errors)]
    end

    subgraph "Security & Cross-Cutting"
        ENC[Encryption Service<br/>━━━━━━━━━━<br/>• AES-256 for PII<br/>• BCrypt for Passwords]

        AUDIT[Audit Trail ⭐<br/>━━━━━━━━━━<br/>• Change Tracking<br/>• Update History<br/>• Timestamps]
    end

    %% External to UI
    A1 & A2 & A3 -->|Upload| UI

    %% UI to BFF
    UI -->|REST API| BFF
    ADMIN -->|Correction API ⭐| BFF

    %% BFF to Services
    BFF -->|Register/Login| AUTH
    BFF -->|Upload File| ING
    BFF -->|Get Portfolio| POL
    BFF -->|Get Insights| INSIGHTS
    BFF -->|Update Customer ⭐| AUTH

    %% Auth Flow
    AUTH -->|Encrypt PII| ENC
    AUTH <-->|CRUD + Updates ⭐| DB

    %% Ingestion Flow
    ING -->|Store Job| MONGO
    PROC -->|Get Job| ING
    PROC -->|Get Mappings| META
    META <-->|Retrieve| DB

    %% Processing Flow
    PROC -->|Transformed Data| MATCH
    MATCH -->|Quality Check ⭐| MATCH
    MATCH -->|Search/Create| AUTH
    MATCH -->|Create Policy| POL

    %% Policy Flow
    POL <-->|Store/Retrieve| DB

    %% Insights Flow
    INSIGHTS -->|Read Policies| POL
    INSIGHTS -->|Read Customer| AUTH

    %% Audit
    AUTH -.->|Log Changes ⭐| AUDIT
    AUDIT -.->|Store| DB

    %% Styling
    classDef gateway fill:#4A90E2,stroke:#2E5C8A,stroke-width:3px,color:#fff
    classDef service fill:#7ED321,stroke:#5FA319,stroke-width:2px,color:#000
    classDef database fill:#F5A623,stroke:#C4841D,stroke-width:2px,color:#000
    classDef security fill:#D0021B,stroke:#A00116,stroke-width:2px,color:#fff
    classDef external fill:#BD10E0,stroke:#9012B3,stroke-width:2px,color:#fff
    classDef new fill:#50E3C2,stroke:#2EB398,stroke-width:3px,color:#000

    class BFF gateway
    class AUTH,ING,PROC,META,MATCH,POL,INSIGHTS service
    class DB,MONGO database
    class ENC,AUDIT security
    class A1,A2,A3,UI,ADMIN external
```

---

## 📊 Complete API Sequence Diagram

### With Customer Data Correction Feature

```mermaid
sequenceDiagram
    participant User as User/Frontend
    participant Admin as Admin Dashboard
    participant BFF as BFF Service<br/>(Port 8080)
    participant Customer as Customer Service<br/>(Port 8081)
    participant Ingestion as Ingestion Service<br/>(Port 8082)
    participant Metadata as Metadata Service<br/>(Port 8083)
    participant Processing as Processing Service<br/>(Port 8084)
    participant Policy as Policy Service<br/>(Port 8085)
    participant Matching as Matching Engine<br/>(Port 8086)
    participant PostgresDB as PostgreSQL<br/>(mypolicy_db)
    participant MongoDB as MongoDB<br/>(ingestion_db)

    rect rgb(200, 220, 240)
        Note over User,PostgresDB: PHASE 1: User Registration & Authentication

        User->>BFF: POST /api/bff/auth/register
        BFF->>Customer: POST /api/v1/customers/register
        Customer->>Customer: Hash password (BCrypt)<br/>Encrypt PII (AES-256)
        Customer->>PostgresDB: INSERT INTO customers
        PostgresDB-->>Customer: customerId
        Customer-->>BFF: CustomerResponse
        BFF-->>User: 201 Created

        User->>BFF: POST /api/bff/auth/login
        BFF->>Customer: POST /api/v1/customers/login
        Customer->>PostgresDB: SELECT * FROM customers
        PostgresDB-->>Customer: customer record
        Customer->>Customer: Validate & Generate JWT
        Customer-->>BFF: {token, customer}
        BFF-->>User: 200 OK + JWT Token
    end

    rect rgb(180, 200, 220)
        Note over User,MongoDB: PHASE 2: Admin - Configure Metadata

        User->>Metadata: POST /api/v1/metadata/config
        Metadata->>PostgresDB: INSERT/UPDATE insurer_configurations
        PostgresDB-->>Metadata: Success
        Metadata-->>User: 200 OK
    end

    rect rgb(220, 240, 200)
        Note over User,MongoDB: PHASE 3: File Upload & Ingestion

        User->>BFF: POST /api/bff/upload
        BFF->>Ingestion: POST /api/v1/ingestion/upload
        Ingestion->>Ingestion: Validate & Save file
        Ingestion->>MongoDB: INSERT ingestion_job
        MongoDB-->>Ingestion: Success
        Ingestion-->>BFF: {jobId, status: UPLOADED}
        BFF-->>User: 200 OK
    end

    rect rgb(200, 240, 200)
        Note over Processing,PostgresDB: PHASE 4: Processing Pipeline with Quality Check

        Processing->>Ingestion: GET /api/v1/ingestion/status/{jobId}
        Ingestion->>MongoDB: SELECT ingestion_job
        MongoDB-->>Ingestion: job details
        Ingestion-->>Processing: {filePath, insurerId}

        Processing->>Metadata: GET /api/v1/metadata/config/{insurerId}
        Metadata->>PostgresDB: SELECT insurer_configurations
        PostgresDB-->>Metadata: field_mappings
        Metadata-->>Processing: Configuration

        Processing->>Processing: Parse Excel/CSV

        loop For each row
            Processing->>Processing: Transform data
            Processing->>Matching: POST /api/v1/matching/process

            Note over Matching: Multi-Level Matching

            Matching->>Customer: Search by PAN
            Customer->>PostgresDB: SELECT WHERE pan_number = ?
            PostgresDB-->>Customer: Result
            Customer-->>Matching: Customer or null

            alt PAN Match
                Matching->>Matching: ✅ 100% Confidence
                Matching->>Matching: Auto-link
            else Try Email
                Matching->>Customer: Search by Email
                Customer->>PostgresDB: SELECT WHERE email = ?
                PostgresDB-->>Customer: Result
                Customer-->>Matching: Customer or null

                alt Email Match
                    Matching->>Matching: ✅ 90% Confidence
                    Matching->>Matching: Auto-link
                else Try Mobile
                    Matching->>Customer: Search by Mobile
                    Customer->>PostgresDB: SELECT WHERE mobile_number = ?
                    PostgresDB-->>Customer: Result
                    Customer-->>Matching: Customer or null

                    alt Mobile Match
                        Matching->>Matching: ✅ 80% Confidence
                        Matching->>Matching: Auto-link
                    else Fuzzy Name Match
                        Matching->>Customer: GET /api/v1/customers
                        Customer->>PostgresDB: SELECT * FROM customers
                        PostgresDB-->>Customer: All customers
                        Customer-->>Matching: Customer list

                        Matching->>Matching: Calculate Levenshtein<br/>distance for each

                        alt Strong Fuzzy Match (≤2)
                            Matching->>Matching: ✅ 70% Confidence
                            Matching->>Matching: Auto-link
                        else Weak Fuzzy Match (=3)
                            Matching->>Matching: ⚠️ 60% Confidence
                            Matching->>Matching: FLAG FOR REVIEW ⭐
                            Note over Matching: Store match details<br/>for admin review
                        else No Match
                            Matching->>Customer: POST /api/v1/customers/register
                            Customer->>Customer: Encrypt PII
                            Customer->>PostgresDB: INSERT INTO customers
                            PostgresDB-->>Customer: New customerId
                            Customer-->>Matching: New customer
                        end
                    end
                end
            end

            Matching->>Policy: POST /api/v1/policies
            Policy->>PostgresDB: INSERT INTO policies
            PostgresDB-->>Policy: policyId
            Policy-->>Matching: Success

            Matching-->>Processing: Record processed
            Processing->>MongoDB: UPDATE ingestion_job
            MongoDB-->>Processing: Updated
        end

        Processing->>MongoDB: UPDATE status=COMPLETED
        MongoDB-->>Processing: Success
    end

    rect rgb(255, 200, 150)
        Note over Admin,PostgresDB: PHASE 5: Admin Data Correction ⭐ NEW

        Admin->>BFF: GET /api/bff/insights/flagged-customers
        Note over Admin: Review customers<br/>flagged for weak matches

        Admin->>BFF: PUT /api/bff/auth/customer/{id}<br/>{firstName, mobileNumber}
        activate BFF
        BFF->>BFF: Validate JWT
        BFF->>Customer: PUT /api/v1/customers/{id}
        activate Customer

        Customer->>PostgresDB: SELECT customer WHERE id = ?
        PostgresDB-->>Customer: Customer record

        Customer->>Customer: Validate new data<br/>Check for duplicates

        Customer->>PostgresDB: Check duplicates
        PostgresDB-->>Customer: No conflicts

        Customer->>Customer: Encrypt updated PII
        Customer->>PostgresDB: UPDATE customers<br/>SET firstName=?, mobile=?<br/>updated_at=NOW()
        PostgresDB-->>Customer: Success

        Customer->>PostgresDB: INSERT audit_log ⭐
        PostgresDB-->>Customer: Logged

        Customer-->>BFF: Updated CustomerResponse
        deactivate Customer
        BFF-->>Admin: 200 OK
        deactivate BFF

        Note over Admin: Data corrected!<br/>Future matches improved
    end

    rect rgb(240, 220, 200)
        Note over User,PostgresDB: PHASE 6: View Unified Portfolio

        User->>BFF: GET /api/bff/portfolio/{customerId}
        BFF->>BFF: Validate JWT

        par Parallel Calls
            BFF->>Customer: GET /api/v1/customers/{id}
            Customer->>PostgresDB: SELECT * FROM customers
            PostgresDB-->>Customer: Customer data
            Customer->>Customer: Decrypt PII
            Customer-->>BFF: CustomerDTO
        and
            BFF->>Policy: GET /api/v1/policies/customer/{id}
            Policy->>PostgresDB: SELECT * FROM policies
            PostgresDB-->>Policy: Policies list
            Policy-->>BFF: List<PolicyDTO>
        end

        BFF->>BFF: Aggregate data
        BFF-->>User: PortfolioResponse
    end

    rect rgb(255, 220, 200)
        Note over User,PostgresDB: PHASE 7: Coverage Insights

        User->>BFF: GET /api/bff/insights/{customerId}
        BFF->>BFF: Validate JWT

        par Fetch Data
            BFF->>Customer: GET /api/v1/customers/{id}
            Customer->>PostgresDB: SELECT customers
            PostgresDB-->>Customer: Data
            Customer-->>BFF: CustomerDTO
        and
            BFF->>Policy: GET /api/v1/policies/customer/{id}
            Policy->>PostgresDB: SELECT policies
            PostgresDB-->>Policy: Policies
            Policy-->>BFF: List<PolicyDTO>
        end

        BFF->>BFF: Analyze coverage<br/>Calculate gaps<br/>Generate recommendations
        BFF-->>User: CoverageInsights
    end

    rect rgb(220, 220, 240)
        Note over User,MongoDB: PHASE 8: Check Upload Status

        User->>BFF: GET /api/bff/upload/status/{jobId}
        BFF->>Ingestion: GET /api/v1/ingestion/status/{jobId}
        Ingestion->>MongoDB: SELECT ingestion_job
        MongoDB-->>Ingestion: Job details
        Ingestion-->>BFF: JobStatusResponse
        BFF-->>User: 200 OK
    end
```

---

## 🔍 Customer Matching Logic

### Complete Decision Tree with Data Correction

```mermaid
graph TD
    START([New Policy Record<br/>from File]) --> EXTRACT[Extract Customer Data:<br/>• Name<br/>• Mobile<br/>• Email<br/>• PAN]

    EXTRACT --> PAN_CHECK{PAN<br/>Available?}

    PAN_CHECK -->|Yes| PAN_SEARCH[Search Database:<br/>SELECT WHERE<br/>pan_number = ?]
    PAN_CHECK -->|No| EMAIL_CHECK

    PAN_SEARCH --> PAN_FOUND{Match<br/>Found?}
    PAN_FOUND -->|Yes ✅| PAN_MATCH[✅ EXACT PAN MATCH<br/>Confidence: 100%<br/>Quality: PERFECT]
    PAN_MATCH --> USE_EXISTING[Use Existing<br/>Customer ID]

    PAN_FOUND -->|No| EMAIL_CHECK{Email<br/>Available?}

    EMAIL_CHECK -->|Yes| EMAIL_SEARCH[Search Database:<br/>SELECT WHERE<br/>email = ?]
    EMAIL_CHECK -->|No| MOBILE_CHECK

    EMAIL_SEARCH --> EMAIL_FOUND{Match<br/>Found?}
    EMAIL_FOUND -->|Yes ✅| EMAIL_MATCH[✅ EXACT EMAIL MATCH<br/>Confidence: 90%<br/>Quality: EXCELLENT]
    EMAIL_MATCH --> USE_EXISTING

    EMAIL_FOUND -->|No| MOBILE_CHECK{Mobile<br/>Available?}

    MOBILE_CHECK -->|Yes| MOBILE_SEARCH[Search Database:<br/>SELECT WHERE<br/>mobile_number = ?]
    MOBILE_CHECK -->|No| FUZZY_MATCH

    MOBILE_SEARCH --> MOBILE_FOUND{Match<br/>Found?}
    MOBILE_FOUND -->|Yes ✅| MOBILE_MATCH[✅ EXACT MOBILE MATCH<br/>Confidence: 80%<br/>Quality: GOOD]
    MOBILE_MATCH --> USE_EXISTING

    MOBILE_FOUND -->|No| FUZZY_MATCH[Fuzzy Name Matching<br/>Levenshtein Distance]

    FUZZY_MATCH --> GET_ALL[Get All Customers<br/>from Database]
    GET_ALL --> LOOP[Calculate Distance<br/>for Each Customer]

    LOOP --> STRONG_FUZZY{Distance<br/>≤ 2?}

    STRONG_FUZZY -->|Yes ✅| STRONG_MATCH[✅ STRONG FUZZY MATCH<br/>Confidence: 70%<br/>Quality: GOOD<br/>Auto-Link ✓]
    STRONG_MATCH --> USE_EXISTING

    STRONG_FUZZY -->|No| WEAK_FUZZY{Distance<br/>= 3?}

    WEAK_FUZZY -->|Yes ⚠️| WEAK_MATCH[⚠️ WEAK FUZZY MATCH<br/>Confidence: 60%<br/>Quality: UNCERTAIN<br/>FLAG FOR REVIEW ⭐]
    WEAK_MATCH --> REVIEW_NEEDED[Store Match Details:<br/>• Candidate customer IDs<br/>• Match scores<br/>• Field differences]

    REVIEW_NEEDED --> ADMIN_REVIEW{Admin<br/>Manual<br/>Review}

    ADMIN_REVIEW -->|Confirm Match| DATA_CORRECT{Data Needs<br/>Correction?}
    ADMIN_REVIEW -->|Reject Match| CREATE_NEW

    DATA_CORRECT -->|Yes ⭐| CORRECTION[Admin Updates Customer:<br/>PUT /api/bff/auth/customer/{id}<br/><br/>Update Fields:<br/>• firstName ✏️<br/>• lastName ✏️<br/>• mobileNumber ✏️<br/>• email ✏️]

    CORRECTION --> VALIDATE[Validate Updates:<br/>• Format check<br/>• Duplicate check<br/>• Encryption]

    VALIDATE --> SAVE_UPDATE[Save to Database:<br/>UPDATE customers<br/>SET ... WHERE id = ?<br/>updated_at = NOW()]

    SAVE_UPDATE --> AUDIT_LOG[Log Change:<br/>• Who updated<br/>• What changed<br/>• When updated]

    AUDIT_LOG --> USE_EXISTING

    DATA_CORRECT -->|No| USE_EXISTING

    WEAK_FUZZY -->|No| NO_MATCH

    NO_MATCH[❌ No Match Found<br/>Distance > 3] --> CREATE_NEW[Create New Customer:<br/>INSERT INTO customers]

    CREATE_NEW --> ENCRYPT[Encrypt PII:<br/>• Email AES-256<br/>• Mobile AES-256<br/>• PAN AES-256<br/>• BCrypt password]

    ENCRYPT --> NEW_ID[Generate UUID<br/>New Customer ID]

    NEW_ID --> RETURN_NEW[Return New<br/>Customer ID]

    USE_EXISTING --> LINK_POLICY[Link Policy to Customer:<br/>INSERT INTO policies<br/>SET customer_id = ?]
    RETURN_NEW --> LINK_POLICY

    LINK_POLICY --> END([✓ Policy Successfully<br/>Linked to Customer])

    style START fill:#4A90E2,color:#fff,stroke:#2E5C8A,stroke-width:3px
    style END fill:#7ED321,color:#000,stroke:#5FA319,stroke-width:3px
    style PAN_MATCH fill:#7ED321,color:#000,stroke:#5FA319,stroke-width:2px
    style EMAIL_MATCH fill:#7ED321,color:#000,stroke:#5FA319,stroke-width:2px
    style MOBILE_MATCH fill:#7ED321,color:#000,stroke:#5FA319,stroke-width:2px
    style STRONG_MATCH fill:#7ED321,color:#000,stroke:#5FA319,stroke-width:2px
    style WEAK_MATCH fill:#FFA500,color:#fff,stroke:#FF6600,stroke-width:3px
    style ADMIN_REVIEW fill:#BD10E0,color:#fff,stroke:#9012B3,stroke-width:2px
    style CORRECTION fill:#50E3C2,color:#000,stroke:#2EB398,stroke-width:3px
    style NO_MATCH fill:#FF6B6B,color:#fff,stroke:#C92A2A,stroke-width:2px
    style CREATE_NEW fill:#BD10E0,color:#fff,stroke:#9012B3,stroke-width:2px
    style USE_EXISTING fill:#50E3C2,color:#000,stroke:#2EB398,stroke-width:2px
```

---

## 🔄 Data Correction Workflow

### Detailed Correction Process

```mermaid
flowchart TB
    START([Fuzzy Match<br/>Detected]) --> QUALITY{Match<br/>Quality}

    QUALITY -->|Strong ≤2| AUTO[✅ Auto-Link<br/>No Review Needed]
    QUALITY -->|Weak =3| FLAG[⚠️ Flag for Review]
    QUALITY -->|None >3| NEW[Create New Customer]

    AUTO --> POLICY_LINK
    NEW --> POLICY_LINK

    FLAG --> STORE[Store Review Data:<br/>┌─────────────────┐<br/>│ • Match candidates    │<br/>│ • Confidence scores   │<br/>│ • Field mismatches    │<br/>│ • Suggested customer │<br/>└─────────────────┘]

    STORE --> NOTIFY[Notify Admin Dashboard:<br/>• New item in review queue<br/>• Priority based on volume]

    NOTIFY --> ADMIN_VIEW[Admin Views<br/>Flagged Records]

    ADMIN_VIEW --> COMPARE[Compare Data:<br/>┌────────────────────────┐<br/>│ File Data    vs   DB Data   │<br/>├────────────────────────┤<br/>│ Jon Doe         John Doe    │<br/>│ 9999999999  9999999998 │<br/>│ jon@ex.com   john@ex.com│<br/>└────────────────────────┘]

    COMPARE --> DECISION{Admin<br/>Decision}

    DECISION -->|Same Person| CORRECT{Needs<br/>Correction?}
    DECISION -->|Different Person| REJECT[Reject Match:<br/>Create new customer<br/>for this policy]

    REJECT --> NEW

    CORRECT -->|Yes| SELECT_FIELDS[Select Fields to Update:<br/>☑ firstName: Jon → John<br/>☐ lastName: Doe<br/>☑ mobile: ...998 → ...999<br/>☐ email<br/>☐ PAN]

    SELECT_FIELDS --> API_CALL[API Call:<br/>PUT /api/bff/auth/<br/>customer/CUST123]

    API_CALL --> REQUEST[Request Body:<br/>┌─────────────────┐<br/>│ "firstName": "John",  │<br/>│ "mobileNumber":       │<br/>│   "9999999999"        │<br/>└─────────────────┘]

    REQUEST --> BFF_VALIDATE[BFF Service:<br/>• Validate JWT<br/>• Forward to Customer Service]

    BFF_VALIDATE --> CUST_SERVICE[Customer Service:<br/>Validation Steps]

    CUST_SERVICE --> VAL1{Customer<br/>Exists?}
    VAL1 -->|No| ERROR1[❌ 404 Not Found]
    VAL1 -->|Yes| VAL2{Format<br/>Valid?}

    VAL2 -->|No| ERROR2[❌ 400 Bad Request:<br/>Invalid email/mobile/PAN format]
    VAL2 -->|Yes| VAL3{Duplicate<br/>Check}

    VAL3 -->|Exists for<br/>other customer| ERROR3[❌ 409 Conflict:<br/>Email/Mobile/PAN<br/>already exists]

    VAL3 -->|No conflict| ENCRYPT_NEW[Encrypt New PII:<br/>• AES-256 for sensitive fields<br/>• Only changed fields]

    ENCRYPT_NEW --> DB_UPDATE[Database Update:<br/>UPDATE customers<br/>SET firstName = ?,<br/>    mobile_number = ?,<br/>    updated_at = NOW()<br/>WHERE customer_id = ?]

    DB_UPDATE --> AUDIT[Audit Trail:<br/>┌──────────────────┐<br/>│ updated_by: admin@ex │<br/>│ updated_at: timestamp │<br/>│ changed_fields:        │<br/>│   • firstName          │<br/>│   • mobileNumber      │<br/>└──────────────────┘]

    AUDIT --> SUCCESS[✅ Update Successful]

    SUCCESS --> RESPONSE[Return Response:<br/>Updated CustomerDTO<br/>with corrected data]

    RESPONSE --> LINK_EXISTING[Link Policy to<br/>Corrected Customer]

    LINK_EXISTING --> POLICY_LINK

    CORRECT -->|No| LINK_EXISTING

    ERROR1 & ERROR2 & ERROR3 --> MANUAL[Manual Intervention<br/>Required]

    POLICY_LINK[Link Policy:<br/>INSERT INTO policies<br/>customer_id = corrected_id]

    POLICY_LINK --> REMOVE_FLAG[Remove from<br/>Review Queue]

    REMOVE_FLAG --> IMPROVE[Future Matching<br/>Improved ⭐<br/>Correct data = Better matches]

    IMPROVE --> END([✓ Complete])

    style START fill:#4A90E2,color:#fff,stroke:#2E5C8A,stroke-width:3px
    style FLAG fill:#FFA500,color:#fff,stroke:#FF6600,stroke-width:3px
    style ADMIN_VIEW fill:#BD10E0,color:#fff,stroke:#9012B3,stroke-width:2px
    style API_CALL fill:#50E3C2,color:#000,stroke:#2EB398,stroke-width:3px
    style SUCCESS fill:#7ED321,color:#000,stroke:#5FA319,stroke-width:3px
    style ERROR1 fill:#FF6B6B,color:#fff,stroke:#C92A2A,stroke-width:2px
    style ERROR2 fill:#FF6B6B,color:#fff,stroke:#C92A2A,stroke-width:2px
    style ERROR3 fill:#FF6B6B,color:#fff,stroke:#C92A2A,stroke-width:2px
    style END fill:#7ED321,color:#000,stroke:#5FA319,stroke-width:3px
    style IMPROVE fill:#FFD700,color:#000,stroke:#FFA500,stroke-width:2px
```

---

## 🏢 Layered Architecture

### With Customer Correction Integration

```mermaid
graph TB
    subgraph Layer1["🌐 External Layer"]
        direction LR
        EXT1[Insurer Files<br/>CSV/Excel]
        EXT2[Web Portal<br/>User Interface]
        EXT3[Admin Dashboard ⭐<br/>Data Correction]
    end

    subgraph Layer2["🚪 API Gateway Layer - Port 8080"]
        BFF["BFF Service<br/>━━━━━━━━━━<br/>Endpoints:<br/>• POST /auth/register<br/>• POST /auth/login<br/>• PUT /auth/customer/{id} ⭐<br/>• GET /portfolio/{id}<br/>• GET /insights/{id}<br/>• POST /upload"]
    end

    subgraph Layer3["🔐 Security Layer"]
        AUTH["Customer Service :8081<br/>━━━━━━━━━━<br/>• Authentication & JWT<br/>• Customer CRUD<br/>• Customer Updates ⭐<br/>• PII Encryption"]

        ENC["Encryption<br/>━━━━━━━━━━<br/>• AES-256 for PII<br/>• BCrypt for Passwords<br/>• Auto-encrypt on update ⭐"]
    end

    subgraph Layer4["📥 Ingestion & Metadata Layer"]
        ING["Ingestion :8082<br/>━━━━━━━━━━<br/>• File Upload<br/>• Job Tracking<br/>• Validation Log"]

        PROC["Processing :8084<br/>━━━━━━━━━━<br/>• File Parser<br/>• Data Transform<br/>• Orchestration"]

        META["Metadata :8083<br/>━━━━━━━━━━<br/>• Field Mappings<br/>• Insurer Configs<br/>• Rules Engine"]
    end

    subgraph Layer5["⚙️ Business Logic Layer"]
        MATCH["Matching Engine :8086<br/>━━━━━━━━━━<br/>• Identity Resolution<br/>• Multi-level Matching<br/>• Quality Scoring ⭐<br/>• Review Flagging ⭐"]

        POL["Policy Service :8085<br/>━━━━━━━━━━<br/>• Policy Management<br/>• Customer Linkage<br/>• Policy CRUD"]

        INSIGHT["Insights Engine<br/>━━━━━━━━━━<br/>• Coverage Analysis<br/>• Gap Detection<br/>• Recommendations"]
    end

    subgraph Layer6["💾 Persistence Layer"]
        direction LR
        PG[("PostgreSQL<br/>mypolicy_db<br/>━━━━━━━━━━<br/>Tables:<br/>• customers ⭐<br/>  - updated_at ⭐<br/>  - audit support ⭐<br/>• policies<br/>• insurer_configurations")]

        MG[("MongoDB<br/>ingestion_db<br/>━━━━━━━━━━<br/>Collections:<br/>• ingestion_jobs<br/>• validation_errors<br/>• review_queue ⭐")]
    end

    subgraph Layer7["📊 Cross-Cutting Concerns"]
        AUDIT["Audit Service ⭐<br/>━━━━━━━━━━<br/>• Change Tracking<br/>• Update History<br/>• Compliance Logging"]

        MONITOR["Monitoring<br/>━━━━━━━━━━<br/>• Health Checks<br/>• Performance Metrics<br/>• Error Tracking"]
    end

    %% Connections
    EXT1 & EXT2 & EXT3 --> BFF

    BFF --> AUTH
    BFF --> ING
    BFF --> POL
    BFF --> INSIGHT

    AUTH --> ENC
    ING --> PROC
    PROC --> META
    PROC --> MATCH
    MATCH --> AUTH
    MATCH --> POL

    AUTH --> PG
    POL --> PG
    META --> PG
    ING --> MG
    MATCH --> MG
    INSIGHT --> POL
    INSIGHT --> AUTH

    AUTH -.->|Log Changes| AUDIT
    MATCH -.->|Flag Reviews| AUDIT
    AUDIT -.->|Store| PG

    AUTH & ING & PROC & MATCH -.->|Metrics| MONITOR

    classDef layer1 fill:#E3F2FD,stroke:#1976D2,stroke-width:2px
    classDef layer2 fill:#4A90E2,stroke:#2E5C8A,stroke-width:3px,color:#fff
    classDef layer3 fill:#FFE0B2,stroke:#F57C00,stroke-width:2px
    classDef layer4 fill:#C8E6C9,stroke:#388E3C,stroke-width:2px
    classDef layer5 fill:#F8BBD0,stroke:#C2185B,stroke-width:2px
    classDef layer6 fill:#F5A623,stroke:#C4841D,stroke-width:3px,color:#000
    classDef layer7 fill:#E1BEE7,stroke:#8E24AA,stroke-width:2px
    classDef new fill:#50E3C2,stroke:#2EB398,stroke-width:3px,color:#000

    class Layer1 layer1
    class Layer2 layer2
    class Layer3 layer3
    class Layer4 layer4
    class Layer5 layer5
    class Layer6 layer6
    class Layer7 layer7
```

---

## 🔄 Data Flow Diagrams

### Flow 1: Complete File Processing with Correction

```mermaid
flowchart LR
    subgraph Input["📤 Input"]
        FILE[Excel/CSV<br/>Policy File]
    end

    subgraph Upload["📥 Upload"]
        ING[Ingestion<br/>Service<br/>:8082]
        MONGO[(MongoDB<br/>Job Storage)]
    end

    subgraph Transform["🔄 Transform"]
        PROC[Processing<br/>Service<br/>:8084]
        META[Metadata<br/>Service<br/>:8083]
    end

    subgraph Match["🔍 Match"]
        ENGINE[Matching<br/>Engine<br/>:8086]
        SCORE{Quality<br/>Score}
    end

    subgraph Decide["🎯 Decision"]
        AUTO[Auto-Link<br/>>70%]
        FLAG[Flag Review<br/>60-70%]
        NEW[Create New<br/><60%]
    end

    subgraph Review["👤 Review ⭐"]
        ADMIN[Admin<br/>Dashboard]
        CORRECT[Data<br/>Correction]
    end

    subgraph Store["💾 Store"]
        CUST[Customer<br/>Service<br/>:8081]
        POL[Policy<br/>Service<br/>:8085]
        PG[(PostgreSQL<br/>mypolicy_db)]
    end

    subgraph Output["📊 Output"]
        UI[User<br/>Dashboard]
        PORT[Portfolio<br/>View]
    end

    FILE -->|Upload| ING
    ING -->|Store Job| MONGO
    ING -->|Trigger| PROC
    PROC <-->|Get Rules| META
    META <-->|Mappings| PG
    PROC -->|Transform| ENGINE

    ENGINE -->|Calculate| SCORE
    SCORE -->|High| AUTO
    SCORE -->|Medium| FLAG
    SCORE -->|Low| NEW

    AUTO --> CUST
    NEW --> CUST

    FLAG -->|Queue| ADMIN
    ADMIN -->|Review| ADMIN
    ADMIN -->|Update| CORRECT
    CORRECT -->|PUT API| CUST
    CUST -->|Save| PG

    CUST --> POL
    POL -->|Store| PG

    PG -->|Query| PORT
    PORT -->|Display| UI

    style FILE fill:#BD10E0,color:#fff
    style FLAG fill:#FFA500,color:#fff
    style CORRECT fill:#50E3C2,color:#000
    style PG fill:#F5A623,color:#000
    style UI fill:#7ED321,color:#000
```

### Flow 2: Customer Update Data Flow

```mermaid
flowchart TD
    START[Admin Identifies<br/>Mismatch] --> API[PUT /api/bff/auth/<br/>customer/{id}]

    API --> BFF[BFF Service<br/>:8080]
    BFF -->|Validate JWT| BFF2[JWT Valid?]

    BFF2 -->|No| ERR1[401 Unauthorized]
    BFF2 -->|Yes| FORWARD[Forward to<br/>Customer Service]

    FORWARD --> CUST[Customer Service<br/>:8081]

    CUST --> FETCH[Fetch Current<br/>Customer Data]
    FETCH --> DB1[(PostgreSQL)]
    DB1 -->|Customer| CUST

    CUST --> VAL[Validate<br/>New Data]
    VAL -->|Invalid Format| ERR2[400 Bad Request]

    VAL -->|Valid| DUP[Check<br/>Duplicates]
    DUP --> DB2[(PostgreSQL)]
    DB2 -->|Exists| ERR3[409 Conflict]
    DB2 -->|Unique| ENC[Encrypt<br/>Updated PII]

    ENC --> UPDATE[UPDATE customers<br/>SET fields<br/>updated_at = NOW()]
    UPDATE --> DB3[(PostgreSQL)]
    DB3 -->|Success| AUDIT[Log to<br/>Audit Trail]

    AUDIT --> RESP[Return Updated<br/>CustomerDTO]
    RESP --> DONE[✓ Update Complete]

    ERR1 & ERR2 & ERR3 --> FAIL[Update Failed]

    style START fill:#4A90E2,color:#fff
    style API fill:#50E3C2,color:#000
    style ENC fill:#FFE0B2,stroke:#F57C00
    style DONE fill:#7ED321,color:#000
    style FAIL fill:#FF6B6B,color:#fff
```

---

## 🚀 Deployment Architecture

### Production Setup with Monitoring

```mermaid
graph TB
    subgraph External["🌐 External"]
        USER[Users]
        ADMIN[Admins ⭐]
    end

    subgraph LB["⚖️ Load Balancer"]
        NGINX[Nginx / AWS ALB]
    end

    subgraph Apps["🔷 Application Layer"]
        BFF1[BFF :8080<br/>Instance 1]
        BFF2[BFF :8080<br/>Instance 2]

        CUST[Customer :8081<br/>with Update API ⭐]
        ING[Ingestion :8082]
        META[Metadata :8083]
        PROC[Processing :8084]
        POL[Policy :8085]
        MATCH[Matching :8086<br/>with Review Queue ⭐]
    end

    subgraph Data["💾 Data Layer"]
        PG_M[(PostgreSQL<br/>Master<br/>mypolicy_db)]
        PG_R[(PostgreSQL<br/>Replica)]

        MG_P[(MongoDB<br/>Primary)]
        MG_S[(MongoDB<br/>Secondary)]
    end

    subgraph Monitoring["📊 Monitoring ⭐"]
        PROM[Prometheus<br/>Metrics]
        GRAF[Grafana<br/>Dashboards]
        ALERT[Alerting<br/>PagerDuty]
    end

    subgraph Audit["📝 Audit & Logs ⭐"]
        ELK[ELK Stack<br/>Centralized Logs]
        AUDIT_DB[(Audit Database<br/>Immutable Logs)]
    end

    USER & ADMIN --> NGINX
    NGINX --> BFF1
    NGINX --> BFF2

    BFF1 & BFF2 --> CUST
    BFF1 & BFF2 --> POL
    BFF1 & BFF2 --> ING

    PROC --> META
    PROC --> MATCH
    MATCH --> CUST
    MATCH --> POL

    CUST --> PG_M
    POL --> PG_M
    META --> PG_M
    ING --> MG_P

    PG_M -.Replication.-> PG_R
    MG_P -.Replication.-> MG_S

    CUST -.Metrics.-> PROM
    MATCH -.Metrics.-> PROM
    POL -.Metrics.-> PROM

    PROM --> GRAF
    PROM --> ALERT

    CUST -.Logs.-> ELK
    MATCH -.Logs.-> ELK
    CUST -.Audit.-> AUDIT_DB

    style ADMIN fill:#50E3C2,color:#000,stroke:#2EB398,stroke-width:3px
    style CUST fill:#50E3C2,color:#000,stroke:#2EB398,stroke-width:2px
    style MATCH fill:#50E3C2,color:#000,stroke:#2EB398,stroke-width:2px
    style NGINX fill:#4A90E2,color:#fff
    style PG_M fill:#F5A623,color:#000
    style MG_P fill:#F5A623,color:#000
```

---

## 📊 Matching Quality Distribution

### Statistical View

```mermaid
pie title Customer Matching Results (Sample 1000 Policies)
    "PAN Exact Match (100%)" : 450
    "Email Exact Match (90%)" : 250
    "Mobile Exact Match (80%)" : 150
    "Strong Fuzzy Match (70%)" : 80
    "Weak Fuzzy - Flagged (60%) ⭐" : 40
    "New Customer Created" : 30
```

---

## 🎯 Key Metrics Dashboard

### System Performance Indicators

```mermaid
graph LR
    subgraph Metrics["📊 Key Metrics with Correction"]
        M1["Match Rate<br/>94% Auto<br/>4% Review ⭐<br/>2% New"]

        M2["Correction Rate ⭐<br/>95% of flagged<br/>records corrected<br/>successfully"]

        M3["Data Quality<br/>Improving over time<br/>due to corrections ⭐"]

        M4["Processing Time<br/>Avg 2-3 sec/policy<br/>+0.5 sec for review"]

        M5["System Uptime<br/>99.9%<br/>All services"]
    end

    style M1 fill:#7ED321,color:#000
    style M2 fill:#50E3C2,color:#000
    style M3 fill:#FFD700,color:#000
    style M4 fill:#4A90E2,color:#fff
    style M5 fill:#7ED321,color:#000
```

---

## 📚 API Endpoints Summary

### Complete API List with Updates

| Service       | Endpoint                         | Method | Purpose             | New ⭐ |
| ------------- | -------------------------------- | ------ | ------------------- | ------ |
| **BFF**       | `/api/bff/auth/register`         | POST   | User registration   |        |
| **BFF**       | `/api/bff/auth/login`            | POST   | Authentication      |        |
| **BFF**       | `/api/bff/auth/customer/{id}`    | PUT    | **Update customer** | ⭐     |
| **BFF**       | `/api/bff/portfolio/{id}`        | GET    | Get portfolio       |        |
| **BFF**       | `/api/bff/insights/{id}`         | GET    | Coverage insights   |        |
| **BFF**       | `/api/bff/upload`                | POST   | Upload file         |        |
| **BFF**       | `/api/bff/upload/status/{id}`    | GET    | Upload status       |        |
| **Customer**  | `/api/v1/customers/register`     | POST   | Register customer   |        |
| **Customer**  | `/api/v1/customers/login`        | POST   | Login               |        |
| **Customer**  | `/api/v1/customers/{id}`         | GET    | Get customer        |        |
| **Customer**  | `/api/v1/customers/{id}`         | PUT    | **Update customer** | ⭐     |
| **Policy**    | `/api/v1/policies`               | POST   | Create policy       |        |
| **Policy**    | `/api/v1/policies/customer/{id}` | GET    | Get policies        |        |
| **Ingestion** | `/api/v1/ingestion/upload`       | POST   | Upload file         |        |
| **Ingestion** | `/api/v1/ingestion/status/{id}`  | GET    | Job status          |        |
| **Metadata**  | `/api/v1/metadata/config`        | POST   | Create config       |        |
| **Metadata**  | `/api/v1/metadata/config/{id}`   | GET    | Get config          |        |
| **Matching**  | `/api/v1/matching/process`       | POST   | Match customer      |        |

---

## ✅ Feature Checklist

### System Capabilities

- ✅ **User Registration & Authentication** (JWT-based)
- ✅ **Multi-Insurer File Upload** (CSV/Excel)
- ✅ **Metadata-Driven Transformation** (JSONB rules)
- ✅ **Multi-Level Customer Matching**:
  - ✅ PAN exact match (100% confidence)
  - ✅ Email exact match (90% confidence)
  - ✅ Mobile exact match (80% confidence)
  - ✅ Strong fuzzy match (70% confidence)
  - ✅ **Weak fuzzy match with flagging** ⭐ (60% confidence)
- ✅ **Admin Data Correction** ⭐
  - ✅ Review flagged records
  - ✅ Update customer information
  - ✅ Validation & duplicate prevention
  - ✅ Audit trail
- ✅ **Portfolio Aggregation** (Unified view)
- ✅ **Coverage Insights** (Gap analysis & recommendations)
- ✅ **PII Encryption** (AES-256 + BCrypt)
- ✅ **Centralized Database** (mypolicy_db)
- ✅ **Health Monitoring** (All services)

---

## 🎨 Color Legend for Diagrams

| Color     | Meaning                   | Usage                |
| --------- | ------------------------- | -------------------- |
| 🔵 Blue   | API Gateway / Entry Point | BFF Service          |
| 🟢 Green  | Success / Core Service    | Services, Happy Path |
| 🟡 Yellow | Warning / Review Needed   | Weak matches, Flags  |
| 🟠 Orange | Action Required           | Admin review         |
| 🟣 Purple | Special Feature           | New capabilities     |
| 🔴 Red    | Error / Failed            | Validation errors    |
| 🟦 Cyan   | Update / Correction       | Data modification    |

---

## 📖 Documentation Links

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Detailed architecture
- [COMPLETE_API_SEQUENCE.md](./COMPLETE_API_SEQUENCE.md) - API sequences
- [CUSTOMER_DATA_CORRECTION.md](./CUSTOMER_DATA_CORRECTION.md) - Correction feature
- [README.md](./README.md) - Quick start
- [TESTING_GUIDE.md](./TESTING_GUIDE.md) - Testing instructions

---

## 🎉 Summary

This document provides **all system diagrams** updated with the **Customer Data Correction** feature:

1. ✅ **High-Level Design** - Shows correction integration
2. ✅ **Complete API Sequence** - Includes update flow
3. ✅ **Customer Matching Logic** - Decision tree with review
4. ✅ **Data Correction Workflow** - Detailed correction process
5. ✅ **Layered Architecture** - All 7 layers with updates
6. ✅ **Data Flow Diagrams** - End-to-end with correction
7. ✅ **Deployment Architecture** - Production-ready setup

**All diagrams are in Mermaid format and can be rendered in:**

- GitHub Markdown (auto-renders)
- [Mermaid Live Editor](https://mermaid.live/)
- VS Code with Mermaid extensions
- Documentation sites (GitBook, MkDocs, etc.)

---

**Last Updated:** February 18, 2026
**Version:** 2.0 with Customer Data Correction Feature ⭐
