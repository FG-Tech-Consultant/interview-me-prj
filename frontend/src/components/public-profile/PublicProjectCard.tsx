import React from 'react';
import {
  Box,
  Typography,
  Chip,
  Stack,
} from '@mui/material';
import { PublicStoryCard } from './PublicStoryCard';
import type { PublicProjectResponse } from '../../types/publicProfile';

interface PublicProjectCardProps {
  project: PublicProjectResponse;
}

export const PublicProjectCard: React.FC<PublicProjectCardProps> = ({ project }) => {
  return (
    <Box sx={{ ml: 2, mt: 1, mb: 2, pl: 2, borderLeft: '2px solid #e0e0e0' }}>
      <Typography variant="subtitle1" fontWeight="bold">
        {project.title}
      </Typography>

      {(project.role || project.teamSize) && (
        <Typography variant="body2" color="text.secondary">
          {project.role && `${project.role}`}
          {project.role && project.teamSize && ' | '}
          {project.teamSize && `Team: ${project.teamSize}`}
        </Typography>
      )}

      {project.techStack && project.techStack.length > 0 && (
        <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5, gap: 0.5 }}>
          {project.techStack.map((tech) => (
            <Chip key={tech} label={tech} size="small" variant="outlined" color="primary" />
          ))}
        </Stack>
      )}

      {project.metrics && Object.keys(project.metrics).length > 0 && (
        <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 0.5, gap: 0.5 }}>
          {Object.entries(project.metrics).map(([key, val]) => (
            <Chip key={key} label={`${key}: ${val}`} size="small" color="info" variant="outlined" />
          ))}
        </Stack>
      )}

      {project.outcomes && (
        <Typography variant="body2" sx={{ mt: 1 }}>
          {project.outcomes}
        </Typography>
      )}

      {project.stories.length > 0 && (
        <Box sx={{ mt: 1 }}>
          {project.stories.map((story, idx) => (
            <PublicStoryCard key={idx} story={story} />
          ))}
        </Box>
      )}
    </Box>
  );
};
