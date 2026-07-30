import type { WorkspaceTab } from './navigation';
import type { SnapshotAccess } from '../api/rental';

const TAB_PERMISSIONS: Partial<Record<WorkspaceTab, string>> = {
  schedule: 'rental:schedule:query',
  orders: 'rental:xianyu:query',
  devices: 'rental:device:query',
  binding: 'rental:xianyu:ship',
  exceptions: 'rental:review:query',
};

export function hasGrantedPermission(permissions: string[], permission: string) {
  return permissions.includes('*:*:*') || permissions.includes(permission);
}

export function canAccessTab(permissions: string[], tab: WorkspaceTab) {
  const required = TAB_PERMISSIONS[tab];
  return !required || hasGrantedPermission(permissions, required);
}

export function permittedTabs(permissions: string[], tabs: WorkspaceTab[]) {
  return tabs.filter((tab) => canAccessTab(permissions, tab));
}

export function buildSnapshotAccess(permissions: string[]): SnapshotAccess {
  const orders = hasGrantedPermission(permissions, 'rental:xianyu:query');
  const pendingShipOrders = hasGrantedPermission(permissions, 'rental:xianyu:ship');
  return {
    devices: hasGrantedPermission(permissions, 'rental:device:query'),
    schedules: hasGrantedPermission(permissions, 'rental:schedule:query'),
    orders,
    pendingShipOrders,
    reviews: hasGrantedPermission(permissions, 'rental:review:query'),
    xianyuConfig: orders,
  };
}
