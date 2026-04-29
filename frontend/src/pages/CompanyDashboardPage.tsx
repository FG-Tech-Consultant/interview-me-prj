import React from 'react';
import { Box, Typography, Grid, Paper, Button, CircularProgress, Alert } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import WorkIcon from '@mui/icons-material/Work';
import PeopleIcon from '@mui/icons-material/People';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { companyApi } from '../api/companyApi';

export const CompanyDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: dashboard, isLoading, error } = useQuery({
    queryKey: ['company-dashboard'],
    queryFn: companyApi.getDashboard,
  });

  if (isLoading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}><CircularProgress /></Box>;
  }

  if (error) {
    return <Alert severity="error">Failed to load dashboard</Alert>;
  }

  const metrics = [
    { label: 'Active Jobs', value: dashboard?.activeJobs ?? 0, icon: <WorkIcon fontSize="large" color="primary" /> },
    { label: 'Candidates', value: dashboard?.totalCandidates ?? 0, icon: <PeopleIcon fontSize="large" color="secondary" /> },
    { label: 'Profile Views', value: dashboard?.profileViews ?? 0, icon: <VisibilityIcon fontSize="large" color="action" /> },
  ];

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        {dashboard?.companyName ?? 'Company'} Dashboard
      </Typography>

      <Grid container spacing={3} sx={{ mb: 4 }}>
        {metrics.map((metric) => (
          <Grid item xs={12} sm={4} key={metric.label}>
            <Paper sx={{ p: 3, textAlign: 'center' }}>
              {metric.icon}
              <Typography variant="h3" sx={{ mt: 1 }}>{metric.value}</Typography>
              <Typography variant="body1" color="text.secondary">{metric.label}</Typography>
            </Paper>
          </Grid>
        ))}
      </Grid>

      <Grid container spacing={2}>
        <Grid item>
          <Button variant="contained" onClick={() => navigate('/company/jobs')}>
            Manage Jobs
          </Button>
        </Grid>
        <Grid item>
          <Button variant="contained" onClick={() => navigate('/company/profile')}>
            Edit Company Profile
          </Button>
        </Grid>
      </Grid>
    </Box>
  );
};
