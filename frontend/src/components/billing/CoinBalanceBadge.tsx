import { useTranslation } from 'react-i18next';
import { Chip, Skeleton } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useWallet } from '../../hooks/useBilling';

export const CoinBalanceBadge = () => {
  const navigate = useNavigate();
  const { data: wallet, isLoading } = useWallet();
  const { t } = useTranslation('billing');

  if (isLoading) {
    return <Skeleton variant="rounded" width={80} height={32} />;
  }

  return (
    <Chip
      label={t('coinsAmount', { amount: wallet?.balance ?? 0 })}
      color="primary"
      variant="outlined"
      size="small"
      onClick={() => navigate('/billing')}
      sx={{ cursor: 'pointer' }}
    />
  );
};
