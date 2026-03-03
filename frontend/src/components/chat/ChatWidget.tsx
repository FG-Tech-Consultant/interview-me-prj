import React, { useEffect, useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { ChatPanel } from './ChatPanel';
import { VisitorIdentificationDialog, type VisitorFormData } from './VisitorIdentificationDialog';
import { useChat } from '../../hooks/useChat';
import { visitorApi } from '../../api/visitorApi';

interface ChatWidgetProps {
  slug: string;
  profileName: string;
  externalOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
}

const VISITOR_TOKEN_KEY = (slug: string) => `visitor_token_${slug}`;

export const ChatWidget: React.FC<ChatWidgetProps> = ({
  slug,
  profileName,
  externalOpen,
  onOpenChange,
}) => {
  const { i18n } = useTranslation();

  const [visitorToken, setVisitorToken] = useState<string | null>(() => {
    try {
      return sessionStorage.getItem(VISITOR_TOKEN_KEY(slug));
    } catch {
      return null;
    }
  });
  const [showIdentify, setShowIdentify] = useState(false);
  const [identifyLoading, setIdentifyLoading] = useState(false);
  const [identifyError, setIdentifyError] = useState<string | null>(null);

  const {
    messages,
    sendMessage,
    isLoading,
    quotaInfo,
    isOpen,
    toggle,
  } = useChat(slug, profileName, visitorToken);

  // When external open is requested, check if visitor is identified
  useEffect(() => {
    if (externalOpen && !isOpen) {
      if (visitorToken) {
        toggle();
      } else {
        setShowIdentify(true);
      }
    }
  }, [externalOpen]); // eslint-disable-line react-hooks/exhaustive-deps

  // Notify parent of open state changes
  useEffect(() => {
    onOpenChange?.(isOpen || showIdentify);
  }, [isOpen, showIdentify]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleIdentify = useCallback(async (data: VisitorFormData) => {
    setIdentifyLoading(true);
    setIdentifyError(null);
    try {
      const response = await visitorApi.identify(slug, { ...data, locale: i18n.language });
      setVisitorToken(response.visitorToken);
      try {
        sessionStorage.setItem(VISITOR_TOKEN_KEY(slug), response.visitorToken);
      } catch {
        // sessionStorage not available
      }
      setShowIdentify(false);
      // Open the chat after identification
      if (!isOpen) {
        toggle();
      }
    } catch (err: any) {
      setIdentifyError(err?.response?.data?.message || 'Failed to identify. Please try again.');
    } finally {
      setIdentifyLoading(false);
    }
  }, [slug, isOpen, toggle, i18n.language]);

  const handleCloseIdentify = useCallback(() => {
    setShowIdentify(false);
    onOpenChange?.(false);
  }, [onOpenChange]);

  const handleToggle = useCallback(() => {
    if (!visitorToken && !isOpen) {
      // Need to identify first
      setShowIdentify(true);
    } else {
      toggle();
    }
  }, [visitorToken, isOpen, toggle]);

  if (!isOpen && !showIdentify) return null;

  return (
    <>
      <VisitorIdentificationDialog
        open={showIdentify}
        profileName={profileName}
        onIdentify={handleIdentify}
        onClose={handleCloseIdentify}
        isLoading={identifyLoading}
        error={identifyError}
      />
      {isOpen && (
        <ChatPanel
          profileName={profileName}
          messages={messages}
          isLoading={isLoading}
          quotaInfo={quotaInfo}
          onSend={sendMessage}
          onClose={handleToggle}
        />
      )}
    </>
  );
};
