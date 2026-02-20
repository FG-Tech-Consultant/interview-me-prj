# Project Overview: Live Resume & Career Copilot

**Project Name:** Live Resume & Career Copilot (aka "Interview Me")
**Type:** Career Management SaaS Platform
**Last Updated:** 2026-02-19
**Current Version:** v0.1.0 (Initial Planning)
**Document Version:** 2.0.0

> **"Interview Me"** – A personal career platform combining live resume, skills graph, AI copilot, and SaaS monetization for developers.

---

## Table of Contents

- [1. What is Live Resume & Career Copilot?](#1-what-is-live-resume--career-copilot)
- [2. Goals and Success Criteria](#2-goals-and-success-criteria)
- [3. Personas](#3-personas)
- [4. Functional Features](#4-functional-features)
- [5. Data Model (High Level)](#5-data-model-high-level)
- [6. Non-Functional Requirements](#6-non-functional-requirements)
- [7. Security, Privacy, and Compliance](#7-security-privacy-and-compliance)
- [8. Pricing and Tiering Model](#8-pricing-and-tiering-model)
- [9. Technical Architecture](#9-technical-architecture)
- [10. Roadmap](#10-roadmap)
- [11. Current Status](#11-current-status)

---

## 1. What is Live Resume & Career Copilot?

Live Resume & Career Copilot (working name: **"Interview Me"**) is a **modern career management platform** that acts as your always-up-to-date professional hub. It combines:

- **Live Resume/Portfolio Site** - A public-facing, always-current representation of your professional profile
- **Structured Skills & Stories Graph** - Answer recruiter questions with concrete examples (scale, traffic, incidents, leadership)
- **AI Career Copilot** providing:
  - Recruiter chat on your profile site
  - LinkedIn inbox reply drafting
  - LinkedIn profile score analysis and optimization suggestions

### Key Differentiators

Unlike traditional resume builders or LinkedIn itself:
- **Always Up-to-Date**: Single source of truth for your career data, automatically reflected everywhere
- **AI-Powered Interactions**: Recruiters can chat with your profile; get instant answers about your experience
- **Privacy-Aware**: Control what's public vs. private; recruiter chat only uses public content
- **Document Generation**: Generate tailored resumes, cover letters, and background presentations on-demand
- **LinkedIn Optimization**: AI-powered profile scoring with actionable improvement suggestions
- **Multi-Tenant SaaS**: Designed for individual use initially, scalable to teams and organizations

---

## 2. Goals and Success Criteria

### Primary Goals (v1 - Personal Use)

1. **Complete Career Representation**: All resume and background presentation content can be maintained in the system without loss of detail
2. **Document Generation**: Generate at least:
   - 1-2 resume formats (Standard, Tech Lead/Architect, Fintech-oriented)
   - 1 background presentation deck
   - 1 cover letter tailored to specific role/location
3. **Recruiter Chat**: Correctly answer typical recruiter questions like:
   - "Do you have experience with X?"
   - "Tell me about a time you handled high scale / big traffic"
   - Questions grounded in your structured stories (STAR format)
4. **LinkedIn Score Analyzer**:
   - Ingest LinkedIn profile (URL or PDF)
   - Assign numeric scores per section (Headline, About, Experience, Education, Skills)
   - Provide at least one concrete improvement suggestion per section

### Secondary Goals (Product/SaaS)

1. **Multi-User Onboarding**: Additional developers can onboard, build profiles, and pay with coins for exports and AI usage
2. **Sustainable Coin Model**: Free tier costs do not exceed paid usage revenue
3. **Horizontal Scalability**: Support growing user base with standard SaaS patterns

---

## 3. Personas

### Owner/Candidate (Primary: You, Later Other Devs)
- **Profile**: Senior engineer / tech leader, often in fintech, with long experience and many projects
- **Needs**: Single place to maintain profile, skills, and case studies; automate recruiter interactions
- **Pain Points**: Manually updating multiple formats (resume, LinkedIn, portfolio site); repetitive recruiter questions

### Recruiter / Hiring Manager
- **Profile**: Needs fast, trustworthy, self-service access to skills, tech stack, scale, and leadership examples
- **Needs**: Ask questions interactively instead of reading long PDFs
- **Pain Points**: Time-consuming resume review; difficulty assessing real-world experience depth

### Platform Admin / Product Owner
- **Profile**: Manages users, coins, usage limits, and LLM providers
- **Needs**: Ensure security, compliance with LinkedIn policies, and cost control
- **Pain Points**: LLM cost overruns; LinkedIn ToS violations; data privacy issues

---

## 4. Functional Features

### 4.1 Account, Auth, and Tenancy

**Authentication:**
- Email/password auth, password reset
- Optional future: SSO (GitHub, Google)

**Multi-tenancy:**
- Soft multi-tenancy from day one:
  - Single database, all tenant-owned entities include `tenant_id`
  - Always filtered by `tenant_id` in backend queries
- User ↔ Tenant relation to support multiple users per tenant in the future

### 4.2 Profile and Timeline

**Profile:**
- Name, headline, short summary, location, relocation/remote preference, languages, links (LinkedIn, GitHub, personal site)
- Career preferences: target roles (e.g., Tech Lead, Architect), domains (Fintech, Payments, Marketplaces), target geos

**Timeline:**
- **Jobs:**
  - Company, role, dates, location, employment type, responsibilities, achievements, metrics (TPS, volume, user counts)
  - Link to multiple projects/stories
- **Education:**
  - Degree, institution, dates, notes, certifications

**Visibility Flags:**
- Each field/section can be marked **public** or **private**
- **Public**: visible on live resume and usable by recruiter chat
- **Private**: visible only to the owner and internal tools; recruiter chat MUST ignore

### 4.3 Skills, Tags, and Stories

**Skills & Tags:**
- Skills catalog with categories: Languages, Frameworks, Cloud, Databases, Messaging, Observability, Methodologies (Scrum, Kanban), Domains (Fintech, Payments, Marketplaces)
- Each `UserSkill`:
  - Years of experience
  - Depth (1–5 scale)
  - Last used date
  - Confidence level
  - Tags (e.g., `#java`, `#spring-boot`, `#kubernetes`, `#high-scale`)

**Experience Projects:**
- "Project/Initiative" entity under a job:
  - Title, context, role, team size, tech stack, architecture type (monolith/microservices), metrics (scale, SLAs), outcomes
  - Linked skills and domains

**Stories (Case Studies):**
- Structured narrative per project using **STAR format** (Situation, Task, Action, Result)
- Each story links to:
  - One `ExperienceProject`
  - One or more `UserSkills`
  - Optional metrics fields for impact
- **Privacy for stories**:
  - Story visibility: public/private
  - Public stories: recruiter chat and public pages can use
  - Private stories: only visible to owner and internal AI assistants

### 4.4 Packages and Shareable Links

**Packages:**
- Owner can define named packages (e.g., "Fintech-Lead-EU", "High-Scale-Backend")
- Each package includes a subset of skills, experiences, and stories (all must be public or package-public)
- Generated **tokenized link** per package (e.g., `/p/{slug}?t={token}`) granting read-only access

**Recruiter Package View:**
- Page showing:
  - Short intro, key skills, selected stories/projects
  - Chatbox constrained to that package's context only

### 4.5 Exports (Resume, Deck, Cover Letter)

**Resume Generator:**
- **Templates:**
  - Standard 1–2 page resume
  - Tech-lead/architect focused resume
  - Fintech/payments-oriented resume
- **Parameters:** Target role, location, seniority, optional language
- **Output:** PDF (and optionally DOCX) generated server-side via templates
- **Coins**: All exports always cost coins

**Background Presentation Export:**
- Generate a deck similar to existing background presentations:
  - Summary slide, skills slide, professional timeline, case studies with metrics, recommendations excerpt
- **Output:** PDF
- **Coins**: Always cost coins

**Cover Letter Generator:**
- **Input:** Target company, role, job description snippet, market (e.g., US, EU, Canada)
- **Output:** Tailored letter grounded in profile, skills, and stories
- **Coins**: Always cost coins

### 4.6 Recruiter Chat on the Live Profile

**Public Profile Page:**
- **Always-free** live webpage acting as your primary online resume and portfolio
- Contains public profile info, public skills, public stories, and possibly a default package

**Recruiter Chatbox:**
- Embedded chat component for recruiters to ask questions like:
  - "Tell me about your biggest high-traffic payments project."
  - "What is your experience with Java 25 and Kubernetes in production?"

**Backend RAG Flow:**
- For each question, retrieve relevant public stories/experiences/skills from database
- Call the configured LLM with retrieved context + question

**Data Constraints:**
- Chat uses **only public content** and/or content in specific package accessed
- Private content is **never included** in recruiter sessions

**Coins:**
- Recruiter chat uses **free tier quotas** (limited messages per month)
- Extra usage consumes coins

### 4.7 LinkedIn Inbox Assistant (Draft-Only)

**Scope and Constraints:**
- Align with LinkedIn ToS by avoiding auto-sending or aggressive automation
- **Only generate drafts** to be sent manually by user

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

### 4.8 LinkedIn Profile Score Analyzer

**Purpose:**
- Provide AI-powered review of user's LinkedIn profile, producing:
  - Overall score
  - Section-by-section scores (Headline, About, Experience, Education, Skills, etc.)
  - Limited improvement suggestions

**Input Options:**
- **Option 1 (Preferred for ToS safety):** User exports LinkedIn as PDF and uploads
- **Option 2 (Future):** User enters public LinkedIn URL, service fetches content within policy limits

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

### 4.9 Coins, Plans, and Limits

**Coin Wallet:**
- Each user has a coin balance and transaction history (earn, spend, refund)
- Coins used for:
  - Additional recruiter chat usage beyond free tier
  - All exports (resume, deck, cover letters)
  - Extra LinkedIn inbox drafts beyond free tier
  - Extra LinkedIn Score Analyzer suggestions beyond first free suggestion per section

**Free Tier (Always-On):**
- Live public resume page
- Limited recruiter chat per month
- Limited LinkedIn inbox drafts per month
- Unrestricted LinkedIn profile analysis (scores) but only 1 free suggestion per section

**Paid Usage:**
- Coins purchased in packs (future Stripe integration)
- Later: monthly "Pro" tier with larger free quotas and/or auto-refilling coins

---

## 5. Data Model (High Level)

**Key Entities** (names subject to refinement):

| Entity | Purpose |
|--------|---------|
| `Tenant` | Logical tenant (organization or individual account) |
| `User` | Authentication user, belongs to a tenant |
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
| `LinkedInAnalysis` | LinkedIn score analyzer result, per-section scores and suggestions |
| `LLMProviderConfig` | Per-tenant configuration of Gemini / ChatGPT / Claude |

---

## 6. Non-Functional Requirements

### Tech Stack

**Frontend:**
- React 18+ with TypeScript
- Component Library: MUI (Material-UI) or Chakra UI (TBD)
- State Management: React Query for server state, Context API for global state
- Routing: React Router
- Build: Vite or Create React App

**Backend:**
- Spring Boot 3.x, Java 25
- Spring Security (JWT auth, RBAC)
- Spring Data JPA (with tenant filtering)
- REST APIs under `/api/v1/*`

**Database:**
- PostgreSQL 18 (primary and only database)
- pgvector extension (for embeddings/RAG)
- Liquibase 4.25.0+ (schema versioning)

**AI/LLM:**
- OpenAI API (ChatGPT)
- Google Gemini API
- Anthropic Claude API
- Backend-controlled prompts and cost tracking

**Infrastructure:**
- Docker (containerized deployment: backend + frontend + PostgreSQL)
- Gradle 8.5+ (build automation)
- Logback + Logstash Encoder (structured JSON logging)
- Spring Boot Actuator (metrics and health checks)

### Performance

- **SaaS baseline**: p95 API latency under ~300–500 ms for core CRUD and chat orchestration (excluding LLM call latency)
- **Asynchronous workflows**: Heavy exports, bulk LinkedIn analysis via background jobs/queues

### Scalability & Isolation

- **Horizontal-scalable** stateless backend services
- **Tenant isolation** at application level using tenant filters and foreign keys
- PostgreSQL in separate container or managed service (AWS RDS, Google Cloud SQL, Azure Database)
- In future, option to migrate high-value tenants to schema/db-per-tenant models

### Observability

- Structured logging, metrics for:
  - LLM calls (count, latency, cost proxy)
  - Export generation (success/failure rates)
  - Coin spends and free tier usage

---

## 7. Security, Privacy, and Compliance

### AuthN & AuthZ

- JWT-based authentication
- RBAC: roles like `OWNER`, `ADMIN`, `RECRUITER_VIEW` (tokenized read-only) respecting least-privilege

### Data Protection

- Secrets and API keys encrypted at rest (environment variables or encrypted DB)
- Clear distinction between public and private content in schema and queries
- Tokenized links for packages use expiring or revocable tokens (JWT)

### LinkedIn Compliance

- **No auto-sending messages**
- Prefer PDF upload for profile analysis to avoid scraping
- If URL-based fetching is added, respect rate limits and ToS; clearly mark as "user-initiated analysis"

---

## 8. Pricing and Tiering Model

### Free Tier (Always)

- Live online resume/profile
- Limited recruiter chat messages/month
- Limited LinkedIn inbox drafts/month
- Unlimited LinkedIn profile scoring, but only 1 free suggestion per section

### Coins

- All exports (resume, deck, cover letters)
- Extra recruiter chat above free limit
- Extra LinkedIn inbox drafts above free limit
- Extra LinkedIn profile suggestions per section beyond first
- **Future:** "Pro" subscription bundling coins and higher limits

---

## 9. Technical Architecture

### 9.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  User's Browser (React SPA)                  │
│              (TypeScript, React Query, MUI/Chakra)          │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/REST API (JSON)
                        │
┌───────────────────────▼─────────────────────────────────────┐
│               Spring Boot Backend (Java 25)                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  REST Controllers (/api/v1/*)                        │  │
│  │  - AuthController, ProfileController,               │  │
│  │    SkillsController, ChatController, etc.           │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Business Logic Services                              │  │
│  │  - ProfileService (career data CRUD)                 │  │
│  │  - RecruiterChatService (RAG + LLM)                  │  │
│  │  - ExportService (resume/deck generation)            │  │
│  │  - LinkedInAnalysisService (profile scoring)         │  │
│  │  - BillingService (coins, transactions)             │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  LLM Integration Layer                                │  │
│  │  - LlmClient (Gemini, ChatGPT, Claude)               │  │
│  │  - PromptTemplateService                             │  │
│  │  - RAG Pipeline (pgvector retrieval)                 │  │
│  └────────────────────┬─────────────────────────────────┘  │
│                       │                                      │
│  ┌────────────────────▼─────────────────────────────────┐  │
│  │  Data Access Layer (Spring Data JPA)                 │  │
│  │  - Repositories (Tenant-filtered)                    │  │
│  │  - PostgreSQL + pgvector                             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**Three-Layer Architecture:**
1. **Controllers** - Handle HTTP requests, validate input, return responses
2. **Business Logic Services** - Orchestrate workflows, implement domain logic, call LLM
3. **Data Access Layer** - Repositories, tenant-filtered queries, database interaction

**Deployment Architecture:**
- Backend container (stateless Spring Boot app)
- Frontend container (Nginx serving React static assets)
- PostgreSQL container or managed service (separate from app containers)
- Cloud storage for exports (S3/GCS) or local volume in development

### 9.2 Backend Package Structure

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

**Within each domain package, follow layered architecture:**
- `controller` (REST endpoints)
- `service` (business logic)
- `repository` (Spring Data JPA repositories)
- `model` or `entity` (JPA entities)
- `dto` + mappers (if you prefer DTOs over exposing entities)

### 9.3 AI Integration Pattern

**Interface:**
```java
public interface LlmClient {
    LlmResponse complete(LlmRequest request);
}
```

**Implementations:**
- `OpenAiLlmClient`
- `GeminiLlmClient`
- `ClaudeLlmClient`

**LlmRouterService** picks provider/model based on:
- Action type (resume, recruiter chat, LinkedIn analysis)
- Tenant settings in `LLMProviderConfig`

**Prompt templates and RAG pipelines live in dedicated services:**
- `RecruiterChatService` – builds prompt, does RAG, calls `LlmClient`
- `ExportPromptService` – prompts for resume/cover/presentation
- `LinkedInAnalysisService` – parses LinkedIn content and prompts for scoring + suggestions

---

## 10. Roadmap

### Phase 1: MVP (Personal Use - v1.0)
- [ ] Auth & tenant setup (email/password, JWT)
- [ ] Profile CRUD (timeline, jobs, education)
- [ ] Skills catalog & user skills management
- [ ] Stories (STAR format) creation & editing
- [ ] Public/private visibility controls
- [ ] Live public profile page (always-free)
- [ ] Recruiter chat (RAG + LLM, free tier)
- [ ] Resume export (1 template, PDF)
- [ ] LinkedIn Profile Score Analyzer (upload PDF, scores + 1 free suggestion per section)
- [ ] Coin wallet (balance tracking, no payment yet)

### Phase 2: Enhanced Features (v1.5)
- [ ] Background presentation export
- [ ] Cover letter generator
- [ ] LinkedIn Inbox Assistant (draft-only)
- [ ] Packages (curated content with tokenized links)
- [ ] Multiple resume templates
- [ ] Stripe integration for coin purchases

### Phase 3: SaaS Scale (v2.0+)
- [ ] Multi-user per tenant (organizations)
- [ ] Horizontal scaling (Docker orchestration, load balancing)
- [ ] Advanced analytics (profile views, chat engagement)
- [ ] Webhooks for export completion
- [ ] Public API for integrations

---

## 11. Current Status

**Version:** v0.1.0 (Initial Planning)

**Completed:**
- ✅ Project overview defined (v2.0.0)
- ✅ Constitution established (v2.0.1)
- ✅ `.specify/` framework adapted from Travian Bot project
- ✅ Technical stack defined (React + Spring Boot + PostgreSQL)
- ✅ Memory files corrected (removed Selenium/embedded DB references)

**In Progress:**
- 🔄 Finalizing project structure

**Next Steps:**
- Define initial database schema (Liquibase migrations)
- Set up Spring Boot project structure (scaffold with Gradle)
- Set up React frontend structure (scaffold with Vite/CRA)
- Implement authentication & tenancy (JWT + tenant filtering)
- Build Profile CRUD endpoints

---

## Key Constraints

From `constitution.md` (v2.0.1):

1. **Simplicity First**: React SPA + Spring Boot REST API; standard patterns
2. **Containerization**: Docker-first design with PostgreSQL in separate container
3. **Modern Java**: Use Java 25 features (virtual threads, records)
4. **Multi-Tenant Architecture**: Soft multi-tenancy with tenant filtering
5. **AI Integration**: Backend-controlled LLM with cost tracking
6. **Security**: JWT auth, BCrypt passwords, public/private content separation
7. **Database Evolution**: Liquibase timestamp-based migrations, PostgreSQL only
8. **LinkedIn Compliance**: Draft-only, user-initiated, ToS-compliant
9. **Modularity**: Backend services max 500 lines, React components max 300 lines
10. **Observability**: Structured logging, metrics, Actuator endpoints

---

## How to Find Information

- **Project Goals & Architecture**: This file (`.specify/memory/project-overview.md`) or `./project-overview.md`
- **Constitutional Principles**: `.specify/memory/constitution.md` (v2.1.0)
- **Technology Stack**: `.specify/memory/tech-context.md` (v1.0.2)
- **Version History**: `.specify/memory/version-tracking.md` (v1.0.0)
- **Liquibase Guidelines**: `.specify/memory/liquibase-guidelines.md` (v1.0.2)
- **Feature Specs**: `specs/` directory (to be created)
- **User Documentation**: `README.md` (to be created)

---

## External References

**Confluence Documentation:**
- Project Space: https://techfernandogomes.atlassian.net/wiki/spaces/INTME/overview?homepageId=310149424

---

**Document Version:** 2.0.0
**Last Updated:** 2026-02-19

**Changelog:**
- v2.0.0: Complete restructure with Table of Contents, improved organization, alignment with reformatted `./project-overview.md`, added deployment architecture details
- v1.0.0: Initial version adapted from Travian Bot project

**AI Agents**: Use this document for project context before implementing features.
