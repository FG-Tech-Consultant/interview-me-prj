import React, { useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Box,
  Paper,
  Typography,
  IconButton,
  Divider,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { ChatMessageBubble } from './ChatMessageBubble';
import { ChatTypingIndicator } from './ChatTypingIndicator';
import { ChatInput } from './ChatInput';
import { ChatQuotaWarning } from './ChatQuotaWarning';
import type { ChatMessageDisplay, QuotaInfo } from '../../types/chat';

interface ChatPanelProps {
  profileName: string;
  messages: ChatMessageDisplay[];
  isLoading: boolean;
  quotaInfo: QuotaInfo | null;
  onSend: (text: string) => void;
  onClose: () => void;
}

export const ChatPanel: React.FC<ChatPanelProps> = ({
  profileName,
  messages,
  isLoading,
  quotaInfo,
  onSend,
  onClose,
}) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const { t } = useTranslation('chat');

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  const isDisabled =
    isLoading ||
    (quotaInfo?.usingCoins === true && quotaInfo?.freeRemaining === 0);

  return (
    <Paper
      elevation={8}
      sx={{
        position: 'fixed',
        bottom: isMobile ? 0 : 24,
        right: isMobile ? 0 : 24,
        width: isMobile ? '100%' : 400,
        height: isMobile ? '100%' : 520,
        display: 'flex',
        flexDirection: 'column',
        borderRadius: isMobile ? 0 : 3,
        overflow: 'hidden',
        zIndex: 1100,
      }}
    >
      {/* Header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          px: 2,
          py: 1.5,
          bgcolor: 'primary.main',
          color: 'primary.contrastText',
        }}
      >
        <Typography variant="subtitle1" fontWeight={600} noWrap>
          {t('careerAssistant', { name: profileName.split(' ')[0] })}
        </Typography>
        <IconButton size="small" onClick={onClose} sx={{ color: 'inherit' }}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </Box>

      <Divider />

      {/* Messages */}
      <Box
        sx={{
          flex: 1,
          overflowY: 'auto',
          py: 1,
          bgcolor: 'background.default',
        }}
      >
        {messages.map((msg) => (
          <ChatMessageBubble key={msg.id} message={msg} />
        ))}
        {isLoading && <ChatTypingIndicator />}
        <div ref={messagesEndRef} />
      </Box>

      {/* Quota Warning */}
      <ChatQuotaWarning quotaInfo={quotaInfo} />

      <Divider />

      {/* Input */}
      <ChatInput
        onSend={onSend}
        disabled={isDisabled}
        placeholder={isDisabled ? t('unavailable') : t('placeholder')}
      />
    </Paper>
  );
};
