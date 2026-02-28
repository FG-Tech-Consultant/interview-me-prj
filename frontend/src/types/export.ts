export interface ExportTemplate {
  id: number;
  name: string;
  type: string;
  description: string | null;
}

export interface ExportHistory {
  id: number;
  template: ExportTemplate;
  type: string;
  status: ExportStatusType;
  parameters: Record<string, string>;
  coinsSpent: number;
  errorMessage: string | null;
  retryCount: number;
  createdAt: string;
  completedAt: string | null;
}

export interface ExportStatus {
  id: number;
  status: ExportStatusType;
  errorMessage: string | null;
  retryCount: number;
  completedAt: string | null;
}

export interface ExportHistoryPage {
  content: ExportHistory[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ExportResumeRequest {
  templateId: number;
  targetRole: string;
  location?: string;
  seniority: string;
  language?: string;
}

export interface ExportCoverLetterRequest {
  templateId: number;
  targetCompany: string;
  targetRole: string;
  jobDescription?: string;
  market: string;
}

export type ExportStatusType = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

export type ExportTypeValue = 'RESUME' | 'COVER_LETTER' | 'BACKGROUND_DECK';

export type SeniorityLevel = 'Junior' | 'Mid' | 'Senior' | 'Lead' | 'Principal' | 'Director';

export const SENIORITY_LEVELS: SeniorityLevel[] = [
  'Junior', 'Mid', 'Senior', 'Lead', 'Principal', 'Director'
];

export const LANGUAGES = [
  { value: 'en', label: 'English' },
  { value: 'pt', label: 'Portuguese' },
  { value: 'es', label: 'Spanish' },
];

export const MARKETS = [
  { value: 'US', label: 'United States' },
  { value: 'EU', label: 'European Union' },
  { value: 'Canada', label: 'Canada' },
];
