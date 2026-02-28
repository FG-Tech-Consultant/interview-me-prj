import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Typography, Button, Container } from '@mui/material';
import { useNavigate } from 'react-router-dom';

export const PublicProfileNotFound: React.FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation('public-profile');

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
          {t('notFound.title')}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
          {t('notFound.message')}
        </Typography>
        <Button variant="contained" onClick={() => navigate('/')}>
          {t('notFound.goHome')}
        </Button>
      </Box>
    </Container>
  );
};
