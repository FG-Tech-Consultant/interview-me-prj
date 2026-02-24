# Implementation Tasks: Live Public Profile Page

**Feature ID:** 005-live-public-profile
**Branch:** 005-live-public-profile
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

## Phase 1: Database Schema

### Liquibase Migration

- [ ] T001 [DATABASE] Add slug and view_count columns to profile table
  - File: `backend/src/main/resources/db/changelog/20260224150000-add-slug-to-profile.xml`
  - ChangeSet 1: Add `slug` column (VARCHAR 50, NULLABLE) to profile table
  - Add unique index: `idx_profile_slug` on slug column
  - ChangeSet 2: Add `view_count` column (BIGINT, NOT NULL, DEFAULT 0) to profile table
  - Include rollback: dropIndex + dropColumn for both columns

- [ ] T002 [DATABASE] Add migration to master changelog
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Include: `db/changelog/20260224150000-add-slug-to-profile.xml`

---

## Phase 2: Entity Updates & Slug Validation

- [ ] T003 [JAVA25] Add slug and viewCount fields to Profile entity
  - File: `backend/src/main/java/com/interviewme/profile/model/Profile.java`
  - Add field: `slug` (String, VARCHAR 50, nullable, unique)
  - Add field: `viewCount` (Long, default 0)
  - Column mapping: `@Column(name = "slug", length = 50, unique = true)` and `@Column(name = "view_count", nullable = false)`

- [ ] T004 [SIMPLE] Create slug validation utility
  - File: `backend/src/main/java/com/interviewme/publicprofile/util/SlugValidator.java`
  - Static method: `boolean isValidSlug(String slug)` - regex: `^[a-z0-9][a-z0-9-]*[a-z0-9]$`, 3-50 chars, no consecutive hyphens
  - Static method: `boolean isReservedSlug(String slug)` - check against reserved list: "admin", "api", "login", "register", "dashboard", "profile", "billing", "settings", "help", "about", "p"
  - Static method: `String normalizeSlug(String slug)` - lowercase, trim

- [ ] T005 [DATA] Add slug query methods to ProfileRepository
  - File: `backend/src/main/java/com/interviewme/profile/repository/ProfileRepository.java`
  - Add method: `Optional<Profile> findBySlugAndDeletedAtIsNull(String slug)`
  - Add method: `boolean existsBySlug(String slug)`

---

## Phase 3: Public Profile DTOs

- [ ] T006 [||] [JAVA25] Create public profile response DTOs
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/PublicProfileResponse.java`
  - Record: PublicProfileResponse(slug, fullName, headline, summary, location, languages, professionalLinks, skills, jobs, education, seo)
  - All fields are human-readable data only -- NO internal IDs

- [ ] T007 [||] [JAVA25] Create public skill response DTO
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/PublicSkillResponse.java`
  - Record: PublicSkillResponse(skillName, category, proficiencyDepth, yearsOfExperience, lastUsedDate)

- [ ] T008 [||] [JAVA25] Create public job response DTO
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/PublicJobResponse.java`
  - Record: PublicJobResponse(company, role, startDate, endDate, isCurrent, location, employmentType, responsibilities, achievements, metrics, projects)

- [ ] T009 [||] [JAVA25] Create public education response DTO
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/PublicEducationResponse.java`
  - Record: PublicEducationResponse(degree, institution, startDate, endDate, fieldOfStudy)

- [ ] T010 [||] [JAVA25] Create public project response DTO
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/PublicProjectResponse.java`
  - Record: PublicProjectResponse(title, context, role, teamSize, techStack, architectureType, metrics, outcomes, linkedSkills, stories)

- [ ] T011 [||] [JAVA25] Create public story response DTO
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/PublicStoryResponse.java`
  - Record: PublicStoryResponse(title, situation, task, action, result, metrics, linkedSkills)

- [ ] T012 [||] [JAVA25] Create SEO metadata and slug check DTOs
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/SeoMetadata.java`
  - Record: SeoMetadata(title, description, canonicalUrl, keywords)
  - File: `backend/src/main/java/com/interviewme/publicprofile/dto/SlugCheckResponse.java`
  - Record: SlugCheckResponse(slug, available, suggestions)

---

## Phase 4: Repository Extensions

- [ ] T013 [||] [DATA] Add public visibility query methods to repositories
  - File: `backend/src/main/java/com/interviewme/profile/repository/JobExperienceRepository.java`
  - Verify existing method: `findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByStartDateDesc(Long profileId, String visibility)`
  - File: `backend/src/main/java/com/interviewme/profile/repository/EducationRepository.java`
  - Add method: `List<Education> findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByEndDateDesc(Long profileId, String visibility)`

- [ ] T014 [||] [DATA] Add public visibility query methods to skills repository
  - File (if exists): `backend/src/main/java/com/interviewme/skills/repository/UserSkillRepository.java`
  - Add method: `List<UserSkill> findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByProficiencyDepthDesc(Long profileId, String visibility)`
  - Graceful handling: if Feature 003 repo doesn't exist yet, skip and handle in service

- [ ] T015 [||] [DATA] Add public visibility query methods to experience repositories
  - Files (if exist): ExperienceProjectRepository, StoryRepository
  - Add method on project repo: `List<ExperienceProject> findByJobExperienceIdInAndVisibilityAndDeletedAtIsNull(List<Long> jobIds, String visibility)`
  - Add method on story repo: `List<Story> findByExperienceProjectIdInAndVisibilityAndDeletedAtIsNull(List<Long> projectIds, String visibility)`
  - Graceful handling: if Feature 004 repos don't exist yet, skip and handle in service

---

## Phase 5: Public Profile Service

- [ ] T016 [MODULAR] Create PublicProfileService
  - File: `backend/src/main/java/com/interviewme/publicprofile/service/PublicProfileService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: ProfileRepository, JobExperienceRepository, EducationRepository, UserSkillRepository (optional), ExperienceProjectRepository (optional), StoryRepository (optional)
  - Method: `@Transactional(readOnly = true) PublicProfileResponse getPublicProfile(String slug)`
    - Find profile by slug (no tenant filter)
    - Throw PublicProfileNotFoundException if not found or deleted
    - Fetch public jobs, education, skills, projects, stories using visibility='public' queries
    - Handle missing Feature 003/004 repos gracefully (try-catch or optional dependency injection)
    - Assemble nested response (projects nested under jobs, stories nested under projects)
    - Build SeoMetadata from profile data
    - Fire-and-forget view count increment
  - Method: `@Transactional(readOnly = true) SlugCheckResponse checkSlugAvailability(String slug)`
    - Validate slug format (SlugValidator)
    - Check if slug exists in database
    - Generate suggestions if taken: slug-1, slug-2, etc.
  - Method: `@Transactional void incrementViewCount(Long profileId)`
    - Increment view_count by 1
    - Best-effort (catch and log exceptions, never fail the public request)
  - Logging: log.info for each public profile view with slug

- [ ] T017 [SIMPLE] Create PublicProfileNotFoundException
  - File: `backend/src/main/java/com/interviewme/publicprofile/exception/PublicProfileNotFoundException.java`
  - Extends RuntimeException, constructor with slug parameter

- [ ] T018 [SIMPLE] Add public profile exception handler to GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/config/GlobalExceptionHandler.java`
  - Add handler: `@ExceptionHandler(PublicProfileNotFoundException.class)` -> 404 Not Found

---

## Phase 6: Public Profile Controller

- [ ] T019 [MODULAR] Create PublicProfileController
  - File: `backend/src/main/java/com/interviewme/publicprofile/controller/PublicProfileController.java`
  - Annotations: @RestController, @RequestMapping("/api/public/profiles"), @RequiredArgsConstructor, @Slf4j
  - Dependencies: PublicProfileService
  - Endpoint: `@GetMapping("/{slug}")` -> getPublicProfile(@PathVariable String slug) -> 200 OK PublicProfileResponse / 404
  - No @PreAuthorize (public endpoint, no auth required)
  - Log: slug, response time

- [ ] T020 [SIMPLE] Add slug management endpoints to ProfileController
  - File: `backend/src/main/java/com/interviewme/profile/controller/ProfileController.java`
  - Endpoint: `@GetMapping("/slug/check")` -> checkSlug(@RequestParam String slug) -> 200 OK SlugCheckResponse
  - Endpoint: `@PutMapping("/{profileId}/slug")` -> setSlug(@PathVariable Long profileId, @RequestBody Map<String, String> body) -> 200 OK ProfileResponse
  - Both endpoints require JWT auth
  - Slug validation: format check + uniqueness check
  - Return 409 Conflict if slug is taken
  - Return 400 Bad Request if slug format is invalid or reserved

---

## Phase 7: Spring Security Configuration

- [ ] T021 [SIMPLE] Update SecurityConfig to permit public profile endpoints
  - File: `backend/src/main/java/com/interviewme/config/SecurityConfig.java` (or equivalent)
  - Add `.requestMatchers("/api/public/**").permitAll()` to HttpSecurity config
  - Add `.requestMatchers("/p/**").permitAll()` for SPA route forwarding
  - Verify existing auth endpoints still work
  - Test: unauthenticated GET to `/api/public/profiles/{slug}` returns 200 (not 401/403)

---

## Phase 8: Profile Service Extension (Slug Update)

- [ ] T022 [SIMPLE] Add slug update logic to ProfileService
  - File: `backend/src/main/java/com/interviewme/profile/service/ProfileService.java`
  - Method: `@Transactional ProfileResponse updateSlug(Long profileId, String slug)`
    - Validate slug format (SlugValidator)
    - Check slug not reserved
    - Check slug not taken (existsBySlug)
    - Update profile.slug
    - Return updated profile response
  - Handle duplicate slug: throw DuplicateProfileException with slug context

---

## Phase 9: Frontend - TypeScript Types & API Client

### TypeScript Types

- [ ] T023 [||] [SIMPLE] Create public profile TypeScript interfaces
  - File: `frontend/src/types/publicProfile.ts`
  - Interfaces: PublicProfileResponse, PublicSkillResponse, PublicJobResponse, PublicEducationResponse, PublicProjectResponse, PublicStoryResponse, SeoMetadata, SlugCheckResponse
  - Match backend DTOs exactly (camelCase for JSON)

### API Client

- [ ] T024 [MODULAR] Create Public Profile API client
  - File: `frontend/src/api/publicProfileApi.ts`
  - Functions:
    - `getPublicProfile(slug: string): Promise<PublicProfileResponse>` (NO auth header)
    - `checkSlugAvailability(slug: string): Promise<SlugCheckResponse>` (WITH auth)
    - `updateSlug(profileId: number, slug: string): Promise<ProfileResponse>` (WITH auth)
  - Public profile API calls must NOT include JWT token in headers

---

## Phase 10: Frontend - TanStack Query Hooks

- [ ] T025 [MODULAR] Create public profile hooks
  - File: `frontend/src/hooks/usePublicProfile.ts`
  - Hooks:
    - `usePublicProfile(slug: string)` (useQuery) - query key: ['publicProfile', slug], no auth
    - `useCheckSlug(slug: string)` (useQuery, enabled when slug.length >= 3) - query key: ['slugCheck', slug], with auth, debounced
    - `useUpdateSlug()` (useMutation) - invalidates profile query on success

---

## Phase 11: Frontend - React Components

### Public Profile Page

- [ ] T026 [MODULAR] Create PublicProfilePage
  - File: `frontend/src/pages/PublicProfilePage.tsx`
  - Route: `/p/:slug`
  - Extract slug from URL params
  - Use `usePublicProfile(slug)` hook
  - Render loading state (skeleton), error state (404 page), and full profile
  - Layout: MUI Container maxWidth="md", sections stacked vertically
  - Conditionally render sections only if data exists (hide empty sections)

### Public Profile Sub-Components

- [ ] T027 [||] [MODULAR] Create PublicProfileHeader
  - File: `frontend/src/components/public-profile/PublicProfileHeader.tsx`
  - Display: Full name (Typography h3), headline (Typography subtitle1), location (with LocationOn icon), language badges (Chip)
  - Professional links as IconButtons: LinkedIn, GitHub, Portfolio (only if present)
  - Responsive: stack vertically on mobile

- [ ] T028 [||] [MODULAR] Create PublicProfileSummary
  - File: `frontend/src/components/public-profile/PublicProfileSummary.tsx`
  - Display: Summary text as Typography body1
  - Only render if summary is non-empty
  - Section header: "About"

- [ ] T029 [||] [MODULAR] Create PublicSkillsSection
  - File: `frontend/src/components/public-profile/PublicSkillsSection.tsx`
  - Group skills by category
  - Each category as section header (Typography h6)
  - Skills as MUI Chips with proficiency indicator (filled dots or stars)
  - Categories ordered by count (most skills first)
  - If > 20 skills total: show first 15 with "Show all N skills" button (Accordion)

- [ ] T030 [||] [MODULAR] Create PublicWorkTimeline
  - File: `frontend/src/components/public-profile/PublicWorkTimeline.tsx`
  - Display jobs as vertical timeline (MUI Timeline from @mui/lab, or custom CSS)
  - Each entry: company (bold), role, date range, location, "Current" badge
  - Expandable achievements/responsibilities section
  - Nest PublicProjectCard components under each job (if projects exist)

- [ ] T031 [||] [MODULAR] Create PublicProjectCard
  - File: `frontend/src/components/public-profile/PublicProjectCard.tsx`
  - Display: title, role, team size, tech stack (as small Chips), architecture type
  - Metrics as key-value pairs (Typography caption)
  - Outcomes text
  - Linked skills as tiny chips
  - Nest PublicStoryCard components under project (if stories exist)

- [ ] T032 [||] [MODULAR] Create PublicStoryCard
  - File: `frontend/src/components/public-profile/PublicStoryCard.tsx`
  - Display: title, linked skill chips, metrics badges
  - MUI Accordion: click to expand STAR sections
  - Expanded view: Situation, Task, Action, Result as labeled paragraphs
  - Each STAR section with subtle label and left-border color coding

- [ ] T033 [||] [MODULAR] Create PublicEducationSection
  - File: `frontend/src/components/public-profile/PublicEducationSection.tsx`
  - Display: education entries as simple list items
  - Each entry: degree, institution, date range, field of study
  - Section header: "Education"

- [ ] T034 [||] [MODULAR] Create PublicProfileNotFound
  - File: `frontend/src/components/public-profile/PublicProfileNotFound.tsx`
  - Display: 404 message with "Profile not found" text
  - Suggestion to check URL
  - Link to homepage
  - Clean, minimal design

- [ ] T035 [||] [MODULAR] Create ChatboxPlaceholder
  - File: `frontend/src/components/public-profile/ChatboxPlaceholder.tsx`
  - Fixed-position FAB (Floating Action Button) in bottom-right
  - Chat bubble icon (MUI ChatBubbleOutline)
  - On click: Tooltip or Snackbar "Recruiter chat coming soon!"
  - Styled to be visually appealing but non-intrusive

### SEO Component

- [ ] T036 [MODULAR] Create PublicProfileSeo
  - File: `frontend/src/components/public-profile/PublicProfileSeo.tsx`
  - Use react-helmet-async for meta tag injection
  - Set: `<title>`, `<meta description>`, Open Graph tags, Twitter Card tags, canonical URL
  - Generate JSON-LD structured data (schema.org/Person)
  - Props: SeoMetadata from API response + additional profile data

---

## Phase 12: Slug Management UI (Profile Editor Extension)

- [ ] T037 [MODULAR] Create SlugSettingsSection component
  - File: `frontend/src/components/profile/SlugSettingsSection.tsx`
  - TextField with startAdornment: "interviewme.app/p/"
  - Live validation: format check (client-side), availability check (debounced API call)
  - Status indicators: green check (available), red X (taken), spinner (checking)
  - Suggestions shown when slug is taken
  - "Save" button to update slug
  - "Preview" button to open public profile in new tab
  - "Copy Link" button to copy URL to clipboard
  - Warning text when changing existing slug

- [ ] T038 [SIMPLE] Integrate SlugSettingsSection into ProfileEditorPage
  - File: `frontend/src/pages/ProfileEditorPage.tsx`
  - Add "Public Profile" tab or section
  - Render SlugSettingsSection component
  - Pass profile data and mutation hooks

---

## Phase 13: Routing & Dependencies

- [ ] T039 [SIMPLE] Add public profile route to App.tsx
  - File: `frontend/src/App.tsx`
  - Add route: `<Route path="/p/:slug" element={<PublicProfilePage />} />` (NOT wrapped in ProtectedRoute)
  - This route must be outside of any auth wrapper

- [ ] T040 [SIMPLE] Install react-helmet-async dependency
  - Command: `cd frontend && npm install react-helmet-async`
  - Add HelmetProvider to App.tsx root (wrap around BrowserRouter)
  - Verify build works

---

## Phase 14: Testing

### Backend Unit Tests

- [ ] T041 [||] [JAVA25] Write PublicProfileService unit tests
  - File: `backend/src/test/java/com/interviewme/publicprofile/service/PublicProfileServiceTest.java`
  - Use @ExtendWith(MockitoExtension.class), mock all repositories
  - Test: getPublicProfile_shouldReturnOnlyPublicContent
  - Test: getPublicProfile_shouldThrow404ForUnknownSlug
  - Test: getPublicProfile_shouldThrow404ForDeletedProfile
  - Test: getPublicProfile_shouldReturnEmptyListsWhenNoPublicContent
  - Test: getPublicProfile_shouldNestProjectsUnderJobs
  - Test: getPublicProfile_shouldNestStoriesUnderProjects
  - Test: checkSlugAvailability_shouldReturnAvailable
  - Test: checkSlugAvailability_shouldReturnUnavailableWithSuggestions

- [ ] T042 [||] [SIMPLE] Write SlugValidator unit tests
  - File: `backend/src/test/java/com/interviewme/publicprofile/util/SlugValidatorTest.java`
  - Test: valid slugs ("fernando-gomes", "john123", "abc")
  - Test: invalid slugs ("-start", "end-", "a", "ab", "UPPERCASE", "sp aces", "special!chars", consecutive hyphens "a--b")
  - Test: reserved slugs ("admin", "api", "login")

### Backend Integration Tests

- [ ] T043 [SIMPLE] Write PublicProfileController integration tests
  - File: `backend/src/test/java/com/interviewme/publicprofile/controller/PublicProfileControllerIntegrationTest.java`
  - Use @SpringBootTest, @AutoConfigureMockMvc
  - Test: GET /api/public/profiles/{slug} without auth -> 200 OK with public data
  - Test: GET /api/public/profiles/{slug} -> response contains NO internal IDs (no profileId, tenantId, userId)
  - Test: GET /api/public/profiles/nonexistent -> 404 Not Found
  - Test: GET /api/public/profiles/{slug} with mixed visibility -> only public items returned
  - Test: Private jobs, skills, stories NEVER appear in response

- [ ] T044 [SIMPLE] Write slug management integration tests
  - File: `backend/src/test/java/com/interviewme/profile/SlugManagementIntegrationTest.java`
  - Test: PUT /api/profiles/{id}/slug with valid slug -> 200 OK, slug set
  - Test: PUT /api/profiles/{id}/slug with taken slug -> 409 Conflict
  - Test: PUT /api/profiles/{id}/slug with invalid format -> 400 Bad Request
  - Test: PUT /api/profiles/{id}/slug with reserved word -> 400 Bad Request
  - Test: GET /api/profiles/slug/check?slug=available-slug -> available: true
  - Test: GET /api/profiles/slug/check?slug=taken-slug -> available: false, with suggestions

### Visibility Enforcement Tests

- [ ] T045 [MODULAR] Write comprehensive visibility filtering tests
  - File: `backend/src/test/java/com/interviewme/publicprofile/VisibilityEnforcementTest.java`
  - Setup: Create profile with:
    - 3 public jobs + 2 private jobs
    - 15 public skills + 5 private skills (if Feature 003 exists)
    - 2 public stories + 2 private stories (if Feature 004 exists)
    - 1 public education + 1 private education
  - Test: Public endpoint returns exactly 3 jobs, 15 skills, 2 stories, 1 education
  - Test: No private data in any field of the response
  - Test: Response does not contain any ID fields (profileId, tenantId, etc.)

### Frontend Tests

- [ ] T046 [||] [SIMPLE] Write PublicProfilePage tests
  - File: `frontend/src/pages/PublicProfilePage.test.tsx`
  - Test: should render profile header with name and headline
  - Test: should display skills grouped by category
  - Test: should render work timeline
  - Test: should show 404 page for invalid slug
  - Test: should show loading skeleton while fetching

- [ ] T047 [||] [SIMPLE] Write PublicSkillsSection tests
  - File: `frontend/src/components/public-profile/PublicSkillsSection.test.tsx`
  - Test: should group skills by category
  - Test: should show proficiency indicators
  - Test: should not render when skills array is empty

- [ ] T048 [||] [SIMPLE] Write SlugSettingsSection tests
  - File: `frontend/src/components/profile/SlugSettingsSection.test.tsx`
  - Test: should show availability status
  - Test: should validate slug format client-side
  - Test: should debounce availability check API calls

---

## Phase 15: Verification & Documentation

- [ ] T049 [SIMPLE] Verify migration runs successfully
  - Command: `./gradlew backend:bootRun`
  - Verify: slug column added to profile table
  - Verify: idx_profile_slug unique index created
  - Verify: view_count column added with default 0

- [ ] T050 [SIMPLE] Run all backend tests
  - Command: `./gradlew backend:test`
  - Verify: All unit and integration tests pass
  - Verify: No private data leaks in public endpoint tests

- [ ] T051 [SIMPLE] Run all frontend tests
  - Command: `cd frontend && npm test`
  - Verify: All React tests pass

- [ ] T052 [SIMPLE] Manual E2E testing
  - Test: Set slug "test-user" in profile editor
  - Test: Open /p/test-user in incognito window (no auth) -> profile loads
  - Test: Verify only public content visible
  - Test: Verify private jobs/skills/stories NOT shown
  - Test: Test on mobile viewport (Chrome DevTools)
  - Test: Verify meta tags in page source (View Source)
  - Test: Test 404 for invalid slug /p/nonexistent
  - Test: Change slug, verify old URL returns 404
  - Test: Click "Copy Link" button, verify clipboard content

---

## Checkpoints

**After Phase 1 (Database Schema):**
- [ ] Migration runs successfully
- [ ] slug column added, unique index created
- [ ] Existing profiles unaffected (slug = NULL)

**After Phase 5 (Service):**
- [ ] PublicProfileService aggregates public data correctly
- [ ] Private content never included in response
- [ ] 404 returned for unknown/deleted slugs
- [ ] Graceful handling when Feature 003/004 not implemented

**After Phase 6+7 (Controller + Security):**
- [ ] GET /api/public/profiles/{slug} works without authentication
- [ ] Response contains zero internal IDs
- [ ] 404 for missing slug
- [ ] Slug management endpoints require auth

**After Phase 11 (Frontend Components):**
- [ ] PublicProfilePage renders with all sections
- [ ] Responsive layout works on mobile/tablet/desktop
- [ ] Empty sections hidden (not shown with "no data")
- [ ] 404 page displayed for invalid slugs
- [ ] SEO meta tags present in HTML

**After Phase 14 (Testing):**
- [ ] All tests pass (unit, integration, visibility)
- [ ] No private data leaks verified by tests
- [ ] Slug validation covers all edge cases

---

## Progress Summary

**Total Tasks:** 52
**Completed:** 0
**In Progress:** 0
**Blocked:** 0
**Skipped:** 0
**Completion:** 0%

---

## Notes

### Key Dependencies
- Phase 2-4 depend on Phase 1 (migration adds slug column)
- Phase 5 (service) depends on Phase 2-4 (entity, DTOs, repos)
- Phase 6 (controller) depends on Phase 5 (service)
- Phase 7 (security) can run in parallel with Phase 6
- Phase 8 (ProfileService slug) can run in parallel with Phase 5-6
- Phase 9-11 (frontend) depends on Phase 6 for API contract
- Phase 12 (slug management UI) depends on Phase 8 (slug update endpoint)
- Phase 14 (testing) depends on all previous phases

### Parallel Execution Opportunities
- Within Phase 3: All DTOs can be created in parallel
- Within Phase 4: All repository extensions can be done in parallel
- Within Phase 9: TypeScript types and API client can be done in parallel
- Within Phase 11: All sub-components can be written in parallel
- Within Phase 14: Unit tests can be written in parallel with integration tests

### Risk Mitigation
- **Privacy Leak Prevention:** Test visibility filtering exhaustively; no internal IDs in response
- **N+1 Queries:** Profile aggregation should use max 5 database queries (profile, jobs, education, skills, projects+stories)
- **Feature 003/004 Dependency:** Use optional dependency injection (@Autowired(required = false)) or try-catch for missing repos
- **SEO Effectiveness:** React Helmet meta tags work for social sharing; true SEO may need SSR (future enhancement)

### Integration Points
- **Feature 006 (Recruiter Chat):** Will replace ChatboxPlaceholder with actual chat widget
- **Feature 008 (Resume Export):** May add "Download Resume" button to public profile (future)
- **Feature 004 (Projects/Stories):** Public profile displays project and story data when available

---

**Last Updated:** 2026-02-24
**Next Review:** After Phase 1 completion (database migration)
