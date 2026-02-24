# Feature Specification: Live Public Profile Page

**Feature ID:** 005-live-public-profile
**Status:** Draft
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Overview

### Problem Statement

Career professionals maintain detailed profiles with jobs, skills, projects, and stories inside the Interview Me platform, but this content is only accessible behind authentication. There is no public-facing representation of their career that can be shared with recruiters, hiring managers, or colleagues. Without a public profile page:
- Professionals cannot share a single URL that represents their full career portfolio
- Recruiters must rely on outdated PDF resumes or incomplete LinkedIn profiles
- There is no landing page for future embedded recruiter chat (Feature 006)
- The platform's core value proposition -- "always-up-to-date live resume" -- is unrealized
- SEO discoverability is impossible; search engines cannot index private content

The public profile page is the primary "always-free" surface of the platform and the key driver for recruiter engagement and organic user acquisition.

### Feature Summary

Implement an always-free, SEO-friendly public profile page that renders a user's public career data as a professional portfolio website. The page is accessible without authentication at a clean URL (e.g., `/p/{slug}`), displays only content explicitly marked as "public" visibility, and is designed to support a future embedded recruiter chatbox. The backend aggregates public data from Profile, JobExperience, Education, UserSkill, ExperienceProject, and Story entities into a single optimized API response. The frontend renders a responsive, professional layout with sections for summary, skills, work timeline, projects, and stories.

### Target Users

- **Recruiters / Hiring Managers (Primary Viewer):** Need fast, self-service access to a candidate's skills, experience, projects, and stories without authentication
- **Career Professionals (Profile Owner):** Want a shareable URL representing their always-up-to-date career portfolio
- **Search Engines (Bot):** Crawl and index public profiles for organic discoverability
- **Colleagues / Network:** View a professional's career summary via shared link

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Single public API endpoint aggregating all public data, one React page component, standard Spring Boot patterns. No complex access control beyond "is it public?"
- **Principle 3: Modern Java Standards** - Java 25 records for the aggregate public profile DTO, virtual threads for parallel data fetching if needed
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - Public endpoint filters by slug/username to resolve tenant, then returns ONLY public-visibility content. Private content NEVER included. Tenant isolation maintained even on unauthenticated endpoints.
- **Principle 6: Observability and Debugging** - Structured logging for public profile views (slug, IP, user-agent), metrics for profile view counts
- **Principle 7: Security, Privacy, and Credential Management** - No authentication required for viewing. Public/private visibility strictly enforced: the backend MUST filter at query level, not at serialization level. Private fields NEVER leave the database for public endpoints.
- **Principle 8: Multi-Tenant Architecture** - Profile resolved by slug; tenant_id derived from the profile's tenant association. No cross-tenant data leakage.
- **Principle 9: Freemium Model** - Public profile page is always-free, unlimited, no coins required. This is the platform's primary free-tier offering.
- **Principle 10: Full-Stack Modularity** - Backend: `com.interviewme.publicprofile` package (controller, service, dto). Frontend: `pages/PublicProfilePage.tsx` with sub-components for each section.
- **Principle 11: Database Schema Evolution** - Liquibase migration to add `slug` column to `profile` table (unique, URL-safe identifier)

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: Recruiter Views Public Profile via Shared Link

**Actor:** Recruiter who received a profile link from a candidate
**Goal:** Quickly assess candidate's skills, experience, and project depth
**Preconditions:** Profile owner has set up profile with some public content and a slug

**Steps:**
1. Recruiter opens URL: `https://interviewme.app/p/fernando-gomes`
2. Browser loads public profile page (no login required)
3. Page displays:
   - Header: Full name, headline, location, professional links (LinkedIn, GitHub)
   - Summary section: Professional bio
   - Skills section: Public skills grouped by category with proficiency indicators
   - Work Timeline: Public job experiences ordered chronologically (most recent first)
   - Projects & Stories: Public projects with tech stack, metrics, and public STAR stories
4. Recruiter scans skills, clicks on a project to expand details
5. Recruiter reads a STAR story about a high-scale payments system
6. Recruiter clicks the LinkedIn link to connect with the candidate

**Success Criteria:**
- Page loads in < 2 seconds (single API call for all data)
- Only public content displayed (private jobs, skills, stories NOT shown)
- Page is responsive (works on mobile and desktop)
- Professional, clean layout that represents the candidate well
- No login prompt or authentication wall

#### Scenario 2: Profile Owner Sets Up Their Public Profile Slug

**Actor:** Career professional setting up their public presence
**Goal:** Choose a unique slug for their public profile URL
**Preconditions:** User has authenticated account and existing profile

**Steps:**
1. User navigates to Profile Editor > "Public Profile Settings" section
2. User enters desired slug: "fernando-gomes"
3. System validates slug: alphanumeric + hyphens only, 3-50 chars, unique across platform
4. If slug taken: system shows "This URL is already in use. Try: fernando-gomes-1"
5. User saves slug
6. System displays: "Your public profile is live at: /p/fernando-gomes"
7. User clicks "Preview" to see their public profile as a recruiter would

**Success Criteria:**
- Slug validated for URL safety (alphanumeric + hyphens, lowercase)
- Uniqueness enforced across all tenants (global unique)
- Preview link opens public profile in new tab
- Slug changeable at any time (old slug returns 404 after change)

#### Scenario 3: Profile Owner Previews Public vs Private Content

**Actor:** Profile owner verifying what recruiters see
**Goal:** Ensure private content is hidden and public content looks good
**Preconditions:** User has mixed public/private content

**Steps:**
1. User has 5 jobs: 3 marked public, 2 marked private
2. User has 20 skills: 15 public, 5 private
3. User has 4 stories: 2 public, 2 private
4. User clicks "Preview Public Profile"
5. Preview page shows: 3 public jobs, 15 public skills, 2 public stories
6. Private content is completely absent (not hidden behind a "show more" button)
7. User toggles a job from private to public in the editor
8. User refreshes preview: now shows 4 public jobs

**Success Criteria:**
- Preview matches exactly what external visitors see
- Zero private data visible in the public view
- Changes to visibility immediately reflected on refresh

#### Scenario 4: Search Engine Crawls Public Profile

**Actor:** Google search bot
**Goal:** Index the public profile for search results
**Preconditions:** Profile has public content and a slug

**Steps:**
1. Googlebot requests `/p/fernando-gomes`
2. Server returns HTML with proper meta tags:
   - `<title>Fernando Gomes - Senior Software Engineer | Interview Me</title>`
   - `<meta name="description" content="Senior Software Engineer with 15+ years...">`
   - Open Graph tags for social sharing preview
   - JSON-LD structured data (Person schema)
3. Page content is rendered (React hydration or SSR-friendly approach)
4. Google indexes the page with correct title, description, and structured data

**Success Criteria:**
- Meta tags present in HTML response
- Open Graph tags enable rich social sharing previews
- Structured data (JSON-LD) helps search engines understand the content
- Page content accessible to crawlers (not blocked by JS-only rendering)

#### Scenario 5: Visitor Views Profile on Mobile Device

**Actor:** Recruiter viewing profile on a phone
**Goal:** Read candidate's profile comfortably on a small screen
**Preconditions:** Public profile has content

**Steps:**
1. Recruiter opens profile link on mobile browser
2. Page renders with mobile-optimized layout:
   - Single column layout
   - Skills chips wrap naturally
   - Job timeline cards stack vertically
   - Story cards are expandable/collapsible
   - Professional links as icon buttons
3. Page scrolls smoothly, no horizontal overflow
4. Text is readable without zooming

**Success Criteria:**
- Responsive design works on viewports from 320px to 1920px+
- No horizontal scrollbar on any viewport
- Touch-friendly interactive elements (expandable sections)
- Performance: loads in < 3 seconds on 3G connection

### Edge Cases

- **Empty public profile:** User has profile but no public content -- show profile header with message "This profile has no public content yet"
- **No slug set:** Profile exists but no slug -- public page inaccessible until slug is set
- **Slug not found:** Invalid slug -- return 404 page with "Profile not found"
- **Deleted profile:** Profile with deleted_at set -- return 404 (same as not found)
- **All private content:** User has extensive profile but everything is private -- show header only with "No public content" message
- **Very long content:** User has 20 jobs, 50 skills, 10 stories -- paginate or lazy-load sections
- **Special characters in names:** Full name with accents, non-Latin characters -- render correctly
- **Concurrent slug change:** User changes slug while recruiter is viewing -- old URL returns 404 immediately
- **Profile with no jobs but has skills:** Show skills section, hide empty timeline section
- **Future chatbox placeholder:** Reserve space for recruiter chat widget (Feature 006)

---

## Functional Requirements

### Core Capabilities

**REQ-001:** Public Profile URL with Slug
- **Description:** Each profile MUST have an optional unique slug used as the public URL identifier
- **Acceptance Criteria:**
  - Add `slug` column to `profile` table (VARCHAR 50, NULLABLE, UNIQUE across all tenants)
  - Slug format: lowercase alphanumeric and hyphens only, 3-50 characters, regex: `^[a-z0-9][a-z0-9-]*[a-z0-9]$`
  - Slug must not start or end with hyphen, no consecutive hyphens
  - Reserved slugs: "admin", "api", "login", "register", "dashboard", "profile", "billing", "settings", "help", "about"
  - Public profile accessible at: `/p/{slug}`
  - Slug is globally unique (no two profiles across any tenant can have the same slug)
  - Profile owner can set/change slug via existing profile update endpoint
  - When slug is NULL, profile has no public page

**REQ-002:** Public Profile Data Aggregation Endpoint
- **Description:** Backend MUST provide a single unauthenticated REST endpoint that returns all public profile data aggregated
- **Acceptance Criteria:**
  - Endpoint: GET `/api/public/profiles/{slug}` (no authentication required)
  - Returns aggregated response including:
    - Profile: full_name, headline, summary, location, languages, professional_links (only if visibility = public or default_visibility = public)
    - Job Experiences: All public, non-deleted jobs ordered by start_date DESC
    - Education: All public, non-deleted education ordered by end_date DESC
    - User Skills: All public, non-deleted skills with catalog skill name and category, ordered by proficiency_depth DESC
    - Experience Projects: All public, non-deleted projects under public jobs, with tech_stack and metrics
    - Stories: All public, non-deleted stories under public projects, with STAR fields and metrics
  - Private content MUST be filtered at the database query level (not post-fetch filtering)
  - Returns 404 if slug not found or profile is deleted
  - Response cached with short TTL (e.g., 60 seconds) for performance
  - Single database round-trip with JPA fetch joins or multiple optimized queries

**REQ-003:** Public Profile Visibility Enforcement
- **Description:** The public profile MUST display ONLY content explicitly marked as "public" visibility
- **Acceptance Criteria:**
  - Profile fields respect `default_visibility` setting
  - Each JobExperience, Education, UserSkill, ExperienceProject, Story has independent `visibility` field
  - Visibility = "public" means: included in public profile response
  - Visibility = "private" means: NEVER included in any public endpoint response
  - Backend queries use `WHERE visibility = 'public' AND deleted_at IS NULL` in all public data queries
  - No "semi-public" or "unlisted" state in Phase 1 (binary public/private only)

**REQ-004:** Public Profile SEO Metadata
- **Description:** Public profile pages MUST include proper SEO meta tags, Open Graph tags, and structured data
- **Acceptance Criteria:**
  - HTML `<title>`: "{Full Name} - {Headline} | Interview Me"
  - `<meta name="description">`: First 160 chars of summary, or headline if no summary
  - Open Graph tags: `og:title`, `og:description`, `og:type` (profile), `og:url`, `og:image` (default avatar or future profile photo)
  - Twitter Card tags: `twitter:card` (summary), `twitter:title`, `twitter:description`
  - JSON-LD structured data (schema.org/Person):
    ```json
    {
      "@context": "https://schema.org",
      "@type": "Person",
      "name": "Fernando Gomes",
      "jobTitle": "Senior Software Engineer",
      "url": "https://interviewme.app/p/fernando-gomes",
      "knowsAbout": ["Java", "Spring Boot", "Kubernetes"]
    }
    ```
  - Meta tags injected via React Helmet or equivalent
  - Canonical URL set to prevent duplicate content

**REQ-005:** Slug Management in Profile Editor
- **Description:** Profile owners MUST be able to set and change their public profile slug
- **Acceptance Criteria:**
  - New "Public Profile" section in Profile Editor page
  - Input field for slug with live validation:
    - Format validation (alphanumeric + hyphens, 3-50 chars)
    - Availability check via API: GET `/api/profiles/slug/check?slug={slug}` (authenticated)
    - Debounced availability check (500ms delay)
  - Display generated public URL: "Your profile: interviewme.app/p/{slug}"
  - "Preview" button opens public profile in new tab
  - "Copy Link" button copies URL to clipboard
  - Clear warning when changing slug: "Changing your URL will make the old one inaccessible immediately"

**REQ-006:** Public Profile View Tracking
- **Description:** System SHOULD track public profile views for analytics
- **Acceptance Criteria:**
  - Log each public profile view with: slug, timestamp, user-agent, referer (no PII like IP address stored in DB)
  - Phase 1: Simple counter increment on profile entity (`view_count` column) or log-based tracking
  - Future: detailed analytics with unique visitors, referral sources, etc.
  - View count visible to profile owner in dashboard (not on public page)

### User Interface Requirements

**REQ-UI-001:** Public Profile Page Layout
- **Description:** Frontend MUST render a professional, responsive public profile page
- **Acceptance Criteria:**
  - Route: `/p/{slug}` (public, no ProtectedRoute wrapper)
  - Layout sections (top to bottom):
    1. **Header:** Full name (large), headline, location (with icon), language badges
    2. **Professional Links Bar:** LinkedIn, GitHub, portfolio, etc. as icon buttons
    3. **Summary Section:** Professional bio/summary text
    4. **Skills Section:** Skills grouped by category, each skill shows name + proficiency (1-5 dots/stars)
    5. **Work Timeline:** Job experiences as timeline cards (company, role, dates, achievements)
    6. **Projects:** Under each job, public projects with tech stack chips, metrics, and outcomes
    7. **Stories:** Under each project, expandable STAR story cards with metrics
    8. **Education Section:** Education entries (degree, institution, dates)
    9. **Footer:** "Powered by Interview Me" with platform link, future chatbox placeholder
  - Empty sections (no public content) are hidden entirely (not shown with "No items" message)
  - Responsive: single column on mobile, two-column layout possible on wide screens
  - Clean, professional design (inspired by modern developer portfolio sites)

**REQ-UI-002:** Skills Section Display
- **Description:** Skills section MUST display public skills in an organized, visually appealing way
- **Acceptance Criteria:**
  - Skills grouped by category (Languages, Frameworks, Cloud, etc.)
  - Each skill displayed as a chip/badge with: skill name, proficiency indicator (1-5 filled dots or stars)
  - Categories as section headers with skill chips below
  - Categories ordered by number of skills (largest first) or predefined order
  - If > 20 skills: show top 15 with "Show all N skills" expandable section

**REQ-UI-003:** Work Timeline Section
- **Description:** Work timeline MUST display public job experiences as a professional timeline
- **Acceptance Criteria:**
  - Vertical timeline with connection line between entries
  - Each entry shows: company name (bold), role/title, date range ("Jan 2020 - Present"), location
  - "Current" badge for active job (is_current = true)
  - Achievements/responsibilities shown as bullet points or paragraph
  - Under each job: list of public projects as sub-cards (if Feature 004 data exists)
  - Smooth expand/collapse for long descriptions

**REQ-UI-004:** Stories Section Display
- **Description:** Public STAR stories MUST be displayed as expandable cards
- **Acceptance Criteria:**
  - Story card shows: title, linked skill chips, metrics badges
  - Click to expand reveals full STAR sections (Situation, Task, Action, Result)
  - Each STAR section has a subtle label (S:, T:, A:, R:) or colored sidebar
  - Metrics shown as key-value badges (e.g., "Peak TPS: 18,000", "Downtime: 0 min")
  - Stories visually nested under their parent project

**REQ-UI-005:** Public Profile Not Found Page
- **Description:** Frontend MUST display a friendly 404 page for invalid slugs
- **Acceptance Criteria:**
  - Route `/p/{invalid-slug}` shows 404 page
  - Message: "Profile not found" with suggestion to check the URL
  - Link to platform homepage
  - Clean design consistent with platform branding

**REQ-UI-006:** Chatbox Placeholder
- **Description:** Public profile page MUST include a placeholder for future recruiter chatbox
- **Acceptance Criteria:**
  - Fixed-position chat icon button in bottom-right corner
  - On click: shows tooltip "Recruiter chat coming soon!" or simple dialog
  - Placeholder is visually consistent with the page design
  - Easy to replace with actual chatbox component in Feature 006

### Data Requirements

**REQ-DATA-001:** Slug Column on Profile Table
- **Description:** Add slug column to existing profile table
- **Acceptance Criteria:**
  - Liquibase migration to add column: `slug` (VARCHAR 50, NULLABLE, UNIQUE)
  - Add index: `idx_profile_slug` (unique)
  - Existing profiles: slug starts as NULL (no public page until set)
  - New column added via ALTER TABLE (non-breaking migration)

**REQ-DATA-002:** View Count Column on Profile Table (Optional Phase 1)
- **Description:** Add optional view counter to profile table
- **Acceptance Criteria:**
  - Liquibase migration to add column: `view_count` (BIGINT, DEFAULT 0)
  - Incremented on each public profile view
  - Not displayed on public page (only in owner's dashboard)

---

## Success Criteria

The feature will be considered successful when:

1. **Public Access Without Auth:** Unauthenticated users can access `/p/{slug}` and see public profile content
   - Measurement: Integration test with no JWT token, verify 200 OK response with public data

2. **Private Content Never Exposed:** No private jobs, skills, or stories appear in public profile response
   - Measurement: Create profile with mixed visibility, verify public endpoint excludes all private content

3. **SEO Tags Present:** Meta tags, Open Graph, and JSON-LD structured data present in page HTML
   - Measurement: Validate with Google Rich Results Test or manual HTML inspection

4. **Responsive Design:** Page renders correctly on mobile (320px) through desktop (1920px)
   - Measurement: Manual testing on 3 viewport sizes (mobile, tablet, desktop)

5. **Performance:** Public profile API responds in < 200ms, page loads in < 2 seconds
   - Measurement: Load test with 100 concurrent requests, verify p95 < 200ms

6. **Slug Uniqueness:** No two profiles can have the same slug
   - Measurement: Integration test attempting duplicate slug returns 409 Conflict

7. **404 for Invalid Slug:** Non-existent slug returns proper 404 response
   - Measurement: Request `/api/public/profiles/nonexistent` returns 404

---

## Key Entities

### Public Profile Aggregate (Read-Only DTO)

This is NOT a new database entity. It is a read-only aggregate DTO assembled by the PublicProfileService from multiple existing entities.

**Attributes (PublicProfileResponse record):**
- slug: Profile slug (String)
- fullName: Full name (String)
- headline: Professional headline (String)
- summary: Bio/summary (String, nullable)
- location: Location (String, nullable)
- languages: List of languages (List<String>, nullable)
- professionalLinks: Map of link types to URLs (Map<String, String>, nullable)
- skills: List of public skills with catalog info (List<PublicSkillResponse>)
- jobs: List of public job experiences (List<PublicJobResponse>)
- education: List of public education entries (List<PublicEducationResponse>)
- projects: List of public projects grouped by job (nested within PublicJobResponse)
- stories: List of public stories grouped by project (nested within PublicProjectResponse)

### PublicSkillResponse (record)
- skillName: Catalog skill name (String)
- category: Skill category (String)
- proficiencyDepth: 1-5 (int)
- yearsOfExperience: Years (int)
- lastUsedDate: Last used (LocalDate, nullable)

### PublicJobResponse (record)
- company: Company name (String)
- role: Job title (String)
- startDate: Start date (LocalDate)
- endDate: End date (LocalDate, nullable)
- isCurrent: Current job flag (boolean)
- location: Location (String, nullable)
- employmentType: Employment type (String, nullable)
- responsibilities: Responsibilities text (String, nullable)
- achievements: Achievements text (String, nullable)
- metrics: JSONB metrics (Map<String, Object>, nullable)
- projects: List of public projects under this job (List<PublicProjectResponse>)

### PublicProjectResponse (record)
- title: Project title (String)
- context: Project context (String, nullable)
- role: Role in project (String, nullable)
- teamSize: Team size (Integer, nullable)
- techStack: Technologies (List<String>, nullable)
- architectureType: Architecture (String, nullable)
- metrics: JSONB metrics (Map<String, Object>, nullable)
- outcomes: Outcomes text (String, nullable)
- linkedSkills: Skills used (List<String>) -- just skill names
- stories: Stories under this project (List<PublicStoryResponse>)

### PublicStoryResponse (record)
- title: Story title (String)
- situation: STAR Situation (String)
- task: STAR Task (String)
- action: STAR Action (String)
- result: STAR Result (String)
- metrics: JSONB metrics (Map<String, Object>, nullable)
- linkedSkills: Skills in this story (List<String>) -- just skill names

### PublicEducationResponse (record)
- degree: Degree/cert name (String)
- institution: Institution name (String)
- startDate: Start date (LocalDate, nullable)
- endDate: End date (LocalDate)
- fieldOfStudy: Field of study (String, nullable)

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Spring Boot backend, React frontend, database infrastructure, Spring Security config (to allow unauthenticated access to public endpoints)
- **Feature 002: Profile CRUD** - Profile, JobExperience, Education entities and repositories
- **Feature 003: Skills Management** - Skill catalog and UserSkill entities and repositories
- **Feature 004: Experience Projects & Stories** - ExperienceProject, Story entities and repositories (graceful handling if not yet implemented: public profile works without projects/stories, showing only profile + jobs + skills)

### External Dependencies

- **React Helmet Async:** For dynamic meta tag injection (SEO, Open Graph)
- **MUI Components:** For page layout (Container, Typography, Chip, Timeline, Card, Accordion)
- **@mui/lab Timeline:** For work timeline display (or custom CSS timeline)

---

## Assumptions

1. Features 001, 002, 003 are implemented. Feature 004 may or may not be implemented (public profile handles missing entities gracefully).
2. Profile slug is the only way to access a public profile (no numeric ID-based public access).
3. One slug per profile (no aliases or redirects in Phase 1).
4. Slug changes are immediate and non-recoverable (old slug becomes available for others).
5. Public profile is always free (no coins, no quotas, no limits on views).
6. No server-side rendering (SSR) in Phase 1. React SPA with client-side rendering. SEO addressed via meta tags in index.html template and React Helmet.
7. No profile photo/avatar in Phase 1 (text-only profile header).
8. No recruiter chat in Phase 1 (placeholder only, implemented in Feature 006).
9. Public profile API does not require any rate limiting in Phase 1 (future consideration for DDoS protection).
10. View count tracking is best-effort (not transactionally accurate; can miss some views).
11. Slug validation is case-insensitive (all slugs stored lowercase).

---

## Out of Scope

The following are explicitly excluded from this feature:

1. **No Recruiter Chat:** Chatbox implemented in Feature 006 (placeholder only here)
2. **No Server-Side Rendering (SSR):** React SPA with client-side rendering only
3. **No Profile Photo/Avatar:** Text-only header (profile photos are a future feature)
4. **No Packages/Tokenized Links:** Curated content packages covered in separate feature
5. **No PDF Export from Public Page:** Exports covered in Feature 008
6. **No Custom Themes/Styling:** One default theme for public profiles
7. **No Analytics Dashboard:** Detailed view analytics (referrers, unique visitors) for profile owners
8. **No Contact Form:** Direct messaging or contact form on public profile
9. **No Social Sharing Buttons:** Share buttons for Twitter, LinkedIn, etc.
10. **No Custom Domain:** Custom domain mapping (e.g., fernando.dev -> /p/fernando-gomes)
11. **No Password-Protected Profiles:** All public profiles are fully public
12. **No QR Code Generation:** QR code for profile URL

---

## Security & Privacy Considerations

### Security Requirements

- Public profile endpoint (`/api/public/profiles/{slug}`) MUST NOT require authentication
- Spring Security configuration MUST explicitly permit unauthenticated access to `/api/public/**` and `/p/**` paths
- Slug check endpoint (`/api/profiles/slug/check`) MUST require authentication (to prevent enumeration attacks)
- Public endpoint MUST NOT expose internal IDs (profile_id, tenant_id, user_id)
- Public endpoint MUST NOT expose: version fields, deleted_at timestamps, tenant information, private visibility items
- No user tracking via cookies on public profiles (GDPR compliance)
- Rate limiting consideration: future feature to prevent scraping (not Phase 1)

### Privacy Requirements

- Private content MUST be filtered at the database query level (`WHERE visibility = 'public'`)
- Private content MUST NEVER be included in the API response, even in a hidden/redacted form
- No PII beyond what the user explicitly marked as public
- Professional links (LinkedIn, GitHub) only shown if user's default_visibility is public
- Location only shown if user explicitly marked it public
- Languages only shown if user explicitly marked them public
- Public profile does not reveal: email address, phone number, tenant name, user ID

---

## Performance Expectations

- **Public Profile API:** p95 latency < 200ms for full aggregated response
- **Page Load Time:** < 2 seconds on broadband, < 3 seconds on 3G
- **API Response Size:** < 100KB for a typical profile (20 skills, 5 jobs, 3 projects, 3 stories)
- **Concurrent Requests:** Support 100 concurrent public profile views without degradation
- **Database Queries:** Use JPA fetch joins or batched queries to avoid N+1 problems. Target 3-5 database queries max for full profile aggregation.
- **Caching:** Consider short-lived cache (60s) on public profile response to reduce DB load for popular profiles
- **Static Assets:** MUI and React bundles cached via browser cache headers

---

## Error Handling

### Error Scenarios

**ERROR-001:** Slug Not Found
- **User Experience:** Public page shows friendly 404: "Profile not found. Please check the URL."
- **API Response:** 404 Not Found with body `{"code": "PROFILE_NOT_FOUND", "message": "No public profile found for this URL"}`
- **Recovery Path:** User checks URL spelling or contacts profile owner for correct link

**ERROR-002:** Slug Already Taken (During Setup)
- **User Experience:** Profile editor shows inline error: "This URL is already in use. Try: fernando-gomes-1"
- **API Response:** 409 Conflict with body `{"code": "SLUG_TAKEN", "message": "This slug is already in use", "field": "slug"}`
- **Recovery Path:** User tries a different slug

**ERROR-003:** Invalid Slug Format
- **User Experience:** Profile editor shows inline validation: "URL must contain only lowercase letters, numbers, and hyphens (3-50 characters)"
- **API Response:** 400 Bad Request with field validation error
- **Recovery Path:** User corrects slug format

**ERROR-004:** Profile Exists But No Public Content
- **User Experience:** Public page shows profile header (name, headline) with message: "This profile has no public content yet."
- **API Response:** 200 OK with empty arrays for skills, jobs, education, projects, stories
- **Recovery Path:** Profile owner makes content public in the editor

**ERROR-005:** Server Error During Profile Aggregation
- **User Experience:** Public page shows generic error: "Something went wrong. Please try again later."
- **API Response:** 500 Internal Server Error (logged with full stack trace)
- **Recovery Path:** Automatic retry by frontend; if persistent, operations team investigates

---

## Testing Scope

### Functional Testing

- **Public Endpoint No Auth:** Verify `/api/public/profiles/{slug}` works without JWT
- **Visibility Filtering:** Create profile with mixed public/private content, verify only public returned
- **Slug Validation:** Test valid slugs, invalid formats, reserved words, duplicates
- **404 Handling:** Test with non-existent slug, deleted profile, NULL slug
- **Data Aggregation:** Verify all entity types included (profile, jobs, education, skills, projects, stories)
- **Empty Sections:** Profile with no public jobs returns empty jobs array (not 404)
- **Ordering:** Jobs by start_date DESC, skills by proficiency DESC, education by end_date DESC

### User Acceptance Testing

- **Complete Public Profile:** Create profile with 3 public jobs, 15 public skills, 2 projects, 2 stories, view public page
- **Mobile Responsiveness:** View public profile on phone, tablet, desktop
- **Slug Setup:** Set slug, preview profile, change slug, verify old URL is 404
- **SEO Validation:** Check meta tags with browser dev tools or SEO validator

### Edge Case Testing

- **All Private Content:** Profile exists but everything is private -- show header only
- **Unicode Names:** Full name with accents and non-Latin characters
- **Very Long Profile:** 20 jobs, 50 skills, 10 projects, 15 stories -- verify performance
- **Concurrent Slug Changes:** Two users try same slug simultaneously
- **Deleted Profile:** Verify deleted profile returns 404 on public endpoint
- **Feature 004 Not Implemented:** Public profile works gracefully without projects/stories data

---

## Notes

This feature is the single most important "always-free" surface of the Interview Me platform. Key design decisions:

- **Slug-Based URLs:** Clean, memorable URLs (not numeric IDs or UUIDs) for professional sharing
- **Single Aggregate Endpoint:** One API call returns everything the public page needs, minimizing round-trips
- **Query-Level Filtering:** Private content filtered in SQL WHERE clauses, never loaded into memory
- **No New Entities:** This feature creates read-only aggregate DTOs from existing entities -- no new database tables (except adding slug column)
- **Graceful Degradation:** Public profile works even if Features 003 or 004 are not yet implemented
- **Chatbox Placeholder:** Reserve UI space for Feature 006 without implementing chat functionality
- **SEO From Day One:** Meta tags and structured data ensure search engine discoverability

Future enhancements:
- Server-side rendering for better SEO
- Profile photo/avatar
- Custom themes (dark mode, color schemes)
- Recruiter chat integration (Feature 006)
- View analytics dashboard
- QR code generation for profile URL
- Custom domain support
- Profile PDF download from public page

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial specification       | Claude Code |
