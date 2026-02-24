# Feature Specification: Resume Export (PDF)

**Feature ID:** 008-resume-export
**Status:** Draft
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Overview

### Problem Statement

Career professionals need to generate tailored, professional PDF resumes from their structured profile data. Currently, users maintain their career information (profile, job experiences, education, skills, stories) in the platform but have no way to export this data into a polished, recruiter-ready PDF document. Without resume export:
- Users cannot leverage their structured profile data for job applications
- The platform provides no tangible output that justifies coin-based monetization
- Users must manually copy-paste data into external resume builders, defeating the purpose of a centralized career hub
- There is no way to tailor resume content for specific roles, locations, or seniority levels

### Feature Summary

Implement a server-side PDF resume generation system that transforms structured profile data (profile, job experiences, education, skills, stories) into professionally formatted PDF documents. The system uses a template engine (Thymeleaf) to render HTML resume layouts and a PDF generation library (OpenPDF or Flying Saucer) to convert them to PDF. Exports are processed asynchronously (Spring @Async with virtual threads) with job status tracking (PENDING -> IN_PROGRESS -> COMPLETED -> FAILED). All exports cost coins (integrated with Feature 007 billing). Failed exports auto-refund coins. Generated files are stored on the local filesystem for development and cloud storage (S3) for production.

### Target Users

- **Career Professionals (Primary User):** Users generating PDF resumes tailored to specific job applications with role, location, and seniority customization
- **Platform Admin:** Manages export templates, monitors export job health, handles failed export support cases
- **System (Internal):** Async job processor that generates PDFs, manages file storage, and handles retries

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Standard REST endpoints for export requests, async processing with Spring @Async, straightforward template-based PDF generation pipeline
- **Principle 3: Modern Java Standards** - Java 25 virtual threads for async processing, records for DTOs, modern Spring Boot 4.x patterns
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - ExportTemplate shared across tenants (no tenant_id), ExportHistory includes tenant_id with automatic Hibernate filtering, generated files stored in tenant-isolated paths
- **Principle 6: Observability and Debugging** - Structured logging for export job lifecycle (submitted, started, completed, failed), metrics for export success/failure rates and generation time
- **Principle 7: Security, Privacy, and Credential Management** - JWT-authenticated endpoints, @Transactional annotations, file access restricted to owning tenant
- **Principle 8: Multi-Tenant Architecture** - ExportHistory tenant-isolated, file storage paths include tenant_id prefix for isolation
- **Principle 9: Freemium Model and Coin-Based Monetization** - All exports cost coins, auto-refund on failure, cost displayed before export, integration with CoinWalletService
- **Principle 10: Full-Stack Modularity** - `com.interviewme.exports` package with controller/service/repository/model/dto layers, frontend ExportsPage component
- **Principle 11: Database Schema Evolution** - Timestamp-based Liquibase migrations for export_template and export_history tables
- **Principle 12: Async Job Processing** - Export generation runs asynchronously with PENDING -> IN_PROGRESS -> COMPLETED -> FAILED status tracking, retry with exponential backoff (max 3 retries), job status API for frontend polling
- **Principle 14: Event-Driven Architecture** - Publish ExportCompletedEvent on completion for audit trail and reactive UI updates

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: User Exports a Standard Resume PDF

**Actor:** Career professional applying for a job
**Goal:** Generate a professionally formatted 1-2 page PDF resume tailored to a target role
**Preconditions:** User has authenticated account, Profile exists with at least 1 job experience and 3 skills, coins available in wallet

**Steps:**
1. User navigates to "Exports" page
2. User clicks "New Resume Export" button
3. System displays export form with fields: template (Standard Resume), target role, location, seniority level, language (optional)
4. User fills in: template=Standard Resume, target role="Senior Backend Engineer", location="Berlin, Germany", seniority="Senior"
5. System displays cost confirmation dialog: "This will cost 10 coins. Current balance: 50 coins."
6. User confirms
7. System deducts 10 coins from wallet, creates ExportHistory record with status=PENDING
8. System submits async job to generate PDF
9. Frontend polls job status API every 3 seconds
10. Async job processes: loads profile data, renders Thymeleaf template with data, converts HTML to PDF
11. Job completes, stores PDF file, updates ExportHistory with status=COMPLETED, file_url
12. Frontend detects COMPLETED status, displays download link
13. User clicks download link, receives PDF file

**Success Criteria:**
- PDF generated within 30 seconds
- PDF contains: full name, headline, summary, job experiences (sorted by date desc), education, skills (grouped by category), target role/location in header
- Coins deducted before generation starts
- Download link works and serves correct PDF
- ExportHistory record contains all metadata (template, params, file_url, coins_spent, status)

#### Scenario 2: Export Fails and Coins Are Auto-Refunded

**Actor:** Career professional
**Goal:** Ensure no coins are lost when export generation fails
**Preconditions:** User has triggered a resume export, coins have been deducted

**Steps:**
1. User triggers resume export, 10 coins deducted
2. Async job starts processing
3. PDF generation fails (e.g., template rendering error, out of memory)
4. System retries with exponential backoff (attempt 1: 1s, attempt 2: 2s, attempt 3: 4s)
5. All 3 retry attempts fail
6. System updates ExportHistory with status=FAILED, error message
7. System auto-refunds 10 coins to wallet (CoinTransaction type=REFUND, refType=EXPORT_REFUND)
8. Frontend detects FAILED status, displays error message: "Resume export failed. Your 10 coins have been refunded."
9. User sees refund in transaction history

**Success Criteria:**
- Coins auto-refunded within 5 seconds of final failure
- ExportHistory status set to FAILED with error details
- Refund transaction linked to original spend via refId
- User clearly informed of both failure and refund
- Retry attempts logged with exponential backoff timing

#### Scenario 3: User Views Export History

**Actor:** Career professional
**Goal:** View previously generated exports and download them
**Preconditions:** User has generated at least one export

**Steps:**
1. User navigates to "Exports" page
2. Page displays export history table with columns: date, template name, target role, status, coins spent, actions
3. Completed exports show "Download" button
4. Failed exports show "Failed" badge with error details on hover
5. Pending/in-progress exports show spinner with status
6. User clicks "Download" on a completed export
7. System serves the stored PDF file

**Success Criteria:**
- Export history shows all user exports, ordered by date desc
- Download works for completed exports
- Failed exports clearly indicate failure with refund note
- In-progress exports show real-time status updates via polling

#### Scenario 4: User Checks Export Cost Before Proceeding

**Actor:** Career professional with low coin balance
**Goal:** See the cost of an export before committing
**Preconditions:** User has 5 coins, export costs 10 coins

**Steps:**
1. User navigates to "Exports" page and clicks "New Resume Export"
2. User fills in export parameters
3. System displays cost confirmation dialog: "This will cost 10 coins. Current balance: 5 coins. Insufficient coins."
4. Confirm button is disabled
5. User navigates to Billing page to request more coins from admin

**Success Criteria:**
- Cost clearly displayed before action
- Insufficient balance clearly communicated
- Confirm button disabled when balance too low
- User directed to billing page for coin acquisition

### Edge Cases

- **Empty profile:** User with no job experiences or skills triggers export -- system generates a minimal resume with name/headline only, or returns validation error requiring minimum data
- **Very large profile:** User with 20+ job experiences and 50+ skills -- PDF may exceed 2 pages, system truncates or paginates appropriately
- **Concurrent exports:** User triggers two exports simultaneously -- both processed independently, each deducting coins
- **Export while profile is being updated:** Profile changes mid-generation -- export uses snapshot of profile data at job start time
- **File storage full:** Local disk or S3 bucket full -- job fails with storage error, coins refunded
- **Template not found:** Requested template ID doesn't exist -- 404 error before coins are deducted
- **Unicode content:** Profile contains non-Latin characters (Japanese, Arabic) -- PDF renders correctly with Unicode-compatible fonts
- **Expired download link:** User tries to download export after file retention period -- 404 or redirect to re-generate

---

## Functional Requirements

### Core Capabilities

**REQ-001:** ExportTemplate Entity
- **Description:** Platform MUST provide export template definitions that describe available resume formats
- **Acceptance Criteria:**
  - ExportTemplate includes: id (BIGSERIAL PK), name (VARCHAR 100 NOT NULL), type (VARCHAR 50 NOT NULL: RESUME, COVER_LETTER, BACKGROUND_DECK), description (TEXT), template_file (VARCHAR 255 NOT NULL, path to Thymeleaf template), is_active (BOOLEAN DEFAULT true), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL)
  - MVP ships with 1 active template: "Standard Resume" (1-2 page professional format)
  - Templates are shared across all tenants (no tenant_id)
  - Deactivated templates cannot be selected for new exports but existing exports remain
  - Template file references a Thymeleaf HTML template on the classpath

**REQ-002:** ExportHistory Entity
- **Description:** System MUST record every export request with full metadata, status, and file reference
- **Acceptance Criteria:**
  - ExportHistory includes: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), profile_id (BIGINT FK NOT NULL), template_id (BIGINT FK NOT NULL), type (VARCHAR 50 NOT NULL: RESUME, COVER_LETTER, BACKGROUND_DECK), status (VARCHAR 20 NOT NULL: PENDING, IN_PROGRESS, COMPLETED, FAILED), parameters (JSONB NOT NULL: target_role, location, seniority, language), file_url (VARCHAR 500), file_size_bytes (BIGINT), coins_spent (INTEGER NOT NULL), error_message (TEXT), retry_count (INTEGER DEFAULT 0), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), completed_at (TIMESTAMPTZ)
  - Status transitions: PENDING -> IN_PROGRESS -> COMPLETED or FAILED
  - Parameters stored as JSONB for flexibility (different export types may have different params)
  - file_url populated only when status=COMPLETED
  - error_message populated only when status=FAILED
  - Tenant-isolated: all queries filtered by tenant_id

**REQ-003:** Resume Export Request (Trigger Export)
- **Description:** User MUST be able to request a resume export by specifying template and parameters
- **Acceptance Criteria:**
  - Endpoint: POST `/api/exports/resume`
  - Request body: `{ "templateId": 1, "targetRole": "Senior Backend Engineer", "location": "Berlin", "seniority": "Senior", "language": "en" }`
  - Validates: template exists and is active, profile has minimum data (at least name and headline), wallet has sufficient coins
  - Coin cost deducted BEFORE async job starts (fail-fast on insufficient balance)
  - Creates ExportHistory with status=PENDING
  - Returns ExportHistory DTO with id and status for polling
  - HTTP 202 Accepted (async processing)

**REQ-004:** Async PDF Generation Pipeline
- **Description:** System MUST generate PDFs asynchronously using template engine + PDF converter
- **Acceptance Criteria:**
  - Job processing flow:
    1. Update ExportHistory status to IN_PROGRESS
    2. Load profile data (profile, job experiences, education, skills, stories)
    3. Prepare template model (Thymeleaf context variables)
    4. Render Thymeleaf HTML template with profile data and parameters
    5. Convert rendered HTML to PDF using Flying Saucer (xhtmlrenderer) or OpenPDF
    6. Store PDF file (local filesystem for dev, S3 for production)
    7. Update ExportHistory with status=COMPLETED, file_url, file_size_bytes, completed_at
    8. Publish ExportCompletedEvent
  - Uses Spring @Async with virtual threads executor
  - Template engine: Thymeleaf (standalone mode, not web mode)
  - PDF library: Flying Saucer (xhtmlrenderer) for HTML-to-PDF conversion with CSS support

**REQ-005:** Retry with Exponential Backoff
- **Description:** Failed export jobs MUST retry with exponential backoff up to 3 attempts
- **Acceptance Criteria:**
  - Retry delays: attempt 1 after 1 second, attempt 2 after 2 seconds, attempt 3 after 4 seconds
  - retry_count incremented on each attempt
  - After max retries exhausted: set status=FAILED, auto-refund coins
  - Transient errors (timeouts, temporary I/O failures) trigger retry
  - Permanent errors (invalid template, missing profile data) fail immediately without retry
  - Each retry attempt logged with attempt number and delay

**REQ-006:** Auto-Refund on Failure
- **Description:** System MUST automatically refund coins when export generation permanently fails
- **Acceptance Criteria:**
  - Calls CoinWalletService.refund(tenantId, originalTransactionId) on final failure
  - Refund creates CoinTransaction with type=REFUND, refType=EXPORT_REFUND, refId=exportHistoryId
  - Refund amount equals original coins_spent
  - Prevents duplicate refunds (idempotent operation)
  - Logs refund event with export ID and error details

**REQ-007:** Job Status API (Polling)
- **Description:** Frontend MUST be able to poll for export job status
- **Acceptance Criteria:**
  - Endpoint: GET `/api/exports/{exportId}/status`
  - Returns: ExportHistory DTO with current status, file_url (if completed), error_message (if failed)
  - Frontend polls every 3 seconds while status is PENDING or IN_PROGRESS
  - Stops polling when status is COMPLETED or FAILED
  - Tenant-isolated: users can only check their own exports

**REQ-008:** File Download
- **Description:** Users MUST be able to download completed export files
- **Acceptance Criteria:**
  - Endpoint: GET `/api/exports/{exportId}/download`
  - Returns PDF file as binary stream with Content-Type: application/pdf
  - Content-Disposition header for browser download: `attachment; filename="resume-{date}.pdf"`
  - Tenant-isolated: users can only download their own exports
  - Returns 404 if export not found or not COMPLETED
  - Returns 404 if file no longer exists on storage

**REQ-009:** Export History List
- **Description:** Users MUST be able to view their export history
- **Acceptance Criteria:**
  - Endpoint: GET `/api/exports`
  - Returns paginated list of ExportHistory for current tenant
  - Ordered by created_at DESC
  - Filterable by type (RESUME, COVER_LETTER, BACKGROUND_DECK) and status
  - Page size: 20 (configurable)
  - Tenant-isolated

**REQ-010:** File Storage Abstraction
- **Description:** System MUST support multiple storage backends (local filesystem for dev, S3 for production)
- **Acceptance Criteria:**
  - FileStorageService interface with methods: store(tenantId, fileName, bytes) -> fileUrl, retrieve(fileUrl) -> bytes, delete(fileUrl) -> void
  - LocalFileStorageService implementation for development (stores in configurable directory, default: `./exports/`)
  - S3FileStorageService implementation for production (stores in configurable S3 bucket with tenant-prefixed keys)
  - Active implementation selected via Spring profile or configuration property
  - File paths include tenant_id prefix for isolation: `{tenantId}/{exportType}/{exportId}.pdf`

### User Interface Requirements

**REQ-UI-001:** Exports Page
- **Description:** Frontend MUST provide an exports page for triggering new exports and viewing history
- **Acceptance Criteria:**
  - Route: `/exports`
  - Header with "New Resume Export" button
  - Export history table with columns: Date, Template, Target Role, Status (colored badge), Coins Spent, Actions (Download/Retry)
  - Completed exports: green badge, download button
  - Failed exports: red badge, error tooltip, "coins refunded" note
  - Pending/in-progress exports: yellow/blue badge, spinner
  - Pagination controls
  - Empty state: "No exports yet. Generate your first resume to get started."

**REQ-UI-002:** Export Form Dialog
- **Description:** Frontend MUST provide a form dialog for configuring export parameters
- **Acceptance Criteria:**
  - Template selector (dropdown, initially just "Standard Resume")
  - Target role (text input, required, max 200 chars)
  - Location (text input, optional, max 200 chars)
  - Seniority level (dropdown: Junior, Mid, Senior, Lead, Principal, Director)
  - Language (dropdown: English, Portuguese, Spanish -- optional, default English)
  - "Generate Resume" button triggers cost confirmation dialog first

**REQ-UI-003:** Export Progress Indicator
- **Description:** Frontend MUST show real-time progress for in-flight exports
- **Acceptance Criteria:**
  - After triggering export: show progress card with status updates
  - Poll GET `/api/exports/{id}/status` every 3 seconds
  - Display: "Generating your resume..." with spinner
  - On completion: show success message with download button
  - On failure: show error message with refund confirmation

### Data Requirements

**REQ-DATA-001:** ExportTemplate Table Schema
- **Description:** PostgreSQL table for storing export template metadata
- **Acceptance Criteria:**
  - Table name: `export_template`
  - Columns: id (BIGSERIAL PK), name (VARCHAR 100 NOT NULL), type (VARCHAR 50 NOT NULL), description (TEXT), template_file (VARCHAR 255 NOT NULL), is_active (BOOLEAN DEFAULT true NOT NULL), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL)
  - Index: `idx_export_template_type` on type column
  - No tenant_id (templates shared globally)
  - Seed data: 1 standard resume template via Liquibase

**REQ-DATA-002:** ExportHistory Table Schema
- **Description:** PostgreSQL table for recording export job history
- **Acceptance Criteria:**
  - Table name: `export_history`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), profile_id (BIGINT FK NOT NULL), template_id (BIGINT FK NOT NULL), type (VARCHAR 50 NOT NULL), status (VARCHAR 20 NOT NULL), parameters (JSONB NOT NULL), file_url (VARCHAR 500), file_size_bytes (BIGINT), coins_spent (INTEGER NOT NULL), coin_transaction_id (BIGINT), error_message (TEXT), retry_count (INTEGER DEFAULT 0 NOT NULL), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), completed_at (TIMESTAMPTZ)
  - Indexes: `idx_export_history_tenant_id`, `idx_export_history_profile_id`, `idx_export_history_status`, `idx_export_history_created_at`
  - Foreign keys: tenant_id -> tenant(id), profile_id -> profile(id), template_id -> export_template(id)
  - Check constraint: status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')
  - Hibernate tenant filter applied

---

## Success Criteria

The feature will be considered successful when:

1. **PDF Generation Works:** Standard resume template generates a valid, well-formatted 1-2 page PDF from profile data
   - Measurement: Generate PDF with full profile (5 jobs, 3 education, 15 skills), verify formatting and content

2. **Async Processing Completes:** Export jobs complete within 30 seconds for typical profiles
   - Measurement: Load test with 10 concurrent export requests, verify all complete within 30s

3. **Coin Integration Works:** Coins deducted before export, refunded on failure
   - Measurement: Integration test: trigger export with 10 coins, verify deduction; trigger failing export, verify refund

4. **Retry Logic Functions:** Transient failures retry up to 3 times with exponential backoff
   - Measurement: Mock transient failure, verify 3 retry attempts with 1s/2s/4s delays

5. **File Download Works:** Completed exports serve valid PDF files via download endpoint
   - Measurement: Generate export, download file, verify it is a valid PDF

6. **Job Status Polling Works:** Frontend correctly polls and displays export progress
   - Measurement: Manual UAT: trigger export, observe status transitions on UI

7. **Tenant Isolation Verified:** Users cannot access other tenants' exports
   - Measurement: Integration test: tenant A cannot download tenant B's export

---

## Key Entities

### ExportTemplate

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- name: Template display name (VARCHAR 100, NOT NULL, e.g., "Standard Resume")
- type: Export type (VARCHAR 50, NOT NULL: RESUME, COVER_LETTER, BACKGROUND_DECK)
- description: Template description (TEXT, optional)
- template_file: Classpath reference to Thymeleaf template (VARCHAR 255, NOT NULL, e.g., "templates/exports/standard-resume.html")
- is_active: Availability flag (BOOLEAN, default true)
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)

**Relationships:**
- One-to-many with ExportHistory (one template used by many exports)

**Validation Rules:**
- name: Required, 1-100 characters
- type: Required, must be one of RESUME, COVER_LETTER, BACKGROUND_DECK
- template_file: Required, must reference existing classpath resource

### ExportHistory

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- profile_id: Foreign key to Profile (BIGINT, NOT NULL, indexed)
- template_id: Foreign key to ExportTemplate (BIGINT, NOT NULL)
- type: Export type (VARCHAR 50, NOT NULL: RESUME, COVER_LETTER, BACKGROUND_DECK)
- status: Job status (VARCHAR 20, NOT NULL: PENDING, IN_PROGRESS, COMPLETED, FAILED)
- parameters: Export parameters as JSONB (NOT NULL, e.g., {"targetRole": "Senior Backend Engineer", "location": "Berlin", "seniority": "Senior", "language": "en"})
- file_url: Path or URL to generated file (VARCHAR 500, nullable, populated on COMPLETED)
- file_size_bytes: Size of generated file (BIGINT, nullable)
- coins_spent: Number of coins charged for this export (INTEGER, NOT NULL)
- coin_transaction_id: Reference to CoinTransaction for tracking (BIGINT, nullable)
- error_message: Error details on failure (TEXT, nullable)
- retry_count: Number of retry attempts (INTEGER, DEFAULT 0)
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- completed_at: Completion timestamp (TIMESTAMPTZ, nullable)

**Relationships:**
- Many-to-one with Tenant
- Many-to-one with Profile
- Many-to-one with ExportTemplate

**Validation Rules:**
- status: Must transition in order: PENDING -> IN_PROGRESS -> COMPLETED or FAILED
- parameters: Must be valid JSON matching export type requirements
- coins_spent: Must be > 0
- file_url: Required when status=COMPLETED, null otherwise

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Requires authentication (JWT), tenant filtering (Hibernate filter), database infrastructure, Spring Boot backend
- **Feature 002: Profile CRUD** - Requires Profile, JobExperience, Education entities and data
- **Feature 003: Skills Management** - Requires UserSkill entity for skills section in resume
- **Feature 007: Coin Wallet** - Requires CoinWalletService for spend/refund operations, BillingProperties for cost configuration

### External Dependencies

- **Thymeleaf (Standalone):** Template engine for rendering HTML resume layouts (already available in Spring Boot ecosystem)
- **Flying Saucer (xhtmlrenderer):** HTML-to-PDF conversion with CSS support
  - `org.xhtmlrenderer:flying-saucer-pdf:9.7.2`
- **OpenPDF:** Alternative/supplement for PDF generation
  - `com.github.librepdf:openpdf:2.0.3`
- **Spring @Async:** For async job execution with virtual threads
- **Liquibase:** For database schema migrations

### Future Dependencies (Not Phase 1)

- **Feature 004: Experience Projects & Stories** - Stories can enrich resume content (STAR format case studies)
- **AWS S3 SDK:** For production file storage (deferred to production deployment)

---

## Assumptions

1. Feature 001, 002, 003, and 007 are fully implemented (auth, tenancy, profiles, skills, billing)
2. MVP ships with exactly 1 resume template (Standard Resume, 1-2 pages)
3. PDF generation uses HTML-to-PDF conversion (Thymeleaf renders HTML, Flying Saucer converts to PDF)
4. Async processing uses Spring @Async with virtual thread executor (no external job queue like JobRunr in Phase 1)
5. Local filesystem storage is sufficient for development; S3 support is designed but implementation deferred
6. Export parameters (target role, location, seniority) are used for header/footer customization, not AI-generated content (no LLM integration in Phase 1)
7. Resume content is deterministic (same profile data + template = same PDF, no AI creativity)
8. PDF fonts support Latin characters; full Unicode support (CJK, Arabic) requires additional font configuration
9. File retention: generated PDFs kept indefinitely in Phase 1 (no automatic cleanup)
10. Concurrent export limit: no per-tenant throttling in Phase 1 (deferred to rate limiting feature)

---

## Out of Scope

The following are explicitly excluded from this feature:

1. **No AI-Generated Content:** Resume content comes directly from profile data, no LLM-based rewriting or optimization
2. **No DOCX Export:** Only PDF format in Phase 1
3. **No Background Presentation Export:** Deck generation deferred to Feature in Phase 2
4. **No Cover Letter Generation:** Cover letter export deferred to Phase 2
5. **No Multiple Resume Templates:** Only 1 standard template in MVP; additional templates (Tech Lead, Fintech-oriented) in Phase 2
6. **No Real-Time Preview:** No live preview of resume before generating PDF
7. **No Custom Template Upload:** Users cannot upload their own resume templates
8. **No S3 Implementation:** S3 storage service interface defined but implementation deferred to production deployment
9. **No File Cleanup:** No automatic deletion of old export files
10. **No Export Sharing:** No shareable links for generated exports
11. **No Rate Limiting:** No per-tenant export rate limits
12. **No Email Notification:** No email when export completes

---

## Security & Privacy Considerations

### Security Requirements

- All export API endpoints MUST require valid JWT authentication
- Tenant filtering MUST be automatically applied to ExportHistory queries
- File download endpoint MUST verify tenant ownership before serving file
- Generated PDFs stored with tenant-prefixed paths to prevent cross-tenant file access
- File storage paths MUST NOT be guessable (include export ID in path)
- @Transactional annotations for data integrity on status transitions

### Privacy Requirements

- Export parameters (target role, location) are tenant-private data
- Generated PDFs may contain sensitive career information -- stored securely
- ExportHistory records tenant-isolated (users only see their own exports)
- No PII in log messages (log export ID and status, not profile content)
- File URLs not exposed directly to frontend (served via authenticated download endpoint)

---

## Performance Expectations

- **Export Request Submission:** p95 latency < 300ms (synchronous: validate, deduct coins, create record, submit async job)
- **PDF Generation Time:** p95 < 30 seconds for profiles with up to 10 job experiences and 30 skills
- **File Download:** p95 latency < 500ms (read from local filesystem or S3)
- **Job Status Polling:** p95 latency < 50ms (single row lookup by ID)
- **Export History List:** p95 latency < 200ms (paginated query with indexes)
- **Concurrent Exports:** Support 10 concurrent export jobs without degradation
- **Generated PDF Size:** Typically 100-500 KB for 1-2 page resume

---

## Error Handling

### Error Scenarios

**ERROR-001:** Insufficient Coins
- **User Experience:** API returns 402 Payment Required: `{"code": "INSUFFICIENT_BALANCE", "message": "Insufficient coins for resume export. Required: 10, Available: 5", "required": 10, "available": 5}`
- **Recovery Path:** User navigates to Billing page, contacts admin for coins

**ERROR-002:** Template Not Found
- **User Experience:** API returns 404 Not Found: `{"code": "TEMPLATE_NOT_FOUND", "message": "Export template with ID 99 not found"}`
- **Recovery Path:** User selects available template from dropdown

**ERROR-003:** Profile Data Insufficient
- **User Experience:** API returns 400 Bad Request: `{"code": "INSUFFICIENT_PROFILE_DATA", "message": "Profile must have at least a name and headline to generate a resume"}`
- **Recovery Path:** User completes profile before exporting

**ERROR-004:** Export Generation Failed (After Retries)
- **User Experience:** Job status shows FAILED. Export history entry shows error. Coins auto-refunded. UI displays: "Resume export failed. Your 10 coins have been refunded."
- **Recovery Path:** User retries export or contacts support

**ERROR-005:** File Not Found on Download
- **User Experience:** API returns 404 Not Found: `{"code": "FILE_NOT_FOUND", "message": "Export file is no longer available"}`
- **Recovery Path:** User generates a new export

**ERROR-006:** Concurrent Export Limit (Future)
- **User Experience:** API returns 429 Too Many Requests (deferred to future rate limiting)
- **Recovery Path:** User waits and retries

---

## Testing Scope

### Functional Testing

- **Export Request:** Test POST /api/exports/resume creates ExportHistory with status=PENDING
- **Coin Deduction:** Test coins deducted before async processing starts
- **PDF Generation:** Test Thymeleaf template renders correctly with profile data
- **HTML-to-PDF:** Test Flying Saucer converts rendered HTML to valid PDF
- **Status Transitions:** Test PENDING -> IN_PROGRESS -> COMPLETED flow
- **Failure Handling:** Test PENDING -> IN_PROGRESS -> FAILED with auto-refund
- **Retry Logic:** Test exponential backoff retries (1s, 2s, 4s)
- **File Download:** Test authenticated download returns correct PDF
- **Tenant Isolation:** Test cross-tenant export access returns 404
- **Export History:** Test paginated history with filters

### User Acceptance Testing

- **Full Export Flow:** User creates export, waits for completion, downloads PDF
- **Cost Confirmation:** User sees cost before confirming, insufficient balance blocks action
- **Failed Export:** User triggers failing export, verifies coins refunded
- **Export History:** User views history, downloads previous exports

### Edge Case Testing

- **Empty Profile Sections:** Profile with no education or skills
- **Large Profile:** 20+ job experiences, 50+ skills
- **Unicode Content:** Non-Latin characters in profile data
- **Concurrent Exports:** Multiple exports by same user
- **Already Refunded:** Duplicate refund attempt prevention

---

## Notes

This feature establishes the export pipeline that will be reused for future export types (cover letters, background presentations). Key design decisions:

- **Thymeleaf + Flying Saucer:** Proven stack for HTML-to-PDF. Thymeleaf provides familiar templating with full CSS support, Flying Saucer renders HTML/CSS to PDF with high fidelity
- **Async with Spring @Async:** Simpler than JobRunr for Phase 1; sufficient for single-instance deployment. Can migrate to JobRunr if horizontal scaling requires distributed job processing
- **Coins Before Processing:** Deduct coins synchronously before submitting async job to prevent free usage if job queue is slow
- **JSONB Parameters:** Different export types (resume, cover letter, deck) have different parameters; JSONB provides schema flexibility
- **File Storage Abstraction:** Interface-based design enables seamless switch from local to S3 without code changes
- **No LLM in Phase 1:** Resume content is deterministic from profile data; AI-powered resume optimization deferred to future feature

Future enhancements:
- AI-powered resume content optimization (LLM rewrites job descriptions for target role)
- DOCX export format
- Background presentation (deck) export
- Cover letter generation with LLM
- Multiple resume templates (Tech Lead, Fintech-oriented)
- Real-time PDF preview
- Export sharing via tokenized links
- Email notification on completion
- File cleanup/retention policy

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial specification       | Claude Code |
