import React from 'react';
import { Box, Typography, Button, Container } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export const PublicProfileNotFound: React.FC = () => {
  const navigate = useNavigate();

  return (
    <Container maxWidth="sm">
      <Box
        sx={{
          mt: 12,
          textAlign: 'center',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
        }}
      >
        <Typography variant="h1" sx={{ fontSize: '6rem', fontWeight: 'bold', color: 'text.secondary' }}>
          404
        </Typography>
        <Typography variant="h5" gutterBottom>
          Profile not found
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          The profile you are looking for does not exist or may have been removed.
          Please check the URL and try again.
        </Typography>
        <Button variant="contained" onClick={() => navigate('/')}>
          Go to Homepage
        </Button>
      </Box>
    </Container>
  );
};
