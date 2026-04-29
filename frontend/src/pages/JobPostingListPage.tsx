import React from 'react';
import {
  Box, Typography, Button, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Chip, IconButton, CircularProgress,
  Alert, Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import VisibilityIcon from '@mui/icons-material/Visibility';
import { jobPostingApi } from '../api/jobPostingApi';

const statusColor: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
  ACTIVE: 'success',
  PAUSED: 'warning',
  CLOSED: 'error',
  DRAFT: 'default',
};

export const JobPostingListPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [deleteId, setDeleteId] = React.useState<number | null>(null);

  const { data: jobs, isLoading, error } = useQuery({
    queryKey: ['job-postings'],
    queryFn: jobPostingApi.list,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => jobPostingApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['job-postings'] });
      setDeleteId(null);
    },
  });

  if (isLoading) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}><CircularProgress /></Box>;
  }

  if (error) {
    return <Alert severity="error">Failed to load job postings</Alert>;
  }

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Job Postings</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => navigate('/company/jobs/new')}
        >
          Create Job
        </Button>
      </Box>

      {!jobs?.length ? (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary" sx={{ mb: 2 }}>
            No job postings yet. Create your first one!
          </Typography>
          <Button variant="outlined" startIcon={<AddIcon />} onClick={() => navigate('/company/jobs/new')}>
            Create Job Posting
          </Button>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Title</TableCell>
                <TableCell>Location</TableCell>
                <TableCell>Level</TableCell>
                <TableCell>Model</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Skills</TableCell>
                <TableCell>Created</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {jobs.map((job) => (
                <TableRow key={job.id} hover>
                  <TableCell>
                    <Typography fontWeight={500}>{job.title}</Typography>
                  </TableCell>
                  <TableCell>{job.location || '—'}</TableCell>
                  <TableCell>{job.experienceLevel || '—'}</TableCell>
                  <TableCell>{job.workModel || '—'}</TableCell>
                  <TableCell>
                    <Chip
                      label={job.status}
                      color={statusColor[job.status] || 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {job.requiredSkills?.slice(0, 3).map((s) => (
                      <Chip key={s} label={s} size="small" sx={{ mr: 0.5, mb: 0.5 }} variant="outlined" />
                    ))}
                    {(job.requiredSkills?.length ?? 0) > 3 && (
                      <Chip label={`+${job.requiredSkills.length - 3}`} size="small" variant="outlined" />
                    )}
                  </TableCell>
                  <TableCell>{new Date(job.createdAt).toLocaleDateString()}</TableCell>
                  <TableCell align="right">
                    <IconButton
                      size="small"
                      onClick={() => window.open(`${import.meta.env.BASE_URL}jobs/${job.slug}`, '_blank')}
                      title="View public page"
                    >
                      <VisibilityIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => navigate(`/company/jobs/${job.id}/edit`)}
                      title="Edit"
                    >
                      <EditIcon fontSize="small" />
                    </IconButton>
                    <IconButton
                      size="small"
                      onClick={() => setDeleteId(job.id)}
                      title="Delete"
                      color="error"
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={deleteId !== null} onClose={() => setDeleteId(null)}>
        <DialogTitle>Delete Job Posting?</DialogTitle>
        <DialogContent>
          <Typography>This will close the job posting and hide it from candidates.</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteId(null)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => deleteId && deleteMutation.mutate(deleteId)}
            disabled={deleteMutation.isPending}
          >
            {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
