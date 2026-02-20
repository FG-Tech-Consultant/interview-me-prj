# Technology Context

**Project:** Live Resume & Career Copilot
**Last Updated:** 2026-02-19

This document tracks the technology stack and architectural decisions made across features for AI agent context.

---

## Core Stack

**Language & Runtime:**
- Java 21 LTS (minimum required version)
- JVM: Eclipse Temurin, OpenJDK, or Oracle JDK
- Node.js 18+ LTS (for React frontend build)

**Backend Framework:**
- Spring Boot 3.x (latest stable 3.x series)
- Spring Framework 6.x
- Spring Security 6.x (JWT authentication, RBAC)
- Spring Data JPA (with tenant filtering)

**Frontend Framework:**
- React 18+ with TypeScript
- Component Library: MUI (Material-UI) or Chakra UI (TBD)
- State Management: React Query for server state, Context API for global state
- Routing: React Router
- Build Tool: Vite or Create React App

**Database:**
- PostgreSQL 14+ (primary database)
- pgvector extension (for embeddings/RAG)
- Liquibase 4.25.0+ (schema versioning)

**AI/LLM Integration:**
- OpenAI API (ChatGPT) - `spring-ai-openai-spring-boot-starter` or custom client
- Google Gemini API - Custom HTTP client
- Anthropic Claude API - Custom HTTP client
- Backend-controlled prompts and cost tracking

**Build & Dependency Management:**
- Gradle 8.5+ with Kotlin DSL
- Spring Dependency Management Plugin

---

## Technology Decisions

### Frontend Stack Choices

**React + TypeScript:**
- Type safety for large codebase
- Better IDE support and refactoring
- Prevents common runtime errors

**React Query:**
- Server state management with caching
- Automatic background refetching
- Optimistic updates for better UX

**MUI vs Chakra UI (TBD):**
- MUI: More components, larger community, heavier bundle
- Chakra UI: Simpler API, lighter, better accessibility defaults
- Decision deferred to Phase 1 implementation

### Backend Stack Choices

**PostgreSQL over MySQL/SQLite:**
- JSONB for semi-structured data (metrics, settings)
- pgvector for embeddings (RAG retrieval)
- Better performance for complex queries
- Production-ready for SaaS scale

**Spring Data JPA:**
- Standard ORM with Spring Boot integration
- Tenant filtering via AOP or custom filters
- Repository pattern for data access

**JWT Authentication:**
- Stateless authentication for horizontal scaling
- Token-based, no server-side session storage
- Refresh token pattern for security

### AI Integration Pattern

**"LLM in Backend" Pattern:**
- All LLM API calls from backend (never frontend)
- API keys stored securely (environment variables or encrypted DB)
- Cost tracking and usage limits enforced server-side
- Prompts versioned and managed centrally

**Multi-Provider Support:**
- Abstract `LlmClient` interface
- Implementations: `OpenAiLlmClient`, `GeminiLlmClient`, `ClaudeLlmClient`
- Router service selects provider based on action type and tenant config
- Enables A/B testing and cost optimization

**RAG with pgvector:**
- Store embeddings for skills, stories, experiences
- Retrieve relevant context for recruiter chat
- Semantic search over career data

---

## Feature 001: Project Setup & Tenancy

**Added Technologies:**

**Backend:**
- `spring-boot-starter-web` - Web server, REST API, embedded Tomcat
- `spring-boot-starter-security` - Authentication, JWT, RBAC
- `spring-boot-starter-data-jpa` - ORM, repositories
- `spring-boot-starter-actuator` - Health checks, monitoring endpoints
- `spring-boot-starter-validation` - Jakarta Bean Validation
- `postgresql` - PostgreSQL JDBC driver
- `liquibase-core` - Database schema versioning
- `jjwt` (JWT library) - JWT token generation and validation
- `net.logstash.logback:logstash-logback-encoder:7.4` - JSON structured logging

**Frontend:**
- React 18+ with TypeScript
- React Router for routing
- React Query for server state
- Axios or Fetch for HTTP
- Component library (MUI or Chakra - TBD)

**Testing:**
- `spring-boot-starter-test` - JUnit 5, AssertJ, Mockito
- `spring-security-test` - Security test utilities
- Jest + React Testing Library (frontend)

**Container:**
- Docker multi-stage builds
- Backend base image (build): `gradle:8.5-jdk21`
- Backend base image (runtime): `eclipse-temurin:21-jre-jammy`
- Frontend base image (build): `node:18-alpine`
- Frontend base image (runtime): `nginx:alpine`

**Key Patterns:**
- JWT-based authentication (stateless)
- BCrypt password encoding
- Soft multi-tenancy (tenant filtering in JPA repositories)
- Java 21 records for DTOs and domain events
- Configuration via `application.yml` + environment variables

---

## Future Features

**Phase 1 (MVP):**
- Profile CRUD endpoints
- Skills catalog management
- Stories (STAR format) CRUD
- Public/private visibility flags
- Recruiter chat (RAG + LLM)
- Resume export (PDF generation)
- LinkedIn Profile Score Analyzer
- Coin wallet (balance tracking, no payment)

**Phase 2:**
- Background presentation export
- Cover letter generator
- LinkedIn Inbox Assistant (draft-only)
- Packages (tokenized shareable links)
- Stripe integration for coin purchases

**Phase 3:**
- Multi-user per tenant
- Horizontal scaling (Kubernetes, load balancing)
- Advanced analytics
- Webhooks
- Public API

---

## Development Environment Constraints

**Windows Development Environment:**
- **Python is NOT available** on the Windows development machine
- **Do NOT use Python scripts** in automation or task execution
- **Use Windows-native tools instead**: PowerShell, cmd, sed, awk, or batch scripts
- **Error encountered**: `Python não foi encontrado; executar sem argumentos para instalar do Microsoft Store...`
- **Workaround**: When Python scripts are needed, convert logic to sed/awk/PowerShell or execute within Docker containers

**Example of problematic pattern:**
```bash
# DON'T: This will fail on Windows dev environment
cat > /tmp/update_tasks.py << 'PYEOF'
import re
...
PYEOF
python /tmp/update_tasks.py
```

**Recommended alternatives:**
- Use `sed` for text manipulation
- Use PowerShell scripts for complex logic
- Use Gradle tasks for build automation
- Execute Python within Docker if absolutely necessary

---

## Constitutional Alignment Notes

- **Simplicity First**: React SPA + Spring Boot REST API; standard patterns
- **Containerization**: Docker-first deployment (backend + frontend)
- **Modern Java**: Java 21 features (virtual threads, records, pattern matching)
- **Multi-Tenant Architecture**: Soft multi-tenancy with tenant filtering
- **AI Integration**: Backend-controlled LLM with cost tracking
- **Database Evolution**: Liquibase timestamp-based migrations
- **Observability**: Actuator + JSON logging from day one

---

## Configuration Management

**Environment Variables (12-Factor App):**
- `DATABASE_URL` - PostgreSQL connection string
- `DATABASE_USERNAME` - DB username
- `DATABASE_PASSWORD` - DB password
- `JWT_SECRET` - JWT signing key
- `OPENAI_API_KEY` - OpenAI API key
- `GEMINI_API_KEY` - Google Gemini API key
- `CLAUDE_API_KEY` - Anthropic Claude API key
- `CORS_ALLOWED_ORIGINS` - Frontend origin for CORS

**Spring Profiles:**
- `dev` - Development (local PostgreSQL via Docker Compose, verbose logging)
- `prod` - Production (managed PostgreSQL service, optimized logging, cloud storage)
- `test` - Testing (Testcontainers with PostgreSQL for integration tests; H2 only for simple unit tests that don't use JSONB/pgvector)

**File Storage:**
- `STORAGE_TYPE` - Storage backend type (`LOCAL`, `S3`, `GCS`)
- `STORAGE_LOCAL_PATH` - Local storage directory (development only, mounted volume)
- `STORAGE_S3_BUCKET` - S3 bucket name (production)
- `STORAGE_S3_REGION` - S3 region
- `STORAGE_GCS_BUCKET` - Google Cloud Storage bucket (alternative to S3)

**File Storage Strategy:**
- **Development**: Local filesystem (Docker volume mount at `/app/storage` or `./storage`)
- **Production**: Cloud object storage (AWS S3, Google Cloud Storage, Azure Blob Storage)
- **Use cases**: Resume PDFs, background presentation PDFs, cover letters, LinkedIn profile analysis PDFs
- **Database stores**: File metadata (ExportHistory) with reference to cloud storage URL

---

## Performance Considerations

**Backend:**
- Use virtual threads (Java 21) for concurrent LLM calls and async job processing
- Connection pooling (HikariCP) for database
- Enable HTTP/2 for better frontend-backend communication
- Caching strategy: Spring Cache with Redis (future)

**Frontend:**
- Code splitting (lazy load routes)
- React Query caching (stale-while-revalidate)
- Optimize bundle size (tree shaking, minification)
- CDN for static assets (future)

**Database:**
- Index tenant_id on all multi-tenant tables
- Index foreign keys and frequently queried columns
- Use JSONB indexes for semi-structured data queries
- pgvector indexes for embedding similarity search

---

## Security Considerations

**Authentication & Authorization:**
- JWT tokens with expiration (15 min access, 7 days refresh)
- BCrypt for password hashing (cost factor: 12)
- RBAC with roles: `OWNER`, `ADMIN`, `RECRUITER_VIEW`

**Data Protection:**
- Encrypt LLM API keys at rest (database encryption)
- HTTPS only (enforce TLS 1.3+)
- CORS configured for React frontend origin only
- SQL injection prevention (parameterized queries via JPA)
- XSS prevention (React escapes by default, validate input)

**Privacy:**
- Public/private content separation enforced in queries
- Recruiter chat MUST NOT access private content
- Tokenized package links use JWT with expiration
- GDPR compliance (data export, deletion endpoints - future)

---

**Document Version:** 1.0.1
**Last Updated:** 2026-02-19

**Changelog:**
- v1.0.1: Added file storage configuration (LOCAL/S3/GCS), clarified Spring profiles (PostgreSQL in dev, not embedded DB)
- v1.0.0: Initial version adapted from Travian Bot
