import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Button,
  IconButton,
  Typography,
  Box,
  Stack,
  Card,
  CardContent,
  Paper,
  CircularProgress,
  Alert,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useEducations, useDeleteEducation } from '../../hooks/useEducation';
import { EducationForm } from './EducationForm';
import type { Education } from '../../types/profile';

interface EducationListProps {
  profileId: number;
}

export const EducationList: React.FC<EducationListProps> = ({ profileId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { t } = useTranslation('profile');

  const { data: educations, isLoading, error } = useEducations(profileId);
  const deleteMutation = useDeleteEducation();

  const handleDelete = (educationId: number) => {
    if (window.confirm(t('education.confirmDelete'))) {
      deleteMutation.mutate({ profileId, educationId });
    }
  };

  const handleEdit = (educationId: number) => {
    setEditingId(educationId);
    setIsAdding(false);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
  };

  const handleAdd = () => {
    setIsAdding(true);
    setEditingId(null);
  };

  const handleCancelAdd = () => {
    setIsAdding(false);
  };

  if (isLoading) {
    return <CircularProgress />;
  }

  if (error) {
    return (
      <Alert severity="error">
        {t('education.errorLoading')}: {error instanceof Error ? error.message : t('common:errors.unknownError')}
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      {!isAdding && !editingId && (
        <Box>
          <Button variant="contained" onClick={handleAdd}>
            {t('education.addButton')}
          </Button>
        </Box>
      )}

      {isAdding && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            {t('education.addTitle')}
          </Typography>
          <EducationForm
            profileId={profileId}
            onSuccess={handleCancelAdd}
            onCancel={handleCancelAdd}
          />
        </Paper>
      )}

      <Stack spacing={2}>
        {educations && educations.length > 0 ? (
          educations.map((education) => (
            <Card key={education.id} variant="outlined">
              <CardContent>
                {editingId === education.id ? (
                  <EducationForm
                    profileId={profileId}
                    education={education}
                    onSuccess={handleCancelEdit}
                    onCancel={handleCancelEdit}
                  />
                ) : (
                  <EducationCard
                    education={education}
                    onEdit={() => handleEdit(education.id)}
                    onDelete={() => handleDelete(education.id)}
                  />
                )}
              </CardContent>
            </Card>
          ))
        ) : (
          <Typography color="text.secondary">
            {t('education.noRecords')}
          </Typography>
        )}
      </Stack>
    </Stack>
  );
};

interface EducationCardProps {
  education: Education;
  onEdit: () => void;
  onDelete: () => void;
}

const EducationCard: React.FC<EducationCardProps> = ({ education, onEdit, onDelete }) => {
  const { t } = useTranslation('profile');

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString(undefined, { year: 'numeric', month: 'short' });
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
        <Box>
          <Typography variant="h6">{education.degree}</Typography>
          <Typography variant="body1">{education.institution}</Typography>
          {education.fieldOfStudy && (
            <Typography variant="body2" color="text.secondary">
              {education.fieldOfStudy}
            </Typography>
          )}
          <Typography variant="caption" color="text.secondary">
            {education.startDate ? formatDate(education.startDate) : ''} - {education.endDate ? formatDate(education.endDate) : t('common:na')}
          </Typography>
        </Box>
        <Stack direction="row" spacing={1}>
          <IconButton size="small" onClick={onEdit}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" color="error" onClick={onDelete}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      </Box>

      {education.gpa && (
        <Typography variant="body2" sx={{ mb: 1 }}>
          <strong>{t('education.gpa')}:</strong> {education.gpa}
        </Typography>
      )}

      {education.notes && (
        <Typography variant="body2" color="text.secondary">
          {education.notes}
        </Typography>
      )}
    </Box>
  );
};
