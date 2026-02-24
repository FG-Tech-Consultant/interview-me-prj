import apiClient from './client';
import type {
  JobExperience,
  CreateJobExperienceRequest,
  UpdateJobExperienceRequest,
} from '../types/profile';

export const jobExperienceApi = {
  // Get all job experiences for a profile
  getJobExperiences: async (profileId: number): Promise<JobExperience[]> => {
    const response = await apiClient.get<JobExperience[]>(
      `/profiles/${profileId}/job-experiences`
    );
    return response.data;
  },

  // Get job experience by ID
  getJobExperienceById: async (
    profileId: number,
    experienceId: number
  ): Promise<JobExperience> => {
    const response = await apiClient.get<JobExperience>(
      `/profiles/${profileId}/job-experiences/${experienceId}`
    );
    return response.data;
  },

  // Create job experience
  createJobExperience: async (
    profileId: number,
    data: CreateJobExperienceRequest
  ): Promise<JobExperience> => {
    const response = await apiClient.post<JobExperience>(
      `/profiles/${profileId}/job-experiences`,
      data
    );
    return response.data;
  },

  // Update job experience
  updateJobExperience: async (
    profileId: number,
    experienceId: number,
    data: UpdateJobExperienceRequest
  ): Promise<JobExperience> => {
    const response = await apiClient.put<JobExperience>(
      `/profiles/${profileId}/job-experiences/${experienceId}`,
      data
    );
    return response.data;
  },

  // Delete job experience
  deleteJobExperience: async (
    profileId: number,
    experienceId: number
  ): Promise<void> => {
    await apiClient.delete(`/profiles/${profileId}/job-experiences/${experienceId}`);
  },
};
