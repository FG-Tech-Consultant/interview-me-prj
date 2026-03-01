import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
  FormHelperText,
} from '@mui/material';
import LogoutIcon from '@mui/icons-material/Logout';
import DeleteForeverIcon from '@mui/icons-material/DeleteForever';
import { getCurrentUser, logout } from '../api/auth';
import { deleteAccount } from '../api/accountApi';
import { settingsApi, AiSettingsRequest } from '../api/settingsApi';

export const SettingsPage = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [confirmText, setConfirmText] = useState('');
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [aiProvider, setAiProvider] = useState<string>('');
  const [aiChatModel, setAiChatModel] = useState('');
  const [aiSaveSuccess, setAiSaveSuccess] = useState(false);
  const [aiSaveError, setAiSaveError] = useState<string | null>(null);
  const { t } = useTranslation('settings');
  const { t: tCommon } = useTranslation('common');
  const { i18n } = useTranslation();
  const queryClient = useQueryClient();

  const { data: user, isLoading } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
  });

  const { data: aiSettings, isLoading: isAiLoading } = useQuery({
    queryKey: ['aiSettings'],
    queryFn: settingsApi.getAiSettings,
  });

  useEffect(() => {
    if (aiSettings) {
      setAiProvider(aiSettings.provider ?? '');
      setAiChatModel(aiSettings.chatModel ?? '');
    }
  }, [aiSettings]);

  const aiSettingsMutation = useMutation({
    mutationFn: (data: AiSettingsRequest) => settingsApi.updateAiSettings(data),
    onSuccess: () => {
      setAiSaveSuccess(true);
      setAiSaveError(null);
      queryClient.invalidateQueries({ queryKey: ['aiSettings'] });
    },
    onError: () => {
      setAiSaveError(t('ai.saveError'));
      setAiSaveSuccess(false);
    },
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

  const handleAiSave = () => {
    setAiSaveSuccess(false);
    setAiSaveError(null);
    aiSettingsMutation.mutate({
      provider: aiProvider || null,
      chatModel: aiChatModel || null,
    });
  };

  const handleAiReset = () => {
    setAiProvider('');
    setAiChatModel('');
    setAiSaveSuccess(false);
    setAiSaveError(null);
    aiSettingsMutation.mutate({
      provider: null,
      chatModel: null,
    });
  };

  const selectedProviderDefault = aiSettings?.availableProviders.find(
    (p) => p.name === aiProvider,
  )?.defaultModel;

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
          <Tab label={t('tabs.ai')} />
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

      {activeTab === 2 && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            {t('ai.title')}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            {t('ai.description')}
          </Typography>

          {isAiLoading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress />
            </Box>
          ) : (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              <FormControl sx={{ minWidth: 200 }}>
                <InputLabel>{t('ai.provider')}</InputLabel>
                <Select
                  value={aiProvider}
                  label={t('ai.provider')}
                  onChange={(e) => {
                    setAiProvider(e.target.value);
                    setAiChatModel('');
                    setAiSaveSuccess(false);
                  }}
                >
                  <MenuItem value="">{t('ai.systemDefault')}</MenuItem>
                  {aiSettings?.availableProviders.map((p) => (
                    <MenuItem key={p.name} value={p.name}>
                      {p.name}
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>{t('ai.providerHelp')}</FormHelperText>
              </FormControl>

              <TextField
                label={t('ai.chatModel')}
                value={aiChatModel}
                onChange={(e) => {
                  setAiChatModel(e.target.value);
                  setAiSaveSuccess(false);
                }}
                placeholder={selectedProviderDefault ?? ''}
                helperText={
                  selectedProviderDefault
                    ? `${t('ai.defaultModel', { model: selectedProviderDefault })}. ${t('ai.chatModelHelp')}`
                    : t('ai.chatModelHelp')
                }
                sx={{ maxWidth: 400 }}
              />

              {aiSaveSuccess && (
                <Alert severity="success" onClose={() => setAiSaveSuccess(false)}>
                  {t('ai.saveSuccess')}
                </Alert>
              )}

              {aiSaveError && (
                <Alert severity="error" onClose={() => setAiSaveError(null)}>
                  {aiSaveError}
                </Alert>
              )}

              <Box sx={{ display: 'flex', gap: 2 }}>
                <Button
                  variant="contained"
                  onClick={handleAiSave}
                  disabled={aiSettingsMutation.isPending}
                  startIcon={aiSettingsMutation.isPending ? <CircularProgress size={16} /> : undefined}
                >
                  {t('ai.save')}
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleAiReset}
                  disabled={aiSettingsMutation.isPending}
                >
                  {t('ai.resetToDefault')}
                </Button>
              </Box>
            </Box>
          )}
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
