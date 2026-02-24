import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { educationApi } from '../api/education';
import type {
  Education,
  CreateEducationRequest,
  UpdateEducationRequest,
} from '../types/profile';

const EDUCATION_KEYS = {
  all: ['education'] as const,
  byProfile: (profileId: number) => [...EDUCATION_KEYS.all, profileId] as const,
  detail: (profileId: number, educationId: number) =>
    [...EDUCATION_KEYS.byProfile(profileId), educationId] as const,
};

// Get all education records for a profile
export const useEducations = (profileId: number) => {
  return useQuery({
    queryKey: EDUCATION_KEYS.byProfile(profileId),
    queryFn: () => educationApi.getEducations(profileId),
    enabled: !!profileId,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

// Get education by ID
export const useEducation = (profileId: number, educationId: number) => {
  return useQuery({
    queryKey: EDUCATION_KEYS.detail(profileId, educationId),
    queryFn: () => educationApi.getEducationById(profileId, educationId),
    enabled: !!profileId && !!educationId,
    staleTime: 5 * 60 * 1000,
  });
};

// Create education
export const useCreateEducation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      profileId,
      data,
    }: {
      profileId: number;
      data: CreateEducationRequest;
    }) => educationApi.createEducation(profileId, data),
    onSuccess: (newEducation: Education, { profileId }) => {
      // Invalidate the list
      queryClient.invalidateQueries({
        queryKey: EDUCATION_KEYS.byProfile(profileId),
      });
      // Set the new education in cache
      queryClient.setQueryData(
        EDUCATION_KEYS.detail(profileId, newEducation.id),
        newEducation
      );
      // Also invalidate the profile to update the embedded education
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
    },
  });
};

// Update education
export const useUpdateEducation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      profileId,
      educationId,
      data,
    }: {
      profileId: number;
      educationId: number;
      data: UpdateEducationRequest;
    }) => educationApi.updateEducation(profileId, educationId, data),
    onSuccess: (updatedEducation: Education, { profileId }) => {
      // Update the cache
      queryClient.setQueryData(
        EDUCATION_KEYS.detail(profileId, updatedEducation.id),
        updatedEducation
      );
      // Invalidate the list
      queryClient.invalidateQueries({
        queryKey: EDUCATION_KEYS.byProfile(profileId),
      });
      // Also invalidate the profile
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
    },
  });
};

// Delete education
export const useDeleteEducation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      profileId,
      educationId,
    }: {
      profileId: number;
      educationId: number;
    }) => educationApi.deleteEducation(profileId, educationId),
    onSuccess: (_, { profileId, educationId }) => {
      // Remove from cache
      queryClient.removeQueries({
        queryKey: EDUCATION_KEYS.detail(profileId, educationId),
      });
      // Invalidate the list
      queryClient.invalidateQueries({
        queryKey: EDUCATION_KEYS.byProfile(profileId),
      });
      // Also invalidate the profile
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
    },
  });
};
