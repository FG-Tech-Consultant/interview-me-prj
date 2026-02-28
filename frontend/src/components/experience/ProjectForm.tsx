import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCreateProject, useUpdateProject } from '../../hooks/useProjects';
import { MetricsEditor } from './MetricsEditor';
import type { ProjectResponse, CreateProjectRequest, UpdateProjectRequest } from '../../types/experienceProject';
import { TextField, Button, Grid, MenuItem, Box, Stack } from '@mui/material';

interface ProjectFormProps {
  jobExperienceId: number;
  project?: ProjectResponse;
  onSuccess: () => void;
  onCancel: () => void;
}

export const ProjectForm: React.FC<ProjectFormProps> = ({
  jobExperienceId,
  project,
  onSuccess,
  onCancel,
}) => {
  const isEdit = !!project;
  const [title, setTitle] = useState(project?.title || '');
  const [context, setContext] = useState(project?.context || '');
  const [role, setRole] = useState(project?.role || '');
  const [teamSize, setTeamSize] = useState<string>(project?.teamSize?.toString() || '');
  const [techStackInput, setTechStackInput] = useState(project?.techStack?.join(', ') || '');
  const [architectureType, setArchitectureType] = useState(project?.architectureType || '');
  const [metrics, setMetrics] = useState<Record<string, unknown>>(
    (project?.metrics as Record<string, unknown>) || {}
  );
  const [outcomes, setOutcomes] = useState(project?.outcomes || '');
  const [visibility, setVisibility] = useState(project?.visibility || 'private');
  const { t } = useTranslation('experience');

  const createMutation = useCreateProject();
  const updateMutation = useUpdateProject();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const techStack = techStackInput
      .split(',')
      .map((s) => s.trim())
      .filter(Boolean);

    if (isEdit && project) {
      const data: UpdateProjectRequest = {
        title,
        context: context || undefined,
        role: role || undefined,
        teamSize: teamSize ? parseInt(teamSize, 10) : undefined,
        techStack: techStack.length > 0 ? techStack : undefined,
        architectureType: architectureType || undefined,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        outcomes: outcomes || undefined,
        visibility,
        version: project.version,
      };
      updateMutation.mutate(
        { projectId: project.id, jobId: jobExperienceId, data },
        { onSuccess }
      );
    } else {
      const data: CreateProjectRequest = {
        title,
        context: context || undefined,
        role: role || undefined,
        teamSize: teamSize ? parseInt(teamSize, 10) : undefined,
        techStack: techStack.length > 0 ? techStack : undefined,
        architectureType: architectureType || undefined,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        outcomes: outcomes || undefined,
        visibility,
      };
      createMutation.mutate({ jobId: jobExperienceId, data }, { onSuccess });
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Stack spacing={2}>
        <TextField
          label={`${t('projects.title')} *`}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          fullWidth
          size="small"
          inputProps={{ maxLength: 255 }}
        />

        <TextField
          label={t('projects.context')}
          value={context}
          onChange={(e) => setContext(e.target.value)}
          fullWidth
          size="small"
          multiline
          rows={3}
          inputProps={{ maxLength: 5000 }}
          placeholder={t('projects.contextPlaceholder')}
        />

        <Grid container spacing={2}>
          <Grid item xs={12} sm={6}>
            <TextField
              label={t('projects.yourRole')}
              value={role}
              onChange={(e) => setRole(e.target.value)}
              fullWidth
              size="small"
              inputProps={{ maxLength: 255 }}
            />
          </Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              label={t('projects.teamSize')}
              type="number"
              value={teamSize}
              onChange={(e) => setTeamSize(e.target.value)}
              fullWidth
              size="small"
              inputProps={{ min: 1, max: 1000 }}
            />
          </Grid>
        </Grid>

        <TextField
          label={t('projects.techStack')}
          value={techStackInput}
          onChange={(e) => setTechStackInput(e.target.value)}
          fullWidth
          size="small"
          placeholder={t('projects.techStackPlaceholder')}
        />

        <TextField
          label={t('projects.architectureType')}
          value={architectureType}
          onChange={(e) => setArchitectureType(e.target.value)}
          fullWidth
          size="small"
          inputProps={{ maxLength: 100 }}
          placeholder={t('projects.architecturePlaceholder')}
        />

        <MetricsEditor value={metrics} onChange={setMetrics} />

        <TextField
          label={t('projects.outcomes')}
          value={outcomes}
          onChange={(e) => setOutcomes(e.target.value)}
          fullWidth
          size="small"
          multiline
          rows={3}
          inputProps={{ maxLength: 5000 }}
          placeholder={t('projects.outcomesPlaceholder')}
        />

        <TextField
          label={t('projects.visibility')}
          select
          value={visibility}
          onChange={(e) => setVisibility(e.target.value)}
          fullWidth
          size="small"
        >
          <MenuItem value="private">{t('common:visibility.private')}</MenuItem>
          <MenuItem value="public">{t('common:visibility.public')}</MenuItem>
        </TextField>

        <Stack direction="row" spacing={2}>
          <Button type="submit" variant="contained" disabled={isLoading}>
            {isLoading ? t('common:status.saving') : isEdit ? t('projects.updateButton') : t('projects.createButton')}
          </Button>
          <Button variant="outlined" onClick={onCancel}>
            {t('common:buttons.cancel')}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
};
