import React from 'react';
import {
  Box,
  Typography,
  Chip,
  IconButton,
  Stack,
} from '@mui/material';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import LinkedInIcon from '@mui/icons-material/LinkedIn';
import GitHubIcon from '@mui/icons-material/GitHub';
import LanguageIcon from '@mui/icons-material/Language';
import LinkIcon from '@mui/icons-material/Link';
import type { PublicProfileResponse } from '../../types/publicProfile';

interface PublicProfileHeaderProps {
  profile: PublicProfileResponse;
}

const LINK_ICONS: Record<string, React.ReactElement> = {
  linkedin: <LinkedInIcon />,
  github: <GitHubIcon />,
  portfolio: <LanguageIcon />,
  website: <LanguageIcon />,
};

export const PublicProfileHeader: React.FC<PublicProfileHeaderProps> = ({ profile }) => {
  return (
    <Box sx={{ mb: 4, textAlign: { xs: 'center', md: 'left' } }}>
      <Typography variant="h3" component="h1" fontWeight="bold" gutterBottom>
        {profile.fullName}
      </Typography>
      <Typography variant="h6" color="text.secondary" gutterBottom>
        {profile.headline}
      </Typography>

      {profile.location && (
        <Stack direction="row" alignItems="center" spacing={0.5} justifyContent={{ xs: 'center', md: 'flex-start' }} sx={{ mb: 1 }}>
          <LocationOnIcon fontSize="small" color="action" />
          <Typography variant="body2" color="text.secondary">
            {profile.location}
          </Typography>
        </Stack>
      )}

      {profile.languages && profile.languages.length > 0 && (
        <Stack direction="row" spacing={1} flexWrap="wrap" justifyContent={{ xs: 'center', md: 'flex-start' }} sx={{ mb: 2, gap: 0.5 }}>
          {profile.languages.map((lang) => (
            <Chip key={lang} label={lang} size="small" variant="outlined" />
          ))}
        </Stack>
      )}

      {profile.professionalLinks && Object.keys(profile.professionalLinks).length > 0 && (
        <Stack direction="row" spacing={1} justifyContent={{ xs: 'center', md: 'flex-start' }}>
          {Object.entries(profile.professionalLinks).map(([key, url]) => (
            <IconButton
              key={key}
              component="a"
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              size="small"
              color="primary"
              title={key}
            >
              {LINK_ICONS[key.toLowerCase()] || <LinkIcon />}
            </IconButton>
          ))}
        </Stack>
      )}
    </Box>
  );
};
