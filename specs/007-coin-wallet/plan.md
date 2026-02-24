# Implementation Plan: Coin Wallet & Billing System

**Feature ID:** 007-coin-wallet
**Status:** Design Complete
**Created:** 2026-02-24
**Last Updated:** 2026-02-24
**Version:** 1.0.0

---

## Executive Summary

This implementation plan defines the technical design for the Coin Wallet & Billing System, which provides coin-based monetization for the Interview Me platform. The implementation follows the established three-layer architecture (Controller -> Service -> Repository) using Spring Boot 4.x REST APIs on the backend and React 18 with MUI components on the frontend. All entities implement multi-tenant isolation via automatic Hibernate filtering.

The core design centers on a CoinWallet entity per tenant with optimistic locking (@Version) to guarantee balance never goes negative under concurrent access. CoinTransaction records provide an immutable audit trail of all coin movements. A FreeTierUsage table tracks monthly free quotas per feature per tenant. Coin costs are configurable via application.yml.

**Key Deliverables:**
- Backend: 3 JPA entities (CoinWallet, CoinTransaction, FreeTierUsage), 3 repositories, 2 services (CoinWalletService, FreeTierService), 2 REST controllers (BillingController, AdminBillingController), 6+ DTOs (Java records)
- Database: 3 Liquibase migrations creating tables with proper indexes, constraints, and foreign keys
- Frontend: BillingPage, CoinBalanceBadge, CoinConfirmationDialog, API client modules, TanStack Query hooks
- Testing: Unit tests for services, integration tests for REST endpoints, concurrent spend tests

---

## Architecture Design

### Backend Package Structure

```
com.interviewme.billing/
  controller/
    BillingController.java           # User-facing billing endpoints
    AdminBillingController.java      # Admin endpoints for coin grants
  service/
    CoinWalletService.java           # Core wallet operations (spend, earn, refund, balance)
    FreeTierService.java             # Free-tier quota checking and tracking
  repository/
    CoinWalletRepository.java        # CoinWallet data access
    CoinTransactionRepository.java   # CoinTransaction data access
    FreeTierUsageRepository.java     # FreeTierUsage data access
  model/
    CoinWallet.java                  # JPA entity
    CoinTransaction.java            # JPA entity
    FreeTierUsage.java               # JPA entity
    TransactionType.java             # Enum: EARN, SPEND, REFUND, PURCHASE
    RefType.java                     # Enum: ADMIN_GRANT, EXPORT, CHAT_MESSAGE, etc.
    FeatureType.java                 # Enum: CHAT_MESSAGE, LINKEDIN_DRAFT, etc.
  dto/
    WalletResponse.java              # Wallet balance response (record)
    TransactionResponse.java         # Transaction history item (record)
    TransactionPageResponse.java     # Paginated transaction response (record)
    AdminGrantRequest.java           # Admin grant request (record)
    SpendRequest.java                # Internal spend request (record)
    FeatureCostResponse.java         # Feature costs response (record)
    QuotaStatusResponse.java         # Free tier quota status (record)
  config/
    BillingProperties.java           # @ConfigurationProperties for billing costs/quotas
  event/
    CoinTransactionEvent.java        # Domain event (record)
  exception/
    InsufficientBalanceException.java
    DuplicateRefundException.java
    WalletNotFoundException.java
```

### Database Schema Design

#### coin_wallet Table

```sql
CREATE TABLE coin_wallet (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL UNIQUE REFERENCES tenant(id),
    balance         BIGINT          NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version         BIGINT          NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_coin_wallet_tenant_id ON coin_wallet(tenant_id);
```

#### coin_transaction Table

```sql
CREATE TABLE coin_transaction (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL REFERENCES tenant(id),
    wallet_id       BIGINT          NOT NULL REFERENCES coin_wallet(id),
    type            VARCHAR(20)     NOT NULL CHECK (type IN ('EARN', 'SPEND', 'REFUND', 'PURCHASE')),
    amount          INTEGER         NOT NULL,
    description     VARCHAR(500),
    ref_type        VARCHAR(50),
    ref_id          VARCHAR(100),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_coin_transaction_tenant_id ON coin_transaction(tenant_id);
CREATE INDEX idx_coin_transaction_wallet_id ON coin_transaction(wallet_id);
CREATE INDEX idx_coin_transaction_type ON coin_transaction(type);
CREATE INDEX idx_coin_transaction_ref_type_ref_id ON coin_transaction(ref_type, ref_id);
CREATE INDEX idx_coin_transaction_created_at ON coin_transaction(created_at);
```

#### free_tier_usage Table

```sql
CREATE TABLE free_tier_usage (
    id              BIGSERIAL       PRIMARY KEY,
    tenant_id       BIGINT          NOT NULL REFERENCES tenant(id),
    feature_type    VARCHAR(50)     NOT NULL,
    usage_count     INTEGER         NOT NULL DEFAULT 0,
    year_month      VARCHAR(7)      NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_free_tier_usage_tenant_feature_month
    ON free_tier_usage(tenant_id, feature_type, year_month);
```

### API Endpoints Design

#### User-Facing Billing Endpoints (BillingController)

| Method | Path | Description | Auth | Response |
|--------|------|-------------|------|----------|
| GET | `/api/billing/wallet` | Get current wallet balance | JWT (USER) | 200 WalletResponse |
| GET | `/api/billing/transactions` | Get transaction history (paginated) | JWT (USER) | 200 TransactionPageResponse |
| GET | `/api/billing/costs` | Get feature costs configuration | JWT (USER) | 200 Map<String, Integer> |
| GET | `/api/billing/quota/{featureType}` | Get free tier quota status | JWT (USER) | 200 QuotaStatusResponse |

#### Admin Billing Endpoints (AdminBillingController)

| Method | Path | Description | Auth | Response |
|--------|------|-------------|------|----------|
| POST | `/api/admin/wallets/{tenantId}/grant` | Grant coins to tenant | JWT (ADMIN) | 200 WalletResponse |
| GET | `/api/admin/wallets/{tenantId}` | View tenant wallet | JWT (ADMIN) | 200 WalletResponse |

#### Query Parameters for GET `/api/billing/transactions`

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | int | 0 | Page number (0-indexed) |
| size | int | 20 | Page size (max 100) |
| type | String | null | Filter by transaction type (EARN, SPEND, REFUND, PURCHASE) |
| refType | String | null | Filter by reference type |
| from | String | null | Start date filter (ISO 8601) |
| to | String | null | End date filter (ISO 8601) |

### Service Layer Design

#### CoinWalletService

The core service for all coin operations. Uses optimistic locking for balance safety.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CoinWalletService {

    private final CoinWalletRepository walletRepository;
    private final CoinTransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long tenantId);

    @Transactional
    public CoinTransaction spend(Long tenantId, int amount, RefType refType,
                                  String refId, String description);

    @Transactional
    public CoinTransaction earn(Long tenantId, int amount, RefType refType,
                                 String refId, String description);

    @Transactional
    public CoinTransaction refund(Long tenantId, Long originalTransactionId);

    @Transactional
    public CoinWallet getOrCreateWallet(Long tenantId);

    @Transactional(readOnly = true)
    public TransactionPageResponse getTransactions(Long tenantId, int page, int size,
                                                    String type, String refType,
                                                    OffsetDateTime from, OffsetDateTime to);
}
```

**Spend Operation Flow:**
1. Get or create wallet for tenant
2. Check balance >= amount (throw InsufficientBalanceException if not)
3. Decrement balance: `wallet.setBalance(wallet.getBalance() - amount)`
4. Save wallet (optimistic locking via @Version detects concurrent modification)
5. Create CoinTransaction(type=SPEND, amount=-amount)
6. Publish CoinTransactionEvent
7. On OptimisticLockingFailureException: retry up to 3 times (re-fetch wallet, re-check balance)

**Retry Pattern for Optimistic Locking:**
```java
@Retryable(
    retryFor = OptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100)
)
public CoinTransaction spend(...) { ... }
```
Alternatively, implement manual retry loop if spring-retry is not already a dependency.

#### FreeTierService

Manages monthly free-tier quota tracking.

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FreeTierService {

    private final FreeTierUsageRepository usageRepository;
    private final BillingProperties billingProperties;

    @Transactional(readOnly = true)
    public QuotaStatusResponse getQuotaStatus(Long tenantId, FeatureType featureType);

    @Transactional
    public boolean tryConsumeFreeTier(Long tenantId, FeatureType featureType);

    private String getCurrentYearMonth();
    private int getQuotaLimit(FeatureType featureType);
}
```

**Quota Check Flow:**
1. Get current year-month string (e.g., "2026-02")
2. Find or create FreeTierUsage record for (tenantId, featureType, yearMonth)
3. If usage_count < limit: increment count, return true (free)
4. If usage_count >= limit: return false (coins required)

### Configuration Design

```yaml
# application.yml
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
  retry:
    max-attempts: 3
    backoff-ms: 100
```

```java
@ConfigurationProperties(prefix = "billing")
@Data
public class BillingProperties {
    private Map<String, Integer> costs = new HashMap<>();
    private FreeTierConfig freeTier = new FreeTierConfig();
    private RetryConfig retry = new RetryConfig();

    @Data
    public static class FreeTierConfig {
        private int chatMessagesPerMonth = 50;
        private int linkedinDraftsPerMonth = 10;
    }

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private int backoffMs = 100;
    }
}
```

### Event-Driven Pattern

```java
public record CoinTransactionEvent(
    Long tenantId,
    Long walletId,
    Long transactionId,
    String type,
    int amount,
    String refType,
    String refId,
    Instant timestamp
) {}
```

Published via `ApplicationEventPublisher` after every successful coin operation. Event listeners can:
- Log metrics (coin spend per feature)
- Update frontend via SSE (future)
- Trigger notifications (future)

### Frontend Design

#### Component Structure

```
frontend/src/
  api/
    billingApi.ts               # API client for billing endpoints
  types/
    billing.ts                  # TypeScript interfaces
  hooks/
    useBilling.ts               # TanStack Query hooks
  pages/
    BillingPage.tsx             # Main billing page
  components/
    billing/
      CoinBalanceBadge.tsx      # Header coin balance badge
      CoinConfirmationDialog.tsx # Cost confirmation dialog
      TransactionHistoryTable.tsx # Transaction table component
      TransactionTypeBadge.tsx  # Colored badge for transaction type
      QuotaStatusCard.tsx       # Free tier quota status display
```

#### BillingPage Layout

```
+------------------------------------------------------+
|  [Coin Icon] Current Balance: 150 coins              |
+------------------------------------------------------+
|  Free Tier Status                                     |
|  +--------------------------------------------------+|
|  | Chat Messages: 35/50 used | LinkedIn Drafts: 2/10||
|  +--------------------------------------------------+|
|                                                       |
|  Transaction History                                  |
|  [Filter: Type ▼] [Filter: Date Range]               |
|  +--------------------------------------------------+|
|  | Date       | Type    | Description      | Amount ||
|  |------------|---------|------------------|--------||
|  | 2026-02-24 | EARN    | Admin grant      | +100   ||
|  | 2026-02-24 | SPEND   | Resume export    | -10    ||
|  | 2026-02-23 | REFUND  | Export failed     | +10    ||
|  +--------------------------------------------------+|
|  [< Previous] [Page 1 of 5] [Next >]                 |
+------------------------------------------------------+
```

#### CoinConfirmationDialog

```
+--------------------------------------+
|  Confirm Coin Spend                   |
|                                       |
|  Feature: Resume Export               |
|  Cost: 10 coins                       |
|  Current Balance: 150 coins           |
|  Balance After: 140 coins             |
|                                       |
|  [Cancel]  [Confirm - Spend 10 Coins] |
+--------------------------------------+
```

### Integration Points

Other features will integrate with the billing system via `CoinWalletService`:

```java
// In ExportService (Feature 008)
@Transactional
public ExportResult generateResume(Long tenantId, ExportRequest request) {
    int cost = billingProperties.getCosts().get("resume-export");

    // Spend coins first
    CoinTransaction tx = coinWalletService.spend(tenantId, cost,
        RefType.EXPORT, null, "Resume export");

    try {
        ExportResult result = doGenerateResume(request);
        // Update transaction ref_id with export ID
        return result;
    } catch (Exception e) {
        // Auto-refund on failure
        coinWalletService.refund(tenantId, tx.getId());
        throw e;
    }
}

// In RecruiterChatService (Feature 006)
@Transactional
public ChatResponse processMessage(Long tenantId, ChatRequest request) {
    boolean isFree = freeTierService.tryConsumeFreeTier(tenantId, FeatureType.CHAT_MESSAGE);

    if (!isFree) {
        int cost = billingProperties.getCosts().get("chat-message");
        coinWalletService.spend(tenantId, cost,
            RefType.CHAT_MESSAGE, request.getSessionId(), "Chat message");
    }

    return doProcessMessage(request);
}
```

---

## DTO Definitions (Java Records)

```java
// WalletResponse.java
public record WalletResponse(
    Long id,
    Long tenantId,
    long balance,
    Instant createdAt,
    Instant updatedAt
) {}

// TransactionResponse.java
public record TransactionResponse(
    Long id,
    String type,
    int amount,
    String description,
    String refType,
    String refId,
    Instant createdAt
) {}

// TransactionPageResponse.java
public record TransactionPageResponse(
    List<TransactionResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}

// AdminGrantRequest.java
public record AdminGrantRequest(
    @NotNull @Min(1) Integer amount,
    @Size(max = 500) String description
) {}

// FeatureCostResponse.java
public record FeatureCostResponse(
    Map<String, Integer> costs,
    Map<String, Integer> freeQuotas
) {}

// QuotaStatusResponse.java
public record QuotaStatusResponse(
    String featureType,
    int used,
    int limit,
    boolean quotaExceeded,
    String yearMonth
) {}
```

---

## Migration Strategy

Three Liquibase migration files:

1. `20260224140000-create-coin-wallet-table.xml` - coin_wallet table with unique tenant_id constraint, balance CHECK >= 0
2. `20260224140100-create-coin-transaction-table.xml` - coin_transaction table with all indexes and type CHECK constraint
3. `20260224140200-create-free-tier-usage-table.xml` - free_tier_usage table with unique composite index

All added to `db.changelog-master.yaml` in order.

---

## Testing Strategy

### Unit Tests
- `CoinWalletServiceTest` - Test spend (success, insufficient balance), earn, refund (success, duplicate), getWallet (auto-create)
- `FreeTierServiceTest` - Test quota check (under limit, at limit, new month), tryConsumeFreeTier

### Integration Tests
- `BillingControllerIntegrationTest` - Test all user-facing endpoints (GET wallet, GET transactions with filters/pagination, GET costs)
- `AdminBillingControllerIntegrationTest` - Test admin grant (success, invalid amount, non-admin rejection)
- `CoinWalletConcurrencyTest` - Test 10 concurrent spend threads, verify balance consistency and no negative balance
- `TenantIsolationBillingTest` - Test cross-tenant wallet access returns 404

### Frontend Tests
- `BillingPage.test.tsx` - Test page rendering, transaction list, pagination
- `CoinBalanceBadge.test.tsx` - Test badge display and click navigation
- `CoinConfirmationDialog.test.tsx` - Test cost display, insufficient balance warning

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Concurrent spend causing negative balance | Medium | High | Optimistic locking with retry; database CHECK constraint as safety net |
| Optimistic lock contention under high load | Low | Medium | Retry with backoff (3 attempts, 100ms delay) |
| Free quota reset missed at month boundary | Low | Low | Use year_month string comparison, not time-based reset |
| Transaction table growing large | Medium | Low | Proper indexes; pagination; future: archival strategy |
| Configuration changes not reflected | Low | Low | Use @ConfigurationProperties with @RefreshScope if needed |

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-24 | Initial design              | Claude Code |
