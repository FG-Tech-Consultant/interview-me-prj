import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import commonEn from './locales/en/common.json';
import authEn from './locales/en/auth.json';
import profileEn from './locales/en/profile.json';
import skillsEn from './locales/en/skills.json';
import experienceEn from './locales/en/experience.json';
import billingEn from './locales/en/billing.json';
import exportsEn from './locales/en/exports.json';
import linkedinEn from './locales/en/linkedin.json';
import chatEn from './locales/en/chat.json';
import dashboardEn from './locales/en/dashboard.json';
import publicProfileEn from './locales/en/public-profile.json';
import errorsEn from './locales/en/errors.json';
import settingsEn from './locales/en/settings.json';

import commonPtBR from './locales/pt-BR/common.json';
import authPtBR from './locales/pt-BR/auth.json';
import profilePtBR from './locales/pt-BR/profile.json';
import skillsPtBR from './locales/pt-BR/skills.json';
import experiencePtBR from './locales/pt-BR/experience.json';
import billingPtBR from './locales/pt-BR/billing.json';
import exportsPtBR from './locales/pt-BR/exports.json';
import linkedinPtBR from './locales/pt-BR/linkedin.json';
import chatPtBR from './locales/pt-BR/chat.json';
import dashboardPtBR from './locales/pt-BR/dashboard.json';
import publicProfilePtBR from './locales/pt-BR/public-profile.json';
import errorsPtBR from './locales/pt-BR/errors.json';
import settingsPtBR from './locales/pt-BR/settings.json';

const resources = {
  en: {
    common: commonEn,
    auth: authEn,
    profile: profileEn,
    skills: skillsEn,
    experience: experienceEn,
    billing: billingEn,
    exports: exportsEn,
    linkedin: linkedinEn,
    chat: chatEn,
    dashboard: dashboardEn,
    'public-profile': publicProfileEn,
    errors: errorsEn,
    settings: settingsEn,
  },
  'pt-BR': {
    common: commonPtBR,
    auth: authPtBR,
    profile: profilePtBR,
    skills: skillsPtBR,
    experience: experiencePtBR,
    billing: billingPtBR,
    exports: exportsPtBR,
    linkedin: linkedinPtBR,
    chat: chatPtBR,
    dashboard: dashboardPtBR,
    'public-profile': publicProfilePtBR,
    errors: errorsPtBR,
    settings: settingsPtBR,
  },
};

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'en',
    supportedLngs: ['en', 'pt-BR'],
    ns: [
      'common',
      'auth',
      'profile',
      'skills',
      'experience',
      'billing',
      'exports',
      'linkedin',
      'chat',
      'dashboard',
      'public-profile',
      'errors',
      'settings',
    ],
    defaultNS: 'common',
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'i18nextLng',
      caches: ['localStorage'],
    },
    interpolation: {
      escapeValue: true,
    },
    react: {
      useSuspense: false,
    },
  });

export default i18n;
