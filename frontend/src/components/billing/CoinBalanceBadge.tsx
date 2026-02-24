import { Chip, Skeleton } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useWallet } from '../../hooks/useBilling';

export const CoinBalanceBadge = () => {
  const navigate = useNavigate();
  const { data: wallet, isLoading } = useWallet();

  if (isLoading) {
    return <Skeleton variant="rounded" width={80} height={32} />;
  }

  return (
    <Chip
      label={`${wallet?.balance ?? 0} coins`}
      color="primary"
      variant="outlined"
      size="small"
      onClick={() => navigate('/billing')}
      sx={{ cursor: 'pointer' }}
    />
  );
};
