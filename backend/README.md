# Backend Module

Spring Boot backend for Live Resume & Career Copilot platform.

## Technology Stack

- **Java 21** with virtual threads enabled
- **Spring Boot 3.4.x**
- **Spring Security 6** with JWT authentication
- **Spring Data JPA** with Hibernate
- **PostgreSQL 18** with pgvector extension
- **Liquibase** for database migrations
- **Lombok** for boilerplate reduction

## Module Structure

```
backend/
├── src/main/java/com/interviewme/
│   ├── config/          # Spring configuration classes
│   ├── controller/      # REST controllers
│   ├── model/           # JPA entities
│   ├── repository/      # Spring Data repositories
│   ├── security/        # JWT and tenant security
│   └── service/         # Business logic
├── src/main/resources/
│   ├── db/changelog/    # Liquibase migrations
│   ├── static/          # Frontend build output (auto-generated)
│   └── application.properties
└── src/test/            # Integration tests
```

## Key Features

### JWT Authentication
- Token-based stateless authentication
- BCrypt password hashing (strength 12)
- 24-hour token expiration (configurable)

### Multi-Tenancy
- Discriminator-based (tenant_id column)
- Automatic filtering via Hibernate filters
- AOP-based filter activation
- ThreadLocal tenant context

### Database
- PostgreSQL 18 with JSONB support
- Liquibase timestamp-based migrations
- HikariCP connection pooling
- Virtual threads for scalability

## Building

```bash
# Build backend only
./gradlew backend:build

# Build including frontend
./gradlew build

# Run backend
./gradlew backend:bootRun

# Run tests
./gradlew backend:test
```

## Configuration

Main configuration in `application.properties`:

```properties
# Database
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION_MS}

# Virtual Threads
spring.threads.virtual.enabled=true
```

## Dependencies

- Common module (shared DTOs)
- Spring Boot starters (web, data-jpa, security, actuator)
- PostgreSQL driver
- Liquibase
- JJWT (JWT library)
- Hypersistence Utils (JSONB support)
- Lombok

See `build.gradle.kts` for full dependency list.
