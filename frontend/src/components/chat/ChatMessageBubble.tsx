import React from 'react';
import { Box, Paper, Typography } from '@mui/material';
import type { ChatMessageDisplay } from '../../types/chat';

interface ChatMessageBubbleProps {
  message: ChatMessageDisplay;
}

function formatTimestamp(date: Date): string {
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);

  if (diffMin < 1) return 'just now';
  if (diffMin < 60) return `${diffMin} min ago`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  return date.toLocaleDateString();
}

export const ChatMessageBubble: React.FC<ChatMessageBubbleProps> = ({ message }) => {
  const isUser = message.role === 'user';
  const isFailed = message.status === 'failed';

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
