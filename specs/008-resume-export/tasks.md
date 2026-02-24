# Tasks: Resume Export (PDF)

**Feature ID:** 008-resume-export
**Status:** Not Started
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Branch:** 008-resume-export
**Spec:** [spec.md](./spec.md)
**Plan:** [plan.md](./plan.md)

---

## Task Status Legend

- `[ ]` Not Started
- `[P]` In Progress
- `[X]` Completed
- `[B]` Blocked (add blocker note)
- `[S]` Skipped (add reason)
- `[PAR]` Can run in parallel (different files, no dependencies)
- `[US-N]` Belongs to User Story N from spec.md

---

## Phase 1: Database Foundation (Day 1)

### Database Schema Migrations

- [ ] T001 [DATABASE] Create Liquibase migration for `export_template` table
  - File: `backend/src/main/resources/db/changelog/20260224150000-create-export-template-table.xml`
  - Table: export_template (id BIGSERIAL PK, name VARCHAR(100) NOT NULL, type VARCHAR(50) NOT NULL, description TEXT, template_file VARCHAR(255) NOT NULL, is_active BOOLEAN DEFAULT true NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)
  - Index: `idx_export_template_type` on type column
  - Seed data: INSERT standard resume template (name='Standard Resume', type='RESUME', template_file='templates/exports/standard-resume.html')
  - Rollback: `<dropTable tableName="export_template"/>`
  - **Reference:** plan.md Database Schema, spec.md REQ-DATA-001

- [ ] T002 [DATABASE] Create Liquibase migration for `export_history` table
  - File: `backend/src/main/resources/db/changelog/20260224150100-create-export-history-table.xml`
  - Table: export_history (id BIGSERIAL PK, tenant_id BIGINT FK NOT NULL, profile_id BIGINT FK NOT NULL, template_id BIGINT FK NOT NULL, type VARCHAR(50) NOT NULL, status VARCHAR(20) NOT NULL, parameters JSONB NOT NULL, file_url VARCHAR(500), file_size_bytes BIGINT, coins_spent INTEGER NOT NULL, coin_transaction_id BIGINT, error_message TEXT, retry_count INTEGER DEFAULT 0 NOT NULL, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, completed_at TIMESTAMPTZ)
  - Indexes: idx_export_history_tenant_id, idx_export_history_profile_id, idx_export_history_status, idx_export_history_created_at
  - Foreign keys: tenant_id -> tenant(id), profile_id -> profile(id), template_id -> export_template(id)
  - Check constraint: status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')
  - Rollback: `<dropTable tableName="export_history"/>`
  - **Depends on:** T001
  - **Reference:** plan.md Database Schema, spec.md REQ-DATA-002

- [ ] T003 [DATABASE] Add migrations to db.changelog-master.yaml
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Add include entries for both new migrations (in order after existing migrations)
  - **Depends on:** T001, T002

### JPA Entities

- [ ] T004 [PAR][DATA] Create ExportTemplate JPA entity
  - File: `backend/src/main/java/com/interviewme/exports/model/ExportTemplate.java`
  - Annotations: @Entity, @Table(name = "export_template"), @Data, @NoArgsConstructor, @AllArgsConstructor
  - Fields: id (Long), name (String), type (String), description (String), templateFile (String), isActive (Boolean), createdAt (Instant), updatedAt (Instant)
  - Lifecycle hooks: @PrePersist, @PreUpdate for timestamps
  - **Reference:** plan.md Java Entities

- [ ] T005 [PAR][DATA] Create ExportHistory JPA entity
  - File: `backend/src/main/java/com/interviewme/exports/model/ExportHistory.java`
  - Annotations: @Entity, @Table(name = "export_history"), @Data, @NoArgsConstructor, @AllArgsConstructor, @FilterDef, @Filter for tenantFilter
  - Fields: id, tenantId, profile (@ManyToOne LAZY), template (@ManyToOne EAGER), type, status, parameters (Map JSONB), fileUrl, fileSizeBytes, coinsSpent, coinTransactionId, errorMessage, retryCount, createdAt, updatedAt, completedAt
  - Lifecycle hooks: @PrePersist, @PreUpdate for timestamps
  - **Reference:** plan.md Java Entities

- [ ] T006 [PAR][DATA] Create ExportStatus and ExportType enums
  - File: `backend/src/main/java/com/interviewme/exports/model/ExportStatus.java`
  - Values: PENDING, IN_PROGRESS, COMPLETED, FAILED
  - File: `backend/src/main/java/com/interviewme/exports/model/ExportType.java`
  - Values: RESUME, COVER_LETTER, BACKGROUND_DECK
  - **Reference:** plan.md Service Decomposition

### Validation

- [ ] T007 [DATABASE] Test migrations on fresh PostgreSQL database
  - Run `./gradlew bootRun` to trigger Liquibase migrations
  - Verify tables created: `\d export_template`, `\d export_history`
  - Verify indexes and constraints
  - Verify seed data: SELECT * FROM export_template (should have 1 row)
  - **Depends on:** T001, T002, T003
  - **Reference:** plan.md Phase 1

---

## Phase 2: Backend Core Services (Day 2-3)

### Repositories

- [ ] T008 [PAR][DATA] Create ExportTemplateRepository
  - File: `backend/src/main/java/com/interviewme/exports/repository/ExportTemplateRepository.java`
  - Extends: JpaRepository<ExportTemplate, Long>
  - Query methods:
    - `List<ExportTemplate> findByTypeAndIsActiveTrue(String type)`
    - `List<ExportTemplate> findByIsActiveTrue()`
  - **Reference:** plan.md Service Decomposition

- [ ] T009 [PAR][DATA] Create ExportHistoryRepository
  - File: `backend/src/main/java/com/interviewme/exports/repository/ExportHistoryRepository.java`
  - Extends: JpaRepository<ExportHistory, Long>
  - Query methods:
    - `Page<ExportHistory> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable)`
    - `Optional<ExportHistory> findByIdAndTenantId(Long id, Long tenantId)`
    - `Page<ExportHistory> findByTenantIdAndTypeOrderByCreatedAtDesc(Long tenantId, String type, Pageable pageable)`
    - `Page<ExportHistory> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status, Pageable pageable)`
  - Tenant filter applied via Hibernate filter
  - **Reference:** plan.md Service Decomposition

### DTOs (Java Records)

- [ ] T010 [PAR][JAVA25] Create export DTOs
  - Files in `backend/src/main/java/com/interviewme/exports/dto/`:
    - `ExportResumeRequest.java` - @NotNull templateId, @NotBlank targetRole, location, @NotBlank seniority, language
    - `ExportHistoryResponse.java` - id, template, type, status, parameters, coinsSpent, errorMessage, retryCount, createdAt, completedAt
    - `ExportStatusResponse.java` - id, status, fileUrl (null for non-completed), errorMessage, retryCount, completedAt
    - `ExportTemplateResponse.java` - id, name, type, description
    - `ExportHistoryPageResponse.java` - content, page, size, totalElements, totalPages
  - All as Java records with validation annotations
  - **Reference:** plan.md DTOs section

### Configuration

- [ ] T011 [PAR][CONFIG] Create ExportProperties configuration
  - File: `backend/src/main/java/com/interviewme/exports/config/ExportProperties.java`
  - @ConfigurationProperties(prefix = "exports")
  - Fields: storagePath (default: "./exports"), retry (maxAttempts=3, initialDelayMs=1000, multiplier=2)
  - Add to application.yml:
    ```yaml
    exports:
      storage-path: ./exports
      retry:
        max-attempts: 3
        initial-delay-ms: 1000
        multiplier: 2
    ```
  - Add `resume-export: 10` to existing `billing.costs` section
  - **Reference:** plan.md Configuration

- [ ] T012 [PAR][CONFIG] Create AsyncConfig for virtual thread executor
  - File: `backend/src/main/java/com/interviewme/exports/config/AsyncConfig.java`
  - @Configuration, @EnableAsync
  - Bean: `exportTaskExecutor` returning `Executors.newVirtualThreadPerTaskExecutor()`
  - **Reference:** plan.md Async Job Architecture

### File Storage

- [ ] T013 [MODULAR] Create FileStorageService interface
  - File: `backend/src/main/java/com/interviewme/exports/service/FileStorageService.java`
  - Methods: store(tenantId, fileName, bytes) -> fileUrl, retrieve(fileUrl) -> bytes, delete(fileUrl) -> void
  - **Reference:** plan.md File Storage Design

- [ ] T014 [MODULAR] Create LocalFileStorageService implementation
  - File: `backend/src/main/java/com/interviewme/exports/service/LocalFileStorageService.java`
  - @Service, @Profile("!production"), @RequiredArgsConstructor, @Slf4j
  - Stores files at: `{storagePath}/{tenantId}/resume-{exportId}.pdf`
  - Creates directories if not exists
  - **Depends on:** T013, T011
  - **Reference:** plan.md File Storage Design

### PDF Generation

- [ ] T015 [MODULAR] Create PdfGenerationService
  - File: `backend/src/main/java/com/interviewme/exports/service/PdfGenerationService.java`
  - @Service, @Slf4j
  - Configures Thymeleaf TemplateEngine in standalone mode (ClassLoaderTemplateResolver)
  - Method: `byte[] generatePdf(String templateFile, Map<String, Object> contextVars)`
    1. Create Thymeleaf Context with variables
    2. Render HTML using templateEngine.process()
    3. Create Flying Saucer ITextRenderer
    4. Set document from rendered HTML string
    5. Layout and create PDF to ByteArrayOutputStream
    6. Return byte array
  - Max 150 lines
  - **Reference:** plan.md PDF Generation Pipeline

- [ ] T016 [TEMPLATE] Create standard resume Thymeleaf HTML template
  - File: `backend/src/main/resources/templates/exports/standard-resume.html`
  - HTML5 document with embedded CSS (required for Flying Saucer)
  - CSS: @page A4 size, margins, professional typography (serif headings, sans-serif body)
  - Sections:
    1. **Header:** Full name (h1), headline, location, target role, professional links (LinkedIn, GitHub)
    2. **Summary:** Professional summary paragraph
    3. **Experience:** th:each for jobExperiences, sorted by date desc. Company, role, date range, responsibilities, achievements
    4. **Skills:** th:each for skills grouped by category. Category headers with skill chips (name + proficiency level)
    5. **Education:** th:each for education entries. Institution, degree, dates
    6. **Footer:** Generation date
  - Uses Thymeleaf expressions: th:text, th:each, th:if, th:unless
  - Print-friendly: no background colors, clean layout, reasonable font sizes
  - Max 2 pages for typical profile
  - **Reference:** plan.md PDF Generation Pipeline

### Business Logic Services

- [ ] T017 [MODULAR] Create ExportService (orchestration)
  - File: `backend/src/main/java/com/interviewme/exports/service/ExportService.java`
  - @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: ExportTemplateRepository, ExportHistoryRepository, CoinWalletService, BillingProperties, ProfileService, ExportJobService
  - Methods:
    - `@Transactional ExportHistoryResponse createResumeExport(Long tenantId, Long profileId, ExportResumeRequest request)`:
      1. Validate template exists and is active
      2. Validate profile has minimum data
      3. Get cost from BillingProperties
      4. Call CoinWalletService.spend()
      5. Create ExportHistory(status=PENDING)
      6. Submit async: ExportJobService.processExport(exportHistoryId)
      7. Return ExportHistoryResponse
    - `@Transactional(readOnly=true) ExportStatusResponse getExportStatus(Long tenantId, Long exportId)`
    - `@Transactional(readOnly=true) byte[] downloadExport(Long tenantId, Long exportId)` - verify COMPLETED, retrieve file
    - `@Transactional(readOnly=true) ExportHistoryPageResponse getExportHistory(Long tenantId, int page, int size, String type, String status)`
    - `@Transactional(readOnly=true) List<ExportTemplateResponse> getActiveTemplates()`
  - Max 250 lines
  - **Depends on:** T008, T009, T010, T013, T014, T015
  - **Reference:** plan.md Service Decomposition

- [ ] T018 [MODULAR] Create ExportJobService (async processing)
  - File: `backend/src/main/java/com/interviewme/exports/service/ExportJobService.java`
  - @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: ExportHistoryRepository, ProfileService, SkillService/UserSkillService, PdfGenerationService, FileStorageService, CoinWalletService, ApplicationEventPublisher, ExportProperties
  - Method: `@Async("exportTaskExecutor") void processExport(Long exportHistoryId)`:
    1. Load ExportHistory
    2. Retry loop (maxAttempts from config):
       a. Set status=IN_PROGRESS, increment retryCount
       b. Load profile, job experiences, education, skills
       c. Build Thymeleaf context variables
       d. Call PdfGenerationService.generatePdf()
       e. Call FileStorageService.store()
       f. Set status=COMPLETED, fileUrl, fileSizeBytes, completedAt
       g. Publish ExportCompletedEvent
       h. On transient error: wait (exponential backoff), retry
       i. On permanent error or max retries: handleFailure()
    3. handleFailure(): set FAILED, call CoinWalletService.refund(), log
  - Max 200 lines
  - **Depends on:** T005, T009, T012, T014, T015
  - **Reference:** plan.md Async Job Architecture

### Event

- [ ] T019 [PAR][SIMPLE] Create ExportCompletedEvent
  - File: `backend/src/main/java/com/interviewme/exports/event/ExportCompletedEvent.java`
  - Java record: tenantId (Long), exportId (Long), type (String), fileUrl (String), coinsSpent (Integer), timestamp (Instant)
  - **Reference:** plan.md Event-Driven Architecture

### DTO Mapper

- [ ] T020 [PAR][SIMPLE] Create ExportMapper utility
  - File: `backend/src/main/java/com/interviewme/exports/mapper/ExportMapper.java`
  - Static methods to convert entities to DTOs:
    - `ExportHistoryResponse toResponse(ExportHistory entity)`
    - `ExportTemplateResponse toResponse(ExportTemplate entity)`
    - `ExportStatusResponse toStatusResponse(ExportHistory entity)`
  - **Reference:** plan.md DTOs

---

## Phase 3: REST API Layer (Day 3-4)

### Controller

- [ ] T021 [MODULAR] Create ExportController
  - File: `backend/src/main/java/com/interviewme/exports/controller/ExportController.java`
  - @RestController, @RequestMapping("/api/exports"), @RequiredArgsConstructor, @Slf4j
  - Endpoints:
    - `POST /resume` - @Transactional, @Valid ExportResumeRequest -> 202 ExportHistoryResponse
    - `GET /{id}/status` - @Transactional(readOnly=true) -> 200 ExportStatusResponse
    - `GET /{id}/download` - @Transactional(readOnly=true) -> 200 PDF (application/pdf, Content-Disposition: attachment)
    - `GET /` - @Transactional(readOnly=true), @RequestParam page/size/type/status -> 200 ExportHistoryPageResponse
    - `GET /templates` - @Transactional(readOnly=true) -> 200 List<ExportTemplateResponse>
  - Extract tenantId and profileId from Authentication (JWT)
  - Max 200 lines
  - **Depends on:** T017
  - **Reference:** plan.md API Implementation

### Exception Handling

- [ ] T022 [SIMPLE] Add export-specific exception handlers to GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/config/GlobalExceptionHandler.java` (existing)
  - Add handlers:
    - ExportNotFoundException -> 404 Not Found
    - InsufficientProfileDataException -> 400 Bad Request
    - ExportFileNotFoundException -> 404 Not Found ("File no longer available")
  - Reuse existing InsufficientBalanceException handler (from Feature 007) -> 402
  - **Reference:** spec.md Error Handling

### Transaction Annotations

- [ ] T023 [SIMPLE] Verify @Transactional annotations on all service methods
  - ExportService: @Transactional on createResumeExport, @Transactional(readOnly=true) on getExportStatus, downloadExport, getExportHistory, getActiveTemplates
  - ExportJobService: processExport runs outside request transaction (async), uses programmatic transaction management for status updates
  - PdfGenerationService: no transactions needed (stateless)
  - FileStorageService: no transactions needed (file I/O)
  - **Reference:** CLAUDE.md Spring/JPA Transaction Guidelines

---

## Phase 4: Integration Tests (Day 4)

### Tests

- [ ] T024 [SIMPLE] Write integration tests for ExportController
  - File: `backend/src/test/java/com/interviewme/exports/controller/ExportControllerTest.java`
  - Tests:
    - POST /resume with valid request returns 202 with PENDING status
    - POST /resume with insufficient coins returns 402
    - POST /resume with invalid template ID returns 404
    - POST /resume with empty profile returns 400
    - GET /{id}/status returns current status
    - GET /{id}/download returns PDF for COMPLETED export
    - GET /{id}/download returns 404 for non-COMPLETED export
    - GET / returns paginated export history
    - Tenant isolation: cross-tenant access returns 404
  - Use @SpringBootTest, @AutoConfigureMockMvc, mock JWT authentication
  - Mock CoinWalletService for coin operations
  - **Depends on:** T021
  - **Reference:** plan.md Testing Strategy

- [ ] T025 [SIMPLE] Write unit tests for PdfGenerationService
  - File: `backend/src/test/java/com/interviewme/exports/service/PdfGenerationServiceTest.java`
  - Tests:
    - generatePdf with sample profile data produces non-empty byte array
    - generatePdf output starts with PDF header bytes (%PDF)
    - generatePdf with empty optional fields renders without errors
    - generatePdf with unicode characters renders correctly
  - **Depends on:** T015, T016
  - **Reference:** plan.md Testing Strategy

- [ ] T026 [SIMPLE] Write unit tests for ExportJobService retry logic
  - File: `backend/src/test/java/com/interviewme/exports/service/ExportJobServiceTest.java`
  - Tests:
    - processExport succeeds on first attempt, status=COMPLETED
    - processExport retries on transient failure, succeeds on retry 2
    - processExport fails after max retries, status=FAILED, coins refunded
    - processExport permanent failure, no retry, status=FAILED, coins refunded
    - Exponential backoff delays verified (mock Thread.sleep or use timing assertions)
  - Mock: PdfGenerationService, FileStorageService, CoinWalletService
  - **Depends on:** T018
  - **Reference:** plan.md Testing Strategy

- [ ] T027 [SIMPLE] Write unit tests for LocalFileStorageService
  - File: `backend/src/test/java/com/interviewme/exports/service/LocalFileStorageServiceTest.java`
  - Tests:
    - store creates file in correct directory
    - store creates directories if not exist
    - retrieve reads stored file content
    - delete removes stored file
    - retrieve non-existent file throws exception
  - Use @TempDir for test file isolation
  - **Depends on:** T014
  - **Reference:** plan.md Testing Strategy

---

## Phase 5: Frontend Implementation (Day 5-6)

### TypeScript Types

- [ ] T028 [PAR][SIMPLE] Create export TypeScript types
  - File: `frontend/src/types/export.ts`
  - Interfaces: ExportTemplate, ExportHistory, ExportStatus, ExportResumeRequest, ExportHistoryPage
  - Match Java DTOs exactly
  - **Reference:** plan.md Frontend Design

### API Client

- [ ] T029 [PAR][MODULAR] Create exportsApi client module
  - File: `frontend/src/api/exportsApi.ts`
  - Functions:
    - `createResumeExport(request: ExportResumeRequest): Promise<ExportHistory>`
    - `getExportStatus(exportId: number): Promise<ExportStatus>`
    - `getExportHistory(page, size, type?, status?): Promise<ExportHistoryPage>`
    - `getExportTemplates(): Promise<ExportTemplate[]>`
    - `downloadExport(exportId: number): Promise<Blob>` - GET /download as blob
  - Use axios with auth headers from existing authUtils
  - Max 150 lines
  - **Reference:** plan.md Frontend Design

### React Query Hooks

- [ ] T030 [PAR][MODULAR] Create useExports hooks
  - File: `frontend/src/hooks/useExports.ts`
  - Hooks:
    - `useExportTemplates()` - Query for available templates
    - `useExportHistory(page, size, type?, status?)` - Query for export history
    - `useExportStatus(exportId: number | null)` - Poll status every 3s while PENDING/IN_PROGRESS, stop on COMPLETED/FAILED
    - `useCreateResumeExport()` - Mutation for triggering export
  - React Query configuration: refetchInterval for polling, invalidation on mutation
  - Max 150 lines
  - **Reference:** plan.md Frontend Design, polling logic

### UI Components

- [ ] T031 [MODULAR] Create ExportStatusBadge component
  - File: `frontend/src/components/exports/ExportStatusBadge.tsx`
  - Props: status (string)
  - Renders MUI Chip with color: PENDING=yellow, IN_PROGRESS=blue, COMPLETED=green, FAILED=red
  - Max 50 lines

- [ ] T032 [MODULAR] Create ExportFormDialog component
  - File: `frontend/src/components/exports/ExportFormDialog.tsx`
  - Props: open (bool), onClose (fn), onSubmit (fn), templates (ExportTemplate[])
  - Form fields:
    - Template selector (MUI Select, populated from templates)
    - Target role (MUI TextField, required, max 200 chars)
    - Location (MUI TextField, optional)
    - Seniority level (MUI Select: Junior, Mid, Senior, Lead, Principal, Director)
    - Language (MUI Select: English, Portuguese, Spanish; default English)
  - On submit: calls CoinConfirmationDialog (from Feature 007) first, then onSubmit
  - Inline validation with error messages
  - Max 250 lines
  - **Reference:** spec.md REQ-UI-002

- [ ] T033 [MODULAR] Create ExportProgressCard component
  - File: `frontend/src/components/exports/ExportProgressCard.tsx`
  - Props: exportId (number), onComplete (fn)
  - Uses useExportStatus hook for polling
  - Displays: spinner + "Generating your resume..." while PENDING/IN_PROGRESS
  - On COMPLETED: success message + download button
  - On FAILED: error message + refund note
  - Max 150 lines
  - **Reference:** spec.md REQ-UI-003

- [ ] T034 [MODULAR] Create ExportHistoryTable component
  - File: `frontend/src/components/exports/ExportHistoryTable.tsx`
  - Props: exports (ExportHistory[]), page, totalPages, onPageChange, onDownload
  - MUI Table with columns: Date, Template, Target Role, Status (ExportStatusBadge), Coins Spent, Actions
  - Completed: Download button
  - Failed: "Refunded" note next to coins spent
  - Pagination controls
  - Max 200 lines

### Pages

- [ ] T035 [MODULAR] Create ExportsPage component
  - File: `frontend/src/pages/ExportsPage.tsx`
  - Layout:
    - Page header: "Exports" title + "New Resume Export" button
    - Active export progress card (if any export is PENDING/IN_PROGRESS)
    - Export history table with pagination
    - Empty state: "No exports yet. Generate your first resume to get started."
  - Uses useExportHistory, useExportTemplates hooks
  - Opens ExportFormDialog on "New Resume Export" click
  - Handles download via exportsApi.downloadExport() -> create blob URL -> trigger download
  - Max 300 lines
  - **Depends on:** T029, T030, T031, T032, T033, T034
  - **Reference:** spec.md REQ-UI-001

### Routing

- [ ] T036 [SIMPLE] Add exports route to React Router
  - File: `frontend/src/App.tsx` (or equivalent router config)
  - Route: `/exports` -> ExportsPage (protected, requires authentication)
  - **Depends on:** T035

- [ ] T037 [SIMPLE] Add "Exports" navigation menu item
  - File: `frontend/src/components/Navigation.tsx` (or equivalent)
  - Add "Exports" menu item in main navigation (routes to /exports)
  - Icon: download or document icon
  - **Depends on:** T035

---

## Phase 6: End-to-End Testing (Day 7)

### E2E Validation

- [ ] T038 [E2E] Full export flow manual testing
  - Steps:
    1. Ensure profile exists with job experiences, education, skills
    2. Ensure wallet has coins (admin grant if needed)
    3. Navigate to /exports
    4. Click "New Resume Export"
    5. Fill form: Standard Resume, target role, location, seniority
    6. Confirm coin spend
    7. Observe progress (PENDING -> IN_PROGRESS -> COMPLETED)
    8. Download PDF
    9. Open PDF and verify content matches profile data
    10. Verify coins deducted in billing page
    11. Verify export appears in history table
  - **Depends on:** All previous tasks

- [ ] T039 [E2E] Failed export flow manual testing
  - Steps:
    1. Simulate failure (temporarily break template or mock error)
    2. Trigger export
    3. Observe retry attempts in logs
    4. Verify status transitions to FAILED
    5. Verify coins auto-refunded
    6. Verify refund visible in billing transactions
  - **Depends on:** All previous tasks

---

## Checkpoints

After each phase, verify:

- [ ] **Phase 1 Complete:** Migrations run successfully, tables created with correct indexes and constraints, seed data present, entities map correctly
- [ ] **Phase 2 Complete:** Services compile, PDF generation produces valid PDF from test data, file storage works, async executor configured
- [ ] **Phase 3 Complete:** All endpoints return correct HTTP status codes, coin integration works (spend before export, refund on failure), tenant isolation enforced
- [ ] **Phase 4 Complete:** Integration tests pass, retry logic verified, file storage tested, concurrent export test passes
- [ ] **Phase 5 Complete:** Export form works, progress polling shows real-time status, download works, history table displays correctly
- [ ] **Phase 6 Complete:** Full E2E flow works: create export -> poll status -> download PDF. Failed export refunds coins automatically.

---

## Success Criteria Verification

### From spec.md

1. **PDF Generation Works**
   - [ ] Generate PDF with full profile (5 jobs, 3 education, 15 skills)
   - [ ] Verify formatting: clean layout, proper sections, 1-2 pages

2. **Async Processing Completes**
   - [ ] Load test with 10 concurrent export requests
   - [ ] All complete within 30 seconds

3. **Coin Integration Works**
   - [ ] Trigger export: 10 coins deducted
   - [ ] Trigger failing export: 10 coins refunded

4. **Retry Logic Functions**
   - [ ] Mock transient failure: verify 3 retry attempts
   - [ ] Verify exponential backoff delays (1s, 2s, 4s)

5. **File Download Works**
   - [ ] Generate export, download file
   - [ ] Verify it is a valid PDF (starts with %PDF header)

6. **Job Status Polling Works**
   - [ ] Trigger export, observe status transitions on UI

7. **Tenant Isolation Verified**
   - [ ] Tenant A cannot download tenant B's export (404)

---

## Notes

- All tasks should be completed in order within each phase (dependencies noted)
- Tasks marked `[PAR]` can run in parallel (different files, no shared state)
- Refer to plan.md for detailed implementation guidance
- Refer to spec.md for functional requirements and success criteria
- Feature 007 (Coin Wallet) MUST be implemented before this feature can fully function
- The resume Thymeleaf template (T016) may require iterative refinement based on PDF output quality

---

**Tasks Version:** 1.0.0
**Last Updated:** 2026-02-24
**Total Tasks:** 39
**Estimated Time:** 5-7 days
