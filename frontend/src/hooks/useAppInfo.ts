import { useQuery } from '@tanstack/react-query';
import { getAppInfo } from '../api/infoApi';

export function useAppInfo() {
  return useQuery({
    queryKey: ['appInfo'],
    queryFn: getAppInfo,
    staleTime: Infinity,
  });
}
