# Implementation Plan: LinkedIn Data Import from ZIP Export

## Architecture Overview

```
User uploads ZIP → LinkedInImportController (REST)
                       ↓
               LinkedInZipParserService (linkedin module)
                  - Extracts ZIP
                  - Parses CSVs (Apache Commons CSV)
                  - Returns LinkedInImportData DTO
                       ↓
               LinkedInImportController stores preview in session/cache
                       ↓
               User confirms → LinkedInImportMappingService (backend module)
                  - Maps parsed data to entities
                  - Handles MERGE/OVERWRITE strategy
                  - Saves to database
                  - Creates LinkedInImport audit record
```

## Module Responsibilities

### linkedin module (parsing)
- `LinkedInZipParserService` - ZIP extraction + CSV parsing
- `LinkedInImportData` record - parsed data DTO
- `LinkedInCsvParser` - individual CSV parsers
- Uses Apache Commons CSV (new dependency)

### backend module (mapping + persistence)
- `LinkedInImportMappingService` - maps parsed data to entities
- `LinkedInImport` entity + repository
- `LinkedInImportController` - REST endpoints
- Liquibase migration for `linkedin_import` table

### frontend (UI)
- `LinkedInImportPage` or section in ProfileEditorPage
- Import wizard (instructions → upload → preview → confirm → success)
- `linkedinImportApi.ts` - API client
- i18n translations (en + pt-BR)

## REST API Design

```
POST   /api/v1/linkedin/import/upload    - Upload ZIP, parse, return preview
POST   /api/v1/linkedin/import/confirm   - Confirm import with strategy
GET    /api/v1/linkedin/import/history    - Get import history
```

## Database Migration

New table: `linkedin_import` with tenant isolation, JSONB for item_counts and errors.

## Dependencies to Add

- `org.apache.commons:commons-csv:1.12.0` in linkedin/build.gradle.kts

## Key Design Decisions

1. **Two-step import** (preview then confirm) - prevents accidental data overwrites
2. **Parsed data stored in-memory between preview and confirm** - no temp tables needed, preview ID links to cached data
3. **Apache Commons CSV over OpenCSV** - lighter, better LinkedIn CSV format handling
4. **Parser in linkedin module** - keeps LinkedIn-specific logic together
5. **Mapping in backend module** - has access to all entity repositories
6. **MERGE as default strategy** - safer for users, additive only
