# Implementation Tasks: Coin Wallet & Billing System

**Feature ID:** 007-coin-wallet
**Branch:** 007-coin-wallet
**Spec:** [spec.md](./spec.md)
**Plan:** [plan.md](./plan.md)

---

## Legend
- `[ ]` Not Started
- `[P]` In Progress
- `[X]` Completed
- `[B]` Blocked (add blocker note)
- `[S]` Skipped (add reason)
- `[||]` Can run in parallel (different files, no dependencies)
- `[US-N]` Belongs to User Story N from spec.md

---

## Phase 1: Database Schema

### Liquibase Migrations

- [ ] T001 [DATABASE] Create coin_wallet table migration
  - File: `backend/src/main/resources/db/changelog/20260224140000-create-coin-wallet-table.xml`
  - Create `coin_wallet` table: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL UNIQUE), balance (BIGINT NOT NULL DEFAULT 0), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL), version (BIGINT NOT NULL DEFAULT 0)
  - Add foreign key: tenant_id -> tenant(id)
  - Add unique index: idx_coin_wallet_tenant_id
  - Add check constraint: balance >= 0
  - Include rollback: dropTable coin_wallet

- [ ] T002 [DATABASE] Create coin_transaction table migration
  - File: `backend/src/main/resources/db/changelog/20260224140100-create-coin-transaction-table.xml`
  - Create `coin_transaction` table: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), wallet_id (BIGINT FK NOT NULL), type (VARCHAR 20 NOT NULL), amount (INTEGER NOT NULL), description (VARCHAR 500), ref_type (VARCHAR 50), ref_id (VARCHAR 100), created_at (TIMESTAMPTZ NOT NULL)
  - Add foreign keys: tenant_id -> tenant(id), wallet_id -> coin_wallet(id)
  - Add indexes: idx_coin_transaction_tenant_id, idx_coin_transaction_wallet_id, idx_coin_transaction_type, idx_coin_transaction_ref_type_ref_id, idx_coin_transaction_created_at
  - Add check constraint: type IN ('EARN', 'SPEND', 'REFUND', 'PURCHASE')
  - Include rollback: dropTable coin_transaction

- [ ] T003 [DATABASE] Create free_tier_usage table migration
  - File: `backend/src/main/resources/db/changelog/20260224140200-create-free-tier-usage-table.xml`
  - Create `free_tier_usage` table: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), feature_type (VARCHAR 50 NOT NULL), usage_count (INTEGER NOT NULL DEFAULT 0), year_month (VARCHAR 7 NOT NULL), created_at (TIMESTAMPTZ NOT NULL), updated_at (TIMESTAMPTZ NOT NULL)
  - Add foreign key: tenant_id -> tenant(id)
  - Add unique index: idx_free_tier_usage_tenant_feature_month (tenant_id, feature_type, year_month)
  - Include rollback: dropTable free_tier_usage

- [ ] T004 [DATABASE] Add migrations to master changelog
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Include: `db/changelog/20260224140000-create-coin-wallet-table.xml`
  - Include: `db/changelog/20260224140100-create-coin-transaction-table.xml`
  - Include: `db/changelog/20260224140200-create-free-tier-usage-table.xml`

---

## Phase 2: Enums and Configuration

- [ ] T005 [||] [JAVA25] Create TransactionType enum
  - File: `backend/src/main/java/com/interviewme/billing/model/TransactionType.java`
  - Values: EARN, SPEND, REFUND, PURCHASE

- [ ] T006 [||] [JAVA25] Create RefType enum
  - File: `backend/src/main/java/com/interviewme/billing/model/RefType.java`
  - Values: ADMIN_GRANT, EXPORT, CHAT_MESSAGE, LINKEDIN_DRAFT, LINKEDIN_SUGGESTION, EXPORT_REFUND

- [ ] T007 [||] [JAVA25] Create FeatureType enum
  - File: `backend/src/main/java/com/interviewme/billing/model/FeatureType.java`
  - Values: CHAT_MESSAGE, LINKEDIN_DRAFT, LINKEDIN_SUGGESTION

- [ ] T008 [||] [SIMPLE] Create BillingProperties configuration class
  - File: `backend/src/main/java/com/interviewme/billing/config/BillingProperties.java`
  - Use @ConfigurationProperties(prefix = "billing")
  - Properties: Map<String, Integer> costs, FreeTierConfig freeTier, RetryConfig retry
  - Inner class FreeTierConfig: chatMessagesPerMonth (int, default 50), linkedinDraftsPerMonth (int, default 10)
  - Inner class RetryConfig: maxAttempts (int, default 3), backoffMs (int, default 100)

- [ ] T009 [SIMPLE] Add billing configuration to application.yml
  - File: `backend/src/main/resources/application.yml`
  - Add billing.costs section with default values
  - Add billing.free-tier section with default quotas
  - Add billing.retry section with retry settings

---

## Phase 3: JPA Entities

- [ ] T010 [||] [JAVA25] Create CoinWallet entity
  - File: `backend/src/main/java/com/interviewme/billing/model/CoinWallet.java`
  - Annotations: @Entity, @Table(name = "coin_wallet"), @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id (BIGSERIAL), tenantId (BIGINT, unique), balance (BIGINT, default 0), createdAt (Instant), updatedAt (Instant), version (BIGINT)
  - Use @Version on version field for optimistic locking
  - Lombok: @Entity, @Data, @NoArgsConstructor, @AllArgsConstructor
  - @PrePersist/@PreUpdate for timestamps

- [ ] T011 [||] [JAVA25] Create CoinTransaction entity
  - File: `backend/src/main/java/com/interviewme/billing/model/CoinTransaction.java`
  - Annotations: @Entity, @Table(name = "coin_transaction"), @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id, tenantId, walletId, type (TransactionType enum), amount (int), description (String 500), refType (RefType enum), refId (String 100), createdAt (Instant)
  - Use @Enumerated(EnumType.STRING) for type and refType
  - Relationship: @ManyToOne with CoinWallet
  - Lombok: @Entity, @Data, @NoArgsConstructor, @AllArgsConstructor
  - @PrePersist for createdAt

- [ ] T012 [||] [JAVA25] Create FreeTierUsage entity
  - File: `backend/src/main/java/com/interviewme/billing/model/FreeTierUsage.java`
  - Annotations: @Entity, @Table(name = "free_tier_usage")
  - Fields: id, tenantId, featureType (FeatureType enum), usageCount (int, default 0), yearMonth (String 7), createdAt, updatedAt
  - Use @Enumerated(EnumType.STRING) for featureType
  - Unique constraint: @Table(uniqueConstraints = @UniqueConstraint(columns = {"tenant_id", "feature_type", "year_month"}))
  - Lombok: @Entity, @Data, @NoArgsConstructor, @AllArgsConstructor
  - @PrePersist/@PreUpdate for timestamps

---

## Phase 4: DTOs

- [ ] T013 [||] [JAVA25] Create billing DTOs
  - Files: `backend/src/main/java/com/interviewme/billing/dto/`
  - `WalletResponse.java`: record(Long id, Long tenantId, long balance, Instant createdAt, Instant updatedAt)
  - `TransactionResponse.java`: record(Long id, String type, int amount, String description, String refType, String refId, Instant createdAt)
  - `TransactionPageResponse.java`: record(List<TransactionResponse> content, int page, int size, long totalElements, int totalPages)
  - `AdminGrantRequest.java`: record(@NotNull @Min(1) Integer amount, @Size(max = 500) String description)
  - `FeatureCostResponse.java`: record(Map<String, Integer> costs, Map<String, Integer> freeQuotas)
  - `QuotaStatusResponse.java`: record(String featureType, int used, int limit, boolean quotaExceeded, String yearMonth)

---

## Phase 5: Repositories

- [ ] T014 [||] [DATA] Create CoinWalletRepository
  - File: `backend/src/main/java/com/interviewme/billing/repository/CoinWalletRepository.java`
  - Extend: JpaRepository<CoinWallet, Long>
  - Query methods:
    - `Optional<CoinWallet> findByTenantId(Long tenantId)`
    - `boolean existsByTenantId(Long tenantId)`

- [ ] T015 [||] [DATA] Create CoinTransactionRepository
  - File: `backend/src/main/java/com/interviewme/billing/repository/CoinTransactionRepository.java`
  - Extend: JpaRepository<CoinTransaction, Long>
  - Query methods:
    - `Page<CoinTransaction> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable)`
    - `Page<CoinTransaction> findByTenantIdAndTypeOrderByCreatedAtDesc(Long tenantId, TransactionType type, Pageable pageable)`
    - `boolean existsByRefTypeAndRefId(RefType refType, String refId)` (for duplicate refund check)
    - `@Query` custom: filtered by type, refType, date range with dynamic conditions

- [ ] T016 [||] [DATA] Create FreeTierUsageRepository
  - File: `backend/src/main/java/com/interviewme/billing/repository/FreeTierUsageRepository.java`
  - Extend: JpaRepository<FreeTierUsage, Long>
  - Query methods:
    - `Optional<FreeTierUsage> findByTenantIdAndFeatureTypeAndYearMonth(Long tenantId, FeatureType featureType, String yearMonth)`

---

## Phase 6: Custom Exceptions

- [ ] T017 [||] [SIMPLE] Create billing custom exceptions
  - Files: `backend/src/main/java/com/interviewme/billing/exception/`
  - `InsufficientBalanceException.java`: extends RuntimeException, fields: required (int), available (long)
  - `DuplicateRefundException.java`: extends RuntimeException, field: originalTransactionId (Long)
  - `WalletNotFoundException.java`: extends RuntimeException, field: tenantId (Long)

- [ ] T018 [SIMPLE] Add billing exception handlers to GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/config/GlobalExceptionHandler.java`
  - Add handler: `@ExceptionHandler(InsufficientBalanceException.class)` -> 402 Payment Required
  - Add handler: `@ExceptionHandler(DuplicateRefundException.class)` -> 409 Conflict
  - Add handler: `@ExceptionHandler(WalletNotFoundException.class)` -> 404 Not Found

---

## Phase 7: Domain Event

- [ ] T019 [SIMPLE] Create CoinTransactionEvent
  - File: `backend/src/main/java/com/interviewme/billing/event/CoinTransactionEvent.java`
  - Java record: CoinTransactionEvent(Long tenantId, Long walletId, Long transactionId, String type, int amount, String refType, String refId, Instant timestamp)

---

## Phase 8: Services

### CoinWalletService

- [ ] T020 [MODULAR] Create CoinWalletService
  - File: `backend/src/main/java/com/interviewme/billing/service/CoinWalletService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: CoinWalletRepository, CoinTransactionRepository, BillingProperties, ApplicationEventPublisher
  - Method: `@Transactional(readOnly = true) WalletResponse getWallet(Long tenantId)` - get or auto-create wallet, return balance
  - Method: `@Transactional CoinTransaction spend(Long tenantId, int amount, RefType refType, String refId, String description)` - check balance, decrement, create SPEND transaction, publish event. Retry on OptimisticLockingFailureException (up to 3 times)
  - Method: `@Transactional CoinTransaction earn(Long tenantId, int amount, RefType refType, String refId, String description)` - increment balance, create EARN transaction, publish event
  - Method: `@Transactional CoinTransaction refund(Long tenantId, Long originalTransactionId)` - lookup original tx, check no duplicate refund, create REFUND transaction, increment balance, publish event
  - Method: `@Transactional CoinWallet getOrCreateWallet(Long tenantId)` - find by tenantId or create with balance 0
  - Method: `@Transactional(readOnly = true) TransactionPageResponse getTransactions(Long tenantId, int page, int size, String type, String refType, OffsetDateTime from, OffsetDateTime to)` - paginated filtered query
  - Method: `@Transactional(readOnly = true) FeatureCostResponse getFeatureCosts()` - return costs from BillingProperties
  - Logging: log.info for all coin operations with tenantId, amount, type

### FreeTierService

- [ ] T021 [MODULAR] Create FreeTierService
  - File: `backend/src/main/java/com/interviewme/billing/service/FreeTierService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: FreeTierUsageRepository, BillingProperties
  - Method: `@Transactional(readOnly = true) QuotaStatusResponse getQuotaStatus(Long tenantId, FeatureType featureType)` - return current usage vs limit for current month
  - Method: `@Transactional boolean tryConsumeFreeTier(Long tenantId, FeatureType featureType)` - if under quota: increment usage_count, return true; if at/over quota: return false
  - Helper: `String getCurrentYearMonth()` - returns "2026-02" format
  - Helper: `int getQuotaLimit(FeatureType featureType)` - reads limit from BillingProperties
  - Logging: log.debug for quota checks

---

## Phase 9: Mappers

- [ ] T022 [SIMPLE] Create BillingMapper
  - File: `backend/src/main/java/com/interviewme/billing/mapper/BillingMapper.java`
  - Static methods:
    - `WalletResponse toWalletResponse(CoinWallet wallet)`
    - `TransactionResponse toTransactionResponse(CoinTransaction tx)`
    - `TransactionPageResponse toPageResponse(Page<CoinTransaction> page)`

---

## Phase 10: Controllers

### User-Facing BillingController

- [ ] T023 [MODULAR] Create BillingController
  - File: `backend/src/main/java/com/interviewme/billing/controller/BillingController.java`
  - Annotations: @RestController, @RequestMapping("/api/billing"), @RequiredArgsConstructor, @Slf4j
  - Dependencies: CoinWalletService, FreeTierService
  - Endpoint: `@GetMapping("/wallet")` -> getWallet() -> 200 OK WalletResponse (tenant from TenantContext)
  - Endpoint: `@GetMapping("/transactions")` -> getTransactions(@RequestParam page, size, type, refType, from, to) -> 200 OK TransactionPageResponse
  - Endpoint: `@GetMapping("/costs")` -> getFeatureCosts() -> 200 OK FeatureCostResponse
  - Endpoint: `@GetMapping("/quota/{featureType}")` -> getQuotaStatus(@PathVariable FeatureType featureType) -> 200 OK QuotaStatusResponse
  - All endpoints: @Transactional(readOnly = true), JWT auth required

### Admin BillingController

- [ ] T024 [MODULAR] Create AdminBillingController
  - File: `backend/src/main/java/com/interviewme/billing/controller/AdminBillingController.java`
  - Annotations: @RestController, @RequestMapping("/api/admin/wallets"), @RequiredArgsConstructor, @Slf4j, @PreAuthorize("hasRole('ADMIN')")
  - Dependencies: CoinWalletService
  - Endpoint: `@PostMapping("/{tenantId}/grant")` -> grantCoins(@PathVariable Long tenantId, @Valid @RequestBody AdminGrantRequest request) -> 200 OK WalletResponse
  - Endpoint: `@GetMapping("/{tenantId}")` -> getWallet(@PathVariable Long tenantId) -> 200 OK WalletResponse
  - Grant endpoint: @Transactional, logs admin userId from Authentication

---

## Phase 11: Frontend - TypeScript Types & API Client

### TypeScript Types

- [ ] T025 [||] [SIMPLE] Create billing TypeScript interfaces
  - File: `frontend/src/types/billing.ts`
  - Interfaces: WalletResponse, TransactionResponse, TransactionPageResponse, AdminGrantRequest, FeatureCostResponse, QuotaStatusResponse, TransactionType (type union), RefType (type union)

### API Client

- [ ] T026 [MODULAR] Create Billing API client
  - File: `frontend/src/api/billingApi.ts`
  - Import: Axios client with JWT interceptor
  - Functions:
    - `getWallet(): Promise<WalletResponse>`
    - `getTransactions(params: TransactionQueryParams): Promise<TransactionPageResponse>`
    - `getFeatureCosts(): Promise<FeatureCostResponse>`
    - `getQuotaStatus(featureType: string): Promise<QuotaStatusResponse>`

---

## Phase 12: Frontend - TanStack Query Hooks

- [ ] T027 [MODULAR] Create billing hooks
  - File: `frontend/src/hooks/useBilling.ts`
  - Hooks:
    - `useWallet()` (useQuery) - query key: ['billing', 'wallet']
    - `useTransactions(params)` (useQuery) - query key: ['billing', 'transactions', params]
    - `useFeatureCosts()` (useQuery) - query key: ['billing', 'costs'], staleTime: 5 min
    - `useQuotaStatus(featureType)` (useQuery) - query key: ['billing', 'quota', featureType]
  - Export invalidation helper: `invalidateWallet()` for use after coin operations

---

## Phase 13: Frontend - React Components

### Billing Components

- [ ] T028 [MODULAR] Create BillingPage
  - File: `frontend/src/pages/BillingPage.tsx`
  - Route: /billing
  - Layout: Balance header section, free-tier quota cards, transaction history table
  - Use useWallet(), useTransactions(), useQuotaStatus() hooks
  - Display loading/error states

- [ ] T029 [||] [MODULAR] Create CoinBalanceBadge
  - File: `frontend/src/components/billing/CoinBalanceBadge.tsx`
  - Small MUI Chip with coin icon and balance number
  - Use useWallet() hook
  - onClick navigates to /billing
  - Show Skeleton on loading

- [ ] T030 [||] [MODULAR] Create CoinConfirmationDialog
  - File: `frontend/src/components/billing/CoinConfirmationDialog.tsx`
  - Props: open, onClose, onConfirm, featureName, cost, currentBalance
  - Display: feature name, cost, current balance, balance after
  - Disable confirm if insufficient balance, show warning
  - MUI Dialog with Cancel/Confirm buttons

- [ ] T031 [||] [MODULAR] Create TransactionHistoryTable
  - File: `frontend/src/components/billing/TransactionHistoryTable.tsx`
  - MUI Table with columns: Date, Type (badge), Description, Amount (colored)
  - Props: transactions, page, totalPages, onPageChange
  - Filter dropdowns for type and date range

- [ ] T032 [||] [MODULAR] Create TransactionTypeBadge
  - File: `frontend/src/components/billing/TransactionTypeBadge.tsx`
  - MUI Chip with colored background per type:
    - EARN: green
    - SPEND: red
    - REFUND: blue
    - PURCHASE: purple

- [ ] T033 [||] [MODULAR] Create QuotaStatusCard
  - File: `frontend/src/components/billing/QuotaStatusCard.tsx`
  - MUI Card showing feature name, usage/limit, progress bar
  - Props: featureType, used, limit
  - Color: green (< 80%), yellow (80-99%), red (100%)

### Integration with App Layout

- [ ] T034 [SIMPLE] Add CoinBalanceBadge to app header/navbar
  - File: `frontend/src/App.tsx` (or layout component)
  - Add CoinBalanceBadge component to header, visible on all authenticated pages

- [ ] T035 [SIMPLE] Add /billing route to router
  - File: `frontend/src/App.tsx` (or router config)
  - Add route: `/billing` -> BillingPage
  - Protected route (requires authentication)

---

## Phase 14: Testing

### Backend Unit Tests

- [ ] T036 [||] [JAVA25] Write CoinWalletService unit tests
  - File: `backend/src/test/java/com/interviewme/billing/service/CoinWalletServiceTest.java`
  - Use @ExtendWith(MockitoExtension.class), mock repositories and eventPublisher
  - Test: spend_shouldDecrementBalance
  - Test: spend_shouldThrowInsufficientBalanceException
  - Test: earn_shouldIncrementBalance
  - Test: refund_shouldCreateRefundTransaction
  - Test: refund_shouldThrowDuplicateRefundException
  - Test: getOrCreateWallet_shouldCreateIfNotExists
  - Test: getTransactions_shouldReturnPaginatedResults

- [ ] T037 [||] [JAVA25] Write FreeTierService unit tests
  - File: `backend/src/test/java/com/interviewme/billing/service/FreeTierServiceTest.java`
  - Test: tryConsumeFreeTier_shouldReturnTrueWhenUnderQuota
  - Test: tryConsumeFreeTier_shouldReturnFalseWhenAtQuota
  - Test: getQuotaStatus_shouldReturnCorrectUsage
  - Test: tryConsumeFreeTier_shouldCreateNewRecordForNewMonth

### Backend Integration Tests

- [ ] T038 [SIMPLE] Write BillingController integration tests
  - File: `backend/src/test/java/com/interviewme/billing/controller/BillingControllerIntegrationTest.java`
  - Use @SpringBootTest, @AutoConfigureMockMvc, @Transactional
  - Test: GET /api/billing/wallet -> 200 OK with wallet balance
  - Test: GET /api/billing/transactions -> 200 OK with paginated list
  - Test: GET /api/billing/transactions?type=SPEND -> filtered results
  - Test: GET /api/billing/costs -> 200 OK with configured costs
  - Test: GET /api/billing/quota/CHAT_MESSAGE -> 200 OK with quota status

- [ ] T039 [SIMPLE] Write AdminBillingController integration tests
  - File: `backend/src/test/java/com/interviewme/billing/controller/AdminBillingControllerIntegrationTest.java`
  - Test: POST /api/admin/wallets/{tenantId}/grant -> 200 OK, balance increased
  - Test: POST /api/admin/wallets/{tenantId}/grant with negative amount -> 400 Bad Request
  - Test: POST /api/admin/wallets/{tenantId}/grant without ADMIN role -> 403 Forbidden
  - Test: GET /api/admin/wallets/{tenantId} -> 200 OK with wallet

### Concurrency Tests

- [ ] T040 [MODULAR] Write concurrent spend test
  - File: `backend/src/test/java/com/interviewme/billing/CoinWalletConcurrencyTest.java`
  - Setup: Create wallet with 100 coins
  - Test: Launch 10 threads each spending 10 coins simultaneously
  - Verify: Final balance is exactly 0 (all spends succeed) OR some spends fail with InsufficientBalanceException
  - Verify: Balance NEVER goes negative
  - Verify: Number of SPEND transactions + failed attempts = 10

### Tenant Isolation Tests

- [ ] T041 [DATA] Write billing tenant isolation tests
  - File: `backend/src/test/java/com/interviewme/billing/TenantIsolationBillingTest.java`
  - Test: Tenant A creates wallet, Tenant B cannot see it
  - Test: Tenant A has transactions, Tenant B's transaction list is empty

### Frontend Tests

- [ ] T042 [||] [SIMPLE] Write BillingPage tests
  - File: `frontend/src/pages/BillingPage.test.tsx`
  - Test: should display wallet balance
  - Test: should show transaction history
  - Test: should show loading state

- [ ] T043 [||] [SIMPLE] Write CoinConfirmationDialog tests
  - File: `frontend/src/components/billing/CoinConfirmationDialog.test.tsx`
  - Test: should display cost and balance
  - Test: should disable confirm when insufficient balance
  - Test: should call onConfirm when clicked

---

## Phase 15: Verification & Documentation

- [ ] T044 [SIMPLE] Verify all migrations run successfully
  - Command: `./gradlew backend:bootRun`
  - Verify: All 3 migrations execute without errors
  - Verify: Tables created with correct schema (check with PostgreSQL client)
  - Verify: Indexes, foreign keys, and check constraints created

- [ ] T045 [SIMPLE] Run all backend tests
  - Command: `./gradlew backend:test`
  - Verify: All unit and integration tests pass
  - Verify: Code coverage > 80% for service layer

- [ ] T046 [SIMPLE] Run all frontend tests
  - Command: `cd frontend && npm test`
  - Verify: All React tests pass

- [ ] T047 [SIMPLE] Manual E2E testing
  - Test: Admin grants 100 coins to tenant
  - Test: User views billing page, sees balance = 100
  - Test: User triggers premium action, cost dialog shows correctly
  - Test: After spend, balance updates and transaction appears
  - Test: Trigger failed operation, verify auto-refund appears
  - Test: Use 50 free chat messages, verify 51st requires coins
  - Test: Insufficient balance shows clear error message

---

## Checkpoints

**After Phase 1 (Database Schema):**
- [ ] All 3 migrations run successfully
- [ ] Tables created with correct schema, indexes, foreign keys, check constraints
- [ ] Balance CHECK >= 0 enforced at database level

**After Phase 8 (Services):**
- [ ] CoinWalletService spend/earn/refund operations work correctly
- [ ] Optimistic locking prevents negative balance
- [ ] FreeTierService tracks quotas per month
- [ ] Event published on every coin operation

**After Phase 10 (Controllers):**
- [ ] All REST endpoints accessible and return correct responses
- [ ] Admin grant requires ADMIN role
- [ ] Pagination and filtering work on transactions endpoint
- [ ] Tenant isolation enforced

**After Phase 13 (Frontend Components):**
- [ ] BillingPage loads and displays balance and transactions
- [ ] CoinBalanceBadge visible in header
- [ ] CoinConfirmationDialog shows cost correctly
- [ ] Transaction filters and pagination work

**After Phase 14 (Testing):**
- [ ] All tests pass (unit, integration, concurrency, E2E)
- [ ] Tenant isolation verified
- [ ] Concurrent spend test proves balance never goes negative

---

## Progress Summary

**Total Tasks:** 47
**Completed:** 0
**In Progress:** 0
**Blocked:** 0
**Skipped:** 0
**Completion:** 0%

---

## Notes

### Key Dependencies
- Phase 2 (Enums/Config) and Phase 3 (Entities) depend on Phase 1 (Migrations)
- Phase 4 (DTOs) can run in parallel with Phase 3
- Phase 5 (Repositories) depends on Phase 3 (Entities)
- Phase 8 (Services) depends on Phase 4, 5, 6, 7
- Phase 10 (Controllers) depends on Phase 8, 9
- Phase 11-13 (Frontend) depends on Phase 10 for API contracts
- Phase 14 (Testing) depends on all previous phases

### Parallel Execution Opportunities
- Within Phase 2: All enums and config can be created in parallel
- Within Phase 3: All 3 entities can be written in parallel
- Within Phase 5: All 3 repositories can be written in parallel
- Within Phase 11: TypeScript types and API client can be written in parallel
- Within Phase 13: All billing components can be written in parallel
- Within Phase 14: Unit tests can be written in parallel with integration tests

### Risk Mitigation
- **Optimistic Locking Correctness:** Test concurrent spend early in Phase 14
- **Balance Check Constraint:** Verify database-level CHECK constraint works alongside JPA optimistic locking
- **Free Quota Month Boundary:** Test with mocked clock to simulate month transitions
- **Transaction History Performance:** Add proper indexes; test with 1000+ transactions

### Integration Points for Other Features
Other features will call CoinWalletService methods:
- **Feature 006 (Recruiter Chat):** `freeTierService.tryConsumeFreeTier()` then `coinWalletService.spend()`
- **Feature 008 (Resume Export):** `coinWalletService.spend()` with auto-refund on failure
- **Feature 009 (LinkedIn Analyzer):** `freeTierService.tryConsumeFreeTier()` for first suggestion, `coinWalletService.spend()` for extras

---

**Last Updated:** 2026-02-24
**Next Review:** After Phase 1 completion (database schema)
