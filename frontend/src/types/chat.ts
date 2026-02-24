export interface ChatRequest {
  message: string;
  sessionToken: string | null;
}

export interface ChatResponse {
  message: string;
  sessionToken: string;
  messageId: number;
  quotaInfo: QuotaInfo;
}

export interface QuotaInfo {
  freeRemaining: number;
  freeLimit: number;
  usingCoins: boolean;
}

export interface ChatMessageDisplay {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  status: 'sending' | 'delivered' | 'failed';
}

export interface ChatAnalyticsResponse {
  totalSessions: number;
  totalMessages: number;
  sessionsThisMonth: number;
  messagesThisMonth: number;
}
