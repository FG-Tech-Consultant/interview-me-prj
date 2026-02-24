# Tasks: LinkedIn Profile Score Analyzer

**Feature ID:** 009-linkedin-analyzer
**Status:** Not Started
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Branch:** 009-linkedin-analyzer
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

- [ ] T001 [DATABASE] Create Liquibase migration for `linkedin_analysis` table
  - File: `backend/src/main/resources/db/changelog/20260224180000-create-linkedin-analysis-table.xml`
  - Table: linkedin_analysis (id BIGSERIAL PK, tenant_id BIGINT FK NOT NULL, profile_id BIGINT FK NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING', overall_score INT, error_message TEXT, pdf_filename VARCHAR(255), analyzed_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL)
  - CHECK constraints: overall_score 0-100 (when not null), status IN ('PENDING','IN_PROGRESS','COMPLETED','FAILED')
  - Foreign keys: tenant_id -> tenant(id), profile_id -> profile(id)
  - Indexes: idx_linkedin_analysis_tenant_id, idx_linkedin_analysis_profile_id, idx_linkedin_analysis_status
  - Rollback: `<dropTable tableName="linkedin_analysis"/>`

- [ ] T002 [DATABASE] Create Liquibase migration for `linkedin_section_score` table
  - File: `backend/src/main/resources/db/changelog/20260224180100-create-linkedin-section-score-table.xml`
  - Table: linkedin_section_score (id BIGSERIAL PK, tenant_id BIGINT FK NOT NULL, analysis_id BIGINT FK NOT NULL, section_name VARCHAR(100) NOT NULL, section_score INT NOT NULL, quality_explanation TEXT NOT NULL, suggestions JSONB, raw_content TEXT, created_at TIMESTAMPTZ NOT NULL)
  - CHECK constraints: section_score 0-100, section_name IN ('HEADLINE','ABOUT','EXPERIENCE','EDUCATION','SKILLS','RECOMMENDATIONS','OTHER')
  - Foreign keys: tenant_id -> tenant(id), analysis_id -> linkedin_analysis(id)
  - Indexes: idx_linkedin_section_score_tenant_id, idx_linkedin_section_score_analysis_id
  - Rollback: `<dropTable tableName="linkedin_section_score"/>`

- [ ] T003 [DATABASE] Add migrations to db.changelog-master.yaml
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Add 2 include entries in order

### JPA Entities

- [ ] T004 [||] [JAVA25] Create LinkedInAnalysis entity
  - File: `backend/src/main/java/com/interviewme/linkedin/entity/LinkedInAnalysis.java`
  - Annotations: @Entity, @Table(name = "linkedin_analysis"), @Data, @NoArgsConstructor, @AllArgsConstructor, @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id, tenantId, profileId, status (String, default "PENDING"), overallScore (Integer), errorMessage, pdfFilename, analyzedAt (Instant), createdAt (Instant), updatedAt (Instant)
  - Lifecycle: @PrePersist, @PreUpdate for timestamps
  - Follow existing entity patterns (Instant for timestamps, no @Version needed since analysis is mostly write-once)

- [ ] T005 [||] [JAVA25] Create LinkedInSectionScore entity
  - File: `backend/src/main/java/com/interviewme/linkedin/entity/LinkedInSectionScore.java`
  - Annotations: @Entity, @Table(name = "linkedin_section_score"), @Data, @NoArgsConstructor, @AllArgsConstructor
  - Fields: id, tenantId, analysisId, sectionName, sectionScore (Integer), qualityExplanation, suggestions (List<String> JSONB), rawContent, createdAt (Instant)
  - JSONB: Use @Type(JsonBinaryType.class) for suggestions field
  - Lifecycle: @PrePersist for createdAt

### Validation

- [ ] T006 [DATABASE] Test migrations on fresh PostgreSQL database
  - Run `./gradlew bootRun` and verify tables created
  - Verify CHECK constraints (score range, status values, section names)
  - Verify indexes and foreign keys
  - Test JSONB suggestions column

---

## Phase 2: PDF Parsing Service (Day 2)

### Dependencies

- [ ] T007 [BUILD] Add Apache PDFBox dependency
  - File: `backend/build.gradle.kts`
  - Add: `implementation("org.apache.pdfbox:pdfbox:3.0.1")`
  - Verify dependency resolution: `./gradlew dependencies`

### PDF Parser

- [ ] T008 [MODULAR] Implement LinkedInPdfParserService
  - File: `backend/src/main/java/com/interviewme/linkedin/service/LinkedInPdfParserService.java`
  - Annotations: @Service, @Slf4j
  - Public method: `Map<String, String> parse(Path pdfPath)` - Extract text and identify sections
  - Implementation:
    - Use PDDocument.load() and PDFTextStripper to extract full text
    - Identify section boundaries using LinkedIn PDF markers:
      - "Headline" area: text near the name at the top
      - "About" section: text after "About" header
      - "Experience" section: text after "Experience" header
      - "Education" section: text after "Education" header
      - "Skills" section: text after "Skills" header
      - "Recommendations" section: text after "Recommendations" header
    - Return Map<String, String> with keys: HEADLINE, ABOUT, EXPERIENCE, EDUCATION, SKILLS, RECOMMENDATIONS, OTHER
    - Handle missing sections (return empty string)
    - Handle malformed PDFs (catch exceptions, return partial results)
  - Max 250 lines

### Parser Tests

- [ ] T009 [SIMPLE] Write LinkedInPdfParserService unit tests
  - File: `backend/src/test/java/com/interviewme/linkedin/service/LinkedInPdfParserServiceTest.java`
  - Test: parse_shouldExtractHeadline
  - Test: parse_shouldExtractAboutSection
  - Test: parse_shouldExtractExperienceSection
  - Test: parse_shouldHandleEmptyPdf
  - Test: parse_shouldHandleNonLinkedInPdf
  - Test: parse_shouldHandleMissingSection (returns empty string)
  - Use sample PDF files in test resources (create minimal test PDFs)

---

## Phase 3: LLM Integration (Day 3)

### LLM Client Stub (if needed)

- [ ] T010 [MODULAR] Create LlmClient interface and stub (if not yet implemented)
  - File: `backend/src/main/java/com/interviewme/ai/LlmClient.java` (interface)
  - File: `backend/src/main/java/com/interviewme/ai/StubLlmClient.java` (stub implementation)
  - Interface: `LlmResponse complete(LlmRequest request)`
  - LlmRequest: record with String prompt, String actionType, Map<String, Object> parameters
  - LlmResponse: record with String content, int tokensUsed, long latencyMs
  - Stub returns hardcoded JSON response for testing
  - Note: Skip if LlmClient already exists from another feature

### Prompt Service

- [ ] T011 [MODULAR] Implement LinkedInPromptService
  - File: `backend/src/main/java/com/interviewme/linkedin/service/LinkedInPromptService.java`
  - Annotations: @Service, @Slf4j
  - Methods:
    - `LlmRequest buildScoringPrompt(Map<String, String> sections)` - Constructs analysis prompt with:
      - Scoring rubric (0-39: Poor, 40-59: Below Average, 60-79: Good, 80-100: Excellent)
      - Section-specific criteria:
        - Headline: keywords, specificity, value proposition
        - About: metrics, storytelling, clarity, length
        - Experience: achievements vs duties, quantification, progression
        - Education: completeness, relevance
        - Skills: breadth, relevance, endorsement note
        - Recommendations: quantity, quality
      - Request for JSON output format
      - 1 concrete, actionable suggestion per section
    - `LlmRequest buildAdditionalSuggestionsPrompt(String sectionName, String rawContent, List<String> existingSuggestions)` - Prompt for N more suggestions avoiding duplicates
    - `LinkedInLlmResult parseScoringResponse(LlmResponse response)` - Parse JSON from LLM output
    - `List<String> parseAdditionalSuggestions(LlmResponse response)` - Parse suggestion list
  - Max 200 lines

### LLM Result DTO

- [ ] T012 [||] [JAVA25] Create LinkedInLlmResult internal DTO
  - File: `backend/src/main/java/com/interviewme/linkedin/dto/LinkedInLlmResult.java`
  - Record: LinkedInLlmResult(int overallScore, List<SectionResult> sections)
  - Inner record: SectionResult(String sectionName, int score, String explanation, String suggestion)

### Prompt Tests

- [ ] T013 [SIMPLE] Write LinkedInPromptService unit tests
  - File: `backend/src/test/java/com/interviewme/linkedin/service/LinkedInPromptServiceTest.java`
  - Test: buildScoringPrompt_shouldIncludeAllSections
  - Test: buildScoringPrompt_shouldIncludeRubric
  - Test: parseScoringResponse_shouldParseValidJson
  - Test: parseScoringResponse_shouldHandleMalformedJson (fallback)
  - Test: buildAdditionalSuggestionsPrompt_shouldExcludeExistingSuggestions

---

## Phase 4: Core Services & Repositories (Day 4-5)

### Repositories

- [ ] T014 [||] [DATA] Create LinkedInAnalysisRepository
  - File: `backend/src/main/java/com/interviewme/linkedin/repository/LinkedInAnalysisRepository.java`
  - Extends: JpaRepository<LinkedInAnalysis, Long>
  - Query methods:
    - `Optional<LinkedInAnalysis> findByIdAndTenantId(Long id, Long tenantId)` (explicit tenant check)
    - `Page<LinkedInAnalysis> findByProfileIdOrderByCreatedAtDesc(Long profileId, Pageable pageable)` (history)
    - `List<LinkedInAnalysis> findByStatusIn(List<String> statuses)` (for monitoring stuck analyses)

- [ ] T015 [||] [DATA] Create LinkedInSectionScoreRepository
  - File: `backend/src/main/java/com/interviewme/linkedin/repository/LinkedInSectionScoreRepository.java`
  - Extends: JpaRepository<LinkedInSectionScore, Long>
  - Query methods:
    - `List<LinkedInSectionScore> findByAnalysisIdOrderBySectionName(Long analysisId)`
    - `Optional<LinkedInSectionScore> findByAnalysisIdAndSectionName(Long analysisId, String sectionName)`

### DTOs

- [ ] T016 [||] [JAVA25] Create LinkedIn analysis DTOs
  - Files in `backend/src/main/java/com/interviewme/linkedin/dto/`:
  - `StartAnalysisResponse.java`: record(Long analysisId, String status, String message)
  - `LinkedInAnalysisResponse.java`: record(Long id, Long profileId, String status, Integer overallScore, String errorMessage, String pdfFilename, Instant analyzedAt, List<SectionScoreResponse> sections, Instant createdAt)
  - `LinkedInAnalysisSummary.java`: record(Long id, String status, Integer overallScore, String pdfFilename, Instant analyzedAt, Instant createdAt)
  - `SectionScoreResponse.java`: record(Long id, String sectionName, Integer sectionScore, String qualityExplanation, List<String> suggestions, boolean canApplyToProfile)
  - `GenerateSuggestionsRequest.java`: record(@Min(1) @Max(5) Integer count) -- default 3
  - `ApplySuggestionRequest.java`: record(@NotNull @Min(0) Integer suggestionIndex)

### Mapper

- [ ] T017 [SIMPLE] Create LinkedInAnalysisMapper
  - File: `backend/src/main/java/com/interviewme/linkedin/mapper/LinkedInAnalysisMapper.java`
  - Static methods:
    - `LinkedInAnalysisResponse toResponse(LinkedInAnalysis, List<LinkedInSectionScore>)`
    - `LinkedInAnalysisSummary toSummary(LinkedInAnalysis)`
    - `SectionScoreResponse toSectionResponse(LinkedInSectionScore)` -- includes canApplyToProfile logic (true for HEADLINE, ABOUT)

### Analysis Service

- [ ] T018 [MODULAR] Implement LinkedInAnalysisService
  - File: `backend/src/main/java/com/interviewme/linkedin/service/LinkedInAnalysisService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: LinkedInAnalysisRepository, LinkedInSectionScoreRepository, LinkedInPdfParserService, LinkedInPromptService, LlmRouterService (or LlmClient), CoinWalletService, ProfileService, TenantContext
  - Methods:
    - `@Transactional StartAnalysisResponse startAnalysis(Long profileId, MultipartFile pdf)`:
      1. Validate PDF (size, type)
      2. Save to temp file (UUID filename)
      3. Create LinkedInAnalysis(status=PENDING, pdfFilename=original name)
      4. Call processAnalysis(analysisId, tempPath) asynchronously
      5. Return StartAnalysisResponse with analysisId
    - `@Async void processAnalysis(Long analysisId, Path pdfPath)`:
      1. Update status -> IN_PROGRESS
      2. Parse PDF via LinkedInPdfParserService
      3. Delete temp PDF file (finally block)
      4. Build prompt via LinkedInPromptService
      5. Call LLM via LlmClient (with retry: 3 attempts, exponential backoff 2s/4s/8s)
      6. Parse LLM response
      7. Save LinkedInSectionScore records (one per section)
      8. Update LinkedInAnalysis: overallScore, status=COMPLETED, analyzedAt=now
      9. On failure: status=FAILED, errorMessage=exception message
    - `@Transactional(readOnly=true) LinkedInAnalysisResponse getAnalysis(Long analysisId)`:
      1. Load analysis + section scores
      2. Return full response
    - `@Transactional(readOnly=true) Page<LinkedInAnalysisSummary> getAnalysisHistory(Long profileId, Pageable pageable)`:
      1. Load analyses for profile, paginated, ordered by createdAt desc
    - `@Transactional SectionScoreResponse generateAdditionalSuggestions(Long analysisId, String sectionName, int count)`:
      1. Load section score record
      2. Calculate coin cost (e.g., count * LINKEDIN_SUGGESTION_COST_PER_ITEM or flat rate)
      3. Call CoinWalletService.spend(tenantId, cost, LINKEDIN_SUGGESTION, sectionScoreId)
      4. Build additional suggestions prompt
      5. Call LLM
      6. Parse new suggestions
      7. Append to existing suggestions JSONB array
      8. Save updated section score
      9. On LLM failure: call CoinWalletService.refund() and rethrow
    - `@Transactional void applySuggestion(Long analysisId, String sectionName, int suggestionIndex)`:
      1. Load section score
      2. Get suggestion at index
      3. Map section to profile field: HEADLINE -> profile.headline, ABOUT -> profile.summary
      4. Reject non-applicable sections (EXPERIENCE, EDUCATION, etc.)
      5. Update profile via ProfileService
  - Max 400 lines

### Async Configuration

- [ ] T019 [SIMPLE] Configure async executor with virtual threads
  - File: `backend/src/main/java/com/interviewme/config/AsyncConfig.java` (create or update)
  - Enable @Async: `@EnableAsync`
  - Configure virtual thread executor:
    ```java
    @Bean
    public Executor taskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    ```
  - Set timeout for async tasks if needed

---

## Phase 5: REST Controller & Exception Handling (Day 5)

### Controller

- [ ] T020 [MODULAR] Create LinkedInController
  - File: `backend/src/main/java/com/interviewme/linkedin/controller/LinkedInController.java`
  - Annotations: @RestController, @RequestMapping("/api/v1/linkedin"), @RequiredArgsConstructor, @Slf4j
  - Endpoints:
    - `POST /analyze` - accepts MultipartFile "file", returns 202 Accepted StartAnalysisResponse
    - `GET /analyses/{id}` - returns LinkedInAnalysisResponse (with section scores if COMPLETED)
    - `GET /analyses` - returns Page<LinkedInAnalysisSummary> with pagination params
    - `POST /analyses/{id}/sections/{sectionName}/suggestions` - @Valid GenerateSuggestionsRequest, returns SectionScoreResponse
    - `POST /analyses/{id}/sections/{sectionName}/apply` - @Valid ApplySuggestionRequest, returns 200 OK
  - File upload config: `spring.servlet.multipart.max-file-size=10MB` (add to application.yml)
  - All endpoints require JWT auth
  - Max 200 lines

### Exception Handling

- [ ] T021 [SIMPLE] Add LinkedIn-specific exception classes
  - Files in `backend/src/main/java/com/interviewme/common/exception/`:
  - `AnalysisNotFoundException.java`: extends RuntimeException
  - `InvalidPdfException.java`: extends RuntimeException (invalid file type or empty)
  - `SectionNotApplicableException.java`: extends RuntimeException (cannot apply Experience suggestions to profile)
  - `AnalysisNotCompletedException.java`: extends RuntimeException (trying to get results of PENDING analysis)

- [ ] T022 [SIMPLE] Add exception handlers to GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/config/GlobalExceptionHandler.java` (existing)
  - Handlers:
    - AnalysisNotFoundException -> 404
    - InvalidPdfException -> 400
    - SectionNotApplicableException -> 400
    - AnalysisNotCompletedException -> 400
    - MaxUploadSizeExceededException -> 413 (Spring multipart exception)
    - InsufficientBalanceException -> 402 (from Feature 007)

### Multipart Config

- [ ] T023 [SIMPLE] Configure multipart file upload limits
  - File: `backend/src/main/resources/application.yml` (or application.properties)
  - Add:
    ```yaml
    spring:
      servlet:
        multipart:
          max-file-size: 10MB
          max-request-size: 12MB
    ```

---

## Phase 6: Backend Testing (Day 5-6)

### Unit Tests

- [ ] T024 [||] [JAVA25] Write LinkedInAnalysisService unit tests
  - File: `backend/src/test/java/com/interviewme/linkedin/service/LinkedInAnalysisServiceTest.java`
  - Use @ExtendWith(MockitoExtension.class)
  - Tests:
    - startAnalysis_shouldCreatePendingRecord
    - startAnalysis_shouldRejectNonPdfFile
    - startAnalysis_shouldRejectOversizedFile
    - processAnalysis_shouldTransitionToCompleted
    - processAnalysis_shouldTransitionToFailedOnLlmError
    - processAnalysis_shouldRetryLlmCalls
    - processAnalysis_shouldDeleteTempFileAfterParsing
    - generateAdditionalSuggestions_shouldDeductCoins
    - generateAdditionalSuggestions_shouldRefundOnLlmFailure
    - applySuggestion_shouldUpdateProfileHeadline
    - applySuggestion_shouldRejectExperienceSection

### Integration Tests

- [ ] T025 [SIMPLE] Write LinkedInController integration tests
  - File: `backend/src/test/java/com/interviewme/linkedin/controller/LinkedInControllerTest.java`
  - Use @SpringBootTest, @AutoConfigureMockMvc
  - Tests:
    - POST /analyze with valid PDF -> 202 Accepted
    - POST /analyze with non-PDF file -> 400 Bad Request
    - POST /analyze with oversized file -> 413
    - GET /analyses/{id} -> returns current status
    - GET /analyses -> returns paginated history
    - POST /analyses/{id}/sections/HEADLINE/suggestions -> returns updated section
    - POST /analyses/{id}/sections/HEADLINE/apply -> updates profile
    - POST /analyses/{id}/sections/EXPERIENCE/apply -> 400 (not applicable)
    - Cross-tenant access -> 404

- [ ] T026 [SIMPLE] Write tenant isolation tests
  - File: `backend/src/test/java/com/interviewme/linkedin/TenantIsolationTest.java`
  - Test: Tenant A creates analysis, Tenant B cannot access -> 404
  - Test: Analysis history only shows current tenant's analyses

---

## Phase 7: Frontend - TypeScript Types & API (Day 6)

### TypeScript Types

- [ ] T027 [||] [SIMPLE] Create LinkedIn analysis TypeScript types
  - File: `frontend/src/types/linkedinAnalysis.ts`
  - Interfaces:
    - StartAnalysisResponse { analysisId: number, status: string, message: string }
    - LinkedInAnalysisResponse { id, profileId, status, overallScore, errorMessage, pdfFilename, analyzedAt, sections: SectionScoreResponse[], createdAt }
    - LinkedInAnalysisSummary { id, status, overallScore, pdfFilename, analyzedAt, createdAt }
    - SectionScoreResponse { id, sectionName, sectionScore, qualityExplanation, suggestions: string[], canApplyToProfile: boolean }
    - GenerateSuggestionsRequest { count: number }
    - ApplySuggestionRequest { suggestionIndex: number }

### API Client

- [ ] T028 [MODULAR] Create LinkedIn API client
  - File: `frontend/src/api/linkedinApi.ts`
  - Functions:
    - `uploadAndAnalyze(file: File): Promise<StartAnalysisResponse>` - POST /analyze (multipart)
    - `getAnalysis(id: number): Promise<LinkedInAnalysisResponse>` - GET /analyses/{id}
    - `getAnalysisHistory(page: number, size: number): Promise<Page<LinkedInAnalysisSummary>>` - GET /analyses
    - `generateSuggestions(analysisId: number, sectionName: string, count: number): Promise<SectionScoreResponse>` - POST suggestions
    - `applySuggestion(analysisId: number, sectionName: string, index: number): Promise<void>` - POST apply
  - Use axios with JWT interceptor, multipart form data for upload
  - Max 200 lines

### React Query Hooks

- [ ] T029 [MODULAR] Create useLinkedInAnalysis hook
  - File: `frontend/src/hooks/useLinkedInAnalysis.ts`
  - Hooks:
    - `useAnalysis(analysisId: number)` - Query with refetchInterval (3s while PENDING/IN_PROGRESS, stop on COMPLETED/FAILED)
    - `useAnalysisHistory(page: number)` - Paginated query
    - `useUploadAndAnalyze()` - Mutation for file upload
    - `useGenerateSuggestions()` - Mutation for paid suggestions
    - `useApplySuggestion()` - Mutation for applying suggestion
  - Polling logic: refetchInterval conditionally set based on status
  - Max 200 lines

---

## Phase 8: Frontend - React Components (Day 7-8)

### Reusable Components

- [ ] T030 [MODULAR] Create PdfUploader component
  - File: `frontend/src/components/linkedin/PdfUploader.tsx`
  - Drag-and-drop zone with file picker fallback
  - Validate: PDF only, max 10MB
  - Show file name after selection
  - "Analyze" button triggers upload
  - Loading state during upload
  - Error display for invalid files
  - Props: onUpload(file: File), isLoading, error
  - Max 150 lines

- [ ] T031 [MODULAR] Create ScoreGauge component
  - File: `frontend/src/components/linkedin/ScoreGauge.tsx`
  - Circular progress indicator showing score 0-100
  - Color coding: 0-39 red, 40-59 orange, 60-79 blue, 80-100 green
  - Score number displayed in center
  - Props: score (number), size ('small' | 'large')
  - Use MUI CircularProgress or custom SVG
  - Max 100 lines

- [ ] T032 [MODULAR] Create SectionScoreCard component
  - File: `frontend/src/components/linkedin/SectionScoreCard.tsx`
  - Displays: section name, ScoreGauge (small), quality explanation text
  - Suggestions list with index indicator (free vs paid)
  - Each suggestion has "Apply to profile" button (only if canApplyToProfile)
  - "Get more suggestions (N coins)" button at bottom
  - Applied suggestions marked with checkmark
  - Props: section (SectionScoreResponse), onGenerateMore(sectionName), onApply(sectionName, index), coinCost
  - Max 200 lines

- [ ] T033 [||] [MODULAR] Create SuggestionItem component
  - File: `frontend/src/components/linkedin/SuggestionItem.tsx`
  - Displays suggestion text, index badge (free/paid), "Apply" button
  - "Applied" badge after successful application
  - Props: text, index, isFree, canApply, isApplied, onApply
  - Max 100 lines

- [ ] T034 [MODULAR] Create AnalysisHistory component
  - File: `frontend/src/components/linkedin/AnalysisHistory.tsx`
  - Table/card list of past analyses: date, overall score, status
  - Score trend arrows: up (improved), down (declined), dash (same)
  - Click to view full details (navigate or expand)
  - Pagination controls
  - "Run new analysis" button
  - Props: analyses (LinkedInAnalysisSummary[]), onViewDetails(id), onNewAnalysis
  - Max 150 lines

### Page

- [ ] T035 [MODULAR] Create LinkedInAnalyzerPage
  - File: `frontend/src/pages/LinkedInAnalyzerPage.tsx`
  - Layout sections:
    1. Header: "LinkedIn Profile Analyzer" with description
    2. PdfUploader component
    3. Processing indicator (spinner while PENDING/IN_PROGRESS)
    4. Results section (when COMPLETED):
       - Overall ScoreGauge (large)
       - Section cards grid (SectionScoreCard for each section)
    5. Error display (when FAILED)
    6. AnalysisHistory section below results
  - Uses useAnalysis hook with polling for active analysis
  - Uses useAnalysisHistory for past analyses
  - Handles coin balance display for paid suggestions
  - Max 300 lines

### Routing & Navigation

- [ ] T036 [SIMPLE] Add LinkedIn analyzer route
  - File: `frontend/src/App.tsx` or equivalent router config
  - Route: `/linkedin-analyzer` -> LinkedInAnalyzerPage (protected, requires auth)

- [ ] T037 [SIMPLE] Add navigation menu item
  - File: `frontend/src/components/Navigation.tsx` or equivalent
  - Add "LinkedIn Analyzer" menu item in main navigation

---

## Checkpoints

After each phase, verify:

- [ ] **Phase 1 Complete:** Tables created with correct schema, constraints, indexes. Entities compile and map to tables.
- [ ] **Phase 2 Complete:** PDF parser extracts sections from sample LinkedIn PDFs. Edge cases handled.
- [ ] **Phase 3 Complete:** Prompt templates produce valid LLM requests. Response parser handles JSON output.
- [ ] **Phase 4 Complete:** Analysis service orchestrates full pipeline (mock LLM). Coin integration works.
- [ ] **Phase 5 Complete:** All endpoints return correct HTTP status codes. File upload works. Exception handling in place.
- [ ] **Phase 6 Complete:** Unit tests pass. Integration tests pass. Tenant isolation verified.
- [ ] **Phase 7 Complete:** API client compiles. Hooks work with polling.
- [ ] **Phase 8 Complete:** Upload works. Scores display with color coding. Suggestions show free/paid distinction. Apply updates profile.

---

## Success Criteria Verification

### From spec.md

1. **PDF Upload and Processing**
   - [ ] Upload PDF -> analysis completes within 60 seconds (with mocked LLM)
   - [ ] Invalid files rejected with appropriate errors

2. **Section Extraction**
   - [ ] 6+ sections extracted from standard LinkedIn PDF
   - [ ] Missing sections handled gracefully (score 0)

3. **Meaningful Scores**
   - [ ] LLM produces scores in valid range (0-100)
   - [ ] Each section has quality explanation and 1 free suggestion

4. **Paid Suggestions**
   - [ ] Coins deducted before LLM call
   - [ ] Auto-refund on LLM failure
   - [ ] Suggestions appended to existing list

5. **Apply Suggestion**
   - [ ] HEADLINE suggestion updates Profile.headline
   - [ ] ABOUT suggestion updates Profile.summary
   - [ ] Non-applicable sections rejected (400)

6. **Analysis History**
   - [ ] Past analyses retrievable and paginated
   - [ ] Score comparison between analyses possible

7. **Tenant Isolation**
   - [ ] Cross-tenant analysis access returns 404

8. **Failed Analysis**
   - [ ] LLM failure -> FAILED status, no coins charged

---

## Notes

### Key Dependencies (Execution Order)
- Phase 1 (DB) must complete before Phase 4 (Services)
- Phase 2 (PDF Parser) must complete before Phase 4
- Phase 3 (LLM) must complete before Phase 4
- Phases 2 and 3 can run in parallel
- Phase 4 must complete before Phase 5 (Controller)
- Phase 7 (Frontend API) can start once API contracts defined (Phase 5)
- Phase 8 (Frontend UI) depends on Phase 7

### Parallel Execution Opportunities
- Phase 2 (PDF Parser) and Phase 3 (LLM Prompts) are independent -- run in parallel
- Within Phase 4: Repositories and DTOs can be created in parallel
- Within Phase 6: Unit tests and integration tests in parallel
- Within Phase 8: ScoreGauge, SuggestionItem, AnalysisHistory in parallel

### Risk Mitigation
- **LinkedIn PDF format:** Start with current format, build flexible parser with section header detection
- **LLM response quality:** Include structured prompt with scoring rubric, validate response format
- **Temp file cleanup:** Always delete in finally block; add scheduled cleanup job for orphaned files
- **Coin integration:** Test spend + refund flow thoroughly before deploying

### External Dependencies
- **LlmClient interface:** If not yet implemented, create stub that returns hardcoded analysis results
- **CoinWalletService:** Must be available from Feature 007 for paid suggestions

---

**Tasks Version:** 1.0.0
**Last Updated:** 2026-02-24
**Total Tasks:** 37
**Estimated Time:** 6-8 days
