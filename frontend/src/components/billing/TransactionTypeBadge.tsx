import { Chip } from '@mui/material';
import type { TransactionType } from '../../types/billing';

const TYPE_CONFIG: Record<TransactionType, { label: string; color: 'success' | 'error' | 'info' | 'secondary' }> = {
  EARN: { label: 'Earn', color: 'success' },
  SPEND: { label: 'Spend', color: 'error' },
  REFUND: { label: 'Refund', color: 'info' },
  PURCHASE: { label: 'Purchase', color: 'secondary' },
};

interface TransactionTypeBadgeProps {
  type: TransactionType;
}

export const TransactionTypeBadge = ({ type }: TransactionTypeBadgeProps) => {
  const config = TYPE_CONFIG[type] || { label: type, color: 'default' as const };
  return <Chip label={config.label} color={config.color} size="small" />;
};
