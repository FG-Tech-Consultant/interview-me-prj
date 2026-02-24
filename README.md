# Live Resume & Career Copilot

A multi-tenant SaaS platform for dynamic resumes and AI-powered career guidance.

## Project Structure

This is a multi-module Gradle project with the following structure:

```
interview-me-prj/
├── backend/          # Spring Boot backend (Java 21)
├── frontend/         # React + TypeScript frontend (Vite)
├── common/           # Shared DTOs and constants
├── .specify/         # Project documentation and specifications
└── specs/            # Feature specifications
```

## Tech Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.x
- **Database**: PostgreSQL 18 with pgvector extension
- **Security**: Spring Security 6 + JWT
- **Database Migrations**: Liquibase (timestamp-based)
- **Multi-Tenancy**: Hibernate Filters + AOP

### Frontend
- **Language**: TypeScript
- **Framework**: React 18
- **Build Tool**: Vite
- **Data Fetching**: TanStack Query
- **UI Library**: Material-UI (MUI)
- **HTTP Client**: Axios

### Infrastructure
- **Build Tool**: Gradle 8.5+
- **Container**: Docker + Docker Compose
- **Runtime**: JRE 21 (Eclipse Temurin)

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- Node.js 20+ (for local frontend development)

### Using Docker (Recommended)

1. Copy environment variables:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and set secure values (especially `JWT_SECRET` and database password)

3. Start the application:
   ```bash
   docker-compose up --build
   ```

4. Access the application:
   - Frontend: http://localhost:8080
   - Health check: http://localhost:8080/actuator/health
   - API: http://localhost:8080/api

### Local Development

#### Backend Only

```bash
# Start PostgreSQL
docker-compose up -d db

# Build and run backend
./gradlew backend:bootRun
```

#### Frontend Only

```bash
# Install dependencies
cd frontend
npm install

# Run dev server (proxies API to localhost:8080)
npm run dev
```

Access frontend at http://localhost:5173

#### Full Build

```bash
# Build everything (backend + frontend)
./gradlew build

# Run backend JAR (includes bundled frontend)
java -jar backend/build/libs/backend-1.0.0.jar
```

## Available Endpoints

### Authentication
- `POST /api/auth/register` - Register new user and tenant
- `POST /api/auth/login` - Login with email and password
- `GET /api/auth/me` - Get current authenticated user (requires JWT)

### Health Check
- `GET /actuator/health` - Application health status

See `specs/001-project-base-structure/contracts/api-spec.yaml` for full API documentation.

## Environment Variables

See `.env.example` for all required environment variables:

- `DATABASE_URL` - PostgreSQL connection string
- `DATABASE_USERNAME` - Database user
- `DATABASE_PASSWORD` - Database password
- `JWT_SECRET` - JWT signing secret (minimum 32 characters)
- `JWT_EXPIRATION_MS` - JWT token expiration in milliseconds
- `SPRING_PROFILES_ACTIVE` - Spring profile (dev/prod)

## Testing

```bash
# Run all tests
./gradlew test

# Run only backend tests
./gradlew backend:test
```

## Project Documentation

- **Specifications**: `specs/` - Feature specifications with design docs
- **Research**: `.specify/research/` - Technical research and decisions
- **Memory**: `.specify/memory/` - Project overview and constitution
- **Templates**: `.specify/templates/` - Document templates

## Multi-Tenancy

This application uses discriminator-based multi-tenancy:

- Each user belongs to a tenant
- Tenant ID is stored in JWT claims
- Hibernate filters automatically filter data by tenant
- All queries are automatically scoped to the current tenant

## Security

- Passwords are hashed with BCrypt (strength 12)
- JWT tokens expire after 24 hours (configurable)
- All API endpoints (except auth) require valid JWT
- CORS disabled (same-origin deployment)
- No secrets in version control

## Database Migrations

This project uses Liquibase with timestamp-based migrations:

### Creating a New Migration

1. Generate timestamp:
   ```bash
   # Windows PowerShell
   Get-Date -Format "yyyyMMddHHmmss"

   # Linux/Mac/Git Bash
   date +"%Y%m%d%H%M%S"
   ```

2. Create migration file:
   ```
   backend/src/main/resources/db/changelog/YYYYMMDDHHMMSS-description.xml
   ```

3. Add to master changelog:
   ```yaml
   # backend/src/main/resources/db/changelog/db.changelog-master.yaml
   databaseChangeLog:
     - include:
         file: db/changelog/YYYYMMDDHHMMSS-description.xml
   ```

See `.specify/memory/constitution.md` Principle 10 for full migration guidelines.

## Building for Production

```bash
# Build Docker image
docker build -t interview-me:latest .

# Or use Docker Compose
docker-compose up --build
```

The final image:
- Uses multi-stage builds
- Includes frontend static files served by Spring Boot
- Runs as non-root user
- Size optimized with Alpine Linux

## Contributing

See `.specify/memory/constitution.md` for project principles and guidelines.

## License

Proprietary
