import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Alert,
} from '@mui/material';

interface CoinConfirmationDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  featureName: string;
  cost: number;
  currentBalance: number;
}

export const CoinConfirmationDialog = ({
  open,
  onClose,
  onConfirm,
  featureName,
  cost,
  currentBalance,
}: CoinConfirmationDialogProps) => {
  const { t } = useTranslation('billing');
  const balanceAfter = currentBalance - cost;
  const insufficientBalance = currentBalance < cost;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('coinConfirm.title')}</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
          <Typography>
            <strong>{t('coinConfirm.feature')}:</strong> {featureName}
          </Typography>
          <Typography>
            <strong>{t('coinConfirm.cost')}:</strong> {t('coinsAmount', { amount: cost })}
          </Typography>
          <Typography>
            <strong>{t('coinConfirm.currentBalanceLabel')}:</strong> {t('coinsAmount', { amount: currentBalance })}
          </Typography>
          <Typography>
            <strong>{t('coinConfirm.balanceAfter')}:</strong> {t('coinsAmount', { amount: balanceAfter })}
          </Typography>
          {insufficientBalance && (
            <Alert severity="error">
              {t('coinConfirm.insufficientBalance', { amount: cost - currentBalance })}
            </Alert>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common:buttons.cancel')}</Button>
        <Button
          onClick={onConfirm}
          variant="contained"
          disabled={insufficientBalance}
        >
          {t('common:buttons.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
