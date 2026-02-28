import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation } from '@tanstack/react-query';
import {
  Container,
  Typography,
  Box,
  Paper,
  Tabs,
  Tab,
  Button,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Divider,
  List,
  ListItem,
  ListItemText,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import { getCurrentUser, logout } from '../api/auth';
import { deleteAccount } from '../api/accountApi';

export const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [confirmText, setConfirmText] = useState('');
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const { t } = useTranslation('settings');
  const { t: tCommon } = useTranslation('common');
  const { i18n } = useTranslation();

  const { data: user, isLoading } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
  });

  const deleteAccountMutation = useMutation({
    mutationFn: () => deleteAccount('DELETE MY ACCOUNT'),
    onSuccess: () => {
      logout();
    },
    onError: (error: Error) => {
      setDeleteError(error.message);
    },
  });

  const handleDeleteConfirm = () => {
    if (confirmText !== 'DELETE MY ACCOUNT') {
      setDeleteError(t('dangerZone.mismatch'));
      return;
    }
    setDeleteError(null);
    deleteAccountMutation.mutate();
  };

  const handleCloseDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setConfirmText('');
    setDeleteError(null);
  };

  const handleLanguageChange = (lang: string) => {
    i18n.changeLanguage(lang);
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Container maxWidth="md" sx={{ mt: 4, mb: 4 }}>
      <Typography variant="h4" gutterBottom>
        {t('title')}
      </Typography>

      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)}>
          <Tab label={t('tabs.account')} />
          <Tab label={t('tabs.preferences')} />
        </Tabs>
      </Paper>

      {activeTab === 0 && (
        <>
          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="h6" gutterBottom>
              {t('account.title')}
            </Typography>
            <List disablePadding>
              <ListItem disableGutters>
                <ListItemText
                  primary={t('account.email')}
                  secondary={user?.email}
                />
              </ListItem>
              <Divider component="li" />
              <ListItem disableGutters>
                <ListItemText
                  primary={t('account.tenantId')}
                  secondary={user?.tenantId}
                />
              </ListItem>
              <Divider component="li" />
              <ListItem disableGutters>
                <ListItemText
                  primary={t('account.memberSince')}
                  secondary={user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : tCommon('na')}
                />
              </ListItem>
            </List>
          </Paper>

          <Paper sx={{ p: 3, mb: 3 }}>
            <Typography variant="subtitle1" gutterBottom>
              {t('account.logout')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {t('account.logoutDescription')}
            </Typography>
            <Button
              variant="outlined"
              startIcon={<LogoutIcon />}
              onClick={logout}
            >
              {t('account.logout')}
            </Button>
          </Paper>

          <Paper sx={{ p: 3, border: '1px solid', borderColor: 'error.main' }}>
            <Typography variant="h6" color="error" gutterBottom>
              {t('dangerZone.title')}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              {t('dangerZone.description')}
            </Typography>
            <Button
              variant="outlined"
              color="error"
              startIcon={<DeleteForeverIcon />}
              onClick={() => setDeleteDialogOpen(true)}
            >
              {t('dangerZone.deleteAccount')}
            </Button>
          </Paper>
        </>
      )}

      {activeTab === 1 && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            {t('preferences.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {t('preferences.languageDescription')}
          </Typography>
          <FormControl sx={{ minWidth: 200 }}>
            <InputLabel>{t('preferences.language')}</InputLabel>
            <Select
              value={i18n.language}
              label={t('preferences.language')}
              onChange={(e) => handleLanguageChange(e.target.value)}
            >
              <MenuItem value="en">{tCommon('language.en')}</MenuItem>
              <MenuItem value="pt-BR">{tCommon('language.ptBR')}</MenuItem>
            </Select>
          </FormControl>
        </Paper>
      )}

      <Dialog open={deleteDialogOpen} onClose={handleCloseDeleteDialog} maxWidth="sm" fullWidth>
        <DialogTitle color="error">
          {t('dangerZone.confirmTitle')}
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" sx={{ mb: 3 }}>
            {t('dangerZone.confirmDescription')}
          </Typography>
          <Typography variant="body2" fontWeight="bold" sx={{ mb: 1 }}>
            {t('dangerZone.confirmInstruction')}
          </Typography>
          <TextField
            fullWidth
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder={t('dangerZone.confirmPlaceholder')}
            autoComplete="off"
          />
          {deleteError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {deleteError}
            </Alert>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDeleteDialog} disabled={deleteAccountMutation.isPending}>
            {t('dangerZone.cancelButton')}
          </Button>
          <Button
            variant="contained"
            color="error"
            onClick={handleDeleteConfirm}
            disabled={deleteAccountMutation.isPending || confirmText !== 'DELETE MY ACCOUNT'}
            startIcon={deleteAccountMutation.isPending ? <CircularProgress size={16} /> : <DeleteForeverIcon />}
          >
            {deleteAccountMutation.isPending ? t('dangerZone.deleting') : t('dangerZone.confirmButton')}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};
