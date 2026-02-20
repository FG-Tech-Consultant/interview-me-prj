# Liquibase Migration Helper Scripts

## Quick Start

### Create New Migration

```bash
# Interactive script with template
.specify/scripts/bash/create-liquibase-migration.sh "description-of-change"
```

**Examples:**
```bash
.specify/scripts/bash/create-liquibase-migration.sh "add-hero-inventory-table"
.specify/scripts/bash/create-liquibase-migration.sh "add-index-to-village-name"
.specify/scripts/bash/create-liquibase-migration.sh "alter-farming-settings-add-random-interval"
```

## What the Script Does

1. ✅ Generates timestamp in `yyyyMMddHHmmss` format
2. ✅ Creates file: `src/main/resources/db/changelog/{timestamp}-{description}.xml`
3. ✅ Populates with XML template including:
   - Proper XML namespaces for Liquibase 4.20+
   - Changeset with unique ID (timestamp-based)
   - Author from git config
   - Comment placeholders for common operations
   - Rollback section (REQUIRED)
4. ✅ Shows next steps (update master changelog, test migration)
5. ✅ Opens file in editor (VS Code, vim, or nano)

## Script Output

The script creates a file like this:

**Filename:** `20260129143000-add-hero-inventory-table.xml`

**Content:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260129143000-1" author="your-name">
        <!-- TODO: Add your database changes here -->

        <!-- Rollback instruction (REQUIRED) -->
        <rollback>
            <!-- TODO: Add rollback instructions -->
        </rollback>
    </changeSet>
</databaseChangeLog>
```

## Workflow

1. **Run script:**
   ```bash
   .specify/scripts/bash/create-liquibase-migration.sh "add-hero-inventory"
   ```

2. **Edit generated file** (opens automatically):
   - Replace TODOs with actual changes
   - Add rollback instructions

3. **Update master changelog** (`src/main/resources/db/changelog/db.changelog-master.yaml`):
   ```yaml
   - include:
       file: db/changelog/20260129143000-add-hero-inventory.xml
   ```

4. **Test migration:**
   ```bash
   rm data/travianbot.db
   ./gradlew bootRun
   sqlite3 data/travianbot.db ".schema"
   ```

## Common Migration Patterns

### Create Table

```xml
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
</createTable>

<rollback>
    <dropTable tableName="hero_inventory"/>
</rollback>
```

### Add Column

```xml
<addColumn tableName="farming_settings">
    <column name="random_interval_enabled" type="BOOLEAN" defaultValueBoolean="false">
        <constraints nullable="false"/>
    </column>
</addColumn>

<rollback>
    <dropColumn tableName="farming_settings" columnName="random_interval_enabled"/>
</rollback>
```

### Create Index

```xml
<createIndex tableName="villages" indexName="idx_villages_name">
    <column name="name"/>
</createIndex>

<rollback>
    <dropIndex tableName="villages" indexName="idx_villages_name"/>
</rollback>
```

### Add Foreign Key

```xml
<addForeignKeyConstraint
    baseTableName="hero_inventory"
    baseColumnNames="hero_id"
    constraintName="fk_hero_inventory_hero"
    referencedTableName="heroes"
    referencedColumnNames="id"
    onDelete="CASCADE"/>

<rollback>
    <dropForeignKeyConstraint
        baseTableName="hero_inventory"
        constraintName="fk_hero_inventory_hero"/>
</rollback>
```

## Naming Convention

**Format:** `yyyyMMddHHmmss-description.xml`

**Why?**
- ✅ Prevents merge conflicts (unique per developer per second)
- ✅ Chronological ordering automatic
- ✅ No coordination needed for "next number"
- ✅ Industry standard (Rails, Django, Flyway)

**Legacy Files:**
- Existing numbered files (`001-` through `018-`) remain unchanged
- All NEW migrations use timestamp format

## Troubleshooting

### "File already exists"

If you run the script twice in the same second:
```bash
# Wait 1 second and retry
sleep 1
.specify/scripts/bash/create-liquibase-migration.sh "my-change"
```

### Migration fails to run

1. Check syntax in XML file
2. Verify file is added to `db.changelog-master.yaml`
3. Check Liquibase logs in application output

### Need to fix a deployed migration

**NEVER modify the original file.** Create a new migration:
```bash
.specify/scripts/bash/create-liquibase-migration.sh "fix-hero-inventory-column-type"
```

## References

- **Constitution:** `.specify/memory/constitution.md` - Principle 10
- **Guidelines:** `CLAUDE.md` - Liquibase Migration Best Practices
- **Liquibase Docs:** https://docs.liquibase.com/
