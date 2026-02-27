import React from 'react';
import {
  Card,
  CardContent,
  Grid,
  Skeleton,
  Typography,
} from '@mui/material';
import PsychologyIcon from '@mui/icons-material/Psychology';
import WorkIcon from '@mui/icons-material/Work';
import SchoolIcon from '@mui/icons-material/School';
import AccountBalanceWalletIcon from '@mui/icons-material/AccountBalanceWallet';
import DescriptionIcon from '@mui/icons-material/Description';
import { useCurrentProfile } from '../../hooks/useProfile';
import { useUserSkills } from '../../hooks/useSkills';
import { useJobExperiences } from '../../hooks/useJobExperience';
import { useEducations } from '../../hooks/useEducation';
import { useWallet } from '../../hooks/useBilling';
import { useExportHistory } from '../../hooks/useExports';
import type { UserSkillsGrouped } from '../../types/skill';

interface StatCardProps {
  icon: React.ReactNode;
  value: number | string;
  label: string;
  loading: boolean;
}

function StatCard({ icon, value, label, loading }: StatCardProps) {
  return (
    <Card elevation={2} sx={{ height: '100%' }}>
      <CardContent sx={{ textAlign: 'center', py: 3 }}>
        {icon}
        {loading ? (
          <Skeleton
            variant="text"
            width={60}
            sx={{ mx: 'auto', fontSize: '2.5rem' }}
          />
        ) : (
          <Typography variant="h3" sx={{ mt: 1 }}>
            {value}
          </Typography>
        )}
        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
          {label}
        </Typography>
      </CardContent>
    </Card>
  );
}

function countSkills(data: UserSkillsGrouped | undefined): number {
  if (!data) return 0;
  return Object.values(data).reduce((sum, arr) => sum + arr.length, 0);
}

export default function StatsCards() {
  const { data: profile } = useCurrentProfile();
  const profileId = profile?.id;

  const { data: skillsData, isLoading: skillsLoading } = useUserSkills(profileId);
  const { data: jobsData, isLoading: jobsLoading } = useJobExperiences(profileId ?? 0);
  const { data: eduData, isLoading: eduLoading } = useEducations(profileId ?? 0);
  const { data: walletData, isLoading: walletLoading } = useWallet();
  const { data: exportsData, isLoading: exportsLoading } = useExportHistory(0, 1);

  const stats = [
    {
      icon: <PsychologyIcon sx={{ fontSize: 40, color: '#9c27b0' }} />,
      value: countSkills(skillsData),
      label: 'Skills',
      loading: skillsLoading,
    },
    {
      icon: <WorkIcon sx={{ fontSize: 40, color: '#1976d2' }} />,
      value: jobsData?.length ?? 0,
      label: 'Job Experiences',
      loading: jobsLoading,
    },
    {
      icon: <SchoolIcon sx={{ fontSize: 40, color: '#2e7d32' }} />,
      value: eduData?.length ?? 0,
      label: 'Education',
      loading: eduLoading,
    },
    {
      icon: <AccountBalanceWalletIcon sx={{ fontSize: 40, color: '#f9a825' }} />,
      value: walletData?.balance ?? 0,
      label: 'Coin Balance',
      loading: walletLoading,
    },
    {
      icon: <DescriptionIcon sx={{ fontSize: 40, color: '#00897b' }} />,
      value: exportsData?.totalElements ?? 0,
      label: 'Exports',
      loading: exportsLoading,
    },
  ];

  return (
    <Grid container spacing={2}>
      {stats.map((stat) => (
        <Grid item xs={6} sm={4} md key={stat.label}>
          <StatCard
            icon={stat.icon}
            value={stat.value}
            label={stat.label}
            loading={stat.loading}
          />
        </Grid>
      ))}
    </Grid>
  );
}
