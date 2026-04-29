# int-005 — Cadastro e Dashboard de Empresas

## MC Task
`int-005` — Cadastro e Dashboard de Empresas

## Objetivo
Implementar o módulo de empresas (B2B side do Interview Me): uma empresa pode se cadastrar, fazer login, ver seu dashboard e gerenciar seu perfil. Este é o ponto de entrada do Portal de Empresas.

## Contexto
O Interview Me está expandindo para B2B — empresas podem se cadastrar para publicar vagas e usar o chatbot de fit. A estrutura multi-tenant atual (`tenant_id` em todas as entidades) já suporta isso. Precisamos de:
1. Um tipo de conta "EMPRESA" separado de "CANDIDATO"
2. Dashboard de empresa com métricas básicas
3. Perfil da empresa (dados da empresa, logo, setor, tamanho, site)

## O que Implementar

### Backend

#### Entidade: Company
Criar entidade `Company` com:
- `id` (BIGINT, PK)
- `tenant_id` (FK para Tenant — cada empresa é um tenant)
- `name` (VARCHAR 255, obrigatório)
- `cnpj` (VARCHAR 20, único — pode ser nulo para empresas internacionais)
- `website` (VARCHAR 500)
- `sector` (VARCHAR 100) — ex: Fintech, Saúde, E-commerce
- `size` (ENUM: STARTUP, SMB, ENTERPRISE)
- `description` (TEXT)
- `logo_url` (VARCHAR 500)
- `country` (VARCHAR 100)
- `city` (VARCHAR 100)
- `created_at`, `updated_at`
- `active` (BOOLEAN default true)

#### Enum: AccountType
Adicionar ao User ou criar enum separado: `CANDIDATE` | `COMPANY`

#### Migration Liquibase
Criar `sboot/src/main/resources/db/changelog/<timestamp>-add-company-table.xml`
com criação da tabela `company` e rollback.

#### Endpoints REST (CompanyController)
`/api/v1/company/`
- `POST /register` — cadastro público de empresa (não requer auth, cria tenant + user + company)
- `GET /profile` — perfil da empresa autenticada
- `PUT /profile` — atualiza perfil
- `GET /dashboard` — métricas do dashboard (vagas ativas, candidatos, visualizações)

#### Service: CompanyService
- `registerCompany(CompanyRegistrationRequest)` — transação: criar Tenant + User (role COMPANY_ADMIN) + Company
- `getProfile(Long tenantId)` — retorna CompanyProfileDto
- `updateProfile(Long tenantId, CompanyUpdateRequest)` — atualiza empresa
- `getDashboardMetrics(Long tenantId)` — retorna métricas (vagas count, etc — pode ser zeros por ora)

#### DTOs
- `CompanyRegistrationRequest` — dados para cadastro (nome empresa + dados do user admin: email, senha)
- `CompanyProfileDto` — resposta com perfil
- `CompanyUpdateRequest` — campos editáveis
- `CompanyDashboardDto` — métricas (vagas, candidatos, visualizações — tudo 0 por ora)

### Frontend

#### Páginas novas
1. **`/company/register`** — formulário de cadastro de empresa
   - Dados da empresa: nome, setor, tamanho, site, país, cidade, descrição
   - Dados do admin: nome, email, senha
   - Submit → chama POST /api/company/register
   - Sucesso → redireciona para /company/dashboard

2. **`/company/dashboard`** — dashboard da empresa (protegida, role COMPANY_ADMIN)
   - Cards: Vagas ativas (0), Candidatos (0), Visualizações (0)
   - Link para gerenciar vagas (int-004 — ainda não implementado, só placeholder)
   - Link para editar perfil

3. **`/company/profile`** — editar perfil da empresa
   - Formulário com todos os campos editáveis
   - Upload de logo (pode ser URL por ora, sem S3)

#### Componentes
- `CompanyRegistrationForm.tsx`
- `CompanyDashboard.tsx`
- `CompanyProfileForm.tsx`
- Page Objects em `interview-me-test-prj` para os seletores (ou deixar como stub se muito trabalho)

#### Roteamento
Adicionar rotas no React Router dentro do subpath `/interviewme-app/`:
- `/company/register` — público
- `/company/dashboard` — protegida (COMPANY_ADMIN)
- `/company/profile` — protegida

### Autenticação Multi-Role
O sistema atual provavelmente tem roles de CANDIDATE. Adicionar role `COMPANY_ADMIN` no Spring Security.
Criar `@PreAuthorize("hasRole('COMPANY_ADMIN')")` nos endpoints de empresa.

## Constraints
- Seguir todos os padrões do CLAUDE.md
- Multi-tenant: toda query filtra por tenant_id
- @Transactional readOnly nos reads, @Transactional nos writes
- NUNCA repository.findAll().stream().filter()
- Versioning: bumpar MINOR version no gradle.properties E package.json
- Subpath: todas as URLs internas usam import.meta.env.BASE_URL
- MUI para componentes React
- Liquibase timestamp-based migration

## Files Likely Involved
### Backend
- `backend/src/main/java/com/interviewme/company/` — novo módulo
  - `Company.java`, `CompanyService.java`, `CompanyRepository.java`, `CompanyController.java`
  - `dto/CompanyRegistrationRequest.java`, `CompanyProfileDto.java`, etc.
- `sboot/src/main/resources/db/changelog/<timestamp>-add-company-table.xml`
- `sboot/src/main/resources/db/changelog/db.changelog-master.yaml`

### Frontend
- `frontend/src/pages/company/` — novas páginas
- `frontend/src/components/company/` — componentes

## Out of Scope
- Upload de logo para S3 (URL string basta por ora)
- CRUD de vagas (int-004)
- Chatbot de fit (int-003)
- Planos/billing para empresas
- E2E tests (pode criar stubs básicos, não precisa ser completo)

## Rules
- Spawn a team of subagents whenever necessary — parallelize work, reduce context size, isolate concerns (backend vs frontend, migration vs service vs controller).
- Follow CLAUDE.md patterns strictly.
- Read `.specify/memory/constitution.md` and `.specify/memory/project-overview.md` before implementing.
- Bump MINOR version before building.
- Run build at the end: `./gradlew :sboot:bootJar -x test` — must succeed.
- Deploy to DEV after build: `cd others/docker && docker compose up -d`
- When done: `openclaw system event --text "Done: int-005 Company registration + dashboard implementado" --mode now`
