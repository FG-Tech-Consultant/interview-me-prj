# Project Instructions

## Build System

**This project uses GRADLE, NOT Maven!**
- Build: `./gradlew :sboot:bootJar -x test`
- Run: `./gradlew :sboot:bootRun`
- The `sboot` module is the Spring Boot bootstrap (Application, configs, resources)
- The `backend` module is a plain library (domain logic, controllers, services)
- NEVER use `mvn` or `mvnw` commands

## Speckit Workflow

This project uses the `.specify/` framework for feature development.

### Before Starting Any Task

1. **Read `.specify/memory/project-overview.md`** - Project goals, architecture, current features, and roadmap
2. **Read `.specify/memory/constitution.md`** - Project rules and constraints
3. **Check `.specify/memory/`** - Additional context (tech stack, version tracking)
4. **Check `.specify/templates/`** - Document structures

### For Simple Tasks (bugs, small changes)

1. Create feature dir: `.specify/scripts/bash/create-new-feature.sh --skip-branch "description"`
2. Write minimal spec.md, plan.md, tasks.md in `specs/[N]-[name]/`
3. Implement and report changes
4. User handles git

### For Complex Tasks

Use full workflow:
1. /speckit.specify - requirements
2. /speckit.plan - technical design
3. /speckit.tasks - task breakdown
4. /speckit.implement - execution

### If Unsure

1. Check `.specify/memory/` first
2. Check existing `specs/` for examples
3. Ask user for clarification

### Rules

- No git operations (user handles branching/commits)
- Always check constitution before implementing
- Keep documentation minimal but clear

## Autonomy Rules
- Do NOT ask for confirmation before file operations
- Do NOT ask "should I proceed?" - just proceed
- Only stop if there's an actual error
- Run all commands without asking permission

## File Editing Rules - IMPORTANT

**To avoid "file modified" errors, follow this hierarchy:**

1. **For small targeted changes (1-2 lines):**
   - Use `sed -i` commands directly via Bash tool
   - Example: `sed -i 's/oldtext/newtext/' file.java`
   - No Read required, no approval needed, fast and reliable

2. **For medium changes (3-10 lines):**
   - Read file once, then use Write tool to rewrite entire file
   - Write tool is more reliable than Edit tool
   - Include all existing content + your changes

3. **For large structural changes:**
   - Use Write tool to rewrite entire sections
   - Don't try multiple Edit operations on same file

4. **NEVER:**
   - Use Edit tool repeatedly on same file in one session
   - Wait for "confirmation" between edits (conflicts with autonomy)
   - Use Read -> Edit pattern if you just read the file
   - Let "file modified" errors block you - switch to sed/Write instead

5. **Performance rules:**
   - NEVER use `repository.findAll().stream().filter()` - always create proper repository queries
   - NEVER load large datasets into memory for filtering
   - Use database queries with WHERE clauses and indexes

## Spring/JPA Transaction Guidelines

**ALWAYS use @Transactional annotations:**

1. **Read-only methods:**
   - Add `@Transactional(readOnly = true)` to all read/query methods
   - Includes: GET endpoints, service methods that only read data
   - Example:
     ```java
     @Transactional(readOnly = true)
     public List<Village> getAllVillages(Long serverId) {
         return villageRepository.findByUserServerId(serverId);
     }
     ```

2. **Write methods:**
   - Add `@Transactional` to all methods that modify data
   - Includes: POST/PUT/DELETE endpoints, service methods with saves/updates/deletes
   - Example:
     ```java
     @Transactional
     public FarmingSettings saveSettings(Long serverId, ...) {
         FarmingSettings settings = getOrCreateSettings(server);
         settings.setAutoStartAll(autoStartAll);
         return farmingSettingsRepository.save(settings);
     }
     ```

3. **Controller methods:**
   - Add `@Transactional(readOnly = true)` to GET endpoints that read data
   - Add `@Transactional` to POST/PUT/DELETE endpoints
   - Example:
     ```java
     @GetMapping("/{serverId}")
     @Transactional(readOnly = true)
     public ResponseEntity<FarmingSettings> getSettings(@PathVariable Long serverId) {
         // ...
     }

     @PostMapping("/{serverId}")
     @Transactional
     public ResponseEntity<Map<String, Object>> saveSettings(@PathVariable Long serverId, ...) {
         // ...
     }
     ```

**Why this matters:**
- `readOnly = true` optimizes database performance and prevents accidental writes
- Ensures data consistency and proper transaction boundaries
- Prevents lazy-loading exceptions and detached entity issues

## Liquibase Migration Best Practices

**ALWAYS follow these rules when creating database migrations:**

### Naming Convention (REQUIRED)

**Timestamp-based naming for ALL new migrations:**
```
yyyyMMddHHmmss-description.xml
```

**Examples:**
- `20260129143000-add-hero-inventory-table.xml`
- `20260129143530-add-index-to-village-name.xml`
- `20260130091500-alter-farming-settings-add-random-interval.xml`

**How to generate timestamp:**
```bash
# Linux/Mac/Git Bash
date +"%Y%m%d%H%M%S"

# PowerShell
Get-Date -Format "yyyyMMddHHmmss"

# Manual
# 2026-01-29 14:30:00 → 20260129143000
```

**Why timestamp-based naming?**
- ✅ Prevents merge conflicts in team environments (unique per developer)
- ✅ Ensures chronological ordering automatically
- ✅ No coordination needed for "next number"
- ✅ Industry standard (Rails, Django, Flyway, etc.)

### Legacy Migrations

**Existing numbered migrations (001-018) remain unchanged:**
- NEVER rename existing migration files
- NEVER renumber existing migrations
- Continue with timestamp format for ALL new migrations after `018-`

### Migration File Structure

**Template for new migration:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260129143000-1" author="your-name">
        <!-- Single logical schema change -->
        <createTable tableName="hero_inventory">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="hero_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="item_type" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="quantity" type="INT" defaultValue="0">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <!-- Rollback instruction -->
        <rollback>
            <dropTable tableName="hero_inventory"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

### Master Changelog Update

**After creating migration file, add to `db.changelog-master.yaml`:**
```yaml
databaseChangeLog:
  # ... existing migrations ...
  - include:
      file: db/changelog/018-village-demolish-queue-schema.xml
  - include:
      file: db/changelog/20260129143000-add-hero-inventory-table.xml  # NEW
```

### Migration Rules

1. **One logical change per file**
   - ✅ GOOD: Create table + indexes in one file
   - ❌ BAD: Create multiple unrelated tables in one file

2. **Always include rollback**
   - Use `<rollback>` tags for reversibility
   - Test rollback before committing

3. **Test on fresh database**
   ```bash
   # Delete existing database
   rm data/travianbot.db

   # Run application (migrations run automatically)
   ./gradlew bootRun

   # Verify schema
   sqlite3 data/travianbot.db ".schema"
   ```

4. **Never modify deployed migrations**
   - If migration has run in ANY environment, create a NEW migration to fix issues
   - Use `ALTER TABLE` in new migration to correct mistakes

5. **Use declarative XML format**
   - Prefer `<createTable>`, `<addColumn>`, `<createIndex>` over raw SQL
   - Only use `<sql>` for complex operations not supported by Liquibase

### Quick Reference

**Create new migration:**
```bash
# 1. Generate timestamp
TIMESTAMP=$(date +"%Y%m%d%H%M%S")

# 2. Create file
touch "sboot/src/main/resources/db/changelog/${TIMESTAMP}-your-description.xml"

# 3. Edit file with template above

# 4. Add to sboot/src/main/resources/db/changelog/db.changelog-master.yaml

# 5. Test migration
./gradlew :sboot:bootRun
```

**Common operations:**
- Create table: `<createTable tableName="...">`
- Add column: `<addColumn tableName="...">`
- Create index: `<createIndex tableName="..." indexName="...">`
- Add constraint: `<addForeignKeyConstraint>`
- Rename column: `<renameColumn tableName="..." oldColumnName="..." newColumnName="..."/>`

**See:** `.specify/memory/constitution.md` Principle 10 for full details
