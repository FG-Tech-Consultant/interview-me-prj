import React from 'react';
import {
  Box,
  Typography,
  Stack,
} from '@mui/material';
import SchoolIcon from '@mui/icons-material/School';
import type { PublicEducationResponse } from '../../types/publicProfile';

interface PublicEducationSectionProps {
  education: PublicEducationResponse[];
}

const formatDate = (date: string): string => {
  const d = new Date(date);
  return d.toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
};

export const PublicEducationSection: React.FC<PublicEducationSectionProps> = ({ education }) => {
  return (
    <Box>
      {education.map((edu, index) => (
        <Stack key={index} direction="row" spacing={2} sx={{ mb: 2 }}>
          <SchoolIcon color="action" sx={{ mt: 0.5 }} />
          <Box>
            <Typography variant="subtitle1" fontWeight="bold">
              {edu.degree}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {edu.institution}
              {edu.fieldOfStudy && ` - ${edu.fieldOfStudy}`}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {edu.startDate ? `${formatDate(edu.startDate)} - ` : ''}
              {formatDate(edu.endDate)}
            </Typography>
          </Box>
        </Stack>
      ))}
    </Box>
  );
};
