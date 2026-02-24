# Implementation Plan: Project Base Structure

**Feature ID:** 001-project-base-structure
**Status:** Design Complete
**Created:** 2026-02-22
**Last Updated:** 2026-02-22
**Version:** 1.0.0

---

## Executive Summary

This implementation plan establishes the foundational project structure for the Live Resume & Career Copilot platform. It creates a production-ready Spring Boot 3.4.x + Java 25 backend with React 18 + TypeScript frontend, PostgreSQL 18 database with pgvector extension, Docker Compose orchestration, JWT authentication, and multi-tenant infrastructure using Liquibase for schema management.

**Key Deliverables:**
- Backend: Spring Boot 3.4.x with Java 25, Gradle 8.5+, JWT auth, multi-tenant support
- Frontend: React 18 + TypeScript, Vite, TanStack Query, MUI
- Database: PostgreSQL 18 + pgvector, Liquibase migrations
- Infrastructure: Docker Compose with multi-stage builds
- Documentation: API spec (OpenAPI), data model, quickstart guide

**Timeline:** 5-7 days | **Complexity:** Medium

---

## Constitution Check - ✅ PASSED

All applicable principles (1, 2, 3, 4, 6, 7, 8, 10, 11) fully satisfied. See [full validation in complete plan](./plan-full.md).

**Key Compliance Points:**
- **Simplicity:** Standard Spring Boot + React patterns, no custom abstractions
- **Containerization:** Docker-first design with multi-stage builds
- **Modern Java:** Java 25 with virtual threads, records, Lombok
- **Data Sovereignty:** PostgreSQL 18 with tenant filtering via Hibernate + AOP
- **Observability:** Spring Boot Actuator, structured logging
- **Security:** JWT auth, BCrypt, environment-based secrets, CORS
- **Multi-Tenancy:** Discriminator pattern with automatic query filtering
- **Modularity:** Domain-based backend packages, feature-based frontend
- **Schema Evolution:** Liquibase timestamp-based migrations

---

## Technical Architecture

### System Diagram

```
React SPA (Vite/Nginx) → Spring Boot 3.4.x (Java 25) → PostgreSQL 18 + pgvector
                          ├─ JWT Auth (Spring Security 6)
                          ├─ Multi-Tenant Filters (Hibernate + AOP)
                          └─ Liquibase Migrations
```

### Request Flow

1. **Registration:** React → POST /api/auth/register → Create Tenant + User → Return JWT
2. **Login:** React → POST /api/auth/login → Validate credentials → Return JWT
3. **Authenticated:** React → GET /api/auth/me → JWT Filter → Extract tenant → Return user

---

## Technology Stack

**Backend:**
- Java 25, Spring Boot 3.4.x, Gradle 8.5+
- PostgreSQL 18 driver, Liquibase 4.25.1, HikariCP
- Spring Security 6, JJWT 0.12.5, BCrypt
- Lombok, Hypersistence Utils (JSONB)

**Frontend:**
- React 18, TypeScript, Vite
- TanStack Query, React Router, MUI
- Axios with JWT interceptors

**Infrastructure:**
- Docker + Docker Compose
- PostgreSQL 18 (pgvector/pgvector:pg18)
- Nginx (production frontend)

---

## Implementation Phases

### Phase 1: Project Init & Docker (Day 1-2)
- Initialize Gradle project with Java 25 toolchain
- Create multi-stage Dockerfiles (backend, frontend)
- Configure docker-compose.yml (dev + prod)
- Set up .env.example and .gitignore

**Validation:** `docker-compose up` starts all containers

### Phase 2: Database & Liquibase (Day 2)
- Configure Spring Data JPA + PostgreSQL
- Create Liquibase master changelog
- Create initial migration (tenant, user tables)
- Define entities with Hibernate filters
- Create repositories

**Validation:** Migrations run automatically, tables created

### Phase 3: Security & JWT (Day 3-4)
- Implement JWT service (generate, validate, extract claims)
- Configure Spring Security with JWT filter chain
- Create auth endpoints (register, login, /me)
- Add CORS for React frontend
- Create DTOs with validation

**Validation:** Can register, login, access protected endpoints

### Phase 4: Multi-Tenancy (Day 4)
- Create TenantContext (ThreadLocal)
- Implement JWT tenant resolver
- Add AOP aspect for Hibernate filters
- Test tenant isolation

**Validation:** Different tenants cannot access each other's data

### Phase 5: React Frontend (Day 5-6)
- Initialize Vite + React + TypeScript
- Install deps (TanStack Query, MUI, Axios)
- Create auth API client with JWT interceptor
- Build Login, Register, Dashboard pages
- Configure React Router

**Validation:** Full auth flow works frontend-to-backend

### Phase 6: Documentation & Testing (Day 6-7)
- Write OpenAPI specification
- Create data model, quickstart guide
- Write unit + integration tests
- Test manual flows with Postman

**Validation:** All tests pass, documentation complete

---

## Key Artifacts

- **[research.md](./research.md):** Technology decisions and best practices
- **[data-model.md](./design/data-model.md):** Database schema, entities, migrations
- **[api-spec.yaml](./contracts/api-spec.yaml):** OpenAPI 3.0 specification
- **[quickstart.md](./design/quickstart.md):** Setup guide for developers

---

## Success Criteria

1. ✅ One-command setup: `docker-compose up` → running app in 3 minutes
2. ✅ Zero manual config (only `.env` copy needed)
3. ✅ Health check passes: `/actuator/health` returns 200 OK
4. ✅ Can register user, login, access protected endpoint
5. ✅ Tenant isolation prevents data leaks
6. ✅ Backend + frontend build successfully
7. ✅ Documentation complete and accurate

---

## Performance Targets

| Metric | Target | Measurement |
|--------|--------|-------------|
| Startup time | < 10s | Docker logs |
| Health check | < 100ms | curl timing |
| Registration | < 300ms | Actuator metrics |
| Login | < 200ms | Actuator metrics |
| HMR | < 2s | Vite logs |
| Memory usage | < 4GB | docker stats |

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Multi-tenant data leak | Critical | Integration tests, code review, ThreadLocal cleanup verification |
| JWT secret exposure | Critical | .gitignore, environment variables, secret management |
| CORS issues | Medium | Explicit configuration, production domain testing |

---

## Sign-Off

**Planning Complete:** ✅ Yes
**Constitution Validated:** ✅ All principles satisfied
**Ready for Implementation:** ✅ Yes

**Next Step:** Run `/speckit.tasks` to generate actionable task breakdown

---

**Version:** 1.0.0 | **Updated:** 2026-02-22 | **Estimated Time:** 5-7 days
