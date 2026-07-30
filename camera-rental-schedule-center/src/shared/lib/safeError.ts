export type SafeErrorCategory =
  | 'network'
  | 'authentication'
  | 'permission'
  | 'timeout'
  | 'partial'
  | 'unknown';

export function classifySafeError(message: string | null | undefined): SafeErrorCategory {
  const normalized = message?.toLowerCase() ?? '';
  if (
    normalized.includes('failed to fetch')
    || normalized.includes('networkerror')
    || normalized.includes('load failed')
    || normalized.includes('connection refused')
  ) {
    return 'network';
  }
  if (
    normalized.includes('auth_required')
    || normalized.includes('no_refresh_token')
    || normalized.includes('401')
    || normalized.includes('登录')
  ) {
    return 'authentication';
  }
  if (normalized.includes('403') || normalized.includes('无权') || normalized.includes('permission')) {
    return 'permission';
  }
  if (normalized.includes('timeout') || normalized.includes('超时')) {
    return 'timeout';
  }
  if (normalized.includes('partial_sync_failed')) {
    return 'partial';
  }
  return 'unknown';
}
