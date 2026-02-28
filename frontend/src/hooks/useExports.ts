import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { exportsApi } from '../api/exportsApi';
import type { ExportResumeRequest, ExportCoverLetterRequest } from '../types/export';

const EXPORT_KEYS = {
  all: ['exports'] as const,
  history: (page: number, size: number, type?: string, status?: string) =>
    [...EXPORT_KEYS.all, 'history', { page, size, type, status }] as const,
  status: (exportId: number | null) =>
    [...EXPORT_KEYS.all, 'status', exportId] as const,
  templates: () => [...EXPORT_KEYS.all, 'templates'] as const,
};

export const useExportTemplates = () => {
  return useQuery({
    queryKey: EXPORT_KEYS.templates(),
    queryFn: exportsApi.getExportTemplates,
    staleTime: 5 * 60 * 1000,
  });
};

export const useExportHistory = (
  page = 0,
  size = 20,
  type?: string,
  status?: string
) => {
  return useQuery({
    queryKey: EXPORT_KEYS.history(page, size, type, status),
    queryFn: () => exportsApi.getExportHistory(page, size, type, status),
    staleTime: 10 * 1000,
  });
};

export const useExportStatus = (exportId: number | null) => {
  return useQuery({
    queryKey: EXPORT_KEYS.status(exportId),
    queryFn: () => exportsApi.getExportStatus(exportId!),
    enabled: !!exportId,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'COMPLETED' || status === 'FAILED') return false;
      return 3000;
    },
  });
};

export const useCreateResumeExport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ExportResumeRequest) =>
      exportsApi.createResumeExport(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: EXPORT_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ['billing'] });
    },
  });
};

export const useCreateCoverLetterExport = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: ExportCoverLetterRequest) =>
      exportsApi.createCoverLetterExport(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: EXPORT_KEYS.all });
      queryClient.invalidateQueries({ queryKey: ['billing'] });
    },
  });
};
