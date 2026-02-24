# Implementation Plan: Live Public Profile Page

**Feature ID:** 005-live-public-profile
**Status:** Design Complete
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Executive Summary

This implementation plan defines the technical design for the Live Public Profile Page, the platform's primary free-tier offering. The feature consists of: (1) a backend public API endpoint that aggregates all public-visibility profile data into a single optimized response, (2) a Liquibase migration to add a `slug` column to the profile table, (3) a Spring Security configuration update to allow unauthenticated access, and (4) a React frontend PublicProfilePage with responsive, SEO-friendly rendering.

The design emphasizes strict privacy enforcement (private content filtered at SQL query level, never loaded into memory), performance (single API call for full profile, < 200ms p95), and SEO readiness (React Helmet for meta tags, JSON-LD structured data). No new database entities are created -- the feature assembles read-only DTOs from existing Profile, JobExperience, Education, UserSkill, ExperienceProject, and Story entities.

**Key Deliverables:**
- Backend: 1 migration (add slug to profile), 1 public controller, 1 public profile service, 8+ DTO records, security config update
- Frontend: PublicProfilePage with 7 sub-components, React Helmet SEO, responsive MUI layout
- Testing: Public access tests, visibility enforcement tests, slug validation tests, responsive UI tests

---

## Architecture Design

### Backend Package Structure

```
com.interviewme.publicprofile/
  controller/
    PublicProfileController.java     # Unauthenticated public profile endpoint
  service/
    PublicProfileService.java        # Aggregates public data from multiple repositories
  dto/
    PublicProfileResponse.java       # Top-level aggregate response (record)
    PublicSkillResponse.java         # Public skill with catalog info (record)
    PublicJobResponse.java           # Public job with nested projects (record)
    PublicEducationResponse.java     # Public education entry (record)
    PublicProjectResponse.java       # Public project with nested stories (record)
    PublicStoryResponse.java         # Public STAR story (record)
    SeoMetadata.java                 # SEO metadata for frontend rendering (record)
    SlugCheckResponse.java           # Slug availability response (record)
```

The public profile package is intentionally separate from `com.interviewme.profile` to enforce a clear boundary: the profile package handles authenticated CRUD, while publicprofile handles unauthenticated read-only aggregation.

### Database Schema Changes

#### Add slug to profile table (Migration)

```sql
ALTER TABLE profile ADD COLUMN slug VARCHAR(50);
CREATE UNIQUE INDEX idx_profile_slug ON profile(slug);
```

- slug is NULLABLE (profiles without slug have no public page)
- UNIQUE constraint is global across all tenants (slugs are platform-wide unique)
- No foreign key (slug is a denormalized URL identifier)

#### Optional: Add view_count to profile table

```sql
ALTER TABLE profile ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;
```

### API Endpoints Design

#### Public Endpoints (No Authentication)

| Method | Path | Description | Auth | Response |
|--------|------|-------------|------|----------|
| GET | `/api/public/profiles/{slug}` | Get full public profile | None | 200 PublicProfileResponse / 404 |

#### Authenticated Endpoints (Existing Profile Controller - Extensions)

| Method | Path | Description | Auth | Response |
|--------|------|-------------|------|----------|
| GET | `/api/profiles/slug/check?slug={slug}` | Check slug availability | JWT | 200 SlugCheckResponse |
| PUT | `/api/profiles/{profileId}/slug` | Set/update profile slug | JWT | 200 ProfileResponse |

The slug check and update endpoints are extensions of the existing ProfileController, not part of the public profile package.

### Spring Security Configuration Update

The Spring Security filter chain must permit unauthenticated access to public profile endpoints:

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            // Existing public endpoints
            .requestMatchers("/api/auth/**").permitAll()
            // NEW: Public profile endpoints
            .requestMatchers("/api/public/**").permitAll()
            // NEW: Public profile SPA route
            .requestMatchers("/p/**").permitAll()
            // Everything else requires auth
            .anyRequest().authenticated()
        );
    return http.build();
}
```

### Service Layer Design

#### PublicProfileService

The core service that aggregates public data from multiple repositories into a single response.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PublicProfileService {

    private final ProfileRepository profileRepository;
    private final JobExperienceRepository jobExperienceRepository;
    private final EducationRepository educationRepository;
    private final UserSkillRepository userSkillRepository;
    // Optional: ExperienceProjectRepository, StoryRepository
    // (graceful handling if not yet available)

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(String slug) {
        // 1. Find profile by slug (no tenant filter -- public endpoint)
        Profile profile = profileRepository.findBySlugAndDeletedAtIsNull(slug)
            .orElseThrow(() -> new PublicProfileNotFoundException(slug));

        Long profileId = profile.getId();
        Long tenantId = profile.getTenantId();

        // 2. Fetch public jobs (visibility = 'public', deleted_at IS NULL)
        List<JobExperience> publicJobs = jobExperienceRepository
            .findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByStartDateDesc(
                profileId, "public");

        // 3. Fetch public education
        List<Education> publicEdu = educationRepository
            .findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByEndDateDesc(
                profileId, "public");

        // 4. Fetch public skills with catalog join
        List<UserSkill> publicSkills = userSkillRepository
            .findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByProficiencyDepthDesc(
                profileId, "public");

        // 5. Fetch public projects and stories (if Feature 004 exists)
        // Graceful handling: if repositories not available, return empty lists

        // 6. Assemble aggregate response
        return assemblePublicProfile(profile, publicJobs, publicEdu,
            publicSkills, publicProjects, publicStories);

        // 7. Increment view count (fire-and-forget)
        incrementViewCount(profileId);
    }
}
```

**Key Design Decisions:**

1. **No Hibernate tenant filter on public queries:** The public endpoint does not have a tenant context (no JWT). Queries use explicit `WHERE profile_id = ?` and `visibility = 'public'` conditions instead of the tenant filter.

2. **Query-level filtering:** All repositories have dedicated methods that filter `visibility = 'public' AND deleted_at IS NULL`. Private data never touches Java memory.

3. **Graceful degradation:** If Feature 004 (projects/stories) repositories are not available (not yet implemented), the service catches the error and returns empty lists for those sections.

4. **View count increment:** Fire-and-forget (async or separate transaction) to not slow down the profile response.

### Repository Extensions

New query methods needed on existing repositories:

```java
// ProfileRepository - NEW method
Optional<Profile> findBySlugAndDeletedAtIsNull(String slug);
boolean existsBySlug(String slug);

// JobExperienceRepository - existing method works
List<JobExperience> findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByStartDateDesc(
    Long profileId, String visibility);

// EducationRepository - NEW method
List<Education> findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByEndDateDesc(
    Long profileId, String visibility);

// UserSkillRepository - NEW method
List<UserSkill> findByProfileIdAndVisibilityAndDeletedAtIsNullOrderByProficiencyDepthDesc(
    Long profileId, String visibility);
```

No new repositories are created. Existing repositories get additional query methods.

### DTO Definitions (Java Records)

```java
// Top-level aggregate
public record PublicProfileResponse(
    String slug,
    String fullName,
    String headline,
    String summary,
    String location,
    List<String> languages,
    Map<String, String> professionalLinks,
    List<PublicSkillResponse> skills,
    List<PublicJobResponse> jobs,
    List<PublicEducationResponse> education,
    SeoMetadata seo
) {}

public record PublicSkillResponse(
    String skillName,
    String category,
    int proficiencyDepth,
    int yearsOfExperience,
    LocalDate lastUsedDate
) {}

public record PublicJobResponse(
    String company,
    String role,
    LocalDate startDate,
    LocalDate endDate,
    boolean isCurrent,
    String location,
    String employmentType,
    String responsibilities,
    String achievements,
    Map<String, Object> metrics,
    List<PublicProjectResponse> projects
) {}

public record PublicEducationResponse(
    String degree,
    String institution,
    LocalDate startDate,
    LocalDate endDate,
    String fieldOfStudy
) {}

public record PublicProjectResponse(
    String title,
    String context,
    String role,
    Integer teamSize,
    List<String> techStack,
    String architectureType,
    Map<String, Object> metrics,
    String outcomes,
    List<String> linkedSkills,
    List<PublicStoryResponse> stories
) {}

public record PublicStoryResponse(
    String title,
    String situation,
    String task,
    String action,
    String result,
    Map<String, Object> metrics,
    List<String> linkedSkills
) {}

public record SeoMetadata(
    String title,
    String description,
    String canonicalUrl,
    List<String> keywords
) {}

public record SlugCheckResponse(
    String slug,
    boolean available,
    List<String> suggestions
) {}
```

**Critical: No internal IDs exposed.** The public DTOs contain zero IDs (no profile_id, tenant_id, user_id, job_id, etc.). Only human-readable data is returned.

### Frontend Design

#### Component Structure

```
frontend/src/
  pages/
    PublicProfilePage.tsx              # Main public profile page
  components/
    public-profile/
      PublicProfileHeader.tsx          # Name, headline, location, links
      PublicProfileSummary.tsx         # Bio/summary section
      PublicSkillsSection.tsx          # Skills grouped by category
      PublicWorkTimeline.tsx           # Job experiences timeline
      PublicProjectCard.tsx            # Project card with tech stack
      PublicStoryCard.tsx              # Expandable STAR story card
      PublicEducationSection.tsx       # Education entries
      PublicProfileNotFound.tsx        # 404 page
      ChatboxPlaceholder.tsx           # Future recruiter chat placeholder
      PublicProfileSeo.tsx             # React Helmet meta tags
  api/
    publicProfileApi.ts                # API client for public profile
  types/
    publicProfile.ts                   # TypeScript interfaces
  hooks/
    usePublicProfile.ts                # TanStack Query hook
```

#### PublicProfilePage Layout

```
+----------------------------------------------------------+
|  HEADER                                                    |
|  Fernando Gomes                                           |
|  Senior Software Engineer | Fintech | Java/Spring         |
|  [location icon] Sao Paulo, Brazil                        |
|  [LinkedIn] [GitHub] [Portfolio]                           |
+----------------------------------------------------------+
|  SUMMARY                                                   |
|  15+ years of experience building high-scale payment...   |
+----------------------------------------------------------+
|  SKILLS                                                    |
|  Languages:  [Java ***] [Python **] [Go *]               |
|  Frameworks: [Spring Boot ****] [React ***]              |
|  Cloud:      [AWS ****] [Kubernetes ****]                |
|  Databases:  [PostgreSQL ****] [Redis ***]               |
+----------------------------------------------------------+
|  EXPERIENCE                                                |
|  o--[ 2020 - Present ]-- Tech Lead @ FinCorp             |
|  |  Payments Gateway Platform                             |
|  |  [Java] [Kafka] [K8s] | TPS: 15,000 | Team: 8        |
|  |  > "Black Friday Traffic Spike" [Expand Story]         |
|  |                                                         |
|  o--[ 2016 - 2020 ]-- Senior Dev @ TechCo               |
|  |  Cloud Migration Initiative                             |
|  |  [AWS] [Terraform] [Docker]                            |
+----------------------------------------------------------+
|  EDUCATION                                                 |
|  B.Sc. Computer Science - USP (2012)                      |
|  AWS Solutions Architect Certification (2023)              |
+----------------------------------------------------------+
|  Footer: Powered by Interview Me                          |
|                                        [Chat icon - soon] |
+----------------------------------------------------------+
```

#### SEO Implementation (React Helmet)

```tsx
<Helmet>
  <title>{profile.fullName} - {profile.headline} | Interview Me</title>
  <meta name="description" content={profile.summary?.slice(0, 160)} />
  <meta property="og:title" content={`${profile.fullName} - ${profile.headline}`} />
  <meta property="og:description" content={profile.summary?.slice(0, 200)} />
  <meta property="og:type" content="profile" />
  <meta property="og:url" content={`https://interviewme.app/p/${profile.slug}`} />
  <meta name="twitter:card" content="summary" />
  <link rel="canonical" href={`https://interviewme.app/p/${profile.slug}`} />
  <script type="application/ld+json">
    {JSON.stringify({
      "@context": "https://schema.org",
      "@type": "Person",
      "name": profile.fullName,
      "jobTitle": profile.headline,
      "url": `https://interviewme.app/p/${profile.slug}`,
      "knowsAbout": profile.skills.map(s => s.skillName)
    })}
  </script>
</Helmet>
```

#### Responsive Design Strategy

- **Mobile (< 768px):** Single column, full-width cards, collapsible sections
- **Tablet (768px - 1024px):** Single column with wider margins
- **Desktop (> 1024px):** Centered content with max-width 900px, optional two-column for skills

MUI `Container` with `maxWidth="md"` provides consistent centering. MUI responsive breakpoints (`sx={{ ... }}`) handle layout adjustments.

### Slug Management (Profile Editor Extension)

Add a "Public Profile" section to the existing ProfileEditorPage:

```tsx
// In ProfileEditorPage.tsx - new section
<Box>
  <Typography variant="h6">Public Profile</Typography>
  <TextField
    label="Profile URL Slug"
    value={slug}
    onChange={handleSlugChange}
    helperText={slugAvailable ? "Available!" : "Already taken"}
    error={!slugAvailable}
    InputProps={{
      startAdornment: <InputAdornment position="start">interviewme.app/p/</InputAdornment>
    }}
  />
  <Button onClick={handleSaveSlug}>Save</Button>
  <Button onClick={() => window.open(`/p/${slug}`, '_blank')}>Preview</Button>
  <IconButton onClick={handleCopyLink}>
    <ContentCopyIcon />
  </IconButton>
</Box>
```

Slug availability check uses debounced API call (500ms):
```typescript
const checkSlugAvailability = useDebouncedCallback(async (slug: string) => {
  const response = await profileApi.checkSlug(slug);
  setSlugAvailable(response.available);
  setSuggestions(response.suggestions);
}, 500);
```

---

## Migration Strategy

Single Liquibase migration file:

**File:** `20260224150000-add-slug-to-profile.xml`

```xml
<changeSet id="20260224150000-1" author="interview-me">
    <addColumn tableName="profile">
        <column name="slug" type="VARCHAR(50)">
            <constraints nullable="true"/>
        </column>
    </addColumn>

    <createIndex tableName="profile" indexName="idx_profile_slug" unique="true">
        <column name="slug"/>
    </createIndex>

    <rollback>
        <dropIndex tableName="profile" indexName="idx_profile_slug"/>
        <dropColumn tableName="profile" columnName="slug"/>
    </rollback>
</changeSet>

<changeSet id="20260224150000-2" author="interview-me">
    <addColumn tableName="profile">
        <column name="view_count" type="BIGINT" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
    </addColumn>

    <rollback>
        <dropColumn tableName="profile" columnName="view_count"/>
    </rollback>
</changeSet>
```

Added to `db.changelog-master.yaml` after existing migrations.

---

## Testing Strategy

### Unit Tests
- `PublicProfileServiceTest` - Test aggregation logic, empty sections handling, deleted profile handling
- Slug validation logic tests (format, reserved words)

### Integration Tests
- `PublicProfileControllerIntegrationTest` - Test GET without auth returns 200, test 404 for missing slug, test private data excluded
- `SlugManagementIntegrationTest` - Test slug set, check availability, duplicate prevention (409), reserved word rejection
- Test with mixed visibility content: create profile with 3 public + 2 private jobs, verify only 3 returned

### Visibility Enforcement Tests
- Create profile with mixed public/private content across all entity types
- Call public API, verify response contains ZERO private items
- Verify no internal IDs in response (no profile_id, tenant_id, etc.)

### Performance Tests
- Profile with 20 jobs, 50 skills, 10 projects, 15 stories
- Verify API response < 200ms
- Verify response size < 100KB

### Frontend Tests
- `PublicProfilePage.test.tsx` - Rendering with full data, empty sections, 404 handling
- `PublicSkillsSection.test.tsx` - Category grouping, proficiency display
- Responsive rendering tests at 320px, 768px, 1920px viewports

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| N+1 query problem in aggregation | High | Medium | Use JPA fetch joins or batch queries; profile with 3-5 queries max |
| Private data leaked in public endpoint | Low | Critical | Filter at SQL WHERE level; integration tests verify; code review |
| Slug enumeration/scraping | Medium | Low | No sensitive data exposed; rate limiting future feature |
| SEO not working (JS-rendered content) | Medium | Medium | React Helmet for meta tags; future SSR if needed |
| Feature 004 not implemented when 005 launches | Medium | Low | Graceful degradation: empty projects/stories arrays |
| Large profiles slow to render | Low | Medium | Lazy-load expandable sections; limit initial display |

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial design              | Claude Code |
