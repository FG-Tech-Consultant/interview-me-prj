#!/bin/bash

# Script to create a new Liquibase migration file with timestamp-based naming
# Usage: ./create-liquibase-migration.sh "description of change"

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if description provided
if [ -z "$1" ]; then
    echo -e "${RED}Error: Migration description required${NC}"
    echo "Usage: $0 \"description-of-change\""
    echo ""
    echo "Examples:"
    echo "  $0 \"add-hero-inventory-table\""
    echo "  $0 \"add-index-to-village-name\""
    echo "  $0 \"alter-farming-settings-add-random-interval\""
    exit 1
fi

DESCRIPTION="$1"

# Validate description (no spaces, use hyphens)
if echo "$DESCRIPTION" | grep -q " "; then
    echo -e "${YELLOW}Warning: Description contains spaces. Converting to hyphens...${NC}"
    DESCRIPTION=$(echo "$DESCRIPTION" | tr ' ' '-')
fi

# Generate timestamp
TIMESTAMP=$(date +"%Y%m%d%H%M%S")

# Construct filename
FILENAME="${TIMESTAMP}-${DESCRIPTION}.xml"
FILEPATH="src/main/resources/db/changelog/${FILENAME}"

# Check if file already exists
if [ -f "$FILEPATH" ]; then
    echo -e "${RED}Error: File already exists: ${FILEPATH}${NC}"
    exit 1
fi

# Get author name (from git config or default)
AUTHOR=$(git config user.name 2>/dev/null || echo "developer")

# Create migration file with template
cat > "$FILEPATH" << XMLEOF
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="${TIMESTAMP}-1" author="${AUTHOR}">
        <!-- TODO: Add your database changes here -->
        <!-- Examples:
        <createTable tableName="example_table">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addColumn tableName="existing_table">
            <column name="new_column" type="VARCHAR(100)"/>
        </addColumn>

        <createIndex tableName="example_table" indexName="idx_example_name">
            <column name="name"/>
        </createIndex>
        -->

        <!-- Rollback instruction (REQUIRED) -->
        <rollback>
            <!-- TODO: Add rollback instructions -->
            <!-- Example: <dropTable tableName="example_table"/> -->
        </rollback>
    </changeSet>
</databaseChangeLog>
XMLEOF

echo -e "${GREEN}✓ Created migration file: ${FILEPATH}${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Edit the file and add your database changes"
echo "2. Add rollback instructions"
echo "3. Add to db.changelog-master.yaml:"
echo ""
echo "   - include:"
echo "       file: db/changelog/${FILENAME}"
echo ""
echo "4. Test migration:"
echo "   rm data/travianbot.db && ./gradlew bootRun"
echo ""
echo -e "${GREEN}Opening file in default editor...${NC}"

# Try to open in editor (if available)
if command -v code &> /dev/null; then
    code "$FILEPATH"
elif command -v vim &> /dev/null; then
    vim "$FILEPATH"
elif command -v nano &> /dev/null; then
    nano "$FILEPATH"
else
    echo -e "${YELLOW}No editor found. Please edit manually: ${FILEPATH}${NC}"
fi
