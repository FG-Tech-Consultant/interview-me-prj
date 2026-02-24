import React from 'react';
import {
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Typography,
  Chip,
  Box,
  Stack,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import type { PublicStoryResponse } from '../../types/publicProfile';

interface PublicStoryCardProps {
  story: PublicStoryResponse;
}

const STAR_SECTIONS = [
  { key: 'situation' as const, label: 'S', color: '#1976d2' },
  { key: 'task' as const, label: 'T', color: '#388e3c' },
  { key: 'action' as const, label: 'A', color: '#f57c00' },
  { key: 'result' as const, label: 'R', color: '#d32f2f' },
];

export const PublicStoryCard: React.FC<PublicStoryCardProps> = ({ story }) => {
  return (
    <Accordion variant="outlined" sx={{ mb: 1 }}>
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap">
          <Typography variant="subtitle2">{story.title}</Typography>
          {story.linkedSkills.map((skill) => (
            <Chip key={skill} label={skill} size="small" variant="outlined" />
          ))}
          {story.metrics && Object.entries(story.metrics).map(([key, val]) => (
            <Chip
              key={key}
              label={`${key}: ${val}`}
              size="small"
              color="info"
              variant="outlined"
            />
          ))}
        </Stack>
      </AccordionSummary>
      <AccordionDetails>
        {STAR_SECTIONS.map(({ key, label, color }) => (
          <Box key={key} sx={{ mb: 2, pl: 2, borderLeft: `3px solid ${color}` }}>
            <Typography variant="caption" fontWeight="bold" color={color}>
              {label}:
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
              {story[key]}
            </Typography>
          </Box>
        ))}
      </AccordionDetails>
    </Accordion>
  );
};
