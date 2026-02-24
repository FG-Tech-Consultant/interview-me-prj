# Tasks: Experience Projects & Stories (STAR Format)

**Feature ID:** 004-experience-stories
**Status:** Not Started
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Branch:** 004-experience-stories
**Spec:** [spec.md](./spec.md)
**Plan:** [plan.md](./plan.md)

---

## Task Status Legend

- `[ ]` Not Started
- `[P]` In Progress
- `[X]` Completed
- `[B]` Blocked (add blocker note)
- `[S]` Skipped (add reason)
- `[||]` Can run in parallel (different files, no dependencies)

---

## Phase 1: Database Schema & Entities (Day 1)

### Liquibase Migrations

- [ ] T001 [DATABASE] Create Liquibase migration for `experience_project` table
  - File: `backend/src/main/resources/db/changelog/20260224160000-create-experience-project-table.xml`
  - Table: experience_project (id BIGSERIAL PK, tenant_id BIGINT FK NOT NULL, job_experience_id BIGINT FK NOT NULL, title VARCHAR(255) NOT NULL, context TEXT, role VARCHAR(255), team_size INT, tech_stack JSONB, architecture_type VARCHAR(100), metrics JSONB, outcomes TEXT, visibility VARCHAR(20) DEFAULT 'private' NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, deleted_at TIMESTAMPTZ, version BIGINT DEFAULT 0 NOT NULL)
  - Foreign keys: tenant_id -> tenant(id), job_experience_id -> job_experience(id)
  - Indexes: idx_experience_project_tenant_id, idx_experience_project_job_experience_id, idx_experience_project_deleted_at
  - Rollback: `<dropTable tableName="experience_project"/>`

- [ ] T002 [DATABASE] Create Liquibase migration for `story` table
  - File: `backend/src/main/resources/db/changelog/20260224160100-create-story-table.xml`
  - Table: story (id BIGSERIAL PK, tenant_id BIGINT FK NOT NULL, experience_project_id BIGINT FK NOT NULL, title VARCHAR(255) NOT NULL, situation TEXT NOT NULL, task TEXT NOT NULL, action TEXT NOT NULL, result TEXT NOT NULL, metrics JSONB, visibility VARCHAR(20) DEFAULT 'private' NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, deleted_at TIMESTAMPTZ, version BIGINT DEFAULT 0 NOT NULL)
  - Foreign keys: tenant_id -> tenant(id), experience_project_id -> experience_project(id)
  - Indexes: idx_story_tenant_id, idx_story_experience_project_id, idx_story_deleted_at, idx_story_visibility
  - Rollback: `<dropTable tableName="story"/>`

- [ ] T003 [DATABASE] Create Liquibase migration for `experience_project_skill` join table
  - File: `backend/src/main/resources/db/changelog/20260224160200-create-experience-project-skill-table.xml`
  - Table: experience_project_skill (id BIGSERIAL PK, tenant_id BIGINT FK NOT NULL, experience_project_id BIGINT FK NOT NULL, user_skill_id BIGINT FK NOT NULL, created_at TIMESTAMPTZ NOT NULL)
  - Foreign keys: tenant_id -> tenant(id), experience_project_id -> experience_project(id), user_skill_id -> user_skill(id)
  - Unique index: idx_experience_project_skill_unique ON (experience_project_id, user_skill_id)
  - Index: idx_experience_project_skill_tenant_id
  - Rollback: `<dropTable tableName="experience_project_skill"/>`

- [ ] T004 [DATABASE] Create Liquibase migration for `story_skill` join table
  - File: `backend/src/main/resources/db/changelog/20260224160300-create-story-skill-table.xml`
  - Table: story_skill (id BIGSERIAL PK, tenant_id BIGINT FK NOT NULL, story_id BIGINT FK NOT NULL, user_skill_id BIGINT FK NOT NULL, created_at TIMESTAMPTZ NOT NULL)
  - Foreign keys: tenant_id -> tenant(id), story_id -> story(id), user_skill_id -> user_skill(id)
  - Unique index: idx_story_skill_unique ON (story_id, user_skill_id)
  - Index: idx_story_skill_tenant_id
  - Rollback: `<dropTable tableName="story_skill"/>`

- [ ] T005 [DATABASE] Add all migrations to db.changelog-master.yaml
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Add 4 include entries in order:
    - `db/changelog/20260224160000-create-experience-project-table.xml`
    - `db/changelog/20260224160100-create-story-table.xml`
    - `db/changelog/20260224160200-create-experience-project-skill-table.xml`
    - `db/changelog/20260224160300-create-story-skill-table.xml`

### JPA Entities

- [ ] T006 [||] [JAVA25] Create ExperienceProject entity
  - File: `backend/src/main/java/com/interviewme/experience/entity/ExperienceProject.java`
  - Annotations: @Entity, @Table(name = "experience_project"), @Data, @NoArgsConstructor, @AllArgsConstructor, @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id, tenantId, jobExperienceId, title, context, role, teamSize, techStack (List<String> JSONB), architectureType, metrics (Map<String, Object> JSONB), outcomes, visibility, createdAt (Instant), updatedAt (Instant), deletedAt (Instant), version
  - Relationship: @ManyToOne(fetch = LAZY) jobExperience (insertable=false, updatable=false)
  - Lifecycle: @PrePersist, @PreUpdate for timestamps
  - Follow existing JobExperience.java pattern (same package structure, Instant for timestamps)

- [ ] T007 [||] [JAVA25] Create Story entity
  - File: `backend/src/main/java/com/interviewme/experience/entity/Story.java`
  - Annotations: @Entity, @Table(name = "story"), @Data, @NoArgsConstructor, @AllArgsConstructor, @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id, tenantId, experienceProjectId, title, situation, task, action, result, metrics (Map<String, Object> JSONB), visibility, createdAt, updatedAt, deletedAt, version
  - Relationship: @ManyToOne(fetch = LAZY) experienceProject (insertable=false, updatable=false)
  - Lifecycle: @PrePersist, @PreUpdate

- [ ] T008 [||] [JAVA25] Create ExperienceProjectSkill entity
  - File: `backend/src/main/java/com/interviewme/experience/entity/ExperienceProjectSkill.java`
  - Annotations: @Entity, @Table(name = "experience_project_skill"), @Data, @NoArgsConstructor, @AllArgsConstructor
  - Fields: id, tenantId, experienceProjectId, userSkillId, createdAt
  - Lifecycle: @PrePersist for createdAt

- [ ] T009 [||] [JAVA25] Create StorySkill entity
  - File: `backend/src/main/java/com/interviewme/experience/entity/StorySkill.java`
  - Annotations: @Entity, @Table(name = "story_skill"), @Data, @NoArgsConstructor, @AllArgsConstructor
  - Fields: id, tenantId, storyId, userSkillId, createdAt
  - Lifecycle: @PrePersist for createdAt

### Validation

- [ ] T010 [DATABASE] Test migrations on fresh PostgreSQL database
  - Run `./gradlew bootRun` and verify all 4 tables created
  - Verify indexes, foreign keys, and constraints
  - Verify JSONB columns work correctly
  - Test rollback for each migration

---

## Phase 2: DTOs, Mappers & Repositories (Day 2)

### DTOs (Java Records)

- [ ] T011 [||] [JAVA25] Create SkillSummary DTO
  - File: `backend/src/main/java/com/interviewme/experience/dto/SkillSummary.java`
  - Record: SkillSummary(Long userSkillId, String skillName, String category)
  - Lightweight skill representation for embedding in project/story responses

- [ ] T012 [||] [JAVA25] Create ExperienceProject DTOs
  - Files in `backend/src/main/java/com/interviewme/experience/dto/`:
  - `CreateProjectRequest.java`: record with @NotBlank title, context, role, teamSize, techStack (List<String>), architectureType, metrics (Map), outcomes, visibility, skillIds (List<Long>)
  - `UpdateProjectRequest.java`: same fields
  - `ProjectResponse.java`: record with all fields + List<SkillSummary> skills + int storyCount + timestamps

- [ ] T013 [||] [JAVA25] Create Story DTOs
  - Files in `backend/src/main/java/com/interviewme/experience/dto/`:
  - `CreateStoryRequest.java`: record with @NotBlank title, @NotBlank situation, @NotBlank task, @NotBlank action, @NotBlank result, metrics (Map), visibility, skillIds (List<Long>)
  - `UpdateStoryRequest.java`: same fields
  - `StoryResponse.java`: record with all fields + List<SkillSummary> skills + timestamps

### Mappers

- [ ] T014 [||] [SIMPLE] Create ExperienceProjectMapper
  - File: `backend/src/main/java/com/interviewme/experience/mapper/ExperienceProjectMapper.java`
  - Static methods: toEntity(CreateProjectRequest), toResponse(ExperienceProject, List<SkillSummary>, int storyCount), updateEntity(ExperienceProject, UpdateProjectRequest)

- [ ] T015 [||] [SIMPLE] Create StoryMapper
  - File: `backend/src/main/java/com/interviewme/experience/mapper/StoryMapper.java`
  - Static methods: toEntity(CreateStoryRequest), toResponse(Story, List<SkillSummary>), updateEntity(Story, UpdateStoryRequest)

### Repositories

- [ ] T016 [||] [DATA] Create ExperienceProjectRepository
  - File: `backend/src/main/java/com/interviewme/experience/repository/ExperienceProjectRepository.java`
  - Extends: JpaRepository<ExperienceProject, Long>
  - Query methods:
    - `List<ExperienceProject> findByJobExperienceIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long jobExperienceId)`
    - `Optional<ExperienceProject> findByIdAndDeletedAtIsNull(Long id)`
    - `List<ExperienceProject> findByJobExperienceIdInAndDeletedAtIsNull(List<Long> jobExperienceIds)` (for profile-level queries)

- [ ] T017 [||] [DATA] Create StoryRepository
  - File: `backend/src/main/java/com/interviewme/experience/repository/StoryRepository.java`
  - Extends: JpaRepository<Story, Long>
  - Query methods:
    - `List<Story> findByExperienceProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long projectId)`
    - `Optional<Story> findByIdAndDeletedAtIsNull(Long id)`
    - `List<Story> findByExperienceProjectIdInAndDeletedAtIsNull(List<Long> projectIds)` (for cascade operations)
    - `@Query` for finding public stories by profile: join through experience_project -> job_experience -> profile, filter visibility='public' AND deleted_at IS NULL

- [ ] T018 [||] [DATA] Create ExperienceProjectSkillRepository
  - File: `backend/src/main/java/com/interviewme/experience/repository/ExperienceProjectSkillRepository.java`
  - Extends: JpaRepository<ExperienceProjectSkill, Long>
  - Query methods:
    - `List<ExperienceProjectSkill> findByExperienceProjectId(Long projectId)`
    - `Optional<ExperienceProjectSkill> findByExperienceProjectIdAndUserSkillId(Long projectId, Long userSkillId)`
    - `void deleteByExperienceProjectId(Long projectId)` (for cascade delete)

- [ ] T019 [||] [DATA] Create StorySkillRepository
  - File: `backend/src/main/java/com/interviewme/experience/repository/StorySkillRepository.java`
  - Extends: JpaRepository<StorySkill, Long>
  - Query methods:
    - `List<StorySkill> findByStoryId(Long storyId)`
    - `Optional<StorySkill> findByStoryIdAndUserSkillId(Long storyId, Long userSkillId)`
    - `void deleteByStoryId(Long storyId)` (for cascade delete)
    - `List<StorySkill> findByUserSkillId(Long userSkillId)` (find stories by skill)

---

## Phase 3: Services (Day 2-3)

### Business Logic

- [ ] T020 [MODULAR] Create ExperienceProjectService
  - File: `backend/src/main/java/com/interviewme/experience/service/ExperienceProjectService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: ExperienceProjectRepository, ExperienceProjectSkillRepository, StoryRepository, StorySkillRepository, JobExperienceRepository, UserSkillRepository, TenantContext
  - Methods:
    - `@Transactional ProjectResponse createProject(Long jobExperienceId, CreateProjectRequest dto)` - validate job exists, create project, link skills
    - `@Transactional ProjectResponse updateProject(Long projectId, UpdateProjectRequest dto)` - validate project exists, update fields, sync skills
    - `@Transactional void deleteProject(Long projectId)` - soft delete project + cascade soft delete stories + delete skill associations
    - `@Transactional(readOnly=true) List<ProjectResponse> getProjectsByJobExperience(Long jobExperienceId)` - list projects with skill summaries and story counts
    - `@Transactional(readOnly=true) ProjectResponse getProjectById(Long projectId)` - single project with skills and story count
    - `@Transactional void addSkillToProject(Long projectId, Long userSkillId)` - validate both exist, create association
    - `@Transactional void removeSkillFromProject(Long projectId, Long userSkillId)` - delete association
  - Validation: verify job experience belongs to current tenant, verify skills belong to same profile
  - Logging: log.info for all create/update/delete with projectId, tenantId
  - Max 350 lines

- [ ] T021 [MODULAR] Create StoryService
  - File: `backend/src/main/java/com/interviewme/experience/service/StoryService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: StoryRepository, StorySkillRepository, ExperienceProjectRepository, UserSkillRepository, TenantContext
  - Methods:
    - `@Transactional StoryResponse createStory(Long projectId, CreateStoryRequest dto)` - validate project exists, create story, link skills
    - `@Transactional StoryResponse updateStory(Long storyId, UpdateStoryRequest dto)` - validate story exists, update fields, sync skills
    - `@Transactional void deleteStory(Long storyId)` - soft delete story + delete skill associations
    - `@Transactional(readOnly=true) List<StoryResponse> getStoriesByProject(Long projectId)` - list stories with skills
    - `@Transactional(readOnly=true) StoryResponse getStoryById(Long storyId)` - single story with skills
    - `@Transactional(readOnly=true) List<StoryResponse> getPublicStoriesByProfile(Long profileId)` - all public stories for a profile (for recruiter chat context)
    - `@Transactional(readOnly=true) List<StoryResponse> getStoriesBySkill(Long userSkillId)` - stories linked to a skill
    - `@Transactional void addSkillToStory(Long storyId, Long userSkillId)` - validate both exist, create association
    - `@Transactional void removeSkillFromStory(Long storyId, Long userSkillId)` - delete association
  - Validation: verify project belongs to current tenant, verify skills belong to same profile, validate all STAR fields present
  - Logging: log.info for all create/update/delete with storyId, projectId, tenantId
  - Max 350 lines

---

## Phase 4: Controllers & Exception Handling (Day 3-4)

### Controllers

- [ ] T022 [MODULAR] Create ExperienceProjectController
  - File: `backend/src/main/java/com/interviewme/experience/controller/ExperienceProjectController.java`
  - Annotations: @RestController, @RequiredArgsConstructor, @Slf4j
  - Endpoints:
    - `GET /api/v1/jobs/{jobId}/projects` -> getProjectsByJob(jobId) -> 200 List<ProjectResponse>
    - `POST /api/v1/jobs/{jobId}/projects` -> createProject(jobId, @Valid CreateProjectRequest) -> 201 ProjectResponse
    - `GET /api/v1/projects/{projectId}` -> getProject(projectId) -> 200 ProjectResponse
    - `PUT /api/v1/projects/{projectId}` -> updateProject(projectId, @Valid UpdateProjectRequest) -> 200 ProjectResponse
    - `DELETE /api/v1/projects/{projectId}` -> deleteProject(projectId) -> 204
    - `POST /api/v1/projects/{projectId}/skills/{userSkillId}` -> addSkillToProject(projectId, userSkillId) -> 204
    - `DELETE /api/v1/projects/{projectId}/skills/{userSkillId}` -> removeSkillFromProject(projectId, userSkillId) -> 204
  - All endpoints require JWT auth
  - Max 200 lines

- [ ] T023 [MODULAR] Create StoryController
  - File: `backend/src/main/java/com/interviewme/experience/controller/StoryController.java`
  - Annotations: @RestController, @RequiredArgsConstructor, @Slf4j
  - Endpoints:
    - `GET /api/v1/projects/{projectId}/stories` -> getStoriesByProject(projectId) -> 200 List<StoryResponse>
    - `POST /api/v1/projects/{projectId}/stories` -> createStory(projectId, @Valid CreateStoryRequest) -> 201 StoryResponse
    - `GET /api/v1/stories/{storyId}` -> getStory(storyId) -> 200 StoryResponse
    - `PUT /api/v1/stories/{storyId}` -> updateStory(storyId, @Valid UpdateStoryRequest) -> 200 StoryResponse
    - `DELETE /api/v1/stories/{storyId}` -> deleteStory(storyId) -> 204
    - `POST /api/v1/stories/{storyId}/skills/{userSkillId}` -> addSkillToStory(storyId, userSkillId) -> 204
    - `DELETE /api/v1/stories/{storyId}/skills/{userSkillId}` -> removeSkillFromStory(storyId, userSkillId) -> 204
    - `GET /api/v1/profiles/{profileId}/stories/public` -> getPublicStories(profileId) -> 200 List<StoryResponse>
  - All endpoints require JWT auth
  - Max 200 lines

### Exception Handling

- [ ] T024 [SIMPLE] Add exception handlers to GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/config/GlobalExceptionHandler.java` (existing)
  - Add handlers for:
    - ProjectNotFoundException -> 404
    - StoryNotFoundException -> 404
    - SkillNotInProfileException -> 400 ("Skill is not in your profile. Add it first.")
  - Create exception classes in `backend/src/main/java/com/interviewme/common/exception/`

---

## Phase 5: Backend Testing (Day 4)

### Unit Tests

- [ ] T025 [||] [JAVA25] Write ExperienceProjectService unit tests
  - File: `backend/src/test/java/com/interviewme/experience/service/ExperienceProjectServiceTest.java`
  - Use @ExtendWith(MockitoExtension.class)
  - Tests:
    - createProject_shouldSaveWithTenantId
    - createProject_shouldLinkSkills
    - createProject_shouldRejectInvalidJobExperience
    - updateProject_shouldSyncSkills
    - deleteProject_shouldCascadeSoftDeleteToStories
    - addSkillToProject_shouldRejectSkillNotInProfile

- [ ] T026 [||] [JAVA25] Write StoryService unit tests
  - File: `backend/src/test/java/com/interviewme/experience/service/StoryServiceTest.java`
  - Tests:
    - createStory_shouldRequireAllStarFields
    - createStory_shouldLinkSkills
    - createStory_shouldRejectInvalidProject
    - deleteStory_shouldSetDeletedAt
    - getPublicStories_shouldFilterByVisibility
    - addSkillToStory_shouldRejectSkillNotInProfile

### Integration Tests

- [ ] T027 [SIMPLE] Write ExperienceProjectController integration tests
  - File: `backend/src/test/java/com/interviewme/experience/controller/ExperienceProjectControllerTest.java`
  - Use @SpringBootTest, @AutoConfigureMockMvc
  - Tests:
    - POST /api/v1/jobs/{jobId}/projects -> 201 Created
    - POST with missing title -> 400 Bad Request
    - GET /api/v1/projects/{id} -> 200 OK
    - PUT /api/v1/projects/{id} -> 200 OK
    - DELETE /api/v1/projects/{id} -> 204 No Content, verify cascade
    - Cross-tenant access -> 404

- [ ] T028 [SIMPLE] Write StoryController integration tests
  - File: `backend/src/test/java/com/interviewme/experience/controller/StoryControllerTest.java`
  - Tests:
    - POST /api/v1/projects/{projectId}/stories -> 201 Created with all STAR fields
    - POST with missing situation -> 400 Bad Request
    - POST with missing task -> 400 Bad Request
    - GET /api/v1/profiles/{profileId}/stories/public -> only public stories
    - DELETE -> 204 No Content
    - Skill linking/unlinking -> 204

- [ ] T029 [SIMPLE] Write tenant isolation tests
  - File: `backend/src/test/java/com/interviewme/experience/TenantIsolationTest.java`
  - Test: Tenant A creates project, Tenant B cannot access -> 404
  - Test: Tenant A creates story, Tenant B cannot access -> 404
  - Test: Skill linking across tenants rejected

---

## Phase 6: Frontend - TypeScript Types & API (Day 5)

### TypeScript Types

- [ ] T030 [||] [SIMPLE] Create ExperienceProject TypeScript types
  - File: `frontend/src/types/experienceProject.ts`
  - Interfaces: CreateProjectRequest, UpdateProjectRequest, ProjectResponse, SkillSummary

- [ ] T031 [||] [SIMPLE] Create Story TypeScript types
  - File: `frontend/src/types/story.ts`
  - Interfaces: CreateStoryRequest, UpdateStoryRequest, StoryResponse

### API Client

- [ ] T032 [MODULAR] Create experience API client
  - File: `frontend/src/api/experienceApi.ts`
  - Project functions: getProjectsByJob, createProject, getProject, updateProject, deleteProject, addSkillToProject, removeSkillFromProject
  - Story functions: getStoriesByProject, createStory, getStory, updateStory, deleteStory, addSkillToStory, removeSkillFromStory, getPublicStoriesByProfile
  - Use axios with JWT interceptor
  - Max 250 lines

### React Query Hooks

- [ ] T033 [||] [MODULAR] Create useProjects hook
  - File: `frontend/src/hooks/useProjects.ts`
  - Hooks: useProjects(jobId), useProject(projectId), useCreateProject(), useUpdateProject(), useDeleteProject()
  - Query keys: ['projects', jobId]
  - Invalidation on mutations

- [ ] T034 [||] [MODULAR] Create useStories hook
  - File: `frontend/src/hooks/useStories.ts`
  - Hooks: useStories(projectId), useStory(storyId), useCreateStory(), useUpdateStory(), useDeleteStory(), usePublicStories(profileId)
  - Query keys: ['stories', projectId]
  - Invalidation on mutations

---

## Phase 7: Frontend - React Components (Day 5-6)

### Shared Components

- [ ] T035 [MODULAR] Create MetricsEditor component
  - File: `frontend/src/components/experience/MetricsEditor.tsx`
  - Reusable key-value editor for metrics
  - Props: value (Record<string, any>), onChange (fn)
  - "Add Metric" button adds new row with key/value TextFields
  - Delete button on each row
  - Max 150 lines

### Project Components

- [ ] T036 [MODULAR] Create ProjectCard component
  - File: `frontend/src/components/experience/ProjectCard.tsx`
  - Props: project (ProjectResponse), onEdit (fn), onDelete (fn)
  - Displays: title, role, team_size, tech_stack (chips), architecture_type, metrics summary, visibility badge, story count
  - Edit/delete icons
  - Collapsible details section with context and outcomes
  - Max 150 lines

- [ ] T037 [MODULAR] Create ProjectFormDialog component
  - File: `frontend/src/components/experience/ProjectFormDialog.tsx`
  - Props: open, onClose, project (for edit mode), jobExperienceId
  - Form fields: title (TextField), context (multiline), role (TextField), team_size (number), tech_stack (Autocomplete freeSolo chips), architecture_type (Select), MetricsEditor, outcomes (multiline), visibility toggle
  - Skill linking: multi-select from user's UserSkills (Autocomplete)
  - Inline validation, save/loading state
  - Mode: 'add' or 'edit'
  - Max 250 lines

- [ ] T038 [MODULAR] Create ProjectList component
  - File: `frontend/src/components/experience/ProjectList.tsx`
  - Props: jobExperienceId
  - Uses useProjects(jobExperienceId) hook
  - Renders ProjectCard for each project
  - "Add Project" button opens ProjectFormDialog
  - Empty state: "No projects yet. Add your first project to document your experience."
  - Confirmation dialog for delete
  - Max 150 lines

### Story Components

- [ ] T039 [MODULAR] Create StoryCard component
  - File: `frontend/src/components/experience/StoryCard.tsx`
  - Props: story (StoryResponse), onEdit (fn), onDelete (fn)
  - Displays: title, STAR preview (first 100 chars of each field), linked skills (chips), visibility badge, metrics summary
  - Expandable STAR sections (click to see full text)
  - Edit/delete icons
  - Max 200 lines

- [ ] T040 [MODULAR] Create StoryFormDialog component
  - File: `frontend/src/components/experience/StoryFormDialog.tsx`
  - Props: open, onClose, story (for edit mode), experienceProjectId
  - Form fields:
    - Title (TextField, required)
    - Situation (multiline TextField, required, max 5000, with helper text "Set the scene. What was the context?")
    - Task (multiline TextField, required, max 5000, with helper text "What was your specific responsibility?")
    - Action (multiline TextField, required, max 5000, with helper text "What steps did you take?")
    - Result (multiline TextField, required, max 5000, with helper text "What was the outcome? Include metrics.")
    - MetricsEditor for impact quantification
    - Skill linking: multi-select from user's UserSkills
    - Visibility toggle
  - Character counter for each STAR field
  - Mode: 'add' or 'edit'
  - Max 300 lines

- [ ] T041 [MODULAR] Create StoryList component
  - File: `frontend/src/components/experience/StoryList.tsx`
  - Props: experienceProjectId
  - Uses useStories(experienceProjectId) hook
  - Renders StoryCard for each story
  - "Add Story" button opens StoryFormDialog
  - Empty state: "No stories yet. Add a STAR story to prepare for behavioral interviews."
  - Confirmation dialog for delete
  - Max 150 lines

### Page Integration

- [ ] T042 [MODULAR] Integrate ProjectList into job experience display
  - File: modify existing job experience card/page component
  - Add ProjectList component nested under each job experience
  - Collapsible section: "Projects (N)" where N is project count
  - Projects section loads lazily (only when expanded)

- [ ] T043 [MODULAR] Integrate StoryList into ProjectCard
  - Embed StoryList within each ProjectCard (collapsible)
  - "Stories (N)" label showing story count
  - Load stories lazily when project card is expanded

---

## Checkpoints

After each phase, verify:

- [ ] **Phase 1 Complete:** All 4 migrations run, tables created with correct schema, entities compile and map to tables
- [ ] **Phase 2 Complete:** DTOs validate correctly, mappers convert between entities and DTOs, repositories compile
- [ ] **Phase 3 Complete:** Services handle CRUD + cascade delete + skill linking, logging captures operations
- [ ] **Phase 4 Complete:** All endpoints return correct HTTP status codes, validation works, exception handlers in place
- [ ] **Phase 5 Complete:** Unit tests pass, integration tests pass, tenant isolation verified
- [ ] **Phase 6 Complete:** API client compiles, hooks work with React Query
- [ ] **Phase 7 Complete:** Project/story forms work, STAR fields validated, skill linking functional, metrics editor works

---

## Success Criteria Verification

### From spec.md

1. **Complete CRUD Operations**
   - [ ] Integration tests cover all project and story CRUD endpoints
   - [ ] Manual test: Create job -> add project -> add story -> edit -> delete

2. **STAR Format Integrity**
   - [ ] Test: Submit story missing "situation" -> 400 Bad Request
   - [ ] Test: Submit story missing "task" -> 400 Bad Request
   - [ ] All 4 STAR fields required

3. **Skill Linking**
   - [ ] Test: Link 5 skills to project, verify associations persist
   - [ ] Test: Link 3 skills to story, verify in response
   - [ ] Test: Attempt to link skill not in profile -> 400 error

4. **Tenant Isolation**
   - [ ] Test: Cross-tenant project access -> 404
   - [ ] Test: Cross-tenant story access -> 404

5. **Visibility**
   - [ ] Test: Create public + private stories, GET /public -> only public returned
   - [ ] Private stories excluded from recruiter chat context endpoint

6. **Cascade Soft Delete**
   - [ ] Test: Delete project with 3 stories -> all 4 deleted_at set
   - [ ] Skill associations cleaned up on project delete

7. **JSONB Metrics**
   - [ ] Test: Store {"tps": 15000, "latency": "45ms"} -> retrieve correctly
   - [ ] Test: Store empty metrics (null) -> works

---

## Notes

### Key Dependencies (Execution Order)
- Phase 1 (DB) must complete before Phase 2 (DTOs/Repos)
- Phase 2 must complete before Phase 3 (Services)
- Phase 3 must complete before Phase 4 (Controllers)
- Phase 6 (Frontend types/API) can start as soon as API contracts are defined (Phase 4)
- Phase 7 (Frontend components) depends on Phase 6

### Parallel Execution Opportunities
- Within Phase 1: T001-T004 migrations can be written in parallel, T006-T009 entities in parallel
- Within Phase 2: All DTOs, mappers, and repositories in parallel
- Within Phase 5: Unit tests and integration tests can be written in parallel
- Within Phase 6: TypeScript types and API client in parallel
- Within Phase 7: Project and story components in parallel

### Risk Mitigation
- **Cascade delete:** Test thoroughly with database verification
- **Skill linking validation:** Ensure skills belong to same profile/tenant
- **JSONB handling:** Test with varied metric formats early
- **Frontend nesting:** Use collapsible sections and lazy loading to avoid UI performance issues

---

**Tasks Version:** 1.0.0
**Last Updated:** 2026-02-24
**Total Tasks:** 43
**Estimated Time:** 5-6 days
