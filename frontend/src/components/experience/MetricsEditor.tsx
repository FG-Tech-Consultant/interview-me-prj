import React, { useState } from 'react';
import { Stack, Typography, IconButton, TextField, Button, Box } from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';

interface MetricsEditorProps {
  value: Record<string, unknown> | null | undefined;
  onChange: (metrics: Record<string, unknown>) => void;
}

export const MetricsEditor: React.FC<MetricsEditorProps> = ({ value, onChange }) => {
  const metrics = value || {};
  const entries = Object.entries(metrics);
  const [newKey, setNewKey] = useState('');
  const [newValue, setNewValue] = useState('');

  const handleAdd = () => {
    if (!newKey.trim()) return;
    const updated = { ...metrics, [newKey.trim()]: newValue.trim() };
    onChange(updated);
    setNewKey('');
    setNewValue('');
  };

  const handleRemove = (key: string) => {
    const updated = { ...metrics };
    delete updated[key];
    onChange(updated);
  };

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1 }}>
        Metrics
      </Typography>

      <Stack spacing={1}>
        {entries.map(([key, val]) => (
          <Stack key={key} direction="row" alignItems="center" spacing={1}>
            <Typography variant="body2" fontWeight="medium" sx={{ minWidth: 100 }}>
              {key}:
            </Typography>
            <Typography variant="body2" sx={{ flex: 1 }}>
              {String(val)}
            </Typography>
            <IconButton size="small" color="error" onClick={() => handleRemove(key)}>
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Stack>
        ))}

        <Stack direction="row" spacing={1} alignItems="center">
          <TextField
            size="small"
            placeholder="Key (e.g. TPS)"
            value={newKey}
            onChange={(e) => setNewKey(e.target.value)}
            sx={{ flex: 1 }}
          />
          <TextField
            size="small"
            placeholder="Value (e.g. 15000)"
            value={newValue}
            onChange={(e) => setNewValue(e.target.value)}
            sx={{ flex: 1 }}
          />
          <Button variant="outlined" size="small" onClick={handleAdd}>
            Add
          </Button>
        </Stack>
      </Stack>
    </Box>
  );
};
