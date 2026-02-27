import React, { useState } from 'react';
import { useStories, useDeleteStory } from '../../hooks/useStories';
import { StoryForm } from './StoryForm';
import type { StoryResponse } from '../../types/story';
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

interface StoryListProps {
  experienceProjectId: number;
}

export const StoryList: React.FC<StoryListProps> = ({ experienceProjectId }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const { data: stories, isLoading, error } = useStories(experienceProjectId);
  const deleteMutation = useDeleteStory();

  const handleDelete = (storyId: number) => {
    if (window.confirm('Are you sure you want to delete this story?')) {
      deleteMutation.mutate({ storyId, projectId: experienceProjectId });
    }
  };

  if (isLoading) return <CircularProgress size={20} />;
  if (error) return <Alert severity="error">Error loading stories.</Alert>;

  return (
    <Stack spacing={1}>
      {!isAdding && !editingId && (
        <Box>
          <Button
            variant="contained"
            size="small"
            onClick={() => setIsAdding(true)}
          >
            + Add Story
          </Button>
        </Box>
      )}

      {isAdding && (
        <Card variant="outlined" sx={{ bgcolor: 'grey.50' }}>
          <CardContent>
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>
              Add STAR Story
            </Typography>
            <StoryForm
              experienceProjectId={experienceProjectId}
              onSuccess={() => setIsAdding(false)}
              onCancel={() => setIsAdding(false)}
            />
          </CardContent>
        </Card>
      )}

      {stories && stories.length > 0 ? (
        stories.map((story) => (
          <Card key={story.id} variant="outlined">
            <CardContent sx={{ py: 1.5, '&:last-child': { pb: 1.5 } }}>
              {editingId === story.id ? (
                <StoryForm
                  experienceProjectId={experienceProjectId}
                  story={story}
                  onSuccess={() => setEditingId(null)}
                  onCancel={() => setEditingId(null)}
                />
              ) : (
                <StoryCard
                  story={story}
                  isExpanded={expandedId === story.id}
                  onToggleExpand={() =>
                    setExpandedId(expandedId === story.id ? null : story.id)
                  }
                  onEdit={() => {
                    setEditingId(story.id);
                    setIsAdding(false);
                  }}
                  onDelete={() => handleDelete(story.id)}
                />
              )}
            </CardContent>
          </Card>
        ))
      ) : (
        !isAdding && (
          <Typography variant="caption" color="text.secondary">
            No stories yet. Add a STAR story to prepare for behavioral interviews.
          </Typography>
        )
      )}
    </Stack>
  );
};

interface StoryCardProps {
  story: StoryResponse;
  isExpanded: boolean;
  onToggleExpand: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

const StoryCard: React.FC<StoryCardProps> = ({
  story,
  isExpanded,
  onToggleExpand,
  onEdit,
  onDelete,
}) => {
  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
        <Box sx={{ flex: 1, cursor: 'pointer' }} onClick={onToggleExpand}>
          <Typography variant="subtitle2" fontWeight="bold">
            {story.title}
          </Typography>
          <Stack direction="row" spacing={0.5} sx={{ mt: 0.5 }}>
            <Chip
              label={story.visibility}
              size="small"
              color={story.visibility === 'public' ? 'success' : 'default'}
            />
          </Stack>
          {!isExpanded && (
            <Typography
              variant="caption"
              color="text.secondary"
              sx={{
                mt: 0.5,
                display: 'block',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
                maxWidth: '100%',
              }}
            >
              S: {story.situation}
            </Typography>
          )}
        </Box>
        <Stack direction="row" spacing={0.5} sx={{ ml: 1 }}>
          <IconButton size="small" onClick={onEdit}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" color="error" onClick={onDelete}>
            <DeleteIcon fontSize="small" />
          </IconButton>
        </Stack>
      </Stack>

      {isExpanded && (
        <Box sx={{ mt: 1, pt: 1 }}>
          <Divider sx={{ mb: 1 }} />
          <Stack spacing={1}>
            {(['Situation', 'Task', 'Action', 'Result'] as const).map((label) => {
              const key = label.toLowerCase() as keyof Pick<StoryResponse, 'situation' | 'task' | 'action' | 'result'>;
              return (
                <Box key={label}>
                  <Typography variant="subtitle2">{label}:</Typography>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {story[key]}
                  </Typography>
                </Box>
              );
            })}

            {story.metrics && Object.keys(story.metrics).length > 0 && (
              <Box>
                <Typography variant="subtitle2">Metrics:</Typography>
                <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5 }} useFlexGap>
                  {Object.entries(story.metrics).map(([key, val]) => (
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
          </Stack>
        </Box>
      )}
    </Box>
  );
};
