import apiClient from './client';
import type {
  WalletResponse,
  TransactionPageResponse,
  FeatureCostResponse,
  QuotaStatusResponse,
  TransactionQueryParams,
} from '../types/billing';

export const billingApi = {
  getWallet: async (): Promise<WalletResponse> => {
    const response = await apiClient.get<WalletResponse>('/billing/wallet');
    return response.data;
  },

  getTransactions: async (
    params: TransactionQueryParams = {}
  ): Promise<TransactionPageResponse> => {
    const response = await apiClient.get<TransactionPageResponse>(
      '/billing/transactions',
      { params }
    );
    return response.data;
  },

  getFeatureCosts: async (): Promise<FeatureCostResponse> => {
    const response = await apiClient.get<FeatureCostResponse>('/billing/costs');
    return response.data;
  },

  getQuotaStatus: async (featureType: string): Promise<QuotaStatusResponse> => {
    const response = await apiClient.get<QuotaStatusResponse>(
      `/billing/quota/${featureType}`
    );
    return response.data;
  },
};
