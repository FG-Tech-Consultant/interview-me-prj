import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Container,
  Box,
  Typography,
  TextField,
  Button,
  Alert,
  Paper
} from '@mui/material';
import { register, RegisterRequest } from '../api/auth';
import AppVersion from '../components/common/AppVersion';

interface FieldErrors {
  email?: string;
  password?: string;
  tenantName?: string;
}

export default function RegisterPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('auth');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [tenantName, setTenantName] = useState('');
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});

  const registerMutation = useMutation({
    mutationFn: (data: RegisterRequest) => register(data),
    onSuccess: () => {
      navigate('/dashboard');
    },
    onError: (error: any) => {
      const data = error?.response?.data;
      if (data?.fieldErrors) {
        setFieldErrors(data.fieldErrors);
      } else {
        setFieldErrors({});
      }
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFieldErrors({});
    registerMutation.mutate({ email, password, tenantName });
  };

  const hasFieldErrors = Object.keys(fieldErrors).length > 0;
  const showGenericError = registerMutation.isError && !hasFieldErrors;

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
          <Typography component="h1" variant="h4" align="center" gutterBottom>
            {t('register.title')}
          </Typography>

          {showGenericError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {(registerMutation.error as any)?.response?.data?.message || t('register.errorDefault')}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
            <TextField
              margin="normal"
              required
              fullWidth
              id="email"
              label={t('register.emailLabel')}
              name="email"
              autoComplete="email"
              autoFocus
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                if (fieldErrors.email) {
                  setFieldErrors((prev) => ({ ...prev, email: undefined }));
                }
              }}
              error={!!fieldErrors.email}
              helperText={fieldErrors.email}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label={t('register.passwordLabel')}
              type="password"
              id="password"
              autoComplete="new-password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                if (fieldErrors.password) {
                  setFieldErrors((prev) => ({ ...prev, password: undefined }));
                }
              }}
              error={!!fieldErrors.password}
              helperText={fieldErrors.password || t('register.passwordHelper')}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              id="tenantName"
              label={t('register.tenantLabel')}
              name="tenantName"
              value={tenantName}
              onChange={(e) => {
                setTenantName(e.target.value);
                if (fieldErrors.tenantName) {
                  setFieldErrors((prev) => ({ ...prev, tenantName: undefined }));
                }
              }}
              error={!!fieldErrors.tenantName}
              helperText={fieldErrors.tenantName || t('register.tenantHelper')}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={registerMutation.isPending}
            >
              {registerMutation.isPending ? t('register.submitting') : t('register.submitButton')}
            </Button>
            <Box sx={{ textAlign: 'center' }}>
              <Link to="/login" style={{ textDecoration: 'none' }}>
                <Typography variant="body2" color="primary">
                  {t('register.hasAccount')}
                </Typography>
              </Link>
            </Box>
          </Box>
        </Paper>
        <AppVersion sx={{ mt: 2 }} />
      </Box>
    </Container>
  );
}
