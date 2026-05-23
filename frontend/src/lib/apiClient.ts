import axios, { AxiosError, AxiosInstance } from 'axios';
import type { ApiResponse } from '@/types';

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8090';

function createApiClient(): AxiosInstance {
  const client = axios.create({
    baseURL: BASE_URL,
    headers: { 'Content-Type': 'application/json' },
    timeout: 30000,
  });

  // Attach access token to every request
  client.interceptors.request.use((config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('access_token');
      if (token) config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  // Handle 401 — refresh token or redirect to login
  client.interceptors.response.use(
    (res) => res,
    async (error: AxiosError) => {
      if (error.response?.status === 401 && typeof window !== 'undefined') {
        localStorage.removeItem('access_token');
        window.location.href = '/auth/login';
      }
      return Promise.reject(error);
    }
  );

  return client;
}

const axiosClient = createApiClient();

export const apiClient = {
  async get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    const res = await axiosClient.get<ApiResponse<T>>(url, { params });
    return res.data.data;
  },
  async post<T>(url: string, body?: unknown): Promise<T> {
    const res = await axiosClient.post<ApiResponse<T>>(url, body);
    return res.data.data;
  },
  async put<T>(url: string, body?: unknown): Promise<T> {
    const res = await axiosClient.put<ApiResponse<T>>(url, body);
    return res.data.data;
  },
  async delete<T>(url: string): Promise<T> {
    const res = await axiosClient.delete<ApiResponse<T>>(url);
    return res.data.data;
  },
};
