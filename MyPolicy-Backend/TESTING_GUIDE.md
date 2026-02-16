# MyPolicy Microservices - Testing Guide

## 🎯 Quick Test: Customer Service (Standalone)

Let's test the Customer Service first as it has minimal dependencies.

---

## Prerequisites Check

### 1. Verify Java Installation
```bash
java --version
# Should show: openjdk 17.0.18 or higher
```

### 2. Verify Maven Installation
```bash
mvn --version
# Should show: Apache Maven 3.8+
```

### 3. Verify PostgreSQL Installation
```bash
psql --version
# Should show: PostgreSQL 14+
```

---

## Option 1: Test Customer Service (Simplest)

### Step 1: Create Database

**Option A - Using psql command line:**
```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE mypolicy_customer_db;

# Verify
\l

# Exit
\q
```

**Option B - Using pgAdmin:**
1. Open pgAdmin
2. Right-click "Databases" → "Create" → "Database"
3. Name: `mypolicy_customer_db`
4. Click "Save"

**Option C - Let Spring Boot handle it (if using H2 for testing):**
Skip database creation - we'll use H2 in-memory database for quick testing.

---

### Step 2: Update Database Password

Edit: `customer-service/src/main/resources/application.properties`

```properties
# Update this line with your PostgreSQL password
spring.datasource.password=YOUR_POSTGRES_PASSWORD
```

---

### Step 3: Start Customer Service

**Open Terminal/PowerShell:**
```bash
cd "d:\New folder (2)\INSURANCE POLICY\MyPolicy-Backend\customer-service"
mvn spring-boot:run
```

**Expected Output:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.1.5)

...
Started CustomerServiceApplication in X.XXX seconds
```

**Service Running on:** http://localhost:8081

---

### Step 4: Test Customer Service APIs

**Open a new terminal and run these commands:**

#### Test 1: Health Check
```bash
curl http://localhost:8081/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

#### Test 2: Register a Customer
```bash
curl -X POST http://localhost:8081/api/v1/customers/register ^
  -H "Content-Type: application/json" ^
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@test.com\",\"mobileNumber\":\"9876543210\",\"panNumber\":\"ABCDE1234F\",\"dateOfBirth\":\"1990-01-01\",\"address\":\"123 Main St\",\"password\":\"Test@123\"}"
```

**Expected Response:**
```json
{
  "customerId": "uuid-here",
  "firstName": "John",
  "lastName": "Doe",
  "email": "encrypted-email",
  "status": "ACTIVE"
}
```

#### Test 3: Login
```bash
curl -X POST http://localhost:8081/api/v1/customers/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"john.doe@test.com\",\"password\":\"Test@123\"}"
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "customer": {
    "customerId": "uuid-here",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

#### Test 4: Get Customer (use customerId from registration)
```bash
curl http://localhost:8081/api/v1/customers/{customerId}
```

---

## Option 2: Test with H2 In-Memory Database (No PostgreSQL Setup)

If you want to test quickly without PostgreSQL setup:

### Step 1: Update pom.xml

Add H2 dependency to `customer-service/pom.xml`:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Step 2: Update application.properties

Create `application-test.properties`:

```properties
server.port=8081
spring.application.name=customer-service

# H2 Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true
spring.h2.console.enabled=true
```

### Step 3: Run with Test Profile
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

---

## Option 3: Test Complete Flow (All Services)

### Prerequisites:
1. PostgreSQL running with 3 databases created
2. MongoDB running

### Start Services in Order:

**Terminal 1 - Customer Service:**
```bash
cd customer-service
mvn spring-boot:run
```

**Terminal 2 - Policy Service:**
```bash
cd policy-service
mvn spring-boot:run
```

**Terminal 3 - Ingestion Service:**
```bash
cd ingestion-service
mvn spring-boot:run
```

**Terminal 4 - Metadata Service:**
```bash
cd metadata-service
mvn spring-boot:run
```

**Terminal 5 - Processing Service:**
```bash
cd processing-service
mvn spring-boot:run
```

**Terminal 6 - Matching Engine:**
```bash
cd matching-engine
mvn spring-boot:run
```

**Terminal 7 - BFF Service:**
```bash
cd bff-service
mvn spring-boot:run
```

### Test Complete Flow:

```bash
# 1. Register via BFF
curl -X POST http://localhost:8080/api/bff/auth/register ^
  -H "Content-Type: application/json" ^
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john@test.com\",\"password\":\"Test@123\",\"mobileNumber\":\"9876543210\"}"

# 2. Login via BFF
curl -X POST http://localhost:8080/api/bff/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"email\":\"john@test.com\",\"password\":\"Test@123\"}"

# Save the token from response
# 3. Get Portfolio (use token)
curl -X GET http://localhost:8080/api/bff/portfolio/{customerId} ^
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

---

## Troubleshooting

### Issue 1: Port Already in Use
```bash
# Windows - Find process on port 8081
netstat -ano | findstr :8081

# Kill process
taskkill /PID <PID> /F
```

### Issue 2: Database Connection Failed
```
Error: Connection refused
```

**Solution:**
- Verify PostgreSQL is running: `pg_ctl status`
- Check credentials in application.properties
- Ensure database exists: `psql -U postgres -l`

### Issue 3: Maven Build Failed
```bash
# Clean and rebuild
mvn clean install -DskipTests
```

### Issue 4: Java Version Mismatch
```bash
# Check Java version
java --version

# Should be 17+
# If not, install Java 17 or set JAVA_HOME
```

---

## Quick Test Script (PowerShell)

Save as `test-customer-service.ps1`:

```powershell
# Test Customer Service
Write-Host "Testing Customer Service..." -ForegroundColor Green

# Health Check
Write-Host "`n1. Health Check:" -ForegroundColor Yellow
curl http://localhost:8081/actuator/health

# Register
Write-Host "`n2. Register Customer:" -ForegroundColor Yellow
$registerResponse = curl -X POST http://localhost:8081/api/v1/customers/register `
  -H "Content-Type: application/json" `
  -d '{"firstName":"Test","lastName":"User","email":"test@example.com","password":"Test@123","mobileNumber":"9876543210"}'

Write-Host $registerResponse

# Login
Write-Host "`n3. Login:" -ForegroundColor Yellow
$loginResponse = curl -X POST http://localhost:8081/api/v1/customers/login `
  -H "Content-Type: application/json" `
  -d '{"email":"test@example.com","password":"Test@123"}'

Write-Host $loginResponse
```

Run:
```bash
.\test-customer-service.ps1
```

---

## Expected Service Startup Times

| Service | Startup Time | Port |
|---------|-------------|------|
| Customer | ~10-15 sec | 8081 |
| Policy | ~10-15 sec | 8085 |
| Ingestion | ~8-12 sec | 8082 |
| Metadata | ~10-15 sec | 8083 |
| Processing | ~8-12 sec | 8084 |
| Matching | ~8-12 sec | 8086 |
| BFF | ~12-18 sec | 8080 |

---

## Recommended Testing Order

1. ✅ **Customer Service** (Standalone - easiest)
2. ✅ **Policy Service** (Standalone)
3. ✅ **Metadata Service** (Standalone)
4. ✅ **Ingestion Service** (Needs MongoDB)
5. ✅ **BFF + Customer + Policy** (Integration test)
6. ✅ **Complete Flow** (All 7 services)

---

## Next Steps

After successful testing:
1. ✅ Configure production database credentials
2. ✅ Set up environment variables
3. ✅ Deploy to Docker containers
4. ✅ Set up CI/CD pipeline
5. ✅ Configure monitoring (Prometheus/Grafana)

---

## Need Help?

- **Logs Location**: Check console output
- **Database Issues**: See DEPLOYMENT.md
- **API Documentation**: See API_REFERENCE.md
- **Architecture**: See ARCHITECTURE.md
