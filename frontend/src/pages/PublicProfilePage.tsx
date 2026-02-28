import React, { useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Container, Box, Typography, Skeleton } from '@mui/material';
import WorkIcon from '@mui/icons-material/Work';
import SchoolIcon from '@mui/icons-material/School';
import StarIcon from '@mui/icons-material/Star';
import { usePublicProfile } from '../hooks/usePublicProfile';
import { PublicProfileSeo } from '../components/public-profile/PublicProfileSeo';
import { PublicProfileHeader } from '../components/public-profile/PublicProfileHeader';
import { PublicProfileSummary } from '../components/public-profile/PublicProfileSummary';
import { PublicSkillsSection } from '../components/public-profile/PublicSkillsSection';
import { PublicWorkTimeline } from '../components/public-profile/PublicWorkTimeline';
import { PublicEducationSection } from '../components/public-profile/PublicEducationSection';
import { PublicProfileNotFound } from '../components/public-profile/PublicProfileNotFound';
import { CareerAssistantCTA } from '../components/public-profile/CareerAssistantCTA';
import { CollapsibleSection } from '../components/public-profile/CollapsibleSection';
import { ChatWidget } from '../components/chat/ChatWidget';
import PoweredByFooter from '../components/common/PoweredByFooter';

const PublicProfilePage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const { data: profile, isLoading, error } = usePublicProfile(slug || '');
  const { t } = useTranslation('public-profile');
  const [chatOpen, setChatOpen] = useState(false);

  const handleChatOpen = useCallback(() => {
    setChatOpen(true);
  }, []);

  const handleChatOpenChange = useCallback((open: boolean) => {
    setChatOpen(open);
  }, []);

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

        <CareerAssistantCTA
          profileName={profile.fullName}
          onChatOpen={handleChatOpen}
        />

        {profile.summary && <PublicProfileSummary summary={profile.summary} />}

        {!hasPublicContent && (
          <Box sx={{ textAlign: 'center', py: 6 }}>
            <Typography variant="h6" color="text.secondary">
              {t('noPublicContent')}
            </Typography>
          </Box>
        )}

        {profile.skills.length > 0 && (
          <CollapsibleSection
            title={t('sections.skills')}
            icon={<StarIcon color="action" />}
          >
            <PublicSkillsSection skills={profile.skills} />
          </CollapsibleSection>
        )}

        {profile.jobs.length > 0 && (
          <CollapsibleSection
            title={t('sections.experience')}
            icon={<WorkIcon color="action" />}
          >
            <PublicWorkTimeline jobs={profile.jobs} />
          </CollapsibleSection>
        )}

        {profile.education.length > 0 && (
          <CollapsibleSection
            title={t('sections.education')}
            icon={<SchoolIcon color="action" />}
          >
            <PublicEducationSection education={profile.education} />
          </CollapsibleSection>
        )}

        <PoweredByFooter />
      </Container>

      <ChatWidget
        slug={slug || ''}
        profileName={profile.fullName}
        externalOpen={chatOpen}
        onOpenChange={handleChatOpenChange}
      />
    </>
  );
};

export default PublicProfilePage;
