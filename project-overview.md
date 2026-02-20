***

# Project: Live Resume & Career Copilot (React + Spring Boot)

> **"Interview Me"** – A personal career platform combining live resume, skills graph, AI copilot, and SaaS monetization for developers.

> **📋 Note:** This is the working PRD document with external references. The canonical version used by AI agents is at `.specify/memory/project-overview.md` (v2.0.0)

***

## Table of Contents

- [1. Business Overview (PRD)](#1-business-overview-prd)
   - [1.1 Goals and Success Criteria](#11-goals-and-success-criteria)
   - [1.2 Personas](#12-personas)
   - [1.3 Functional Features](#13-functional-features)
   - [1.4 Data Model (High Level)](#14-data-model-high-level)
   - [1.5 Non-Functional Requirements](#15-non-functional-requirements)
   - [1.6 Security, Privacy, and Compliance](#16-security-privacy-and-compliance)
   - [1.7 Pricing and Tiering Model](#17-pricing-and-tiering-model)
- [2. Technical Breakdown](#2-technical-breakdown)
   - [2.1 High-Level Architecture](#21-high-level-architecture)
   - [2.2 Backend Structure (Spring Boot)](#22-backend-structure-spring-boot)
   - [2.3 Frontend Structure (React)](#23-frontend-structure-react)
   - [2.4 Cross-Cutting Concerns](#24-cross-cutting-concerns)

***

## 1. Business Overview (PRD)

### 1.1 Goals and Success Criteria

**Goal:** Build a personal career platform that acts as:

- A **live resume / portfolio site** that is always up to date.
- A **structured skills and stories graph** to answer recruiter questions with concrete examples (scale, traffic, incidents, leadership, etc.). [drive.google](https://drive.google.com/file/d/1SMGlFhVWdntlg2lN2KNFF2F8CEtMoxJF/view?usp=drivesdk)
- An **AI copilot** for:
   - Recruiter chat on the profile site.
   - LinkedIn inbox reply drafting.
   - LinkedIn profile "score" analysis and optimization suggestions.

**Success Criteria (v1 - Personal Use):**

- All current resume and background presentation content can be represented without loss of important detail. [drive.google](https://drive.google.com/file/d/1O11IjacKnY9088AXFNIi_LQBtbP2NeMA/view?usp=drivesdk)
- Generate at least:
   - 1–2 resume formats
   - 1 background presentation
   - 1 cover letter tailored to specific role/location
- Recruiter chat correctly answers questions like:
   - "Do you have experience with X?"
   - "Tell me about a time you handled high scale / big traffic" [drive.google](https://drive.google.com/file/d/1SMGlFhVWdntlg2lN2KNFF2F8CEtMoxJF/view?usp=drivesdk)
- LinkedIn Score Analyzer can:
   - Ingest LinkedIn (URL or PDF)
   - Assign numeric scores per section (Headline, About, Experience, Education, Skills)
   - Provide at least one concrete improvement suggestion per section [redactai](https://redactai.io/free-tools/linkedin-profile-review)

**Success Criteria (v2 - Product/SaaS):**

- Additional dev users can onboard, build profiles, and pay with coins for exports and AI usage
- Coin model is sustainable: free tier costs do not exceed paid usage [getmonetizely](https://www.getmonetizely.com/articles/whats-the-optimal-free-tier-limit-for-developer-focused-saas-products)

***

### 1.2 Personas

#### 1. Owner Candidate (Primary: You, Later Other Devs)
- Senior engineer / tech leader, often in fintech, with long experience and many projects [drive.google](https://drive.google.com/file/d/1O11IjacKnY9088AXFNIi_LQBtbP2NeMA/view?usp=drivesdk)
- Wants a single place to maintain profile, skills, and case studies
- Wants to automate recruiter interactions

#### 2. Recruiter / Hiring Manager
- Needs fast, trustworthy, self-service access to skills, tech stack, scale, and leadership examples
- Wants to ask questions interactively instead of reading long PDFs

#### 3. Platform Admin / Product Owner
- Manages users, coins, usage limits, and LLM providers
- Ensures security, compliance with LinkedIn policies, and cost control [blog.reachy](https://blog.reachy.ai/article/linkedin-automation-is-it-a-violation-of-the-terms-of-service)

***

### 1.3 Functional Features

#### 3.1 Account, Auth, and Tenancy

- **Authentication:**
   - Email/password auth, password reset
   - Optional future: SSO (GitHub, Google)

- **Multi-tenancy:**
   - **Soft multi-tenancy from day one:**
      - Single database
      - All tenant-owned entities include `tenant_id`
      - Always filtered by `tenant_id` in backend [workos](https://workos.com/blog/tenant-isolation-in-multi-tenant-systems)
   - User ↔ tenant relation to support multiple users per tenant in the future

***

#### 3.2 Profile and Timeline

**Profile:**
- Name, headline, short summary, location, relocation/remote preference, languages, links (LinkedIn, GitHub, personal site) [drive.google](https://drive.google.com/file/d/1SMGlFhVWdntlg2lN2KNFF2F8CEtMoxJF/view?usp=drivesdk)
- Career preferences: target roles (e.g., Tech Lead, Architect), domains (Fintech, Payments, Marketplaces), target geos

**Timeline:**
- **Jobs:**
   - Company, role, dates, location, employment type, responsibilities, achievements
   - Metrics (e.g., TPS, volume, user counts) [drive.google](https://drive.google.com/file/d/1O11IjacKnY9088AXFNIi_LQBtbP2NeMA/view?usp=drivesdk)
   - Link to multiple projects/stories
- **Education:**
   - Degree, institution, dates, notes, certifications

**Visibility Flags:**
- Each field/section can be marked **public** or **private**
- **Public:** visible on live resume and usable by recruiter chat
- **Private:** visible only to owner and internal tools; recruiter chat must ignore [suridata](https://www.suridata.ai/blog/guide-to-role-based-access-control-in-saas-applications/)

***

#### 3.3 Skills, Tags, and Stories

**Skills & Tags:**
- Skills catalog with categories: Languages, Frameworks, Cloud, Databases, Messaging, Observability, Methodologies (Scrum, Kanban), Domains (Fintech, Payments, Marketplaces) [drive.google](https://drive.google.com/file/d/1SMGlFhVWdntlg2lN2KNFF2F8CEtMoxJF/view?usp=drivesdk)
- Each `UserSkill`:
   - Years of experience
   - Depth (1–5 scale)
   - Last used date
   - Confidence level
   - #tags (e.g., `#java`, `#spring-boot`, `#kubernetes`, `#high-scale`)

**Experience Projects:**
- "Project/Initiative" entity under a job:
   - Title, context, role, team size, tech stack
   - Architecture type (monolith/microservices)
   - Metrics (scale, SLAs), outcomes [drive.google](https://drive.google.com/file/d/1O11IjacKnY9088AXFNIi_LQBtbP2NeMA/view?usp=drivesdk)
   - Linked skills and domains

**Stories (Case Studies):**
- Structured narrative per project using **STAR format** (Situation, Task, Action, Result)
- Each story links to:
   - One `ExperienceProject`
   - One or more `UserSkills`
   - Optional metrics fields for impact

**Privacy for Stories:**
- Story visibility: **public/private**
- **Public stories:** recruiter chat and public pages can use
- **Private stories:** only visible to owner and internal AI assistants, never exposed or used in recruiter sessions

***

#### 3.4 Packages and Shareable Links

**Packages:**
- Owner can define named packages (e.g., "Fintech-Lead-EU", "High-Scale-Backend")
- Each package includes a subset of skills, experiences, and stories (all must be public or package-public)
- Generated **tokenized link** per package (e.g., `/p/{slug}?t={token}`) granting read-only access [help.cloudspot](https://help.cloudspot.io/en/articles/8993037-share-galleries-through-a-private-portfolio-page)

**Recruiter Package View:**
- Page showing:
   - Short intro, key skills, selected stories/projects
   - Chatbox constrained to that package's context only

***

#### 3.5 Exports (Resume, Deck, Cover Letter)

**Resume Generator:**
- **Templates:**
   - Standard 1–2 page resume
   - Tech-lead/architect focused resume
   - Fintech/payments-oriented resume [drive.google](https://drive.google.com/file/d/1SMGlFhVWdntlg2lN2KNFF2F8CEtMoxJF/view?usp=drivesdk)
- **Parameters:** Target role, location, seniority, optional language
- **Output:** PDF (and optionally DOCX) generated server-side via templates

**Background Presentation Export:**
- Generate a deck similar to existing background presentation:
   - Summary slide, skills slide, professional timeline, case studies with metrics, recommendations excerpt [drive.google](https://drive.google.com/file/d/1O11IjacKnY9088AXFNIi_LQBtbP2NeMA/view?usp=drivesdk)
- **Output:** PDF

**Cover Letter Generator:**
- **Input:** Target company, role, job description snippet, market (e.g., US, EU, Canada)
- **Output:** Tailored letter grounded in profile, skills, and stories

**Coins:**
- **All exports always cost coins**

***

#### 3.6 Recruiter Chat on the Live Profile

**Public Profile Page:**
- **Always-free** live webpage acting as primary online resume and portfolio
- Contains public profile info, public skills, public stories, and possibly a default package

**Recruiter Chatbox:**
- Embedded chat component for recruiters to ask questions like:
   - "Tell me about your biggest high-traffic payments project"
   - "What is your experience with Java 21 and Kubernetes in production?" [drive.google](https://drive.google.com/file/d/1SMGlFhVWdntlg2lN2KNFF2F8CEtMoxJF/view?usp=drivesdk)

**Backend RAG Flow:**
- For each question, retrieve relevant public stories/experiences/skills
- Call the configured LLM with retrieved context + question

**Data Constraints:**
- Chat uses **only public content** and/or content in the specific package accessed
- Private content is **never included** in recruiter sessions

**Coins:**
- Recruiter chat uses **free tier quotas** (limited messages per month)
- Extra usage consumes coins

***

#### 3.7 LinkedIn Inbox Assistant (Draft-Only)

**Scope and Constraints:**
- Align with LinkedIn ToS by avoiding auto-sending or aggressive automation
- **Only generate drafts** to be sent manually by user [blog.closelyhq](https://blog.closelyhq.com/is-linkedin-automation-legal-understanding-platform-policies/)

**Integration:**
- **Phase 1 (MVP):** User manually pastes messages
- **Phase 2 (Future):** Optional API or extension-based integration (subject to LinkedIn policy)

**Features:**
- Categorize incoming messages (recruiter, agency, founder, spam, etc.)
- Suggest concise replies following pre-configured tone and rules (e.g., relocation, salary range, role fit)
- Allow quick insert of pre-defined stories or links (e.g., to specific packages)

**Guardrails:**
- **Draft-only:** UI clearly indicates "Copy reply" or "Insert into LinkedIn," no automated send
- **Free tier:** Limited number of AI-generated drafts per month; more drafts consume coins

***

#### 3.8 LinkedIn Profile Score Analyzer

**Purpose:**
- Provide an AI-powered review of LinkedIn profile, producing:
   - Overall score
   - Section-by-section scores (Headline, About, Experience, Education, Skills, etc.)
   - Limited improvement suggestions [jobwinner](https://jobwinner.ai/free-linkedin-profile-review/)

**Input Options:**
- **Option 1 (Preferred for ToS safety):** User exports LinkedIn as PDF and uploads
- **Option 2 (Future):** User enters public LinkedIn URL, service fetches content within policy limits [2pr](https://2pr.io/linkedinreview)

**Output:**
- Overall profile score (0–100)
- Per-section scores (0–100) for:
   - Photo/Banner (optional), Headline, About, Experience, Education, Skills, Recommendations, Other sections
- For each section:
   - Short explanation of current quality (e.g., "About is clear but lacks metrics")
   - **Suggestions list** with small, concrete actions

**Free vs Coins:**
- **Full analysis (scores + brief comments) is free**
- For each section:
   - **1 free suggestion** item (e.g., one concrete rewrite idea or structured bullet list)
   - **Additional suggestions** for the same section require coins (e.g., generate multiple alternative headlines or new "About" drafts)

**Integration with Profile:**
- User can choose to "Apply suggestion" or "Import suggestion" to update fields in this platform's profile (not LinkedIn directly)
- Optionally store last analysis snapshot for tracking improvements over time

***

#### 3.9 Coins, Plans, and Limits

**Coin Wallet:**
- Each user has a coin balance and transaction history (earn, spend, refund)
- Coins used for:
   - Additional recruiter chat usage beyond free tier
   - All exports (resume, deck, cover letters)
   - Extra LinkedIn inbox drafts beyond free tier
   - Extra LinkedIn Score Analyzer suggestions beyond the first free suggestion per section [maxio](https://www.maxio.com/blog/freemium-model)

**Free Tier (Always-On):**
- Live public resume page
- Limited recruiter chat per month
- Limited LinkedIn inbox drafts per month
- Unrestricted LinkedIn profile analysis (scores) but only 1 free suggestion per section

**Paid Usage:**
- Coins purchased in packs (future Stripe integration)
- Later: monthly "Pro" tier with larger free quotas and/or auto-refilling coins

***

### 1.4 Data Model (High Level)

**Key Entities:**

| Entity | Purpose |
|--------|---------|
| `Tenant` | Logical tenant |
| `User` | Auth user, belongs to a tenant |
| `Profile` | Personal info, preferences, and links |
| `JobExperience` | Employment history entries |
| `Education` | Education entries |
| `Skill` | Canonical skill definition (name, category, description, tags) |
| `UserSkill` | Relation between user and skill (years, depth, last used, confidence) |
| `ExperienceProject` | Project/initiative under a job |
| `Story` | Structured case study, links to project and skills, has visibility |
| `Package` | Curated set of skills, projects, stories with share token |
| `ExportTemplate` | Metadata for resume/cover/deck templates |
| `ExportHistory` | Records of generated exports (type, params, file location, coins spent) |
| `CoinWallet` & `CoinTransaction` | Balances and movements |
| `ChatSession` & `ChatMessage` | Recruiter chat logs (with tenant and visibility constraints) |
| `LinkedInDraft` | Generated drafts for inbox replies |
| `LinkedInAnalysis` | LinkedIn score analyzer result, including per-section scores and suggestions |
| `LLMProviderConfig` | Per-tenant configuration of Gemini / ChatGPT / Claude etc. |

***

### 1.5 Non-Functional Requirements

**Tech Stack:**
- **Frontend:** React + TypeScript, component library (e.g., MUI/Chakra), React Query or equivalent
- **Backend:** Spring Boot, Java 17+ (or 21), REST APIs, JWT auth, multi-tenant filters
- **Database:** Relational (Postgres/SQL Server) with optional pgvector or similar for RAG embeddings

**Performance:**
- Standard SaaS baseline: p95 API latency under ~300–500 ms for core CRUD and chat orchestration (excluding LLM call latency)
- Asynchronous workflows (e.g., heavy exports, bulk LinkedIn analysis) via background jobs/queues

**Scalability & Isolation:**
- Horizontal-scalable stateless backend services
- Tenant isolation at application level using tenant filters and foreign keys
- In future, option to migrate high-value tenants to schema/db-per-tenant models [learn.microsoft](https://learn.microsoft.com/en-us/azure/architecture/guide/multitenant/considerations/tenancy-models)

**Observability:**
- Structured logging, metrics for:
   - LLM calls (count, latency, cost proxy)
   - Export generation
   - Coin spends and free tier usage

***

### 1.6 Security, Privacy, and Compliance

**AuthN & AuthZ:**
- JWT-based authentication
- RBAC: roles like `OWNER`, `ADMIN`, `RECRUITER_VIEW` (tokenized read-only) respecting least-privilege [spendflo](https://www.spendflo.com/blog/saas-access-control)

**Data Protection:**
- Secrets and API keys encrypted at rest
- Clear distinction between public and private content in schema and queries
- Tokenized links for packages use expiring or revocable tokens

**LinkedIn Compliance:**
- **No auto-sending messages**
- Prefer PDF upload for profile analysis to avoid scraping
- If URL-based fetching is added, respect rate limits and ToS; clearly mark as "user-initiated analysis" [trykondo](https://www.trykondo.com/blog/is-expandi-safe-navigating-linkedin-s-terms-of-service)

***

### 1.7 Pricing and Tiering Model (Conceptual)

**Free Tier (Always):**
- Live online resume/profile
- Limited recruiter chat messages/month
- Limited LinkedIn inbox drafts/month
- Unlimited LinkedIn profile scoring, but only 1 free suggestion per section of profile

**Coins:**
- All exports
- Extra recruiter chat above free limit
- Extra LinkedIn inbox drafts above free limit
- Extra LinkedIn profile suggestions per section beyond first
- **Future:** "Pro" subscription bundling coins and higher limits [softwarepricing](https://softwarepricing.com/blog/freemium-saas/)

***

## 2. Technical Breakdown

### 2.1 High-Level Architecture

- **Frontend:** React + TypeScript SPA, running on its own origin in dev (e.g., `localhost:3000`), talking to backend via REST APIs under `/api/*` [dhiwise](https://www.dhiwise.com/post/a-step-by-step-guide-to-implementing-react-spring-boot)
- **Backend:** Spring Boot app exposing REST endpoints, following a **layered architecture** (Controller → Service → Repository) [learncodewithdurgesh](https://learncodewithdurgesh.com/tutorials/spring-boot-tutorials/spring-boot-layered-architecture)
- **AI Integration:** All LLM logic lives in the backend ("LLM in backend" pattern: prompts, provider routing, RAG, and rate limits are server-side) [monarchwadia](https://www.monarchwadia.com/pages/LlmInBackend.html)
- **Database:** Relational DB (Postgres is a good default) with tenant-aware schemas; optional pgvector for embeddings later [docs.aws.amazon](https://docs.aws.amazon.com/whitepapers/latest/saas-architecture-fundamentals/tenant-isolation.html)
- **Auth:** JWT-based auth with roles (`OWNER`, `ADMIN`, `RECRUITER_VIEW_TOKEN`)
- **Deployment:** Initially one backend service + one frontend app; later can split modules or add workers for async tasks (exports, heavy LinkedIn analysis)

***

### 2.2 Backend Structure (Spring Boot)

#### 2.2.1 Packages / Modules

Use a modular package structure inside a single Spring Boot app:

```
com.yourapp.config       – security config, tenant filter, CORS, LLM provider config
com.yourapp.auth         – user registration, login, JWT issuing/validation
com.yourapp.tenancy      – Tenant entity, tenant resolver (from JWT), multi-tenant filter
com.yourapp.profile      – profile, jobs, education
com.yourapp.skills       – skills catalog, user skills, tagging
com.yourapp.experience   – projects and stories
com.yourapp.packages     – packages and tokenized share links
com.yourapp.exports      – resume/deck/cover letter generation
com.yourapp.ai           – LLM client abstraction, prompts, RAG pipeline
com.yourapp.chat         – recruiter chat sessions/messages
com.yourapp.linkedin     – inbox assistant + profile analyzer
com.yourapp.billing      – coins, transactions, usage limits
```

**Within each domain package, follow layered architecture:** [geeksforgeeks](https://www.geeksforgeeks.org/springboot/spring-boot-architecture/)
- `controller` (REST endpoints)
- `service` (business logic)
- `repository` (Spring Data JPA repositories)
- `model` or `entity` (JPA entities)
- `dto` + mappers (if you prefer DTOs over exposing entities)

***

#### 2.2.2 Key Entities (Simplified)

```java
Tenant(id, name, createdAt, ...)
User(id, tenantId, email, passwordHash, roles, ...)
Profile(id, tenantId, userId, headline, summary, location, visibility flags, ...)
JobExperience(id, tenantId, profileId, company, role, startDate, endDate, description, metricsJson, visibility)
Education(id, tenantId, profileId, institution, degree, startDate, endDate, description, visibility)
Skill(id, name, category, description, tags)
UserSkill(id, tenantId, userId, skillId, years, depth, lastUsed, confidence)
ExperienceProject(id, tenantId, jobId, title, context, role, teamSize, techStackJson, architectureType, metricsJson, visibility)
Story(id, tenantId, projectId, title, situation, task, action, result, visibility, linkedSkills)
Package(id, tenantId, name, description, visibility, createdAt)
PackageItem(id, packageId, type, refId) // e.g., story, project, skill
PackageShareToken(id, packageId, token, expiresAt, revoked)
CoinWallet(id, tenantId, balance)
CoinTransaction(id, walletId, type, amount, refType, refId, createdAt)
ExportTemplate(id, tenantId/null, type, name, configJson)
ExportHistory(id, tenantId, type, templateId, paramsJson, fileUrl, coinsSpent, createdAt)
ChatSession(id, tenantId, contextType (PROFILE/PACKAGE), contextId, startedBy, createdAt)
ChatMessage(id, sessionId, sender (USER/RECRUITER/SYSTEM), content, createdAt)
LinkedInDraft(id, tenantId, userId, messageSnippet, draftText, createdAt, used)
LinkedInAnalysis(id, tenantId, userId, sourceType (PDF/URL), overallScore, sectionScoresJson, createdAt)
LLMProviderConfig(id, tenantId/null, provider, model, apiKeyRef, settingsJson)
```

***

#### 2.2.3 Main REST API Surfaces (High Level)

Use `/api/v1/...` for all JSON endpoints.

**Auth & Tenant:**
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
GET    /api/v1/auth/me
```

**Profile & Timeline:**
```
GET/PUT         /api/v1/profile
GET/POST/PUT/DELETE   /api/v1/profile/jobs
GET/POST/PUT/DELETE   /api/v1/profile/education
```

**Skills & Stories:**
```
GET/POST/PUT/DELETE   /api/v1/skills                    (catalog, admin-ish)
GET/POST/PUT/DELETE   /api/v1/profile/skills
GET/POST/PUT/DELETE   /api/v1/experience/projects
GET/POST/PUT/DELETE   /api/v1/experience/stories
```

**Packages & Public Access:**
```
GET/POST/PUT/DELETE   /api/v1/packages
POST                  /api/v1/packages/{id}/share-token

Public/anonymous endpoints:
GET   /public/profile/{profileSlug}
GET   /public/package/{token}
```

**Exports:**
```
GET   /api/v1/exports/templates
POST  /api/v1/exports/resume
POST  /api/v1/exports/background-presentation
POST  /api/v1/exports/cover-letter
GET   /api/v1/exports/{id}   (download meta or signed URL)
```

**Recruiter Chat:**
```
POST  /public/profile/{profileSlug}/chat
POST  /public/package/{token}/chat
GET   /api/v1/chat/sessions   (optional, for owner to review)
```

**LinkedIn Inbox Assistant:**
```
MVP (manual input):
POST  /api/v1/linkedin/drafts      (send incoming message snippet + context, get draft reply)
GET   /api/v1/linkedin/drafts      (history and coin usage)
```

**LinkedIn Profile Score Analyzer:**
```
POST  /api/v1/linkedin/analyze                                  (upload PDF or submit URL; returns analysis id + scores)
GET   /api/v1/linkedin/analysis/{id}                            (fetch result)
POST  /api/v1/linkedin/analysis/{id}/section/{sectionKey}/suggest   (generate suggestions for a section; first free, next cost coins)
```

**Coins & Billing:**
```
GET   /api/v1/billing/wallet
GET   /api/v1/billing/transactions
POST  /api/v1/billing/checkout-session   (future Stripe integration)
```

***

#### 2.2.4 AI Integration Pattern

Implement an interface such as:

```java
public interface LlmClient {
    LlmResponse complete(LlmRequest request);
}
```

**Have implementations:**
- `OpenAiLlmClient`
- `GeminiLlmClient`
- `ClaudeLlmClient`

**A `LlmRouterService` picks provider/model based on:** [ijfmr](https://www.ijfmr.com/papers/2025/1/36083.pdf)
- Action type (resume, recruiter chat, LinkedIn analysis)
- Tenant settings in `LLMProviderConfig`

**Prompt templates and RAG pipelines live in dedicated services:**
- `RecruiterChatService` – builds prompt, does RAG, calls `LlmClient`
- `ExportPromptService` – prompts for resume/cover/presentation
- `LinkedInAnalysisService` – parses LinkedIn content and prompts for scoring + suggestions

***

### 2.3 Frontend Structure (React)

#### 2.3.1 App Layout

```
src/
├── app/
│   ├── App.tsx              – routing and global layout
│   └── routes.tsx           – route definitions
├── pages/
│   ├── DashboardPage
│   ├── ProfileEditorPage    (tabs: About, Jobs, Education, Skills, Stories)
│   ├── PackagesPage
│   ├── PackageEditorPage
│   ├── ExportsPage
│   ├── LinkedInAssistantPage   (Inbox drafts + Profile analyzer tabs)
│   ├── BillingPage          (coins, usage)
│   └── Public pages:
│       ├── PublicProfilePage
│       └── PublicPackagePage
├── components/
│   ├── Generic: Form, TextField, TagInput, SkillChip, StoryCard, ChatBox, CoinBalanceBadge
│   └── Domain: JobForm, EducationForm, ProjectForm, StoryForm, PackageSelector, LinkedInSectionScoreCard
├── api/
│   └── authApi.ts, profileApi.ts, skillsApi.ts, experienceApi.ts, packageApi.ts, exportApi.ts, chatApi.ts, linkedinApi.ts, billingApi.ts
├── state/
│   └── Global auth store, user/tenant context, maybe global config (current provider, feature flags)
├── hooks/
│   └── useProfile, useSkills, useStories, useChatSession, useCoins, etc.
└── styles/
```

***

#### 2.3.2 Key UX Flows

**Profile Completion Flow:**
- Landing in dashboard, showing completion percentage and quick links:
   - Add experience
   - Add at least X stories
   - Link skills to stories
   - Run LinkedIn Score Analyzer

**Story Builder:**
- Wizard component:
   - Step 1: Select job + project
   - Step 2: STAR form fields
   - Step 3: Attach skills and metrics
   - Step 4: Set visibility (public/private) and save

**Package Builder:**
- Multi-select skills, stories, projects
- Option to "Preview package page" and generate share link

**Recruiter Chat:**
- Embed `ChatBox` in public pages:
   - Plain text input + streaming responses
   - Optional "suggested questions" chips

**LinkedIn Profile Analyzer UI:**
- Upload PDF or paste URL
- Show:
   - Overall score graph
   - Cards for each section with score and short comment
   - 1 free suggestion per section, plus a "More suggestions (cost X coins)" button

***

### 2.4 Cross-Cutting Concerns

**Error Handling and Validation:**
- Use standard API error format (code, message, fieldErrors)
- On frontend, generic `ErrorBoundary` and toast notifications

**Security:**
- CORS configured to allow your React origin in dev
- Role checks on all mutating endpoints; public endpoints careful to only use public/package data [bettercloud](https://www.bettercloud.com/monitor/the-fundamentals-of-role-based-access-control/)

**Observability & Metrics:**
- Basic logging for:
   - LLM calls (action type, provider, latency, success/failure)
   - Coin spends per tenant and feature
   - Errors on export generation and LinkedIn analysis

***

## Next Steps

1. **Refine entities and ERD** – Claude can help create detailed entity relationship diagrams
2. **Break into epics and stories** – Use this document as input for Claude to generate implementation tasks
3. **Setup project scaffolding** – Initialize Spring Boot + React projects with basic structure
4. **Iterate on MVP features** – Start with Profile + Skills + Basic Chat, then expand

***