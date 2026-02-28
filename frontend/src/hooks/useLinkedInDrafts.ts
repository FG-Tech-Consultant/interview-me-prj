import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { linkedinDraftApi } from '../api/linkedinDraftApi';
import type { CreateDraftRequest, UpdateDraftStatusRequest } from '../types/linkedinDraft';

const DRAFT_KEYS = {
  all: ['linkedin-drafts'] as const,
  list: (page: number) => [...DRAFT_KEYS.all, 'list', page] as const,
  detail: (id: number) => [...DRAFT_KEYS.all, 'detail', id] as const,
};

export const useLinkedInDrafts = (page = 0) => {
  return useQuery({
    queryKey: DRAFT_KEYS.list(page),
    queryFn: () => linkedinDraftApi.listDrafts(page),
    staleTime: 30 * 1000,
  });
};

export const useLinkedInDraft = (id: number | null) => {
  return useQuery({
    queryKey: DRAFT_KEYS.detail(id ?? 0),
    queryFn: () => linkedinDraftApi.getDraft(id!),
    enabled: id != null && id > 0,
  });
};

export const useCreateLinkedInDraft = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: CreateDraftRequest) => linkedinDraftApi.createDraft(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: DRAFT_KEYS.all });
    },
  });
};

export const useDeleteLinkedInDraft = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => linkedinDraftApi.deleteDraft(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: DRAFT_KEYS.all });
    },
  });
};

export const useUpdateDraftStatus = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, request }: { id: number; request: UpdateDraftStatusRequest }) =>
      linkedinDraftApi.updateStatus(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: DRAFT_KEYS.all });
    },
  });
};
