import apiClient from './client';
import type {
  ExportHistory,
  ExportHistoryPage,
  ExportResumeRequest,
  ExportStatus,
  ExportTemplate,
} from '../types/export';

export const exportsApi = {
  createResumeExport: async (request: ExportResumeRequest): Promise<ExportHistory> => {
    const response = await apiClient.post<ExportHistory>('/exports/resume', request);
    return response.data;
  },

  getExportStatus: async (exportId: number): Promise<ExportStatus> => {
    const response = await apiClient.get<ExportStatus>(`/exports/${exportId}/status`);
    return response.data;
  },

  getExportHistory: async (
    page = 0,
    size = 20,
    type?: string,
    status?: string
  ): Promise<ExportHistoryPage> => {
    const params: Record<string, string | number> = { page, size };
    if (type) params.type = type;
    if (status) params.status = status;
    const response = await apiClient.get<ExportHistoryPage>('/exports', { params });
    return response.data;
  },

  getExportTemplates: async (): Promise<ExportTemplate[]> => {
    const response = await apiClient.get<ExportTemplate[]>('/exports/templates');
    return response.data;
  },

  downloadExport: async (exportId: number): Promise<Blob> => {
    const response = await apiClient.get(`/exports/${exportId}/download`, {
      responseType: 'blob',
    });
    return response.data;
  },
};
