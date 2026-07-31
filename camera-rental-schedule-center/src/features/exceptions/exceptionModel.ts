import type { ExceptionItem } from '../../types';
import type { PresentationTone } from '../orders/orderModel';
import type { DeliveryOrderSummary } from '../tracking/trackingModel';

export type ExceptionFilter = 'OPEN' | 'RESOLVED' | 'ALL';

export interface ExceptionListItem {
  id: string;
  kind: 'manual' | 'tracking';
  title: string;
  description: string;
  relatedOrderId?: string;
  relatedDeviceId?: string;
  severity: 'high' | 'medium' | 'low';
  createdTime: string;
  resolved: boolean;
  trackingRiskCode?: string;
}

function trackingCreatedTime(summary: DeliveryOrderSummary) {
  const latestPackage = [...summary.packages]
    .sort((left, right) => (right.latestEventTime || right.lastSyncedAt || '').localeCompare(left.latestEventTime || left.lastSyncedAt || ''))[0];
  return latestPackage?.latestEventTime || latestPackage?.lastSyncedAt || '';
}

export function mergeExceptionItems(
  manualItems: ExceptionItem[],
  trackingSummaries: DeliveryOrderSummary[]
): ExceptionListItem[] {
  const manual = manualItems.map<ExceptionListItem>((item) => ({
    ...item,
    kind: 'manual',
  }));
  const tracking = trackingSummaries.flatMap<ExceptionListItem>((summary) =>
    summary.risks.map((risk) => ({
      id: `tracking-${summary.rentalOrderId}-${risk.code}-${(risk.deviceIds || []).join('-')}`,
      kind: 'tracking',
      title: risk.code,
      description: risk.safeMessage,
      relatedOrderId: String(summary.rentalOrderId),
      relatedDeviceId:
        risk.deviceIds && risk.deviceIds.length === 1
          ? String(risk.deviceIds[0])
          : undefined,
      severity: risk.severity,
      createdTime: trackingCreatedTime(summary),
      resolved: false,
      trackingRiskCode: risk.code,
    }))
  );

  return [...manual, ...tracking].sort((left, right) =>
    (right.createdTime || '').localeCompare(left.createdTime || '')
  );
}

export function filterExceptions(items: ExceptionListItem[], filter: ExceptionFilter) {
  if (filter === 'OPEN') return items.filter((item) => !item.resolved);
  if (filter === 'RESOLVED') return items.filter((item) => item.resolved);
  return items;
}

export function exceptionSeverityTone(severity: ExceptionListItem['severity']): PresentationTone {
  return severity === 'high' ? 'red' : severity === 'medium' ? 'amber' : 'neutral';
}

export function exceptionActions(
  item: ExceptionListItem,
  permissions: { canResolve: boolean; canAssign: boolean; canViewDevice: boolean; canReadTracking: boolean }
) {
  return {
    canResolve: item.kind === 'manual' && !item.resolved && permissions.canResolve,
    canAssign: item.kind === 'manual' && !item.resolved && Boolean(item.relatedOrderId) && permissions.canAssign,
    canViewDevice: Boolean(item.relatedDeviceId) && permissions.canViewDevice,
    canOpenTracking:
      item.kind === 'tracking'
      && Boolean(item.relatedOrderId)
      && permissions.canReadTracking,
  };
}
