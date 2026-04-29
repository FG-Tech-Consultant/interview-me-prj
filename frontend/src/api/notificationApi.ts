import apiClient from './client';
import type { NotificationItem } from '../types/notification';

export const notificationApi = {
  list: async (unreadOnly = false): Promise<NotificationItem[]> => {
    const response = await apiClient.get<NotificationItem[]>(
      `/v1/notifications${unreadOnly ? '?unreadOnly=true' : ''}`
    );
    return response.data;
  },

  countUnread: async (): Promise<number> => {
    const response = await apiClient.get<{ unread: number }>('/v1/notifications/count');
    return response.data.unread;
  },

  markAsRead: async (id: number): Promise<void> => {
    await apiClient.post(`/v1/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.post('/v1/notifications/read-all');
  },
};
