# Docker Deployment Guide for MyPolicy Backend

## 📋 Overview

This guide explains how to deploy all MyPolicy microservices using Docker Compose. The setup includes:

- **5 Microservices**: Config Server + 4 Application Services
- **2 Databases**: PostgreSQL + MongoDB
- **Automated Orchestration**: Proper startup order and health checks
- **Network Isolation**: All services in a dedicated Docker network

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Docker Network                          │
│                                                              │
│  ┌──────────────┐                                           │
│  │   Config     │  (Port 8888) - Starts First              │
│  │   Service    │  Centralized Configuration               │
│  └──────┬───────┘                                           │
│         │                                                    │
│         ├─────────┬──────────┬──────────────┬──────────┐   │
│         ▼         ▼          ▼              ▼          ▼   │
│  ┌──────────┐ ┌────────┐ ┌──────────┐ ┌─────────┐ ┌─────┐   │
│  │   BFF    │ │Customer│ │  Policy  │ │  Data   │ │DBs  │   │
│  │ Service  │ │ Service│ │ Service  │ │Pipeline │ │     │   │
│  │  :8080   │ │  :8081 │ │  :8085   │ │  :8082  │ │     │   │
│  └──────────┘ └────────┘ └──────────┘ └─────────┘ └─────┘   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- **Docker**: Version 20.10 or higher
- **Docker Compose**: Version 2.0 or higher
- **System Resources**: At least 8GB RAM, 20GB disk space

### Check Prerequisites

```powershell
# Check Docker version
docker --version

# Check Docker Compose version
docker compose version

# Verify Docker is running
docker ps
```

## 📦 Deployment Steps

### 1. Build and Start All Services

```powershell
# Navigate to project root
cd "d:\New folder (2)\INSURANCE POLICY\MyPolicy-Backend"

# Build and start all services in detached mode
docker compose up -d --build
```

### 2. Monitor Startup Progress

```powershell
# View logs for all services
docker compose logs -f

# View logs for specific service
docker compose logs -f config-service
docker compose logs -f customer-service

# Check service status
docker compose ps
```

### 3. Verify Services Are Running

```powershell
# Check Config Service
curl http://localhost:8888/actuator/health

# Check Customer Service
curl http://localhost:8081/actuator/health

# Check Policy Service
curl http://localhost:8085/actuator/health

# Check Data Pipeline Service
curl http://localhost:8082/actuator/health

# Check BFF Service
curl http://localhost:8080/actuator/health
```

## 🎯 Service Details

| Service                   | Port  | Status Endpoint    | Description                          |
| ------------------------- | ----- | ------------------ | ------------------------------------ |
| **Config Service**        | 8888  | `/actuator/health` | Centralized configuration management |
| **Customer Service**      | 8081  | `/actuator/health` | Customer data and operations         |
| **Policy Service**        | 8085  | `/actuator/health` | Insurance policy management          |
| **Data Pipeline Service** | 8082  | `/actuator/health` | Data ingestion, processing, matching |
| **BFF Service**           | 8080  | `/actuator/health` | API gateway and orchestration        |
| **PostgreSQL**            | 5432  | N/A                | Relational database                  |
| **MongoDB**               | 27017 | N/A                | Document database                    |

## 🔍 Service Startup Order

The docker-compose.yml ensures services start in the correct order:

1. **Databases** (PostgreSQL & MongoDB) - Start first with health checks
2. **Config Service** - Starts after databases, waits until healthy
3. **Application Services** - Start after Config Service is healthy
   - Customer Service
   - Policy Service
   - Data Pipeline Service (waits for Customer + Policy)
4. **BFF Service** - Starts last, after all backend services are ready

## 📊 Managing Services

### Stop All Services

```powershell
# Stop all services (keeps data)
docker compose stop

# Stop and remove containers (keeps volumes)
docker compose down

# Stop, remove containers, and remove volumes (DELETES ALL DATA)
docker compose down -v
```

### Restart Specific Service

```powershell
# Restart a single service
docker compose restart customer-service

# Rebuild and restart a service
docker compose up -d --build customer-service
```

### Scale Services (Optional)

```powershell
# Scale Customer Service to 3 instances
docker compose up -d --scale customer-service=3

# Note: You'll need a load balancer for this to work properly
```

### View Resource Usage

```powershell
# Check CPU and memory usage
docker stats

# Check disk usage
docker system df
```

## 🔧 Troubleshooting

### Service Won't Start

```powershell
# Check logs for errors
docker compose logs service-name

# Check if port is already in use
netstat -ano | findstr :8080

# Restart with fresh build
docker compose down
docker compose up -d --build
```

### Database Connection Issues

```powershell
# Connect to PostgreSQL container
docker exec -it mypolicy-postgres psql -U postgres -d mypolicy_db

# Connect to MongoDB container
docker exec -it mypolicy-mongodb mongosh -u admin -p admin123
```

### Config Service Not Responding

```powershell
# Check Config Service is healthy
docker compose ps config-service

# View Config Service logs
docker compose logs config-service

# Restart Config Service
docker compose restart config-service

# If other services fail, they'll auto-restart when Config is healthy
```

### Clean Slate Restart

```powershell
# Stop everything
docker compose down -v

# Remove all images
docker compose down --rmi all -v

# Rebuild from scratch
docker compose up -d --build
```

## 🌐 Network Configuration

All services communicate through the `mypolicy-network` Docker network:

```powershell
# Inspect network
docker network inspect mypolicy-backend_mypolicy-network

# List containers in network
docker network inspect mypolicy-backend_mypolicy-network --format '{{range .Containers}}{{.Name}} {{end}}'
```

## 💾 Data Persistence

### Volumes

Data is persisted in Docker volumes:

- `postgres_data`: PostgreSQL database files
- `mongodb_data`: MongoDB database files

```powershell
# List volumes
docker volume ls

# Inspect volume
docker volume inspect mypolicy-backend_postgres_data

# Backup PostgreSQL data
docker exec mypolicy-postgres pg_dump -U postgres mypolicy_db > backup.sql

# Backup MongoDB data
docker exec mypolicy-mongodb mongodump --out=/backup
```

## 🔐 Security Configuration

### Default Credentials

**Config Service:**

- Username: `admin`
- Password: `config123`

**PostgreSQL:**

- Username: `postgres`
- Password: `postgres`
- Database: `mypolicy_db`

**MongoDB:**

- Username: `admin`
- Password: `admin123`
- Database: `mypolicy`

⚠️ **IMPORTANT**: Change these credentials before deploying to production!

## 🧪 Testing the Setup

### 1. Health Check All Services

```powershell
# PowerShell script to check all services
$services = @(
    @{Name="Config Service"; Port=8888},
    @{Name="Customer Service"; Port=8081},
    @{Name="Policy Service"; Port=8085},
    @{Name="Data Pipeline Service"; Port=8082},
    @{Name="BFF Service"; Port=8080}
)

foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$($service.Port)/actuator/health" -UseBasicParsing
        Write-Host "$($service.Name): ✅ HEALTHY" -ForegroundColor Green
    } catch {
        Write-Host "$($service.Name): ❌ NOT RESPONDING" -ForegroundColor Red
    }
}
```

### 2. Test API Endpoints

```powershell
# Test Customer Service
curl http://localhost:8081/api/customers

# Test Policy Service
curl http://localhost:8085/api/policies

# Test Data Pipeline Service
curl http://localhost:8082/api/metadata/schema

# Test BFF Service
curl http://localhost:8080/api/health
```

## 📈 Monitoring and Logs

### View Logs

```powershell
# All services
docker compose logs -f

# Last 100 lines
docker compose logs --tail=100

# Specific time range
docker compose logs --since="2024-01-01T00:00:00" --until="2024-01-01T23:59:59"

# Filter by service
docker compose logs -f config-service customer-service
```

### Access Service Containers

```powershell
# Open shell in container
docker exec -it mypolicy-customer-service sh

# Run command in container
docker exec mypolicy-customer-service ps aux
```

## 🚀 Production Deployment

For production, modify `docker-compose.yml`:

1. **Use Environment Variables**

   ```yaml
   environment:
     SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
   ```

2. **Enable HTTPS**
   - Add SSL certificates
   - Configure reverse proxy (Nginx/Traefik)

3. **Add Health Checks**
   - Already configured for all services

4. **Resource Limits**

   ```yaml
   deploy:
     resources:
       limits:
         cpus: "2"
         memory: 2G
   ```

5. **Logging Drivers**
   ```yaml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

## 🆘 Common Issues

### Issue: "Port already in use"

```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID with actual ID)
taskkill /PID <PID> /F
```

### Issue: "Cannot connect to Docker daemon"

```powershell
# Start Docker Desktop
# Or start Docker service
net start docker
```

### Issue: "Service unhealthy"

```powershell
# Check service logs
docker compose logs service-name

# Restart with fresh build
docker compose up -d --build service-name
```

## 📚 Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)

## 🎓 Tips and Best Practices

1. **Always wait for Config Service** to be healthy before accessing other services
2. **Use health checks** to ensure services are truly ready
3. **Monitor resource usage** with `docker stats`
4. **Regular backups** of database volumes
5. **Update images regularly** for security patches
6. **Use `.dockerignore`** to reduce build context size
7. **Tag images** for versioning in production

---

**Ready to deploy?** Run `docker compose up -d --build` and access your services! 🚀
