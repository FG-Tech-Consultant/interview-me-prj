# Plan: Account Deletion & E2E Test Data Cleanup

**Feature ID:** 013
**Plan Version:** 1.0.0
**Date:** 2026-02-28
**Status:** Draft

---

## 1. Technical Design

### 1.1 Architecture Overview

The deletion feature spans three layers:

```
E2E Tests (Playwright)
    |
    | afterAll: DELETE /api/v1/account
    | globalTeardown: DELETE /api/internal/test/cleanup
    v
Spring Boot Backend
    |
    +-- AccountController (REST endpoints)
    +-- TestCleanupController (@Profile("dev","test") only)
    +-- AccountDeletionService (orchestrates deletion)
    |     +-- Uses bulk DELETE queries per table
    |     +-- Ordered leaf-to-root deletion
    |     +-- Single @Transactional boundary
    +-- AccountDeletionRepository (native SQL queries)
    v
PostgreSQL
    +-- ON DELETE CASCADE FKs (safety net migration)
```

### 1.2 Key Design Decisions

**1. Service-layer ordered deletion (not JPA cascade):**
JPA `CascadeType.REMOVE` on `@OneToMany` loads all entities into memory before deleting, generating N+1 queries per association. For a tenant with hundreds of records, this is wasteful. Instead, use native bulk `DELETE FROM table WHERE tenant_id = ?` queries executed in the correct FK order. This is O(number-of-tables), not O(number-of-rows).

**2. Single transaction:**
The entire deletion is wrapped in `@Transactional`. If any step fails, the whole operation rolls back. This prevents partial deletions that leave orphaned data.

**3. Database CASCADE as safety net:**
A Liquibase migration adds `ON DELETE CASCADE` to all tenant-owned FKs. This means if the service-layer deletion misses a table (e.g., a new table is added later), the database will still cascade-delete when the tenant row is removed. Defense in depth.

**4. No soft delete:**
Hard delete only. GDPR requires actual erasure. Soft delete adds complexity (filtering everywhere) without clear benefit for MVP.

---

## 2. Component Design

### 2.1 AccountController

**Location:** `backend/src/main/java/com/interviewme/controller/AccountController.java`

```java
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountDeletionService accountDeletionService;

    @DeleteMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {

        String confirmation = body.get("confirmation");
        if (!"DELETE MY ACCOUNT".equals(confirmation)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "CONFIRMATION_REQUIRED",
                "message", "Provide confirmation text 'DELETE MY ACCOUNT' to proceed."
            ));
        }

        Map<String, Integer> deletedCounts = accountDeletionService.deleteAccount(
            user.getTenantId(), user.getId()
        );

        return ResponseEntity.ok(Map.of(
            "message", "Account and all associated data have been permanently deleted.",
            "deletedCounts", deletedCounts
        ));
    }
}
```

### 2.2 TestCleanupController

**Location:** `backend/src/main/java/com/interviewme/controller/TestCleanupController.java`

```java
@RestController
@RequestMapping("/api/internal/test")
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Slf4j
public class TestCleanupController {

    private final AccountDeletionService accountDeletionService;

    @Value("${test.cleanup.api-key:}")
    private String testApiKey;

    @DeleteMapping("/cleanup")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupTestAccounts(
            @RequestHeader("X-Test-Api-Key") String apiKey,
            @RequestBody Map<String, String> body) {

        if (testApiKey.isEmpty() || !testApiKey.equals(apiKey)) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "FORBIDDEN",
                "message", "Invalid test API key."
            ));
        }

        String emailPattern = body.get("emailPattern");
        int deletedCount = accountDeletionService.deleteAccountsByEmailPattern(emailPattern);

        return ResponseEntity.ok(Map.of(
            "deletedAccounts", deletedCount,
            "message", "Test accounts matching pattern have been deleted."
        ));
    }
}
```

### 2.3 AccountDeletionService

**Location:** `backend/src/main/java/com/interviewme/service/AccountDeletionService.java`

Core orchestrator. Performs bulk deletions in FK-safe order.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final AccountDeletionRepository deletionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Integer> deleteAccount(Long tenantId, Long userId) {
        log.info("Deleting account for tenant={}, user={}", tenantId, userId);

        Map<String, Integer> counts = new LinkedHashMap<>();

        // Delete in leaf-to-root order
        counts.put("storySkills", deletionRepository.deleteStorySkillsByTenantId(tenantId));
        counts.put("experienceProjectSkills", deletionRepository.deleteExperienceProjectSkillsByTenantId(tenantId));
        counts.put("stories", deletionRepository.deleteStoriesByTenantId(tenantId));
        counts.put("experienceProjects", deletionRepository.deleteExperienceProjectsByTenantId(tenantId));
        counts.put("chatMessages", deletionRepository.deleteChatMessagesByTenantId(tenantId));
        counts.put("chatSessions", deletionRepository.deleteChatSessionsByTenantId(tenantId));
        counts.put("linkedInSectionScores", deletionRepository.deleteLinkedInSectionScoresByTenantId(tenantId));
        counts.put("linkedInAnalyses", deletionRepository.deleteLinkedInAnalysesByTenantId(tenantId));
        counts.put("exportHistories", deletionRepository.deleteExportHistoriesByTenantId(tenantId));
        counts.put("contentEmbeddings", deletionRepository.deleteContentEmbeddingsByTenantId(tenantId));
        counts.put("userSkills", deletionRepository.deleteUserSkillsByTenantId(tenantId));
        counts.put("educations", deletionRepository.deleteEducationsByTenantId(tenantId));
        counts.put("jobExperiences", deletionRepository.deleteJobExperiencesByTenantId(tenantId));
        counts.put("coinTransactions", deletionRepository.deleteCoinTransactionsByTenantId(tenantId));
        counts.put("freeTierUsages", deletionRepository.deleteFreeTierUsagesByTenantId(tenantId));
        counts.put("coinWallets", deletionRepository.deleteCoinWalletsByTenantId(tenantId));
        counts.put("profiles", deletionRepository.deleteProfilesByTenantId(tenantId));
        counts.put("users", deletionRepository.deleteUsersByTenantId(tenantId));
        counts.put("tenants", deletionRepository.deleteTenantById(tenantId));

        // Delete physical export files
        deleteExportFiles(tenantId);

        log.info("Account deleted for tenant={}. Counts: {}", tenantId, counts);
        return counts;
    }

    @Transactional
    public int deleteAccountsByEmailPattern(String emailPattern) {
        // Find all users matching the pattern
        List<User> users = userRepository.findByEmailPattern(emailPattern);
        int count = 0;
        for (User user : users) {
            deleteAccount(user.getTenantId(), user.getId());
            count++;
        }
        return count;
    }

    private void deleteExportFiles(Long tenantId) {
        // Delete export files from disk/S3 for this tenant
        // Implementation depends on storage backend
    }
}
```

### 2.4 AccountDeletionRepository

**Location:** `backend/src/main/java/com/interviewme/repository/AccountDeletionRepository.java`

Uses `@Modifying` + `@Query` with native SQL for bulk deletes:

```java
@Repository
public interface AccountDeletionRepository extends JpaRepository<Tenant, Long> {

    @Modifying
    @Query(value = "DELETE FROM story_skill WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteStorySkillsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM experience_project_skill WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteExperienceProjectSkillsByTenantId(@Param("tenantId") Long tenantId);

    // ... similar methods for all 19 tables ...

    @Modifying
    @Query(value = "DELETE FROM tenant WHERE id = :tenantId", nativeQuery = true)
    int deleteTenantById(@Param("tenantId") Long tenantId);
}
```

Each method returns `int` (rows deleted), which is aggregated into the response.

---

## 3. Liquibase Migration

### 3.1 Migration File

**File:** `sboot/src/main/resources/db/changelog/20260228120000-add-cascade-delete-to-tenant-fks.xml`

This migration drops existing FK constraints and re-creates them with `ON DELETE CASCADE`. This is safe because:
- Data integrity is maintained (same FK, just with CASCADE behavior added)
- No data is modified
- The constraint names remain the same

**Approach:** For each FK, use `dropForeignKeyConstraint` + `addForeignKeyConstraint` with `onDelete="CASCADE"`.

### 3.2 Tables Affected

All FKs referencing `tenant.id`, plus child-to-parent FKs:

| FK Name | Table | Column | References | Cascade |
|---------|-------|--------|------------|---------|
| fk_profile_tenant | profile | tenant_id | tenant.id | CASCADE |
| fk_profile_user | profile | user_id | users.id | CASCADE |
| fk_job_experience_tenant | job_experience | tenant_id | tenant.id | CASCADE |
| fk_job_experience_profile | job_experience | profile_id | profile.id | CASCADE |
| fk_education_tenant | education | tenant_id | tenant.id | CASCADE |
| fk_education_profile | education | profile_id | profile.id | CASCADE |
| fk_user_skill_tenant | user_skill | tenant_id | tenant.id | CASCADE |
| fk_user_skill_profile | user_skill | profile_id | profile.id | CASCADE |
| fk_experience_project_tenant | experience_project | tenant_id | tenant.id | CASCADE |
| fk_experience_project_job_experience | experience_project | job_experience_id | job_experience.id | CASCADE |
| fk_story_tenant | story | tenant_id | tenant.id | CASCADE |
| fk_story_experience_project | story | experience_project_id | experience_project.id | CASCADE |
| fk_story_skill_tenant | story_skill | tenant_id | tenant.id | CASCADE |
| fk_story_skill_story | story_skill | story_id | story.id | CASCADE |
| fk_exp_project_skill_tenant | experience_project_skill | tenant_id | tenant.id | CASCADE |
| fk_exp_project_skill_project | experience_project_skill | experience_project_id | experience_project.id | CASCADE |
| fk_exp_project_skill_user_skill | experience_project_skill | user_skill_id | user_skill.id | CASCADE |
| fk_story_skill_user_skill | story_skill | user_skill_id | user_skill.id | CASCADE |
| fk_chat_session_tenant | chat_session | tenant_id | tenant.id | CASCADE |
| fk_chat_session_profile | chat_session | profile_id | profile.id | CASCADE |
| fk_chat_message_tenant | chat_message | tenant_id | tenant.id | CASCADE |
| fk_chat_message_session | chat_message | session_id | chat_session.id | CASCADE |
| fk_linkedin_analysis_tenant | linkedin_analysis | tenant_id | tenant.id | CASCADE |
| fk_linkedin_analysis_profile | linkedin_analysis | profile_id | profile.id | CASCADE |
| fk_linkedin_section_score_tenant | linkedin_section_score | tenant_id | tenant.id | CASCADE |
| fk_linkedin_section_score_analysis | linkedin_section_score | analysis_id | linkedin_analysis.id | CASCADE |
| fk_export_history_tenant | export_history | tenant_id | tenant.id | CASCADE |
| fk_export_history_profile | export_history | profile_id | profile.id | CASCADE |
| fk_coin_wallet_tenant (new name) | coin_wallet | tenant_id | tenant.id | CASCADE |
| fk_free_tier_usage_tenant (new name) | free_tier_usage | tenant_id | tenant.id | CASCADE |
| fk_content_embedding_tenant | content_embedding | tenant_id | tenant.id | CASCADE |

---

## 4. Security Configuration

### 4.1 SecurityFilterChain Updates

The `DELETE /api/v1/account` endpoint requires authentication (existing JWT filter handles this).

The `DELETE /api/internal/test/cleanup` endpoint is excluded from JWT auth but protected by API key header validation in the controller.

Add to `SecurityConfig.java`:
```java
// Internal test endpoints - no JWT required (API key auth in controller)
.requestMatchers("/api/internal/test/**").permitAll()
```

### 4.2 Application Configuration

Add to `application.yml`:
```yaml
test:
  cleanup:
    api-key: ${TEST_CLEANUP_API_KEY:}
```

Add to `application-dev.yml`:
```yaml
test:
  cleanup:
    api-key: test-cleanup-secret-key-dev
```

---

## 5. E2E Test Changes

### 5.1 Add Cleanup to Test Helpers

**File:** `interview-me-test-prj/src/utils/helpers.ts`

Add a `deleteAccount` helper:
```typescript
export async function deleteAccountViaAPI(
  request: APIRequestContext,
  token: string
): Promise<void> {
  await request.delete('/api/v1/account', {
    headers: { Authorization: `Bearer ${token}` },
    data: { confirmation: 'DELETE MY ACCOUNT' },
  });
}
```

### 5.2 Add afterAll to Each Test File

Each test file that creates a user should call `deleteAccountViaAPI` in its `afterAll` hook:

```typescript
let testToken: string;

test.beforeAll(async ({ request }) => {
  testToken = await registerViaAPI(request, uniqueEmail(), 'TestPassword123!', 'Test Tenant');
});

test.afterAll(async ({ request }) => {
  if (testToken) {
    await deleteAccountViaAPI(request, testToken);
  }
});
```

### 5.3 Add Global Teardown

**File:** `interview-me-test-prj/src/global-teardown.ts`

Safety net that cleans up any remaining test accounts:
```typescript
export default async function globalTeardown() {
  const apiKey = process.env.TEST_CLEANUP_API_KEY || 'test-cleanup-secret-key-dev';
  await fetch('http://localhost:8080/api/internal/test/cleanup', {
    method: 'DELETE',
    headers: {
      'X-Test-Api-Key': apiKey,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ emailPattern: 'e2e-%@interview-me.dev' }),
  });
}
```

### 5.4 Update playwright.config.ts

```typescript
export default defineConfig({
  // ... existing config ...
  globalTeardown: './src/global-teardown.ts',
});
```

---

## 6. Testing Strategy

### 6.1 Unit Tests

- `AccountDeletionServiceTest`: Mock repository, verify deletion order and counts
- `AccountControllerTest`: Test confirmation validation, auth requirements

### 6.2 Integration Tests

- `AccountDeletionIntegrationTest`: Create full tenant data, delete, verify all tables are empty
- `TestCleanupControllerTest`: Verify profile gating, API key validation, email pattern matching

### 6.3 E2E Tests

- The E2E tests themselves validate the deletion endpoint by using it in `afterAll`
- A dedicated `account-deletion.spec.ts` test can verify the full flow:
  1. Register user
  2. Create profile, skills, jobs
  3. Call DELETE /api/v1/account
  4. Verify login fails
  5. Verify public profile returns 404

---

## 7. Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Accidental deletion of real account | High | Confirmation text required |
| Test endpoint exposed in production | High | `@Profile({"dev","test"})` annotation |
| Missing table in deletion order | Medium | ON DELETE CASCADE as DB-level safety net |
| Deletion timeout for large tenants | Low | Bulk DELETE is fast; set transaction timeout |
| New tables added without updating deletion service | Medium | Integration test verifies all tables empty |

---

## 8. Implementation Order

1. Liquibase migration (add ON DELETE CASCADE)
2. AccountDeletionRepository (native queries)
3. AccountDeletionService (orchestration)
4. AccountController (REST endpoint)
5. TestCleanupController (internal endpoint)
6. Security config updates
7. Unit + integration tests
8. E2E test helpers + afterAll hooks
9. Global teardown
10. Playwright config update

---

## Revision History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0.0 | 2026-02-28 | Initial plan | Claude (researcher) |
