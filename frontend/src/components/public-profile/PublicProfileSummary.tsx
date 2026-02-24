import React from 'react';
import { Box, Typography, Divider } from '@mui/material';

interface PublicProfileSummaryProps {
  summary: string;
}

export const PublicProfileSummary: React.FC<PublicProfileSummaryProps> = ({ summary }) => {
  return (
    <Box sx={{ mb: 4 }}>
      <Typography variant="h5" fontWeight="bold" gutterBottom>
        About
      </Typography>
      <Divider sx={{ mb: 2 }} />
      <Typography variant="body1" sx={{ whiteSpace: 'pre-line', lineHeight: 1.8 }}>
        {summary}
      </Typography>
    </Box>
  );
};
