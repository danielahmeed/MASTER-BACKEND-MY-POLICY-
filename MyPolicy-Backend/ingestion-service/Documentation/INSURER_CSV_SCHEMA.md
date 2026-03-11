# Insurer CSV Upload Schema – MyPolicy

This document defines the **mandatory schema** that all insurers must follow when uploading policy data via the MyPolicy ingestion API.

---

## 1. File Format

- **Format:** CSV (comma-separated) or Excel (.xls, .xlsx)
- **Encoding:** UTF-8
- **Max size:** 50 MB
- **First row:** Must be a header row with column names

---

## 2. Required Columns (All Policy Types)

These columns **must** be present in every upload:

| Column Name       | Type   | Required | Description                                      | Example                    |
|-------------------|--------|----------|--------------------------------------------------|----------------------------|
| **policyNumber**  | String | Yes      | Unique policy identifier                         | AUPOL100000, LIPOL1000     |
| **customerName**  | String | Yes      | Full name of policyholder                        | Rahul Sharma               |
| **insurerId**     | String | Yes      | Insurer code (uppercase, underscore)             | HDFC_LIFE, ICICI_LOMBARD   |
| **policyType**    | String | Yes      | One of: TERM_LIFE, HEALTH, MOTOR, HOME, TRAVEL   | TERM_LIFE                  |
| **sumAssured**    | Number | Yes      | Coverage amount (₹)                              | 500000                     |
| **premiumAmount** | Number | Yes      | Annual premium (₹)                               | 25000                      |
| **startDate**     | Date   | Yes      | Policy start date (YYYY-MM-DD or YYYYMMDD)        | 2024-01-15                 |
| **endDate**       | Date   | Yes      | Policy end date (YYYY-MM-DD or YYYYMMDD)         | 2034-01-14                 |
| **email**         | String | Yes      | Customer email                                   | customer@example.com       |
| **mobileNumber**  | String | Yes      | Customer mobile (10+ digits)                      | 9876543210                 |
| **panNumber**     | String | Yes      | PAN (10 chars) – for deduplication               | ABCDE1234F                 |
| **dateOfBirth**   | Date   | Yes      | Customer DOB (YYYY-MM-DD or YYYYMMDD)             | 1990-05-15                 |
| **planName**      | String | Yes      | Plan/product name                                | Standard Plan              |
| **city**          | String | Yes      | City/location                                    | MUMBAI                     |

---

## 3. Policy-Type-Specific Columns

### 3.1 TERM_LIFE / Life Insurance

| Column Name     | Type   | Required | Description              |
|-----------------|--------|----------|--------------------------|
| policyTerm      | Number | No       | Term in years            |
| nomineeName     | String | No       | Nominee full name        |
| nomineeRelation | String | No       | e.g. Spouse, Parent      |
| gender          | String | No       | M / F / O                |

### 3.2 MOTOR / Auto Insurance

| Column Name   | Type   | Required | Description           |
|---------------|--------|----------|-----------------------|
| vehicleType   | String | No       | Car, Bike, etc.       |
| vehicleRegNo  | String | No       | Registration number   |
| idv           | Number | No       | Insured declared value|

### 3.3 HEALTH

| Column Name   | Type   | Required | Description                    |
|---------------|--------|----------|--------------------------------|
| coverageType  | String | No       | Individual, Family Floater     |

---

## 4. Column Name Aliases (Accepted)

Insurers may use these **alternative header names**; the system will map them:

| Standard Name  | Acceptable Aliases                                          |
|----------------|------------------------------------------------------------|
| policyNumber   | PolicyNumber, Policy Num, PolicyNum                        |
| customerName   | CustomerName, Customer Name                                |
| sumAssured     | SumAssured, Coverage Amount, IDV (for Motor)               |
| premiumAmount  | premiumAmount, AnnualPremium, AnnualPrem                    |
| startDate      | startDate, PolicyStart, PolicyStartDate                     |
| endDate        | endDate, PolicyEnd, PolicyEndDate                           |
| dateOfBirth    | DOB, dateOfBirth                                           |
| panNumber      | PAN, panNumber                                             |

---

## 5. Data Format Rules

| Field          | Format                    | Rules                                      |
|----------------|---------------------------|--------------------------------------------|
| **Dates**      | YYYY-MM-DD or YYYYMMDD    | Valid calendar date                        |
| **Numbers**    | Decimal, no currency symbol| sumAssured > 0, premiumAmount >= 0         |
| **policyType** | Enum                      | TERM_LIFE, HEALTH, MOTOR, HOME, TRAVEL     |
| **insurerId**  | Uppercase, underscore     | e.g. HDFC_LIFE, ICICI_LOMBARD              |
| **mobileNumber** | Digits only             | Min 10 digits                              |
| **email**      | Valid email format        |                                            |

---

## 6. Example Minimal Valid CSV

```csv
policyNumber,customerName,insurerId,policyType,sumAssured,premiumAmount,startDate,endDate,email,mobileNumber
POL001,Rahul Sharma,HDFC_LIFE,TERM_LIFE,5000000,25000,2024-01-01,2034-01-01,rahul@example.com,9876543210
POL002,Priya Patel,ICICI_LOMBARD,HEALTH,1000000,15000,2024-02-01,2025-02-01,priya@example.com,9876543211
```

---

## 7. Validation Errors

Uploads that fail schema validation will be rejected with specific error messages indicating:
- Missing required columns
- Invalid data types or formats
- policyType not in allowed values
- Duplicate policyNumber within the file

---

## 8. Supported insurerId Values

Coordinate with MyPolicy to register your `insurerId`. Common examples:
- HDFC_LIFE
- ICICI_LOMBARD
- SBI_LIFE
- BAJAJ_ALLIANZ
- TATA_AIG

---

*Document version: 1.0 | MyPolicy Ingestion API*
