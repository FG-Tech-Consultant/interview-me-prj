# int-008 — Setup e Modelagem do Graph DB (Spike + ADR)

## MC Task
`int-008` — Setup e Modelagem do Graph DB (FalkorDB/Neo4j/Kuzu) — ADR obrigatório

## Objetivo
Fazer um spike técnico para decidir e implementar o Graph DB do projeto Interview Me.
O graph DB vai complementar o PostgreSQL existente para armazenar e consultar:
- Conexões entre Skills (relações: "similar_to", "requires", "part_of")
- Conexões Skills ↔ JobExperience ↔ ExperienceProject
- Tags e domínios como nós
- Matching semântico via traversal de grafo

## O que fazer

### Fase 1 — Spike/Pesquisa (subagent isolado)
Pesquisar e comparar os 3 candidatos:
1. **FalkorDB** — Redis-based, Cypher-compatible, Apache 2.0, Docker simples
2. **Neo4j** — padrão da indústria, Cypher nativo, community free
3. **Kuzu** — embedded (sem servidor separado), DuckDB do mundo de grafos

Critérios de avaliação:
- Facilidade de Docker setup (já temos docker-compose.yml e `others/docker/compose.yaml`)
- Integração com Java/Spring Boot (drivers disponíveis)
- Compatibilidade com o existente (PostgreSQL + LangChain4j já em uso)
- Performance em consultas de matching de skills (N-hop traversal)
- Maturidade e suporte a longo prazo
- Licença open source

### Fase 2 — ADR
Criar arquivo `docs/adr/ADR-001-graph-db-selection.md` com:
- Título, Data, Status: Accepted
- Contexto (o que precisamos resolver)
- Decisão (qual DB escolher e por quê)
- Consequências positivas e negativas
- Alternativas rejeitadas com justificativa

### Fase 3 — Docker Setup
Adicionar o Graph DB escolhido ao Docker Compose de DEV:
- Arquivo: `others/docker/compose.yaml`
- Usar imagem oficial ou Docker Hub
- Porta exposta localmente apenas (127.0.0.1)
- Volume persistente para dados
- Health check
- Incluir no mesmo docker network do PostgreSQL

### Fase 4 — Dependency no Gradle
Adicionar dependência do driver Java no módulo `backend/build.gradle` ou `sboot/build.gradle`:
- FalkorDB → Jedis (Redis client) com suporte a graph queries
- Neo4j → `neo4j-java-driver`
- Kuzu → Java binding oficial

Adicionar versão em `gradle.properties`.

### Fase 5 — Schema Inicial do Grafo
Criar classe de inicialização/seed `GraphSchemaInitializer.java` em:
`backend/src/main/java/com/interviewme/graph/`

Definir os tipos de nós e relações:
```
Nodes:
  (:Skill {id, name, category, description})
  (:Domain {id, name})
  (:Tag {name})
  (:Tenant {id})

Relationships:
  (:Skill)-[:SIMILAR_TO {weight}]->(:Skill)
  (:Skill)-[:REQUIRES]->(:Skill)
  (:Skill)-[:BELONGS_TO]->(:Domain)
  (:Skill)-[:TAGGED_WITH]->(:Tag)
```

### Fase 6 — Smoke test
Criar teste de integração simples que:
1. Conecta no Graph DB
2. Cria 2 skill nodes
3. Cria relação SIMILAR_TO entre eles
4. Faz query de traversal e retorna o resultado
5. Verifica que o resultado está correto

## Constraints
- Seguir CLAUDE.md patterns (Gradle, versões em gradle.properties, @Transactional, etc.)
- Docker DEV first — não alterar docker-compose.yml (PROD) ainda
- Criar `docs/adr/` se não existir
- Não fazer migration Liquibase agora — o grafo tem seu próprio schema
- NÃO implementar ainda a integração com Skills/UserSkills do PostgreSQL (isso é int-011)

## Files Likely Involved
- `others/docker/compose.yaml` — adicionar serviço graph db
- `gradle.properties` — adicionar versão do driver
- `backend/build.gradle` ou `ai-chat/build.gradle` — adicionar dependency
- `backend/src/main/java/com/interviewme/graph/GraphSchemaInitializer.java` — novo
- `docs/adr/ADR-001-graph-db-selection.md` — novo

## Out of Scope
- Integração com skills/experiences do PostgreSQL (int-011)
- API REST de matching (int-010)
- RAG pipeline (int-012)
- Frontend

## Rules
- Spawn a team of subagents whenever necessary — parallelize work, reduce context size, isolate concerns (e.g. one agent researches, another implements Docker, another writes the ADR).
- Follow CLAUDE.md patterns strictly.
- Run build at the end: `./gradlew :sboot:bootJar -x test` — must succeed.
- When done: `openclaw system event --text "Done: int-008 Graph DB setup + ADR completo" --mode now`
