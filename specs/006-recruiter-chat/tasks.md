# Implementation Tasks: Recruiter Chat (RAG + LLM)

**Feature ID:** 006-recruiter-chat
**Branch:** 006-recruiter-chat
**Spec:** [spec.md](./spec.md)
**Plan:** [plan.md](./plan.md)

---

## Legend
- `[ ]` Not Started
- `[P]` In Progress
- `[X]` Completed
- `[B]` Blocked (add blocker note)
- `[S]` Skipped (add reason)
- `[||]` Can run in parallel (different files, no dependencies)
- `[US-N]` Belongs to User Story N from spec.md

---

## Phase 1: Database Schema

### Liquibase Migrations

- [ ] T001 [DATABASE] Enable pgvector extension migration
  - File: `backend/src/main/resources/db/changelog/20260224160000-enable-pgvector-extension.xml`
  - ChangeSet: `CREATE EXTENSION IF NOT EXISTS vector`
  - Use `<sql>` tag since Liquibase has no native pgvector support
  - Include rollback: `DROP EXTENSION IF EXISTS vector`
  - Precondition: PostgreSQL with pgvector installed

- [ ] T002 [DATABASE] Create chat_session table migration
  - File: `backend/src/main/resources/db/changelog/20260224160100-create-chat-session-table.xml`
  - Create `chat_session` table: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), profile_id (BIGINT FK NOT NULL), session_token (UUID NOT NULL), status (VARCHAR 20 NOT NULL DEFAULT 'ACTIVE'), started_at (TIMESTAMPTZ NOT NULL DEFAULT NOW()), last_message_at (TIMESTAMPTZ), message_count (INTEGER NOT NULL DEFAULT 0), metadata (JSONB), created_at (TIMESTAMPTZ NOT NULL DEFAULT NOW()), updated_at (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - Add foreign keys: tenant_id -> tenant(id), profile_id -> profile(id)
  - Add unique index: idx_chat_session_token (session_token)
  - Add indexes: idx_chat_session_tenant_id, idx_chat_session_profile_id, idx_chat_session_status
  - Include rollback: dropTable chat_session

- [ ] T003 [DATABASE] Create chat_message table migration
  - File: `backend/src/main/resources/db/changelog/20260224160200-create-chat-message-table.xml`
  - Create `chat_message` table: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), session_id (BIGINT FK NOT NULL), role (VARCHAR 20 NOT NULL), content (TEXT NOT NULL), status (VARCHAR 20 NOT NULL DEFAULT 'SENT'), tokens_used (INTEGER), llm_provider (VARCHAR 50), llm_model (VARCHAR 100), latency_ms (INTEGER), created_at (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - Add foreign keys: tenant_id -> tenant(id), session_id -> chat_session(id)
  - Add indexes: idx_chat_message_tenant_id, idx_chat_message_session_id, idx_chat_message_created_at
  - Include rollback: dropTable chat_message

- [ ] T004 [DATABASE] Create content_embedding table migration
  - File: `backend/src/main/resources/db/changelog/20260224160300-create-content-embedding-table.xml`
  - Create `content_embedding` table: id (BIGSERIAL PK), tenant_id (BIGINT FK NOT NULL), content_type (VARCHAR 20 NOT NULL), content_id (BIGINT NOT NULL), content_text (TEXT NOT NULL), embedding (vector(1536) NOT NULL), created_at (TIMESTAMPTZ NOT NULL DEFAULT NOW()), updated_at (TIMESTAMPTZ NOT NULL DEFAULT NOW())
  - Add foreign key: tenant_id -> tenant(id)
  - Add unique index: idx_content_embedding_type_id (tenant_id, content_type, content_id)
  - Add index: idx_content_embedding_tenant_id
  - Add HNSW index: idx_content_embedding_hnsw on embedding column using `vector_cosine_ops` with m=16, ef_construction=64
  - Use `<sql>` for HNSW index creation (Liquibase native does not support HNSW)
  - Include rollback: dropTable content_embedding

- [ ] T005 [DATABASE] Add migrations to master changelog
  - File: `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
  - Include: `db/changelog/20260224160000-enable-pgvector-extension.xml`
  - Include: `db/changelog/20260224160100-create-chat-session-table.xml`
  - Include: `db/changelog/20260224160200-create-chat-message-table.xml`
  - Include: `db/changelog/20260224160300-create-content-embedding-table.xml`

---

## Phase 2: Enums and Configuration

- [ ] T006 [||] [JAVA25] Create ContentType enum
  - File: `backend/src/main/java/com/interviewme/ai/model/ContentType.java`
  - Values: SKILL, STORY, PROJECT, JOB

- [ ] T007 [||] [JAVA25] Create LlmProvider enum
  - File: `backend/src/main/java/com/interviewme/ai/model/LlmProvider.java`
  - Values: OPENAI, GEMINI, CLAUDE

- [ ] T008 [||] [JAVA25] Create ChatSessionStatus enum
  - File: `backend/src/main/java/com/interviewme/chat/model/ChatSessionStatus.java`
  - Values: ACTIVE, EXPIRED, CLOSED

- [ ] T009 [||] [JAVA25] Create ChatMessageStatus enum
  - File: `backend/src/main/java/com/interviewme/chat/model/ChatMessageStatus.java`
  - Values: SENT, DELIVERED, FAILED

- [ ] T010 [||] [JAVA25] Create ChatMessageRole enum
  - File: `backend/src/main/java/com/interviewme/chat/model/ChatMessageRole.java`
  - Values: USER, ASSISTANT, SYSTEM

- [ ] T011 [||] [SIMPLE] Create AiProperties configuration class
  - File: `backend/src/main/java/com/interviewme/ai/config/AiProperties.java`
  - Use @ConfigurationProperties(prefix = "ai")
  - Properties: defaultProvider (String), openai (OpenAiConfig), gemini (GeminiConfig), claude (ClaudeConfig), chat (ChatConfig), embedding (EmbeddingConfig)
  - Inner class OpenAiConfig: apiKey, chatModel, embeddingModel, baseUrl
  - Inner class GeminiConfig: apiKey, chatModel, baseUrl
  - Inner class ClaudeConfig: apiKey, chatModel, baseUrl
  - Inner class ChatConfig: maxTokens (500), temperature (0.3), maxContextMessages (10), rateLimitPerMinute (5), sessionExpiryHours (24)
  - Inner class EmbeddingConfig: dimension (1536), similarityThreshold (0.3), topK (5)

- [ ] T012 [SIMPLE] Add AI configuration to application.yml
  - File: `backend/src/main/resources/application.yml`
  - Add `ai:` section with default-provider, openai, gemini, claude, chat, and embedding sub-sections
  - API keys via environment variables: ${OPENAI_API_KEY:}, ${GEMINI_API_KEY:}, ${CLAUDE_API_KEY:}

---

## Phase 3: JPA Entities

- [ ] T013 [||] [JAVA25] Create ChatSession entity
  - File: `backend/src/main/java/com/interviewme/chat/model/ChatSession.java`
  - Annotations: @Entity, @Table(name = "chat_session"), @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id (Long, BIGSERIAL), tenantId (Long), profileId (Long), sessionToken (UUID), status (ChatSessionStatus, @Enumerated STRING), startedAt (Instant), lastMessageAt (Instant), messageCount (int), metadata (Map<String,Object>, @JdbcTypeCode JSONB), createdAt (Instant), updatedAt (Instant)
  - Relationship: @OneToMany with ChatMessage (lazy)
  - Lombok: @Data, @NoArgsConstructor, @AllArgsConstructor
  - @PrePersist/@PreUpdate for timestamps

- [ ] T014 [||] [JAVA25] Create ChatMessage entity
  - File: `backend/src/main/java/com/interviewme/chat/model/ChatMessage.java`
  - Annotations: @Entity, @Table(name = "chat_message"), @FilterDef, @Filter(name = "tenantFilter")
  - Fields: id (Long), tenantId (Long), sessionId (Long), role (ChatMessageRole, @Enumerated STRING), content (String TEXT), status (ChatMessageStatus, @Enumerated STRING), tokensUsed (Integer), llmProvider (String), llmModel (String), latencyMs (Integer), createdAt (Instant)
  - Relationship: @ManyToOne with ChatSession
  - Lombok: @Data, @NoArgsConstructor, @AllArgsConstructor
  - @PrePersist for createdAt

- [ ] T015 [||] [JAVA25] Create ContentEmbedding entity
  - File: `backend/src/main/java/com/interviewme/ai/model/ContentEmbedding.java`
  - Annotations: @Entity, @Table(name = "content_embedding", uniqueConstraints = @UniqueConstraint(columns = {"tenant_id", "content_type", "content_id"}))
  - Fields: id (Long), tenantId (Long), contentType (ContentType, @Enumerated STRING), contentId (Long), contentText (String TEXT), embedding (float[] or pgvector type), createdAt (Instant), updatedAt (Instant)
  - Note: pgvector column mapping may require custom Hibernate type or `@Column(columnDefinition = "vector(1536)")` with String representation
  - Lombok: @Data, @NoArgsConstructor, @AllArgsConstructor
  - @PrePersist/@PreUpdate for timestamps

---

## Phase 4: DTOs

- [ ] T016 [||] [JAVA25] Create chat DTOs
  - Files in `backend/src/main/java/com/interviewme/chat/dto/`:
  - `ChatRequest.java`: record(@NotBlank @Size(max = 500) String message, UUID sessionToken)
  - `ChatResponse.java`: record(String message, UUID sessionToken, Long messageId, QuotaInfo quotaInfo)
  - `QuotaInfo.java`: record(int freeRemaining, int freeLimit, boolean usingCoins)
  - `ChatAnalyticsResponse.java`: record(long totalSessions, long totalMessages, long sessionsThisMonth, long messagesThisMonth)
  - `ChatErrorResponse.java`: record(String code, String message)

- [ ] T017 [||] [JAVA25] Create AI client DTOs
  - Files in `backend/src/main/java/com/interviewme/ai/client/`:
  - `LlmRequest.java`: record(String systemPrompt, List<LlmChatMessage> messages, int maxTokens, double temperature)
  - `LlmResponse.java`: record(String content, int tokensUsed, String provider, String model, long latencyMs)
  - `LlmChatMessage.java`: record(String role, String content)

---

## Phase 5: Repositories

- [ ] T018 [||] [DATA] Create ChatSessionRepository
  - File: `backend/src/main/java/com/interviewme/chat/repository/ChatSessionRepository.java`
  - Extend: JpaRepository<ChatSession, Long>
  - Query methods:
    - `Optional<ChatSession> findBySessionTokenAndStatus(UUID sessionToken, ChatSessionStatus status)`
    - `long countByProfileIdAndCreatedAtAfter(Long profileId, Instant after)` (for analytics)
    - `long countByTenantIdAndCreatedAtBetween(Long tenantId, Instant start, Instant end)`

- [ ] T019 [||] [DATA] Create ChatMessageRepository
  - File: `backend/src/main/java/com/interviewme/chat/repository/ChatMessageRepository.java`
  - Extend: JpaRepository<ChatMessage, Long>
  - Query methods:
    - `List<ChatMessage> findTop10BySessionIdOrderByCreatedAtDesc(Long sessionId)` (conversation context)
    - `long countBySessionIdAndRoleAndCreatedAtAfter(Long sessionId, ChatMessageRole role, Instant after)` (rate limiting)
    - `long countByTenantIdAndCreatedAtBetween(Long tenantId, Instant start, Instant end)` (analytics)

- [ ] T020 [||] [DATA] Create ContentEmbeddingRepository
  - File: `backend/src/main/java/com/interviewme/ai/repository/ContentEmbeddingRepository.java`
  - Extend: JpaRepository<ContentEmbedding, Long>
  - Query methods:
    - `Optional<ContentEmbedding> findByTenantIdAndContentTypeAndContentId(Long tenantId, ContentType contentType, Long contentId)`
    - `void deleteByTenantIdAndContentTypeAndContentId(Long tenantId, ContentType contentType, Long contentId)`
    - `void deleteByTenantIdAndContentType(Long tenantId, ContentType contentType)`
    - `@Query(nativeQuery = true)` pgvector similarity search: `findTopKBySimilarity(Long tenantId, String embedding, int topK, double threshold)` -- returns List<ContentEmbedding> ordered by cosine similarity

---

## Phase 6: Custom Exceptions

- [ ] T021 [||] [SIMPLE] Create chat custom exceptions
  - Files in `backend/src/main/java/com/interviewme/chat/exception/`:
  - `ChatQuotaExceededException.java`: extends RuntimeException, field: tenantId (Long)
  - `ChatRateLimitException.java`: extends RuntimeException, field: retryAfterSeconds (int)
  - File in `backend/src/main/java/com/interviewme/ai/exception/`:
  - `LlmUnavailableException.java`: extends RuntimeException, wraps original exception

- [ ] T022 [SIMPLE] Add chat exception handlers to GlobalExceptionHandler
  - File: `backend/src/main/java/com/interviewme/config/GlobalExceptionHandler.java`
  - Add handler: `@ExceptionHandler(ChatQuotaExceededException.class)` -> 402 Payment Required with `{"code": "CHAT_QUOTA_EXCEEDED", "message": "..."}`
  - Add handler: `@ExceptionHandler(ChatRateLimitException.class)` -> 429 Too Many Requests with Retry-After header
  - Add handler: `@ExceptionHandler(LlmUnavailableException.class)` -> 503 Service Unavailable with `{"code": "LLM_UNAVAILABLE", "message": "..."}`

---

## Phase 7: Domain Events

- [ ] T023 [SIMPLE] Create ChatMessageEvent
  - File: `backend/src/main/java/com/interviewme/chat/event/ChatMessageEvent.java`
  - Java record: ChatMessageEvent(Long tenantId, Long sessionId, Long messageId, String llmProvider, int tokensUsed, Instant timestamp)

---

## Phase 8: LLM Client Interface and Implementations

### LLM Client Interface

- [ ] T024 [JAVA25] Create LlmClient interface
  - File: `backend/src/main/java/com/interviewme/ai/client/LlmClient.java`
  - Methods: `LlmResponse complete(LlmRequest request)`, `String getModel()`, `String getProvider()`

### Embedding Client Interface

- [ ] T025 [JAVA25] Create EmbeddingClient interface
  - File: `backend/src/main/java/com/interviewme/ai/embedding/EmbeddingClient.java`
  - Methods: `float[] embed(String text)`, `List<float[]> embedBatch(List<String> texts)`

### LLM Implementations

- [ ] T026 [||] [MODULAR] Create OpenAiLlmClient
  - File: `backend/src/main/java/com/interviewme/ai/client/OpenAiLlmClient.java`
  - Annotations: @Component("openai"), @RequiredArgsConstructor, @Slf4j
  - Use RestClient to call OpenAI Chat Completions API (POST /v1/chat/completions)
  - Parse response: extract content, token usage (prompt_tokens + completion_tokens)
  - Retry: @Retryable for 429 and 5xx errors, 3 attempts, exponential backoff (500ms, 1s, 2s)
  - Timeout: 30 seconds
  - Read API key and model from AiProperties.openai

- [ ] T027 [||] [MODULAR] Create GeminiLlmClient
  - File: `backend/src/main/java/com/interviewme/ai/client/GeminiLlmClient.java`
  - Annotations: @Component("gemini"), @RequiredArgsConstructor, @Slf4j
  - Use RestClient to call Gemini generateContent API
  - Map LlmRequest messages to Gemini format (contents array with parts)
  - Parse response: extract text from candidates[0].content.parts[0].text
  - Extract token count from usageMetadata
  - Same retry and timeout as OpenAI

- [ ] T028 [||] [MODULAR] Create ClaudeLlmClient
  - File: `backend/src/main/java/com/interviewme/ai/client/ClaudeLlmClient.java`
  - Annotations: @Component("claude"), @RequiredArgsConstructor, @Slf4j
  - Use RestClient to call Anthropic Messages API (POST /v1/messages)
  - Set headers: x-api-key, anthropic-version
  - Map LlmRequest to Anthropic format (system prompt separate, messages array)
  - Parse response: extract content[0].text, usage.input_tokens + usage.output_tokens
  - Same retry and timeout as OpenAI

### Embedding Implementation

- [ ] T029 [MODULAR] Create OpenAiEmbeddingClient
  - File: `backend/src/main/java/com/interviewme/ai/embedding/OpenAiEmbeddingClient.java`
  - Annotations: @Component, @RequiredArgsConstructor, @Slf4j
  - Use RestClient to call OpenAI Embeddings API (POST /v1/embeddings)
  - Model: text-embedding-3-small (from AiProperties.openai.embeddingModel)
  - Parse response: extract embedding array from data[0].embedding
  - `embed(String text)`: single text -> single embedding
  - `embedBatch(List<String> texts)`: batch request for multiple texts
  - Timeout: 10 seconds

---

## Phase 9: AI Services

### LlmRouterService

- [ ] T030 [MODULAR] Create LlmRouterService
  - File: `backend/src/main/java/com/interviewme/ai/service/LlmRouterService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: Map<String, LlmClient> clients (Spring auto-injects by qualifier name), AiProperties
  - Method: `LlmResponse complete(Long tenantId, String systemPrompt, List<LlmChatMessage> history)`:
    1. Read defaultProvider from AiProperties
    2. Look up LlmClient by provider name in clients map
    3. Build LlmRequest with systemPrompt, history, maxTokens, temperature from config
    4. Call client.complete(request)
    5. Log: provider, model, tenantId, tokensUsed, latencyMs
    6. Return LlmResponse
  - Phase 1: global provider only (no per-tenant routing)

### EmbeddingService

- [ ] T031 [MODULAR] Create EmbeddingService
  - File: `backend/src/main/java/com/interviewme/ai/service/EmbeddingService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: EmbeddingClient, ContentEmbeddingRepository
  - Method: `@Transactional void generateEmbedding(Long tenantId, ContentType type, Long contentId, String contentText)` - generate embedding via EmbeddingClient, upsert into content_embedding table
  - Method: `@Transactional void deleteEmbedding(Long tenantId, ContentType type, Long contentId)` - remove embedding when content is deleted or made private
  - Method: `@Async @Transactional void refreshAllEmbeddings(Long tenantId)` - re-embed all public content for a tenant (used when major changes occur)
  - Logging: log.info for each embedding operation with contentType, contentId

### RagService

- [ ] T032 [MODULAR] Create RagService
  - File: `backend/src/main/java/com/interviewme/ai/service/RagService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: EmbeddingClient, ContentEmbeddingRepository, AiProperties
  - Method: `@Transactional(readOnly = true) String retrieveContext(Long tenantId, String question)`:
    1. Call embeddingClient.embed(question) to get question embedding
    2. Convert float[] to pgvector string format: "[0.1,0.2,...]"
    3. Call contentEmbeddingRepository.findTopKBySimilarity(tenantId, embeddingStr, topK, threshold)
    4. Format results into structured text with content type headers
    5. Return formatted context (or "No specific information available." if empty)
  - Log: number of results retrieved, top similarity score

---

## Phase 10: Content Change Event Listener

- [ ] T033 [MODULAR] Create ContentChangedEventListener
  - File: `backend/src/main/java/com/interviewme/ai/event/ContentChangedEventListener.java`
  - Annotations: @Component, @RequiredArgsConstructor, @Slf4j
  - Dependencies: EmbeddingService
  - Listen for domain events from Features 002-004 when content is created/updated/deleted
  - Phase 1: Manual integration -- other features call EmbeddingService directly when modifying public content
  - Phase 2: Full event-driven integration with @EventListener
  - Content text formatting methods:
    - `formatSkillText(UserSkill skill)` - "{name} - Category: {category} - {years} years - Proficiency: {depth}/5"
    - `formatStoryText(Story story)` - "{title}. Situation: {situation}. Task: {task}. Action: {action}. Result: {result}"
    - `formatProjectText(ExperienceProject project)` - "{title} at {company}. {context}. Technologies: {techStack}. Outcomes: {outcomes}"
    - `formatJobText(JobExperience job)` - "{company} - {role} ({startDate} to {endDate}). Key achievements: {achievements}"

---

## Phase 11: Chat Service (Orchestrator)

- [ ] T034 [MODULAR] Create ChatService
  - File: `backend/src/main/java/com/interviewme/chat/service/ChatService.java`
  - Annotations: @Service, @RequiredArgsConstructor, @Slf4j
  - Dependencies: ChatSessionRepository, ChatMessageRepository, ProfileRepository, RagService, LlmRouterService, FreeTierService, CoinWalletService, BillingProperties, AiProperties, ApplicationEventPublisher
  - Method: `@Transactional ChatResponse processMessage(String slug, ChatRequest request)`:
    1. Resolve profile by slug (throw PublicProfileNotFoundException if not found)
    2. Get or create ChatSession (by sessionToken or create new)
    3. Enforce rate limit (5 msgs/min per session using messageRepository count query)
    4. Billing: tryConsumeFreeTier -> if false, coinWalletService.spend()
    5. Save user ChatMessage with status=DELIVERED
    6. Call ragService.retrieveContext() for relevant content
    7. Build conversation history from last 10 messages
    8. Build system prompt with profile name/headline + retrieved context
    9. Call llmRouterService.complete()
    10. Save assistant ChatMessage with LLM metadata (tokens, provider, model, latency)
    11. Update session (lastMessageAt, messageCount)
    12. Publish ChatMessageEvent
    13. Build QuotaInfo from FreeTierService
    14. Return ChatResponse
  - Error handling: catch LLM exceptions, refund coins if spent, save FAILED message, throw LlmUnavailableException
  - Method: `@Transactional(readOnly = true) ChatAnalyticsResponse getAnalytics(Long tenantId)` - aggregate counts from repositories
  - Private helper: `getOrCreateSession(Long tenantId, Long profileId, UUID sessionToken)` - find by token or create new with UUID
  - Private helper: `enforceRateLimit(ChatSession session)` - count recent USER messages
  - Private helper: `buildConversationHistory(ChatSession session)` - fetch last 10 messages, map to LlmChatMessage records
  - Private helper: `buildSystemPrompt(Profile profile, String retrievedContext)` - format system prompt template
  - Private helper: `buildQuotaInfo(Long tenantId)` - get free tier status for CHAT_MESSAGE

---

## Phase 12: Controllers

### Public Chat Controller

- [ ] T035 [MODULAR] Create PublicChatController
  - File: `backend/src/main/java/com/interviewme/chat/controller/PublicChatController.java`
  - Annotations: @RestController, @RequestMapping("/api/public/chat"), @RequiredArgsConstructor, @Slf4j
  - Dependencies: ChatService
  - Endpoint: `@PostMapping("/{slug}/messages")` -> processMessage(@PathVariable String slug, @Valid @RequestBody ChatRequest request) -> 200 OK ChatResponse
  - No authentication required (public endpoint)
  - Log: slug, sessionToken (masked), message length
  - Input validation: @Valid on ChatRequest, @Size(max=500) on message

### Chat Analytics Controller

- [ ] T036 [MODULAR] Create ChatAnalyticsController
  - File: `backend/src/main/java/com/interviewme/chat/controller/ChatAnalyticsController.java`
  - Annotations: @RestController, @RequestMapping("/api/chat"), @RequiredArgsConstructor, @Slf4j
  - Dependencies: ChatService
  - Endpoint: `@GetMapping("/analytics")` @Transactional(readOnly = true) -> getAnalytics(Authentication auth) -> 200 OK ChatAnalyticsResponse
  - JWT authentication required, tenant from TenantContext

---

## Phase 13: Spring Security Update

- [ ] T037 [SIMPLE] Update Spring Security to permit chat endpoints
  - File: `backend/src/main/java/com/interviewme/config/SecurityConfig.java` (or equivalent)
  - Add: `.requestMatchers("/api/public/chat/**").permitAll()` to the SecurityFilterChain
  - Verify: `/api/public/chat/{slug}/messages` accessible without JWT
  - Verify: `/api/chat/analytics` still requires JWT

---

## Phase 14: Frontend - TypeScript Types & API Client

### TypeScript Types

- [ ] T038 [||] [SIMPLE] Create chat TypeScript interfaces
  - File: `frontend/src/types/chat.ts`
  - Interfaces: ChatRequest (message, sessionToken), ChatResponse (message, sessionToken, messageId, quotaInfo), QuotaInfo (freeRemaining, freeLimit, usingCoins), ChatMessage (id, role, content, timestamp, status), ChatAnalyticsResponse
  - Type union: ChatMessageRole = 'user' | 'assistant' | 'system'

### API Client

- [ ] T039 [MODULAR] Create Chat API client
  - File: `frontend/src/api/chatApi.ts`
  - Import: Axios (no JWT interceptor needed for public endpoint)
  - Functions:
    - `sendMessage(slug: string, message: string, sessionToken: string | null): Promise<ChatResponse>` - POST /api/public/chat/{slug}/messages
    - `getAnalytics(): Promise<ChatAnalyticsResponse>` - GET /api/chat/analytics (with JWT)
  - Error handling: map 402 -> quota exceeded, 429 -> rate limited, 503 -> LLM unavailable

---

## Phase 15: Frontend - Chat Hook

- [ ] T040 [MODULAR] Create useChat hook
  - File: `frontend/src/hooks/useChat.ts`
  - State: messages (ChatMessage[]), isLoading, error, isOpen, sessionToken, quotaInfo
  - Session token: persist in localStorage keyed by slug (`chat_session_{slug}`)
  - `sendMessage(text: string)`:
    1. Add user message to messages array optimistically
    2. Set isLoading = true
    3. Call chatApi.sendMessage(slug, text, sessionToken)
    4. On success: add assistant message, update sessionToken, update quotaInfo
    5. On error: set error message, handle 402/429/503 specifically
    6. Set isLoading = false
  - `toggle()`: toggle isOpen, add greeting message on first open
  - Initial greeting message (static, not an API call): "Hi! I'm {name}'s career assistant. Ask me anything about their experience, skills, and projects."

---

## Phase 16: Frontend - React Components

### Chat Components

- [ ] T041 [MODULAR] Create ChatWidget
  - File: `frontend/src/components/chat/ChatWidget.tsx`
  - Props: slug (string), profileName (string)
  - Renders: FAB (MUI Fab with ChatIcon) in bottom-right corner, position: fixed
  - On click: toggle chat panel visibility
  - When open: render ChatPanel
  - Responsive: FAB visible on all screen sizes

- [ ] T042 [MODULAR] Create ChatPanel
  - File: `frontend/src/components/chat/ChatPanel.tsx`
  - Props: slug, profileName, onClose
  - Layout: header (title + close button), scrollable message area, ChatQuotaWarning, ChatInput
  - Use useChat hook for state management
  - Desktop: fixed position panel (400px wide, 500px tall, bottom-right)
  - Mobile: full-screen overlay
  - Auto-scroll to bottom on new message

- [ ] T043 [||] [MODULAR] Create ChatMessageBubble
  - File: `frontend/src/components/chat/ChatMessageBubble.tsx`
  - Props: message (ChatMessage)
  - User messages: right-aligned, blue background, white text
  - Assistant messages: left-aligned, gray background, dark text
  - Support markdown rendering for assistant messages (bold, lists, code blocks)
  - Display relative timestamp ("2 min ago")
  - Error state: red background with "Retry" link
  - Use MUI Paper/Box for styling

- [ ] T044 [||] [SIMPLE] Create ChatTypingIndicator
  - File: `frontend/src/components/chat/ChatTypingIndicator.tsx`
  - Animated three-dot indicator (CSS animation)
  - Displayed when isLoading is true
  - Left-aligned (same position as assistant messages)

- [ ] T045 [||] [MODULAR] Create ChatInput
  - File: `frontend/src/components/chat/ChatInput.tsx`
  - Props: onSend (callback), disabled (boolean), placeholder (string)
  - MUI TextField with send IconButton
  - Submit on Enter key (not Shift+Enter which adds newline)
  - Disable when isLoading or quota exceeded
  - Max length: 500 characters with counter

- [ ] T046 [||] [SIMPLE] Create ChatQuotaWarning
  - File: `frontend/src/components/chat/ChatQuotaWarning.tsx`
  - Props: quotaInfo (QuotaInfo | null)
  - When freeRemaining > 0 and < 20% of limit: "X free messages remaining this month"
  - When usingCoins: "Using coins (1 coin/message)"
  - When quota exceeded and no coins: "Chat quota reached. Contact the profile owner." + disable chat

### Integration with Public Profile

- [ ] T047 [SIMPLE] Add ChatWidget to PublicProfilePage
  - File: `frontend/src/pages/PublicProfilePage.tsx` (or equivalent from Feature 005)
  - Import ChatWidget component
  - Render `<ChatWidget slug={slug} profileName={profile.fullName} />` at the bottom of the page
  - ChatWidget renders outside the main content flow (fixed position)

---

## Phase 17: Testing

### Backend Unit Tests

- [ ] T048 [||] [JAVA25] Write ChatService unit tests
  - File: `backend/src/test/java/com/interviewme/chat/service/ChatServiceTest.java`
  - Use @ExtendWith(MockitoExtension.class), mock all dependencies
  - Test: processMessage_shouldCreateSessionAndReturnResponse
  - Test: processMessage_shouldReuseExistingSession
  - Test: processMessage_shouldEnforceRateLimit
  - Test: processMessage_shouldUseFreeQuotaFirst
  - Test: processMessage_shouldSpendCoinsAfterFreeQuota
  - Test: processMessage_shouldThrowQuotaExceededWhenNoCoins
  - Test: processMessage_shouldRefundCoinsOnLlmFailure
  - Test: processMessage_shouldStoreBothUserAndAssistantMessages
  - Test: processMessage_shouldBuildConversationHistory

- [ ] T049 [||] [JAVA25] Write RagService unit tests
  - File: `backend/src/test/java/com/interviewme/ai/service/RagServiceTest.java`
  - Test: retrieveContext_shouldReturnFormattedContext
  - Test: retrieveContext_shouldReturnDefaultMessageWhenNoResults
  - Test: retrieveContext_shouldRespectSimilarityThreshold
  - Test: retrieveContext_shouldLimitToTopK

- [ ] T050 [||] [JAVA25] Write EmbeddingService unit tests
  - File: `backend/src/test/java/com/interviewme/ai/service/EmbeddingServiceTest.java`
  - Test: generateEmbedding_shouldCreateNewRecord
  - Test: generateEmbedding_shouldUpdateExistingRecord
  - Test: deleteEmbedding_shouldRemoveRecord

- [ ] T051 [||] [JAVA25] Write LlmRouterService unit tests
  - File: `backend/src/test/java/com/interviewme/ai/service/LlmRouterServiceTest.java`
  - Test: complete_shouldRouteToConfiguredProvider
  - Test: complete_shouldThrowIfProviderNotFound
  - Test: complete_shouldLogProviderAndTokens

### Backend Integration Tests

- [ ] T052 [SIMPLE] Write PublicChatController integration tests
  - File: `backend/src/test/java/com/interviewme/chat/controller/PublicChatControllerIntegrationTest.java`
  - Use @SpringBootTest, @AutoConfigureMockMvc
  - Mock LlmClient and EmbeddingClient (external API calls)
  - Test: POST /api/public/chat/{slug}/messages without auth -> 200 OK with response
  - Test: POST /api/public/chat/{slug}/messages with invalid slug -> 404
  - Test: POST /api/public/chat/{slug}/messages with empty message -> 400
  - Test: POST /api/public/chat/{slug}/messages with message > 500 chars -> 400
  - Test: Session token in response matches localStorage persistence flow
  - Test: Rate limiting returns 429 after 5 messages in 1 minute

- [ ] T053 [SIMPLE] Write ContentEmbeddingRepository integration test
  - File: `backend/src/test/java/com/interviewme/ai/repository/ContentEmbeddingRepositoryTest.java`
  - Requires Testcontainers with PostgreSQL + pgvector extension
  - Test: save embedding and retrieve by similarity search
  - Test: similarity threshold filters out low-relevance results
  - Test: top-K limit works correctly
  - Test: unique constraint on (tenant_id, content_type, content_id)

### Privacy Enforcement Tests

- [ ] T054 [MODULAR] Write privacy enforcement tests
  - File: `backend/src/test/java/com/interviewme/chat/ChatPrivacyTest.java`
  - Setup: profile with public story "K8s migration" and private story "salary negotiation"
  - Generate embeddings for public content only
  - Test: verify content_embedding table has no records with private content IDs
  - Test: ask about "salary negotiation" -> response says "I don't have information about that"
  - Test: ask about "Kubernetes" -> response references the public K8s story

### Frontend Tests

- [ ] T055 [||] [SIMPLE] Write ChatWidget tests
  - File: `frontend/src/components/chat/ChatWidget.test.tsx`
  - Test: should render FAB button
  - Test: should open ChatPanel on FAB click
  - Test: should close ChatPanel on close button click

- [ ] T056 [||] [SIMPLE] Write ChatPanel tests
  - File: `frontend/src/components/chat/ChatPanel.test.tsx`
  - Test: should display greeting message on open
  - Test: should show typing indicator when loading
  - Test: should display user and assistant messages
  - Test: should show error message on failure

- [ ] T057 [||] [SIMPLE] Write ChatInput tests
  - File: `frontend/src/components/chat/ChatInput.test.tsx`
  - Test: should call onSend when Enter pressed
  - Test: should not submit empty message
  - Test: should disable when disabled prop is true

---

## Phase 18: Verification

- [ ] T058 [SIMPLE] Verify all migrations run successfully
  - Command: `./gradlew backend:bootRun`
  - Verify: pgvector extension enabled
  - Verify: chat_session, chat_message, content_embedding tables created
  - Verify: HNSW index created on embedding column
  - Verify: All indexes and foreign keys created

- [ ] T059 [SIMPLE] Run all backend tests
  - Command: `./gradlew backend:test`
  - Verify: All unit and integration tests pass
  - Verify: Privacy enforcement tests pass

- [ ] T060 [SIMPLE] Run all frontend tests
  - Command: `cd frontend && npm test`
  - Verify: All React tests pass

- [ ] T061 [SIMPLE] Manual E2E testing
  - Test: Open public profile, click chat FAB, chat panel opens
  - Test: Type question, see typing indicator, receive grounded response
  - Test: Ask follow-up question that references prior context
  - Test: Refresh page, session persists, previous messages loaded
  - Test: Send 5 messages rapidly, see rate limit error on 6th
  - Test: Verify response is based on public profile data (not hallucinated)
  - Test: Ask about something private, verify "I don't have information" response
  - Test: Exhaust free quota (50 messages), verify coins are charged for message 51
  - Test: When no coins available, verify 402 error and chat disabled
  - Test: Simulate LLM failure, verify error message and coin refund

---

## Checkpoints

**After Phase 1 (Database Schema):**
- [ ] pgvector extension enabled
- [ ] All 3 tables created with correct schema
- [ ] HNSW index on embedding column functional
- [ ] Unique constraint on content_embedding(tenant_id, content_type, content_id) enforced

**After Phase 9 (AI Services):**
- [ ] EmbeddingService generates and stores embeddings successfully
- [ ] RagService retrieves relevant content via pgvector similarity search
- [ ] LlmRouterService routes to configured provider and returns responses
- [ ] All 3 LLM client implementations work (with mocked API for testing)

**After Phase 11 (Chat Service):**
- [ ] Full chat flow works: question -> RAG -> LLM -> response
- [ ] Session management works (create, reuse, expire)
- [ ] Rate limiting enforced (5/min)
- [ ] Billing integration works (free tier + coin spend + refund on failure)
- [ ] Privacy enforcement: only public content in RAG context

**After Phase 16 (Frontend Components):**
- [ ] ChatWidget FAB renders on public profile page
- [ ] ChatPanel opens/closes correctly
- [ ] Messages display with correct styling (user/assistant)
- [ ] Typing indicator shows during LLM processing
- [ ] Quota warnings display correctly
- [ ] Session token persists in localStorage across page refreshes

**After Phase 17 (Testing):**
- [ ] All unit tests pass
- [ ] Integration tests pass (including pgvector repository test)
- [ ] Privacy enforcement tests pass
- [ ] Frontend tests pass
- [ ] E2E chat flow verified manually

---

## Progress Summary

**Total Tasks:** 61
**Completed:** 0
**In Progress:** 0
**Blocked:** 0
**Skipped:** 0
**Completion:** 0%

---

## Notes

### Key Dependencies
- Phase 1 (Migrations) must complete before Phase 3 (Entities)
- Phase 2 (Enums/Config) can run in parallel with Phase 1
- Phase 3 (Entities) must complete before Phase 5 (Repositories)
- Phase 4 (DTOs) can run in parallel with Phase 3
- Phase 5 (Repositories) must complete before Phase 9 (AI Services)
- Phase 8 (LLM Clients) must complete before Phase 9
- Phase 9 (AI Services) must complete before Phase 11 (Chat Service)
- Phase 11 (Chat Service) must complete before Phase 12 (Controllers)
- Phase 12 (Controllers) must complete before Phase 14 (Frontend)
- Phase 14-16 (Frontend) depend on backend API contract from Phase 12

### Parallel Execution Opportunities
- Within Phase 2: All 6 enums and config can be created in parallel
- Within Phase 3: All 3 entities can be written in parallel
- Within Phase 5: All 3 repositories can be written in parallel
- Phase 8: All 3 LLM clients can be written in parallel (after interface T024)
- Within Phase 16: ChatMessageBubble, ChatTypingIndicator, ChatInput, ChatQuotaWarning can be written in parallel
- Within Phase 17: All unit tests can be written in parallel

### External Dependencies
- **pgvector extension:** Must be installed in PostgreSQL. Use Docker image `pgvector/pgvector:pg18` for development
- **OpenAI API key:** Required for embedding generation (text-embedding-3-small) and default LLM provider
- **LLM API costs:** gpt-4o-mini costs ~$0.15/1M input + $0.60/1M output tokens. At 500 token responses, each chat message costs ~$0.001

### Risk Mitigation
- **pgvector availability:** Testcontainers with pgvector image for integration tests; skip tests if extension unavailable
- **LLM latency:** Use gpt-4o-mini (fastest); show typing indicator; future: SSE streaming
- **Embedding staleness:** Event listener pattern ensures embeddings refresh on content changes
- **Prompt injection:** System prompt includes anti-injection guardrail; input sanitized; max 500 chars
- **Cost control:** Free tier (50 msgs/month), coin billing, low temperature (0.3), max 500 output tokens

### Integration Points for Other Features
- **Feature 005 (Public Profile):** ChatWidget embedded on PublicProfilePage
- **Feature 007 (Billing):** Uses FreeTierService and CoinWalletService for quota and billing
- **Features 002-004:** Content changes trigger embedding refresh via event listener
- **Future Feature 008 (Resume Export):** Can reuse LlmClient and LlmRouterService
- **Future Feature 009 (LinkedIn Analyzer):** Can reuse AI package (LlmClient, EmbeddingService)

---

**Last Updated:** 2026-02-24
**Next Review:** After Phase 1 completion (database schema)
