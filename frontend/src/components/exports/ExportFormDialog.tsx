import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Typography,
  Box,
  Alert,
} from '@mui/material';
import type { ExportTemplate, ExportResumeRequest } from '../../types/export';
import { SENIORITY_LEVELS, LANGUAGES } from '../../types/export';
import { useWallet, useFeatureCosts } from '../../hooks/useBilling';

interface ExportFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (request: ExportResumeRequest) => void;
  templates: ExportTemplate[];
  isSubmitting: boolean;
}

export const ExportFormDialog = ({
  open,
  onClose,
  onSubmit,
  templates,
  isSubmitting,
}: ExportFormDialogProps) => {
  const [templateId, setTemplateId] = useState<number>(templates[0]?.id || 0);
  const [targetRole, setTargetRole] = useState('');
  const [location, setLocation] = useState('');
  const [seniority, setSeniority] = useState('Senior');
  const [language, setLanguage] = useState('en');
  const [showConfirm, setShowConfirm] = useState(false);

  const { data: wallet } = useWallet();
  const { data: costs } = useFeatureCosts();

  const exportCost = costs?.costs?.RESUME_EXPORT ?? 10;
  const balance = wallet?.balance ?? 0;
  const hasEnoughCoins = balance >= exportCost;

  const handleSubmit = () => {
    if (!showConfirm) {
      setShowConfirm(true);
      return;
    }

    onSubmit({
      templateId,
      targetRole: targetRole.trim(),
      location: location.trim() || undefined,
      seniority,
      language,
    });

    resetForm();
  };

  const resetForm = () => {
    setTargetRole('');
    setLocation('');
    setSeniority('Senior');
    setLanguage('en');
    setShowConfirm(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const isValid = targetRole.trim().length > 0 && seniority.length > 0;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>New Resume Export</DialogTitle>
      <DialogContent>
        {!showConfirm ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Template</InputLabel>
              <Select
                value={templateId}
                label="Template"
                onChange={(e) => setTemplateId(e.target.value as number)}
              >
                {templates.map((t) => (
                  <MenuItem key={t.id} value={t.id}>
                    {t.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label="Target Role"
              value={targetRole}
              onChange={(e) => setTargetRole(e.target.value)}
              required
              fullWidth
              inputProps={{ maxLength: 200 }}
              placeholder="e.g., Senior Backend Engineer"
            />

            <TextField
              label="Location"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              fullWidth
              inputProps={{ maxLength: 200 }}
              placeholder="e.g., Berlin, Germany"
            />

            <FormControl fullWidth>
              <InputLabel>Seniority Level</InputLabel>
              <Select
                value={seniority}
                label="Seniority Level"
                onChange={(e) => setSeniority(e.target.value)}
              >
                {SENIORITY_LEVELS.map((level) => (
                  <MenuItem key={level} value={level}>
                    {level}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <FormControl fullWidth>
              <InputLabel>Language</InputLabel>
              <Select
                value={language}
                label="Language"
                onChange={(e) => setLanguage(e.target.value)}
              >
                {LANGUAGES.map((lang) => (
                  <MenuItem key={lang.value} value={lang.value}>
                    {lang.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Box>
        ) : (
          <Box sx={{ mt: 1 }}>
            <Typography variant="body1" gutterBottom>
              This will cost <strong>{exportCost} coins</strong>.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Current balance: {balance} coins
            </Typography>
            {!hasEnoughCoins && (
              <Alert severity="error" sx={{ mt: 2 }}>
                Insufficient coins. You need {exportCost - balance} more coins.
              </Alert>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>Cancel</Button>
        {showConfirm ? (
          <>
            <Button onClick={() => setShowConfirm(false)}>Back</Button>
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={!hasEnoughCoins || isSubmitting}
            >
              {isSubmitting ? 'Generating...' : 'Confirm & Generate'}
            </Button>
          </>
        ) : (
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={!isValid}
          >
            Next
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};
