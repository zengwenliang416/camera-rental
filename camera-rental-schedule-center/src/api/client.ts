import {
  buildAdminLoginUrl,
  clearCachedPermissionInfo,
  getAccessToken,
  getRefreshToken,
  getTenantId,
  getVisitTenantId,
  removeTokenPair,
  setTenantId,
  setTokenPair,
} from './auth';

interface ApiEnvelope<T> {
  code: number;
  msg?: string;
  data: T;
}

export class ApiError extends Error {
  code?: number;
  status?: number;

  constructor(message: string, code?: number, status?: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

export interface PageResult<T> {
  list: T[];
  total: number;
}

const apiBase = `${import.meta.env.VITE_BASE_URL || window.location.origin}${import.meta.env.VITE_API_URL || '/admin-api'}`;
const tenantEnabled = import.meta.env.VITE_APP_TENANT_ENABLE !== 'false';
const defaultTenantName = import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '芋道源码';
let refreshing: Promise<void> | null = null;

interface AdminPasswordLoginParams {
  tenantName?: string;
  username: string;
  password: string;
  rememberMe?: boolean;
}

function appendParams(url: URL, params?: Record<string, unknown>) {
  if (!params) return;
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    url.searchParams.set(key, String(value));
  });
}

async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error('NO_REFRESH_TOKEN');
  const tenantId = getTenantId();
  const url = new URL(`${apiBase}/system/auth/refresh-token`);
  url.searchParams.set('refreshToken', refreshToken);
  const headers: HeadersInit = {};
  if (tenantId) headers['tenant-id'] = String(tenantId);
  const res = await fetch(url, { method: 'POST', headers });
  const json = (await res.json()) as ApiEnvelope<{ accessToken: string; refreshToken: string }>;
  if (json.code !== 0 && json.code !== 200) {
    throw new Error(json.msg || '刷新登录态失败');
  }
  setTokenPair(json.data);
}

function authHeaders(withJsonContentType = true): HeadersInit {
  const headers: HeadersInit = {};
  if (withJsonContentType) headers['Content-Type'] = 'application/json';
  const accessToken = getAccessToken();
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const tenantId = getTenantId();
  const visitTenantId = getVisitTenantId();
  if (tenantId) headers['tenant-id'] = String(tenantId);
  if (accessToken && visitTenantId) headers['visit-tenant-id'] = String(visitTenantId);
  return headers;
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  if (!getAccessToken()) {
    throw new Error('AUTH_REQUIRED');
  }
  const res = await fetch(`${apiBase}${path}`, {
    ...init,
    headers: {
      ...authHeaders(!(init.body instanceof FormData)),
      ...(init.headers || {}),
    },
  });
  const json = (await res.json().catch(() => ({}))) as ApiEnvelope<T>;
  if ((json.code === 401 || res.status === 401) && retry) {
    refreshing ||= refreshAccessToken().finally(() => {
      refreshing = null;
    });
    try {
      await refreshing;
    } catch {
      removeTokenPair();
      clearCachedPermissionInfo();
      throw new Error('AUTH_REQUIRED');
    }
    return request<T>(path, init, false);
  }
  if (json.code === 403 || res.status === 403) {
    throw new ApiError(json.msg || '无权限执行该操作', json.code, res.status);
  }
  if (json.code !== 0 && json.code !== 200) {
    throw new ApiError(json.msg || `接口请求失败: ${path}`, json.code, res.status);
  }
  return json.data;
}

async function publicRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`${apiBase}${path}`, {
    ...init,
    headers: {
      ...authHeaders(!(init.body instanceof FormData)),
      ...(init.headers || {}),
    },
  });
  const json = (await res.json().catch(() => ({}))) as ApiEnvelope<T>;
  if (json.code !== 0 && json.code !== 200) {
    throw new ApiError(json.msg || `接口请求失败: ${path}`, json.code, res.status);
  }
  return json.data;
}

export function redirectToAdminLogin() {
  window.location.href = buildAdminLoginUrl();
}

export async function loginWithAdminPassword(params: AdminPasswordLoginParams) {
  let tenantName = params.tenantName?.trim();
  let tenantId = getTenantId();
  if (tenantEnabled) {
    if (!tenantName && !tenantId) {
      tenantName = defaultTenantName;
    }
    if (tenantName) {
      tenantId = await publicRequest<number>(
        `/system/tenant/get-id-by-name?name=${encodeURIComponent(tenantName)}`
      );
    }
    if (!tenantId) {
      throw new ApiError('请输入租户名称或先在管理后台登录');
    }
    setTenantId(tenantId);
  }

  const token = await publicRequest<{ accessToken: string; refreshToken: string }>('/system/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      isEncrypt: 'false',
    },
    body: JSON.stringify({
      username: params.username,
      password: params.password,
      captchaVerification: '',
      rememberMe: Boolean(params.rememberMe),
      tenantName: tenantName || undefined,
    }),
  });
  setTokenPair(token);
  return token;
}

export async function logoutFromAdmin() {
  try {
    if (getAccessToken()) {
      await request('/system/auth/logout', { method: 'POST' }, false);
    }
  } finally {
    removeTokenPair();
    clearCachedPermissionInfo();
  }
}

export const apiClient = {
  get<T>(path: string, params?: Record<string, unknown>) {
    const url = new URL(path, 'http://local');
    appendParams(url, params);
    return request<T>(`${url.pathname}${url.search}`);
  },
  post<T>(path: string, body?: unknown) {
    return request<T>(path, {
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  },
  put<T>(path: string, body?: unknown) {
    return request<T>(path, {
      method: 'PUT',
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  },
  upload<T>(path: string, file: File, fieldName = 'file') {
    const data = new FormData();
    data.append(fieldName, file);
    return request<T>(path, {
      method: 'POST',
      body: data,
    });
  },
};
