# Tasks: User Account Public Slug

**Feature ID:** 012
**Created:** 2026-02-27

---

## Task List

### TASK-1: Create SlugGenerator utility
- **File:** `backend/src/main/java/com/interviewme/util/SlugGenerator.java`
- **Type:** New file
- **Dependencies:** None
- **Description:** Static utility class to generate slug from full name. Methods: `generateSlug(String fullName)` and `generateUniqueSlug(String fullName, Function<String, Boolean> existsChecker)`.

### TASK-2: Add SLUG_CHANGE to RefType enum
- **File:** `billing/src/main/java/com/interviewme/billing/model/RefType.java`
- **Type:** Modify
- **Dependencies:** None
- **Description:** Add `SLUG_CHANGE` enum value.

### TASK-3: Add SLUG_CHANGE cost to application.yml
- **File:** `sboot/src/main/resources/application.yml`
- **Type:** Modify
- **Dependencies:** None
- **Description:** Add `SLUG_CHANGE: 5` to `billing.costs` map.

### TASK-4: Update SlugCheckResponse DTO
- **File:** `backend/src/main/java/com/interviewme/dto/publicprofile/SlugCheckResponse.java`
- **Type:** Modify
- **Dependencies:** None
- **Description:** Add `changeCost` (int) field to record.

### TASK-5: Auto-generate slug in ProfileService.createProfile
- **File:** `backend/src/main/java/com/interviewme/service/ProfileService.java`
- **Type:** Modify
- **Dependencies:** TASK-1
- **Description:** In createProfile(), generate unique slug from fullName before saving.

### TASK-6: Charge coins in ProfileService.updateSlug
- **File:** `backend/src/main/java/com/interviewme/service/ProfileService.java`
- **Type:** Modify
- **Dependencies:** TASK-2, TASK-3
- **Description:** If profile already has a slug, call CoinWalletService.spend() before updating. Add CoinWalletService dependency.

### TASK-7: Update PublicProfileService.checkSlugAvailability
- **File:** `backend/src/main/java/com/interviewme/service/PublicProfileService.java`
- **Type:** Modify
- **Dependencies:** TASK-4, TASK-3
- **Description:** Include changeCost from BillingProperties in SlugCheckResponse.

### TASK-8: Update frontend SlugCheckResponse type
- **File:** `frontend/src/types/publicProfile.ts`
- **Type:** Modify
- **Dependencies:** TASK-4
- **Description:** Add `changeCost: number` to SlugCheckResponse interface.

### TASK-9: Update SlugSettingsSection with coin integration
- **File:** `frontend/src/components/profile/SlugSettingsSection.tsx`
- **Type:** Modify
- **Dependencies:** TASK-8
- **Description:** Show cost, check balance, disable save if insufficient. Use existing billing hooks.
