import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Grid, TextField, Button, Alert } from '@mui/material';
import { useUpdateProfile, useCreateProfile } from '../../hooks/useProfile';
import type { Profile, CreateProfileRequest } from '../../types/profile';

interface ProfileFormProps {
  profile?: Profile;
}

export const ProfileForm: React.FC<ProfileFormProps> = ({ profile }) => {
  const [formData, setFormData] = useState({
    fullName: profile?.fullName || '',
    headline: profile?.headline || '',
    summary: profile?.summary || '',
    location: profile?.location || '',
  });
  const { t } = useTranslation('profile');

  const updateMutation = useUpdateProfile();
  const createMutation = useCreateProfile();

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (profile) {
      updateMutation.mutate({
        profileId: profile.id,
        data: {
          fullName: formData.fullName,
          headline: formData.headline || undefined,
          summary: formData.summary || undefined,
          location: formData.location || undefined,
          version: profile.version,
        },
      });
    } else {
      const createData: CreateProfileRequest = {
        fullName: formData.fullName,
        headline: formData.headline || undefined,
        summary: formData.summary || undefined,
        location: formData.location || undefined,
      };
      createMutation.mutate(createData);
    }
  };

  const isLoading = updateMutation.isPending || createMutation.isPending;
  const error = updateMutation.error || createMutation.error;
  const isSuccess = updateMutation.isSuccess || createMutation.isSuccess;

  return (
    <Box component="form" onSubmit={handleSubmit} sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      <Grid container spacing={3}>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            id="fullName"
            name="fullName"
            label={t('form.fullName')}
            value={formData.fullName}
            onChange={handleChange}
            required
          />
        </Grid>
        <Grid item xs={12} md={6}>
          <TextField
            fullWidth
            id="headline"
            name="headline"
            label={t('form.headline')}
            value={formData.headline}
            onChange={handleChange}
          />
        </Grid>
      </Grid>

      <TextField
        fullWidth
        id="summary"
        name="summary"
        label={t('form.summary')}
        value={formData.summary}
        onChange={handleChange}
        multiline
        rows={4}
      />

      <TextField
        fullWidth
        id="location"
        name="location"
        label={t('form.location')}
        value={formData.location}
        onChange={handleChange}
      />

      {isSuccess && (
        <Alert severity="success">{t('form.saveSuccess')}</Alert>
      )}

      {error && (
        <Alert severity="error">
          {error instanceof Error ? error.message : t('form.failedToSave')}
        </Alert>
      )}

      <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          type="submit"
          variant="contained"
          disabled={isLoading}
        >
          {isLoading ? t('common:status.saving') : profile ? t('form.updateButton') : t('form.createButton')}
        </Button>
      </Box>
    </Box>
  );
};
