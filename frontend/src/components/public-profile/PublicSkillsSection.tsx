import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Typography,
  Chip,
  Rating,
  Button,
  Stack,
} from '@mui/material';
import type { PublicSkillResponse } from '../../types/publicProfile';

interface PublicSkillsSectionProps {
  skills: PublicSkillResponse[];
}

const MAX_VISIBLE_SKILLS = 15;

export const PublicSkillsSection: React.FC<PublicSkillsSectionProps> = ({ skills }) => {
  const [showAll, setShowAll] = useState(false);
  const { t } = useTranslation('public-profile');

  // Group skills by category
  const grouped = skills.reduce<Record<string, PublicSkillResponse[]>>((acc, skill) => {
    const cat = skill.category || 'Other';
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(skill);
    return acc;
  }, {});

  // Sort categories by count (most skills first)
  const sortedCategories = Object.entries(grouped).sort((a, b) => b[1].length - a[1].length);

  // Flatten for show-more logic
  const totalSkills = skills.length;
  const shouldTruncate = totalSkills > MAX_VISIBLE_SKILLS && !showAll;

  let visibleCount = 0;

  return (
    <Box>
      {sortedCategories.map(([category, catSkills]) => {
        if (shouldTruncate && visibleCount >= MAX_VISIBLE_SKILLS) return null;

        const remainingSlots = shouldTruncate ? MAX_VISIBLE_SKILLS - visibleCount : catSkills.length;
        const visibleSkills = catSkills.slice(0, remainingSlots);
        visibleCount += visibleSkills.length;

        return (
          <Box key={category} sx={{ mb: 2 }}>
            <Typography variant="subtitle2" color="text.secondary" sx={{ mb: 1 }}>
              {category}
            </Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ gap: 1 }}>
              {visibleSkills.map((skill) => (
                <Chip
                  key={skill.skillName}
                  label={
                    <Stack direction="row" alignItems="center" spacing={0.5}>
                      <span>{skill.skillName}</span>
                      <Rating
                        value={skill.proficiencyDepth}
                        max={5}
                        size="small"
                        readOnly
                        sx={{ ml: 0.5 }}
                      />
                    </Stack>
                  }
                  variant="outlined"
                  size="medium"
                />
              ))}
            </Stack>
          </Box>
        );
      })}

      {totalSkills > MAX_VISIBLE_SKILLS && (
        <Button
          size="small"
          onClick={() => setShowAll(!showAll)}
          sx={{ mt: 1 }}
        >
          {showAll ? t('showLess') : t('showAll', { count: totalSkills })}
        </Button>
      )}
    </Box>
  );
};
