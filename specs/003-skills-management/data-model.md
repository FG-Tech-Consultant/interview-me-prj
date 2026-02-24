# Data Model: Skills Management

**Feature ID:** 003-skills-management
**Created:** 2026-02-24
**Version:** 1.0.0

---

## Overview

This document defines the data model for the Skills Management feature, which implements a two-tier architecture:
1. **Skills Catalog** (shared across all tenants) - Canonical skill definitions
2. **User Skills** (tenant-isolated) - User skill claims with rich metadata

---

## Entity Relationship Diagram

```
┌──────────────┐
│   Tenant     │
│              │
│ - id (PK)    │
│ - name       │
└──────┬───────┘
       │
       │ 1:N
       │
┌──────▼───────┐         ┌──────────────┐
│   Profile    │         │    Skill     │ (Shared Catalog - No tenant_id)
│              │         │              │
│ - id (PK)    │         │ - id (PK)    │
│ - tenant_id  │         │ - name       │ (UNIQUE)
│ - user_id    │         │ - category   │
│ - full_name  │         │ - description│
└──────┬───────┘         │ - tags       │ (JSONB)
       │                 │ - is_active  │
       │                 └──────┬───────┘
       │                        │
       │ 1:N                    │ 1:N
       │                        │
       │      ┌─────────────────┘
       │      │
       │      │
┌──────▼──────▼─────┐
│   UserSkill       │ (Tenant-Isolated)
│                   │
│ - id (PK)         │
│ - tenant_id (FK)  │ → Tenant.id
│ - profile_id (FK) │ → Profile.id
│ - skill_id (FK)   │ → Skill.id
│ - years_of_experience
│ - proficiency_depth (1-5)
│ - last_used_date
│ - confidence_level
│ - tags (JSONB)
│ - visibility (public/private)
│ - created_at
│ - updated_at
│ - deleted_at (soft delete)
│ - version (optimistic locking)
└───────────────────┘

UNIQUE (tenant_id, profile_id, skill_id) WHERE deleted_at IS NULL
```

---

## Entity Definitions

### Skill (Canonical Catalog)

**Purpose:** Shared catalog of standardized skill definitions across all tenants.

**Table Name:** `skill`

**Attributes:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing skill ID |
| name | VARCHAR(255) | NOT NULL, UNIQUE | Skill name (e.g., "Java", "React", "Kubernetes") |
| category | VARCHAR(100) | NOT NULL | Skill category: Languages, Frameworks, Cloud, Databases, Messaging, Observability, Methodologies, Domains |
| description | TEXT | NULL | Detailed skill description for tooltips |
| tags | JSONB | NULL | Array of tags (e.g., ["#backend", "#jvm", "#enterprise"]) |
| is_active | BOOLEAN | NOT NULL, DEFAULT true | Availability flag (false hides from autocomplete) |
| created_at | TIMESTAMPTZ | NOT NULL | Creation timestamp (UTC) |
| updated_at | TIMESTAMPTZ | NOT NULL | Last update timestamp (UTC) |

**Indexes:**
- `idx_skill_name` (UNIQUE): Fast autocomplete search
- `idx_skill_category`: Grouping skills by category
- `idx_skill_is_active`: Filter active skills

**Relationships:**
- One-to-many with UserSkill (one catalog skill claimed by many users)

**Validation Rules:**
- `name`: Required, 1-255 characters, unique across catalog, trimmed
- `category`: Required, 1-100 characters, one of predefined categories
- `description`: Optional, max 2000 characters
- `tags`: Array of strings, each 1-50 characters, max 20 tags
- `is_active`: Boolean, default true

**Example Data:**

```json
{
  "id": 1,
  "name": "Java",
  "category": "Languages",
  "description": "Object-oriented programming language for enterprise applications",
  "tags": ["#backend", "#jvm", "#enterprise"],
  "is_active": true,
  "created_at": "2026-01-15T10:00:00Z",
  "updated_at": "2026-01-15T10:00:00Z"
}
```

---

### UserSkill (Tenant-Isolated Claims)

**Purpose:** User-specific skill claims with metadata (proficiency, years, last used, etc.)

**Table Name:** `user_skill`

**Attributes:**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | Auto-incrementing user skill ID |
| tenant_id | BIGINT | NOT NULL, FK → tenant(id) | Tenant isolation (automatic filter) |
| profile_id | BIGINT | NOT NULL, FK → profile(id) | Profile this skill belongs to |
| skill_id | BIGINT | NOT NULL, FK → skill(id) | Reference to canonical catalog skill |
| years_of_experience | INT | NOT NULL, DEFAULT 0, CHECK >= 0 | Years of practical experience (0-70) |
| proficiency_depth | INT | NOT NULL, CHECK 1-5 | Proficiency level: 1=Beginner, 2=Intermediate, 3=Proficient, 4=Advanced, 5=Expert |
| last_used_date | DATE | NULL | Most recent use date (month precision) |
| confidence_level | VARCHAR(20) | NOT NULL, DEFAULT 'MEDIUM' | Self-assessed confidence: LOW, MEDIUM, HIGH |
| tags | JSONB | NULL | Custom user tags (e.g., ["#production", "#scalability"]) |
| visibility | VARCHAR(20) | NOT NULL, DEFAULT 'private' | public or private (controls recruiter chat visibility) |
| created_at | TIMESTAMPTZ | NOT NULL | Creation timestamp (UTC) |
| updated_at | TIMESTAMPTZ | NOT NULL | Last update timestamp (UTC) |
| deleted_at | TIMESTAMPTZ | NULL | Soft delete timestamp (NULL = active) |
| version | BIGINT | NOT NULL, DEFAULT 0 | Optimistic locking version |

**Indexes:**
- `idx_user_skill_tenant_id`: Tenant filtering
- `idx_user_skill_profile_id`: Profile-based queries
- `idx_user_skill_skill_id`: Skill-based lookups
- `idx_user_skill_deleted_at`: Soft delete filtering
- `idx_user_skill_unique` (UNIQUE): (tenant_id, profile_id, skill_id) WHERE deleted_at IS NULL

**Relationships:**
- Many-to-one with Tenant (many user skills belong to one tenant)
- Many-to-one with Profile (many user skills belong to one profile)
- Many-to-one with Skill catalog (many user skills reference one canonical skill)

**Validation Rules:**
- `years_of_experience`: 0-70 (sanity check)
- `proficiency_depth`: 1-5 inclusive, required
- `last_used_date`: <= today (no future dates)
- `confidence_level`: Must be "LOW", "MEDIUM", or "HIGH"
- `tags`: Array of strings, each 1-50 characters, max 20 tags
- `visibility`: Must be "public" or "private"
- Unique constraint: Cannot add same skill_id twice to same profile (per tenant)

**Proficiency Scale:**

| Level | Name | Description |
|-------|------|-------------|
| 1 | Beginner | Basic understanding, limited practical use |
| 2 | Intermediate | Can work with guidance |
| 3 | Proficient | Independent work, production experience |
| 4 | Advanced | Deep expertise, mentors others |
| 5 | Expert | Recognized authority, architects solutions |

**Example Data:**

```json
{
  "id": 101,
  "tenant_id": 5,
  "profile_id": 25,
  "skill_id": 1,
  "years_of_experience": 12,
  "proficiency_depth": 5,
  "last_used_date": "2026-01-01",
  "confidence_level": "HIGH",
  "tags": ["#spring-boot", "#microservices", "#production"],
  "visibility": "public",
  "created_at": "2026-02-20T14:30:00Z",
  "updated_at": "2026-02-20T14:30:00Z",
  "deleted_at": null,
  "version": 0
}
```

---

## Database Constraints

### Foreign Keys

- `user_skill.tenant_id` → `tenant.id` (ON DELETE RESTRICT)
- `user_skill.profile_id` → `profile.id` (ON DELETE CASCADE)
- `user_skill.skill_id` → `skill.id` (ON DELETE RESTRICT)

### Check Constraints

```sql
ALTER TABLE user_skill ADD CONSTRAINT chk_years_of_experience
    CHECK (years_of_experience >= 0 AND years_of_experience <= 70);

ALTER TABLE user_skill ADD CONSTRAINT chk_proficiency_depth
    CHECK (proficiency_depth BETWEEN 1 AND 5);

ALTER TABLE user_skill ADD CONSTRAINT chk_confidence_level
    CHECK (confidence_level IN ('LOW', 'MEDIUM', 'HIGH'));

ALTER TABLE user_skill ADD CONSTRAINT chk_visibility
    CHECK (visibility IN ('public', 'private'));
```

### Unique Constraints

```sql
CREATE UNIQUE INDEX idx_user_skill_unique ON user_skill(tenant_id, profile_id, skill_id)
    WHERE deleted_at IS NULL;
```

This ensures a user cannot add the same skill twice to their profile (excluding soft-deleted skills).

---

## Soft Delete Strategy

**Approach:** Set `deleted_at` timestamp instead of hard DELETE.

**Benefits:**
- Preserves skill history for audit trails
- Enables potential "undo" or skill resurrection
- Maintains referential integrity with other tables (e.g., future story_skill links)

**Implementation:**
- All queries filter `WHERE deleted_at IS NULL` automatically
- Unique constraint excludes deleted records: `WHERE deleted_at IS NULL`
- Deletion operation: `UPDATE user_skill SET deleted_at = NOW() WHERE id = ?`

---

## Tenant Isolation

**UserSkill Table:**
- Every row includes `tenant_id` FK
- Hibernate filter from Feature 001 automatically applies `WHERE tenant_id = :currentTenantId` to all queries
- Cross-tenant access returns 404 (not 403 to avoid leaking tenant information)

**Skill Catalog Table:**
- NO `tenant_id` (shared across all tenants)
- All tenants benefit from expanding catalog
- Admin CRUD operations available to ADMIN role users

---

## Public/Private Visibility

**UserSkill.visibility Field:**
- `public`: Skill visible in recruiter chat, public profile pages, and exports
- `private`: Skill visible only to profile owner (not in public APIs)

**Implementation:**
- Service layer filters skills by visibility flag
- `getPublicSkillsByProfile(profileId)` returns only public skills
- `getSkillsByProfile(profileId)` returns all skills (for profile owner view)

---

## Optimistic Locking

**UserSkill.version Field:**
- Incremented on every UPDATE
- Prevents lost updates when two sessions edit same skill concurrently
- JPA `@Version` annotation handles this automatically
- Concurrent update attempt returns 409 Conflict

**Example:**
1. User A reads UserSkill with version=0
2. User B reads same UserSkill with version=0
3. User A updates proficiency, version incremented to 1, save succeeds
4. User B updates years, tries to save with version=0, save fails (409 Conflict)

---

## Future Extensions

### Skill Catalog Enhancements
- **Skill synonyms table:** Map "React" → "React.js" for deduplication
- **Skill hierarchy:** Parent-child relationships (e.g., Spring Boot → Java)
- **Skill embeddings:** Add vector column for semantic search

### User Skill Enhancements
- **story_skill join table:** Link skills to STAR stories (Feature 004)
- **skill_endorsements table:** LinkedIn-style skill endorsements (Feature 012)
- **skill_proficiency_history:** Track proficiency changes over time

---

## Migration Strategy

### Initial Migration (Feature 003)

1. Create `skill` table with indexes
2. Create `user_skill` table with indexes and constraints
3. Seed `skill` table with 100-200 common skills (separate seed script)

### Seed Data Script

```sql
-- Sample seed data for skill catalog
INSERT INTO skill (name, category, description, tags, is_active) VALUES
('Java', 'Languages', 'Object-oriented programming language for enterprise applications', '["#backend", "#jvm", "#enterprise"]', true),
('Spring Boot', 'Frameworks', 'Spring Framework for building stand-alone production-grade applications', '["#backend", "#java", "#microservices"]', true),
('React', 'Frameworks', 'JavaScript library for building user interfaces', '["#frontend", "#javascript", "#ui"]', true),
('PostgreSQL', 'Databases', 'Open-source relational database', '["#database", "#sql", "#backend"]', true),
('Kubernetes', 'Cloud', 'Container orchestration platform', '["#devops", "#cloud", "#infrastructure"]', true);
-- ... (continue with 100-200 skills)
```

---

**Data Model Version:** 1.0.0
**Last Updated:** 2026-02-24
