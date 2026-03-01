import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Button,
  IconButton,
  Typography,
  Box,
  Stack,
  Card,
  CardContent,
  Paper,
  Alert,
} from '@mui/material';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { useUpdateProfile } from '../../hooks/useProfile';
import { LanguageForm } from './LanguageForm';
import type { Profile } from '../../types/profile';

interface LanguageListProps {
  profile: Profile;
}

export const LanguageList: React.FC<LanguageListProps> = ({ profile }) => {
  const [isAdding, setIsAdding] = useState(false);
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const { t } = useTranslation('profile');

  const updateMutation = useUpdateProfile();

  const languages = profile.languages || [];

  const saveLanguages = (newLanguages: string[]) => {
    updateMutation.mutate({
      profileId: profile.id,
      data: {
        fullName: profile.fullName,
        headline: profile.headline || undefined,
        summary: profile.summary || undefined,
        location: profile.location || undefined,
        languages: newLanguages,
        version: profile.version,
      },
    });
  };

  const handleAdd = () => {
    setIsAdding(true);
    setEditingIndex(null);
  };

  const handleCancelAdd = () => {
    setIsAdding(false);
  };

  const handleEdit = (index: number) => {
    setEditingIndex(index);
    setIsAdding(false);
  };

  const handleCancelEdit = () => {
    setEditingIndex(null);
  };

  const handleSaveNew = (language: string) => {
    saveLanguages([...languages, language]);
    setIsAdding(false);
  };

  const handleSaveEdit = (index: number, language: string) => {
    const updated = [...languages];
    updated[index] = language;
    saveLanguages(updated);
    setEditingIndex(null);
  };

  const handleDelete = (index: number) => {
    if (window.confirm(t('languages.confirmDelete'))) {
      const updated = languages.filter((_, i) => i !== index);
      saveLanguages(updated);
    }
  };

  return (
    <Stack spacing={3}>
      {!isAdding && editingIndex === null && (
        <Box>
          <Button variant="contained" onClick={handleAdd}>
            {t('languages.addButton')}
          </Button>
        </Box>
      )}

      {isAdding && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" sx={{ mb: 2 }}>
            {t('languages.addTitle')}
          </Typography>
          <LanguageForm
            onSave={handleSaveNew}
            onCancel={handleCancelAdd}
            isLoading={updateMutation.isPending}
            error={updateMutation.error}
            existingLanguages={languages}
          />
        </Paper>
      )}

      {updateMutation.isSuccess && !isAdding && editingIndex === null && (
        <Alert severity="success">{t('languages.saveSuccess')}</Alert>
      )}

      <Stack spacing={2}>
        {languages.length > 0 ? (
          languages.map((lang, index) => (
            <Card key={index} variant="outlined">
              <CardContent>
                {editingIndex === index ? (
                  <LanguageForm
                    language={lang}
                    onSave={(updated) => handleSaveEdit(index, updated)}
                    onCancel={handleCancelEdit}
                    isLoading={updateMutation.isPending}
                    error={updateMutation.error}
                    existingLanguages={languages}
                  />
                ) : (
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Box>
                      <Typography variant="body1">{lang}</Typography>
                    </Box>
                    <Stack direction="row" spacing={1}>
                      <IconButton size="small" aria-label="edit" onClick={() => handleEdit(index)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" aria-label="delete" color="error" onClick={() => handleDelete(index)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  </Box>
                )}
              </CardContent>
            </Card>
          ))
        ) : (
          <Typography color="text.secondary">
            {t('languages.noRecords')}
          </Typography>
        )}
      </Stack>
    </Stack>
  );
};
