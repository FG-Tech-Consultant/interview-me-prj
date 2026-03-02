export type AnalysisSourceType = 'PDF' | 'ZIP' | 'PROFILE';

export interface StartAnalysisResponse {
  analysisId: number;
  status: string;
  message: string;
}

export interface LinkedInAnalysisResponse {
  id: number;
  profileId: number;
  status: string;
  sourceType: AnalysisSourceType;
  overallScore: number | null;
  errorMessage: string | null;
  pdfFilename: string | null;
  analyzedAt: string | null;
  sections: SectionScoreResponse[];
  createdAt: string;
}

export interface LinkedInAnalysisSummary {
  id: number;
  status: string;
  sourceType: AnalysisSourceType;
  overallScore: number | null;
  pdfFilename: string | null;
  analyzedAt: string | null;
  createdAt: string;
}

export interface SectionScoreResponse {
  id: number;
  sectionName: string;
  sectionScore: number;
  qualityExplanation: string;
  suggestions: string[];
  canApplyToProfile: boolean;
}

export interface GenerateSuggestionsRequest {
  count: number;
}

export interface ApplySuggestionRequest {
  suggestionIndex: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
