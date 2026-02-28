import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  TextField,
  Button,
  Grid,
  Checkbox,
  FormControlLabel,
  Alert,
  Stack,
} from '@mui/material';
import { useCreateJobExperience, useUpdateJobExperience } from '../../hooks/useJobExperience';
import type { JobExperience, CreateJobExperienceRequest } from '../../types/profile';

interface JobExperienceFormProps {
  profileId: number;
  experience?: JobExperience;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export const JobExperienceForm: React.FC<JobExperienceFormProps> = ({
  profileId,
  experience,
  onSuccess,
  onCancel,
}) => {
  const [formData, setFormData] = useState({
    company: experience?.company || '',
    role: experience?.role || '',
    location: experience?.location || '',
    startDate: experience?.startDate || '',
    endDate: experience?.endDate || '',
    isCurrent: experience?.isCurrent || false,
    employmentType: experience?.employmentType || '',
    responsibilities: experience?.responsibilities || '',
    achievements: experience?.achievements || '',
  });
  const { t } = useTranslation('profile');

  const createMutation = useCreateJobExperience();
  const updateMutation = useUpdateJobExperience();

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (experience) {
      updateMutation.mutate(
        {
          profileId,
          experienceId: experience.id,
          data: {
            company: formData.company,
            role: formData.role,
            location: formData.location || undefined,
            startDate: formData.startDate,
            endDate: formData.isCurrent ? undefined : formData.endDate || undefined,
            isCurrent: formData.isCurrent,
            employmentType: formData.employmentType || undefined,
            responsibilities: formData.responsibilities || undefined,
            achievements: formData.achievements || undefined,
            version: experience.version,
          },
        },
        {
          onSuccess: () => {
            onSuccess?.();
          },
        }
      );
    } else {
      const createData: CreateJobExperienceRequest = {
        company: formData.company,
        role: formData.role,
        location: formData.location || undefined,
        startDate: formData.startDate,
        endDate: formData.isCurrent ? undefined : formData.endDate || undefined,
        isCurrent: formData.isCurrent,
        employmentType: formData.employmentType || undefined,
        responsibilities: formData.responsibilities || undefined,
        achievements: formData.achievements || undefined,
      };

      createMutation.mutate(
        { profileId, data: createData },
        {
          onSuccess: () => {
            onSuccess?.();
          },
        }
      );
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;
  const error = createMutation.error || updateMutation.error;

  return (
    <form onSubmit={handleSubmit}>
      <Stack spacing={3}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={t('job.role')}
              name="role"
              value={formData.role}
              onChange={handleChange}
              required
              size="small"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={t('job.company')}
              name="company"
              value={formData.company}
              onChange={handleChange}
              required
              size="small"
            />
          </Grid>
        </Grid>

        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={t('job.location')}
              name="location"
              value={formData.location}
              onChange={handleChange}
              size="small"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={t('job.employmentType')}
              name="employmentType"
              value={formData.employmentType}
              onChange={handleChange}
              placeholder={t('job.employmentTypePlaceholder')}
              size="small"
            />
          </Grid>
        </Grid>

        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={t('job.startDate')}
              name="startDate"
              type="date"
              value={formData.startDate}
              onChange={handleChange}
              required
              size="small"
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={`${t('job.endDate')}${!formData.isCurrent ? ' *' : ''}`}
              name="endDate"
              type="date"
              value={formData.endDate}
              onChange={handleChange}
              disabled={formData.isCurrent}
              required={!formData.isCurrent}
              size="small"
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
        </Grid>

        <FormControlLabel
          control={
            <Checkbox
              name="isCurrent"
              checked={formData.isCurrent}
              onChange={handleChange}
            />
          }
          label={t('job.isCurrent')}
        />

        <TextField
          fullWidth
          label={t('job.responsibilities')}
          name="responsibilities"
          value={formData.responsibilities}
          onChange={handleChange}
          multiline
          rows={3}
          size="small"
        />

        <TextField
          fullWidth
          label={t('job.achievements')}
          name="achievements"
          value={formData.achievements}
          onChange={handleChange}
          multiline
          rows={3}
          placeholder={t('job.achievementsPlaceholder')}
          size="small"
        />

        {error && (
          <Alert severity="error">
            {error instanceof Error ? error.message : t('common:errors.failedToSave')}
          </Alert>
        )}

        <Stack direction="row" spacing={2} justifyContent="flex-end">
          {onCancel && (
            <Button variant="outlined" onClick={onCancel}>
              {t('common:buttons.cancel')}
            </Button>
          )}
          <Button
            type="submit"
            variant="contained"
            disabled={isLoading}
          >
            {isLoading ? t('common:status.saving') : experience ? t('common:buttons.update') : t('common:buttons.add')}
          </Button>
        </Stack>
      </Stack>
    </form>
  );
};
