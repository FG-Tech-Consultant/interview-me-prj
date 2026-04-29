import apiClient from './client';
import type { CompanyProfile, CompanyRegistrationRequest, CompanyRegistrationResponse, CompanyUpdateRequest, CompanyDashboard } from '../types/company';

export const companyApi = {
  register: async (data: CompanyRegistrationRequest): Promise<CompanyRegistrationResponse> => {
    const response = await apiClient.post<CompanyRegistrationResponse>('/v1/company/register', data);
    return response.data;
  },

  getProfile: async (): Promise<CompanyProfile> => {
    const response = await apiClient.get<CompanyProfile>('/v1/company/profile');
    return response.data;
  },

  updateProfile: async (data: CompanyUpdateRequest): Promise<CompanyProfile> => {
    const response = await apiClient.put<CompanyProfile>('/v1/company/profile', data);
    return response.data;
  },

  getDashboard: async (): Promise<CompanyDashboard> => {
    const response = await apiClient.get<CompanyDashboard>('/v1/company/dashboard');
    return response.data;
  },
};
