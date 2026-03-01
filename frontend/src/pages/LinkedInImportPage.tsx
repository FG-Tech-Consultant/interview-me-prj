import { useState, useCallback, useRef, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Container,
  Box,
  Typography,
  Paper,
  Stepper,
  Step,
  StepLabel,
  Button,
  Alert,
  CircularProgress,
  Card,
  CardContent,
  Chip,
  RadioGroup,
  Radio,
  FormControlLabel,
  Checkbox,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import {
  linkedinImportApi,
  type ImportPreviewResponse,
  type ImportResultResponse,
} from '../api/linkedinImportApi';
import { useCurrentProfile } from '../hooks/useProfile';
import { useUserSkills } from '../hooks/useSkills';

const MAX_SIZE_MB = 50;
const MAX_SIZE_BYTES = MAX_SIZE_MB * 1024 * 1024;

export function LinkedInImportPage() {
  const { t } = useTranslation('linkedin');
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [activeStep, setActiveStep] = useState(0);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [preview, setPreview] = useState<ImportPreviewResponse | null>(null);
  const [importResult, setImportResult] = useState<ImportResultResponse | null>(null);
  const [strategy, setStrategy] = useState<string>('MERGE');
  const [overwriteConfirmed, setOverwriteConfirmed] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const steps = [
    t('import.step1.title'),
    t('import.step2.title'),
    t('import.step3.title'),
    t('import.step4.title'),
  ];

  // Fetch existing profile and skills to show data counts
  const { data: profile } = useCurrentProfile();
  const profileId = profile?.id;
  const { data: skillsGrouped } = useUserSkills(profileId);

  const existingDataCounts = useMemo(() => {
    const jobs = profile?.jobs?.length ?? 0;
    const education = profile?.education?.length ?? 0;
    let skills = 0;
    if (skillsGrouped) {
      for (const group of Object.values(skillsGrouped)) {
        skills += group.length;
      }
    }
    return { jobs, education, skills };
  }, [profile, skillsGrouped]);

  const hasExistingData =
    existingDataCounts.jobs > 0 ||
    existingDataCounts.education > 0 ||
    existingDataCounts.skills > 0;

  const { data: history } = useQuery({
    queryKey: ['linkedin-import-history'],
    queryFn: linkedinImportApi.getHistory,
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => linkedinImportApi.uploadZip(file),
    onSuccess: (data) => {
      setPreview(data);
      setActiveStep(2);
    },
  });

  const confirmMutation = useMutation({
    mutationFn: () =>
      linkedinImportApi.confirmImport(preview!.previewId, strategy),
    onSuccess: (data) => {
      setImportResult(data);
      setActiveStep(3);
      queryClient.invalidateQueries({ queryKey: ['linkedin-import-history'] });
    },
  });

  const validateFile = (file: File): boolean => {
    if (!file.name.toLowerCase().endsWith('.zip')) {
      setValidationError(t('import.step2.onlyZip'));
      return false;
    }
    if (file.size > MAX_SIZE_BYTES) {
      setValidationError(t('import.step2.fileTooLarge', { size: MAX_SIZE_MB }));
      return false;
    }
    setValidationError(null);
    return true;
  };

  const handleFileSelect = (file: File) => {
    if (validateFile(file)) {
      setSelectedFile(file);
    }
  };

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) handleFileSelect(file);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleUpload = () => {
    if (selectedFile) {
      uploadMutation.mutate(selectedFile);
    }
  };

  const handleConfirmImport = () => {
    confirmMutation.mutate();
  };

  const handleReset = () => {
    setActiveStep(0);
    setSelectedFile(null);
    setValidationError(null);
    setPreview(null);
    setImportResult(null);
    setStrategy('MERGE');
    setOverwriteConfirmed(false);
    uploadMutation.reset();
    confirmMutation.reset();
  };

  const renderStep0 = () => (
    <Paper elevation={2} sx={{ p: 3, mt: 3 }}>
      <Typography variant="h6" gutterBottom>
        {t('import.step1.title')}
      </Typography>
      <Alert severity="info" sx={{ mb: 2 }}>
        {t('import.step1.description')}
      </Alert>
      <Box component="ol" sx={{ pl: 2 }}>
        <li>
          <Typography variant="body1" sx={{ mb: 1 }}>
            {t('import.step1.instruction1')}{' '}
            <a
              href="https://www.linkedin.com/mypreferences/d/download-my-data"
              target="_blank"
              rel="noopener noreferrer"
            >
              linkedin.com/mypreferences/d/download-my-data
            </a>
          </Typography>
        </li>
        <li>
          <Typography variant="body1" sx={{ mb: 1 }}>
            {t('import.step1.instruction2')}
          </Typography>
        </li>
        <li>
          <Typography variant="body1" sx={{ mb: 1 }}>
            {t('import.step1.instruction3')}
          </Typography>
        </li>
        <li>
          <Typography variant="body1" sx={{ mb: 1 }}>
            {t('import.step1.instruction4')}
          </Typography>
        </li>
        <li>
          <Typography variant="body1" sx={{ mb: 1 }}>
            {t('import.step1.instruction5')}
          </Typography>
        </li>
      </Box>
      <Alert severity="warning" sx={{ mt: 2 }}>
        {t('import.step1.note')}
      </Alert>
      <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button variant="contained" onClick={() => setActiveStep(1)}>
          {t('common:buttons.next')}
        </Button>
      </Box>
    </Paper>
  );

  const renderStep1 = () => (
    <Box sx={{ mt: 3 }}>
      <Paper
        elevation={2}
        sx={{
          p: 3,
          border: isDragging ? '2px dashed #1976d2' : '2px dashed #ccc',
          backgroundColor: isDragging ? 'action.hover' : 'background.paper',
          textAlign: 'center',
          cursor: 'pointer',
          transition: 'all 0.2s',
        }}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={() =>
          !uploadMutation.isPending && fileInputRef.current?.click()
        }
      >
        <input
          ref={fileInputRef}
          type="file"
          accept=".zip"
          onChange={handleInputChange}
          style={{ display: 'none' }}
        />

        {!selectedFile ? (
          <Box sx={{ py: 3 }}>
            <CloudUploadIcon
              sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }}
            />
            <Typography variant="h6" color="text.secondary">
              {t('import.step2.dropzone')}
            </Typography>
            <Typography
              variant="body2"
              color="text.secondary"
              sx={{ mt: 1 }}
            >
              {t('import.step2.maxSize')}
            </Typography>
          </Box>
        ) : (
          <Box sx={{ py: 2 }} onClick={(e) => e.stopPropagation()}>
            <InsertDriveFileIcon
              sx={{ fontSize: 36, color: 'primary.main', mb: 1 }}
            />
            <Typography variant="body1">{selectedFile.name}</Typography>
            <Typography variant="body2" color="text.secondary">
              {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
            </Typography>
            <Box
              sx={{
                mt: 2,
                display: 'flex',
                gap: 2,
                justifyContent: 'center',
              }}
            >
              <Button
                variant="contained"
                onClick={handleUpload}
                disabled={uploadMutation.isPending}
                startIcon={
                  uploadMutation.isPending ? (
                    <CircularProgress size={20} />
                  ) : undefined
                }
              >
                {uploadMutation.isPending
                  ? t('import.step2.uploading')
                  : t('import.step2.upload')}
              </Button>
              <Button
                variant="outlined"
                onClick={() => {
                  setSelectedFile(null);
                  setValidationError(null);
                }}
                disabled={uploadMutation.isPending}
              >
                {t('common:buttons.clear')}
              </Button>
            </Box>
          </Box>
        )}

        {(validationError || uploadMutation.error) && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {validationError ||
              (uploadMutation.error as Error).message ||
              t('import.step2.uploadFailed')}
          </Alert>
        )}
      </Paper>
      <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-start' }}>
        <Button variant="text" onClick={() => setActiveStep(0)}>
          {t('common:buttons.back')}
        </Button>
      </Box>
    </Box>
  );

  const renderStep2 = () => {
    if (!preview) return null;
    const { profilePreview, counts, warnings } = preview;

    return (
      <Box sx={{ mt: 3 }}>
        {profilePreview && (
          <Card sx={{ mb: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                {t('import.step3.profileInfo')}
              </Typography>
              <Typography variant="body1">
                {profilePreview.firstName} {profilePreview.lastName}
              </Typography>
              {profilePreview.headline && (
                <Typography variant="body2" color="text.secondary">
                  {profilePreview.headline}
                </Typography>
              )}
              {profilePreview.location && (
                <Typography variant="body2" color="text.secondary">
                  {profilePreview.location}
                </Typography>
              )}
              {profilePreview.summary && (
                <Typography variant="body2" sx={{ mt: 1 }}>
                  {profilePreview.summary}
                </Typography>
              )}
            </CardContent>
          </Card>
        )}

        {/* Existing vs New data comparison */}
        {hasExistingData && (
          <Paper elevation={1} sx={{ p: 2, mb: 2, bgcolor: 'grey.50' }}>
            <Typography variant="subtitle2" gutterBottom>
              {t('import.currentProfile')}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1.5 }}>
              <Chip
                label={`${t('import.step3.jobs')}: ${existingDataCounts.jobs}`}
                size="small"
                variant="outlined"
              />
              <Chip
                label={`${t('import.step3.education')}: ${existingDataCounts.education}`}
                size="small"
                variant="outlined"
              />
              <Chip
                label={`${t('import.step3.skills')}: ${existingDataCounts.skills}`}
                size="small"
                variant="outlined"
              />
            </Box>
            <Typography variant="subtitle2" gutterBottom>
              {t('import.title')}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              {Object.entries(counts).map(([key, value]) => (
                <Chip
                  key={key}
                  label={`${t(`import.step3.${key.toLowerCase()}`, { defaultValue: key })}: ${value}`}
                  size="small"
                  color="primary"
                  variant="outlined"
                />
              ))}
            </Box>
          </Paper>
        )}

        {/* New data counts (when no existing data) */}
        {!hasExistingData && (
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
              gap: 2,
              mb: 2,
            }}
          >
            {Object.entries(counts).map(([key, value]) => (
              <Card key={key}>
                <CardContent
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                  }}
                >
                  <Typography variant="body1">
                    {t(`import.step3.${key.toLowerCase()}`, { defaultValue: key })}
                  </Typography>
                  <Chip label={value} color="primary" />
                </CardContent>
              </Card>
            ))}
          </Box>
        )}

        {warnings.length > 0 && (
          <Alert severity="warning" icon={<WarningAmberIcon />} sx={{ mb: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              {t('import.step3.warnings')}
            </Typography>
            {warnings.map((w, i) => (
              <Typography key={i} variant="body2">
                {w}
              </Typography>
            ))}
          </Alert>
        )}

        <Paper elevation={2} sx={{ p: 3, mb: 2 }}>
          <Typography variant="h6" gutterBottom>
            {t('import.step3.strategy')}
          </Typography>
          <RadioGroup
            value={strategy}
            onChange={(e) => {
              setStrategy(e.target.value);
              setOverwriteConfirmed(false);
            }}
          >
            <FormControlLabel
              value="MERGE"
              control={<Radio />}
              label={
                <Box>
                  <Typography variant="body1">
                    {t('import.step3.merge')}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('import.step3.mergeDesc')}
                  </Typography>
                </Box>
              }
            />
            <FormControlLabel
              value="OVERWRITE"
              control={<Radio />}
              label={
                <Box>
                  <Typography variant="body1">
                    {t('import.step3.overwrite')}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {t('import.step3.overwriteDesc')}
                  </Typography>
                </Box>
              }
            />
          </RadioGroup>

          {strategy === 'MERGE' && (
            <Alert severity="success" sx={{ mt: 1 }}>
              {t('import.step3.mergeInfo')}
            </Alert>
          )}

          {strategy === 'OVERWRITE' && (
            <Box sx={{ mt: 1 }}>
              <Alert severity="error">
                {t('import.step3.overwriteWarningDetailed', {
                  jobs: existingDataCounts.jobs,
                  education: existingDataCounts.education,
                  skills: existingDataCounts.skills,
                })}
              </Alert>
              <FormControlLabel
                sx={{ mt: 1 }}
                control={
                  <Checkbox
                    checked={overwriteConfirmed}
                    onChange={(e) => setOverwriteConfirmed(e.target.checked)}
                    color="error"
                  />
                }
                label={
                  <Typography variant="body2" fontWeight="bold">
                    {t('import.step3.overwriteConfirm')}
                  </Typography>
                }
              />
            </Box>
          )}
        </Paper>

        {confirmMutation.error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {(confirmMutation.error as Error).message ||
              t('import.step4.error')}
          </Alert>
        )}

        <Box
          sx={{
            display: 'flex',
            justifyContent: 'space-between',
          }}
        >
          <Button variant="text" onClick={() => setActiveStep(1)}>
            {t('common:buttons.back')}
          </Button>
          <Button
            variant="contained"
            onClick={handleConfirmImport}
            disabled={
              confirmMutation.isPending ||
              (strategy === 'OVERWRITE' && !overwriteConfirmed)
            }
            startIcon={
              confirmMutation.isPending ? (
                <CircularProgress size={20} />
              ) : undefined
            }
          >
            {confirmMutation.isPending
              ? t('common:status.loading')
              : t('common:buttons.confirm')}
          </Button>
        </Box>
      </Box>
    );
  };

  const renderStep3 = () => {
    if (!importResult) return null;

    const isSuccess = importResult.status === 'COMPLETED';

    return (
      <Box sx={{ mt: 3 }}>
        <Paper
          elevation={2}
          sx={{ p: 4, textAlign: 'center' }}
        >
          {isSuccess ? (
            <>
              <CheckCircleIcon
                sx={{ fontSize: 64, color: 'success.main', mb: 2 }}
              />
              <Typography variant="h5" gutterBottom>
                {t('import.step4.success')}
              </Typography>
              <Typography variant="body1" color="text.secondary" gutterBottom>
                {t('import.step4.imported')}
              </Typography>
              <Box
                sx={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: 1,
                  justifyContent: 'center',
                  mt: 2,
                  mb: 3,
                }}
              >
                {Object.entries(importResult.importedCounts).map(
                  ([key, value]) => (
                    <Chip
                      key={key}
                      label={`${key}: ${value}`}
                      color="success"
                      variant="outlined"
                    />
                  )
                )}
              </Box>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'center' }}>
                <Button
                  variant="contained"
                  onClick={() => navigate('/profile')}
                >
                  {t('import.step4.viewProfile')}
                </Button>
                <Button variant="outlined" onClick={handleReset}>
                  {t('import.step4.importAnother')}
                </Button>
              </Box>
            </>
          ) : (
            <>
              <ErrorIcon
                sx={{ fontSize: 64, color: 'error.main', mb: 2 }}
              />
              <Typography variant="h5" gutterBottom>
                {t('import.step4.error')}
              </Typography>
              {importResult.errors.map((err, i) => (
                <Typography
                  key={i}
                  variant="body2"
                  color="error"
                  sx={{ mb: 1 }}
                >
                  {err}
                </Typography>
              ))}
              <Button
                variant="contained"
                onClick={handleReset}
                sx={{ mt: 2 }}
              >
                {t('import.step4.retry')}
              </Button>
            </>
          )}
        </Paper>
      </Box>
    );
  };

  const renderHistory = () => {
    if (!history || history.length === 0) return null;

    return (
      <Box sx={{ mt: 4 }}>
        <Typography variant="h6" gutterBottom>
          {t('import.history.title')}
        </Typography>
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>{t('import.history.date')}</TableCell>
                <TableCell>{t('import.history.filename')}</TableCell>
                <TableCell>{t('import.history.strategy')}</TableCell>
                <TableCell>{t('import.history.status')}</TableCell>
                <TableCell>{t('import.history.counts')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {history.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>
                    {new Date(item.importedAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>{item.filename}</TableCell>
                  <TableCell>
                    <Chip
                      label={item.strategy}
                      size="small"
                      variant="outlined"
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={item.status}
                      size="small"
                      color={item.status === 'COMPLETED' ? 'success' : 'error'}
                    />
                  </TableCell>
                  <TableCell>
                    <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                      {Object.entries(item.itemCounts).map(([k, v]) => (
                        <Chip
                          key={k}
                          label={`${k}: ${v}`}
                          size="small"
                          variant="outlined"
                        />
                      ))}
                    </Box>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Box>
    );
  };

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          {t('import.title')}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          {t('import.subtitle')}
        </Typography>

        {hasExistingData && activeStep < 2 && (
          <Alert severity="info" sx={{ mb: 3 }}>
            {t('import.existingDataWarning', {
              jobs: existingDataCounts.jobs,
              education: existingDataCounts.education,
              skills: existingDataCounts.skills,
            })}
          </Alert>
        )}

        <Stepper activeStep={activeStep} alternativeLabel>
          {steps.map((label) => (
            <Step key={label}>
              <StepLabel>{label}</StepLabel>
            </Step>
          ))}
        </Stepper>

        {activeStep === 0 && renderStep0()}
        {activeStep === 1 && renderStep1()}
        {activeStep === 2 && renderStep2()}
        {activeStep === 3 && renderStep3()}

        {renderHistory()}
      </Box>
    </Container>
  );
}
