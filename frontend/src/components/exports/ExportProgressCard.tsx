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
            <Typography variant="subtitle1">Generating your resume...</Typography>
            <Typography variant="body2" color="text.secondary">
              {status.status === 'PENDING' ? 'Queued for processing' : 'Processing...'}
              {status.retryCount > 0 && ` (attempt ${status.retryCount + 1})`}
            </Typography>
          </Box>
        </Box>
      )}

      {status.status === 'COMPLETED' && (
        <Box>
          <Alert severity="success" sx={{ mb: 2 }}>
            Resume generated successfully!
          </Alert>
          <Button variant="contained" onClick={handleDownload}>
            Download PDF
          </Button>
        </Box>
      )}

      {status.status === 'FAILED' && (
        <Box>
          <Alert severity="error" sx={{ mb: 1 }}>
            Resume export failed. Your coins have been refunded.
          </Alert>
          <Typography variant="body2" color="text.secondary">
            {status.errorMessage}
          </Typography>
          <Button variant="outlined" onClick={onComplete} sx={{ mt: 1 }}>
            Dismiss
          </Button>
        </Box>
      )}
    </Paper>
  );
};
