import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
  type SetStateAction,
} from 'react';

import {
  fetchDeliveryTrackingDetail,
  fetchDeliveryTrackingSummaries,
  requestDeliveryTrackingRefresh,
} from '../../api/rental';
import { isAuthenticationFailure } from '../../api/client';
import { useLatestRequest } from '../../shared/hooks/useLatestRequest';
import { classifySafeError, type SafeErrorCategory } from '../../shared/lib/safeError';
import { useScheduleCenterData } from '../data/ScheduleCenterDataContext';
import { usePermissions } from '../permissions/PermissionContext';
import { useSession } from '../session/SessionContext';
import { startVisibleSummaryPolling } from './trackingPolling';
import {
  groupTrackingByOrderId,
  toDeliveryOrderSummary,
  toDeliveryPackageDetail,
  toDeliveryRefreshResult,
  visibleRentalOrderIds,
  type DeliveryOrderSummary,
  type DeliveryPackageDetail,
  type DeliveryRefreshResult,
} from './trackingModel';

interface DeliveryDetailState {
  detail: DeliveryPackageDetail | null;
  isLoading: boolean;
  error: SafeErrorCategory | null;
  refreshResult: DeliveryRefreshResult | null;
  isRefreshPending: boolean;
}

interface DeliveryTrackingContextValue {
  canReadTracking: boolean;
  trackingByOrderId: Record<string, DeliveryOrderSummary>;
  visibleTrackingSummaries: DeliveryOrderSummary[];
  visibleTrackingOrderIds: number[];
  isSummaryLoading: boolean;
  summaryError: SafeErrorCategory | null;
  lastSummarySyncAt: number | null;
  refreshSummaries: () => Promise<void>;
  getDetailState: (deliveryId: number) => DeliveryDetailState;
  loadDetail: (deliveryId: number, force?: boolean) => Promise<void>;
  refreshDelivery: (deliveryId: number) => Promise<DeliveryRefreshResult | null>;
}

const DeliveryTrackingContext = createContext<DeliveryTrackingContextValue | null>(null);

const emptyTracking: Record<string, DeliveryOrderSummary> = {};
const emptyDetailState: DeliveryDetailState = {
  detail: null,
  isLoading: false,
  error: null,
  refreshResult: null,
  isRefreshPending: false,
};

function localDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function plusDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

export function DeliveryTrackingProvider({ children }: { children: ReactNode }) {
  const { blocks, isDataLoading } = useScheduleCenterData();
  const { hasPermission, permissionRevision } = usePermissions();
  const { isLoggedIn, requireAuthentication, sessionRevision } = useSession();
  const canReadTracking = hasPermission('rental:delivery:tracking');
  const today = useMemo(() => new Date(), [blocks.length]);
  const windowStart = localDateString(today);
  const windowEnd = localDateString(plusDays(today, 13));
  const visibleTrackingOrderIds = useMemo(
    () => (canReadTracking ? visibleRentalOrderIds(blocks, windowStart, windowEnd) : []),
    [blocks, canReadTracking, windowEnd, windowStart]
  );
  const visibleKey = visibleTrackingOrderIds.join(',');
  const beginSummaryRequest = useLatestRequest(
    `${sessionRevision}:${permissionRevision}:${visibleKey}:${String(canReadTracking)}`
  );
  const detailRequestTokens = useRef(new Map<number, symbol>());
  const [trackingByOrderId, setTrackingByOrderId] =
    useState<Record<string, DeliveryOrderSummary>>(emptyTracking);
  const [detailStates, setDetailStates] =
    useState<Record<number, DeliveryDetailState>>({});
  const detailStatesRef = useRef<Record<number, DeliveryDetailState>>({});
  const [isSummaryLoading, setIsSummaryLoading] = useState(false);
  const [summaryError, setSummaryError] = useState<SafeErrorCategory | null>(null);
  const [lastSummarySyncAt, setLastSummarySyncAt] = useState<number | null>(null);
  const updateDetailStates = useCallback(
    (action: SetStateAction<Record<number, DeliveryDetailState>>) => {
      setDetailStates((current) => {
        const next = typeof action === 'function' ? action(current) : action;
        detailStatesRef.current = next;
        return next;
      });
    },
    []
  );

  const refreshSummaries = useCallback(async () => {
    const isCurrent = beginSummaryRequest();
    if (!isLoggedIn || !canReadTracking || visibleTrackingOrderIds.length === 0) {
      if (isCurrent()) {
        setTrackingByOrderId(emptyTracking);
        setSummaryError(null);
        setLastSummarySyncAt(null);
        setIsSummaryLoading(false);
      }
      return;
    }
    setIsSummaryLoading(true);
    setSummaryError(null);
    try {
      const result = await fetchDeliveryTrackingSummaries(visibleTrackingOrderIds);
      if (!isCurrent()) return;
      const summaries = Object.values(result).map(toDeliveryOrderSummary);
      setTrackingByOrderId(groupTrackingByOrderId(summaries));
      setLastSummarySyncAt(Date.now());
    } catch (error) {
      if (!isCurrent()) return;
      if (isAuthenticationFailure(error)) {
        requireAuthentication();
        setSummaryError('authentication');
      } else {
        const message = error instanceof Error ? error.message : null;
        setSummaryError(classifySafeError(message));
      }
    } finally {
      if (isCurrent()) setIsSummaryLoading(false);
    }
  }, [
    beginSummaryRequest,
    canReadTracking,
    isLoggedIn,
    requireAuthentication,
    visibleTrackingOrderIds,
  ]);

  useEffect(() => {
    if (isDataLoading) return;
    void refreshSummaries();
  }, [isDataLoading, refreshSummaries]);

  useEffect(() => {
    if (!canReadTracking || !isLoggedIn || visibleTrackingOrderIds.length === 0) return;
    if (typeof document === 'undefined' || typeof window === 'undefined') return;
    return startVisibleSummaryPolling({
      visibility: document,
      scheduler: window,
      refresh: () => {
        void refreshSummaries();
      },
    });
  }, [canReadTracking, isLoggedIn, refreshSummaries, visibleKey, visibleTrackingOrderIds.length]);

  const loadDetail = useCallback(
    async (deliveryId: number, force = false) => {
      if (!canReadTracking || !deliveryId) return;
      const existing = detailStatesRef.current[deliveryId];
      if (!force && existing?.detail) return;
      const token = Symbol(String(deliveryId));
      detailRequestTokens.current.set(deliveryId, token);
      updateDetailStates((current) => ({
        ...current,
        [deliveryId]: {
          ...(current[deliveryId] || emptyDetailState),
          isLoading: true,
          error: null,
        },
      }));
      try {
        const result = await fetchDeliveryTrackingDetail(deliveryId);
        if (detailRequestTokens.current.get(deliveryId) !== token) return;
        updateDetailStates((current) => ({
          ...current,
          [deliveryId]: {
            ...(current[deliveryId] || emptyDetailState),
            detail: result ? toDeliveryPackageDetail(result) : null,
            isLoading: false,
            error: null,
          },
        }));
      } catch (error) {
        if (detailRequestTokens.current.get(deliveryId) !== token) return;
        if (isAuthenticationFailure(error)) {
          requireAuthentication();
          updateDetailStates((current) => ({
            ...current,
            [deliveryId]: {
              ...(current[deliveryId] || emptyDetailState),
              isLoading: false,
              error: 'authentication',
            },
          }));
        } else {
          const message = error instanceof Error ? error.message : null;
          updateDetailStates((current) => ({
            ...current,
            [deliveryId]: {
              ...(current[deliveryId] || emptyDetailState),
              isLoading: false,
              error: classifySafeError(message),
            },
          }));
        }
      }
    },
    [canReadTracking, requireAuthentication, updateDetailStates]
  );

  const refreshDelivery = useCallback(
    async (deliveryId: number) => {
      if (!canReadTracking || !deliveryId) return null;
      updateDetailStates((current) => ({
        ...current,
        [deliveryId]: {
          ...(current[deliveryId] || emptyDetailState),
          isRefreshPending: true,
        },
      }));
      try {
        const result = toDeliveryRefreshResult(
          await requestDeliveryTrackingRefresh(deliveryId)
        );
        updateDetailStates((current) => ({
          ...current,
          [deliveryId]: {
            ...(current[deliveryId] || emptyDetailState),
            refreshResult: result,
            isRefreshPending: false,
            error: null,
          },
        }));
        await Promise.all([
          refreshSummaries(),
          loadDetail(deliveryId, true),
        ]);
        return result;
      } catch (error) {
        if (isAuthenticationFailure(error)) {
          requireAuthentication();
          updateDetailStates((current) => ({
            ...current,
            [deliveryId]: {
              ...(current[deliveryId] || emptyDetailState),
              isRefreshPending: false,
              error: 'authentication',
            },
          }));
        } else {
          const message = error instanceof Error ? error.message : null;
          updateDetailStates((current) => ({
            ...current,
            [deliveryId]: {
              ...(current[deliveryId] || emptyDetailState),
              isRefreshPending: false,
              error: classifySafeError(message),
            },
          }));
        }
        return null;
      }
    },
    [
      canReadTracking,
      loadDetail,
      refreshSummaries,
      requireAuthentication,
      updateDetailStates,
    ]
  );

  const value = useMemo<DeliveryTrackingContextValue>(
    () => ({
      canReadTracking,
      trackingByOrderId,
      visibleTrackingSummaries: visibleTrackingOrderIds
        .map((orderId) => trackingByOrderId[String(orderId)])
        .filter((summary): summary is DeliveryOrderSummary => Boolean(summary)),
      visibleTrackingOrderIds,
      isSummaryLoading,
      summaryError,
      lastSummarySyncAt,
      refreshSummaries,
      getDetailState: (deliveryId) => detailStates[deliveryId] || emptyDetailState,
      loadDetail,
      refreshDelivery,
    }),
    [
      canReadTracking,
      detailStates,
      isSummaryLoading,
      lastSummarySyncAt,
      loadDetail,
      refreshDelivery,
      refreshSummaries,
      summaryError,
      trackingByOrderId,
      visibleTrackingOrderIds,
    ]
  );

  return (
    <DeliveryTrackingContext.Provider value={value}>
      {children}
    </DeliveryTrackingContext.Provider>
  );
}

export function useDeliveryTracking() {
  const context = useContext(DeliveryTrackingContext);
  if (!context) {
    throw new Error('useDeliveryTracking must be used within DeliveryTrackingProvider');
  }
  return context;
}
