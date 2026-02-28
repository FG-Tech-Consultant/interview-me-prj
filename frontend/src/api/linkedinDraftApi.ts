import apiClient from './client';
import type {
  CreateDraftRequest,
  DraftResponse,
  DraftPageResponse,
  UpdateDraftStatusRequest,
} from '../types/linkedinDraft';

export const linkedinDraftApi = {
  createDraft: async (request: CreateDraftRequest): Promise<DraftResponse> => {
    const response = await apiClient.post<DraftResponse>('/linkedin/drafts', request);
    return response.data;
  },

  listDrafts: async (page = 0, size = 20): Promise<DraftPageResponse> => {
    const response = await apiClient.get<DraftPageResponse>('/linkedin/drafts', {
      params: { page, size },
    });
    return response.data;
  },

  getDraft: async (id: number): Promise<DraftResponse> => {
    const response = await apiClient.get<DraftResponse>(`/linkedin/drafts/${id}`);
    return response.data;
  },

  deleteDraft: async (id: number): Promise<void> => {
    await apiClient.delete(`/linkedin/drafts/${id}`);
  },

  updateStatus: async (id: number, request: UpdateDraftStatusRequest): Promise<DraftResponse> => {
    const response = await apiClient.put<DraftResponse>(`/linkedin/drafts/${id}/status`, request);
    return response.data;
  },
};
