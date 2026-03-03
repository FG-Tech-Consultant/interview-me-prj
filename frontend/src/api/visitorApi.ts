import axios from 'axios';
import apiClient from './client';
import type { VisitorFormData } from '../components/chat/VisitorIdentificationDialog';

// Public client for visitor identification (no auth)
const publicClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

export interface VisitorIdentifyResponse {
  visitorId: number;
  visitorToken: string;
}

export interface VisitorResponse {
  id: number;
  name: string;
  company: string;
  jobRole: string;
  linkedinUrl: string | null;
  contactEmail: string | null;
  contactWhatsapp: string | null;
  createdAt: string;
  sessionCount: number;
  totalMessages: number;
  isRevealed: boolean;
}

export interface VisitorSessionResponse {
  id: number;
  visitorId: number;
  visitorName: string;
  visitorCompany: string;
  startedAt: string;
  endedAt: string | null;
  messageCount: number;
}

export interface VisitorChatLogResponse {
  id: number;
  role: string;
  content: string;
  tokensUsed: number | null;
  llmRequest: string | null;
  createdAt: string;
}

export interface VisitorStatsResponse {
  profileViews: number;
  totalVisitors: number;
  chatVisitors: number;
}

export interface GlobalStatsResponse {
  totalAccounts: number;
  totalProfileViews: number;
  totalVisitors: number;
  totalInterviews: number;
}

export interface AccountResponse {
  id: number;
  email: string;
  fullName: string;
  role: string;
  slug: string | null;
  publicProfileUrl: string | null;
  createdAt: string;
}

export const visitorApi = {
  // Public: visitor identifies themselves
  identify: async (slug: string, data: VisitorFormData & { locale?: string }): Promise<VisitorIdentifyResponse> => {
    const response = await publicClient.post<VisitorIdentifyResponse>(
      `/public/visitors/${slug}/identify`,
      data
    );
    return response.data;
  },

  // Authenticated: get my visitors
  getMyVisitors: async (page = 0, size = 20): Promise<{ content: VisitorResponse[]; totalElements: number }> => {
    const response = await apiClient.get('/v1/visitors', { params: { page, size } });
    return response.data;
  },

  // Authenticated: get visitor stats
  getVisitorStats: async (): Promise<VisitorStatsResponse> => {
    const response = await apiClient.get<VisitorStatsResponse>('/v1/visitors/stats');
    return response.data;
  },

  // Authenticated: get visitor sessions
  getVisitorSessions: async (visitorId: number): Promise<VisitorSessionResponse[]> => {
    const response = await apiClient.get<VisitorSessionResponse[]>(`/v1/visitors/${visitorId}/sessions`);
    return response.data;
  },

  // Authenticated: get session messages
  getSessionMessages: async (sessionId: number): Promise<VisitorChatLogResponse[]> => {
    const response = await apiClient.get<VisitorChatLogResponse[]>(`/v1/visitors/sessions/${sessionId}/messages`);
    return response.data;
  },

  // Authenticated: reveal visitor contact (costs credits)
  revealContact: async (visitorId: number): Promise<VisitorResponse> => {
    const response = await apiClient.post<VisitorResponse>(`/v1/visitors/${visitorId}/reveal`);
    return response.data;
  },

  // Admin: get global stats
  adminGetGlobalStats: async (): Promise<GlobalStatsResponse> => {
    const response = await apiClient.get<GlobalStatsResponse>('/admin/global-stats');
    return response.data;
  },

  // Admin: get accounts list
  adminGetAccounts: async (): Promise<AccountResponse[]> => {
    const response = await apiClient.get<AccountResponse[]>('/admin/accounts');
    return response.data;
  },

  // Admin: get all visitors
  adminGetVisitors: async (page = 0, size = 20): Promise<{ content: VisitorResponse[]; totalElements: number }> => {
    const response = await apiClient.get('/admin/visitors', { params: { page, size } });
    return response.data;
  },

  // Admin: get visitor sessions
  adminGetVisitorSessions: async (visitorId: number): Promise<VisitorSessionResponse[]> => {
    const response = await apiClient.get<VisitorSessionResponse[]>(`/admin/visitors/${visitorId}/sessions`);
    return response.data;
  },

  // Admin: get session messages
  adminGetSessionMessages: async (sessionId: number): Promise<VisitorChatLogResponse[]> => {
    const response = await apiClient.get<VisitorChatLogResponse[]>(`/admin/sessions/${sessionId}/messages`);
    return response.data;
  },

  // Admin: send test email
  adminSendTestEmail: async (to: string): Promise<{ message: string }> => {
    const response = await apiClient.post<{ message: string }>('/admin/test-email', { to });
    return response.data;
  },
};
