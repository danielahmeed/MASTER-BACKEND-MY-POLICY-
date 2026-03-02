# MongoDB Atlas Setup Guide – Data Pipeline Service

> **Note:** As of the latest update, the data-pipeline-service uses **H2 only** (no MongoDB). This guide is kept for reference if you need to integrate Atlas in the future. To run the service: `mvn spring-boot:run` (uses H2 in-memory).

---

This guide walks you through creating a MongoDB database on MongoDB Atlas (for reference).

---

## Prerequisites

- A MongoDB Atlas account (free at [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas))
- An email address for sign-up

---

## Step 1: Create a MongoDB Atlas Account

1. Go to **https://www.mongodb.com/cloud/atlas/register**
2. Enter your **email**, **first name**, **last name**, and **password**
3. Click **Create your Atlas account**
4. Complete email verification if prompted
5. Log in to the Atlas dashboard

---

## Step 2: Create an Organization and Project (First-Time Setup)

If this is your first project:

1. **Create Organization**
   - Organization name: e.g. `MyPolicy` or your company name
   - Click **Next**

2. **Create Project**
   - Project name: e.g. `MyPolicy-DataPipeline`
   - Click **Next**
   - Skip adding team members (or add later)
   - Click **Create Project**

---

## Step 3: Build a Database (Create Cluster)

1. In the Atlas dashboard, click **"Build a Database"** (or **Create** if you already have clusters)

2. **Choose deployment type**
   - Select **M0 FREE** (Shared) – free tier, enough for dev/testing
   - Or **M10+** for production (paid)

3. **Choose a Cloud Provider & Region**
   - Provider: **AWS**, **Google Cloud**, or **Azure**
   - Region: Choose closest to your app (e.g. `us-east-1`, `eu-west-1`, `ap-south-1`)
   - Keep the default cluster name or change it (e.g. `Cluster0` or `DataPipelineCluster`)

4. Click **Create**

5. Wait 1–3 minutes for the cluster to provision (status changes to **Available**)

---

## Step 4: Create a Database User

1. During setup you’ll see **"Security Quickstart"** or **"Create Database User"**
   - If you skip it, go to **Database Access** in the left sidebar and click **Add New Database User**

2. **Authentication Method**
   - Select **Password**

3. **User Details**
   - Username: e.g. `datapipeline` or `mypolicy_user`
   - Password: generate a strong one (click **Autogenerate Secure Password**) and **copy/save it**

4. **Database User Privileges**
   - Select **Built-in Role** → **Read and write to any database**
   - (For production, consider a more restricted role and separate DB)

5. Click **Add User**

---

## Step 5: Configure Network Access (IP Whitelist)

1. In the left sidebar, go to **Network Access**
2. Click **Add IP Address**

3. **Development / local testing**
   - Click **Allow Access from Anywhere**
   - This adds `0.0.0.0/0` (allows access from any IP)
   - Use only for development; restrict IPs for production

4. **Production**
   - Click **Add Current IP Address** (for your server IP)
   - Or add specific IP ranges (e.g. `203.0.113.0/24`)
   - Add each server or CI/CD IP that will connect

5. Optionally add a **Comment** (e.g. "Data Pipeline Service – Production")

6. Click **Confirm**

---

## Step 6: Get the Connection String

1. Go back to **Database** in the left sidebar
2. Find your cluster and click **Connect**

3. **Choose connection method**
   - Select **Drivers** (for application connection)

4. **Driver and version**
   - Language: **Java**
   - Version: **4.1 or later** (or latest)
   - Copy the connection string, e.g.:

```
mongodb+srv://<username>:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
```

5. **Adjust the connection string**
   - Replace `<username>` with your DB username (e.g. `datapipeline`)
   - Replace `<password>` with your DB password
   - Add database name if desired: before `?` add `/ingestion_db`:

**Example:**
```
mongodb+srv://datapipeline:YourPassword123@cluster0.abc12.mongodb.net/ingestion_db?retryWrites=true&w=majority
```

---

## Step 7: Create the Database (Optional but Recommended)

1. In the left sidebar, click **Database**
2. Click **Browse Collections** on your cluster
3. Click **Add My Own Data**
4. **Database name:** `ingestion_db`
5. **Collection name:** `ingestion_jobs` (first collection)
6. Click **Create**

Repeat for:
- `customer_portfolios` (in the same `ingestion_db` database)

Or let the app create collections when it first writes.

---

## Step 8: Configure Data Pipeline Service

### Option A: application.yaml (for deployment)

Add or update in `src/main/resources/application.yaml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb+srv://datapipeline:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/ingestion_db?retryWrites=true&w=majority
```

Replace:
- `YOUR_PASSWORD` with the real password (URL-encode special chars)
- `cluster0.xxxxx` with your actual cluster host from Atlas

### Option B: Environment Variable (recommended for production)

Set before starting the app:

```bash
export SPRING_DATA_MONGODB_URI="mongodb+srv://datapipeline:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/ingestion_db?retryWrites=true&w=majority"
```

Windows PowerShell:
```powershell
$env:SPRING_DATA_MONGODB_URI = "mongodb+srv://datapipeline:YOUR_PASSWORD@cluster0.xxxxx.mongodb.net/ingestion_db?retryWrites=true&w=majority"
```

### Option C: application-{profile}.yaml

Create `application-prod.yaml`:

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
```

Then set `MONGODB_URI` in the environment.

---

## Step 9: Run with Atlas Profile

The data-pipeline-service has an **atlas** profile that connects to MongoDB Atlas and uses H2 for job tracking (no PostgreSQL needed).

**Important:** Use **only** the `atlas` profile. Do **not** add `local`—it overrides MongoDB to `localhost`, so you would still connect to a local MongoDB instead of Atlas.

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=atlas"
```

**Port conflict?** If port 8082 is already in use:
- Windows: `netstat -ano | findstr :8082` then `taskkill /PID <pid> /F`
- Or stop the other data-pipeline instance

---

## Step 10: Verify Connection

1. Start the data-pipeline-service
2. Check logs for Mongo connection messages (no connection errors)
3. Upload a file via the Insurer Portal
4. In Atlas: **Database** → **Browse Collections** → **ingestion_db** → **ingestion_jobs**
5. Confirm a new document appears after upload

---

## URL-Encoding Special Characters in Password

If the password has special characters, URL-encode them:

| Character | Encoded |
|-----------|---------|
| @         | %40     |
| #         | %23     |
| $         | %24     |
| %         | %25     |
| &         | %26     |
| +         | %2B     |
| =         | %3D     |
| :         | %3A     |
| /         | %2F     |

Example: password `P@ss#123` → `P%40ss%23123` in the URI.

---

## Security Checklist for Production

- [ ] Use a restricted DB user (read/write only to `ingestion_db`)
- [ ] Remove `0.0.0.0/0` and whitelist only your app server IPs
- [ ] Store the connection string in environment variables or secrets (not in code)
- [ ] Enable Atlas audit logging if required
- [ ] Use VPC Peering or PrivateLink if available for your cloud provider

---

## Quick Reference

| Item             | Example                                             |
|------------------|-----------------------------------------------------|
| Cluster URL      | `cluster0.abc12.mongodb.net`                        |
| Database         | `ingestion_db`                                      |
| Collections      | `ingestion_jobs`, `customer_portfolios`             |
| Connection string| `mongodb+srv://user:pass@cluster0.xxx.mongodb.net/ingestion_db?retryWrites=true&w=majority` |

---

## Troubleshooting

| Issue                 | Possible fix                                                        |
|-----------------------|---------------------------------------------------------------------|
| Connection timeout    | Check Network Access: add your IP, or allow `0.0.0.0/0` for testing |
| Authentication failed| Verify username/password and URL encoding                           |
| SSL/TLS error         | Atlas uses TLS by default; ensure the app’s MongoDB driver is current |
| Database not found   | Create `ingestion_db` in Atlas or let the app create it on first write |

---

*End of Document*
