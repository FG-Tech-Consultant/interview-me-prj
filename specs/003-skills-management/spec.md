# Feature Specification: Skills Management - Catalog and User Skills CRUD

**Feature ID:** 003-skills-management
**Status:** Draft
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Overview

### Problem Statement

Career professionals need to maintain a comprehensive, structured inventory of their technical and domain skills with contextual metadata (years of experience, proficiency levels, last used dates). Currently, skills are scattered across LinkedIn profiles, resumes, and cover letters with inconsistent representation:
- No standardized skill taxonomy (different names for same skill: "React.js" vs "React" vs "ReactJS")
- Missing critical context (proficiency, recency, confidence)
- Difficulty linking skills to concrete experience stories
- No way to filter or search skills by category, depth, or recency

Without a structured skills management system, users cannot effectively demonstrate their expertise depth, recruiters cannot filter candidates by skill criteria, and AI systems cannot accurately match candidates to opportunities.

### Feature Summary

Implement a comprehensive skills management system with two layers: (1) a canonical **Skills Catalog** providing standardized skill definitions, categories, and tags, and (2) **User Skills** allowing users to claim skills with rich metadata (years of experience, proficiency depth 1-5, last used date, confidence level). This system enables skill-based search, supports recruiter chat context retrieval, powers resume generation, and lays groundwork for future AI-powered skill gap analysis and career recommendations.

### Target Users

- **Career Professionals (Primary User):** Senior engineers, tech leads, architects who need to maintain detailed skill inventories with proficiency levels and recency
- **Profile Owners:** Any professional building their Live Resume to demonstrate expertise breadth and depth
- **Recruiters (Indirect User):** Benefit from structured, searchable skill data when evaluating candidates via recruiter chat
- **Platform Admin:** Curate and expand the canonical skills catalog over time

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Standard REST CRUD with Spring Data JPA, conventional controller-service-repository layering, React forms with MUI components
- **Principle 3: Modern Java Standards** - Use Java 25 records for skill DTOs, Lombok for entity boilerplate, modern JPA features
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - All UserSkill entities MUST include tenant_id, automatic filtering via Hibernate filter from Feature 001, PostgreSQL with JSONB for flexible skill tags
- **Principle 6: Observability and Debugging** - Structured logging for skill CRUD operations, track skill catalog updates for admin audit
- **Principle 7: Security, Privacy, and Credential Management** - Public/private visibility for individual user skills (some skills may be private experimental knowledge), @Transactional annotations
- **Principle 8: Multi-Tenant Architecture** - UserSkill entities include tenant_id, Skills catalog is shared across tenants (no tenant_id), filtered at service layer
- **Principle 10: Full-Stack Modularity and Separation of Concerns** - Backend: `com.interviewme.skills` package (controller, service, repository), Frontend: `pages/SkillsPage.tsx`, `components/SkillSelector`, `api/skillsApi.ts`
- **Principle 11: Database Schema Evolution** - Liquibase timestamp-based migrations for skills catalog and user_skills tables

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: User Adds Skills to Profile from Catalog

**Actor:** Career professional building their Live Resume profile
**Goal:** Add 10-15 technical skills with accurate proficiency levels and experience duration
**Preconditions:** User has authenticated account, Profile exists (Feature 002), Skills catalog is populated

**Steps:**
1. User navigates to "Skills" section in profile editor
2. User clicks "Add Skill" button, sees autocomplete search field
3. User types "Java", autocomplete suggests "Java", "JavaScript", "Java EE", "Java 17+"
4. User selects "Java" from suggestions
5. System pre-fills skill name, category ("Languages"), description from catalog
6. User fills in:
   - Years of experience: 12
   - Proficiency depth: 5 (Expert - scale 1-5)
   - Last used: 2026-01 (current month)
   - Confidence: High
   - Tags: #backend, #spring-boot, #microservices
7. User marks skill as "public" (visible to recruiters)
8. System validates inputs, saves UserSkill with tenant association
9. User repeats for additional skills: Spring Boot, PostgreSQL, Kubernetes, React
10. System displays skill inventory grouped by category with visual badges

**Success Criteria:**
- All 10-15 skills added successfully with metadata
- Skills appear in profile editor grouped by category
- Autocomplete suggests relevant matches from catalog
- Validation prevents duplicate skills (same skill twice)
- Public skills immediately available for recruiter chat context

#### Scenario 2: User Updates Existing Skill Proficiency

**Actor:** Profile owner who recently gained deeper expertise in a skill
**Goal:** Update proficiency level and last used date for "Kubernetes"
**Preconditions:** User has "Kubernetes" skill already added with depth=3, last used 2024-06

**Steps:**
1. User navigates to Skills section, sees existing "Kubernetes" skill card
2. User clicks "Edit" icon on Kubernetes skill
3. System opens edit dialog with pre-filled current values
4. User updates:
   - Proficiency depth: 3 → 4 (Advanced to Expert)
   - Last used: 2024-06 → 2026-02
   - Tags: adds #helm, #istio
5. User saves changes
6. System validates (depth 1-5, last used not in future), persists updates
7. System updates `updated_at` timestamp
8. Updated skill reflected in UI with new proficiency badge (4/5 stars)

**Success Criteria:**
- Skill metadata updated without creating duplicate
- UI reflects changes immediately
- Audit trail (updated_at timestamp) recorded
- Public/private visibility setting preserved

#### Scenario 3: User Removes Obsolete Skill (Soft Delete)

**Actor:** Profile owner cleaning up outdated skills
**Goal:** Remove "Adobe Flash" skill no longer relevant to career
**Preconditions:** User has "Adobe Flash" skill in profile

**Steps:**
1. User navigates to Skills section, sees "Adobe Flash" skill card
2. User clicks "Delete" icon on skill
3. System prompts confirmation: "Remove 'Adobe Flash' from your profile? This will hide it from all exports and recruiter chat."
4. User confirms deletion
5. System performs soft delete: sets `deleted_at` timestamp
6. Skill disappears from UI and public APIs
7. System logs deletion event with tenant context

**Success Criteria:**
- Skill no longer visible in profile editor
- Skill excluded from recruiter chat context and exports
- Data remains in database (soft delete) for potential recovery
- Stories/projects linked to this skill remain intact (skill relationship archived)

#### Scenario 4: Admin Adds New Skill to Catalog

**Actor:** Platform admin expanding canonical skills catalog
**Goal:** Add "Astro.js" framework to catalog as new frontend skill
**Preconditions:** Admin authenticated with ADMIN role, "Astro.js" not in catalog

**Steps:**
1. Admin navigates to Admin → Skills Catalog page
2. Admin clicks "Add Skill to Catalog"
3. Admin fills in:
   - Name: Astro.js
   - Category: Frameworks
   - Description: Modern static site generator with component islands architecture
   - Tags: #frontend, #javascript, #static-site-generator
4. Admin marks skill as "active" (available for user selection)
5. System validates uniqueness (no duplicate "Astro.js"), saves to catalog
6. New skill immediately available in autocomplete for all users across all tenants

**Success Criteria:**
- New skill appears in catalog visible to all tenants
- Autocomplete includes "Astro.js" when users type "Astro"
- Skill categorized correctly under "Frameworks"
- Tags indexed for future advanced search

#### Scenario 5: User Searches and Filters Skills

**Actor:** Profile owner with 30+ skills, needs to find specific skill quickly
**Goal:** Find all "Cloud" category skills last used in 2024 or earlier (identify stale skills)
**Preconditions:** User has 30+ skills across multiple categories

**Steps:**
1. User navigates to Skills section
2. User applies filters:
   - Category: Cloud
   - Last used: Before 2025-01
3. System filters skill list, displays 5 skills: AWS EC2 (2023-08), GCP (2024-03), Azure (2024-11), Docker (2024-12), Terraform (2023-05)
4. User identifies stale skills (AWS EC2, Terraform) for update or removal
5. User can bulk update or remove filtered skills

**Success Criteria:**
- Filters applied correctly (category AND date range)
- Results displayed sorted by last used (oldest first)
- Filter UI intuitive with dropdowns and date pickers
- Bulk actions available on filtered results

### Edge Cases

- **Duplicate skill prevention:** User attempts to add "React" when "React.js" already exists (system suggests existing skill or allows alias)
- **Invalid proficiency values:** User enters depth=7 (valid range 1-5), system rejects with validation error
- **Future last used dates:** User enters last used 2027-01 (future), system rejects
- **Catalog skill deactivation:** Admin deactivates "Adobe Flash" in catalog, existing user skills remain but new additions blocked
- **Empty skills profile:** User with no skills added, UI shows empty state with suggestions ("Add your first skill to get started")
- **Extremely long tag lists:** User adds 50+ tags to single skill (system limits to 20 tags max)
- **Unicode in skill names:** Catalog includes "日本語" (Japanese language), system handles correctly
- **Concurrent catalog updates:** Two admins adding same skill simultaneously (optimistic locking or unique constraint)
- **Orphaned skills:** Skill catalog entry deleted while users still have UserSkills referencing it (handle gracefully with "Unknown Skill" fallback)

---

## Functional Requirements

### Core Capabilities

**REQ-001:** Skills Catalog Creation and Management
- **Description:** Platform MUST provide a canonical skills catalog with standardized skill definitions, categories, and tags. Admins MUST be able to add, edit, deactivate, and reactivate skills.
- **Acceptance Criteria:**
  - Skills catalog table with columns: id, name (unique), category, description, tags (JSONB array), is_active (boolean), created_at, updated_at
  - Categories include: Languages, Frameworks, Cloud, Databases, Messaging, Observability, Methodologies, Domains (extensible)
  - Tags stored as JSONB array: ["#backend", "#java", "#spring"]
  - Admins can CRUD skills via admin API endpoints
  - Deactivated skills not shown in autocomplete but existing UserSkills preserved
  - Skills catalog shared across all tenants (no tenant_id)

**REQ-002:** User Skills - Add Skill to Profile
- **Description:** Users MUST be able to claim skills from the catalog with metadata: years of experience, proficiency depth (1-5), last used date, confidence level, custom tags, and visibility flag
- **Acceptance Criteria:**
  - UserSkill entity includes: id, tenant_id, profile_id, skill_id (FK to catalog), years_of_experience (integer), proficiency_depth (1-5 scale), last_used_date (DATE), confidence_level (enum: LOW, MEDIUM, HIGH), tags (JSONB array), visibility (public/private), created_at, updated_at, deleted_at
  - Users select skill from autocomplete (searches catalog by name)
  - Validation: years_of_experience >= 0, proficiency_depth 1-5, last_used_date <= today
  - Duplicate prevention: Cannot add same skill_id twice to same profile (unique constraint)
  - Default visibility: private (user must explicitly make public)

**REQ-003:** User Skills - Update Skill Metadata
- **Description:** Users MUST be able to update all metadata fields for existing user skills (proficiency, years, last used, tags, visibility)
- **Acceptance Criteria:**
  - All fields editable except id, tenant_id, profile_id, skill_id, created_at
  - Validation rules same as creation
  - Updated_at timestamp automatically set
  - Changes immediately reflected in UI and public APIs

**REQ-004:** User Skills - Remove Skill (Soft Delete)
- **Description:** Users MUST be able to remove skills from their profile using soft delete (set deleted_at timestamp)
- **Acceptance Criteria:**
  - Soft delete sets deleted_at timestamp, preserves all data
  - Deleted skills excluded from UI, exports, and recruiter chat context
  - Deleted skills remain in database for audit and potential recovery
  - System logs deletion event with tenant context

**REQ-005:** Skills Catalog - Autocomplete Search
- **Description:** System MUST provide fast autocomplete search of skills catalog to help users find skills quickly
- **Acceptance Criteria:**
  - Autocomplete searches catalog by skill name (case-insensitive, prefix match)
  - Returns top 10 matches sorted by relevance (exact match first, then alphabetical)
  - Filters out deactivated skills (is_active=false)
  - Response time < 100ms for autocomplete queries
  - Frontend debounces input (300ms) to reduce API calls

**REQ-006:** User Skills - List and Filter
- **Description:** Users MUST be able to view all their skills grouped by category with filtering by category, proficiency, and recency
- **Acceptance Criteria:**
  - Skills list endpoint returns all non-deleted user skills
  - Default grouping by category (Languages, Frameworks, Cloud, etc.)
  - Filters available: category (dropdown), proficiency_depth (range 1-5), last_used (date range), visibility (public/private/all)
  - Sorting options: alphabetical, proficiency (high to low), last used (recent first), years of experience (desc)
  - Pagination support for users with 100+ skills

**REQ-007:** Skills Catalog - Admin CRUD
- **Description:** Platform admins MUST be able to create, read, update, deactivate, and reactivate skills in the canonical catalog
- **Acceptance Criteria:**
  - Admin API endpoints for catalog CRUD (requires ADMIN role)
  - Create: name (unique), category, description, tags, is_active=true
  - Update: all fields except id, created_at
  - Deactivate: set is_active=false (soft deactivation, existing UserSkills unaffected)
  - Reactivate: set is_active=true
  - No hard delete (preserve skill history)
  - Catalog changes audit logged (admin user, timestamp, action)

### User Interface Requirements

**REQ-UI-001:** Skills Page - Skill Inventory Display
- **Description:** Frontend MUST display user's skills grouped by category with visual proficiency indicators and metadata
- **Acceptance Criteria:**
  - Skills grouped by category with collapsible sections
  - Each skill card shows: name, proficiency (stars 1-5), years of experience, last used (formatted as "Jan 2026"), tags, visibility badge (public/private)
  - Color-coded proficiency: 1-2 (yellow), 3 (blue), 4-5 (green)
  - Empty state message: "No skills added yet. Add your first skill to showcase your expertise."
  - "Add Skill" button always visible

**REQ-UI-002:** Add/Edit Skill Dialog
- **Description:** Frontend MUST provide modal dialog for adding/editing skills with autocomplete and validation
- **Acceptance Criteria:**
  - Autocomplete skill name field with suggestions from catalog
  - Form fields: years of experience (number input), proficiency depth (1-5 slider or select), last used (month picker), confidence level (dropdown: Low/Medium/High), tags (multi-input chip field), visibility toggle (public/private)
  - Inline validation: years >= 0, depth 1-5, last used <= today
  - Save button disabled until valid
  - Loading state during API call
  - Success/error snackbar notifications

**REQ-UI-003:** Skills Filters and Search
- **Description:** Frontend MUST provide filter controls to narrow skill list by category, proficiency, and recency
- **Acceptance Criteria:**
  - Filter bar with: category dropdown (multi-select), proficiency range slider (1-5), last used date range picker, visibility filter (all/public/private)
  - Search input for skill name (client-side filter on loaded skills)
  - "Clear filters" button
  - Filter state persists during session (localStorage)
  - Filtered count displayed: "Showing 8 of 32 skills"

**REQ-UI-004:** Admin Skills Catalog Page
- **Description:** Frontend MUST provide admin interface for managing canonical skills catalog
- **Acceptance Criteria:**
  - Admin-only route: `/admin/skills-catalog`
  - Table view of all catalog skills with columns: name, category, tags, is_active, actions (edit/deactivate/reactivate)
  - "Add Skill to Catalog" button opens form dialog
  - Inline editing for skill name, category, description, tags
  - Deactivate/reactivate toggle with confirmation
  - Search and filter by category, active status

### Data Requirements

**REQ-DATA-001:** Skills Catalog Table Schema
- **Description:** PostgreSQL table for storing canonical skill definitions shared across all tenants
- **Acceptance Criteria:**
  - Table name: `skill`
  - Columns: id (BIGSERIAL PK), name (VARCHAR 255 UNIQUE NOT NULL), category (VARCHAR 100 NOT NULL), description (TEXT), tags (JSONB array), is_active (BOOLEAN DEFAULT true), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL)
  - Indexes: `idx_skill_name` (unique), `idx_skill_category`, `idx_skill_is_active`
  - No tenant_id (catalog shared globally)
  - Categories as VARCHAR for flexibility (no enum constraint, allows easy extension)

**REQ-DATA-002:** User Skills Table Schema
- **Description:** PostgreSQL table for storing user skill claims with metadata
- **Acceptance Criteria:**
  - Table name: `user_skill`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), profile_id (BIGINT FK NOT NULL), skill_id (BIGINT FK NOT NULL), years_of_experience (INT DEFAULT 0), proficiency_depth (INT CHECK 1-5, NOT NULL), last_used_date (DATE), confidence_level (VARCHAR 20 DEFAULT 'MEDIUM'), tags (JSONB array), visibility (VARCHAR 20 DEFAULT 'private'), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), deleted_at (TIMESTAMPTZ), version (BIGINT DEFAULT 0)
  - Indexes: `idx_user_skill_tenant_id`, `idx_user_skill_profile_id`, `idx_user_skill_skill_id`, `idx_user_skill_deleted_at`
  - Foreign keys: tenant_id → tenant(id), profile_id → profile(id), skill_id → skill(id)
  - Unique constraint: (tenant_id, profile_id, skill_id) WHERE deleted_at IS NULL (prevent duplicate active skills)
  - Check constraints: proficiency_depth BETWEEN 1 AND 5, years_of_experience >= 0

---

## Success Criteria

The feature will be considered successful when:

1. **Complete Skill Lifecycle:** Users can add, update, view, and remove skills with full metadata without errors
   - Measurement: Integration tests cover all CRUD operations with 100% success rate

2. **Skills Catalog Autocomplete Performance:** Autocomplete returns relevant suggestions in < 100ms
   - Measurement: Load test with 1000+ catalog skills, p95 latency < 100ms

3. **Duplicate Skill Prevention:** Users cannot add same skill twice to their profile
   - Measurement: Test adding duplicate skill, verify unique constraint violation returns 409 Conflict

4. **Public/Private Visibility Enforced:** Private skills excluded from public APIs and recruiter chat context
   - Measurement: Create user with mixed public/private skills, verify public API response excludes private

5. **Tenant Isolation Verified:** Users in different tenants cannot see each other's user skills
   - Measurement: Create two tenants with overlapping skills, verify cross-tenant access returns 404

6. **Skill Grouping by Category:** UI displays skills logically grouped by category (Languages, Frameworks, Cloud, etc.)
   - Measurement: Manual UAT with 20+ skills across 5 categories, verify grouping correct

7. **Admin Catalog Management:** Admins can add new skills to catalog, immediately available in autocomplete
   - Measurement: Admin adds "Deno" to catalog, user sees it in autocomplete within 1 second

---

## Key Entities

### Skill (Canonical Catalog)

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- name: Skill name (VARCHAR 255, UNIQUE, NOT NULL, e.g., "Java", "React", "Kubernetes")
- category: Skill category (VARCHAR 100, NOT NULL, e.g., "Languages", "Frameworks", "Cloud")
- description: Detailed skill description (TEXT, optional, e.g., "Enterprise object-oriented programming language")
- tags: Array of tags (JSONB, e.g., ["#backend", "#jvm", "#enterprise"])
- is_active: Availability flag (BOOLEAN, default true, false hides from autocomplete)
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)

**Relationships:**
- One-to-many with UserSkill (one catalog skill claimed by many users)

**Validation Rules:**
- name: Required, 1-255 characters, unique across catalog
- category: Required, 1-100 characters
- description: Optional, max 2000 characters
- tags: Array of strings, each 1-50 characters, max 20 tags
- is_active: Boolean, default true

### UserSkill

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, indexed)
- profile_id: Foreign key to Profile (BIGINT, NOT NULL, indexed)
- skill_id: Foreign key to Skill catalog (BIGINT, NOT NULL, indexed)
- years_of_experience: Years of practical experience (INT, default 0, >= 0)
- proficiency_depth: Skill proficiency level (INT, 1-5 scale, NOT NULL)
  - 1: Beginner (basic understanding, limited practical use)
  - 2: Intermediate (can work with guidance)
  - 3: Proficient (independent work, production experience)
  - 4: Advanced (deep expertise, mentors others)
  - 5: Expert (recognized authority, architects solutions)
- last_used_date: Most recent use date (DATE, optional, <= today)
- confidence_level: Self-assessed confidence (VARCHAR 20, enum: LOW, MEDIUM, HIGH, default MEDIUM)
- tags: Custom user tags (JSONB array, e.g., ["#production", "#scalability"])
- visibility: Public/private flag (VARCHAR 20, "public" or "private", default "private")
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- deleted_at: Soft delete timestamp (TIMESTAMPTZ, NULL)
- version: Optimistic locking version (BIGINT, default 0)

**Relationships:**
- Many-to-one with Tenant (many user skills belong to one tenant)
- Many-to-one with Profile (many user skills belong to one profile)
- Many-to-one with Skill catalog (many user skills reference one canonical skill)
- Many-to-many with Story (future feature: skills linked to STAR stories)

**Validation Rules:**
- years_of_experience: >= 0, max 70 (sanity check)
- proficiency_depth: 1-5 inclusive, required
- last_used_date: <= today (no future dates)
- confidence_level: Must be "LOW", "MEDIUM", or "HIGH"
- tags: Array of strings, each 1-50 characters, max 20 tags
- visibility: Must be "public" or "private"
- Unique constraint: (tenant_id, profile_id, skill_id) WHERE deleted_at IS NULL

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Requires authentication, tenant filtering, database infrastructure
- **Feature 002: Profile CRUD** - Requires Profile entity and profile_id foreign key

### External Dependencies

- **MUI Autocomplete:** For skill search autocomplete in frontend
- **MUI Slider/Rating:** For proficiency depth input (1-5 scale)
- **Hypersistence Utils:** For JSONB type mapping in JPA entities
- **Spring Data JPA:** For repository abstractions and query methods
- **Liquibase:** For database schema migrations

---

## Assumptions

1. Feature 001 and 002 are fully implemented (auth, tenancy, profiles exist)
2. Skills catalog is pre-populated with 100-200 common skills before user onboarding
3. Skill categories are fixed initially (Languages, Frameworks, Cloud, Databases, Messaging, Observability, Methodologies, Domains) but extensible via data
4. Proficiency depth 1-5 scale is sufficient (no need for more granular 1-10 scale)
5. Tags are free-form strings (no tag taxonomy or validation initially)
6. Last used date precision is month-level (day precision not critical)
7. Each user profile has reasonable skill count (< 100 skills per profile)
8. Catalog skills are immutable once created (name changes create new catalog entry)
9. Skill endorsements/validations by others (LinkedIn-style) are out of scope for MVP
10. Skill synonyms/aliases (e.g., "React" = "React.js") handled manually by admin (no auto-merge)

---

## Out of Scope

The following are explicitly excluded from this feature:

1. **No Skill Endorsements:** LinkedIn-style skill endorsements or validations by other users
2. **No Skill Gap Analysis:** AI-powered recommendations for skills to learn based on career goals
3. **No Skill Trends:** Charts showing skill usage trends over time
4. **No Skill Synonyms/Aliases:** Automatic merging of similar skills (e.g., "React" vs "React.js")
5. **No Skill Levels/Certifications:** Integration with certification providers (AWS, Google Cloud, etc.)
6. **No Skill Recommendations:** AI suggesting skills to add based on job history
7. **No Import from LinkedIn:** Automated skill import from LinkedIn profile
8. **No Skill Taxonomy:** Hierarchical skill relationships (e.g., "Spring Boot" is a child of "Java")
9. **No Skill Popularity Scores:** Ranking skills by demand or market trends
10. **No Collaborative Filtering:** Suggesting skills based on what similar users have
11. **No Skill Quizzes/Tests:** Validating proficiency through assessments
12. **No Skill Decay:** Automatic proficiency reduction for skills not used recently

---

## Security & Privacy Considerations

### Security Requirements

- All skills API endpoints MUST require valid JWT authentication
- Tenant filtering MUST be automatically applied to UserSkill queries (prevent cross-tenant access)
- Admin catalog endpoints MUST require ADMIN role (role-based access control)
- Input validation MUST sanitize skill names and tags to prevent XSS
- Optimistic locking (version field) prevents concurrent update conflicts
- Audit fields (created_at, updated_at) MUST be system-managed

### Privacy Requirements

- Default visibility for new user skills is "private"
- Public/private flags MUST be respected in all API responses (filter at service layer)
- Private skills NEVER included in recruiter chat context or public profile pages
- Deleted skills (deleted_at not null) MUST be excluded from all user-facing queries
- Skills catalog is public (no privacy concerns, shared globally)
- User skill metadata (proficiency, years, confidence) respects visibility flag

---

## Performance Expectations

- **Skills Catalog Autocomplete:** p95 latency < 100ms (indexed name column)
- **User Skills List:** p95 latency < 200ms for 100 skills with category grouping
- **Add/Update User Skill:** p95 latency < 300ms
- **Skills Catalog Admin CRUD:** p95 latency < 200ms
- **Database Queries:** Use indexes on tenant_id, profile_id, skill_id, deleted_at, name for efficient filtering
- **Concurrent Requests:** Support 20 concurrent skill operations per tenant without degradation
- **Memory Usage:** Skills catalog cached in application memory (read-mostly, 1000+ skills < 1MB)

---

## Error Handling

### Error Scenarios

**ERROR-001:** Duplicate Skill (User Attempts to Add Same Skill Twice)
- **User Experience:** API returns 409 Conflict with JSON error: `{"field": "skill_id", "message": "You already have 'Java' in your profile", "code": "DUPLICATE_SKILL"}`
- **Recovery Path:** User edits existing skill instead of adding duplicate

**ERROR-002:** Invalid Proficiency Depth (Out of Range)
- **User Experience:** API returns 400 Bad Request: `{"field": "proficiency_depth", "message": "Proficiency must be between 1 and 5", "code": "INVALID_PROFICIENCY"}`
- **Recovery Path:** User corrects proficiency value in form

**ERROR-003:** Future Last Used Date
- **User Experience:** API returns 400 Bad Request: `{"field": "last_used_date", "message": "Last used date cannot be in the future", "code": "INVALID_DATE"}`
- **Recovery Path:** User selects current or past date

**ERROR-004:** Skill Not Found in Catalog
- **User Experience:** API returns 404 Not Found: `{"message": "Skill with ID 9999 not found in catalog", "code": "SKILL_NOT_FOUND"}`
- **Recovery Path:** Admin adds missing skill to catalog, user retries

**ERROR-005:** Deactivated Skill Addition Attempt
- **User Experience:** API returns 400 Bad Request: `{"message": "Skill 'Adobe Flash' is no longer available", "code": "SKILL_DEACTIVATED"}`
- **Recovery Path:** User selects different skill or contacts admin to reactivate

**ERROR-006:** Catalog Skill Duplicate Name (Admin)
- **User Experience:** Admin API returns 409 Conflict: `{"field": "name", "message": "Skill 'React' already exists in catalog", "code": "DUPLICATE_CATALOG_SKILL"}`
- **Recovery Path:** Admin uses existing skill or creates with different name (e.g., "React 18")

---

## Testing Scope

### Functional Testing

- **Skills Catalog CRUD:** Test admin can create, read, update, deactivate, reactivate catalog skills
- **User Skills CRUD:** Test create, read, update, soft delete for user skills
- **Tenant Filtering:** Verify users only see their tenant's user skills
- **Public/Private Visibility:** Test API responses filter private skills correctly
- **Soft Delete:** Verify deleted_at timestamp prevents skills from appearing in queries
- **Duplicate Prevention:** Test unique constraint prevents same skill twice
- **Proficiency Validation:** Test proficiency depth 1-5 enforced, out-of-range rejected
- **Autocomplete:** Test autocomplete returns top 10 matches, filters deactivated skills
- **Category Grouping:** Test skills grouped correctly by category in list API

### User Acceptance Testing

- **Complete Skill Profile:** User adds 15 skills across 5 categories, verify all saved correctly
- **Edit Skill Proficiency:** Update existing skill from depth 3 to 5, verify changes persist
- **Remove Obsolete Skill:** Soft delete skill, verify it disappears from UI
- **Public Skills in Recruiter Chat:** Mark 5 skills public, verify they appear in chat context (future integration test)
- **Admin Catalog Expansion:** Admin adds new skill, verify it appears in autocomplete immediately

### Edge Case Testing

- **Empty Skills Profile:** User with no skills sees helpful empty state
- **Duplicate Skill Attempt:** Try adding "Java" twice, verify 409 Conflict error
- **Invalid Proficiency:** Enter depth=7, verify validation error
- **Future Last Used:** Enter last used 2027-01, verify validation error
- **Deactivated Skill:** Admin deactivates skill, existing user skills remain but new additions blocked
- **Unicode Skill Names:** Add skill "日本語", verify correct storage and retrieval
- **Very Long Tag Lists:** Add 30 tags, verify only 20 saved (limit enforced)
- **Orphaned Skill Reference:** Delete catalog skill with existing user skills, verify graceful handling

---

## Notes

This feature establishes the skills layer critical for recruiter chat context, resume generation, and future AI-powered features. Key design decisions:

- **Two-tier design:** Canonical catalog (shared) + user skills (tenant-isolated) provides standardization while allowing personalization
- **JSONB for tags:** Flexible tagging without rigid schema constraints, enables future advanced search
- **Soft delete:** Preserves skill history and enables potential "undo" or skill resurrection
- **Proficiency 1-5 scale:** Simple enough for quick input, detailed enough for meaningful differentiation
- **Public/private at skill level:** Granular control (some skills may be experimental private knowledge)
- **Shared catalog:** All tenants benefit from expanding catalog, reduces admin overhead
- **Optimistic locking:** Prevents lost updates without complex database locking

Future enhancements could include:
- Skill gap analysis (compare profile to job descriptions)
- AI-suggested skills based on job history
- Skill synonyms/aliases handling
- Hierarchical skill taxonomy (e.g., Spring Boot → Java)
- Skill endorsements from colleagues
- Integration with certification providers (AWS, Google Cloud)
- Skill decay over time (proficiency reduction if not used)
- Import skills from LinkedIn profile

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial specification       | Claude Code |
