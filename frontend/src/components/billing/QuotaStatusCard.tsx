import { useTranslation } from 'react-i18next';
import {
  Card,
  CardContent,
  Typography,
  LinearProgress,
  Box,
} from '@mui/material';

interface QuotaStatusCardProps {
  featureType: string;
  used: number;
  limit: number;
}

const FEATURE_LABEL_KEYS: Record<string, string> = {
  CHAT_MESSAGE: 'quota.chatMessages',
  LINKEDIN_DRAFT: 'quota.linkedinDrafts',
  LINKEDIN_SUGGESTION: 'quota.linkedinSuggestions',
};

export const QuotaStatusCard = ({ featureType, used, limit }: QuotaStatusCardProps) => {
  const { t } = useTranslation('billing');
  const percentage = limit > 0 ? Math.min((used / limit) * 100, 100) : 0;
  const labelKey = FEATURE_LABEL_KEYS[featureType];
  const label = labelKey ? t(labelKey) : featureType;

  const getColor = (): 'success' | 'warning' | 'error' => {
    if (percentage >= 100) return 'error';
    if (percentage >= 80) return 'warning';
    return 'success';
  };

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          {label}
        </Typography>
        <Typography variant="h6">
          {used} / {limit}
        </Typography>
        <Box sx={{ mt: 1 }}>
          <LinearProgress
            variant="determinate"
            value={percentage}
            color={getColor()}
          />
        </Box>
        <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
          {limit - used > 0 ? t('quota.remaining', { count: limit - used }) : t('quota.quotaReached')}
        </Typography>
      </CardContent>
    </Card>
  );
};
