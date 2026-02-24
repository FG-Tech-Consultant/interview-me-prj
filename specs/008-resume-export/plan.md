# Implementation Plan: Resume Export (PDF)

**Feature ID:** 008-resume-export
**Status:** Design Complete
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Executive Summary

This plan defines the technical design for a server-side PDF resume generation system. The implementation follows the established three-layer architecture (Controller -> Service -> Repository) using Spring Boot 4.x. Export jobs run asynchronously via Spring @Async with Java 25 virtual threads. PDF generation uses a Thymeleaf HTML template rendered to PDF via Flying Saucer (xhtmlrenderer). All exports integrate with the Coin Wallet billing system (Feature 007) for cost enforcement and auto-refund on failure.

The design introduces two new entities (ExportTemplate, ExportHistory), a file storage abstraction (FileStorageService), and an async export pipeline (ExportJobService). The frontend adds an ExportsPage with export form, job progress polling, and export history table.

**Key Deliverables:**
- Database: 2 Liquibase migrations (export_template, export_history tables) + 1 seed data migration
- Backend: 2 JPA entities, 2 repositories, 4 services (ExportService, ExportJobService, PdfGenerationService, FileStorageService), 1 REST controller, 6+ DTOs
- Template: 1 Thymeleaf HTML resume template with CSS styling
- Frontend: ExportsPage, ExportFormDialog, ExportProgressCard, API client, React Query hooks
- Integration: CoinWalletService for billing, Spring @Async for async processing

**Complexity:** Medium-High
**Estimated Time:** 5-7 days

---

## Constitution Check

### Applicable Principles Validation

**Principle 1: Simplicity First**
- **Alignment:** Standard REST endpoint triggers async job, Thymeleaf + Flying Saucer is a proven PDF pipeline, no complex distributed job queue
- **Evidence:** Single ExportController, ExportService orchestrates flow, PdfGenerationService handles rendering
- **Gate:** PASSED

**Principle 3: Modern Java Standards**
- **Alignment:** Java 25 virtual threads for async processing, records for DTOs, Spring Boot 4.x
- **Evidence:** @Async with virtual thread executor, ExportRequest/ExportResponse as records
- **Gate:** PASSED

**Principle 4: Data Sovereignty and Multi-Tenant Isolation**
- **Alignment:** ExportHistory includes tenant_id with Hibernate filter, ExportTemplate shared (no tenant_id), file paths tenant-prefixed
- **Evidence:** ExportHistory entity has @Filter annotation, file storage uses `{tenantId}/{type}/{id}.pdf` path pattern
- **Gate:** PASSED

**Principle 6: Observability and Debugging**
- **Alignment:** Structured logging for export lifecycle, metrics for success/failure rates and generation time
- **Evidence:** @Slf4j on services, log export ID + status + duration, error details on failure
- **Gate:** PASSED

**Principle 7: Security, Privacy, and Credential Management**
- **Alignment:** JWT authentication, @Transactional annotations, authenticated file download
- **Evidence:** All endpoints require authentication, download endpoint verifies tenant ownership, @Transactional on all mutations
- **Gate:** PASSED

**Principle 9: Freemium Model and Coin-Based Monetization**
- **Alignment:** All exports cost coins, auto-refund on failure, cost displayed before action
- **Evidence:** ExportService calls CoinWalletService.spend() before async job, refund on failure via CoinWalletService.refund()
- **Gate:** PASSED

**Principle 11: Database Schema Evolution**
- **Alignment:** Timestamp-based Liquibase migrations with rollback support
- **Evidence:** `20260224150000-create-export-template-table.xml`, `20260224150100-create-export-history-table.xml`
- **Gate:** PASSED

**Principle 12: Async Job Processing**
- **Alignment:** Spring @Async with virtual threads, PENDING->IN_PROGRESS->COMPLETED/FAILED status, retry with exponential backoff, job status API
- **Evidence:** ExportJobService annotated with @Async, retry loop with backoff, GET /api/exports/{id}/status
- **Gate:** PASSED

**Principle 14: Event-Driven Architecture**
- **Alignment:** ExportCompletedEvent published on completion
- **Evidence:** ApplicationEventPublisher used in ExportJobService after status update
- **Gate:** PASSED

### Overall Constitution Compliance: PASSED

---

## Technical Architecture

### Component Diagram

```
+-------------------------------------------------------------+
|                  User's Browser (React SPA)                  |
|              TypeScript, React Query, MUI                    |
+---------------------------+---------------------------------+
                            | HTTP/REST API (JSON)
                            |
+---------------------------v---------------------------------+
|               Spring Boot Backend (Java 25)                  |
|  +--------------------------------------------------------+ |
|  |  ExportController (/api/exports/*)                      | |
|  |  - POST /resume (trigger export)                        | |
|  |  - GET /{id}/status (poll status)                       | |
|  |  - GET /{id}/download (download PDF)                    | |
|  |  - GET / (export history)                               | |
|  +----------------------------+---------------------------+  |
|                               |                              |
|  +----------------------------v---------------------------+  |
|  |  ExportService (orchestration)                          | |
|  |  - Validates request, checks coins, creates ExportHistory|
|  |  - Delegates async processing to ExportJobService       | |
|  +----------------------------+---------------------------+  |
|                               |                              |
|  +----------------------------v---------------------------+  |
|  |  ExportJobService (@Async + virtual threads)            | |
|  |  - Runs export pipeline asynchronously                  | |
|  |  - Retry with exponential backoff                       | |
|  |  - Auto-refund on final failure                         | |
|  +----+-----------------------+---------------------------+  |
|       |                       |                              |
|  +----v---------+    +--------v---------+                    |
|  | PdfGenService |    | FileStorageService|                   |
|  | - Thymeleaf   |    | - LocalStorage   |                   |
|  | - FlyingSaucer|    | - (S3 future)    |                   |
|  +--------------+    +------------------+                    |
|                               |                              |
|  +----------------------------v---------------------------+  |
|  |  CoinWalletService (Feature 007)                        | |
|  |  - spend() before job                                   | |
|  |  - refund() on failure                                  | |
|  +--------------------------------------------------------+  |
+--------------------------------------------------------------+
                            |
+---------------------------v---------------------------------+
|              PostgreSQL 18 Database                          |
|  export_template (shared) + export_history (tenant-isolated) |
+-------------------------------------------------------------+
```

### Request Flow: User Exports a Resume

```
1. User fills export form (template, target role, location, seniority)
   |
2. Frontend shows CoinConfirmationDialog: "Cost: 10 coins. Balance: 50"
   |
3. User confirms -> POST /api/exports/resume
   |
4. ExportController.createResumeExport(@Valid ExportResumeRequest)
   |
5. ExportService.createResumeExport(tenantId, profileId, request):
   a. Validate template exists and is active
   b. Validate profile has minimum data (name, headline)
   c. Get export cost from BillingProperties: 10 coins
   d. Call CoinWalletService.spend(tenantId, 10, EXPORT, null, "Resume export")
   e. Create ExportHistory(status=PENDING, coins_spent=10, coin_transaction_id=txId)
   f. Save ExportHistory
   g. Submit async job: ExportJobService.processExport(exportHistoryId)
   h. Return ExportHistoryResponse with id, status=PENDING
   |
6. HTTP 202 Accepted with ExportHistoryResponse
   |
7. Frontend starts polling: GET /api/exports/{id}/status every 3 seconds
   |
8. ExportJobService.processExport(exportHistoryId) [@Async]:
   a. Update status to IN_PROGRESS
   b. Load full profile data (profile, jobs, education, skills)
   c. Prepare Thymeleaf context variables
   d. Call PdfGenerationService.generatePdf(templateFile, contextVars)
      - Render Thymeleaf HTML template
      - Convert HTML to PDF via Flying Saucer
   e. Call FileStorageService.store(tenantId, fileName, pdfBytes)
   f. Update ExportHistory: status=COMPLETED, file_url, file_size, completed_at
   g. Publish ExportCompletedEvent
   |
9. Frontend detects status=COMPLETED, shows download button
   |
10. User clicks download -> GET /api/exports/{id}/download
    |
11. ExportController.downloadExport(exportId):
    a. Verify tenant ownership
    b. Load file via FileStorageService.retrieve(fileUrl)
    c. Return ResponseEntity with PDF bytes, Content-Type: application/pdf
```

---

## Technology Stack

### New Dependencies

```kotlin
dependencies {
    // Thymeleaf (standalone mode for PDF templates)
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")

    // Flying Saucer - HTML to PDF conversion with CSS support
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.7.2")

    // OpenPDF (transitive dependency of Flying Saucer, explicit for clarity)
    implementation("com.github.librepdf:openpdf:2.0.3")

    // Existing dependencies (already in project)
    // spring-boot-starter-web, spring-boot-starter-data-jpa,
    // spring-boot-starter-security, spring-boot-starter-validation,
    // postgresql, liquibase-core, lombok, hypersistence-utils
}
```

### Frontend Technologies

**New React Components:**
- `ExportsPage.tsx`: Main exports page with history table (< 300 lines)
- `ExportFormDialog.tsx`: Export configuration form (< 250 lines)
- `ExportProgressCard.tsx`: Real-time progress display with polling (< 150 lines)

**New API Client:**
- `api/exportsApi.ts`: API client for export endpoints (< 150 lines)

**New Hooks:**
- `hooks/useExports.ts`: React Query hooks for exports (< 150 lines)

**New Types:**
- `types/export.ts`: TypeScript interfaces for export DTOs (< 60 lines)

---

## Service Decomposition

### Backend Package Structure

```
com.interviewme.exports/
  controller/
    ExportController.java           # REST endpoints for exports
  service/
    ExportService.java              # Orchestration: validation, billing, job submission
    ExportJobService.java           # Async job processing with retry
    PdfGenerationService.java       # Thymeleaf + Flying Saucer PDF pipeline
    FileStorageService.java         # Interface for file storage
    LocalFileStorageService.java    # Local filesystem implementation
  repository/
    ExportTemplateRepository.java   # ExportTemplate data access
    ExportHistoryRepository.java    # ExportHistory data access
  model/
    ExportTemplate.java             # JPA entity
    ExportHistory.java              # JPA entity
    ExportStatus.java               # Enum: PENDING, IN_PROGRESS, COMPLETED, FAILED
    ExportType.java                 # Enum: RESUME, COVER_LETTER, BACKGROUND_DECK
  dto/
    ExportResumeRequest.java        # Request body for resume export (record)
    ExportHistoryResponse.java      # Export history item (record)
    ExportStatusResponse.java       # Job status response (record)
    ExportHistoryPageResponse.java  # Paginated response (record)
    ExportTemplateResponse.java     # Template info (record)
  config/
    AsyncConfig.java                # Virtual thread executor configuration
    ExportProperties.java           # @ConfigurationProperties for export settings
  event/
    ExportCompletedEvent.java       # Domain event (record)
```

### Service Responsibilities

**ExportService** (~250 lines):
- Validates export request (template exists, profile has data, coins available)
- Calls CoinWalletService.spend() to deduct coins
- Creates ExportHistory with status=PENDING
- Submits async job to ExportJobService
- Retrieves export history and status
- Handles file download
- Dependencies: ExportTemplateRepository, ExportHistoryRepository, CoinWalletService, ProfileService, ExportJobService

**ExportJobService** (~200 lines):
- Annotated with @Async for virtual thread execution
- Implements retry loop with exponential backoff
- Orchestrates: load data -> render template -> generate PDF -> store file
- Updates ExportHistory status throughout lifecycle
- Calls CoinWalletService.refund() on final failure
- Publishes ExportCompletedEvent
- Dependencies: ExportHistoryRepository, ProfileService, PdfGenerationService, FileStorageService, CoinWalletService, ApplicationEventPublisher

**PdfGenerationService** (~150 lines):
- Configures Thymeleaf template engine (standalone mode)
- Renders HTML from template with profile data context
- Converts rendered HTML to PDF using Flying Saucer ITextRenderer
- Returns PDF as byte array
- Dependencies: None (stateless utility)

**FileStorageService** (interface ~30 lines):
- `String store(Long tenantId, String fileName, byte[] content)` - stores file, returns URL/path
- `byte[] retrieve(String fileUrl)` - retrieves file content
- `void delete(String fileUrl)` - deletes file

**LocalFileStorageService** (~80 lines):
- Implements FileStorageService for local development
- Stores files in `./exports/{tenantId}/{type}/{exportId}.pdf`
- Configurable base directory via ExportProperties

---

## Data Model Implementation

### Database Schema

```sql
-- export_template table - Shared template definitions
CREATE TABLE export_template (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    type            VARCHAR(50)     NOT NULL,
    description     TEXT,
    template_file   VARCHAR(255)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_export_template_type ON export_template(type);

-- Seed data: Standard Resume template
INSERT INTO export_template (name, type, description, template_file, is_active, created_at, updated_at)
VALUES ('Standard Resume', 'RESUME', 'Professional 1-2 page resume format',
        'templates/exports/standard-resume.html', true, NOW(), NOW());

-- export_history table - Tenant-isolated export records
CREATE TABLE export_history (
    id                  BIGSERIAL       PRIMARY KEY,
    tenant_id           BIGINT          NOT NULL REFERENCES tenant(id),
    profile_id          BIGINT          NOT NULL REFERENCES profile(id),
    template_id         BIGINT          NOT NULL REFERENCES export_template(id),
    type                VARCHAR(50)     NOT NULL,
    status              VARCHAR(20)     NOT NULL CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    parameters          JSONB           NOT NULL,
    file_url            VARCHAR(500),
    file_size_bytes     BIGINT,
    coins_spent         INTEGER         NOT NULL,
    coin_transaction_id BIGINT,
    error_message       TEXT,
    retry_count         INTEGER         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE INDEX idx_export_history_tenant_id ON export_history(tenant_id);
CREATE INDEX idx_export_history_profile_id ON export_history(profile_id);
CREATE INDEX idx_export_history_status ON export_history(status);
CREATE INDEX idx_export_history_created_at ON export_history(created_at);
```

### Java Entities

```java
// ExportTemplate.java
@Entity
@Table(name = "export_template")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_file", nullable = false, length = 255)
    private String templateFile;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

// ExportHistory.java
@Entity
@Table(name = "export_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ExportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "template_id", nullable = false)
    private ExportTemplate template;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> parameters;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "coins_spent", nullable = false)
    private Integer coinsSpent;

    @Column(name = "coin_transaction_id")
    private Long coinTransactionId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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
```

### DTOs (Java Records)

```java
// ExportResumeRequest.java
public record ExportResumeRequest(
    @NotNull Long templateId,
    @NotBlank @Size(max = 200) String targetRole,
    @Size(max = 200) String location,
    @NotBlank String seniority,
    String language
) {}

// ExportHistoryResponse.java
public record ExportHistoryResponse(
    Long id,
    ExportTemplateResponse template,
    String type,
    String status,
    Map<String, Object> parameters,
    Integer coinsSpent,
    String errorMessage,
    Integer retryCount,
    Instant createdAt,
    Instant completedAt
) {}

// ExportStatusResponse.java
public record ExportStatusResponse(
    Long id,
    String status,
    String fileUrl,
    String errorMessage,
    Integer retryCount,
    Instant completedAt
) {}

// ExportTemplateResponse.java
public record ExportTemplateResponse(
    Long id,
    String name,
    String type,
    String description
) {}

// ExportHistoryPageResponse.java
public record ExportHistoryPageResponse(
    List<ExportHistoryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
```

---

## API Implementation

### Endpoints

| Method | Path | Description | Auth | Response |
|--------|------|-------------|------|----------|
| POST | `/api/exports/resume` | Trigger resume export | JWT (USER) | 202 ExportHistoryResponse |
| GET | `/api/exports/{id}/status` | Get export job status | JWT (USER) | 200 ExportStatusResponse |
| GET | `/api/exports/{id}/download` | Download completed export PDF | JWT (USER) | 200 PDF binary |
| GET | `/api/exports` | List export history (paginated) | JWT (USER) | 200 ExportHistoryPageResponse |
| GET | `/api/exports/templates` | List available export templates | JWT (USER) | 200 List\<ExportTemplateResponse\> |

### Query Parameters for GET `/api/exports`

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Page size (max 100) |
| type | String | null | Filter by export type (RESUME, COVER_LETTER, BACKGROUND_DECK) |
| status | String | null | Filter by status (PENDING, IN_PROGRESS, COMPLETED, FAILED) |

---

## Async Job Architecture

### Virtual Thread Executor Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "exportTaskExecutor")
    public Executor exportTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

### Retry with Exponential Backoff

```java
@Async("exportTaskExecutor")
public void processExport(Long exportHistoryId) {
    ExportHistory export = exportHistoryRepository.findById(exportHistoryId)
        .orElseThrow(() -> new ExportNotFoundException(exportHistoryId));

    int maxRetries = 3;

    for (int attempt = 0; attempt <= maxRetries; attempt++) {
        try {
            export.setStatus("IN_PROGRESS");
            export.setRetryCount(attempt);
            exportHistoryRepository.save(export);

            // Generate PDF
            byte[] pdfBytes = generatePdf(export);

            // Store file
            String fileUrl = fileStorageService.store(
                export.getTenantId(),
                buildFileName(export),
                pdfBytes
            );

            // Update success
            export.setStatus("COMPLETED");
            export.setFileUrl(fileUrl);
            export.setFileSizeBytes((long) pdfBytes.length);
            export.setCompletedAt(Instant.now());
            exportHistoryRepository.save(export);

            // Publish event
            eventPublisher.publishEvent(new ExportCompletedEvent(
                export.getTenantId(), export.getId(), export.getType(),
                fileUrl, export.getCoinsSpent(), Instant.now()
            ));

            log.info("Export completed: exportId={}, duration={}ms",
                exportHistoryId, Duration.between(export.getCreatedAt(), Instant.now()).toMillis());
            return; // Success, exit retry loop

        } catch (PermanentExportException e) {
            // Permanent failure - no retry
            log.error("Permanent export failure: exportId={}, error={}", exportHistoryId, e.getMessage());
            handleFailure(export, e.getMessage());
            return;

        } catch (Exception e) {
            log.warn("Export attempt {} failed: exportId={}, error={}",
                attempt + 1, exportHistoryId, e.getMessage());

            if (attempt == maxRetries) {
                // Final failure after all retries
                handleFailure(export, "Export failed after " + (maxRetries + 1) + " attempts: " + e.getMessage());
                return;
            }

            // Exponential backoff: 1s, 2s, 4s
            long delayMs = (long) Math.pow(2, attempt) * 1000;
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                handleFailure(export, "Export interrupted");
                return;
            }
        }
    }
}

private void handleFailure(ExportHistory export, String errorMessage) {
    export.setStatus("FAILED");
    export.setErrorMessage(errorMessage);
    exportHistoryRepository.save(export);

    // Auto-refund coins
    try {
        coinWalletService.refund(export.getTenantId(), export.getCoinTransactionId());
        log.info("Auto-refunded {} coins for failed export: exportId={}",
            export.getCoinsSpent(), export.getId());
    } catch (Exception e) {
        log.error("Failed to refund coins for export: exportId={}, error={}",
            export.getId(), e.getMessage());
    }
}
```

---

## PDF Generation Pipeline

### Thymeleaf Configuration (Standalone Mode)

```java
@Service
@Slf4j
public class PdfGenerationService {

    private final TemplateEngine templateEngine;

    public PdfGenerationService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public byte[] generatePdf(String templateFile, Map<String, Object> contextVars) {
        // 1. Render HTML
        Context context = new Context();
        context.setVariables(contextVars);
        String html = templateEngine.process(templateFile, context);

        // 2. Convert HTML to PDF using Flying Saucer
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);
            return os.toByteArray();
        }
    }
}
```

### Resume Template Context Variables

```java
// Context variables passed to Thymeleaf template
Map<String, Object> context = Map.of(
    "profile", profileData,           // fullName, headline, summary, location, languages, links
    "jobExperiences", jobExperiences,  // sorted by startDate DESC
    "education", educationList,        // sorted by startDate DESC
    "skills", skillsByCategory,        // Map<String, List<UserSkillDto>> grouped by category
    "targetRole", request.targetRole(),
    "location", request.location(),
    "seniority", request.seniority(),
    "language", request.language(),
    "generatedDate", LocalDate.now()
);
```

### Standard Resume HTML Template (Thymeleaf)

The template (`templates/exports/standard-resume.html`) renders a clean, professional 1-2 page resume with:

1. **Header:** Full name, headline, location, target role, professional links
2. **Summary:** Brief professional summary
3. **Experience:** Job experiences sorted by date (most recent first) with company, role, dates, responsibilities, achievements
4. **Skills:** Skills grouped by category with proficiency indicators
5. **Education:** Education entries with institution, degree, dates
6. **Footer:** Generated date, "Powered by Interview Me"

CSS is embedded in the HTML for Flying Saucer compatibility. Uses @page CSS for page size (A4) and margins.

---

## File Storage Design

### Interface

```java
public interface FileStorageService {
    String store(Long tenantId, String fileName, byte[] content);
    byte[] retrieve(String fileUrl);
    void delete(String fileUrl);
}
```

### Local Implementation

```java
@Service
@Profile("!production")
@RequiredArgsConstructor
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final ExportProperties exportProperties;

    @Override
    public String store(Long tenantId, String fileName, byte[] content) {
        Path directory = Path.of(exportProperties.getStoragePath(), tenantId.toString());
        Files.createDirectories(directory);
        Path filePath = directory.resolve(fileName);
        Files.write(filePath, content);
        return filePath.toString();
    }

    @Override
    public byte[] retrieve(String fileUrl) {
        return Files.readAllBytes(Path.of(fileUrl));
    }

    @Override
    public void delete(String fileUrl) {
        Files.deleteIfExists(Path.of(fileUrl));
    }
}
```

### Configuration

```yaml
# application.yml
exports:
  storage-path: ./exports
  costs:
    resume: 10
  retry:
    max-attempts: 3
    initial-delay-ms: 1000
    multiplier: 2
```

---

## Integration with Billing (Feature 007)

### Coin Flow

```
1. User triggers export
   |
2. ExportService reads cost: billingProperties.getCosts().get("resume-export") = 10
   |
3. CoinWalletService.spend(tenantId, 10, RefType.EXPORT, null, "Resume export")
   |-- SUCCESS: Returns CoinTransaction (id, type=SPEND, amount=-10)
   |-- FAILURE: Throws InsufficientBalanceException -> 402 to frontend
   |
4. Store coin_transaction_id in ExportHistory for tracking
   |
5. On export FAILURE (after all retries):
   CoinWalletService.refund(tenantId, coinTransactionId)
   |-- Creates CoinTransaction (type=REFUND, amount=+10, refType=EXPORT_REFUND)
```

### BillingProperties Integration

The export cost is read from the existing `billing.costs` configuration defined in Feature 007:

```yaml
billing:
  costs:
    resume-export: 10
```

---

## Frontend Design

### Component Structure

```
frontend/src/
  api/
    exportsApi.ts                # API client for export endpoints
  types/
    export.ts                    # TypeScript interfaces
  hooks/
    useExports.ts                # React Query hooks
  pages/
    ExportsPage.tsx              # Main exports page
  components/
    exports/
      ExportFormDialog.tsx       # Export configuration form
      ExportProgressCard.tsx     # Real-time progress display
      ExportHistoryTable.tsx     # Export history table
      ExportStatusBadge.tsx      # Colored status badge
```

### ExportsPage Layout

```
+------------------------------------------------------+
|  Exports                     [New Resume Export]      |
+------------------------------------------------------+
|  [In-Progress Export Card - if any]                   |
|  +--------------------------------------------------+|
|  | Generating your resume...  [||||    ] 60%         ||
|  | Template: Standard Resume                         ||
|  | Target Role: Senior Backend Engineer              ||
|  +--------------------------------------------------+|
|                                                       |
|  Export History                                        |
|  +--------------------------------------------------+|
|  | Date       | Template  | Target Role | Status    ||
|  |            |           |             | Coins | Act||
|  |------------|-----------|-------------|-------|----||
|  | 2026-02-24 | Standard  | Sr Backend  | Done  |   ||
|  |            | Resume    | Engineer    | 10    | DL ||
|  | 2026-02-23 | Standard  | Tech Lead   | Failed|   ||
|  |            | Resume    |             | 10(R) |    ||
|  +--------------------------------------------------+|
|  [< Previous] [Page 1 of 3] [Next >]                 |
+------------------------------------------------------+
```

### Frontend Polling Logic

```typescript
// useExports.ts
export function useExportStatus(exportId: number | null) {
  return useQuery({
    queryKey: ['export-status', exportId],
    queryFn: () => exportsApi.getExportStatus(exportId!),
    enabled: !!exportId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'COMPLETED' || status === 'FAILED') return false;
      return 3000; // Poll every 3 seconds while pending/in-progress
    },
  });
}
```

---

## Security Implementation

### Authentication/Authorization

- All endpoints require JWT authentication (existing from Feature 001)
- Tenant context extracted from JWT, applied to ExportHistory queries via Hibernate filter
- File download endpoint verifies export belongs to requesting tenant

### File Access Security

- File storage paths include tenant_id: `./exports/{tenantId}/resume-{exportId}.pdf`
- Download endpoint serves files through authenticated controller (no direct file URL access)
- File URLs stored in database, never exposed to frontend (frontend uses `/api/exports/{id}/download`)

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Export request submission | p95 < 300ms | Validate + deduct coins + create record |
| PDF generation (typical) | p95 < 15s | Profile with 5 jobs, 15 skills |
| PDF generation (large) | p95 < 30s | Profile with 20 jobs, 50 skills |
| File download | p95 < 500ms | Local filesystem read |
| Job status poll | p95 < 50ms | Single row lookup by ID |
| Export history list | p95 < 200ms | Paginated query with indexes |
| Generated PDF size | < 500 KB | 1-2 page resume |

---

## Testing Strategy

### Unit Tests

- `ExportServiceTest`: Test validation (template exists, profile has data, coins available), test coin deduction, test async job submission
- `ExportJobServiceTest`: Test retry logic (mock failures), test exponential backoff delays, test auto-refund on final failure
- `PdfGenerationServiceTest`: Test Thymeleaf template rendering with sample data, test HTML-to-PDF conversion produces valid PDF
- `LocalFileStorageServiceTest`: Test file store/retrieve/delete operations

### Integration Tests

- `ExportControllerIntegrationTest`:
  - POST /resume returns 202 with PENDING status
  - GET /{id}/status returns current status
  - GET /{id}/download returns PDF for COMPLETED export
  - GET / returns paginated export history
  - Tenant isolation: cross-tenant access returns 404
  - Insufficient coins returns 402
  - Invalid template returns 404

### Concurrency Tests

- `ExportConcurrencyTest`: 5 concurrent export requests, verify all complete without data corruption

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Flying Saucer CSS rendering differences from browser | Medium | Medium | Use simple CSS, test with multiple profile sizes, avoid complex layouts |
| PDF generation timeout for large profiles | Low | Medium | Set processing timeout (60s), truncate skills/jobs if too many |
| Local file storage disk space | Low | Low | Log warnings at thresholds, implement cleanup in future |
| Virtual thread pinning during I/O | Low | Low | Flying Saucer uses NIO-compatible I/O; monitor thread metrics |
| Thymeleaf template errors crash job | Medium | Medium | Validate template on startup, catch rendering errors gracefully |
| Unicode font issues in PDF | Medium | Medium | Bundle Unicode-compatible fonts (NotoSans), test with non-Latin content |

---

## Migration Path (Future Features)

### To Cover Letter Generation (Phase 2)
1. Add new ExportTemplate for cover letters
2. Create Thymeleaf template for cover letter layout
3. Add POST `/api/exports/cover-letter` endpoint
4. Reuse existing ExportJobService, PdfGenerationService, FileStorageService

### To Background Presentation Export (Phase 2)
1. Add new ExportTemplate for deck format
2. Create Thymeleaf template with slide-like layout
3. Add POST `/api/exports/deck` endpoint
4. Reuse pipeline

### To S3 Storage (Production)
1. Implement S3FileStorageService
2. Activate via Spring profile: `spring.profiles.active=production`
3. No changes to ExportService or ExportJobService (interface-based)

---

## Open Questions / Deferred Decisions

**Resolved During Planning:**
- [x] **PDF library choice?** -> Flying Saucer (HTML-to-PDF with CSS support) + Thymeleaf for templating
- [x] **Async framework?** -> Spring @Async with virtual threads (simpler than JobRunr for Phase 1)
- [x] **File storage?** -> Abstraction with local implementation now, S3 later
- [x] **Coin deduction timing?** -> Before async job starts (fail-fast on insufficient balance)

**Deferred:**
- [ ] **S3 implementation** -> Production deployment
- [ ] **File cleanup/retention policy** -> Future feature
- [ ] **Rate limiting per tenant** -> Future feature
- [ ] **AI-powered content optimization** -> Future feature requiring LLM integration

---

## References

- [Feature Specification](./spec.md)
- [Feature 007: Coin Wallet Spec](../007-coin-wallet/spec.md)
- [Feature 007: Coin Wallet Plan](../007-coin-wallet/plan.md)
- [Project Constitution](../../.specify/memory/constitution.md)
- [Project Overview](../../.specify/memory/project-overview.md)

---

## Sign-Off

**Planning Complete:** Yes
**Constitution Validated:** All principles satisfied
**Ready for Implementation:** Yes

---

**Plan Version:** 1.0.0
**Last Updated:** 2026-02-24
**Estimated Implementation Time:** 5-7 days
