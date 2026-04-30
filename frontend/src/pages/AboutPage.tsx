import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import {
  Box,
  Container,
  Typography,
  Grid,
  Paper,
  Button,
  ToggleButton,
  ToggleButtonGroup
} from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import PersonSearchIcon from '@mui/icons-material/PersonSearch';
import EditNoteIcon from '@mui/icons-material/EditNote';
import ShareIcon from '@mui/icons-material/Share';
import SmartToyIcon from '@mui/icons-material/SmartToy';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import BusinessIcon from '@mui/icons-material/Business';
import WorkIcon from '@mui/icons-material/Work';
import GroupsIcon from '@mui/icons-material/Groups';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import LanguageSelector from '../components/layout/LanguageSelector';
import AppVersion from '../components/common/AppVersion';

type Audience = 'professionals' | 'companies';

export default function AboutPage() {
  const { t } = useTranslation('about');
  const [showEmail, setShowEmail] = useState(false);
  const [audience, setAudience] = useState<Audience>('professionals');

  const professionalSteps = [
    { icon: <EditNoteIcon sx={{ fontSize: 40 }} color="primary" />, title: t('how.step1Title'), desc: t('how.step1Desc') },
    { icon: <ShareIcon sx={{ fontSize: 40 }} color="primary" />, title: t('how.step2Title'), desc: t('how.step2Desc') },
    { icon: <SmartToyIcon sx={{ fontSize: 40 }} color="primary" />, title: t('how.step3Title'), desc: t('how.step3Desc') },
  ];

  const companySteps = [
    { icon: <BusinessIcon sx={{ fontSize: 40 }} color="secondary" />, title: t('howCompany.step1Title'), desc: t('howCompany.step1Desc') },
    { icon: <WorkIcon sx={{ fontSize: 40 }} color="secondary" />, title: t('howCompany.step2Title'), desc: t('howCompany.step2Desc') },
    { icon: <TrendingUpIcon sx={{ fontSize: 40 }} color="secondary" />, title: t('howCompany.step3Title'), desc: t('howCompany.step3Desc') },
  ];

  const steps = audience === 'professionals' ? professionalSteps : companySteps;
  const howTitle = audience === 'professionals' ? t('how.title') : t('howCompany.title');

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
        <Typography variant="h6" color="text.secondary" sx={{ mb: 2 }}>
          {t('subtitle')}
        </Typography>

        {/* Contact */}
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 4 }}>
          <EmailIcon fontSize="small" color="primary" sx={{ mr: 0.5 }} />
          <Typography variant="body2" color="text.secondary">
            {t('contact')} —{' '}
            {showEmail ? (
              <a href="mailto:fernando@fhgomes.com" style={{ color: 'inherit' }}>
                {t('contactEmail')}
              </a>
            ) : (
              <Typography
                component="span"
                variant="body2"
                color="primary"
                sx={{ cursor: 'pointer', textDecoration: 'underline' }}
                onClick={() => setShowEmail(true)}
              >
                {t('contactClickHere')}
              </Typography>
            )}
          </Typography>
        </Box>

        {/* What is it */}
        <Paper elevation={1} sx={{ p: 4, mb: 4 }}>
          <Typography variant="h5" fontWeight={600} gutterBottom>{t('what.title')}</Typography>
          <Typography variant="body1" color="text.secondary">{t('what.description')}</Typography>
        </Paper>

        {/* Audience Toggle */}
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 4 }}>
          <ToggleButtonGroup
            value={audience}
            exclusive
            onChange={(_, val) => { if (val) setAudience(val); }}
            sx={{
              '& .MuiToggleButton-root': {
                px: 4, py: 1.5, textTransform: 'none', fontWeight: 600, fontSize: '1rem',
              },
              '& .Mui-selected': {
                fontWeight: 700,
              },
            }}
          >
            <ToggleButton value="professionals">{t('audienceToggle.professionals')}</ToggleButton>
            <ToggleButton value="companies">{t('audienceToggle.companies')}</ToggleButton>
          </ToggleButtonGroup>
        </Box>

        {/* How it works — 3 steps (audience-dependent) */}
        <Typography variant="h5" fontWeight={600} gutterBottom sx={{ mb: 3 }}>{howTitle}</Typography>
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

        {/* Audience-specific sections */}
        {audience === 'professionals' ? (
          <Grid container spacing={4} sx={{ mb: 5 }}>
            {/* For Professionals */}
            <Grid item xs={12} md={6}>
              <Paper elevation={1} sx={{ p: 4, height: '100%' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <EditNoteIcon color="primary" sx={{ fontSize: 32, mr: 1 }} />
                  <Typography variant="h5" fontWeight={600}>{t('forProfessionals.title')}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{t('forProfessionals.description')}</Typography>
                {['bullet1', 'bullet2', 'bullet3'].map((key) => (
                  <Box key={key} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <CheckCircleOutlineIcon fontSize="small" color="primary" sx={{ mr: 1 }} />
                    <Typography variant="body2">{t(`forProfessionals.${key}`)}</Typography>
                  </Box>
                ))}
              </Paper>
            </Grid>

            {/* For Recruiters */}
            <Grid item xs={12} md={6}>
              <Paper elevation={1} sx={{ p: 4, height: '100%' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <PersonSearchIcon color="secondary" sx={{ fontSize: 32, mr: 1 }} />
                  <Typography variant="h5" fontWeight={600}>{t('forRecruiters.title')}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{t('forRecruiters.description')}</Typography>
                {['bullet1', 'bullet2', 'bullet3'].map((key) => (
                  <Box key={key} sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <CheckCircleOutlineIcon fontSize="small" color="secondary" sx={{ mr: 1 }} />
                    <Typography variant="body2">{t(`forRecruiters.${key}`)}</Typography>
                  </Box>
                ))}
              </Paper>
            </Grid>
          </Grid>
        ) : (
          <Grid container spacing={4} sx={{ mb: 5 }}>
            {/* For Companies — full width */}
            <Grid item xs={12}>
              <Paper elevation={1} sx={{ p: 4 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                  <GroupsIcon color="secondary" sx={{ fontSize: 32, mr: 1 }} />
                  <Typography variant="h5" fontWeight={600}>{t('forCompanies.title')}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>{t('forCompanies.description')}</Typography>
                <Grid container spacing={1}>
                  {['bullet1', 'bullet2', 'bullet3', 'bullet4'].map((key) => (
                    <Grid item xs={12} sm={6} key={key}>
                      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                        <CheckCircleOutlineIcon fontSize="small" color="secondary" sx={{ mr: 1 }} />
                        <Typography variant="body2">{t(`forCompanies.${key}`)}</Typography>
                      </Box>
                    </Grid>
                  ))}
                </Grid>
              </Paper>
            </Grid>
          </Grid>
        )}

        {/* CTA */}
        <Paper elevation={2} sx={{ p: 4, textAlign: 'center', mb: 4 }}>
          <Typography variant="h5" fontWeight={600} gutterBottom>{t('cta.title')}</Typography>
          <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>{t('cta.description')}</Typography>
          <Box sx={{ display: 'flex', justifyContent: 'center', gap: 2, flexWrap: 'wrap' }}>
            <Link to="/register" style={{ textDecoration: 'none' }}>
              <Button variant="contained" size="large">{t('cta.button')}</Button>
            </Link>
            <Link to="/company/register" style={{ textDecoration: 'none' }}>
              <Button variant="outlined" size="large" color="secondary">{t('cta.companyButton')}</Button>
            </Link>
          </Box>
          <Box sx={{ mt: 2 }}>
            <Link to="/login" style={{ textDecoration: 'none' }}>
              <Typography variant="body2" color="primary">{t('cta.loginLink')}</Typography>
            </Link>
          </Box>
        </Paper>

        <Box sx={{ textAlign: 'center', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 1 }}>
          <Typography variant="body2" color="text.secondary">
            Created and &copy; by{' '}
            <a href="https://github.com/fhgomes" target="_blank" rel="noopener noreferrer" style={{ color: 'inherit' }}>
              fhgomes
            </a>
          </Typography>
          <AppVersion sx={{}} />
        </Box>
      </Container>
    </Box>
  );
}
