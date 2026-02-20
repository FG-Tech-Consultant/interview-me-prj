# Project Constitution

<!--
Sync Impact Report (Version 2.1.0 - New Principle Added):
- Version: 2.0.1 → 2.1.0 (MINOR - New principle added)
- Added Principle 9: Freemium Model and Coin-Based Monetization
- Renumbered subsequent principles: old 9→10, 10→11, 11→12, 12→13, 13→14
- Templates requiring updates: None (new principle, not replacing existing)
- Previous versions:
  - v2.0.1: Technical corrections (PostgreSQL clarifications, removed embedded DB references)
  - v2.0.0: Complete project context change (Travian Bot → Live Resume & Career Copilot)
- All principles adapted to new context while preserving valuable technical patterns
- Modified principles:
  - ALL principles: Updated examples and context from game automation → career platform
  - Principle 1: Simplicity First → Now emphasizes React SPA + REST API architecture
  - Principle 2: Containerization → Maintained (applies to any Spring Boot app)
  - Principle 3: Modern Java Standards → Maintained with Java 25
  - Principle 4: Data Sovereignty → Updated to multi-tenant career data with Postgres/pgvector
  - Principle 5: Browser Automation Reliability → REMOVED (not applicable to career platform)
  - Principle 6: Observability → Maintained with career platform examples
  - Principle 7: Security → Enhanced for multi-tenant SaaS with LinkedIn compliance
  - Principle 8: Reference Implementation → REMOVED (no game automation reference)
  - Principle 9: Modularity → Maintained with React + Spring Boot context
  - Principle 10: Database Evolution → Maintained (Liquibase still applies)
  - Principle 11: Action Queue → REMOVED (replaced with async job queue principle)
  - Principle 12: Piggybacking → REMOVED (not applicable)
  - Principle 13: Event-Driven → Maintained with career platform examples
- Added principles:
  - Principle 5: AI Integration and LLM Management (NEW)
  - Principle 8: Multi-Tenant Architecture (NEW)
  - Principle 11: Async Job Processing (NEW)
  - Principle 12: LinkedIn Compliance and ToS Respect (NEW)
- Removed sections:
  - Browser automation, game automation references
  - TravianBotSharp references
  - Piggybacking pattern (game-specific)
- Templates requiring updates:
  ⚠ spec-template.md (needs review for new principles)
  ⚠ plan-template.md (needs review for new principles)
  ⚠ tasks-template.md (needs review for new task categories)
- Follow-up TODOs:
  - Review all spec/plan/task templates for applicability to career platform
  - Update command files in .specify/templates/commands/
-->

**Project Name:** Live Resume & Career Copilot

**Version:** 2.1.0

**Ratification Date:** 2026-02-19

**Last Amended:** 2026-02-19

---

## Purpose

This constitution establishes the foundational principles, architectural decisions, and governance model for the Live Resume & Career Copilot project. This platform provides a modern career management solution combining a live resume/portfolio site, AI-powered recruiter chat, LinkedIn optimization tools, and document generation capabilities. Built with React, Spring Boot, Java 25, PostgreSQL 18, and deployed as a containerized SaaS application, this document ensures consistency, quality, and maintainability throughout the development lifecycle.

---

## Core Principles

### Principle 1: Simplicity First

**Statement:** The system MUST prioritize simplicity in both architecture and implementation. Technical solutions MUST favor straightforward patterns over complex abstractions unless complexity is demonstrably necessary. The frontend-backend separation MUST be clean and conventional.

**Rationale:** A clean separation between React frontend and Spring Boot REST API backend ensures maintainability, testability, and enables independent scaling. Simplicity reduces the learning curve for contributors and operational overhead.

**Application:**
- React SPA communicating via REST APIs under `/api/*`
- Prefer standard Spring Boot patterns over custom abstractions
- Use conventional React patterns (React Query, context for global state)
- Choose proven libraries (MUI/Chakra UI, Spring Data JPA)
- Keep backend layered: Controller → Service → Repository
- Avoid premature optimization; profile before optimizing

---

### Principle 2: Containerization as First-Class Citizen

**Statement:** The application MUST be designed from the ground up to run in Docker containers. All configuration, data persistence, and external dependencies MUST support containerized deployment without modification.

**Rationale:** Docker deployment ensures consistent runtime environments, simplified installation, and portability across systems. SaaS deployment requires container orchestration for horizontal scaling. Stateless application containers enable horizontal scaling while PostgreSQL handles persistence.

**Application:**
- All builds MUST produce Docker-compatible artifacts (Spring Boot JAR for backend, Nginx-served static assets for frontend)
- Configuration MUST support environment variables for container orchestration (DATABASE_URL, JWT_SECRET, LLM API keys, etc.)
- Backend container is stateless - no local file storage (use cloud storage for exports: S3, GCS, or local volume for development)
- PostgreSQL runs in separate container or managed service (AWS RDS, Google Cloud SQL, Azure Database)
- No hardcoded filesystem paths; use environment variables for file storage locations
- Dockerfile MUST be maintained as a primary artifact alongside source code
- Support Docker Compose for local development environment (backend + frontend + PostgreSQL)
- Use multi-stage builds to optimize image size (separate build and runtime stages)

---

### Principle 3: Modern Java Standards

**Statement:** The project MUST leverage Java 25 features and follow current Java ecosystem best practices. Code MUST be idiomatic and utilize language improvements that enhance readability, performance, or safety.

**Rationale:** Java 25 provides significant improvements including enhanced virtual threads, pattern matching, records, and performance optimizations. Using modern features ensures the codebase benefits from language evolution and remains relevant for future maintenance.

**Application:**
- Use Java 25 as the minimum required version
- Leverage virtual threads for concurrent LLM calls and async job processing
- Use records for immutable data transfer objects (DTOs)
- Apply pattern matching where it improves clarity
- Follow Spring Boot 3.x conventions aligned with Java 25
- Use Lombok annotations (@Slf4j, @RequiredArgsConstructor) for boilerplate reduction

---

### Principle 4: Data Sovereignty and Multi-Tenant Isolation

**Statement:** All user data, career profiles, and tenant-specific settings MUST be stored in a PostgreSQL relational database with strict tenant isolation. Multi-tenancy MUST be enforced at the application layer using tenant filters. Schema evolution MUST be managed through Liquibase migrations with explicit versioning.

**Rationale:** Multi-tenant SaaS requires strict data isolation to prevent data leaks. PostgreSQL provides JSONB for semi-structured data (metrics, settings), pgvector for embeddings (RAG), and robust performance for complex queries. Application-level tenant filtering provides scalability and simplicity. Liquibase provides declarative, version-controlled schema management that enables safe upgrades and rollbacks.

**Application:**
- Use PostgreSQL 18 as the primary and only database (production and development)
- Configure database connection via environment variables (DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD)
- All tenant-owned entities MUST include `tenant_id` column with index
- Tenant filter MUST be applied to all queries automatically via Spring AOP or custom JPA filter
- Use Liquibase for ALL schema changes (never manual SQL)
- Timestamp-based migration naming: `yyyyMMddHHmmss-description.xml`
- Master changelog at `src/main/resources/db/changelog/db.changelog-master.yaml`
- Each migration MUST include rollback instructions where possible
- Use pgvector extension for storing skill/story embeddings for RAG (vector similarity search)
- Use JSONB columns for semi-structured data (metrics, preferences, LLM prompts)

---

### Principle 5: AI Integration and LLM Management

**Statement:** All AI/LLM functionality MUST reside in the backend ("LLM in backend" pattern). The system MUST support multiple LLM providers (Gemini, ChatGPT, Claude) with per-tenant configuration. Prompts, RAG pipelines, and rate limits MUST be server-side. Cost tracking and usage limits MUST be enforced.

**Rationale:** Backend-controlled AI ensures security (API keys never exposed), cost control (usage limits per tenant), and flexibility (provider switching). Centralizing prompts enables versioning and A/B testing.

**Application:**
- Define `LlmClient` interface with implementations: `OpenAiLlmClient`, `GeminiLlmClient`, `ClaudeLlmClient`
- Store API keys encrypted in database or environment variables
- Use `LlmRouterService` to select provider/model based on action type and tenant settings
- Implement prompt templates in dedicated services (e.g., `RecruiterChatService`, `ExportPromptService`, `LinkedInAnalysisService`)
- Track LLM calls in metrics: count, latency, estimated cost
- Enforce coin-based usage limits for paid features
- Use RAG with pgvector for context retrieval (skills, stories, experiences)
- Cache LLM responses where appropriate (e.g., LinkedIn profile analysis)

---

### Principle 6: Observability and Debugging

**Statement:** The system MUST provide comprehensive logging, monitoring capabilities, and debugging tools suitable for diagnosing failures and system health in production SaaS environments.

**Rationale:** SaaS applications require visibility into tenant-specific issues, API latency, LLM usage, and export generation failures. Clear observability enables proactive issue resolution and cost optimization.

**Application:**
- Use structured logging (JSON format via Logstash encoder) with configurable levels
- Expose key metrics via Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/metrics`)
- Track critical metrics:
  - LLM calls per tenant (count, provider, model, latency, cost proxy)
  - Export generation (resume, deck, cover letter) success/failure rates
  - Coin spends per feature
  - API response times (p50, p95, p99)
- Use `@Slf4j` Lombok annotation for logger injection
- Never log credentials or sensitive PII in plain text (redact in logs)
- Maintain audit trail for coin transactions and exports
- Implement rate limiting alerts (e.g., free tier quota exceeded)

---

### Principle 7: Security, Privacy, and Credential Management

**Statement:** User credentials, API keys, and sensitive career data MUST be stored securely. The system MUST implement role-based access control (RBAC), JWT authentication, and respect data privacy (public/private content separation). Authentication mechanisms MUST follow industry-standard practices.

**Rationale:** The platform handles sensitive career information, LinkedIn credentials, and payment data. Proper security prevents unauthorized access, ensures compliance with privacy regulations, and builds user trust.

**Application:**
- JWT-based authentication for frontend-backend communication
- Encrypt stored passwords using BCrypt (Spring Security `BCryptPasswordEncoder`)
- Use environment variables or secret management for LLM API keys
- Implement RBAC: roles like `OWNER`, `ADMIN`, `RECRUITER_VIEW` (tokenized read-only)
- Never log credentials, API keys, or sensitive PII in plain text
- Implement session management with appropriate timeouts
- Use `@Transactional` annotations to enforce proper transaction boundaries
- Use `readOnly=true` for read-only transactional methods
- Respect public/private content flags:
  - Public content: visible on live resume and usable by recruiter chat
  - Private content: visible only to owner and internal tools; recruiter chat MUST ignore
- Tokenized package links use expiring or revocable tokens

---

### Principle 8: Multi-Tenant Architecture

**Statement:** The system MUST support soft multi-tenancy from day one. All tenant-owned entities MUST include `tenant_id` and be filtered by it in all backend queries. Users MUST be associated with tenants to support future multi-user organizations.

**Rationale:** Multi-tenant architecture enables SaaS business model, cost efficiency (shared infrastructure), and future growth (multiple users per organization). Soft multi-tenancy (single database, tenant-filtered queries) provides simplicity and scalability for early-stage SaaS.

**Application:**
- Define `Tenant` entity (id, name, createdAt, settings)
- Define `User` entity with `tenant_id` foreign key
- All domain entities (Profile, JobExperience, Education, UserSkill, Story, Package, etc.) MUST include `tenant_id`
- Implement tenant resolver from JWT (extract tenant from authenticated user)
- Apply tenant filter to all JPA repositories automatically (Spring AOP or custom filter)
- Prevent cross-tenant data leaks (NEVER return data from different tenant)
- LLM provider configurations can be per-tenant (override global defaults)
- Coin wallets are per-tenant
- Public endpoints (recruiter chat, package view) MUST respect tenant isolation

---

### Principle 9: Freemium Model and Coin-Based Monetization

**Statement:** The platform MUST implement a sustainable freemium business model with coin-based monetization. All premium features (exports, extra AI usage) MUST cost coins. The free tier MUST be generous enough to demonstrate value while incentivizing upgrades. Coin transactions MUST be tracked, auditable, and reversible (for refunds).

**Rationale:** A coin-based model provides flexibility for users (pay-as-you-go) while ensuring platform sustainability. Clear pricing and transparent coin usage build trust. Freemium allows users to experience core value (live resume, basic chat) before paying, reducing friction and increasing conversion.

**Application:**

**Coin Wallet System:**
- Each tenant has ONE `CoinWallet` with balance (integer, non-negative)
- All coin movements tracked in `CoinTransaction` table with:
  - `type`: EARN, SPEND, REFUND, PURCHASE
  - `amount`: Integer (positive for earn/purchase, negative for spend)
  - `refType` and `refId`: Link to related entity (e.g., EXPORT → exportHistoryId)
  - `createdAt`: Timestamp for audit trail
- Wallet balance MUST NEVER go negative (enforce in service layer with optimistic locking)

**Free Tier (Always-On):**
- Live public resume page (unlimited, free forever)
- Recruiter chat: X messages per month (e.g., 50 free messages/month per tenant)
- LinkedIn inbox drafts: Y drafts per month (e.g., 10 free drafts/month)
- LinkedIn Profile Score Analyzer: Unlimited scoring, but only 1 free suggestion per section

**Coin-Based Features (Always Cost Coins):**
- ALL exports (resume, deck, cover letter): Cost coins
- Recruiter chat beyond free tier: Cost coins per message
- LinkedIn inbox drafts beyond free tier: Cost coins per draft
- LinkedIn Score Analyzer extra suggestions: Cost coins per additional suggestion (beyond first free one)

**Coin Pricing Strategy:**
- Define base costs per feature in configuration (e.g., `RESUME_EXPORT_COST=10`, `CHAT_MESSAGE_COST=1`)
- Costs stored in database or environment variables for easy adjustment
- Display costs to user BEFORE action ("This will cost X coins")
- Track "estimated cost" in LLM metrics for future pricing adjustments

**Coin Acquisition:**
- Phase 1 (MVP): Manual coin grants by admin (for testing)
- Phase 2: Stripe integration for coin purchases (packs: 100 coins, 500 coins, 1000 coins)
- Phase 3: Subscription tiers ("Pro" plan with auto-refilling coins per month)

**Usage Limits and Quotas:**
- Implement rate limiting per tenant (e.g., max 100 chat messages/day even with coins to prevent abuse)
- Track free tier usage separately from paid usage
- Reset free tier quotas monthly (e.g., 1st of each month)
- Notify users when approaching free tier limits

**Transparency and Refunds:**
- All coin transactions MUST be visible to user in billing page
- Failed operations (e.g., export generation error) MUST refund coins automatically
- Admins can manually refund coins for customer support cases
- Display current balance and recent transactions prominently in UI

---

### Principle 10: Full-Stack Modularity and Separation of Concerns

**Statement:** All layers of the application MUST be organized into specialized, cohesive components with clear boundaries and single responsibilities. This applies to both backend (Java services) and frontend (React components). Components exceeding reasonable size limits MUST be refactored.

**Rationale:** Modular architecture improves code maintainability, testability, reusability, and team collaboration. Specialized components with narrow scope are easier to understand, modify, and debug.

**Application:**

**Backend (Java Services):**
- Follow layered architecture: Controller → Service → Repository
- Domain packages (modular structure):
  - `com.yourapp.config` - Security, CORS, LLM provider config
  - `com.yourapp.auth` - User registration, login, JWT
  - `com.yourapp.tenancy` - Tenant entity, tenant resolver, multi-tenant filter
  - `com.yourapp.profile` - Profile, jobs, education
  - `com.yourapp.skills` - Skills catalog, user skills, tagging
  - `com.yourapp.experience` - Projects and stories
  - `com.yourapp.packages` - Packages and tokenized share links
  - `com.yourapp.exports` - Resume/deck/cover letter generation
  - `com.yourapp.ai` - LLM client abstraction, prompts, RAG pipeline
  - `com.yourapp.chat` - Recruiter chat sessions/messages
  - `com.yourapp.linkedin` - Inbox assistant + profile analyzer
  - `com.yourapp.billing` - Coins, transactions, usage limits
- Each service MUST have a clearly defined responsibility
- Services SHOULD NOT exceed 500 lines of code; split into sub-services if larger
- Use dependency injection (`@Service`, `@RequiredArgsConstructor`)
- Apply Single Responsibility Principle (SRP) rigorously

**Frontend (React Components):**
- Organize components by feature/domain:
  - `components/` - Generic reusable components (Form, TextField, TagInput, SkillChip, StoryCard, ChatBox, CoinBalanceBadge)
  - `pages/` - Route-level page components (DashboardPage, ProfileEditorPage, PackagesPage, ExportsPage, LinkedInAssistantPage, BillingPage)
  - `api/` - API client modules (authApi.ts, profileApi.ts, skillsApi.ts, chatApi.ts, linkedinApi.ts, billingApi.ts)
  - `hooks/` - Custom React hooks (useProfile, useSkills, useStories, useChatSession, useCoins)
  - `state/` - Global state management (auth, tenant context, feature flags)
- Each React component SHOULD NOT exceed 300 lines; split if larger
- Use React Query for server state management
- Use TypeScript for type safety
- Prefer composition over inheritance

---

### Principle 11: Database Schema Evolution

**Statement:** All database schema changes MUST be managed through Liquibase migrations with explicit versioning, timestamp-based naming, and rollback support. Direct SQL modifications to production schemas are PROHIBITED. Each migration MUST be atomic, idempotent, and include descriptive changelogs.

**Rationale:** Database schema is a critical project artifact that requires version control, auditability, and safe deployment across environments. Liquibase provides declarative schema management that enables reproducible builds, rollback capabilities, and prevents schema drift.

**Application:**
- Use Liquibase Core 4.25.0+ as the migration framework
- Master changelog: `src/main/resources/db/changelog/db.changelog-master.yaml`
- **Naming Convention (REQUIRED):**
  - **Timestamp-based naming:** `yyyyMMddHHmmss-description.xml`
  - Example: `20260219143000-add-hero-inventory-table.xml`
  - Ensures chronological ordering and prevents conflicts in team environments
- Each changeset MUST:
  - Have a unique identifier (timestamp ensures uniqueness)
  - Include author and ID attributes in XML
  - Contain a single logical schema change
  - Include rollback instructions where possible (use `<rollback>` tags)
  - Use declarative XML format (avoid raw SQL unless necessary)
  - Be added to `db.changelog-master.yaml` in chronological order
- Never modify existing changesets after deployment (create new ones to fix issues)
- Test migrations on fresh database before committing
- Document breaking changes in commit messages
- Liquibase MUST run automatically on application startup via Spring Boot integration

**PostgreSQL-Specific Patterns:**
- Use `BIGSERIAL` for auto-increment primary keys
- Use `JSONB` for semi-structured data (e.g., metrics, settings)
- Use `TIMESTAMPTZ` for timestamps (timezone-aware)
- Use `VARCHAR` instead of `TEXT` when length limits make sense
- Define foreign keys and indexes explicitly
- Use `pgvector` extension for embeddings storage

---

### Principle 12: Async Job Processing and Background Tasks

**Statement:** Long-running operations (export generation, bulk LinkedIn analysis, email notifications) MUST use asynchronous job processing via background queues. Jobs MUST be retryable, idempotent, and include proper error handling.

**Rationale:** Synchronous processing of heavy operations (PDF generation, LLM calls) blocks HTTP threads and degrades user experience. Async job queues enable horizontal scaling, retry logic, and better resource utilization.

**Application:**
- Use Spring's `@Async` for simple async tasks
- For complex workflows, use job queue library (e.g., JobRunr, Spring Batch)
- Define job types:
  - `GENERATE_RESUME` - Resume PDF generation
  - `GENERATE_BACKGROUND_PRESENTATION` - Deck generation
  - `GENERATE_COVER_LETTER` - Cover letter generation
  - `ANALYZE_LINKEDIN_PROFILE` - LinkedIn profile scoring
  - `SEND_NOTIFICATION` - Email/Discord/Telegram notifications
- Job queue MUST support:
  - Priority levels (HIGH, NORMAL, LOW)
  - Retry with exponential backoff (max 3 retries)
  - Job status tracking (PENDING, IN_PROGRESS, COMPLETED, FAILED)
  - Dead letter queue for permanently failed jobs
- Store job metadata (tenant, user, parameters, result) in database
- Expose job status API for frontend polling or SSE
- Use virtual threads (Java 25) for concurrent job processing
- Track job metrics (count, latency, success rate) per type

---

### Principle 13: LinkedIn Compliance and ToS Respect

**Statement:** All LinkedIn-related features MUST respect LinkedIn's Terms of Service and avoid aggressive automation. The system MUST prefer user-initiated actions and manual workflows over automated scraping or message sending.

**Rationale:** Violating LinkedIn ToS risks account bans, legal action, and reputational damage. Ethical automation ensures long-term viability and user trust.

**Application:**
- **LinkedIn Inbox Assistant:**
  - DRAFT-ONLY: Generate reply drafts for manual copy/paste by user
  - Never auto-send messages via LinkedIn API or browser automation
  - UI clearly indicates "Copy reply" or "Insert into LinkedIn"
- **LinkedIn Profile Score Analyzer:**
  - Prefer PDF upload (user manually exports LinkedIn as PDF)
  - If URL-based fetching, clearly mark as "user-initiated analysis"
  - Respect rate limits and avoid scraping patterns
- Never automate:
  - Connection requests
  - Mass messaging
  - Profile scraping at scale
- Clearly document compliance approach in user-facing docs
- Monitor LinkedIn policy changes and adapt features accordingly

---

### Principle 14: Event-Driven Architecture and Reactive Patterns

**Statement:** The system SHOULD adopt event-driven patterns for domain events (resources updated, exports completed, coin transactions). Components SHOULD respond to state changes via event listeners rather than tight coupling. This enables reactive workflows and better decoupling.

**Rationale:** Event-driven architecture enables loosely-coupled components, reactive workflows, and better scalability. Domain events create an audit trail and enable future features (e.g., webhooks, real-time notifications).

**Application:**

**Domain Events:**
- Define domain events for significant state changes:
  - `ProfileUpdatedEvent(tenantId, userId, timestamp)`
  - `ExportCompletedEvent(tenantId, userId, exportType, fileUrl, coinsSpent)`
  - `CoinTransactionEvent(tenantId, walletId, amount, type, refId)`
  - `LinkedInAnalysisCompletedEvent(tenantId, userId, analysisId, overallScore)`
- Publish events using Spring's `ApplicationEventPublisher`:
  ```java
  @Service
  public class ExportService {
      @Autowired
      private ApplicationEventPublisher eventPublisher;

      public void generateResume(Tenant tenant, User user, ...) {
          // ... generation logic
          eventPublisher.publishEvent(new ExportCompletedEvent(tenant.getId(), user.getId(), "RESUME", fileUrl, coinsSpent));
      }
  }
  ```
- Subscribe to events using `@EventListener`:
  ```java
  @Component
  public class NotificationService {
      @EventListener
      public void onExportCompleted(ExportCompletedEvent event) {
          // Send notification to user
      }
  }
  ```

**Event Processing Rules:**
- Event listeners MUST be non-blocking (use `@Async` if needed)
- Events MUST be immutable (use Java records for event classes)
- Event handlers MUST NOT throw uncaught exceptions (use try-catch)
- Events SHOULD include timestamps for audit trails

**Reactive UI Updates:**
- Frontend SHOULD poll for state changes or use Server-Sent Events (SSE) for real-time updates
- Backend emits events that trigger UI refresh notifications

---

## Governance

### Amendment Procedure

1. Proposed amendments MUST be submitted as a pull request modifying this constitution
2. Amendments MUST include rationale explaining the change necessity
3. The `CONSTITUTION_VERSION` MUST be incremented following semantic versioning:
   - **MAJOR**: Breaking changes to principles or governance (e.g., removing a principle, changing project context)
   - **MINOR**: Adding new principles or significantly expanding existing ones
   - **PATCH**: Clarifications, wording improvements, typo fixes
4. `LAST_AMENDED` date MUST be updated to the merge date
5. The Sync Impact Report comment MUST be updated listing all changes and affected templates

### Versioning Policy

This constitution follows semantic versioning (MAJOR.MINOR.PATCH). Each version change MUST be documented in the Sync Impact Report comment at the top of this file.

### Compliance and Review

- All feature specifications MUST include a "Constitution Compliance" section referencing applicable principles
- Plan documents MUST validate designs against relevant constitutional principles
- Task breakdowns MUST categorize work items by the principles they support
- Quarterly reviews SHOULD assess whether principles remain relevant or require amendment

### Dependent Artifacts

The following template files MUST remain synchronized with this constitution:

- `.specify/templates/plan-template.md` - Design planning template
- `.specify/templates/spec-template.md` - Feature specification template
- `.specify/templates/tasks-template.md` - Task breakdown template
- `.specify/templates/commands/*.md` - Workflow command definitions

When amending this constitution, review and update dependent templates to maintain alignment.

---

## Ratification

This constitution was ratified on 2026-02-19 as version 2.0.0 to establish the foundational governance and technical principles for the Live Resume & Career Copilot project (adapted from Travian Bot project template).

**Signatories:** Project Creator

**Status:** Active
