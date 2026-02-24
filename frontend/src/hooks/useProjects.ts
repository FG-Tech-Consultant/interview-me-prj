import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { projectApi } from '../api/experienceApi';
import type {
  ProjectResponse,
  CreateProjectRequest,
  UpdateProjectRequest,
} from '../types/experienceProject';

const PROJECT_KEYS = {
  all: ['projects'] as const,
  byJob: (jobId: number) => [...PROJECT_KEYS.all, jobId] as const,
  detail: (projectId: number) => [...PROJECT_KEYS.all, 'detail', projectId] as const,
};

export const useProjects = (jobId: number) => {
  return useQuery({
    queryKey: PROJECT_KEYS.byJob(jobId),
    queryFn: () => projectApi.getProjectsByJob(jobId),
    enabled: !!jobId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useProject = (projectId: number) => {
  return useQuery({
    queryKey: PROJECT_KEYS.detail(projectId),
    queryFn: () => projectApi.getProject(projectId),
    enabled: !!projectId,
    staleTime: 5 * 60 * 1000,
  });
};

export const useCreateProject = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      jobId,
      data,
    }: {
      jobId: number;
      data: CreateProjectRequest;
    }) => projectApi.createProject(jobId, data),
    onSuccess: (newProject: ProjectResponse, { jobId }) => {
      queryClient.invalidateQueries({
        queryKey: PROJECT_KEYS.byJob(jobId),
      });
      queryClient.setQueryData(
        PROJECT_KEYS.detail(newProject.id),
        newProject
      );
    },
  });
};

export const useUpdateProject = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      projectId,
      data,
    }: {
      projectId: number;
      jobId: number;
      data: UpdateProjectRequest;
    }) => projectApi.updateProject(projectId, data),
    onSuccess: (updatedProject: ProjectResponse, { jobId }) => {
      queryClient.setQueryData(
        PROJECT_KEYS.detail(updatedProject.id),
        updatedProject
      );
      queryClient.invalidateQueries({
        queryKey: PROJECT_KEYS.byJob(jobId),
      });
    },
  });
};

export const useDeleteProject = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      projectId,
    }: {
      projectId: number;
      jobId: number;
    }) => projectApi.deleteProject(projectId),
    onSuccess: (_, { projectId, jobId }) => {
      queryClient.removeQueries({
        queryKey: PROJECT_KEYS.detail(projectId),
      });
      queryClient.invalidateQueries({
        queryKey: PROJECT_KEYS.byJob(jobId),
      });
    },
  });
};
