import axios from 'axios';
import apiClient from './client';
import type { ChatResponse, ChatAnalyticsResponse } from '../types/chat';

// Separate axios instance for public chat endpoints (no auth token)
const publicChatClient = axios.create({
  baseURL: `${import.meta.env.BASE_URL}api`,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const chatApi = {
  sendMessage: async (
    slug: string,
    message: string,
    sessionToken: string | null,
    visitorToken?: string | null
  ): Promise<ChatResponse> => {
    const response = await publicChatClient.post<ChatResponse>(
      `/public/chat/${slug}/messages`,
      { message, sessionToken, visitorToken }
    );
    return response.data;
  },

  getAnalytics: async (): Promise<ChatAnalyticsResponse> => {
    const response = await apiClient.get<ChatAnalyticsResponse>('/chat/analytics');
    return response.data;
  },
};
