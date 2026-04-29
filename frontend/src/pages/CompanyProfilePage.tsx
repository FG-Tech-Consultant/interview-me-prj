import React, { useState, useEffect } from 'react';
import {
  Box, Button, TextField, Typography, Grid, Paper,
  MenuItem, Select, FormControl, InputLabel, Alert, CircularProgress
} from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { companyApi } from '../api/companyApi';
import type { CompanyUpdateRequest } from '../types/company';

const COMPANY_SIZES = [
  { value: 'STARTUP', label: 'Startup (1-50)' },
  { value: 'SMB', label: 'SMB (51-500)' },
  { value: 'ENTERPRISE', label: 'Enterprise (500+)' },
];

export const CompanyProfilePage: React.FC = () => {
  const queryClient = useQueryClient();
  const [success, setSuccess] = useState(false);

  const { data: profile, isLoading, error } = useQuery({
    queryKey: ['company-profile'],
    queryFn: companyApi.getProfile,
  });

  const [formData, setFormData] = useState<CompanyUpdateRequest>({
    name: '', cnpj: '', website: '', sector: '', size: '',
    description: '', logoUrl: '', country: '', city: '',
  });

  useEffect(() => {
    if (profile) {
      setFormData({
        name: profile.name,
        cnpj: profile.cnpj || '',
        website: profile.website || '',
        sector: profile.sector || '',
        size: profile.size || '',
        description: profile.description || '',
        logoUrl: profile.logoUrl || '',
        country: profile.country || '',
        city: profile.city || '',
      });
    }
  }, [profile]);

  const mutation = useMutation({
    mutationFn: (data: CompanyUpdateRequest) => companyApi.updateProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['company-profile'] });
      queryClient.invalidateQueries({ queryKey: ['company-dashboard'] });
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    },
  });

  const handleChange = (field: keyof CompanyUpdateRequest) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement> | { target: { value: string } }
  ) => {
    setFormData(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    mutation.mutate(formData);
  };

  if (isLoading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}><CircularProgress /></Box>;
  }

  if (error) {
    return <Alert severity="error">Failed to load company profile</Alert>;
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>Company Profile</Typography>

      {success && <Alert severity="success" sx={{ mb: 2 }}>Profile updated successfully</Alert>}
      {mutation.isError && <Alert severity="error" sx={{ mb: 2 }}>Failed to update profile</Alert>}

      <Paper sx={{ p: 3 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth required label="Company Name"
                value={formData.name}
                onChange={handleChange('name')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="CNPJ"
                value={formData.cnpj}
                onChange={handleChange('cnpj')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Website"
                value={formData.website}
                onChange={handleChange('website')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Sector"
                value={formData.sector}
                onChange={handleChange('sector')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Company Size</InputLabel>
                <Select
                  value={formData.size}
                  label="Company Size"
                  onChange={(e) => setFormData(prev => ({ ...prev, size: e.target.value }))}
                >
                  <MenuItem value="">None</MenuItem>
                  {COMPANY_SIZES.map(s => (
                    <MenuItem key={s.value} value={s.value}>{s.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Country"
                value={formData.country}
                onChange={handleChange('country')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="City"
                value={formData.city}
                onChange={handleChange('city')}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth label="Logo URL"
                placeholder="https://yourcompany.com/logo.png"
                value={formData.logoUrl}
                onChange={handleChange('logoUrl')}
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth label="Description" multiline rows={4}
                value={formData.description}
                onChange={handleChange('description')}
              />
            </Grid>
          </Grid>

          <Button
            type="submit" variant="contained" size="large"
            sx={{ mt: 3 }}
            disabled={mutation.isPending}
          >
            {mutation.isPending ? <CircularProgress size={24} /> : 'Save Changes'}
          </Button>
        </form>
      </Paper>
    </Box>
  );
};
