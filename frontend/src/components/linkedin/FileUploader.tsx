import { useState, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Button,
  Typography,
  Paper,
  Alert,
  CircularProgress,
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import FolderZipIcon from '@mui/icons-material/FolderZip';
import type { AnalysisSourceType } from '../../types/linkedinAnalysis';

interface FileUploaderProps {
  sourceType: AnalysisSourceType;
  onUpload: (file: File) => void;
  isLoading: boolean;
  error: string | null;
}

const MAX_SIZE_MB_PDF = 10;
const MAX_SIZE_MB_ZIP = 50;

export default function FileUploader({ sourceType, onUpload, isLoading, error }: FileUploaderProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { t } = useTranslation('linkedin');

  const maxSizeMB = sourceType === 'ZIP' ? MAX_SIZE_MB_ZIP : MAX_SIZE_MB_PDF;
  const maxSizeBytes = maxSizeMB * 1024 * 1024;
  const acceptedExt = sourceType === 'ZIP' ? '.zip' : '.pdf';
  const extLabel = sourceType === 'ZIP' ? 'ZIP' : 'PDF';

  const validateFile = (file: File): boolean => {
    const filename = file.name.toLowerCase();
    if (sourceType === 'ZIP' && !filename.endsWith('.zip')) {
      setValidationError(t('uploader.onlyZip'));
      return false;
    }
    if (sourceType === 'PDF' && !filename.endsWith('.pdf')) {
      setValidationError(t('uploader.onlyPdf'));
      return false;
    }
    if (file.size > maxSizeBytes) {
      setValidationError(t('uploader.fileTooLarge', { size: maxSizeMB }));
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
  }, [sourceType]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFileSelect(file);
  };

  const handleAnalyze = () => {
    if (selectedFile) {
      onUpload(selectedFile);
    }
  };

  const FileIcon = sourceType === 'ZIP' ? FolderZipIcon : InsertDriveFileIcon;

  return (
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
      onClick={() => !isLoading && fileInputRef.current?.click()}
    >
      <input
        ref={fileInputRef}
        type="file"
        accept={acceptedExt}
        onChange={handleInputChange}
        style={{ display: 'none' }}
      />

      {!selectedFile ? (
        <Box sx={{ py: 3 }}>
          <CloudUploadIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 1 }} />
          <Typography variant="h6" color="text.secondary">
            {t('uploader.dropHereFile', { type: extLabel })}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            {t('uploader.orBrowse', { size: maxSizeMB })}
          </Typography>
        </Box>
      ) : (
        <Box sx={{ py: 2 }} onClick={(e) => e.stopPropagation()}>
          <FileIcon sx={{ fontSize: 36, color: 'primary.main', mb: 1 }} />
          <Typography variant="body1">{selectedFile.name}</Typography>
          <Typography variant="body2" color="text.secondary">
            {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
          </Typography>
          <Box sx={{ mt: 2, display: 'flex', gap: 2, justifyContent: 'center' }}>
            <Button
              variant="contained"
              onClick={handleAnalyze}
              disabled={isLoading}
              startIcon={isLoading ? <CircularProgress size={20} /> : undefined}
            >
              {isLoading ? t('uploader.analyzingButton') : t('uploader.analyzeProfile')}
            </Button>
            <Button
              variant="outlined"
              onClick={() => {
                setSelectedFile(null);
                setValidationError(null);
              }}
              disabled={isLoading}
            >
              {t('common:buttons.clear')}
            </Button>
          </Box>
        </Box>
      )}

      {(validationError || error) && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {validationError || error}
        </Alert>
      )}
    </Paper>
  );
}
