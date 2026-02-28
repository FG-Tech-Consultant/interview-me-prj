import { useTranslation } from 'react-i18next';
import { Chip } from '@mui/material';
import type { ExportStatusType } from '../../types/export';

const statusConfig: Record<ExportStatusType, { labelKey: string; color: 'warning' | 'info' | 'success' | 'error' }> = {
  PENDING: { labelKey: 'status.pending', color: 'warning' },
  IN_PROGRESS: { labelKey: 'status.inProgress', color: 'info' },
  COMPLETED: { labelKey: 'status.completed', color: 'success' },
  FAILED: { labelKey: 'status.failed', color: 'error' },
};

interface ExportStatusBadgeProps {
  status: ExportStatusType;
}

export const ExportStatusBadge = ({ status }: ExportStatusBadgeProps) => {
  const { t } = useTranslation('exports');
  const config = statusConfig[status] || { labelKey: status, color: 'default' as const };
  return <Chip label={t(config.labelKey)} color={config.color} size="small" />;
};
