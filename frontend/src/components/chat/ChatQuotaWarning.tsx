import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Typography } from '@mui/material';
import type { QuotaInfo } from '../../types/chat';

interface ChatQuotaWarningProps {
  quotaInfo: QuotaInfo | null;
}

export const ChatQuotaWarning: React.FC<ChatQuotaWarningProps> = ({ quotaInfo }) => {
  const { t } = useTranslation('chat');

  if (!quotaInfo) return null;

  const threshold = Math.floor(quotaInfo.freeLimit * 0.2);

  if (quotaInfo.usingCoins) {
    return (
      <Box sx={{ px: 2, py: 0.5, bgcolor: 'warning.light', textAlign: 'center' }}>
        <Typography variant="caption" color="warning.contrastText">
          {t('usingCredits')}
        </Typography>
      </Box>
    );
  }

  if (quotaInfo.freeRemaining > 0 && quotaInfo.freeRemaining <= threshold) {
    return (
      <Box sx={{ px: 2, py: 0.5, bgcolor: 'info.light', textAlign: 'center' }}>
        <Typography variant="caption" color="info.contrastText">
          {t('freeRemaining', { count: quotaInfo.freeRemaining })}
        </Typography>
      </Box>
    );
  }

  return null;
};
