import type { RentalOrder, ScheduleBlock } from '../../types';
import type { StatusTone } from '../../shared/ui/StatusBadge';
import type {
  RentalDeliveryRefreshRespVO,
  RentalDeliveryTrackingDetailRespVO,
  RentalDeliveryTrackingOrderSummaryRespVO,
} from '../../api/rental';

export type DeliveryDirection = 'OUTBOUND' | 'RETURN' | 'EXCHANGE_OUT' | 'EXCHANGE_RETURN';

export type DeliveryTrackingStatus =
  | 'CREATED'
  | 'INFO_RECEIVED'
  | 'PICKED_UP'
  | 'IN_TRANSIT'
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED'
  | 'EXCEPTION'
  | 'RETURNING'
  | 'RETURNED'
  | 'CUSTOMS'
  | 'UNKNOWN';

export interface DeliveryRisk {
  code: string;
  severity: 'high' | 'medium' | 'low';
  safeMessage: string;
  nextAction?: string;
  deviceIds?: number[];
}

export interface DeliveryPackageSummary {
  deliveryId: number;
  rentalOrderId: number;
  direction: DeliveryDirection;
  packageSeq: number;
  carrierName?: string;
  maskedWaybillNo?: string | null;
  trackingStatus: DeliveryTrackingStatus;
  mappingStatus: string;
  subscribeStatus: string;
  queryStatus: string;
  latestTraceText?: string;
  latestEventTime?: string;
  lastSyncedAt?: string;
  estimatedDeliveryAt?: string;
  stale: boolean;
  risk?: DeliveryRisk;
}

export interface DeliveryOrderSummary {
  rentalOrderId: number;
  packageCount: number;
  statusCounts: Partial<Record<DeliveryTrackingStatus, number>>;
  packages: DeliveryPackageSummary[];
  risks: DeliveryRisk[];
}

export interface DeliveryTrackingDevice {
  deviceId?: number;
  deviceNo: string;
  equipmentModelCode?: string;
}

export interface DeliveryTrackingTrace {
  eventSeq?: number;
  businessTime?: string;
  trackingStatus: DeliveryTrackingStatus;
  traceText?: string;
  location?: string;
}

export interface DeliveryPackageDetail extends Omit<DeliveryPackageSummary, 'risk'> {
  devices: DeliveryTrackingDevice[];
  traces: DeliveryTrackingTrace[];
  risks: DeliveryRisk[];
}

export interface DeliveryRefreshResult {
  accepted: boolean;
  reason: string;
  nextAllowedAt?: string;
}

export interface TrackingStatusPresentation {
  tone: StatusTone;
  icon: 'clock' | 'truck' | 'check' | 'alert' | 'package';
}

const statusPresentation: Record<DeliveryTrackingStatus, TrackingStatusPresentation> = {
  CREATED: { tone: 'neutral', icon: 'package' },
  INFO_RECEIVED: { tone: 'amber', icon: 'clock' },
  PICKED_UP: { tone: 'blue', icon: 'truck' },
  IN_TRANSIT: { tone: 'blue', icon: 'truck' },
  OUT_FOR_DELIVERY: { tone: 'blue', icon: 'truck' },
  DELIVERED: { tone: 'green', icon: 'check' },
  EXCEPTION: { tone: 'red', icon: 'alert' },
  RETURNING: { tone: 'amber', icon: 'truck' },
  RETURNED: { tone: 'green', icon: 'check' },
  CUSTOMS: { tone: 'amber', icon: 'clock' },
  UNKNOWN: { tone: 'neutral', icon: 'package' },
};

export function trackingStatusPresentation(status: DeliveryTrackingStatus) {
  return statusPresentation[status] || statusPresentation.UNKNOWN;
}

export function groupTrackingByOrderId(items: DeliveryOrderSummary[]) {
  return Object.fromEntries(items.map((item) => [String(item.rentalOrderId), item]));
}

export function onlyTrackedSummaries(items: DeliveryOrderSummary[]) {
  return items.filter((item) => item.packageCount > 0 && item.packages.length > 0);
}

export function summarizeMultiPackageStatus(summary: DeliveryOrderSummary) {
  if (summary.packageCount <= 1) {
    return summary.packages[0]?.trackingStatus || 'CREATED';
  }
  const parts = Object.entries(summary.statusCounts)
    .filter((entry): entry is [DeliveryTrackingStatus, number] => Boolean(entry[1]))
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([status, count]) => `${status}:${count}`);
  return `${summary.packageCount}|${parts.join(',')}`;
}

export function visibleRentalOrderIds(
  blocks: ScheduleBlock[],
  orders: RentalOrder[],
  windowStart: string,
  windowEnd: string
) {
  const scheduledRentalOrderIds = blocks
        .filter((block) => Boolean(block.orderId))
        .filter((block) => block.startDate <= windowEnd && block.endDate >= windowStart)
        .map((block) => Number(block.orderId))
        .filter((orderId) => Number.isSafeInteger(orderId) && orderId > 0);
  const shippedChannelOrderIds = orders
    .filter((order) => order.status === 'RENTING')
    .filter((order) => Boolean(order.logisticsNumber?.trim()))
    .map((order) => Number(order.rentalOrderId ?? order.id))
    .filter((orderId) => Number.isSafeInteger(orderId) && orderId > 0);
  return Array.from(new Set([
    ...scheduledRentalOrderIds,
    ...shippedChannelOrderIds,
  ])).sort((left, right) => left - right);
}

export function riskTone(severity: DeliveryRisk['severity']): StatusTone {
  if (severity === 'high') return 'red';
  if (severity === 'medium') return 'amber';
  return 'neutral';
}

function normalizeRiskSeverity(value?: string | null): DeliveryRisk['severity'] {
  const normalized = String(value || '').toLowerCase();
  if (normalized === 'high') return 'high';
  if (normalized === 'medium') return 'medium';
  return 'low';
}

function normalizeTrackingStatus(value?: string | null): DeliveryTrackingStatus {
  const normalized = String(value || '').toUpperCase();
  if (normalized in statusPresentation) {
    return normalized as DeliveryTrackingStatus;
  }
  return 'UNKNOWN';
}

export function toDeliveryRisk(input: {
  code?: string | null;
  severity?: string | null;
  safeMessage?: string | null;
  nextAction?: string | null;
  deviceIds?: number[] | null;
}): DeliveryRisk {
  return {
    code: String(input.code || 'UNKNOWN'),
    severity: normalizeRiskSeverity(input.severity),
    safeMessage: String(input.safeMessage || input.code || 'UNKNOWN'),
    nextAction: input.nextAction || undefined,
    deviceIds: input.deviceIds ? [...input.deviceIds] : undefined,
  };
}

export function toDeliveryOrderSummary(input: RentalDeliveryTrackingOrderSummaryRespVO): DeliveryOrderSummary {
  return {
    rentalOrderId: input.orderId,
    packageCount: input.packageCount,
    statusCounts: Object.fromEntries(
      Object.entries(input.statusCounts || {}).map(([status, count]) => [
        normalizeTrackingStatus(status),
        count,
      ])
    ) as Partial<Record<DeliveryTrackingStatus, number>>,
    packages: (input.packages || []).map((item) => ({
      deliveryId: item.deliveryId,
      rentalOrderId: input.orderId,
      direction: item.direction,
      packageSeq: item.packageSeq,
      carrierName: item.carrierName,
      maskedWaybillNo: item.maskedWaybillNo ?? null,
      trackingStatus: normalizeTrackingStatus(item.trackingStatus),
      mappingStatus: item.mappingStatus,
      subscribeStatus: item.subscribeStatus,
      queryStatus: item.queryStatus,
      latestTraceText: item.latestTraceText || undefined,
      latestEventTime: item.latestEventTime || undefined,
      lastSyncedAt: item.lastSyncedAt || undefined,
      estimatedDeliveryAt: item.estimatedDeliveryAt || undefined,
      stale: Boolean(item.stale),
      risk: item.risk ? toDeliveryRisk(item.risk) : undefined,
    })),
    risks: (input.risks || []).map(toDeliveryRisk),
  };
}

export function toDeliveryPackageDetail(input: RentalDeliveryTrackingDetailRespVO): DeliveryPackageDetail {
  return {
    deliveryId: input.deliveryId,
    rentalOrderId: input.rentalOrderId,
    direction: input.direction,
    packageSeq: input.packageSeq,
    carrierName: input.carrierName,
    maskedWaybillNo: input.maskedWaybillNo ?? null,
    trackingStatus: normalizeTrackingStatus(input.trackingStatus),
    mappingStatus: input.mappingStatus,
    subscribeStatus: input.subscribeStatus,
    queryStatus: input.queryStatus,
    latestTraceText: input.latestTraceText || undefined,
    latestEventTime: input.latestEventTime || undefined,
    lastSyncedAt: input.lastSyncedAt || undefined,
    estimatedDeliveryAt: input.estimatedDeliveryAt || undefined,
    stale: Boolean(input.stale),
    devices: (input.devices || []).map((device) => ({
      deviceId: device.deviceId,
      deviceNo: device.deviceNo,
      equipmentModelCode: device.equipmentModelCode || undefined,
    })),
    traces: (input.traces || []).map((trace) => ({
      eventSeq: trace.eventSeq,
      businessTime: trace.businessTime || undefined,
      trackingStatus: normalizeTrackingStatus(trace.trackingStatus),
      traceText: trace.traceText || undefined,
      location: trace.location || undefined,
    })),
    risks: (input.risks || []).map(toDeliveryRisk),
  };
}

export function toDeliveryRefreshResult(input: RentalDeliveryRefreshRespVO): DeliveryRefreshResult {
  return {
    accepted: input.accepted,
    reason: input.reason,
    nextAllowedAt: input.nextAllowedAt || undefined,
  };
}
