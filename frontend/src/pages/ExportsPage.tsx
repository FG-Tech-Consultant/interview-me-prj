import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Container,
  Typography,
  Box,
  Button,
  CircularProgress,
  Alert,
  ButtonGroup,
} from '@mui/material';
import {
  useExportTemplates,
  useExportHistory,
  useCreateResumeExport,
  useCreateCoverLetterExport,
  useCreateBackgroundDeckExport,
} from '../hooks/useExports';
import { ExportFormDialog } from '../components/exports/ExportFormDialog';
import { CoverLetterFormDialog } from '../components/exports/CoverLetterFormDialog';
import { ExportProgressCard } from '../components/exports/ExportProgressCard';
import { ExportHistoryTable } from '../components/exports/ExportHistoryTable';
import { exportsApi } from '../api/exportsApi';
import type { ExportResumeRequest, ExportCoverLetterRequest } from '../types/export';

export const ExportsPage = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [resumeFormOpen, setResumeFormOpen] = useState(false);
  const [coverLetterFormOpen, setCoverLetterFormOpen] = useState(false);
  const [activeExportId, setActiveExportId] = useState<number | null>(null);
  const { t } = useTranslation('exports');

  const { data: templates, isLoading: templatesLoading } = useExportTemplates();
  const { data: history, isLoading: historyLoading } = useExportHistory(page, rowsPerPage);
  const createResumeExport = useCreateResumeExport();
  const createCoverLetterExport = useCreateCoverLetterExport();
  const createBackgroundDeckExport = useCreateBackgroundDeckExport();

  const handleResumeSubmit = (request: ExportResumeRequest) => {
    createResumeExport.mutate(request, {
      onSuccess: (data) => {
        setResumeFormOpen(false);
        setActiveExportId(data.id);
      },
    });
  };

  const handleCoverLetterSubmit = (request: ExportCoverLetterRequest) => {
    createCoverLetterExport.mutate(request, {
      onSuccess: (data) => {
        setCoverLetterFormOpen(false);
        setActiveExportId(data.id);
      },
    });
  };

  const handleBackgroundDeckSubmit = (templateId: number) => {
    createBackgroundDeckExport.mutate({ templateId }, {
      onSuccess: (data) => {
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
      link.download = `export-${new Date().toISOString().split('T')[0]}.pdf`;
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

  const hasResumeTemplates = templates?.some((t) => t.type === 'RESUME') ?? false;
  const hasCoverLetterTemplates = templates?.some((t) => t.type === 'COVER_LETTER') ?? false;
  const backgroundDeckTemplate = templates?.find((t) => t.type === 'BACKGROUND_DECK');

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">{t('title')}</Typography>
        <ButtonGroup variant="contained">
          <Button
            onClick={() => setResumeFormOpen(true)}
            disabled={!hasResumeTemplates}
          >
            {t('newExport')}
          </Button>
          <Button
            onClick={() => setCoverLetterFormOpen(true)}
            disabled={!hasCoverLetterTemplates}
          >
            New Cover Letter
          </Button>
          <Button
            onClick={() => backgroundDeckTemplate && handleBackgroundDeckSubmit(backgroundDeckTemplate.id)}
            disabled={!backgroundDeckTemplate || createBackgroundDeckExport.isPending}
          >
            New Background Deck
          </Button>
        </ButtonGroup>
      </Box>

      {(createResumeExport.isError || createCoverLetterExport.isError || createBackgroundDeckExport.isError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {(createResumeExport.error as Error)?.message
            || (createCoverLetterExport.error as Error)?.message
            || (createBackgroundDeckExport.error as Error)?.message
            || t('failedToCreate')}
        </Alert>
      )}

      {activeExportId && (
        <ExportProgressCard
          exportId={activeExportId}
          onComplete={() => setActiveExportId(null)}
        />
      )}

      <Typography variant="h6" gutterBottom>
        {t('exportHistory')}
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
        <>
          <ExportFormDialog
            open={resumeFormOpen}
            onClose={() => setResumeFormOpen(false)}
            onSubmit={handleResumeSubmit}
            templates={templates.filter((t) => t.type === 'RESUME')}
            isSubmitting={createResumeExport.isPending}
          />
          <CoverLetterFormDialog
            open={coverLetterFormOpen}
            onClose={() => setCoverLetterFormOpen(false)}
            onSubmit={handleCoverLetterSubmit}
            templates={templates}
            isSubmitting={createCoverLetterExport.isPending}
          />
        </>
      )}
    </Container>
  );
};
