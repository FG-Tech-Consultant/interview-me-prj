import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('experience');

  const { data: stories, isLoading, error } = useStories(experienceProjectId);
  const deleteMutation = useDeleteStory();

  const handleDelete = (storyId: number) => {
    if (window.confirm(t('common:confirm.deleteStory'))) {
      deleteMutation.mutate({ storyId, projectId: experienceProjectId });
    }
  };

  if (isLoading) return <CircularProgress size={20} />;
  if (error) return <Alert severity="error">{t('stories.errorLoading')}</Alert>;

  return (
    <Stack spacing={1}>
      {!isAdding && !editingId && (
        <Box>
          <Button
            variant="contained"
            size="small"
            onClick={() => setIsAdding(true)}
          >
            {t('stories.addButton')}
          </Button>
        </Box>
      )}

      {isAdding && (
        <Card variant="outlined" sx={{ bgcolor: 'grey.50' }}>
          <CardContent>
            <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>
              {t('stories.addTitle')}
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
            {t('stories.noStories')}
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
  const { t } = useTranslation('experience');

  const STAR_LABELS = {
    situation: t('stories.situation'),
    task: t('stories.task'),
    action: t('stories.action'),
    result: t('stories.result'),
  };

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
        <Box sx={{ flex: 1, cursor: 'pointer' }} onClick={onToggleExpand}>
          <Typography variant="subtitle2" fontWeight="bold">
            {story.title}
          </Typography>
          <Stack direction="row" spacing={0.5} sx={{ mt: 0.5 }}>
            <Chip
              label={t(`common:visibility.${story.visibility}`)}
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
            {(['situation', 'task', 'action', 'result'] as const).map((key) => (
              <Box key={key}>
                <Typography variant="subtitle2">{STAR_LABELS[key]}:</Typography>
                <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                  {story[key]}
                </Typography>
              </Box>
            ))}

            {story.metrics && Object.keys(story.metrics).length > 0 && (
              <Box>
                <Typography variant="subtitle2">{t('stories.metricsLabel')}:</Typography>
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
