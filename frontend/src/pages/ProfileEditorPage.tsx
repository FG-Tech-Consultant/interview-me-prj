import React, { useState } from 'react';
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
import { SlugSettingsSection } from '../components/profile/SlugSettingsSection';

export const ProfileEditorPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const { data: profile, isLoading, error } = useCurrentProfile();

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
          Error loading profile: {error instanceof Error ? error.message : 'Unknown error'}
        </Alert>
      </Box>
    );
  }

  const hasProfile = !!profile;

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Career Profile Editor
      </Typography>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs
          value={activeTab}
          onChange={(_, newValue) => setActiveTab(newValue)}
          variant="scrollable"
          scrollButtons="auto"
        >
          <Tab label="Profile Information" />
          <Tab label="Work Experience" disabled={!hasProfile} />
          <Tab label="Education" disabled={!hasProfile} />
          <Tab label="Public Profile" disabled={!hasProfile} />
        </Tabs>
      </Box>

      <Paper sx={{ p: 3 }}>
        {activeTab === 0 && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Profile Information
            </Typography>
            {!hasProfile && (
              <Alert severity="info" sx={{ mb: 3 }}>
                Create your profile to get started. You can add work experience, education, and set up your public profile after.
              </Alert>
            )}
            <ProfileForm profile={profile} />
          </Box>
        )}

        {activeTab === 1 && profile && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Work Experience
            </Typography>
            <JobExperienceList profileId={profile.id} />
          </Box>
        )}

        {activeTab === 2 && profile && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Education
            </Typography>
            <EducationList profileId={profile.id} />
          </Box>
        )}

        {activeTab === 3 && profile && (
          <Box>
            <Typography variant="h6" gutterBottom>
              Public Profile Settings
            </Typography>
            <SlugSettingsSection profileId={profile.id} currentSlug={profile.slug ?? null} />
          </Box>
        )}
      </Paper>
    </Box>
  );
};
