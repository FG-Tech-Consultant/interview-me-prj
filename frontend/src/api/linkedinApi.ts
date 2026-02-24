import apiClient from './client';
import type {
  StartAnalysisResponse,
  LinkedInAnalysisResponse,
  LinkedInAnalysisSummary,
  SectionScoreResponse,
  PageResponse,
} from '../types/linkedinAnalysis';

export const linkedinApi = {
  uploadAndAnalyze: async (
    file: File,
    profileId: number
  ): Promise<StartAnalysisResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('profileId', String(profileId));

    const response = await apiClient.post<StartAnalysisResponse>(
      '/v1/linkedin/analyze',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    return response.data;
  },

  getAnalysis: async (id: number): Promise<LinkedInAnalysisResponse> => {
    const response = await apiClient.get<LinkedInAnalysisResponse>(
      `/v1/linkedin/analyses/${id}`
    );
    return response.data;
  },

  getAnalysisHistory: async (
    profileId: number,
    page = 0,
    size = 10
  ): Promise<PageResponse<LinkedInAnalysisSummary>> => {
    const response = await apiClient.get<PageResponse<LinkedInAnalysisSummary>>(
      '/v1/linkedin/analyses',
      { params: { profileId, page, size } }
    );
    return response.data;
  },

  generateSuggestions: async (
    analysisId: number,
    sectionName: string,
    count = 3
  ): Promise<SectionScoreResponse> => {
    const response = await apiClient.post<SectionScoreResponse>(
      `/v1/linkedin/analyses/${analysisId}/sections/${sectionName}/suggestions`,
      { count }
    );
    return response.data;
  },

  applySuggestion: async (
    analysisId: number,
    sectionName: string,
    suggestionIndex: number
  ): Promise<void> => {
    await apiClient.post(
      `/v1/linkedin/analyses/${analysisId}/sections/${sectionName}/apply`,
      { suggestionIndex }
    );
  },
};
