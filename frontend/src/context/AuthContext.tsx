import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import type { Klient, KlientCreate } from '../types/klient';
import { klienciService } from '../services/klienciService';

interface AuthValue {
  user: Klient | null;
  isAuthenticated: boolean;
  login: (email: string, haslo?: string) => Promise<Klient>;
  register: (data: KlientCreate) => Promise<Klient>;
  logout: () => void;
}

const AuthContext = createContext<AuthValue | null>(null);
const STORAGE_KEY = 'apteka_user';

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<Klient | null>(() => {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as Klient) : null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    if (user) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(STORAGE_KEY);
    }
  }, [user]);

  const value = useMemo<AuthValue>(
    () => ({
      user,
      isAuthenticated: user !== null,
      login: async (email, haslo) => {
        const loggedUser = await klienciService.login(email, haslo);
        setUser(loggedUser);
        return loggedUser;
      },
      register: async (data) => {
        const createdUser = await klienciService.create(data);
        setUser(createdUser);
        return createdUser;
      },
      logout: () => setUser(null),
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth musi być użyty wewnątrz AuthProvider');
  return ctx;
};
