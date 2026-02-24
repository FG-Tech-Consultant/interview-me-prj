import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { storyApi } from '../api/experienceApi';
import type {
  StoryResponse,
  CreateStoryRequest,
  UpdateStoryRequest,
} from '../types/story';

const STORY_KEYS = {
  all: ['stories'] as const,
  byProject: (projectId: number) => [...STORY_KEYS.all, projectId] as const,
  detail: (storyId: number) => [...STORY_KEYS.all, 'detail', storyId] as const,
  publicByProfile: (profileId: number) =>
    [...STORY_KEYS.all, 'public', profileId] as const,
};

export const useStories = (projectId: number) => {
  return useQuery({
    queryKey: STORY_KEYS.byProject(projectId),
    queryFn: () => storyApi.getStoriesByProject(projectId),
    enabled: !!projectId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useStory = (storyId: number) => {
  return useQuery({
    queryKey: STORY_KEYS.detail(storyId),
    queryFn: () => storyApi.getStory(storyId),
    enabled: !!storyId,
    staleTime: 5 * 60 * 1000,
  });
};

export const usePublicStories = (profileId: number) => {
  return useQuery({
    queryKey: STORY_KEYS.publicByProfile(profileId),
    queryFn: () => storyApi.getPublicStoriesByProfile(profileId),
    enabled: !!profileId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useCreateStory = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      projectId,
      data,
    }: {
      projectId: number;
      data: CreateStoryRequest;
    }) => storyApi.createStory(projectId, data),
    onSuccess: (newStory: StoryResponse, { projectId }) => {
      queryClient.invalidateQueries({
        queryKey: STORY_KEYS.byProject(projectId),
      });
      queryClient.setQueryData(
        STORY_KEYS.detail(newStory.id),
        newStory
      );
      // Invalidate projects to update story count
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });
};

export const useUpdateStory = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storyId,
      data,
    }: {
      storyId: number;
      projectId: number;
      data: UpdateStoryRequest;
    }) => storyApi.updateStory(storyId, data),
    onSuccess: (updatedStory: StoryResponse, { projectId }) => {
      queryClient.setQueryData(
        STORY_KEYS.detail(updatedStory.id),
        updatedStory
      );
      queryClient.invalidateQueries({
        queryKey: STORY_KEYS.byProject(projectId),
      });
    },
  });
};

export const useDeleteStory = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      storyId,
    }: {
      storyId: number;
      projectId: number;
    }) => storyApi.deleteStory(storyId),
    onSuccess: (_, { storyId, projectId }) => {
      queryClient.removeQueries({
        queryKey: STORY_KEYS.detail(storyId),
      });
      queryClient.invalidateQueries({
        queryKey: STORY_KEYS.byProject(projectId),
      });
      // Invalidate projects to update story count
      queryClient.invalidateQueries({ queryKey: ['projects'] });
    },
  });
};
