const ACCESS_TOKEN_KEY = 'ACCESS_TOKEN';
const REFRESH_TOKEN_KEY = 'REFRESH_TOKEN';
const TENANT_ID_KEY = 'tenantId';
const VISIT_TENANT_ID_KEY = 'visitTenantId';
const USER_KEY = 'user';

type CacheItem = {
  c?: number;
  e?: number;
  v?: string;
};

export interface AdminUserCache {
  permissions?: string[];
  roles?: string[];
  user?: {
    id?: number;
    username?: string;
    nickname?: string;
    avatar?: string;
  };
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

function browserStorage() {
  if (typeof window === 'undefined') return null;
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

function readCacheValue<T>(key: string): T | undefined {
  const storage = browserStorage();
  if (!storage) return undefined;
  let raw: string | null = null;
  try {
    raw = storage.getItem(key);
  } catch {
    return undefined;
  }
  if (!raw) return undefined;

  try {
    const maybeCache = JSON.parse(raw) as CacheItem;
    if (maybeCache && typeof maybeCache === 'object' && 'v' in maybeCache) {
      if (maybeCache.e && Date.now() > maybeCache.e) return undefined;
      return maybeCache.v ? (JSON.parse(maybeCache.v) as T) : undefined;
    }
  } catch {
    return raw as T;
  }

  return raw as T;
}

function writeCacheValue(key: string, value: unknown) {
  const storage = browserStorage();
  if (!storage) return;
  const cacheItem = {
    c: Date.now(),
    e: new Date('9999-12-31T23:59:59.999Z').getTime(),
    v: JSON.stringify(value),
  };
  try {
    storage.setItem(key, JSON.stringify(cacheItem));
  } catch {
    // Authentication still works for the current page when storage is unavailable.
  }
}

function removeCacheValue(key: string) {
  const storage = browserStorage();
  if (!storage) return;
  try {
    storage.removeItem(key);
  } catch {
    // Treat unavailable storage as already cleared.
  }
}

export function getAccessToken() {
  return readCacheValue<string>(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  return readCacheValue<string>(REFRESH_TOKEN_KEY);
}

export function setTokenPair(token: TokenPair) {
  writeCacheValue(ACCESS_TOKEN_KEY, token.accessToken);
  writeCacheValue(REFRESH_TOKEN_KEY, token.refreshToken);
}

export function removeTokenPair() {
  removeCacheValue(ACCESS_TOKEN_KEY);
  removeCacheValue(REFRESH_TOKEN_KEY);
}

export function getTenantId() {
  return readCacheValue<number | string>(TENANT_ID_KEY);
}

export function setTenantId(tenantId: number | string) {
  writeCacheValue(TENANT_ID_KEY, tenantId);
}

export function getVisitTenantId() {
  return readCacheValue<number | string>(VISIT_TENANT_ID_KEY);
}

export function getAdminUser() {
  return readCacheValue<AdminUserCache>(USER_KEY)?.user;
}

export function getCachedPermissionInfo() {
  return readCacheValue<AdminUserCache>(USER_KEY);
}

export function setCachedPermissionInfo(info: AdminUserCache) {
  writeCacheValue(USER_KEY, info);
}

export function clearCachedPermissionInfo() {
  removeCacheValue(USER_KEY);
}

export function buildAdminLoginUrl() {
  const loginPath = import.meta.env.VITE_ADMIN_LOGIN_PATH || '/admin/login';
  const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  const url = new URL(loginPath, window.location.origin);
  url.searchParams.set('redirect', redirect);
  return url.toString();
}
