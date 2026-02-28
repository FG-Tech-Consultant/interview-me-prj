import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Stack,
  IconButton,
  Alert,
  CircularProgress,
  Paper,
  Divider,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useJobExperiences, useDeleteJobExperience } from '../../hooks/useJobExperience';
import { JobExperienceForm } from './JobExperienceForm';
import { ProjectList } from '../experience/ProjectList';
import type { JobExperience } from '../../types/profile';

interface JobExperienceListProps {
  profileId: number;
}

export const JobExperienceList: React.FC<JobExperienceListProps> = ({ profileId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const { t } = useTranslation('profile');

  const { data: experiences, isLoading, error } = useJobExperiences(profileId);
  const deleteMutation = useDeleteJobExperience();

  const handleDelete = (experienceId: number) => {
    if (window.confirm(t('job.confirmDelete'))) {
      deleteMutation.mutate({ profileId, experienceId });
    }
  };

  const handleEdit = (experienceId: number) => {
    setEditingId(experienceId);
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
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        {t('job.errorLoading')}: {error instanceof Error ? error.message : t('common:errors.unknownError')}
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      {/* Add Button */}
      {!isAdding && !editingId && (
        <Box>
          <Button variant="contained" onClick={handleAdd}>
            {t('job.addButton')}
          </Button>
        </Box>
      )}

      {/* Add Form */}
      {isAdding && (
        <Paper elevation={2} sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            {t('job.addTitle')}
          </Typography>
          <JobExperienceForm
            profileId={profileId}
            onSuccess={handleCancelAdd}
            onCancel={handleCancelAdd}
          />
        </Paper>
      )}

      {/* Experience List */}
      <Stack spacing={2}>
        {experiences && experiences.length > 0 ? (
          experiences.map((experience) => (
            <Card key={experience.id} variant="outlined">
              <CardContent>
                {editingId === experience.id ? (
                  <JobExperienceForm
                    profileId={profileId}
                    experience={experience}
                    onSuccess={handleCancelEdit}
                    onCancel={handleCancelEdit}
                  />
                ) : (
                  <JobExperienceCard
                    experience={experience}
                    onEdit={() => handleEdit(experience.id)}
                    onDelete={() => handleDelete(experience.id)}
                  />
                )}
              </CardContent>
            </Card>
          ))
        ) : (
          <Typography color="text.secondary">
            {t('job.noExperiences')}
          </Typography>
        )}
      </Stack>
    </Stack>
  );
};

interface JobExperienceCardProps {
  experience: JobExperience;
  onEdit: () => void;
  onDelete: () => void;
}

const JobExperienceCard: React.FC<JobExperienceCardProps> = ({ experience, onEdit, onDelete }) => {
  const [showProjects, setShowProjects] = useState(false);
  const { t } = useTranslation('profile');

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString(undefined, { year: 'numeric', month: 'short' });
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1.5 }}>
        <Box>
          <Typography variant="h6">{experience.role}</Typography>
          <Typography variant="subtitle1" color="text.secondary">
            {experience.company}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {formatDate(experience.startDate)} -{' '}
            {experience.isCurrent ? t('common:present') : experience.endDate ? formatDate(experience.endDate) : t('common:na')}
            {experience.location && ` \u2022 ${experience.location}`}
          </Typography>
        </Box>
        <Stack direction="row" spacing={0.5}>
          <IconButton size="small" onClick={onEdit}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" onClick={onDelete} color="error">
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      </Box>

      {experience.responsibilities && (
        <Typography variant="body2" sx={{ mb: 1.5 }}>
          {experience.responsibilities}
        </Typography>
      )}

      {experience.achievements && (
        <Box sx={{ mb: 1.5 }}>
          <Typography variant="subtitle2">{t('job.achievements')}:</Typography>
          <Typography variant="body2">{experience.achievements}</Typography>
        </Box>
      )}

      {/* Projects Section */}
      <Divider sx={{ my: 1.5 }} />
      <Button
        size="small"
        onClick={() => setShowProjects(!showProjects)}
      >
        {showProjects ? t('job.hideProjects') : t('job.showProjects')}
      </Button>
      {showProjects && <ProjectList jobExperienceId={experience.id} />}
    </Box>
  );
};
