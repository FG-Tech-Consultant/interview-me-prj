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
      label={t('creditsAmount', { amount: wallet?.balance ?? 0 })}
      size="small"
      onClick={() => navigate('/billing')}
      sx={{
        cursor: 'pointer',
        backgroundColor: 'rgba(255,255,255,0.15)',
        color: 'inherit',
        border: '1px solid rgba(255,255,255,0.3)',
        '&:hover': { backgroundColor: 'rgba(255,255,255,0.25)' },
      }}
    />
  );
};
