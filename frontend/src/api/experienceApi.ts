import apiClient from './client';
import type {
  ProjectResponse,
  CreateProjectRequest,
  UpdateProjectRequest,
} from '../types/experienceProject';
import type {
  StoryResponse,
  CreateStoryRequest,
  UpdateStoryRequest,
} from '../types/story';

// Project API
export const projectApi = {
  getProjectsByJob: async (jobId: number): Promise<ProjectResponse[]> => {
    const response = await apiClient.get<ProjectResponse[]>(
      `/v1/jobs/${jobId}/projects`
    );
    return response.data;
  },

  getProject: async (projectId: number): Promise<ProjectResponse> => {
    const response = await apiClient.get<ProjectResponse>(
      `/v1/projects/${projectId}`
    );
    return response.data;
  },

  createProject: async (
    jobId: number,
    data: CreateProjectRequest
  ): Promise<ProjectResponse> => {
    const response = await apiClient.post<ProjectResponse>(
      `/v1/jobs/${jobId}/projects`,
      data
    );
    return response.data;
  },

  updateProject: async (
    projectId: number,
    data: UpdateProjectRequest
  ): Promise<ProjectResponse> => {
    const response = await apiClient.put<ProjectResponse>(
      `/v1/projects/${projectId}`,
      data
    );
    return response.data;
  },

  deleteProject: async (projectId: number): Promise<void> => {
    await apiClient.delete(`/v1/projects/${projectId}`);
  },
};

// Story API
export const storyApi = {
  getStoriesByProject: async (projectId: number): Promise<StoryResponse[]> => {
    const response = await apiClient.get<StoryResponse[]>(
      `/v1/projects/${projectId}/stories`
    );
    return response.data;
  },

  getStory: async (storyId: number): Promise<StoryResponse> => {
    const response = await apiClient.get<StoryResponse>(
      `/v1/stories/${storyId}`
    );
    return response.data;
  },

  createStory: async (
    projectId: number,
    data: CreateStoryRequest
  ): Promise<StoryResponse> => {
    const response = await apiClient.post<StoryResponse>(
      `/v1/projects/${projectId}/stories`,
      data
    );
    return response.data;
  },

  updateStory: async (
    storyId: number,
    data: UpdateStoryRequest
  ): Promise<StoryResponse> => {
    const response = await apiClient.put<StoryResponse>(
      `/v1/stories/${storyId}`,
      data
    );
    return response.data;
  },

  deleteStory: async (storyId: number): Promise<void> => {
    await apiClient.delete(`/v1/stories/${storyId}`);
  },

  getPublicStoriesByProfile: async (
    profileId: number
  ): Promise<StoryResponse[]> => {
    const response = await apiClient.get<StoryResponse[]>(
      `/v1/profiles/${profileId}/stories/public`
    );
    return response.data;
  },
};
