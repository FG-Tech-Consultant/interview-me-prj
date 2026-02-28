export interface CreateDraftRequest {
  originalMessage: string;
  tone?: string;
}

export interface DraftResponse {
  id: number;
  originalMessage: string;
  category: DraftCategory;
  suggestedReply: string;
  tone: string;
  status: DraftStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DraftPageResponse {
  content: DraftResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UpdateDraftStatusRequest {
  status: 'SENT' | 'ARCHIVED';
}

export type DraftCategory = 'RECRUITER' | 'AGENCY' | 'FOUNDER' | 'SPAM' | 'OTHER';
export type DraftStatus = 'DRAFT' | 'SENT' | 'ARCHIVED';

export const TONE_OPTIONS = [
  { value: 'professional', label: 'Professional' },
  { value: 'friendly', label: 'Friendly' },
  { value: 'enthusiastic', label: 'Enthusiastic' },
  { value: 'concise', label: 'Concise' },
  { value: 'formal', label: 'Formal' },
] as const;

export const CATEGORY_COLORS: Record<DraftCategory, string> = {
  RECRUITER: '#1976d2',
  AGENCY: '#7b1fa2',
  FOUNDER: '#2e7d32',
  SPAM: '#d32f2f',
  OTHER: '#757575',
};

export const STATUS_COLORS: Record<DraftStatus, 'default' | 'success' | 'info'> = {
  DRAFT: 'default',
  SENT: 'success',
  ARCHIVED: 'info',
};
