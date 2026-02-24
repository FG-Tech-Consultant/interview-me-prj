# Implementation Tasks: Profile CRUD (Timeline, Jobs, Education)

**Feature ID:** 002-profile-crud
**Branch:** 002-profile-crud
**Spec:** [spec.md](./spec.md)
**Plan:** [plan.md](./plan.md)

---

## Legend
- `[ ]` Not Started
- `[P]` In Progress
- `[X]` Completed
- `[B]` Blocked (add blocker note)
- `[S]` Skipped (add reason)
- `[||]` Can run in parallel (different files, no dependencies)
- `[US-N]` Belongs to User Story N from spec.md

---

## Phase 1: Database Schema & Entities

### Liquibase Migrations

- [ ] T001 [DATABASE] Create profile table migration
  - File: `backend/src/main/resources/db/changelog/20260223120000-create-profile-table.xml`
  - Create `profile` table with all columns (id, tenant_id, user_id, full_name, headline, summary, location, languages JSONB, professional_links JSONB, career_preferences JSONB, default_visibility, created_at, updated_at, deleted_at, version)
  - Add foreign keys: tenant_id → tenant(id), user_id → user(id)
  - Add indexes: idx_profile_tenant_id, idx_profile_user_id
  - Add unique constraint: (tenant_id, user_id)
  - Include rollback: dropTable profile

- [ ] T002 [DATABASE] Create job_experience table migration
  - File: `backend/src/main/resources/db/changelog/20260223120100-create-job-experience-table.xml`
  - Create `job_experience` table with all columns (id, tenant_id, profile_id, company, role, start_date, end_date, is_current, location, employment_type, responsibilities, achievements, metrics JSONB, visibility, created_at, updated_at, deleted_at, version)
  - Add foreign keys: tenant_id → tenant(id), profile_id → profile(id)
  - Add indexes: idx_job_experience_tenant_id, idx_job_experience_profile_id, idx_job_experience_deleted_at
  - Add check constraint: end_date IS NULL OR end_date >= start_date
  - Include rollback: dropTable job_experience

- [ ] T003 [DATABASE] Create education table migration
  - File: `backend/src/main/resources/db/changelog/20260223120200-create-education-table.xml`
  - Create `education` table with all columns (id, tenant_id, profile_id, degree, institution, start_date, end_date, field_of_study, gpa, notes, visibility, created_at, updated_at, deleted_at, version)
  - Add foreign keys: tenant_id → tenant(id), profile_id → profile(id)
  - Add indexes: idx_education_tenant_id, idx_education_profile_id, idx_education_deleted_at
  - Add check constraint: end_date >= start_date (if start_date not null)
  - Include rollback: dropTable education

- [ ] T004 [DATABASE] Add migrations to master changelog
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Include: `db/changelog/20260223120000-create-profile-table.xml`
  - Include: `db/changelog/20260223120100-create-job-experience-table.xml`
  - Include: `db/changelog/20260223120200-create-education-table.xml`

### JPA Entities

- [ ] T005 [||] [JAVA25] Create Profile entity
  - File: `backend/src/main/java/com/interviewme/profile/model/Profile.java`
  - Annotations: @Entity, @Table(name = "profile"), @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id (BIGSERIAL), tenantId (BIGINT), userId (BIGINT), fullName, headline, summary, location, languages (List<String> with JSONB), professionalLinks (Map<String, String> with JSONB), careerPreferences (Map<String, Object> with JSONB), defaultVisibility, createdAt, updatedAt, deletedAt, version
  - Relationships: @ManyToOne(tenant), @ManyToOne(user), @OneToMany(jobExperiences), @OneToMany(education)
  - Use `@Type(JsonBinaryType.class)` for JSONB fields (Hypersistence Utils)
  - Lombok: @Entity, @Data, @NoArgsConstructor, @AllArgsConstructor

- [ ] T006 [||] [JAVA25] Create JobExperience entity
  - File: `backend/src/main/java/com/interviewme/profile/model/JobExperience.java`
  - Annotations: @Entity, @Table(name = "job_experience"), @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id, tenantId, profileId, company, role, startDate (LocalDate), endDate (LocalDate), isCurrent (boolean), location, employmentType, responsibilities, achievements, metrics (Map<String, Object> with JSONB), visibility, createdAt, updatedAt, deletedAt, version
  - Relationships: @ManyToOne(tenant), @ManyToOne(profile)
  - Use `@Type(JsonBinaryType.class)` for metrics JSONB field
  - Lombok: @Entity, @Data, @NoArgsConstructor, @AllArgsConstructor

- [ ] T007 [||] [JAVA25] Create Education entity
  - File: `backend/src/main/java/com/interviewme/profile/model/Education.java`
  - Annotations: @Entity, @Table(name = "education"), @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id, tenantId, profileId, degree, institution, startDate (LocalDate), endDate (LocalDate), fieldOfStudy, gpa, notes, visibility, createdAt, updatedAt, deletedAt, version
  - Relationships: @ManyToOne(tenant), @ManyToOne(profile)
  - Lombok: @Entity, @Data, @NoArgsConstructor, @AllArgsConstructor

---

## Phase 2: DTOs & Mappers

### DTOs (Java Records)

- [ ] T008 [||] [JAVA25] Create Profile DTOs
  - Files: `backend/src/main/java/com/interviewme/profile/dto/`
  - `CreateProfileRequest.java`: record with @NotBlank fullName, @NotBlank headline, summary, location, languages, professionalLinks, careerPreferences, defaultVisibility
  - `UpdateProfileRequest.java`: record with same fields + version (Long) for optimistic locking
  - `ProfileResponse.java`: record with all fields + nested List<JobExperienceResponse> jobs, List<EducationResponse> education
  - Use Jakarta Validation annotations: @NotBlank, @Size, @Email, @Pattern

- [ ] T009 [||] [JAVA25] Create JobExperience DTOs
  - Files: `backend/src/main/java/com/interviewme/profile/dto/`
  - `CreateJobExperienceRequest.java`: record with @NotBlank company, @NotBlank role, @NotNull startDate, endDate, isCurrent, location, employmentType, responsibilities, achievements, metrics, visibility
  - `UpdateJobExperienceRequest.java`: record with same fields + version
  - `JobExperienceResponse.java`: record with all fields
  - Custom validator: `@ValidDateRange` (start_date before end_date)

- [ ] T010 [||] [JAVA25] Create Education DTOs
  - Files: `backend/src/main/java/com/interviewme/profile/dto/`
  - `CreateEducationRequest.java`: record with @NotBlank degree, @NotBlank institution, startDate, @NotNull endDate, fieldOfStudy, gpa, notes, visibility
  - `UpdateEducationRequest.java`: record with same fields + version
  - `EducationResponse.java`: record with all fields
  - Custom validator: `@ValidDateRange` (start_date before end_date if present)

### Mappers

- [ ] T011 [||] [SIMPLE] Create ProfileMapper
  - File: `backend/src/main/java/com/interviewme/profile/mapper/ProfileMapper.java`
  - Static methods: `Profile toEntity(CreateProfileRequest request)`, `ProfileResponse toResponse(Profile entity)`, `void updateEntity(Profile entity, UpdateProfileRequest request)`
  - Handle JSONB conversions for languages, professionalLinks, careerPreferences

- [ ] T012 [||] [SIMPLE] Create JobExperienceMapper
  - File: `backend/src/main/java/com/interviewme/profile/mapper/JobExperienceMapper.java`
  - Static methods: `JobExperience toEntity(CreateJobExperienceRequest request)`, `JobExperienceResponse toResponse(JobExperience entity)`, `void updateEntity(JobExperience entity, UpdateJobExperienceRequest request)`
  - Handle JSONB conversions for metrics

- [ ] T013 [||] [SIMPLE] Create EducationMapper
  - File: `backend/src/main/java/com/interviewme/profile/mapper/EducationMapper.java`
  - Static methods: `Education toEntity(CreateEducationRequest request)`, `EducationResponse toResponse(Education entity)`, `void updateEntity(Education entity, UpdateEducationRequest request)`

---

## Phase 3: Repositories

- [ ] T014 [||] [DATA] Create ProfileRepository
  - File: `backend/src/main/java/com/interviewme/profile/repository/ProfileRepository.java`
  - Extend: `JpaRepository<Profile, Long>`
  - Query methods: `Optional<Profile> findByUserIdAndDeletedAtIsNull(Long userId)`, `boolean existsByUserIdAndDeletedAtIsNull(Long userId)`, `Optional<Profile> findByIdAndDeletedAtIsNull(Long id)`
  - Custom query: `@Query` for findByIdAndTenantId with tenant_id filter

- [ ] T015 [||] [DATA] Create JobExperienceRepository
  - File: `backend/src/main/java/com/interviewme/profile/repository/JobExperienceRepository.java`
  - Extend: `JpaRepository<JobExperience, Long>`
  - Query methods: `List<JobExperience> findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(Long profileId)`, `List<JobExperience> findByProfileIdAndVisibilityAndDeletedAtIsNull(Long profileId, String visibility)`, `Optional<JobExperience> findByIdAndDeletedAtIsNull(Long id)`

- [ ] T016 [||] [DATA] Create EducationRepository
  - File: `backend/src/main/java/com/interviewme/profile/repository/EducationRepository.java`
  - Extend: `JpaRepository<Education, Long>`
  - Query methods: `List<Education> findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(Long profileId)`, `List<Education> findByProfileIdAndVisibilityAndDeletedAtIsNull(Long profileId, String visibility)`, `Optional<Education> findByIdAndDeletedAtIsNull(Long id)`

---

## Phase 4: Services

### Profile Service

- [ ] T017 [SIMPLE] Create ProfileService
  - File: `backend/src/main/java/com/interviewme/profile/service/ProfileService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: ProfileRepository, UserRepository
  - Method: `@Transactional(readOnly = true) ProfileResponse getProfileByUserId(Long userId)` - retrieves profile with jobs and education
  - Method: `@Transactional ProfileResponse createProfile(Long userId, CreateProfileRequest request)` - validates user exists, checks duplicate profile, sets tenantId from TenantContext, saves
  - Method: `@Transactional ProfileResponse updateProfile(Long profileId, UpdateProfileRequest request)` - validates profile exists, checks version for optimistic locking, updates, saves
  - Method: `@Transactional void deleteProfile(Long profileId)` - soft delete (sets deleted_at)
  - Logging: log.info for all operations with profileId, userId, tenantId

### Job Experience Service

- [ ] T018 [SIMPLE] Create JobExperienceService
  - File: `backend/src/main/java/com/interviewme/profile/service/JobExperienceService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: JobExperienceRepository, ProfileRepository
  - Method: `@Transactional(readOnly = true) List<JobExperienceResponse> getJobExperiencesByProfileId(Long profileId)` - returns all non-deleted jobs ordered by start_date DESC
  - Method: `@Transactional JobExperienceResponse createJobExperience(Long profileId, CreateJobExperienceRequest request)` - validates profile exists, validates dates (end >= start), sets tenantId, saves
  - Method: `@Transactional JobExperienceResponse updateJobExperience(Long jobId, UpdateJobExperienceRequest request)` - validates job exists, checks version, validates dates, updates, saves
  - Method: `@Transactional void deleteJobExperience(Long jobId)` - soft delete (sets deleted_at)
  - Date validation: if isCurrent = true, end_date must be NULL; if end_date present, must be >= start_date
  - Logging: log.info for all operations

### Education Service

- [ ] T019 [SIMPLE] Create EducationService
  - File: `backend/src/main/java/com/interviewme/profile/service/EducationService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: EducationRepository, ProfileRepository
  - Method: `@Transactional(readOnly = true) List<EducationResponse> getEducationByProfileId(Long profileId)` - returns all non-deleted education ordered by end_date DESC
  - Method: `@Transactional EducationResponse createEducation(Long profileId, CreateEducationRequest request)` - validates profile exists, validates dates, sets tenantId, saves
  - Method: `@Transactional EducationResponse updateEducation(Long educationId, UpdateEducationRequest request)` - validates education exists, checks version, validates dates, updates, saves
  - Method: `@Transactional void deleteEducation(Long educationId)` - soft delete (sets deleted_at)
  - Date validation: if start_date present, must be < end_date; end_date must not be in future
  - Logging: log.info for all operations

---

## Phase 5: Controllers

### Profile Controller

- [ ] T020 [MODULAR] Create ProfileController
  - File: `backend/src/main/java/com/interviewme/profile/controller/ProfileController.java`
  - Annotations: @RestController, @RequestMapping("/api/v1/profiles"), @RequiredArgsConstructor, @Slf4j
  - Dependencies: ProfileService
  - Endpoint: `@GetMapping("/{profileId}")` → `getProfile(Long profileId)` → 200 OK ProfileResponse
  - Endpoint: `@PostMapping` → `createProfile(@Valid CreateProfileRequest request)` → 201 Created ProfileResponse, Location header
  - Endpoint: `@PutMapping("/{profileId}")` → `updateProfile(Long profileId, @Valid UpdateProfileRequest request)` → 200 OK ProfileResponse
  - Endpoint: `@DeleteMapping("/{profileId}")` → `deleteProfile(Long profileId)` → 204 No Content
  - All endpoints require JWT auth: `@PreAuthorize("hasRole('USER')")`

### Job Experience Controller

- [ ] T021 [MODULAR] Create JobExperienceController
  - File: `backend/src/main/java/com/interviewme/profile/controller/JobExperienceController.java`
  - Annotations: @RestController, @RequestMapping("/api/v1/profiles/{profileId}/jobs"), @RequiredArgsConstructor, @Slf4j
  - Dependencies: JobExperienceService
  - Endpoint: `@GetMapping` → `getJobExperiences(Long profileId)` → 200 OK List<JobExperienceResponse>
  - Endpoint: `@PostMapping` → `createJobExperience(Long profileId, @Valid CreateJobExperienceRequest request)` → 201 Created JobExperienceResponse, Location header
  - Endpoint: `@PutMapping("/{jobId}")` → `updateJobExperience(Long profileId, Long jobId, @Valid UpdateJobExperienceRequest request)` → 200 OK JobExperienceResponse
  - Endpoint: `@DeleteMapping("/{jobId}")` → `deleteJobExperience(Long profileId, Long jobId)` → 204 No Content
  - All endpoints require JWT auth

### Education Controller

- [ ] T022 [MODULAR] Create EducationController
  - File: `backend/src/main/java/com/interviewme/profile/controller/EducationController.java`
  - Annotations: @RestController, @RequestMapping("/api/v1/profiles/{profileId}/education"), @RequiredArgsConstructor, @Slf4j
  - Dependencies: EducationService
  - Endpoint: `@GetMapping` → `getEducation(Long profileId)` → 200 OK List<EducationResponse>
  - Endpoint: `@PostMapping` → `createEducation(Long profileId, @Valid CreateEducationRequest request)` → 201 Created EducationResponse, Location header
  - Endpoint: `@PutMapping("/{educationId}")` → `updateEducation(Long profileId, Long educationId, @Valid UpdateEducationRequest request)` → 200 OK EducationResponse
  - Endpoint: `@DeleteMapping("/{educationId}")` → `deleteEducation(Long profileId, Long educationId)` → 204 No Content
  - All endpoints require JWT auth

---

## Phase 6: Exception Handling

- [ ] T023 [||] [SIMPLE] Create custom exceptions
  - Files: `backend/src/main/java/com/interviewme/common/exception/`
  - `ProfileNotFoundException.java`: extends RuntimeException, constructor with profileId
  - `DuplicateProfileException.java`: extends RuntimeException, constructor with message
  - `OptimisticLockException.java`: extends RuntimeException, constructor with message
  - `ValidationException.java`: extends RuntimeException, constructor with field and message

- [ ] T024 [SIMPLE] Create GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/config/GlobalExceptionHandler.java`
  - Annotation: @ControllerAdvice
  - Handler: `@ExceptionHandler(ProfileNotFoundException.class)` → 404 Not Found ErrorResponse
  - Handler: `@ExceptionHandler(DuplicateProfileException.class)` → 400 Bad Request ErrorResponse
  - Handler: `@ExceptionHandler(OptimisticLockException.class)` → 409 Conflict ErrorResponse
  - Handler: `@ExceptionHandler(MethodArgumentNotValidException.class)` → 400 Bad Request with field errors
  - Error response format: `{"code": "ERROR_CODE", "message": "...", "fields": [{"field": "...", "message": "..."}]}`

---

## Phase 7: Frontend - TypeScript Types & API Clients

### TypeScript Types

- [ ] T025 [||] [SIMPLE] Create Profile TypeScript interfaces
  - File: `frontend/src/types/profile.ts`
  - Interfaces: `CreateProfileRequest`, `UpdateProfileRequest`, `ProfileResponse`
  - Match backend DTOs exactly (camelCase for JSON)

- [ ] T026 [||] [SIMPLE] Create JobExperience TypeScript interfaces
  - File: `frontend/src/types/jobExperience.ts`
  - Interfaces: `CreateJobExperienceRequest`, `UpdateJobExperienceRequest`, `JobExperienceResponse`

- [ ] T027 [||] [SIMPLE] Create Education TypeScript interfaces
  - File: `frontend/src/types/education.ts`
  - Interfaces: `CreateEducationRequest`, `UpdateEducationRequest`, `EducationResponse`

### API Clients

- [ ] T028 [||] [MODULAR] Create Profile API client
  - File: `frontend/src/api/profileApi.ts`
  - Import: Axios client with JWT interceptor from Feature 001
  - Functions: `getProfile(profileId: number)`, `createProfile(request: CreateProfileRequest)`, `updateProfile(profileId: number, request: UpdateProfileRequest)`, `deleteProfile(profileId: number)`
  - All functions return typed responses (ProfileResponse)

- [ ] T029 [||] [MODULAR] Create JobExperience API client
  - File: `frontend/src/api/jobExperienceApi.ts`
  - Functions: `getJobExperiences(profileId: number)`, `createJobExperience(profileId: number, request: CreateJobExperienceRequest)`, `updateJobExperience(profileId: number, jobId: number, request: UpdateJobExperienceRequest)`, `deleteJobExperience(profileId: number, jobId: number)`

- [ ] T030 [||] [MODULAR] Create Education API client
  - File: `frontend/src/api/educationApi.ts`
  - Functions: `getEducation(profileId: number)`, `createEducation(profileId: number, request: CreateEducationRequest)`, `updateEducation(profileId: number, educationId: number, request: UpdateEducationRequest)`, `deleteEducation(profileId: number, educationId: number)`

---

## Phase 8: Frontend - TanStack Query Hooks

- [ ] T031 [||] [MODULAR] Create Profile hooks
  - File: `frontend/src/hooks/useProfile.ts`
  - Hooks: `useProfile(profileId)` (useQuery), `useCreateProfile()` (useMutation), `useUpdateProfile()` (useMutation), `useDeleteProfile()` (useMutation)
  - Query keys: `['profile', profileId]`
  - Invalidation: Invalidate profile query on create/update/delete

- [ ] T032 [||] [MODULAR] Create JobExperience hooks
  - File: `frontend/src/hooks/useJobExperiences.ts`
  - Hooks: `useJobExperiences(profileId)` (useQuery), `useCreateJobExperience()` (useMutation), `useUpdateJobExperience()` (useMutation), `useDeleteJobExperience()` (useMutation)
  - Query keys: `['jobExperiences', profileId]`
  - Invalidation: Invalidate job experiences and profile queries on mutations

- [ ] T033 [||] [MODULAR] Create Education hooks
  - File: `frontend/src/hooks/useEducation.ts`
  - Hooks: `useEducation(profileId)` (useQuery), `useCreateEducation()` (useMutation), `useUpdateEducation()` (useMutation), `useDeleteEducation()` (useMutation)
  - Query keys: `['education', profileId]`
  - Invalidation: Invalidate education and profile queries on mutations

---

## Phase 9: Frontend - React Components

### Profile Components

- [ ] T034 [MODULAR] Create ProfileEditorPage
  - File: `frontend/src/pages/ProfileEditorPage.tsx`
  - Use MUI Tabs component for sections: Personal Info, Work Experience, Education, Career Preferences
  - Use `useProfile(profileId)` hook to load profile data
  - Display loading state (CircularProgress) and error state
  - Render appropriate section based on selected tab

- [ ] T035 [||] [MODULAR] Create ProfileInfoForm component
  - File: `frontend/src/components/profile/ProfileInfoForm.tsx`
  - MUI form: TextField for fullName, headline, summary, location
  - TextField array for languages (comma-separated or chips)
  - TextField for professional links (LinkedIn, GitHub, Portfolio)
  - Save button calls `useUpdateProfile()` mutation
  - Display validation errors inline

- [ ] T036 [||] [MODULAR] Create CareerPreferencesForm component
  - File: `frontend/src/components/profile/CareerPreferencesForm.tsx`
  - MUI form: Select or Autocomplete for target roles, domains, geographies
  - Checkbox for remote willing, relocation willing
  - Save button calls `useUpdateProfile()` mutation

- [ ] T037 [||] [MODULAR] Create ProfileCompletenessIndicator component
  - File: `frontend/src/components/profile/ProfileCompletenessIndicator.tsx`
  - MUI LinearProgress with percentage label
  - Calculate percentage: profile info (30%), >= 1 job (30%), >= 1 education (20%), public visibility (20%)
  - Display suggestions for incomplete sections

- [ ] T038 [||] [MODULAR] Create VisibilityToggle component
  - File: `frontend/src/components/profile/VisibilityToggle.tsx`
  - MUI Switch with label: "Public" vs "Private"
  - Icon: Lock (private) vs LockOpen (public)
  - Props: value (public/private), onChange callback

### Job Experience Components

- [ ] T039 [MODULAR] Create JobExperienceList component
  - File: `frontend/src/components/job-experience/JobExperienceList.tsx`
  - Use `useJobExperiences(profileId)` hook to load jobs
  - Display jobs as timeline cards (most recent first)
  - "Add Job" button opens JobExperienceFormDialog
  - Pass onEdit, onDelete callbacks to JobExperienceCard

- [ ] T040 [||] [MODULAR] Create JobExperienceCard component
  - File: `frontend/src/components/job-experience/JobExperienceCard.tsx`
  - MUI Card with company, role, dates, location, achievements preview
  - Visibility badge (Chip with Lock/LockOpen icon)
  - Current job badge (Chip with "Current" label)
  - Edit and Delete IconButtons
  - Props: job (JobExperienceResponse), onEdit, onDelete

- [ ] T041 [MODULAR] Create JobExperienceFormDialog component
  - File: `frontend/src/components/job-experience/JobExperienceFormDialog.tsx`
  - MUI Dialog with form: TextField (company, role, location, employment_type), DatePicker (start_date, end_date), Checkbox (is_current), TextField multiline (responsibilities, achievements), VisibilityToggle
  - If is_current checked, disable end_date field
  - Validate dates: end_date >= start_date
  - Submit button calls `useCreateJobExperience()` or `useUpdateJobExperience()` based on mode (create/edit)
  - Display validation errors from backend

### Education Components

- [ ] T042 [MODULAR] Create EducationList component
  - File: `frontend/src/components/education/EducationList.tsx`
  - Use `useEducation(profileId)` hook to load education
  - Display education as list items (most recent first)
  - "Add Education" button opens EducationFormDialog
  - Pass onEdit, onDelete callbacks to EducationListItem

- [ ] T043 [||] [MODULAR] Create EducationListItem component
  - File: `frontend/src/components/education/EducationListItem.tsx`
  - MUI ListItem with degree, institution, dates, field_of_study
  - Visibility badge (Chip)
  - Edit and Delete IconButtons
  - Props: education (EducationResponse), onEdit, onDelete

- [ ] T044 [MODULAR] Create EducationFormDialog component
  - File: `frontend/src/components/education/EducationFormDialog.tsx`
  - MUI Dialog with form: TextField (degree, institution, field_of_study, gpa), DatePicker (start_date, end_date), TextField multiline (notes), VisibilityToggle
  - Validate dates: if start_date provided, must be < end_date
  - Submit button calls `useCreateEducation()` or `useUpdateEducation()` based on mode
  - Display validation errors from backend

---

## Phase 10: Testing

### Backend Unit Tests

- [ ] T045 [||] [JAVA25] Write ProfileService unit tests
  - File: `backend/src/test/java/com/interviewme/profile/service/ProfileServiceTest.java`
  - Use @ExtendWith(MockitoExtension.class), mock ProfileRepository and UserRepository
  - Test: createProfile_shouldSaveProfileWithTenantId
  - Test: createProfile_shouldThrowExceptionIfProfileExists
  - Test: updateProfile_shouldThrowOptimisticLockException
  - Test: deleteProfile_shouldSetDeletedAt

- [ ] T046 [||] [JAVA25] Write JobExperienceService unit tests
  - File: `backend/src/test/java/com/interviewme/profile/service/JobExperienceServiceTest.java`
  - Test: createJobExperience_shouldValidateDates
  - Test: createJobExperience_shouldSetTenantId
  - Test: updateJobExperience_shouldThrowOptimisticLockException

- [ ] T047 [||] [JAVA25] Write EducationService unit tests
  - File: `backend/src/test/java/com/interviewme/profile/service/EducationServiceTest.java`
  - Test: createEducation_shouldValidateDates
  - Test: deleteEducation_shouldSetDeletedAt

### Backend Integration Tests

- [ ] T048 [SIMPLE] Write ProfileController integration tests
  - File: `backend/src/test/java/com/interviewme/profile/controller/ProfileControllerIntegrationTest.java`
  - Use @SpringBootTest, @AutoConfigureMockMvc, @Transactional
  - Test: POST /api/v1/profiles → 201 Created with valid request
  - Test: POST /api/v1/profiles → 400 Bad Request for invalid request (missing required fields)
  - Test: PUT /api/v1/profiles/{id} → 200 OK with valid update
  - Test: PUT /api/v1/profiles/{id} → 409 Conflict with version mismatch
  - Test: GET /api/v1/profiles/{id} → 404 Not Found for non-existent profile
  - Test: DELETE /api/v1/profiles/{id} → 204 No Content, verify soft delete in database

- [ ] T049 [SIMPLE] Write JobExperienceController integration tests
  - File: `backend/src/test/java/com/interviewme/profile/controller/JobExperienceControllerIntegrationTest.java`
  - Test: POST /api/v1/profiles/{profileId}/jobs → 201 Created
  - Test: POST /api/v1/profiles/{profileId}/jobs → 400 Bad Request for invalid dates
  - Test: GET /api/v1/profiles/{profileId}/jobs → 200 OK with list ordered by start_date DESC

- [ ] T050 [SIMPLE] Write EducationController integration tests
  - File: `backend/src/test/java/com/interviewme/profile/controller/EducationControllerIntegrationTest.java`
  - Test: POST /api/v1/profiles/{profileId}/education → 201 Created
  - Test: DELETE /api/v1/profiles/{profileId}/education/{id} → 204 No Content

### Tenant Isolation Tests

- [ ] T051 [DATA] Write tenant isolation integration tests
  - File: `backend/src/test/java/com/interviewme/profile/TenantIsolationTest.java`
  - Test: Create two users in different tenants
  - Test: User 1 creates profile, user 2 tries to access → 404 Not Found
  - Test: User 1 creates job experience, user 2 tries to access → 404 Not Found
  - Verify Hibernate filter is applied automatically

### Frontend Tests

- [ ] T052 [||] [SIMPLE] Write ProfileEditorPage tests
  - File: `frontend/src/pages/ProfileEditorPage.test.tsx`
  - Use React Testing Library, mock API calls
  - Test: should display profile data when loaded
  - Test: should show loading state while fetching
  - Test: should display error message on fetch failure

- [ ] T053 [||] [SIMPLE] Write JobExperienceFormDialog tests
  - File: `frontend/src/components/job-experience/JobExperienceFormDialog.test.tsx`
  - Test: should validate required fields
  - Test: should validate date range (end_date >= start_date)
  - Test: should disable end_date when is_current checked
  - Test: should call createJobExperience mutation on submit

---

## Phase 11: Documentation & Deployment

- [ ] T054 [SIMPLE] Create API documentation
  - File: `specs/002-profile-crud/contracts/api-spec.yaml`
  - OpenAPI 3.0 spec with all endpoints (Profile, JobExperience, Education)
  - Include request/response schemas, examples, validation rules
  - Document error responses (400, 404, 409)

- [ ] T055 [||] [SIMPLE] Update backend README
  - File: `backend/README.md`
  - Add section: "Profile CRUD Endpoints"
  - Document new dependencies (Hypersistence Utils for JSONB)
  - Add example requests for all CRUD operations

- [ ] T056 [||] [SIMPLE] Update frontend README
  - File: `frontend/README.md`
  - Document new pages and components
  - Add screenshots of profile editor (if available)

- [ ] T057 [SIMPLE] Verify all migrations run successfully
  - Command: `./gradlew backend:bootRun`
  - Verify: All 3 migrations execute without errors
  - Verify: Tables created with correct schema (check with PostgreSQL client)
  - Verify: Indexes and foreign keys created

- [ ] T058 [SIMPLE] Run all backend tests
  - Command: `./gradlew backend:test`
  - Verify: All unit and integration tests pass
  - Verify: Code coverage > 80% for service layer

- [ ] T059 [SIMPLE] Run all frontend tests
  - Command: `cd frontend && npm test`
  - Verify: All React tests pass

- [ ] T060 [SIMPLE] Manual E2E testing
  - Test: Register new user → create profile → add 3 jobs → add 2 education entries
  - Test: Edit job experience → verify changes persist
  - Test: Toggle visibility from private to public → verify badge updates
  - Test: Delete job experience → verify it disappears from UI
  - Test: Concurrent editing in two tabs → verify optimistic lock conflict (409 Conflict)

---

## Checkpoints

**After Phase 1 (Database Schema):**
- [ ] All migrations run successfully
- [ ] Tables created with correct schema, indexes, foreign keys
- [ ] JPA entities map correctly to database tables

**After Phase 4 (Services):**
- [ ] All service methods work correctly with tenant filtering
- [ ] Validation logic enforces date constraints
- [ ] Soft delete implemented correctly

**After Phase 6 (Controllers):**
- [ ] All REST endpoints accessible and return correct responses
- [ ] JWT authentication required for all endpoints
- [ ] Error handling returns appropriate HTTP status codes

**After Phase 9 (Frontend Components):**
- [ ] Profile editor page loads and displays data correctly
- [ ] Forms validate input and display errors
- [ ] CRUD operations trigger API calls and update UI

**After Phase 10 (Testing):**
- [ ] All tests pass (unit, integration, E2E)
- [ ] Tenant isolation verified
- [ ] Code coverage meets targets

---

## Progress Summary

**Total Tasks:** 60
**Completed:** 0
**In Progress:** 0
**Blocked:** 0
**Skipped:** 0
**Completion:** 0%

---

## Notes

### Key Dependencies
- Phase 2 (DTOs) depends on Phase 1 (Entities)
- Phase 3 (Repositories) depends on Phase 1 (Entities)
- Phase 4 (Services) depends on Phase 2 (DTOs) and Phase 3 (Repositories)
- Phase 5 (Controllers) depends on Phase 4 (Services)
- Phase 7-9 (Frontend) depends on Phase 5 (Controllers) for API contracts
- Phase 10 (Testing) depends on all previous phases

### Parallel Execution Opportunities
- Within Phase 1: All 3 migrations can be written in parallel
- Within Phase 2: All DTOs and Mappers can be written in parallel
- Within Phase 3: All 3 repositories can be written in parallel
- Within Phase 7: All TypeScript types and API clients can be written in parallel
- Within Phase 10: Unit tests can be written in parallel with integration tests

### Risk Mitigation
- **JSONB Handling:** Test JSONB serialization/deserialization early with Hypersistence Utils
- **Optimistic Locking:** Verify version field updates correctly on every save
- **Tenant Filtering:** Write tenant isolation tests early to catch cross-tenant bugs
- **Date Validation:** Ensure both backend and frontend validate dates consistently

---

**Last Updated:** 2026-02-23
**Next Review:** After Phase 1 completion (database schema)
