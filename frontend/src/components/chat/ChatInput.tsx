import React, { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, TextField, IconButton, Typography } from '@mui/material';
import SendIcon from '@mui/icons-material/Send';

interface ChatInputProps {
  onSend: (text: string) => void;
  disabled: boolean;
  placeholder?: string;
}

export const ChatInput: React.FC<ChatInputProps> = ({
  onSend,
  disabled,
  placeholder,
}) => {
  const [text, setText] = useState('');
  const { t } = useTranslation('chat');

  const handleSend = useCallback(() => {
    if (text.trim() && !disabled) {
      onSend(text.trim());
      setText('');
    }
  }, [text, disabled, onSend]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Box sx={{ display: 'flex', alignItems: 'flex-end', gap: 1, p: 1.5 }}>
      <Box sx={{ flex: 1, position: 'relative' }}>
        <TextField
          fullWidth
          size="small"
          multiline
          maxRows={3}
          value={text}
          onChange={(e) => setText(e.target.value.slice(0, 500))}
          onKeyDown={handleKeyDown}
          placeholder={placeholder || t('placeholderDefault')}
          disabled={disabled}
          variant="outlined"
          sx={{ '& .MuiOutlinedInput-root': { borderRadius: 3 } }}
        />
        {text.length > 400 && (
          <Typography
            variant="caption"
            color={text.length >= 500 ? 'error' : 'text.secondary'}
            sx={{ position: 'absolute', right: 8, bottom: -18 }}
          >
            {text.length}/500
          </Typography>
        )}
      </Box>
      <IconButton
        color="primary"
        onClick={handleSend}
        disabled={disabled || !text.trim()}
        size="small"
      >
        <SendIcon />
      </IconButton>
    </Box>
  );
};
