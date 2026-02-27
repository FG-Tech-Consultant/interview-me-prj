# Implementation Plan: Internationalization (i18n) Support

**Feature ID:** 010-internationalization-i18n
**Status:** Design Complete
**Created:** 2026-02-25
**Last Updated:** 2026-02-25
**Version:** 1.0.0

---

## Executive Summary

This implementation plan defines the technical design for adding internationalization (i18n) support to the Interview Me platform. The implementation spans both the React frontend (using react-i18next with namespaced JSON translation files) and the Spring Boot backend (using Spring MessageSource for validation messages and i18n-aware error responses). The design follows a phased, incremental approach where each phase leaves the application fully functional in English while progressively adding Portuguese (pt-BR) support.

**Key Deliverables:**
- Frontend: i18n initialization module, 11 namespace translation files per locale (en + pt-BR = 22 JSON files), language selector component, migrated components using `useTranslation` hook
- Backend: MessageSource configuration, ValidationMessages properties files (en + pt-BR), updated GlobalExceptionHandler with message keys, locale persistence in profile entity
- Database: 1 Liquibase migration (add `locale` column to `profile` table)

---

## Technical Design

### 1. Frontend i18n Architecture

#### 1.1 Library Stack

| Library | Version | Purpose |
|---------|---------|---------|
| `i18next` | ^23.x | Core i18n framework |
| `react-i18next` | ^14.x | React bindings (hooks, components) |
| `i18next-browser-languagedetector` | ^7.x | Automatic browser language detection |

#### 1.2 Initialization (`frontend/src/i18n.ts`)

```typescript
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

// Import all namespace translations
import enCommon from './locales/en/common.json';
import enAuth from './locales/en/auth.json';
import enProfile from './locales/en/profile.json';
import enSkills from './locales/en/skills.json';
import enExperience from './locales/en/experience.json';
import enPublicProfile from './locales/en/public-profile.json';
import enChat from './locales/en/chat.json';
import enBilling from './locales/en/billing.json';
import enExports from './locales/en/exports.json';
import enLinkedin from './locales/en/linkedin.json';
import enErrors from './locales/en/errors.json';

import ptBRCommon from './locales/pt-BR/common.json';
// ... same pattern for all pt-BR namespaces

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: {
        common: enCommon,
        auth: enAuth,
        profile: enProfile,
        skills: enSkills,
        experience: enExperience,
        'public-profile': enPublicProfile,
        chat: enChat,
        billing: enBilling,
        exports: enExports,
        linkedin: enLinkedin,
        errors: enErrors,
      },
      'pt-BR': {
        common: ptBRCommon,
        // ... all pt-BR namespaces
      },
    },
    fallbackLng: 'en',
    defaultNS: 'common',
    interpolation: {
      escapeValue: true, // XSS protection
    },
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'i18nextLng',
      caches: ['localStorage'],
    },
  });

export default i18n;
```

#### 1.3 Translation File Structure

```
frontend/src/locales/
  en/
    common.json        # Nav, buttons, generic labels
    auth.json          # Login, register pages
    profile.json       # Profile editor, job, education forms
    skills.json        # Skills page, skill dialogs
    experience.json    # Projects, stories
    public-profile.json # Public profile UI chrome
    chat.json          # Chat widget labels
    billing.json       # Billing page, coin displays
    exports.json       # Export dialogs
    linkedin.json      # LinkedIn analyzer
    errors.json        # Validation + business error messages
  pt-BR/
    common.json
    auth.json
    profile.json
    skills.json
    experience.json
    public-profile.json
    chat.json
    billing.json
    exports.json
    linkedin.json
    errors.json
```

#### 1.4 Component Migration Pattern

**Before (hardcoded):**
```tsx
<Button>Save</Button>
<TextField label="Full Name" />
<Alert>Profile saved successfully</Alert>
```

**After (i18n):**
```tsx
import { useTranslation } from 'react-i18next';

function ProfileForm() {
  const { t } = useTranslation('profile');
  const { t: tCommon } = useTranslation('common');

  return (
    <>
      <Button>{tCommon('buttons.save')}</Button>
      <TextField label={t('form.full_name')} />
      <Alert>{tCommon('status.success')}</Alert>
    </>
  );
}
```

#### 1.5 Language Selector Component

New component: `frontend/src/components/LanguageSelector.tsx`

- Renders a MUI `Select` or `IconButton` + `Menu` in the navigation bar
- Calls `i18n.changeLanguage(locale)` on selection
- For authenticated users: also calls `PUT /api/v1/profile/locale` to persist
- Shows locale codes "EN" / "PT" with optional flag icons
- Available on all pages (authenticated and unauthenticated)

#### 1.6 Frontend Error Handling Update

Current pattern in hooks/components:
```typescript
// Current: displays error.response.data.fieldErrors.fieldName (string)
const errorMessage = error.response?.data?.fieldErrors?.fullName;
```

Updated pattern:
```typescript
// New: looks up key from error response, falls back to defaultMessage
const fieldError = error.response?.data?.fieldErrors?.fullName;
const errorMessage = fieldError?.key
  ? t(fieldError.key, { ns: 'errors', defaultValue: fieldError.defaultMessage })
  : fieldError?.defaultMessage || fieldError;
```

This maintains backward compatibility: if the response contains a plain string (old format), it displays as-is. If it contains `{ key, defaultMessage }`, it translates.

### 2. Backend i18n Architecture

#### 2.1 Spring MessageSource Configuration

Add to `sboot/src/main/resources/application.yml` or create a `@Configuration` class:

```yaml
spring:
  messages:
    basename: ValidationMessages
    encoding: UTF-8
    fallback-to-system-locale: false
```

Or via Java config:

```java
@Configuration
public class MessageSourceConfig {
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
            new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
```

#### 2.2 Validation Message Key Convention

**Pattern:** `validation.<entity>.<field>.<rule>`

**Rules:**
- `required` for `@NotBlank` / `@NotNull`
- `max_length` for `@Size(max=N)`
- `min_length` for `@Size(min=N)`
- `pattern` for `@Pattern`
- `invalid` for custom validators

**Example migration of `CreateProfileRequest.java`:**

```java
// BEFORE
@NotBlank(message = "Full name is required")
@Size(max = 255, message = "Full name must not exceed 255 characters")
String fullName,

// AFTER
@NotBlank(message = "{validation.profile.full_name.required}")
@Size(max = 255, message = "{validation.profile.full_name.max_length}")
String fullName,
```

#### 2.3 Updated Error Response Format

**GlobalExceptionHandler changes:**

For validation errors, change `fieldErrors` from `Map<String, String>` to `Map<String, FieldErrorDetail>`:

```java
record FieldErrorDetail(String key, String defaultMessage) {}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidationExceptions(
        MethodArgumentNotValidException ex) {
    Map<String, Object> response = new HashMap<>();
    Map<String, FieldErrorDetail> fieldErrors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach(error -> {
        String fieldName = ((FieldError) error).getField();
        String messageKey = extractMessageKey(error);
        String defaultMessage = error.getDefaultMessage();
        fieldErrors.put(fieldName, new FieldErrorDetail(messageKey, defaultMessage));
    });

    response.put("timestamp", OffsetDateTime.now().toString());
    response.put("status", HttpStatus.BAD_REQUEST.value());
    response.put("error", "Bad Request");
    response.put("message", "Validation failed");
    response.put("fieldErrors", fieldErrors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
}

private String extractMessageKey(ObjectError error) {
    String[] codes = error.getCodes();
    // The message template from annotation (e.g., "{validation.profile.full_name.required}")
    String defaultMessage = error.getDefaultMessage();
    if (defaultMessage != null && defaultMessage.startsWith("{") && defaultMessage.endsWith("}")) {
        return defaultMessage.substring(1, defaultMessage.length() - 1);
    }
    // If not a message key, return null (frontend falls back to defaultMessage)
    return null;
}
```

For business errors, add `code` field to all exception handlers:

```java
@ExceptionHandler(ProfileNotFoundException.class)
public ResponseEntity<Map<String, Object>> handleProfileNotFoundException(
        ProfileNotFoundException ex) {
    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", OffsetDateTime.now().toString());
    response.put("status", HttpStatus.NOT_FOUND.value());
    response.put("error", "Not Found");
    response.put("code", "error.profile.not_found");
    response.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
}
```

#### 2.4 Locale Persistence

**Profile entity update:**
```java
@Column(name = "locale", nullable = false, length = 10)
private String locale = "en";
```

**New endpoint in ProfileController:**
```java
@PutMapping("/locale")
@Transactional
public ResponseEntity<Void> updateLocale(
        @RequestBody Map<String, String> request,
        @AuthenticationPrincipal UserDetails userDetails) {
    String locale = request.get("locale");
    // Validate against whitelist
    if (!Set.of("en", "pt-BR").contains(locale)) {
        return ResponseEntity.badRequest().build();
    }
    profileService.updateLocale(userDetails, locale);
    return ResponseEntity.ok().build();
}
```

**Profile response DTO update:**
Include `locale` field in `ProfileResponse` record.

### 3. Database Migration

**File:** `sboot/src/main/resources/db/changelog/20260225120000-add-locale-to-profile.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="20260225120000-1" author="interview-me">
        <addColumn tableName="profile">
            <column name="locale" type="VARCHAR(10)" defaultValue="en">
                <constraints nullable="false"/>
            </column>
        </addColumn>

        <rollback>
            <dropColumn tableName="profile" columnName="locale"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

---

## Implementation Phases

### Phase 1: Infrastructure (2-3 days)

| Task | Layer | Description |
|------|-------|-------------|
| Install npm packages | FE | Add i18next, react-i18next, i18next-browser-languagedetector |
| Create i18n.ts | FE | Initialize i18next with config, detection, fallback |
| Create locale directory structure | FE | Create `locales/en/` and `locales/pt-BR/` with all namespace JSON files (initially empty or with common keys) |
| Create LanguageSelector component | FE | MUI Select in nav bar, calls changeLanguage + persists |
| Add locale column migration | BE | Liquibase migration for profile.locale |
| Update Profile entity | BE | Add locale field to Profile JPA entity |
| Add locale endpoint | BE | PUT /api/v1/profile/locale |
| Update ProfileResponse | BE | Include locale in GET response |
| Import i18n in main.tsx | FE | Ensure i18n initializes before React renders |

### Phase 2: Frontend Migration (4-5 days)

| Task | Components | Estimated Keys |
|------|-----------|----------------|
| Auth pages | LoginPage, RegisterPage | ~20 keys |
| Navigation + layout | App.tsx, DashboardPage | ~15 keys |
| Profile editor | ProfileForm, ProfileEditorPage, JobExperienceForm/List, EducationForm/List, SlugSettingsSection | ~60 keys |
| Skills page | SkillsPage, SkillCard, SkillFilters, SkillFormDialog, SkillSelector | ~30 keys |
| Experience pages | ProjectForm, ProjectList, StoryForm, StoryList, MetricsEditor | ~40 keys |
| Public profile | PublicProfilePage + all Public* components | ~25 keys |
| Chat | ChatPanel, ChatInput, ChatWidget, ChatMessageBubble, ChatQuotaWarning, ChatTypingIndicator | ~20 keys |
| Billing | BillingPage, CoinBalanceBadge, CoinConfirmationDialog, QuotaStatusCard, TransactionHistoryTable, TransactionTypeBadge | ~30 keys |
| Exports | ExportsPage, ExportFormDialog, ExportHistoryTable, ExportProgressCard, ExportStatusBadge | ~25 keys |
| LinkedIn | LinkedInAnalyzerPage, PdfUploader, ScoreGauge, SectionScoreCard, SuggestionItem, AnalysisHistory | ~25 keys |

**Total estimated: ~290 translation keys per locale**

### Phase 3: Backend Validation Migration (2-3 days)

| Task | Description |
|------|-------------|
| Configure MessageSource | Add MessageSource bean with ValidationMessages bundle |
| Create ValidationMessages.properties | English defaults for all validation annotations |
| Create ValidationMessages_pt_BR.properties | Portuguese translations |
| Update all DTO annotations | Replace hardcoded strings with message keys in all Request DTOs |
| Update GlobalExceptionHandler | Return FieldErrorDetail with key + defaultMessage |
| Add error codes to exception handlers | Add `code` field to all business error responses |
| Update frontend error handling | Use translation keys from error responses |

**DTOs to update (14 files):**
- CreateProfileRequest, UpdateProfileRequest
- CreateJobExperienceRequest, UpdateJobExperienceRequest
- CreateEducationRequest, UpdateEducationRequest
- CreateSkillDto, UpdateSkillDto
- AddUserSkillDto, UpdateUserSkillDto
- CreateProjectRequest, UpdateProjectRequest
- CreateStoryRequest, UpdateStoryRequest

### Phase 4: Verification (1-2 days)

| Task | Description |
|------|-------------|
| Translation completeness script | Node script comparing en and pt-BR key sets |
| Manual review in pt-BR | Walk through all pages verifying no hardcoded English |
| Layout review | Check no UI breakage with longer pt-BR strings |
| Date/number formatting | Verify Intl formatting per locale |

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Missing translations cause blank text | Medium | Fallback to English configured; completeness script catches gaps |
| Longer pt-BR strings break layout | Low | Use flexbox/responsive design; review layouts during Phase 4 |
| Frontend error handling regression | Medium | Backward-compatible response format; gradual migration with fallback |
| Performance impact from loading all translations | Low | Total < 50KB; no lazy loading needed at this scale |
| Validation annotation migration breaks existing tests | Medium | Run full test suite after each DTO update; default messages remain English |

---

## Constitution Compliance Verification

| Principle | Compliance |
|-----------|------------|
| Simplicity First | Standard libraries (react-i18next, MessageSource); no custom abstractions |
| Modern Java Standards | Uses records for FieldErrorDetail; standard Spring patterns |
| Multi-Tenant Isolation | Locale stored in tenant-isolated profile table |
| Full-Stack Modularity | Namespace-per-domain; frontend and backend i18n independent |
| Database Schema Evolution | Timestamp-based Liquibase migration with rollback |

---

## Revision History

| Version | Date       | Changes                     | Author      |
|---------|------------|-----------------------------|-------------|
| 1.0.0   | 2026-02-25 | Initial plan                | Claude Code |
