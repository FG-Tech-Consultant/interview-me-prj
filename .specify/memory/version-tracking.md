# Version Tracking Protocol

## Important Reminder
**ALWAYS update version numbers in ALL relevant files when making releases!**

## Version Locations (Current: v0.1.0 - Initial Planning)

### Backend (Spring Boot)
- **application.yml**: Application version property
- **build.gradle**: Project version
- **README.md**: Current version badge/mention

### Frontend (React)
- **package.json**: Version property
- **README.md**: Current version badge/mention
- **About page** (future): Version display in UI

## Version Update Checklist

When releasing a new version:
- [ ] Update backend `build.gradle` version
- [ ] Update backend `application.yml` version (if applicable)
- [ ] Update frontend `package.json` version
- [ ] Update README.md in both frontend and backend (if separate)
- [ ] Update this file (version-tracking.md)
- [ ] Ensure all locations show the SAME version number
- [ ] Tag git commit with version (e.g., `v1.0.0`)

## Version History

### v0.1.0 (2026-02-19) - Initial Planning
**Status:** Planning phase
**Major Changes:**
- Project structure defined
- Constitution established (v2.0.0)
- Memory files adapted from Travian Bot project to Live Resume & Career Copilot
- Technology stack defined (React + Spring Boot + PostgreSQL)
- Multi-tenant architecture planned
- AI/LLM integration pattern established

**Next Milestone:** v0.2.0 - Project scaffolding (Spring Boot + React setup)

---

## Future Version Planning

### v0.2.0 (Target: TBD) - Project Scaffolding
- Spring Boot project structure
- React frontend project structure
- Docker setup (backend + frontend)
- CI/CD pipeline (GitHub Actions or equivalent)

### v0.3.0 (Target: TBD) - Authentication & Tenancy
- User registration and login (email/password)
- JWT authentication
- Tenant model and filtering
- Basic RBAC (OWNER role)

### v0.4.0 (Target: TBD) - Profile CRUD
- Profile entity and endpoints
- JobExperience CRUD
- Education CRUD
- Public/private visibility flags

### v0.5.0 (Target: TBD) - Skills & Stories
- Skills catalog management
- UserSkill CRUD
- ExperienceProject CRUD
- Story (STAR format) CRUD

### v1.0.0 (Target: TBD) - MVP Release
- Live public profile page
- Recruiter chat (RAG + LLM)
- Resume export (1 template, PDF)
- LinkedIn Profile Score Analyzer
- Coin wallet (balance tracking, no payment yet)

### v1.5.0 (Target: TBD) - Enhanced Features
- Background presentation export
- Cover letter generator
- LinkedIn Inbox Assistant (draft-only)
- Packages (tokenized shareable links)
- Multiple resume templates

### v2.0.0 (Target: TBD) - SaaS Scale
- Stripe integration for coin purchases
- Multi-user per tenant
- Horizontal scaling support
- Advanced analytics
- Public API

---

## Versioning Strategy

**Semantic Versioning (SemVer):**
- **MAJOR** (X.0.0): Breaking changes, major feature additions
- **MINOR** (0.X.0): New features, backwards-compatible
- **PATCH** (0.0.X): Bug fixes, minor improvements

**Pre-1.0.0 Versions:**
- v0.x.x indicates pre-production/MVP development
- Breaking changes are acceptable in v0.x.x versions
- v1.0.0 will be the first production-ready release

**Release Cadence:**
- No fixed schedule during initial development
- Release when features are complete and stable
- Tag all releases in git with `vX.Y.Z` format

---

## Changelog Guidelines

When creating release notes, include:
- **New Features**: List of new capabilities
- **Improvements**: Enhancements to existing features
- **Bug Fixes**: Issues resolved
- **Breaking Changes**: Changes requiring user/developer action
- **Migration Guide** (if applicable): Steps to upgrade

**Example Release Note Template:**
```markdown
## v1.0.0 (YYYY-MM-DD)

### New Features
- Live public profile page with recruiter chat
- Resume export (PDF generation)
- LinkedIn Profile Score Analyzer

### Improvements
- Optimized RAG retrieval performance
- Improved frontend loading times

### Bug Fixes
- Fixed tenant isolation in recruiter chat
- Corrected coin transaction calculation

### Breaking Changes
- None

### Migration Guide
- Run database migrations: `./gradlew update`
```

---

## Constitutional Alignment

This version tracking protocol aligns with:
- **Principle 6 (Observability)**: Clear version tracking aids debugging
- **Principle 9 (Modularity)**: Separate frontend and backend versioning (if needed)
- **Governance**: Version changes require documentation and review

---

**Document Version:** 1.0.0
**Last Updated:** 2026-02-19
