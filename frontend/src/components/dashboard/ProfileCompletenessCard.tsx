import { useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Skeleton,
  Typography,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import { useCurrentProfile } from '../../hooks/useProfile';
import { useUserSkills } from '../../hooks/useSkills';

interface CompletionItem {
  label: string;
  completed: boolean;
  path: string;
}

const ProfileCompletenessCard = () => {
  const navigate = useNavigate();
  const { data: profile, isLoading: profileLoading } = useCurrentProfile();
  const { data: skills, isLoading: skillsLoading } = useUserSkills(profile?.id);

  const isLoading = profileLoading || skillsLoading;

  const items = useMemo<CompletionItem[]>(() => {
    if (!profile) return [];
    return [
      {
        label: 'Full name',
        completed: !!profile.fullName?.trim(),
        path: '/profile',
      },
      {
        label: 'Headline',
        completed: !!profile.headline?.trim(),
        path: '/profile',
      },
      {
        label: 'Summary',
        completed: !!profile.summary?.trim(),
        path: '/profile',
      },
      {
        label: 'Work experience',
        completed: profile.jobs?.length > 0,
        path: '/profile',
      },
      {
        label: 'Education',
        completed: profile.education?.length > 0,
        path: '/profile',
      },
      {
        label: 'Skills',
        completed: !!skills && Object.values(skills).some((arr) => arr.length > 0),
        path: '/skills',
      },
      {
        label: 'Public profile',
        completed: !!profile.slug?.trim(),
        path: '/profile',
      },
    ];
  }, [profile, skills]);

  const completedCount = items.filter((i) => i.completed).length;
  const percentage = items.length > 0 ? Math.round((completedCount / items.length) * 100) : 0;

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Skeleton variant="text" width="60%" height={32} />
          <Box sx={{ display: 'flex', justifyContent: 'center', my: 3 }}>
            <Skeleton variant="circular" width={80} height={80} />
          </Box>
          {Array.from({ length: 7 }).map((_, i) => (
            <Skeleton key={i} variant="text" height={40} />
          ))}
        </CardContent>
      </Card>
    );
  }

  if (!profile) return null;

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Profile Completeness
        </Typography>

        <Box sx={{ display: 'flex', justifyContent: 'center', my: 2 }}>
          <Box sx={{ position: 'relative', display: 'inline-flex' }}>
            <CircularProgress
              variant="determinate"
              value={percentage}
              size={80}
              thickness={5}
              color={percentage === 100 ? 'success' : 'primary'}
            />
            <Box
              sx={{
                position: 'absolute',
                top: 0,
                left: 0,
                bottom: 0,
                right: 0,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Typography variant="body1" fontWeight="bold" color="text.secondary">
                {percentage}%
              </Typography>
            </Box>
          </Box>
        </Box>

        <List disablePadding dense>
          {items.map((item) =>
            item.completed ? (
              <ListItemButton key={item.label} disabled sx={{ opacity: 1 }}>
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <CheckCircleIcon color="success" fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ) : (
              <ListItemButton key={item.label} onClick={() => navigate(item.path)}>
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <RadioButtonUncheckedIcon color="disabled" fontSize="small" />
                </ListItemIcon>
                <ListItemText
                  primary={item.label}
                  secondary="Click to add"
                  secondaryTypographyProps={{ variant: 'caption' }}
                />
              </ListItemButton>
            )
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default ProfileCompletenessCard;
