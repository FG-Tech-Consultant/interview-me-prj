import { useTranslation } from 'react-i18next';
import {
  Paper,
  Typography,
  Box,
  CircularProgress,
  Button,
  Alert,
} from '@mui/material';
import { useExportStatus } from '../../hooks/useExports';
import { exportsApi } from '../../api/exportsApi';

interface ExportProgressCardProps {
  exportId: number;
  onComplete: () => void;
}

export const ExportProgressCard = ({ exportId, onComplete }: ExportProgressCardProps) => {
  const { data: status } = useExportStatus(exportId);
  const { t } = useTranslation('exports');

  const handleDownload = async () => {
    try {
      const blob = await exportsApi.downloadExport(exportId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `resume-${new Date().toISOString().split('T')[0]}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      onComplete();
    } catch {
      // Error handled silently
    }
  };

  if (!status) return null;

  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      {(status.status === 'PENDING' || status.status === 'IN_PROGRESS') && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <CircularProgress size={24} />
          <Box>
            <Typography variant="subtitle1">{t('progress.generating')}</Typography>
            <Typography variant="body2" color="text.secondary">
              {status.status === 'PENDING' ? t('progress.queued') : t('progress.processing')}
              {status.retryCount > 0 && ` ${t('progress.attempt', { count: status.retryCount + 1 })}`}
            </Typography>
          </Box>
        </Box>
      )}

      {status.status === 'COMPLETED' && (
        <Box>
          <Alert severity="success" sx={{ mb: 2 }}>
            {t('progress.success')}
          </Alert>
          <Button variant="contained" onClick={handleDownload}>
            {t('progress.downloadPdf')}
          </Button>
        </Box>
      )}

      {status.status === 'FAILED' && (
        <Box>
          <Alert severity="error" sx={{ mb: 1 }}>
            {t('progress.failed')}
          </Alert>
          <Typography variant="body2" color="text.secondary">
            {status.errorMessage}
          </Typography>
          <Button variant="outlined" onClick={onComplete} sx={{ mt: 1 }}>
            {t('common:buttons.dismiss')}
          </Button>
        </Box>
      )}
    </Paper>
  );
};
