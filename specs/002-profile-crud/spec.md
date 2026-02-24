# Feature Specification: Profile CRUD (Timeline, Jobs, Education)

**Feature ID:** 002-profile-crud
**Status:** Draft
**Created:** 2026-02-23
**Last Updated:** 2026-02-23
**Version:** 1.0.0

---

## Overview

### Problem Statement

Career professionals need a single source of truth for their professional timeline that can power multiple outputs (resumes, portfolios, recruiter interactions). Currently, maintaining separate versions of career history across LinkedIn, PDF resumes, and portfolio sites leads to:
- Inconsistent information across platforms
- Manual effort to update multiple locations
- Lost detail when condensing for different formats
- Difficulty answering recruiter questions with concrete examples

Without a structured, comprehensive profile management system, users cannot effectively maintain their career data or generate tailored outputs for different audiences.

### Feature Summary

Implement a comprehensive profile management system that allows users to create, read, update, and delete their professional timeline including personal profile information, job experiences, and education history. This system serves as the foundational data layer for all career-related features (exports, recruiter chat, public profiles) and supports tenant isolation, public/private visibility controls, and rich metadata capture for impressive career storytelling.

### Target Users

- **Career Professionals (Primary User):** Senior engineers, tech leads, architects who need to maintain detailed career history with metrics and achievements
- **Profile Owners:** Any professional maintaining their Live Resume profile for recruiter interactions and document generation
- **Platform Admin:** System administrators managing tenant data and troubleshooting profile issues

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Standard REST CRUD pattern with Spring Data JPA repositories, conventional controller-service-repository layering, no complex abstractions
- **Principle 3: Modern Java Standards** - Use Java 25 records for DTOs, optional virtual threads for future async operations, modern JPA features
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - All profile entities MUST include tenant_id with automatic filtering, PostgreSQL with JSONB for flexible metadata storage
- **Principle 6: Observability and Debugging** - Structured logging for all CRUD operations with tenant context, validation errors logged for troubleshooting
- **Principle 7: Security, Privacy, and Credential Management** - Public/private visibility flags at field and entity level, JWT-based tenant authentication, @Transactional annotations for data integrity
- **Principle 8: Multi-Tenant Architecture** - All entities (Profile, JobExperience, Education) include tenant_id, filtered automatically via Hibernate filter from Feature 001
- **Principle 10: Full-Stack Modularity and Separation of Concerns** - Clear separation: controllers (HTTP layer), services (business logic), repositories (data access), DTOs for API contracts
- **Principle 11: Database Schema Evolution** - Liquibase timestamp-based migrations for all profile-related tables, includes rollback support

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: New User Creates Profile for First Time

**Actor:** Career professional who just registered on the platform
**Goal:** Create a complete professional profile with timeline, jobs, and education
**Preconditions:** User is authenticated with valid JWT token, tenant is created

**Steps:**
1. User accesses profile editor page
2. User fills in personal information: name, headline, summary, location, languages, professional links (LinkedIn, GitHub)
3. User adds career preferences: target roles, domains, geographic preferences, remote/relocation willingness
4. User adds first job experience: company, role, dates, location, responsibilities, achievements, metrics (TPS, volume)
5. User adds education: degree, institution, dates, certifications
6. System validates all inputs (required fields, date consistency, email/URL formats)
7. System saves profile data with tenant_id association
8. User sees confirmation message with option to continue editing or view public preview

**Success Criteria:**
- Profile is created with all required fields validated
- All data is tenant-isolated (only accessible by authenticated user in same tenant)
- Public/private flags default to private for all fields
- Profile appears in user's dashboard with "70% complete" progress indicator

#### Scenario 2: User Updates Existing Job Experience

**Actor:** Profile owner maintaining their career timeline
**Goal:** Update a job experience with new achievements and metrics
**Preconditions:** User has existing profile with at least one job experience

**Steps:**
1. User navigates to profile editor and selects specific job experience
2. User edits achievements section, adding quantitative metrics (e.g., "Reduced latency from 500ms to 50ms")
3. User updates employment end date (role recently ended)
4. User marks specific achievements as "public" for recruiter visibility
5. System validates date consistency (start < end, not in future)
6. System saves updated job experience with audit trail (last_updated timestamp)
7. User sees updated profile with changes reflected

**Success Criteria:**
- Only the specific job experience is updated (no unintended changes)
- Public/private visibility controls persist correctly
- Audit timestamps updated (updated_at field)
- Changes immediately reflected in public profile if marked public

#### Scenario 3: User Adds Multiple Education Entries

**Actor:** Profile owner with multiple degrees and certifications
**Goal:** Add complete education history including certifications
**Preconditions:** User has existing profile

**Steps:**
1. User accesses education section in profile editor
2. User adds Bachelor's degree: institution, graduation date, field of study
3. User adds Master's degree: institution, graduation date, field of study, GPA (optional)
4. User adds certifications: AWS Solutions Architect, dates, certification ID
5. System orders education entries chronologically (most recent first)
6. User marks Bachelor's and certifications as "public", Master's as "private"
7. System saves all education entries with tenant isolation

**Success Criteria:**
- Multiple education entries created under same profile
- Chronological ordering maintained
- Public/private visibility respected in exports and public profile
- Each entry independently editable and deletable

#### Scenario 4: User Deletes Job Experience (Soft Delete)

**Actor:** Profile owner removing irrelevant job history
**Goal:** Remove early-career job that's no longer relevant for current role targeting
**Preconditions:** User has multiple job experiences

**Steps:**
1. User selects job experience to delete
2. System prompts confirmation: "Are you sure? This will hide this job from all exports and public profiles."
3. User confirms deletion
4. System performs soft delete: sets `deleted_at` timestamp, keeps data for audit
5. Job experience no longer appears in profile editor or exports
6. System logs deletion event for audit trail

**Success Criteria:**
- Job experience not visible in UI after deletion
- Data remains in database (soft delete) for recovery if needed
- Associated data (projects, stories) also soft-deleted or marked orphaned
- Deletion event logged with tenant and user context

### Edge Cases

- **Date validation failures:** End date before start date, future dates in past employment
- **Concurrent updates:** Two browser tabs editing same profile simultaneously (optimistic locking with version field)
- **Empty profile:** User with no job experiences or education (should allow profile creation with only basic info)
- **Extremely long text:** Job descriptions exceeding reasonable limits (10,000 characters)
- **Special characters:** Unicode characters in names, emojis in job titles
- **Orphaned data:** Job experience deleted but referenced by stories (handle gracefully)
- **Timezone handling:** Users in different timezones creating profiles (use UTC for storage, display in user's timezone)

---

## Functional Requirements

### Core Capabilities

**REQ-001:** Profile Creation
- **Description:** Users MUST be able to create a new professional profile with personal information, career preferences, and contact details
- **Acceptance Criteria:**
  - Profile includes: full name (required), headline (required), summary (optional), location, languages (array), professional links (LinkedIn, GitHub, portfolio)
  - Career preferences include: target roles (array), domains (array), target geographies (array), remote willingness (boolean), relocation willingness (boolean)
  - All fields validate format (emails, URLs) and length constraints
  - Profile is tenant-isolated (tenant_id foreign key)
  - Each user can have exactly ONE profile per tenant
  - Default visibility for all fields is "private"

**REQ-002:** Profile Update
- **Description:** Users MUST be able to update any field in their existing profile with validation
- **Acceptance Criteria:**
  - All profile fields are editable except id, tenant_id, created_at
  - System validates updated data (same rules as creation)
  - Optimistic locking prevents concurrent update conflicts (version field)
  - Update timestamp (updated_at) automatically set on save
  - Changes immediately reflected in all dependent views

**REQ-003:** Job Experience Management
- **Description:** Users MUST be able to create, read, update, and delete job experiences linked to their profile
- **Acceptance Criteria:**
  - Job experience includes: company name (required), role/title (required), start date (required), end date (optional for current roles), location, employment type (full-time, contract, freelance), responsibilities (text), achievements (text), metrics (JSONB for flexible structure)
  - Each job experience has visibility flag (public/private)
  - Jobs are ordered chronologically (most recent first)
  - Users can mark current job with "current" flag (no end date)
  - Deletion is soft delete (deleted_at timestamp)
  - Each job experience belongs to exactly one profile and one tenant

**REQ-004:** Education History Management
- **Description:** Users MUST be able to create, read, update, and delete education entries linked to their profile
- **Acceptance Criteria:**
  - Education entry includes: degree/certification name (required), institution (required), start date (optional), end date (required), field of study (optional), GPA (optional), notes (text)
  - Each education entry has visibility flag (public/private)
  - Education entries ordered chronologically (most recent first)
  - Deletion is soft delete (deleted_at timestamp)
  - Each education entry belongs to exactly one profile and one tenant

**REQ-005:** Public/Private Visibility Control
- **Description:** Users MUST be able to control which profile data is public (visible to recruiters and in exports) vs. private (only visible to owner)
- **Acceptance Criteria:**
  - Profile entity has `default_visibility` field (public/private)
  - Each field in Profile, JobExperience, Education can override default visibility
  - System provides "Make All Public" and "Make All Private" bulk actions
  - Public data ONLY appears in recruiter chat context and public profile pages
  - Private data NEVER exposed in public APIs or recruiter sessions
  - Visibility changes take effect immediately

**REQ-006:** Profile Retrieval
- **Description:** Users MUST be able to retrieve their complete profile including all job experiences and education history
- **Acceptance Criteria:**
  - Single API endpoint returns profile + job experiences + education in one response
  - Response includes only non-deleted entries (soft delete filter)
  - Response ordered chronologically (jobs and education separately)
  - Tenant filter applied automatically (user only sees own profile)
  - Read-only transaction for performance (`@Transactional(readOnly = true)`)

**REQ-007:** Validation and Error Handling
- **Description:** System MUST validate all user inputs and return clear error messages for validation failures
- **Acceptance Criteria:**
  - Required fields: 400 Bad Request with field-specific errors
  - Date validation: Start date must be before end date, no future dates for past employment
  - Format validation: URLs must be valid HTTP(S), emails must be valid format
  - Length validation: Text fields have maximum lengths (name: 255, summary: 5000, achievements: 10000)
  - Unique constraints: Each user has exactly one profile per tenant
  - Error responses include: HTTP status code, error message, field name, validation rule violated

### User Interface Requirements

**REQ-UI-001:** Profile Editor Page
- **Description:** Frontend MUST provide an intuitive profile editor with tabbed or sectioned layout for profile, jobs, and education
- **Acceptance Criteria:**
  - Page organized in sections: "Personal Info", "Career Preferences", "Work Experience", "Education"
  - Forms use MUI TextField, Select, DatePicker, and Checkbox components
  - Inline validation errors displayed as user types
  - Save button with loading state during API call
  - Success/error notifications using MUI Snackbar
  - "Public/Private" toggle for each field with visual indicators (lock icon for private)

**REQ-UI-002:** Job Experience List and Edit Forms
- **Description:** Frontend MUST display job experiences as cards with inline editing
- **Acceptance Criteria:**
  - Job experiences displayed as timeline cards (most recent first)
  - Each card shows: company, role, dates, visibility badge (public/private)
  - "Add Job" button opens form dialog
  - Edit icon on each card opens edit dialog with pre-filled data
  - Delete icon with confirmation dialog
  - Current job indicated with "Current" badge

**REQ-UI-003:** Education List and Edit Forms
- **Description:** Frontend MUST display education entries as list with inline editing
- **Acceptance Criteria:**
  - Education entries displayed as list items (most recent first)
  - Each item shows: degree, institution, dates, visibility badge
  - "Add Education" button opens form dialog
  - Edit and delete actions similar to job experiences

**REQ-UI-004:** Profile Completeness Indicator
- **Description:** Frontend MUST show a profile completeness percentage to encourage users to fill in all sections
- **Acceptance Criteria:**
  - Percentage calculated based on: profile info (30%), at least one job (30%), at least one education (20%), public visibility set (20%)
  - Displayed as progress bar with percentage
  - Suggestions shown for incomplete sections (e.g., "Add your first job experience")

### Data Requirements

**REQ-DATA-001:** Profile Table Schema
- **Description:** PostgreSQL table for storing user profile information
- **Acceptance Criteria:**
  - Table name: `profile`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), user_id (BIGINT FK NOT NULL UNIQUE), full_name (VARCHAR 255 NOT NULL), headline (VARCHAR 255 NOT NULL), summary (TEXT), location (VARCHAR 255), languages (JSONB array), professional_links (JSONB object), career_preferences (JSONB object), default_visibility (VARCHAR 20 DEFAULT 'private'), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), version (BIGINT DEFAULT 0)
  - Indexes: `idx_profile_tenant_id`, `idx_profile_user_id`
  - Foreign keys: tenant_id → tenant(id), user_id → user(id)
  - Unique constraint: (tenant_id, user_id)

**REQ-DATA-002:** Job Experience Table Schema
- **Description:** PostgreSQL table for storing job experiences
- **Acceptance Criteria:**
  - Table name: `job_experience`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), profile_id (BIGINT FK NOT NULL), company (VARCHAR 255 NOT NULL), role (VARCHAR 255 NOT NULL), start_date (DATE NOT NULL), end_date (DATE), is_current (BOOLEAN DEFAULT false), location (VARCHAR 255), employment_type (VARCHAR 50), responsibilities (TEXT), achievements (TEXT), metrics (JSONB), visibility (VARCHAR 20 DEFAULT 'private'), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), deleted_at (TIMESTAMPTZ), version (BIGINT DEFAULT 0)
  - Indexes: `idx_job_experience_tenant_id`, `idx_job_experience_profile_id`, `idx_job_experience_deleted_at`
  - Foreign keys: tenant_id → tenant(id), profile_id → profile(id)
  - Check constraint: end_date IS NULL OR end_date >= start_date

**REQ-DATA-003:** Education Table Schema
- **Description:** PostgreSQL table for storing education history
- **Acceptance Criteria:**
  - Table name: `education`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), profile_id (BIGINT FK NOT NULL), degree (VARCHAR 255 NOT NULL), institution (VARCHAR 255 NOT NULL), start_date (DATE), end_date (DATE NOT NULL), field_of_study (VARCHAR 255), gpa (VARCHAR 50), notes (TEXT), visibility (VARCHAR 20 DEFAULT 'private'), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), deleted_at (TIMESTAMPTZ), version (BIGINT DEFAULT 0)
  - Indexes: `idx_education_tenant_id`, `idx_education_profile_id`, `idx_education_deleted_at`
  - Foreign keys: tenant_id → tenant(id), profile_id → profile(id)
  - Check constraint: end_date >= start_date (if start_date not null)

---

## Success Criteria

The feature will be considered successful when:

1. **Complete CRUD Operations:** Users can create, read, update, and delete profile, job experiences, and education entries without errors
   - Measurement: Integration tests cover all CRUD endpoints with 100% success rate

2. **Tenant Isolation Verified:** Users in different tenants cannot access each other's profile data
   - Measurement: Integration test creates two tenants, verifies cross-tenant access returns 404 Not Found

3. **Public/Private Visibility Enforced:** Private fields are never exposed in public API responses
   - Measurement: Test with mixed public/private data, verify public API response excludes private fields

4. **Data Validation Prevents Invalid Input:** All validation rules enforce data integrity (dates, formats, required fields)
   - Measurement: Unit tests for 20+ validation scenarios all return expected 400 Bad Request errors

5. **Profile Completeness Indicator Accurate:** UI correctly calculates and displays profile completion percentage
   - Measurement: Manual UAT with 5 different profile states (0%, 30%, 50%, 80%, 100%)

6. **Soft Delete Preserves Data:** Deleted job experiences and education remain in database but hidden from UI
   - Measurement: Database query after deletion shows `deleted_at` timestamp set, data still present

7. **Concurrent Update Handling:** Optimistic locking prevents lost updates when two users edit simultaneously
   - Measurement: Concurrent update test with two threads, second update returns 409 Conflict

---

## Key Entities

### Profile

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- user_id: Foreign key to User (BIGINT, NOT NULL, UNIQUE per tenant)
- full_name: User's full name (VARCHAR 255, NOT NULL)
- headline: Professional headline (VARCHAR 255, NOT NULL, e.g., "Senior Software Engineer | Fintech | Java/Spring")
- summary: Professional summary/bio (TEXT, optional, max 5000 chars)
- location: Current location (VARCHAR 255, optional)
- languages: Array of languages (JSONB, e.g., ["English", "Portuguese", "Spanish"])
- professional_links: Map of link types to URLs (JSONB, e.g., {"linkedin": "...", "github": "...", "portfolio": "..."})
- career_preferences: Map of preferences (JSONB, e.g., {"target_roles": ["Tech Lead", "Architect"], "domains": ["Fintech"], "remote": true})
- default_visibility: Default visibility for all fields (VARCHAR 20, "public" or "private", default "private")
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- version: Optimistic locking version (BIGINT, default 0)

**Relationships:**
- Many-to-one with Tenant (many profiles belong to one tenant)
- One-to-one with User (each user has one profile per tenant)
- One-to-many with JobExperience (one profile has many job experiences)
- One-to-many with Education (one profile has many education entries)

**Validation Rules:**
- full_name: Required, 1-255 characters
- headline: Required, 1-255 characters
- summary: Optional, max 5000 characters
- professional_links: URLs must be valid HTTP(S) format
- languages: Array of strings, each 1-50 characters
- default_visibility: Must be "public" or "private"

### JobExperience

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- profile_id: Foreign key to Profile (BIGINT, NOT NULL, indexed)
- company: Company name (VARCHAR 255, NOT NULL)
- role: Job title/role (VARCHAR 255, NOT NULL)
- start_date: Employment start date (DATE, NOT NULL)
- end_date: Employment end date (DATE, optional for current roles)
- is_current: Flag for current employment (BOOLEAN, default false)
- location: Job location (VARCHAR 255, optional)
- employment_type: Type of employment (VARCHAR 50, e.g., "full-time", "contract", "freelance")
- responsibilities: Job responsibilities (TEXT, optional, max 10000 chars)
- achievements: Key achievements (TEXT, optional, max 10000 chars)
- metrics: Flexible metrics structure (JSONB, e.g., {"tps": 50000, "users": "10M", "latency_p95": "50ms"})
- visibility: Visibility setting (VARCHAR 20, "public" or "private", default "private")
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- deleted_at: Soft delete timestamp (TIMESTAMPTZ, NULL)
- version: Optimistic locking version (BIGINT, default 0)

**Relationships:**
- Many-to-one with Tenant (many job experiences belong to one tenant)
- Many-to-one with Profile (many job experiences belong to one profile)
- One-to-many with ExperienceProject (future feature, one job has many projects)

**Validation Rules:**
- company: Required, 1-255 characters
- role: Required, 1-255 characters
- start_date: Required, must not be in future
- end_date: Optional, must be >= start_date if present
- is_current: If true, end_date must be NULL
- employment_type: Must be one of: "full-time", "part-time", "contract", "freelance", "internship"
- responsibilities: Optional, max 10000 characters
- achievements: Optional, max 10000 characters
- visibility: Must be "public" or "private"

### Education

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- profile_id: Foreign key to Profile (BIGINT, NOT NULL, indexed)
- degree: Degree or certification name (VARCHAR 255, NOT NULL, e.g., "Bachelor of Science in Computer Science")
- institution: School or organization name (VARCHAR 255, NOT NULL)
- start_date: Start date (DATE, optional)
- end_date: Graduation/completion date (DATE, NOT NULL)
- field_of_study: Major or focus area (VARCHAR 255, optional)
- gpa: GPA or grade (VARCHAR 50, optional, e.g., "3.8/4.0")
- notes: Additional notes or honors (TEXT, optional, max 2000 chars)
- visibility: Visibility setting (VARCHAR 20, "public" or "private", default "private")
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- deleted_at: Soft delete timestamp (TIMESTAMPTZ, NULL)
- version: Optimistic locking version (BIGINT, default 0)

**Relationships:**
- Many-to-one with Tenant (many education entries belong to one tenant)
- Many-to-one with Profile (many education entries belong to one profile)

**Validation Rules:**
- degree: Required, 1-255 characters
- institution: Required, 1-255 characters
- start_date: Optional, if present must be < end_date
- end_date: Required, must not be in future
- field_of_study: Optional, 1-255 characters
- gpa: Optional, 1-50 characters
- notes: Optional, max 2000 characters
- visibility: Must be "public" or "private"

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Requires authentication, tenant filtering, database infrastructure, Spring Boot backend, React frontend

### External Dependencies

- **MUI DatePicker:** For date selection in frontend forms
- **Hypersistence Utils:** For JSONB type mapping in JPA entities
- **Spring Data JPA:** For repository abstractions and query methods
- **Liquibase:** For database schema migrations

---

## Assumptions

1. Feature 001 (Project Base Structure) is fully implemented with working authentication and tenant filtering
2. Users have basic understanding of career timeline concepts (jobs, education)
3. Profile completion is not mandatory - users can create partial profiles
4. Soft delete is sufficient (no hard delete/GDPR purge in MVP)
5. JSONB fields (metrics, preferences, links) have flexible structure - no strict schema validation initially
6. Date fields use ISO 8601 format in API (YYYY-MM-DD)
7. All timestamps stored in UTC in database, displayed in user's local timezone in UI
8. Each user has exactly one profile per tenant (no multi-profile support)
9. Profile data size is reasonable (< 100 job experiences, < 50 education entries per profile)
10. Concurrent editing by same user in multiple tabs is rare (optimistic locking handles this)

---

## Out of Scope

The following are explicitly excluded from this feature:

1. **No Skills Management:** Skills catalog and user skills covered in separate feature (003)
2. **No Projects/Stories:** Experience projects and STAR stories covered in separate feature (004)
3. **No Public Profile Page:** Public-facing profile page covered in separate feature (006)
4. **No Document Exports:** Resume/deck generation covered in separate feature (008)
5. **No File Uploads:** No profile photos, resumes, or document attachments in MVP
6. **No Recommendations:** LinkedIn-style recommendations or endorsements
7. **No Activity Feed:** No timeline of profile changes or updates
8. **No Import from LinkedIn:** No automated import from external sources
9. **No Multi-Language Support:** All UI text in English only
10. **No Real-Time Collaboration:** No multi-user editing or live presence indicators
11. **No Versioning/History:** No ability to view previous versions of profile data
12. **No GDPR Purge:** Hard delete for compliance covered in future feature

---

## Security & Privacy Considerations

### Security Requirements

- All profile API endpoints MUST require valid JWT authentication
- Tenant filtering MUST be automatically applied to all queries (prevent cross-tenant access)
- Optimistic locking (version field) prevents concurrent update conflicts
- Input validation MUST sanitize HTML/XSS in text fields (Spring validation)
- Soft delete prevents accidental permanent data loss
- Audit fields (created_at, updated_at) MUST be system-managed (not user-provided)

### Privacy Requirements

- Default visibility for all new fields is "private"
- Public/private flags MUST be respected in all API responses (filter at service layer)
- Private profile data NEVER included in public APIs or recruiter chat context
- Deleted entries (deleted_at not null) MUST be excluded from all user-facing queries
- Profile data export (future feature) MUST respect visibility flags
- Users can make entire profile private with single toggle

---

## Performance Expectations

- **Profile Retrieval:** p95 latency < 200ms for full profile (including all jobs and education)
- **Profile Update:** p95 latency < 300ms for any update operation
- **List Jobs/Education:** p95 latency < 150ms (should be fast with proper indexing on profile_id and tenant_id)
- **Database Queries:** Use indexes on tenant_id, profile_id, deleted_at for efficient filtering
- **Concurrent Requests:** Support 10 concurrent profile updates per tenant without performance degradation
- **Memory Usage:** Profile entities use pagination for large datasets (limit 100 jobs/education per page)

---

## Error Handling

### Error Scenarios

**ERROR-001:** Validation Failure (Invalid Dates)
- **User Experience:** API returns 400 Bad Request with JSON error: `{"field": "end_date", "message": "End date must be after start date", "code": "INVALID_DATE_RANGE"}`
- **Recovery Path:** User corrects dates in form and resubmits

**ERROR-002:** Concurrent Update Conflict
- **User Experience:** API returns 409 Conflict with message: "Profile was updated by another session. Please refresh and try again."
- **Recovery Path:** UI automatically refreshes profile data, user reapplies changes

**ERROR-003:** Profile Not Found
- **User Experience:** API returns 404 Not Found when user tries to access non-existent profile
- **Recovery Path:** UI redirects to "Create Profile" page if profile doesn't exist

**ERROR-004:** Tenant Isolation Violation (Attempted Cross-Tenant Access)
- **User Experience:** API returns 404 Not Found (not 403 to avoid leaking tenant information)
- **Recovery Path:** Security event logged, user sees generic "not found" message

**ERROR-005:** Exceeding Text Length Limits
- **User Experience:** API returns 400 Bad Request with field-specific error: `{"field": "achievements", "message": "Text exceeds maximum length of 10000 characters"}`
- **Recovery Path:** UI shows character counter, user reduces text length

---

## Testing Scope

### Functional Testing

- **CRUD Operations:** Test create, read, update, delete for Profile, JobExperience, Education
- **Tenant Filtering:** Verify users can only access own tenant's data
- **Public/Private Visibility:** Test API responses filter private data correctly
- **Soft Delete:** Verify deleted_at timestamp prevents data from appearing in queries
- **Date Validation:** Test all date validation rules (start < end, no future dates, etc.)
- **JSONB Fields:** Test storing and retrieving complex JSON structures in metrics, preferences, links
- **Optimistic Locking:** Test concurrent updates trigger version conflict

### User Acceptance Testing

- **Complete Profile Creation:** Register new user, create profile with 3 jobs and 2 education entries, verify all data saved correctly
- **Edit Existing Data:** Update job achievements, change dates, verify changes persist
- **Visibility Toggle:** Mark some jobs public, others private, verify public API respects flags
- **Delete Job:** Soft delete job experience, verify it disappears from UI but remains in database

### Edge Case Testing

- **Empty Profile:** User with no jobs or education can still access profile
- **Very Long Text:** Test 10,000 character achievements field
- **Special Characters:** Unicode names, emojis in job titles
- **Concurrent Editing:** Two tabs editing same profile simultaneously
- **Invalid Date Ranges:** End date before start date, future dates for past jobs
- **Orphaned Data:** Delete profile, verify cascading behavior for jobs/education

---

## Notes

This feature establishes the core data model for the entire career platform. Key design decisions:

- **JSONB for flexibility:** Metrics, preferences, and links use JSONB to avoid rigid schema constraints
- **Soft delete:** Preserves data integrity and enables potential "undo" feature in future
- **Optimistic locking:** Prevents lost updates without complex database locking
- **Public/private at entity level:** Each job/education has its own visibility flag for granular control
- **Tenant isolation:** All queries automatically filtered by tenant_id via Hibernate filter from Feature 001
- **One profile per user:** Simplifies data model, avoids multi-profile complexity in MVP

Future enhancements could include:
- Profile versioning (history of changes)
- Import from LinkedIn PDF
- AI-suggested improvements to job descriptions
- Duplicate detection (same company, overlapping dates)
- Profile completeness recommendations

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-23 | Initial specification       | Claude Code |
