import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Slider,
  Typography,
  Box,
  Switch,
  FormControlLabel,
  Chip,
  Autocomplete,
} from '@mui/material';
import { SkillSelector } from './SkillSelector';
import type { SkillDto, UserSkillDto, AddUserSkillRequest, UpdateUserSkillRequest } from '../../types/skill';
import { PROFICIENCY_LABELS } from '../../types/skill';

interface SkillFormDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: AddUserSkillRequest | UpdateUserSkillRequest) => void;
  skill?: UserSkillDto | null;
  mode: 'add' | 'edit';
  isLoading?: boolean;
}

export const SkillFormDialog: React.FC<SkillFormDialogProps> = ({
  open,
  onClose,
  onSubmit,
  skill,
  mode,
  isLoading,
}) => {
  const [selectedSkill, setSelectedSkill] = useState<SkillDto | null>(null);
  const [yearsOfExperience, setYearsOfExperience] = useState(0);
  const [proficiencyDepth, setProficiencyDepth] = useState(3);
  const [lastUsedDate, setLastUsedDate] = useState('');
  const [confidenceLevel, setConfidenceLevel] = useState('MEDIUM');
  const [tags, setTags] = useState<string[]>([]);
  const [isPublic, setIsPublic] = useState(false);
  const [skillError, setSkillError] = useState<string | null>(null);
  const { t } = useTranslation('skills');

  useEffect(() => {
    if (mode === 'edit' && skill) {
      setYearsOfExperience(skill.yearsOfExperience);
      setProficiencyDepth(skill.proficiencyDepth);
      setLastUsedDate(skill.lastUsedDate || '');
      setConfidenceLevel(skill.confidenceLevel);
      setTags(skill.tags || []);
      setIsPublic(skill.visibility === 'public');
      setSelectedSkill(skill.skill);
    } else {
      setSelectedSkill(null);
      setYearsOfExperience(0);
      setProficiencyDepth(3);
      setLastUsedDate('');
      setConfidenceLevel('MEDIUM');
      setTags([]);
      setIsPublic(false);
      setSkillError(null);
    }
  }, [open, mode, skill]);

  const handleSubmit = () => {
    if (mode === 'add') {
      if (!selectedSkill) {
        setSkillError(t('form.pleaseSelectSkill'));
        return;
      }
      const data: AddUserSkillRequest = {
        skillId: selectedSkill.id,
        yearsOfExperience,
        proficiencyDepth,
        lastUsedDate: lastUsedDate || undefined,
        confidenceLevel,
        tags: tags.length > 0 ? tags : undefined,
        visibility: isPublic ? 'public' : 'private',
      };
      onSubmit(data);
    } else {
      const data: UpdateUserSkillRequest = {
        yearsOfExperience,
        proficiencyDepth,
        lastUsedDate: lastUsedDate || undefined,
        confidenceLevel,
        tags,
        visibility: isPublic ? 'public' : 'private',
      };
      onSubmit(data);
    }
  };

  const proficiencyMarks = [1, 2, 3, 4, 5].map((v) => ({
    value: v,
    label: PROFICIENCY_LABELS[v],
  }));

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{mode === 'add' ? t('form.addTitle') : t('form.editTitle')}</DialogTitle>
      <DialogContent>
        <Box display="flex" flexDirection="column" gap={2} mt={1}>
          {mode === 'add' ? (
            <SkillSelector
              value={selectedSkill}
              onChange={(s) => {
                setSelectedSkill(s);
                setSkillError(null);
              }}
              error={skillError}
            />
          ) : (
            <TextField
              label={t('form.skill')}
              value={skill?.skill?.name || ''}
              disabled
              fullWidth
            />
          )}

          <TextField
            label={t('form.yearsOfExperience')}
            type="number"
            value={yearsOfExperience}
            onChange={(e) => setYearsOfExperience(Math.max(0, Math.min(70, parseInt(e.target.value) || 0)))}
            inputProps={{ min: 0, max: 70 }}
            fullWidth
          />

          <Box>
            <Typography gutterBottom>
              {t('form.proficiency')}: {PROFICIENCY_LABELS[proficiencyDepth]}
            </Typography>
            <Slider
              value={proficiencyDepth}
              onChange={(_, v) => setProficiencyDepth(v as number)}
              min={1}
              max={5}
              step={1}
              marks={proficiencyMarks}
              valueLabelDisplay="auto"
            />
          </Box>

          <TextField
            label={t('form.lastUsed')}
            type="date"
            value={lastUsedDate}
            onChange={(e) => setLastUsedDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ max: new Date().toISOString().split('T')[0] }}
            fullWidth
          />

          <FormControl fullWidth>
            <InputLabel>{t('form.confidenceLevel')}</InputLabel>
            <Select
              value={confidenceLevel}
              label={t('form.confidenceLevel')}
              onChange={(e) => setConfidenceLevel(e.target.value)}
            >
              <MenuItem value="LOW">{t('form.confidenceLow')}</MenuItem>
              <MenuItem value="MEDIUM">{t('form.confidenceMedium')}</MenuItem>
              <MenuItem value="HIGH">{t('form.confidenceHigh')}</MenuItem>
            </Select>
          </FormControl>

          <Autocomplete
            multiple
            freeSolo
            value={tags}
            onChange={(_, newValue) => setTags(newValue.slice(0, 20))}
            options={[]}
            renderTags={(value, getTagProps) =>
              value.map((option, index) => (
                <Chip
                  label={option}
                  size="small"
                  {...getTagProps({ index })}
                  key={option}
                />
              ))
            }
            renderInput={(params) => (
              <TextField
                {...params}
                label={t('form.tags')}
                placeholder={t('form.tagsPlaceholder')}
                helperText={t('form.tagsCount', { count: tags.length })}
              />
            )}
          />

          <FormControlLabel
            control={
              <Switch checked={isPublic} onChange={(e) => setIsPublic(e.target.checked)} />
            }
            label={isPublic ? t('form.visibilityPublic') : t('form.visibilityPrivate')}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('common:buttons.cancel')}</Button>
        <Button onClick={handleSubmit} variant="contained" disabled={isLoading}>
          {isLoading ? t('common:status.saving') : mode === 'add' ? t('form.addTitle') : t('form.saveChanges')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
