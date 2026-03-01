import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  TextField,
  Button,
  Grid,
  Alert,
  Stack,
  MenuItem,
  Autocomplete,
} from '@mui/material';

interface LanguageFormProps {
  language?: string;
  onSave: (language: string) => void;
  onCancel?: () => void;
  isLoading?: boolean;
  error?: Error | null;
  existingLanguages?: string[];
}

const PROFICIENCY_LEVELS = [
  'Native',
  'Fluent',
  'Advanced',
  'Intermediate',
  'Basic',
] as const;

/**
 * Static list of common world languages (~50).
 * Stored as English names — the display is always the same regardless of locale.
 */
const LANGUAGES = [
  'Afrikaans',
  'Albanian',
  'Arabic',
  'Armenian',
  'Bengali',
  'Bulgarian',
  'Burmese',
  'Catalan',
  'Chinese (Mandarin)',
  'Chinese (Cantonese)',
  'Croatian',
  'Czech',
  'Danish',
  'Dutch',
  'English',
  'Estonian',
  'Finnish',
  'French',
  'Georgian',
  'German',
  'Greek',
  'Gujarati',
  'Hebrew',
  'Hindi',
  'Hungarian',
  'Icelandic',
  'Indonesian',
  'Italian',
  'Japanese',
  'Kannada',
  'Kazakh',
  'Korean',
  'Latvian',
  'Lithuanian',
  'Malay',
  'Malayalam',
  'Marathi',
  'Nepali',
  'Norwegian',
  'Persian',
  'Polish',
  'Portuguese',
  'Punjabi',
  'Romanian',
  'Russian',
  'Serbian',
  'Slovak',
  'Slovenian',
  'Spanish',
  'Swahili',
  'Swedish',
  'Tagalog',
  'Tamil',
  'Telugu',
  'Thai',
  'Turkish',
  'Ukrainian',
  'Urdu',
  'Vietnamese',
] as const;

export const LanguageForm: React.FC<LanguageFormProps> = ({
  language,
  onSave,
  onCancel,
  isLoading,
  error,
  existingLanguages = [],
}) => {
  const parsedName = language ? language.split(' - ')[0] : '';
  const parsedProficiency = language ? language.split(' - ')[1] || '' : '';

  const [name, setName] = useState(parsedName);
  const [proficiency, setProficiency] = useState(parsedProficiency);
  const { t } = useTranslation('profile');

  // Filter out languages already added (except the one being edited)
  const availableLanguages = LANGUAGES.filter(
    (l) => !existingLanguages.some((existing) => existing.split(' - ')[0] === l) || l === parsedName
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const value = proficiency ? `${name} - ${proficiency}` : name;
    onSave(value);
  };

  return (
    <form onSubmit={handleSubmit}>
      <Stack spacing={3}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Autocomplete
              options={availableLanguages as unknown as string[]}
              value={name || null}
              onChange={(_e, newValue) => setName(newValue || '')}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={t('languages.name')}
                  required
                  placeholder={t('languages.namePlaceholder')}
                  size="small"
                />
              )}
              freeSolo={false}
              disableClearable={!!name}
              autoHighlight
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              fullWidth
              select
              label={t('languages.proficiency')}
              value={proficiency}
              onChange={(e) => setProficiency(e.target.value)}
              size="small"
            >
              <MenuItem value="">{t('languages.selectProficiency')}</MenuItem>
              {PROFICIENCY_LEVELS.map((level) => (
                <MenuItem key={level} value={level}>
                  {t(`languages.levels.${level}`)}
                </MenuItem>
              ))}
            </TextField>
          </Grid>
        </Grid>

        {error && (
          <Alert severity="error">
            {error instanceof Error ? error.message : t('common:errors.failedToSave')}
          </Alert>
        )}

        <Stack direction="row" spacing={2} justifyContent="flex-end">
          {onCancel && (
            <Button variant="outlined" onClick={onCancel}>
              {t('common:buttons.cancel')}
            </Button>
          )}
          <Button
            type="submit"
            variant="contained"
            disabled={isLoading || !name.trim()}
          >
            {isLoading ? t('common:status.saving') : language ? t('common:buttons.update') : t('common:buttons.add')}
          </Button>
        </Stack>
      </Stack>
    </form>
  );
};
