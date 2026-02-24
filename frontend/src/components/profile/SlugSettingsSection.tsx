import React, { useState } from 'react';
import {
  Box,
  Typography,
  TextField,
  Button,
  InputAdornment,
  IconButton,
  Alert,
  Stack,
  CircularProgress,
} from '@mui/material';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { useCheckSlug, useUpdateSlug } from '../../hooks/usePublicProfile';

interface SlugSettingsSectionProps {
  profileId: number;
  currentSlug: string | null;
}

export const SlugSettingsSection: React.FC<SlugSettingsSectionProps> = ({
  profileId,
  currentSlug,
}) => {
  const [slug, setSlug] = useState(currentSlug || '');
  const [saved, setSaved] = useState(false);
  const [copied, setCopied] = useState(false);

  const { data: slugCheck, isLoading: isChecking } = useCheckSlug(slug);
  const updateSlug = useUpdateSlug();

  const isCurrentSlug = slug === currentSlug;
  const isValid = slug.length >= 3 && /^[a-z0-9][a-z0-9-]*[a-z0-9]$/.test(slug) && !slug.includes('--');
  const isAvailable = slugCheck?.available || isCurrentSlug;
  const canSave = isValid && isAvailable && !isCurrentSlug && !updateSlug.isPending;

  const handleSlugChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '');
    setSlug(value);
    setSaved(false);
  };

  const handleSave = async () => {
    try {
      await updateSlug.mutateAsync({ profileId, slug });
      setSaved(true);
    } catch {
      // Error handled by mutation
    }
  };

  const handleCopyLink = () => {
    const url = `${window.location.origin}/p/${slug}`;
    navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handlePreview = () => {
    window.open(`/p/${slug}`, '_blank');
  };

  const getHelperText = () => {
    if (!slug || slug.length < 3) return 'Minimum 3 characters';
    if (!isValid) return 'Lowercase letters, numbers, and hyphens only. No consecutive hyphens.';
    if (isCurrentSlug) return 'This is your current slug';
    if (isChecking) return 'Checking availability...';
    if (slugCheck && !slugCheck.available) {
      const suggestions = slugCheck.suggestions.join(', ');
      return suggestions ? `Already taken. Try: ${suggestions}` : 'Already taken';
    }
    if (slugCheck?.available) return 'Available!';
    return '';
  };

  const getStatusIcon = () => {
    if (isCurrentSlug && slug.length >= 3) return <CheckCircleIcon color="success" />;
    if (isChecking) return <CircularProgress size={20} />;
    if (!isValid || !slug || slug.length < 3) return null;
    if (slugCheck?.available) return <CheckCircleIcon color="success" />;
    if (slugCheck && !slugCheck.available) return <CancelIcon color="error" />;
    return null;
  };

  return (
    <Box sx={{ mt: 4 }}>
      <Typography variant="h6" gutterBottom>
        Public Profile
      </Typography>

      {currentSlug && slug !== currentSlug && slug.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Changing your URL will make the old one inaccessible immediately.
        </Alert>
      )}

      <TextField
        fullWidth
        label="Profile URL Slug"
        value={slug}
        onChange={handleSlugChange}
        helperText={getHelperText()}
        error={slug.length >= 3 && !isValid}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <Typography variant="body2" color="text.secondary">
                interviewme.app/p/
              </Typography>
            </InputAdornment>
          ),
          endAdornment: (
            <InputAdornment position="end">{getStatusIcon()}</InputAdornment>
          ),
        }}
        sx={{ mb: 2 }}
      />

      <Stack direction="row" spacing={1}>
        <Button
          variant="contained"
          onClick={handleSave}
          disabled={!canSave}
        >
          {updateSlug.isPending ? 'Saving...' : 'Save'}
        </Button>

        {(currentSlug || saved) && (
          <>
            <Button
              variant="outlined"
              startIcon={<OpenInNewIcon />}
              onClick={handlePreview}
            >
              Preview
            </Button>
            <IconButton onClick={handleCopyLink} title="Copy link">
              <ContentCopyIcon />
            </IconButton>
          </>
        )}
      </Stack>

      {saved && (
        <Alert severity="success" sx={{ mt: 2 }}>
          Slug updated successfully! Your profile is live at: /p/{slug}
        </Alert>
      )}

      {copied && (
        <Alert severity="info" sx={{ mt: 1 }}>
          Link copied to clipboard!
        </Alert>
      )}

      {updateSlug.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to update slug. Please try again.
        </Alert>
      )}
    </Box>
  );
};
