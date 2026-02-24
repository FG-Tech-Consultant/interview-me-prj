import React, { useState, useMemo } from 'react';
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

  // Flatten all skills for filtering
  const allSkills = useMemo(() => {
    if (!skillsGrouped) return [];
    return Object.values(skillsGrouped).flat();
  }, [skillsGrouped]);

  // Apply filters
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
        setSnackbar({ open: true, message: 'Skill added successfully', severity: 'success' });
      } else if (formMode === 'edit' && editingSkill) {
        await updateSkill.mutateAsync({ id: editingSkill.id, data: data as UpdateUserSkillRequest });
        setSnackbar({ open: true, message: 'Skill updated successfully', severity: 'success' });
      }
      setFormOpen(false);
    } catch (err: any) {
      const message = err?.response?.data?.message || 'An error occurred';
      setSnackbar({ open: true, message, severity: 'error' });
    }
  };

  const handleDeleteConfirm = async () => {
    if (!deletingSkill) return;
    try {
      await deleteSkill.mutateAsync(deletingSkill.id);
      setSnackbar({ open: true, message: 'Skill removed from profile', severity: 'success' });
    } catch (err: any) {
      const message = err?.response?.data?.message || 'An error occurred';
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
          <Alert severity="info">Create a profile first to manage your skills.</Alert>
        </Box>
      </Container>
    );
  }

  if (error) {
    return (
      <Container>
        <Box sx={{ mt: 8 }}>
          <Alert severity="error">Failed to load skills.</Alert>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      <Box sx={{ mt: 4, mb: 4 }}>
        <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
          <Typography variant="h4">Skills</Typography>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleAddClick}>
            Add Skill
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
              No skills added yet
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={2}>
              Add your first skill to showcase your expertise.
            </Typography>
            <Button variant="outlined" startIcon={<AddIcon />} onClick={handleAddClick}>
              Add Your First Skill
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
        <DialogTitle>Remove Skill</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Remove '{deletingSkill?.skill?.name}' from your profile? This will hide it from all
            exports and recruiter chat.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleDeleteConfirm} color="error" variant="contained">
            Remove
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
