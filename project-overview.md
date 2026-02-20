Project: Live Resume & Career Copilot (React + Spring Boot)
1. Goals and success criteria
   Goal: Build a personal career platform that acts as:
   A live resume / portfolio site that is always up to date.
   A structured skills and stories graph to answer recruiter questions with concrete examples (scale, traffic, incidents, leadership, etc.).FERNANDO_GOMES_RESUME_2025_LEAD_EN_LATEX.pdf+1
   An AI copilot for:
   Recruiter chat on the profile site.
   LinkedIn inbox reply drafting.
   LinkedIn profile “score” analysis and optimization suggestions.
   Success criteria (v1 personal use):
   All of your current resume and background presentation can be represented in the system without loss of important detail.FERNANDO_GOMES_BACKGROUND_PRESENTATION_2025_FULL.pdf+1
   You can generate at least:
   1–2 resume formats,
   1 background presentation, and
   1 cover letter tailored to a specific role/location.
   Recruiter chat can correctly answer typical “Do you have experience with X?” and “Tell me about a time you handled high scale / big traffic” questions using your stories.FERNANDO_GOMES_RESUME_2025_LEAD_EN_LATEX.pdf+1
   LinkedIn Score Analyzer can:
   Ingest your LinkedIn (URL or PDF),
   Assign a numeric score per section (headline, About, Experience, Education, Skills), and
   Provide at least one concrete improvement suggestion per section.redactai+3
   Later success criteria (product/SaaS):
   Additional dev users can onboard, build profiles, and pay with coins for exports and AI usage.
   Coin model is sustainable: free tier costs do not exceed paid usage.getmonetizely+2

2. Personas
   Owner candidate (primary: you, later other devs)
   Senior engineer / tech leader, often in fintech, with long experience and many projects.FERNANDO_GOMES_BACKGROUND_PRESENTATION_2025_FULL.pdf+1
   Wants a single place to maintain profile, skills, and case studies, and to automate recruiter interactions.
   Recruiter / hiring manager
   Needs fast, trustworthy, self‑service access to skills, tech stack, scale, and leadership examples.
   Wants to ask questions interactively instead of reading long PDFs.
   Platform admin / product owner
   Manages users, coins, usage limits, and LLM providers.
   Ensures security, compliance with LinkedIn policies, and cost control.blog.reachy+3

3. Functional scope and features
   3.1 Account, auth, and tenancy
   Email/password auth, password reset.
   Optional future: SSO (GitHub, Google).
   Soft multi‑tenancy from day one:
   Single database, all tenant‑owned entities include tenant_id and are always filtered by it in backend.workos+2
   User ↔ tenant relation to support multiple users per tenant in the future.
   3.2 Profile and timeline
   Profile:
   Name, headline, short summary, location, relocation/remote preference, languages, links (LinkedIn, GitHub, personal site).FERNANDO_GOMES_RESUME_2025_LEAD_EN_LATEX.pdf+1
   Career preferences: target roles (e.g., Tech Lead, Architect), domains (Fintech, Payments, Marketplaces), and target geos.
   Timeline:
   Jobs:
   Company, role, dates, location, employment type, responsibilities, achievements, metrics (e.g., TPS, volume, user counts).FERNANDO_GOMES_BACKGROUND_PRESENTATION_2025_FULL.pdf+1
   Link to multiple projects/stories.
   Education:
   Degree, institution, dates, notes, certifications.
   Visibility flags:
   Each field/section can be marked public or private.
   Public: visible on live resume and usable by recruiter chat.
   Private: visible only to the owner and internal tools; recruiter chat must ignore.suridata+2
   3.3 Skills, tags, and stories
   Skills & tags:
   Skills catalog with categories: Languages, Frameworks, Cloud, Databases, Messaging, Observability, Methodologies (Scrum, Kanban), Domains (Fintech, Payments, Marketplaces).FERNANDO_GOMES_RESUME_2025_LEAD_EN_LATEX.pdf+1
   Each UserSkill:
   Years of experience,
   Depth (1–5 scale),
   Last used date,
   Confidence level,
   #tags (e.g., #java, #spring-boot, #kubernetes, #high-scale).
   Experience projects:
   “Project/Initiative” entity under a job:
   Title, context, role, team size, tech stack, architecture type (monolith/microservices), metrics (scale, SLAs), and outcomes.FERNANDO_GOMES_BACKGROUND_PRESENTATION_2025_FULL.pdf+1
   Linked skills and domains.
   Stories (case studies):
   Structured narrative per project (e.g. STAR: Situation, Task, Action, Result).
   Each story links to:
   One ExperienceProject,
   One or more UserSkills,
   Optional metrics fields for impact.
   Privacy for stories:
   Story visibility: public/private.
   Public stories: recruiter chat and public pages can use.
   Private stories: only visible to owner and internal AI assistants (e.g., personal coaching), never exposed or used in recruiter sessions.
   3.4 Packages and shareable links
   Packages:
   Owner can define named packages (e.g., “Fintech‑Lead‑EU”, “High‑Scale‑Backend”).
   Each package includes a subset of skills, experiences, and stories (all must be public or package‑public).
   Generated tokenized link per package (e.g. /p/{slug}?t={token}) granting read‑only access to that package.help.cloudspot+3
   Recruiter package view:
   Page showing:
   Short intro, key skills, selected stories/projects.
   Chatbox constrained to that package’s context only.
   3.5 Exports (resume, deck, cover letter)
   Resume generator:
   Templates:
   Standard 1–2 page resume.
   Tech‑lead/architect focused resume.
   Fintech/payments‑oriented resume.FERNANDO_GOMES_RESUME_2025_LEAD_EN_LATEX.pdf+1
   Parameters:
   Target role, location, seniority, and optional language.
   Output:
   PDF (and optionally DOCX) generated server‑side via templates.
   Background presentation export:
   Generate a deck similar to your existing background presentation:
   Summary slide, skills slide, professional timeline, case studies with metrics, recommendations excerpt.[drive.google]​
   Output: PDF.
   Cover letter generator:
   Input: target company, role, job description snippet, market (e.g. US, EU, Canada).
   Output: tailored letter grounded in profile, skills, and stories.
   Coins:
   All exports (resume, deck, cover letter) always cost coins.
   3.6 Recruiter chat on the live profile
   Public profile page:
   Always‑free live webpage acting as your primary online resume and portfolio.
   Contains public profile info, public skills, public stories, and possibly a default package.
   Recruiter chatbox:
   Embedded chat component for recruiters to ask questions like:
   “Tell me about your biggest high‑traffic payments project.”
   “What is your experience with Java 21 and Kubernetes in production?”FERNANDO_GOMES_RESUME_2025_LEAD_EN_LATEX.pdf+1
   Backend RAG flow:
   For each question, retrieve relevant public stories/experiences/skills from your store.
   Call the configured LLM with retrieved context + question.
   Data constraints:
   Chat uses only public content and/or content in the specific package accessed.
   Private content is never included in recruiter sessions.
   Coins:
   Recruiter chat uses free tier quotas (limited messages per month).
   Extra usage (e.g., beyond monthly limit or extended conversations) consumes coins.
   3.7 LinkedIn Inbox Assistant (draft‑only)
   Scope and constraints:
   Align with LinkedIn ToS by avoiding auto‑sending or aggressive automation; only generate drafts to be sent manually by user.blog.closelyhq+4
   Integration via:
   Phase 1: user manually pastes messages (MVP).
   Phase 2: optional API or extension‑based integration (subject to LinkedIn policy).
   Features:
   Categorize incoming messages (recruiter, agency, founder, spam, etc.).
   Suggest concise replies following pre‑configured tone and rules (e.g., relocation, salary range, role fit).
   Allow quick insert of your pre‑defined stories or links (e.g., to specific packages).
   Guardrails:
   Draft‑only: UI clearly indicates “Copy reply” or “Insert into LinkedIn,” no automated send.
   Free tier: limited number of AI‑generated drafts per month; more drafts consume coins.
   3.8 LinkedIn Profile Score Analyzer
   Purpose:
   Provide an AI‑powered review of the user’s LinkedIn profile, producing:
   Overall score,
   Section‑by‑section scores (Headline, About, Experience, Education, Skills, etc.),
   Limited improvement suggestions.jobwinner+6
   Input options:
   Option 1 (preferred for ToS safety): user exports LinkedIn as PDF and uploads.
   Option 2 (future): user enters public LinkedIn URL, and service fetches content within policy limits (still recommended to guide user to export PDF).2pr+1
   Output:
   Overall profile score (0–100).
   Per‑section scores (0–100) for:
   Photo/Banner (optional), Headline, About, Experience, Education, Skills, Recommendations, Other sections.
   For each section:
   Short explanation of current quality (e.g., “About is clear but lacks metrics”),
   Suggestions list with small, concrete actions.
   Free vs coins:
   Full analysis (scores + brief comments) is free.
   For each section:
   1 free suggestion item (e.g., one concrete rewrite idea or structured bullet list).
   Additional suggestions for the same section require coins (e.g., generate multiple alternative headlines or new “About” drafts).
   Integration with profile:
   The user can choose to “Apply suggestion” or “Import suggestion” to update fields in this platform’s profile (not LinkedIn directly).
   Optionally store last analysis snapshot for tracking improvements over time.
   3.9 Coins, plans, and limits
   Coin wallet:
   Each user has a coin balance and transaction history (earn, spend, refund).
   Coins used for:
   Additional recruiter chat usage beyond free tier.
   All exports (resume, deck, cover letters).
   Extra LinkedIn inbox drafts beyond free tier.
   Extra LinkedIn Score Analyzer suggestions beyond the first free suggestion per section.maxio+4
   Free tier:
   Always‑on free tier with:
   Live public resume page.
   Limited recruiter chat per month.
   Limited LinkedIn inbox drafts per month.
   Unrestricted LinkedIn profile analysis (scores) but only 1 free suggestion per section.
   Paid usage:
   Coins purchased in packs (future Stripe integration).
   Later: monthly “Pro” tier with larger free quotas and/or auto‑refilling coins.

4. Data model (high level)
   Key entities (names not final):
   Tenant – logical tenant.
   User – auth user, belongs to a tenant.
   Profile – personal info, preferences, and links.
   JobExperience – employment history entries.
   Education – education entries.
   Skill – canonical skill definition (name, category, description, tags).
   UserSkill – relation between user and skill (years, depth, last used, confidence).
   ExperienceProject – project/initiative under a job.
   Story – structured case study, links to project and skills, has visibility.
   Package – curated set of skills, projects, stories with share token.
   ExportTemplate – metadata for resume/cover/deck templates.
   ExportHistory – records of generated exports (type, params, file location, coins spent).
   CoinWallet & CoinTransaction – balances and movements.
   ChatSession & ChatMessage – recruiter chat logs (with tenant and visibility constraints).
   LinkedInDraft – generated drafts for inbox replies.
   LinkedInAnalysis – LinkedIn score analyzer result, including per‑section scores and suggestions.
   LLMProviderConfig – per‑tenant configuration of Gemini / ChatGPT / Claude etc.

5. Non‑functional & architecture overview
   Tech stack:
   Frontend: React + TypeScript, component library (e.g. MUI/Chakra), React Query or equivalent.
   Backend: Spring Boot, Java 17+ (or 21), REST APIs, JWT auth, multi‑tenant filters.
   Database: relational (Postgres/SQL Server) with optional pgvector or similar for RAG embeddings.
   Performance:
   Standard SaaS baseline: p95 API latency under ~300–500 ms for core CRUD and chat orchestration (excluding LLM call latency).
   Asynchronous workflows (e.g., heavy exports, bulk LinkedIn analysis) via background jobs/queues.
   Scalability & isolation:
   Horizontal‑scalable stateless backend services.
   Tenant isolation at application level using tenant filters and foreign keys. In future, option to migrate high‑value tenants to schema/db‑per‑tenant models.learn.microsoft+2
   Observability:
   Structured logging, metrics for:
   LLM calls (count, latency, cost proxy),
   Export generation,
   Coin spends and free tier usage.

6. Security, privacy, and compliance
   AuthN & AuthZ:
   JWT‑based authentication.
   RBAC: roles like OWNER, ADMIN, RECRUITER_VIEW (tokenized read‑only) respecting least‑privilege.spendflo+3
   Data protection:
   Secrets and API keys encrypted at rest.
   Clear distinction between public and private content in schema and queries.
   Tokenized links for packages use expiring or revocable tokens.
   LinkedIn compliance:
   No auto‑sending messages.
   Prefer PDF upload for profile analysis to avoid scraping.
   If URL‑based fetching is added, respect rate limits and ToS; clearly mark as “user‑initiated analysis.”trykondo+5

7. Pricing and tiering model (conceptual)
   Free tier (always):
   Live online resume/profile.
   Limited recruiter chat messages/month.
   Limited LinkedIn inbox drafts/month.
   Unlimited LinkedIn profile scoring, but only 1 free suggestion per section of profile.
   Coins:
   All exports.
   Extra recruiter chat above free limit.
   Extra LinkedIn inbox drafts above free limit.
   Extra LinkedIn profile suggestions per section beyond first.
   Future: “Pro” subscription bundling coins and higher limits.softwarepricing+2


Technical overall guide:

1. High‑level architecture
   Frontend: React + TypeScript SPA, running on its own origin in dev (e.g., localhost:3000), talking to the backend via REST APIs under /api/*.dhiwise+2
   Backend: Spring Boot app exposing REST endpoints, following a layered architecture (Controller → Service → Repository).learncodewithdurgesh+1youtube+1
   AI integration: All LLM logic lives in the backend (“LLM in backend” pattern: prompts, provider routing, RAG, and rate limits are server‑side).monarchwadia+3
   Database: Relational DB (Postgres is a good default) with tenant‑aware schemas; optional pgvector for embeddings later.workos+2
   Auth: JWT‑based auth with roles (OWNER, ADMIN, RECRUITER_VIEW_TOKEN).
   Deployment: Initially one backend service + one frontend app; later can split modules or add workers for async tasks (exports, heavy LinkedIn analysis).

2. Backend structure (Spring Boot)
   2.1 Packages / modules
   Use a modular package structure inside a single Spring Boot app:
   com.yourapp.config – security config, tenant filter, CORS, LLM provider config.
   com.yourapp.auth – user registration, login, JWT issuing/validation.
   com.yourapp.tenancy – Tenant entity, tenant resolver (from JWT), multi‑tenant filter.
   com.yourapp.profile – profile, jobs, education.
   com.yourapp.skills – skills catalog, user skills, tagging.
   com.yourapp.experience – projects and stories.
   com.yourapp.packages – packages and tokenized share links.
   com.yourapp.exports – resume/deck/cover letter generation.
   com.yourapp.ai – LLM client abstraction, prompts, RAG pipeline.
   com.yourapp.chat – recruiter chat sessions/messages.
   com.yourapp.linkedin – inbox assistant + profile analyzer.
   com.yourapp.billing – coins, transactions, usage limits.
   Within each domain package, follow layered architecture:
   controller (REST endpoints)
   service (business logic)
   repository (Spring Data JPA repositories)
   model or entity (JPA entities)
   dto + mappers (if you prefer DTOs over exposing entities).geeksforgeeks+1[youtube]​
   2.2 Key entities (simplified)
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
   (Claude can later normalize/narrow this.)

2.3 Main REST API surfaces (high level)
Use /api/v1/... for all JSON endpoints.
Auth & tenant
POST /api/v1/auth/register
POST /api/v1/auth/login
GET /api/v1/auth/me
Profile & timeline
GET/PUT /api/v1/profile
GET/POST/PUT/DELETE /api/v1/profile/jobs
GET/POST/PUT/DELETE /api/v1/profile/education
Skills & stories
GET/POST/PUT/DELETE /api/v1/skills (catalog, admin‑ish)
GET/POST/PUT/DELETE /api/v1/profile/skills
GET/POST/PUT/DELETE /api/v1/experience/projects
GET/POST/PUT/DELETE /api/v1/experience/stories
Packages & public access
GET/POST/PUT/DELETE /api/v1/packages
POST /api/v1/packages/{id}/share-token
Public/anonymous endpoints:
GET /public/profile/{profileSlug}
GET /public/package/{token}
Exports
GET /api/v1/exports/templates
POST /api/v1/exports/resume
POST /api/v1/exports/background-presentation
POST /api/v1/exports/cover-letter
GET /api/v1/exports/{id} (download meta or signed URL)
Recruiter chat
POST /public/profile/{profileSlug}/chat
POST /public/package/{token}/chat
(Optional) GET /api/v1/chat/sessions for owner to review.
LinkedIn Inbox Assistant
MVP (manual input):
POST /api/v1/linkedin/drafts – send incoming message snippet + context, get draft reply.
GET /api/v1/linkedin/drafts – history and coin usage.
LinkedIn Profile Score Analyzer
POST /api/v1/linkedin/analyze – upload PDF or submit URL; returns analysis id + scores.
GET /api/v1/linkedin/analysis/{id} – fetch result.
POST /api/v1/linkedin/analysis/{id}/section/{sectionKey}/suggest – generate suggestions for a section (first free, next cost coins).
Coins & billing
GET /api/v1/billing/wallet
GET /api/v1/billing/transactions
POST /api/v1/billing/checkout-session (future Stripe integration).
2.4 AI integration pattern
Implement an interface such as:
java
public interface LlmClient {
LlmResponse complete(LlmRequest request);
}


Have implementations:
OpenAiLlmClient, GeminiLlmClient, ClaudeLlmClient etc.
A LlmRouterService picks provider/model based on:
Action type (resume, recruiter chat, LinkedIn analysis).
Tenant settings in LLMProviderConfig.ijfmr+3
Prompt templates and RAG pipelines live in dedicated services, e.g.:
RecruiterChatService – builds prompt, does RAG, calls LlmClient.
ExportPromptService – prompts for resume/cover/presentation.
LinkedInAnalysisService – parses LinkedIn content and prompts for scoring + suggestions.

3. Frontend structure (React)
   3.1 App layout
   src/
   app/
   App.tsx – routing and global layout.
   routes.tsx – route definitions.
   pages/
   DashboardPage
   ProfileEditorPage (tabs: About, Jobs, Education, Skills, Stories)
   PackagesPage and PackageEditorPage
   ExportsPage
   LinkedInAssistantPage (Inbox drafts + Profile analyzer tabs)
   BillingPage (coins, usage)
   Public pages:
   PublicProfilePage
   PublicPackagePage
   components/
   Generic: Form, TextField, TagInput, SkillChip, StoryCard, ChatBox, CoinBalanceBadge.
   Domain: JobForm, EducationForm, ProjectForm, StoryForm, PackageSelector, LinkedInSectionScoreCard.
   api/
   authApi.ts, profileApi.ts, skillsApi.ts, experienceApi.ts, packageApi.ts, exportApi.ts, chatApi.ts, linkedinApi.ts, billingApi.ts.
   state/
   Global auth store, user/tenant context, maybe global config (current provider, feature flags).
   hooks/
   useProfile, useSkills, useStories, useChatSession, useCoins, etc.
   styles/ or whatever system you prefer.

3.2 Key UX flows
Profile completion flow
Landing in dashboard, showing completion percentage and quick links:
Add experience
Add at least X stories
Link skills to stories
Run LinkedIn Score Analyzer
Story builder
Wizard component:
Step 1: select job + project.
Step 2: STAR form fields.
Step 3: attach skills and metrics.
Step 4: set visibility (public/private) and save.
Package builder
Multi‑select skills, stories, projects.
Option to “Preview package page” and generate share link.
Recruiter chat
Embed ChatBox in public pages:
Plain text input + streaming responses.
Optional “suggested questions” chips.
LinkedIn Profile Analyzer UI
Upload PDF or paste URL.
Show:
Overall score graph,
Cards for each section with score and short comment,
1 free suggestion per section, plus a “More suggestions (cost X coins)” button.

4. Cross‑cutting concerns
   Error handling and validation
   Use standard API error format (code, message, fieldErrors).
   On frontend, generic ErrorBoundary and toast notifications.
   Security
   CORS configured to allow your React origin in dev.
   Role checks on all mutating endpoints; public endpoints careful to only use public/package data.suridata+3
   Observability & metrics
   Basic logging for:
   LLM calls (action type, provider, latency, success/failure).
   Coin spends per tenant and feature.
   Errors on export generation and LinkedIn analysis.
