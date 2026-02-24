# Feature Specification: LinkedIn Profile Score Analyzer

**Feature ID:** 009-linkedin-analyzer
**Status:** Draft
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Overview

### Problem Statement

Career professionals invest significant effort in maintaining LinkedIn profiles but lack objective, actionable feedback on profile quality. Common issues include:
- No way to objectively assess how strong a LinkedIn profile appears to recruiters
- Headline, About, and Experience sections often lack metrics, keywords, and concrete achievements
- Improvement suggestions from generic online articles are not personalized to the user's profile content
- No tracking of profile improvements over time
- Existing LinkedIn Profile Strength indicators are opaque and non-actionable

Without structured analysis and concrete suggestions, professionals miss opportunities to optimize their LinkedIn presence for recruiter visibility, search ranking, and first-impression quality.

### Feature Summary

Implement an AI-powered LinkedIn Profile Score Analyzer that accepts a user-uploaded LinkedIn PDF export, processes it asynchronously via an LLM, and produces:
1. An **overall profile score** (0-100)
2. **Per-section scores** (0-100) for: Headline, About, Experience, Education, Skills, Recommendations, and Other sections
3. A **quality explanation** per section (e.g., "About is clear but lacks quantitative metrics")
4. **Improvement suggestions** per section -- 1 free suggestion included, additional suggestions cost coins

The analysis runs asynchronously (PDF parsing + LLM call), stores results as snapshots for historical tracking, and integrates with the coin wallet (Feature 007) for paid suggestions. Users can optionally "Apply suggestion" to update their platform profile fields.

### Target Users

- **Career Professionals (Primary User):** Senior engineers, tech leads, architects who want to optimize their LinkedIn profiles for recruiter visibility
- **Job Seekers:** Professionals actively looking for new roles who need their LinkedIn profile to stand out
- **Platform Admin:** Monitors LLM usage and costs for analysis features

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Standard REST API with async job processing, no complex ML pipelines, LLM-based analysis via existing LlmClient interface
- **Principle 3: Modern Java Standards** - Java 25 records for DTOs, virtual threads for async processing, pattern matching where applicable
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - All entities (LinkedInAnalysis, LinkedInSectionScore) MUST include tenant_id with automatic Hibernate filtering
- **Principle 5: AI Integration and LLM Management** - Uses LlmClient interface for analysis, backend-controlled prompts, cost tracking per LLM call, per-tenant provider configuration
- **Principle 6: Observability and Debugging** - Structured logging for analysis lifecycle (upload, parsing, LLM call, completion), LLM call metrics (latency, token count, cost proxy)
- **Principle 7: Security, Privacy, and Credential Management** - JWT authentication for all endpoints, @Transactional annotations, uploaded PDFs stored securely and deleted after parsing
- **Principle 8: Multi-Tenant Architecture** - All entities include tenant_id, analysis results tenant-isolated
- **Principle 9: Freemium Model** - Full analysis (scores + brief comments) is free. 1 free suggestion per section, additional suggestions cost coins
- **Principle 10: Full-Stack Modularity** - Backend: `com.interviewme.linkedin` package, Frontend: LinkedInAnalyzerPage component
- **Principle 11: Database Schema Evolution** - Liquibase timestamp-based migrations for linkedin_analysis and linkedin_section_score tables
- **Principle 12: Async Job Processing** - Analysis runs asynchronously using @Async or job queue, job status exposed via API for frontend polling
- **Principle 13: LinkedIn Compliance** - PDF upload (user-initiated), no scraping, no automated LinkedIn interactions

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: User Uploads LinkedIn PDF for First Analysis

**Actor:** Career professional wanting to assess LinkedIn profile quality
**Goal:** Upload LinkedIn PDF and receive comprehensive profile scoring
**Preconditions:** User is authenticated, has existing profile (Feature 002)

**Steps:**
1. User navigates to "LinkedIn Analyzer" page
2. User clicks "Upload LinkedIn PDF" button
3. User selects LinkedIn PDF export from file system
4. Frontend uploads PDF via multipart form POST
5. System validates file (PDF format, max 10MB, not empty)
6. System creates LinkedInAnalysis record with status=PENDING
7. System returns analysis ID immediately (202 Accepted)
8. Frontend begins polling for analysis status
9. Backend asynchronously:
   a. Parses PDF to extract text content per section (Headline, About, Experience, Education, Skills, Recommendations)
   b. Calls LLM via LlmClient with structured prompt containing extracted sections
   c. LLM returns: overall score, per-section scores, quality explanations, and 1 suggestion per section
   d. System stores results in LinkedInAnalysis and LinkedInSectionScore tables
   e. System updates analysis status to COMPLETED
10. Frontend detects COMPLETED status, displays results
11. User sees overall score (e.g., 72/100) and per-section breakdown

**Success Criteria:**
- PDF uploaded and parsed successfully
- Analysis completes within 30-60 seconds
- Overall score displayed prominently
- Per-section scores with quality explanations visible
- 1 free suggestion displayed per section
- "Get more suggestions" button visible (coins required)

#### Scenario 2: User Requests Additional Suggestions (Paid)

**Actor:** Professional wanting deeper improvement advice for a specific section
**Goal:** Get 3 additional headline alternatives (costs coins)
**Preconditions:** User has completed analysis, has sufficient coin balance

**Steps:**
1. User views analysis results on LinkedIn Analyzer page
2. User sees "Headline" section scored 65/100 with 1 free suggestion
3. User clicks "Get more suggestions" for Headline section
4. System shows: "Generate 3 additional headline suggestions for 5 coins. Balance: 50 coins."
5. User confirms
6. System calls CoinWalletService.spend(tenantId, 5, LINKEDIN_SUGGESTION, sectionScoreId)
7. System calls LLM to generate 3 additional headline suggestions based on profile data
8. System appends new suggestions to LinkedInSectionScore record
9. User sees 4 total suggestions (1 free + 3 paid) for Headline section
10. Each suggestion shows "Apply to profile" option

**Success Criteria:**
- Coin balance checked before LLM call
- InsufficientBalanceException returns clear error if not enough coins
- Coins deducted only after successful LLM response
- Auto-refund if LLM call fails
- New suggestions appended to existing section score record
- Transaction recorded: type=SPEND, refType=LINKEDIN_SUGGESTION

#### Scenario 3: User Applies Suggestion to Platform Profile

**Actor:** Professional importing a suggestion into their Interview Me profile
**Goal:** Use an AI-generated headline suggestion to update platform profile
**Preconditions:** User has analysis with suggestions, has existing profile

**Steps:**
1. User views a suggestion: "Rewrite your headline to: 'Senior Software Engineer | 12+ Years Java & Cloud | Building High-Scale Payments Systems'"
2. User clicks "Apply to profile"
3. System identifies the target profile field (headline)
4. System updates Profile.headline with the suggestion text
5. User sees confirmation: "Profile headline updated successfully"
6. User can undo by editing profile directly

**Success Criteria:**
- Correct profile field updated based on section type
- Only applicable sections can be "applied" (Headline -> profile.headline, About -> profile.summary)
- Profile update follows existing Profile CRUD logic (Feature 002)
- Applied suggestion marked in UI to prevent re-application

#### Scenario 4: User Views Analysis History

**Actor:** Professional tracking LinkedIn improvements over time
**Goal:** Compare current analysis with previous analysis from 3 months ago
**Preconditions:** User has multiple past analyses

**Steps:**
1. User navigates to LinkedIn Analyzer page
2. User sees "Analysis History" section showing list of past analyses
3. Each entry shows: date, overall score, status
4. User clicks on older analysis to view full details
5. User sees score trend: 58 (Jan) -> 65 (Feb) -> 72 (Mar)
6. System highlights sections that improved or declined

**Success Criteria:**
- All past analyses accessible (not deleted)
- Chronological ordering (most recent first)
- Score comparison between analyses possible
- Previous analysis snapshots preserved exactly as they were

#### Scenario 5: Analysis Fails (Error Handling)

**Actor:** User whose analysis fails due to LLM error
**Goal:** Gracefully handle analysis failure
**Preconditions:** User uploaded PDF, analysis is processing

**Steps:**
1. User uploads PDF, gets analysis ID
2. Backend starts processing: PDF parsed successfully
3. LLM call times out after 60 seconds
4. System retries LLM call (max 3 retries with exponential backoff)
5. All retries fail
6. System updates analysis status to FAILED with error message
7. Frontend shows: "Analysis failed. Please try again later. No coins were charged."
8. User can retry by re-uploading the PDF

**Success Criteria:**
- Analysis status set to FAILED with descriptive error
- No coins charged for failed analyses (free tier analysis)
- User can retry without restrictions
- Error logged with tenant context for debugging

### Edge Cases

- **Invalid PDF:** User uploads a non-PDF file or corrupted PDF -> 400 Bad Request with clear message
- **Empty LinkedIn PDF:** PDF has no parseable text -> Analysis completes with low scores and explanation "Unable to extract content"
- **Very large PDF:** File exceeds 10MB limit -> 413 Payload Too Large
- **Concurrent analyses:** User triggers two analyses simultaneously -> Allow (each tracked independently)
- **LLM provider unavailable:** No LLM provider configured for tenant -> 503 Service Unavailable
- **Analysis while previous is pending:** Allow multiple pending analyses (user may upload corrected PDF)
- **Section not found in PDF:** LinkedIn PDF missing "Recommendations" section -> Score 0/100 with explanation "Section not found"
- **Non-English LinkedIn profiles:** LLM should handle multilingual content -> Best-effort analysis
- **PDF from non-LinkedIn source:** User uploads generic PDF -> Analysis produces low-confidence results with note

---

## Functional Requirements

### Core Capabilities

**REQ-001:** LinkedIn PDF Upload
- **Description:** Users MUST be able to upload a LinkedIn profile PDF export for analysis
- **Acceptance Criteria:**
  - Accept multipart file upload (PDF only)
  - Maximum file size: 10MB
  - Validate file extension (.pdf) and MIME type (application/pdf)
  - Store uploaded PDF temporarily for processing (delete after parsing complete)
  - Return analysis ID immediately (202 Accepted) for async tracking
  - Each upload creates a new LinkedInAnalysis record

**REQ-002:** Asynchronous PDF Processing
- **Description:** System MUST process LinkedIn PDF asynchronously to avoid blocking HTTP threads
- **Acceptance Criteria:**
  - Analysis runs in background thread (Spring @Async or job queue)
  - Processing pipeline: PDF parsing -> text extraction -> section identification -> LLM analysis -> result storage
  - Job status tracked: PENDING -> IN_PROGRESS -> COMPLETED or FAILED
  - Maximum processing time: 120 seconds (timeout with FAILED status)
  - Retry failed LLM calls up to 3 times with exponential backoff (2s, 4s, 8s)
  - Frontend polls for status via GET endpoint (every 3 seconds while PENDING/IN_PROGRESS)

**REQ-003:** PDF Section Extraction
- **Description:** System MUST parse LinkedIn PDF and extract text content for each profile section
- **Acceptance Criteria:**
  - Extract sections: Headline, About, Experience, Education, Skills, Recommendations
  - Handle varied LinkedIn PDF formats (different export versions, languages)
  - Store raw extracted text per section for LLM prompt construction
  - If a section cannot be found, mark as "not found" (score 0)
  - Use Apache PDFBox or similar library for PDF text extraction

**REQ-004:** LLM-Based Profile Scoring
- **Description:** System MUST call LLM to analyze extracted profile content and produce scores
- **Acceptance Criteria:**
  - Use existing LlmClient interface (LlmRouterService selects provider based on action type ANALYZE_LINKEDIN_PROFILE)
  - Structured prompt includes: extracted section text, scoring rubric (0-100), request for quality explanation and 1 suggestion per section
  - LLM response parsed into: overall score (0-100), per-section scores (0-100), quality explanations (string), suggestions (array of strings)
  - Prompt template stored in dedicated service (LinkedInPromptService)
  - Track LLM call metrics: latency, token count, estimated cost

**REQ-005:** Analysis Result Storage
- **Description:** System MUST store analysis results as immutable snapshots
- **Acceptance Criteria:**
  - LinkedInAnalysis entity: id, tenant_id, profile_id, status (PENDING/IN_PROGRESS/COMPLETED/FAILED), overall_score (INT 0-100), error_message (TEXT), pdf_filename (VARCHAR), analyzed_at (TIMESTAMPTZ), created_at, updated_at
  - LinkedInSectionScore entity: id, tenant_id, analysis_id (FK), section_name (VARCHAR), section_score (INT 0-100), quality_explanation (TEXT), suggestions (JSONB array of strings), raw_content (TEXT, extracted section text), created_at
  - Analysis results are immutable after COMPLETED (no updates)
  - Historical analyses preserved for trend tracking

**REQ-006:** Free Suggestion + Paid Additional Suggestions
- **Description:** Each section includes 1 free suggestion; additional suggestions cost coins
- **Acceptance Criteria:**
  - Initial analysis includes 1 suggestion per section (FREE, no coin cost)
  - "Get more suggestions" endpoint generates N additional suggestions via LLM call
  - Cost: configurable coins per batch of suggestions (e.g., LINKEDIN_SUGGESTION_COST = 5 coins for 3 suggestions)
  - Integrate with CoinWalletService.spend() from Feature 007
  - Auto-refund via CoinWalletService.refund() if LLM call fails
  - New suggestions appended to existing LinkedInSectionScore.suggestions JSONB array
  - Track which suggestions are free vs paid (index 0 = free, rest = paid)

**REQ-007:** Apply Suggestion to Profile
- **Description:** Users MUST be able to apply an AI-generated suggestion to their platform profile
- **Acceptance Criteria:**
  - "Apply to profile" action available for applicable sections:
    - Headline suggestion -> Profile.headline
    - About suggestion -> Profile.summary
  - Uses existing ProfileService.updateProfile() from Feature 002
  - Confirmation before applying (irreversible via this action, but user can manually edit)
  - Applied suggestion marked in UI (prevents accidental double-apply)
  - Not all sections are applicable (Experience, Education, Skills, Recommendations cannot be directly applied as single fields)

**REQ-008:** Analysis Status Polling
- **Description:** Frontend MUST be able to poll for analysis status until completion
- **Acceptance Criteria:**
  - GET endpoint returns current analysis status and results if COMPLETED
  - Poll interval: 3 seconds (frontend responsibility)
  - Returns partial data if available (e.g., status=IN_PROGRESS, progress percentage optional)
  - Terminal statuses: COMPLETED, FAILED (stop polling)

**REQ-009:** Analysis History
- **Description:** Users MUST be able to view all past analyses for trend tracking
- **Acceptance Criteria:**
  - GET endpoint returns list of past analyses (paginated, most recent first)
  - Each entry shows: date, overall_score, status
  - User can click to view full details of any past analysis
  - No limit on number of stored analyses (soft limit: 100 per tenant)

### User Interface Requirements

**REQ-UI-001:** LinkedIn Analyzer Page
- **Description:** Frontend MUST provide a dedicated page for LinkedIn profile analysis
- **Acceptance Criteria:**
  - Page accessible via "/linkedin-analyzer" route
  - Sections: Upload area, Current/Latest Analysis results, Analysis History
  - Upload area: drag-and-drop or file picker for PDF, max 10MB indicator
  - Processing indicator: spinner with "Analyzing your profile..." message while PENDING/IN_PROGRESS
  - Results display: overall score prominently (circular gauge or large number), per-section breakdown below

**REQ-UI-002:** Score Display Component
- **Description:** Frontend MUST display scores visually with color coding
- **Acceptance Criteria:**
  - Overall score: large circular progress indicator (0-100)
  - Color coding: 0-39 (red), 40-59 (orange), 60-79 (blue), 80-100 (green)
  - Per-section cards: section name, score bar, quality explanation, suggestions list
  - Score bars use same color coding as overall score

**REQ-UI-003:** Section Detail Card
- **Description:** Frontend MUST display section details with suggestions
- **Acceptance Criteria:**
  - Section name and score (e.g., "Headline - 65/100")
  - Quality explanation text (e.g., "Your headline mentions your role but lacks keywords and metrics")
  - Free suggestion displayed by default
  - "Get more suggestions (5 coins)" button for additional suggestions
  - Each suggestion has "Apply to profile" button (only for applicable sections)
  - Applied suggestions visually marked (checkmark or "Applied" badge)

**REQ-UI-004:** Analysis History Section
- **Description:** Frontend MUST display past analyses for comparison
- **Acceptance Criteria:**
  - Table or card list of past analyses: date, overall score, status
  - Click to expand/view full details
  - Score trend indicator: arrow up (improved), arrow down (declined), dash (same)
  - "Run new analysis" button always visible

### Data Requirements

**REQ-DATA-001:** LinkedInAnalysis Table Schema
- **Description:** PostgreSQL table for storing analysis metadata and results
- **Acceptance Criteria:**
  - Table name: `linkedin_analysis`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), profile_id (BIGINT FK NOT NULL), status (VARCHAR 20 NOT NULL DEFAULT 'PENDING'), overall_score (INT CHECK 0-100), error_message (TEXT), pdf_filename (VARCHAR 255), analyzed_at (TIMESTAMPTZ), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL)
  - Indexes: idx_linkedin_analysis_tenant_id, idx_linkedin_analysis_profile_id, idx_linkedin_analysis_status
  - Foreign keys: tenant_id -> tenant(id), profile_id -> profile(id)
  - Status values: PENDING, IN_PROGRESS, COMPLETED, FAILED

**REQ-DATA-002:** LinkedInSectionScore Table Schema
- **Description:** PostgreSQL table for storing per-section analysis results
- **Acceptance Criteria:**
  - Table name: `linkedin_section_score`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), analysis_id (BIGINT FK NOT NULL), section_name (VARCHAR 100 NOT NULL), section_score (INT NOT NULL CHECK 0-100), quality_explanation (TEXT NOT NULL), suggestions (JSONB array of strings), raw_content (TEXT), created_at (TIMESTAMPTZ NOT NULL)
  - Indexes: idx_linkedin_section_score_tenant_id, idx_linkedin_section_score_analysis_id
  - Foreign keys: tenant_id -> tenant(id), analysis_id -> linkedin_analysis(id)
  - Section names: HEADLINE, ABOUT, EXPERIENCE, EDUCATION, SKILLS, RECOMMENDATIONS, OTHER

---

## Success Criteria

The feature will be considered successful when:

1. **PDF Upload and Processing:** User uploads LinkedIn PDF, analysis completes within 60 seconds
   - Measurement: Integration test uploads sample PDF, verifies COMPLETED status within timeout

2. **Accurate Section Extraction:** PDF parser correctly identifies and extracts 6+ LinkedIn sections
   - Measurement: Test with 3 different LinkedIn PDF formats, verify section extraction accuracy > 90%

3. **Meaningful Scores:** LLM produces scores that correlate with profile quality (manually verified)
   - Measurement: Analyze 5 profiles of known quality, verify scores align with human assessment

4. **Free Suggestion Included:** Each section in completed analysis has exactly 1 free suggestion
   - Measurement: Integration test verifies all 7 sections have suggestions array with >= 1 entry

5. **Paid Suggestions Work:** Additional suggestions generated and deducted from coin balance
   - Measurement: Test with 50-coin balance, request 3 additional suggestions (5 coins), verify balance = 45

6. **Apply Suggestion Works:** Headline/About suggestions applied to profile fields
   - Measurement: Apply headline suggestion, verify Profile.headline updated via Profile API

7. **Analysis History Preserved:** Past analyses accessible and scores comparable
   - Measurement: Run 3 analyses, verify all 3 retrievable via history endpoint

8. **Tenant Isolation:** Users cannot access other tenants' analyses
   - Measurement: Cross-tenant access returns 404

9. **Failed Analysis Handling:** LLM failure results in FAILED status with no coin charges
   - Measurement: Mock LLM to fail, verify status=FAILED, no coin transactions created

---

## Key Entities

### LinkedInAnalysis

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- profile_id: Foreign key to Profile (BIGINT, NOT NULL, indexed)
- status: Analysis status (VARCHAR 20, NOT NULL, values: PENDING, IN_PROGRESS, COMPLETED, FAILED)
- overall_score: Overall profile score (INT, 0-100, NULL until completed)
- error_message: Error details if FAILED (TEXT, NULL)
- pdf_filename: Original uploaded filename (VARCHAR 255)
- analyzed_at: Timestamp when analysis completed (TIMESTAMPTZ, NULL until completed)
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)

**Relationships:**
- Many-to-one with Tenant
- Many-to-one with Profile (many analyses belong to one profile)
- One-to-many with LinkedInSectionScore (one analysis has many section scores)

**Validation Rules:**
- status: Required, must be one of PENDING, IN_PROGRESS, COMPLETED, FAILED
- overall_score: 0-100 inclusive, NULL until analysis completes
- pdf_filename: Required at creation time

### LinkedInSectionScore

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- analysis_id: Foreign key to LinkedInAnalysis (BIGINT, NOT NULL, indexed)
- section_name: LinkedIn section identifier (VARCHAR 100, NOT NULL)
- section_score: Section score (INT, NOT NULL, 0-100)
- quality_explanation: Text explanation of current quality (TEXT, NOT NULL)
- suggestions: Array of improvement suggestions (JSONB, array of strings)
- raw_content: Extracted text from this section (TEXT, may be NULL if section not found)
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)

**Relationships:**
- Many-to-one with Tenant
- Many-to-one with LinkedInAnalysis (many section scores belong to one analysis)

**Validation Rules:**
- section_name: Required, one of HEADLINE, ABOUT, EXPERIENCE, EDUCATION, SKILLS, RECOMMENDATIONS, OTHER
- section_score: 0-100 inclusive, required
- quality_explanation: Required
- suggestions: JSONB array of strings, index 0 = free suggestion, rest = paid

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Authentication, tenant filtering, database infrastructure
- **Feature 002: Profile CRUD** - Profile entity for linking analyses and applying suggestions
- **Feature 007: Coin Wallet & Billing** - CoinWalletService for paid suggestions (spend, refund)

### External Dependencies

- **Apache PDFBox (org.apache.pdfbox:pdfbox):** PDF text extraction library
- **LlmClient interface (Feature 005/006):** LLM integration for analysis -- if not yet implemented, use a stub/mock
- **Spring @Async:** For asynchronous analysis processing
- **MUI File Upload:** For frontend file upload component
- **MUI CircularProgress / LinearProgress:** For score display components

---

## Assumptions

1. Features 001 and 002 are fully implemented (auth, tenancy, profiles)
2. Feature 007 (Coin Wallet) is implemented for paid suggestions integration
3. LlmClient interface exists (at minimum as an interface with stub implementation)
4. LinkedIn PDF export format is relatively consistent (standard LinkedIn PDF export)
5. PDF parsing produces sufficient text quality for LLM analysis (no OCR needed -- LinkedIn PDFs are text-based)
6. LLM can produce consistent structured output (JSON format) with scores and suggestions
7. Average analysis time is 15-45 seconds (PDF parsing ~2s + LLM call ~15-40s)
8. Users upload genuine LinkedIn PDFs (no adversarial input validation needed beyond basic checks)
9. Analysis results are immutable after completion (no editing scores)
10. Uploaded PDFs are temporary files deleted after parsing (not stored permanently)
11. Free-tier allows unlimited analyses (no limit on number of analyses per month)
12. Each suggestion batch request generates 3 additional suggestions (configurable)

---

## Out of Scope

1. **No URL-Based Analysis:** Fetching LinkedIn profiles by URL (ToS compliance, deferred to future)
2. **No Auto-Refresh Analysis:** Periodic re-analysis of profiles
3. **No PDF Storage:** Uploaded PDFs are not stored permanently (parsed and deleted)
4. **No Photo/Banner Analysis:** Image analysis of profile photos (would require vision model)
5. **No Competitor Comparison:** Comparing user's profile against industry benchmarks or peers
6. **No LinkedIn API Integration:** Direct LinkedIn API calls for profile data
7. **No Multi-Language UI:** Analysis page in English only
8. **No Export of Analysis Results:** No PDF/CSV export of analysis results
9. **No Collaborative Analysis:** Sharing analysis results with others
10. **No Automated LinkedIn Updates:** No pushing suggestions back to LinkedIn
11. **No OCR for Scanned PDFs:** Only text-based PDFs supported
12. **No Real-Time Analysis:** All analyses are asynchronous (no synchronous LLM call)

---

## Security & Privacy Considerations

### Security Requirements

- All LinkedIn analyzer endpoints MUST require valid JWT authentication
- Tenant filtering MUST be applied to all analysis queries (prevent cross-tenant access)
- Uploaded PDF files MUST be validated (size, type) and stored temporarily with secure filenames
- PDF files MUST be deleted after text extraction is complete
- LLM prompts MUST NOT include tenant_id or user credentials
- Analysis results MUST be tenant-isolated

### Privacy Requirements

- LinkedIn profile content (extracted from PDF) is sensitive personal data
- Raw extracted text stored in linkedin_section_score.raw_content for LLM re-analysis (suggestions)
- Users own their analysis data (accessible only to them)
- No sharing of analysis results across tenants
- Uploaded PDFs processed in-memory or temporary storage only, deleted after extraction
- LLM provider receives profile text -- users implicitly consent by uploading

---

## Performance Expectations

- **PDF Upload:** p95 latency < 2 seconds (file reception and validation)
- **PDF Parsing:** p95 latency < 5 seconds (text extraction via PDFBox)
- **LLM Analysis Call:** p95 latency < 45 seconds (depends on LLM provider)
- **Total Analysis Time:** p95 < 60 seconds (parsing + LLM)
- **Status Polling:** p95 latency < 100ms (simple database read)
- **Analysis History List:** p95 latency < 200ms (paginated, indexed)
- **Additional Suggestions Generation:** p95 < 30 seconds (LLM call for specific section)
- **Apply Suggestion:** p95 < 300ms (simple profile update)

---

## Error Handling

### Error Scenarios

**ERROR-001:** Invalid File Upload
- **User Experience:** API returns 400 Bad Request: `{"message": "Only PDF files are accepted", "code": "INVALID_FILE_TYPE"}`
- **Recovery Path:** User uploads correct PDF file

**ERROR-002:** File Too Large
- **User Experience:** API returns 413 Payload Too Large: `{"message": "File exceeds maximum size of 10MB", "code": "FILE_TOO_LARGE"}`
- **Recovery Path:** User compresses or re-exports LinkedIn PDF

**ERROR-003:** PDF Parsing Failure
- **User Experience:** Analysis status set to FAILED: `{"status": "FAILED", "error_message": "Unable to extract text from PDF. Please ensure this is a LinkedIn PDF export."}`
- **Recovery Path:** User re-exports from LinkedIn and uploads again

**ERROR-004:** LLM Call Failure (After Retries)
- **User Experience:** Analysis status set to FAILED: `{"status": "FAILED", "error_message": "Analysis service temporarily unavailable. Please try again later."}`
- **Recovery Path:** User retries later, system retried 3 times automatically

**ERROR-005:** Insufficient Coins for Additional Suggestions
- **User Experience:** API returns 402 Payment Required: `{"message": "Insufficient balance. You need 5 coins for additional suggestions. Current balance: 2 coins.", "code": "INSUFFICIENT_BALANCE"}`
- **Recovery Path:** User acquires more coins or uses free suggestions only

**ERROR-006:** Analysis Not Found
- **User Experience:** API returns 404 Not Found: `{"message": "Analysis not found", "code": "ANALYSIS_NOT_FOUND"}`
- **Recovery Path:** User navigates to analysis history

**ERROR-007:** Section Not Applicable for "Apply"
- **User Experience:** API returns 400 Bad Request: `{"message": "Suggestions for 'Experience' section cannot be directly applied to profile", "code": "SECTION_NOT_APPLICABLE"}`
- **Recovery Path:** User manually updates Experience via Profile CRUD

---

## Testing Scope

### Functional Testing

- **PDF Upload:** Valid PDF accepted, invalid files rejected, size limit enforced
- **Async Processing:** Analysis transitions PENDING -> IN_PROGRESS -> COMPLETED
- **PDF Parsing:** Sections correctly extracted from sample LinkedIn PDFs
- **LLM Scoring:** Scores returned in valid range (0-100), suggestions present per section
- **Free Suggestion:** 1 suggestion per section included in initial analysis
- **Paid Suggestions:** Coins debited, suggestions appended, auto-refund on failure
- **Apply Suggestion:** Profile fields updated for applicable sections
- **Analysis History:** Past analyses retrievable, ordered chronologically
- **Tenant Isolation:** Cross-tenant access returns 404
- **Error Handling:** Failed analyses have FAILED status, clear error messages

### User Acceptance Testing

- **Complete Flow:** Upload PDF -> wait for analysis -> view scores -> request additional suggestions -> apply suggestion to profile
- **Score Quality:** Analyze 3 different quality profiles, verify scores make intuitive sense
- **History Tracking:** Run 3 analyses over time, verify trend comparison works
- **Coin Integration:** Verify coin deduction for paid suggestions, verify refund on failure

### Edge Case Testing

- **Empty PDF:** Upload PDF with no text -> graceful handling
- **Non-LinkedIn PDF:** Upload random PDF -> low-confidence results
- **Very large LinkedIn profile:** PDF with 20+ years experience -> parsing handles correctly
- **Concurrent analyses:** Two analyses for same profile simultaneously -> both complete independently
- **LLM timeout:** Mock LLM timeout -> FAILED status after retries
- **Apply to empty profile:** No profile exists -> create profile or 404

---

## Notes

Key design decisions:

- **PDF-first approach:** LinkedIn ToS compliance by avoiding scraping; user-initiated PDF upload is the safest approach
- **Async processing:** LLM calls take 15-45 seconds, must not block HTTP threads (Principle 12)
- **Immutable snapshots:** Analysis results are never modified after completion, enabling historical comparison
- **JSONB for suggestions:** Flexible array storage allows appending paid suggestions without schema changes
- **1 free + paid model:** Balances free value demonstration with monetization via coin system
- **Section-level granularity:** Per-section scores and suggestions enable targeted improvements
- **Apply suggestion integration:** Bridges LinkedIn analysis with platform profile, increasing platform stickiness
- **Temporary PDF storage:** PDFs deleted after parsing to minimize sensitive data storage

Future enhancements:
- URL-based profile fetching (with LinkedIn ToS evaluation)
- Photo/banner analysis via vision models
- Industry benchmark comparison
- Automated re-analysis scheduling
- Analysis sharing/export
- Multi-language support for non-English profiles
- OCR for scanned PDF documents
- A/B testing different LLM prompt strategies for better suggestions

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial specification       | Claude Code |
