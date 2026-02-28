import apiClient from './client';

export interface DeleteAccountResponse {
  message: string;
  deletedCounts: Record<string, number>;
}

export const deleteAccount = async (confirmation: string): Promise<DeleteAccountResponse> => {
  const response = await apiClient.delete<DeleteAccountResponse>('/v1/account', {
    data: { confirmation },
  });
  return response.data;
};
