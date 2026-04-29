import apiClient from './client';
import type {
  JobPosting,
  CreateJobPostingRequest,
  UpdateJobPostingRequest,
  AiJobDescriptionRequest,
  AiJobDescriptionResponse,
} from '../types/jobPosting';

export const jobPostingApi = {
  list: async (): Promise<JobPosting[]> => {
    const response = await apiClient.get<JobPosting[]>('/v1/jobs');
    return response.data;
  },

  getById: async (id: number): Promise<JobPosting> => {
    const response = await apiClient.get<JobPosting>(`/v1/jobs/${id}`);
    return response.data;
  },

  create: async (data: CreateJobPostingRequest): Promise<JobPosting> => {
    const response = await apiClient.post<JobPosting>('/v1/jobs', data);
    return response.data;
  },

  update: async (id: number, data: UpdateJobPostingRequest): Promise<JobPosting> => {
    const response = await apiClient.put<JobPosting>(`/v1/jobs/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await apiClient.delete(`/v1/jobs/${id}`);
  },

  generateDescription: async (data: AiJobDescriptionRequest): Promise<AiJobDescriptionResponse> => {
    const response = await apiClient.post<AiJobDescriptionResponse>('/v1/jobs/ai/generate-description', data);
    return response.data;
  },
};
