import React from 'react';
import {
  Box, Typography, TextField, Button, Paper, Grid, MenuItem,
  CircularProgress, Alert, Chip, Divider,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate, useParams } from 'react-router-dom';
import AutoFixHighIcon from '@mui/icons-material/AutoFixHigh';
import SaveIcon from '@mui/icons-material/Save';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { jobPostingApi } from '../api/jobPostingApi';
import type { CreateJobPostingRequest, UpdateJobPostingRequest } from '../types/jobPosting';

const EXPERIENCE_LEVELS = ['Junior', 'Mid-Level', 'Senior', 'Lead', 'Principal', 'Staff'];
const WORK_MODELS = ['Remote', 'Hybrid', 'On-site'];

export const JobPostingFormPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [title, setTitle] = React.useState('');
  const [description, setDescription] = React.useState('');
  const [requirements, setRequirements] = React.useState('');
  const [benefits, setBenefits] = React.useState('');
  const [location, setLocation] = React.useState('');
  const [workModel, setWorkModel] = React.useState('');
  const [salaryRange, setSalaryRange] = React.useState('');
  const [experienceLevel, setExperienceLevel] = React.useState('');
  const [skillInput, setSkillInput] = React.useState('');
  const [requiredSkills, setRequiredSkills] = React.useState<string[]>([]);
  const [niceSkillInput, setNiceSkillInput] = React.useState('');
  const [niceToHaveSkills, setNiceToHaveSkills] = React.useState<string[]>([]);
  const [error, setError] = React.useState('');

  const { isLoading: loadingJob } = useQuery({
    queryKey: ['job-posting', id],
    queryFn: () => jobPostingApi.getById(Number(id)),
    enabled: isEdit,
    gcTime: 0,
    meta: {
      onSuccess: undefined,
    },
  });

  // Load existing job data for editing
  const { data: existingJob } = useQuery({
    queryKey: ['job-posting', id],
    queryFn: () => jobPostingApi.getById(Number(id)),
    enabled: isEdit,
  });

  React.useEffect(() => {
    if (existingJob) {
      setTitle(existingJob.title);
      setDescription(existingJob.description);
      setRequirements(existingJob.requirements || '');
      setBenefits(existingJob.benefits || '');
      setLocation(existingJob.location || '');
      setWorkModel(existingJob.workModel || '');
      setSalaryRange(existingJob.salaryRange || '');
      setExperienceLevel(existingJob.experienceLevel || '');
      setRequiredSkills(existingJob.requiredSkills || []);
      setNiceToHaveSkills(existingJob.niceToHaveSkills || []);
    }
  }, [existingJob]);

  const createMutation = useMutation({
    mutationFn: (data: CreateJobPostingRequest) => jobPostingApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['job-postings'] });
      navigate('/company/jobs');
    },
    onError: () => setError('Failed to create job posting'),
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdateJobPostingRequest) => jobPostingApi.update(Number(id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['job-postings'] });
      navigate('/company/jobs');
    },
    onError: () => setError('Failed to update job posting'),
  });

  const aiMutation = useMutation({
    mutationFn: () =>
      jobPostingApi.generateDescription({
        title,
        experienceLevel: experienceLevel || undefined,
        workModel: workModel || undefined,
        requiredSkills: requiredSkills.length ? requiredSkills : undefined,
      }),
    onSuccess: (data) => {
      setDescription(data.description);
      setRequirements(data.requirements);
      setBenefits(data.benefits);
      if (data.suggestedSkills?.length) {
        setRequiredSkills((prev) => [...new Set([...prev, ...data.suggestedSkills])]);
      }
      if (data.suggestedNiceToHave?.length) {
        setNiceToHaveSkills((prev) => [...new Set([...prev, ...data.suggestedNiceToHave])]);
      }
    },
    onError: () => setError('AI generation failed. Please fill in the fields manually.'),
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    const data = {
      title,
      description,
      requirements: requirements || undefined,
      benefits: benefits || undefined,
      location: location || undefined,
      workModel: workModel || undefined,
      salaryRange: salaryRange || undefined,
      experienceLevel: experienceLevel || undefined,
      requiredSkills,
      niceToHaveSkills,
    };

    if (isEdit) {
      updateMutation.mutate(data);
    } else {
      createMutation.mutate(data as CreateJobPostingRequest);
    }
  };

  const addSkill = (value: string, setter: React.Dispatch<React.SetStateAction<string[]>>, inputSetter: React.Dispatch<React.SetStateAction<string>>) => {
    const trimmed = value.trim();
    if (trimmed) {
      setter((prev) => prev.includes(trimmed) ? prev : [...prev, trimmed]);
      inputSetter('');
    }
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  if (isEdit && loadingJob) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', mt: 8 }}><CircularProgress /></Box>;
  }

  return (
    <Box>
      <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/company/jobs')} sx={{ mb: 2 }}>
        Back to Jobs
      </Button>

      <Typography variant="h4" gutterBottom>
        {isEdit ? 'Edit Job Posting' : 'Create Job Posting'}
      </Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

      <Paper sx={{ p: 3 }}>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
                <TextField
                  label="Job Title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                  fullWidth
                  placeholder="e.g. Senior Backend Engineer"
                />
                <Button
                  variant="outlined"
                  startIcon={aiMutation.isPending ? <CircularProgress size={18} /> : <AutoFixHighIcon />}
                  onClick={() => aiMutation.mutate()}
                  disabled={!title || aiMutation.isPending}
                  sx={{ whiteSpace: 'nowrap', minWidth: 180, mt: 0.5 }}
                >
                  {aiMutation.isPending ? 'Generating...' : 'AI Generate'}
                </Button>
              </Box>
            </Grid>

            <Grid item xs={12} sm={4}>
              <TextField
                select
                label="Experience Level"
                value={experienceLevel}
                onChange={(e) => setExperienceLevel(e.target.value)}
                fullWidth
              >
                <MenuItem value="">—</MenuItem>
                {EXPERIENCE_LEVELS.map((l) => <MenuItem key={l} value={l}>{l}</MenuItem>)}
              </TextField>
            </Grid>

            <Grid item xs={12} sm={4}>
              <TextField
                select
                label="Work Model"
                value={workModel}
                onChange={(e) => setWorkModel(e.target.value)}
                fullWidth
              >
                <MenuItem value="">—</MenuItem>
                {WORK_MODELS.map((m) => <MenuItem key={m} value={m}>{m}</MenuItem>)}
              </TextField>
            </Grid>

            <Grid item xs={12} sm={4}>
              <TextField
                label="Salary Range"
                value={salaryRange}
                onChange={(e) => setSalaryRange(e.target.value)}
                fullWidth
                placeholder="e.g. $120k - $160k"
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Location"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                fullWidth
                placeholder="e.g. São Paulo, Brazil"
              />
            </Grid>

            <Grid item xs={12}>
              <Divider sx={{ my: 1 }}>Required Skills</Divider>
              <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                <TextField
                  size="small"
                  value={skillInput}
                  onChange={(e) => setSkillInput(e.target.value)}
                  placeholder="Add skill and press Enter"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      addSkill(skillInput, setRequiredSkills, setSkillInput);
                    }
                  }}
                  fullWidth
                />
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => addSkill(skillInput, setRequiredSkills, setSkillInput)}
                >
                  Add
                </Button>
              </Box>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {requiredSkills.map((s) => (
                  <Chip
                    key={s}
                    label={s}
                    onDelete={() => setRequiredSkills((prev) => prev.filter((x) => x !== s))}
                    color="primary"
                    size="small"
                  />
                ))}
              </Box>
            </Grid>

            <Grid item xs={12}>
              <Divider sx={{ my: 1 }}>Nice-to-Have Skills</Divider>
              <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                <TextField
                  size="small"
                  value={niceSkillInput}
                  onChange={(e) => setNiceSkillInput(e.target.value)}
                  placeholder="Add skill and press Enter"
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault();
                      addSkill(niceSkillInput, setNiceToHaveSkills, setNiceSkillInput);
                    }
                  }}
                  fullWidth
                />
                <Button
                  variant="outlined"
                  size="small"
                  onClick={() => addSkill(niceSkillInput, setNiceToHaveSkills, setNiceSkillInput)}
                >
                  Add
                </Button>
              </Box>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {niceToHaveSkills.map((s) => (
                  <Chip
                    key={s}
                    label={s}
                    onDelete={() => setNiceToHaveSkills((prev) => prev.filter((x) => x !== s))}
                    variant="outlined"
                    size="small"
                  />
                ))}
              </Box>
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                required
                fullWidth
                multiline
                minRows={4}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Requirements"
                value={requirements}
                onChange={(e) => setRequirements(e.target.value)}
                fullWidth
                multiline
                minRows={3}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                label="Benefits"
                value={benefits}
                onChange={(e) => setBenefits(e.target.value)}
                fullWidth
                multiline
                minRows={3}
              />
            </Grid>

            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                <Button variant="outlined" onClick={() => navigate('/company/jobs')}>
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={isSaving ? <CircularProgress size={18} /> : <SaveIcon />}
                  disabled={isSaving || !title || !description}
                >
                  {isSaving ? 'Saving...' : isEdit ? 'Update Job' : 'Create Job'}
                </Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Paper>
    </Box>
  );
};
