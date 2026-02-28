import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Autocomplete, TextField, Typography, Box } from '@mui/material';
import { useSkillCatalogSearch } from '../../hooks/useSkillCatalog';
import type { SkillDto } from '../../types/skill';

interface SkillSelectorProps {
  value: SkillDto | null;
  onChange: (skill: SkillDto | null) => void;
  error?: string | null;
}

export const SkillSelector: React.FC<SkillSelectorProps> = ({ value, onChange, error }) => {
  const [inputValue, setInputValue] = useState('');
  const { data: options = [], isLoading } = useSkillCatalogSearch(inputValue);
  const { t } = useTranslation('skills');

  return (
    <Autocomplete
      value={value}
      onChange={(_, newValue) => onChange(newValue)}
      inputValue={inputValue}
      onInputChange={(_, newInputValue) => setInputValue(newInputValue)}
      options={options}
      getOptionLabel={(option) => option.name}
      isOptionEqualToValue={(option, val) => option.id === val.id}
      loading={isLoading}
      noOptionsText={inputValue.length < 2 ? t('selector.minChars') : t('selector.noResults')}
      renderOption={(props, option) => (
        <Box component="li" {...props} key={option.id}>
          <Box>
            <Typography variant="body1">{option.name}</Typography>
            <Typography variant="caption" color="text.secondary">
              {option.category}
            </Typography>
          </Box>
        </Box>
      )}
      renderInput={(params) => (
        <TextField
          {...params}
          label={t('selector.label')}
          placeholder={t('selector.placeholder')}
          error={!!error}
          helperText={error}
          fullWidth
        />
      )}
    />
  );
};
