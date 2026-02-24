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
  const balanceAfter = currentBalance - cost;
  const insufficientBalance = currentBalance < cost;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Confirm Coin Spend</DialogTitle>
      <DialogContent>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5, mt: 1 }}>
          <Typography>
            <strong>Feature:</strong> {featureName}
          </Typography>
          <Typography>
            <strong>Cost:</strong> {cost} coins
          </Typography>
          <Typography>
            <strong>Current Balance:</strong> {currentBalance} coins
          </Typography>
          <Typography>
            <strong>Balance After:</strong> {balanceAfter} coins
          </Typography>
          {insufficientBalance && (
            <Alert severity="error">
              Insufficient balance. You need {cost - currentBalance} more coins.
            </Alert>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Cancel</Button>
        <Button
          onClick={onConfirm}
          variant="contained"
          disabled={insufficientBalance}
        >
          Confirm
        </Button>
      </DialogActions>
    </Dialog>
  );
};
