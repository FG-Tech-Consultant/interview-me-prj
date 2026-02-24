import apiClient from './client';
import type {
  Profile,
  CreateProfileRequest,
  UpdateProfileRequest,
} from '../types/profile';

export const profileApi = {
  // Get current user's profile
  getCurrentProfile: async (): Promise<Profile> => {
    const response = await apiClient.get<Profile>('/profiles/me');
    return response.data;
  },

  // Get profile by ID
  getProfileById: async (profileId: number): Promise<Profile> => {
    const response = await apiClient.get<Profile>(`/profiles/${profileId}`);
    return response.data;
  },

  // Create profile
  createProfile: async (data: CreateProfileRequest): Promise<Profile> => {
    const response = await apiClient.post<Profile>('/profiles', data);
    return response.data;
  },

  // Update profile
  updateProfile: async (
    profileId: number,
    data: UpdateProfileRequest
  ): Promise<Profile> => {
    const response = await apiClient.put<Profile>(`/profiles/${profileId}`, data);
    return response.data;
  },

  // Delete profile
  deleteProfile: async (profileId: number): Promise<void> => {
    await apiClient.delete(`/profiles/${profileId}`);
  },

  // Check if profile exists
  checkProfileExists: async (): Promise<boolean> => {
    const response = await apiClient.get<boolean>('/profiles/exists');
    return response.data;
  },
};
