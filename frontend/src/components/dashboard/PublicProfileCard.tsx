import { useState } from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  IconButton,
  Skeleton,
  Snackbar,
  Tooltip,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import LinkIcon from '@mui/icons-material/Link';
import { useNavigate } from 'react-router-dom';
import { useCurrentProfile } from '../../hooks/useProfile';

const PublicProfileCard = () => {
  const { data: profile, isLoading } = useCurrentProfile();
  const navigate = useNavigate();
  const [snackbarOpen, setSnackbarOpen] = useState(false);

  if (isLoading) {
    return (
      <Card variant="outlined">
        <CardContent>
          <Skeleton width={120} height={28} />
          <Skeleton width="100%" height={20} sx={{ mt: 1 }} />
          <Skeleton width={80} height={36} sx={{ mt: 1 }} />
        </CardContent>
      </Card>
    );
  }

  const slug = profile?.slug;
  const publicUrl = slug ? `${window.location.origin}/p/${slug}` : null;

  const handleCopy = async () => {
    if (publicUrl) {
      await navigator.clipboard.writeText(publicUrl);
      setSnackbarOpen(true);
    }
  };

  return (
    <Card variant="outlined">
      <CardContent>
        <Box display="flex" alignItems="center" gap={1} mb={1}>
          <LinkIcon color="action" fontSize="small" />
          <Typography variant="subtitle1" fontWeight="bold">
            Public Profile
          </Typography>
        </Box>

        {publicUrl ? (
          <>
            <Box
              display="flex"
              alignItems="center"
              gap={1}
              sx={{
                bgcolor: 'grey.50',
                borderRadius: 1,
                px: 1.5,
                py: 0.75,
              }}
            >
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
              >
                {publicUrl}
              </Typography>
              <Tooltip title="Copy URL">
                <IconButton size="small" onClick={handleCopy}>
                  <ContentCopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
            <Box mt={1.5}>
              <Button
                size="small"
                variant="outlined"
                endIcon={<OpenInNewIcon />}
                href={publicUrl}
                target="_blank"
                rel="noopener noreferrer"
              >
                View Profile
              </Button>
            </Box>
          </>
        ) : (
          <>
            <Typography variant="body2" color="text.secondary" mb={1.5}>
              Set up your public profile URL
            </Typography>
            <Button size="small" variant="outlined" onClick={() => navigate('/profile')}>
              Go to Profile
            </Button>
          </>
        )}
      </CardContent>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={2000}
        onClose={() => setSnackbarOpen(false)}
        message="Copied!"
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </Card>
  );
};

export default PublicProfileCard;
