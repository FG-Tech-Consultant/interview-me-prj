export interface CompanyProfile {
  id: number;
  name: string;
  cnpj: string | null;
  website: string | null;
  sector: string | null;
  size: string | null;
  description: string | null;
  logoUrl: string | null;
  country: string | null;
  city: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CompanyRegistrationRequest {
  companyName: string;
  sector?: string;
  size?: string;
  website?: string;
  country?: string;
  city?: string;
  description?: string;
  adminName: string;
  email: string;
  password: string;
}

export interface CompanyRegistrationResponse {
  companyId: number;
  token: string;
  message: string;
}

export interface CompanyUpdateRequest {
  name: string;
  cnpj?: string;
  website?: string;
  sector?: string;
  size?: string;
  description?: string;
  logoUrl?: string;
  country?: string;
  city?: string;
}

export interface CompanyDashboard {
  companyName: string;
  activeJobs: number;
  totalCandidates: number;
  profileViews: number;
}
