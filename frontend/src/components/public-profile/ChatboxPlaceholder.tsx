import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Fab, Snackbar, Tooltip } from '@mui/material';
import ChatBubbleOutlineIcon from '@mui/icons-material/ChatBubbleOutline';

export const ChatboxPlaceholder: React.FC = () => {
  const [open, setOpen] = useState(false);
  const { t } = useTranslation('public-profile');

  return (
    <>
      <Tooltip title={t('chatComingSoon')} placement="left">
        <Fab
          color="primary"
          size="medium"
          onClick={() => setOpen(true)}
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
      <Snackbar
        open={open}
        autoHideDuration={3000}
        onClose={() => setOpen(false)}
        message={t('chatComingSoon')}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </>
  );
};
