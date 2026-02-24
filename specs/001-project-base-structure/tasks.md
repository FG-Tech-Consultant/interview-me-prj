# Tasks: Project Base Structure

**Feature ID:** 001-project-base-structure
**Status:** Not Started
**Created:** 2026-02-22
**Last Updated:** 2026-02-22

---

## Task Status Legend

- `[ ]` Not Started
- `[P]` In Progress
- `[X]` Completed
- `[B]` Blocked (add blocker note)
- `[S]` Skipped (add reason)

## Task Priority Tags

- **[SIMPLE]** - Simplicity First (Principle 1)
- **[DOCKER]** - Containerization (Principle 2)
- **[JAVA25]** - Modern Java Standards (Principle 3)
- **[DATA]** - Data Sovereignty (Principle 4)
- **[OBSERV]** - Observability and Debugging (Principle 6)
- **[SECURITY]** - Security and Credential Management (Principle 7)
- **[MODULAR]** - Full-Stack Modularity (Principle 10)
- **[DATABASE]** - Database Schema Evolution (Principle 11)

---

## Phase 1: Multi-Module Gradle Setup (US1, US2)

**User Stories:** US1 (Developer Sets Up Local Environment), US2 (Developer Adds New Feature)

### Root Project Configuration

- [ ] T001 [MODULAR] Create root `settings.gradle.kts` declaring subprojects
  - File: `settings.gradle.kts`
  - Declare subprojects: `backend`, `frontend`, `common`
  - Set root project name: `interview-me-prj`
  - Configure plugin repositories

- [ ] T002 [MODULAR] Create root `build.gradle.kts` with common configuration
  - File: `build.gradle.kts`
  - Configure allprojects block with shared repositories
  - Define buildscript dependencies
  - Set Java toolchain to 25

- [ ] T003 [SIMPLE] Create `.gitignore` for Gradle multi-module project
  - File: `.gitignore`
  - Ignore `build/`, `.gradle/`, `node_modules/`, `dist/`
  - Ignore IDE files (`.idea/`, `*.iml`, `.vscode/`)
  - Ignore environment files (`.env`, `*.local`)

### Backend Module Setup

- [ ] T004 [MODULAR] Create `backend/build.gradle.kts` with Spring Boot 4 configuration
  - File: `backend/build.gradle.kts`
  - Apply plugins: `java`, `org.springframework.boot`, `io.spring.dependency-management`
  - Set sourceCompatibility to Java 25
  - Add dependency on `common` module: `implementation(project(":common"))`

- [ ] T005 [JAVA25] Add Spring Boot 4.x dependencies to backend module
  - File: `backend/build.gradle.kts`
  - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-actuator`
  - Add PostgreSQL driver, Liquibase 4.25.1, JJWT 0.12.5, Lombok
  - Add Hypersistence Utils for JSONB support

- [ ] T006 [MODULAR] Create backend source structure
  - Directories: `backend/src/main/java/com/interviewme/`, `backend/src/main/resources/`
  - Create `Application.java` main class with `@SpringBootApplication`
  - Create package structure: `controller/`, `service/`, `repository/`, `model/`, `dto/`, `config/`, `security/`

- [ ] T007 [SIMPLE] Create `backend/src/main/resources/application.properties`
  - Database connection: PostgreSQL 18 JDBC URL, username, password (from env vars)
  - JPA settings: `hibernate.ddl-auto=validate`, `show-sql=false`
  - Liquibase: `spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml`
  - JWT settings: secret, expiration (from env vars)
  - Server port: 8080

### Frontend Module Setup

- [ ] T008 [MODULAR] Create `frontend/build.gradle.kts` with npm/Vite integration
  - File: `frontend/build.gradle.kts`
  - Use `com.github.node-gradle.node` plugin for npm tasks
  - Configure tasks: `npmInstall`, `npmBuild`, `npmDev`
  - Output directory: `frontend/dist/` → copy to `backend/src/main/resources/static/`

- [ ] T009 [SIMPLE] Initialize Vite + React + TypeScript project in frontend module
  - File: `frontend/package.json`
  - Run: `cd frontend && npm create vite@latest . -- --template react-ts`
  - Dependencies: React 18, TypeScript 5.x, Vite
  - Dev dependencies: `@vitejs/plugin-react`, TypeScript ESLint

- [ ] T010 [MODULAR] Add TanStack Query, React Router, MUI to frontend dependencies
  - File: `frontend/package.json`
  - Install: `@tanstack/react-query`, `react-router-dom`, `@mui/material`, `@emotion/react`, `@emotion/styled`
  - Install Axios for HTTP client

- [ ] T011 [SIMPLE] Configure Vite for production build served by Spring Boot
  - File: `frontend/vite.config.ts`
  - Set `base: '/'` for root-relative paths
  - Configure build output: `outDir: 'dist'`
  - Configure proxy for dev server (optional): `proxy: { '/api': 'http://localhost:8080' }`

- [ ] T012 [MODULAR] Create frontend source structure
  - Directories: `frontend/src/pages/`, `frontend/src/components/`, `frontend/src/api/`, `frontend/src/hooks/`, `frontend/src/types/`
  - Create `main.tsx` entry point with React root
  - Create `App.tsx` with router configuration

### Common Module Setup

- [ ] T013 [MODULAR] Create `common/build.gradle.kts` with shared dependencies
  - File: `common/build.gradle.kts`
  - Apply plugin: `java-library`
  - Add dependencies: Lombok, validation-api
  - Expose shared DTOs and constants

- [ ] T014 [JAVA25] Create common source structure
  - Directories: `common/src/main/java/com/interviewme/common/dto/`, `common/src/main/java/com/interviewme/common/constants/`
  - Create placeholder README: `common/README.md` explaining shared module purpose

---

## Phase 2: Database, Liquibase & Docker (US1, US3)

**User Stories:** US1 (Developer Sets Up Local Environment), US3 (Database Schema Initialization)

### Database Configuration

- [ ] T015 [DATA] Configure Spring Data JPA for PostgreSQL 18
  - File: `backend/src/main/java/com/interviewme/config/JpaConfig.java`
  - Enable JPA repositories: `@EnableJpaRepositories`
  - Configure Hibernate properties: `hibernate.dialect=PostgreSQLDialect`
  - Enable Hibernate filters for multi-tenancy

- [ ] T016 [DATABASE] Create Liquibase master changelog
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - YAML format with includes list
  - Include initial migration file

- [ ] T017 [DATABASE] Create initial migration for tenant and user tables
  - File: `backend/src/main/resources/db/changelog/20260222140000-initial-schema.xml`
  - Create `tenant` table: id (BIGSERIAL), name (VARCHAR), created_at (TIMESTAMPTZ), settings (JSONB)
  - Create `user` table: id (BIGSERIAL), tenant_id (BIGINT FK), email (VARCHAR unique), password_hash (VARCHAR), created_at (TIMESTAMPTZ)
  - Add indexes: `idx_user_tenant_id`, `idx_user_email`
  - Include rollback instructions

### Entity and Repository Layer

- [ ] T018 [DATA] Create `Tenant` entity
  - File: `backend/src/main/java/com/interviewme/model/Tenant.java`
  - Fields: id, name, createdAt, settings (Map stored as JSONB)
  - Use `@Type(JsonBinaryType.class)` for JSONB mapping
  - Add Lombok annotations: `@Entity`, `@Table`, `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

- [ ] T019 [DATA] Create `User` entity with Hibernate tenant filter
  - File: `backend/src/main/java/com/interviewme/model/User.java`
  - Fields: id, tenantId, email, passwordHash, createdAt
  - Add filter: `@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))`
  - Add filter: `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")`
  - Implement `UserDetails` interface for Spring Security

- [ ] T020 [DATA] Create `TenantRepository`
  - File: `backend/src/main/java/com/interviewme/repository/TenantRepository.java`
  - Extend `JpaRepository<Tenant, Long>`
  - Add query method: `Optional<Tenant> findByName(String name)`

- [ ] T021 [DATA] Create `UserRepository`
  - File: `backend/src/main/java/com/interviewme/repository/UserRepository.java`
  - Extend `JpaRepository<User, Long>`
  - Add query methods: `Optional<User> findByEmail(String email)`, `List<User> findByTenantId(Long tenantId)`

### Docker Configuration

- [ ] T022 [DOCKER] Create multi-stage `Dockerfile` for unified backend+frontend image
  - File: `Dockerfile`
  - Stage 1: Node.js 20 for frontend build (`npm install && npm run build`)
  - Stage 2: Gradle 8.5 for backend build (`./gradlew backend:bootJar`)
  - Stage 3: Eclipse Temurin 25 JRE runtime
  - Copy frontend dist to backend static resources before backend build
  - Expose port 8080

- [ ] T023 [DOCKER] Create `docker-compose.yml` for local development
  - File: `docker-compose.yml`
  - Service 1: `db` using `pgvector/pgvector:pg18` image
  - Service 2: `app` using Dockerfile build
  - Environment variables from `.env` file
  - Volumes: PostgreSQL data persistence
  - Healthcheck for database and application

- [ ] T024 [SIMPLE] Create `.env.example` with all required environment variables
  - File: `.env.example`
  - Variables: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `SPRING_PROFILES_ACTIVE`
  - Document each variable with comments

---

## Phase 3: Security & JWT Authentication (US1)

**User Story:** US1 (Developer Sets Up Local Environment - Auth Flow)

### JWT Infrastructure

- [ ] T025 [SECURITY] Create `JwtService` for token generation and validation
  - File: `backend/src/main/java/com/interviewme/security/JwtService.java`
  - Methods: `generateToken(String email, Long tenantId)`, `validateToken(String token)`, `extractEmail(String token)`, `extractTenantId(String token)`
  - Use JJWT library with HS256 algorithm
  - Read JWT secret and expiration from environment variables

- [ ] T026 [SECURITY] Create `JwtAuthenticationFilter` for request interception
  - File: `backend/src/main/java/com/interviewme/security/JwtAuthenticationFilter.java`
  - Extend `OncePerRequestFilter`
  - Extract JWT from `Authorization: Bearer` header
  - Validate token and set `SecurityContextHolder` authentication
  - Extract tenant ID and set `TenantContext`

- [ ] T027 [SECURITY] Configure Spring Security with JWT filter chain
  - File: `backend/src/main/java/com/interviewme/config/SecurityConfig.java`
  - Bean: `SecurityFilterChain` with `HttpSecurity` configuration
  - Permit: `/api/auth/register`, `/api/auth/login`, `/actuator/health`
  - Require authentication for all other `/api/**` endpoints
  - Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
  - **No CORS configuration** (same-origin deployment)

- [ ] T028 [SECURITY] Create `BCryptPasswordEncoder` bean
  - File: `backend/src/main/java/com/interviewme/config/SecurityConfig.java`
  - Bean: `PasswordEncoder` using `BCryptPasswordEncoder` with strength 12

### Authentication DTOs

- [ ] T029 [JAVA25] Create `RegisterRequest` DTO (Java record)
  - File: `common/src/main/java/com/interviewme/common/dto/RegisterRequest.java`
  - Fields: `@NotBlank String email`, `@Size(min=8) String password`, `@NotBlank String tenantName`
  - Validation annotations

- [ ] T030 [JAVA25] Create `LoginRequest` DTO (Java record)
  - File: `common/src/main/java/com/interviewme/common/dto/LoginRequest.java`
  - Fields: `@NotBlank String email`, `@NotBlank String password`

- [ ] T031 [JAVA25] Create `AuthResponse` DTO (Java record)
  - File: `common/src/main/java/com/interviewme/common/dto/AuthResponse.java`
  - Fields: `String token`, `String email`, `Long tenantId`

- [ ] T032 [JAVA25] Create `UserInfoResponse` DTO (Java record)
  - File: `common/src/main/java/com/interviewme/common/dto/UserInfoResponse.java`
  - Fields: `Long id`, `String email`, `Long tenantId`, `String createdAt`

### Authentication Service & Controller

- [ ] T033 [SIMPLE] Create `AuthService` for registration and login logic
  - File: `backend/src/main/java/com/interviewme/service/AuthService.java`
  - Method: `register(RegisterRequest request)` → create Tenant + User, return JWT
  - Method: `login(LoginRequest request)` → validate credentials, return JWT
  - Use `BCryptPasswordEncoder` for password hashing
  - Add `@Slf4j` for structured logging

- [ ] T034 [MODULAR] Create `AuthController` REST endpoints
  - File: `backend/src/main/java/com/interviewme/controller/AuthController.java`
  - POST `/api/auth/register` → `@Valid RegisterRequest` → 201 Created with `AuthResponse`
  - POST `/api/auth/login` → `@Valid LoginRequest` → 200 OK with `AuthResponse`
  - GET `/api/auth/me` → requires authentication → 200 OK with `UserInfoResponse`
  - Add `@RestController`, `@RequestMapping("/api/auth")`, `@RequiredArgsConstructor`

---

## Phase 4: Multi-Tenancy Infrastructure (US1)

**User Story:** US1 (Developer Sets Up Local Environment - Tenant Isolation)

### Tenant Context Management

- [ ] T035 [DATA] Create `TenantContext` for ThreadLocal tenant storage
  - File: `backend/src/main/java/com/interviewme/security/TenantContext.java`
  - ThreadLocal variable: `currentTenantId`
  - Static methods: `setTenantId(Long tenantId)`, `getTenantId()`, `clear()`
  - Call `clear()` in filter's `finally` block to prevent leaks

- [ ] T036 [DATA] Create AOP aspect for automatic Hibernate filter activation
  - File: `backend/src/main/java/com/interviewme/config/TenantFilterAspect.java`
  - Aspect: `@Around("execution(* com.interviewme.repository..*(..))")`
  - Get `EntityManager` from `EntityManagerFactory`
  - Enable filter: `session.enableFilter("tenantFilter").setParameter("tenantId", TenantContext.getTenantId())`
  - Proceed with method execution

- [ ] T037 [DATA] Update `JwtAuthenticationFilter` to set tenant context
  - File: `backend/src/main/java/com/interviewme/security/JwtAuthenticationFilter.java`
  - After token validation, extract tenant ID from JWT claims
  - Call `TenantContext.setTenantId(tenantId)` before setting authentication
  - Ensure `TenantContext.clear()` in `finally` block

### Tenant Isolation Testing

- [ ] T038 [DATA] Create integration test for tenant isolation
  - File: `backend/src/test/java/com/interviewme/TenantIsolationTest.java`
  - Register two users in different tenants
  - Login as user 1, verify cannot access user 2's data
  - Login as user 2, verify cannot access user 1's data
  - Use `@SpringBootTest` and `@Transactional`

---

## Phase 5: React Frontend Integration (US1)

**User Story:** US1 (Developer Sets Up Local Environment - Full Auth Flow)

### API Client Setup

- [ ] T039 [MODULAR] Create Axios instance with JWT interceptor
  - File: `frontend/src/api/client.ts`
  - Create Axios instance with `baseURL: '/api'`
  - Request interceptor: Add `Authorization: Bearer ${token}` from localStorage
  - Response interceptor: Handle 401 errors (clear token, redirect to login)

- [ ] T040 [SIMPLE] Create authentication API methods
  - File: `frontend/src/api/auth.ts`
  - Function: `register(email, password, tenantName)` → POST `/api/auth/register`
  - Function: `login(email, password)` → POST `/api/auth/login`
  - Function: `getCurrentUser()` → GET `/api/auth/me`
  - Store JWT in localStorage: `localStorage.setItem('token', response.data.token)`

### React Pages & Components

- [ ] T041 [MODULAR] Create `LoginPage` component
  - File: `frontend/src/pages/LoginPage.tsx`
  - MUI form: email (TextField), password (TextField type="password"), submit button
  - Use TanStack Query mutation: `useMutation({ mutationFn: login })`
  - On success: Save token to localStorage, redirect to `/dashboard`
  - On error: Display error message using MUI Alert

- [ ] T042 [MODULAR] Create `RegisterPage` component
  - File: `frontend/src/pages/RegisterPage.tsx`
  - MUI form: email, password, tenantName, submit button
  - Use TanStack Query mutation: `useMutation({ mutationFn: register })`
  - On success: Save token to localStorage, redirect to `/dashboard`
  - On error: Display error message

- [ ] T043 [MODULAR] Create `DashboardPage` placeholder component
  - File: `frontend/src/pages/DashboardPage.tsx`
  - Use TanStack Query: `useQuery({ queryKey: ['currentUser'], queryFn: getCurrentUser })`
  - Display: "Welcome, {user.email}" with tenant info
  - Logout button: Clear localStorage, redirect to `/login`

- [ ] T044 [MODULAR] Create `ProtectedRoute` wrapper component
  - File: `frontend/src/components/ProtectedRoute.tsx`
  - Check if token exists in localStorage
  - If not authenticated: Redirect to `/login` using `<Navigate to="/login" />`
  - If authenticated: Render children

### Router Configuration

- [ ] T045 [SIMPLE] Configure React Router with auth routes
  - File: `frontend/src/App.tsx`
  - Routes: `/login` → `<LoginPage />`, `/register` → `<RegisterPage />`, `/dashboard` → `<ProtectedRoute><DashboardPage /></ProtectedRoute>`
  - Default route: Redirect `/` to `/login` or `/dashboard` based on auth state
  - Use `BrowserRouter` from `react-router-dom`

- [ ] T046 [SIMPLE] Create TanStack Query provider wrapper
  - File: `frontend/src/main.tsx`
  - Create `QueryClient` instance
  - Wrap `<App />` with `<QueryClientProvider client={queryClient}>`

### Static Resource Serving

- [ ] T047 [DOCKER] Configure Spring Boot to serve React static files
  - File: `backend/src/main/java/com/interviewme/config/WebConfig.java`
  - Bean: `WebMvcConfigurer` with `addResourceHandlers` override
  - Map `/**` to `classpath:/static/` (frontend build output)
  - Forward unmatched routes to `index.html` for SPA routing

- [ ] T048 [MODULAR] Update Gradle backend build to include frontend dist
  - File: `backend/build.gradle.kts`
  - Add task dependency: `bootJar.dependsOn(":frontend:npmBuild")`
  - Add task: Copy `frontend/dist/*` to `backend/src/main/resources/static/` before bootJar

---

## Phase 6: Observability & Health Checks (US1)

**User Story:** US1 (Developer Sets Up Local Environment - Monitoring)

### Spring Boot Actuator

- [ ] T049 [OBSERV] Configure Spring Boot Actuator endpoints
  - File: `backend/src/main/resources/application.properties`
  - Enable endpoints: `management.endpoints.web.exposure.include=health,info,metrics`
  - Health endpoint: `management.endpoint.health.show-details=when-authorized`
  - Add database health indicator

- [ ] T050 [OBSERV] Add structured logging configuration
  - File: `backend/src/main/resources/logback-spring.xml`
  - Development profile: Human-readable console logs
  - Production profile: JSON format logs with MDC context
  - Include: timestamp, level, logger, message, tenant_id (from MDC)

- [ ] T051 [OBSERV] Add Lombok `@Slf4j` to all service classes
  - Files: All `*Service.java`, `*Controller.java`
  - Replace manual logger creation with `@Slf4j` annotation
  - Use structured logging: `log.info("User registered: email={}, tenantId={}", email, tenantId)`

---

## Phase 7: Documentation (US1, US2)

**User Stories:** US1 (Developer Sets Up Local Environment), US2 (Developer Adds New Feature)

### API Documentation

- [ ] T052 [SIMPLE] Verify OpenAPI specification accuracy
  - File: `specs/001-project-base-structure/contracts/api-spec.yaml`
  - Test all endpoints with actual implementation
  - Update response schemas if needed
  - Ensure examples match actual responses

### Developer Guides

- [ ] T053 [SIMPLE] Verify quickstart guide accuracy
  - File: `specs/001-project-base-structure/design/quickstart.md`
  - Test setup instructions on fresh machine
  - Update commands if Gradle/Docker syntax changed
  - Add troubleshooting section for common errors

- [ ] T054 [MODULAR] Update project overview with multi-module structure
  - File: `.specify/memory/project-overview.md`
  - Add section: "Multi-Module Gradle Structure"
  - Document module responsibilities: backend (Spring Boot), frontend (React), common (DTOs)
  - Update architecture diagram

- [ ] T055 [SIMPLE] Create README.md for each module
  - Files: `backend/README.md`, `frontend/README.md`, `common/README.md`
  - Describe module purpose, dependencies, build commands
  - Link to main project README

---

## Phase 8: Testing & Validation (US1, US2, US3)

**User Stories:** All user stories validation

### Unit Tests

- [ ] T056 [JAVA25] Write unit tests for `JwtService`
  - File: `backend/src/test/java/com/interviewme/security/JwtServiceTest.java`
  - Test token generation, validation, claim extraction
  - Test expired token rejection
  - Test invalid token rejection

- [ ] T057 [JAVA25] Write unit tests for `AuthService`
  - File: `backend/src/test/java/com/interviewme/service/AuthServiceTest.java`
  - Test registration with new tenant creation
  - Test login with valid credentials
  - Test login with invalid credentials (should throw)
  - Mock repositories and password encoder

### Integration Tests

- [ ] T058 [SIMPLE] Write integration tests for auth endpoints
  - File: `backend/src/test/java/com/interviewme/controller/AuthControllerTest.java`
  - Test POST `/api/auth/register` → 201 Created
  - Test POST `/api/auth/login` → 200 OK with token
  - Test GET `/api/auth/me` → 401 Unauthorized without token
  - Test GET `/api/auth/me` → 200 OK with valid token
  - Use `@SpringBootTest` and `MockMvc`

- [ ] T059 [DATA] Write integration tests for database migrations
  - File: `backend/src/test/java/com/interviewme/LiquibaseIntegrationTest.java`
  - Test: Application starts successfully with Liquibase migrations
  - Test: `tenant` and `user` tables exist with correct schema
  - Test: Foreign key constraint from `user.tenant_id` to `tenant.id`
  - Use `@DataJpaTest` or `@SpringBootTest`

### End-to-End Tests

- [ ] T060 [SIMPLE] Manual E2E testing workflow
  - Test: `docker-compose up` → application starts in 3 minutes
  - Test: Access `http://localhost:8080` → React app loads
  - Test: Register new user → JWT received, redirected to dashboard
  - Test: Logout → token cleared, redirected to login
  - Test: Login with registered user → JWT received, dashboard shows user info
  - Test: Access `/actuator/health` → 200 OK with database status

### Performance Testing

- [ ] T061 [OBSERV] Measure performance targets from plan
  - Test: Application startup time < 10s (measure from `docker logs`)
  - Test: Health check response time < 100ms (use `curl -w "@-"`)
  - Test: Registration endpoint < 300ms (use Spring Boot Actuator metrics)
  - Test: Login endpoint < 200ms (use Spring Boot Actuator metrics)
  - Document results in test report

---

## Phase 9: Deployment Readiness (US1)

**User Story:** US1 (Developer Sets Up Local Environment - Production Verification)

### Build Verification

- [ ] T062 [SIMPLE] Test full Gradle build
  - Command: `./gradlew clean build`
  - Verify: All modules compile successfully
  - Verify: Frontend dist copied to backend static resources
  - Verify: `backend/build/libs/backend.jar` created
  - Verify: All tests pass

- [ ] T063 [DOCKER] Test Docker multi-stage build
  - Command: `docker build -t interview-me:latest .`
  - Verify: Frontend build stage completes
  - Verify: Backend build stage completes
  - Verify: Final image size < 500MB
  - Verify: Image contains JRE 25, backend JAR, static frontend assets

- [ ] T064 [DOCKER] Test Docker Compose orchestration
  - Command: `docker-compose up -d`
  - Verify: Both containers start successfully
  - Verify: Database migrations run automatically
  - Verify: Application accessible at `http://localhost:8080`
  - Verify: No CORS errors in browser console

### Security Audit

- [ ] T065 [SECURITY] Verify no secrets in version control
  - Check: `.env` file in `.gitignore`
  - Check: No hardcoded JWT secrets in `application.properties`
  - Check: No passwords in logs (use test logs)
  - Check: BCrypt hashes in database (not plaintext passwords)

- [ ] T066 [SECURITY] Verify environment variable configuration
  - Check: All secrets loaded from environment variables
  - Check: `.env.example` has all required variables documented
  - Check: Application fails gracefully if required env vars missing

### Final Validation

- [ ] T067 [SIMPLE] Verify all success criteria from spec
  - Success Criteria 1: One-command setup with `docker-compose up` ✓
  - Success Criteria 2: Zero configuration (only `.env` copy needed) ✓
  - Success Criteria 3: Clear module architecture (verified with new developer) ✓
  - Success Criteria 4: Automated database setup via Liquibase ✓
  - Success Criteria 5: Unified build success with `./gradlew build` ✓
  - Success Criteria 6: Health check returns 200 OK ✓
  - Success Criteria 7: Same-origin deployment (no CORS config) ✓

- [ ] T068 [SIMPLE] Create deployment checklist
  - File: `specs/001-project-base-structure/DEPLOYMENT.md`
  - Document: Pre-deployment steps (environment variables, secrets)
  - Document: Deployment steps (Docker build, Docker Compose up)
  - Document: Post-deployment verification (health check, smoke tests)
  - Document: Rollback procedure

---

## Blocked Tasks

_No blocked tasks at this time_

---

## Skipped Tasks

_No skipped tasks at this time_

---

## Notes

### Key Design Decisions

1. **Multi-Module Gradle Structure**: Enables clear separation between backend, frontend, and shared code while maintaining single build command
2. **Monolithic Deployment**: Frontend served by Spring Boot eliminates CORS complexity and simplifies deployment
3. **Timestamp-Based Migrations**: Prevents merge conflicts in team environments vs sequential numbering
4. **JWT in ThreadLocal**: TenantContext uses ThreadLocal to propagate tenant ID from JWT to Hibernate filters
5. **Spring Boot 4.x**: Latest framework version chosen for long-term support and modern Java 25 compatibility

### Multi-Module Build Flow

```
./gradlew build
  ↓
1. common:build (shared DTOs)
  ↓
2. frontend:npmBuild (React → dist/)
  ↓
3. backend:processResources (copy frontend/dist → backend/src/main/resources/static/)
  ↓
4. backend:bootJar (Spring Boot JAR with embedded frontend)
```

### Docker Build Flow

```
Dockerfile
  ↓
Stage 1: Node.js → npm install && npm run build → frontend/dist/
  ↓
Stage 2: Gradle → ./gradlew backend:bootJar → backend.jar (includes static files)
  ↓
Stage 3: JRE 25 → COPY backend.jar → CMD java -jar backend.jar
```

### Tenant Isolation Flow

```
HTTP Request → JwtAuthenticationFilter
  ↓
Extract tenant ID from JWT → TenantContext.setTenantId()
  ↓
Repository method call → TenantFilterAspect @Around
  ↓
Enable Hibernate filter: session.enableFilter("tenantFilter").setParameter("tenantId", ...)
  ↓
SQL query: SELECT * FROM user WHERE tenant_id = :tenantId
```

---

## Progress Summary

**Total Tasks:** 68
**Completed:** 0
**In Progress:** 0
**Blocked:** 0
**Skipped:** 0
**Completion:** 0%

---

**Last Updated:** 2026-02-22
**Next Review:** After Phase 1 completion (multi-module Gradle setup)
