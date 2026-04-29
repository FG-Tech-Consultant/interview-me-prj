# ADR-001: Graph Database Selection

## Date
2026-04-29

## Status
Accepted

## Context

The Interview Me platform needs a graph database to complement PostgreSQL for storing and querying:
- Connections between Skills (relations: "similar_to", "requires", "part_of")
- Connections between Skills ↔ JobExperience ↔ ExperienceProject
- Tags and domains as nodes
- Semantic matching via graph traversal (N-hop)

The existing stack is Spring Boot 3.4 + PostgreSQL (with pgvector for embeddings) + LangChain4j. We need a graph DB that integrates well with this ecosystem.

## Decision

We chose **Neo4j Community Edition** as our graph database.

## Rationale

### Why Neo4j

1. **Industry standard** — Most widely adopted graph database with the largest community, extensive documentation, and proven track record in production.
2. **Spring Boot integration** — First-class support via `neo4j-java-driver` and optional Spring Data Neo4j. Best Java ecosystem support among all candidates.
3. **Cypher query language** — The de facto standard for graph queries, readable and expressive. OpenCypher is an industry standard.
4. **Docker simplicity** — Official Docker image with straightforward configuration, health checks, and volume management.
5. **Performance** — Purpose-built for graph traversal. Excellent for N-hop queries needed for skill matching.
6. **Maturity** — 15+ years in production use, stable APIs, predictable release cycles.
7. **Tooling** — Neo4j Browser for visual exploration, APOC procedures for advanced operations.

### Alternatives Rejected

#### FalkorDB (Redis-based, Cypher-compatible)
- **Pros:** Lightweight, Apache 2.0 license, fast for simple queries
- **Cons:** Less mature (relatively new), smaller community, Redis dependency adds complexity, limited advanced graph features, fewer Java integration options
- **Rejected because:** Maturity concerns and limited Spring Boot ecosystem support

#### Kuzu (Embedded, DuckDB-like)
- **Pros:** No separate server needed, embedded mode, good performance for analytics
- **Cons:** Very young project (pre-1.0), Java bindings are experimental, no Docker service model (embedded only), limited production references
- **Rejected because:** Too immature for production use, experimental Java support, embedded model doesn't fit our Docker-based architecture

## Consequences

### Positive
- Well-documented integration path with Spring Boot
- Large talent pool familiar with Neo4j/Cypher
- Proven scalability for our use case (skill graphs are typically <100K nodes)
- Excellent visualization and debugging tools (Neo4j Browser)
- Future option to upgrade to Enterprise if needed

### Negative
- Neo4j Community Edition uses GPLv3 license (acceptable for our use case as we don't distribute the software)
- Requires a separate Docker container (additional ~500MB memory)
- Community Edition has no clustering (acceptable — single instance is sufficient for our scale)
- Learning curve for Cypher (mitigated by excellent documentation)

## Implementation

- Docker: Neo4j 5.x Community Edition in DEV compose
- Driver: `org.neo4j.driver:neo4j-java-driver`
- Port: 7687 (Bolt protocol), 7474 (HTTP/Browser) — bound to 127.0.0.1
- Initial schema: Skill, Domain, Tag nodes with SIMILAR_TO, REQUIRES, BELONGS_TO, TAGGED_WITH relationships
