export interface WalletResponse {
  id: number;
  tenantId: number;
  balance: number;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionResponse {
  id: number;
  type: TransactionType;
  amount: number;
  description: string | null;
  refType: string | null;
  refId: string | null;
  createdAt: string;
}

export interface TransactionPageResponse {
  content: TransactionResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminGrantRequest {
  amount: number;
  description?: string;
}

export interface FeatureCostResponse {
  costs: Record<string, number>;
  freeQuotas: Record<string, number>;
}

export interface QuotaStatusResponse {
  featureType: string;
  used: number;
  limit: number;
  quotaExceeded: boolean;
  yearMonth: string;
}

export type TransactionType = 'EARN' | 'SPEND' | 'REFUND' | 'PURCHASE';

export type RefType =
  | 'ADMIN_GRANT'
  | 'EXPORT'
  | 'CHAT_MESSAGE'
  | 'LINKEDIN_DRAFT'
  | 'LINKEDIN_SUGGESTION'
  | 'EXPORT_REFUND';

export interface TransactionQueryParams {
  page?: number;
  size?: number;
  type?: TransactionType;
  refType?: RefType;
  from?: string;
  to?: string;
}
