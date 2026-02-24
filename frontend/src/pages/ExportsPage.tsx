import { useState } from 'react';
import {
  Container,
  Typography,
  Box,
  Button,
  CircularProgress,
  Alert,
} from '@mui/material';
import {
  useExportTemplates,
  useExportHistory,
  useCreateResumeExport,
} from '../hooks/useExports';
import { ExportFormDialog } from '../components/exports/ExportFormDialog';
import { ExportProgressCard } from '../components/exports/ExportProgressCard';
import { ExportHistoryTable } from '../components/exports/ExportHistoryTable';
import { exportsApi } from '../api/exportsApi';
import type { ExportResumeRequest } from '../types/export';

export const ExportsPage = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [formOpen, setFormOpen] = useState(false);
  const [activeExportId, setActiveExportId] = useState<number | null>(null);

  const { data: templates, isLoading: templatesLoading } = useExportTemplates();
  const { data: history, isLoading: historyLoading } = useExportHistory(page, rowsPerPage);
  const createExport = useCreateResumeExport();

  const handleSubmit = (request: ExportResumeRequest) => {
    createExport.mutate(request, {
      onSuccess: (data) => {
        setFormOpen(false);
        setActiveExportId(data.id);
      },
    });
  };

  const handleDownload = async (exportId: number) => {
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
    } catch {
      // Error handled silently
    }
  };

  if (templatesLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Exports</Typography>
        <Button
          variant="contained"
          onClick={() => setFormOpen(true)}
          disabled={!templates || templates.length === 0}
        >
          New Resume Export
        </Button>
      </Box>

      {createExport.isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {(createExport.error as Error)?.message || 'Failed to create export'}
        </Alert>
      )}

      {activeExportId && (
        <ExportProgressCard
          exportId={activeExportId}
          onComplete={() => setActiveExportId(null)}
        />
      )}

      <Typography variant="h6" gutterBottom>
        Export History
      </Typography>

      {historyLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress size={24} />
        </Box>
      ) : (
        <ExportHistoryTable
          exports={history?.content ?? []}
          page={page}
          totalElements={history?.totalElements ?? 0}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={(size) => {
            setRowsPerPage(size);
            setPage(0);
          }}
          onDownload={handleDownload}
        />
      )}

      {templates && (
        <ExportFormDialog
          open={formOpen}
          onClose={() => setFormOpen(false)}
          onSubmit={handleSubmit}
          templates={templates}
          isSubmitting={createExport.isPending}
        />
      )}
    </Container>
  );
};
