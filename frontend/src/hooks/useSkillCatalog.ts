import { useQuery } from '@tanstack/react-query';
import { skillsApi } from '../api/skillsApi';
import { useState, useEffect } from 'react';

const CATALOG_KEYS = {
  all: ['skillCatalog'] as const,
  search: (query: string) => [...CATALOG_KEYS.all, 'search', query] as const,
};

export const useSkillCatalogSearch = (query: string) => {
  const [debouncedQuery, setDebouncedQuery] = useState(query);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query);
    }, 300);
    return () => clearTimeout(timer);
  }, [query]);

  return useQuery({
    queryKey: CATALOG_KEYS.search(debouncedQuery),
    queryFn: () => skillsApi.searchCatalog(debouncedQuery),
    enabled: debouncedQuery.length >= 2,
    staleTime: 30 * 1000,
  });
};
