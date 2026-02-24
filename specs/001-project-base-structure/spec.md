# Feature Specification: Project Base Structure

**Feature ID:** 001-project-base-structure
**Status:** Draft
**Created:** 2026-02-20
**Last Updated:** 2026-02-22
**Version:** 2.0.0

---

## Overview

### Problem Statement

The Live Resume & Career Copilot platform currently lacks the foundational project structure needed to support development. Without a well-organized codebase, consistent build system, containerization setup, and initial architecture, development cannot proceed efficiently. Developers need a clear, standardized starting point that adheres to modern best practices (including monolithic deployment with multi-module Gradle) and enables rapid feature development.

### Feature Summary

Create the complete foundational structure for the Live Resume & Career Copilot platform using a **monolithic deployment architecture** where the React frontend is served as static assets by the Spring Boot backend. The project will use a **multi-module Gradle structure** (backend, frontend, common modules) with Spring Boot 4, Java 25, PostgreSQL 18, and single-container Docker deployment. This establishes the architectural skeleton that all future features will build upon.

### Target Users

- **Developers:** Need a well-organized, modern codebase with clear module boundaries to implement features
- **DevOps Engineers:** Need simplified single-container deployment and environment management
- **Project Owner:** Needs confidence that the foundation follows best practices and constitutional principles (monolithic deployment, multi-module Gradle)

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Monolithic deployment with React SPA served by Spring Boot, single Docker image, no CORS configuration needed
- **Principle 2: Containerization as First-Class Citizen** - Single Docker container with multi-stage build (frontend build → backend build → single JAR with embedded static assets)
- **Principle 3: Modern Java Standards** - Java 25 with Gradle 8.5+, Spring Boot 4.x, records, virtual threads support
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - PostgreSQL 18 with pgvector extension, Liquibase migrations, tenant-aware schema design
- **Principle 6: Observability and Debugging** - Structured logging with Logback, Spring Boot Actuator, JSON log format
- **Principle 7: Security and Credential Management** - Environment variables for secrets, BCrypt for passwords, JWT authentication setup, no CORS needed (same-origin)
- **Principle 8: Multi-Tenant Architecture** - Tenant entity and tenant filtering infrastructure from day one
- **Principle 10: Full-Stack Modularity and Separation of Concerns** - **REQUIRED multi-module Gradle structure** (backend, frontend, common), domain-based package structure for backend, feature-based component structure for frontend
- **Principle 11: Database Schema Evolution** - Liquibase with timestamp-based migrations, master changelog setup

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: Developer Sets Up Local Environment

**Actor:** New developer joining the project
**Goal:** Get the application running locally within 15 minutes
**Preconditions:** Docker and Git installed on developer machine

**Steps:**
1. Developer clones the repository
2. Developer runs `docker-compose up` command
3. Docker Compose starts single application container (containing both frontend and backend) and PostgreSQL container
4. Developer accesses application at `http://localhost:8080` (backend serves both API and frontend)
5. Backend health check endpoint responds successfully at `http://localhost:8080/actuator/health`

**Success Criteria:**
- Two containers (application, database) start successfully
- Application is accessible and responsive within 2 minutes
- No manual configuration files needed beyond `.env.example` template
- Frontend served from same origin as backend (no CORS issues)

#### Scenario 2: Developer Adds New Feature

**Actor:** Developer implementing a new feature
**Goal:** Understand where to place new code in multi-module Gradle structure
**Preconditions:** Project structure is documented and organized

**Steps:**
1. Developer reads project structure documentation
2. Developer identifies appropriate Gradle module (backend, frontend, or common) for new feature
3. Developer navigates to correct package/directory within the module
4. Developer creates service, repository, and controller following existing patterns
5. Developer runs unified build command to verify compilation
6. Developer commits code following the established structure

**Success Criteria:**
- Developer finds correct Gradle module and package within 5 minutes
- New code follows consistent patterns with existing codebase
- Single build command (`./gradlew build`) compiles both frontend and backend
- Build succeeds without modification to build scripts

#### Scenario 3: Database Schema Initialization

**Actor:** System during first-time startup
**Goal:** Create initial database schema automatically
**Preconditions:** PostgreSQL container is running

**Steps:**
1. Application starts for the first time
2. Liquibase detects empty database
3. Liquibase executes initial migration creating tenant and user tables
4. Application confirms successful schema creation in logs
5. Application becomes ready to accept requests

**Success Criteria:**
- Schema is created automatically without manual SQL execution
- All tables, indexes, and constraints are present
- Liquibase changelog tracking table is initialized

### Edge Cases

- **Docker not installed:** Provide clear error message and installation instructions
- **Port conflicts:** Document required port (8080 for combined frontend+backend, 5432 for database) and how to customize
- **Database initialization failure:** Automatic rollback and clear error logging
- **Environment variables missing:** Validation on startup with descriptive error messages
- **Frontend build failure:** Multi-stage Docker build should fail fast with clear error

---

## Functional Requirements

### Core Capabilities

**REQ-001:** Multi-Module Gradle Structure
- **Description:** Root Gradle project with three modules: backend (Spring Boot), frontend (React/Vite), and common (shared code)
- **Acceptance Criteria:**
  - Root `settings.gradle.kts` declares subprojects: `backend`, `frontend`, `common`
  - Root `build.gradle.kts` coordinates module builds
  - `backend` module depends on `common` module
  - `frontend` module is independent (JavaScript/TypeScript)
  - Single build command (`./gradlew build`) compiles all modules
  - Gradle wrapper included (`./gradlew` and `./gradlew.bat`)

**REQ-002:** Backend Module Structure
- **Description:** Spring Boot 4.x backend organized with domain-based packages, layered architecture, and modern Java 25 features
- **Acceptance Criteria:**
  - Located in `backend/` directory
  - Gradle 8.5+ build configuration with Spring Boot 4.x dependencies
  - Package structure follows constitution Principle 10 (auth, tenancy, profile, skills, etc.)
  - Base entities for Tenant and User with multi-tenant relationships
  - Application properties externalized via environment variables
  - JAR artifact builds successfully via `./gradlew backend:bootJar`
  - Static resources directory configured to serve frontend build output

**REQ-003:** Frontend Module Structure
- **Description:** React 18+ frontend with TypeScript, organized by feature, using Vite build tool
- **Acceptance Criteria:**
  - Located in `frontend/` directory
  - Vite setup with TypeScript configuration
  - Directory structure: `components/`, `pages/`, `api/`, `hooks/`, `state/`
  - React Query configured for server state management
  - MUI or Chakra UI component library integrated
  - React Router configured with placeholder routes
  - Production build generates optimized static assets
  - Build task in `frontend/build.gradle.kts` executes `npm run build`
  - Build output copied to `backend/src/main/resources/static`

**REQ-004:** Common Module Structure
- **Description:** Shared code module for DTOs, constants, and utilities used by both frontend and backend
- **Acceptance Criteria:**
  - Located in `common/` directory
  - Contains shared DTOs (Java records)
  - Contains API constants (endpoint paths, error codes)
  - Contains validation rules shared between frontend and backend
  - Minimal dependencies (no Spring Boot or React dependencies)

**REQ-005:** Database Infrastructure
- **Description:** PostgreSQL 18 with pgvector extension, managed via Liquibase migrations
- **Acceptance Criteria:**
  - Docker Compose includes PostgreSQL 18 service with pgvector extension
  - Liquibase master changelog at `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Initial migration creates `tenant` and `user` tables with proper relationships
  - Initial migration creates `databasechangelog` and `databasechangeloglock` tables
  - Connection parameters configurable via environment variables

**REQ-006:** Unified Containerization Setup
- **Description:** Single Docker image containing both frontend static assets and backend JAR, orchestrated with PostgreSQL via Docker Compose
- **Acceptance Criteria:**
  - `docker-compose.yml` defines application service (frontend+backend) and PostgreSQL service
  - Single multi-stage Dockerfile:
    - Stage 1: Build frontend with Node.js (Vite build)
    - Stage 2: Build backend with Gradle (bootJar)
    - Stage 3: Copy frontend build output to backend static resources
    - Stage 4: Runtime stage with JRE only (single JAR with embedded frontend)
  - Volume mounts for hot-reloading during development (optional dev compose file)
  - Named volumes for PostgreSQL data persistence
  - Health checks configured for both services
  - Application accessible at single port (8080) serving both API and frontend

**REQ-007:** Unified Build System
- **Description:** Gradle multi-module build system that compiles frontend, backend, and common modules in coordinated fashion
- **Acceptance Criteria:**
  - `backend:bootJar` task depends on `frontend:build` task
  - `frontend:build` task executes npm/Vite build
  - Frontend build output automatically copied to backend static resources
  - Backend run command: `./gradlew backend:bootRun` serves both API and frontend
  - Test commands available for both backend and frontend modules
  - Clean command removes all build artifacts: `./gradlew clean`

**REQ-008:** Logging and Observability Infrastructure
- **Description:** Structured JSON logging and Spring Boot Actuator endpoints
- **Acceptance Criteria:**
  - Logback configuration with Logstash JSON encoder
  - Spring Boot Actuator enabled with health and metrics endpoints
  - Logger injection via Lombok `@Slf4j`
  - Log levels configurable via environment variables
  - Development mode uses human-readable logs, production uses JSON

**REQ-009:** Security Foundation
- **Description:** JWT authentication infrastructure and environment-based secret management (no CORS needed)
- **Acceptance Criteria:**
  - Spring Security configured for JWT authentication
  - BCryptPasswordEncoder bean configured for password hashing
  - JWT secret and expiration configurable via environment variables
  - **No CORS configuration** (frontend served from same origin as backend)
  - `.env.example` file with all required environment variables documented

**REQ-010:** Multi-Tenant Foundation
- **Description:** Tenant entity, tenant resolver, and tenant filtering infrastructure
- **Acceptance Criteria:**
  - `Tenant` entity with id, name, createdAt, settings fields
  - `User` entity with tenant_id foreign key
  - Tenant resolver extracts tenant from JWT claims
  - Tenant filter (AOP or JPA filter) applied to repository queries
  - Base abstract entity class with `tenant_id` for all tenant-owned entities

### User Interface Requirements

**REQ-UI-001:** Frontend Skeleton Pages
- **Description:** Placeholder pages for main application routes served by Spring Boot
- **Acceptance Criteria:**
  - Login page with mock form
  - Dashboard page (placeholder)
  - Profile editor page (placeholder)
  - Navigation component with links to all pages
  - Responsive layout using component library grid system
  - All pages accessible from single origin (http://localhost:8080)

**REQ-UI-002:** Development Environment Experience
- **Description:** Hot-reloading and developer-friendly tooling for multi-module development
- **Acceptance Criteria:**
  - Frontend hot-reloads on file changes (via Vite dev server proxy to backend, or rebuild on change)
  - Backend restarts on file changes (via Spring DevTools)
  - Clear error messages displayed in browser console
  - TypeScript errors displayed during development
  - Single command starts full stack: `./gradlew backend:bootRun`

### Data Requirements

**REQ-DATA-001:** Initial Schema Migration
- **Description:** First Liquibase migration creating tenant and user tables
- **Acceptance Criteria:**
  - Migration follows timestamp-based naming convention
  - Creates `tenant` table with: id (BIGSERIAL), name (VARCHAR), created_at (TIMESTAMPTZ), settings (JSONB)
  - Creates `user` table with: id (BIGSERIAL), tenant_id (BIGINT FK), email (VARCHAR unique), password_hash (VARCHAR), created_at (TIMESTAMPTZ)
  - Includes indexes on tenant_id and email
  - Includes rollback instructions

---

## Success Criteria

The feature will be considered successful when:

1. **One-Command Local Setup:** Developers can run `docker-compose up` and have a fully functional development environment within 3 minutes
   - Measurement: Timed setup from clone to running application

2. **Zero Configuration:** Application runs without manual configuration file editing (using `.env.example` as template)
   - Measurement: Fresh checkout works immediately after copying `.env.example` to `.env`

3. **Clear Module Architecture:** New developers can identify which Gradle module to modify for 5 common tasks within 10 minutes of reviewing the structure
   - Measurement: Developer survey or onboarding documentation walkthrough

4. **Automated Database Setup:** Database schema is created automatically on first run without manual SQL scripts
   - Measurement: Fresh database container initializes successfully via Liquibase

5. **Unified Build Success:** Single build command compiles all modules (frontend, backend, common) with zero errors
   - Measurement: `./gradlew build` completes with exit code 0

6. **Health Check Pass:** Backend health endpoint returns 200 OK status and database connectivity confirmed
   - Measurement: Application accessible at `http://localhost:8080`, health endpoint returns `{"status":"UP"}`

7. **Same-Origin Deployment:** Frontend and backend served from single origin (no CORS configuration present)
   - Measurement: Verify no CORS configuration in Spring Security, frontend accessible at `http://localhost:8080`

---

## Key Entities

### Tenant

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- name: Organization or user name (VARCHAR 255, NOT NULL)
- created_at: Timestamp with timezone (TIMESTAMPTZ, NOT NULL)
- settings: JSON configuration object (JSONB, nullable)

**Relationships:**
- One-to-many with User (one tenant has many users)

### User

**Attributes:**
- id: Auto-incrementing primary key (BIGSERIAL)
- tenant_id: Foreign key to Tenant (BIGINT, NOT NULL)
- email: Unique email address (VARCHAR 255, NOT NULL, UNIQUE)
- password_hash: BCrypt hashed password (VARCHAR 255, NOT NULL)
- created_at: Timestamp with timezone (TIMESTAMPTZ, NOT NULL)

**Relationships:**
- Many-to-one with Tenant (many users belong to one tenant)

---

## Dependencies

### External Dependencies

- **Docker:** Required for containerized local development
- **Java 25 JDK:** Required for backend compilation (provided in Docker image)
- **Node.js 20+:** Required for frontend build (provided in Docker multi-stage build)
- **PostgreSQL 18:** Provided via Docker Compose
- **Gradle 8.5+:** Provided via wrapper scripts
- **npm or yarn:** For frontend package management (used in Docker build)

---

## Assumptions

1. Developers have Docker and Docker Compose installed locally
2. Standard development port (8080 for combined frontend+backend, 5432 for database) is available
3. Internet connectivity is available for downloading dependencies
4. Git is installed for version control
5. Operating system supports Docker (Linux, macOS, Windows with WSL2)
6. Developers are familiar with Spring Boot, React, and Gradle multi-module projects
7. UTF-8 encoding is used for all source files
8. Project will use MIT or Apache 2.0 license (to be confirmed)
9. Spring Boot 4 is available and compatible with Java 25 (using preview/early-access if needed)

---

## Out of Scope

The following are explicitly excluded from this feature:

1. **No Business Logic:** No actual feature implementations (profile CRUD, chat, exports, etc.)
2. **No Authentication UI:** Only skeleton/placeholder login page, no working authentication flow
3. **No Database Seeding:** No sample data or seed scripts
4. **No Production Deployment:** Only local development setup, no Kubernetes/cloud deployment configs
5. **No CI/CD Pipelines:** No GitHub Actions or Jenkins configurations
6. **No Monitoring Stack:** No Prometheus, Grafana, or ELK stack integration
7. **No Email Service:** No email configuration or SMTP setup
8. **No Payment Integration:** No Stripe or billing infrastructure
9. **No LLM Integration:** No OpenAI/Gemini/Claude API clients
10. **No Frontend Styling:** Only basic component library defaults, no custom branding
11. **No Separate Frontend Server:** Frontend is NOT served via Vite dev server or Nginx in production (served by Spring Boot)
12. **No CORS Configuration:** Not needed since frontend served from same origin

---

## Security & Privacy Considerations

### Security Requirements

- All secrets (database password, JWT secret) MUST be environment variables, never committed to Git
- `.gitignore` MUST exclude `.env`, `*.log`, and build artifacts from all modules
- Default passwords in `.env.example` MUST be clearly marked as insecure examples
- JWT secret MUST be strong random string (min 256 bits) in production
- Database connection requires authentication (no anonymous access)
- No CORS configuration needed (same-origin deployment eliminates CORS attack surface)

### Privacy Requirements

- No telemetry or analytics in base structure
- No third-party JavaScript libraries with tracking (verify component library choices)
- Database credentials isolated to backend module only

---

## Performance Expectations

- **Startup Time:** Application container starts within 2 minutes on modern hardware (quad-core, 16GB RAM)
- **Build Time:** Full multi-module build completes within 90 seconds (frontend build + backend JAR)
- **Hot Reload:** Frontend changes reflected within 2 seconds, backend changes within 5 seconds
- **Memory Usage:** Development environment consumes max 3GB RAM total (single application container + PostgreSQL)

---

## Testing Scope

### Functional Testing

- **Container Orchestration:** Verify two containers (application, database) start successfully via `docker-compose up`
- **Backend Health:** Verify `/actuator/health` endpoint returns 200 OK
- **Database Connectivity:** Verify backend connects to PostgreSQL successfully
- **Liquibase Migration:** Verify initial migration creates tenant and user tables
- **Frontend Serving:** Verify React app is accessible at `http://localhost:8080` (same origin as backend)
- **Multi-Module Build:** Verify `./gradlew build` compiles all three modules successfully
- **Static Resources:** Verify frontend build output is embedded in backend JAR

### User Acceptance Testing

- **Fresh Environment Setup:** Clone repo, copy `.env.example` to `.env`, run `docker-compose up`, verify application loads at `http://localhost:8080`
- **Build from Scratch:** Run `./gradlew clean build` and verify successful multi-module build
- **Code Organization:** Navigate multi-module Gradle structure and verify intuitive organization
- **Same-Origin Verification:** Confirm frontend and API accessible from same origin, no CORS headers present

### Edge Case Testing

- **Missing Environment Variables:** Start application without `.env` file and verify clear error message
- **Port Conflicts:** Start application when port 8080 is already in use and verify meaningful error
- **Database Connection Failure:** Stop PostgreSQL container and verify backend handles gracefully
- **Frontend Build Failure:** Introduce syntax error in React code and verify Docker build fails with clear message

---

## Notes

This is the foundational feature that all subsequent features depend on. It establishes the architectural patterns, tooling, and conventions that will be followed throughout the project lifecycle.

Key design decisions:
- **Multi-module Gradle:** Clear separation of concerns while maintaining monolithic deployment
- **Frontend served by backend:** Simplifies deployment, eliminates CORS complexity
- **Single Docker image:** Simpler operations than multi-container orchestration
- **Timestamp-based migrations:** Following constitution Principle 11
- **Soft multi-tenancy:** Following constitution Principle 8
- **Stateless container:** Following constitution Principle 2
- **Spring Boot 4:** Using latest Spring framework version

---

## Revision History

| Version | Date       | Changes                                                   | Author      |
|---------|------------|-----------------------------------------------------------|-------------|
| 1.0.0   | 2026-02-20 | Initial specification                                     | Claude Code |
| 2.0.0   | 2026-02-22 | Updated for monolithic deployment, multi-module Gradle, Spring Boot 4 | Claude Code |
