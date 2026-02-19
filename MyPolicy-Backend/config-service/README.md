# Configuration Service - Centralized Config Management

## Overview

The **Configuration Service** (port 8888) is a centralized configuration server that manages all microservice configurations from a single location using **Spring Cloud Config Server**.

## Why Configuration Service?

### Problems Without Config Server:

- ❌ Configuration scattered across 5+ service directories
- ❌ Updating common settings requires editing multiple files
- ❌ No version control for configuration changes
- ❌ Sensitive data (passwords, keys) stored in plain text
- ❌ Manual service restarts needed for config changes

### Benefits of Config Server:

- ✅ **Single Source of Truth**: All configs in one place
- ✅ **Environment Management**: dev, staging, prod profiles
- ✅ **Dynamic Updates**: Change config without service restart
- ✅ **Version Control**: Track who changed what and when (Git)
- ✅ **Encryption**: Secure sensitive properties
- ✅ **Consistency**: Same config format across all services

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│          Config Service (Port 8888)                      │
│                                                          │
│  ┌────────────────────────────────────────────────┐    │
│  │         config-repo/                           │    │
│  │  ├── bff-service.properties                    │    │
│  │  ├── customer-service.properties               │    │
│  │  ├── data-pipeline-service.properties          │    │
│  │  └── policy-service.properties                 │    │
│  └────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
                        ▲  ▲  ▲  ▲
                        │  │  │  │ (HTTP GET /service-name/profile)
            ┌───────────┴──┴──┴──┴──────────┐
            │                               │
    ┌───────▼─────┐  ┌──────────▼──────┐  │
    │ BFF Service │  │ Customer Service │  │
    │  (8080)     │  │     (8081)      │  │
    └─────────────┘  └─────────────────┘  │
                                           │
    ┌────────────────▼────┐  ┌───────────▼──────┐
    │ Data-Pipeline       │  │  Policy Service  │
    │   Service (8082)    │  │     (8085)       │
    └─────────────────────┘  └──────────────────┘
```

## Directory Structure

```
config-service/
├── pom.xml                                    # Maven dependencies
├── src/
│   └── main/
│       ├── java/com/mypolicy/config/
│       │   └── ConfigServiceApplication.java  # Main class with @EnableConfigServer
│       └── resources/
│           ├── application.properties         # Config server settings
│           └── config-repo/                   # Configuration files
│               ├── bff-service.properties
│               ├── customer-service.properties
│               ├── data-pipeline-service.properties
│               └── policy-service.properties
└── README.md
```

## Configuration Files

### config-repo/bff-service.properties

- Service URLs (customer, policy, data-pipeline)
- Feign configuration
- BFF-specific settings

### config-repo/customer-service.properties

- PostgreSQL connection settings
- JPA/Hibernate configuration
- Customer service settings

### config-repo/data-pipeline-service.properties

- PostgreSQL (Metadata module)
- MongoDB (Ingestion module)
- Module-specific settings (ingestion, metadata, processing, matching)

### config-repo/policy-service.properties

- PostgreSQL connection settings
- Policy service settings

## How It Works

### 1. Config Server Startup

```bash
cd config-service
mvn spring-boot:run

# Server starts on port 8888
# Loads all .properties files from config-repo/
```

### 2. Client Service Requests Config

When a service (e.g., Customer Service) starts:

```
1. Service reads bootstrap.properties:
   spring.cloud.config.uri=http://localhost:8888

2. Service requests config from Config Server:
   GET http://localhost:8888/customer-service/default

3. Config Server returns customer-service.properties

4. Service applies configuration and starts
```

### 3. Configuration Access

**Manual Testing**:

```bash
# Get BFF Service config
curl -u admin:config123 http://localhost:8888/bff-service/default

# Get Customer Service config
curl -u admin:config123 http://localhost:8888/customer-service/default

# Get Data-Pipeline Service config
curl -u admin:config123 http://localhost:8888/data-pipeline-service/default
```

**Response Format**:

```json
{
  "name": "customer-service",
  "profiles": ["default"],
  "label": null,
  "propertySources": [{
    "name": "file:./config-repo/customer-service.properties",
    "source": {
      "server.port": "8081",
      "spring.datasource.url": "jdbc:postgresql://localhost:5432/mypolicy_db",
      ...
    }
  }]
}
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Spring Cloud 2022.0.4

## Setup

### 1. Build Config Service

```bash
cd config-service
mvn clean install
```

### 2. Start Config Service (MUST START FIRST!)

```bash
mvn spring-boot:run
```

### 3. Verify Config Server

```bash
# Health check
curl http://localhost:8888/actuator/health

# Should return: {"status":"UP"}

# Test config retrieval
curl -u admin:config123 http://localhost:8888/bff-service/default
```

### 4. Update Client Services

Each service needs these dependencies in `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```

And create `src/main/resources/bootstrap.properties`:

```properties
spring.application.name=customer-service
spring.cloud.config.uri=http://localhost:8888
spring.cloud.config.username=admin
spring.cloud.config.password=config123
```

### 5. Start Services

```bash
# Start in order:
# 1. Config Service (8888) - FIRST!
# 2. All other services fetch config from 8888

cd customer-service && mvn spring-boot:run &
cd policy-service && mvn spring-boot:run &
cd data-pipeline-service && mvn spring-boot:run &
cd bff-service && mvn spring-boot:run &
```

## Environment-Specific Configuration

### Development (default)

```
config-repo/customer-service.properties
```

### Staging

```
config-repo/customer-service-staging.properties
```

### Production

```
config-repo/customer-service-production.properties
```

**Usage**:

```bash
# Start service with staging profile
java -jar customer-service.jar --spring.profiles.active=staging
```

## Dynamic Configuration Updates

### Using /refresh Endpoint

1. Change config in config-repo/
2. Refresh service without restart:

```bash
curl -X POST http://localhost:8081/actuator/refresh
```

3. Service reloads configuration automatically

**Note**: Requires `@RefreshScope` annotation on beans that use config:

```java
@RefreshScope
@RestController
public class MyController {
    @Value("${my.dynamic.property}")
    private String dynamicProperty;
}
```

## Security

### Basic Authentication

Default credentials (for development):

- **Username**: admin
- **Password**: config123

**Production**: Change in `application.properties`:

```properties
spring.security.user.name=${CONFIG_USERNAME}
spring.security.user.password=${CONFIG_PASSWORD}
```

### Encryption (Future Enhancement)

Encrypt sensitive properties:

```properties
spring.datasource.password={cipher}ENCRYPTED_VALUE
```

## Git Backend (Production Setup)

For production, use Git repository:

1. Create config repo on GitHub:

```bash
git clone https://github.com/yourorg/mypolicy-config-repo
cd mypolicy-config-repo
# Add all .properties files
git add .
git commit -m "Initial config"
git push
```

2. Update Config Server:

```properties
# application.properties
spring.cloud.config.server.git.uri=https://github.com/yourorg/mypolicy-config-repo
spring.cloud.config.server.git.default-label=main
spring.cloud.config.server.git.username=your-github-user
spring.cloud.config.server.git.password=your-github-token
```

3. Benefits:

- ✅ Version control for all config changes
- ✅ Audit trail (who changed what)
- ✅ Easy rollback to previous versions
- ✅ Code review for config changes

## Troubleshooting

### Issue: Service can't connect to Config Server

```
Error: Could not resolve placeholder 'server.port'
```

**Solution**: Ensure Config Service is running on port 8888

```bash
curl http://localhost:8888/actuator/health
```

### Issue: 401 Unauthorized

```
Error: 401 Unauthorized when fetching config
```

**Solution**: Check credentials in bootstrap.properties

```properties
spring.cloud.config.username=admin
spring.cloud.config.password=config123
```

### Issue: Config not reloading

```
Changed property value not reflecting
```

**Solution**: Call refresh endpoint

```bash
curl -X POST http://localhost:8081/actuator/refresh
```

## Best Practices

1. **Start Config Server First**: Always start before other services
2. **Use Environment Variables**: For sensitive data (passwords, API keys)
3. **Version Control**: Use Git backend in production
4. **Documentation**: Document all config properties
5. **Testing**: Test config changes in dev before production
6. **Monitoring**: Monitor Config Server health and availability
7. **Backup**: Regular backups of config repository

## Monitoring

### Health Check

```bash
curl http://localhost:8888/actuator/health
```

### Environment

```bash
curl -u admin:config123 http://localhost:8888/actuator/env
```

### Metrics

```bash
curl http://localhost:8888/actuator/metrics
```

## Benefits Realized

| Aspect                 | Before               | After               |
| ---------------------- | -------------------- | ------------------- |
| Config files           | 5 separate locations | 1 central location  |
| Config updates         | Edit 5 files         | Edit 1 file         |
| Service restart        | Required             | Optional (/refresh) |
| Version control        | Manual               | Automatic (Git)     |
| Environment management | Duplicated files     | Profile-based       |
| Sensitive data         | Plain text           | Encrypted           |

## Next Steps

1. **Add to existing services**:
   - Add spring-cloud-starter-config dependency
   - Create bootstrap.properties
   - Test config retrieval

2. **Enable dynamic refresh**:
   - Add @RefreshScope to beans
   - Expose /refresh endpoint

3. **Move to Git backend**:
   - Create config repository
   - Update Config Server URI

4. **Add encryption**:
   - Configure encryption key
   - Encrypt sensitive properties

## Additional Resources

- [Spring Cloud Config Documentation](https://spring.io/projects/spring-cloud-config)
- [CONSOLIDATION_COMPLETE.md](../CONSOLIDATION_COMPLETE.md) - Architecture overview
- [README.md](../README.md) - Main project documentation
