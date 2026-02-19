# 🚀 Service Startup Scripts

Quick and easy way to start MyPolicy Backend services on Windows.

## 📋 Prerequisites

Before running these scripts, ensure you have:

- ✅ **Java 17 or higher** installed
- ✅ **PostgreSQL** running on `localhost:5432`
  - Database: `mypolicy_db`
  - Username: `postgres`
  - Password: `postgres123`
- ✅ **MongoDB** running on `localhost:27017` (for Ingestion Service)

## 🎯 Available Scripts

### 1. **start-customer-service.ps1** ⭐ (Quick Start)

Starts only the Customer Service (Port 8081) with customer update feature.

**Usage:**

```powershell
# Right-click PowerShell → "Run as Administrator"
cd "d:\New folder (2)\INSURANCE POLICY\MyPolicy-Backend"
.\start-customer-service.ps1
```

**What it does:**

- ✅ Checks if Maven is installed (installs if needed)
- ✅ Checks Java and PostgreSQL
- ✅ Builds the Customer Service
- ✅ Starts the service on port 8081

**Endpoints available:**

- `POST /api/v1/customers/register` - Register new customer
- `POST /api/v1/customers/login` - User login
- `GET /api/v1/customers/{id}` - Get customer details
- `PUT /api/v1/customers/{id}` - Update customer (NEW ⭐)

---

### 2. **start-services.ps1** (Full Stack)

Interactive menu to start any or all services.

**Usage:**

```powershell
# Right-click PowerShell → "Run as Administrator"
cd "d:\New folder (2)\INSURANCE POLICY\MyPolicy-Backend"
.\start-services.ps1
```

**Menu Options:**

```
1. Customer Service (Port 8081)
2. BFF Service (Port 8080) - API Gateway
3. Ingestion Service (Port 8082)
4. Metadata Service (Port 8083)
5. Processing Service (Port 8084)
6. Policy Service (Port 8085)
7. Matching Engine (Port 8086)
8. Start ALL Services
9. Exit
```

**What it does:**

- ✅ Installs Maven automatically if missing
- ✅ Checks all prerequisites
- ✅ Opens each service in a separate PowerShell window
- ✅ Manages build and startup for selected services

---

## ⚡ Quick Start Guide

### First Time Setup:

1. **Open PowerShell as Administrator:**
   - Press `Win + X`
   - Select "Windows PowerShell (Admin)" or "Terminal (Admin)"

2. **Navigate to project:**

   ```powershell
   cd "d:\New folder (2)\INSURANCE POLICY\MyPolicy-Backend"
   ```

3. **Run the quick start script:**

   ```powershell
   .\start-customer-service.ps1
   ```

4. **Wait for startup messages:**

   ```
   Started CustomerServiceApplication in X.XXX seconds
   ```

5. **Service is ready!**
   - URL: http://localhost:8081
   - Ready to accept requests

---

## 🔧 Troubleshooting

### Maven Installation Issues

If Maven fails to install automatically:

**Install Chocolatey first:**

```powershell
# Run as Administrator
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

**Then install Maven:**

```powershell
choco install maven -y
```

**Restart PowerShell** after Maven installation.

---

### PostgreSQL Connection Error

If you see database connection errors:

1. **Check if PostgreSQL is running:**

   ```powershell
   Get-Service -Name "postgresql*"
   ```

2. **Start PostgreSQL if stopped:**

   ```powershell
   Start-Service -Name "postgresql-x64-XX"  # Replace XX with your version
   ```

3. **Create database if missing:**

   ```sql
   -- Connect with psql or pgAdmin
   CREATE DATABASE mypolicy_db;
   ```

4. **Update credentials in application.properties:**
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/mypolicy_db
   spring.datasource.username=postgres
   spring.datasource.password=YOUR_PASSWORD
   ```

---

### Port Already in Use

If you see "Port 8081 is already in use":

**Find process using the port:**

```powershell
netstat -ano | findstr :8081
```

**Kill the process:**

```powershell
taskkill /PID <PID_NUMBER> /F
```

Or use a different port in `application.properties`:

```properties
server.port=8091
```

---

## 🧪 Testing the Service

Once the Customer Service is running, test it:

### Using PowerShell:

**Register a new user:**

```powershell
$body = @{
    firstName = "John"
    lastName = "Doe"
    email = "john.doe@example.com"
    password = "Test@1234"
    mobileNumber = "9876543210"
    panNumber = "ABCDE1234F"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/customers/register" -Method POST -Body $body -ContentType "application/json"
```

**Login:**

```powershell
$loginBody = @{
    email = "john.doe@example.com"
    password = "Test@1234"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8081/api/v1/customers/login" -Method POST -Body $loginBody -ContentType "application/json"
$token = $response.token
```

**Update customer (NEW ⭐):**

```powershell
$updateBody = @{
    firstName = "Jonathan"
    mobileNumber = "9876543211"
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $token"
}

Invoke-RestMethod -Uri "http://localhost:8081/api/v1/customers/CUSTOMER_ID" -Method PUT -Body $updateBody -ContentType "application/json" -Headers $headers
```

---

## 📊 Service Ports Reference

| Service                 | Port | Purpose                                |
| ----------------------- | ---- | -------------------------------------- |
| **BFF Service**         | 8080 | API Gateway - Entry point for frontend |
| **Customer Service** ⭐ | 8081 | User management & authentication       |
| **Ingestion Service**   | 8082 | File upload & job tracking             |
| **Metadata Service**    | 8083 | Insurer configurations & mappings      |
| **Processing Service**  | 8084 | File parsing & transformation          |
| **Policy Service**      | 8085 | Policy management                      |
| **Matching Engine**     | 8086 | Customer matching & fuzzy logic        |

---

## 🔄 Stopping Services

**To stop a running service:**

- Press `Ctrl + C` in the PowerShell window
- Or close the PowerShell window

**To stop all services at once:**

```powershell
# Find all Maven processes
Get-Process -Name "java" | Where-Object {$_.CommandLine -like "*maven*"} | Stop-Process -Force
```

---

## 📚 Additional Documentation

- **[README.md](./README.md)** - Complete project documentation
- **[QUICK_START.md](./QUICK_START.md)** - Detailed startup guide
- **[TESTING_GUIDE.md](./TESTING_GUIDE.md)** - Testing instructions
- **[CUSTOMER_DATA_CORRECTION.md](./CUSTOMER_DATA_CORRECTION.md)** - Customer update feature guide
- **[COMPLETE_DIAGRAMS.md](./COMPLETE_DIAGRAMS.md)** - All system diagrams

---

## ✅ Script Features

Both startup scripts include:

- ✅ **Automatic Maven installation** (via Chocolatey)
- ✅ **Administrator privilege check**
- ✅ **Java version verification**
- ✅ **Database connection check**
- ✅ **Automatic build before startup**
- ✅ **Clear error messages and guidance**
- ✅ **Color-coded console output**
- ✅ **Service health verification**

---

## 💡 Tips

1. **First time running?** Use `start-customer-service.ps1` - it's simpler
2. **Need all services?** Use `start-services.ps1` option 8
3. **Development mode?** Start only the services you're working on
4. **After Maven install?** Restart PowerShell for changes to take effect
5. **Multiple instances?** Each service opens in its own window

---

## 🆘 Need Help?

If you encounter issues:

1. ✅ Check you're running as **Administrator**
2. ✅ Verify **PostgreSQL** is running
3. ✅ Ensure **Java 17+** is installed
4. ✅ Check **port availability** (8080-8086)
5. ✅ Review error messages in the console
6. ✅ Check logs in `target/` folders

---

**Happy coding! 🚀**
