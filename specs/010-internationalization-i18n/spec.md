# Feature Specification: Internationalization (i18n) Support

**Feature ID:** 010-internationalization-i18n
**Status:** Draft
**Created:** 2026-02-25
**Last Updated:** 2026-02-25
**Version:** 1.0.0

---

## Overview

### Problem Statement

The Interview Me platform currently has all UI labels, validation messages, error messages, and button text hardcoded in English throughout the codebase. This creates several problems:

- The platform cannot serve non-English-speaking users (e.g., Brazilian Portuguese speakers)
- Backend validation messages (e.g., `"Full name is required"`) are hardcoded strings in Java annotations and exception handlers, making them impossible to translate on the frontend
- Error responses from the API contain human-readable English text instead of machine-readable keys, coupling the backend to a single language
- Adding new languages requires touching every component file and every DTO validation annotation
- The public profile page and recruiter chat interface are English-only, limiting the platform's reach

### Feature Summary

Implement full internationalization (i18n) support across both the React frontend and Spring Boot backend. The frontend will use `react-i18next` with JSON translation files organized by namespace. The backend will use Spring `MessageSource` with properties files for validation messages and adopt an error response format that includes both a machine-readable message key and a default English message. The initial supported locales are English (en) as the default and Brazilian Portuguese (pt-BR). The system will be designed so that adding new locales only requires adding translation files without code changes.

### Target Users

- **Career Professionals (Brazilian):** Portuguese-speaking users who prefer to interact with the platform in their native language
- **Career Professionals (English):** English-speaking users who continue to use the platform as before (no regression)
- **Recruiters:** May view public profiles in different languages depending on the profile owner's locale preference
- **Future Users:** Users in other locales (Spanish, French, etc.) who benefit from the i18n infrastructure without additional code changes

---

## Constitution Compliance

**Applicable Principles:**

- **Principle 1: Simplicity First** - Uses standard, well-established i18n libraries (react-i18next, Spring MessageSource) rather than custom solutions. No complex abstractions; translation files are simple JSON/properties files. The monolithic deployment means locale detection and switching happen entirely within the single application.
- **Principle 2: Containerization as First-Class Citizen** - N/A (no container changes needed; translation files bundled in the JAR/static assets)
- **Principle 3: Modern Java Standards** - Uses Java records for i18n-aware error response DTOs. Leverages Spring Boot 4.x MessageSource auto-configuration.
- **Principle 4: Data Sovereignty and Multi-Tenant Isolation** - Locale preference stored per-profile with tenant_id isolation; migration follows PostgreSQL patterns.
- **Principle 5: AI Integration and LLM Management** - N/A
- **Principle 6: Observability and Debugging** - Console warnings for missing translation keys in development mode; structured logging for locale resolution.
- **Principle 7: Security, Privacy, and Credential Management** - Locale parameter validated against whitelist of supported locales to prevent injection; i18next escaping enabled.
- **Principle 8: Multi-Tenant Architecture** - Locale preference is tenant-isolated like all other profile data.
- **Principle 9: Freemium Model and Coin-Based Monetization** - N/A
- **Principle 10: Full-Stack Modularity and Separation of Concerns** - Translation files organized by namespace matching domain modules (auth, profile, skills, etc.). Frontend and backend i18n are independent.
- **Principle 11: Database Schema Evolution** - Requires a Liquibase timestamp-based migration to add `locale` column to the profile table.
- **Principle 12: Async Job Processing** - N/A
- **Principle 13: LinkedIn Compliance** - N/A
- **Principle 14: Event-Driven Architecture** - N/A

---

## User Scenarios

### Primary Use Cases

#### Scenario 1: User Switches Language to Portuguese

**Actor:** Brazilian career professional who registered with English defaults
**Goal:** Switch the entire UI to Brazilian Portuguese
**Preconditions:** User is authenticated, platform has pt-BR translations available

**Steps:**
1. User clicks language selector in the top navigation bar (globe icon or language code display)
2. User selects "Portugues (Brasil)" from the dropdown
3. System immediately updates all visible UI text to Portuguese without page reload
4. System persists the language preference to the user's profile via API call
5. On next login, the platform loads in Portuguese automatically

**Success Criteria:**
- All UI labels, buttons, navigation items, and form placeholders display in Portuguese
- Validation error messages display in Portuguese when form validation fails
- Language preference persists across sessions (stored in user profile)
- No page reload required when switching languages
- All hardcoded English text is replaced; no mixed-language UI elements

#### Scenario 2: New User with Browser in Portuguese

**Actor:** New user whose browser language is set to pt-BR
**Goal:** Experience the platform in Portuguese from the first interaction
**Preconditions:** User has not registered yet, browser Accept-Language header includes pt-BR

**Steps:**
1. User navigates to the login/register page
2. System detects browser language as pt-BR via `navigator.language`
3. Login and registration forms display in Portuguese
4. User registers and the detected locale is saved as their preference
5. After login, dashboard and all pages render in Portuguese

**Success Criteria:**
- Unauthenticated pages (login, register, public profile) respect browser language
- Detected language is persisted upon registration
- Fallback to English if browser language is not supported

#### Scenario 3: Backend Validation Error in User's Language

**Actor:** User with locale set to pt-BR submitting a profile form
**Goal:** See validation errors in Portuguese
**Preconditions:** User is authenticated with pt-BR locale preference

**Steps:**
1. User submits profile form with missing required field (e.g., empty "Full Name")
2. Backend returns 400 response with field errors containing message keys
3. Frontend receives error response with key `validation.profile.full_name.required`
4. Frontend looks up the key in pt-BR translation file and displays: "Nome completo e obrigatorio"
5. User sees the error message in Portuguese next to the form field

**Success Criteria:**
- Backend validation responses include message keys (not hardcoded English strings)
- Frontend resolves message keys to the user's active locale
- All validation messages for all forms have translations in both en and pt-BR
- Error message display is identical to current behavior, just translated

#### Scenario 4: Public Profile Page in Specific Locale

**Actor:** Recruiter viewing a public profile
**Goal:** View the public profile page with UI chrome in their preferred language
**Preconditions:** Public profile page is accessible without authentication

**Steps:**
1. Recruiter navigates to a public profile URL (e.g., `/p/john-doe`)
2. System detects browser language and renders UI labels (section headers, buttons) in that language
3. User-generated content (name, headline, job descriptions, stories) remains in the language the profile owner wrote it in
4. Recruiter can switch the UI language via a language toggle if desired

**Success Criteria:**
- UI chrome (headers like "Work Experience", "Education", "Skills") is translated
- User-generated content is NOT translated (displayed as-is)
- Language toggle available on public profile page
- Default language detected from browser; falls back to English

### Edge Cases

- **Unsupported locale:** User's browser sends `Accept-Language: fr-FR`. System falls back to English (en).
- **Partial translations:** A translation key exists in `en` but is missing in `pt-BR`. System falls back to the English value for that specific key.
- **Right-to-left languages:** Not in scope for this version, but the architecture should not prevent future RTL support.
- **Very long translated strings:** Some Portuguese translations are longer than English equivalents. UI must handle varying text lengths gracefully (responsive design, text wrapping).
- **Concurrent locale change:** User changes locale in one tab while another tab is open. Next API call from the other tab should pick up the new locale.
- **Interpolation variables:** Messages with dynamic values (e.g., "Maximum {max} characters") must use interpolation syntax correctly in all locales.

---

## Functional Requirements

### Core Capabilities

**REQ-001:** Frontend i18n Framework Setup
- **Description:** The React frontend MUST integrate `react-i18next` as the internationalization framework, with translation files loaded from JSON files organized by locale and namespace.
- **Acceptance Criteria:**
  - `react-i18next` and `i18next` packages installed as dependencies
  - i18n initialization configured in `frontend/src/i18n.ts` with language detection (browser, localStorage, user profile)
  - Translation files stored at `frontend/src/locales/{locale}/{namespace}.json`
  - Default locale is `en`, additional locale is `pt-BR`
  - `i18next-browser-languagedetector` plugin used for automatic language detection
  - Fallback language set to `en` when a translation key is missing in the active locale

**REQ-002:** Frontend Translation Namespace Organization
- **Description:** Translation files MUST be organized into namespaces that align with the application's domain modules for maintainability and lazy-loading potential.
- **Acceptance Criteria:**
  - Namespace `common` - Shared strings: navigation, buttons ("Save", "Cancel", "Delete", "Edit"), generic labels ("Loading...", "Error", "Success"), date formats
  - Namespace `auth` - Login, register, password reset pages
  - Namespace `profile` - Profile editor, job experience, education forms
  - Namespace `skills` - Skills management page, skill form dialogs
  - Namespace `experience` - Projects and stories (STAR format labels)
  - Namespace `public-profile` - Public profile page UI chrome
  - Namespace `chat` - Recruiter chat interface labels
  - Namespace `billing` - Coin wallet, transaction history, quota displays
  - Namespace `exports` - Export dialogs, export history
  - Namespace `linkedin` - LinkedIn analyzer page
  - Namespace `errors` - Error messages, validation messages displayed from backend error keys
  - Each namespace has one JSON file per locale: e.g., `frontend/src/locales/en/profile.json`, `frontend/src/locales/pt-BR/profile.json`

**REQ-003:** Frontend Component Translation Pattern
- **Description:** All React components MUST use the `useTranslation` hook to access translated strings. No hardcoded user-facing strings in TSX files.
- **Acceptance Criteria:**
  - Components use `const { t } = useTranslation('namespace')` to access translations
  - All user-facing text uses `t('key')` instead of hardcoded strings
  - Dynamic values use interpolation: `t('key', { value: someVar })`
  - Pluralization uses i18next plural syntax where needed (e.g., `t('items', { count: n })`)
  - Component `label` props, `placeholder` props, `title` attributes all use translated strings
  - MUI component text (button labels, dialog titles, snackbar messages) all use translated strings

**REQ-004:** Language Selector Component
- **Description:** The application MUST provide a language selector component accessible from the navigation bar, allowing users to switch the active locale at any time.
- **Acceptance Criteria:**
  - Language selector displayed as a dropdown or icon button in the top navigation bar
  - Shows current language as a flag icon or locale code (e.g., "EN" / "PT")
  - Available options: "English" and "Portugues (Brasil)"
  - Switching language immediately updates all visible text (no page reload)
  - For authenticated users: persists the preference via API call to `PUT /api/v1/profile/locale`
  - For unauthenticated users: stores preference in `localStorage`
  - Language selector available on all pages including public profile and login/register

**REQ-005:** Backend Validation Message Keys
- **Description:** Backend validation annotations MUST use message keys instead of hardcoded English strings. The keys follow a consistent naming convention that the frontend can look up in translation files.
- **Acceptance Criteria:**
  - All `@NotBlank`, `@Size`, `@Pattern`, `@NotNull`, and custom validation annotations use message keys
  - Message key format: `{validation.<entity>.<field>.<rule>}`
  - Examples:
    - `@NotBlank(message = "{validation.profile.full_name.required}")` (replaces `"Full name is required"`)
    - `@Size(max = 255, message = "{validation.profile.full_name.max_length}")` (replaces `"Full name must not exceed 255 characters"`)
    - `@NotBlank(message = "{validation.profile.headline.required}")`
  - Spring `MessageSource` configured with `ValidationMessages` resource bundle
  - Properties files: `ValidationMessages.properties` (English defaults), `ValidationMessages_pt_BR.properties` (Portuguese)
  - Properties files located at `backend/src/main/resources/`

**REQ-006:** Backend Error Response Format with Message Keys
- **Description:** The `GlobalExceptionHandler` MUST return error responses that include both a machine-readable message key and a default English message. This allows the frontend to translate errors using its own translation files while maintaining backward compatibility.
- **Acceptance Criteria:**
  - Validation error response format:
    ```json
    {
      "timestamp": "2026-02-25T10:30:00Z",
      "status": 400,
      "error": "Bad Request",
      "message": "Validation failed",
      "fieldErrors": {
        "fullName": {
          "key": "validation.profile.full_name.required",
          "defaultMessage": "Full name is required"
        }
      }
    }
    ```
  - Business error response format:
    ```json
    {
      "timestamp": "2026-02-25T10:30:00Z",
      "status": 404,
      "error": "Not Found",
      "code": "error.profile.not_found",
      "message": "Profile not found"
    }
    ```
  - The `code` field contains a stable, translatable key for all non-validation errors
  - The `message` field always contains the English default for backward compatibility and debugging
  - Frontend uses the `key`/`code` field to look up translations, falling back to `message`/`defaultMessage` if key not found

**REQ-007:** User Locale Preference Persistence
- **Description:** The system MUST persist the user's preferred locale in their profile and use it for all subsequent sessions.
- **Acceptance Criteria:**
  - `locale` column added to the `profile` table (VARCHAR 10, default 'en')
  - API endpoint `PUT /api/v1/profile/locale` accepts `{ "locale": "pt-BR" }` and updates the user's preference
  - API endpoint `GET /api/v1/profile` returns the user's locale preference in the response
  - On login, the frontend reads the user's stored locale and switches to it
  - Liquibase migration adds the `locale` column with default value 'en'

**REQ-008:** Static vs Dynamic Content Separation
- **Description:** The i18n system MUST clearly distinguish between translatable static content (UI labels, validation messages) and non-translatable dynamic content (user-generated data).
- **Acceptance Criteria:**
  - **Translated (static):** Navigation labels, button text, form field labels, form placeholders, section headings, validation error messages, system error messages, dialog titles, tooltips, empty state messages
  - **NOT translated (dynamic):** User's full name, headline, summary, job company names, job titles, education degree names, institution names, skill names, story content (situation, task, action, result), project descriptions, chat messages
  - Translation boundaries are clearly documented and enforced by code review conventions
  - No `t()` calls wrapping user-generated content

**REQ-009:** Fallback Behavior
- **Description:** The i18n system MUST gracefully handle missing translations by falling back to English.
- **Acceptance Criteria:**
  - If a translation key is missing in pt-BR, the English value is displayed
  - If a translation key is missing in all locales, the raw key is displayed (for debugging)
  - `i18next` `fallbackLng` configured to `'en'`
  - Backend `MessageSource` configured with English as the default properties file
  - No blank text or errors displayed due to missing translations
  - Console warning logged in development mode for missing translation keys

### User Interface Requirements

**REQ-UI-001:** Language Selector in Navigation
- **Description:** A language selector MUST be visible in the main navigation bar on all pages.
- **Acceptance Criteria:**
  - Positioned in the top-right area of the navigation bar, near the user menu
  - Displays current language as abbreviated code (EN / PT) or with a small flag icon
  - Dropdown shows available languages with their native names: "English", "Portugues (Brasil)"
  - Selection triggers immediate locale switch via `i18n.changeLanguage()`
  - For authenticated users, triggers API call to persist preference
  - Works on both desktop and mobile layouts

**REQ-UI-002:** Date and Number Formatting
- **Description:** Dates and numbers displayed in the UI MUST follow the conventions of the active locale.
- **Acceptance Criteria:**
  - Dates formatted according to locale (en: "Feb 25, 2026", pt-BR: "25 de fev. de 2026")
  - Numbers formatted according to locale (en: "1,234.56", pt-BR: "1.234,56")
  - Use `Intl.DateTimeFormat` and `Intl.NumberFormat` or i18next formatting plugins
  - Date pickers (MUI DatePicker) respect the active locale

### Data Requirements

**REQ-DATA-001:** Profile Locale Column
- **Description:** The `profile` table MUST include a `locale` column to persist the user's language preference.
- **Acceptance Criteria:**
  - Column name: `locale`
  - Type: `VARCHAR(10)`
  - Default value: `'en'`
  - Nullable: false
  - Valid values enforced at application level: `'en'`, `'pt-BR'`
  - Liquibase migration file: timestamp-based naming (e.g., `20260225120000-add-locale-to-profile.xml`)
  - Migration added to `db.changelog-master.yaml`

---

## Success Criteria

The feature will be considered successful when:

1. **Complete UI Translation:** All user-facing static text in the application displays correctly in both English and Brazilian Portuguese when the respective locale is active.
   - Measurement: Manual review of all pages in both locales; no hardcoded English text visible when pt-BR is active

2. **Validation Messages Translated:** Backend validation errors display in the user's preferred language on the frontend.
   - Measurement: Submit invalid forms in pt-BR locale; all field-level error messages appear in Portuguese

3. **Language Switching Works:** Users can switch between English and Portuguese at any time without page reload, and the preference persists across sessions.
   - Measurement: Switch language, navigate to multiple pages, log out, log back in; language remains as set

4. **No Regression for English Users:** English-speaking users experience no change in behavior or appearance.
   - Measurement: Complete E2E test suite passes with English locale active

5. **Fallback Behavior Correct:** Missing translations in pt-BR gracefully fall back to English without blank text or errors.
   - Measurement: Temporarily remove a pt-BR translation key; English value appears instead

6. **Public Profile Accessible in Both Languages:** Public profile page UI chrome renders in the visitor's browser language.
   - Measurement: Visit public profile with browser language set to pt-BR; section headers display in Portuguese

7. **Error Response Format Supports i18n:** Backend error responses include message keys that the frontend can translate.
   - Measurement: API returns validation error with `key` field; frontend displays translated message

---

## Dependencies

### Internal Dependencies

- **Feature 001: Project Base Structure** - Authentication, tenant filtering, navigation bar (for language selector placement)
- **Feature 002: Profile CRUD** - Profile entity for locale preference storage, profile API for locale update endpoint

### External Dependencies

- **react-i18next** (npm): React integration for i18next (frontend i18n framework)
- **i18next** (npm): Core internationalization framework
- **i18next-browser-languagedetector** (npm): Plugin for automatic browser language detection
- **Spring MessageSource** (included in Spring Boot): Backend message resolution for validation messages

---

## Assumptions

1. English (en) and Brazilian Portuguese (pt-BR) are the only initial locales; additional locales can be added later by creating new translation files
2. The frontend build includes all translation files statically (no runtime fetching from server)
3. User-generated content is always stored and displayed in the language the user wrote it in (no translation of user data)
4. The public profile page can detect visitor language from browser settings since no authentication is required
5. MUI components support locale customization out of the box (date pickers, number fields)
6. The team will provide Portuguese translations for all keys (translation is a content task, not a code task)
7. Validation message keys in the backend follow a consistent naming convention that the frontend mirrors in its `errors` namespace
8. The existing `GlobalExceptionHandler` error response format can be extended without breaking the current frontend error handling logic
9. The recruiter chat widget on public profiles uses the same i18n infrastructure as the rest of the frontend
10. RTL (right-to-left) language support is not required in this version

---

## Out of Scope

The following are explicitly excluded from this feature:

1. **No RTL (Right-to-Left) support:** Arabic, Hebrew, and other RTL languages are not supported in this version
2. **No server-side rendering (SSR) for SEO in multiple languages:** Public profiles are client-rendered; SEO meta tags remain in English
3. **No translation of user-generated content:** User data (names, descriptions, stories) is not translated by the system
4. **No translation management UI:** Translations are managed via JSON/properties files in the codebase, not via an admin interface
5. **No machine translation integration:** No automatic translation using AI/LLM; all translations are manually authored
6. **No per-field locale for user content:** Users cannot specify the language of each field (e.g., write a summary in both English and Portuguese)
7. **No locale-specific resume templates:** Export templates generate documents in a single language (determined by user locale at time of export)
8. **No third locale beyond en and pt-BR:** Additional languages deferred to future work
9. **No URL-based locale routing:** The app does not use `/en/dashboard` or `/pt-BR/dashboard` URL patterns; locale is managed via state/cookies
10. **No currency or timezone localization:** Currency display (coins) and timezone handling are out of scope for this feature

---

## Security & Privacy Considerations

### Security Requirements

- Locale parameter input MUST be validated against a whitelist of supported locales (`en`, `pt-BR`) to prevent injection attacks
- Translation files MUST NOT contain executable code or HTML that could cause XSS; use i18next's built-in escaping
- Backend MessageSource MUST sanitize interpolated values in translated messages

### Privacy Requirements

- User locale preference is non-sensitive data and can be stored in plain text
- Locale preference is tenant-isolated like all other profile data

---

## Performance Expectations

- **Language switching:** Immediate (< 100ms perceived delay), no page reload required
- **Translation file loading:** All translations loaded on app initialization; total bundle size for all namespaces in one locale should be under 50KB
- **Backend MessageSource resolution:** Negligible overhead (< 1ms per message resolution); MessageSource is cached by Spring
- **No additional API calls for translations:** Translations are bundled with the frontend; only the locale preference API call is needed

---

## Error Handling

### Error Scenarios

**ERROR-001:** Unsupported Locale Requested
- **User Experience:** System silently falls back to English; no error message displayed
- **Recovery Path:** User can manually select a supported language from the language selector

**ERROR-002:** Missing Translation Key in Active Locale
- **User Experience:** English fallback text displayed; no error visible to user. Console warning in development mode.
- **Recovery Path:** Developer adds the missing translation key to the locale's JSON file

**ERROR-003:** Failed Locale Preference Save
- **User Experience:** Language switches immediately in the UI (client-side), but if the API call to persist preference fails, user sees a non-blocking warning: "Could not save language preference. It will reset on next login."
- **Recovery Path:** User can try switching language again; or the preference is re-detected from browser on next login

**ERROR-004:** Invalid Locale Value in API Request
- **User Experience:** API returns 400 Bad Request with message: "Unsupported locale. Supported locales: en, pt-BR"
- **Recovery Path:** Frontend prevents this by only allowing selection from supported locales

---

## Testing Scope

### Functional Testing

- **Translation completeness:** Verify all keys in `en/*.json` exist in `pt-BR/*.json` (automated script)
- **Component translation:** Verify no hardcoded strings remain in TSX files (ESLint rule or manual review)
- **Language switching:** Switch between en and pt-BR; verify all visible text updates
- **Validation messages:** Submit invalid forms in both locales; verify correct translated messages
- **Locale persistence:** Set locale, log out, log in; verify locale is restored
- **Browser detection:** Clear localStorage, set browser language to pt-BR; verify app loads in Portuguese
- **Fallback behavior:** Remove a key from pt-BR; verify English fallback is displayed
- **Date/number formatting:** Verify dates and numbers format correctly per locale

### User Acceptance Testing

- **Full workflow in Portuguese:** Register, create profile, add job, add education, manage skills, view public profile - all in pt-BR
- **Mixed content display:** Verify user-generated content (English names/descriptions) displays unchanged when UI is in Portuguese
- **Language selector usability:** Toggle between languages multiple times; verify no UI glitches or stale text

### Edge Case Testing

- **Long Portuguese translations:** Verify UI handles longer pt-BR strings without layout breaking
- **Interpolated messages:** Test messages with dynamic values in both locales (e.g., "Maximum {max} characters")
- **Empty translation values:** Ensure empty strings in translation files show fallback, not blank text
- **Multiple tabs:** Switch language in one tab; open new tab; verify new tab uses updated locale

---

## Migration Plan

The migration from hardcoded English strings to i18n MUST be gradual and non-breaking:

### Phase 1: Infrastructure Setup
1. Install `react-i18next`, `i18next`, `i18next-browser-languagedetector`
2. Create `frontend/src/i18n.ts` initialization file
3. Create translation directory structure: `frontend/src/locales/en/`, `frontend/src/locales/pt-BR/`
4. Create initial `common.json` namespace with shared strings (buttons, navigation)
5. Add language selector component to navigation bar
6. Add `locale` column to profile table via Liquibase migration

### Phase 2: Frontend Component Migration (Incremental)
1. Start with authentication pages (LoginPage, RegisterPage) - highest visibility, fewest strings
2. Migrate navigation and layout components (App.tsx, DashboardPage)
3. Migrate profile editor pages and forms
4. Migrate skills management page
5. Migrate experience/stories pages
6. Migrate billing, exports, LinkedIn pages
7. Migrate public profile page
8. Each migration: extract strings to namespace JSON, replace with `t()` calls, add pt-BR translations

### Phase 3: Backend Validation Message Migration
1. Configure Spring `MessageSource` with `ValidationMessages` resource bundle
2. Replace hardcoded message strings in all DTO validation annotations with message keys
3. Update `GlobalExceptionHandler` to include message keys in error responses
4. Create `ValidationMessages.properties` (English) and `ValidationMessages_pt_BR.properties`
5. Update frontend error handling to look up message keys from `errors` namespace

### Phase 4: Verification and Polish
1. Run translation completeness check (script that compares en and pt-BR key sets)
2. Review all pages in pt-BR for layout issues with longer strings
3. Verify date/number formatting across locales
4. Update E2E tests to cover both locales

**Key Principle:** At every phase, the application MUST remain fully functional in English. Portuguese translations are additive; missing translations fall back to English. No phase should break existing functionality.

---

## Translation File Examples

### Frontend: `frontend/src/locales/en/common.json`
```json
{
  "nav": {
    "dashboard": "Dashboard",
    "profile": "Profile",
    "skills": "Skills",
    "experience": "Experience",
    "exports": "Exports",
    "billing": "Billing",
    "linkedin": "LinkedIn Analyzer",
    "logout": "Logout"
  },
  "buttons": {
    "save": "Save",
    "cancel": "Cancel",
    "delete": "Delete",
    "edit": "Edit",
    "add": "Add",
    "close": "Close",
    "confirm": "Confirm",
    "back": "Back"
  },
  "status": {
    "loading": "Loading...",
    "saving": "Saving...",
    "error": "An error occurred",
    "success": "Saved successfully",
    "no_data": "No data available"
  }
}
```

### Frontend: `frontend/src/locales/pt-BR/common.json`
```json
{
  "nav": {
    "dashboard": "Painel",
    "profile": "Perfil",
    "skills": "Habilidades",
    "experience": "Experiencia",
    "exports": "Exportacoes",
    "billing": "Faturamento",
    "linkedin": "Analisador LinkedIn",
    "logout": "Sair"
  },
  "buttons": {
    "save": "Salvar",
    "cancel": "Cancelar",
    "delete": "Excluir",
    "edit": "Editar",
    "add": "Adicionar",
    "close": "Fechar",
    "confirm": "Confirmar",
    "back": "Voltar"
  },
  "status": {
    "loading": "Carregando...",
    "saving": "Salvando...",
    "error": "Ocorreu um erro",
    "success": "Salvo com sucesso",
    "no_data": "Nenhum dado disponivel"
  }
}
```

### Frontend: `frontend/src/locales/en/errors.json`
```json
{
  "validation": {
    "profile": {
      "full_name": {
        "required": "Full name is required",
        "max_length": "Full name must not exceed {{max}} characters"
      },
      "headline": {
        "required": "Headline is required",
        "max_length": "Headline must not exceed {{max}} characters"
      }
    }
  },
  "error": {
    "profile": {
      "not_found": "Profile not found"
    },
    "generic": "An unexpected error occurred",
    "network": "Unable to connect to the server"
  }
}
```

### Backend: `ValidationMessages.properties`
```properties
validation.profile.full_name.required=Full name is required
validation.profile.full_name.max_length=Full name must not exceed {max} characters
validation.profile.headline.required=Headline is required
validation.profile.headline.max_length=Headline must not exceed {max} characters
validation.profile.summary.max_length=Summary must not exceed {max} characters
```

### Backend: `ValidationMessages_pt_BR.properties`
```properties
validation.profile.full_name.required=Nome completo e obrigatorio
validation.profile.full_name.max_length=Nome completo nao deve exceder {max} caracteres
validation.profile.headline.required=Titulo profissional e obrigatorio
validation.profile.headline.max_length=Titulo profissional nao deve exceder {max} caracteres
validation.profile.summary.max_length=Resumo nao deve exceder {max} caracteres
```

---

## Notes

**Design Decisions:**

- **react-i18next chosen over react-intl:** react-i18next has a larger ecosystem, simpler API with hooks, better namespace support, and more flexible interpolation. It is the most popular React i18n library.
- **JSON translation files over YAML/XLIFF:** JSON is native to JavaScript, directly importable, and easily type-checked. No build step needed.
- **Namespace-per-domain over single-file:** Keeps translation files small and maintainable. Teams can work on different namespaces independently. Enables potential lazy-loading in the future.
- **Backend returns message keys, frontend translates:** This keeps the backend language-agnostic and allows the frontend to manage all user-facing translations in one place. The backend only needs to define keys and default messages.
- **Locale stored in profile, not separate table:** Simple approach for MVP; one locale per user. If multi-locale preferences become necessary (e.g., UI language vs. export language), a separate settings table can be added later.

**Future Considerations:**

- Additional locales (es, fr, de) can be added by creating new `locales/{code}/` directories and `ValidationMessages_{locale}.properties` files
- Translation management platforms (Crowdin, Lokalise) can be integrated later to manage translations at scale
- SSR with locale-specific meta tags for SEO could be added if the platform moves to Next.js
- Export templates could support locale-specific formatting (date formats, number formats in generated PDFs)

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-25 | Initial specification       | Claude Code |
