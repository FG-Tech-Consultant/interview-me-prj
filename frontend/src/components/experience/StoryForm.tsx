import React, { useState } from 'react';
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

const STAR_HELPERS = {
  situation: 'Set the scene. What was the context?',
  task: 'What was your specific responsibility?',
  action: 'What steps did you take?',
  result: 'What was the outcome? Include metrics.',
};

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

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Stack spacing={2}>
        <TextField
          label="Title *"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
          fullWidth
          size="small"
          inputProps={{ maxLength: 255 }}
          placeholder="Brief title for this STAR story"
        />

        {(['situation', 'task', 'action', 'result'] as const).map((field) => {
          const value = { situation, task, action, result }[field];
          const setter = { situation: setSituation, task: setTask, action: setAction, result: setResult }[field];
          return (
            <TextField
              key={field}
              label={`${field.charAt(0).toUpperCase() + field.slice(1)} *`}
              value={value}
              onChange={(e) => setter(e.target.value)}
              required
              fullWidth
              size="small"
              multiline
              rows={3}
              inputProps={{ maxLength: 5000 }}
              placeholder={STAR_HELPERS[field]}
              helperText={`${value.length}/5000`}
            />
          );
        })}

        <MetricsEditor value={metrics} onChange={setMetrics} />

        <TextField
          label="Visibility"
          select
          value={visibility}
          onChange={(e) => setVisibility(e.target.value)}
          fullWidth
          size="small"
        >
          <MenuItem value="private">Private</MenuItem>
          <MenuItem value="public">Public</MenuItem>
        </TextField>

        <Stack direction="row" spacing={2}>
          <Button type="submit" variant="contained" disabled={isLoading}>
            {isLoading ? 'Saving...' : isEdit ? 'Update Story' : 'Create Story'}
          </Button>
          <Button variant="outlined" onClick={onCancel}>
            Cancel
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
};
