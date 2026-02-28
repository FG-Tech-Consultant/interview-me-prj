import { useTranslation } from 'react-i18next';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Typography,
  Box,
  Button,
  Chip,
  IconButton,
} from '@mui/material';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import RemoveIcon from '@mui/icons-material/Remove';
import VisibilityIcon from '@mui/icons-material/Visibility';
import type { LinkedInAnalysisSummary } from '../../types/linkedinAnalysis';

interface AnalysisHistoryProps {
  analyses: LinkedInAnalysisSummary[];
  onViewDetails: (id: number) => void;
  onNewAnalysis: () => void;
}

function getStatusColor(status: string): 'success' | 'warning' | 'error' | 'default' {
  switch (status) {
    case 'COMPLETED': return 'success';
    case 'PENDING':
    case 'IN_PROGRESS': return 'warning';
    case 'FAILED': return 'error';
    default: return 'default';
  }
}

function TrendIndicator({ current, previous }: { current: number | null; previous: number | null }) {
  if (current == null || previous == null) {
    return <RemoveIcon fontSize="small" color="disabled" />;
  }
  if (current > previous) {
    return <ArrowUpwardIcon fontSize="small" color="success" />;
  }
  if (current < previous) {
    return <ArrowDownwardIcon fontSize="small" color="error" />;
  }
  return <RemoveIcon fontSize="small" color="disabled" />;
}

export default function AnalysisHistory({
  analyses,
  onViewDetails,
  onNewAnalysis,
}: AnalysisHistoryProps) {
  const { t } = useTranslation('linkedin');

  if (analyses.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', py: 3 }}>
        <Typography variant="body2" color="text.secondary">
          {t('history.noHistory')}
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">{t('history.title')}</Typography>
        <Button variant="outlined" size="small" onClick={onNewAnalysis}>
          {t('history.newAnalysis')}
        </Button>
      </Box>

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>{t('history.date')}</TableCell>
              <TableCell>{t('history.file')}</TableCell>
              <TableCell align="center">{t('history.score')}</TableCell>
              <TableCell align="center">{t('history.trend')}</TableCell>
              <TableCell align="center">{t('history.status')}</TableCell>
              <TableCell align="center">{t('history.actions')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {analyses.map((analysis, index) => (
              <TableRow key={analysis.id} hover>
                <TableCell>
                  {analysis.analyzedAt
                    ? new Date(analysis.analyzedAt).toLocaleDateString()
                    : new Date(analysis.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell>
                  <Typography variant="body2" noWrap sx={{ maxWidth: 200 }}>
                    {analysis.pdfFilename || '-'}
                  </Typography>
                </TableCell>
                <TableCell align="center">
                  {analysis.overallScore != null ? (
                    <Typography fontWeight="bold">{analysis.overallScore}</Typography>
                  ) : (
                    '-'
                  )}
                </TableCell>
                <TableCell align="center">
                  <TrendIndicator
                    current={analysis.overallScore}
                    previous={index + 1 < analyses.length ? analyses[index + 1].overallScore : null}
                  />
                </TableCell>
                <TableCell align="center">
                  <Chip
                    label={analysis.status}
                    size="small"
                    color={getStatusColor(analysis.status)}
                  />
                </TableCell>
                <TableCell align="center">
                  <IconButton size="small" onClick={() => onViewDetails(analysis.id)}>
                    <VisibilityIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
