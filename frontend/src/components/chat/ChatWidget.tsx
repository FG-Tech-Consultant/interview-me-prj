import React, { useEffect } from 'react';
import { ChatPanel } from './ChatPanel';
import { useChat } from '../../hooks/useChat';

interface ChatWidgetProps {
  slug: string;
  profileName: string;
  externalOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
}

export const ChatWidget: React.FC<ChatWidgetProps> = ({
  slug,
  profileName,
  externalOpen,
  onOpenChange,
}) => {
  const {
    messages,
    sendMessage,
    isLoading,
    quotaInfo,
    isOpen,
    toggle,
  } = useChat(slug, profileName);

  // Sync external open state
  useEffect(() => {
    if (externalOpen && !isOpen) {
      toggle();
    }
  }, [externalOpen]); // eslint-disable-line react-hooks/exhaustive-deps

  // Notify parent of open state changes
  useEffect(() => {
    onOpenChange?.(isOpen);
  }, [isOpen]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!isOpen) return null;

  return (
    <ChatPanel
      profileName={profileName}
      messages={messages}
      isLoading={isLoading}
      quotaInfo={quotaInfo}
      onSend={sendMessage}
      onClose={toggle}
    />
  );
};
