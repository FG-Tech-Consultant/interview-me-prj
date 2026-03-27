import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Grid,
  Paper,
  Button
} from '@mui/material';
import PersonSearchIcon from '@mui/icons-material/PersonSearch';
import EditNoteIcon from '@mui/icons-material/EditNote';
import ShareIcon from '@mui/icons-material/Share';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import LanguageSelector from '../components/layout/LanguageSelector';
import AppVersion from '../components/common/AppVersion';

export default function AboutPage() {
  const { t } = useTranslation('about');

  const steps = [
    { icon: <EditNoteIcon sx={{ fontSize: 40 }} color="primary" />, title: t('how.step1Title'), desc: t('how.step1Desc') },
    { icon: <ShareIcon sx={{ fontSize: 40 }} color="primary" />, title: t('how.step2Title'), desc: t('how.step2Desc') },
    { icon: <SmartToyIcon sx={{ fontSize: 40 }} color="primary" />, title: t('how.step3Title'), desc: t('how.step3Desc') },
  ];

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: '#f5f5f5', py: 4 }}>
      <Box sx={{ position: 'absolute', top: 16, right: 16 }}>
        <LanguageSelector />
      </Box>

      <Container maxWidth="md">
        {/* Back link */}
        <Link to="/login" style={{ textDecoration: 'none', display: 'inline-flex', alignItems: 'center', marginBottom: 16 }}>
          <ArrowBackIcon fontSize="small" color="primary" />
          <Typography variant="body2" color="primary" sx={{ ml: 0.5 }}>{t('backToLogin')}</Typography>
        </Link>

        {/* Header */}
        <Typography variant="h3" fontWeight={700} gutterBottom color="primary.main">
          {t('title')}
        </Typography>
        <Typography variant="h6" color="text.secondary" sx={{ mb: 5 }}>
          {t('subtitle')}
        </Typography>

        {/* What is it */}
        <Paper elevation={1} sx={{ p: 4, mb: 4 }}>
          <Typography variant="h5" fontWeight={600} gutterBottom>{t('what.title')}</Typography>
          <Typography variant="body1" color="text.secondary">{t('what.description')}</Typography>
        </Paper>

        {/* How it works — 3 steps */}
        <Typography variant="h5" fontWeight={600} gutterBottom sx={{ mb: 3 }}>{t('how.title')}</Typography>
        <Grid container spacing={3} sx={{ mb: 5 }}>
          {steps.map((step, i) => (
            <Grid item xs={12} md={4} key={i}>
              <Paper elevation={1} sx={{ p: 3, textAlign: 'center', height: '100%' }}>
                {step.icon}
                <Typography variant="h6" fontWeight={600} sx={{ mt: 1, mb: 1 }}>{step.title}</Typography>
                <Typography variant="body2" color="text.secondary">{step.desc}</Typography>
              </Paper>
            </Grid>
          ))}
        </Grid>

        {/* For Recruiters */}
        <Grid container spacing={4} sx={{ mb: 5 }}>
          <Grid item xs={12} md={6}>
            <Paper elevation={1} sx={{ p: 4, height: '100%' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <PersonSearchIcon color="primary" sx={{ fontSize: 32, mr: 1 }} />
                <Typography variant="h5" fontWeight={600}>{t('forRecruiters.title')}</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{t('forRecruiters.description')}</Typography>
              {['bullet1', 'bullet2', 'bullet3'].map((key) => (
                <Box key={key} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <CheckCircleOutlineIcon fontSize="small" color="primary" sx={{ mr: 1 }} />
                  <Typography variant="body2">{t(`forRecruiters.${key}`)}</Typography>
                </Box>
              ))}
            </Paper>
          </Grid>

          {/* For Professionals */}
          <Grid item xs={12} md={6}>
            <Paper elevation={1} sx={{ p: 4, height: '100%' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                <EditNoteIcon color="secondary" sx={{ fontSize: 32, mr: 1 }} />
                <Typography variant="h5" fontWeight={600}>{t('forProfessionals.title')}</Typography>
              </Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{t('forProfessionals.description')}</Typography>
              {['bullet1', 'bullet2', 'bullet3'].map((key) => (
                <Box key={key} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                  <CheckCircleOutlineIcon fontSize="small" color="secondary" sx={{ mr: 1 }} />
                  <Typography variant="body2">{t(`forProfessionals.${key}`)}</Typography>
                </Box>
              ))}
            </Paper>
          </Grid>
        </Grid>

        {/* CTA */}
        <Paper elevation={2} sx={{ p: 4, textAlign: 'center', mb: 4 }}>
          <Typography variant="h5" fontWeight={600} gutterBottom>{t('cta.title')}</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>{t('cta.description')}</Typography>
          <Link to="/register" style={{ textDecoration: 'none' }}>
            <Button variant="contained" size="large">{t('cta.button')}</Button>
          </Link>
          <Box sx={{ mt: 2 }}>
            <Link to="/login" style={{ textDecoration: 'none' }}>
              <Typography variant="body2" color="primary">{t('cta.loginLink')}</Typography>
            </Link>
          </Box>
        </Paper>

        <AppVersion sx={{ textAlign: 'center' }} />
        <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', mt: 1 }}>
          Created and &copy; by{' '}
          <a href="https://github.com/fhgomes" target="_blank" rel="noopener noreferrer" style={{ color: 'inherit' }}>
            fhgomes
          </a>
        </Typography>
      </Container>
    </Box>
  );
}
