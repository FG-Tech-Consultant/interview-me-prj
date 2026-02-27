# Feature Specification: User Account Public Slug

**Feature ID:** 012-user-account-public-slug
**Status:** Draft
**Created:** 2026-02-27
**Last Updated:** 2026-02-27
**Version:** 1.0.0

---

## Overview

### Problem Statement

When users create a new account and profile, no public slug is assigned automatically. Users must manually set a slug before their public profile URL becomes accessible. This creates friction during onboarding and means new profiles are not publicly reachable until the user discovers and configures the slug setting. Additionally, the current slug update mechanism has no cost associated with it, allowing unlimited free changes and undermining the platform's coin-based monetization model.

### Feature Summary

This feature adds two capabilities: (1) automatic slug generation on profile creation based on the user's full name, ensuring every profile is immediately accessible via a public URL, and (2) coin-based pricing for slug changes, where the first change is free and subsequent changes cost coins. This integrates with the existing slug infrastructure (validation, availability checking, public profile routing) and the CoinWalletService billing system.

### Target Users

- **Owner/Candidate** - Users creating accounts who benefit from an auto-generated public URL
- **Owner/Candidate** - Users who want to customize their slug and understand the coin cost

---

## Constitution Compliance

**Applicable Principles:**
- **Principle 1: Simplicity First** - Builds on existing slug infrastructure (SlugValidator, ProfileService.updateSlug, SlugSettingsSection). No new entities or complex patterns introduced.
- **Principle 3: Modern Java Standards** - Uses existing Java service patterns; no new language features required.
- **Principle 4: Data Sovereignty and Persistence** - Adds `slug_change_count` column to Profile via Liquibase migration. Tenant isolation already enforced.
- **Principle 7: Security and Credential Management** - Slug validation and reserved word checking already implemented. No new security concerns.
- **Principle 9: Freemium Model and Coin-Based Monetization** - First slug change is free (onboarding-friendly); subsequent changes cost coins, integrating with CoinWalletService.
- **Principle 10: Full-Stack Modularity** - Changes confined to ProfileService (backend), SlugSettingsSection (frontend), and BillingProperties (config).
- **Principle 11: Database Schema Evolution** - New migration uses timestamp-based naming convention.

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: Auto-Generated Slug on Profile Creation

**Actor:** New user creating their first profile
**Goal:** Have an immediately accessible public profile URL without manual configuration
**Preconditions:** User has registered and is creating their profile with a full name

**Steps:**
1. User fills in the Create Profile form including full name (e.g., "Fernando Gomes")
2. System creates the profile and auto-generates a slug from the full name (e.g., "fernando-gomes")
3. If "fernando-gomes" is already taken, system appends a numeric suffix (e.g., "fernando-gomes-1", "fernando-gomes-2")
4. Profile is saved with the generated slug
5. User's public profile is immediately accessible at `/p/fernando-gomes`

**Success Criteria:**
- Every new profile has a non-null, unique, valid slug
- Slug is derived from the user's full name
- Slug follows existing validation rules (3-50 chars, lowercase alphanumeric + hyphens, no consecutive hyphens)
- Collisions are resolved by appending numeric suffixes

#### Scenario 2: Free First Slug Change

**Actor:** User who wants to customize their auto-generated slug
**Goal:** Change the slug for free the first time
**Preconditions:** User has a profile with an auto-generated slug and has never changed it (slug_change_count = 0)

**Steps:**
1. User navigates to Profile Editor > Public Profile section
2. User sees their current slug and a note that the first change is free
3. User enters a new desired slug
4. System checks availability (existing functionality)
5. User clicks Save
6. System updates the slug without charging coins
7. System increments slug_change_count to 1

**Success Criteria:**
- First change completes without coin deduction
- UI clearly indicates the first change is free
- slug_change_count is incremented

#### Scenario 3: Paid Subsequent Slug Change

**Actor:** User who wants to change their slug again after the free change
**Goal:** Change the slug by spending coins
**Preconditions:** User has changed their slug at least once (slug_change_count >= 1) and has sufficient coin balance

**Steps:**
1. User navigates to Profile Editor > Public Profile section
2. User sees their current slug and the cost for changing (e.g., "Changing your slug costs X coins")
3. User enters a new desired slug
4. System checks availability
5. User clicks Save
6. System verifies sufficient coin balance
7. System deducts coins via CoinWalletService.spend()
8. System updates the slug and increments slug_change_count
9. UI confirms change and shows updated coin balance

**Success Criteria:**
- Coins are deducted before slug is updated
- Transaction is recorded with RefType.SLUG_CHANGE
- If insufficient balance, user sees clear error with current balance and required amount
- UI shows cost before the user confirms

#### Scenario 4: Insufficient Balance for Slug Change

**Actor:** User who wants to change their slug but lacks coins
**Goal:** Understand why the change cannot be made and how to get coins
**Preconditions:** User has slug_change_count >= 1 and insufficient coin balance

**Steps:**
1. User navigates to slug settings
2. User enters a new slug
3. Save button is disabled or shows cost warning
4. If user attempts to save, system returns "Insufficient balance" error
5. UI displays current balance vs required cost

**Success Criteria:**
- Clear error message with balance details
- No partial state change (slug remains unchanged, no coins deducted)

### Edge Cases

- **Name produces invalid slug**: Names with only special characters or very short names (e.g., "Li") - system must handle gracefully by padding or generating a fallback slug
- **All numeric suffixes exhausted**: Extremely unlikely, but system should handle by trying random suffixes after sequential ones fail
- **Concurrent slug claims**: Two users trying to claim the same slug simultaneously - handled by existing unique constraint on slug column
- **User changes name after slug generation**: Slug is NOT automatically updated; user must manually change it (possibly using coins)
- **Reserved word collision**: If user's name produces a reserved slug (e.g., "Admin User" -> "admin"), system appends suffix

---

## Functional Requirements

### Core Capabilities

**REQ-001:** Auto-generate slug on profile creation
- **Description:** When `ProfileService.createProfile()` is called, the system MUST generate a slug from the user's full name if no slug is provided. The slug is generated by lowercasing the full name, replacing spaces and special characters with hyphens, and removing invalid characters. If the generated slug is already taken or reserved, append numeric suffixes (-1, -2, ...) until a unique valid slug is found.
- **Acceptance Criteria:**
  - "Fernando Gomes" produces "fernando-gomes"
  - "Jean-Pierre Dupont" produces "jean-pierre-dupont"
  - "Mary O'Brien" produces "mary-obrien"
  - If "fernando-gomes" is taken, "fernando-gomes-1" is tried, then "fernando-gomes-2", etc.
  - Reserved slugs (from SlugValidator.RESERVED_SLUGS) are skipped and suffixed
  - Generated slug passes SlugValidator.isValidSlug()
  - Maximum 100 suffix attempts before falling back to null (user must set manually)

**REQ-002:** Track slug change count on Profile
- **Description:** Add a `slug_change_count` integer field to the Profile entity, defaulting to 0. This counter is incremented each time the user explicitly changes their slug (not on auto-generation). The auto-generated slug during profile creation does NOT count as a change.
- **Acceptance Criteria:**
  - New profiles have slug_change_count = 0
  - Each successful slug update via updateSlug() increments the count by 1
  - Counter is persisted and survives application restarts

**REQ-003:** First slug change is free
- **Description:** When a user changes their slug and their slug_change_count is 0, the change is performed without charging coins. After the free change, slug_change_count becomes 1.
- **Acceptance Criteria:**
  - User with slug_change_count = 0 can change slug without coins
  - After the free change, slug_change_count = 1
  - No CoinTransaction is created for the free change

**REQ-004:** Subsequent slug changes cost coins
- **Description:** When a user changes their slug and their slug_change_count >= 1, the system MUST charge coins via CoinWalletService.spend(). The cost is configurable via BillingProperties (key: `SLUG_CHANGE`). If the user has insufficient balance, the change is rejected with an InsufficientBalanceException.
- **Acceptance Criteria:**
  - CoinWalletService.spend() is called with RefType.SLUG_CHANGE
  - Coins are deducted BEFORE the slug is updated (fail-fast on insufficient balance)
  - Transaction description includes the new slug value
  - Configurable cost via `billing.costs.SLUG_CHANGE` property (default: 5 coins)

**REQ-005:** Add SLUG_CHANGE to RefType enum
- **Description:** Add `SLUG_CHANGE` value to the RefType enum in the billing module so slug change transactions can be tracked and filtered.
- **Acceptance Criteria:**
  - RefType.SLUG_CHANGE exists in the enum
  - Slug change transactions appear in transaction history with this type

**REQ-006:** Expose slug change cost and status in API
- **Description:** The profile API response and/or a dedicated endpoint must communicate: (a) whether the next slug change is free, (b) the coin cost if not free, and (c) the current slug_change_count. This enables the frontend to display accurate pricing information.
- **Acceptance Criteria:**
  - ProfileResponse includes slug_change_count
  - Frontend can determine if next change is free (slug_change_count == 0)
  - Slug change cost is available via existing getFeatureCosts() endpoint (key: SLUG_CHANGE)

### User Interface Requirements

**REQ-UI-001:** Display slug change cost in SlugSettingsSection
- **Description:** The existing SlugSettingsSection component must show whether the next slug change is free or costs coins. If paid, display the cost and current balance. Show a confirmation step before paid changes.
- **Acceptance Criteria:**
  - When slug_change_count == 0: show "First change is free" badge/label
  - When slug_change_count >= 1: show "Changing your slug costs X coins" with current balance
  - Save button label changes to "Save (Free)" or "Save (X coins)" accordingly
  - If insufficient balance: Save button disabled with tooltip explaining why

### Data Requirements

**REQ-DATA-001:** Add slug_change_count column to profile table
- **Description:** Create a Liquibase migration adding `slug_change_count` (INTEGER, NOT NULL, DEFAULT 0) to the `profile` table. Existing profiles get default value 0.
- **Acceptance Criteria:**
  - Column exists with correct type and default
  - Existing profiles unaffected (default 0)
  - Migration is reversible (rollback drops column)

---

## Success Criteria

1. **Auto-generation works on profile creation:** Every new profile created via the API has a non-null, unique, valid slug derived from the user's full name.
   - Measurement: Create profiles with various names and verify slugs are generated correctly

2. **Coin integration works for slug changes:** After the free first change, subsequent slug changes deduct coins and create proper transactions.
   - Measurement: Change slug twice; verify first is free, second costs coins and appears in transaction history

3. **UI communicates cost clearly:** Users see whether their next slug change is free or paid before attempting the change.
   - Measurement: UI displays correct pricing info based on slug_change_count

4. **Collision handling is robust:** Auto-generated slugs handle name collisions gracefully with numeric suffixes.
   - Measurement: Create two profiles with the same name; verify both get unique slugs

---

## Key Entities

### Profile (existing, modified)

**New Attributes:**
- `slug_change_count` (INTEGER, NOT NULL, DEFAULT 0): Number of times the user has explicitly changed their slug

**Existing Attributes (unchanged):**
- `slug` (VARCHAR 50, UNIQUE): The public profile URL slug

### CoinTransaction (existing, unchanged)

**New RefType Value:**
- `SLUG_CHANGE`: Used when coins are spent on changing a slug

---

## Dependencies

### Internal Dependencies

- **SlugValidator** (existing) - Validation, reserved words, normalization
- **ProfileService.updateSlug()** (existing) - Will be modified to integrate coin charging
- **CoinWalletService.spend()** (existing) - Used for charging coins on paid slug changes
- **BillingProperties** (existing) - Configuration for slug change cost
- **SlugSettingsSection.tsx** (existing) - Will be modified to show pricing
- **ProfileMapper / ProfileResponse** (existing) - Will be extended with slug_change_count

### External Dependencies

- None

---

## Assumptions

1. The full name field is always populated when creating a profile (required field in CreateProfileRequest)
2. The default slug change cost of 5 coins is reasonable; can be adjusted via configuration
3. The first slug change being free is a permanent policy, not a promotional offer
4. Slug auto-generation on profile creation is not counted as a "change" for billing purposes
5. The existing SlugValidator rules (3-50 chars, no consecutive hyphens, no reserved words) apply to auto-generated slugs

---

## Out of Scope

1. **Automatic slug update when user changes their name** - Slug remains static; user must manually change it
2. **Slug history / redirect from old slugs** - Old slugs become immediately available for others to claim
3. **Custom vanity slug marketplace** - No trading, auctioning, or premium slug pricing
4. **Slug expiration or reclamation** - Slugs remain assigned indefinitely
5. **Batch slug generation for existing profiles** - Only new profiles get auto-generated slugs; existing profiles without slugs are not backfilled by this feature

---

## Error Handling

### Error Scenarios

**ERROR-001:** Slug generation fails (all suffixes exhausted)
- **User Experience:** Profile creation succeeds but slug is set to null; user sees a notification to set their slug manually
- **Recovery Path:** User navigates to Profile Editor > Public Profile and sets slug manually

**ERROR-002:** Insufficient coins for slug change
- **User Experience:** Clear error message: "You need X coins to change your slug. Current balance: Y coins."
- **Recovery Path:** User purchases more coins via billing page, then retries

**ERROR-003:** Concurrent slug claim (race condition)
- **User Experience:** "This slug was just claimed by another user. Please try a different one."
- **Recovery Path:** User enters a different slug; coins are NOT deducted (fail happens at DB constraint level before commit)

---

## Testing Scope

### Test Categories

**Functional Testing:**
- Slug auto-generation from various name formats (accented characters, hyphens, apostrophes, spaces)
- Collision resolution with numeric suffixes
- Free first slug change (slug_change_count = 0)
- Paid slug change with sufficient balance
- Paid slug change with insufficient balance (rejected)
- slug_change_count incremented correctly
- CoinTransaction created with correct RefType.SLUG_CHANGE

**User Acceptance Testing:**
- New user creates profile and sees auto-generated slug in profile editor
- User changes slug for free the first time
- User sees cost displayed before second slug change
- User with insufficient balance sees clear error

**Edge Case Testing:**
- Names producing very short slugs (< 3 chars) - system pads or uses fallback
- Names with only special characters
- Names matching reserved slugs exactly
- Very long names (slug truncation to 50 chars)
- Concurrent profile creation with identical names

---

## Notes

- The existing `SlugValidator.normalizeSlug()` only lowercases and trims. A new slug generation method is needed that also handles spaces, special characters, and diacritics (e.g., using java.text.Normalizer to strip accents).
- The `ProfileService.createProfile()` method currently does not set a slug. This is the primary integration point for auto-generation.
- The `ProfileService.updateSlug()` method currently does not interact with billing. This is where coin charging logic will be added.
- Consider adding `SLUG_CHANGE` cost to the default BillingProperties and the `billing.costs` configuration in application.yml.

---

## Revision History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0.0   | 2026-02-27 | Initial specification | AI Agent |
