import axios from 'axios';
import type { AuthCsrf, AuthLogin, AuthUser } from '../types/auth';

const authClient = axios.create({
  baseURL: '/api/v1/auth',
  withCredentials: true,
});

export const authService = {
  getCsrf: async (): Promise<AuthCsrf> => {
    const response = await authClient.get<AuthCsrf>('/csrf');
    return response.data;
  },

  login: async (payload: AuthLogin): Promise<AuthUser> => {
    const csrf = await authService.getCsrf();
    const response = await authClient.post<AuthUser>('/login', payload, {
      headers: {
        [csrf.headerName]: csrf.token,
      },
    });
    return response.data;
  },

  me: async (): Promise<AuthUser> => {
    const response = await authClient.get<AuthUser>('/me');
    return response.data;
  },

  logout: async (): Promise<void> => {
    const csrf = await authService.getCsrf();
    await authClient.post('/logout', undefined, {
      headers: {
        [csrf.headerName]: csrf.token,
      },
    });
  },
};
