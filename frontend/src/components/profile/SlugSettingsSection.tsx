import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
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
import { useWallet, useInvalidateWallet } from '../../hooks/useBilling';

interface SlugSettingsSectionProps {
  profileId: number;
  currentSlug: string | null;
  slugChangeCount: number;
}

export const SlugSettingsSection: React.FC<SlugSettingsSectionProps> = ({
  profileId,
  currentSlug,
  slugChangeCount,
}) => {
  const [slug, setSlug] = useState(currentSlug || '');
  const [saved, setSaved] = useState(false);
  const [copied, setCopied] = useState(false);
  const { t } = useTranslation('profile');

  const { data: slugCheck, isLoading: isChecking } = useCheckSlug(slug);
  const updateSlug = useUpdateSlug();
  const { data: wallet } = useWallet();
  const invalidateWallet = useInvalidateWallet();

  const isCurrentSlug = slug === currentSlug;
  const isValid = slug.length >= 3 && /^[a-z0-9][a-z0-9-]*[a-z0-9]$/.test(slug) && !slug.includes('--');
  const isAvailable = slugCheck?.available || isCurrentSlug;
  const isChangingSlug = !!currentSlug && !isCurrentSlug;
  const isFirstChange = slugChangeCount === 0;
  const changeCost = slugCheck?.changeCost ?? 0;
  const balance = wallet?.balance ?? 0;
  const willCostCredits = isChangingSlug && !isFirstChange;
  const hasEnoughCredits = !willCostCredits || balance >= changeCost;
  const canSave = isValid && isAvailable && !isCurrentSlug && !updateSlug.isPending && hasEnoughCredits;

  const handleSlugChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, '');
    setSlug(value);
    setSaved(false);
  };

  const handleSave = async () => {
    try {
      await updateSlug.mutateAsync({ profileId, slug });
      setSaved(true);
      if (willCostCredits) {
        invalidateWallet();
      }
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
    if (!slug || slug.length < 3) return t('slug.minChars');
    if (!isValid) return t('slug.invalidFormat');
    if (isCurrentSlug) return t('slug.currentSlug');
    if (isChecking) return t('slug.checking');
    if (slugCheck && !slugCheck.available) {
      const suggestions = slugCheck.suggestions.join(', ');
      return suggestions ? t('slug.takenWithSuggestions', { suggestions }) : t('slug.taken');
    }
    if (slugCheck?.available) return t('slug.available');
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
        {t('slug.title')}
      </Typography>

      {currentSlug && slug !== currentSlug && slug.length > 0 && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {t('slug.changeWarning')}
        </Alert>
      )}

      {isChangingSlug && isFirstChange && slug.length >= 3 && (
        <Alert severity="success" sx={{ mb: 2 }}>
          {t('slug.firstChangeFree')}
        </Alert>
      )}

      {willCostCredits && changeCost > 0 && slug.length >= 3 && (
        <Alert severity={hasEnoughCredits ? 'info' : 'error'} sx={{ mb: 2 }}>
          {t('slug.changeCost', { cost: changeCost, balance })}
          {!hasEnoughCredits && ` ${t('slug.notEnoughCredits')}`}
        </Alert>
      )}

      <TextField
        fullWidth
        label={t('slug.label')}
        value={slug}
        onChange={handleSlugChange}
        helperText={getHelperText()}
        error={slug.length >= 3 && !isValid}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <Typography variant="body2" color="text.secondary">
                {t('slug.prefix')}
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
          {updateSlug.isPending ? t('common:status.saving') : willCostCredits ? t('slug.saveWithCost', { cost: changeCost }) : t('common:buttons.save')}
        </Button>

        {(currentSlug || saved) && (
          <>
            <Button
              variant="outlined"
              startIcon={<OpenInNewIcon />}
              onClick={handlePreview}
            >
              {t('common:buttons.preview')}
            </Button>
            <IconButton onClick={handleCopyLink} title={t('slug.copyLink')}>
              <ContentCopyIcon />
            </IconButton>
          </>
        )}
      </Stack>

      {saved && (
        <Alert severity="success" sx={{ mt: 2 }}>
          {t('slug.savedSuccess', { slug })}
          {willCostCredits && changeCost > 0 && ` ${t('slug.creditsDeducted', { cost: changeCost })}`}
        </Alert>
      )}

      {copied && (
        <Alert severity="info" sx={{ mt: 1 }}>
          {t('slug.linkCopied')}
        </Alert>
      )}

      {updateSlug.isError && (
        <Alert severity="error" sx={{ mt: 2 }}>
          {t('slug.failedToUpdate')}
        </Alert>
      )}
    </Box>
  );
};
