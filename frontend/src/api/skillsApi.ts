import apiClient from './client';
import type {
  SkillDto,
  UserSkillDto,
  UserSkillsGrouped,
  AddUserSkillRequest,
  UpdateUserSkillRequest,
  CreateSkillRequest,
  UpdateSkillRequest,
} from '../types/skill';

export const skillsApi = {
  // Catalog - Search
  searchCatalog: async (query: string): Promise<SkillDto[]> => {
    const response = await apiClient.get<SkillDto[]>('/v1/skills/catalog/search', {
      params: { q: query },
    });
    return response.data;
  },

  // Catalog - Admin: List all
  getAllCatalogSkills: async (): Promise<SkillDto[]> => {
    const response = await apiClient.get<SkillDto[]>('/v1/skills/catalog');
    return response.data;
  },

  // Catalog - Admin: Create
  createCatalogSkill: async (data: CreateSkillRequest): Promise<SkillDto> => {
    const response = await apiClient.post<SkillDto>('/v1/skills/catalog', data);
    return response.data;
  },

  // Catalog - Admin: Update
  updateCatalogSkill: async (id: number, data: UpdateSkillRequest): Promise<SkillDto> => {
    const response = await apiClient.put<SkillDto>(`/v1/skills/catalog/${id}`, data);
    return response.data;
  },

  // Catalog - Admin: Deactivate
  deactivateCatalogSkill: async (id: number): Promise<SkillDto> => {
    const response = await apiClient.post<SkillDto>(`/v1/skills/catalog/${id}/deactivate`);
    return response.data;
  },

  // Catalog - Admin: Reactivate
  reactivateCatalogSkill: async (id: number): Promise<SkillDto> => {
    const response = await apiClient.post<SkillDto>(`/v1/skills/catalog/${id}/reactivate`);
    return response.data;
  },

  // User Skills - Get grouped by category
  getUserSkills: async (profileId: number): Promise<UserSkillsGrouped> => {
    const response = await apiClient.get<UserSkillsGrouped>(`/v1/skills/user/${profileId}`);
    return response.data;
  },

  // User Skills - Add
  addUserSkill: async (profileId: number, data: AddUserSkillRequest): Promise<UserSkillDto> => {
    const response = await apiClient.post<UserSkillDto>(`/v1/skills/user/${profileId}`, data);
    return response.data;
  },

  // User Skills - Get single
  getUserSkillById: async (id: number): Promise<UserSkillDto> => {
    const response = await apiClient.get<UserSkillDto>(`/v1/skills/user/detail/${id}`);
    return response.data;
  },

  // User Skills - Update
  updateUserSkill: async (id: number, data: UpdateUserSkillRequest): Promise<UserSkillDto> => {
    const response = await apiClient.put<UserSkillDto>(`/v1/skills/user/detail/${id}`, data);
    return response.data;
  },

  // User Skills - Delete (soft)
  deleteUserSkill: async (id: number): Promise<void> => {
    await apiClient.delete(`/v1/skills/user/detail/${id}`);
  },

  // User Skills - Get public only
  getPublicUserSkills: async (profileId: number): Promise<UserSkillDto[]> => {
    const response = await apiClient.get<UserSkillDto[]>(`/v1/skills/user/${profileId}/public`);
    return response.data;
  },
};
