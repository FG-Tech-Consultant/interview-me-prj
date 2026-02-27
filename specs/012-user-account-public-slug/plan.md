# Implementation Plan: User Account Public Slug

**Feature ID:** 012
**Created:** 2026-02-27

---

## Summary

Enhance existing slug system with auto-generation on profile creation and coin-based changes. Minimal changes needed since slug infrastructure already exists.

## Changes Required

### 1. Backend - SlugGenerator Utility (NEW)

**File:** `backend/src/main/java/com/interviewme/util/SlugGenerator.java`

Static utility to generate a slug from a full name:
- Lowercase, trim, replace spaces with hyphens
- Remove non-alphanumeric characters (except hyphens)
- Collapse consecutive hyphens
- Truncate to max 50 chars
- Handle edge cases (null, empty, very short names → generate random slug)

### 2. Backend - ProfileService.createProfile (MODIFY)

**File:** `backend/src/main/java/com/interviewme/service/ProfileService.java`

In `createProfile()`, after building the Profile entity and before saving:
- Generate slug from `fullName` using SlugGenerator
- Check uniqueness via `profileRepository.existsBySlug()`
- If taken, append `-1`, `-2`, etc. until unique (max 10 attempts, then random suffix)
- Set slug on profile entity

### 3. Backend - ProfileService.updateSlug (MODIFY)

**File:** `backend/src/main/java/com/interviewme/service/ProfileService.java`

Modify `updateSlug()` to charge coins:
- If profile already has a slug (not null), charge coins via CoinWalletService
- Inject CoinWalletService dependency
- Get tenantId, call `coinWalletService.spend()` with RefType.SLUG_CHANGE
- If insufficient balance, InsufficientBalanceException propagates (returns 402)

### 4. Backend - RefType Enum (MODIFY)

**File:** `billing/src/main/java/com/interviewme/billing/model/RefType.java`

Add `SLUG_CHANGE` value.

### 5. Backend - BillingProperties / application.yml (MODIFY)

Add default cost:
```yaml
billing:
  costs:
    SLUG_CHANGE: 5
```

### 6. Backend - SlugCheckResponse DTO (MODIFY)

Add `changeCost` field to the existing SlugCheckResponse record.

### 7. Backend - PublicProfileService.checkSlugAvailability (MODIFY)

Include `changeCost` from BillingProperties in the response.

### 8. Frontend - SlugSettingsSection (MODIFY)

- Fetch coin balance (from existing billing hooks)
- Display cost warning when changing existing slug
- Disable save if insufficient coins
- Update success message to mention coin deduction

### 9. Frontend - Types (MODIFY)

Update `SlugCheckResponse` type to include `changeCost`.

---

## No Migration Needed

The `slug` column already exists on the `profile` table with a unique constraint. No schema changes required.

## Configuration

Add to `sboot/src/main/resources/application.yml`:
```yaml
billing:
  costs:
    SLUG_CHANGE: 5
```
