# MyPolicy System - Complete Beginner's Guide to Java Spring Boot

**For developers new to Java Spring Boot**  
**Last Updated**: February 19, 2026

---

## Table of Contents

1. [What is Spring Boot?](#1-what-is-spring-boot)
2. [System Architecture Overview](#2-system-architecture-overview)
3. [Understanding the Layers](#3-understanding-the-layers)
4. [Complete Code Flow Example](#4-complete-code-flow-example)
5. [Key Spring Boot Concepts](#5-key-spring-boot-concepts)
6. [Database Integration](#6-database-integration)
7. [How the Consolidated Service Works](#7-how-the-consolidated-service-works)
8. [Real World Example Walkthrough](#8-real-world-example-walkthrough)

---

## 1. What is Spring Boot?

### Simple Definition

**Spring Boot** is a Java framework that makes it easy to create web applications and microservices. Think of it as a toolkit that handles all the boring setup so you can focus on writing your business logic.

### Key Benefits

- **Auto-configuration**: Spring Boot automatically configures your application based on dependencies
- **Embedded Server**: No need to install Tomcat separately - it's built-in
- **Dependency Management**: Maven/Gradle handles all library versions
- **Production Ready**: Built-in health checks, metrics, and monitoring

### How It's Different from Regular Java

```java
// Regular Java (Lots of setup needed)
public class MyApp {
    public static void main(String[] args) {
        // You need to manually set up:
        // - HTTP server
        // - Database connection
        // - Request routing
        // - JSON parsing
        // ...hundreds of lines of configuration
    }
}

// Spring Boot (Just run it!)
@SpringBootApplication  // This one annotation does ALL the setup!
public class DataPipelineApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataPipelineApplication.class, args);
        // Spring Boot automatically:
        // ✓ Starts web server on port 8082
        // ✓ Connects to PostgreSQL
        // ✓ Connects to MongoDB
        // ✓ Sets up REST endpoints
        // ✓ Handles JSON conversion
        // ✓ Manages database transactions
    }
}
```

---

## 2. System Architecture Overview

### The Big Picture

Your MyPolicy system has **4 microservices** (independent applications):

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React/Angular)                │
│                          (User Interface)                       │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BFF SERVICE (Port 8080)                      │
│                  (Backend for Frontend - API Gateway)           │
│  - Receives all requests from frontend                          │
│  - Routes to appropriate microservice                           │
│  - Aggregates responses                                         │
└─────────────┬───────────────────┬───────────────┬───────────────┘
              │                   │               │
              ▼                   ▼               ▼
┌─────────────────────┐  ┌──────────────────────────────┐  ┌──────────────┐
│  CUSTOMER SERVICE   │  │  DATA-PIPELINE SERVICE       │  │POLICY SERVICE│
│    (Port 8081)      │  │      (Port 8082)             │  │ (Port 8085)  │
│                     │  │  ┌────────────────────────┐  │  │              │
│ - User registration │  │  │  Ingestion Module     │  │  │ - Store      │
│ - Login/Auth        │  │  │  (File uploads)       │  │  │   policies   │
│ - Customer data     │  │  ├────────────────────────┤  │  │ - Query      │
│ - Encrypt PII       │  │  │  Metadata Module      │  │  │   policies   │
│                     │  │  │  (Field mappings)     │  │  │              │
│                     │  │  ├────────────────────────┤  │  │              │
│                     │  │  │  Processing Module    │  │  │              │
│                     │  │  │  (Parse Excel)        │  │  │              │
│                     │  │  ├────────────────────────┤  │  │              │
│                     │  │  │  Matching Module      │  │  │              │
│                     │  │  │  (Identity matching)  │  │  │              │
│                     │  │  └────────────────────────┘  │  │              │
└──────────┬──────────┘  └──────────────┬───────────────┘  └──────┬───────┘
           │                            │                          │
           ▼                            ▼                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      PostgreSQL Database                        │
│     (Port 5432 - Stores: customers, policies, metadata)         │
└─────────────────────────────────────────────────────────────────┘

                            ┌─────────────────────────┐
                            │    MongoDB Database     │
                            │  (Port 27017 - Stores:  │
                            │   ingestion jobs)       │
                            └─────────────────────────┘
```

### What Each Service Does

| Service                   | Port | Purpose                                      | Database             |
| ------------------------- | ---- | -------------------------------------------- | -------------------- |
| **BFF Service**           | 8080 | API Gateway - receives all frontend requests | None                 |
| **Customer Service**      | 8081 | Manages users, authentication, customer data | PostgreSQL           |
| **Data-Pipeline Service** | 8082 | Processes insurance files, matches customers | PostgreSQL + MongoDB |
| **Policy Service**        | 8085 | Manages insurance policies                   | PostgreSQL           |

---

## 3. Understanding the Layers

Spring Boot follows a **layered architecture**. Think of it like a restaurant:

```
┌─────────────────────────────────────────────────────────────┐
│  CONTROLLER LAYER (Waiter)                                  │
│  - Takes orders from customers (HTTP requests)              │
│  - Returns dishes (HTTP responses)                          │
│  - Doesn't cook, just serves                                │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  SERVICE LAYER (Chef)                                       │
│  - Contains business logic                                  │
│  - Processes data                                           │
│  - Makes decisions                                          │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  REPOSITORY LAYER (Warehouse)                               │
│  - Stores and retrieves data                                │
│  - Database operations (CRUD)                               │
│  - No business logic                                        │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  ENTITY/MODEL LAYER (Ingredients)                           │
│  - Defines data structure                                   │
│  - Maps to database tables                                  │
│  - Plain data objects                                       │
└─────────────────────────────────────────────────────────────┘
```

### Let's Look at the InsurerConfiguration You're Currently Viewing

```java
// FILE: InsurerConfiguration.java
// LAYER: Entity/Model
// PURPOSE: Represents a database table row

@Entity  // ← This tells Spring: "This is a database table"
@Table(name = "insurer_configurations")  // ← Table name in PostgreSQL
public class InsurerConfiguration {

    @Id  // ← Primary key
    @GeneratedValue(strategy = GenerationType.UUID)  // ← Auto-generate UUID
    private String configId;  // Maps to "config_id" column

    @Column(nullable = false, unique = true, name = "insurer_id")
    private String insurerId;  // Example: "HDFC_LIFE"

    @Column(nullable = false, name = "insurer_name")
    private String insurerName;  // Example: "HDFC Life Insurance"

    @Type(JsonBinaryType.class)  // ← Store JSON in PostgreSQL
    @Column(columnDefinition = "jsonb", name = "field_mappings")
    private Map<String, List<FieldMapping>> fieldMappings;  // Mapping rules

    @Column(name = "active", nullable = false)
    private boolean active = true;  // Is this config active?

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;  // Last update timestamp

    @PreUpdate  // ← Automatically called before saving
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();  // Update timestamp
    }
}
```

**What this Entity does:**

- Defines the structure of the `insurer_configurations` table
- Each instance = one row in the database
- Spring JPA automatically creates/reads/updates/deletes rows

**Example Data:**

```json
{
  "configId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "insurerId": "HDFC_LIFE",
  "insurerName": "HDFC Life Insurance",
  "fieldMappings": {
    "TERM_LIFE": [
      { "sourceField": "Name", "targetField": "firstName", "required": true },
      {
        "sourceField": "Mobile",
        "targetField": "mobileNumber",
        "required": true
      }
    ]
  },
  "active": true,
  "updatedAt": "2026-02-19T10:30:00"
}
```

---

## 4. Complete Code Flow Example

Let's trace **one complete request** through all layers:

### Scenario: Admin configures field mappings for HDFC Life

```
USER ACTION: POST http://localhost:8082/api/v1/metadata/config?insurerId=HDFC_LIFE
Body: { "TERM_LIFE": [...field mappings...] }
```

#### Step 1: Controller Receives Request (The Waiter)

```java
// FILE: MetadataController.java
// LAYER: Controller
// LOCATION: data-pipeline-service/metadata/controller/

@RestController  // ← Makes this a REST API controller
@RequestMapping("/api/v1/metadata")  // ← Base URL path
public class MetadataController {

  @Autowired  // ← Spring automatically injects MetadataService
  private final MetadataService metadataService;

  @PostMapping("/config")  // ← Handles POST /api/v1/metadata/config
  public ResponseEntity<InsurerConfiguration> createConfiguration(
      @RequestParam String insurerId,  // ← From query param ?insurerId=HDFC_LIFE
      @RequestParam String insurerName,
      @RequestBody Map<String, List<FieldMapping>> mappings) {  // ← JSON body

    // Controller's job: Just pass data to service layer
    InsurerConfiguration config = metadataService.saveConfiguration(
        insurerId, insurerName, mappings);

    return ResponseEntity.ok(config);  // Return 200 OK with config
  }
}
```

**What happens:**

1. Spring Boot receives HTTP POST request
2. Spring automatically converts JSON → Java objects
3. Controller method is called with parameters
4. Controller calls Service layer
5. Spring converts Java object → JSON for response

#### Step 2: Service Processes Business Logic (The Chef)

```java
// FILE: MetadataService.java
// LAYER: Service
// LOCATION: data-pipeline-service/metadata/service/

@Service  // ← Tells Spring this is a service bean
@RequiredArgsConstructor  // ← Lombok creates constructor for dependency injection
public class MetadataService {

  private final MetadataRepository repository;  // ← Injected by Spring

  public InsurerConfiguration saveConfiguration(
      String insurerId, String insurerName,
      Map<String, List<FieldMapping>> mappings) {

    log.info("Saving configuration for insurer: {}", insurerId);

    // BUSINESS LOGIC: Check if config already exists
    Optional<InsurerConfiguration> existing = repository.findByInsurerId(insurerId);

    // Create new or update existing
    InsurerConfiguration config = existing.orElse(new InsurerConfiguration());
    config.setInsurerId(insurerId);
    config.setInsurerName(insurerName);
    config.setFieldMappings(mappings);
    config.setActive(true);
    config.setUpdatedAt(LocalDateTime.now());

    // Save to database
    InsurerConfiguration saved = repository.save(config);

    log.info("Configuration saved with ID: {}", saved.getConfigId());

    return saved;
  }
}
```

**What happens:**

1. Service receives request from Controller
2. Checks if configuration already exists
3. Creates or updates the configuration object
4. Calls Repository to save to database
5. Returns saved configuration

#### Step 3: Repository Saves to Database (The Warehouse)

```java
// FILE: MetadataRepository.java
// LAYER: Repository
// LOCATION: data-pipeline-service/metadata/repository/

@Repository  // ← Tells Spring this is a repository
public interface MetadataRepository extends JpaRepository<InsurerConfiguration, String> {

  // Spring automatically implements this method!
  Optional<InsurerConfiguration> findByInsurerId(String insurerId);

  // Also available (auto-implemented by Spring):
  // - save(config)
  // - findById(id)
  // - findAll()
  // - deleteById(id)
  // ...and 20+ more methods
}
```

**What happens:**

1. Repository is just an interface (no implementation needed!)
2. Spring Data JPA automatically generates the implementation
3. `save()` method generates SQL: `INSERT INTO insurer_configurations ...`
4. PostgreSQL executes the SQL and returns the saved row
5. Spring converts database row → Java object

#### Step 4: Response Flows Back

```
Database → Repository → Service → Controller → HTTP Response
  (SQL)      (Java)      (Java)     (JSON)
```

---

## 5. Key Spring Boot Concepts

### 5.1 Dependency Injection (DI)

**Problem**: How do classes get instances of other classes?

**Old Way (Manual)**:

```java
public class MetadataController {
    private MetadataService service = new MetadataService();  // ❌ Tightly coupled
}
```

**Spring Way (Automatic)**:

```java
@RestController
public class MetadataController {

    @Autowired  // ← Spring finds MetadataService and injects it
    private final MetadataService service;  // ✅ Loosely coupled

    // Alternative using constructor (preferred):
    public MetadataController(MetadataService service) {
        this.service = service;  // Spring injects via constructor
    }
}
```

**Benefits:**

- No `new` keyword needed
- Easy to swap implementations
- Easy to test (can inject mock objects)
- Spring manages object lifecycle

### 5.2 Annotations (The Magic Keywords)

Annotations are **instructions to Spring** about what to do with your code:

| Annotation               | Purpose                            | Example                   |
| ------------------------ | ---------------------------------- | ------------------------- |
| `@SpringBootApplication` | Main entry point                   | Application startup class |
| `@RestController`        | This class handles HTTP requests   | Controllers               |
| `@Service`               | This class contains business logic | Services                  |
| `@Repository`            | This class interacts with database | Repositories              |
| `@Entity`                | This class maps to database table  | Models/Entities           |
| `@Autowired`             | Inject dependency automatically    | Any class                 |
| `@GetMapping`            | Handle HTTP GET requests           | Controller methods        |
| `@PostMapping`           | Handle HTTP POST requests          | Controller methods        |
| `@RequestParam`          | Extract query parameter            | Method parameters         |
| `@RequestBody`           | Extract JSON body                  | Method parameters         |
| `@PathVariable`          | Extract URL path variable          | Method parameters         |

### 5.3 Application Properties

Configuration file that tells Spring how to behave:

```properties
# FILE: application.properties
# LOCATION: src/main/resources/

# What port to run on
server.port=8082

# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/mypolicy_db
spring.datasource.username=postgres
spring.datasource.password=postgres123

# MongoDB Database
spring.data.mongodb.uri=mongodb://localhost:27017/ingestion_db

# File Upload Settings
spring.servlet.multipart.max-file-size=50MB

# External Services
customer.service.url=http://localhost:8081
policy.service.url=http://localhost:8085
```

Spring reads this file at startup and configures everything automatically.

---

## 6. Database Integration

### Two Types of Databases in This Project

#### PostgreSQL (Relational Database)

- **Used for**: Structured data (customers, policies, metadata)
- **Accessed via**: Spring Data JPA
- **Tables**: Well-defined schema with relationships

```java
// Spring JPA automatically converts between Java objects and SQL

// Java Code:
InsurerConfiguration config = new InsurerConfiguration();
config.setInsurerId("HDFC_LIFE");
repository.save(config);

// Generated SQL:
// INSERT INTO insurer_configurations (config_id, insurer_id, ...)
// VALUES ('uuid...', 'HDFC_LIFE', ...);
```

#### MongoDB (Document Database)

- **Used for**: Flexible data (ingestion jobs with varying structure)
- **Accessed via**: Spring Data MongoDB
- **Collections**: JSON-like documents, no fixed schema

```java
// Spring MongoDB automatically converts between Java objects and JSON

// Java Code:
IngestionJob job = new IngestionJob();
job.setJobId("job123");
job.setStatus(IngestionStatus.UPLOADED);
ingestionRepository.save(job);

// Generated MongoDB command:
// db.ingestion_jobs.insertOne({
//   jobId: "job123",
//   status: "UPLOADED",
//   ...
// });
```

### How Spring Makes Database Access Easy

**Without Spring (100+ lines of code)**:

```java
Connection conn = DriverManager.getConnection("jdbc:postgresql://...");
PreparedStatement stmt = conn.prepareStatement("INSERT INTO ...");
stmt.setString(1, insurerId);
stmt.executeUpdate();
// ...handle exceptions, close connections, etc.
```

**With Spring (1 line of code)**:

```java
repository.save(config);  // That's it! Spring handles everything.
```

---

## 7. How the Consolidated Service Works

### Why Consolidation?

**Before**: 4 separate services

```
Ingestion Service (8082) → HTTP → Metadata Service (8083)
                                       ↓ HTTP
                          Processing Service (8084)
                                       ↓ HTTP
                          Matching Engine (8086)
```

- **Problem**: 250+ HTTP calls, slow, complex, 4 deployments

**After**: 1 consolidated service with 4 modules

```
Data-Pipeline Service (8082)
  └─ Ingestion Module   ──┐
  └─ Metadata Module    ──┤ All in same JVM
  └─ Processing Module  ──┤ Direct method calls (<1ms)
  └─ Matching Module    ──┘
```

- **Benefit**: Just method calls, fast, simple, 1 deployment

### How Modules Talk to Each Other

```java
// FILE: ProcessingService.java
// This is in the Processing Module

@Service
public class ProcessingService {

    // Spring injects these - they're in the SAME application!
    private final MetadataService metadataService;     // Metadata Module
    private final IngestionService ingestionService;   // Ingestion Module
    private final MatchingService matchingService;     // Matching Module

    public void processFile(String jobId, String filePath, String insurerId) {

        // 1. Get metadata (DIRECT METHOD CALL - same JVM, < 1ms)
        InsurerConfiguration config = metadataService.getConfiguration(insurerId);

        // 2. Parse file
        List<Map<String, Object>> records = parseExcel(filePath, config);

        // 3. Match each record (DIRECT METHOD CALL - same JVM, < 1ms)
        for (Map<String, Object> record : records) {
            matchingService.processAndMatchPolicy(record);
        }

        // 4. Update status (DIRECT METHOD CALL - same JVM, < 1ms)
        ingestionService.updateStatus(jobId, IngestionStatus.COMPLETED);
    }
}
```

**Key Point**: These method calls are **50x faster** than HTTP calls because:

- No network overhead
- No JSON serialization/deserialization
- Same memory space
- Spring manages everything

---

## 8. Real World Example Walkthrough

Let's trace a **complete business scenario** from start to finish:

### Scenario: User uploads an HDFC Life insurance Excel file

#### Phase 1: File Upload

**Frontend Action:**

```javascript
// User clicks "Upload" button with Excel file
fetch("http://localhost:8080/api/bff/upload", {
  method: "POST",
  body: formData, // Contains Excel file
});
```

**What Happens:**

1. **BFF Service (Port 8080)** receives request

```java
@RestController
public class FileUploadController {

    @Autowired
    private IngestionClient ingestionClient;  // Feign client to Data-Pipeline

    @PostMapping("/api/bff/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        // BFF forwards to Data-Pipeline Service
        return ingestionClient.uploadFile(file, "HDFC_LIFE", "user123");
    }
}
```

2. **Data-Pipeline Service (Port 8082)** - Ingestion Module

```java
@RestController
public class IngestionController {

    @Autowired
    private IngestionService ingestionService;

    @PostMapping("/api/v1/ingestion/upload")
    public ResponseEntity<?> uploadFile(@RequestParam MultipartFile file) {

        // 1. Save file to disk
        String filePath = saveFileToDisk(file);

        // 2. Create job in MongoDB
        IngestionJob job = new IngestionJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setFilePath(filePath);
        job.setStatus(IngestionStatus.UPLOADED);
        ingestionRepository.save(job);  // Saves to MongoDB

        return ResponseEntity.ok(job);
    }
}
```

**Result**: File saved, job created in MongoDB with status "UPLOADED"

#### Phase 2: Background Processing

**Trigger**: Processing service picks up the job

```java
// FILE: ProcessingService.java

public void processFile(String jobId, String filePath, String insurerId) {

    // STEP 1: Get field mappings from Metadata Module
    // (Direct method call - NOT HTTP!)
    InsurerConfiguration config = metadataService.getConfiguration("HDFC_LIFE");
    // Returns: { "TERM_LIFE": [{ sourceField: "Name", targetField: "firstName" }...] }

    // STEP 2: Read Excel file
    FileInputStream fis = new FileInputStream(filePath);
    Workbook workbook = WorkbookFactory.create(fis);
    Sheet sheet = workbook.getSheetAt(0);

    // STEP 3: Parse each row
    for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);

        // Transform data using field mappings
        Map<String, Object> standardRecord = new HashMap<>();
        standardRecord.put("firstName", row.getCell(0).getStringCellValue());
        standardRecord.put("mobileNumber", row.getCell(1).getStringCellValue());
        standardRecord.put("policyNumber", row.getCell(2).getStringCellValue());
        // ...more fields

        // STEP 4: Match customer (Direct method call - NOT HTTP!)
        matchingService.processAndMatchPolicy(standardRecord);
    }

    // STEP 5: Update job status (Direct method call - NOT HTTP!)
    ingestionService.updateStatus(jobId, IngestionStatus.COMPLETED);
}
```

#### Phase 3: Customer Matching

```java
// FILE: MatchingService.java

public void processAndMatchPolicy(Map<String, Object> record) {

    String mobile = (String) record.get("mobileNumber");

    // Search for existing customer (HTTP call to Customer Service)
    Optional<CustomerDTO> customer = customerClient.searchByMobile(mobile);

    if (customer.isPresent()) {
        // Customer exists - stitch policy to them
        String customerId = customer.get().getCustomerId();

        // Create policy (HTTP call to Policy Service)
        PolicyDTO policy = new PolicyDTO();
        policy.setCustomerId(customerId);
        policy.setPolicyNumber((String) record.get("policyNumber"));
        policy.setSumAssured((BigDecimal) record.get("sumAssured"));

        policyClient.createPolicy(policy);  // Saves to PostgreSQL

        log.info("Policy {} stitched to customer {}", policy.getPolicyNumber(), customerId);
    } else {
        log.warn("Customer not found - sending to manual review");
    }
}
```

#### Phase 4: User Views Portfolio

**Frontend Action:**

```javascript
// User clicks "View My Policies"
fetch("http://localhost:8080/api/bff/portfolio/cust123", {
  headers: { Authorization: "Bearer jwt-token" },
});
```

**What Happens:**

```java
// BFF Service aggregates data from multiple services

@RestController
public class PortfolioController {

    @Autowired
    private CustomerClient customerClient;  // Port 8081

    @Autowired
    private PolicyClient policyClient;      // Port 8085

    @GetMapping("/api/bff/portfolio/{customerId}")
    public ResponseEntity<?> getPortfolio(@PathVariable String customerId) {

        // Parallel calls to multiple services
        CustomerDTO customer = customerClient.getCustomer(customerId);
        List<PolicyDTO> policies = policyClient.getPoliciesByCustomer(customerId);

        // Aggregate response
        PortfolioResponse response = new PortfolioResponse();
        response.setCustomer(customer);
        response.setPolicies(policies);
        response.setTotalPolicies(policies.size());
        response.setTotalCoverage(calculateTotalCoverage(policies));

        return ResponseEntity.ok(response);
    }
}
```

**Result**: Frontend displays all policies for the customer

---

## Key Takeaways

### 1. Spring Boot Architecture Pattern

```
HTTP Request → Controller (REST endpoint)
                   ↓
              Service (Business logic)
                   ↓
              Repository (Database)
                   ↓
              Database (PostgreSQL/MongoDB)
```

### 2. Dependency Injection

- Spring automatically creates and connects objects
- Use `@Autowired` or constructor injection
- No manual `new` keyword needed

### 3. Annotations Drive Behavior

- `@RestController` = Handles HTTP
- `@Service` = Business logic
- `@Repository` = Database access
- `@Entity` = Database table mapping

### 4. Consolidated Service Benefits

- **Internal modules**: Direct method calls (fast)
- **External services**: HTTP calls via Feign (when needed)
- **Best of both worlds**: Microservices architecture with performance

### 5. Configuration Over Code

- `application.properties` controls everything
- Database URLs, ports, external services
- No hardcoding needed

---

## Next Steps for Learning

1. **Start with one module**: Pick Customer Service - it's the simplest
2. **Read in order**: Controller → Service → Repository → Entity
3. **Run the application**: See logs, understand the flow
4. **Make a small change**: Add a new field, see what breaks
5. **Debug with breakpoints**: IntelliJ IDEA makes this easy
6. **Read Spring Boot docs**: https://spring.io/guides

---

## Common Questions

**Q: Why use interfaces for repositories?**  
A: Spring Data JPA implements them automatically. You just declare what you want.

**Q: What's @Autowired doing?**  
A: Spring finds the matching object and injects it (dependency injection).

**Q: How does Spring know which port to use?**  
A: From `application.properties`: `server.port=8082`

**Q: Why so many layers?**  
A: Separation of concerns - each layer has one job. Makes testing and maintenance easier.

**Q: What's the difference between JPA and MongoDB repositories?**  
A: JPA = SQL databases (PostgreSQL), MongoDB = NoSQL. Spring provides similar interfaces for both.

**Q: How do services communicate?**  
A: **Internal modules**: Direct method calls. **External services**: HTTP via Feign clients.

---

## Glossary

| Term           | Definition                                                          |
| -------------- | ------------------------------------------------------------------- |
| **Bean**       | An object managed by Spring (services, controllers, repositories)   |
| **JPA**        | Java Persistence API - standard for database access                 |
| **Feign**      | HTTP client for calling other services                              |
| **DTO**        | Data Transfer Object - simple object for moving data between layers |
| **Entity**     | Java class that maps to a database table                            |
| **Repository** | Interface for database operations                                   |
| **REST**       | Representational State Transfer - HTTP API style                    |
| **CRUD**       | Create, Read, Update, Delete - basic database operations            |
| **IoC**        | Inversion of Control - Spring manages object creation               |
| **DI**         | Dependency Injection - Spring injects dependencies                  |

---

## Diagram: Complete Request Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│ 1. HTTP Request                                                     │
│    POST http://localhost:8082/api/v1/metadata/config                │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2. Spring Boot receives request                                     │
│    - Parses JSON → Java objects                                     │
│    - Finds matching @PostMapping                                    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 3. MetadataController.createConfiguration() called                  │
│    - Validates parameters                                           │
│    - Calls metadataService.saveConfiguration()                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 4. MetadataService processes business logic                         │
│    - Checks if config exists                                        │
│    - Creates/updates InsurerConfiguration object                    │
│    - Calls repository.save()                                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 5. MetadataRepository.save() called                                 │
│    - Spring generates SQL INSERT/UPDATE                             │
│    - Executes query in PostgreSQL                                   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 6. PostgreSQL saves data                                            │
│    - Row inserted in insurer_configurations table                   │
│    - Returns saved row with generated ID                            │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 7. Response flows back up                                           │
│    Repository → Service → Controller                                │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 8. Spring converts Java → JSON                                      │
│    Returns HTTP 200 OK with configuration                           │
└─────────────────────────────────────────────────────────────────────┘
```

---

**You're now ready to explore the codebase!** Start with the Customer Service, then move to Data-Pipeline Service. Good luck! 🚀
