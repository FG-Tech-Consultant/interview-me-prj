import { Chip } from '@mui/material';
import type { ExportStatusType } from '../../types/export';

const statusConfig: Record<ExportStatusType, { color: 'warning' | 'info' | 'success' | 'error'; label: string }> = {
  PENDING: { color: 'warning', label: 'Pending' },
  IN_PROGRESS: { color: 'info', label: 'In Progress' },
  COMPLETED: { color: 'success', label: 'Completed' },
  FAILED: { color: 'error', label: 'Failed' },
};

interface ExportStatusBadgeProps {
  status: ExportStatusType;
}

export const ExportStatusBadge = ({ status }: ExportStatusBadgeProps) => {
  const config = statusConfig[status] || { color: 'default' as const, label: status };
  return <Chip label={config.label} color={config.color} size="small" />;
};
