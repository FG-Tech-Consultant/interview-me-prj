import React, { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Container,
  Box,
  Typography,
  Button,
  CircularProgress,
  Alert,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Snackbar,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import AddIcon from '@mui/icons-material/Add';
import { useCurrentProfile } from '../hooks/useProfile';
import { useUserSkills, useAddUserSkill, useUpdateUserSkill, useDeleteUserSkill } from '../hooks/useSkills';
import { SkillCard } from '../components/skills/SkillCard';
import { SkillFormDialog } from '../components/skills/SkillFormDialog';
import { SkillFilters, defaultFilters } from '../components/skills/SkillFilters';
import type { SkillFilterValues } from '../components/skills/SkillFilters';
import type { UserSkillDto, AddUserSkillRequest, UpdateUserSkillRequest } from '../types/skill';

export const SkillsPage: React.FC = () => {
  const { data: profile, isLoading: profileLoading } = useCurrentProfile();
  const { data: skillsGrouped, isLoading: skillsLoading, error } = useUserSkills(profile?.id);
  const { t } = useTranslation('skills');

  const addSkill = useAddUserSkill();
  const updateSkill = useUpdateUserSkill(profile?.id);
  const deleteSkill = useDeleteUserSkill(profile?.id);

  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<'add' | 'edit'>('add');
  const [editingSkill, setEditingSkill] = useState<UserSkillDto | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [deletingSkill, setDeletingSkill] = useState<UserSkillDto | null>(null);
  const [snackbar, setSnackbar] = useState<{ open: boolean; message: string; severity: 'success' | 'error' }>({
    open: false,
    message: '',
    severity: 'success',
  });
  const [filters, setFilters] = useState<SkillFilterValues>(defaultFilters);

  const allSkills = useMemo(() => {
    if (!skillsGrouped) return [];
    return Object.values(skillsGrouped).flat();
  }, [skillsGrouped]);

  const filteredGrouped = useMemo(() => {
    if (!skillsGrouped) return {};

    const result: Record<string, UserSkillDto[]> = {};
    for (const [category, skills] of Object.entries(skillsGrouped)) {
      const filtered = skills.filter((s) => {
        if (filters.category && s.skill.category !== filters.category) return false;
        if (s.proficiencyDepth < filters.minProficiency || s.proficiencyDepth > filters.maxProficiency)
          return false;
        if (filters.visibility !== 'all' && s.visibility !== filters.visibility) return false;
        if (filters.search && !s.skill.name.toLowerCase().includes(filters.search.toLowerCase()))
          return false;
        return true;
      });
      if (filtered.length > 0) {
        result[category] = filtered;
      }
    }
    return result;
  }, [skillsGrouped, filters]);

  const filteredCount = useMemo(
    () => Object.values(filteredGrouped).reduce((sum, arr) => sum + arr.length, 0),
    [filteredGrouped]
  );

  const handleAddClick = () => {
    setFormMode('add');
    setEditingSkill(null);
    setFormOpen(true);
  };

  const handleEditClick = (skill: UserSkillDto) => {
    setFormMode('edit');
    setEditingSkill(skill);
    setFormOpen(true);
  };

  const handleDeleteClick = (skill: UserSkillDto) => {
    setDeletingSkill(skill);
    setDeleteDialogOpen(true);
  };

  const handleFormSubmit = async (data: AddUserSkillRequest | UpdateUserSkillRequest) => {
    try {
      if (formMode === 'add' && profile) {
        await addSkill.mutateAsync({ profileId: profile.id, data: data as AddUserSkillRequest });
        setSnackbar({ open: true, message: t('addedSuccess'), severity: 'success' });
      } else if (formMode === 'edit' && editingSkill) {
        await updateSkill.mutateAsync({ id: editingSkill.id, data: data as UpdateUserSkillRequest });
        setSnackbar({ open: true, message: t('updatedSuccess'), severity: 'success' });
      }
      setFormOpen(false);
    } catch (err: any) {
      const message = err?.response?.data?.message || t('common:status.error');
      setSnackbar({ open: true, message, severity: 'error' });
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deletingSkill) return;
    try {
      await deleteSkill.mutateAsync(deletingSkill.id);
      setSnackbar({ open: true, message: t('removedSuccess'), severity: 'success' });
    } catch (err: any) {
      const message = err?.response?.data?.message || t('common:status.error');
      setSnackbar({ open: true, message, severity: 'error' });
    }
    setDeleteDialogOpen(false);
    setDeletingSkill(null);
  };

  if (profileLoading || skillsLoading) {
    return (
      <Container>
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}>
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  if (!profile) {
    return (
      <Container>
        <Box sx={{ mt: 8 }}>
          <Alert severity="info">{t('createProfileFirst')}</Alert>
        </Box>
      </Container>
    );
  }

  if (error) {
    return (
      <Container>
        <Box sx={{ mt: 8 }}>
          <Alert severity="error">{t('failedToLoad')}</Alert>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h4">{t('title')}</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleAddClick}>
            {t('addSkill')}
          </Button>
        </Box>

        <SkillFilters
          filters={filters}
          onFilterChange={setFilters}
          totalCount={allSkills.length}
          filteredCount={filteredCount}
        />

        {allSkills.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 8 }}>
            <Typography variant="h6" color="text.secondary" gutterBottom>
              {t('noSkills')}
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              {t('noSkillsDescription')}
            </Typography>
            <Button variant="outlined" startIcon={<AddIcon />} onClick={handleAddClick}>
              {t('addFirstSkill')}
            </Button>
          </Box>
        ) : (
          Object.entries(filteredGrouped).map(([category, skills]) => (
            <Accordion key={category} defaultExpanded>
              <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                <Typography variant="h6">
                  {category} ({skills.length})
                </Typography>
              </AccordionSummary>
              <AccordionDetails>
                {skills.map((skill) => (
                  <SkillCard
                    key={skill.id}
                    skill={skill}
                    onEdit={handleEditClick}
                    onDelete={handleDeleteClick}
                  />
                ))}
              </AccordionDetails>
            </Accordion>
          ))
        )}
      </Box>

      <SkillFormDialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleFormSubmit}
        skill={editingSkill}
        mode={formMode}
        isLoading={addSkill.isPending || updateSkill.isPending}
      />

      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>{t('removeDialog.title')}</DialogTitle>
        <DialogContent>
          <DialogContentText>
            {t('removeDialog.message', { name: deletingSkill?.skill?.name })}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>{t('common:buttons.cancel')}</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">
            {t('common:buttons.remove')}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        message={snackbar.message}
      />
    </Container>
  );
};
