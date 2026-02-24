import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { linkedinApi } from '../api/linkedinApi';

const LINKEDIN_KEYS = {
  all: ['linkedin'] as const,
  analysis: (id: number) => [...LINKEDIN_KEYS.all, 'analysis', id] as const,
  history: (profileId: number, page: number) =>
    [...LINKEDIN_KEYS.all, 'history', profileId, page] as const,
};

export const useAnalysis = (analysisId: number | null) => {
  return useQuery({
    queryKey: LINKEDIN_KEYS.analysis(analysisId ?? 0),
    queryFn: () => linkedinApi.getAnalysis(analysisId!),
    enabled: analysisId != null && analysisId > 0,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'PENDING' || status === 'IN_PROGRESS') {
        return 3000; // Poll every 3 seconds
      }
      return false; // Stop polling
    },
  });
};

export const useAnalysisHistory = (profileId: number | null, page = 0) => {
  return useQuery({
    queryKey: LINKEDIN_KEYS.history(profileId ?? 0, page),
    queryFn: () => linkedinApi.getAnalysisHistory(profileId!, page),
    enabled: profileId != null && profileId > 0,
    staleTime: 30 * 1000,
  });
};

export const useUploadAndAnalyze = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      file,
      profileId,
    }: {
      file: File;
      profileId: number;
    }) => linkedinApi.uploadAndAnalyze(file, profileId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LINKEDIN_KEYS.all });
    },
  });
};

export const useGenerateSuggestions = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      analysisId,
      sectionName,
      count,
    }: {
      analysisId: number;
      sectionName: string;
      count?: number;
    }) => linkedinApi.generateSuggestions(analysisId, sectionName, count),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LINKEDIN_KEYS.all });
    },
  });
};

export const useApplySuggestion = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      analysisId,
      sectionName,
      suggestionIndex,
    }: {
      analysisId: number;
      sectionName: string;
      suggestionIndex: number;
    }) => linkedinApi.applySuggestion(analysisId, sectionName, suggestionIndex),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LINKEDIN_KEYS.all });
    },
  });
};
