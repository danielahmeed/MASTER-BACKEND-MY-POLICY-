# MyPolicy Deployment Guide

## 📋 Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Database Configuration](#database-configuration)
4. [Service Configuration](#service-configuration)
5. [Starting Services](#starting-services)
6. [Health Checks](#health-checks)
7. [Troubleshooting](#troubleshooting)
8. [Production Deployment](#production-deployment)

---

## Prerequisites

### Required Software
- **Java**: JDK 17 or higher
- **Maven**: 3.8 or higher
- **PostgreSQL**: 14 or higher
- **MongoDB**: 6.0 or higher
- **Git**: Latest version

### Verify Installation
```bash
java -version        # Should show Java 17+
mvn -version         # Should show Maven 3.8+
psql --version       # Should show PostgreSQL 14+
mongod --version     # Should show MongoDB 6.0+
```

---

## Local Development Setup

### 1. Clone Repository
```bash
git clone <repository-url>
cd MyPolicy-Backend
```

### 2. Build All Services
```bash
# Build all services at once
for service in customer-service ingestion-service metadata-service processing-service policy-service matching-engine bff-service; do
    cd $service
    mvn clean install -DskipTests
    cd ..
done
```

Or build individually:
```bash
cd customer-service && mvn clean install -DskipTests
cd ../ingestion-service && mvn clean install -DskipTests
# ... repeat for all services
```

---

## Database Configuration

### PostgreSQL Setup

#### 1. Install PostgreSQL
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib

# macOS
brew install postgresql@14
brew services start postgresql@14

# Windows
# Download installer from https://www.postgresql.org/download/windows/
```

#### 2. Create Databases
```bash
# Connect to PostgreSQL
psql -U postgres

# Create databases
CREATE DATABASE mypolicy_customer_db;
CREATE DATABASE mypolicy_metadata_db;
CREATE DATABASE mypolicy_policy_db;

# Create user (optional)
CREATE USER mypolicy WITH PASSWORD 'your_secure_password';

# Grant privileges
GRANT ALL PRIVILEGES ON DATABASE mypolicy_customer_db TO mypolicy;
GRANT ALL PRIVILEGES ON DATABASE mypolicy_metadata_db TO mypolicy;
GRANT ALL PRIVILEGES ON DATABASE mypolicy_policy_db TO mypolicy;

# Exit
\q
```

#### 3. Verify Databases
```bash
psql -U postgres -l
# Should show all three databases
```

### MongoDB Setup

#### 1. Install MongoDB
```bash
# Ubuntu/Debian
wget -qO - https://www.mongodb.org/static/pgp/server-6.0.asc | sudo apt-key add -
sudo apt-get install -y mongodb-org

# macOS
brew tap mongodb/brew
brew install mongodb-community@6.0

# Windows
# Download installer from https://www.mongodb.com/try/download/community
```

#### 2. Start MongoDB
```bash
# Ubuntu/Debian
sudo systemctl start mongod
sudo systemctl enable mongod

# macOS
brew services start mongodb-community@6.0

# Windows
# MongoDB runs as a service automatically
```

#### 3. Verify MongoDB
```bash
mongosh
> show dbs
> use mypolicy_ingestion_db
> exit
```

---

## Service Configuration

### Update Application Properties

Each service has an `application.properties` file. Update database credentials if needed:

#### Customer Service
```properties
# customer-service/src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mypolicy_customer_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

#### Metadata Service
```properties
# metadata-service/src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mypolicy_metadata_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

#### Policy Service
```properties
# policy-service/src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mypolicy_policy_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

#### Ingestion Service
```properties
# ingestion-service/src/main/resources/application.properties
spring.data.mongodb.uri=mongodb://localhost:27017/mypolicy_ingestion_db
```

---

## Starting Services

### Option 1: Manual Start (Recommended for Development)

Start services in this order:

#### 1. Core Services (Parallel)
```bash
# Terminal 1 - Customer Service
cd customer-service
mvn spring-boot:run

# Terminal 2 - Policy Service
cd policy-service
mvn spring-boot:run

# Terminal 3 - Ingestion Service
cd ingestion-service
mvn spring-boot:run

# Terminal 4 - Metadata Service
cd metadata-service
mvn spring-boot:run
```

#### 2. Processing Services (After core services are up)
```bash
# Terminal 5 - Processing Service
cd processing-service
mvn spring-boot:run

# Terminal 6 - Matching Engine
cd matching-engine
mvn spring-boot:run
```

#### 3. API Gateway (Last)
```bash
# Terminal 7 - BFF Service
cd bff-service
mvn spring-boot:run
```

### Option 2: Background Start (Linux/Mac)
```bash
#!/bin/bash
# start-all.sh

# Start core services
cd customer-service && mvn spring-boot:run > logs/customer.log 2>&1 &
cd ../policy-service && mvn spring-boot:run > logs/policy.log 2>&1 &
cd ../ingestion-service && mvn spring-boot:run > logs/ingestion.log 2>&1 &
cd ../metadata-service && mvn spring-boot:run > logs/metadata.log 2>&1 &

# Wait for core services to start
sleep 30

# Start processing services
cd ../processing-service && mvn spring-boot:run > logs/processing.log 2>&1 &
cd ../matching-engine && mvn spring-boot:run > logs/matching.log 2>&1 &

# Wait for processing services
sleep 20

# Start BFF
cd ../bff-service && mvn spring-boot:run > logs/bff.log 2>&1 &

echo "All services started. Check logs/ directory for output."
```

Make executable and run:
```bash
chmod +x start-all.sh
./start-all.sh
```

### Option 3: Using JAR Files (Production-like)
```bash
# Build JARs
for service in customer-service ingestion-service metadata-service processing-service policy-service matching-engine bff-service; do
    cd $service
    mvn clean package -DskipTests
    cd ..
done

# Run JARs
java -jar customer-service/target/customer-service-0.0.1-SNAPSHOT.jar &
java -jar policy-service/target/policy-service-0.0.1-SNAPSHOT.jar &
java -jar ingestion-service/target/ingestion-service-0.0.1-SNAPSHOT.jar &
java -jar metadata-service/target/metadata-service-0.0.1-SNAPSHOT.jar &
sleep 30
java -jar processing-service/target/processing-service-0.0.1-SNAPSHOT.jar &
java -jar matching-engine/target/matching-engine-0.0.1-SNAPSHOT.jar &
sleep 20
java -jar bff-service/target/bff-service-0.0.1-SNAPSHOT.jar &
```

---

## Health Checks

### Verify All Services Are Running

```bash
# Check all services
curl http://localhost:8080/actuator/health  # BFF Service
curl http://localhost:8081/actuator/health  # Customer Service
curl http://localhost:8082/actuator/health  # Ingestion Service
curl http://localhost:8083/actuator/health  # Metadata Service
curl http://localhost:8084/actuator/health  # Processing Service
curl http://localhost:8085/actuator/health  # Policy Service
curl http://localhost:8086/actuator/health  # Matching Engine
```

Expected response for each:
```json
{
  "status": "UP"
}
```

### Automated Health Check Script
```bash
#!/bin/bash
# health-check.sh

services=(
  "BFF:8080"
  "Customer:8081"
  "Ingestion:8082"
  "Metadata:8083"
  "Processing:8084"
  "Policy:8085"
  "Matching:8086"
)

for service in "${services[@]}"; do
  name="${service%%:*}"
  port="${service##*:}"
  
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)
  
  if [ "$status" == "200" ]; then
    echo "✅ $name Service (Port $port): UP"
  else
    echo "❌ $name Service (Port $port): DOWN"
  fi
done
```

---

## Troubleshooting

### Common Issues

#### 1. Port Already in Use
```bash
# Find process using port
lsof -i :8080  # Replace with your port

# Kill process
kill -9 <PID>
```

#### 2. Database Connection Failed
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Check MongoDB is running
sudo systemctl status mongod

# Test connection
psql -U postgres -h localhost -p 5432
mongosh --host localhost --port 27017
```

#### 3. Service Won't Start
```bash
# Check logs
tail -f customer-service/logs/spring.log

# Check Java version
java -version  # Must be 17+

# Check Maven version
mvn -version   # Must be 3.8+

# Rebuild service
cd customer-service
mvn clean install -DskipTests
```

#### 4. Feign Client Errors
```bash
# Ensure dependent services are running
# Example: BFF depends on Customer, Policy, Ingestion

# Check service URLs in application.properties
# Verify ports match running services
```

#### 5. Database Schema Issues
```bash
# Enable auto-create (development only)
spring.jpa.hibernate.ddl-auto=update

# Or manually create tables
psql -U postgres -d mypolicy_customer_db -f schema.sql
```

---

## Production Deployment

### Docker Deployment

#### 1. Create Dockerfiles

**customer-service/Dockerfile**:
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/customer-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Repeat for all services.

#### 2. Docker Compose
```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  mongodb:
    image: mongo:6.0
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

  customer-service:
    build: ./customer-service
    ports:
      - "8081:8081"
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/mypolicy_customer_db

  # ... add all other services

volumes:
  postgres-data:
  mongo-data:
```

#### 3. Run with Docker Compose
```bash
docker-compose up -d
```

### Kubernetes Deployment

#### 1. Create Deployment YAML
```yaml
# customer-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: customer-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: customer-service
  template:
    metadata:
      labels:
        app: customer-service
    spec:
      containers:
      - name: customer-service
        image: mypolicy/customer-service:latest
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres-service:5432/mypolicy_customer_db
---
apiVersion: v1
kind: Service
metadata:
  name: customer-service
spec:
  selector:
    app: customer-service
  ports:
  - port: 8081
    targetPort: 8081
```

#### 2. Deploy to Kubernetes
```bash
kubectl apply -f customer-service-deployment.yaml
# Repeat for all services
```

### Environment Variables

For production, use environment variables instead of hardcoded values:

```properties
# application.properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/mypolicy_customer_db}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:password}
```

Set in environment:
```bash
export DB_URL=jdbc:postgresql://prod-db:5432/mypolicy_customer_db
export DB_USERNAME=mypolicy_user
export DB_PASSWORD=secure_prod_password
```

---

## Performance Tuning

### JVM Options
```bash
java -Xms512m -Xmx2g -XX:+UseG1GC -jar customer-service.jar
```

### Database Connection Pool
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Feign Timeouts
```properties
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=10000
```

---

## Monitoring

### Enable Actuator Endpoints
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.health.show-details=always
```

### Access Metrics
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
```

---

## Backup & Recovery

### Database Backup
```bash
# PostgreSQL
pg_dump -U postgres mypolicy_customer_db > customer_backup.sql

# MongoDB
mongodump --db mypolicy_ingestion_db --out /backup/
```

### Database Restore
```bash
# PostgreSQL
psql -U postgres mypolicy_customer_db < customer_backup.sql

# MongoDB
mongorestore --db mypolicy_ingestion_db /backup/mypolicy_ingestion_db/
```

---

## Security Checklist

- [ ] Change default passwords
- [ ] Enable HTTPS/TLS
- [ ] Configure firewall rules
- [ ] Set up JWT secret key
- [ ] Enable database encryption
- [ ] Configure CORS properly
- [ ] Set up rate limiting
- [ ] Enable audit logging
- [ ] Regular security updates

---

## Next Steps

1. ✅ Complete local setup
2. ✅ Verify all services are running
3. ✅ Run health checks
4. ✅ Test end-to-end flow
5. ✅ Configure monitoring
6. ✅ Set up CI/CD pipeline
7. ✅ Deploy to staging
8. ✅ Deploy to production

---

For detailed architecture information, see [ARCHITECTURE.md](./ARCHITECTURE.md)
