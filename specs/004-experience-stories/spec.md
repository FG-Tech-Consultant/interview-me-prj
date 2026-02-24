# Feature Specification: Experience Projects & Stories (STAR Format)

**Feature ID:** 004-experience-stories
**Status:** Draft
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Overview

### Problem Statement

Career professionals accumulate rich project experience across their jobs, but lack a structured way to capture and present this context:
- Project details (scale, architecture, team size) live only in memory or scattered notes
- Behavioral interview questions ("Tell me about a time you...") require prepared STAR-format stories, which most professionals don't maintain systematically
- Recruiters ask about concrete experience (traffic volumes, incident handling, leadership) but candidates struggle to recall specifics on the spot
- No linkage between skills claimed and projects where those skills were actually applied
- AI-powered recruiter chat (Feature 006) needs structured, retrievable stories to answer questions accurately

Without structured project and story data, the platform cannot generate compelling resumes, power meaningful recruiter chat responses, or demonstrate the depth behind claimed skills.

### Feature Summary

Implement a two-entity system for capturing detailed career experience:

1. **ExperienceProject** - A project or initiative under a specific job (JobExperience), capturing title, context, role, team size, tech stack, architecture type, metrics, and outcomes. Projects link to UserSkills to demonstrate which skills were applied in practice.

2. **Story** - A structured narrative in STAR format (Situation, Task, Action, Result) linked to one ExperienceProject and one or more UserSkills. Stories capture behavioral interview answers with optional impact metrics and support public/private visibility for recruiter chat integration.

Together, these entities bridge the gap between "skills I have" and "how I used them," enabling rich resume generation, recruiter chat grounding, and career storytelling.

### Target Users

- **Career Professionals (Primary User):** Senior engineers, tech leads, architects who need to document projects with metrics and prepare STAR-format stories for interviews
- **Profile Owners:** Any professional maintaining detailed career history for recruiter interactions
- **Recruiters (Indirect User):** Benefit from structured stories when evaluating candidates via recruiter chat
- **AI Systems (Indirect Consumer):** Recruiter chat RAG pipeline uses public stories as context for answering questions

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Standard REST CRUD with Spring Data JPA, conventional controller-service-repository layering, React forms with MUI components
- **Principle 3: Modern Java Standards** - Use Java 25 records for DTOs, Lombok for entity boilerplate, modern JPA features
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - All entities (ExperienceProject, Story, StorySkill) MUST include tenant_id with automatic filtering via Hibernate filter, PostgreSQL with JSONB for flexible metrics/tech stack storage
- **Principle 6: Observability and Debugging** - Structured logging for all CRUD operations with tenant context
- **Principle 7: Security, Privacy, and Credential Management** - Public/private visibility at story level, @Transactional annotations, JWT authentication required
- **Principle 8: Multi-Tenant Architecture** - All entities include tenant_id, automatically filtered via Hibernate filter from Feature 001
- **Principle 10: Full-Stack Modularity and Separation of Concerns** - Backend: `com.interviewme.experience` package (controller, service, repository, entity, dto, mapper), Frontend: pages, components, api, hooks organized by domain
- **Principle 11: Database Schema Evolution** - Liquibase timestamp-based migrations for experience_project, story, and story_skill tables with rollback support

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: User Documents a Major Project Under a Job

**Actor:** Senior engineer documenting a high-traffic payments platform
**Goal:** Create a detailed project record linked to a job experience and relevant skills
**Preconditions:** User has authenticated account, Profile exists (Feature 002), JobExperience exists, UserSkills exist (Feature 003)

**Steps:**
1. User navigates to a specific job experience in profile editor
2. User clicks "Add Project" under that job
3. User fills in project details:
   - Title: "Real-Time Payments Gateway"
   - Context: "Built a new real-time payments processing gateway for the Brazilian market, replacing a batch-based legacy system"
   - Role: "Tech Lead"
   - Team size: 8
   - Tech stack: ["Java 21", "Spring Boot 3", "Kafka", "PostgreSQL", "Kubernetes", "Istio"]
   - Architecture type: "Microservices"
   - Metrics: {"tps": 15000, "latency_p95": "45ms", "availability": "99.99%", "daily_volume": "$2B"}
   - Outcomes: "Reduced settlement time from T+1 to real-time, increased throughput 10x"
4. User links relevant skills: Java, Spring Boot, Kafka, PostgreSQL, Kubernetes
5. System validates inputs and saves ExperienceProject with tenant association
6. Project appears under the job experience with linked skills displayed

**Success Criteria:**
- Project created with all fields validated
- Project linked to correct JobExperience
- Skills linked via join table (experience_project_skill)
- Tenant isolation enforced

#### Scenario 2: User Creates a STAR Story for a Project

**Actor:** Professional preparing for behavioral interviews
**Goal:** Create a structured STAR story about handling a production incident
**Preconditions:** User has ExperienceProject created

**Steps:**
1. User navigates to an existing project
2. User clicks "Add Story" under the project
3. User fills in STAR format fields:
   - Title: "Handling the Black Friday Traffic Spike"
   - Situation: "Our payments gateway was processing 5,000 TPS when Black Friday traffic hit 15,000 TPS, exceeding our capacity planning estimates by 3x"
   - Task: "As tech lead, I needed to keep the system running without transaction loss while scaling infrastructure in real-time"
   - Action: "Implemented emergency horizontal scaling by adding 12 new pods via Kubernetes HPA, activated circuit breakers on non-critical downstream services, and introduced priority queues in Kafka to ensure payment transactions were processed before analytics events"
   - Result: "Zero transaction loss during the 6-hour peak. System handled 18,000 TPS at peak. Led to permanent auto-scaling policy that reduced future incident response from 45 minutes to 3 minutes. Recognized with engineering excellence award."
   - Metrics: {"peak_tps": 18000, "downtime": "0 minutes", "response_time": "3 minutes"}
4. User links skills to this story: Kubernetes, Kafka, Java
5. User sets visibility to "public" (available for recruiter chat)
6. System validates and saves Story

**Success Criteria:**
- Story created with all STAR fields populated
- Story linked to ExperienceProject
- Skills linked to story via story_skill join table
- Visibility flag respected in public APIs

#### Scenario 3: User Edits Project Metrics After Performance Review

**Actor:** Profile owner updating project metrics with latest data
**Goal:** Update metrics for an existing project after end-of-year review
**Preconditions:** User has existing ExperienceProject

**Steps:**
1. User navigates to existing project
2. User clicks edit icon
3. User updates metrics: TPS increased from 15,000 to 22,000, adds new metric "cost_reduction": "40%"
4. User updates outcomes with new achievement
5. System validates and saves updated project
6. Updated metrics reflected in all linked stories and exports

**Success Criteria:**
- Metrics updated without creating duplicate project
- Updated_at timestamp set
- Linked stories unaffected by project metadata changes

#### Scenario 4: User Deletes an Obsolete Story (Soft Delete)

**Actor:** Profile owner removing a story that's no longer relevant
**Goal:** Remove an old story about a technology no longer in use
**Preconditions:** User has existing story

**Steps:**
1. User navigates to story list under a project
2. User clicks delete icon on the story
3. System prompts confirmation: "Delete this story? It will be hidden from all exports and recruiter chat."
4. User confirms deletion
5. System performs soft delete: sets deleted_at timestamp
6. Story disappears from UI and public APIs
7. Linked skill associations preserved in database (soft delete only)

**Success Criteria:**
- Story not visible in UI after deletion
- Data remains in database (soft delete)
- Linked skill relationships preserved for potential recovery
- Deletion event logged with tenant context

### Edge Cases

- **Project without stories:** Projects can exist without stories (stories are optional enrichment)
- **Story without skills:** Stories can exist without linked skills (skills are optional)
- **Orphaned project:** Job experience deleted while projects exist (cascade soft delete)
- **Duplicate project titles:** Allow duplicate titles under different jobs (no unique constraint on title)
- **Very long STAR fields:** Each STAR field limited to 5000 characters
- **Empty tech stack:** Tech stack is optional (some projects may not be technology-focused)
- **Metrics with varied formats:** JSONB allows flexible metric structures (numbers, strings, percentages)
- **Concurrent editing:** Optimistic locking prevents lost updates (version field)
- **Skills not in profile:** Only UserSkills from the user's profile can be linked (validated in service layer)

---

## Functional Requirements

### Core Capabilities

**REQ-001:** ExperienceProject Creation
- **Description:** Users MUST be able to create projects under a specific job experience with detailed metadata
- **Acceptance Criteria:**
  - ExperienceProject includes: title (required), context (optional), role (optional), team_size (optional), tech_stack (JSONB array), architecture_type (optional), metrics (JSONB object), outcomes (optional), visibility (public/private)
  - Each project belongs to exactly one JobExperience (job_experience_id FK)
  - Each project belongs to one tenant (tenant_id FK)
  - Default visibility is "private"
  - Tech stack stored as JSONB array: ["Java", "Spring Boot", "Kafka"]
  - Metrics stored as JSONB object: {"tps": 15000, "latency_p95": "45ms"}

**REQ-002:** ExperienceProject Update
- **Description:** Users MUST be able to update all project fields with validation
- **Acceptance Criteria:**
  - All fields editable except id, tenant_id, job_experience_id, created_at
  - Optimistic locking prevents concurrent update conflicts (version field)
  - Updated_at timestamp automatically set
  - Changes immediately reflected in linked stories and public APIs

**REQ-003:** ExperienceProject Deletion (Soft Delete)
- **Description:** Users MUST be able to soft-delete projects, which also soft-deletes associated stories
- **Acceptance Criteria:**
  - Soft delete sets deleted_at timestamp
  - All stories under the deleted project also get soft-deleted (cascade)
  - Deleted projects excluded from UI, exports, and recruiter chat context
  - Data preserved in database for audit and potential recovery

**REQ-004:** ExperienceProject-Skill Linking
- **Description:** Users MUST be able to link existing UserSkills to projects
- **Acceptance Criteria:**
  - Many-to-many relationship via experience_project_skill join table
  - Only active (non-deleted) UserSkills from the user's profile can be linked
  - Skills can be added/removed from a project independently
  - Join table includes tenant_id for isolation

**REQ-005:** Story Creation (STAR Format)
- **Description:** Users MUST be able to create structured STAR stories linked to an ExperienceProject
- **Acceptance Criteria:**
  - Story includes: title (required), situation (required), task (required), action (required), result (required), metrics (JSONB optional), visibility (public/private)
  - Each story belongs to exactly one ExperienceProject (experience_project_id FK)
  - Each story belongs to one tenant (tenant_id FK)
  - Default visibility is "private"
  - Each STAR field accepts up to 5000 characters
  - Metrics field allows flexible impact quantification: {"revenue_impact": "$2M", "time_saved": "40%"}

**REQ-006:** Story Update
- **Description:** Users MUST be able to update all story fields
- **Acceptance Criteria:**
  - All fields editable except id, tenant_id, experience_project_id, created_at
  - Optimistic locking with version field
  - Updated_at timestamp automatically set

**REQ-007:** Story Deletion (Soft Delete)
- **Description:** Users MUST be able to soft-delete stories
- **Acceptance Criteria:**
  - Soft delete sets deleted_at timestamp
  - Deleted stories excluded from UI, exports, and recruiter chat
  - Story-skill associations preserved in database

**REQ-008:** Story-Skill Linking
- **Description:** Users MUST be able to link one or more UserSkills to a story
- **Acceptance Criteria:**
  - Many-to-many relationship via story_skill join table
  - Only active (non-deleted) UserSkills from the user's profile can be linked
  - Skills can be added/removed from a story independently
  - Join table includes tenant_id for isolation

**REQ-009:** Story Visibility Control
- **Description:** Users MUST be able to control story visibility (public/private)
- **Acceptance Criteria:**
  - Public stories: visible in recruiter chat context, public profile, and exports
  - Private stories: visible only to profile owner
  - Visibility can be toggled independently of project visibility
  - Public stories MUST belong to public or semi-public projects

**REQ-010:** Project and Story Retrieval
- **Description:** Users MUST be able to retrieve projects and stories by job, profile, or skill
- **Acceptance Criteria:**
  - Get all projects for a job experience (ordered by creation date)
  - Get all stories for a project (ordered by creation date)
  - Get all stories linked to a specific skill
  - Get all public stories for a profile (recruiter chat context)
  - Read-only transactions for all retrieval operations

### User Interface Requirements

**REQ-UI-001:** Projects Section Under Job Experience
- **Description:** Frontend MUST display projects as a section under each job experience in the profile editor
- **Acceptance Criteria:**
  - "Projects" section visible within each job experience card
  - Each project shows: title, role, team size, tech stack (as chips), architecture type, visibility badge
  - "Add Project" button opens project form dialog
  - Edit and delete icons on each project card
  - Collapsible project details showing metrics and outcomes

**REQ-UI-002:** Project Form Dialog
- **Description:** Frontend MUST provide a modal dialog for creating/editing projects
- **Acceptance Criteria:**
  - Form fields: title (TextField), context (TextField multiline), role (TextField), team_size (number input), tech_stack (Autocomplete freeSolo chips), architecture_type (Select: Monolith, Microservices, Serverless, Event-Driven, Other), metrics (key-value editor), outcomes (TextField multiline), visibility toggle
  - Skill linking: multi-select from user's existing skills (autocomplete from UserSkills)
  - Inline validation with error messages
  - Save button with loading state

**REQ-UI-003:** Stories Section Under Project
- **Description:** Frontend MUST display stories as cards under each project
- **Acceptance Criteria:**
  - Each story card shows: title, STAR fields preview (truncated), linked skills (chips), visibility badge, metrics summary
  - "Add Story" button opens story form dialog
  - Edit and delete icons on each story card
  - Expandable STAR sections (click to read full text)

**REQ-UI-004:** Story Form Dialog (STAR Format)
- **Description:** Frontend MUST provide a modal dialog for creating/editing STAR stories
- **Acceptance Criteria:**
  - Clearly labeled STAR sections: Situation, Task, Action, Result
  - Each section is a multiline TextField with character counter (max 5000)
  - Title field (required)
  - Metrics editor: key-value pairs for impact quantification
  - Skill linking: multi-select from user's existing skills
  - Visibility toggle (public/private)
  - Helper text explaining each STAR section:
    - Situation: "Set the scene. What was the context?"
    - Task: "What was your specific responsibility?"
    - Action: "What steps did you take?"
    - Result: "What was the outcome? Include metrics."

### Data Requirements

**REQ-DATA-001:** ExperienceProject Table Schema
- **Description:** PostgreSQL table for storing project/initiative details
- **Acceptance Criteria:**
  - Table name: `experience_project`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), job_experience_id (BIGINT FK NOT NULL), title (VARCHAR 255 NOT NULL), context (TEXT), role (VARCHAR 255), team_size (INT), tech_stack (JSONB array), architecture_type (VARCHAR 100), metrics (JSONB object), outcomes (TEXT), visibility (VARCHAR 20 DEFAULT 'private'), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), deleted_at (TIMESTAMPTZ), version (BIGINT DEFAULT 0)
  - Indexes: idx_experience_project_tenant_id, idx_experience_project_job_experience_id, idx_experience_project_deleted_at
  - Foreign keys: tenant_id -> tenant(id), job_experience_id -> job_experience(id)

**REQ-DATA-002:** Story Table Schema
- **Description:** PostgreSQL table for storing STAR-format stories
- **Acceptance Criteria:**
  - Table name: `story`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), experience_project_id (BIGINT FK NOT NULL), title (VARCHAR 255 NOT NULL), situation (TEXT NOT NULL), task (TEXT NOT NULL), action (TEXT NOT NULL), result (TEXT NOT NULL), metrics (JSONB object), visibility (VARCHAR 20 DEFAULT 'private'), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), deleted_at (TIMESTAMPTZ), version (BIGINT DEFAULT 0)
  - Indexes: idx_story_tenant_id, idx_story_experience_project_id, idx_story_deleted_at, idx_story_visibility
  - Foreign keys: tenant_id -> tenant(id), experience_project_id -> experience_project(id)

**REQ-DATA-003:** ExperienceProject-Skill Join Table
- **Description:** PostgreSQL join table for many-to-many relationship between projects and UserSkills
- **Acceptance Criteria:**
  - Table name: `experience_project_skill`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), experience_project_id (BIGINT FK NOT NULL), user_skill_id (BIGINT FK NOT NULL), created_at (TIMESTAMPTZ NOT NULL)
  - Unique constraint: (experience_project_id, user_skill_id)
  - Foreign keys: tenant_id -> tenant(id), experience_project_id -> experience_project(id), user_skill_id -> user_skill(id)

**REQ-DATA-004:** Story-Skill Join Table
- **Description:** PostgreSQL join table for many-to-many relationship between stories and UserSkills
- **Acceptance Criteria:**
  - Table name: `story_skill`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), story_id (BIGINT FK NOT NULL), user_skill_id (BIGINT FK NOT NULL), created_at (TIMESTAMPTZ NOT NULL)
  - Unique constraint: (story_id, user_skill_id)
  - Foreign keys: tenant_id -> tenant(id), story_id -> story(id), user_skill_id -> user_skill(id)

---

## Success Criteria

The feature will be considered successful when:

1. **Complete CRUD Operations:** Users can create, read, update, and delete ExperienceProjects and Stories without errors
   - Measurement: Integration tests cover all CRUD endpoints with 100% success rate

2. **STAR Format Integrity:** Stories enforce all four STAR fields (Situation, Task, Action, Result) as required
   - Measurement: Validation tests verify 400 Bad Request when any STAR field is missing

3. **Skill Linking Works:** Projects and stories can be linked to UserSkills, and skill associations are retrievable
   - Measurement: Test adding 5 skills to a project and 3 skills to a story, verify all associations persist

4. **Tenant Isolation Verified:** Users in different tenants cannot access each other's projects or stories
   - Measurement: Create two tenants with projects, verify cross-tenant access returns 404

5. **Public/Private Visibility Enforced:** Private stories excluded from public APIs
   - Measurement: Create mixed-visibility stories, verify public API response excludes private ones

6. **Cascade Soft Delete:** Deleting a project soft-deletes all its stories
   - Measurement: Delete project with 3 stories, verify all 4 records have deleted_at set

7. **JSONB Metrics Stored Correctly:** Flexible metrics stored and retrieved with correct types
   - Measurement: Store metrics with mixed types (numbers, strings, percentages), retrieve and verify

---

## Key Entities

### ExperienceProject

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- job_experience_id: Foreign key to JobExperience (BIGINT, NOT NULL, indexed)
- title: Project title (VARCHAR 255, NOT NULL, e.g., "Real-Time Payments Gateway")
- context: Project context/background (TEXT, optional, max 5000 chars)
- role: User's role in the project (VARCHAR 255, optional, e.g., "Tech Lead")
- team_size: Number of team members (INT, optional, >= 1)
- tech_stack: Technologies used (JSONB array, e.g., ["Java 21", "Spring Boot 3", "Kafka"])
- architecture_type: System architecture (VARCHAR 100, optional, e.g., "Microservices")
- metrics: Quantitative project metrics (JSONB object, e.g., {"tps": 15000, "availability": "99.99%"})
- outcomes: Project outcomes/results (TEXT, optional, max 5000 chars)
- visibility: Public/private flag (VARCHAR 20, default "private")
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- deleted_at: Soft delete timestamp (TIMESTAMPTZ, NULL)
- version: Optimistic locking version (BIGINT, default 0)

**Relationships:**
- Many-to-one with Tenant
- Many-to-one with JobExperience (many projects belong to one job)
- One-to-many with Story (one project has many stories)
- Many-to-many with UserSkill (via experience_project_skill join table)

**Validation Rules:**
- title: Required, 1-255 characters
- context: Optional, max 5000 characters
- role: Optional, 1-255 characters
- team_size: Optional, >= 1, <= 1000
- tech_stack: Array of strings, each 1-100 characters, max 30 items
- architecture_type: Optional, 1-100 characters
- metrics: JSONB object, flexible structure
- outcomes: Optional, max 5000 characters
- visibility: Must be "public" or "private"

### Story

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- experience_project_id: Foreign key to ExperienceProject (BIGINT, NOT NULL, indexed)
- title: Story title (VARCHAR 255, NOT NULL, e.g., "Handling the Black Friday Traffic Spike")
- situation: STAR - Situation (TEXT, NOT NULL, max 5000 chars)
- task: STAR - Task (TEXT, NOT NULL, max 5000 chars)
- action: STAR - Action (TEXT, NOT NULL, max 5000 chars)
- result: STAR - Result (TEXT, NOT NULL, max 5000 chars)
- metrics: Impact metrics (JSONB object, optional, e.g., {"peak_tps": 18000, "downtime": "0 min"})
- visibility: Public/private flag (VARCHAR 20, default "private")
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- deleted_at: Soft delete timestamp (TIMESTAMPTZ, NULL)
- version: Optimistic locking version (BIGINT, default 0)

**Relationships:**
- Many-to-one with Tenant
- Many-to-one with ExperienceProject (many stories belong to one project)
- Many-to-many with UserSkill (via story_skill join table)

**Validation Rules:**
- title: Required, 1-255 characters
- situation: Required, 1-5000 characters
- task: Required, 1-5000 characters
- action: Required, 1-5000 characters
- result: Required, 1-5000 characters
- metrics: JSONB object, flexible structure
- visibility: Must be "public" or "private"

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Requires authentication, tenant filtering, database infrastructure
- **Feature 002: Profile CRUD** - Requires Profile and JobExperience entities (job_experience_id FK)
- **Feature 003: Skills Management** - Requires UserSkill entity for skill linking (user_skill_id FK in join tables)

### External Dependencies

- **Hypersistence Utils:** For JSONB type mapping in JPA entities (tech_stack, metrics)
- **Spring Data JPA:** For repository abstractions and query methods
- **Liquibase:** For database schema migrations
- **MUI:** For frontend form components (TextField, Select, Autocomplete, Dialog)

---

## Assumptions

1. Features 001, 002, and 003 are fully implemented
2. Users have between 1-20 projects per job experience (reasonable cardinality)
3. Users have between 0-5 stories per project (not every project needs a story)
4. STAR format with all four fields required provides sufficient structure for interview preparation
5. Metrics are free-form JSONB (no strict schema, allows varied formats)
6. Tech stack items are free-text strings (no validation against skill catalog)
7. Architecture types are free-text (no enum constraint initially)
8. Cascade soft delete from project to stories is sufficient (no cascade from job to projects - handled at service layer)
9. Story-skill and project-skill join tables use hard delete (no soft delete needed for associations)
10. A story must belong to a project (no standalone stories)

---

## Out of Scope

1. **No AI Story Generation:** Auto-generating STAR stories from job descriptions
2. **No Story Templates:** Pre-built story templates for common scenarios
3. **No Story Scoring:** AI-powered quality scoring of STAR stories
4. **No Story Versioning:** Version history for story edits
5. **No Media Attachments:** Images, diagrams, or documents attached to projects/stories
6. **No Story Endorsements:** Others validating/endorsing your stories
7. **No Story Tags/Categories:** Beyond skill linking, no additional categorization
8. **No Story Search:** Full-text search across stories (deferred to recruiter chat RAG)
9. **No Import from External Sources:** Importing projects from GitHub, Jira, etc.
10. **No Project Timeline Visualization:** Gantt or timeline view of projects
11. **No Story Collaboration:** Multiple people contributing to the same story
12. **No Standalone Stories:** Every story must belong to an ExperienceProject

---

## Security & Privacy Considerations

### Security Requirements

- All project and story API endpoints MUST require valid JWT authentication
- Tenant filtering MUST be automatically applied to all queries (prevent cross-tenant access)
- Input validation MUST sanitize HTML/XSS in text fields (STAR content, project context)
- Optimistic locking (version field) prevents concurrent update conflicts
- Soft delete prevents accidental permanent data loss
- Audit fields (created_at, updated_at) MUST be system-managed (not user-provided)

### Privacy Requirements

- Default visibility for new projects and stories is "private"
- Public/private flags MUST be respected in all API responses
- Private stories NEVER included in recruiter chat context or public profile pages
- Deleted entries (deleted_at not null) MUST be excluded from all user-facing queries
- Story STAR content may contain sensitive business details (treated as private by default)

---

## Performance Expectations

- **Projects List per Job:** p95 latency < 200ms (typically 1-20 projects per job)
- **Stories List per Project:** p95 latency < 150ms (typically 0-5 stories per project)
- **Project Create/Update:** p95 latency < 300ms
- **Story Create/Update:** p95 latency < 300ms
- **Skill Linking Operations:** p95 latency < 200ms
- **Public Stories Retrieval (for RAG):** p95 latency < 200ms (all public stories for a profile)
- **Database Queries:** Use indexes on tenant_id, job_experience_id, experience_project_id, deleted_at, visibility

---

## Error Handling

### Error Scenarios

**ERROR-001:** Job Experience Not Found
- **User Experience:** API returns 404 Not Found: `{"message": "Job experience not found", "code": "JOB_EXPERIENCE_NOT_FOUND"}`
- **Recovery Path:** User navigates to correct job experience

**ERROR-002:** Project Not Found
- **User Experience:** API returns 404 Not Found: `{"message": "Project not found", "code": "PROJECT_NOT_FOUND"}`
- **Recovery Path:** User navigates to project list

**ERROR-003:** Missing Required STAR Fields
- **User Experience:** API returns 400 Bad Request: `{"fields": [{"field": "situation", "message": "Situation is required"}, {"field": "task", "message": "Task is required"}]}`
- **Recovery Path:** User fills in missing STAR fields

**ERROR-004:** Invalid Skill Link (Skill Not in User's Profile)
- **User Experience:** API returns 400 Bad Request: `{"message": "Skill 'Docker' is not in your profile. Add it first.", "code": "SKILL_NOT_IN_PROFILE"}`
- **Recovery Path:** User adds skill to profile first, then links to project/story

**ERROR-005:** Concurrent Update Conflict
- **User Experience:** API returns 409 Conflict: `{"message": "This item was updated by another session. Please refresh and try again."}`
- **Recovery Path:** UI refreshes data, user reapplies changes

**ERROR-006:** STAR Field Too Long
- **User Experience:** API returns 400 Bad Request: `{"field": "action", "message": "Action text exceeds maximum length of 5000 characters"}`
- **Recovery Path:** User reduces text length (character counter shown in UI)

---

## Testing Scope

### Functional Testing

- **Project CRUD:** Create, read, update, soft-delete projects
- **Story CRUD:** Create, read, update, soft-delete stories with all STAR fields
- **Skill Linking:** Add/remove skills from projects and stories
- **Cascade Delete:** Delete project cascades to stories
- **Tenant Isolation:** Cross-tenant access returns 404
- **Visibility:** Private stories excluded from public API
- **JSONB Fields:** Store and retrieve metrics and tech_stack correctly
- **Validation:** Required fields, text length limits, STAR field requirements

### User Acceptance Testing

- **Complete Project Documentation:** Create job with 3 projects, each with 2 stories and 5 linked skills
- **STAR Story Quality:** Write 3 STAR stories for different interview scenarios
- **Visibility Controls:** Mark stories public/private, verify recruiter chat respects visibility
- **Edit and Refine:** Update metrics and outcomes after initial creation

### Edge Case Testing

- **Empty projects:** Project with no stories
- **Stories with no skills:** Story without linked skills
- **Very long STAR content:** 5000 character limits enforced
- **Empty metrics:** Project/story with no metrics (JSONB null)
- **Deleted job experience:** Project retrieval when parent job is deleted
- **Concurrent editing:** Two tabs editing same story

---

## Notes

Key design decisions:

- **Two-entity model:** ExperienceProject (structured metadata) + Story (narrative STAR format) provides both quantitative and qualitative career data
- **JSONB for metrics and tech stack:** Flexible structure accommodates varied project types without rigid schema
- **Join tables for skill linking:** Many-to-many relationships enable rich cross-referencing between skills, projects, and stories
- **Cascade soft delete:** Deleting a project cascades to stories, maintaining data integrity
- **STAR format enforced:** All four fields required ensures stories are interview-ready
- **Visibility at story level:** Granular control (some stories may contain sensitive business details)
- **Tenant isolation on join tables:** Even association records include tenant_id for defense-in-depth

Future enhancements:
- AI-generated STAR stories from project context
- Story quality scoring and improvement suggestions
- Story templates for common interview scenarios (scale, leadership, conflict resolution)
- Skills graph visualization (projects -> skills -> stories)
- Import projects from GitHub/Jira
- Embeddings for RAG-powered recruiter chat

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial specification       | Claude Code |
