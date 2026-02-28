import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useProjects, useDeleteProject } from '../../hooks/useProjects';
import { ProjectForm } from './ProjectForm';
import { StoryList } from './StoryList';
import type { ProjectResponse } from '../../types/experienceProject';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Alert,
  IconButton,
  Stack,
  Typography,
  Divider,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';

interface ProjectListProps {
  jobExperienceId: number;
}

export const ProjectList: React.FC<ProjectListProps> = ({ jobExperienceId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const { t } = useTranslation('experience');

  const { data: projects, isLoading, error } = useProjects(jobExperienceId);
  const deleteMutation = useDeleteProject();

  const handleDelete = (projectId: number) => {
    if (window.confirm(t('common:confirm.deleteProject'))) {
      deleteMutation.mutate({ projectId, jobId: jobExperienceId });
    }
  };

  if (isLoading) return <CircularProgress size={24} />;
  if (error) return <Alert severity="error">{t('projects.errorLoading')}</Alert>;

  return (
    <Stack spacing={2} sx={{ mt: 2 }}>
      {!isAdding && !editingId && (
        <Box>
          <Button
            variant="contained"
            color="success"
            size="small"
            onClick={() => setIsAdding(true)}
          >
            {t('projects.addButton')}
          </Button>
        </Box>
      )}

      {isAdding && (
        <Card variant="outlined" sx={{ bgcolor: 'grey.50' }}>
          <CardContent>
            <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 2 }}>
              {t('projects.addTitle')}
            </Typography>
            <ProjectForm
              jobExperienceId={jobExperienceId}
              onSuccess={() => setIsAdding(false)}
              onCancel={() => setIsAdding(false)}
            />
          </CardContent>
        </Card>
      )}

      {projects && projects.length > 0 ? (
        projects.map((project) => (
          <Card key={project.id} variant="outlined">
            <CardContent>
              {editingId === project.id ? (
                <ProjectForm
                  jobExperienceId={jobExperienceId}
                  project={project}
                  onSuccess={() => setEditingId(null)}
                  onCancel={() => setEditingId(null)}
                />
              ) : (
                <ProjectCard
                  project={project}
                  isExpanded={expandedId === project.id}
                  onToggleExpand={() =>
                    setExpandedId(expandedId === project.id ? null : project.id)
                  }
                  onEdit={() => {
                    setEditingId(project.id);
                    setIsAdding(false);
                  }}
                  onDelete={() => handleDelete(project.id)}
                />
              )}
            </CardContent>
          </Card>
        ))
      ) : (
        !isAdding && (
          <Typography variant="body2" color="text.secondary">
            {t('projects.noProjects')}
          </Typography>
        )
      )}
    </Stack>
  );
};

interface ProjectCardProps {
  project: ProjectResponse;
  isExpanded: boolean;
  onToggleExpand: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

const ProjectCard: React.FC<ProjectCardProps> = ({
  project,
  isExpanded,
  onToggleExpand,
  onEdit,
  onDelete,
}) => {
  const { t } = useTranslation('experience');

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
        <Box sx={{ flex: 1, cursor: 'pointer' }} onClick={onToggleExpand}>
          <Typography variant="h6">{project.title}</Typography>
          <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 0.5 }} useFlexGap>
            {project.role && (
              <Typography variant="body2" color="text.secondary">
                {project.role}
              </Typography>
            )}
            {project.teamSize && (
              <Typography variant="body2" color="text.secondary">
                {t('projects.team', { size: project.teamSize })}
              </Typography>
            )}
            {project.architectureType && (
              <Chip label={project.architectureType} size="small" variant="outlined" />
            )}
            <Chip
              label={t(`common:visibility.${project.visibility}`)}
              size="small"
              color={project.visibility === 'public' ? 'success' : 'default'}
            />
            <Chip
              label={t('projects.story', { count: project.storyCount })}
              size="small"
              color="primary"
              variant="outlined"
            />
          </Stack>
          {project.techStack && project.techStack.length > 0 && (
            <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 1 }} useFlexGap>
              {project.techStack.map((tech) => (
                <Chip
                  key={tech}
                  label={tech}
                  size="small"
                  color="primary"
                  variant="outlined"
                />
              ))}
            </Stack>
          )}
        </Box>
        <Stack direction="row" spacing={0.5} sx={{ ml: 2 }}>
          <IconButton size="small" onClick={onEdit}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" color="error" onClick={onDelete}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      </Stack>

      {isExpanded && (
        <Box sx={{ mt: 2, pt: 2 }}>
          <Divider sx={{ mb: 2 }} />
          {project.context && (
            <Box sx={{ mb: 1 }}>
              <Typography variant="subtitle2" component="span">{t('projects.contextLabel')}: </Typography>
              <Typography variant="body2" component="span" color="text.secondary">
                {project.context}
              </Typography>
            </Box>
          )}
          {project.outcomes && (
            <Box sx={{ mb: 1 }}>
              <Typography variant="subtitle2" component="span">{t('projects.outcomesLabel')}: </Typography>
              <Typography variant="body2" component="span" color="text.secondary">
                {project.outcomes}
              </Typography>
            </Box>
          )}
          {project.metrics && Object.keys(project.metrics).length > 0 && (
            <Box sx={{ mb: 2 }}>
              <Typography variant="subtitle2">{t('projects.metricsLabel')}:</Typography>
              <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }} useFlexGap>
                {Object.entries(project.metrics).map(([key, val]) => (
                  <Chip
                    key={key}
                    label={`${key}: ${String(val)}`}
                    size="small"
                    color="secondary"
                    variant="outlined"
                  />
                ))}
              </Stack>
            </Box>
          )}

          <Box sx={{ mt: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              {t('stories.storiesCount', { count: project.storyCount })}
            </Typography>
            <StoryList experienceProjectId={project.id} />
          </Box>
        </Box>
      )}
    </Box>
  );
};
