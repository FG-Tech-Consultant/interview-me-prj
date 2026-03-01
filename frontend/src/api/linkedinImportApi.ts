import apiClient from './client';

export interface ImportPreviewResponse {
  previewId: string;
  counts: Record<string, number>;
  warnings: string[];
  profilePreview: {
    firstName: string;
    lastName: string;
    headline: string;
    summary: string;
    location: string;
  } | null;
}

export interface ImportResultResponse {
  importId: number;
  status: string;
  importedCounts: Record<string, number>;
  errors: string[];
}

export interface ImportHistoryItem {
  id: number;
  filename: string;
  strategy: string;
  status: string;
  itemCounts: Record<string, number>;
  importedAt: string;
}

export const linkedinImportApi = {
  uploadZip: async (file: File): Promise<ImportPreviewResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<ImportPreviewResponse>(
      '/v1/linkedin/import/upload',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return response.data;
  },

  confirmImport: async (
    previewId: string,
    importStrategy: string
  ): Promise<ImportResultResponse> => {
    const response = await apiClient.post<ImportResultResponse>(
      '/v1/linkedin/import/confirm',
      { previewId, importStrategy }
    );
    return response.data;
  },

  getHistory: async (): Promise<ImportHistoryItem[]> => {
    const response = await apiClient.get<ImportHistoryItem[]>(
      '/v1/linkedin/import/history'
    );
    return response.data;
  },
};
