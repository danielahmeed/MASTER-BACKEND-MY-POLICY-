# MyPolicy Backend - Quick Reference Guide

## 🚀 Getting Started in 5 Minutes

### 1. Setup Databases
```bash
# PostgreSQL
createdb mypolicy_customer_db
createdb mypolicy_metadata_db
createdb mypolicy_policy_db

# MongoDB (auto-created)
mongod --dbpath /data/db
```

### 2. Start All Services
```bash
cd customer-service && mvn spring-boot:run &
cd policy-service && mvn spring-boot:run &
cd ingestion-service && mvn spring-boot:run &
cd metadata-service && mvn spring-boot:run &
cd processing-service && mvn spring-boot:run &
cd matching-engine && mvn spring-boot:run &
cd bff-service && mvn spring-boot:run &
```

### 3. Test the System
```bash
# Register
curl -X POST http://localhost:8080/api/bff/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@test.com","password":"Pass123","mobileNumber":"9876543210"}'

# Login
curl -X POST http://localhost:8080/api/bff/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@test.com","password":"Pass123"}'
```

---

## 📊 Service Ports

| Service | Port | Health Check |
|---------|------|--------------|
| BFF | 8080 | http://localhost:8080/actuator/health |
| Customer | 8081 | http://localhost:8081/actuator/health |
| Ingestion | 8082 | http://localhost:8082/actuator/health |
| Metadata | 8083 | http://localhost:8083/actuator/health |
| Processing | 8084 | http://localhost:8084/actuator/health |
| Policy | 8085 | http://localhost:8085/actuator/health |
| Matching | 8086 | http://localhost:8086/actuator/health |

---

## 🎯 Key API Endpoints

### Authentication
```bash
POST /api/bff/auth/register  # Register user
POST /api/bff/auth/login     # Login & get JWT
```

### Portfolio
```bash
GET /api/bff/portfolio/{customerId}  # Unified view
```

### Insights
```bash
GET /api/bff/insights/{customerId}   # Coverage analysis
```

### File Upload
```bash
POST /api/bff/upload                 # Upload file
GET /api/bff/upload/status/{jobId}   # Check status
```

---

## 📚 Documentation Index

| Document | Purpose |
|----------|---------|
| [README.md](./README.md) | Quick start & overview |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Complete system design |
| [DEPLOYMENT.md](./DEPLOYMENT.md) | Deployment guide |
| [API_REFERENCE.md](./bff-service/API_REFERENCE.md) | API documentation |
| [PHASE3_IMPLEMENTATION.md](./PHASE3_IMPLEMENTATION.md) | Insights feature |
| [SEQUENCE_COMPLIANCE.md](./SEQUENCE_COMPLIANCE.md) | Design compliance |

---

## 🔧 Common Commands

### Build All Services
```bash
for service in customer-service ingestion-service metadata-service processing-service policy-service matching-engine bff-service; do
    cd $service && mvn clean install -DskipTests && cd ..
done
```

### Check All Services
```bash
for port in 8080 8081 8082 8083 8084 8085 8086; do
    curl -s http://localhost:$port/actuator/health | jq .
done
```

### View Logs
```bash
tail -f bff-service/logs/spring.log
```

---

## 🎉 Features Implemented

✅ 7 Microservices
✅ BFF API Gateway
✅ JWT Authentication
✅ PII Encryption
✅ Unified Portfolio View
✅ Coverage Insights & Recommendations
✅ Fuzzy Customer Matching
✅ Metadata-Driven Transformation
✅ Multi-Database Support
✅ 100% Sequence Diagram Compliance

---

## 🆘 Troubleshooting

### Port in use?
```bash
lsof -i :8080
kill -9 <PID>
```

### Database connection failed?
```bash
# Check PostgreSQL
sudo systemctl status postgresql

# Check MongoDB
sudo systemctl status mongod
```

### Service won't start?
```bash
# Check Java version
java -version  # Must be 17+

# Rebuild
cd <service> && mvn clean install
```

---

## 📞 Support

- **Documentation**: See docs above
- **Issues**: Check logs in `logs/` directory
- **Architecture**: See [ARCHITECTURE.md](./ARCHITECTURE.md)

---

**Built with ❤️ for MyPolicy Insurance Aggregation Platform**
