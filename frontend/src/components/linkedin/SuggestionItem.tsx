import { useTranslation } from 'react-i18next';
import { Box, Typography, Button, Chip } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';

interface SuggestionItemProps {
  text: string;
  index?: number;
  isFree: boolean;
  canApply: boolean;
  isApplied: boolean;
  onApply: () => void;
  isApplying: boolean;
}

export default function SuggestionItem({
  text,
  isFree,
  canApply,
  isApplied,
  onApply,
  isApplying,
}: SuggestionItemProps) {
  const { t } = useTranslation('linkedin');

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 1,
        p: 1.5,
        borderRadius: 1,
        backgroundColor: 'grey.50',
        mb: 1,
      }}
    >
      <Chip
        label={isFree ? t('suggestion.free') : t('suggestion.paid')}
        size="small"
        color={isFree ? 'success' : 'default'}
        variant="outlined"
        sx={{ flexShrink: 0, mt: 0.25 }}
      />
      <Box sx={{ flex: 1 }}>
        <Typography variant="body2">{text}</Typography>
      </Box>
      {canApply && (
        <Box sx={{ flexShrink: 0 }}>
          {isApplied ? (
            <Chip
              icon={<CheckCircleIcon />}
              label={t('suggestion.applied')}
              size="small"
              color="success"
            />
          ) : (
            <Button
              size="small"
              variant="outlined"
              onClick={onApply}
              disabled={isApplying}
            >
              {t('suggestion.apply')}
            </Button>
          )}
        </Box>
      )}
    </Box>
  );
}
