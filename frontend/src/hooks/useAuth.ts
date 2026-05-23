'use client';
import { useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { apiClient } from '@/lib/apiClient';
import { useAuthStore } from '@/stores/authStore';
import type { User, AuthTokens } from '@/types';

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

interface GoogleLoginRequest {
  idToken: string;
}

interface RefreshTokenRequest {
  refreshToken: string;
}

interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

async function fetchCurrentUser(token: string): Promise<User> {
  // Store token temporarily so apiClient picks it up
  if (typeof window !== 'undefined') {
    localStorage.setItem('access_token', token);
  }
  return apiClient.get<User>('/api/v1/users/me');
}

export function useLogin() {
  const { setAuth } = useAuthStore();
  const router = useRouter();

  return useMutation<AuthResponse, Error, LoginRequest>({
    mutationFn: (data) => apiClient.post<AuthResponse>('/api/v1/auth/login', data),
    onSuccess: async (data) => {
      let user = data.user;
      if (!user) {
        user = await fetchCurrentUser(data.accessToken);
      }
      setAuth(user, {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        expiresIn: data.expiresIn,
      });
      router.push('/dashboard');
    },
  });
}

export function useRegister() {
  const router = useRouter();

  return useMutation<AuthResponse, Error, RegisterRequest>({
    mutationFn: (data) => apiClient.post<AuthResponse>('/api/v1/auth/register', data),
    onSuccess: () => {
      router.push('/auth/login');
    },
  });
}

export function useLogout() {
  const { clearAuth } = useAuthStore();
  const router = useRouter();

  return useMutation<void, Error, void>({
    mutationFn: async () => {
      // Best-effort: ignore server errors on logout
      try {
        await apiClient.post<void>('/api/v1/auth/logout');
      } catch {
        // Intentionally swallowed
      }
    },
    onSettled: () => {
      clearAuth();
      router.push('/');
    },
  });
}

export function useGoogleLogin() {
  const { setAuth } = useAuthStore();
  const router = useRouter();

  return useMutation<AuthResponse, Error, GoogleLoginRequest>({
    mutationFn: (data) => apiClient.post<AuthResponse>('/api/v1/auth/oauth2/google', data),
    onSuccess: async (data) => {
      let user = data.user;
      if (!user) {
        user = await fetchCurrentUser(data.accessToken);
      }
      setAuth(user, {
        accessToken: data.accessToken,
        refreshToken: data.refreshToken,
        expiresIn: data.expiresIn,
      });
      router.push('/dashboard');
    },
  });
}

export function useRefreshToken() {
  const { setAuth, user } = useAuthStore();

  return useMutation<AuthTokens, Error, RefreshTokenRequest>({
    mutationFn: (data) => apiClient.post<AuthTokens>('/api/v1/auth/refresh-token', data),
    onSuccess: (tokens) => {
      if (user) {
        setAuth(user, tokens);
      }
    },
  });
}
