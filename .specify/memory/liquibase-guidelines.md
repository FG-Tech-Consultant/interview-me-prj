# Liquibase Database Evolution Guidelines

**Last Updated:** 2026-02-19
**Version:** 1.0.1

**Changelog:**
- v1.0.1: Clarified PostgreSQL as only database (removed confusing H2 reference for development)
- v1.0.0: Initial version adapted from Travian Bot

---

## Overview

This project uses **Liquibase** for all database schema management. Hibernate is configured with `ddl-auto=none` to delegate all schema control to Liquibase. **Liquibase is the sole authority** for creating and modifying database structures.

---

## Core Principles

### 1. PostgreSQL as Primary Database

**IMPORTANT:** This project uses PostgreSQL 14+ as the primary and only database for both production AND development. PostgreSQL-specific features (JSONB, pgvector) are core to the application and cannot be replaced with simpler databases.

**Note on Testing:** H2 in-memory database may be used ONLY for isolated unit tests that do not require PostgreSQL-specific features. For integration tests, use Testcontainers with PostgreSQL to ensure schema compatibility.

**PostgreSQL Features Used:**
- **JSONB**: Semi-structured data (metrics, settings, LLM prompts)
- **pgvector Extension**: Vector embeddings for RAG retrieval
- **BIGSERIAL**: Auto-increment primary keys
- **TIMESTAMPTZ**: Timezone-aware timestamps
- **Foreign Keys**: Referential integrity
- **Composite Unique Constraints**: Multi-column uniqueness

**Example - Table with JSONB and Foreign Key:**
```xml
<changeSet id="20260219100000-create-profiles-table" author="liquibase">
    <createTable tableName="profiles">
        <column name="id" type="BIGSERIAL">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="tenant_id" type="BIGINT">
            <constraints nullable="false"
                         foreignKeyName="fk_profiles_tenant_id"
                         referencedTableName="tenants"
                         referencedColumnNames="id"/>
        </column>
        <column name="user_id" type="BIGINT">
            <constraints nullable="false"
                         foreignKeyName="fk_profiles_user_id"
                         referencedTableName="users"
                         referencedColumnNames="id"/>
        </column>
        <column name="name" type="VARCHAR(255)">
            <constraints nullable="false"/>
        </column>
        <column name="headline" type="VARCHAR(500)"/>
        <column name="summary" type="TEXT"/>
        <column name="preferences" type="JSONB"/>
        <column name="created_at" type="TIMESTAMPTZ" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="updated_at" type="TIMESTAMPTZ" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <createIndex indexName="idx_profiles_tenant_id" tableName="profiles">
        <column name="tenant_id"/>
    </createIndex>

    <createIndex indexName="idx_profiles_user_id" tableName="profiles">
        <column name="user_id"/>
    </createIndex>
</changeSet>
```

**Important Notes:**
- Always use `BIGSERIAL` for primary keys (PostgreSQL auto-increment)
- Use `TIMESTAMPTZ` for timestamps (timezone-aware)
- Use `JSONB` for semi-structured data (better indexing and querying than JSON)
- Define foreign keys and unique constraints inline or via `<addForeignKeyConstraint>`
- Always index `tenant_id` on multi-tenant tables

### 2. Never Edit Existing Changelog Files

**CRITICAL RULE:** Once a changelog file has been committed to version control and potentially applied to any database (local, staging, or production), **NEVER edit it**.

**Why?**
- Liquibase tracks applied changesets by their checksum
- Modifying an applied changeset breaks Liquibase's tracking mechanism
- This can cause deployment failures or data inconsistencies

**What to do instead:**
- Always create a NEW changelog file with a new timestamp
- Add the new file to `db.changelog-master.yaml`

### 3. Timestamp-Based Naming Convention

All changelog files follow a strict timestamp pattern:

```
20260219143000-create-tenants-table.xml
20260219143530-create-users-table.xml
20260219144000-create-profiles-table.xml
...
20260220091500-add-profile-visibility-column.xml
```

**Format:** `yyyyMMddHHmmss-description.xml`

**Rules:**
- Use UTC timestamp or consistent local timezone
- Generate timestamp when creating the file (e.g., `date +"%Y%m%d%H%M%S"` on Linux/Mac, `Get-Date -Format "yyyyMMddHHmmss"` on PowerShell)
- Use descriptive names after the timestamp (e.g., `create-skills-table`, `add-index-stories-visibility`)
- Ensures chronological ordering and prevents conflicts in team environments

### 4. Master Changelog Updates

After creating a new changelog file, **always** add it to `db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/20260219143000-create-tenants-table.xml
  - include:
      file: db/changelog/20260219143530-create-users-table.xml
  # ... existing files ...
  - include:
      file: db/changelog/20260220091500-add-profile-visibility-column.xml  # <-- Add new file here
```

**Important:** New includes must be added **at the end** of the list to maintain execution order.

---

## Current Schema (Baseline)

**Status:** No migrations exist yet (project in planning phase)

**Next changeset number:** `20260219HHMMSS` (use current timestamp)

**Planned Initial Schema** (to be implemented in Phase 1):
- Tenants
- Users (auth)
- Profiles
- JobExperiences
- Education
- Skills (catalog)
- UserSkills
- ExperienceProjects
- Stories
- Packages
- CoinWallets & CoinTransactions
- ChatSessions & ChatMessages
- ExportHistory
- LinkedInAnalysis

---

## Common Evolution Patterns

### Adding a New Table

Create a new file (e.g., `20260219150000-create-stories-table.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="20260219150000-create-stories-table" author="liquibase">
        <createTable tableName="stories">
            <column name="id" type="BIGSERIAL">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="BIGINT">
                <constraints nullable="false"
                             foreignKeyName="fk_stories_tenant_id"
                             referencedTableName="tenants"
                             referencedColumnNames="id"/>
            </column>
            <column name="project_id" type="BIGINT">
                <constraints nullable="false"
                             foreignKeyName="fk_stories_project_id"
                             referencedTableName="experience_projects"
                             referencedColumnNames="id"/>
            </column>
            <column name="title" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="situation" type="TEXT"/>
            <column name="task" type="TEXT"/>
            <column name="action" type="TEXT"/>
            <column name="result" type="TEXT"/>
            <column name="visibility" type="VARCHAR(20)" defaultValue="PRIVATE">
                <constraints nullable="false"/>
            </column>
            <column name="metrics" type="JSONB"/>
            <column name="created_at" type="TIMESTAMPTZ" defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_stories_tenant_id" tableName="stories">
            <column name="tenant_id"/>
        </createIndex>

        <createIndex indexName="idx_stories_project_id" tableName="stories">
            <column name="project_id"/>
        </column>

        <createIndex indexName="idx_stories_visibility" tableName="stories">
            <column name="visibility"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

### Adding a Column to an Existing Table

Create a new file (e.g., `20260220100000-add-profile-languages-column.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="20260220100000-add-profile-languages-column" author="liquibase">
        <addColumn tableName="profiles">
            <column name="languages" type="JSONB"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

### Modifying a Column

Create a new file (e.g., `20260220110000-increase-headline-length.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="20260220110000-increase-profile-headline-length" author="liquibase">
        <modifyDataType tableName="profiles" columnName="headline" newDataType="VARCHAR(1000)"/>
    </changeSet>

</databaseChangeLog>
```

### Adding an Index

Create a new file (e.g., `20260220120000-add-index-stories-created-at.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="20260220120000-add-index-stories-created-at" author="liquibase">
        <createIndex indexName="idx_stories_created_at" tableName="stories">
            <column name="created_at"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

### Adding pgvector Extension

Create a new file (e.g., `20260221100000-enable-pgvector-extension.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="20260221100000-enable-pgvector-extension" author="liquibase">
        <sql dbms="postgresql">
            CREATE EXTENSION IF NOT EXISTS vector;
        </sql>
    </changeSet>

</databaseChangeLog>
```

### Adding Vector Embeddings Column

Create a new file (e.g., `20260221101000-add-story-embeddings.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="20260221101000-add-story-embeddings" author="liquibase">
        <sql dbms="postgresql">
            ALTER TABLE stories ADD COLUMN embedding vector(1536);
        </sql>

        <createIndex indexName="idx_stories_embedding" tableName="stories">
            <column name="embedding"/>
        </createIndex>
    </changeSet>

</databaseChangeLog>
```

**Note:** `vector(1536)` is for OpenAI `text-embedding-ada-002` (1536 dimensions). Adjust based on your embedding model.

---

## ChangeSet ID Naming Convention

Each `<changeSet>` element requires a unique `id` attribute:

**Format:** `{timestamp}-{brief-description}`

**Examples:**
- `id="20260219150000-create-stories-table"` - in file `20260219150000-create-stories-table.xml`
- `id="20260220100000-add-profile-languages-column"` - in file `20260220100000-add-profile-languages-column.xml`
- `id="20260220110000-increase-profile-headline-length"` - in file `20260220110000-increase-headline-length.xml`

**Rules:**
- Must be globally unique across all changelog files
- Use lowercase with hyphens
- Should match the file timestamp prefix
- Be descriptive but concise

---

## Author Attribution

Use `author="liquibase"` for all changesets to maintain consistency:

```xml
<changeSet id="20260219150000-create-stories-table" author="liquibase">
```

---

## Testing Liquibase Changes Locally

### 1. Clean Database (Fresh Start)

To test Liquibase from scratch:

```bash
# Delete existing PostgreSQL database (if using Docker)
docker-compose down -v

# Start fresh database
docker-compose up -d postgres

# Start application - Liquibase will create all tables
./gradlew bootRun
```

### 2. Incremental Changes (Simulating Production)

To test a new changeset:

```bash
# 1. Ensure current schema is applied
./gradlew bootRun

# 2. Stop application

# 3. Add new changeset file (e.g., 20260220HHMMSS-xxx.xml)

# 4. Update db.changelog-master.yaml

# 5. Restart application - only new changeset runs
./gradlew bootRun
```

### 3. PostgreSQL Type Compatibility

PostgreSQL supports full DDL capabilities:
- `BIGSERIAL` = Auto-increment BIGINT
- `TIMESTAMPTZ` = Timezone-aware timestamp
- `JSONB` = Binary JSON with indexing support
- `vector(N)` = pgvector extension for embeddings

**Trust the migration:** If the application starts successfully and Liquibase reports success, the schema is correct.

---

## Rollback Strategy

### Liquibase Rollback Commands

**WARNING:** Rollbacks are **not recommended** in production. Instead, create forward-fixing changesets.

For local development rollback:

```bash
# Rollback last changeset
./gradlew liquibaseRollbackCount -PliquibaseCommandValue=1

# Rollback to specific tag
./gradlew liquibaseRollback -PliquibaseCommandValue=v1.0.0
```

### Forward-Fixing (Recommended)

Instead of rollback, create a new changeset that reverses the change:

**Example:** If `20260219150000-create-stories-table.xml` was wrong, create `20260220150000-drop-stories-table.xml`:

```xml
<changeSet id="20260220150000-drop-stories-table" author="liquibase">
    <dropTable tableName="stories"/>
</changeSet>
```

---

## Continuous Integration / Production Deployment

### Pre-Deployment Validation

Before deploying to production:

1. **Run Liquibase validation:**
   ```bash
   ./gradlew liquibaseValidate
   ```

2. **Generate SQL preview (dry-run):**
   ```bash
   ./gradlew liquibaseUpdateSQL
   ```

3. **Review generated SQL** in the build output

### Docker Deployment

The application's Docker container automatically runs Liquibase on startup:

```dockerfile
# Liquibase runs when Spring Boot starts
CMD ["java", "-jar", "app.jar"]
```

**Important:** Ensure PostgreSQL is accessible via environment variables:

```yaml
# docker-compose.yml
services:
  backend:
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/career_copilot
      DATABASE_USERNAME: career_user
      DATABASE_PASSWORD: secure_password
```

---

## Troubleshooting

### Error: "Validation Failed: ... column not found"

**Cause:** JPA entity expects a column that doesn't exist in database.

**Solution:** Create a new Liquibase changeset to add the missing column.

### Error: "Liquibase checksum mismatch"

**Cause:** An existing changeset file was modified.

**Solution:**
1. **Revert the modification** to the original changeset
2. Create a **new changeset** with the intended change

### Error: "Table already exists"

**Cause:** Database has tables from old Hibernate `ddl-auto=update` mode.

**Solution (Local Dev):**
```bash
# Delete database and start fresh
docker-compose down -v
docker-compose up -d postgres
./gradlew bootRun
```

**Solution (Production):**
- Manually mark changesets as executed:
  ```bash
  ./gradlew liquibaseChangelogSync
  ```

---

## Liquibase Metadata

Liquibase tracks applied changesets in two tables:

1. **DATABASECHANGELOG** - History of all executed changesets
2. **DATABASECHANGELOGLOCK** - Prevents concurrent migrations

**Never manually modify these tables** unless you know exactly what you're doing.

---

## Quick Reference

| Task | Action |
|------|--------|
| Add new table | Create `yyyyMMddHHmmss-create-{table}-table.xml`, update master changelog |
| Add new column | Create `yyyyMMddHHmmss-add-{table}-{column}.xml`, update master changelog |
| Modify column | Create `yyyyMMddHHmmss-modify-{table}-{column}.xml`, update master changelog |
| Add index | Create `yyyyMMddHHmmss-add-index-{table}-{column}.xml`, update master changelog |
| Enable pgvector | Create `yyyyMMddHHmmss-enable-pgvector-extension.xml` with `CREATE EXTENSION IF NOT EXISTS vector;` |
| Add embeddings | Use `ALTER TABLE {table} ADD COLUMN embedding vector({dimensions});` in raw SQL |
| Fix mistake | Create forward-fixing changeset (never edit existing files) |
| Test locally | `docker-compose down -v && docker-compose up -d && ./gradlew bootRun` |
| Validate schema | `./gradlew liquibaseValidate` |
| Preview SQL | `./gradlew liquibaseUpdateSQL` |

---

## Related Documents

- **Project Constitution:** `.specify/memory/constitution.md` - Principle 10 (Database Schema Evolution)
- **Tech Stack:** `.specify/memory/tech-context.md` - Database technology details
- **Project Overview:** `.specify/memory/project-overview.md` - Architecture context

---

**Document Version:** 1.0.0
**AI Agents:** Consult this document before making any database schema changes.
