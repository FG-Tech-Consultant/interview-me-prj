import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Skeleton,
  Box,
} from '@mui/material';
import DescriptionIcon from '@mui/icons-material/Description';
import AssessmentIcon from '@mui/icons-material/Assessment';
import { useExportHistory } from '../../hooks/useExports';
import { useAnalysisHistory } from '../../hooks/useLinkedInAnalysis';
import { useCurrentProfile } from '../../hooks/useProfile';
import { ExportStatusBadge } from '../exports/ExportStatusBadge';
import type { ExportHistory } from '../../types/export';
import type { LinkedInAnalysisSummary } from '../../types/linkedinAnalysis';

interface ActivityItem {
  type: 'export' | 'linkedin';
  label: string;
  date: string;
  extra: React.ReactNode;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
  });
}

function buildActivities(
  exports: ExportHistory[] | undefined,
  analyses: LinkedInAnalysisSummary[] | undefined
): ActivityItem[] {
  const items: ActivityItem[] = [];

  if (exports) {
    for (const e of exports.slice(0, 3)) {
      items.push({
        type: 'export',
        label: e.template?.name ?? 'Resume Export',
        date: e.createdAt,
        extra: <ExportStatusBadge status={e.status} />,
      });
    }
  }

  if (analyses) {
    for (const a of analyses.slice(0, 3)) {
      items.push({
        type: 'linkedin',
        label: a.pdfFilename ?? 'LinkedIn Analysis',
        date: a.createdAt,
        extra: a.overallScore != null ? (
          <Typography variant="body2" fontWeight="bold" color="primary">
            {a.overallScore}/100
          </Typography>
        ) : null,
      });
    }
  }

  items.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  return items.slice(0, 5);
}

export default function RecentActivityCard() {
  const { data: profile } = useCurrentProfile();
  const { data: exportsData, isLoading: exportsLoading } = useExportHistory(0, 3);
  const { data: analysisData, isLoading: analysisLoading } = useAnalysisHistory(
    profile?.id ?? null,
    0
  );

  const isLoading = exportsLoading || analysisLoading;
  const activities = buildActivities(
    exportsData?.content,
    analysisData?.content
  );

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
          Recent Activity
        </Typography>

        {isLoading ? (
          <Box>
            {Array.from({ length: 3 }).map((_, i) => (
              <Skeleton key={i} height={48} />
            ))}
          </Box>
        ) : activities.length === 0 ? (
          <Typography variant="body2" color="text.secondary">
            No recent activity
          </Typography>
        ) : (
          <List dense disablePadding>
            {activities.map((item, idx) => (
              <ListItem key={idx} disableGutters secondaryAction={item.extra}>
                <ListItemIcon sx={{ minWidth: 36 }}>
                  {item.type === 'export' ? (
                    <DescriptionIcon fontSize="small" color="action" />
                  ) : (
                    <AssessmentIcon fontSize="small" color="action" />
                  )}
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  secondary={formatDate(item.date)}
                  primaryTypographyProps={{ noWrap: true, sx: { maxWidth: '60%' } }}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
}
