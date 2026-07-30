import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';

import {
  deriveCategories,
  deriveModels,
  mapChannelOrders,
  mapDevices,
  mapReviews,
  mapSchedules,
} from '../../api/mappers';
import {
  fetchScheduleCenterSnapshot,
  fetchXianyuConfig,
  type XianyuConfigVO,
} from '../../api/rental';
import { isAuthenticationFailure } from '../../api/client';
import { buildSnapshotAccess } from '../../app/accessModel';
import { useLatestRequest } from '../../shared/hooks/useLatestRequest';
import type {
  DeviceInstance,
  EquipmentModel,
  ExceptionItem,
  ModelCategory,
  RentalOrder,
  ScheduleBlock,
} from '../../types';
import { usePermissions } from '../permissions/PermissionContext';
import { useSession } from '../session/SessionContext';
import { queryHealth } from './dataModel';

interface ScheduleCenterDataContextValue {
  categories: ModelCategory[];
  models: EquipmentModel[];
  devices: DeviceInstance[];
  orders: RentalOrder[];
  blocks: ScheduleBlock[];
  exceptions: ExceptionItem[];
  isDataLoading: boolean;
  dataError: string | null;
  xianyuConfig: XianyuConfigVO | null;
  xianyuConfigUnavailable: boolean;
  lastSyncAt: number | null;
  lastSyncDeviceCount: number;
  lastSyncOrderCount: number;
  refreshData: () => Promise<void>;
}

const ScheduleCenterDataContext =
  createContext<ScheduleCenterDataContextValue | null>(null);

const emptyCollections = {
  categories: [] as ModelCategory[],
  models: [] as EquipmentModel[],
  devices: [] as DeviceInstance[],
  orders: [] as RentalOrder[],
  blocks: [] as ScheduleBlock[],
  exceptions: [] as ExceptionItem[],
};

interface ScheduleCenterDataProviderProps {
  children: ReactNode;
  loadSnapshot?: typeof fetchScheduleCenterSnapshot;
  loadXianyuConfig?: typeof fetchXianyuConfig;
  now?: () => number;
}

function mapSnapshot(snapshot: Awaited<ReturnType<typeof fetchScheduleCenterSnapshot>>) {
  const devices = mapDevices(snapshot.devices);
  const models = deriveModels(snapshot.devices);
  const orders = mapChannelOrders(snapshot.channelOrders, snapshot.pendingShipOrders);
  const deviceUsage = new Map<string, Partial<DeviceInstance>>();

  orders.forEach((order) => {
    order.items.forEach((item) => {
      item.assignedDeviceIds.forEach((deviceId) => {
        deviceUsage.set(deviceId, {
          currentOrderId: order.orderNumber,
          currentCustomer: order.customerName,
          logisticsNumber: order.logisticsNumber,
          currentPeriod: order.rentalPeriodReady
            ? { startDate: order.startDate, endDate: order.endDate }
            : undefined,
        });
      });
    });
  });

  return {
    categories: models.length > 0 ? deriveCategories() : [],
    models,
    devices: devices.map((device) => ({ ...device, ...deviceUsage.get(device.id) })),
    orders,
    blocks: mapSchedules(snapshot.schedules),
    exceptions: mapReviews(snapshot.reviews),
  };
}

export function ScheduleCenterDataProvider({
  children,
  loadSnapshot = fetchScheduleCenterSnapshot,
  loadXianyuConfig = fetchXianyuConfig,
  now = Date.now,
}: ScheduleCenterDataProviderProps) {
  const { isLoggedIn, requireAuthentication, sessionRevision } = useSession();
  const {
    permissions,
    accessDenied,
    isPermissionLoading,
    permissionRevision,
  } = usePermissions();
  const [collections, setCollections] = useState(emptyCollections);
  const [isDataLoading, setIsDataLoading] = useState(isLoggedIn);
  const [dataError, setDataError] = useState<string | null>(null);
  const [xianyuConfig, setXianyuConfig] = useState<XianyuConfigVO | null>(null);
  const [xianyuConfigUnavailable, setXianyuConfigUnavailable] = useState(true);
  const [lastSyncAt, setLastSyncAt] = useState<number | null>(null);
  const [lastSyncDeviceCount, setLastSyncDeviceCount] = useState(0);
  const [lastSyncOrderCount, setLastSyncOrderCount] = useState(0);
  const [dataSessionRevision, setDataSessionRevision] = useState(sessionRevision);
  const [dataPermissionRevision, setDataPermissionRevision] =
    useState(permissionRevision);
  const beginRequest = useLatestRequest(
    `${sessionRevision}:${permissionRevision}:${String(isLoggedIn)}`
  );

  const commitGeneration = useCallback(() => {
    setDataSessionRevision(sessionRevision);
    setDataPermissionRevision(permissionRevision);
  }, [permissionRevision, sessionRevision]);

  const clearData = useCallback(() => {
    setCollections(emptyCollections);
    setXianyuConfig(null);
    setXianyuConfigUnavailable(true);
    setLastSyncAt(null);
    setLastSyncDeviceCount(0);
    setLastSyncOrderCount(0);
  }, []);

  const refreshData = useCallback(async () => {
    const isCurrent = beginRequest();
    if (!isLoggedIn) {
      clearData();
      commitGeneration();
      setDataError('AUTH_REQUIRED');
      setIsDataLoading(false);
      return;
    }
    if (isPermissionLoading) return;
    if (accessDenied) {
      clearData();
      commitGeneration();
      setDataError(null);
      setIsDataLoading(false);
      return;
    }

    setIsDataLoading(true);
    setDataError(null);
    setXianyuConfigUnavailable(false);
    try {
      const access = buildSnapshotAccess(permissions);
      const [snapshot, configResult] = await Promise.all([
        loadSnapshot(access),
        access.xianyuConfig
          ? loadXianyuConfig()
              .then((config) => ({ config, unavailable: false }))
              .catch((error) => {
                if (isAuthenticationFailure(error)) throw error;
                return { config: null, unavailable: true };
              })
          : Promise.resolve({ config: null, unavailable: false }),
      ]);
      if (!isCurrent()) return;
      setCollections(mapSnapshot(snapshot));
      setXianyuConfig(configResult.config);
      setXianyuConfigUnavailable(configResult.unavailable);
      setDataError(queryHealth(snapshot.failures) === 'partial' ? 'PARTIAL_SYNC_FAILED' : null);
      setLastSyncAt(now());
      setLastSyncDeviceCount(snapshot.totals.devices);
      setLastSyncOrderCount(snapshot.totals.channelOrders);
      commitGeneration();
    } catch (error) {
      if (!isCurrent()) return;
      clearData();
      commitGeneration();
      if (isAuthenticationFailure(error)) {
        setDataError('AUTH_REQUIRED');
        requireAuthentication();
      } else {
        setDataError(error instanceof Error ? error.message : 'MANAGEMENT_SYNC_FAILED');
      }
    } finally {
      if (isCurrent()) setIsDataLoading(false);
    }
  }, [
    accessDenied,
    beginRequest,
    clearData,
    commitGeneration,
    isLoggedIn,
    isPermissionLoading,
    loadSnapshot,
    loadXianyuConfig,
    now,
    permissions,
    requireAuthentication,
  ]);

  useEffect(() => {
    void refreshData();
  }, [permissionRevision, refreshData, sessionRevision]);

  const isCurrentGeneration =
    dataSessionRevision === sessionRevision
    && dataPermissionRevision === permissionRevision;
  const visibleCollections = isCurrentGeneration ? collections : emptyCollections;

  return (
    <ScheduleCenterDataContext.Provider
      value={{
        ...visibleCollections,
        isDataLoading:
          isDataLoading
          || (
            isLoggedIn
            && !accessDenied
            && !isPermissionLoading
            && !isCurrentGeneration
          ),
        dataError: isCurrentGeneration ? dataError : null,
        xianyuConfig: isCurrentGeneration ? xianyuConfig : null,
        xianyuConfigUnavailable:
          isCurrentGeneration ? xianyuConfigUnavailable : true,
        lastSyncAt: isCurrentGeneration ? lastSyncAt : null,
        lastSyncDeviceCount: isCurrentGeneration ? lastSyncDeviceCount : 0,
        lastSyncOrderCount: isCurrentGeneration ? lastSyncOrderCount : 0,
        refreshData,
      }}
    >
      {children}
    </ScheduleCenterDataContext.Provider>
  );
}

export function useScheduleCenterData() {
  const context = useContext(ScheduleCenterDataContext);
  if (!context) {
    throw new Error(
      'useScheduleCenterData must be used within ScheduleCenterDataProvider'
    );
  }
  return context;
}
