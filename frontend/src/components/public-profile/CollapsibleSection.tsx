import React, { useState } from 'react';
import { Box, Typography, Collapse, Stack } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

interface CollapsibleSectionProps {
  title: string;
  icon?: React.ReactNode;
  defaultExpanded?: boolean;
  children: React.ReactNode;
}

export const CollapsibleSection: React.FC<CollapsibleSectionProps> = ({
  title,
  icon,
  defaultExpanded = false,
  children,
}) => {
  const [expanded, setExpanded] = useState(defaultExpanded);

  return (
    <Box sx={{ mb: 3 }}>
      <Box
        onClick={() => setExpanded((prev) => !prev)}
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          cursor: 'pointer',
          py: 1.5,
          px: 2,
          borderRadius: 2,
          bgcolor: 'action.hover',
          border: '1px solid',
          borderColor: 'divider',
          transition: 'background-color 0.2s',
          '&:hover': {
            bgcolor: 'action.selected',
          },
        }}
      >
        <Stack direction="row" alignItems="center" spacing={1}>
          {icon}
          <Typography variant="h5" fontWeight="bold">
            {title}
          </Typography>
        </Stack>
        <ExpandMoreIcon
          sx={{
            transform: expanded ? 'rotate(180deg)' : 'rotate(0deg)',
            transition: 'transform 0.3s',
            color: 'text.secondary',
          }}
        />
      </Box>
      <Collapse in={expanded} timeout={300}>
        <Box sx={{ pt: 2 }}>{children}</Box>
      </Collapse>
    </Box>
  );
};
