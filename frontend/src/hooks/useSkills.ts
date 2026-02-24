import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { skillsApi } from '../api/skillsApi';
import type {
  AddUserSkillRequest,
  UpdateUserSkillRequest,
} from '../types/skill';

const SKILL_KEYS = {
  all: ['skills'] as const,
  userSkills: (profileId: number) => [...SKILL_KEYS.all, 'user', profileId] as const,
  userSkillDetail: (id: number) => [...SKILL_KEYS.all, 'user', 'detail', id] as const,
};

export const useUserSkills = (profileId: number | undefined) => {
  return useQuery({
    queryKey: SKILL_KEYS.userSkills(profileId!),
    queryFn: () => skillsApi.getUserSkills(profileId!),
    enabled: !!profileId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useAddUserSkill = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ profileId, data }: { profileId: number; data: AddUserSkillRequest }) =>
      skillsApi.addUserSkill(profileId, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({
        queryKey: SKILL_KEYS.userSkills(variables.profileId),
      });
    },
  });
};

export const useUpdateUserSkill = (profileId: number | undefined) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserSkillRequest }) =>
      skillsApi.updateUserSkill(id, data),
    onSuccess: () => {
      if (profileId) {
        queryClient.invalidateQueries({
          queryKey: SKILL_KEYS.userSkills(profileId),
        });
      }
    },
  });
};

export const useDeleteUserSkill = (profileId: number | undefined) => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => skillsApi.deleteUserSkill(id),
    onSuccess: () => {
      if (profileId) {
        queryClient.invalidateQueries({
          queryKey: SKILL_KEYS.userSkills(profileId),
        });
      }
    },
  });
};
