import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Box, Grid, Typography, Skeleton } from '@mui/material';
import { getCurrentUser } from '../api/auth';
import { useCurrentProfile } from '../hooks/useProfile';
import StatsCards from '../components/dashboard/StatsCards';
import ProfileCompletenessCard from '../components/dashboard/ProfileCompletenessCard';
import QuickActionsGrid from '../components/dashboard/QuickActionsGrid';
import RecentActivityCard from '../components/dashboard/RecentActivityCard';
import PublicProfileCard from '../components/dashboard/PublicProfileCard';

export default function DashboardPage() {
  const { data: user, isLoading: userLoading } = useQuery({
    queryKey: ['currentUser'],
    queryFn: getCurrentUser,
  });
  const { data: profile, isLoading: profileLoading } = useCurrentProfile();
  const { t } = useTranslation('dashboard');

  const isLoading = userLoading || profileLoading;
  const displayName = profile?.fullName?.trim() || user?.email?.split('@')[0];

  return (
    <Box>
      {/* Welcome */}
      <Typography variant="h4" gutterBottom>
        {isLoading ? (
          <Skeleton width={300} />
        ) : (
          displayName
            ? t('welcomeBack', { name: displayName })
            : t('welcomeBackDefault')
        )}
      </Typography>

      {/* Stats row */}
      <Box sx={{ mb: 3 }}>
        <StatsCards />
      </Box>

      {/* Middle row: completeness + public profile */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={8}>
          <ProfileCompletenessCard />
        </Grid>
        <Grid item xs={12} md={4}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <PublicProfileCard />
            <RecentActivityCard />
          </Box>
        </Grid>
      </Grid>

      {/* Quick actions */}
      <Typography variant="h6" gutterBottom>
        {t('quickActions')}
      </Typography>
      <QuickActionsGrid />
    </Box>
  );
}
