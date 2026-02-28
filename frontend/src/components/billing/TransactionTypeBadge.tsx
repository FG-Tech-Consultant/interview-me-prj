import { useTranslation } from 'react-i18next';
import { Chip } from '@mui/material';
import type { TransactionType } from '../../types/billing';

const TYPE_CONFIG: Record<TransactionType, { labelKey: string; color: 'success' | 'error' | 'info' | 'secondary' }> = {
  EARN: { labelKey: 'filterEarn', color: 'success' },
  SPEND: { labelKey: 'filterSpend', color: 'error' },
  REFUND: { labelKey: 'filterRefund', color: 'info' },
  PURCHASE: { labelKey: 'filterPurchase', color: 'secondary' },
};

interface TransactionTypeBadgeProps {
  type: TransactionType;
}

export const TransactionTypeBadge = ({ type }: TransactionTypeBadgeProps) => {
  const { t } = useTranslation('billing');
  const config = TYPE_CONFIG[type] || { labelKey: type, color: 'default' as const };
  return <Chip label={t(config.labelKey)} color={config.color} size="small" />;
};
