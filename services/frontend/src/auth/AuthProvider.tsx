'use client';

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import { apiClient, UNAUTHENTICATED_EVENT } from '@/api/client';

export type Role = 'ADMIN' | 'MANAGER' | 'INVESTOR' | 'EMPLOYEE';

/** Body of GET /api/auth/me. */
export interface SessionUser {
  id: number;
  username: string | null;
  email: string;
  emailVerified: boolean;
  name: string;
  role: Role;
  hasPassword: boolean;
}

interface SessionContext {
  user: SessionUser | null;
  isLoading: boolean;
  /** Reloads the current user from the API; returns it (null when signed out). */
  refresh: () => Promise<SessionUser | null>;
  logout: () => Promise<void>;
  /** For when the server session is already gone (e.g. "sign out every device"). */
  signedOut: () => void;
}

const Context = createContext<SessionContext | null>(null);

/** Pages reachable without being signed in. */
const PUBLIC_PATHS = ['/', '/login', '/verify-email', '/forgot-password', '/reset-password', '/auth/callback'];

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();
  const pathname = usePathname();

  const refresh = useCallback(async () => {
    try {
      const { data, response } = await apiClient.GET('/api/auth/me');
      const me = response.ok && data ? (data as unknown as SessionUser) : null;
      setUser(me);
      return me;
    } catch {
      setUser(null);
      return null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiClient.POST('/api/auth/logout');
    } finally {
      setUser(null);
      router.push('/login');
    }
  }, [router]);

  const signedOut = useCallback(() => {
    setUser(null);
    router.push('/login');
  }, [router]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  // Any 401 from the API: the session is over
  useEffect(() => {
    const onUnauthenticated = () => {
      setUser(null);
      if (!PUBLIC_PATHS.includes(window.location.pathname)) {
        router.push('/login');
      }
    };
    window.addEventListener(UNAUTHENTICATED_EVENT, onUnauthenticated);
    return () => window.removeEventListener(UNAUTHENTICATED_EVENT, onUnauthenticated);
  }, [router]);

  // Signed-out visitors are sent to the login page from any private page
  useEffect(() => {
    if (!isLoading && !user && !PUBLIC_PATHS.includes(pathname)) {
      router.replace('/login');
    }
  }, [isLoading, user, pathname, router]);

  const value = useMemo(() => ({ user, isLoading, refresh, logout, signedOut }),
    [user, isLoading, refresh, logout, signedOut]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}

export function useSession(): SessionContext {
  const context = useContext(Context);
  if (!context) {
    throw new Error('useSession must be used inside <AuthProvider>');
  }
  return context;
}

const USERNAME_SKIPPED_KEY = 'agrishare.usernameSkipped';

/** Where to go after signing in: the username prompt, unless it was postponed on this browser. */
export function afterLoginPath(user: SessionUser | null): string {
  if (!user || user.username) {
    return '/';
  }
  try {
    return window.localStorage.getItem(USERNAME_SKIPPED_KEY) === String(user.id) ? '/' : '/choose-username';
  } catch {
    return '/choose-username';
  }
}

/** "Plus tard" on the username prompt: do not ask again on this browser. */
export function postponeUsername(user: SessionUser) {
  try {
    window.localStorage.setItem(USERNAME_SKIPPED_KEY, String(user.id));
  } catch {
    // Storage unavailable (private mode...): the prompt will just come back
  }
}

export const ROLE_LABELS: Record<Role, string> = {
  ADMIN: 'Administrateur',
  MANAGER: 'Gestionnaire',
  INVESTOR: 'Investisseur',
  EMPLOYEE: 'Employé',
};
