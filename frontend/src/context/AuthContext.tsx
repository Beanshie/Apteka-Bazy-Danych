import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { AuthLogin, AuthUser } from '../types/auth';
import type { KlientCreate } from '../types/klient';
import { authService } from '../services/authService';
import { klienciService } from '../services/klienciService';

interface AuthValue {
  user: AuthUser | null;
  loading: boolean;
  isAuthenticated: boolean;
  isKlient: boolean;
  login: (payload: AuthLogin) => Promise<AuthUser>;
  register: (data: KlientCreate) => Promise<AuthUser>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthValue | null>(null);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    authService
      .me()
      .then((me) => {
        if (active) setUser(me);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const value = useMemo<AuthValue>(
    () => ({
      user,
      loading,
      isAuthenticated: user !== null,
      isKlient: user?.typ_konta === 'KLIENT',
      login: async (payload) => {
        const loggedUser = await authService.login(payload);
        setUser(loggedUser);
        return loggedUser;
      },
      register: async (data) => {
        await klienciService.create(data);
        const loggedUser = await authService.login({
          typ_konta: 'KLIENT',
          identyfikator: data.email,
          haslo: data.haslo,
        });
        setUser(loggedUser);
        return loggedUser;
      },
      logout: async () => {
        try {
          await authService.logout();
        } finally {
          setUser(null);
        }
      },
    }),
    [user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth musi być użyty wewnątrz AuthProvider');
  return ctx;
};
