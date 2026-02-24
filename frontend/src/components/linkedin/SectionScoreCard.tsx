import {
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  CircularProgress,
} from '@mui/material';
import ScoreGauge from './ScoreGauge';
import SuggestionItem from './SuggestionItem';
import type { SectionScoreResponse } from '../../types/linkedinAnalysis';

interface SectionScoreCardProps {
  section: SectionScoreResponse;
  analysisId?: number;
  coinCost: number;
  onGenerateMore: (sectionName: string, count: number) => void;
  onApply: (sectionName: string, index: number) => void;
  isGenerating: boolean;
  isApplying: boolean;
  appliedIndices: Set<number>;
}

const SECTION_LABELS: Record<string, string> = {
  HEADLINE: 'Headline',
  ABOUT: 'About',
  EXPERIENCE: 'Experience',
  EDUCATION: 'Education',
  SKILLS: 'Skills',
  RECOMMENDATIONS: 'Recommendations',
  OTHER: 'Other',
};

export default function SectionScoreCard({
  section,
  coinCost,
  onGenerateMore,
  onApply,
  isGenerating,
  isApplying,
  appliedIndices,
}: SectionScoreCardProps) {
  const label = SECTION_LABELS[section.sectionName] || section.sectionName;

  return (
    <Card variant="outlined" sx={{ mb: 2 }}>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2 }}>
          <ScoreGauge score={section.sectionScore} size="small" />
          <Box>
            <Typography variant="h6">{label}</Typography>
            <Typography variant="body2" color="text.secondary">
              {section.sectionScore}/100
            </Typography>
          </Box>
        </Box>

        <Typography variant="body2" sx={{ mb: 2, fontStyle: 'italic' }}>
          {section.qualityExplanation}
        </Typography>

        {section.suggestions.length > 0 && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              Suggestions
            </Typography>
            {section.suggestions.map((suggestion, index) => (
              <SuggestionItem
                key={index}
                text={suggestion}
                index={index}
                isFree={index === 0}
                canApply={section.canApplyToProfile}
                isApplied={appliedIndices.has(index)}
                onApply={() => onApply(section.sectionName, index)}
                isApplying={isApplying}
              />
            ))}
          </Box>
        )}

        <Button
          variant="text"
          size="small"
          onClick={() => onGenerateMore(section.sectionName, 3)}
          disabled={isGenerating}
          startIcon={isGenerating ? <CircularProgress size={16} /> : undefined}
        >
          {isGenerating
            ? 'Generating...'
            : `Get more suggestions (${coinCost} coins)`}
        </Button>
      </CardContent>
    </Card>
  );
}
