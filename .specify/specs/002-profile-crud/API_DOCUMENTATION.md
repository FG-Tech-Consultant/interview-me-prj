# Profile CRUD API Documentation

## Overview

The Profile CRUD API provides endpoints for managing user career profiles, including personal information, job experiences, and education history. The API implements multi-tenant architecture with soft delete and optimistic locking for data consistency.

## Base URL

```
http://localhost:8080/api
```

## Authentication

All endpoints require JWT authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

---

## Profile Endpoints

### 1. Get Current User's Profile

**GET** `/profiles/me`

Returns the profile for the currently authenticated user.

**Response:**
```json
{
  "id": 1,
  "userId": 100,
  "fullName": "John Doe",
  "professionalTitle": "Senior Software Engineer",
  "bio": "Experienced software engineer with 10+ years...",
  "email": "john@example.com",
  "phone": "+1234567890",
  "location": "San Francisco, CA",
  "website": "https://johndoe.com",
  "linkedinUrl": "https://linkedin.com/in/johndoe",
  "githubUrl": "https://github.com/johndoe",
  "yearsOfExperience": 10,
  "currentJobTitle": "Staff Engineer",
  "currentCompany": "Tech Corp",
  "languages": ["English", "Spanish"],
  "professionalLinks": {},
  "careerPreferences": {},
  "version": 1,
  "createdAt": "2026-02-23T12:00:00Z",
  "updatedAt": "2026-02-23T12:00:00Z",
  "jobExperiences": [...],
  "educations": [...]
}
```

**Status Codes:**
- `200 OK` - Profile found
- `404 Not Found` - Profile not found for user
- `401 Unauthorized` - Invalid/missing token

---

### 2. Get Profile by ID

**GET** `/profiles/{profileId}`

Returns a specific profile by ID (within tenant scope).

**Parameters:**
- `profileId` (path) - Profile ID

**Response:** Same as Get Current User's Profile

**Status Codes:**
- `200 OK` - Profile found
- `404 Not Found` - Profile not found
- `401 Unauthorized` - Invalid/missing token

---

### 3. Create Profile

**POST** `/profiles`

Creates a new profile for the authenticated user.

**Request Body:**
```json
{
  "fullName": "John Doe",
  "professionalTitle": "Senior Software Engineer",
  "bio": "Experienced software engineer...",
  "email": "john@example.com",
  "phone": "+1234567890",
  "location": "San Francisco, CA",
  "website": "https://johndoe.com",
  "linkedinUrl": "https://linkedin.com/in/johndoe",
  "githubUrl": "https://github.com/johndoe",
  "yearsOfExperience": 10,
  "currentJobTitle": "Staff Engineer",
  "currentCompany": "Tech Corp",
  "languages": ["English", "Spanish"],
  "professionalLinks": {},
  "careerPreferences": {}
}
```

**Required Fields:**
- `fullName` - User's full name (max 255 chars)

**Response:** Same as Get Current User's Profile

**Status Codes:**
- `201 Created` - Profile created successfully
- `400 Bad Request` - Validation error
- `409 Conflict` - Profile already exists for user
- `401 Unauthorized` - Invalid/missing token

---

### 4. Update Profile

**PUT** `/profiles/{profileId}`

Updates an existing profile.

**Parameters:**
- `profileId` (path) - Profile ID

**Request Body:**
```json
{
  "fullName": "John Doe Jr.",
  "professionalTitle": "Principal Engineer",
  "bio": "Updated bio...",
  "email": "john.updated@example.com",
  "phone": "+1234567890",
  "location": "New York, NY",
  "website": "https://johndoe.com",
  "linkedinUrl": "https://linkedin.com/in/johndoe",
  "githubUrl": "https://github.com/johndoe",
  "yearsOfExperience": 12,
  "currentJobTitle": "Principal Engineer",
  "currentCompany": "Tech Corp",
  "languages": ["English", "Spanish", "French"],
  "professionalLinks": {},
  "careerPreferences": {},
  "version": 1
}
```

**Required Fields:**
- `version` - Current version for optimistic locking

**Response:** Same as Get Current User's Profile

**Status Codes:**
- `200 OK` - Profile updated successfully
- `400 Bad Request` - Validation error
- `404 Not Found` - Profile not found
- `409 Conflict` - Optimistic lock failure (profile was modified)
- `401 Unauthorized` - Invalid/missing token

---

### 5. Delete Profile

**DELETE** `/profiles/{profileId}`

Soft deletes a profile.

**Parameters:**
- `profileId` (path) - Profile ID

**Response:** No content

**Status Codes:**
- `204 No Content` - Profile deleted successfully
- `404 Not Found` - Profile not found
- `401 Unauthorized` - Invalid/missing token

---

### 6. Check Profile Exists

**GET** `/profiles/exists`

Checks if the current user has a profile.

**Response:**
```json
true
```

**Status Codes:**
- `200 OK` - Returns boolean
- `401 Unauthorized` - Invalid/missing token

---

## Job Experience Endpoints

### 1. Get Job Experiences

**GET** `/profiles/{profileId}/job-experiences`

Returns all job experiences for a profile.

**Parameters:**
- `profileId` (path) - Profile ID

**Response:**
```json
[
  {
    "id": 1,
    "companyName": "Tech Corp",
    "jobTitle": "Senior Engineer",
    "location": "San Francisco, CA",
    "startDate": "2020-01-15",
    "endDate": "2023-06-30",
    "currentlyWorking": false,
    "description": "Led development of...",
    "achievements": [
      "Increased performance by 40%",
      "Led team of 5 developers"
    ],
    "technologies": ["Java", "Spring Boot", "PostgreSQL"],
    "metrics": {},
    "version": 1,
    "createdAt": "2026-02-23T12:00:00Z",
    "updatedAt": "2026-02-23T12:00:00Z"
  }
]
```

**Status Codes:**
- `200 OK` - Success
- `404 Not Found` - Profile not found
- `401 Unauthorized` - Invalid/missing token

---

### 2. Get Job Experience by ID

**GET** `/profiles/{profileId}/job-experiences/{experienceId}`

Returns a specific job experience.

**Parameters:**
- `profileId` (path) - Profile ID
- `experienceId` (path) - Job experience ID

**Response:** Single job experience object

**Status Codes:**
- `200 OK` - Success
- `404 Not Found` - Profile or experience not found
- `401 Unauthorized` - Invalid/missing token

---

### 3. Create Job Experience

**POST** `/profiles/{profileId}/job-experiences`

Creates a new job experience.

**Parameters:**
- `profileId` (path) - Profile ID

**Request Body:**
```json
{
  "companyName": "Tech Corp",
  "jobTitle": "Senior Engineer",
  "location": "San Francisco, CA",
  "startDate": "2020-01-15",
  "endDate": "2023-06-30",
  "currentlyWorking": false,
  "description": "Led development of microservices...",
  "achievements": [
    "Increased system performance by 40%"
  ],
  "technologies": ["Java", "Spring Boot", "PostgreSQL"],
  "metrics": {}
}
```

**Required Fields:**
- `companyName` - Company name (max 255 chars)
- `jobTitle` - Job title (max 255 chars)
- `startDate` - Start date (YYYY-MM-DD)
- `currentlyWorking` - Boolean
- `endDate` - Required if currentlyWorking is false

**Validation:**
- End date cannot be before start date

**Response:** Created job experience object

**Status Codes:**
- `201 Created` - Experience created successfully
- `400 Bad Request` - Validation error
- `404 Not Found` - Profile not found
- `401 Unauthorized` - Invalid/missing token

---

### 4. Update Job Experience

**PUT** `/profiles/{profileId}/job-experiences/{experienceId}`

Updates an existing job experience.

**Parameters:**
- `profileId` (path) - Profile ID
- `experienceId` (path) - Job experience ID

**Request Body:**
```json
{
  "companyName": "Tech Corp",
  "jobTitle": "Staff Engineer",
  "location": "San Francisco, CA",
  "startDate": "2020-01-15",
  "endDate": null,
  "currentlyWorking": true,
  "description": "Leading platform architecture...",
  "achievements": [
    "Designed scalable microservices architecture"
  ],
  "technologies": ["Java", "Spring Boot", "Kubernetes"],
  "metrics": {},
  "version": 1
}
```

**Required Fields:**
- `version` - Current version for optimistic locking

**Response:** Updated job experience object

**Status Codes:**
- `200 OK` - Experience updated successfully
- `400 Bad Request` - Validation error
- `404 Not Found` - Profile or experience not found
- `409 Conflict` - Optimistic lock failure
- `401 Unauthorized` - Invalid/missing token

---

### 5. Delete Job Experience

**DELETE** `/profiles/{profileId}/job-experiences/{experienceId}`

Soft deletes a job experience.

**Parameters:**
- `profileId` (path) - Profile ID
- `experienceId` (path) - Job experience ID

**Response:** No content

**Status Codes:**
- `204 No Content` - Experience deleted successfully
- `404 Not Found` - Profile or experience not found
- `401 Unauthorized` - Invalid/missing token

---

## Education Endpoints

### 1. Get Educations

**GET** `/profiles/{profileId}/education`

Returns all education records for a profile.

**Parameters:**
- `profileId` (path) - Profile ID

**Response:**
```json
[
  {
    "id": 1,
    "institutionName": "Stanford University",
    "degree": "Bachelor of Science",
    "fieldOfStudy": "Computer Science",
    "location": "Stanford, CA",
    "startDate": "2010-09-01",
    "endDate": "2014-06-15",
    "currentlyStudying": false,
    "grade": "3.8/4.0",
    "description": "Focus on distributed systems...",
    "achievements": [
      "Dean's List all semesters",
      "President of CS Club"
    ],
    "version": 1,
    "createdAt": "2026-02-23T12:00:00Z",
    "updatedAt": "2026-02-23T12:00:00Z"
  }
]
```

**Status Codes:**
- `200 OK` - Success
- `404 Not Found` - Profile not found
- `401 Unauthorized` - Invalid/missing token

---

### 2. Get Education by ID

**GET** `/profiles/{profileId}/education/{educationId}`

Returns a specific education record.

**Parameters:**
- `profileId` (path) - Profile ID
- `educationId` (path) - Education ID

**Response:** Single education object

**Status Codes:**
- `200 OK` - Success
- `404 Not Found` - Profile or education not found
- `401 Unauthorized` - Invalid/missing token

---

### 3. Create Education

**POST** `/profiles/{profileId}/education`

Creates a new education record.

**Parameters:**
- `profileId` (path) - Profile ID

**Request Body:**
```json
{
  "institutionName": "Stanford University",
  "degree": "Bachelor of Science",
  "fieldOfStudy": "Computer Science",
  "location": "Stanford, CA",
  "startDate": "2010-09-01",
  "endDate": "2014-06-15",
  "currentlyStudying": false,
  "grade": "3.8/4.0",
  "description": "Focus on distributed systems",
  "achievements": [
    "Dean's List all semesters"
  ]
}
```

**Required Fields:**
- `institutionName` - Institution name (max 255 chars)
- `degree` - Degree name (max 255 chars)
- `startDate` - Start date (YYYY-MM-DD)
- `currentlyStudying` - Boolean
- `endDate` - Required if currentlyStudying is false

**Validation:**
- End date cannot be before start date

**Response:** Created education object

**Status Codes:**
- `201 Created` - Education created successfully
- `400 Bad Request` - Validation error
- `404 Not Found` - Profile not found
- `401 Unauthorized` - Invalid/missing token

---

### 4. Update Education

**PUT** `/profiles/{profileId}/education/{educationId}`

Updates an existing education record.

**Parameters:**
- `profileId` (path) - Profile ID
- `educationId` (path) - Education ID

**Request Body:**
```json
{
  "institutionName": "Stanford University",
  "degree": "Master of Science",
  "fieldOfStudy": "Computer Science",
  "location": "Stanford, CA",
  "startDate": "2014-09-01",
  "endDate": null,
  "currentlyStudying": true,
  "grade": "4.0/4.0",
  "description": "Research in machine learning",
  "achievements": [
    "Published 2 research papers"
  ],
  "version": 1
}
```

**Required Fields:**
- `version` - Current version for optimistic locking

**Response:** Updated education object

**Status Codes:**
- `200 OK` - Education updated successfully
- `400 Bad Request` - Validation error
- `404 Not Found` - Profile or education not found
- `409 Conflict` - Optimistic lock failure
- `401 Unauthorized` - Invalid/missing token

---

### 5. Delete Education

**DELETE** `/profiles/{profileId}/education/{educationId}`

Soft deletes an education record.

**Parameters:**
- `profileId` (path) - Profile ID
- `educationId` (path) - Education ID

**Response:** No content

**Status Codes:**
- `204 No Content` - Education deleted successfully
- `404 Not Found` - Profile or education not found
- `401 Unauthorized` - Invalid/missing token

---

## Error Response Format

All error responses follow this format:

```json
{
  "timestamp": "2026-02-23T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "fullName": "Full name is required"
  }
}
```

## Multi-Tenant Architecture

- All data is isolated by `tenantId`
- Tenant context is automatically extracted from JWT token
- Hibernate filters ensure automatic tenant filtering on all queries
- Cross-tenant data access is prevented at the database level

## Optimistic Locking

- All update operations require the current `version` field
- If the version doesn't match (entity was modified by another user), a 409 Conflict error is returned
- Client should refresh data and retry the update with the new version

## Soft Delete

- Delete operations set `deletedAt` timestamp instead of physically removing data
- Deleted entities are automatically filtered from queries
- Soft-deleted data can be restored if needed (requires separate admin endpoint)
