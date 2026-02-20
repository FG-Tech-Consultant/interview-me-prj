# Research Directory

This directory contains standalone technical research documents that inform implementation decisions across the Travian Bot project.

## Directory Structure

```
.specify/research/
├── README.md (this file)
├── [topic-slug]/
│   └── research.md (research findings and recommendations)
└── [topic-slug]/
    └── research.md
```

## When to Create Research Documents

### Feature-Specific Research
**Location:** `specs/[N]-[feature-name]/research/research.md`
**When:** Research is specific to a single feature implementation

**Example:**
- `specs/003-task-scheduler/research/research.md` - Task scheduling library comparison

### Cross-Cutting Research
**Location:** `.specify/research/[topic-slug]/research.md`
**When:** Research applies to multiple features or architectural decisions

**Examples:**
- `.specify/research/selenium-retry-patterns/research.md` - Retry patterns for all Selenium services
- `.specify/research/embedded-databases/research.md` - H2 vs SQLite comparison for data persistence
- `.specify/research/logging-frameworks/research.md` - Structured logging options for observability

## Research Command

Use `/research [topic]` to generate research documents:

```bash
# Example: Research task scheduling libraries
/research task scheduling libraries for Spring Boot

# Example: Research retry patterns
/research Selenium retry logic with exponential backoff best practices

# Example: Research database options
/research H2 vs SQLite for embedded database in Docker containers
```

## Research Document Template

All research documents follow this structure:

1. **Research Question** - What are we trying to answer?
2. **Background & Context** - Why is this needed?
3. **Options Evaluated** - Detailed analysis of each option (3-5 options)
4. **Decision Matrix** - Weighted scoring comparison
5. **Recommendation** - Primary and alternative recommendations
6. **Implementation Guidance** - Dependencies, config, code examples
7. **References** - Sources used for research

## Constitutional Compliance

All research must validate against the 9 constitutional principles:

1. ✅ **Simplicity First** - Prefer simple solutions
2. ✅ **Containerization** - Must work in Docker
3. ✅ **Modern Java Standards** - Java 21, Spring Boot 3.x
4. ✅ **Data Sovereignty** - Embedded database support
5. ✅ **Browser Automation Reliability** - Selenium robustness
6. ✅ **Observability** - Logging and monitoring
7. ✅ **Security** - Credential management
8. ✅ **Reference Compatibility** - Check TravianBotSharp
9. ✅ **Modularity** - Services <500 lines, modules <400 lines

## Evidence Standards

Every recommendation must be supported by:
- **Primary source:** Official documentation
- **Secondary source:** Authoritative guide (Baeldung, Spring.io)
- **Validation:** Code example or benchmark

## Research Workflow

```
1. Identify research question
   ↓
2. Run /research [topic]
   ↓
3. Review generated research.md
   ↓
4. Approve/modify recommendation
   ↓
5. Update tech-context.md with decision
   ↓
6. Use research findings in plan.md
```

## Existing Research

[Add links to completed research documents here as they are created]

- None yet

---

**Last Updated:** 2026-01-24
