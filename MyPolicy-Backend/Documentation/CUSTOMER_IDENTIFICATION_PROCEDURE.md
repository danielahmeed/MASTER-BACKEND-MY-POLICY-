# Customer Identification Procedure – Multi-Level Matching with Safety

**Document Version:** 1.0  
**Purpose:** Define how to identify “whose details we need to get” when processing policy data, with clear fallback levels and security controls.

---

## 1. Overview

When ingesting policy data (CSV/Excel), we must determine which customer a record belongs to. This document defines:

1. **Identifier hierarchy** – Which identifiers to try first and in what order  
2. **Fallback levels** – What to do when a higher-priority identifier does not match  
3. **Confidence levels** – How certain we are about a match  
4. **Safety measures** – Data protection, audit, and consent

---

## 2. Identifier Hierarchy (Priority Order)

Use identifiers in this order. Stop as soon as a confident match is found.

| Level | Identifier | Type | Confidence | When to Use |
|-------|------------|------|------------|-------------|
| **1** | **PAN** (Permanent Account Number) | Exact | **Highest** | PAN is unique per person in India; government-issued |
| **2** | **Email** | Exact | **High** | Unique per user; rarely shared |
| **3** | **Mobile Number** | Exact | **High** | Unique per SIM; stable identifier |
| **4** | **PAN + Mobile** | Composite | **High** | Both must match |
| **5** | **PAN + Email** | Composite | **High** | Both must match |
| **6** | **Mobile + DOB** | Composite | **Medium** | Reduces false positives |
| **7** | **Email + DOB** | Composite | **Medium** | Reduces false positives |
| **8** | **No Match** | — | — | Create new customer or route to manual review |

---

## 3. Detailed Procedure

### Step 1: Validate Input Identifiers

Before any lookup, validate that identifiers meet format and business rules:

| Identifier | Validation | Example |
|------------|------------|---------|
| PAN | Format: 5 letters + 4 digits + 1 letter (e.g. ABCDE1234F) | `^[A-Z]{5}[0-9]{4}[A-Z]{1}$` |
| Email | RFC 5322, non-empty | `john@example.com` |
| Mobile | 10 digits (India) | `9876543210` |
| Date of Birth | Valid date, reasonable range (e.g. 1950–today) | `1990-01-15` |
| Name | Non-empty, reasonable length | `John Doe` |

**Safety:** Reject or sanitize invalid formats. Log validation failures.

---

### Step 2: Level 1 – PAN Match (Highest Confidence)

**Procedure:**
1. If PAN is provided and valid:
   - Call `GET /api/v1/customers/by-pan/{panNumber}`
   - If exactly one customer is found → **MATCH** (confidence: HIGH)
   - If no customer found → go to Step 3

**Safety:**
- PAN is sensitive; use only for lookup, never log raw PAN
- Compare using normalized PAN (uppercase, trimmed)
- PAN is assumed unique per person; no fuzzy matching

---

### Step 3: Level 2 – Email Match

**Procedure:**
1. If PAN did not match and email is provided:
   - Call `GET /api/v1/customers/search/email/{email}` (or equivalent)
   - If exactly one customer found → **MATCH** (confidence: HIGH)
   - If no customer found → go to Step 4

**Safety:**
- Email stored encrypted (AES-256); search uses encrypted comparison or indexed hash
- Normalize email (lowercase, trim) before search
- Check for multiple results (duplicate emails); if > 1, treat as NO MATCH and flag

---

### Step 4: Level 3 – Mobile Match

**Procedure:**
1. If email did not match and mobile is provided:
   - Call `GET /api/v1/customers/search/mobile/{mobile}`
   - If exactly one customer found → **MATCH** (confidence: HIGH)
   - If no customer found → go to Step 5

**Safety:**
- Mobile can be reassigned; consider composite matches (Mobile+DOB) for extra certainty
- Log all lookups for audit

---

### Step 5: Level 4–5 – Composite Matches (PAN + Mobile/Email)

**Procedure:**
1. If we have both PAN and Mobile (or PAN and Email):
   - Search by PAN first
   - If found, verify Mobile (or Email) matches
   - If both match → **MATCH** (confidence: HIGH)
   - If PAN matches but Mobile/Email differs → flag for data correction (customer may have updated contact)

---

### Step 6: Level 6–7 – Composite Matches (Mobile/Email + DOB)

**Procedure:**
1. If no PAN and we have Mobile + DOB (or Email + DOB):
   - Search by Mobile (or Email)
   - If found, verify DOB matches
   - If both match → **MATCH** (confidence: MEDIUM)
   - If DOB differs → NO MATCH; do not link

**Safety:**
- DOB is PII; handle with same care as PAN
- Slight date differences (e.g. DD/MM vs MM/DD) can be normalized before comparison

---

### Step 8: No Match – Create New or Manual Review

**Procedure:**
1. If no match at any level:
   - **Option A:** Create new customer (for first-time policies)
   - **Option B:** Route to manual review queue (for correction/update scenarios)
   - **Option C:** Reject record and log (e.g. duplicate file, bad data)

**Safety:**
- New customers: validate all mandatory fields, allow no duplicate PAN/email/mobile
- Manual review: store in queue with full context (source file, row, identifiers)

---

## 4. Confidence Levels and Actions

| Confidence | Definition | Action |
|------------|------------|--------|
| **HIGH** | Exact match on PAN, Email, or Mobile; or composite PAN+Mobile, PAN+Email | Auto-link policy to customer |
| **MEDIUM** | Composite match (Mobile+DOB, Email+DOB) | Auto-link if configured; else manual review |
| **NONE** | No match at any level | Create new customer or route to manual review |

---

## 5. Safety Measures

### 5.1 Data Protection

| Measure | Implementation |
|---------|----------------|
| **PII Encryption** | Store PAN, email, mobile, DOB, address encrypted (AES-256) at rest |
| **No Plain-Text Logging** | Never log raw PAN, full mobile, or full email; use masks (e.g. `ABC***34F`, `9876****10`) |
| **Secure Transport** | HTTPS for all APIs; TLS 1.2+ only |
| **Access Control** | RBAC; only authorized roles can access customer lookup APIs |
| **Audit Trail** | Log who searched, when, which identifiers (masked), and match result |

### 5.2 Validation and Integrity

| Measure | Implementation |
|---------|----------------|
| **Format Validation** | Validate PAN, email, mobile before any lookup |
| **Duplicate Prevention** | Before creating customer, check PAN/email/mobile uniqueness |
| **Idempotency** | Same record + same identifiers = same result; no duplicate policies |
| **Transaction Safety** | Use transactions for create/update; rollback on error |

### 5.3 Matching Safety

| Measure | Implementation |
|---------|----------------|
| **Conservative Defaults** | When in doubt, route to manual review; prefer false negatives over false positives |
| **Multi-Factor Preferred** | Prefer matches with 2+ matching identifiers over single-identifier matches |
| **Review Queue** | All low-confidence matches go to a review queue with full context |

### 5.4 Compliance and Consent

| Measure | Implementation |
|---------|----------------|
| **Data Minimization** | Store only identifiers needed for matching and business |
| **Purpose Limitation** | Use identifiers only for customer resolution and policy linking |
| **Retention** | Define retention for manual review queue and audit logs |
| **Right to Correction** | Support customer data correction APIs (as per CUSTOMER_DATA_CORRECTION.md) |

---

## 6. Decision Flow (Summary)

```
Input: policy record (PAN, email, mobile, DOB)
    |
    v
[1] PAN valid? --> Search by PAN --> Found? --YES--> MATCH (HIGH)
    |                             --NO--> continue
    v
[2] Email valid? --> Search by Email --> Found? --YES--> MATCH (HIGH)
    |                                  --NO--> continue
    v
[3] Mobile valid? --> Search by Mobile --> Found? --YES--> MATCH (HIGH)
    |                                     --NO--> continue
    v
[4] PAN + Mobile? --> Search PAN, verify Mobile --> Both match? --YES--> MATCH (HIGH)
    |                                                    --NO--> continue
    v
[5] PAN + Email? --> Search PAN, verify Email --> Both match? --YES--> MATCH (HIGH)
    |                                                   --NO--> continue
    v
[6] Mobile + DOB? --> Search Mobile, verify DOB --> Both match? --YES--> MATCH (MEDIUM)
    |                                                     --NO--> continue
    v
[7] Email + DOB? --> Search Email, verify DOB --> Both match? --YES--> MATCH (MEDIUM)
    |                                                    --NO--> continue
    v
[8] NO MATCH --> Create New Customer OR Manual Review
```

---

## 7. API Endpoints Required

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/customers/by-pan/{panNumber}` | Level 1: PAN lookup |
| `GET /api/v1/customers/search/email/{email}` | Level 2: Email lookup *(to be added if missing)* |
| `GET /api/v1/customers/search/mobile/{mobile}` | Level 3: Mobile lookup |
| `GET /api/v1/customers/{customerId}` | Fetch full customer for verification |

---

## 8. Current vs. Recommended Implementation

| Aspect | Current (MatchingService) | Recommended |
|--------|---------------------------|-------------|
| Primary identifier | Mobile only | PAN first, then Email, then Mobile |
| Fallback | Mobile only | Full hierarchy (Levels 1–8, no fuzzy) |
| Composite matches | Not implemented | PAN+Mobile, Mobile+DOB, etc. |
| Confidence handling | Single threshold | HIGH / MEDIUM / LOW with different actions |
| Manual review | TODO | Implement review queue |

---

**End of Document**
