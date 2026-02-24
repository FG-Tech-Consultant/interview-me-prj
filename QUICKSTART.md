# Quick Start Guide

Get the Live Resume & Career Copilot application running in under 5 minutes.

## Prerequisites

- **Docker** and **Docker Compose** installed
- **Git** (to clone the repository)

That's it! Docker handles everything else.

## Step 1: Clone and Navigate

```bash
git clone <repository-url>
cd interview-me-prj
```

## Step 2: Configure Environment

```bash
# Copy the example environment file
cp .env.example .env

# Edit .env and set secure values
# IMPORTANT: Change JWT_SECRET and DATABASE_PASSWORD!
```

### Generate Secure JWT Secret

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

Paste the output into `.env` as `JWT_SECRET`.

## Step 3: Start the Application

```bash
docker-compose up --build
```

This command:
1. Builds the frontend (React + TypeScript)
2. Builds the backend (Spring Boot + Java 21)
3. Creates a PostgreSQL 18 database with pgvector
4. Runs database migrations automatically
5. Starts the application

**First build takes 3-5 minutes.** Subsequent starts are much faster.

## Step 4: Access the Application

Once you see:
```
interview-me-app  | Started Application in X.XXX seconds
```

Open your browser:

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **API Docs**: See `specs/001-project-base-structure/contracts/api-spec.yaml`

## Step 5: Create Your First Account

1. Go to http://localhost:8080
2. Click "Register"
3. Fill in:
   - Email: your@email.com
   - Password: (minimum 8 characters)
   - Organization Name: Your Company
4. Click "Register"

You'll be automatically logged in and redirected to the dashboard.

## Troubleshooting

### Port Already in Use

If port 8080 or 5432 is already in use:

```bash
# Stop the conflicting service, or edit docker-compose.yml:
# Change "8080:8080" to "9090:8080" for the app
# Change "5432:5432" to "5433:5432" for the database
```

### Database Connection Failed

Wait a few seconds for PostgreSQL to fully start. The app retries automatically.

### Build Failed

```bash
# Clean and rebuild
docker-compose down -v
docker-compose up --build
```

### Cannot Access Application

1. Check if containers are running:
   ```bash
   docker-compose ps
   ```

2. Check logs:
   ```bash
   docker-compose logs app
   docker-compose logs db
   ```

3. Verify health:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Stopping the Application

```bash
# Stop containers (preserves data)
docker-compose down

# Stop and remove volumes (fresh start)
docker-compose down -v
```

## Local Development (Without Docker)

### Backend

```bash
# Start database only
docker-compose up -d db

# Run backend
./gradlew backend:bootRun
```

Backend runs on http://localhost:8080

### Frontend

```bash
# Install dependencies
cd frontend
npm install

# Run dev server (proxies to backend)
npm run dev
```

Frontend runs on http://localhost:5173

## Next Steps

- Read `README.md` for full documentation
- Explore `specs/001-project-base-structure/` for feature specs
- Check `.specify/memory/constitution.md` for project principles
- Review API specification in `specs/001-project-base-structure/contracts/api-spec.yaml`

## Support

For issues, check:
1. Docker logs: `docker-compose logs`
2. Application health: `curl http://localhost:8080/actuator/health`
3. Database connectivity: `docker-compose exec db psql -U postgres`
