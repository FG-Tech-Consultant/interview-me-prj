import React from 'react';
import { useTranslation } from 'react-i18next';
import { Fab, Tooltip } from '@mui/material';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline';
import { ChatPanel } from './ChatPanel';
import { useChat } from '../../hooks/useChat';

interface ChatWidgetProps {
  slug: string;
  profileName: string;
}

export const ChatWidget: React.FC<ChatWidgetProps> = ({ slug, profileName }) => {
  const {
    messages,
    sendMessage,
    isLoading,
    quotaInfo,
    isOpen,
    toggle,
  } = useChat(slug, profileName);
  const { t } = useTranslation('chat');

  return (
    <>
      {isOpen && (
        <ChatPanel
          profileName={profileName}
          messages={messages}
          isLoading={isLoading}
          quotaInfo={quotaInfo}
          onSend={sendMessage}
          onClose={toggle}
        />
      )}

      {!isOpen && (
        <Tooltip title={t('tooltip')} placement="left">
          <Fab
            color="primary"
            size="medium"
            onClick={toggle}
            sx={{
              position: 'fixed',
              bottom: 24,
              right: 24,
              zIndex: 1000,
            }}
          >
            <ChatBubbleOutlineIcon />
          </Fab>
        </Tooltip>
      )}
    </>
  );
};
