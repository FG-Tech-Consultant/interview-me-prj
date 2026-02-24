# Feature Specification: Coin Wallet & Billing System

**Feature ID:** 007-coin-wallet
**Status:** Draft
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Overview

### Problem Statement

The Interview Me platform needs a monetization system to sustain premium features (exports, extra AI usage, LinkedIn suggestions). Without a billing mechanism, there is no way to:
- Track and limit usage of premium features
- Monetize AI-powered capabilities (recruiter chat, exports, LinkedIn analysis)
- Enforce free-tier quotas to prevent cost overruns from LLM API calls
- Provide transparency into what users are paying for

A coin-based system provides flexible pay-as-you-go pricing, transparent cost tracking, and a foundation for future Stripe integration.

### Feature Summary

Implement a coin wallet and billing system that tracks coin balances per tenant, records all coin transactions (earn, spend, refund, purchase), enforces free-tier monthly quotas, and provides a billing page for users to view their balance and transaction history. Phase 1 focuses on manual coin grants by admin (no Stripe), configurable coin costs per feature, balance enforcement with optimistic locking (balance MUST NEVER go negative), and auto-refund on failed operations.

### Target Users

- **Career Professionals (Primary User):** Users spending coins on premium features (exports, extra chat messages, LinkedIn suggestions)
- **Platform Admin:** Administrators granting coins manually, managing pricing configuration, and handling refund requests
- **System (Internal):** Services that debit/credit coins when users trigger premium actions

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Standard REST CRUD pattern for wallet and transactions, straightforward service layer for balance operations, no complex payment gateway in Phase 1
- **Principle 3: Modern Java Standards** - Java 25 records for DTOs, proper use of `@Version` for optimistic locking on wallet balance
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - CoinWallet and CoinTransaction entities include `tenant_id` with automatic Hibernate filtering, one wallet per tenant
- **Principle 6: Observability and Debugging** - Structured logging for all coin transactions, metrics for coin spends per feature, audit trail via CoinTransaction table
- **Principle 7: Security, Privacy, and Credential Management** - Admin-only endpoint for coin grants, JWT-based authentication for all billing endpoints, `@Transactional` annotations for data integrity
- **Principle 8: Multi-Tenant Architecture** - Each tenant has exactly ONE CoinWallet, all transactions tenant-isolated
- **Principle 9: Freemium Model and Coin-Based Monetization** - Core principle governing this feature; coin wallet system, free-tier quotas, configurable costs, transparency, auto-refunds
- **Principle 10: Full-Stack Modularity** - `com.interviewme.billing` package with controller/service/repository/model/dto layers, frontend BillingPage and CoinBalanceBadge components
- **Principle 11: Database Schema Evolution** - Timestamp-based Liquibase migrations for coin_wallet and coin_transaction tables
- **Principle 14: Event-Driven Architecture** - Publish `CoinTransactionEvent` on every coin movement for audit trail and reactive updates

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: Admin Grants Coins to a Tenant

**Actor:** Platform administrator
**Goal:** Grant coins to a tenant for testing or customer support
**Preconditions:** Admin is authenticated with ADMIN role

**Steps:**
1. Admin calls POST `/api/admin/wallets/{tenantId}/grant` with amount and reason
2. System validates amount is positive integer
3. System finds or creates CoinWallet for the tenant
4. System creates CoinTransaction with type=EARN, amount=+N, refType=ADMIN_GRANT
5. System increments wallet balance by N using optimistic locking
6. System publishes CoinTransactionEvent
7. Admin receives 200 OK with updated wallet balance

**Success Criteria:**
- Wallet balance increases by exact amount
- CoinTransaction record created with proper audit fields
- Event published for downstream consumers

#### Scenario 2: User Views Wallet Balance and Transaction History

**Actor:** Career professional
**Goal:** Check current coin balance and see recent transactions
**Preconditions:** User is authenticated

**Steps:**
1. User navigates to Billing page
2. Frontend calls GET `/api/billing/wallet` to fetch current balance
3. Frontend calls GET `/api/billing/transactions` with pagination to fetch history
4. Page displays: current balance prominently, transaction list with type, amount, description, date
5. User can filter transactions by type (EARN, SPEND, REFUND) and date range

**Success Criteria:**
- Balance displayed accurately
- Transactions ordered by date descending (most recent first)
- Pagination works correctly (page size: 20)
- Tenant isolation: user only sees own tenant's data

#### Scenario 3: System Spends Coins for Premium Feature

**Actor:** System (triggered by user action, e.g., exporting a resume)
**Goal:** Debit coins from wallet when user uses a premium feature
**Preconditions:** User has sufficient coin balance, feature cost is configured

**Steps:**
1. User triggers premium action (e.g., "Export Resume as PDF")
2. Frontend displays cost: "This will cost 10 coins. Current balance: 50 coins."
3. User confirms action
4. Backend service calls `CoinWalletService.spend(tenantId, amount, refType, refId)`
5. Service checks balance >= amount (with optimistic locking)
6. Service creates CoinTransaction with type=SPEND, amount=-N
7. Service decrements wallet balance
8. If balance insufficient, throws InsufficientBalanceException (400 Bad Request)
9. Calling service proceeds with the premium action (e.g., generate PDF)

**Success Criteria:**
- Balance decremented by exact cost
- Transaction recorded with reference to the feature that consumed coins
- Insufficient balance prevents the action with clear error message
- Balance NEVER goes negative

#### Scenario 4: System Auto-Refunds on Failed Operation

**Actor:** System (automatic)
**Goal:** Refund coins when a paid operation fails (e.g., PDF generation error)
**Preconditions:** Coins were previously spent for the operation

**Steps:**
1. User triggers export, coins are spent (CoinTransaction with refType=EXPORT, refId=exportId)
2. Export generation fails (LLM error, timeout, etc.)
3. Calling service catches the error and calls `CoinWalletService.refund(tenantId, originalTransactionId)`
4. Service creates CoinTransaction with type=REFUND, amount=+N, refType=EXPORT_REFUND, refId=originalTransactionId
5. Service increments wallet balance
6. User sees refund in transaction history: "Refund: Resume export failed"

**Success Criteria:**
- Refund amount equals original spend amount
- Refund transaction linked to original spend transaction via refId
- Balance restored to pre-spend value
- User notified of both failure and refund

#### Scenario 5: Free-Tier Quota Check

**Actor:** System (automatic)
**Goal:** Track and enforce monthly free-tier quotas
**Preconditions:** Tenant is on free tier

**Steps:**
1. User sends recruiter chat message
2. System checks monthly usage: "How many chat messages has this tenant sent this month?"
3. If count < FREE_CHAT_MESSAGES_PER_MONTH (e.g., 50): allow for free, no coins spent
4. If count >= FREE_CHAT_MESSAGES_PER_MONTH: check coin balance and spend coins
5. If no coins available: return 402 Payment Required with message "Free quota exceeded. Purchase coins to continue."
6. Monthly counters reset on the 1st of each month

**Success Criteria:**
- Free usage tracked per tenant per month
- Quota resets monthly
- Clear distinction between free usage and paid usage in transaction history
- User notified when approaching free-tier limits (80% threshold)

### Edge Cases

- **Concurrent coin spends:** Two premium actions triggered simultaneously for same tenant (optimistic locking prevents double-spend)
- **Zero-cost features:** Some features are always free (live profile page) -- no coin check needed
- **Wallet not yet created:** First coin grant or spend auto-creates the wallet with balance 0
- **Refund for already-refunded transaction:** Prevent duplicate refunds (check if refund already exists for that refId)
- **Admin grants negative amount:** Validation rejects negative amounts
- **Integer overflow:** Wallet balance stored as BIGINT, maximum 9.2 quintillion coins (practically unlimited)
- **Month boundary:** Free quota check when user sends message at 11:59 PM on last day of month vs 12:00 AM next month

---

## Functional Requirements

### Core Capabilities

**REQ-001:** Coin Wallet Entity
- **Description:** Each tenant MUST have exactly ONE coin wallet tracking their current balance
- **Acceptance Criteria:**
  - CoinWallet includes: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL UNIQUE), balance (BIGINT NOT NULL DEFAULT 0), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), version (BIGINT DEFAULT 0 for optimistic locking)
  - Balance MUST NEVER go negative (enforced in service layer AND database CHECK constraint)
  - One wallet per tenant (unique constraint on tenant_id)
  - Wallet auto-created on first coin operation if not exists
  - Optimistic locking via @Version prevents concurrent balance corruption

**REQ-002:** Coin Transaction Entity
- **Description:** Every coin movement MUST be recorded as an immutable transaction for audit trail
- **Acceptance Criteria:**
  - CoinTransaction includes: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), wallet_id (BIGINT FK NOT NULL), type (VARCHAR 20 NOT NULL: EARN, SPEND, REFUND, PURCHASE), amount (INTEGER NOT NULL), description (VARCHAR 500), ref_type (VARCHAR 50: ADMIN_GRANT, EXPORT, CHAT_MESSAGE, LINKEDIN_DRAFT, LINKEDIN_SUGGESTION, EXPORT_REFUND), ref_id (VARCHAR 100: ID of related entity), created_at (TIMESTAMPTZ NOT NULL)
  - Transactions are immutable (no update or delete endpoints)
  - Amount is signed: positive for EARN/REFUND/PURCHASE, negative for SPEND
  - Each transaction MUST reference the wallet it belongs to
  - Transactions filterable by type, date range, ref_type

**REQ-003:** Spend Coins with Balance Enforcement
- **Description:** System MUST debit coins from wallet with guaranteed non-negative balance
- **Acceptance Criteria:**
  - Service method: `spend(tenantId, amount, refType, refId, description)` returns CoinTransaction
  - Throws `InsufficientBalanceException` if balance < amount
  - Uses optimistic locking (@Version) to prevent race conditions
  - On optimistic lock failure, retries up to 3 times before throwing error
  - Creates SPEND transaction with negative amount
  - Updates wallet balance atomically

**REQ-004:** Earn/Grant Coins
- **Description:** Admin MUST be able to grant coins to any tenant
- **Acceptance Criteria:**
  - Admin endpoint: POST `/api/admin/wallets/{tenantId}/grant`
  - Request body: `{ "amount": 100, "description": "Welcome bonus" }`
  - Amount must be positive integer (> 0)
  - Creates EARN transaction with positive amount
  - Increments wallet balance
  - Requires ADMIN role authentication

**REQ-005:** Refund Coins
- **Description:** System MUST auto-refund coins when a paid operation fails
- **Acceptance Criteria:**
  - Service method: `refund(tenantId, originalTransactionId)` returns CoinTransaction
  - Looks up original SPEND transaction by ID
  - Creates REFUND transaction with positive amount equal to original spend (absolute value)
  - Prevents duplicate refunds (check if REFUND with refId=originalTransactionId already exists)
  - Increments wallet balance
  - Logs refund event for audit

**REQ-006:** Free-Tier Quota Tracking
- **Description:** System MUST track monthly free-tier usage per tenant per feature
- **Acceptance Criteria:**
  - Configurable quotas per feature: `FREE_CHAT_MESSAGES_PER_MONTH=50`, `FREE_LINKEDIN_DRAFTS_PER_MONTH=10`
  - Quotas stored in application.yml (not database, for simplicity in Phase 1)
  - Service method: `checkAndConsumeQuota(tenantId, featureType)` returns QuotaResult (FREE, COINS_REQUIRED, QUOTA_EXCEEDED)
  - Monthly usage tracked in coin_transaction table (count SPEND transactions per ref_type per month) OR dedicated `free_tier_usage` table
  - Quotas reset on 1st of each month
  - When quota exceeded and no coins: return 402 Payment Required

**REQ-007:** Configurable Coin Costs
- **Description:** Coin costs per feature MUST be configurable without code changes
- **Acceptance Criteria:**
  - Configuration in application.yml:
    ```yaml
    billing:
      costs:
        resume-export: 10
        deck-export: 15
        cover-letter-export: 10
        chat-message: 1
        linkedin-draft: 2
        linkedin-suggestion: 3
      free-tier:
        chat-messages-per-month: 50
        linkedin-drafts-per-month: 10
    ```
  - Service reads costs from configuration
  - Endpoint GET `/api/billing/costs` returns all feature costs to frontend
  - Frontend displays cost before user confirms action

**REQ-008:** Wallet and Transaction Retrieval
- **Description:** Users MUST be able to view their wallet balance and transaction history
- **Acceptance Criteria:**
  - GET `/api/billing/wallet` returns current wallet balance for authenticated tenant
  - GET `/api/billing/transactions` returns paginated transaction history (page size: 20)
  - Transactions ordered by created_at DESC
  - Filterable by: type (EARN, SPEND, REFUND, PURCHASE), date range, ref_type
  - Tenant isolation: user only sees own tenant's transactions

### User Interface Requirements

**REQ-UI-001:** Billing Page
- **Description:** Frontend MUST provide a billing page showing wallet balance and transaction history
- **Acceptance Criteria:**
  - Route: `/billing`
  - Header section: Current balance displayed prominently (e.g., large number with coin icon)
  - Transaction history table with columns: Date, Type (with colored badge), Description, Amount (green for positive, red for negative), Running Balance
  - Filter controls: dropdown for transaction type, date range picker
  - Pagination controls (Previous/Next)
  - Empty state: "No transactions yet" with explanation of how coins work

**REQ-UI-002:** Coin Balance Badge (Header Component)
- **Description:** Frontend MUST show current coin balance in the application header/navbar
- **Acceptance Criteria:**
  - Small badge/chip showing coin count (e.g., coin icon + "50")
  - Clickable: navigates to Billing page
  - Updates reactively after coin transactions (via React Query invalidation)
  - Shows loading state on initial fetch

**REQ-UI-003:** Cost Confirmation Dialog
- **Description:** Frontend MUST display cost before any coin-consuming action
- **Acceptance Criteria:**
  - Dialog shows: feature name, coin cost, current balance, balance after action
  - "Confirm" and "Cancel" buttons
  - If insufficient balance: disable Confirm button, show warning "Insufficient coins. You need X more coins."
  - Reusable component: `CoinConfirmationDialog`

### Data Requirements

**REQ-DATA-001:** CoinWallet Table Schema
- **Description:** PostgreSQL table for storing tenant coin wallets
- **Acceptance Criteria:**
  - Table name: `coin_wallet`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL UNIQUE), balance (BIGINT NOT NULL DEFAULT 0), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), version (BIGINT NOT NULL DEFAULT 0)
  - Indexes: `idx_coin_wallet_tenant_id` (unique)
  - Foreign key: tenant_id -> tenant(id)
  - Check constraint: balance >= 0
  - Unique constraint on tenant_id

**REQ-DATA-002:** CoinTransaction Table Schema
- **Description:** PostgreSQL table for recording all coin movements
- **Acceptance Criteria:**
  - Table name: `coin_transaction`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), wallet_id (BIGINT FK NOT NULL), type (VARCHAR 20 NOT NULL), amount (INTEGER NOT NULL), description (VARCHAR 500), ref_type (VARCHAR 50), ref_id (VARCHAR 100), created_at (TIMESTAMPTZ NOT NULL)
  - Indexes: `idx_coin_transaction_tenant_id`, `idx_coin_transaction_wallet_id`, `idx_coin_transaction_type`, `idx_coin_transaction_ref_type_ref_id`, `idx_coin_transaction_created_at`
  - Foreign keys: tenant_id -> tenant(id), wallet_id -> coin_wallet(id)
  - Check constraint: type IN ('EARN', 'SPEND', 'REFUND', 'PURCHASE')

**REQ-DATA-003:** FreeTierUsage Table Schema
- **Description:** PostgreSQL table for tracking monthly free-tier usage per tenant per feature
- **Acceptance Criteria:**
  - Table name: `free_tier_usage`
  - Columns: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), feature_type (VARCHAR 50 NOT NULL), usage_count (INTEGER NOT NULL DEFAULT 0), year_month (VARCHAR 7 NOT NULL, format: "2026-02"), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL)
  - Indexes: `idx_free_tier_usage_tenant_feature_month` (tenant_id, feature_type, year_month) UNIQUE
  - Foreign key: tenant_id -> tenant(id)
  - Unique constraint on (tenant_id, feature_type, year_month)

---

## Success Criteria

The feature will be considered successful when:

1. **Balance Never Negative:** Under concurrent load (10 simultaneous spend requests), wallet balance never goes below zero
   - Measurement: Concurrent integration test with multiple threads

2. **Transaction Audit Trail Complete:** Every coin movement creates exactly one CoinTransaction record
   - Measurement: Integration test verifying transaction count matches operations count

3. **Tenant Isolation Verified:** Users in different tenants cannot see each other's wallet or transactions
   - Measurement: Integration test with two tenants, cross-tenant access returns 404

4. **Free-Tier Quotas Enforced:** Users cannot exceed monthly free quotas without coins
   - Measurement: Test sending 51st free chat message returns 402 Payment Required

5. **Auto-Refund Works:** Failed operations automatically refund coins within same transaction
   - Measurement: Integration test triggering export failure, verifying refund transaction created

6. **Admin Grant Works:** Admin can grant coins to any tenant
   - Measurement: Integration test granting coins, verifying balance increase

7. **Frontend Displays Correctly:** Billing page shows accurate balance and transaction history
   - Measurement: Manual UAT with 10 transactions of mixed types

---

## Key Entities

### CoinWallet

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL, UNIQUE)
- balance: Current coin balance (BIGINT, NOT NULL, DEFAULT 0, CHECK >= 0)
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)
- version: Optimistic locking version (BIGINT, NOT NULL, DEFAULT 0)

**Relationships:**
- One-to-one with Tenant (each tenant has exactly one wallet)
- One-to-many with CoinTransaction (one wallet has many transactions)

**Validation Rules:**
- balance: Must be >= 0 (enforced at DB and service layer)
- tenant_id: Must reference valid tenant, unique per wallet

### CoinTransaction

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL)
- wallet_id: Foreign key to CoinWallet (BIGINT, NOT NULL)
- type: Transaction type (VARCHAR 20, NOT NULL: EARN, SPEND, REFUND, PURCHASE)
- amount: Coin amount (INTEGER, NOT NULL, signed: positive for earn/refund, negative for spend)
- description: Human-readable description (VARCHAR 500, optional)
- ref_type: Reference type linking to feature (VARCHAR 50, optional: ADMIN_GRANT, EXPORT, CHAT_MESSAGE, LINKEDIN_DRAFT, LINKEDIN_SUGGESTION, EXPORT_REFUND)
- ref_id: Reference ID linking to specific entity (VARCHAR 100, optional)
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)

**Relationships:**
- Many-to-one with Tenant
- Many-to-one with CoinWallet

**Validation Rules:**
- type: Must be one of EARN, SPEND, REFUND, PURCHASE
- amount: Must be non-zero; positive for EARN/REFUND/PURCHASE, negative for SPEND
- Immutable: No update or delete operations

### FreeTierUsage

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL)
- feature_type: Feature identifier (VARCHAR 50, NOT NULL: CHAT_MESSAGE, LINKEDIN_DRAFT, LINKEDIN_SUGGESTION)
- usage_count: Number of free uses consumed this month (INTEGER, NOT NULL, DEFAULT 0)
- year_month: Month period (VARCHAR 7, NOT NULL, format: "2026-02")
- created_at: Creation timestamp (TIMESTAMPTZ, NOT NULL)
- updated_at: Last update timestamp (TIMESTAMPTZ, NOT NULL)

**Relationships:**
- Many-to-one with Tenant

**Validation Rules:**
- usage_count: Must be >= 0
- year_month: Must match format "YYYY-MM"
- Unique constraint on (tenant_id, feature_type, year_month)

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Requires authentication (JWT), tenant filtering (Hibernate filter), database infrastructure, Spring Boot backend, React frontend
- **Feature 002: Profile CRUD** - Wallet is created per tenant, which already exists via auth system

### External Dependencies

- **Spring Data JPA:** For repository abstractions and optimistic locking
- **Liquibase:** For database schema migrations
- **MUI Components:** For BillingPage UI (Table, Chip, Pagination, Dialog)

### Future Dependencies (Not Phase 1)

- **Stripe SDK:** For coin purchase integration (Phase 2)
- **Feature 008: Resume Export** - Will call CoinWalletService.spend() when generating exports
- **Feature 006: Recruiter Chat** - Will check free quota and spend coins

---

## Assumptions

1. Feature 001 (Project Base Structure) is fully implemented with working authentication and tenant filtering
2. One wallet per tenant (not per user) -- multiple users in same tenant share the wallet
3. Phase 1 uses manual admin grants only (no Stripe, no self-service purchase)
4. Coin costs are integers (no fractional coins)
5. Free-tier quotas are the same for all tenants in Phase 1 (no per-tenant customization)
6. Monthly quotas reset on the 1st of each month (UTC)
7. Transaction history does not need to show running balance (computed client-side if needed)
8. No currency conversion (coins are abstract units, not tied to real currency)
9. Optimistic locking is sufficient for concurrency control (no pessimistic locking needed)
10. Free-tier usage tracking can use a simple counter table (no need for complex analytics)

---

## Out of Scope

The following are explicitly excluded from this feature:

1. **No Stripe Integration:** Payment processing via Stripe covered in Phase 2
2. **No Subscription Plans:** Monthly Pro tier covered in Phase 3
3. **No Per-User Wallets:** Wallet is per-tenant, not per-user
4. **No Coin Expiration:** Coins do not expire
5. **No Coin Transfer:** Coins cannot be transferred between tenants
6. **No Detailed Analytics Dashboard:** Admin analytics for coin usage patterns
7. **No Notification System:** Email/push notifications for low balance or quota limits (future feature)
8. **No Currency Display:** No mapping of coins to real currency values
9. **No Rate Limiting:** Rate limiting per tenant (max actions/day even with coins) handled separately
10. **No Webhook Integration:** Webhooks for coin events (future feature)

---

## Security & Privacy Considerations

### Security Requirements

- All billing API endpoints MUST require valid JWT authentication
- Admin coin grant endpoint MUST require ADMIN role (`@PreAuthorize("hasRole('ADMIN')")`)
- Tenant filtering MUST be automatically applied to all wallet and transaction queries
- CoinWallet balance changes MUST use optimistic locking to prevent race conditions
- Transaction records are immutable (no update/delete endpoints exposed)
- Audit fields (created_at) MUST be system-managed (not user-provided)

### Privacy Requirements

- Users can only view their own tenant's wallet and transactions
- Admin grant operations logged with admin user ID for accountability
- No sensitive financial data stored (coins are not real currency)
- Transaction descriptions should not contain PII

---

## Performance Expectations

- **Wallet Balance Retrieval:** p95 latency < 50ms (single row lookup by tenant_id with index)
- **Transaction History:** p95 latency < 200ms (paginated query with indexes on tenant_id, created_at)
- **Coin Spend Operation:** p95 latency < 100ms (single wallet update with optimistic locking)
- **Free Quota Check:** p95 latency < 50ms (single row lookup in free_tier_usage)
- **Concurrent Spend Requests:** Support 10 concurrent spend requests per tenant without data corruption (optimistic locking retries)
- **Database Queries:** Use indexes on tenant_id, wallet_id, type, created_at for efficient filtering

---

## Error Handling

### Error Scenarios

**ERROR-001:** Insufficient Balance
- **User Experience:** API returns 402 Payment Required with JSON: `{"code": "INSUFFICIENT_BALANCE", "message": "Insufficient coins. Required: 10, Available: 5", "required": 10, "available": 5}`
- **Recovery Path:** User navigates to billing page, contacts admin for coin grant (Phase 1)

**ERROR-002:** Optimistic Lock Conflict on Wallet
- **User Experience:** System retries automatically (up to 3 times). If all retries fail: API returns 409 Conflict: `{"code": "CONCURRENT_MODIFICATION", "message": "Wallet was updated by another operation. Please try again."}`
- **Recovery Path:** User retries the action

**ERROR-003:** Wallet Not Found (Auto-Create)
- **User Experience:** Transparent to user. System auto-creates wallet with balance 0 on first access
- **Recovery Path:** No user action needed

**ERROR-004:** Invalid Admin Grant (Negative Amount)
- **User Experience:** API returns 400 Bad Request: `{"code": "INVALID_AMOUNT", "message": "Amount must be a positive integer"}`
- **Recovery Path:** Admin corrects amount and retries

**ERROR-005:** Free Quota Exceeded
- **User Experience:** API returns 402 Payment Required: `{"code": "FREE_QUOTA_EXCEEDED", "message": "Free quota for chat messages exhausted this month. Cost: 1 coin per message.", "feature": "CHAT_MESSAGE", "used": 50, "limit": 50}`
- **Recovery Path:** User contacts admin for coins (Phase 1) or waits for monthly reset

**ERROR-006:** Duplicate Refund Attempt
- **User Experience:** API returns 409 Conflict: `{"code": "DUPLICATE_REFUND", "message": "This transaction has already been refunded"}`
- **Recovery Path:** No action needed, refund already processed

---

## Testing Scope

### Functional Testing

- **CRUD Operations:** Test wallet retrieval, transaction history with pagination and filtering
- **Spend Operation:** Test successful spend, insufficient balance, concurrent spend (optimistic locking)
- **Earn/Grant:** Test admin grant with valid/invalid amounts
- **Refund:** Test auto-refund, duplicate refund prevention
- **Free-Tier Quotas:** Test quota check, quota increment, monthly reset
- **Tenant Isolation:** Test cross-tenant wallet access returns 404

### User Acceptance Testing

- **Billing Page:** Navigate to billing, verify balance and transaction list
- **Cost Display:** Trigger premium action, verify cost confirmation dialog
- **Admin Grant:** Admin grants coins, user sees updated balance
- **Spend Flow:** User exports resume, verify coins deducted and transaction recorded

### Edge Case Testing

- **Concurrent Spends:** 10 threads spending coins simultaneously
- **Zero Balance Spend:** Attempt to spend with 0 balance
- **Large Transaction History:** 1000+ transactions, verify pagination performance
- **Month Boundary:** Free quota check near month boundary
- **Wallet Auto-Creation:** First-time access triggers wallet creation

---

## Notes

This feature establishes the monetization foundation for the entire platform. Key design decisions:

- **Optimistic Locking over Pessimistic Locking:** Simpler, scales better for typical usage patterns (low contention per tenant)
- **Signed Amount Convention:** SPEND transactions store negative amounts for easy balance calculation via SUM
- **Separate FreeTierUsage Table:** Cleaner than counting transactions, avoids complex queries, easy monthly reset
- **Configuration-Driven Costs:** Coin costs in application.yml, not database, for Phase 1 simplicity
- **Auto-Create Wallet:** Reduces setup friction; wallet created transparently on first use
- **Immutable Transactions:** CoinTransaction records are never modified or deleted, ensuring audit integrity

Future enhancements:
- Stripe integration for self-service coin purchases
- Subscription tiers with auto-refilling monthly coins
- Usage analytics dashboard for admins
- Webhook events for coin transactions
- Per-tenant custom quotas and pricing

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial specification       | Claude Code |
