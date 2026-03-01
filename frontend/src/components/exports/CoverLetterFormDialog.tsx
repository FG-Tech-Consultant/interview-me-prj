import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  const coverLetterTemplates = templates.filter((tmpl) => tmpl.type === 'COVER_LETTER');
  const [templateId, setTemplateId] = useState<number>(coverLetterTemplates[0]?.id || 0);
  const [targetCompany, setTargetCompany] = useState('');
  const [targetRole, setTargetRole] = useState('');
  const [jobDescription, setJobDescription] = useState('');
  const [market, setMarket] = useState('US');
  const [showConfirm, setShowConfirm] = useState(false);
  const { t } = useTranslation('exports');

  const { data: wallet } = useWallet();
  const { data: costs } = useFeatureCosts();

  const exportCost = costs?.costs?.COVER_LETTER_EXPORT ?? 10;
  const balance = wallet?.balance ?? 0;
  const hasEnoughCredits = balance >= exportCost;

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
      <DialogTitle>{t('coverLetter.title')}</DialogTitle>
      <DialogContent>
        {!showConfirm ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            {coverLetterTemplates.length > 1 && (
              <FormControl fullWidth>
                <InputLabel>{t('coverLetter.template')}</InputLabel>
                <Select
                  value={templateId}
                  label={t('coverLetter.template')}
                  onChange={(e) => setTemplateId(e.target.value as number)}
                >
                  {coverLetterTemplates.map((tmpl) => (
                    <MenuItem key={tmpl.id} value={tmpl.id}>
                      {tmpl.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}

            <TextField
              label={t('coverLetter.targetCompany')}
              value={targetCompany}
              onChange={(e) => setTargetCompany(e.target.value)}
              required
              fullWidth
              inputProps={{ maxLength: 200 }}
              placeholder={t('coverLetter.targetCompanyPlaceholder')}
            />

            <TextField
              label={t('coverLetter.targetRole')}
              value={targetRole}
              onChange={(e) => setTargetRole(e.target.value)}
              required
              fullWidth
              inputProps={{ maxLength: 200 }}
              placeholder={t('coverLetter.targetRolePlaceholder')}
            />

            <TextField
              label={t('coverLetter.jobDescription')}
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              fullWidth
              multiline
              rows={4}
              inputProps={{ maxLength: 5000 }}
              placeholder={t('coverLetter.jobDescriptionPlaceholder')}
            />

            <FormControl fullWidth>
              <InputLabel>{t('coverLetter.market')}</InputLabel>
              <Select
                value={market}
                label={t('coverLetter.market')}
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
            <Typography variant="body1" gutterBottom fontWeight="bold">
              {t('form.costMessage', { cost: exportCost })}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('form.currentBalance', { balance })}
            </Typography>
            {!hasEnoughCredits && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {t('form.insufficientCredits', { amount: exportCost - balance })}
              </Alert>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose}>{t('common:buttons.cancel')}</Button>
        {showConfirm ? (
          <>
            <Button onClick={() => setShowConfirm(false)}>{t('common:buttons.back')}</Button>
            <Button
              variant="contained"
              onClick={handleSubmit}
              disabled={!hasEnoughCredits || isSubmitting}
            >
              {isSubmitting ? t('form.generating') : t('form.confirmGenerate')}
            </Button>
          </>
        ) : (
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={!isValid}
          >
            {t('common:buttons.next')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};
