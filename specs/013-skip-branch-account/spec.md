# Spec: Account Deletion & E2E Test Data Cleanup

**Feature ID:** 013
**Status:** Draft
**Created:** 2026-02-28
**Last Updated:** 2026-02-28
**Version:** 1.0.0

---

## Overview

### Problem Statement

The Interview Me project has two related problems:

1. **E2E Test Data Pollution:** Playwright tests create users, tenants, profiles, skills, and other entities in PostgreSQL. Each test run registers new accounts with unique emails (e.g., `e2e-1709123456789-abc12@interview-me.dev`). Over time, this accumulates thousands of orphan records across 20+ tables. There is currently no cleanup mechanism -- no `afterAll` teardown in tests, no deletion API in the backend.

2. **Missing GDPR Feature:** As a SaaS platform, Interview Me must comply with GDPR Article 17 ("Right to Erasure"). Users must be able to request complete deletion of their account and all associated personal data. No such feature exists today.

### Feature Summary

Build a real "Delete My Account" API endpoint that performs hard deletion of an entire tenant and all associated data. E2E tests use this same endpoint in `afterAll` hooks for cleanup. A separate profile-gated internal endpoint provides a safety-net for bulk test cleanup.

### Target Users

- **Account owners** who want to delete their account and all data (GDPR right)
- **E2E test suite** needing automated cleanup of test-created data
- **Platform administrators** needing to remove accounts (future)

---

## Constitution Compliance

- **Principle 1: Simplicity First** - Single REST endpoint, standard DELETE pattern, no complex soft-delete machinery
- **Principle 2: Containerization** - No infrastructure changes needed
- **Principle 3: Modern Java** - Use records for DTOs
- **Principle 4: Data Sovereignty** - Complete tenant-scoped data removal, enforced at application layer
- **Principle 7: Security** - JWT authentication, confirmation text required, profile-gated test endpoint
- **Principle 8: Multi-Tenant** - Deletes entire tenant's data, respects tenant isolation
- **Principle 11: Schema Evolution** - New Liquibase migration to add ON DELETE CASCADE to FKs

---

## Evaluation of Options

### Option A: Internal Test-Only Cleanup Endpoint

`POST /api/internal/test-cleanup`

| Pros | Cons |
|------|------|
| Simple to implement | Does not serve any product need |
| Can hard-delete with raw SQL | Must be carefully gated (security risk) |
| Fast execution | Diverges from production code paths |
| | Still need GDPR deletion later anyway |
| | Two code paths to maintain |

### Option B: Real "Delete & Forget My Account" Feature (Recommended)

`DELETE /api/v1/account`

| Pros | Cons |
|------|------|
| Serves both product and test needs | Slightly more work than raw SQL |
| GDPR-compliant from day one | Must handle edge cases |
| Tests exercise real production code | |
| Single code path to maintain | |
| Industry standard for SaaS | |

### Recommendation: Option B

Build a real account deletion feature. E2E tests use it for cleanup. This eliminates maintenance of test-only code and ensures the deletion logic is always exercised by the E2E suite.

---

## Data Model: Cascade Deletion Analysis

### Entity Dependency Tree

```
Tenant (root)
  +-- User (FK: tenant_id, ON DELETE CASCADE already exists)
  |     +-- Profile (FK: user_id, FK: tenant_id)
  |           +-- JobExperience (FK: profile_id)
  |           |     +-- ExperienceProject (FK: job_experience_id)
  |           |           +-- ExperienceProjectSkill (FK: experience_project_id)
  |           |           +-- Story (FK: experience_project_id)
  |           |                 +-- StorySkill (FK: story_id)
  |           +-- Education (FK: profile_id)
  |           +-- UserSkill (FK: profile_id)
  |           +-- ChatSession (FK: profile_id)
  |           |     +-- ChatMessage (FK: session_id)
  |           +-- LinkedInAnalysis (FK: profile_id)
  |           |     +-- LinkedInSectionScore (FK: analysis_id)
  |           +-- ExportHistory (FK: profile_id)
  +-- CoinWallet (FK: tenant_id)
  |     +-- CoinTransaction (FK: wallet_id)
  +-- FreeTierUsage (FK: tenant_id)
  +-- ContentEmbedding (FK: tenant_id)
```

### Tables NOT Deleted (Global Catalogs)

- **skill** - Global skill catalog, not tenant-owned
- **export_template** - Global templates, not tenant-owned

### Current FK CASCADE Status

Only `users.tenant_id -> tenant.id` has `ON DELETE CASCADE`. All other FKs default to `NO ACTION`, meaning:
- Service-layer must delete in correct order (leaf-to-root)
- A migration should add `ON DELETE CASCADE` to all tenant-owned FKs as a safety net

### Required Deletion Order (Bottom-Up)

1. StorySkill (by tenant_id)
2. ExperienceProjectSkill (by tenant_id)
3. Story (by tenant_id)
4. ExperienceProject (by tenant_id)
5. ChatMessage (by tenant_id)
6. ChatSession (by tenant_id)
7. LinkedInSectionScore (by tenant_id)
8. LinkedInAnalysis (by tenant_id)
9. ExportHistory (by tenant_id)
10. ContentEmbedding (by tenant_id)
11. UserSkill (by tenant_id)
12. Education (by tenant_id)
13. JobExperience (by tenant_id)
14. CoinTransaction (via wallet -> tenant_id)
15. FreeTierUsage (by tenant_id)
16. CoinWallet (by tenant_id)
17. Profile (by tenant_id)
18. User (by tenant_id)
19. Tenant (by id)

---

## Hard Delete vs. Soft Delete Decision

**Decision: Hard Delete**

| Factor | Hard Delete | Soft Delete |
|--------|-------------|-------------|
| GDPR compliance | Fully compliant | Data still exists, can be subpoenaed |
| Simplicity | Simple DELETE | Requires `deleted_at IS NULL` filters everywhere |
| Storage | Frees space | Dead data accumulates |
| E2E cleanup | Clean, no residue | Soft-deleted rows remain |
| Undo | None | Grace period possible |

GDPR Article 17 requires actual erasure. Hard delete is simpler, cleaner for tests, and truly removes PII. A grace period is an optional Phase 2 enhancement.

---

## API Design

### Primary Endpoint: Delete Own Account

```
DELETE /api/v1/account
Authorization: Bearer <jwt>
Content-Type: application/json

Request Body:
{
  "confirmation": "DELETE MY ACCOUNT"
}

Response 200:
{
  "message": "Account and all associated data have been permanently deleted.",
  "deletedCounts": {
    "profiles": 1,
    "jobExperiences": 3,
    "educations": 2,
    "userSkills": 15,
    "stories": 8,
    "chatSessions": 2,
    "chatMessages": 47,
    "coinTransactions": 12
  }
}

Response 400:
{
  "error": "CONFIRMATION_REQUIRED",
  "message": "Provide confirmation text 'DELETE MY ACCOUNT' to proceed."
}
```

### Internal Test Cleanup Endpoint (dev/test profiles only)

```
DELETE /api/internal/test/cleanup
X-Test-Api-Key: <configured-key>
Content-Type: application/json

Request Body:
{
  "emailPattern": "e2e-%@interview-me.dev"
}

Response 200:
{
  "deletedAccounts": 5,
  "message": "Test accounts matching pattern have been deleted."
}
```

This endpoint:
- Only available when `spring.profiles.active` includes `dev` or `test`
- Requires a pre-shared API key (`TEST_CLEANUP_API_KEY` env var)
- Accepts an email pattern to match test users
- Calls the same `AccountDeletionService` internally

---

## Security Considerations

1. **Authentication required:** Only the authenticated user can delete their own account via JWT
2. **Confirmation text:** Prevents accidental deletion
3. **Profile-gated test endpoint:** `@Profile({"dev", "test"})` -- never available in production
4. **Test API key:** Internal endpoint requires pre-shared key
5. **Rate limiting:** Max 1 deletion request per minute per user
6. **Audit logging:** Log tenant ID, user ID, timestamp, and deleted counts (NOT the deleted PII)
7. **No cross-tenant deletion:** User can only delete their own tenant's data

---

## GDPR Compliance

1. **Right to Erasure (Article 17):** Hard delete removes all personal data
2. **Timeliness:** Deletion is immediate, within the 30-day requirement
3. **Completeness:** All 19 tenant-owned tables are covered
4. **Confirmation:** API response confirms deletion counts
5. **Export files:** Physical files (PDFs) on disk/S3 are deleted as part of cleanup
6. **Third-party:** Currently no third-party stores PII; when Stripe is added, deletion must extend to it

---

## E2E Test Integration

### Recommended: afterAll Hook Per Test File

```typescript
test.afterAll(async ({ request }) => {
  if (!testUserToken) return; // Nothing to clean up
  await request.delete('/api/v1/account', {
    headers: { Authorization: `Bearer ${testUserToken}` },
    data: { confirmation: 'DELETE MY ACCOUNT' },
  });
});
```

### Safety Net: Global Teardown

```typescript
// playwright.config.ts
globalTeardown: './src/global-teardown.ts',

// src/global-teardown.ts
export default async function globalTeardown() {
  await fetch('http://localhost:8080/api/internal/test/cleanup', {
    method: 'DELETE',
    headers: {
      'X-Test-Api-Key': process.env.TEST_CLEANUP_API_KEY!,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ emailPattern: 'e2e-%@interview-me.dev' }),
  });
}
```

### Why Both?

- `afterAll` hooks: Exercise the real deletion endpoint, test it as part of E2E coverage
- Global teardown: Catches stragglers from crashed tests where `afterAll` never ran

---

## Schema Migration Required

New Liquibase migration to add `ON DELETE CASCADE` to all tenant-owned FK constraints. This acts as a database-level safety net for the service-layer deletion.

**Tables needing CASCADE on tenant_id FK:**
profile, job_experience, education, user_skill, experience_project, story, story_skill, experience_project_skill, chat_session, chat_message, linkedin_analysis, linkedin_section_score, export_history, coin_wallet, free_tier_usage, content_embedding

**Additional cascades needed:**
- chat_message.session_id -> chat_session.id (CASCADE)
- linkedin_section_score.analysis_id -> linkedin_analysis.id (CASCADE)
- story.experience_project_id -> experience_project.id (CASCADE)
- experience_project.job_experience_id -> job_experience.id (CASCADE)
- experience_project_skill.experience_project_id -> experience_project.id (CASCADE)
- story_skill.story_id -> story.id (CASCADE)
- coin_transaction.wallet_id -> coin_wallet.id (CASCADE)
- job_experience.profile_id -> profile.id (CASCADE)
- education.profile_id -> profile.id (CASCADE)
- user_skill.profile_id -> profile.id (CASCADE)
- profile.user_id -> users.id (CASCADE)

---

## Success Criteria

1. **Account deletion works end-to-end:** Calling `DELETE /api/v1/account` removes all data for the tenant across all 19 tables
2. **GDPR compliant:** No PII remains in the database after deletion
3. **E2E tests clean up:** All E2E test files use `afterAll` hooks to delete test accounts
4. **Global teardown works:** Stale test data from crashed runs is cleaned by global teardown
5. **Security:** Test endpoint is inaccessible in production profile
6. **Verification:** A query for the deleted tenant_id across all tables returns zero rows

---

## Assumptions

1. Each tenant currently has exactly one user (single-user tenants). Multi-user tenant deletion can be enhanced later.
2. The `Skill` and `ExportTemplate` tables are truly global and never need per-tenant cleanup.
3. Physical export files are stored at `./exports` (as per config) and can be deleted by filename pattern.
4. No external systems (Stripe, SendGrid) currently store user PII.

---

## Out of Scope

1. **Soft-delete grace period** (30-day undo window)
2. **Email confirmation** before deletion
3. **Admin panel** for admin-initiated account deletion
4. **Data export** ("Download my data" -- GDPR Article 20)
5. **Stripe cleanup** (no Stripe integration yet)
6. **UI "Delete Account" page** (backend API only for now)

---

## Revision History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0.0 | 2026-02-28 | Initial specification | Claude (researcher) |
