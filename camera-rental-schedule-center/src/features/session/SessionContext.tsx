import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

import {
  clearCachedPermissionInfo,
  getAccessToken,
  getAdminUser,
  removeTokenPair,
} from '../../api/auth';
import { loginWithAdminPassword, logoutFromAdmin } from '../../api/client';
import { createSessionState, resetSessionState } from './sessionModel';

export interface SessionUser {
  id?: number;
  username?: string;
  nickname?: string;
  avatar?: string;
}

interface LoginParams {
  tenantName?: string;
  username: string;
  password: string;
  rememberMe?: boolean;
}

interface SessionContextValue {
  currentUser?: SessionUser;
  isLoggedIn: boolean;
  authRequired: boolean;
  isSessionPending: boolean;
  sessionRevision: number;
  login: (params: LoginParams) => Promise<void>;
  logout: () => Promise<void>;
  requireAuthentication: () => void;
  updateCurrentUser: (user?: SessionUser) => void;
}

const SessionContext = createContext<SessionContextValue | null>(null);

interface SessionProviderProps {
  children: ReactNode;
  loginService?: typeof loginWithAdminPassword;
  logoutService?: typeof logoutFromAdmin;
}

export function SessionProvider({
  children,
  loginService = loginWithAdminPassword,
  logoutService = logoutFromAdmin,
}: SessionProviderProps) {
  const [session, setSession] = useState(() =>
    createSessionState(getAccessToken(), getAdminUser())
  );
  const [isSessionPending, setIsSessionPending] = useState(false);
  const [sessionRevision, setSessionRevision] = useState(0);

  const login = useCallback(async (params: LoginParams) => {
    setIsSessionPending(true);
    try {
      await loginService(params);
      clearCachedPermissionInfo();
      setSession(createSessionState(getAccessToken()));
      setSessionRevision((value) => value + 1);
    } catch (error) {
      removeTokenPair();
      clearCachedPermissionInfo();
      setSession(resetSessionState());
      setSessionRevision((value) => value + 1);
      throw error;
    } finally {
      setIsSessionPending(false);
    }
  }, [loginService]);

  const logout = useCallback(async () => {
    setIsSessionPending(true);
    try {
      await logoutService();
    } finally {
      setSession(resetSessionState());
      setSessionRevision((value) => value + 1);
      setIsSessionPending(false);
    }
  }, [logoutService]);

  const requireAuthentication = useCallback(() => {
    setSession(resetSessionState());
    setSessionRevision((value) => value + 1);
  }, []);

  const updateCurrentUser = useCallback((currentUser?: SessionUser) => {
    setSession((value) => ({ ...value, currentUser }));
  }, []);

  return (
    <SessionContext.Provider
      value={{
        ...session,
        isSessionPending,
        sessionRevision,
        login,
        logout,
        requireAuthentication,
        updateCurrentUser,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
}

export function useSession() {
  const context = useContext(SessionContext);
  if (!context) throw new Error('useSession must be used within SessionProvider');
  return context;
}
