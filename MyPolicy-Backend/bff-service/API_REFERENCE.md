# BFF Service - API Reference

## Base URL
```
http://localhost:8080
```

---

## Authentication Endpoints

### Register User
```http
POST /api/bff/auth/register
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "panNumber": "ABCDE1234F",
  "dateOfBirth": "1990-01-01",
  "address": "123 Main St",
  "password": "SecurePass123"
}

Response: 200 OK
{
  "customerId": "uuid",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "mobileNumber": "9876543210",
  "status": "ACTIVE"
}
```

### Login
```http
POST /api/bff/auth/login
Content-Type: application/json

{
  "email": "john.doe@example.com",
  "password": "SecurePass123"
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "customer": {
    "customerId": "uuid",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  }
}
```

---

## Portfolio Endpoints

### Get Complete Portfolio
**Aggregates customer data + all policies in single call**

```http
GET /api/bff/portfolio/{customerId}
Authorization: Bearer <JWT_TOKEN>

Response: 200 OK
{
  "customer": {
    "customerId": "CUST123",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "mobileNumber": "9876543210",
    "status": "ACTIVE"
  },
  "policies": [
    {
      "id": "POL001",
      "policyNumber": "HDFC/TERM/2024/001",
      "insurerId": "HDFC_LIFE",
      "policyType": "TERM_LIFE",
      "planName": "Click 2 Protect Plus",
      "premiumAmount": 15000,
      "sumAssured": 5000000,
      "startDate": "2024-01-01",
      "endDate": "2044-01-01",
      "status": "ACTIVE"
    },
    {
      "id": "POL002",
      "policyNumber": "ICICI/HEALTH/2024/002",
      "insurerId": "ICICI_LOMBARD",
      "policyType": "HEALTH",
      "planName": "Complete Health Insurance",
      "premiumAmount": 12000,
      "sumAssured": 1000000,
      "startDate": "2024-02-01",
      "endDate": "2025-02-01",
      "status": "ACTIVE"
    }
  ],
  "totalPolicies": 2,
  "totalPremium": 27000,
  "totalCoverage": 6000000
}
```

---

## File Upload Endpoints

### Upload Insurer File
```http
POST /api/bff/upload
Content-Type: multipart/form-data
Authorization: Bearer <JWT_TOKEN>

Form Data:
- file: [Excel/CSV file]
- customerId: CUST123
- insurerId: HDFC_LIFE

Response: 200 OK
{
  "jobId": "JOB_uuid",
  "status": "UPLOADED",
  "fileName": "hdfc_policies.xlsx",
  "uploadedAt": "2024-01-15T10:30:00"
}
```

### Get Upload Status
```http
GET /api/bff/upload/status/{jobId}
Authorization: Bearer <JWT_TOKEN>

Response: 200 OK
{
  "jobId": "JOB_uuid",
  "status": "PROCESSING",
  "totalRecords": 100,
  "processedRecords": 45,
  "validationErrors": []
}
```

---

## Benefits of Using BFF

### Before BFF (Multiple Calls)
```javascript
// Frontend had to make 2 separate calls
const customer = await fetch('/customer-service/api/v1/customers/123');
const policies = await fetch('/policy-service/api/v1/policies/customer/123');

// Then aggregate manually
const totalPremium = policies.reduce((sum, p) => sum + p.premium, 0);
```

### After BFF (Single Call)
```javascript
// Single call, pre-aggregated data
const portfolio = await fetch('/api/bff/portfolio/123');
// portfolio.totalPremium already calculated
```

---

## Error Responses

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

### 404 Not Found
```json
{
  "error": "Not Found",
  "message": "Customer not found"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal Server Error",
  "message": "Service temporarily unavailable"
}
```

---

## Coverage Insights & Recommendations

### Get Coverage Analysis
**Analyzes customer's current coverage and provides gap analysis with recommendations**

```http
GET /api/bff/insights/{customerId}
Authorization: Bearer <JWT_TOKEN>

Response: 200 OK
{
  "customerId": "CUST123",
  "customerName": "John Doe",
  "coverageByType": {
    "TERM_LIFE": {
      "policyType": "TERM_LIFE",
      "policyCount": 2,
      "totalCoverage": 5000000,
      "totalPremium": 25000,
      "recommendedCoverage": 10000000,
      "adequate": false
    },
    "HEALTH": {
      "policyType": "HEALTH",
      "policyCount": 1,
      "totalCoverage": 1000000,
      "totalPremium": 30000,
      "recommendedCoverage": 1000000,
      "adequate": true
    }
  },
  "totalCoverage": 6000000,
  "totalPremium": 55000,
  "gaps": [
    {
      "policyType": "TERM_LIFE",
      "currentCoverage": 5000000,
      "recommendedCoverage": 10000000,
      "gap": 5000000,
      "severity": "HIGH",
      "advisory": "Your current term life coverage of ₹50 L is below the recommended ₹1 Cr. Consider increasing by ₹50 L."
    },
    {
      "policyType": "MOTOR",
      "currentCoverage": 0,
      "recommendedCoverage": 500000,
      "gap": 500000,
      "severity": "HIGH",
      "advisory": "You don't have any motor coverage. We recommend ₹5 L coverage to protect yourself and your family."
    }
  ],
  "recommendations": [
    {
      "policyType": "TERM_LIFE",
      "title": "Increase Life Insurance Coverage",
      "description": "We recommend adding ₹50 L in term life coverage to ensure comprehensive protection.",
      "suggestedCoverage": 10000000,
      "estimatedPremium": 50000,
      "priority": "CRITICAL",
      "rationale": "Life insurance should cover 10-15 times your annual income to ensure your family's financial security."
    },
    {
      "policyType": "MOTOR",
      "title": "Add Motor Insurance Coverage",
      "description": "We recommend adding ₹5 L in motor coverage to ensure comprehensive protection.",
      "suggestedCoverage": 500000,
      "estimatedPremium": 10000,
      "priority": "CRITICAL",
      "rationale": "Comprehensive motor insurance protects your vehicle and provides third-party liability coverage as mandated by law."
    }
  ],
  "overallScore": {
    "score": 40,
    "rating": "FAIR",
    "summary": "Fair coverage. We recommend addressing 2 coverage gap(s) to improve your protection."
  }
}
```

### Coverage Score Ratings
- **EXCELLENT** (80-100): Comprehensive coverage across all key areas
- **GOOD** (60-79): Adequate coverage with minor gaps
- **FAIR** (40-59): Moderate coverage with several gaps
- **POOR** (0-39): Significant coverage gaps requiring immediate attention

### Gap Severity Levels
- **HIGH**: No coverage or less than 50% of recommended
- **MEDIUM**: 50-75% of recommended coverage
- **LOW**: 75-99% of recommended coverage

---

## Service Health Check


```http
GET /actuator/health

Response: 200 OK
{
  "status": "UP"
}
```
