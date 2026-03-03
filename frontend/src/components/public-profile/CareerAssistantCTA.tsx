import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Button, Typography, Stack } from '@mui/material';
import ChatIcon from '@mui/icons-material/Chat';

interface CareerAssistantCTAProps {
  profileName: string;
  onChatOpen: () => void;
}

export const CareerAssistantCTA: React.FC<CareerAssistantCTAProps> = ({
  profileName,
  onChatOpen,
}) => {
  const { t } = useTranslation('public-profile');
  const firstName = profileName.split(' ')[0];

  return (
    <Box
      sx={{
        mb: 4,
        p: { xs: 2.5, md: 3 },
        borderRadius: 3,
        background: 'linear-gradient(135deg, #1976d2 0%, #1565c0 50%, #0d47a1 100%)',
        color: 'white',
        cursor: 'pointer',
        transition: 'transform 0.2s, box-shadow 0.2s',
        position: 'relative',
        zIndex: 1,
        overflow: 'hidden',
        '&:hover': {
          transform: 'translateY(-2px)',
          boxShadow: '0 8px 24px rgba(25, 118, 210, 0.4)',
        },
      }}
      onClick={onChatOpen}
    >
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        alignItems="center"
        justifyContent="space-between"
        spacing={2}
      >
        <Stack direction="row" alignItems="center" spacing={2}>
          <ChatIcon sx={{ fontSize: { xs: 36, md: 44 } }} />
          <Box>
            <Typography
              variant="h6"
              fontWeight="bold"
              sx={{ fontSize: { xs: '1rem', md: '1.25rem' } }}
            >
              {t('cta.title', { name: firstName })}
            </Typography>
            <Typography
              variant="body2"
              sx={{ opacity: 0.9, fontSize: { xs: '0.8rem', md: '0.875rem' } }}
            >
              {t('cta.subtitle')}
            </Typography>
          </Box>
        </Stack>
        <Button
          variant="contained"
          size="large"
          startIcon={<ChatIcon />}
          onClick={(e) => {
            e.stopPropagation();
            onChatOpen();
          }}
          sx={{
            bgcolor: 'white',
            color: 'primary.dark',
            fontWeight: 'bold',
            px: 3,
            py: 1.2,
            borderRadius: 2,
            textTransform: 'none',
            fontSize: '0.95rem',
            whiteSpace: 'nowrap',
            '&:hover': {
              bgcolor: 'rgba(255,255,255,0.9)',
            },
          }}
        >
          {t('cta.button')}
        </Button>
      </Stack>
    </Box>
  );
};
