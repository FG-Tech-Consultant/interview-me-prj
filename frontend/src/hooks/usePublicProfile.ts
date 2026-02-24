import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { publicProfileApi } from '../api/publicProfileApi';
import { useState, useEffect } from 'react';

const PUBLIC_PROFILE_KEYS = {
  all: ['publicProfile'] as const,
  detail: (slug: string) => [...PUBLIC_PROFILE_KEYS.all, slug] as const,
  slugCheck: (slug: string) => ['slugCheck', slug] as const,
};

export const usePublicProfile = (slug: string) => {
  return useQuery({
    queryKey: PUBLIC_PROFILE_KEYS.detail(slug),
    queryFn: () => publicProfileApi.getPublicProfile(slug),
    enabled: !!slug,
    staleTime: 60 * 1000, // 1 minute cache
    retry: false,
  });
};

export const useCheckSlug = (slug: string) => {
  const [debouncedSlug, setDebouncedSlug] = useState(slug);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSlug(slug);
    }, 500);
    return () => clearTimeout(timer);
  }, [slug]);

  return useQuery({
    queryKey: PUBLIC_PROFILE_KEYS.slugCheck(debouncedSlug),
    queryFn: () => publicProfileApi.checkSlugAvailability(debouncedSlug),
    enabled: debouncedSlug.length >= 3,
    staleTime: 30 * 1000,
  });
};

export const useUpdateSlug = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ profileId, slug }: { profileId: number; slug: string }) =>
      publicProfileApi.updateSlug(profileId, slug),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profiles'] });
    },
  });
};
