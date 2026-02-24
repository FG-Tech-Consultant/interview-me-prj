# Implementation Plan: Skills Management - Catalog and User Skills CRUD

**Feature ID:** 003-skills-management
**Status:** Design Complete
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Executive Summary

This plan outlines the implementation of a comprehensive skills management system with two layers: a canonical **Skills Catalog** (shared across all tenants) and **User Skills** (tenant-isolated skill claims with rich metadata). The implementation follows a bottom-up approach: database schema → backend services → REST APIs → frontend components.

The design emphasizes simplicity through standard Spring Data JPA repositories, conventional three-layer architecture (Controller → Service → Repository), and React Query for frontend state management. Skills catalog provides standardization (preventing "React" vs "React.js" fragmentation) while user skills enable personalization with proficiency levels (1-5), years of experience, and custom tags.

Key technical decisions include JSONB for flexible tag storage, soft delete for audit trails, optimistic locking for concurrent updates, and indexed autocomplete for sub-100ms search performance. The implementation integrates seamlessly with existing Profile CRUD (Feature 002) and lays groundwork for future recruiter chat context retrieval and resume generation.

**Key Deliverables:**
- PostgreSQL schema with Liquibase migrations: `skill` (catalog) and `user_skill` (tenant-isolated) tables
- Backend REST APIs: 10 endpoints covering catalog admin CRUD, user skills CRUD, and autocomplete search
- Frontend React components: Skills page with category grouping, add/edit dialogs, filters, and admin catalog management
- Integration with existing tenancy and authentication infrastructure

**Timeline:** 4-5 days
**Complexity:** Medium

---

## Constitution Check

### Applicable Principles Validation

**✅ Principle 1: Simplicity First**
- **Alignment:** Standard REST CRUD with Spring Data JPA, conventional three-layer architecture, no complex abstractions
- **Evidence:** SkillRepository and UserSkillRepository use built-in query methods, SkillService < 300 lines, SkillController handles HTTP layer only
- **Gate:** PASSED

**✅ Principle 3: Modern Java Standards**
- **Alignment:** Java 25 records for DTOs, Lombok for entity boilerplate, Spring Boot 4.x conventions
- **Evidence:** SkillDto and UserSkillDto as records, @Slf4j and @RequiredArgsConstructor for services, modern JPA @ManyToOne mappings
- **Gate:** PASSED

**✅ Principle 4: Data Sovereignty and Multi-Tenant Isolation**
- **Alignment:** UserSkill table includes tenant_id with automatic filtering, Skills catalog shared (no tenant_id), PostgreSQL with JSONB for tags
- **Evidence:** UserSkill entity has tenant_id FK, tenant filter applied via existing Hibernate filter from Feature 001, skill table has no tenant_id (shared globally)
- **Gate:** PASSED

**✅ Principle 6: Observability and Debugging**
- **Alignment:** Structured logging for all CRUD operations with tenant context, admin actions audit logged
- **Evidence:** @Slf4j on services, log statements include tenantId and userId, admin catalog changes logged with admin username
- **Gate:** PASSED

**✅ Principle 7: Security, Privacy, and Credential Management**
- **Alignment:** Public/private visibility at skill level, @Transactional annotations, JWT authentication required, admin endpoints RBAC protected
- **Evidence:** UserSkill.visibility field filtered in service layer, @Transactional(readOnly=true) for queries, @Transactional for mutations, @PreAuthorize("hasRole('ADMIN')") on catalog endpoints
- **Gate:** PASSED

**✅ Principle 8: Multi-Tenant Architecture**
- **Alignment:** UserSkill entities include tenant_id, automatic filtering via tenant filter, catalog shared across tenants
- **Evidence:** user_skill table has tenant_id FK NOT NULL, unique constraint (tenant_id, profile_id, skill_id), tenant filter from Feature 001 applied automatically
- **Gate:** PASSED

**✅ Principle 10: Full-Stack Modularity and Separation of Concerns**
- **Alignment:** Backend: com.interviewme.skills package with controller/service/repository layers, Frontend: pages/SkillsPage.tsx, components/SkillSelector, api/skillsApi.ts
- **Evidence:** SkillController (< 200 lines), SkillService (< 300 lines), SkillsPage component (< 300 lines), clear separation between API client, hooks, and UI components
- **Gate:** PASSED

**✅ Principle 11: Database Schema Evolution**
- **Alignment:** Liquibase timestamp-based migrations for skill and user_skill tables with rollback support
- **Evidence:** Migration files: 20260224143000-create-skill-table.xml and 20260224143500-create-user-skill-table.xml, includes <rollback> tags
- **Gate:** PASSED

### Overall Constitution Compliance: ✅ PASSED

All applicable constitutional principles are satisfied. No exceptions or waivers required.

---

## Technical Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  User's Browser (React SPA)                  │
│              TypeScript, React Query, MUI                    │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/REST API (JSON)
                        │
┌───────────────────────▼─────────────────────────────────────┐
│               Spring Boot Backend (Java 25)                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  REST Controllers (/api/v1/skills/*)                 │  │
│  │  - SkillController: catalog autocomplete, admin CRUD │  │
│  │  - UserSkillController: user skills CRUD             │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Business Logic Services                              │  │
│  │  - SkillService: catalog management, autocomplete    │  │
│  │  - UserSkillService: user skill CRUD, filtering      │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Data Access Layer (Spring Data JPA)                 │  │
│  │  - SkillRepository: catalog queries                  │  │
│  │  - UserSkillRepository: tenant-filtered queries      │  │
│  └────────────────────┬─────────────────────────────────┘  │
└───────────────────────┼─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              PostgreSQL 18 Database                          │
│  skill table (shared catalog) + user_skill (tenant-isolated)│
└─────────────────────────────────────────────────────────────┘
```

### Request Flow: User Adds Skill to Profile

```
1. User searches "Java" in autocomplete field
   ↓
2. Frontend (SkillSelector.tsx) debounces input (300ms), calls skillsApi.searchCatalog("Java")
   ↓
3. GET /api/v1/skills/catalog/search?q=Java
   ↓
4. SkillController.searchCatalog(query) → SkillService.searchActive(query)
   ↓
5. SkillRepository.findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName("Java")
   ↓
6. Returns: ["Java", "JavaScript", "Java EE", "Java 17+"]
   ↓
7. User selects "Java", fills proficiency=5, years=12, last_used=2026-01
   ↓
8. Frontend calls skillsApi.addUserSkill({skillId, proficiency, years, lastUsed, visibility: "public"})
   ↓
9. POST /api/v1/skills/user with JSON body
   ↓
10. UserSkillController.addSkill(dto) → UserSkillService.addSkill(profileId, dto)
   ↓
11. Validate: proficiency 1-5, lastUsed <= today, skill exists in catalog, no duplicate
   ↓
12. Create UserSkill entity with tenant_id from JWT, save via UserSkillRepository
   ↓
13. Return UserSkillDto with id, skill name, proficiency, years, visibility
   ↓
14. Frontend updates UI, displays skill card in "Languages" category group
```

---

## Technology Stack

### Core Framework
- **Spring Boot**: 4.x (latest stable)
- **Java**: 25 LTS (required)
- **Build Tool**: Gradle 8.5+ multi-module structure (backend module)

### New Dependencies

```kotlin
dependencies {
    // Already in project (Feature 001-002)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql")

    // JSON type handling (if not already added)
    implementation("io.hypersistence:hypersistence-utils-hibernate-62:3.5.0") // JSONB type support

    // Liquibase (already in project from Feature 001)
    implementation("org.liquibase:liquibase-core:4.25.0+")

    // Lombok (already in project)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
```

### Frontend Technologies

**New React Components:**
- `SkillsPage.tsx`: Main skills management page with category grouping (< 300 lines)
- `SkillSelector.tsx`: Autocomplete component for skill catalog search (< 200 lines)
- `SkillFormDialog.tsx`: Add/edit skill dialog with validation (< 250 lines)
- `SkillCard.tsx`: Individual skill display with proficiency stars (< 100 lines)
- `SkillFilters.tsx`: Filter controls for category, proficiency, date range (< 150 lines)
- `AdminSkillsCatalogPage.tsx`: Admin page for managing catalog (< 300 lines)

**New API Client Modules:**
- `api/skillsApi.ts`: API client for skills endpoints (< 200 lines)

**New Custom Hooks:**
- `hooks/useSkills.ts`: React Query hooks for skills data (< 150 lines)
- `hooks/useSkillCatalog.ts`: React Query hooks for catalog autocomplete (< 100 lines)

### External Services/Tools

No new external services required. Uses existing PostgreSQL database from Feature 001.

---

## Service Decomposition

### Backend Services (Java)

**Business Logic Services:**

- `SkillService`: Manages skills catalog (shared across tenants)
  - Line count estimate: 250-300 lines
  - Dependencies: SkillRepository
  - Public methods:
    - `searchActive(String query)`: Autocomplete search (top 10 matches)
    - `createSkill(CreateSkillDto dto)`: Admin creates catalog skill
    - `updateSkill(Long id, UpdateSkillDto dto)`: Admin updates catalog skill
    - `deactivateSkill(Long id)`: Admin deactivates skill (soft)
    - `reactivateSkill(Long id)`: Admin reactivates skill
    - `findById(Long id)`: Get skill by ID
    - `findAll()`: List all catalog skills (admin only)

- `UserSkillService`: Manages user skill claims (tenant-isolated)
  - Line count estimate: 350-400 lines
  - Dependencies: UserSkillRepository, SkillRepository, ProfileRepository, TenantContext
  - Public methods:
    - `addSkill(Long profileId, AddUserSkillDto dto)`: User adds skill
    - `updateSkill(Long userSkillId, UpdateUserSkillDto dto)`: User updates skill metadata
    - `deleteSkill(Long userSkillId)`: User soft-deletes skill
    - `getSkillsByProfile(Long profileId)`: Get all user skills grouped by category
    - `getSkillById(Long userSkillId)`: Get single user skill
    - `filterSkills(SkillFilterDto filters)`: Filter skills by category, proficiency, date
    - `getPublicSkillsByProfile(Long profileId)`: Get only public skills (for recruiter chat context)

### Frontend Modules (React)

**New Modules:**

- `pages/SkillsPage.tsx`: Skills management page
  - Line count estimate: 280 lines
  - Exports: SkillsPage (default)
  - Dependencies: useSkills, SkillCard, SkillFormDialog, SkillFilters

- `components/SkillSelector.tsx`: Autocomplete skill search
  - Line count estimate: 180 lines
  - Exports: SkillSelector (default)
  - Dependencies: useSkillCatalog, MUI Autocomplete

- `api/skillsApi.ts`: Skills API client
  - Line count estimate: 160 lines
  - Exports: skillsApi object
  - Dependencies: axios, authUtils

---

## Implementation Phases

### Phase 1: Database Foundation (Day 1)

**Tasks:**
1. Create Liquibase migration for `skill` table (catalog)
2. Create Liquibase migration for `user_skill` table (tenant-isolated)
3. Add migrations to db.changelog-master.yaml
4. Create JPA entities: Skill, UserSkill with proper annotations (@Entity, @Table, @ManyToOne, @JsonIgnore for circular refs)
5. Test migrations on fresh PostgreSQL database

**Deliverables:**
- `20260224143000-create-skill-table.xml`
- `20260224143500-create-user-skill-table.xml`
- `Skill.java` entity with JSONB tags field
- `UserSkill.java` entity with tenant_id, proficiency validation

**Validation:**
- [ ] Migrations run successfully on fresh database
- [ ] skill table has unique index on name
- [ ] user_skill table has unique constraint (tenant_id, profile_id, skill_id) WHERE deleted_at IS NULL
- [ ] Check constraints enforce proficiency 1-5, years >= 0

---

### Phase 2: Backend Core (Day 2)

**Tasks:**
1. Create SkillRepository with autocomplete query methods
2. Create UserSkillRepository with tenant-filtered query methods
3. Implement SkillService with catalog CRUD and search
4. Implement UserSkillService with user skill CRUD and filtering
5. Add DTOs: SkillDto, CreateSkillDto, UserSkillDto, AddUserSkillDto, UpdateUserSkillDto, SkillFilterDto (as Java records)
6. Add validation annotations (@NotNull, @Min, @Max, @Size, @Past) to DTOs

**Deliverables:**
- `SkillRepository.java` with findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName method
- `UserSkillRepository.java` with findByProfileIdAndDeletedAtIsNull, tenant filter applied
- `SkillService.java` with catalog management logic
- `UserSkillService.java` with user skill logic and public/private filtering
- DTO records with validation

**Validation:**
- [ ] Autocomplete returns top 10 matches in < 100ms (tested with 1000+ catalog entries)
- [ ] Duplicate skill prevention enforced (unique constraint violation)
- [ ] Tenant filter prevents cross-tenant access
- [ ] Public/private filtering works correctly

---

### Phase 3: REST API Layer (Day 3)

**Tasks:**
1. Implement SkillController with endpoints: GET /catalog/search, POST /catalog (admin), PUT /catalog/{id} (admin), POST /catalog/{id}/deactivate (admin)
2. Implement UserSkillController with endpoints: POST /user, GET /user, PUT /user/{id}, DELETE /user/{id}, GET /user/filter
3. Add @PreAuthorize("hasRole('ADMIN')") to admin endpoints
4. Add request validation with @Valid
5. Add exception handlers for 404 Not Found, 409 Conflict (duplicate), 400 Bad Request (validation)
6. Add @Transactional annotations (readOnly=true for GET, default for mutations)
7. Write integration tests for all endpoints

**Deliverables:**
- `SkillController.java` with 5 endpoints
- `UserSkillController.java` with 5 endpoints
- Exception handling in GlobalExceptionHandler
- 20+ integration tests covering CRUD, validation, tenant isolation

**Validation:**
- [ ] All endpoints return correct HTTP status codes
- [ ] Validation errors return 400 with field-specific messages
- [ ] Admin endpoints return 403 for non-admin users
- [ ] Tenant isolation prevents cross-tenant access (returns 404)
- [ ] Optimistic locking prevents concurrent update conflicts (returns 409)

---

### Phase 4: Frontend Implementation (Day 4-5)

**Tasks:**
1. Create skillsApi.ts with API client methods
2. Create useSkills and useSkillCatalog React Query hooks
3. Implement SkillsPage with category grouping and empty state
4. Implement SkillSelector autocomplete component with 300ms debounce
5. Implement SkillFormDialog with MUI form fields and validation
6. Implement SkillCard with proficiency stars and visibility badge
7. Implement SkillFilters with category, proficiency, date range controls
8. Implement AdminSkillsCatalogPage for admin users
9. Add routing: /skills (user page), /admin/skills-catalog (admin page)
10. Add navigation menu items for skills pages

**Deliverables:**
- Complete React components for user and admin flows
- React Query integration for caching and optimistic updates
- Responsive UI with MUI components
- Inline validation with error messages

**Validation:**
- [ ] Autocomplete suggests skills as user types (debounced)
- [ ] Skills grouped by category with collapsible sections
- [ ] Add/edit/delete operations work with instant UI updates
- [ ] Filters correctly narrow skill list
- [ ] Admin catalog page allows CRUD operations
- [ ] Empty state shows helpful message

---

## Data Model Implementation

### Database Schema

```sql
-- skill table - Canonical skills catalog (shared across tenants)
CREATE TABLE skill (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    tags JSONB,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE INDEX idx_skill_name ON skill(name);
CREATE INDEX idx_skill_category ON skill(category);
CREATE INDEX idx_skill_is_active ON skill(is_active);

-- user_skill table - Tenant-isolated user skill claims
CREATE TABLE user_skill (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    profile_id BIGINT NOT NULL REFERENCES profile(id),
    skill_id BIGINT NOT NULL REFERENCES skill(id),
    years_of_experience INT DEFAULT 0 CHECK (years_of_experience >= 0),
    proficiency_depth INT NOT NULL CHECK (proficiency_depth BETWEEN 1 AND 5),
    last_used_date DATE,
    confidence_level VARCHAR(20) DEFAULT 'MEDIUM',
    tags JSONB,
    visibility VARCHAR(20) DEFAULT 'private',
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_user_skill_tenant_id ON user_skill(tenant_id);
CREATE INDEX idx_user_skill_profile_id ON user_skill(profile_id);
CREATE INDEX idx_user_skill_skill_id ON user_skill(skill_id);
CREATE INDEX idx_user_skill_deleted_at ON user_skill(deleted_at);

CREATE UNIQUE INDEX idx_user_skill_unique ON user_skill(tenant_id, profile_id, skill_id)
    WHERE deleted_at IS NULL;
```

### Java Entities

```java
// Skill.java - Canonical skills catalog entity
@Entity
@Table(name = "skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

// UserSkill.java - Tenant-isolated user skill claims entity
@Entity
@Table(name = "user_skill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "years_of_experience", nullable = false)
    private Integer yearsOfExperience = 0;

    @Column(name = "proficiency_depth", nullable = false)
    private Integer proficiencyDepth;

    @Column(name = "last_used_date")
    private LocalDate lastUsedDate;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel = "MEDIUM";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(nullable = false, length = 20)
    private String visibility = "private";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### DTOs (Data Transfer Objects)

```java
// SkillDto.java - Catalog skill response
public record SkillDto(
    Long id,
    String name,
    String category,
    String description,
    List<String> tags,
    Boolean isActive
) {}

// CreateSkillDto.java - Admin creates catalog skill
public record CreateSkillDto(
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 100) String category,
    @Size(max = 2000) String description,
    @Size(max = 20) List<String> tags
) {}

// UserSkillDto.java - User skill response
public record UserSkillDto(
    Long id,
    SkillDto skill,
    Integer yearsOfExperience,
    Integer proficiencyDepth,
    LocalDate lastUsedDate,
    String confidenceLevel,
    List<String> tags,
    String visibility,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

// AddUserSkillDto.java - User adds skill
public record AddUserSkillDto(
    @NotNull Long skillId,
    @Min(0) @Max(70) Integer yearsOfExperience,
    @NotNull @Min(1) @Max(5) Integer proficiencyDepth,
    @Past LocalDate lastUsedDate,
    @NotNull String confidenceLevel,
    @Size(max = 20) List<String> tags,
    @NotNull String visibility
) {}

// UpdateUserSkillDto.java - User updates skill
public record UpdateUserSkillDto(
    @Min(0) @Max(70) Integer yearsOfExperience,
    @Min(1) @Max(5) Integer proficiencyDepth,
    @Past LocalDate lastUsedDate,
    String confidenceLevel,
    @Size(max = 20) List<String> tags,
    String visibility
) {}

// SkillFilterDto.java - Filter criteria
public record SkillFilterDto(
    String category,
    Integer minProficiency,
    Integer maxProficiency,
    LocalDate lastUsedBefore,
    LocalDate lastUsedAfter,
    String visibility
) {}
```

---

## API Implementation

### New Endpoints

| Method | Path | Description | Auth Required | Request Body | Response |
|--------|------|-------------|---------------|--------------|----------|
| GET | `/api/v1/skills/catalog/search?q={query}` | Autocomplete search catalog | Yes | - | List<SkillDto> |
| GET | `/api/v1/skills/catalog` | List all catalog skills (admin) | Yes (ADMIN) | - | List<SkillDto> |
| POST | `/api/v1/skills/catalog` | Create catalog skill (admin) | Yes (ADMIN) | CreateSkillDto | SkillDto |
| PUT | `/api/v1/skills/catalog/{id}` | Update catalog skill (admin) | Yes (ADMIN) | UpdateSkillDto | SkillDto |
| POST | `/api/v1/skills/catalog/{id}/deactivate` | Deactivate skill (admin) | Yes (ADMIN) | - | SkillDto |
| POST | `/api/v1/skills/catalog/{id}/reactivate` | Reactivate skill (admin) | Yes (ADMIN) | - | SkillDto |
| GET | `/api/v1/skills/user` | Get user skills (grouped by category) | Yes | - | Map<String, List<UserSkillDto>> |
| POST | `/api/v1/skills/user` | Add user skill | Yes | AddUserSkillDto | UserSkillDto |
| PUT | `/api/v1/skills/user/{id}` | Update user skill | Yes | UpdateUserSkillDto | UserSkillDto |
| DELETE | `/api/v1/skills/user/{id}` | Delete user skill (soft) | Yes | - | 204 No Content |
| GET | `/api/v1/skills/user/filter` | Filter user skills | Yes | SkillFilterDto (query params) | List<UserSkillDto> |

---

## Security Implementation

### Authentication/Authorization Changes

- All endpoints require JWT authentication (existing from Feature 001)
- Admin catalog endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`
- Tenant context extracted from JWT and applied automatically to UserSkill queries

### Data Protection Measures

- **Encryption:** Tenant ID and profile ID never exposed in public APIs (internal IDs only)
- **Logging Redaction:** User tags and skill names logged but not sensitive (no PII)
- **Session Security:** Existing JWT session management (no changes)

### Security Testing

- [ ] Non-admin users receive 403 when accessing admin endpoints
- [ ] Tenant isolation prevents cross-tenant skill access (returns 404)
- [ ] Soft delete prevents deleted skills from appearing in any API response
- [ ] Public/private visibility flag correctly filters skills in public APIs

---

## Performance Targets

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Autocomplete search latency | p95 < 100ms | Load test with 1000+ catalog skills, measure GET /catalog/search |
| User skills list latency | p95 < 200ms | Load test with 100 skills per profile, measure GET /user |
| Add user skill latency | p95 < 300ms | Load test with concurrent POST /user requests |
| Database query efficiency | < 10ms per query | Monitor JPA query execution time with SQL logging |
| Concurrent requests | 20 concurrent/tenant | Load test with JMeter, verify no degradation |

---

## Testing Strategy

### Test Pyramid

```
                 E2E Tests (5%)
                /              \
            Integration Tests (35%)
           /                        \
      Unit Tests (60%)
```

### Test Coverage Breakdown

**Unit Tests:**
- SkillService: searchActive, createSkill, deactivateSkill
- UserSkillService: addSkill (duplicate prevention), updateSkill (validation), deleteSkill (soft delete), filterSkills
- DTOs: validation annotations (@Min, @Max, @NotNull, @Past)

**Integration Tests:**
- SkillController: all endpoints with mock authentication
- UserSkillController: all endpoints with tenant context
- Repository queries: autocomplete performance, tenant filtering
- Unique constraint violation (duplicate skill)
- Optimistic locking (concurrent update conflict)

**E2E Tests (Optional):**
- User journey: Add 5 skills → Edit proficiency → Filter by category → Delete obsolete skill

**Target Coverage:** 85%

---

## Deployment Strategy

### Local Development

```bash
# Run PostgreSQL in Docker
docker run -d -p 5432:5432 -e POSTGRES_DB=interviewme -e POSTGRES_USER=dev -e POSTGRES_PASSWORD=dev postgres:18

# Run backend (auto-runs Liquibase migrations)
cd backend
./gradlew bootRun

# Verify skills tables created
psql -h localhost -U dev -d interviewme -c "\d skill"
psql -h localhost -U dev -d interviewme -c "\d user_skill"
```

### Docker Deployment

No changes to existing Docker deployment (Feature 001 already containerizes backend + PostgreSQL).

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DATABASE_URL | jdbc:postgresql://localhost:5432/interviewme | PostgreSQL connection URL |
| DATABASE_USERNAME | dev | PostgreSQL username |
| DATABASE_PASSWORD | dev | PostgreSQL password |

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Autocomplete performance degradation with large catalog (10,000+ skills) | Medium | Medium | Index on skill.name, limit results to top 10, consider full-text search later |
| User adds 100+ skills, UI performance suffers | Low | Low | Frontend pagination, lazy load category sections |
| JSONB tags query performance issues | Low | Low | Use GIN indexes on tags column if needed (deferred) |
| Skill catalog synonyms ("React" vs "React.js") cause confusion | Medium | Low | Admin deduplication workflow (future feature) |
| Concurrent admin catalog updates create duplicate skills | Low | Medium | Optimistic locking or unique constraint handles this |

---

## Success Criteria Verification

### From Specification

1. **Complete Skill Lifecycle**
   - **Test**: Integration tests cover all CRUD operations
   - **Target**: 100% success rate for add, update, view, delete
   - **Status**: TBD

2. **Skills Catalog Autocomplete Performance**
   - **Test**: Load test with 1000+ catalog skills
   - **Target**: p95 latency < 100ms
   - **Status**: TBD

3. **Duplicate Skill Prevention**
   - **Test**: Attempt to add same skill twice
   - **Target**: 409 Conflict error with clear message
   - **Status**: TBD

4. **Public/Private Visibility Enforced**
   - **Test**: Create user with mixed public/private skills, call public API
   - **Target**: Private skills excluded from response
   - **Status**: TBD

5. **Tenant Isolation Verified**
   - **Test**: Create two tenants with overlapping skills, cross-tenant access
   - **Target**: 404 Not Found (not 403 to avoid leaking tenant info)
   - **Status**: TBD

6. **Skill Grouping by Category**
   - **Test**: Manual UAT with 20+ skills across 5 categories
   - **Target**: Skills correctly grouped by category in UI
   - **Status**: TBD

7. **Admin Catalog Management**
   - **Test**: Admin adds "Deno" to catalog, user sees it in autocomplete
   - **Target**: Skill available within 1 second (cache invalidation)
   - **Status**: TBD

---

## Migration Path (Future Features)

### To Feature 004: Experience Projects and STAR Stories

1. Add `story_skill` join table to link stories to skills
2. Update UserSkillService to include story counts per skill
3. Update frontend to show "X stories using this skill"

**Backward Compatibility:**
- Existing user skills remain functional without stories
- Skills can exist without linked stories

### To Feature 006: Recruiter Chat RAG Context

1. Extend UserSkillService.getPublicSkillsByProfile to return skills with embeddings
2. Skills contribute to RAG context: "Candidate has 12 years Java experience, proficiency level 5/5, last used Jan 2026"

**Backward Compatibility:**
- Public skills immediately available for chat context (no migration needed)

---

## Open Questions / Deferred Decisions

**Resolved During Planning:**
- [x] **Skill categories fixed or extensible?** → Fixed initially (Languages, Frameworks, Cloud, etc.) but stored as VARCHAR for future extensibility
- [x] **Proficiency scale 1-5 or 1-10?** → 1-5 (simpler, sufficient for meaningful differentiation)
- [x] **Tags free-form or validated?** → Free-form JSONB array (no validation initially, admin can curate later)
- [x] **Skill catalog tenant-isolated or shared?** → Shared across tenants (reduces duplication, easier admin)

**Deferred to Future Features:**
- [ ] **Skill synonyms/aliases handling** → Defer to Feature 008 (AI-powered skill normalization)
- [ ] **Skill endorsements by others** → Defer to Feature 012 (Social validation)
- [ ] **Import skills from LinkedIn** → Defer to Feature 010 (LinkedIn integration)

---

## References

- [Feature Specification](./spec.md)
- [Data Model](./data-model.md) (to be created in next phase)
- [Project Constitution](../../.specify/memory/constitution.md)
- [Project Overview](../../.specify/memory/project-overview.md)

---

## Sign-Off

**Planning Complete:** ✅ Yes
**Constitution Validated:** ✅ All principles satisfied
**Ready for Implementation:** ✅ Yes

**Recommended Next Command:** `/speckit.tasks` to generate actionable task breakdown

---

**Plan Version:** 1.0.0
**Last Updated:** 2026-02-24
**Estimated Implementation Time:** 4-5 days
