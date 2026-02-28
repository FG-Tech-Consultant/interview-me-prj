import React from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Container, Box, Typography, Skeleton, Divider, Link } from '@mui/material';
import { usePublicProfile } from '../hooks/usePublicProfile';
import { PublicProfileSeo } from '../components/public-profile/PublicProfileSeo';
import { PublicProfileHeader } from '../components/public-profile/PublicProfileHeader';
import { PublicProfileSummary } from '../components/public-profile/PublicProfileSummary';
import { PublicSkillsSection } from '../components/public-profile/PublicSkillsSection';
import { PublicWorkTimeline } from '../components/public-profile/PublicWorkTimeline';
import { PublicEducationSection } from '../components/public-profile/PublicEducationSection';
import { PublicProfileNotFound } from '../components/public-profile/PublicProfileNotFound';
import { ChatWidget } from '../components/chat/ChatWidget';

const PublicProfilePage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const { data: profile, isLoading, error } = usePublicProfile(slug || '');
  const { t } = useTranslation('public-profile');

  if (isLoading) {
    return (
      <Container maxWidth="md" sx={{ py: 6 }}>
        <Skeleton variant="text" width="60%" height={60} />
        <Skeleton variant="text" width="40%" height={30} />
        <Skeleton variant="text" width="30%" height={20} sx={{ mb: 4 }} />
        <Skeleton variant="rectangular" height={100} sx={{ mb: 3 }} />
        <Skeleton variant="rectangular" height={200} sx={{ mb: 3 }} />
        <Skeleton variant="rectangular" height={300} />
      </Container>
    );
  }

  if (error || !profile) {
    return <PublicProfileNotFound />;
  }

  const hasPublicContent =
    profile.skills.length > 0 ||
    profile.jobs.length > 0 ||
    profile.education.length > 0;

  return (
    <>
      <PublicProfileSeo profile={profile} />

      <Container maxWidth="md" sx={{ py: 6 }}>
        <PublicProfileHeader profile={profile} />

        {profile.summary && <PublicProfileSummary summary={profile.summary} />}

        {!hasPublicContent && (
          <Box sx={{ textAlign: 'center', py: 6 }}>
            <Typography variant="h6" color="text.secondary">
              {t('noPublicContent')}
            </Typography>
          </Box>
        )}

        {profile.skills.length > 0 && (
          <PublicSkillsSection skills={profile.skills} />
        )}

        {profile.jobs.length > 0 && (
          <PublicWorkTimeline jobs={profile.jobs} />
        )}

        {profile.education.length > 0 && (
          <PublicEducationSection education={profile.education} />
        )}

        {/* Footer */}
        <Divider sx={{ mt: 4, mb: 2 }} />
        <Box sx={{ textAlign: 'center', py: 2 }}>
          <Typography variant="caption" color="text.secondary">
            {t('poweredBy')}{' '}
            <Link href="/" color="primary" underline="hover">
              {t('interviewMe')}
            </Link>
          </Typography>
        </Box>
      </Container>

      <ChatWidget slug={slug || ''} profileName={profile.fullName} />
    </>
  );
};

export default PublicProfilePage;
