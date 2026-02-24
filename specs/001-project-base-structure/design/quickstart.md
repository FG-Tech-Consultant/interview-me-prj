# Quickstart Guide: Project Base Structure

**Feature ID:** 001-project-base-structure
**Version:** 1.0.0
**Last Updated:** 2026-02-22

---

## Overview

This guide walks you through setting up the Live Resume & Career Copilot project base structure from scratch. After completing this guide, you'll have:

- ✅ Spring Boot 3.4.x backend with Java 25
- ✅ React 18 + TypeScript frontend with Vite
- ✅ PostgreSQL 18 with pgvector extension
- ✅ Docker Compose orchestration
- ✅ JWT authentication
- ✅ Multi-tenant infrastructure
- ✅ Liquibase migrations

**Time Estimate:** 30-45 minutes

---

## Prerequisites

Before starting, ensure you have:

- [x] **Docker Desktop** (version 20.10+)
- [x] **Docker Compose** (version 2.0+)
- [x] **Java 25 JDK** (for local development)
- [x] **Node.js 20+** and npm (for local development)
- [x] **Git** (for version control)
- [x] **IDE** (IntelliJ IDEA, VS Code, or similar)

### Verify Prerequisites

```bash
# Check Docker
docker --version
# Output: Docker version 24.x.x or higher

# Check Docker Compose
docker-compose --version
# Output: Docker Compose version 2.x.x or higher

# Check Java
java --version
# Output: openjdk 25 or java version "25"

# Check Node.js
node --version
# Output: v20.x.x or higher

# Check npm
npm --version
# Output: 10.x.x or higher
```

---

## Step 1: Clone Repository and Navigate to Project

```bash
# Clone the repository
git clone <repository-url>
cd interview-me-prj

# Verify you're on the correct branch
git status
# Output: On branch 001-project-base-structure
```

---

## Step 2: Environment Configuration

### Create `.env` File

Create a `.env` file in the project root:

```bash
# Copy example environment file (if it exists)
cp .env.example .env

# Or create a new .env file with the following content:
cat > .env << 'EOF'
# Database Configuration
DATABASE_URL=jdbc:postgresql://postgres:5432/interviewme_db
DATABASE_USERNAME=app_user
DATABASE_PASSWORD=secure_dev_password_12345

# JWT Configuration (IMPORTANT: Change these in production!)
JWT_SECRET=your-256-bit-secret-key-here-min-32-chars-base64-encoded
JWT_EXPIRATION=86400000

# Spring Profiles
SPRING_PROFILES_ACTIVE=dev

# PostgreSQL Configuration (for Docker Compose)
POSTGRES_USER=app_user
POSTGRES_PASSWORD=secure_dev_password_12345
POSTGRES_DB=interviewme_db

# Frontend Configuration
VITE_API_URL=http://localhost:8080
EOF
```

**⚠️ Security Warning:**
- NEVER commit `.env` file to Git
- Use strong, random secrets in production
- Generate JWT secret with: `openssl rand -base64 32`

---

## Step 3: One-Command Docker Setup

### Option A: Full Docker Deployment (Recommended for Testing)

Start all services (backend + frontend + database):

```bash
docker-compose up --build
```

**What this does:**
1. Builds Spring Boot backend Docker image
2. Builds React frontend Docker image
3. Pulls PostgreSQL 18 + pgvector image
4. Starts all three containers
5. Runs Liquibase migrations automatically
6. Exposes services on ports 8080 (backend), 3000 (frontend), 5432 (database)

**Expected Output:**
```
✔ Container postgres-pgvector       Started
✔ Container interview-me-backend    Started
✔ Container interview-me-frontend   Started
```

**Access the Application:**
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Health Check: http://localhost:8080/actuator/health

---

### Option B: Local Development (Hot Reload)

For active development with hot-reload:

**Terminal 1: Start Database Only**
```bash
docker-compose up postgres
```

**Terminal 2: Run Backend Locally**
```bash
cd backend
./gradlew bootRun
```

**Terminal 3: Run Frontend Locally**
```bash
cd frontend
npm install
npm run dev
```

**Access the Application:**
- Frontend: http://localhost:5173 (Vite dev server)
- Backend API: http://localhost:8080

---

## Step 4: Verify Installation

### 4.1 Check Backend Health

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### 4.2 Verify Database Schema

Connect to PostgreSQL and verify tables:

```bash
# Access PostgreSQL container
docker exec -it postgres-pgvector psql -U app_user -d interviewme_db

# List tables
\dt

# Expected output:
#                  List of relations
#  Schema |        Name         | Type  |  Owner
# --------+---------------------+-------+----------
#  public | databasechangelog   | table | app_user
#  public | databasechangeloglock | table | app_user
#  public | tenant              | table | app_user
#  public | user                | table | app_user

# Check tenant table structure
\d tenant

# Check user table structure
\d user

# Verify pgvector extension
SELECT * FROM pg_extension WHERE extname = 'vector';

# Exit psql
\q
```

### 4.3 Test Authentication Endpoints

**Register a New User:**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!",
    "tenantName": "Test Tenant"
  }'
```

**Expected Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "test@example.com",
  "tenantId": 1
}
```

**Login:**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123!"
  }'
```

**Get Current User (requires token from above):**

```bash
# Replace <TOKEN> with actual token from register/login response
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <TOKEN>"
```

**Expected Response:**
```json
{
  "id": 1,
  "email": "test@example.com",
  "tenantId": 1,
  "createdAt": "2026-02-22T10:00:00Z"
}
```

---

## Step 5: Project Structure Overview

### Backend Structure (Spring Boot)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/interviewme/
│   │   │       ├── config/             # Security, CORS, multi-tenant config
│   │   │       ├── auth/               # Authentication (controllers, services, DTOs)
│   │   │       │   ├── controller/
│   │   │       │   ├── service/
│   │   │       │   ├── repository/
│   │   │       │   ├── entity/
│   │   │       │   └── dto/
│   │   │       ├── tenancy/            # Tenant entity, filters, context
│   │   │       │   ├── entity/
│   │   │       │   ├── repository/
│   │   │       │   ├── filter/
│   │   │       │   └── context/
│   │   │       └── InterviewMeApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/
│   │           └── changelog/
│   │               ├── db.changelog-master.yaml
│   │               └── 20260222000001-initial-tenant-user-tables.xml
│   └── test/
│       └── java/
├── build.gradle.kts
├── settings.gradle.kts
├── Dockerfile
└── Dockerfile.dev
```

### Frontend Structure (React + TypeScript)

```
frontend/
├── src/
│   ├── app/
│   │   ├── App.tsx              # Main app component
│   │   └── routes.tsx           # Route definitions
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   └── DashboardPage.tsx
│   ├── components/
│   │   ├── common/              # Reusable components
│   │   └── layout/              # Layout components
│   ├── api/
│   │   └── auth.ts              # Auth API client
│   ├── hooks/
│   │   └── useAuth.ts           # Auth hooks
│   ├── state/
│   │   └── authContext.tsx      # Auth context
│   └── main.tsx
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── Dockerfile
└── Dockerfile.dev
```

---

## Step 6: Development Workflow

### Build Commands

**Backend:**
```bash
# Build JAR (skip tests)
./gradlew clean bootJar -x test

# Run application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build directory
./gradlew clean
```

**Frontend:**
```bash
# Install dependencies
npm install

# Run development server (hot reload)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run linter
npm run lint
```

### Docker Commands

```bash
# Start all services
docker-compose up

# Start in detached mode (background)
docker-compose up -d

# Rebuild images
docker-compose up --build

# Stop all services
docker-compose down

# Stop and remove volumes (⚠️ deletes database data)
docker-compose down -v

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Database Management

**Connect to PostgreSQL:**
```bash
# Via Docker
docker exec -it postgres-pgvector psql -U app_user -d interviewme_db

# Or via local psql client
psql -h localhost -p 5432 -U app_user -d interviewme_db
```

**Reset Database (⚠️ Destroys all data):**
```bash
# Stop containers
docker-compose down

# Remove volume
docker volume rm interview-me-prj_postgres_data

# Start again (Liquibase will recreate schema)
docker-compose up
```

**Create New Migration:**
```bash
# Generate timestamp
TIMESTAMP=$(date +"%Y%m%d%H%M%S")

# Create migration file
touch backend/src/main/resources/db/changelog/${TIMESTAMP}-your-description.xml

# Add to db.changelog-master.yaml:
# - include:
#     file: db/changelog/${TIMESTAMP}-your-description.xml
```

---

## Step 7: Testing the Application

### Manual Testing Checklist

- [ ] Backend health endpoint returns 200 OK
- [ ] Database connection is working
- [ ] Can register a new user
- [ ] Can login with registered user
- [ ] JWT token is returned on login
- [ ] Can access protected endpoint with valid token
- [ ] Invalid token returns 401 Unauthorized
- [ ] Frontend loads successfully
- [ ] Frontend can make API calls to backend

### Automated Tests

```bash
# Run backend tests
cd backend
./gradlew test

# Run frontend tests (when tests are added)
cd frontend
npm test
```

---

## Troubleshooting

### Problem: Port Already in Use

**Symptom:** `Error: bind: address already in use`

**Solution:**
```bash
# Find process using port 8080 (backend)
lsof -i :8080  # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process or change port in docker-compose.yml
```

### Problem: Database Connection Refused

**Symptom:** `Connection refused` or `could not connect to server`

**Solution:**
```bash
# Ensure PostgreSQL container is running
docker ps | grep postgres

# Check PostgreSQL logs
docker-compose logs postgres

# Verify environment variables
echo $DATABASE_URL
```

### Problem: Liquibase Migration Failed

**Symptom:** `Migration failed` or `Liquibase error`

**Solution:**
```bash
# Check Liquibase logs in backend container
docker-compose logs backend | grep Liquibase

# Reset database (⚠️ destroys data)
docker-compose down -v
docker-compose up
```

### Problem: Frontend Cannot Connect to Backend

**Symptom:** `Network Error` or `CORS error` in browser console

**Solution:**
1. Verify `VITE_API_URL` in `.env` is correct
2. Check CORS configuration in Spring Security
3. Ensure backend is running on port 8080

```bash
# Test backend from command line
curl http://localhost:8080/actuator/health
```

---

## Next Steps

After completing the base structure setup:

1. **Explore the Code:**
   - Review security configuration in `SecurityConfig.java`
   - Examine JWT service in `JwtService.java`
   - Study multi-tenant filter in `TenantFilterAspect.java`

2. **Add Features:**
   - Implement profile CRUD endpoints (Feature 002)
   - Add skills management (Feature 003)
   - Build story/experience tracking (Feature 004)

3. **Production Deployment:**
   - Set up CI/CD pipeline
   - Configure Kubernetes or cloud deployment
   - Set up monitoring and logging

4. **Learn More:**
   - Read [research.md](../research.md) for technology deep-dives
   - Review [data-model.md](./data-model.md) for database schema
   - Check [api-spec.yaml](../contracts/api-spec.yaml) for API documentation

---

## Quick Reference

### Useful URLs

| Service       | URL                                    |
|---------------|----------------------------------------|
| Frontend      | http://localhost:3000                  |
| Frontend (dev)| http://localhost:5173                  |
| Backend API   | http://localhost:8080                  |
| Health Check  | http://localhost:8080/actuator/health  |
| API Docs      | (Swagger UI - to be added)             |

### Key Commands

| Task                  | Command                           |
|-----------------------|-----------------------------------|
| Start all services    | `docker-compose up`               |
| Stop all services     | `docker-compose down`             |
| Rebuild images        | `docker-compose up --build`       |
| View logs             | `docker-compose logs -f`          |
| Backend local         | `./gradlew bootRun`               |
| Frontend local        | `npm run dev`                     |
| Backend build         | `./gradlew clean bootJar -x test` |
| Frontend build        | `npm run build`                   |
| Database console      | `docker exec -it postgres-pgvector psql -U app_user -d interviewme_db` |

---

**Quickstart Guide Version:** 1.0.0
**Last Updated:** 2026-02-22
**Maintained By:** Project Team
