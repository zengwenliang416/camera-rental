import type { XianyuConfigVO } from '../../api/rental';

export function shipmentGuard(
  hasPermission: boolean,
  config: XianyuConfigVO | null,
  canInspectConfig = true
) {
  if (!hasPermission) {
    return '当前账号缺少 rental:xianyu:ship，不能执行闲管家真实发货。';
  }
  if (!config && canInspectConfig) {
    return '未能读取闲管家写配置，不能执行真实发货。';
  }
  if (!config) return null;
  if (config.enabled === false || config.status === 'DISABLED') {
    return '服务器未启用闲管家集成，不能执行真实发货。';
  }
  if (config.status === 'MISSING_CREDENTIALS') {
    return '服务器缺少闲管家应用凭据，不能执行真实发货。';
  }
  if (config.writeEnabled === false) {
    return '当前租户已关闭闲管家写操作，请到管理端“闲管家配置”中开启。';
  }
  return null;
}

export function canStartCommand(
  pendingKeys: Pick<ReadonlySet<string>, 'has'>,
  key: string
) {
  return !pendingKeys.has(key);
}
