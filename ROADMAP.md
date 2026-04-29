# Interview Me — Roadmap

## 1. Analytics & Tracking

**Objetivo:** Saber quem acessou os perfis publicos e como interagiu.

- [ ] Integrar **Google Analytics 4** (GA4) no frontend
  - Pageviews por perfil publico (`/p/:slug`)
  - Eventos customizados: inicio de chat, mensagens enviadas, tempo na pagina
  - Dashboard de metricas no GA4
- [ ] Integrar **Meta Pixel** (Facebook/Instagram Analytics)
  - Tracking de visitantes para remarketing futuro
  - Eventos de conversao (registro, inicio de chat)
- [ ] Dashboard interno de analytics (opcional, complementar ao GA4)
  - Metricas ja existentes no visitor tracking podem ser expandidas
  - Graficos de visitantes por periodo, origem, dispositivo

---

## 2. Integracao LinkedIn Avancada

**Objetivo:** Facilitar a importacao de perfil do LinkedIn para montar o perfil no Interview Me.

- [ ] Browser automation para captura de perfil LinkedIn
  - Usar Playwright/Puppeteer no backend para scraping do perfil publico
  - Extrair: experiencia, educacao, skills, certificacoes
  - Mapear dados do LinkedIn para o modelo do Interview Me
- [ ] Import via LinkedIn PDF export (alternativa mais simples)
  - Usuario faz download do PDF do perfil no LinkedIn
  - Upload no Interview Me, parsing automatico com AI
- [ ] Sync periodico (se viavel)
  - Detectar mudancas no perfil LinkedIn e sugerir atualizacoes
- [ ] LinkedIn OAuth login (complementar ao import)
  - Login via LinkedIn para profissionais
  - Puxar dados basicos do perfil na criacao da conta

---

## 3. Portal de Recrutadores

**Objetivo:** Criar uma experiencia dedicada para recrutadores e empresas.

- [ ] **Login de recrutador** (novo tipo de conta)
  - Registro com email corporativo
  - Perfil da empresa (nome, logo, setor, tamanho)
  - Planos/billing separado do profissional
- [ ] **Dashboard do recrutador**
  - Historico de perfis visitados e chats realizados
  - Favoritos / shortlist de candidatos
  - Notas internas por candidato
- [ ] **Busca de candidatos**
  - Pesquisa por skills, experiencia, localizacao, senioridade
  - Filtros avancados (anos de experiencia, stack, idiomas)
  - Ranking por relevancia (AI-powered)
- [ ] **Contato direto**
  - Enviar convite/mensagem ao profissional via plataforma
  - Profissional recebe notificacao por email

---

## 4. Visibilidade do Perfil (Searchable Flag)

**Objetivo:** Dar ao profissional controle sobre quem pode encontrar seu perfil.

- [ ] **Flag "Pesquisavel por recrutadores"** nas configuracoes do perfil
  - `searchable_by_recruiters: boolean` (default: false)
  - Se ativado, o perfil aparece na busca do portal de recrutadores
  - Se desativado, o perfil so e acessivel via link direto (`/p/:slug`)
- [ ] **Niveis de visibilidade** (evolucao futura)
  - `public` — qualquer pessoa com o link
  - `searchable` — aparece na busca de recrutadores
  - `private` — apenas via convite/link autenticado
- [ ] **Badge "Open to Work"** (inspirado no LinkedIn)
  - Profissional sinaliza que esta aberto a oportunidades
  - Destaque visual na busca de recrutadores

---

## 5. Portal de Empresas & Chatbot de Candidatos

**Objetivo:** Criar um lado B da plataforma — empresas publicam vagas e candidatos descobrem oportunidades via chatbot conversacional.

### 5.1 Cadastro de Empresas
- [ ] **Registro de empresa** (novo tipo de conta)
  - CNPJ, razao social, nome fantasia, logo, setor, tamanho, site
  - Email corporativo verificado
  - Planos/billing especificos para empresas
- [ ] **Dashboard da empresa**
  - Gerenciamento de vagas (criar, editar, pausar, encerrar)
  - Pipeline de candidatos por vaga
  - Historico de chats e candidatos interessados
  - Metricas: visualizacoes, conversas iniciadas, candidaturas

### 5.2 Gerenciamento de Vagas
- [ ] **CRUD de vagas**
  - Titulo, descricao, skills requeridas, senioridade, regime (CLT/PJ/hibrido/remoto)
  - Faixa salarial (opcional)
  - Stack tecnica (tags estruturadas)
  - Status: rascunho | ativa | pausada | encerrada
- [ ] **Matching automatico**
  - Sugestao de candidatos com base em skills + experiencia (AI-powered)
  - Score de fit por perfil

### 5.3 Chatbot de Fit para Candidatos
- [ ] **Chatbot de vaga** — candidato conversa com AI sobre a vaga
  - Candidato acessa link publico da vaga
  - AI explica a vaga de forma conversacional
  - Perguntas interativas: "Voce tem experiencia com X? Ja trabalhou com Y?"
  - AI calcula e exibe score de fit ao vivo
  - Ao final: candidato decide se candidata ou nao
- [ ] **Fluxo de candidatura via chatbot**
  - Candidato nao logado: AI guia registro passo a passo
  - Alternativa: importar perfil do LinkedIn (OAuth basico — nome, foto, headline)
  - Alternativa avancada: upload PDF do LinkedIn para preenchimento automatico via AI
  - Conta criada com perfil ja preenchido
  - Candidatura registrada automaticamente

### 5.4 Notificacoes & Comunicacao
- [ ] Empresa recebe notificacao quando candidato se candidata
- [ ] Candidato recebe confirmacao + proximo passo da empresa
- [ ] Canal de comunicacao in-platform (mensagens empresa ↔ candidato)

---

## 6. Infraestrutura Avancada de Dados

**Objetivo:** Evoluir a plataforma com grafos de conhecimento e RAG hibrido/avancado para matching e respostas muito mais precisas.

### 6.1 Banco de Dados de Grafos

> Referencia: banco estilo grafo (ex: Neo4j, FalkorDB, Kuzu) para modelar conexoes semanticas entre entidades.

- [ ] **Modelagem do grafo de skills e experiencias**
  - Nos: `Skill`, `Technology`, `SeniorityLevel`, `Industry`, `Role`, `Candidate`, `Company`, `Job`
  - Arestas: `HAS_SKILL`, `REQUIRES_SKILL`, `WORKED_WITH`, `RELATED_TO`, `EVOLVES_INTO`
  - Exemplo: `Java` → `RELATED_TO` → `Kotlin`, `Spring Boot` → `REQUIRES_SKILL` → `Java`
- [ ] **Casos de uso do grafo**
  - Matching semantico: "candidatos com skills adjacentes a vaga"
  - Recomendacao de skills: "dado seu perfil, aprenda X para acessar vagas Y"
  - Clustering de perfis similares
  - Deteccao de comunidades tecnicas (ex: ecossistema JVM, ecossistema JS)
  - Gap analysis: "faltam N skills para voce ser compativel com essa vaga"
- [ ] **Integracao com backend Spring Boot**
  - Driver Neo4j/Spring Data Neo4j ou REST API para FalkorDB/Kuzu
  - Sincronizar nos ao criar/atualizar perfis e vagas
  - API de matching que combina grafo + embeddings pgvector
- [ ] **Populacao inicial do grafo**
  - Seed de taxonomia de skills (ex: ESCO, O*NET, curado manualmente)
  - Pipeline de extracao: ao importar perfil, extrair skills e criar nos no grafo

### 6.2 RAG Hibrido

> Evolucao do RAG atual (pgvector + dense retrieval) para busca hibrida — combina recuperacao semantica e por palavra-chave.

- [ ] **Sparse retrieval com BM25**
  - Integrar BM25 (ex: via PostgreSQL `tsvector` ou servico dedicado como Elasticsearch/OpenSearch)
  - Indexar conteudo dos perfis e vagas para busca textual
- [ ] **Fusao hibrida — RRF (Reciprocal Rank Fusion)**
  - Combinar scores de dense (pgvector cosine) + sparse (BM25) via RRF
  - Implementar no `ai-chat` module como strategy configuravel
- [ ] **Re-ranking**
  - Cross-encoder re-ranker (ex: Cohere Rerank API ou modelo local)
  - Aplicar no top-K recuperado antes de enviar ao LLM
- [ ] **Chunk strategy otimizada**
  - Parent-child chunking: chunk pequeno para retrieval, chunk pai para contexto
  - Overlap calibrado por tipo de documento (perfil vs. vaga vs. FAQ)

### 6.3 RAG Avancado

> Tecnicas que tornam o RAG mais robusto e preciso para o dominio de RH/tech.

- [ ] **Multi-query retrieval**
  - LLM gera N variantes da pergunta do usuario
  - Executa retrieval para cada variante, faz merge e dedup dos resultados
  - Garante cobertura mesmo quando a pergunta e ambigua
- [ ] **HyDE (Hypothetical Document Embedding)**
  - LLM gera um "documento hipotetico" que seria a resposta ideal
  - Usa embedding desse documento para buscar no vector store
  - Melhora drasticamente precisao em dominios especializados
- [ ] **Contextual Compression**
  - Apos retrieval, LLM comprime/filtra os chunks para remover ruido
  - Passa apenas a parte relevante ao LLM final
- [ ] **RAG com grafos (Graph RAG)**
  - Combinar recuperacao vetorial com travessia de grafo
  - Ex: pergunta sobre "devs Java senior" recupera via embedding + expande via grafo para skills relacionadas
- [ ] **Avaliacao e monitoramento de RAG**
  - Metricas: faithfulness, answer relevancy, context precision (RAGAS framework)
  - Pipeline de avaliacao automatica em CI/CD
  - Dashboard interno de qualidade das respostas AI

---

## Prioridade Sugerida

| Prioridade | Feature | Esforco | Impacto |
|:---:|---------|:---:|:---:|
| 1 | Analytics (GA4 + Meta Pixel) | Baixo | Alto |
| 2 | Flag de visibilidade / searchable | Medio | Alto |
| 3 | Import LinkedIn (PDF primeiro) | Medio | Alto |
| 4 | Portal de recrutadores | Alto | Alto |
| 5 | LinkedIn browser automation | Alto | Medio |
| 6 | Portal de Empresas + CRUD de vagas | Alto | Alto |
| 7 | Chatbot de fit para candidatos | Alto | Alto |
| 8 | RAG Hibrido (BM25 + RRF) | Medio | Alto |
| 9 | Grafo de skills (Neo4j/FalkorDB) | Alto | Alto |
| 10 | RAG Avancado (multi-query, HyDE, re-rank) | Alto | Medio |

---

*Ultima atualizacao: 2026-04-28*
