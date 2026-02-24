import React from 'react';
import { Box } from '@mui/material';

export const ChatTypingIndicator: React.FC = () => (
  <Box
    sx={{
      display: 'flex',
      gap: 0.5,
      p: 1.5,
      maxWidth: '80px',
      bgcolor: 'grey.100',
      borderRadius: 2,
      ml: 1,
      mb: 1,
    }}
  >
    {[0, 1, 2].map((i) => (
      <Box
        key={i}
        sx={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          bgcolor: 'grey.400',
          animation: 'typing-bounce 1.4s infinite ease-in-out both',
          animationDelay: `${i * 0.16}s`,
          '@keyframes typing-bounce': {
            '0%, 80%, 100%': { transform: 'scale(0.6)' },
            '40%': { transform: 'scale(1)' },
          },
        }}
      />
    ))}
  </Box>
);
