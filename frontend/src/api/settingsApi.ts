import apiClient from './client';

export interface AvailableProvider {
  name: string;
  defaultModel: string;
}

export interface AiSettings {
  provider: string | null;
  chatModel: string | null;
  availableProviders: AvailableProvider[];
}

export interface AiSettingsRequest {
  provider: string | null;
  chatModel: string | null;
}

export const settingsApi = {
  getAiSettings: async (): Promise<AiSettings> => {
    const response = await apiClient.get<AiSettings>('/settings/ai');
    return response.data;
  },
  updateAiSettings: async (data: AiSettingsRequest): Promise<AiSettings> => {
    const response = await apiClient.put<AiSettings>('/settings/ai', data);
    return response.data;
  },
};
