# Implementation Plan: LinkedIn Profile Score Analyzer

**Feature ID:** 009-linkedin-analyzer
**Status:** Design Complete
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Executive Summary

This plan defines the technical design for the LinkedIn Profile Score Analyzer, an AI-powered feature that accepts LinkedIn PDF exports, analyzes them asynchronously via LLM, and produces comprehensive profile scoring with actionable improvement suggestions.

The implementation introduces two new database tables (`linkedin_analysis`, `linkedin_section_score`), a PDF parsing pipeline using Apache PDFBox, LLM integration via the existing `LlmClient` interface, and async processing using Spring `@Async` with virtual threads. The feature integrates with the Coin Wallet (Feature 007) for paid suggestions and with Profile CRUD (Feature 002) for applying suggestions.

The backend lives in the `com.interviewme.linkedin` package with three main services: `LinkedInAnalysisService` (orchestration), `LinkedInPdfParserService` (PDF text extraction), and `LinkedInPromptService` (LLM prompt construction). The frontend provides a dedicated LinkedInAnalyzerPage with file upload, score visualization, suggestion display, and analysis history.

**Key Deliverables:**
- Database: 2 Liquibase migrations (linkedin_analysis, linkedin_section_score tables)
- Backend: 2 JPA entities, 2 repositories, 3 services, 1 REST controller, 8+ DTOs
- LLM integration: Prompt templates for scoring and additional suggestions
- Frontend: LinkedInAnalyzerPage with file upload, score display, section cards, history
- Async pipeline: PDF parsing -> LLM analysis -> result storage
- Coin integration: Paid suggestions debit/refund via CoinWalletService

**Timeline:** 6-8 days
**Complexity:** High (async processing, LLM integration, file upload, coin integration)

---

## Constitution Check

### Applicable Principles Validation

**Principle 1: Simplicity First**
- **Alignment:** Standard REST endpoints, @Async for background processing, no complex job queue framework needed for MVP
- **Evidence:** Single controller, three focused services, polling-based status updates (simpler than SSE/WebSocket)
- **Gate:** PASSED

**Principle 3: Modern Java Standards**
- **Alignment:** Java 25 records for DTOs, virtual threads for async processing, sealed interfaces where applicable
- **Evidence:** All DTOs as records, @Async with virtual thread executor, pattern matching in PDF parser
- **Gate:** PASSED

**Principle 4: Data Sovereignty and Multi-Tenant Isolation**
- **Alignment:** Both tables include tenant_id with automatic Hibernate filtering
- **Evidence:** linkedin_analysis.tenant_id FK, linkedin_section_score.tenant_id FK, tenant filter applied
- **Gate:** PASSED

**Principle 5: AI Integration and LLM Management**
- **Alignment:** Uses LlmClient interface, backend-controlled prompts, cost tracking
- **Evidence:** LinkedInPromptService constructs prompts, LlmRouterService selects provider, LLM metrics tracked
- **Gate:** PASSED

**Principle 6: Observability and Debugging**
- **Alignment:** Structured logging for entire analysis lifecycle, LLM call metrics
- **Evidence:** @Slf4j on all services, log analysis start/complete/fail with duration, track LLM token usage
- **Gate:** PASSED

**Principle 7: Security, Privacy, and Credential Management**
- **Alignment:** JWT auth, @Transactional, temporary PDF storage, no credential exposure in prompts
- **Evidence:** All endpoints require auth, PDF deleted after parsing, LLM prompt excludes tenant details
- **Gate:** PASSED

**Principle 8: Multi-Tenant Architecture**
- **Alignment:** All entities tenant-isolated, analyses scoped per tenant
- **Gate:** PASSED

**Principle 9: Freemium Model**
- **Alignment:** Full analysis free, 1 free suggestion per section, additional suggestions cost coins
- **Evidence:** Initial analysis no coin cost, "get more suggestions" calls CoinWalletService.spend()
- **Gate:** PASSED

**Principle 10: Full-Stack Modularity**
- **Alignment:** com.interviewme.linkedin package, services < 400 lines each, components < 300 lines
- **Gate:** PASSED

**Principle 11: Database Schema Evolution**
- **Alignment:** Timestamp-based Liquibase migrations with rollback
- **Gate:** PASSED

**Principle 12: Async Job Processing**
- **Alignment:** Analysis runs asynchronously via @Async, status polling
- **Evidence:** ANALYZE_LINKEDIN_PROFILE job type, PENDING -> IN_PROGRESS -> COMPLETED/FAILED lifecycle
- **Gate:** PASSED

**Principle 13: LinkedIn Compliance**
- **Alignment:** PDF upload only, no scraping, user-initiated analysis
- **Gate:** PASSED

### Overall Constitution Compliance: PASSED

---

## Technical Architecture

### Component Diagram

```
+-----------------------------------------------------------+
|                User's Browser (React SPA)                   |
|  LinkedInAnalyzerPage: upload, scores, suggestions, history |
+---------------------------+-------------------------------+
                            | HTTP/REST (JSON + multipart)
                            |
+---------------------------v-------------------------------+
|              Spring Boot Backend (Java 25)                  |
|  +------------------------------------------------------+ |
|  |  LinkedInController (/api/v1/linkedin/*)              | |
|  |  - POST /analyze (multipart PDF upload)               | |
|  |  - GET /analyses/{id} (status + results)              | |
|  |  - GET /analyses (history list)                       | |
|  |  - POST /analyses/{id}/sections/{name}/suggestions    | |
|  |  - POST /analyses/{id}/sections/{name}/apply          | |
|  +------------------------+-----------------------------+ |
|                           |                               |
|  +------------------------v-----------------------------+ |
|  |  Services                                             | |
|  |  - LinkedInAnalysisService (orchestration, async)     | |
|  |  - LinkedInPdfParserService (PDF text extraction)     | |
|  |  - LinkedInPromptService (LLM prompt construction)    | |
|  +--+-----------+-----------+---------------------------+ |
|     |           |           |                             |
|     v           v           v                             |
|  LlmClient   PDFBox   CoinWalletService                  |
|  (AI layer)  (parser)  (billing, Feature 007)             |
|                                                           |
|  +------------------------------------------------------+ |
|  |  Repositories                                         | |
|  |  - LinkedInAnalysisRepository                         | |
|  |  - LinkedInSectionScoreRepository                     | |
|  +------------------------+-----------------------------+ |
+---------------------------+-------------------------------+
                            |
+---------------------------v-------------------------------+
|                PostgreSQL 18 Database                       |
|  linkedin_analysis + linkedin_section_score                 |
+-----------------------------------------------------------+
```

### Async Processing Flow

```
1. POST /api/v1/linkedin/analyze (multipart PDF)
   |
2. LinkedInController: validate PDF, save temp file, create LinkedInAnalysis(PENDING)
   |
3. Return 202 Accepted with {analysisId}  <-- Immediate response
   |
4. @Async LinkedInAnalysisService.processAnalysis(analysisId):
   |
   4a. Update status -> IN_PROGRESS
   |
   4b. LinkedInPdfParserService.parse(pdfPath):
       - Read PDF via PDFBox
       - Extract text content
       - Identify sections (Headline, About, Experience, Education, Skills, Recommendations)
       - Return Map<SectionName, String> of extracted text
   |
   4c. Delete temp PDF file
   |
   4d. LinkedInPromptService.buildScoringPrompt(sections):
       - Construct structured prompt with scoring rubric
       - Include extracted section text
       - Request JSON output: {overallScore, sections: [{name, score, explanation, suggestion}]}
   |
   4e. LlmRouterService.complete(prompt, ANALYZE_LINKEDIN_PROFILE):
       - Select LLM provider based on action type and tenant config
       - Call LLM API (15-40 seconds)
       - Return structured response
   |
   4f. Parse LLM response into LinkedInAnalysis + LinkedInSectionScore records
   |
   4g. Save results to database, update status -> COMPLETED
   |
   4h. On failure (after 3 retries): update status -> FAILED with error_message

5. Frontend polls GET /api/v1/linkedin/analyses/{id} every 3s
   |
6. When COMPLETED: display scores, explanations, and suggestions
```

---

## Technology Stack

### Core Framework
- **Spring Boot:** 4.x (existing)
- **Java:** 25 LTS (existing)
- **Build Tool:** Gradle 8.5+ (existing)

### New Dependencies

```kotlin
dependencies {
    // PDF parsing
    implementation("org.apache.pdfbox:pdfbox:3.0.1")

    // All other dependencies already in project:
    // Spring Data JPA, Spring Web, Spring Security, Spring Validation
    // Hypersistence Utils for JSONB, Lombok, Liquibase
}
```

### Frontend Technologies

**New React Components:**
- `LinkedInAnalyzerPage.tsx`: Main page with upload, results, history (< 300 lines)
- `PdfUploader.tsx`: Drag-and-drop PDF upload component (< 150 lines)
- `ScoreGauge.tsx`: Circular score display component (< 100 lines)
- `SectionScoreCard.tsx`: Per-section score with suggestions (< 200 lines)
- `AnalysisHistory.tsx`: Past analyses list with score trends (< 150 lines)
- `SuggestionItem.tsx`: Individual suggestion with "Apply" button (< 100 lines)

**New API Client:**
- `api/linkedinApi.ts`: API client for LinkedIn analyzer (< 200 lines)

**New Hooks:**
- `hooks/useLinkedInAnalysis.ts`: React Query hooks for analysis lifecycle (< 200 lines)

---

## Service Decomposition

### Backend Services (Java)

**LinkedInAnalysisService** (Orchestration):
- Line count estimate: 350-400 lines
- Dependencies: LinkedInAnalysisRepository, LinkedInSectionScoreRepository, LinkedInPdfParserService, LinkedInPromptService, LlmRouterService, CoinWalletService, ProfileService
- Public methods:
  - `LinkedInAnalysisResponse startAnalysis(Long profileId, MultipartFile pdf)` - Validates, creates record, triggers async
  - `@Async void processAnalysis(Long analysisId)` - Full async pipeline (parse -> LLM -> store)
  - `LinkedInAnalysisResponse getAnalysis(Long analysisId)` - Get analysis with section scores
  - `List<LinkedInAnalysisSummary> getAnalysisHistory(Long profileId, Pageable pageable)` - Past analyses
  - `LinkedInSectionScoreResponse generateAdditionalSuggestions(Long analysisId, String sectionName)` - Paid suggestions via LLM + coins
  - `void applySuggestion(Long analysisId, String sectionName, int suggestionIndex)` - Apply to profile

**LinkedInPdfParserService** (PDF Processing):
- Line count estimate: 200-250 lines
- Dependencies: Apache PDFBox
- Public methods:
  - `Map<String, String> parse(Path pdfPath)` - Extract text and identify sections
  - Returns map of section name -> extracted text content
- Implementation notes:
  - Use PDFBox PDDocument and PDFTextStripper
  - Identify sections by known LinkedIn PDF markers (e.g., "About", "Experience", section headers)
  - Handle different LinkedIn PDF export formats
  - Return empty string for sections not found

**LinkedInPromptService** (Prompt Construction):
- Line count estimate: 150-200 lines
- Dependencies: None (pure prompt logic)
- Public methods:
  - `LlmRequest buildScoringPrompt(Map<String, String> sections)` - Main analysis prompt
  - `LlmRequest buildAdditionalSuggestionsPrompt(String sectionName, String sectionContent, List<String> existingSuggestions)` - Prompt for more suggestions
  - `LinkedInLlmResult parseScoringResponse(LlmResponse response)` - Parse LLM JSON output
  - `List<String> parseAdditionalSuggestions(LlmResponse response)` - Parse additional suggestions
- Prompt includes:
  - Scoring rubric (what makes each score range 0-39, 40-59, 60-79, 80-100)
  - Section-specific evaluation criteria
  - Request for JSON-formatted output
  - 1 concrete, actionable suggestion per section

---

## Data Model Implementation

### Database Schema

```sql
-- linkedin_analysis table
CREATE TABLE linkedin_analysis (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    profile_id BIGINT NOT NULL REFERENCES profile(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    overall_score INT,
    error_message TEXT,
    pdf_filename VARCHAR(255),
    analyzed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT chk_overall_score CHECK (overall_score IS NULL OR (overall_score >= 0 AND overall_score <= 100)),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_linkedin_analysis_tenant_id ON linkedin_analysis(tenant_id);
CREATE INDEX idx_linkedin_analysis_profile_id ON linkedin_analysis(profile_id);
CREATE INDEX idx_linkedin_analysis_status ON linkedin_analysis(status);

-- linkedin_section_score table
CREATE TABLE linkedin_section_score (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    analysis_id BIGINT NOT NULL REFERENCES linkedin_analysis(id),
    section_name VARCHAR(100) NOT NULL,
    section_score INT NOT NULL,
    quality_explanation TEXT NOT NULL,
    suggestions JSONB,
    raw_content TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    CONSTRAINT chk_section_score CHECK (section_score >= 0 AND section_score <= 100),
    CONSTRAINT chk_section_name CHECK (section_name IN ('HEADLINE', 'ABOUT', 'EXPERIENCE', 'EDUCATION', 'SKILLS', 'RECOMMENDATIONS', 'OTHER'))
);

CREATE INDEX idx_linkedin_section_score_tenant_id ON linkedin_section_score(tenant_id);
CREATE INDEX idx_linkedin_section_score_analysis_id ON linkedin_section_score(analysis_id);
```

### Java Entities

```java
// LinkedInAnalysis.java
@Entity
@Table(name = "linkedin_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class LinkedInAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "pdf_filename", length = 255)
    private String pdfFilename;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "created_at", nullable = false)
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

// LinkedInSectionScore.java
@Entity
@Table(name = "linkedin_section_score")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkedInSectionScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "section_name", nullable = false, length = 100)
    private String sectionName;

    @Column(name = "section_score", nullable = false)
    private Integer sectionScore;

    @Column(name = "quality_explanation", nullable = false, columnDefinition = "TEXT")
    private String qualityExplanation;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> suggestions;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

### DTOs

```java
// StartAnalysisResponse.java
public record StartAnalysisResponse(
    Long analysisId,
    String status,
    String message
) {}

// LinkedInAnalysisResponse.java
public record LinkedInAnalysisResponse(
    Long id,
    Long profileId,
    String status,
    Integer overallScore,
    String errorMessage,
    String pdfFilename,
    Instant analyzedAt,
    List<SectionScoreResponse> sections,
    Instant createdAt
) {}

// LinkedInAnalysisSummary.java (for history list)
public record LinkedInAnalysisSummary(
    Long id,
    String status,
    Integer overallScore,
    String pdfFilename,
    Instant analyzedAt,
    Instant createdAt
) {}

// SectionScoreResponse.java
public record SectionScoreResponse(
    Long id,
    String sectionName,
    Integer sectionScore,
    String qualityExplanation,
    List<String> suggestions,
    boolean canApplyToProfile
) {}

// GenerateSuggestionsRequest.java
public record GenerateSuggestionsRequest(
    @Min(1) @Max(5) Integer count  // default 3
) {}

// ApplySuggestionRequest.java
public record ApplySuggestionRequest(
    @NotNull @Min(0) Integer suggestionIndex
) {}

// LinkedInLlmResult.java (internal, parsed LLM output)
public record LinkedInLlmResult(
    int overallScore,
    List<SectionResult> sections
) {
    public record SectionResult(
        String sectionName,
        int score,
        String explanation,
        String suggestion
    ) {}
}
```

---

## API Implementation

### New Endpoints

| Method | Path | Description | Auth | Request | Response |
|--------|------|-------------|------|---------|----------|
| POST | `/api/v1/linkedin/analyze` | Upload PDF and start analysis | Yes | Multipart (file) | 202 StartAnalysisResponse |
| GET | `/api/v1/linkedin/analyses/{id}` | Get analysis status and results | Yes | - | LinkedInAnalysisResponse |
| GET | `/api/v1/linkedin/analyses` | List analysis history | Yes | Pageable | Page\<LinkedInAnalysisSummary> |
| POST | `/api/v1/linkedin/analyses/{id}/sections/{name}/suggestions` | Generate additional suggestions (paid) | Yes | GenerateSuggestionsRequest | SectionScoreResponse |
| POST | `/api/v1/linkedin/analyses/{id}/sections/{name}/apply` | Apply suggestion to profile | Yes | ApplySuggestionRequest | 200 OK |

---

## Implementation Phases

### Phase 1: Database Foundation (Day 1)

**Tasks:**
1. Create Liquibase migration for `linkedin_analysis` table
2. Create Liquibase migration for `linkedin_section_score` table
3. Add migrations to db.changelog-master.yaml
4. Create JPA entities: LinkedInAnalysis, LinkedInSectionScore
5. Test migrations on fresh database

**Validation:**
- [ ] Tables created with correct constraints and indexes
- [ ] CHECK constraints enforce score range 0-100 and valid status values
- [ ] JSONB suggestions column works correctly

### Phase 2: PDF Parsing Service (Day 2)

**Tasks:**
1. Add Apache PDFBox dependency to build.gradle.kts
2. Implement LinkedInPdfParserService with section extraction logic
3. Write unit tests with sample LinkedIn PDFs
4. Handle edge cases: empty PDF, non-LinkedIn PDF, missing sections

**Validation:**
- [ ] Extracts 6+ sections from standard LinkedIn PDF export
- [ ] Returns empty string for missing sections
- [ ] Handles malformed PDFs gracefully (no exceptions)

### Phase 3: LLM Integration (Day 3)

**Tasks:**
1. Create LlmClient interface stub (if not yet implemented)
2. Implement LinkedInPromptService with scoring prompt template
3. Implement additional suggestions prompt template
4. Implement LLM response parsing (JSON -> LinkedInLlmResult)
5. Write unit tests for prompt construction and response parsing

**Validation:**
- [ ] Scoring prompt includes all sections and rubric
- [ ] Response parser handles valid JSON output
- [ ] Parser handles malformed LLM output gracefully (fallback scores)

### Phase 4: Analysis Service (Day 4-5)

**Tasks:**
1. Create repositories: LinkedInAnalysisRepository, LinkedInSectionScoreRepository
2. Create DTOs (Java records)
3. Implement LinkedInAnalysisService:
   - startAnalysis (sync: validate, create record, trigger async)
   - processAnalysis (async: parse -> LLM -> store)
   - getAnalysis (read with section scores)
   - getAnalysisHistory (paginated list)
   - generateAdditionalSuggestions (LLM + coins)
   - applySuggestion (update profile)
4. Configure @Async with virtual thread executor
5. Add retry logic for LLM calls (3 retries, exponential backoff)
6. Integrate with CoinWalletService for paid suggestions
7. Add @Transactional annotations
8. Add structured logging

**Validation:**
- [ ] Analysis lifecycle works: PENDING -> IN_PROGRESS -> COMPLETED
- [ ] Failed analysis sets FAILED status with error message
- [ ] Paid suggestions deduct coins, refund on failure
- [ ] Apply suggestion updates correct profile field

### Phase 5: REST Controller (Day 5)

**Tasks:**
1. Implement LinkedInController with 5 endpoints
2. Add multipart file upload handling with validation
3. Add exception handlers for analysis-specific errors
4. Write integration tests

**Validation:**
- [ ] PDF upload accepts multipart, validates size and type
- [ ] Status polling returns current state
- [ ] Paid suggestions return updated section with new suggestions
- [ ] Tenant isolation verified

### Phase 6: Frontend (Day 6-8)

**Tasks:**
1. Create linkedinApi.ts API client
2. Create useLinkedInAnalysis React Query hooks (with polling)
3. Implement PdfUploader component (drag-and-drop)
4. Implement ScoreGauge component (circular progress)
5. Implement SectionScoreCard component
6. Implement SuggestionItem component with "Apply" button
7. Implement AnalysisHistory component
8. Implement LinkedInAnalyzerPage (composing all components)
9. Add route: /linkedin-analyzer
10. Add navigation menu item

**Validation:**
- [ ] PDF upload with drag-and-drop works
- [ ] Polling shows spinner during processing, results on completion
- [ ] Scores displayed with correct color coding
- [ ] "Get more suggestions" shows coin cost and triggers payment flow
- [ ] "Apply to profile" updates profile fields
- [ ] Analysis history shows past analyses with score trends

---

## Security Implementation

### Authentication/Authorization
- All endpoints require JWT authentication
- Tenant context extracted from JWT automatically
- No admin-specific endpoints (all user-facing)

### File Upload Security
- Validate file extension (.pdf) and MIME type (application/pdf)
- Maximum file size: 10MB (Spring property: `spring.servlet.multipart.max-file-size=10MB`)
- Generate secure temp filename (UUID-based, no user-controlled paths)
- Delete PDF file immediately after text extraction
- Store in temp directory, not permanent storage

### Data Protection
- Raw LinkedIn content (raw_content) is sensitive -- tenant-isolated, not exposed in public APIs
- LLM prompts do not include tenant_id, user_id, or authentication credentials
- Analysis results immutable after completion

---

## Performance Targets

| Metric | Target | Method |
|--------|--------|--------|
| PDF upload + validation | p95 < 2s | Multipart handling |
| Status polling | p95 < 100ms | Simple DB read by ID |
| Total analysis time | p95 < 60s | Async: PDF parse 5s + LLM 45s |
| Additional suggestions | p95 < 30s | Single-section LLM call |
| Analysis history list | p95 < 200ms | Indexed query with pagination |
| Apply suggestion | p95 < 300ms | Profile update |

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
- LinkedInPdfParserService: section extraction from sample PDFs
- LinkedInPromptService: prompt construction, response parsing
- LinkedInAnalysisService: analysis lifecycle, coin integration, suggestion application

**Integration Tests:**
- LinkedInController: file upload, status polling, paid suggestions, apply suggestion
- Async processing: full pipeline with mocked LLM
- Tenant isolation: cross-tenant access
- Coin integration: spend, insufficient balance, refund on failure

**Target Coverage:** 80%

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| LinkedIn PDF format changes | Medium | High | Flexible parser with fallback; monitor and update |
| LLM produces inconsistent scores | Medium | Medium | Structured prompt with rubric; validate score ranges |
| LLM response not valid JSON | Medium | Medium | Robust parser with fallback parsing; retry logic |
| Large PDF parsing slow | Low | Low | PDFBox handles large files well; timeout at 30s |
| LLM timeout (> 60s) | Medium | Medium | Retry with exponential backoff, FAILED status |
| Coin integration complexity | Low | Medium | Well-defined CoinWalletService API from Feature 007 |
| Temp file cleanup | Low | Medium | Try-finally cleanup; scheduled cleanup job as backup |

---

## Migration Path (Future Features)

### URL-Based Analysis (Future)
1. Add `LinkedInUrlFetcherService` alongside PDF parser
2. Same analysis pipeline after content extraction
3. Requires LinkedIn ToS evaluation before implementation

### Photo/Banner Analysis (Future)
1. Add vision model integration to LlmClient
2. Extract images from PDF via PDFBox
3. Add PHOTO and BANNER section types

---

## References

- [Feature Specification](./spec.md)
- [Project Constitution](../../.specify/memory/constitution.md)
- [Project Overview Section 4.8](../../.specify/memory/project-overview.md)
- [Feature 002: Profile CRUD](../002-profile-crud/)
- [Feature 007: Coin Wallet](../007-coin-wallet/)

---

## Sign-Off

**Planning Complete:** Yes
**Constitution Validated:** All principles satisfied
**Ready for Implementation:** Yes

---

**Plan Version:** 1.0.0
**Last Updated:** 2026-02-24
**Estimated Implementation Time:** 6-8 days
