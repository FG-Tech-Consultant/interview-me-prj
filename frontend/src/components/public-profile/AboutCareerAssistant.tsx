import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Typography, Link, Stack } from '@mui/material';
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
        mb: 2,
        px: { xs: 1.5, md: 2 },
        py: { xs: 1, md: 1.2 },
        borderRadius: 1.5,
        bgcolor: 'action.hover',
        border: '1px solid',
        borderColor: 'divider',
      }}
    >
      <Stack direction="row" spacing={1} alignItems="center">
        <SmartToyOutlinedIcon
          sx={{ color: 'primary.main', fontSize: 20 }}
        />
        <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1.5 }}>
          {t('aboutAssistant.description', { name: firstName })}{' '}
          <Link
            href={`${import.meta.env.BASE_URL}about`}
            target="_blank"
            rel="noopener noreferrer"
            sx={{ fontWeight: 600 }}
          >
            {t('aboutAssistant.learnMore')}
          </Link>
        </Typography>
      </Stack>
    </Box>
  );
};
