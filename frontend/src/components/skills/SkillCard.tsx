import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  Box,
  Chip,
  IconButton,
  Rating,
  Tooltip,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import type { UserSkillDto } from '../../types/skill';
import { PROFICIENCY_LABELS } from '../../types/skill';

interface SkillCardProps {
  skill: UserSkillDto;
  onEdit: (skill: UserSkillDto) => void;
  onDelete: (skill: UserSkillDto) => void;
}

const getProficiencyColor = (depth: number): string => {
  if (depth <= 2) return '#ed6c02';
  if (depth === 3) return '#1976d2';
  return '#2e7d32';
};

const formatLastUsed = (date: string | null): string => {
  if (!date) return 'N/A';
  const d = new Date(date);
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short' });
};

export const SkillCard: React.FC<SkillCardProps> = ({ skill, onEdit, onDelete }) => {
  return (
    <Card variant="outlined" sx={{ mb: 1 }}>
      <CardContent sx={{ py: 1.5, px: 2, '&:last-child': { pb: 1.5 } }}>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Box flex={1}>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="subtitle1" fontWeight="bold">
                {skill.skill.name}
              </Typography>
              <Tooltip title={skill.visibility === 'public' ? 'Public' : 'Private'}>
                {skill.visibility === 'public' ? (
                  <VisibilityIcon fontSize="small" color="success" />
                ) : (
                  <VisibilityOffIcon fontSize="small" color="disabled" />
                )}
              </Tooltip>
            </Box>
            <Box display="flex" alignItems="center" gap={2} mt={0.5}>
              <Tooltip title={PROFICIENCY_LABELS[skill.proficiencyDepth] || ''}>
                <Box>
                  <Rating
                    value={skill.proficiencyDepth}
                    max={5}
                    readOnly
                    size="small"
                    sx={{
                      '& .MuiRating-iconFilled': {
                        color: getProficiencyColor(skill.proficiencyDepth),
                      },
                    }}
                  />
                </Box>
              </Tooltip>
              <Typography variant="body2" color="text.secondary">
                {skill.yearsOfExperience}y exp
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Last used: {formatLastUsed(skill.lastUsedDate)}
              </Typography>
              <Chip
                label={skill.confidenceLevel}
                size="small"
                variant="outlined"
                color={
                  skill.confidenceLevel === 'HIGH'
                    ? 'success'
                    : skill.confidenceLevel === 'LOW'
                    ? 'warning'
                    : 'default'
                }
              />
            </Box>
            {skill.tags && skill.tags.length > 0 && (
              <Box display="flex" gap={0.5} mt={0.5} flexWrap="wrap">
                {skill.tags.map((tag) => (
                  <Chip key={tag} label={tag} size="small" sx={{ fontSize: '0.7rem' }} />
                ))}
              </Box>
            )}
          </Box>
          <Box>
            <IconButton size="small" onClick={() => onEdit(skill)}>
              <EditIcon fontSize="small" />
            </IconButton>
            <IconButton size="small" onClick={() => onDelete(skill)} color="error">
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
};
