# Tasks: Skills Management - Catalog and User Skills CRUD

**Feature ID:** 003-skills-management
**Status:** Not Started
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Branch:** 003-skills-management
**Spec:** [spec.md](./spec.md)
**Plan:** [plan.md](./plan.md)

---

## Task Status Legend

- `[ ]` Not Started
- `[P]` In Progress
- `[X]` Completed
- `[B]` Blocked (add blocker note)
- `[S]` Skipped (add reason)
- `[P]` Can run in parallel (different files, no dependencies)
- `[US-N]` Belongs to User Story N from spec.md

---

## Phase 1: Database Foundation (Day 1)

### Database Schema Migrations

- [ ] T001 [DATABASE] Create Liquibase migration for `skill` table
  - File: `backend/src/main/resources/db/changelog/20260224143000-create-skill-table.xml`
  - Table: skill (id, name UNIQUE, category, description, tags JSONB, is_active, created_at, updated_at)
  - Indexes: idx_skill_name (unique), idx_skill_category, idx_skill_is_active
  - Rollback: <dropTable tableName="skill"/>
  - **Reference:** plan.md Database Schema section

- [ ] T002 [DATABASE] Create Liquibase migration for `user_skill` table
  - File: `backend/src/main/resources/db/changelog/20260224143500-create-user-skill-table.xml`
  - Table: user_skill (id, tenant_id FK, profile_id FK, skill_id FK, years_of_experience, proficiency_depth, last_used_date, confidence_level, tags JSONB, visibility, created_at, updated_at, deleted_at, version)
  - Indexes: idx_user_skill_tenant_id, idx_user_skill_profile_id, idx_user_skill_skill_id, idx_user_skill_deleted_at
  - Unique constraint: (tenant_id, profile_id, skill_id) WHERE deleted_at IS NULL
  - Check constraints: proficiency_depth BETWEEN 1 AND 5, years_of_experience >= 0
  - Rollback: <dropTable tableName="user_skill"/>
  - **Reference:** plan.md Database Schema section, data-model.md

- [ ] T003 [DATABASE] Add migrations to db.changelog-master.yaml
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Add include entries for both new migrations (in order)
  - **Reference:** plan.md Phase 1 tasks

### JPA Entities

- [ ] T004 [DATA][JAVA21] Create Skill JPA entity
  - File: `backend/src/main/java/com/interviewme/skills/entity/Skill.java`
  - Annotations: @Entity, @Table(name = "skill"), @Getter, @Setter, @NoArgsConstructor
  - Fields: id (Long), name (String), category (String), description (String), tags (List<String> JSONB), isActive (Boolean), createdAt, updatedAt
  - JSONB mapping: Use Hypersistence Utils @Type(JsonBinaryType.class)
  - Lifecycle hooks: @PrePersist, @PreUpdate for timestamps
  - **Reference:** plan.md Data Model Implementation, data-model.md Skill entity

- [ ] T005 [DATA][JAVA21] Create UserSkill JPA entity
  - File: `backend/src/main/java/com/interviewme/skills/entity/UserSkill.java`
  - Annotations: @Entity, @Table(name = "user_skill"), @Getter, @Setter, @NoArgsConstructor
  - Fields: id, tenantId, profile (@ManyToOne), skill (@ManyToOne EAGER), yearsOfExperience, proficiencyDepth, lastUsedDate, confidenceLevel, tags (JSONB), visibility, createdAt, updatedAt, deletedAt, version (@Version)
  - Lifecycle hooks: @PrePersist, @PreUpdate for timestamps
  - **Reference:** plan.md Data Model Implementation, data-model.md UserSkill entity

### Validation

- [ ] T006 [DATA] Test migrations on fresh PostgreSQL database
  - Drop existing database if exists: `DROP DATABASE interviewme;`
  - Create fresh database: `CREATE DATABASE interviewme;`
  - Run Spring Boot application (Liquibase auto-runs migrations)
  - Verify schema: `psql -d interviewme -c "\d skill"` and `psql -d interviewme -c "\d user_skill"`
  - Verify indexes: `\di` in psql
  - Verify constraints: Check proficiency 1-5, years >= 0, unique constraint on (tenant_id, profile_id, skill_id)
  - **Reference:** plan.md Phase 1 Validation checkpoints

---

## Phase 2: Backend - Repository and Service Layers (Day 2)

### Repositories

- [ ] T007 [P][DATA] Create SkillRepository
  - File: `backend/src/main/java/com/interviewme/skills/repository/SkillRepository.java`
  - Extends: JpaRepository<Skill, Long>
  - Query methods:
    - `List<Skill> findTop10ByNameContainingIgnoreCaseAndIsActiveTrueOrderByName(String query)`
    - `List<Skill> findByIsActiveTrue()`
    - `Optional<Skill> findByNameIgnoreCase(String name)`
  - **Reference:** plan.md Service Decomposition

- [ ] T008 [P][DATA] Create UserSkillRepository
  - File: `backend/src/main/java/com/interviewme/skills/repository/UserSkillRepository.java`
  - Extends: JpaRepository<UserSkill, Long>
  - Query methods:
    - `List<UserSkill> findByProfileIdAndDeletedAtIsNull(Long profileId)`
    - `Optional<UserSkill> findByIdAndDeletedAtIsNull(Long id)`
    - `List<UserSkill> findByProfileIdAndVisibilityAndDeletedAtIsNull(Long profileId, String visibility)`
  - Note: Tenant filter applied automatically via existing Hibernate filter from Feature 001
  - **Reference:** plan.md Service Decomposition

### DTOs (Java Records)

- [ ] T009 [P][JAVA21] Create SkillDto record
  - File: `backend/src/main/java/com/interviewme/skills/dto/SkillDto.java`
  - Fields: Long id, String name, String category, String description, List<String> tags, Boolean isActive
  - **Reference:** plan.md DTOs section

- [ ] T010 [P][JAVA21] Create CreateSkillDto record (admin)
  - File: `backend/src/main/java/com/interviewme/skills/dto/CreateSkillDto.java`
  - Fields: @NotBlank @Size(max=255) String name, @NotBlank @Size(max=100) String category, @Size(max=2000) String description, @Size(max=20) List<String> tags
  - **Reference:** plan.md DTOs section

- [ ] T011 [P][JAVA21] Create UserSkillDto record
  - File: `backend/src/main/java/com/interviewme/skills/dto/UserSkillDto.java`
  - Fields: Long id, SkillDto skill, Integer yearsOfExperience, Integer proficiencyDepth, LocalDate lastUsedDate, String confidenceLevel, List<String> tags, String visibility, LocalDateTime createdAt, LocalDateTime updatedAt
  - **Reference:** plan.md DTOs section

- [ ] T012 [P][JAVA21] Create AddUserSkillDto record
  - File: `backend/src/main/java/com/interviewme/skills/dto/AddUserSkillDto.java`
  - Fields: @NotNull Long skillId, @Min(0) @Max(70) Integer yearsOfExperience, @NotNull @Min(1) @Max(5) Integer proficiencyDepth, @Past LocalDate lastUsedDate, @NotNull String confidenceLevel, @Size(max=20) List<String> tags, @NotNull String visibility
  - **Reference:** plan.md DTOs section

- [ ] T013 [P][JAVA21] Create UpdateUserSkillDto record
  - File: `backend/src/main/java/com/interviewme/skills/dto/UpdateUserSkillDto.java`
  - Fields: @Min(0) @Max(70) Integer yearsOfExperience, @Min(1) @Max(5) Integer proficiencyDepth, @Past LocalDate lastUsedDate, String confidenceLevel, @Size(max=20) List<String> tags, String visibility
  - **Reference:** plan.md DTOs section

### Business Logic Services

- [ ] T014 [MODULAR] Create SkillService (catalog management)
  - File: `backend/src/main/java/com/interviewme/skills/service/SkillService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: SkillRepository
  - Methods:
    - `List<SkillDto> searchActive(String query)` - Autocomplete (top 10 matches)
    - `SkillDto createSkill(CreateSkillDto dto)` - Admin creates catalog skill
    - `SkillDto updateSkill(Long id, UpdateSkillDto dto)` - Admin updates skill
    - `SkillDto deactivateSkill(Long id)` - Admin soft-deactivates skill
    - `SkillDto reactivateSkill(Long id)` - Admin reactivates skill
    - `SkillDto findById(Long id)` - Get skill by ID
    - `List<SkillDto> findAll()` - List all (admin only)
  - Max 300 lines
  - **Reference:** plan.md Service Decomposition

- [ ] T015 [MODULAR] Create UserSkillService (user skill CRUD)
  - File: `backend/src/main/java/com/interviewme/skills/service/UserSkillService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: UserSkillRepository, SkillRepository, ProfileRepository, TenantContext
  - Methods:
    - `UserSkillDto addSkill(Long profileId, AddUserSkillDto dto)` - User adds skill
    - `UserSkillDto updateSkill(Long userSkillId, UpdateUserSkillDto dto)` - User updates metadata
    - `void deleteSkill(Long userSkillId)` - User soft-deletes skill
    - `Map<String, List<UserSkillDto>> getSkillsByProfile(Long profileId)` - Grouped by category
    - `UserSkillDto getSkillById(Long userSkillId)` - Get single user skill
    - `List<UserSkillDto> getPublicSkillsByProfile(Long profileId)` - Only public skills
  - Max 400 lines
  - **Reference:** plan.md Service Decomposition

### Logging

- [ ] T016 [OBSERV] Add structured logging to SkillService
  - Log skill catalog operations: create, update, deactivate with admin username
  - Log format: JSON with fields: timestamp, level, logger, message, adminUserId, skillId, action
  - Redaction: No sensitive data in skill names or tags
  - **Reference:** plan.md Constitution Principle 6

- [ ] T017 [OBSERV] Add structured logging to UserSkillService
  - Log user skill operations: add, update, delete with tenantId, userId, skillId
  - Log public/private visibility changes
  - Log duplicate skill prevention (unique constraint violations)
  - **Reference:** plan.md Constitution Principle 6

---

## Phase 3: Backend - REST API Layer (Day 3)

### Controllers

- [ ] T018 [MODULAR] Create SkillController (catalog endpoints)
  - File: `backend/src/main/java/com/interviewme/skills/controller/SkillController.java`
  - Annotations: @RestController, @RequestMapping("/api/v1/skills"), @RequiredArgsConstructor, @Slf4j
  - Endpoints:
    - `GET /catalog/search?q={query}` - Autocomplete search (all users)
    - `GET /catalog` - List all skills (@PreAuthorize("hasRole('ADMIN')"))
    - `POST /catalog` - Create skill (@PreAuthorize("hasRole('ADMIN')"), @Valid CreateSkillDto)
    - `PUT /catalog/{id}` - Update skill (@PreAuthorize("hasRole('ADMIN')"), @Valid UpdateSkillDto)
    - `POST /catalog/{id}/deactivate` - Deactivate skill (@PreAuthorize("hasRole('ADMIN')"))
    - `POST /catalog/{id}/reactivate` - Reactivate skill (@PreAuthorize("hasRole('ADMIN')"))
  - Max 200 lines
  - **Reference:** plan.md API Implementation

- [ ] T019 [MODULAR] Create UserSkillController (user skill endpoints)
  - File: `backend/src/main/java/com/interviewme/skills/controller/UserSkillController.java`
  - Annotations: @RestController, @RequestMapping("/api/v1/skills/user"), @RequiredArgsConstructor, @Slf4j
  - Endpoints:
    - `GET /` - Get user skills grouped by category
    - `POST /` - Add user skill (@Valid AddUserSkillDto)
    - `GET /{id}` - Get single user skill
    - `PUT /{id}` - Update user skill (@Valid UpdateUserSkillDto)
    - `DELETE /{id}` - Soft delete user skill (returns 204 No Content)
  - Max 200 lines
  - **Reference:** plan.md API Implementation

### Transaction Annotations

- [ ] T020 [DATA] Add @Transactional annotations to service methods
  - SkillService: @Transactional(readOnly = true) for searchActive, findById, findAll
  - SkillService: @Transactional for createSkill, updateSkill, deactivateSkill, reactivateSkill
  - UserSkillService: @Transactional(readOnly = true) for getSkillsByProfile, getSkillById, getPublicSkillsByProfile
  - UserSkillService: @Transactional for addSkill, updateSkill, deleteSkill
  - **Reference:** plan.md Constitution Principle 7, CLAUDE.md Spring/JPA guidelines

### Exception Handling

- [ ] T021 [SIMPLE] Add exception handlers to GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/common/exception/GlobalExceptionHandler.java`
  - Handle SkillNotFoundException → 404 Not Found
  - Handle DuplicateSkillException → 409 Conflict with message "You already have 'Java' in your profile"
  - Handle ValidationException → 400 Bad Request with field-specific errors
  - Handle OptimisticLockException → 409 Conflict with message "Resource was updated by another session"
  - **Reference:** plan.md API Implementation, spec.md Error Handling

### Integration Tests

- [ ] T022 [SIMPLE] Write integration tests for SkillController
  - File: `backend/src/test/java/com/interviewme/skills/controller/SkillControllerTest.java`
  - Test autocomplete: GET /catalog/search returns top 10 matches
  - Test admin create: POST /catalog creates skill (requires ADMIN role)
  - Test admin update: PUT /catalog/{id} updates skill
  - Test admin deactivate: POST /catalog/{id}/deactivate sets is_active=false
  - Test non-admin access: 403 Forbidden for admin endpoints
  - Use @SpringBootTest, @AutoConfigureMockMvc, mock JWT authentication
  - **Reference:** plan.md Testing Strategy

- [ ] T023 [SIMPLE] Write integration tests for UserSkillController
  - File: `backend/src/test/java/com/interviewme/skills/controller/UserSkillControllerTest.java`
  - Test add skill: POST /user creates user skill
  - Test duplicate prevention: POST /user with same skill_id returns 409 Conflict
  - Test update skill: PUT /user/{id} updates proficiency and metadata
  - Test delete skill: DELETE /user/{id} sets deleted_at timestamp
  - Test tenant isolation: Cross-tenant access returns 404 Not Found
  - Test validation: Invalid proficiency depth (7) returns 400 Bad Request
  - Use mock TenantContext to simulate different tenants
  - **Reference:** plan.md Testing Strategy, spec.md Success Criteria

---

## Phase 4: Frontend - React Components (Day 4-5)

### API Client

- [ ] T024 [P][MODULAR] Create skillsApi client module
  - File: `frontend/src/api/skillsApi.ts`
  - Functions:
    - `searchCatalog(query: string): Promise<SkillDto[]>` - GET /catalog/search
    - `getUserSkills(): Promise<Map<string, UserSkillDto[]>>` - GET /user
    - `addUserSkill(dto: AddUserSkillDto): Promise<UserSkillDto>` - POST /user
    - `updateUserSkill(id: number, dto: UpdateUserSkillDto): Promise<UserSkillDto>` - PUT /user/{id}
    - `deleteUserSkill(id: number): Promise<void>` - DELETE /user/{id}
  - Use axios with auth headers from existing authUtils
  - Max 200 lines
  - **Reference:** plan.md Service Decomposition Frontend

### React Query Hooks

- [ ] T025 [P][MODULAR] Create useSkills hook
  - File: `frontend/src/hooks/useSkills.ts`
  - Hooks:
    - `useUserSkills()` - Query for user skills (grouped by category)
    - `useAddUserSkill()` - Mutation for adding skill
    - `useUpdateUserSkill()` - Mutation for updating skill
    - `useDeleteUserSkill()` - Mutation for deleting skill (soft delete)
  - React Query configuration: staleTime, cacheTime, optimistic updates
  - Max 150 lines
  - **Reference:** plan.md Service Decomposition Frontend

- [ ] T026 [P][MODULAR] Create useSkillCatalog hook
  - File: `frontend/src/hooks/useSkillCatalog.ts`
  - Hooks:
    - `useSkillCatalogSearch(query: string)` - Debounced autocomplete search
  - Debounce: 300ms to reduce API calls
  - Max 100 lines
  - **Reference:** plan.md Service Decomposition Frontend

### UI Components

- [ ] T027 [MODULAR] Create SkillCard component
  - File: `frontend/src/components/SkillCard.tsx`
  - Props: skill (UserSkillDto), onEdit (fn), onDelete (fn)
  - Displays: skill name, proficiency stars (1-5), years of experience, last used (formatted "Jan 2026"), tags (chips), visibility badge (public/private icon)
  - Color-coded proficiency: 1-2 (yellow), 3 (blue), 4-5 (green)
  - Edit and delete icon buttons
  - Max 100 lines
  - **Reference:** plan.md Service Decomposition Frontend, spec.md REQ-UI-001

- [ ] T028 [MODULAR] Create SkillSelector autocomplete component
  - File: `frontend/src/components/SkillSelector.tsx`
  - Props: value (Skill | null), onChange (fn), error (string | null)
  - Uses MUI Autocomplete with useSkillCatalogSearch hook (debounced 300ms)
  - Displays suggestions with skill name and category
  - Max 200 lines
  - **Reference:** plan.md Service Decomposition Frontend, spec.md REQ-UI-002

- [ ] T029 [MODULAR] Create SkillFormDialog component
  - File: `frontend/src/components/SkillFormDialog.tsx`
  - Props: open (bool), onClose (fn), skill (UserSkillDto | null for edit), mode ('add' | 'edit')
  - Form fields:
    - SkillSelector (autocomplete) - only for add mode
    - Years of experience (number input, min 0, max 70)
    - Proficiency depth (MUI Slider 1-5 or Select with labels: Beginner, Intermediate, Proficient, Advanced, Expert)
    - Last used (MUI DatePicker month precision, max today)
    - Confidence level (Select: Low, Medium, High)
    - Tags (MUI Autocomplete freeSolo for multi-input chips, max 20)
    - Visibility toggle (Switch: Public/Private)
  - Inline validation with error messages
  - Save button disabled until valid
  - Loading state during API call
  - Max 250 lines
  - **Reference:** plan.md Service Decomposition Frontend, spec.md REQ-UI-002

- [ ] T030 [MODULAR] Create SkillFilters component
  - File: `frontend/src/components/SkillFilters.tsx`
  - Props: onFilterChange (fn)
  - Filter controls:
    - Category (MUI Select multi-select: All, Languages, Frameworks, Cloud, Databases, etc.)
    - Proficiency range (MUI Slider 1-5)
    - Last used date range (MUI DatePicker from-to)
    - Visibility (Radio: All, Public, Private)
  - "Clear filters" button
  - Filtered count displayed: "Showing 8 of 32 skills"
  - Max 150 lines
  - **Reference:** plan.md Service Decomposition Frontend, spec.md REQ-UI-003

### Pages

- [ ] T031 [MODULAR] Create SkillsPage component
  - File: `frontend/src/pages/SkillsPage.tsx`
  - Layout: Page header with "Add Skill" button, SkillFilters bar, skills grouped by category (collapsible sections)
  - Uses useUserSkills hook for data
  - Renders SkillCard for each skill
  - Empty state: "No skills added yet. Add your first skill to showcase your expertise."
  - Opens SkillFormDialog on "Add Skill" or edit click
  - Confirmation dialog for delete with message "Remove '{skill name}' from your profile? This will hide it from all exports and recruiter chat."
  - Max 300 lines
  - **Reference:** plan.md Service Decomposition Frontend, spec.md REQ-UI-001

- [ ] T032 [MODULAR] Create AdminSkillsCatalogPage component (optional for admin)
  - File: `frontend/src/pages/AdminSkillsCatalogPage.tsx`
  - Layout: Page header with "Add Skill to Catalog" button, MUI Table with columns: name, category, tags, is_active, actions (edit/deactivate/reactivate)
  - Admin-only route: /admin/skills-catalog
  - Requires ADMIN role (guard with authUtils.hasRole('ADMIN'))
  - Inline editing for skill name, category, description, tags
  - Deactivate/reactivate toggle with confirmation
  - Max 300 lines
  - **Reference:** plan.md Service Decomposition Frontend, spec.md REQ-UI-004

### Routing

- [ ] T033 [SIMPLE] Add skills routes to React Router
  - File: `frontend/src/App.tsx` or equivalent router config
  - Routes:
    - `/skills` → SkillsPage (protected, requires authentication)
    - `/admin/skills-catalog` → AdminSkillsCatalogPage (protected, requires ADMIN role)
  - **Reference:** plan.md Phase 4 tasks

- [ ] T034 [SIMPLE] Add navigation menu items for skills pages
  - File: `frontend/src/components/Navigation.tsx` or equivalent
  - Add "Skills" menu item in main navigation (routes to /skills)
  - Add "Skills Catalog" menu item in admin section (routes to /admin/skills-catalog, only visible to ADMIN role)
  - **Reference:** plan.md Phase 4 tasks

---

## Checkpoints

After each phase, verify:

- [ ] **Phase 1 Complete:** Migrations run successfully, tables created with correct indexes and constraints, entities map correctly
- [ ] **Phase 2 Complete:** Repositories query correctly, services pass unit tests, DTOs validate input, logging captures operations
- [ ] **Phase 3 Complete:** All endpoints return correct status codes, validation works, tenant isolation enforced, integration tests pass
- [ ] **Phase 4 Complete:** Autocomplete suggests skills, add/edit/delete operations work, filters narrow list, UI responsive on mobile/desktop

---

## Success Criteria Verification

### From spec.md

1. **Complete Skill Lifecycle**
   - [ ] Integration tests cover all CRUD operations with 100% success rate
   - [ ] Manual test: Add, update, view, delete skill without errors

2. **Skills Catalog Autocomplete Performance**
   - [ ] Load test with 1000+ catalog skills: p95 latency < 100ms
   - [ ] Monitor query execution time in PostgreSQL logs

3. **Duplicate Skill Prevention**
   - [ ] Test: Attempt to add same skill twice → 409 Conflict error
   - [ ] Error message: "You already have 'Java' in your profile"

4. **Public/Private Visibility Enforced**
   - [ ] Create user with mixed public/private skills
   - [ ] Call getPublicSkillsByProfile → verify private skills excluded

5. **Tenant Isolation Verified**
   - [ ] Create two tenants with overlapping skills
   - [ ] Cross-tenant access attempt → 404 Not Found

6. **Skill Grouping by Category**
   - [ ] Manual UAT: Add 20+ skills across 5 categories
   - [ ] Verify grouping correct in UI (Languages, Frameworks, Cloud, etc.)

7. **Admin Catalog Management**
   - [ ] Admin adds "Deno" to catalog
   - [ ] User sees it in autocomplete within 1 second

---

## Notes

- All tasks should be completed in order within each phase (dependencies)
- Tasks marked `[P]` can run in parallel (different files, no shared state)
- Refer to plan.md for detailed implementation guidance
- Refer to spec.md for functional requirements and success criteria
- Refer to data-model.md for database schema details

---

**Tasks Version:** 1.0.0
**Last Updated:** 2026-02-24
**Total Tasks:** 34
**Estimated Time:** 4-5 days
