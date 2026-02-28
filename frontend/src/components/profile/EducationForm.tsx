import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  TextField,
  Button,
  Grid,
  Alert,
  Stack,
} from '@mui/material';
import { useCreateEducation, useUpdateEducation } from '../../hooks/useEducation';
import type { Education, CreateEducationRequest } from '../../types/profile';

interface EducationFormProps {
  profileId: number;
  education?: Education;
  onSuccess?: () => void;
  onCancel?: () => void;
}

export const EducationForm: React.FC<EducationFormProps> = ({
  profileId,
  education,
  onSuccess,
  onCancel,
}) => {
  const [formData, setFormData] = useState({
    institution: education?.institution || '',
    degree: education?.degree || '',
    fieldOfStudy: education?.fieldOfStudy || '',
    startDate: education?.startDate || '',
    endDate: education?.endDate || '',
    gpa: education?.gpa || '',
    notes: education?.notes || '',
  });
  const { t } = useTranslation('profile');

  const createMutation = useCreateEducation();
  const updateMutation = useUpdateEducation();

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

    if (education) {
      updateMutation.mutate(
        {
          profileId,
          educationId: education.id,
          data: {
            institution: formData.institution,
            degree: formData.degree,
            fieldOfStudy: formData.fieldOfStudy || undefined,
            startDate: formData.startDate || undefined,
            endDate: formData.endDate,
            gpa: formData.gpa || undefined,
            notes: formData.notes || undefined,
            version: education.version,
          },
        },
        {
          onSuccess: () => {
            onSuccess?.();
          },
        }
      );
    } else {
      const createData: CreateEducationRequest = {
        institution: formData.institution,
        degree: formData.degree,
        fieldOfStudy: formData.fieldOfStudy || undefined,
        startDate: formData.startDate || undefined,
        endDate: formData.endDate,
        gpa: formData.gpa || undefined,
        notes: formData.notes || undefined,
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
              label={t('education.institution')}
              name="institution"
              value={formData.institution}
              onChange={handleChange}
              required
              size="small"
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={t('education.degree')}
              name="degree"
              value={formData.degree}
              onChange={handleChange}
              required
              placeholder={t('education.degreePlaceholder')}
              size="small"
            />
          </Grid>
        </Grid>

        <TextField
          fullWidth
          label={t('education.fieldOfStudy')}
          name="fieldOfStudy"
          value={formData.fieldOfStudy}
          onChange={handleChange}
          placeholder={t('education.fieldOfStudyPlaceholder')}
          size="small"
        />

        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={t('education.startDate')}
              name="startDate"
              type="date"
              value={formData.startDate}
              onChange={handleChange}
              size="small"
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              label={`${t('education.endDate')} *`}
              name="endDate"
              type="date"
              value={formData.endDate}
              onChange={handleChange}
              required
              size="small"
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
        </Grid>

        <TextField
          fullWidth
          label={t('education.gpa')}
          name="gpa"
          value={formData.gpa}
          onChange={handleChange}
          placeholder={t('education.gpaPlaceholder')}
          size="small"
        />

        <TextField
          fullWidth
          label={t('education.notes')}
          name="notes"
          value={formData.notes}
          onChange={handleChange}
          multiline
          rows={3}
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
            {isLoading ? t('common:status.saving') : education ? t('common:buttons.update') : t('common:buttons.add')}
          </Button>
        </Stack>
      </Stack>
    </form>
  );
};
