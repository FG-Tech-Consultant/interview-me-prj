# Project Overview

**Project Name:** Live Resume & Career Copilot
**Type:** Career Management SaaS Platform
**Last Updated:** 2026-02-19
**Current Version:** v0.1.0 (Initial Planning)

---

## What is Live Resume & Career Copilot?

Live Resume & Career Copilot is a **modern career management platform** that acts as your always-up-to-date professional hub. It combines:

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

## Project Goals

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

## Personas

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

## How It Works

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                  User's Browser (React SPA)                  │
│              (TypeScript, React Query, MUI/Chakra)          │
└───────────────────────┬─────────────────────────────────────┘
                        │ HTTP/REST API (JSON)
                        │
┌───────────────────────▼─────────────────────────────────────┐
│               Spring Boot Backend (Java 21)                  │
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

### Core Technology Stack

**Frontend:**
- **React 18+** with TypeScript
- **Component Library**: MUI (Material-UI) or Chakra UI (TBD)
- **State Management**: React Query for server state, Context API for global state
- **Routing**: React Router
- **Build**: Vite or Create React App

**Backend:**
- **Java 21** (LTS) - Virtual threads, records, pattern matching
- **Spring Boot 3.x** - Web framework, dependency injection, REST APIs
- **Spring Security** - JWT authentication, RBAC
- **Spring Data JPA** - ORM with tenant filtering
- **PostgreSQL** - Relational database with JSONB and pgvector support
- **Liquibase** - Database schema versioning

**AI/LLM:**
- **OpenAI API** (ChatGPT), **Google Gemini API**, **Anthropic Claude API**
- **pgvector** - Vector embeddings for RAG (skills, stories retrieval)
- **Backend-controlled** prompts and cost tracking

**Infrastructure:**
- **Docker** - Containerized deployment
- **Gradle** - Build automation
- **Logback + Logstash Encoder** - Structured JSON logging
- **Spring Boot Actuator** - Metrics and health checks

---

## Functional Scope and Features

### 3.1 Account, Auth, and Tenancy

- Email/password authentication, password reset
- Optional future: SSO (GitHub, Google)
- **Soft multi-tenancy** from day one:
  - Single database, all tenant-owned entities include `tenant_id`
  - Always filtered by tenant in backend queries
  - User ↔ Tenant relation to support multiple users per tenant in the future

### 3.2 Profile and Timeline

**Profile:**
- Name, headline, short summary, location, relocation/remote preference, languages, links (LinkedIn, GitHub, personal site)
- Career preferences: target roles (e.g., Tech Lead, Architect), domains (Fintech, Payments, Marketplaces), target geos

**Timeline:**
- **Jobs**: Company, role, dates, location, employment type, responsibilities, achievements, metrics (TPS, volume, user counts)
  - Link to multiple projects/stories
- **Education**: Degree, institution, dates, notes, certifications

**Visibility Flags:**
- Each field/section can be marked public or private
- **Public**: visible on live resume and usable by recruiter chat
- **Private**: visible only to the owner and internal tools; recruiter chat MUST ignore

### 3.3 Skills, Tags, and Stories

**Skills & Tags:**
- Skills catalog with categories: Languages, Frameworks, Cloud, Databases, Messaging, Observability, Methodologies, Domains
- Each UserSkill:
  - Years of experience
  - Depth (1-5 scale)
  - Last used date
  - Confidence level
  - Tags (e.g., #java, #spring-boot, #kubernetes, #high-scale)

**Experience Projects:**
- "Project/Initiative" entity under a job:
  - Title, context, role, team size, tech stack, architecture type (monolith/microservices), metrics (scale, SLAs), outcomes
  - Linked skills and domains

**Stories (Case Studies):**
- Structured narrative per project (e.g., STAR: Situation, Task, Action, Result)
- Each story links to:
  - One ExperienceProject
  - One or more UserSkills
  - Optional metrics fields for impact
- **Privacy for stories**:
  - Story visibility: public/private
  - Public stories: recruiter chat and public pages can use
  - Private stories: only visible to owner and internal AI assistants

### 3.4 Packages and Shareable Links

**Packages:**
- Owner can define named packages (e.g., "Fintech-Lead-EU", "High-Scale-Backend")
- Each package includes a subset of skills, experiences, and stories (all must be public or package-public)
- Generated tokenized link per package (e.g., `/p/{slug}?t={token}`) granting read-only access

**Recruiter Package View:**
- Page showing:
  - Short intro, key skills, selected stories/projects
  - Chatbox constrained to that package's context only

### 3.5 Exports (Resume, Deck, Cover Letter)

**Resume Generator:**
- Templates: Standard 1-2 page resume, Tech-lead/architect focused, Fintech/payments-oriented
- Parameters: Target role, location, seniority, optional language
- Output: PDF (and optionally DOCX) generated server-side via templates
- **Coins**: All exports always cost coins

**Background Presentation Export:**
- Generate a deck similar to existing background presentations:
  - Summary slide, skills slide, professional timeline, case studies with metrics, recommendations excerpt
- Output: PDF

**Cover Letter Generator:**
- Input: target company, role, job description snippet, market (US, EU, Canada)
- Output: tailored letter grounded in profile, skills, and stories
- **Coins**: Always cost coins

### 3.6 Recruiter Chat on the Live Profile

**Public Profile Page:**
- Always-free live webpage acting as your primary online resume and portfolio
- Contains public profile info, public skills, public stories, and possibly a default package

**Recruiter Chatbox:**
- Embedded chat component for recruiters to ask questions like:
  - "Tell me about your biggest high-traffic payments project."
  - "What is your experience with Java 21 and Kubernetes in production?"
- **Backend RAG flow**:
  - For each question, retrieve relevant public stories/experiences/skills from database
  - Call configured LLM with retrieved context + question
- **Data constraints**:
  - Chat uses only public content and/or content in specific package accessed
  - Private content is never included in recruiter sessions
- **Coins**:
  - Recruiter chat uses free tier quotas (limited messages per month)
  - Extra usage consumes coins

### 3.7 LinkedIn Inbox Assistant (Draft-Only)

**Scope and Constraints:**
- Align with LinkedIn ToS by avoiding auto-sending or aggressive automation
- Only generate drafts to be sent manually by user

**Integration:**
- Phase 1: user manually pastes messages (MVP)
- Phase 2: optional API or extension-based integration (subject to LinkedIn policy)

**Features:**
- Categorize incoming messages (recruiter, agency, founder, spam, etc.)
- Suggest concise replies following pre-configured tone and rules (relocation, salary range, role fit)
- Allow quick insert of your pre-defined stories or links (to specific packages)

**Guardrails:**
- Draft-only: UI clearly indicates "Copy reply" or "Insert into LinkedIn," no automated send
- Free tier: limited number of AI-generated drafts per month; more drafts consume coins

### 3.8 LinkedIn Profile Score Analyzer

**Purpose:**
- Provide AI-powered review of user's LinkedIn profile, producing:
  - Overall score
  - Section-by-section scores (Headline, About, Experience, Education, Skills, etc.)
  - Improvement suggestions

**Input Options:**
- Option 1 (preferred for ToS safety): user exports LinkedIn as PDF and uploads
- Option 2 (future): user enters public LinkedIn URL, service fetches content within policy limits

**Output:**
- Overall profile score (0-100)
- Per-section scores (0-100) for: Photo/Banner (optional), Headline, About, Experience, Education, Skills, Recommendations, Other sections
- For each section:
  - Short explanation of current quality
  - Suggestions list with small, concrete actions

**Free vs Coins:**
- Full analysis (scores + brief comments) is free
- For each section:
  - 1 free suggestion item (e.g., one concrete rewrite idea)
  - Additional suggestions for same section require coins

**Integration with Profile:**
- User can "Apply suggestion" or "Import suggestion" to update fields in this platform's profile (not LinkedIn directly)
- Optionally store last analysis snapshot for tracking improvements over time

### 3.9 Coins, Plans, and Limits

**Coin Wallet:**
- Each user has a coin balance and transaction history (earn, spend, refund)
- Coins used for:
  - Additional recruiter chat usage beyond free tier
  - All exports (resume, deck, cover letters)
  - Extra LinkedIn inbox drafts beyond free tier
  - Extra LinkedIn Score Analyzer suggestions beyond first free suggestion per section

**Free Tier:**
- Always-on free tier with:
  - Live public resume page
  - Limited recruiter chat per month
  - Limited LinkedIn inbox drafts per month
  - Unrestricted LinkedIn profile analysis (scores) but only 1 free suggestion per section

**Paid Usage:**
- Coins purchased in packs (future Stripe integration)
- Later: monthly "Pro" tier with larger free quotas and/or auto-refilling coins

---

## Data Model (High Level)

**Key Entities** (names subject to refinement):

- **Tenant** - Logical tenant (organization or individual account)
- **User** - Authentication user, belongs to a tenant
- **Profile** - Personal info, preferences, and links
- **JobExperience** - Employment history entries
- **Education** - Education entries
- **Skill** - Canonical skill definition (name, category, description, tags)
- **UserSkill** - Relation between user and skill (years, depth, last used, confidence)
- **ExperienceProject** - Project/initiative under a job
- **Story** - Structured case study, links to project and skills, has visibility
- **Package** - Curated set of skills, projects, stories with share token
- **ExportTemplate** - Metadata for resume/cover/deck templates
- **ExportHistory** - Records of generated exports (type, params, file location, coins spent)
- **CoinWallet & CoinTransaction** - Balances and movements
- **ChatSession & ChatMessage** - Recruiter chat logs (with tenant and visibility constraints)
- **LinkedInDraft** - Generated drafts for inbox replies
- **LinkedInAnalysis** - LinkedIn score analyzer result, per-section scores and suggestions
- **LLMProviderConfig** - Per-tenant configuration of Gemini / ChatGPT / Claude

---

## Roadmap

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

## Current Status

**Version:** v0.1.0 (Initial Planning)

**Completed:**
- ✅ Project overview defined
- ✅ Constitution established (v2.0.0)
- ✅ `.specify/` framework adapted from Travian Bot project

**In Progress:**
- 🔄 Adapting memory files to new project context

**Next Steps:**
- Define initial database schema (Liquibase migrations)
- Set up Spring Boot project structure
- Set up React frontend structure
- Implement authentication & tenancy
- Build Profile CRUD endpoints

---

## Key Constraints

From `constitution.md`:

1. **Simplicity First**: React SPA + Spring Boot REST API; standard patterns
2. **Containerization**: Docker-first design
3. **Modern Java**: Use Java 21 features (virtual threads, records)
4. **Multi-Tenant Architecture**: Soft multi-tenancy with tenant filtering
5. **AI Integration**: Backend-controlled LLM with cost tracking
6. **Security**: JWT auth, BCrypt passwords, public/private content separation
7. **Database Evolution**: Liquibase timestamp-based migrations
8. **LinkedIn Compliance**: Draft-only, user-initiated, ToS-compliant
9. **Modularity**: Backend services max 500 lines, React components max 300 lines
10. **Observability**: Structured logging, metrics, Actuator endpoints

---

## How to Find Information

- **Project Goals & Architecture**: This file (`.specify/memory/project-overview.md`)
- **Constitutional Principles**: `.specify/memory/constitution.md`
- **Technology Stack**: `.specify/memory/tech-context.md`
- **Version History**: `.specify/memory/version-tracking.md`
- **Liquibase Guidelines**: `.specify/memory/liquibase-guidelines.md`
- **Feature Specs**: `specs/` directory (to be created)
- **User Documentation**: `README.md`

---

**Document Version:** 1.0.0
**AI Agents**: Use this document for project context before implementing features.
