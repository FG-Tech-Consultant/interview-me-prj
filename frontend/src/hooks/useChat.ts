import { useState, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { chatApi } from '../api/chatApi';
import type { ChatMessageDisplay, QuotaInfo } from '../types/chat';

interface UseChatReturn {
  messages: ChatMessageDisplay[];
  sendMessage: (text: string) => Promise<void>;
  isLoading: boolean;
  error: string | null;
  quotaInfo: QuotaInfo | null;
  isOpen: boolean;
  toggle: () => void;
  clearError: () => void;
  sessionToken: string | null;
}

export function useChat(slug: string, profileName: string): UseChatReturn {
  const { t } = useTranslation('chat');
  const [messages, setMessages] = useState<ChatMessageDisplay[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [quotaInfo, setQuotaInfo] = useState<QuotaInfo | null>(null);
  const [isOpen, setIsOpen] = useState(false);
  const [sessionToken, setSessionToken] = useState<string | null>(() => {
    try {
      return localStorage.getItem(`chat_session_${slug}`);
    } catch {
      return null;
    }
  });
  const hasGreeted = useRef(false);

  const addGreeting = useCallback(() => {
    if (!hasGreeted.current) {
      hasGreeted.current = true;
      const firstName = profileName.split(' ')[0];
      setMessages([
        {
          id: 'greeting',
          role: 'assistant',
          content: t('greeting', { name: firstName }),
          timestamp: new Date(),
          status: 'delivered',
        },
      ]);
    }
  }, [profileName, t]);

  const toggle = useCallback(() => {
    setIsOpen((prev) => {
      if (!prev) {
        addGreeting();
      }
      return !prev;
    });
  }, [addGreeting]);

  const clearError = useCallback(() => setError(null), []);

  const sendMessage = useCallback(
    async (text: string) => {
      if (!text.trim() || isLoading) return;

      setError(null);

      // Optimistic add user message
      const userMsg: ChatMessageDisplay = {
        id: `user-${Date.now()}`,
        role: 'user',
        content: text.trim(),
        timestamp: new Date(),
        status: 'delivered',
      };
      setMessages((prev) => [...prev, userMsg]);
      setIsLoading(true);

      try {
        const response = await chatApi.sendMessage(slug, text.trim(), sessionToken);

        // Save session token
        if (response.sessionToken) {
          setSessionToken(response.sessionToken);
          try {
            localStorage.setItem(`chat_session_${slug}`, response.sessionToken);
          } catch {
            // localStorage not available
          }
        }

        // Add assistant message
        const assistantMsg: ChatMessageDisplay = {
          id: `assistant-${response.messageId}`,
          role: 'assistant',
          content: response.message,
          timestamp: new Date(),
          status: 'delivered',
        };
        setMessages((prev) => [...prev, assistantMsg]);
        setQuotaInfo(response.quotaInfo);
      } catch (err: any) {
        const status = err?.response?.status;
        const data = err?.response?.data;

        let errorMessage: string;
        if (status === 402) {
          errorMessage = t('errors.quotaReached');
        } else if (status === 429) {
          errorMessage = t('errors.rateLimit');
        } else if (status === 503) {
          errorMessage = t('errors.unavailable');
        } else if (status === 404) {
          errorMessage = t('errors.notFound');
        } else {
          errorMessage = data?.message || t('errors.generic');
        }

        setError(errorMessage);

        // Add error message
        const errorMsg: ChatMessageDisplay = {
          id: `error-${Date.now()}`,
          role: 'assistant',
          content: errorMessage,
          timestamp: new Date(),
          status: 'failed',
        };
        setMessages((prev) => [...prev, errorMsg]);
      } finally {
        setIsLoading(false);
      }
    },
    [slug, sessionToken, isLoading, t]
  );

  return {
    messages,
    sendMessage,
    isLoading,
    error,
    quotaInfo,
    isOpen,
    toggle,
    clearError,
    sessionToken,
  };
}
