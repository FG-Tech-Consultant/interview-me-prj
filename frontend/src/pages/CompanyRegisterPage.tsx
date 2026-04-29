import React, { useState } from 'react';
import {
  Box, Button, Container, TextField, Typography, Paper, Grid,
  MenuItem, Select, FormControl, InputLabel, Alert, CircularProgress,
  Link as MuiLink
} from '@mui/material';
import { useNavigate, Link } from 'react-router-dom';
import { companyApi } from '../api/companyApi';
import type { CompanyRegistrationRequest } from '../types/company';

const COMPANY_SIZES = [
  { value: 'STARTUP', label: 'Startup (1-50)' },
  { value: 'SMB', label: 'SMB (51-500)' },
  { value: 'ENTERPRISE', label: 'Enterprise (500+)' },
];

export const CompanyRegisterPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<CompanyRegistrationRequest>({
    companyName: '',
    sector: '',
    size: '',
    website: '',
    country: '',
    city: '',
    description: '',
    adminName: '',
    email: '',
    password: '',
  });

  const handleChange = (field: keyof CompanyRegistrationRequest) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement> | { target: { value: string } }
  ) => {
    setFormData(prev => ({ ...prev, [field]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const response = await companyApi.register(formData);
      localStorage.setItem('token', response.token);
      navigate('/company/dashboard');
    } catch (err: unknown) {
      const errorMsg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
        || 'Registration failed. Please try again.';
      setError(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Paper sx={{ p: 4 }}>
        <Typography variant="h4" gutterBottom align="center">
          Register Your Company
        </Typography>
        <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 3 }}>
          Create your company account to post jobs and find candidates
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        <form onSubmit={handleSubmit}>
          <Typography variant="h6" sx={{ mt: 2, mb: 1 }}>Company Information</Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth required label="Company Name"
                value={formData.companyName}
                onChange={handleChange('companyName')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Sector"
                placeholder="e.g. Fintech, Healthcare, E-commerce"
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
                  {COMPANY_SIZES.map(s => (
                    <MenuItem key={s.value} value={s.value}>{s.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Website"
                placeholder="https://yourcompany.com"
                value={formData.website}
                onChange={handleChange('website')}
              />
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
                fullWidth label="Description" multiline rows={3}
                placeholder="Brief description of your company"
                value={formData.description}
                onChange={handleChange('description')}
              />
            </Grid>
          </Grid>

          <Typography variant="h6" sx={{ mt: 3, mb: 1 }}>Admin Account</Typography>
          <Grid container spacing={2}>
            <Grid item xs={12}>
              <TextField
                fullWidth required label="Your Name"
                value={formData.adminName}
                onChange={handleChange('adminName')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth required label="Email" type="email"
                value={formData.email}
                onChange={handleChange('email')}
              />
            </Grid>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth required label="Password" type="password"
                inputProps={{ minLength: 6 }}
                value={formData.password}
                onChange={handleChange('password')}
              />
            </Grid>
          </Grid>

          <Button
            type="submit" variant="contained" size="large" fullWidth
            sx={{ mt: 3 }}
            disabled={loading}
          >
            {loading ? <CircularProgress size={24} /> : 'Register Company'}
          </Button>
        </form>

        <Box sx={{ mt: 2, textAlign: 'center' }}>
          <Typography variant="body2">
            Already have an account?{' '}
            <MuiLink component={Link} to="/login">Sign in</MuiLink>
          </Typography>
        </Box>
      </Paper>
    </Container>
  );
};
