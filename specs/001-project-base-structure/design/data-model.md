# Data Model: Project Base Structure

**Feature ID:** 001-project-base-structure
**Created:** 2026-02-22
**Version:** 1.0.0

---

## Overview

This data model defines the foundational entities for the Live Resume & Career Copilot platform's base structure. It focuses on multi-tenant authentication and core infrastructure entities required for subsequent features.

**Database:** PostgreSQL 18 with pgvector extension
**Migration Tool:** Liquibase with timestamp-based migrations
**Multi-Tenancy Pattern:** Discriminator-based (shared database, tenant_id column)

---

## Entity Relationship Diagram

```
┌──────────────────────┐
│      Tenant          │
│──────────────────────│
│ id (PK)              │
│ name                 │
│ created_at           │
│ settings (JSONB)     │
└──────────────────────┘
         │
         │ 1:N
         ▼
┌──────────────────────┐
│      User            │
│──────────────────────│
│ id (PK)              │
│ tenant_id (FK)       │
│ email                │
│ password_hash        │
│ created_at           │
└──────────────────────┘
```

---

## Entities

### 1. Tenant

**Purpose:** Represents a logical tenant in the multi-tenant system. Each user belongs to exactly one tenant.

**Attributes:**

| Column      | Type          | Constraints           | Description                                      |
|-------------|---------------|-----------------------|--------------------------------------------------|
| id          | BIGSERIAL     | PRIMARY KEY, NOT NULL | Auto-incrementing unique identifier              |
| name        | VARCHAR(255)  | NOT NULL              | Organization or user name                        |
| created_at  | TIMESTAMPTZ   | NOT NULL, DEFAULT NOW()| Timestamp when tenant was created               |
| settings    | JSONB         | NULL                  | Tenant-specific configuration (JSON object)      |

**Indexes:**
- PRIMARY KEY on `id`

**Validation Rules:**
- `name` must not be empty
- `name` max length: 255 characters
- `created_at` defaults to current timestamp if not provided

**Relationships:**
- One-to-many with User (one tenant has many users)

**Notes:**
- `settings` JSONB column allows flexible tenant configuration without schema changes
- Future features will reference tenant_id for data isolation

---

### 2. User

**Purpose:** Represents an authenticated user in the system. Users belong to a tenant and have authentication credentials.

**Attributes:**

| Column         | Type          | Constraints                    | Description                                      |
|----------------|---------------|--------------------------------|--------------------------------------------------|
| id             | BIGSERIAL     | PRIMARY KEY, NOT NULL          | Auto-incrementing unique identifier              |
| tenant_id      | BIGINT        | NOT NULL, FK → Tenant(id)      | Foreign key to tenant                            |
| email          | VARCHAR(255)  | NOT NULL, UNIQUE               | User's email address (used for login)            |
| password_hash  | VARCHAR(255)  | NOT NULL                       | BCrypt hashed password                           |
| created_at     | TIMESTAMPTZ   | NOT NULL, DEFAULT NOW()        | Timestamp when user was created                  |

**Indexes:**
- PRIMARY KEY on `id`
- UNIQUE INDEX on `email`
- INDEX on `tenant_id` (critical for multi-tenant filtering)

**Validation Rules:**
- `email` must be valid email format
- `email` must be unique across entire system
- `password_hash` must be BCrypt hash (60 characters)
- `tenant_id` must reference existing tenant

**Relationships:**
- Many-to-one with Tenant (many users belong to one tenant)

**Notes:**
- Password is NEVER stored in plain text; only BCrypt hash
- Email is used as username for authentication
- `tenant_id` index is critical for multi-tenant query performance

---

## Database Schema (SQL DDL)

### Tenant Table

```sql
CREATE TABLE tenant (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    settings        JSONB
);

-- Indexes
CREATE INDEX idx_tenant_created_at ON tenant(created_at);
```

### User Table

```sql
CREATE TABLE "user" (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES tenant(id)
        ON DELETE CASCADE
);

-- Indexes
CREATE UNIQUE INDEX idx_user_email ON "user"(email);
CREATE INDEX idx_user_tenant_id ON "user"(tenant_id);
CREATE INDEX idx_user_created_at ON "user"(created_at);
```

**Notes:**
- Table name `"user"` is quoted because `user` is a reserved keyword in PostgreSQL
- `ON DELETE CASCADE` ensures user cleanup when tenant is deleted
- UNIQUE constraint on email prevents duplicate accounts

---

## Java Entity Models

### Tenant Entity

```java
package com.interviewme.tenancy.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "tenant")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Type(value = io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "settings", columnDefinition = "jsonb")
    private Map<String, Object> settings;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
```

### User Entity

```java
package com.interviewme.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "\"user\"")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Default role for now; future features will add role management
        return List.of(() -> "ROLE_USER");
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

---

## Liquibase Migration

### Migration File: `20260222000001-initial-tenant-user-tables.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <!-- Tenant Table -->
    <changeSet id="20260222000001-1" author="claude-code">
        <createTable tableName="tenant">
            <column name="id" type="BIGSERIAL" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMPTZ" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="settings" type="JSONB"/>
        </createTable>

        <createIndex indexName="idx_tenant_created_at" tableName="tenant">
            <column name="created_at"/>
        </createIndex>

        <rollback>
            <dropIndex tableName="tenant" indexName="idx_tenant_created_at"/>
            <dropTable tableName="tenant"/>
        </rollback>
    </changeSet>

    <!-- User Table -->
    <changeSet id="20260222000001-2" author="claude-code">
        <createTable tableName="user">
            <column name="id" type="BIGSERIAL" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="email" type="VARCHAR(255)">
                <constraints nullable="false" unique="true"/>
            </column>
            <column name="password_hash" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMPTZ" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!-- Foreign Key -->
        <addForeignKeyConstraint
            baseTableName="user"
            baseColumnNames="tenant_id"
            constraintName="fk_user_tenant"
            referencedTableName="tenant"
            referencedColumnNames="id"
            onDelete="CASCADE"/>

        <!-- Indexes -->
        <createIndex indexName="idx_user_email" tableName="user" unique="true">
            <column name="email"/>
        </createIndex>

        <createIndex indexName="idx_user_tenant_id" tableName="user">
            <column name="tenant_id"/>
        </createIndex>

        <createIndex indexName="idx_user_created_at" tableName="user">
            <column name="created_at"/>
        </createIndex>

        <rollback>
            <dropIndex tableName="user" indexName="idx_user_created_at"/>
            <dropIndex tableName="user" indexName="idx_user_tenant_id"/>
            <dropIndex tableName="user" indexName="idx_user_email"/>
            <dropForeignKeyConstraint baseTableName="user" constraintName="fk_user_tenant"/>
            <dropTable tableName="user"/>
        </rollback>
    </changeSet>

    <!-- Enable pgvector extension -->
    <changeSet id="20260222000001-3" author="claude-code">
        <sql>CREATE EXTENSION IF NOT EXISTS vector;</sql>
        <rollback>
            <sql>DROP EXTENSION IF EXISTS vector;</sql>
        </rollback>
    </changeSet>

</databaseChangeLog>
```

### Master Changelog: `db.changelog-master.yaml`

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/20260222000001-initial-tenant-user-tables.xml
```

---

## Data Transfer Objects (DTOs)

### Tenant DTOs

```java
// TenantResponse.java
package com.interviewme.tenancy.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class TenantResponse {
    private Long id;
    private String name;
    private OffsetDateTime createdAt;
    private Map<String, Object> settings;
}
```

### User/Auth DTOs

```java
// RegisterRequest.java
package com.interviewme.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Tenant name is required")
    private String tenantName;
}

// LoginRequest.java
package com.interviewme.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}

// AuthResponse.java
package com.interviewme.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private Long tenantId;
}
```

---

## Repositories

### Tenant Repository

```java
package com.interviewme.tenancy.repository;

import com.interviewme.tenancy.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByName(String name);
}
```

### User Repository

```java
package com.interviewme.auth.repository;

import com.interviewme.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

---

## Multi-Tenancy Considerations

### Tenant-Aware Entities

**Base Abstract Class for Future Entities:**

```java
package com.interviewme.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Data
public abstract class TenantAwareEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}
```

**Usage in Future Entities:**

```java
@Entity
@Table(name = "profile")
public class Profile extends TenantAwareEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String headline;
    private String summary;
    // ... other fields
}
```

---

## Validation & Business Rules

### User Registration
1. Email must be unique across entire system
2. Password must be at least 8 characters
3. Password is hashed with BCrypt before storage
4. Tenant is created automatically for new user registration
5. User is associated with newly created tenant

### Authentication
1. User authenticates with email and password
2. JWT token contains user ID, email, and tenant ID claims
3. Token expires after 24 hours (configurable)
4. Tenant context is resolved from JWT for all authenticated requests

---

## Security Considerations

1. **Password Storage:** NEVER store plain text passwords; always use BCrypt
2. **Tenant Isolation:** All queries MUST filter by tenant_id (automated via Hibernate filters)
3. **Email Uniqueness:** Prevents duplicate accounts
4. **Cascading Deletes:** User deletion cascades from tenant deletion
5. **Index on tenant_id:** Critical for query performance in multi-tenant system

---

## Performance Considerations

1. **Indexes:** All foreign keys and frequently queried columns are indexed
2. **JSONB for Settings:** Allows flexible configuration without schema changes
3. **TIMESTAMPTZ:** Timezone-aware timestamps for global applications
4. **Connection Pooling:** HikariCP configured for optimal performance (see research.md)

---

## Future Extensions

The following entities will be added in subsequent features:
- Profile (user profile information)
- JobExperience (employment history)
- Education (educational background)
- Skill / UserSkill (skills catalog and user skills)
- ExperienceProject (projects under jobs)
- Story (STAR-format case studies)
- Package (shareable content packages)
- ExportHistory (generated exports)
- ChatSession / ChatMessage (recruiter chat)
- CoinWallet / CoinTransaction (billing)
- LinkedInDraft / LinkedInAnalysis (LinkedIn features)

All future entities will extend `TenantAwareEntity` or include `tenant_id` for proper multi-tenant isolation.

---

**Data Model Version:** 1.0.0
**Last Updated:** 2026-02-22
**Valid For:** Feature 001 - Project Base Structure
