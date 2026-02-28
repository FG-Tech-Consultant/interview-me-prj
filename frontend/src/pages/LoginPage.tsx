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
import { login, LoginRequest } from '../api/auth';
import AppVersion from '../components/common/AppVersion';

export default function LoginPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('auth');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const loginMutation = useMutation({
    mutationFn: (data: LoginRequest) => login(data),
    onSuccess: () => {
      navigate('/dashboard');
    }
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    loginMutation.mutate({ email, password });
  };

  return (
    <Container maxWidth="sm">
      <Box sx={{ mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <Paper elevation={3} sx={{ p: 4, width: '100%' }}>
          <Typography component="h1" variant="h4" align="center" gutterBottom>
            {t('login.title')}
          </Typography>

          {loginMutation.isError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {(loginMutation.error as any)?.response?.data?.message || t('login.errorDefault')}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} sx={{ mt: 2 }}>
            <TextField
              margin="normal"
              required
              fullWidth
              id="email"
              label={t('login.emailLabel')}
              name="email"
              autoComplete="email"
              autoFocus
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            <TextField
              margin="normal"
              required
              fullWidth
              name="password"
              label={t('login.passwordLabel')}
              type="password"
              id="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              sx={{ mt: 3, mb: 2 }}
              disabled={loginMutation.isPending}
            >
              {loginMutation.isPending ? t('login.submitting') : t('login.submitButton')}
            </Button>
            <Box sx={{ textAlign: 'center' }}>
              <Link to="/register" style={{ textDecoration: 'none' }}>
                <Typography variant="body2" color="primary">
                  {t('login.noAccount')}
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
