import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import { getCachedPermissionInfo } from '../../api/auth';
import { fetchPermissionInfo } from '../../api/rental';
import { isAuthenticationFailure } from '../../api/client';
import { useLatestRequest } from '../../shared/hooks/useLatestRequest';
import { useSession } from '../session/SessionContext';
import { hasScheduleCenterAccess, permissionAllows } from './permissionModel';

interface PermissionContextValue {
  permissions: string[];
  accessDenied: boolean;
  isPermissionLoading: boolean;
  permissionError: string | null;
  permissionRevision: number;
  hasPermission: (permission: string | string[]) => boolean;
  refreshPermissions: () => Promise<void>;
}

const PermissionContext = createContext<PermissionContextValue | null>(null);

interface PermissionProviderProps {
  children: ReactNode;
  loadPermissionInfo?: typeof fetchPermissionInfo;
}

export function PermissionProvider({
  children,
  loadPermissionInfo = fetchPermissionInfo,
}: PermissionProviderProps) {
  const {
    isLoggedIn,
    sessionRevision,
    requireAuthentication,
    updateCurrentUser,
  } = useSession();
  const [permissions, setPermissions] = useState(
    () => getCachedPermissionInfo()?.permissions || []
  );
  const [accessDenied, setAccessDenied] = useState(false);
  const [isPermissionLoading, setIsPermissionLoading] = useState(isLoggedIn);
  const [permissionError, setPermissionError] = useState<string | null>(null);
  const [permissionRevision, setPermissionRevision] = useState(0);
  const [permissionSessionRevision, setPermissionSessionRevision] =
    useState(sessionRevision);
  const beginRequest = useLatestRequest(sessionRevision);

  const refreshPermissions = useCallback(async () => {
    const isCurrent = beginRequest();
    if (!isLoggedIn) {
      setPermissions([]);
      setAccessDenied(false);
      setPermissionError(null);
      setIsPermissionLoading(false);
      setPermissionSessionRevision(sessionRevision);
      setPermissionRevision((value) => value + 1);
      return;
    }

    setIsPermissionLoading(true);
    setPermissionError(null);
    const cachedInfo = getCachedPermissionInfo();
    try {
      const info = await loadPermissionInfo();
      if (!isCurrent()) return;
      const nextPermissions = info.permissions || [];
      setPermissions(nextPermissions);
      setAccessDenied(!hasScheduleCenterAccess(nextPermissions));
      setPermissionSessionRevision(sessionRevision);
      updateCurrentUser(info.user);
    } catch (error) {
      if (!isCurrent()) return;
      setPermissionSessionRevision(sessionRevision);
      if (isAuthenticationFailure(error)) {
        requireAuthentication();
        setPermissions([]);
        setAccessDenied(false);
        setPermissionError('AUTH_REQUIRED');
      } else if (cachedInfo) {
        const nextPermissions = cachedInfo.permissions || [];
        setPermissions(nextPermissions);
        setAccessDenied(!hasScheduleCenterAccess(nextPermissions));
        updateCurrentUser(cachedInfo.user);
      } else {
        setPermissions([]);
        setAccessDenied(false);
        setPermissionError('PERMISSION_SYNC_FAILED');
      }
    } finally {
      if (isCurrent()) {
        setIsPermissionLoading(false);
        setPermissionRevision((value) => value + 1);
      }
    }
  }, [
    beginRequest,
    isLoggedIn,
    loadPermissionInfo,
    requireAuthentication,
    sessionRevision,
    updateCurrentUser,
  ]);

  useEffect(() => {
    void refreshPermissions();
  }, [refreshPermissions, sessionRevision]);

  const isCurrentSession = permissionSessionRevision === sessionRevision;
  const visiblePermissions = isCurrentSession ? permissions : [];

  return (
    <PermissionContext.Provider
      value={{
        permissions: visiblePermissions,
        accessDenied: isCurrentSession ? accessDenied : false,
        isPermissionLoading:
          isLoggedIn && (!isCurrentSession || isPermissionLoading),
        permissionError: isCurrentSession ? permissionError : null,
        permissionRevision,
        hasPermission: (permission) =>
          permissionAllows(visiblePermissions, permission),
        refreshPermissions,
      }}
    >
      {children}
    </PermissionContext.Provider>
  );
}

export function usePermissions() {
  const context = useContext(PermissionContext);
  if (!context) throw new Error('usePermissions must be used within PermissionProvider');
  return context;
}
