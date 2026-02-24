import React from 'react';
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
          label="Search"
          size="small"
          value={filters.search}
          onChange={(e) => onFilterChange({ ...filters, search: e.target.value })}
          sx={{ minWidth: 200 }}
        />

        <FormControl size="small" sx={{ minWidth: 150 }}>
          <InputLabel>Category</InputLabel>
          <Select
            value={filters.category}
            label="Category"
            onChange={(e) => onFilterChange({ ...filters, category: e.target.value })}
          >
            <MenuItem value="">All Categories</MenuItem>
            {SKILL_CATEGORIES.map((cat) => (
              <MenuItem key={cat} value={cat}>
                {cat}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>Visibility</InputLabel>
          <Select
            value={filters.visibility}
            label="Visibility"
            onChange={(e) => onFilterChange({ ...filters, visibility: e.target.value })}
          >
            <MenuItem value="all">All</MenuItem>
            <MenuItem value="public">Public</MenuItem>
            <MenuItem value="private">Private</MenuItem>
          </Select>
        </FormControl>

        <Box sx={{ minWidth: 180 }}>
          <Typography variant="caption" color="text.secondary">
            Proficiency: {filters.minProficiency}-{filters.maxProficiency}
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
            Clear Filters
          </Button>
        )}

        <Chip
          label={`Showing ${filteredCount} of ${totalCount} skills`}
          size="small"
          variant="outlined"
        />
      </Box>
    </Box>
  );
};

export { defaultFilters };
