import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Typography, Stack } from '@mui/material';
import SmartToyOutlinedIcon from '@mui/icons-material/SmartToyOutlined';

interface AboutCareerAssistantProps {
  profileName: string;
}

export const AboutCareerAssistant: React.FC<AboutCareerAssistantProps> = ({ profileName }) => {
  const { t } = useTranslation('public-profile');
  const firstName = profileName.split(' ')[0];

  return (
    <Box
      sx={{
        mb: 3,
        p: { xs: 2, md: 2.5 },
        borderRadius: 2,
        bgcolor: 'action.hover',
        border: '1px solid',
        borderColor: 'divider',
      }}
    >
      <Stack direction="row" spacing={1.5} alignItems="flex-start">
        <SmartToyOutlinedIcon
          sx={{ color: 'primary.main', fontSize: 28, mt: 0.3 }}
        />
        <Box>
          <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
            {t('aboutAssistant.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ lineHeight: 1.7 }}>
            {t('aboutAssistant.description', { name: firstName })}
          </Typography>
        </Box>
      </Stack>
    </Box>
  );
};
