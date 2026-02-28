import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Paper, Typography } from '@mui/material';
import type { ChatMessageDisplay } from '../../types/chat';

interface ChatMessageBubbleProps {
  message: ChatMessageDisplay;
}

function useFormatTimestamp() {
  const { t } = useTranslation('chat');

  return (date: Date): string => {
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);

    if (diffMin < 1) return t('time.justNow');
    if (diffMin < 60) return t('time.minAgo', { count: diffMin });
    const diffHours = Math.floor(diffMin / 60);
    if (diffHours < 24) return t('time.hoursAgo', { count: diffHours });
    return date.toLocaleDateString();
  };
}

export const ChatMessageBubble: React.FC<ChatMessageBubbleProps> = ({ message }) => {
  const isUser = message.role === 'user';
  const isFailed = message.status === 'failed';
  const formatTimestamp = useFormatTimestamp();

  return (
    <Box
      sx={{
        display: 'flex',
        justifyContent: isUser ? 'flex-end' : 'flex-start',
        mb: 1.5,
        px: 1,
      }}
    >
      <Paper
        elevation={0}
        sx={{
          p: 1.5,
          maxWidth: '80%',
          borderRadius: 2,
          bgcolor: isFailed
            ? 'error.light'
            : isUser
              ? 'primary.main'
              : 'grey.100',
          color: isFailed
            ? 'error.contrastText'
            : isUser
              ? 'primary.contrastText'
              : 'text.primary',
        }}
      >
        <Typography
          variant="body2"
          sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}
        >
          {message.content}
        </Typography>
        <Typography
          variant="caption"
          sx={{
            display: 'block',
            mt: 0.5,
            textAlign: isUser ? 'right' : 'left',
            opacity: 0.7,
          }}
        >
          {formatTimestamp(message.timestamp)}
        </Typography>
      </Paper>
    </Box>
  );
};
