import axios, { AxiosError } from 'axios';
import type { InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse, AuthResponse } from '../types';

const normalizeApiBaseUrl = (rawUrl?: string): string => {
  let url = (rawUrl || 'http://localhost:8080/api/v1').trim();
  // Strip trailing slashes
  url = url.replace(/\/+$/, '');
  // If the user forgot /api/v1, append it automatically
  if (!url.endsWith('/api/v1')) {
    url = `${url}/api/v1`;
  }
  return url;
};

const API_BASE_URL = normalizeApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Do not attach token for public auth endpoints (login and register)
    const isAuthRoute = config.url?.includes('/auth/login') || config.url?.includes('/auth/register');
    if (!isAuthRoute) {
      const token = localStorage.getItem('accessToken');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: AxiosError | null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve();
    }
  });
  failedQueue = [];
};

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/auth/')) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => apiClient(originalRequest))
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('auth:logout'));
        isRefreshing = false;
        return Promise.reject(error);
      }

      try {
        const response = await axios.post<ApiResponse<AuthResponse>>(
          `${API_BASE_URL}/auth/refresh`,
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } }
        );

        const newAuth = response.data.data;
        localStorage.setItem('accessToken', newAuth.accessToken);
        localStorage.setItem('refreshToken', newAuth.refreshToken);

        if (originalRequest.headers) {
          originalRequest.headers.Authorization = `Bearer ${newAuth.accessToken}`;
        }

        processQueue(null);
        return apiClient(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr as AxiosError);
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('auth:logout'));
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
