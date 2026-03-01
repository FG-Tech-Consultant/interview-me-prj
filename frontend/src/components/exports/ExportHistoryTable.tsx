import { useTranslation } from 'react-i18next';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Typography,
  TablePagination,
  Tooltip,
} from '@mui/material';
import { ExportStatusBadge } from './ExportStatusBadge';
import type { ExportHistory } from '../../types/export';

interface ExportHistoryTableProps {
  exports: ExportHistory[];
  page: number;
  totalElements: number;
  rowsPerPage: number;
  onPageChange: (page: number) => void;
  onRowsPerPageChange: (size: number) => void;
  onDownload: (exportId: number) => void;
}

export const ExportHistoryTable = ({
  exports,
  page,
  totalElements,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  onDownload,
}: ExportHistoryTableProps) => {
  const { t } = useTranslation('exports');

  if (exports.length === 0 && page === 0) {
    return (
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <Typography color="text.secondary">
          {t('noExports')}
        </Typography>
      </Paper>
    );
  }

  return (
    <Paper>
      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>{t('tableHeaders.date')}</TableCell>
              <TableCell>{t('tableHeaders.template')}</TableCell>
              <TableCell>{t('tableHeaders.targetRole')}</TableCell>
              <TableCell>{t('tableHeaders.status')}</TableCell>
              <TableCell>{t('tableHeaders.credits')}</TableCell>
              <TableCell>{t('tableHeaders.actions')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {exports.map((exp) => (
              <TableRow key={exp.id}>
                <TableCell>
                  {new Date(exp.createdAt).toLocaleDateString()}
                </TableCell>
                <TableCell>{exp.template?.name}</TableCell>
                <TableCell>{exp.parameters?.targetRole || '-'}</TableCell>
                <TableCell>
                  <ExportStatusBadge status={exp.status} />
                  {exp.status === 'FAILED' && exp.errorMessage && (
                    <Tooltip title={exp.errorMessage}>
                      <Typography
                        variant="caption"
                        color="error"
                        sx={{ display: 'block', cursor: 'help' }}
                      >
                        {t('hoverForDetails')}
                      </Typography>
                    </Tooltip>
                  )}
                </TableCell>
                <TableCell>
                  {exp.coinsSpent}
                  {exp.status === 'FAILED' && (
                    <Typography variant="caption" color="success.main" sx={{ display: 'block' }}>
                      {t('refunded')}
                    </Typography>
                  )}
                </TableCell>
                <TableCell>
                  {exp.status === 'COMPLETED' && (
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => onDownload(exp.id)}
                    >
                      {t('common:buttons.download')}
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      <TablePagination
        component="div"
        count={totalElements}
        page={page}
        onPageChange={(_, newPage) => onPageChange(newPage)}
        rowsPerPage={rowsPerPage}
        onRowsPerPageChange={(e) => {
          onRowsPerPageChange(parseInt(e.target.value, 10));
          onPageChange(0);
        }}
        rowsPerPageOptions={[10, 20, 50]}
      />
    </Paper>
  );
};
