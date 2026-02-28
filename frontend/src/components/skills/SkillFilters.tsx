import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Slider,
  Typography,
  Button,
  TextField,
  Chip,
} from '@mui/material';
import { SKILL_CATEGORIES } from '../../types/skill';

export interface SkillFilterValues {
  category: string;
  minProficiency: number;
  maxProficiency: number;
  visibility: string;
  search: string;
}

interface SkillFiltersProps {
  filters: SkillFilterValues;
  onFilterChange: (filters: SkillFilterValues) => void;
  totalCount: number;
  filteredCount: number;
}

const defaultFilters: SkillFilterValues = {
  category: '',
  minProficiency: 1,
  maxProficiency: 5,
  visibility: 'all',
  search: '',
};

export const SkillFilters: React.FC<SkillFiltersProps> = ({
  filters,
  onFilterChange,
  totalCount,
  filteredCount,
}) => {
  const { t } = useTranslation('skills');

  const handleClear = () => {
    onFilterChange(defaultFilters);
  };

  const hasActiveFilters =
    filters.category !== '' ||
    filters.minProficiency !== 1 ||
    filters.maxProficiency !== 5 ||
    filters.visibility !== 'all' ||
    filters.search !== '';

  return (
    <Box sx={{ mb: 2, p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}>
      <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
        <TextField
          label={t('filters.search')}
          size="small"
          value={filters.search}
          onChange={(e) => onFilterChange({ ...filters, search: e.target.value })}
          sx={{ minWidth: 200 }}
        />

        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>{t('filters.category')}</InputLabel>
          <Select
            value={filters.category}
            label={t('filters.category')}
            onChange={(e) => onFilterChange({ ...filters, category: e.target.value })}
          >
            <MenuItem value="">{t('filters.allCategories')}</MenuItem>
            {SKILL_CATEGORIES.map((cat) => (
              <MenuItem key={cat} value={cat}>
                {cat}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>{t('filters.visibility')}</InputLabel>
          <Select
            value={filters.visibility}
            label={t('filters.visibility')}
            onChange={(e) => onFilterChange({ ...filters, visibility: e.target.value })}
          >
            <MenuItem value="all">{t('filters.all')}</MenuItem>
            <MenuItem value="public">{t('common:visibility.public')}</MenuItem>
            <MenuItem value="private">{t('common:visibility.private')}</MenuItem>
          </Select>
        </FormControl>

        <Box sx={{ minWidth: 180 }}>
          <Typography variant="caption" color="text.secondary">
            {t('filters.proficiency')}: {filters.minProficiency}-{filters.maxProficiency}
          </Typography>
          <Slider
            value={[filters.minProficiency, filters.maxProficiency]}
            onChange={(_, v) => {
              const [min, max] = v as number[];
              onFilterChange({ ...filters, minProficiency: min, maxProficiency: max });
            }}
            min={1}
            max={5}
            step={1}
            size="small"
            valueLabelDisplay="auto"
          />
        </Box>

        {hasActiveFilters && (
          <Button size="small" onClick={handleClear}>
            {t('filters.clearFilters')}
          </Button>
        )}

        <Chip
          label={t('filters.showing', { filtered: filteredCount, total: totalCount })}
          size="small"
          variant="outlined"
        />
      </Box>
    </Box>
  );
};

export { defaultFilters };
