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
  const { t } = useTranslation('exports');

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
      <DialogTitle>{t('form.title')}</DialogTitle>
      <DialogContent>
        {!showConfirm ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>{t('form.template')}</InputLabel>
              <Select
                value={templateId}
                label={t('form.template')}
                onChange={(e) => setTemplateId(e.target.value as number)}
              >
                {templates.map((tmpl) => (
                  <MenuItem key={tmpl.id} value={tmpl.id}>
                    {tmpl.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              label={t('form.targetRole')}
              value={targetRole}
              onChange={(e) => setTargetRole(e.target.value)}
              required
              fullWidth
              inputProps={{ maxLength: 200 }}
              placeholder={t('form.targetRolePlaceholder')}
            />

            <TextField
              label={t('form.location')}
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              fullWidth
              inputProps={{ maxLength: 200 }}
              placeholder={t('form.locationPlaceholder')}
            />

            <FormControl fullWidth>
              <InputLabel>{t('form.seniorityLevel')}</InputLabel>
              <Select
                value={seniority}
                label={t('form.seniorityLevel')}
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
              <InputLabel>{t('form.language')}</InputLabel>
              <Select
                value={language}
                label={t('form.language')}
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
            <Typography variant="body1" gutterBottom fontWeight="bold">
              {t('form.costMessage', { cost: exportCost })}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {t('form.currentBalance', { balance })}
            </Typography>
            {!hasEnoughCoins && (
              <Alert severity="error" sx={{ mt: 2 }}>
                {t('form.insufficientCoins', { amount: exportCost - balance })}
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
              disabled={!hasEnoughCoins || isSubmitting}
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
