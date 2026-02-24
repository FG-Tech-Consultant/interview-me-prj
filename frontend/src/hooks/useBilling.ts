import { useQuery, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '../api/billingApi';
import type { TransactionQueryParams } from '../types/billing';

const BILLING_KEYS = {
  all: ['billing'] as const,
  wallet: () => [...BILLING_KEYS.all, 'wallet'] as const,
  transactions: (params: TransactionQueryParams) =>
    [...BILLING_KEYS.all, 'transactions', params] as const,
  costs: () => [...BILLING_KEYS.all, 'costs'] as const,
  quota: (featureType: string) =>
    [...BILLING_KEYS.all, 'quota', featureType] as const,
};

export const useWallet = () => {
  return useQuery({
    queryKey: BILLING_KEYS.wallet(),
    queryFn: billingApi.getWallet,
    staleTime: 30 * 1000, // 30 seconds
  });
};

export const useTransactions = (params: TransactionQueryParams = {}) => {
  return useQuery({
    queryKey: BILLING_KEYS.transactions(params),
    queryFn: () => billingApi.getTransactions(params),
    staleTime: 30 * 1000,
  });
};

export const useFeatureCosts = () => {
  return useQuery({
    queryKey: BILLING_KEYS.costs(),
    queryFn: billingApi.getFeatureCosts,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
};

export const useQuotaStatus = (featureType: string) => {
  return useQuery({
    queryKey: BILLING_KEYS.quota(featureType),
    queryFn: () => billingApi.getQuotaStatus(featureType),
    enabled: !!featureType,
    staleTime: 30 * 1000,
  });
};

export const useInvalidateWallet = () => {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: BILLING_KEYS.wallet() });
    queryClient.invalidateQueries({
      queryKey: [...BILLING_KEYS.all, 'transactions'],
    });
  };
};
