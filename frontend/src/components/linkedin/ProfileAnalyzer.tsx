import { useTranslation } from 'react-i18next';
import {
  Box,
  Button,
  Typography,
  Paper,
  Alert,
  CircularProgress,
} from '@mui/material';
import PersonSearchIcon from '@mui/icons-material/PersonSearch';

interface ProfileAnalyzerProps {
  onAnalyze: () => void;
  isLoading: boolean;
  error: string | null;
}

export default function ProfileAnalyzer({ onAnalyze, isLoading, error }: ProfileAnalyzerProps) {
  const { t } = useTranslation('linkedin');

  return (
    <Paper
      elevation={2}
      sx={{
        p: 4,
        textAlign: 'center',
      }}
    >
      <PersonSearchIcon sx={{ fontSize: 64, color: 'primary.main', mb: 2 }} />
      <Typography variant="h6" sx={{ mb: 1 }}>
        {t('profileAnalyzer.title')}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3, maxWidth: 480, mx: 'auto' }}>
        {t('profileAnalyzer.description')}
      </Typography>
      <Box>
        <Button
          variant="contained"
          size="large"
          onClick={onAnalyze}
          disabled={isLoading}
          startIcon={isLoading ? <CircularProgress size={20} /> : <PersonSearchIcon />}
        >
          {isLoading ? t('profileAnalyzer.analyzingButton') : t('profileAnalyzer.analyzeButton')}
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {error}
        </Alert>
      )}
    </Paper>
  );
}
