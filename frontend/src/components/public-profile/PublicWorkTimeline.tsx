import React from 'react';
import {
  Box,
  Typography,
  Chip,
  Divider,
  Stack,
} from '@mui/material';
import { PublicProjectCard } from './PublicProjectCard';
import type { PublicJobResponse } from '../../types/publicProfile';

interface PublicWorkTimelineProps {
  jobs: PublicJobResponse[];
}

const formatDate = (date: string): string => {
  const d = new Date(date);
  return d.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
};

export const PublicWorkTimeline: React.FC<PublicWorkTimelineProps> = ({ jobs }) => {
  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h5" fontWeight="bold" gutterBottom>
        Experience
      </Typography>
      <Divider sx={{ mb: 2 }} />

      {jobs.map((job, index) => (
        <Box key={index} sx={{ mb: 3, position: 'relative', pl: 4 }}>
          {/* Timeline connector */}
          <Box
            sx={{
              position: 'absolute',
              left: 8,
              top: 0,
              bottom: index < jobs.length - 1 ? -24 : 'auto',
              width: 2,
              backgroundColor: '#e0e0e0',
            }}
          />
          {/* Timeline dot */}
          <Box
            sx={{
              position: 'absolute',
              left: 2,
              top: 4,
              width: 14,
              height: 14,
              borderRadius: '50%',
              backgroundColor: job.isCurrent ? 'primary.main' : '#bdbdbd',
              border: '2px solid white',
            }}
          />

          <Stack direction="row" alignItems="center" spacing={1} flexWrap="wrap">
            <Typography variant="h6" fontWeight="bold">
              {job.company}
            </Typography>
            {job.isCurrent && (
              <Chip label="Current" size="small" color="primary" />
            )}
          </Stack>

          <Typography variant="subtitle1" color="text.secondary">
            {job.role}
          </Typography>

          <Typography variant="body2" color="text.secondary">
            {formatDate(job.startDate)} - {job.endDate ? formatDate(job.endDate) : 'Present'}
            {job.location && ` | ${job.location}`}
            {job.employmentType && ` | ${job.employmentType}`}
          </Typography>

          {job.responsibilities && (
            <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-line' }}>
              {job.responsibilities}
            </Typography>
          )}

          {job.achievements && (
            <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-line' }}>
              {job.achievements}
            </Typography>
          )}

          {job.metrics && Object.keys(job.metrics).length > 0 && (
            <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ mt: 1, gap: 0.5 }}>
              {Object.entries(job.metrics).map(([key, val]) => (
                <Chip key={key} label={`${key}: ${val}`} size="small" color="info" variant="outlined" />
              ))}
            </Stack>
          )}

          {job.projects.length > 0 && (
            <Box sx={{ mt: 1 }}>
              {job.projects.map((project, pIdx) => (
                <PublicProjectCard key={pIdx} project={project} />
              ))}
            </Box>
          )}
        </Box>
      ))}
    </Box>
  );
};
