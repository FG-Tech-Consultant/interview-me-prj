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
import type { ExportTemplate, ExportCoverLetterRequest } from '../../types/export';
import { MARKETS } from '../../types/export';
import { useWallet, useFeatureCosts } from '../../hooks/useBilling';

interface CoverLetterFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (request: ExportCoverLetterRequest) => void;
  templates: ExportTemplate[];
  isSubmitting: boolean;
}

export const CoverLetterFormDialog = ({
  open,
  onClose,
  onSubmit,
  templates,
  isSubmitting,
}: CoverLetterFormDialogProps) => {
  const coverLetterTemplates = templates.filter((t) => t.type === 'COVER_LETTER');
  const [templateId, setTemplateId] = useState<number>(coverLetterTemplates[0]?.id || 0);
  const [targetCompany, setTargetCompany] = useState('');
  const [targetRole, setTargetRole] = useState('');
  const [jobDescription, setJobDescription] = useState('');
  const [market, setMarket] = useState('US');
  const [showConfirm, setShowConfirm] = useState(false);

  const { data: wallet } = useWallet();
  const { data: costs } = useFeatureCosts();

  const exportCost = costs?.costs?.COVER_LETTER_EXPORT ?? 10;
  const balance = wallet?.balance ?? 0;
  const hasEnoughCoins = balance >= exportCost;

  const handleSubmit = () => {
    if (!showConfirm) {
      setShowConfirm(true);
      return;
    }

    onSubmit({
      templateId,
      targetCompany: targetCompany.trim(),
      targetRole: targetRole.trim(),
      jobDescription: jobDescription.trim() || undefined,
      market,
    });

    resetForm();
  };

  const resetForm = () => {
    setTargetCompany('');
    setTargetRole('');
    setJobDescription('');
    setMarket('US');
    setShowConfirm(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  const isValid = targetCompany.trim().length > 0 && targetRole.trim().length > 0 && market.length > 0;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>New Cover Letter</DialogTitle>
      <DialogContent>
        {!showConfirm ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {coverLetterTemplates.length > 1 && (
              <FormControl fullWidth>
                <InputLabel>Template</InputLabel>
                <Select
                  value={templateId}
                  label="Template"
                  onChange={(e) => setTemplateId(e.target.value as number)}
                >
                  {coverLetterTemplates.map((t) => (
                    <MenuItem key={t.id} value={t.id}>
                      {t.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            <TextField
              label="Target Company"
              value={targetCompany}
              onChange={(e) => setTargetCompany(e.target.value)}
              required
              fullWidth
              inputProps={{ maxLength: 200 }}
              placeholder="e.g., Google, Stripe, Spotify"
            />

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
              label="Job Description (optional)"
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              fullWidth
              multiline
              rows={4}
              inputProps={{ maxLength: 5000 }}
              placeholder="Paste the job description here to tailor the cover letter..."
            />

            <FormControl fullWidth>
              <InputLabel>Market</InputLabel>
              <Select
                value={market}
                label="Market"
                onChange={(e) => setMarket(e.target.value)}
              >
                {MARKETS.map((m) => (
                  <MenuItem key={m.value} value={m.value}>
                    {m.label}
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
