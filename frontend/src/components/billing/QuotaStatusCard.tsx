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

const FEATURE_LABELS: Record<string, string> = {
  CHAT_MESSAGE: 'Chat Messages',
  LINKEDIN_DRAFT: 'LinkedIn Drafts',
  LINKEDIN_SUGGESTION: 'LinkedIn Suggestions',
};

export const QuotaStatusCard = ({ featureType, used, limit }: QuotaStatusCardProps) => {
  const percentage = limit > 0 ? Math.min((used / limit) * 100, 100) : 0;
  const label = FEATURE_LABELS[featureType] || featureType;

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
          {limit - used > 0 ? `${limit - used} remaining` : 'Quota reached'}
        </Typography>
      </CardContent>
    </Card>
  );
};
