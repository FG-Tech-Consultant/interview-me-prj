# Implementation Plan: Profile CRUD (Timeline, Jobs, Education)

**Feature ID:** 002-profile-crud
**Status:** Design Complete
**Created:** 2026-02-23
**Last Updated:** 2026-02-23
**Version:** 1.0.0

---

## Executive Summary

This implementation plan defines the technical design for the Profile CRUD feature, which provides comprehensive career timeline management (profile, job experiences, education history) for the Live Resume & Career Copilot platform. The implementation follows a standard three-layer architecture (Controller → Service → Repository) using Spring Boot 4.x REST APIs on the backend and React 18 with MUI components on the frontend. All entities implement multi-tenant isolation via automatic Hibernate filtering, public/private visibility controls, and soft delete patterns established in Feature 001.

The design leverages PostgreSQL JSONB columns for flexible metadata storage (career preferences, job metrics, professional links), enabling rich data capture without rigid schema constraints. Optimistic locking with version fields prevents concurrent update conflicts. The frontend provides an intuitive profile editor with tabbed sections, inline validation, and visual public/private indicators.

**Key Deliverables:**
- Backend: 3 JPA entities (Profile, JobExperience, Education), 3 repositories, 3 services, 3 REST controllers, 6 DTOs (request/response records)
- Database: 3 Liquibase migrations creating tables with proper indexes, foreign keys, and constraints
- Frontend: 4 React pages (ProfileEditorPage, JobExperienceForm, EducationForm, ProfileCompleteness), API client modules, TanStack Query hooks
- Testing: Unit tests for services, integration tests for REST endpoints, edge case tests for validation

**Timeline:** 10 days (assuming Feature 001 is complete)
**Complexity:** Medium (standard CRUD with multi-tenant isolation and JSONB handling)

---

## Constitution Check

### Applicable Principles Validation

**✅ Principle 1: Simplicity First**
- **Alignment:** Uses standard Spring Data JPA CRUD patterns, conventional REST endpoints, no complex abstractions
- **Evidence:** Controller → Service → Repository layering, standard HTTP methods (GET/POST/PUT/DELETE), MUI form components
- **Gate:** PASSED

**✅ Principle 2: Containerization as First-Class Citizen**
- **Alignment:** No new container requirements, uses existing backend+frontend container from Feature 001
- **Evidence:** Stateless services, database-backed persistence, no local file storage
- **Gate:** PASSED (N/A for feature-level changes)

**✅ Principle 3: Modern Java Standards**
- **Alignment:** Uses Java 25 records for DTOs, JPA 3.x features, Spring Boot 4.x conventions, Lombok annotations
- **Evidence:** DTOs as records (e.g., `CreateProfileRequest`, `ProfileResponse`), `@Transactional(readOnly = true)` for read operations, `@Slf4j` for logging
- **Gate:** PASSED

**✅ Principle 4: Data Sovereignty and Multi-Tenant Isolation**
- **Alignment:** All entities include tenant_id with automatic Hibernate filtering, PostgreSQL JSONB for flexible data, Liquibase migrations
- **Evidence:** Profile/JobExperience/Education entities have tenant_id foreign keys, indexed for performance, filtered automatically via TenantContext + Hibernate filter from Feature 001
- **Gate:** PASSED

**✅ Principle 6: Observability and Debugging**
- **Alignment:** Structured logging for all CRUD operations with tenant context, validation errors logged, metrics tracked
- **Evidence:** `@Slf4j` on all services, `log.info("Profile created: profileId={}, tenantId={}, userId={}", ...)`), Spring Actuator metrics
- **Gate:** PASSED

**✅ Principle 7: Security, Privacy, and Credential Management**
- **Alignment:** JWT authentication required for all endpoints, public/private visibility flags enforced at service layer, soft delete prevents data loss
- **Evidence:** `@PreAuthorize("hasRole('USER')")` on controllers, service methods filter by visibility flag, optimistic locking with version field
- **Gate:** PASSED

**✅ Principle 8: Multi-Tenant Architecture**
- **Alignment:** All entities (Profile, JobExperience, Education) include tenant_id, filtered automatically by Hibernate filter
- **Evidence:** Entity annotations: `@FilterDef`, `@Filter(name = "tenantFilter")`, TenantContext.getTenantId() in services
- **Gate:** PASSED

**✅ Principle 10: Full-Stack Modularity and Separation of Concerns**
- **Alignment:** Backend organized in packages (profile, controller, service, repository), frontend in feature directories, services <500 LOC, components <300 LOC
- **Evidence:** `com.interviewme.profile.controller`, `com.interviewme.profile.service`, frontend components in `pages/ProfileEditor/`, `components/JobExperienceCard/`
- **Gate:** PASSED

**✅ Principle 11: Database Schema Evolution**
- **Alignment:** Liquibase timestamp-based migrations for all profile tables, rollback support, master changelog integration
- **Evidence:** Migrations: `20260223120000-create-profile-table.xml`, `20260223120100-create-job-experience-table.xml`, `20260223120200-create-education-table.xml`
- **Gate:** PASSED

### Overall Constitution Compliance: ✅ PASSED

No exceptions or waivers required. This design fully aligns with all applicable constitutional principles.

---

## Technical Architecture

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                  User's Browser (React SPA)                  │
│           Served by Spring Boot from /static                 │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/REST API (JSON over same origin)
                        │
┌───────────────────────▼─────────────────────────────────────┐
│               Spring Boot Backend (Java 25)                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  REST Controllers (/api/v1/profiles)                  │  │
│  │  - ProfileController                                 │  │
│  │  - JobExperienceController                           │  │
│  │  - EducationController                               │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Business Logic Services                              │  │
│  │  - ProfileService: CRUD, visibility filtering        │  │
│  │  - JobExperienceService: CRUD, soft delete           │  │
│  │  - EducationService: CRUD, soft delete               │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Data Access Layer (Spring Data JPA)                 │  │
│  │  - ProfileRepository                                 │  │
│  │  - JobExperienceRepository                           │  │
│  │  │  - EducationRepository                             │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Hibernate + Tenant Filter Aspect                    │  │
│  │  - Automatically adds WHERE tenant_id = :tenantId    │  │
│  └────────────────────┬─────────────────────────────────┘  │
└───────────────────────┼─────────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────────┐
│              PostgreSQL 18 + pgvector                        │
│  Tables: profile, job_experience, education                 │
│  JSONB columns: languages, professional_links,               │
│                 career_preferences, metrics                  │
└─────────────────────────────────────────────────────────────┘
```

### Request Flow: Create Job Experience

```
1. User fills out "Add Job Experience" form in frontend
   - Company: "Acme Corp"
   - Role: "Senior Software Engineer"
   - Start date: 2020-01-01, End date: 2023-12-31
   - Achievements: "Built high-scale payment system..."
   - Metrics: {"tps": 50000, "latency_p95": "50ms"}

2. Frontend submits POST /api/v1/profiles/{profileId}/jobs
   - Axios POST request with CreateJobExperienceRequest DTO
   - JWT token in Authorization header

3. JwtAuthenticationFilter extracts tenant ID from JWT
   - Sets TenantContext.setTenantId(123L)
   - Proceeds to controller

4. JobExperienceController.createJobExperience(@Valid CreateJobExperienceRequest request)
   - Validates request body (Spring @Valid)
   - Calls jobExperienceService.create(profileId, request)

5. JobExperienceService.create(Long profileId, CreateJobExperienceRequest request)
   - @Transactional method starts transaction
   - Validates profile exists and belongs to tenant (via ProfileRepository)
   - Creates JobExperience entity with tenant_id from TenantContext
   - Validates date logic (end_date >= start_date)
   - Calls jobExperienceRepository.save(jobExperience)
   - Logs: log.info("Job experience created: jobId={}, profileId={}, tenantId={}", ...)
   - Returns JobExperienceResponse DTO

6. Hibernate applies tenant filter automatically
   - TenantFilterAspect @Around advice enables filter: session.enableFilter("tenantFilter").setParameter("tenantId", 123L)
   - INSERT query includes tenant_id = 123

7. Controller returns 201 Created with Location header
   - Response body: JobExperienceResponse JSON
   - Location: /api/v1/profiles/{profileId}/jobs/{jobId}

8. Frontend receives response
   - TanStack Query mutation onSuccess callback
   - Invalidates profile query cache
   - Updates UI with new job experience card
   - Shows success Snackbar: "Job experience added successfully"
```

---

## Data Model

### Entity-Relationship Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                           Tenant                             │
│  - id: BIGSERIAL PK                                          │
│  - name: VARCHAR(255)                                        │
│  - created_at: TIMESTAMPTZ                                   │
│  - settings: JSONB                                           │
└────────────────┬────────────────────────────────────────────┘
                 │ 1
                 │
                 │ N
┌────────────────▼────────────────────────────────────────────┐
│                            User                              │
│  - id: BIGSERIAL PK                                          │
│  - tenant_id: BIGINT FK (Tenant)                             │
│  - email: VARCHAR(255) UNIQUE                                │
│  - password_hash: VARCHAR(255)                               │
│  - created_at: TIMESTAMPTZ                                   │
└────────────────┬────────────────────────────────────────────┘
                 │ 1
                 │
                 │ 1
┌────────────────▼────────────────────────────────────────────┐
│                          Profile                             │
│  - id: BIGSERIAL PK                                          │
│  - tenant_id: BIGINT FK (Tenant)                             │
│  - user_id: BIGINT FK (User) UNIQUE                          │
│  - full_name: VARCHAR(255) NOT NULL                          │
│  - headline: VARCHAR(255) NOT NULL                           │
│  - summary: TEXT                                             │
│  - location: VARCHAR(255)                                    │
│  - languages: JSONB (array)                                  │
│  - professional_links: JSONB (object)                        │
│  - career_preferences: JSONB (object)                        │
│  - default_visibility: VARCHAR(20) DEFAULT 'private'         │
│  - created_at: TIMESTAMPTZ NOT NULL                          │
│  - updated_at: TIMESTAMPTZ NOT NULL                          │
│  - version: BIGINT DEFAULT 0 (optimistic locking)            │
└────────────────┬────────────────────────────────────────────┘
                 │ 1
                 │
       ┌─────────┴─────────┐
       │ N                 │ N
       │                   │
┌──────▼─────────┐   ┌─────▼──────────┐
│ JobExperience  │   │   Education    │
│                │   │                │
│ - id           │   │ - id           │
│ - tenant_id    │   │ - tenant_id    │
│ - profile_id   │   │ - profile_id   │
│ - company      │   │ - degree       │
│ - role         │   │ - institution  │
│ - start_date   │   │ - start_date   │
│ - end_date     │   │ - end_date     │
│ - is_current   │   │ - field_study  │
│ - location     │   │ - gpa          │
│ - emp_type     │   │ - notes        │
│ - resp.        │   │ - visibility   │
│ - achieve.     │   │ - created_at   │
│ - metrics(JSON)│   │ - updated_at   │
│ - visibility   │   │ - deleted_at   │
│ - created_at   │   │ - version      │
│ - updated_at   │   │                │
│ - deleted_at   │   │                │
│ - version      │   │                │
└────────────────┘   └────────────────┘
```

### JSONB Schema Examples

**Profile.languages** (JSONB array):
```json
["English", "Portuguese", "Spanish"]
```

**Profile.professional_links** (JSONB object):
```json
{
  "linkedin": "https://linkedin.com/in/username",
  "github": "https://github.com/username",
  "portfolio": "https://example.com"
}
```

**Profile.career_preferences** (JSONB object):
```json
{
  "target_roles": ["Tech Lead", "Principal Engineer", "Architect"],
  "domains": ["Fintech", "Payments", "Marketplaces"],
  "target_geographies": ["EU", "Remote"],
  "remote_willing": true,
  "relocation_willing": false
}
```

**JobExperience.metrics** (JSONB object):
```json
{
  "tps": 50000,
  "latency_p50": "20ms",
  "latency_p95": "50ms",
  "latency_p99": "100ms",
  "users": "10M",
  "transactions_per_day": 50000000,
  "uptime": "99.99%"
}
```

---

## Backend Design

### Package Structure

```
backend/src/main/java/com/interviewme/
├── profile/
│   ├── controller/
│   │   ├── ProfileController.java
│   │   ├── JobExperienceController.java
│   │   └── EducationController.java
│   ├── service/
│   │   ├── ProfileService.java
│   │   ├── JobExperienceService.java
│   │   └── EducationService.java
│   ├── repository/
│   │   ├── ProfileRepository.java
│   │   ├── JobExperienceRepository.java
│   │   └── EducationRepository.java
│   ├── model/
│   │   ├── Profile.java
│   │   ├── JobExperience.java
│   │   └── Education.java
│   └── dto/
│       ├── CreateProfileRequest.java (record)
│       ├── UpdateProfileRequest.java (record)
│       ├── ProfileResponse.java (record)
│       ├── CreateJobExperienceRequest.java (record)
│       ├── JobExperienceResponse.java (record)
│       ├── CreateEducationRequest.java (record)
│       └── EducationResponse.java (record)
└── common/
    └── exception/
        ├── ProfileNotFoundException.java
        ├── OptimisticLockException.java
        └── ValidationException.java
```

### REST API Endpoints

**Profile Endpoints:**

```
GET    /api/v1/profiles/{profileId}
  → Get full profile with jobs and education
  → Returns: ProfileResponse (includes nested jobs and education arrays)
  → Auth: Required (JWT)
  → Filters: Tenant automatic, soft delete excluded

POST   /api/v1/profiles
  → Create new profile for authenticated user
  → Body: CreateProfileRequest
  → Returns: 201 Created, ProfileResponse
  → Auth: Required (JWT)
  → Validation: One profile per user per tenant (unique constraint)

PUT    /api/v1/profiles/{profileId}
  → Update existing profile
  → Body: UpdateProfileRequest
  → Returns: 200 OK, ProfileResponse
  → Auth: Required (JWT), tenant isolation
  → Validation: Optimistic locking (version field)

DELETE /api/v1/profiles/{profileId}
  → Soft delete profile (sets deleted_at)
  → Returns: 204 No Content
  → Auth: Required (JWT), tenant isolation
```

**Job Experience Endpoints:**

```
GET    /api/v1/profiles/{profileId}/jobs
  → List all job experiences for profile
  → Query params: visibility (public/private/all), sort (start_date DESC)
  → Returns: List<JobExperienceResponse>
  → Auth: Required (JWT), tenant isolation

POST   /api/v1/profiles/{profileId}/jobs
  → Create new job experience
  → Body: CreateJobExperienceRequest
  → Returns: 201 Created, JobExperienceResponse
  → Auth: Required (JWT), tenant isolation

PUT    /api/v1/profiles/{profileId}/jobs/{jobId}
  → Update existing job experience
  → Body: UpdateJobExperienceRequest
  → Returns: 200 OK, JobExperienceResponse
  → Auth: Required (JWT), tenant isolation, optimistic locking

DELETE /api/v1/profiles/{profileId}/jobs/{jobId}
  → Soft delete job experience (sets deleted_at)
  → Returns: 204 No Content
  → Auth: Required (JWT), tenant isolation
```

**Education Endpoints:**

```
GET    /api/v1/profiles/{profileId}/education
  → List all education entries for profile
  → Query params: visibility (public/private/all), sort (end_date DESC)
  → Returns: List<EducationResponse>
  → Auth: Required (JWT), tenant isolation

POST   /api/v1/profiles/{profileId}/education
  → Create new education entry
  → Body: CreateEducationRequest
  → Returns: 201 Created, EducationResponse
  → Auth: Required (JWT), tenant isolation

PUT    /api/v1/profiles/{profileId}/education/{educationId}
  → Update existing education entry
  → Body: UpdateEducationRequest
  → Returns: 200 OK, EducationResponse
  → Auth: Required (JWT), tenant isolation, optimistic locking

DELETE /api/v1/profiles/{profileId}/education/{educationId}
  → Soft delete education entry (sets deleted_at)
  → Returns: 204 No Content
  → Auth: Required (JWT), tenant isolation
```

### Service Layer Design

**ProfileService** (com.interviewme.profile.service.ProfileService):

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByUserId(Long userId) {
        Long tenantId = TenantContext.getTenantId();
        Profile profile = profileRepository
            .findByUserIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new ProfileNotFoundException(userId));
        log.info("Profile retrieved: profileId={}, userId={}, tenantId={}",
                 profile.getId(), userId, tenantId);
        return ProfileMapper.toResponse(profile);
    }

    @Transactional
    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        Long tenantId = TenantContext.getTenantId();

        // Validate user exists and belongs to tenant
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Check if profile already exists (one per user)
        if (profileRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
            throw new DuplicateProfileException("User already has a profile");
        }

        Profile profile = ProfileMapper.toEntity(request);
        profile.setTenantId(tenantId);
        profile.setUserId(userId);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        Profile saved = profileRepository.save(profile);
        log.info("Profile created: profileId={}, userId={}, tenantId={}",
                 saved.getId(), userId, tenantId);
        return ProfileMapper.toResponse(saved);
    }

    @Transactional
    public ProfileResponse updateProfile(Long profileId, UpdateProfileRequest request) {
        Long tenantId = TenantContext.getTenantId();

        Profile profile = profileRepository
            .findByIdAndDeletedAtIsNull(profileId)
            .orElseThrow(() -> new ProfileNotFoundException(profileId));

        // Optimistic locking check
        if (!profile.getVersion().equals(request.version())) {
            throw new OptimisticLockException("Profile was modified by another session");
        }

        ProfileMapper.updateEntity(profile, request);
        profile.setUpdatedAt(Instant.now());

        Profile saved = profileRepository.save(profile);
        log.info("Profile updated: profileId={}, tenantId={}", profileId, tenantId);
        return ProfileMapper.toResponse(saved);
    }

    @Transactional
    public void deleteProfile(Long profileId) {
        Long tenantId = TenantContext.getTenantId();

        Profile profile = profileRepository
            .findByIdAndDeletedAtIsNull(profileId)
            .orElseThrow(() -> new ProfileNotFoundException(profileId));

        profile.setDeletedAt(Instant.now());
        profileRepository.save(profile);
        log.info("Profile soft deleted: profileId={}, tenantId={}", profileId, tenantId);
    }
}
```

**JobExperienceService** follows similar pattern with additional validation:
- Start date must be before end date
- If `is_current = true`, end_date must be NULL
- employment_type must be in allowed set

**EducationService** follows similar pattern with validation:
- If start_date provided, must be before end_date
- end_date must not be in future

### Repository Layer Design

**ProfileRepository** (com.interviewme.profile.repository.ProfileRepository):

```java
@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByUserIdAndDeletedAtIsNull(Long userId);

    Optional<Profile> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT p FROM Profile p " +
           "WHERE p.id = :id AND p.deletedAt IS NULL AND p.tenantId = :tenantId")
    Optional<Profile> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
```

**JobExperienceRepository**:

```java
@Repository
public interface JobExperienceRepository extends JpaRepository<JobExperience, Long> {

    @Query("SELECT j FROM JobExperience j " +
           "WHERE j.profileId = :profileId AND j.deletedAt IS NULL " +
           "ORDER BY j.startDate DESC")
    List<JobExperience> findByProfileIdOrderByStartDateDesc(@Param("profileId") Long profileId);

    @Query("SELECT j FROM JobExperience j " +
           "WHERE j.profileId = :profileId AND j.visibility = :visibility AND j.deletedAt IS NULL " +
           "ORDER BY j.startDate DESC")
    List<JobExperience> findByProfileIdAndVisibility(
        @Param("profileId") Long profileId,
        @Param("visibility") String visibility
    );

    Optional<JobExperience> findByIdAndDeletedAtIsNull(Long id);
}
```

**EducationRepository** follows similar pattern.

---

## Frontend Design

### Component Structure

```
frontend/src/
├── pages/
│   ├── ProfileEditorPage.tsx (main profile editor, tabbed layout)
│   └── DashboardPage.tsx (updated to show profile completeness)
├── components/
│   ├── profile/
│   │   ├── ProfileInfoForm.tsx (personal info section)
│   │   ├── CareerPreferencesForm.tsx (career preferences section)
│   │   ├── ProfileCompletenessIndicator.tsx (progress bar)
│   │   └── VisibilityToggle.tsx (public/private switch)
│   ├── job-experience/
│   │   ├── JobExperienceList.tsx (timeline cards)
│   │   ├── JobExperienceCard.tsx (single job card)
│   │   ├── JobExperienceFormDialog.tsx (create/edit dialog)
│   │   └── CurrentJobBadge.tsx (badge for current job)
│   └── education/
│       ├── EducationList.tsx (list items)
│       ├── EducationListItem.tsx (single education item)
│       └── EducationFormDialog.tsx (create/edit dialog)
├── api/
│   ├── profileApi.ts (profile CRUD methods)
│   ├── jobExperienceApi.ts (job CRUD methods)
│   └── educationApi.ts (education CRUD methods)
├── hooks/
│   ├── useProfile.ts (TanStack Query hook for profile)
│   ├── useJobExperiences.ts (TanStack Query hook for jobs)
│   └── useEducation.ts (TanStack Query hook for education)
└── types/
    ├── profile.ts (TypeScript interfaces)
    ├── jobExperience.ts
    └── education.ts
```

### API Client Implementation

**frontend/src/api/profileApi.ts**:

```typescript
import { apiClient } from './client'; // Axios instance with JWT interceptor

export interface CreateProfileRequest {
  fullName: string;
  headline: string;
  summary?: string;
  location?: string;
  languages?: string[];
  professionalLinks?: Record<string, string>;
  careerPreferences?: {
    targetRoles?: string[];
    domains?: string[];
    targetGeographies?: string[];
    remoteWilling?: boolean;
    relocationWilling?: boolean;
  };
  defaultVisibility?: 'public' | 'private';
}

export interface ProfileResponse {
  id: number;
  tenantId: number;
  userId: number;
  fullName: string;
  headline: string;
  summary?: string;
  location?: string;
  languages?: string[];
  professionalLinks?: Record<string, string>;
  careerPreferences?: any;
  defaultVisibility: 'public' | 'private';
  createdAt: string;
  updatedAt: string;
  version: number;
  jobs?: JobExperienceResponse[];
  education?: EducationResponse[];
}

export const profileApi = {
  getProfile: async (profileId: number): Promise<ProfileResponse> => {
    const response = await apiClient.get(`/api/v1/profiles/${profileId}`);
    return response.data;
  },

  createProfile: async (request: CreateProfileRequest): Promise<ProfileResponse> => {
    const response = await apiClient.post('/api/v1/profiles', request);
    return response.data;
  },

  updateProfile: async (profileId: number, request: CreateProfileRequest & { version: number }): Promise<ProfileResponse> => {
    const response = await apiClient.put(`/api/v1/profiles/${profileId}`, request);
    return response.data;
  },

  deleteProfile: async (profileId: number): Promise<void> => {
    await apiClient.delete(`/api/v1/profiles/${profileId}`);
  },
};
```

### React Hooks (TanStack Query)

**frontend/src/hooks/useProfile.ts**:

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { profileApi, CreateProfileRequest, ProfileResponse } from '../api/profileApi';

export const useProfile = (profileId: number) => {
  return useQuery<ProfileResponse>({
    queryKey: ['profile', profileId],
    queryFn: () => profileApi.getProfile(profileId),
  });
};

export const useCreateProfile = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateProfileRequest) => profileApi.createProfile(request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['profile', data.id] });
    },
  });
};

export const useUpdateProfile = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ profileId, request }: { profileId: number; request: CreateProfileRequest & { version: number } }) =>
      profileApi.updateProfile(profileId, request),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['profile', data.id] });
    },
  });
};
```

### UI Components

**ProfileEditorPage.tsx** (main page):

```tsx
import React, { useState } from 'react';
import { Tabs, Tab, Box, CircularProgress } from '@mui/material';
import { useProfile } from '../hooks/useProfile';
import ProfileInfoForm from '../components/profile/ProfileInfoForm';
import JobExperienceList from '../components/job-experience/JobExperienceList';
import EducationList from '../components/education/EducationList';
import ProfileCompletenessIndicator from '../components/profile/ProfileCompletenessIndicator';

export const ProfileEditorPage: React.FC = () => {
  const [currentTab, setCurrentTab] = useState(0);
  const { data: profile, isLoading, error } = useProfile(1); // TODO: Get from auth context

  if (isLoading) return <CircularProgress />;
  if (error) return <div>Error loading profile</div>;

  return (
    <Box sx={{ width: '100%', maxWidth: 1200, mx: 'auto', p: 3 }}>
      <ProfileCompletenessIndicator profile={profile} />

      <Tabs value={currentTab} onChange={(e, newValue) => setCurrentTab(newValue)}>
        <Tab label="Personal Info" />
        <Tab label="Work Experience" />
        <Tab label="Education" />
        <Tab label="Career Preferences" />
      </Tabs>

      <Box sx={{ mt: 3 }}>
        {currentTab === 0 && <ProfileInfoForm profile={profile} />}
        {currentTab === 1 && <JobExperienceList profileId={profile.id} jobs={profile.jobs} />}
        {currentTab === 2 && <EducationList profileId={profile.id} education={profile.education} />}
        {currentTab === 3 && <CareerPreferencesForm profile={profile} />}
      </Box>
    </Box>
  );
};
```

**JobExperienceCard.tsx** (timeline card):

```tsx
import React from 'react';
import { Card, CardContent, Typography, IconButton, Chip, Box } from '@mui/material';
import { Edit, Delete, Lock, LockOpen } from '@mui/icons-material';
import { JobExperienceResponse } from '../../types/jobExperience';

interface Props {
  job: JobExperienceResponse;
  onEdit: (job: JobExperienceResponse) => void;
  onDelete: (jobId: number) => void;
}

export const JobExperienceCard: React.FC<Props> = ({ job, onEdit, onDelete }) => {
  return (
    <Card sx={{ mb: 2, position: 'relative' }}>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
          <Box>
            <Typography variant="h6">{job.role}</Typography>
            <Typography variant="subtitle1" color="text.secondary">{job.company}</Typography>
            <Typography variant="body2" color="text.secondary">
              {job.startDate} - {job.isCurrent ? 'Present' : job.endDate}
            </Typography>
          </Box>
          <Box>
            <Chip
              icon={job.visibility === 'public' ? <LockOpen /> : <Lock />}
              label={job.visibility}
              size="small"
              color={job.visibility === 'public' ? 'success' : 'default'}
            />
            {job.isCurrent && <Chip label="Current" size="small" color="primary" sx={{ ml: 1 }} />}
          </Box>
        </Box>

        <Typography variant="body2" sx={{ mt: 2 }}>{job.achievements}</Typography>

        <Box sx={{ mt: 2 }}>
          <IconButton size="small" onClick={() => onEdit(job)}><Edit /></IconButton>
          <IconButton size="small" onClick={() => onDelete(job.id)}><Delete /></IconButton>
        </Box>
      </CardContent>
    </Card>
  );
};
```

---

## Database Schema

### Liquibase Migrations

**Migration 1: Profile Table** (`20260223120000-create-profile-table.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260223120000-1" author="claude-code">
        <createTable tableName="profile">
            <column name="id" type="BIGSERIAL" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="user_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="full_name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="headline" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="summary" type="TEXT"/>
            <column name="location" type="VARCHAR(255)"/>
            <column name="languages" type="JSONB"/>
            <column name="professional_links" type="JSONB"/>
            <column name="career_preferences" type="JSONB"/>
            <column name="default_visibility" type="VARCHAR(20)" defaultValue="private">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMPTZ">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMPTZ">
                <constraints nullable="false"/>
            </column>
            <column name="deleted_at" type="TIMESTAMPTZ"/>
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="profile"
            baseColumnNames="tenant_id"
            constraintName="fk_profile_tenant"
            referencedTableName="tenant"
            referencedColumnNames="id"/>

        <addForeignKeyConstraint
            baseTableName="profile"
            baseColumnNames="user_id"
            constraintName="fk_profile_user"
            referencedTableName="user"
            referencedColumnNames="id"/>

        <createIndex indexName="idx_profile_tenant_id" tableName="profile">
            <column name="tenant_id"/>
        </createIndex>

        <createIndex indexName="idx_profile_user_id" tableName="profile">
            <column name="user_id"/>
        </createIndex>

        <addUniqueConstraint
            tableName="profile"
            columnNames="tenant_id, user_id"
            constraintName="uq_profile_tenant_user"/>

        <rollback>
            <dropTable tableName="profile"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Migration 2: Job Experience Table** (`20260223120100-create-job-experience-table.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260223120100-1" author="claude-code">
        <createTable tableName="job_experience">
            <column name="id" type="BIGSERIAL" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="profile_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="company" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="role" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="start_date" type="DATE">
                <constraints nullable="false"/>
            </column>
            <column name="end_date" type="DATE"/>
            <column name="is_current" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="location" type="VARCHAR(255)"/>
            <column name="employment_type" type="VARCHAR(50)"/>
            <column name="responsibilities" type="TEXT"/>
            <column name="achievements" type="TEXT"/>
            <column name="metrics" type="JSONB"/>
            <column name="visibility" type="VARCHAR(20)" defaultValue="private">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMPTZ">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMPTZ">
                <constraints nullable="false"/>
            </column>
            <column name="deleted_at" type="TIMESTAMPTZ"/>
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <addForeignKeyConstraint
            baseTableName="job_experience"
            baseColumnNames="tenant_id"
            constraintName="fk_job_experience_tenant"
            referencedTableName="tenant"
            referencedColumnNames="id"/>

        <addForeignKeyConstraint
            baseTableName="job_experience"
            baseColumnNames="profile_id"
            constraintName="fk_job_experience_profile"
            referencedTableName="profile"
            referencedColumnNames="id"/>

        <createIndex indexName="idx_job_experience_tenant_id" tableName="job_experience">
            <column name="tenant_id"/>
        </createIndex>

        <createIndex indexName="idx_job_experience_profile_id" tableName="job_experience">
            <column name="profile_id"/>
        </createIndex>

        <createIndex indexName="idx_job_experience_deleted_at" tableName="job_experience">
            <column name="deleted_at"/>
        </createIndex>

        <sql>
            ALTER TABLE job_experience ADD CONSTRAINT chk_job_experience_dates
            CHECK (end_date IS NULL OR end_date >= start_date);
        </sql>

        <rollback>
            <dropTable tableName="job_experience"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

**Migration 3: Education Table** (`20260223120200-create-education-table.xml`):

Similar structure to job_experience, with degree/institution fields instead of company/role.

---

## Testing Strategy

### Unit Tests

**ProfileServiceTest.java**:

```java
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void createProfile_shouldSavePro fileWithTenantId() {
        // Arrange
        Long userId = 1L;
        Long tenantId = 10L;
        TenantContext.setTenantId(tenantId);

        CreateProfileRequest request = new CreateProfileRequest(
            "John Doe",
            "Senior Engineer",
            null, null, null, null, null, "private"
        );

        User user = new User();
        user.setId(userId);
        user.setTenantId(tenantId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(profileRepository.existsByUserIdAndDeletedAtIsNull(userId)).thenReturn(false);
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ProfileResponse response = profileService.createProfile(userId, request);

        // Assert
        assertThat(response.fullName()).isEqualTo("John Doe");
        verify(profileRepository).save(argThat(profile ->
            profile.getTenantId().equals(tenantId) &&
            profile.getUserId().equals(userId)
        ));

        TenantContext.clear();
    }

    @Test
    void createProfile_shouldThrowExceptionIfProfileExists() {
        // Arrange
        Long userId = 1L;
        TenantContext.setTenantId(10L);

        when(profileRepository.existsByUserIdAndDeletedAtIsNull(userId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> profileService.createProfile(userId, new CreateProfileRequest(...)))
            .isInstanceOf(DuplicateProfileException.class);

        TenantContext.clear();
    }
}
```

### Integration Tests

**ProfileControllerIntegrationTest.java**:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfileRepository profileRepository;

    @Test
    void createProfile_shouldReturn201WithValidRequest() throws Exception {
        String jwt = generateJwtForUser(1L, 10L); // userId=1, tenantId=10

        String requestBody = """
            {
              "fullName": "Jane Doe",
              "headline": "Tech Lead",
              "defaultVisibility": "private"
            }
            """;

        mockMvc.perform(post("/api/v1/profiles")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fullName").value("Jane Doe"))
            .andExpect(jsonPath("$.headline").value("Tech Lead"));

        // Verify database
        Profile saved = profileRepository.findByUserIdAndDeletedAtIsNull(1L).orElseThrow();
        assertThat(saved.getTenantId()).isEqualTo(10L);
        assertThat(saved.getFullName()).isEqualTo("Jane Doe");
    }

    @Test
    void createProfile_shouldReturn400ForInvalidRequest() throws Exception {
        String jwt = generateJwtForUser(1L, 10L);

        String requestBody = """
            {
              "fullName": "",
              "headline": "Tech Lead"
            }
            """;

        mockMvc.perform(post("/api/v1/profiles")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.field").value("fullName"))
            .andExpect(jsonPath("$.message").value("must not be blank"));
    }
}
```

### Frontend Tests (React Testing Library)

**ProfileEditorPage.test.tsx**:

```typescript
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProfileEditorPage } from './ProfileEditorPage';
import { profileApi } from '../api/profileApi';

jest.mock('../api/profileApi');

describe('ProfileEditorPage', () => {
  it('should display profile data when loaded', async () => {
    const mockProfile = {
      id: 1,
      fullName: 'John Doe',
      headline: 'Senior Engineer',
      jobs: [],
      education: [],
    };

    (profileApi.getProfile as jest.Mock).mockResolvedValue(mockProfile);

    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <ProfileEditorPage />
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('John Doe')).toBeInTheDocument();
      expect(screen.getByText('Senior Engineer')).toBeInTheDocument();
    });
  });
});
```

---

## Performance Considerations

**Database Indexing:**
- All `tenant_id` columns indexed for tenant filter performance
- `profile_id` indexed in job_experience and education tables for JOIN performance
- `deleted_at` indexed for soft delete filtering efficiency
- Composite unique index on (tenant_id, user_id) for profile uniqueness check

**Query Optimization:**
- Use `@Transactional(readOnly = true)` for GET endpoints (PostgreSQL query optimization)
- Fetch profile with jobs and education in single query using `@EntityGraph` or JOIN FETCH
- Pagination for large job/education lists (limit 100 per page)

**Caching Strategy:**
- TanStack Query caches profile data in frontend for 5 minutes
- Invalidate cache on mutations (create, update, delete)
- No backend caching in MVP (rely on database performance)

**Frontend Performance:**
- Lazy load profile editor tabs (code splitting)
- Debounce inline validation (300ms)
- Use React.memo for JobExperienceCard to prevent unnecessary re-renders

---

## Security & Privacy

**Authentication & Authorization:**
- All endpoints require JWT authentication (`@PreAuthorize("hasRole('USER')")`)
- Tenant ID extracted from JWT claims, stored in ThreadLocal (TenantContext)
- Hibernate filter automatically adds `WHERE tenant_id = :tenantId` to all queries

**Public/Private Visibility:**
- Service layer filters by visibility flag when returning public data
- Private data NEVER exposed in public API responses
- Future: Public profile endpoint will only return data with `visibility = 'public'`

**Input Validation:**
- Spring Validation annotations on DTOs (`@NotBlank`, `@Size`, `@Email`, `@Past`)
- Custom validators for date logic (end_date >= start_date)
- SQL injection prevented by JPA parameterized queries

**XSS Prevention:**
- Text fields sanitized on input (Spring Security XSS filters)
- Frontend uses React's built-in XSS protection (automatic escaping)

---

## Error Handling

**Exception Hierarchy:**

```
RuntimeException
├── ProfileNotFoundException (404 Not Found)
├── DuplicateProfileException (400 Bad Request)
├── OptimisticLockException (409 Conflict)
└── ValidationException (400 Bad Request)
```

**Global Exception Handler** (`@ControllerAdvice`):

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            null
        ));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse(
            "CONFLICT",
            "Resource was modified by another session. Please refresh and try again.",
            null
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
            .toList();
        return ResponseEntity.status(400).body(new ErrorResponse(
            "VALIDATION_ERROR",
            "Invalid request data",
            errors
        ));
    }
}
```

---

## Deployment Checklist

- [ ] All Liquibase migrations added to master changelog
- [ ] Database indexes created (tenant_id, profile_id, deleted_at)
- [ ] Environment variables documented (if any new ones added)
- [ ] Swagger/OpenAPI spec updated with new endpoints
- [ ] Integration tests pass (100% success rate)
- [ ] Frontend E2E tests pass (Cypress or Playwright)
- [ ] Performance benchmarks met (p95 < 200ms for profile GET)
- [ ] Security audit passed (no tenant isolation violations)

---

## Revision History

| Version | Date       | Changes                                         | Author      |
|---------|------------|-------------------------------------------------|-------------|
| 1.0.0   | 2026-02-23 | Initial technical design                        | Claude Code |
