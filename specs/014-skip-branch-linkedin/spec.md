# Feature Specification: LinkedIn Data Import from ZIP Export

**Feature ID:** 014
**Status:** In Progress
**Created:** 2026-03-01
**Last Updated:** 2026-03-01
**Version:** 1.0.0

---

## Overview

### Problem Statement

Users currently have to manually enter all their career data (jobs, education, skills, projects) into the platform. This is tedious and time-consuming, especially for users with extensive LinkedIn profiles. The existing LinkedIn PDF parser only supports profile scoring/analysis, not data import into the user's profile.

### Feature Summary

Allow users to upload their LinkedIn data export ZIP file (downloaded from LinkedIn Settings > Data Privacy > Get a copy of your data) and automatically parse and import structured career data into their Interview Me profile. The ZIP contains CSV files (Profile.csv, Positions.csv, Education.csv, Skills.csv, Languages.csv, Projects.csv, Certifications.csv) which map directly to existing app entities.

### Target Users

- New users who want to quickly populate their profile from LinkedIn
- Existing users who want to sync/update their profile with latest LinkedIn data

---

## Constitution Compliance

- **Principle 1: Simplicity First** - Simple ZIP upload flow, reuses existing entities
- **Principle 4: Data Sovereignty** - All data stored in PostgreSQL, tenant-isolated
- **Principle 7: Security & Privacy** - ZIP processed server-side, temp files cleaned up, no LinkedIn API keys needed
- **Principle 8: Multi-Tenant Architecture** - All imported data tagged with tenant_id
- **Principle 10: Full-Stack Modularity** - Parser in linkedin module, mapping in backend module
- **Principle 11: Database Schema Evolution** - Liquibase migration for import history table
- **Principle 13: LinkedIn Compliance** - 100% ToS compliant (user downloads their own data)

---

## User Scenarios

### Scenario 1: First-time Import

**Actor:** New user with empty profile
**Goal:** Populate profile from LinkedIn data

**Steps:**
1. User navigates to Profile Editor > "Import from LinkedIn" section
2. User sees instructions on how to download LinkedIn data export
3. User uploads the ZIP file
4. System parses and shows preview: "Found: 5 jobs, 3 education entries, 25 skills, 2 languages"
5. User selects import strategy (Merge recommended for first import)
6. User confirms import
7. System imports data and shows success summary

### Scenario 2: Update Existing Profile

**Actor:** Existing user with data already in profile
**Goal:** Update profile with latest LinkedIn data

**Steps:**
1. User uploads new ZIP file
2. System shows preview with diff: "New: 1 job, 3 skills. Updated: 0. Existing: 4 jobs, 22 skills"
3. User chooses MERGE (add new, keep existing) or OVERWRITE (replace all)
4. System executes import and shows results

### Edge Cases

- ZIP missing expected CSV files (partial export) → import what's available, warn about missing
- CSV with unexpected encoding → handle UTF-8 and Latin-1
- Duplicate skills (same name) → skip duplicates, log warning
- Date format variations → support multiple LinkedIn date formats
- Empty ZIP or non-LinkedIn ZIP → clear error message
- Very large ZIP (>50MB) → reject with size limit error

---

## Functional Requirements

### Core Capabilities

**REQ-001:** ZIP File Upload and Validation
- Accept ZIP files up to 50MB
- Validate ZIP structure (must contain at least one recognized LinkedIn CSV)
- Reject non-ZIP files and corrupted archives

**REQ-002:** CSV Parsing
- Parse Profile.csv → fullName, headline, summary, location
- Parse Positions.csv → company, title, startDate, endDate, description
- Parse Education.csv → school, degree, fieldOfStudy, startDate, endDate, notes/activities
- Parse Skills.csv → skill names
- Parse Languages.csv → language names
- Parse Projects.csv → title, description, URL, dates (optional, if file exists)
- Parse Certifications.csv → name, authority, dates (optional, if file exists)

**REQ-003:** Import Preview
- Show parsed data summary before import (counts per category)
- Allow user to review what will be imported
- No data persisted until user confirms

**REQ-004:** Import Strategies
- MERGE: Add new items, skip existing duplicates (matched by company+role for jobs, institution+degree for education, name for skills)
- OVERWRITE: Delete all existing data in category and replace with imported data

**REQ-005:** Import History
- Track each import: timestamp, filename, strategy, item counts, status
- Allow viewing past imports

**REQ-006:** Data Mapping
- Map LinkedIn CSV fields to existing entities (Profile, JobExperience, Education, Skill, UserSkill)
- Parse LinkedIn date formats (Month Year, e.g., "Jan 2020", "2020")
- Set sensible defaults for missing fields (visibility=PUBLIC, proficiencyDepth=5)

---

## Key Entities

### LinkedInImport (NEW)

**Attributes:**
- id: BIGINT (PK)
- tenant_id: BIGINT (FK)
- profile_id: BIGINT (FK)
- status: VARCHAR (PENDING, PREVIEW, COMPLETED, FAILED)
- filename: VARCHAR
- import_strategy: VARCHAR (MERGE, OVERWRITE)
- item_counts: JSONB ({"jobs": 5, "education": 3, "skills": 25, "languages": 2})
- errors: JSONB (list of warning/error messages)
- imported_at: TIMESTAMPTZ
- created_at: TIMESTAMPTZ
- updated_at: TIMESTAMPTZ

---

## Out of Scope

1. LinkedIn OAuth / API integration (not needed for ZIP import)
2. Automatic LinkedIn data sync (user must manually download and upload)
3. Web scraping of LinkedIn profiles
4. Importing connections, messages, or recommendations
5. Importing profile photos from ZIP
6. DMA Portability API (EU-only, future enhancement)

---

## Testing Scope

### Functional Testing
- ZIP parsing with valid LinkedIn export
- Each CSV parser individually
- MERGE vs OVERWRITE strategies
- Error handling for malformed data

### Integration Testing
- Full import flow: upload → preview → confirm → verify DB
- Tenant isolation verification
- Duplicate handling

### E2E Testing
- Frontend wizard flow with Playwright
- Upload, preview, confirm, verify imported data appears in profile

---

## Revision History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0.0 | 2026-03-01 | Initial specification | Claude |
