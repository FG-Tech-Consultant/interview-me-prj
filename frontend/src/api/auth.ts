import apiClient from './client';

export interface RegisterRequest {
  email: string;
  password: string;
  tenantName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  email: string;
  tenantId: number;
}

export interface UserInfoResponse {
  id: number;
  email: string;
  tenantId: number;
  role: string;
  createdAt: string;
}

export const register = async (data: RegisterRequest): Promise<AuthResponse> => {
  const response = await apiClient.post<AuthResponse>('/auth/register', data);
  localStorage.setItem('token', response.data.token);
  return response.data;
};

export const login = async (data: LoginRequest): Promise<AuthResponse> => {
  const response = await apiClient.post<AuthResponse>('/auth/login', data);
  localStorage.setItem('token', response.data.token);
  return response.data;
};

export const getCurrentUser = async (): Promise<UserInfoResponse> => {
  const response = await apiClient.get<UserInfoResponse>('/auth/me');
  return response.data;
};

export const logout = () => {
  localStorage.removeItem('token');
  window.location.href = `${import.meta.env.BASE_URL}login`;
};
