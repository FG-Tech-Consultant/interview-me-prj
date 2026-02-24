import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '../api/profile';
import type {
  Profile,
  CreateProfileRequest,
  UpdateProfileRequest,
} from '../types/profile';

const PROFILE_KEYS = {
  all: ['profiles'] as const,
  current: () => [...PROFILE_KEYS.all, 'current'] as const,
  detail: (id: number) => [...PROFILE_KEYS.all, id] as const,
  exists: () => [...PROFILE_KEYS.all, 'exists'] as const,
};

// Get current user's profile
export const useCurrentProfile = () => {
  return useQuery({
    queryKey: PROFILE_KEYS.current(),
    queryFn: profileApi.getCurrentProfile,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

// Get profile by ID
export const useProfile = (profileId: number) => {
  return useQuery({
    queryKey: PROFILE_KEYS.detail(profileId),
    queryFn: () => profileApi.getProfileById(profileId),
    enabled: !!profileId,
    staleTime: 5 * 60 * 1000,
  });
};

// Check if profile exists
export const useProfileExists = () => {
  return useQuery({
    queryKey: PROFILE_KEYS.exists(),
    queryFn: profileApi.checkProfileExists,
    staleTime: 5 * 60 * 1000,
  });
};

// Create profile
export const useCreateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateProfileRequest) => profileApi.createProfile(data),
    onSuccess: (newProfile: Profile) => {
      // Invalidate and refetch
      queryClient.invalidateQueries({ queryKey: PROFILE_KEYS.current() });
      queryClient.invalidateQueries({ queryKey: PROFILE_KEYS.exists() });
      // Set the new profile in cache
      queryClient.setQueryData(PROFILE_KEYS.detail(newProfile.id), newProfile);
    },
  });
};

// Update profile
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      profileId,
      data,
    }: {
      profileId: number;
      data: UpdateProfileRequest;
    }) => profileApi.updateProfile(profileId, data),
    onSuccess: (updatedProfile: Profile) => {
      // Update the cache
      queryClient.setQueryData(
        PROFILE_KEYS.detail(updatedProfile.id),
        updatedProfile
      );
      queryClient.invalidateQueries({ queryKey: PROFILE_KEYS.current() });
    },
  });
};

// Delete profile
export const useDeleteProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (profileId: number) => profileApi.deleteProfile(profileId),
    onSuccess: (_, profileId) => {
      // Remove from cache and invalidate
      queryClient.removeQueries({ queryKey: PROFILE_KEYS.detail(profileId) });
      queryClient.invalidateQueries({ queryKey: PROFILE_KEYS.current() });
      queryClient.invalidateQueries({ queryKey: PROFILE_KEYS.exists() });
    },
  });
};
