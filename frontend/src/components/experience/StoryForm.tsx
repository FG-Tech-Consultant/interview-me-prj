import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useCreateStory, useUpdateStory } from '../../hooks/useStories';
import { MetricsEditor } from './MetricsEditor';
import type { StoryResponse, CreateStoryRequest, UpdateStoryRequest } from '../../types/story';
import { TextField, Button, MenuItem, Box, Stack } from '@mui/material';

interface StoryFormProps {
  experienceProjectId: number;
  story?: StoryResponse;
  onSuccess: () => void;
  onCancel: () => void;
}

const STAR_FIELDS = ['situation', 'task', 'action', 'result'] as const;

export const StoryForm: React.FC<StoryFormProps> = ({
  experienceProjectId,
  story,
  onSuccess,
  onCancel,
}) => {
  const isEdit = !!story;
  const [title, setTitle] = useState(story?.title || '');
  const [situation, setSituation] = useState(story?.situation || '');
  const [task, setTask] = useState(story?.task || '');
  const [action, setAction] = useState(story?.action || '');
  const [result, setResult] = useState(story?.result || '');
  const [metrics, setMetrics] = useState<Record<string, unknown>>(
    (story?.metrics as Record<string, unknown>) || {}
  );
  const [visibility, setVisibility] = useState(story?.visibility || 'private');
  const { t } = useTranslation('experience');

  const createMutation = useCreateStory();
  const updateMutation = useUpdateStory();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (isEdit && story) {
      const data: UpdateStoryRequest = {
        title,
        situation,
        task,
        action,
        result,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        visibility,
        version: story.version,
      };
      updateMutation.mutate(
        { storyId: story.id, projectId: experienceProjectId, data },
        { onSuccess }
      );
    } else {
      const data: CreateStoryRequest = {
        title,
        situation,
        task,
        action,
        result,
        metrics: Object.keys(metrics).length > 0 ? metrics : undefined,
        visibility,
      };
      createMutation.mutate({ projectId: experienceProjectId, data }, { onSuccess });
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending;

  const fieldState = { situation, task, action, result };
  const fieldSetters = { situation: setSituation, task: setTask, action: setAction, result: setResult };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Stack spacing={2}>
        <TextField
          label={`${t('stories.title')} *`}
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          fullWidth
          size="small"
          inputProps={{ maxLength: 255 }}
          placeholder={t('stories.titlePlaceholder')}
        />

        {STAR_FIELDS.map((field) => (
          <TextField
            key={field}
            label={`${t(`stories.${field}`)} *`}
            value={fieldState[field]}
            onChange={(e) => fieldSetters[field](e.target.value)}
            required
            fullWidth
            size="small"
            multiline
            rows={3}
            inputProps={{ maxLength: 5000 }}
            placeholder={t(`stories.${field}Helper`)}
            helperText={`${fieldState[field].length}/5000`}
          />
        ))}

        <MetricsEditor value={metrics} onChange={setMetrics} />

        <TextField
          label={t('stories.visibility')}
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
            {isLoading ? t('common:status.saving') : isEdit ? t('stories.updateButton') : t('stories.createButton')}
          </Button>
          <Button variant="outlined" onClick={onCancel}>
            {t('common:buttons.cancel')}
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
};
