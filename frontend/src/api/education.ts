import apiClient from './client';
import type {
  Education,
  CreateEducationRequest,
  UpdateEducationRequest,
} from '../types/profile';

export const educationApi = {
  // Get all education records for a profile
  getEducations: async (profileId: number): Promise<Education[]> => {
    const response = await apiClient.get<Education[]>(
      `/profiles/${profileId}/education`
    );
    return response.data;
  },

  // Get education by ID
  getEducationById: async (
    profileId: number,
    educationId: number
  ): Promise<Education> => {
    const response = await apiClient.get<Education>(
      `/profiles/${profileId}/education/${educationId}`
    );
    return response.data;
  },

  // Create education
  createEducation: async (
    profileId: number,
    data: CreateEducationRequest
  ): Promise<Education> => {
    const response = await apiClient.post<Education>(
      `/profiles/${profileId}/education`,
      data
    );
    return response.data;
  },

  // Update education
  updateEducation: async (
    profileId: number,
    educationId: number,
    data: UpdateEducationRequest
  ): Promise<Education> => {
    const response = await apiClient.put<Education>(
      `/profiles/${profileId}/education/${educationId}`,
      data
    );
    return response.data;
  },

  // Delete education
  deleteEducation: async (
    profileId: number,
    educationId: number
  ): Promise<void> => {
    await apiClient.delete(`/profiles/${profileId}/education/${educationId}`);
  },
};
