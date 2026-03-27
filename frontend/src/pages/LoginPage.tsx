import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Typography,
  TextField,
  Button,
  Alert,
  Paper,
  Grid,
  useMediaQuery,
  useTheme
} from '@mui/material';
import ChatIcon from '@mui/icons-material/Chat';
import WorkIcon from '@mui/icons-material/Work';
import ShareIcon from '@mui/icons-material/Share';
import { login, LoginRequest } from '../api/auth';
import AppVersion from '../components/common/AppVersion';
import LanguageSelector from '../components/layout/LanguageSelector';

export default function LoginPage() {
  const navigate = useNavigate();
  const { t } = useTranslation('auth');
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
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

  const bullets = [
    { icon: <ChatIcon color="primary" />, text: t('login.heroBullet1') },
    { icon: <WorkIcon color="primary" />, text: t('login.heroBullet2') },
    { icon: <ShareIcon color="primary" />, text: t('login.heroBullet3') },
  ];

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', bgcolor: '#f5f5f5' }}>
      <Box sx={{ position: 'absolute', top: 16, right: 16 }}>
        <LanguageSelector />
      </Box>
      <Grid container sx={{ maxWidth: 1000, mx: 'auto', px: 2 }}>
        {/* Left — Login Form */}
        <Grid item xs={12} md={5}>
          <Paper elevation={3} sx={{ p: 4 }}>
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
          <AppVersion sx={{ mt: 2, textAlign: 'center' }} />
        </Grid>

        {/* Right — Hero Text */}
        <Grid item xs={12} md={7}>
          <Box sx={{
            pl: isMobile ? 0 : 6,
            pt: isMobile ? 4 : 0,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'center',
            height: '100%'
          }}>
            <Typography variant="h4" fontWeight={700} gutterBottom color="primary.main">
              {t('login.heroTitle')}
            </Typography>
            <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
              {t('login.heroSubtitle')}
            </Typography>

            {bullets.map((b, i) => (
              <Box key={i} sx={{ display: 'flex', alignItems: 'center', mb: 1.5 }}>
                {b.icon}
                <Typography variant="body1" sx={{ ml: 1.5 }}>{b.text}</Typography>
              </Box>
            ))}

            <Box sx={{ mt: 3 }}>
              <Link to="/about" style={{ textDecoration: 'none' }}>
                <Typography variant="body2" color="primary" fontWeight={500}>
                  {t('login.learnMore')} &rarr;
                </Typography>
              </Link>
            </Box>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
}
