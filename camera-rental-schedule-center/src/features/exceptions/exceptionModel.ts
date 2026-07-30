import type { ExceptionItem } from '../../types';
import type { PresentationTone } from '../orders/orderModel';

export type ExceptionFilter = 'OPEN' | 'RESOLVED' | 'ALL';

export function filterExceptions(items: ExceptionItem[], filter: ExceptionFilter) {
  if (filter === 'OPEN') return items.filter((item) => !item.resolved);
  if (filter === 'RESOLVED') return items.filter((item) => item.resolved);
  return items;
}

export function exceptionSeverityTone(severity: ExceptionItem['severity']): PresentationTone {
  return severity === 'high' ? 'red' : severity === 'medium' ? 'amber' : 'neutral';
}

export function exceptionActions(
  item: ExceptionItem,
  permissions: { canResolve: boolean; canAssign: boolean; canViewDevice: boolean }
) {
  return {
    canResolve: !item.resolved && permissions.canResolve,
    canAssign: !item.resolved && Boolean(item.relatedOrderId) && permissions.canAssign,
    canViewDevice: Boolean(item.relatedDeviceId) && permissions.canViewDevice,
  };
}
