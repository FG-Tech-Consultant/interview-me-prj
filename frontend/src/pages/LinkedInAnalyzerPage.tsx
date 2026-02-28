import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Container,
  Box,
  Typography,
  Paper,
  CircularProgress,
  Alert,
  Divider,
} from '@mui/material';
import PdfUploader from '../components/linkedin/PdfUploader';
import ScoreGauge from '../components/linkedin/ScoreGauge';
import SectionScoreCard from '../components/linkedin/SectionScoreCard';
import AnalysisHistory from '../components/linkedin/AnalysisHistory';
import {
  useAnalysis,
  useAnalysisHistory,
  useUploadAndAnalyze,
  useGenerateSuggestions,
  useApplySuggestion,
} from '../hooks/useLinkedInAnalysis';
import { useCurrentProfile } from '../hooks/useProfile';

export function LinkedInAnalyzerPage() {
  const [activeAnalysisId, setActiveAnalysisId] = useState<number | null>(null);
  const [appliedSuggestions, setAppliedSuggestions] = useState<
    Record<string, Set<number>>
  >({});
  const { t } = useTranslation('linkedin');

  const { data: profile } = useCurrentProfile();
  const profileId = profile?.id ?? null;

  const { data: analysis } = useAnalysis(activeAnalysisId);
  const { data: history } = useAnalysisHistory(profileId);
  const uploadMutation = useUploadAndAnalyze();
  const generateMutation = useGenerateSuggestions();
  const applyMutation = useApplySuggestion();

  const handleUpload = useCallback(
    (file: File) => {
      if (!profileId) return;
      uploadMutation.mutate(
        { file, profileId },
        {
          onSuccess: (response) => {
            setActiveAnalysisId(response.analysisId);
            setAppliedSuggestions({});
          },
        }
      );
    },
    [profileId, uploadMutation]
  );

  const handleGenerateMore = useCallback(
    (sectionName: string, count: number) => {
      if (!activeAnalysisId) return;
      generateMutation.mutate({
        analysisId: activeAnalysisId,
        sectionName,
        count,
      });
    },
    [activeAnalysisId, generateMutation]
  );

  const handleApply = useCallback(
    (sectionName: string, suggestionIndex: number) => {
      if (!activeAnalysisId) return;
      applyMutation.mutate(
        { analysisId: activeAnalysisId, sectionName, suggestionIndex },
        {
          onSuccess: () => {
            setAppliedSuggestions((prev) => {
              const sectionSet = new Set(prev[sectionName] || []);
              sectionSet.add(suggestionIndex);
              return { ...prev, [sectionName]: sectionSet };
            });
          },
        }
      );
    },
    [activeAnalysisId, applyMutation]
  );

  const handleViewDetails = useCallback((id: number) => {
    setActiveAnalysisId(id);
    setAppliedSuggestions({});
  }, []);

  const handleNewAnalysis = useCallback(() => {
    setActiveAnalysisId(null);
    setAppliedSuggestions({});
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, []);

  const isProcessing =
    analysis?.status === 'PENDING' || analysis?.status === 'IN_PROGRESS';

  return (
    <Container maxWidth="md">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Typography variant="h4" gutterBottom>
          {t('title')}
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          {t('description')}
        </Typography>

        {/* Upload Section */}
        {!activeAnalysisId && (
          <PdfUploader
            onUpload={handleUpload}
            isLoading={uploadMutation.isPending}
            error={
              uploadMutation.error
                ? (uploadMutation.error as Error).message ||
                  t('uploadFailed')
                : null
            }
          />
        )}

        {/* Processing Indicator */}
        {isProcessing && (
          <Paper elevation={2} sx={{ p: 4, mt: 3, textAlign: 'center' }}>
            <CircularProgress size={48} sx={{ mb: 2 }} />
            <Typography variant="h6">{t('analyzing')}</Typography>
            <Typography variant="body2" color="text.secondary">
              {t('analyzingWait')}
            </Typography>
          </Paper>
        )}

        {/* Error Display */}
        {analysis?.status === 'FAILED' && (
          <Alert severity="error" sx={{ mt: 3 }}>
            <Typography variant="subtitle2">{t('analysisFailed')}</Typography>
            <Typography variant="body2">
              {analysis.errorMessage || t('analysisFailedDefault')}
            </Typography>
          </Alert>
        )}

        {/* Results */}
        {analysis?.status === 'COMPLETED' && (
          <Box sx={{ mt: 3 }}>
            {/* Overall Score */}
            <Paper elevation={2} sx={{ p: 3, mb: 3, textAlign: 'center' }}>
              <Typography variant="h6" sx={{ mb: 2 }}>
                {t('overallScore')}
              </Typography>
              <ScoreGauge score={analysis.overallScore ?? 0} size="large" />
              <Typography
                variant="body2"
                color="text.secondary"
                sx={{ mt: 1 }}
              >
                {t('outOf100')}
              </Typography>
              {analysis.pdfFilename && (
                <Typography variant="caption" color="text.secondary" display="block" sx={{ mt: 1 }}>
                  {t('analyzed', { filename: analysis.pdfFilename })}
                </Typography>
              )}
            </Paper>

            {/* Section Scores */}
            <Typography variant="h6" sx={{ mb: 2 }}>
              {t('sectionBreakdown')}
            </Typography>
            {analysis.sections.map((section) => (
              <SectionScoreCard
                key={section.id}
                section={section}
                analysisId={analysis.id}
                coinCost={6}
                onGenerateMore={handleGenerateMore}
                onApply={handleApply}
                isGenerating={
                  generateMutation.isPending &&
                  generateMutation.variables?.sectionName === section.sectionName
                }
                isApplying={applyMutation.isPending}
                appliedIndices={
                  appliedSuggestions[section.sectionName] || new Set()
                }
              />
            ))}
          </Box>
        )}

        {/* History Section */}
        {history && history.content.length > 0 && (
          <Box sx={{ mt: 4 }}>
            <Divider sx={{ mb: 3 }} />
            <AnalysisHistory
              analyses={history.content}
              onViewDetails={handleViewDetails}
              onNewAnalysis={handleNewAnalysis}
            />
          </Box>
        )}
      </Box>
    </Container>
  );
}
