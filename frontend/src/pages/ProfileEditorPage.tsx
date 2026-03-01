import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Tabs,
  Tab,
  Paper,
  Typography,
  Alert,
  CircularProgress,
} from '@mui/material';
import { useCurrentProfile } from '../hooks/useProfile';
import { ProfileForm } from '../components/profile/ProfileForm';
import { JobExperienceList } from '../components/profile/JobExperienceList';
import { EducationList } from '../components/profile/EducationList';
import { LanguageList } from '../components/profile/LanguageList';
import { SlugSettingsSection } from '../components/profile/SlugSettingsSection';

export const ProfileEditorPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const { data: profile, isLoading, error } = useCurrentProfile();
  const { t } = useTranslation('profile');

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error && !(error as { response?: { status?: number } })?.response?.status) {
    return (
      <Box sx={{ mt: 4 }}>
        <Alert severity="error">
          {t('errorLoading')}: {error instanceof Error ? error.message : t('common:errors.unknownError', 'Unknown error')}
        </Alert>
      </Box>
    );
  }

  const hasProfile = !!profile;

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        {t('title')}
      </Typography>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={(_, newValue) => setActiveTab(newValue)}
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label={t('tabs.profileInfo')} />
          <Tab label={t('tabs.workExperience')} disabled={!hasProfile} />
          <Tab label={t('tabs.education')} disabled={!hasProfile} />
          <Tab label={t('tabs.languages')} disabled={!hasProfile} />
          <Tab label={t('tabs.publicProfile')} disabled={!hasProfile} />
        </Tabs>
      </Box>

      <Paper sx={{ p: 3 }}>
        {activeTab === 0 && (
          <Box>
            <Typography variant="h6" gutterBottom>
              {t('tabs.profileInfo')}
            </Typography>
            {!hasProfile && (
              <Alert severity="info" sx={{ mb: 3 }}>
                {t('createPrompt')}
              </Alert>
            )}
            <ProfileForm profile={profile} />
          </Box>
        )}

        {activeTab === 1 && profile && (
          <Box>
            <Typography variant="h6" gutterBottom>
              {t('job.title')}
            </Typography>
            <JobExperienceList profileId={profile.id} />
          </Box>
        )}

        {activeTab === 2 && profile && (
          <Box>
            <Typography variant="h6" gutterBottom>
              {t('education.title')}
            </Typography>
            <EducationList profileId={profile.id} />
          </Box>
        )}

        {activeTab === 3 && profile && (
          <Box>
            <Typography variant="h6" gutterBottom>
              {t('languages.title')}
            </Typography>
            <LanguageList profile={profile} />
          </Box>
        )}

        {activeTab === 4 && profile && (
          <Box>
            <Typography variant="h6" gutterBottom>
              {t('publicProfileSettings')}
            </Typography>
            <SlugSettingsSection profileId={profile.id} currentSlug={profile.slug ?? null} slugChangeCount={profile.slugChangeCount ?? 0} />
          </Box>
        )}
      </Paper>
    </Box>
  );
};
