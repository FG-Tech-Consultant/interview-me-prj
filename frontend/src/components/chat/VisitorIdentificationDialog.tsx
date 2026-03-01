import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Stack,
  Typography,
  Alert,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';

interface VisitorIdentificationDialogProps {
  open: boolean;
  profileName: string;
  onIdentify: (data: VisitorFormData) => Promise<void>;
  onClose: () => void;
  isLoading?: boolean;
  error?: string | null;
}

export interface VisitorFormData {
  name: string;
  company: string;
  jobRole: string;
  linkedinUrl?: string;
  contactEmail?: string;
  contactWhatsapp?: string;
}

export const VisitorIdentificationDialog: React.FC<VisitorIdentificationDialogProps> = ({
  open,
  profileName,
  onIdentify,
  onClose,
  isLoading,
  error,
}) => {
  const { t } = useTranslation('chat');
  const [form, setForm] = useState<VisitorFormData>({
    name: '',
    company: '',
    jobRole: '',
    linkedinUrl: '',
    contactEmail: '',
    contactWhatsapp: '',
  });

  const handleChange = (field: keyof VisitorFormData) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setForm((prev) => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onIdentify(form);
  };

  const isValid = form.name.trim() && form.company.trim() && form.jobRole.trim();

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <form onSubmit={handleSubmit}>
        <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <PersonIcon color="primary" />
          {t('visitor.title')}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            {t('visitor.subtitle', { name: profileName.split(' ')[0] })}
          </Typography>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <Stack spacing={2}>
            <TextField
              label={t('visitor.name')}
              value={form.name}
              onChange={handleChange('name')}
              required
              fullWidth
              size="small"
              autoFocus
            />
            <TextField
              label={t('visitor.company')}
              value={form.company}
              onChange={handleChange('company')}
              required
              fullWidth
              size="small"
            />
            <TextField
              label={t('visitor.jobRole')}
              value={form.jobRole}
              onChange={handleChange('jobRole')}
              required
              fullWidth
              size="small"
            />
            <TextField
              label={t('visitor.linkedinUrl')}
              value={form.linkedinUrl}
              onChange={handleChange('linkedinUrl')}
              fullWidth
              size="small"
              placeholder="https://linkedin.com/in/..."
            />
            <TextField
              label={t('visitor.contactEmail')}
              value={form.contactEmail}
              onChange={handleChange('contactEmail')}
              fullWidth
              size="small"
              type="email"
            />
            <TextField
              label={t('visitor.contactWhatsapp')}
              value={form.contactWhatsapp}
              onChange={handleChange('contactWhatsapp')}
              fullWidth
              size="small"
              placeholder="+55 11 99999-9999"
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={onClose} variant="outlined">
            {t('visitor.cancel')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={!isValid || isLoading}
          >
            {isLoading ? t('visitor.identifying') : t('visitor.startChat')}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};
