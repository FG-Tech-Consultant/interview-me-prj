import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { jobExperienceApi } from '../api/jobExperience';
import type {
  JobExperience,
  CreateJobExperienceRequest,
  UpdateJobExperienceRequest,
} from '../types/profile';

const JOB_EXPERIENCE_KEYS = {
  all: ['jobExperiences'] as const,
  byProfile: (profileId: number) =>
    [...JOB_EXPERIENCE_KEYS.all, profileId] as const,
  detail: (profileId: number, experienceId: number) =>
    [...JOB_EXPERIENCE_KEYS.byProfile(profileId), experienceId] as const,
};

// Get all job experiences for a profile
export const useJobExperiences = (profileId: number) => {
  return useQuery({
    queryKey: JOB_EXPERIENCE_KEYS.byProfile(profileId),
    queryFn: () => jobExperienceApi.getJobExperiences(profileId),
    enabled: !!profileId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

// Get job experience by ID
export const useJobExperience = (profileId: number, experienceId: number) => {
  return useQuery({
    queryKey: JOB_EXPERIENCE_KEYS.detail(profileId, experienceId),
    queryFn: () => jobExperienceApi.getJobExperienceById(profileId, experienceId),
    enabled: !!profileId && !!experienceId,
    staleTime: 5 * 60 * 1000,
  });
};

// Create job experience
export const useCreateJobExperience = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      profileId,
      data,
    }: {
      profileId: number;
      data: CreateJobExperienceRequest;
    }) => jobExperienceApi.createJobExperience(profileId, data),
    onSuccess: (newExperience: JobExperience, { profileId }) => {
      // Invalidate the list
      queryClient.invalidateQueries({
        queryKey: JOB_EXPERIENCE_KEYS.byProfile(profileId),
      });
      // Set the new experience in cache
      queryClient.setQueryData(
        JOB_EXPERIENCE_KEYS.detail(profileId, newExperience.id),
        newExperience
      );
      // Also invalidate the profile to update the embedded experiences
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
    },
  });
};

// Update job experience
export const useUpdateJobExperience = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      profileId,
      experienceId,
      data,
    }: {
      profileId: number;
      experienceId: number;
      data: UpdateJobExperienceRequest;
    }) => jobExperienceApi.updateJobExperience(profileId, experienceId, data),
    onSuccess: (updatedExperience: JobExperience, { profileId }) => {
      // Update the cache
      queryClient.setQueryData(
        JOB_EXPERIENCE_KEYS.detail(profileId, updatedExperience.id),
        updatedExperience
      );
      // Invalidate the list
      queryClient.invalidateQueries({
        queryKey: JOB_EXPERIENCE_KEYS.byProfile(profileId),
      });
      // Also invalidate the profile
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
    },
  });
};

// Delete job experience
export const useDeleteJobExperience = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      profileId,
      experienceId,
    }: {
      profileId: number;
      experienceId: number;
    }) => jobExperienceApi.deleteJobExperience(profileId, experienceId),
    onSuccess: (_, { profileId, experienceId }) => {
      // Remove from cache
      queryClient.removeQueries({
        queryKey: JOB_EXPERIENCE_KEYS.detail(profileId, experienceId),
      });
      // Invalidate the list
      queryClient.invalidateQueries({
        queryKey: JOB_EXPERIENCE_KEYS.byProfile(profileId),
      });
      // Also invalidate the profile
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
    },
  });
};
