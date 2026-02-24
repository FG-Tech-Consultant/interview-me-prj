# Implementation Plan: Experience Projects & Stories (STAR Format)

**Feature ID:** 004-experience-stories
**Status:** Design Complete
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Executive Summary

This plan defines the technical design for the Experience Projects & Stories feature, which adds structured project documentation and STAR-format behavioral stories to the career platform. The implementation follows the same three-layer architecture (Controller -> Service -> Repository) established in Features 002 and 003.

The design introduces four database tables: `experience_project` (project metadata under jobs), `story` (STAR narratives), `experience_project_skill` (project-skill links), and `story_skill` (story-skill links). All entities are tenant-isolated with automatic Hibernate filtering, support soft delete, and use JSONB for flexible metrics/tech stack storage.

The backend lives in the `com.interviewme.experience` package, separate from the profile and skills modules, following the modular package structure defined in the constitution. The frontend adds project and story components nested under existing job experience cards.

**Key Deliverables:**
- Database: 4 Liquibase migrations (experience_project, story, experience_project_skill, story_skill tables)
- Backend: 4 JPA entities, 4 repositories, 2 services (ExperienceProjectService, StoryService), 2 REST controllers, 10+ DTOs (Java records)
- Frontend: Project and story components (forms, cards, lists), API clients, React Query hooks
- Testing: Unit tests for services, integration tests for REST endpoints

**Timeline:** 5-6 days
**Complexity:** Medium-High (more entities and relationships than previous features)

---

## Constitution Check

### Applicable Principles Validation

**Principle 1: Simplicity First**
- **Alignment:** Standard REST CRUD, three-layer architecture, no complex abstractions
- **Evidence:** ExperienceProjectService and StoryService follow same patterns as ProfileService and UserSkillService
- **Gate:** PASSED

**Principle 3: Modern Java Standards**
- **Alignment:** Java 25 records for DTOs, Lombok for entities, Spring Boot 4.x conventions
- **Evidence:** All DTOs as records with Jakarta Validation annotations, @Slf4j and @RequiredArgsConstructor
- **Gate:** PASSED

**Principle 4: Data Sovereignty and Multi-Tenant Isolation**
- **Alignment:** All 4 tables include tenant_id with automatic filtering, JSONB for flexible data
- **Evidence:** experience_project.tenant_id FK, story.tenant_id FK, join tables include tenant_id
- **Gate:** PASSED

**Principle 6: Observability and Debugging**
- **Alignment:** Structured logging for all CRUD operations with tenant context
- **Evidence:** @Slf4j on services, log statements include tenantId, projectId, storyId
- **Gate:** PASSED

**Principle 7: Security, Privacy, and Credential Management**
- **Alignment:** Public/private visibility, @Transactional annotations, JWT authentication
- **Evidence:** Story.visibility field, @Transactional(readOnly=true) for reads, @Transactional for writes
- **Gate:** PASSED

**Principle 8: Multi-Tenant Architecture**
- **Alignment:** All entities include tenant_id, filtered automatically
- **Evidence:** Hibernate filter applied to all queries, join tables tenant-isolated
- **Gate:** PASSED

**Principle 10: Full-Stack Modularity**
- **Alignment:** Backend: com.interviewme.experience package, Frontend: components/experience/
- **Evidence:** ExperienceProjectController < 200 lines, StoryController < 200 lines, services < 400 lines
- **Gate:** PASSED

**Principle 11: Database Schema Evolution**
- **Alignment:** Liquibase timestamp-based migrations with rollback
- **Evidence:** 4 migration files with <rollback> tags
- **Gate:** PASSED

### Overall Constitution Compliance: PASSED

---

## Technical Architecture

### Component Diagram

```
+---------------------------------------------------------+
|              User's Browser (React SPA)                   |
|           TypeScript, React Query, MUI                    |
+---------------------------+-----------------------------+
                            | HTTP/REST API (JSON)
                            |
+---------------------------v-----------------------------+
|            Spring Boot Backend (Java 25)                  |
|  +----------------------------------------------------+ |
|  |  REST Controllers (/api/v1/*)                       | |
|  |  - ExperienceProjectController                      | |
|  |  - StoryController                                  | |
|  +---------------------+------------------------------+ |
|                         |                                |
|  +---------------------v------------------------------+ |
|  |  Business Logic Services                            | |
|  |  - ExperienceProjectService: project CRUD + skills  | |
|  |  - StoryService: story CRUD + skills                | |
|  +---------------------+------------------------------+ |
|                         |                                |
|  +---------------------v------------------------------+ |
|  |  Data Access Layer (Spring Data JPA)                | |
|  |  - ExperienceProjectRepository                      | |
|  |  - StoryRepository                                  | |
|  |  - ExperienceProjectSkillRepository                 | |
|  |  - StorySkillRepository                             | |
|  +---------------------+------------------------------+ |
+-------------------------+-------------------------------+
                          |
+-------------------------v-------------------------------+
|              PostgreSQL 18 Database                       |
|  experience_project + story +                             |
|  experience_project_skill + story_skill                   |
+----------------------------------------------------------+
```

### Request Flow: User Creates Story with Skill Links

```
1. User fills STAR form and selects skills
   |
2. Frontend calls experienceApi.createStory(projectId, {title, situation, task, action, result, metrics, visibility, skillIds})
   |
3. POST /api/v1/projects/{projectId}/stories with JSON body
   |
4. StoryController.createStory(projectId, @Valid CreateStoryRequest)
   |
5. StoryService.createStory(projectId, request):
   a. Verify project exists and belongs to tenant
   b. Validate STAR fields (all required, max 5000 chars)
   c. Create Story entity with tenant_id from context
   d. Save Story via StoryRepository
   e. For each skillId: verify UserSkill exists and belongs to same profile
   f. Create StorySkill entries via StorySkillRepository
   g. Return StoryResponse with linked skills
   |
6. 201 Created with StoryResponse body + Location header
   |
7. Frontend invalidates queries, displays new story card
```

---

## Technology Stack

### Core Framework
- **Spring Boot**: 4.x (existing)
- **Java**: 25 LTS (existing)
- **Build Tool**: Gradle 8.5+ multi-module structure (existing)

### Dependencies
No new dependencies required. Uses existing:
- Spring Data JPA, Spring Web, Spring Security, Spring Validation
- Hypersistence Utils for JSONB
- Lombok for boilerplate reduction
- Liquibase for migrations

### Frontend Technologies

**New React Components:**
- `ProjectCard.tsx`: Project display with tech stack chips and metrics (< 150 lines)
- `ProjectFormDialog.tsx`: Create/edit project dialog (< 250 lines)
- `ProjectList.tsx`: Projects list under a job experience (< 150 lines)
- `StoryCard.tsx`: STAR story display with expandable sections (< 200 lines)
- `StoryFormDialog.tsx`: STAR form dialog with skill linking (< 300 lines)
- `StoryList.tsx`: Stories list under a project (< 150 lines)
- `MetricsEditor.tsx`: Key-value metrics input component (< 150 lines)

**New API Client Modules:**
- `api/experienceApi.ts`: API client for projects and stories (< 250 lines)

**New Custom Hooks:**
- `hooks/useProjects.ts`: React Query hooks for projects (< 150 lines)
- `hooks/useStories.ts`: React Query hooks for stories (< 150 lines)

---

## Service Decomposition

### Backend Services (Java)

**ExperienceProjectService:**
- Line count estimate: 300-350 lines
- Dependencies: ExperienceProjectRepository, ExperienceProjectSkillRepository, JobExperienceRepository, UserSkillRepository, TenantContext
- Public methods:
  - `createProject(Long jobExperienceId, CreateProjectRequest dto)`: Create project with skill links
  - `updateProject(Long projectId, UpdateProjectRequest dto)`: Update project metadata
  - `deleteProject(Long projectId)`: Soft delete project + cascade to stories
  - `getProjectsByJobExperience(Long jobExperienceId)`: List projects for a job
  - `getProjectById(Long projectId)`: Get single project with skills
  - `addSkillToProject(Long projectId, Long userSkillId)`: Link skill to project
  - `removeSkillFromProject(Long projectId, Long userSkillId)`: Unlink skill from project

**StoryService:**
- Line count estimate: 300-350 lines
- Dependencies: StoryRepository, StorySkillRepository, ExperienceProjectRepository, UserSkillRepository, TenantContext
- Public methods:
  - `createStory(Long projectId, CreateStoryRequest dto)`: Create STAR story with skill links
  - `updateStory(Long storyId, UpdateStoryRequest dto)`: Update story
  - `deleteStory(Long storyId)`: Soft delete story
  - `getStoriesByProject(Long projectId)`: List stories for a project
  - `getStoryById(Long storyId)`: Get single story with skills
  - `getPublicStoriesByProfile(Long profileId)`: Public stories for recruiter chat
  - `getStoriesBySkill(Long userSkillId)`: Stories linked to a specific skill
  - `addSkillToStory(Long storyId, Long userSkillId)`: Link skill to story
  - `removeSkillFromStory(Long storyId, Long userSkillId)`: Unlink skill from story

---

## Data Model Implementation

### Database Schema

```sql
-- experience_project table
CREATE TABLE experience_project (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    job_experience_id BIGINT NOT NULL REFERENCES job_experience(id),
    title VARCHAR(255) NOT NULL,
    context TEXT,
    role VARCHAR(255),
    team_size INT,
    tech_stack JSONB,
    architecture_type VARCHAR(100),
    metrics JSONB,
    outcomes TEXT,
    visibility VARCHAR(20) DEFAULT 'private' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_experience_project_tenant_id ON experience_project(tenant_id);
CREATE INDEX idx_experience_project_job_experience_id ON experience_project(job_experience_id);
CREATE INDEX idx_experience_project_deleted_at ON experience_project(deleted_at);

-- story table
CREATE TABLE story (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    experience_project_id BIGINT NOT NULL REFERENCES experience_project(id),
    title VARCHAR(255) NOT NULL,
    situation TEXT NOT NULL,
    task TEXT NOT NULL,
    action TEXT NOT NULL,
    result TEXT NOT NULL,
    metrics JSONB,
    visibility VARCHAR(20) DEFAULT 'private' NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMPTZ,
    version BIGINT DEFAULT 0 NOT NULL
);

CREATE INDEX idx_story_tenant_id ON story(tenant_id);
CREATE INDEX idx_story_experience_project_id ON story(experience_project_id);
CREATE INDEX idx_story_deleted_at ON story(deleted_at);
CREATE INDEX idx_story_visibility ON story(visibility);

-- experience_project_skill join table
CREATE TABLE experience_project_skill (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    experience_project_id BIGINT NOT NULL REFERENCES experience_project(id),
    user_skill_id BIGINT NOT NULL REFERENCES user_skill(id),
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE UNIQUE INDEX idx_experience_project_skill_unique
    ON experience_project_skill(experience_project_id, user_skill_id);
CREATE INDEX idx_experience_project_skill_tenant_id ON experience_project_skill(tenant_id);

-- story_skill join table
CREATE TABLE story_skill (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    story_id BIGINT NOT NULL REFERENCES story(id),
    user_skill_id BIGINT NOT NULL REFERENCES user_skill(id),
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

CREATE UNIQUE INDEX idx_story_skill_unique ON story_skill(story_id, user_skill_id);
CREATE INDEX idx_story_skill_tenant_id ON story_skill(tenant_id);
```

### Java Entities

```java
// ExperienceProject.java
@Entity
@Table(name = "experience_project")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ExperienceProject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "job_experience_id", nullable = false)
    private Long jobExperienceId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String context;

    @Column(length = 255)
    private String role;

    @Column(name = "team_size")
    private Integer teamSize;

    @Type(JsonBinaryType.class)
    @Column(name = "tech_stack", columnDefinition = "jsonb")
    private List<String> techStack;

    @Column(name = "architecture_type", length = 100)
    private String architectureType;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(columnDefinition = "TEXT")
    private String outcomes;

    @Column(nullable = false, length = 20)
    private String visibility = "private";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_experience_id", insertable = false, updatable = false)
    private JobExperience jobExperience;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

// Story.java
@Entity
@Table(name = "story")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "experience_project_id", nullable = false)
    private Long experienceProjectId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String situation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String task;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String result;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(nullable = false, length = 20)
    private String visibility = "private";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experience_project_id", insertable = false, updatable = false)
    private ExperienceProject experienceProject;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

// ExperienceProjectSkill.java
@Entity
@Table(name = "experience_project_skill")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceProjectSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "experience_project_id", nullable = false)
    private Long experienceProjectId;

    @Column(name = "user_skill_id", nullable = false)
    private Long userSkillId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

// StorySkill.java
@Entity
@Table(name = "story_skill")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorySkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "story_id", nullable = false)
    private Long storyId;

    @Column(name = "user_skill_id", nullable = false)
    private Long userSkillId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

### DTOs (Data Transfer Objects)

```java
// CreateProjectRequest.java
public record CreateProjectRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 5000) String context,
    @Size(max = 255) String role,
    @Min(1) @Max(1000) Integer teamSize,
    @Size(max = 30) List<String> techStack,
    @Size(max = 100) String architectureType,
    Map<String, Object> metrics,
    @Size(max = 5000) String outcomes,
    String visibility,
    List<Long> skillIds
) {}

// UpdateProjectRequest.java
public record UpdateProjectRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 5000) String context,
    @Size(max = 255) String role,
    @Min(1) @Max(1000) Integer teamSize,
    @Size(max = 30) List<String> techStack,
    @Size(max = 100) String architectureType,
    Map<String, Object> metrics,
    @Size(max = 5000) String outcomes,
    String visibility,
    List<Long> skillIds
) {}

// ProjectResponse.java
public record ProjectResponse(
    Long id,
    Long jobExperienceId,
    String title,
    String context,
    String role,
    Integer teamSize,
    List<String> techStack,
    String architectureType,
    Map<String, Object> metrics,
    String outcomes,
    String visibility,
    List<SkillSummary> skills,
    int storyCount,
    Instant createdAt,
    Instant updatedAt
) {}

// SkillSummary.java (lightweight skill info for embedding in responses)
public record SkillSummary(
    Long userSkillId,
    String skillName,
    String category
) {}

// CreateStoryRequest.java
public record CreateStoryRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 5000) String situation,
    @NotBlank @Size(max = 5000) String task,
    @NotBlank @Size(max = 5000) String action,
    @NotBlank @Size(max = 5000) String result,
    Map<String, Object> metrics,
    String visibility,
    List<Long> skillIds
) {}

// UpdateStoryRequest.java
public record UpdateStoryRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 5000) String situation,
    @NotBlank @Size(max = 5000) String task,
    @NotBlank @Size(max = 5000) String action,
    @NotBlank @Size(max = 5000) String result,
    Map<String, Object> metrics,
    String visibility,
    List<Long> skillIds
) {}

// StoryResponse.java
public record StoryResponse(
    Long id,
    Long experienceProjectId,
    String title,
    String situation,
    String task,
    String action,
    String result,
    Map<String, Object> metrics,
    String visibility,
    List<SkillSummary> skills,
    Instant createdAt,
    Instant updatedAt
) {}
```

---

## API Implementation

### New Endpoints

| Method | Path | Description | Auth | Request Body | Response |
|--------|------|-------------|------|--------------|----------|
| GET | `/api/v1/jobs/{jobId}/projects` | List projects for a job | Yes | - | List\<ProjectResponse> |
| POST | `/api/v1/jobs/{jobId}/projects` | Create project under a job | Yes | CreateProjectRequest | ProjectResponse (201) |
| GET | `/api/v1/projects/{projectId}` | Get project by ID | Yes | - | ProjectResponse |
| PUT | `/api/v1/projects/{projectId}` | Update project | Yes | UpdateProjectRequest | ProjectResponse |
| DELETE | `/api/v1/projects/{projectId}` | Soft delete project + stories | Yes | - | 204 No Content |
| POST | `/api/v1/projects/{projectId}/skills/{userSkillId}` | Link skill to project | Yes | - | 204 No Content |
| DELETE | `/api/v1/projects/{projectId}/skills/{userSkillId}` | Unlink skill from project | Yes | - | 204 No Content |
| GET | `/api/v1/projects/{projectId}/stories` | List stories for a project | Yes | - | List\<StoryResponse> |
| POST | `/api/v1/projects/{projectId}/stories` | Create story under project | Yes | CreateStoryRequest | StoryResponse (201) |
| GET | `/api/v1/stories/{storyId}` | Get story by ID | Yes | - | StoryResponse |
| PUT | `/api/v1/stories/{storyId}` | Update story | Yes | UpdateStoryRequest | StoryResponse |
| DELETE | `/api/v1/stories/{storyId}` | Soft delete story | Yes | - | 204 No Content |
| POST | `/api/v1/stories/{storyId}/skills/{userSkillId}` | Link skill to story | Yes | - | 204 No Content |
| DELETE | `/api/v1/stories/{storyId}/skills/{userSkillId}` | Unlink skill from story | Yes | - | 204 No Content |
| GET | `/api/v1/profiles/{profileId}/stories/public` | Get all public stories | Yes | - | List\<StoryResponse> |

---

## Implementation Phases

### Phase 1: Database Foundation (Day 1)

**Tasks:**
1. Create Liquibase migration for `experience_project` table
2. Create Liquibase migration for `story` table
3. Create Liquibase migration for `experience_project_skill` join table
4. Create Liquibase migration for `story_skill` join table
5. Add all migrations to db.changelog-master.yaml
6. Create JPA entities: ExperienceProject, Story, ExperienceProjectSkill, StorySkill
7. Test migrations on fresh database

**Validation:**
- [ ] Migrations run successfully
- [ ] Tables created with correct indexes, FKs, and constraints
- [ ] JPA entities map correctly to tables
- [ ] JSONB fields serialize/deserialize correctly

### Phase 2: Backend Core (Day 2-3)

**Tasks:**
1. Create repositories: ExperienceProjectRepository, StoryRepository, ExperienceProjectSkillRepository, StorySkillRepository
2. Create DTOs (Java records): CreateProjectRequest, UpdateProjectRequest, ProjectResponse, CreateStoryRequest, UpdateStoryRequest, StoryResponse, SkillSummary
3. Create mappers: ExperienceProjectMapper, StoryMapper
4. Implement ExperienceProjectService with project CRUD and skill linking
5. Implement StoryService with story CRUD, skill linking, and public stories retrieval
6. Add @Transactional annotations to all service methods
7. Add structured logging

**Validation:**
- [ ] All service methods work with tenant filtering
- [ ] Cascade soft delete works (project -> stories)
- [ ] Skill linking validates skills belong to same profile
- [ ] Public stories retrieval filters correctly

### Phase 3: REST API Layer (Day 3-4)

**Tasks:**
1. Implement ExperienceProjectController with 7 endpoints
2. Implement StoryController with 8 endpoints
3. Add @Valid annotation on request bodies
4. Add exception handlers in GlobalExceptionHandler
5. Write integration tests for all endpoints

**Validation:**
- [ ] All endpoints return correct HTTP status codes
- [ ] Validation errors return 400 with field-specific messages
- [ ] Tenant isolation prevents cross-tenant access
- [ ] Cascade delete works via API

### Phase 4: Frontend Implementation (Day 5-6)

**Tasks:**
1. Create experienceApi.ts with API client methods
2. Create useProjects and useStories React Query hooks
3. Implement MetricsEditor reusable component
4. Implement ProjectCard component
5. Implement ProjectFormDialog with skill selector
6. Implement ProjectList (embedded under job experience cards)
7. Implement StoryCard with expandable STAR sections
8. Implement StoryFormDialog with STAR fields and skill linking
9. Implement StoryList (embedded under project cards)
10. Integrate with existing ProfileEditorPage

**Validation:**
- [ ] Projects display under correct job experiences
- [ ] Stories display under correct projects
- [ ] STAR form clearly labels all four sections
- [ ] Skill linking works with autocomplete from user's skills
- [ ] Metrics editor allows adding/removing key-value pairs
- [ ] Visibility toggle works correctly

---

## Security Implementation

### Authentication/Authorization
- All endpoints require JWT authentication (existing from Feature 001)
- Tenant context extracted from JWT and applied automatically
- No admin-specific endpoints (unlike Feature 003's catalog)

### Data Protection
- Tenant ID never exposed in responses
- STAR content may contain sensitive business details (private by default)
- Soft delete prevents permanent data loss

### Security Testing
- [ ] Cross-tenant access returns 404
- [ ] Soft delete prevents deleted data from appearing
- [ ] Public/private visibility correctly filters stories
- [ ] XSS prevention in STAR text fields

---

## Performance Targets

| Metric | Target | Method |
|--------|--------|--------|
| Projects list per job | p95 < 200ms | Index on job_experience_id |
| Stories list per project | p95 < 150ms | Index on experience_project_id |
| Create project with skills | p95 < 300ms | Single transaction |
| Create story with skills | p95 < 300ms | Single transaction |
| Public stories for profile | p95 < 200ms | Index on visibility + tenant_id |

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

### Test Coverage

**Unit Tests:**
- ExperienceProjectService: createProject, updateProject, deleteProject (cascade), addSkill, removeSkill
- StoryService: createStory (STAR validation), updateStory, deleteStory, getPublicStories, addSkill, removeSkill
- DTOs: validation annotations

**Integration Tests:**
- ExperienceProjectController: all 7 endpoints
- StoryController: all 8 endpoints
- Cascade delete verification
- Tenant isolation
- Skill linking validation

**Target Coverage:** 85%

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Cascade delete complexity | Medium | Medium | Test thoroughly, use database transactions |
| JSONB metrics query performance | Low | Low | No queries on metrics content (read-only display) |
| Large STAR text fields | Low | Low | TEXT type in PostgreSQL handles large strings well |
| Skill linking validation complexity | Medium | Low | Validate in service layer, return clear errors |
| Frontend nesting complexity (job > project > story) | Medium | Medium | Use collapsible sections, lazy loading |

---

## Migration Path (Future Features)

### To Feature 006: Recruiter Chat RAG Context
1. Use `StoryService.getPublicStoriesByProfile(profileId)` for RAG context
2. Stories provide structured data for answering "Tell me about a time..." questions
3. Metrics enable quantitative answers ("handled 18,000 TPS")

### To Feature 008: Resume Export
1. Projects and stories power "Key Projects" and "Achievements" resume sections
2. Metrics provide quantitative highlights for resume bullets
3. Tech stack populates "Technologies Used" sections per job

---

## References

- [Feature Specification](./spec.md)
- [Project Constitution](../../.specify/memory/constitution.md)
- [Project Overview](../../.specify/memory/project-overview.md)
- [Feature 002: Profile CRUD](../002-profile-crud/)
- [Feature 003: Skills Management](../003-skills-management/)

---

## Sign-Off

**Planning Complete:** Yes
**Constitution Validated:** All principles satisfied
**Ready for Implementation:** Yes

---

**Plan Version:** 1.0.0
**Last Updated:** 2026-02-24
**Estimated Implementation Time:** 5-6 days
