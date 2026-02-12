# MyPolicy Backend - Insurance Aggregation Platform

> Complete microservices architecture for aggregating insurance policies from multiple insurers with intelligent insights and recommendations.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14+-blue.svg)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-6.0+-green.svg)](https://www.mongodb.com/)

---

## 🎯 Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- MongoDB 6.0+

### Setup Databases
```bash
# PostgreSQL
createdb mypolicy_customer_db
createdb mypolicy_metadata_db
createdb mypolicy_policy_db

# MongoDB (auto-created on first use)
mongod --dbpath /data/db
```

### Start Services
```bash
# Start all services in order
cd customer-service && mvn spring-boot:run &
cd policy-service && mvn spring-boot:run &
cd ingestion-service && mvn spring-boot:run &
cd metadata-service && mvn spring-boot:run &
cd processing-service && mvn spring-boot:run &
cd matching-engine && mvn spring-boot:run &
cd bff-service && mvn spring-boot:run &
```

### Test the System
```bash
# Register user
curl -X POST http://localhost:8080/api/bff/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@example.com","password":"Pass123"}'

# Login
curl -X POST http://localhost:8080/api/bff/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"Pass123"}'
```

---

## 📊 System Architecture

```
Frontend → BFF Service (8080) → [Customer, Policy, Ingestion, Metadata]
                                        ↓
                                [Processing, Matching Engine]
                                        ↓
                                [PostgreSQL, MongoDB]
```

### Services Overview

| Service | Port | Purpose | Database |
|---------|------|---------|----------|
| **BFF Service** | 8080 | API Gateway & Aggregator | - |
| Customer Service | 8081 | User Management & Auth | PostgreSQL |
| Ingestion Service | 8082 | File Upload Handling | MongoDB |
| Metadata Service | 8083 | Field Mapping Rules | PostgreSQL |
| Processing Service | 8084 | Data Transformation | - |
| Policy Service | 8085 | Policy Storage | PostgreSQL |
| Matching Engine | 8086 | Customer Matching | - |

---

## 🚀 Key Features

### 1. **Unified Portfolio View**
Single API call to get complete customer portfolio with all policies and totals.

```http
GET /api/bff/portfolio/{customerId}
```

**Response**: Customer details + All policies + Aggregated totals

### 2. **Coverage Insights & Recommendations** ⭐
AI-powered coverage gap analysis with personalized recommendations.

```http
GET /api/bff/insights/{customerId}
```

**Features**:
- Coverage breakdown by policy type
- Gap analysis (current vs recommended)
- Severity levels (HIGH/MEDIUM/LOW)
- Actionable recommendations
- Coverage score (0-100)
- Human-readable advisory

### 3. **Multi-Insurer File Upload**
Upload Excel/CSV files from any insurer with automatic data transformation.

```http
POST /api/bff/upload
```

**Features**:
- Metadata-driven field mapping
- Automatic data validation
- Fuzzy customer matching
- Job tracking

### 4. **Secure Authentication**
JWT-based authentication with PII encryption.

```http
POST /api/bff/auth/login
```

---

## 📖 Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Complete system architecture and design |
| [API_REFERENCE.md](./bff-service/API_REFERENCE.md) | BFF API endpoints with examples |
| [PHASE3_IMPLEMENTATION.md](./PHASE3_IMPLEMENTATION.md) | Coverage insights implementation details |
| [SEQUENCE_COMPLIANCE.md](./SEQUENCE_COMPLIANCE.md) | API sequence diagram compliance |

---

## 🔄 Data Flow

### User Registration → Login → Portfolio View
```
1. User registers → Customer Service → JWT token
2. User logs in → BFF validates → JWT token
3. User requests portfolio → BFF aggregates → [Customer + Policies]
4. User gets insights → BFF analyzes → [Gaps + Recommendations]
```

### File Upload → Processing → Matching
```
1. User uploads file → Ingestion Service → MongoDB
2. Processing Service → Reads file → Applies metadata rules
3. Matching Engine → Finds/creates customer → Links policy
4. Policy Service → Stores policy → Complete
```

---

## 🎨 API Examples

### Get Portfolio (Aggregated)
```bash
curl -X GET "http://localhost:8080/api/bff/portfolio/CUST123" \
  -H "Authorization: Bearer <JWT>"
```

**Response**:
```json
{
  "customer": { "customerId": "CUST123", "firstName": "John", ... },
  "policies": [ { "policyNumber": "POL001", "premium": 15000, ... } ],
  "totalPolicies": 5,
  "totalPremium": 50000,
  "totalCoverage": 10000000
}
```

### Get Coverage Insights
```bash
curl -X GET "http://localhost:8080/api/bff/insights/CUST123" \
  -H "Authorization: Bearer <JWT>"
```

**Response**:
```json
{
  "overallScore": { "score": 60, "rating": "GOOD" },
  "gaps": [
    {
      "policyType": "TERM_LIFE",
      "gap": 5000000,
      "severity": "HIGH",
      "advisory": "Your current coverage of ₹50 L is below recommended ₹1 Cr"
    }
  ],
  "recommendations": [
    {
      "title": "Increase Life Insurance Coverage",
      "priority": "CRITICAL",
      "suggestedCoverage": 10000000,
      "estimatedPremium": 50000
    }
  ]
}
```

---

## 🔐 Security Features

- ✅ JWT authentication with 24-hour expiration
- ✅ BCrypt password hashing (strength: 10)
- ✅ AES-256 encryption for PII fields
- ✅ HTTPS for all communications
- ✅ Input validation at API gateway
- ✅ SQL injection prevention (JPA)

---

## 🧪 Testing

### End-to-End Test Flow
```bash
# 1. Register
curl -X POST http://localhost:8080/api/bff/auth/register -d '{...}'

# 2. Login
curl -X POST http://localhost:8080/api/bff/auth/login -d '{...}'

# 3. Configure metadata
curl -X POST http://localhost:8083/api/v1/metadata/config -d '{...}'

# 4. Upload file
curl -X POST http://localhost:8080/api/bff/upload -F "file=@policies.xlsx"

# 5. Get portfolio
curl -X GET http://localhost:8080/api/bff/portfolio/CUST123

# 6. Get insights
curl -X GET http://localhost:8080/api/bff/insights/CUST123
```

---

## 📦 Technology Stack

### Backend
- **Framework**: Spring Boot 3.1.5
- **Language**: Java 17
- **Build Tool**: Maven 3.8+

### Databases
- **PostgreSQL 14+**: Customer, Metadata, Policy data
- **MongoDB 6.0+**: Ingestion job tracking

### Libraries
- **Spring Cloud OpenFeign**: Inter-service communication
- **Spring Security + JWT**: Authentication
- **Apache POI**: Excel processing
- **Apache Commons Text**: Fuzzy matching
- **Hypersistence Utils**: JSONB support
- **Lombok**: Boilerplate reduction

---

## 🎯 Sequence Diagram Compliance

✅ **Phase 1**: Data Ingestion & Stitching - **100% Complete**
✅ **Phase 2**: User Access & Unified View - **100% Complete**
✅ **Phase 3**: Coverage Insights & Metrics - **100% Complete**

**Overall Compliance**: **100%** ✅

See [SEQUENCE_COMPLIANCE.md](./SEQUENCE_COMPLIANCE.md) for detailed analysis.

---

## 🚧 Future Enhancements

- [ ] Kafka/RabbitMQ for async processing
- [ ] Spring Cloud Gateway for advanced routing
- [ ] Eureka Server for service discovery
- [ ] Resilience4j for circuit breaking
- [ ] Zipkin/Jaeger for distributed tracing
- [ ] Redis for caching
- [ ] Prometheus + Grafana for monitoring
- [ ] ML-based personalized recommendations

---

## 📝 Project Structure

```
MyPolicy-Backend/
├── bff-service/              # API Gateway (Port 8080)
├── customer-service/         # User Management (Port 8081)
├── ingestion-service/        # File Upload (Port 8082)
├── metadata-service/         # Field Mappings (Port 8083)
├── processing-service/       # Data Transformation (Port 8084)
├── policy-service/           # Policy Storage (Port 8085)
├── matching-engine/          # Customer Matching (Port 8086)
├── ARCHITECTURE.md           # Complete architecture docs
├── API_REFERENCE.md          # API documentation
└── README.md                 # This file
```

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

---

## 📄 License

This project is licensed under the MIT License.

---

## 📧 Support

For issues or questions:
- **Email**: support@mypolicy.com
- **Documentation**: [ARCHITECTURE.md](./ARCHITECTURE.md)
- **API Docs**: [API_REFERENCE.md](./bff-service/API_REFERENCE.md)

---

## ⭐ Highlights

- **7 Microservices** working in harmony
- **3 Databases** (PostgreSQL + MongoDB)
- **100% Sequence Diagram Compliance**
- **Production-Ready** architecture
- **Comprehensive Documentation**
- **Secure & Scalable**

---

**Built with ❤️ for insurance aggregation**
