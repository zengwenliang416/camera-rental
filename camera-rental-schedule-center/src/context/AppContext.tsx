import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import {
  EquipmentModel,
  ModelCategory,
  DeviceInstance,
  RentalOrder,
  ScheduleBlock,
  ExceptionItem,
  DeviceStatus,
} from '../types';
import { getAccessToken, getAdminUser, getCachedPermissionInfo } from '../api/auth';
import { loginWithAdminPassword, logoutFromAdmin } from '../api/client';
import {
  assignRentalDevice,
  dispatchRentalDevice,
  fetchPermissionInfo,
  fetchScheduleCenterSnapshot,
  fetchXianyuConfig,
  resolveManualReview,
  returnRentalDevice,
  shipXianyuOrder,
  XianyuConfigVO,
} from '../api/rental';
import { expressCodeFromName } from '../lib/expressCompanies';
import {
  deriveCategories,
  deriveModels,
  mapDevices,
  mapChannelOrders,
  mapReviews,
  mapSchedules,
} from '../api/mappers';

interface AppContextType {
  categories: ModelCategory[];
  models: EquipmentModel[];
  devices: DeviceInstance[];
  orders: RentalOrder[];
  blocks: ScheduleBlock[];
  exceptions: ExceptionItem[];
  isLoading: boolean;
  loadError: string | null;
  authRequired: boolean;
  accessDenied: boolean;
  permissions: string[];
  hasPermission: (permission: string | string[]) => boolean;
  xianyuConfig: XianyuConfigVO | null;
  currentUser?: {
    id?: number;
    username?: string;
    nickname?: string;
    avatar?: string;
  };
  isLoggedIn: boolean;
  isLoginPageVisible: boolean;
  setIsLoginPageVisible: (visible: boolean) => void;
  login: (params: { tenantName?: string; username: string; password: string; rememberMe?: boolean }) => Promise<void>;
  logout: () => Promise<void>;
  selectedModelId: string;
  setSelectedModelId: (id: string) => void;
  activeTab: 'dashboard' | 'schedule' | 'orders' | 'devices' | 'exceptions' | 'binding';
  setActiveTab: (tab: 'dashboard' | 'schedule' | 'orders' | 'devices' | 'exceptions' | 'binding') => void;

  // Modal / Drawer controls
  selectedOrderIdForAllocation: string | null;
  openAllocationModal: (orderId: string | null) => void;
  selectedDeviceIdForDetail: string | null;
  openDeviceDetail: (deviceId: string | null) => void;

  // Quick Binding Modal controls
  isQuickBindingOpen: boolean;
  openQuickBindingModal: (open: boolean) => void;
  preselectedOrderForBinding: string | null;
  setPreselectedOrderForBinding: (orderId: string | null) => void;

  // Actions
  assignDevicesToOrder: (orderId: string, allocationMap: Record<string, string[]>) => Promise<void>;
  bindDeviceWithOrderAndLogistics: (params: {
    deviceId: string;
    orderId: string;
    logisticsNumber?: string;
    expressCode?: string;
    expressName?: string;
    waybillNo?: string;
    note?: string;
  }) => Promise<void>;
  dispatchOrder: (orderId: string) => Promise<void>;
  returnOrder: (orderId: string, createRepair?: boolean) => Promise<void>;
  updateDeviceStatus: (deviceId: string, status: DeviceStatus, note?: string) => void;
  resolveException: (exceptionId: string) => Promise<void>;
  syncFromManagementSystem: () => Promise<void>;
  lastSyncTime: string;
}

const AppContext = createContext<AppContextType | undefined>(undefined);

export const AppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [categories, setCategories] = useState<ModelCategory[]>([]);
  const [models, setModels] = useState<EquipmentModel[]>([]);
  const [devices, setDevices] = useState<DeviceInstance[]>([]);
  const [orders, setOrders] = useState<RentalOrder[]>([]);
  const [blocks, setBlocks] = useState<ScheduleBlock[]>([]);
  const [exceptions, setExceptions] = useState<ExceptionItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [authRequired, setAuthRequired] = useState<boolean>(false);
  const [accessDenied, setAccessDenied] = useState<boolean>(false);
  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(Boolean(getAccessToken()));
  const [isLoginPageVisible, setIsLoginPageVisible] = useState<boolean>(false);
  const [permissions, setPermissions] = useState<string[]>(getCachedPermissionInfo()?.permissions || []);
  const [xianyuConfig, setXianyuConfig] = useState<XianyuConfigVO | null>(null);
  const [currentUser, setCurrentUser] = useState(getAdminUser());

  const [selectedModelId, setSelectedModelId] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'dashboard' | 'schedule' | 'orders' | 'devices' | 'exceptions' | 'binding'>('dashboard');

  const [selectedOrderIdForAllocation, setSelectedOrderIdForAllocation] = useState<string | null>(null);
  const [selectedDeviceIdForDetail, setSelectedDeviceIdForDetail] = useState<string | null>(null);
  const [isQuickBindingOpen, setIsQuickBindingOpen] = useState<boolean>(false);
  const [preselectedOrderForBinding, setPreselectedOrderForBinding] = useState<string | null>(null);
  const [lastSyncTime, setLastSyncTime] = useState<string>('等待管理端数据同步');

  const hasPermission = useCallback(
    (permission: string | string[]) => {
      const required = Array.isArray(permission) ? permission : [permission];
      return permissions.includes('*:*:*') || required.some((item) => permissions.includes(item));
    },
    [permissions]
  );

  const idempotencyKey = (prefix: string, parts: Array<string | number | undefined>) =>
    `${prefix}:${parts.filter(Boolean).join(':')}:${Date.now()}`;

  const loadManagementData = useCallback(async () => {
    if (!getAccessToken()) {
      setCategories([]);
      setModels([]);
      setDevices([]);
      setOrders([]);
      setBlocks([]);
      setExceptions([]);
      setXianyuConfig(null);
      setAuthRequired(true);
      setIsLoggedIn(false);
      setLoadError('需要先登录管理后台');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setLoadError(null);
    setAuthRequired(false);
    setAccessDenied(false);
    try {
      const cachedInfo = getCachedPermissionInfo();
      let permissionInfo = cachedInfo;
      try {
        permissionInfo = await fetchPermissionInfo();
      } catch (error) {
        if (!cachedInfo) throw error;
      }
      const nextPermissions = permissionInfo?.permissions || [];
      const hasAnyPermission =
        nextPermissions.includes('*:*:*') ||
        [
          'rental:schedule-center:access',
          'rental:device:query',
          'rental:schedule:query',
          'rental:xianyu:query',
        ].some((permission) => nextPermissions.includes(permission));
      setPermissions(nextPermissions);
      setCurrentUser(permissionInfo?.user || getAdminUser());
      setIsLoggedIn(true);

      if (!hasAnyPermission) {
        setAccessDenied(true);
        setLoadError('当前账号没有设备排期中心访问权限，请在管理后台角色中授权。');
        return;
      }

      const [snapshot, config] = await Promise.all([
        fetchScheduleCenterSnapshot(),
        fetchXianyuConfig().catch(() => null),
      ]);
      const mappedDevices = mapDevices(snapshot.devices);
      const mappedModels = deriveModels(snapshot.devices);
      const mappedOrders = mapChannelOrders(snapshot.channelOrders, snapshot.pendingShipOrders);
      const deviceUsage = new Map<string, {
        currentOrderId: string;
        currentCustomer: string;
        logisticsNumber?: string;
        currentPeriod?: { startDate: string; endDate: string };
      }>();
      mappedOrders.forEach((order) => {
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
      setCategories(mappedModels.length > 0 ? deriveCategories() : []);
      setModels(mappedModels);
      setSelectedModelId(mappedModels[0]?.id || '');
      setDevices(mappedDevices.map((device) => ({
        ...device,
        ...deviceUsage.get(device.id),
      })));
      setBlocks(mapSchedules(snapshot.schedules));
      setOrders(mappedOrders);
      setExceptions(mapReviews(snapshot.reviews));
      setXianyuConfig(config);
      const nowStr = new Date().toLocaleTimeString('zh-CN', {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      });
      setLastSyncTime(
        `刚才 ${nowStr} (${snapshot.totals.devices} 台设备 / ${snapshot.totals.channelOrders} 个渠道订单)`
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : '管理端数据同步失败';
      setCategories([]);
      setModels([]);
      setDevices([]);
      setOrders([]);
      setBlocks([]);
      setExceptions([]);
      setXianyuConfig(null);
      setSelectedModelId('');
      setLastSyncTime('管理端数据同步失败');
      if (message === 'AUTH_REQUIRED' || message === 'NO_REFRESH_TOKEN') {
        setAuthRequired(true);
        setIsLoggedIn(false);
        setLoadError('登录态已失效，请重新登录管理后台');
      } else {
        setLoadError(message);
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadManagementData();
  }, [loadManagementData]);

  const openAllocationModal = (orderId: string | null) => {
    setSelectedOrderIdForAllocation(orderId);
  };

  const openDeviceDetail = (deviceId: string | null) => {
    setSelectedDeviceIdForDetail(deviceId);
  };

  const openQuickBindingModal = (open: boolean) => {
    setIsQuickBindingOpen(open);
  };

  const login = async (params: {
    tenantName?: string;
    username: string;
    password: string;
    rememberMe?: boolean;
  }) => {
    setIsLoading(true);
    setLoadError(null);
    try {
      await loginWithAdminPassword(params);
      setIsLoggedIn(true);
      setAuthRequired(false);
      setIsLoginPageVisible(false);
      await loadManagementData();
    } catch (error) {
      setIsLoggedIn(false);
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    setIsLoading(true);
    try {
      await logoutFromAdmin();
    } finally {
      setPermissions([]);
      setCurrentUser(undefined);
      setIsLoggedIn(false);
      setAuthRequired(true);
      setIsLoginPageVisible(false);
      setLoadError('已退出登录');
      setIsLoading(false);
    }
  };

  // Bind Device + Order + Logistics Tracking Number + Record Period
  const bindDeviceWithOrderAndLogistics = async ({
    deviceId,
    orderId,
    logisticsNumber,
    expressCode,
    expressName,
    waybillNo,
  }: {
    deviceId: string;
    orderId: string;
    logisticsNumber?: string;
    expressCode?: string;
    expressName?: string;
    waybillNo?: string;
    note?: string;
  }) => {
    const targetOrder = orders.find((o) => o.id === orderId);
    const targetDevice = devices.find((d) => d.id === deviceId);

    if (!targetOrder || !targetDevice) return;
    if (!hasPermission('rental:xianyu:ship')) {
      const message = '当前账号缺少 rental:xianyu:ship，不能执行闲管家真实发货。';
      setLoadError(message);
      throw new Error(message);
    }
    if (xianyuConfig?.enabled === false || xianyuConfig?.status === 'DISABLED') {
      const message = '服务器未启用闲管家集成，不能执行真实发货。';
      setLoadError(message);
      throw new Error(message);
    }
    if (xianyuConfig?.status === 'MISSING_CREDENTIALS') {
      const message = '服务器缺少闲管家应用凭据，不能执行真实发货。';
      setLoadError(message);
      throw new Error(message);
    }
    if (xianyuConfig?.writeEnabled === false) {
      const message = '服务器已关闭闲管家写操作，请先开启 XGJ_WRITE_ENABLED。';
      setLoadError(message);
      throw new Error(message);
    }
    const legacyLogistics = logisticsNumber?.trim() || '';
    const [legacyExpressName, legacyWaybillNo] = legacyLogistics.includes(':')
      ? legacyLogistics.split(':', 2).map((item) => item.trim())
      : ['', legacyLogistics];
    const resolvedExpressName = expressName?.trim()
      || legacyExpressName
      || targetOrder.expressName
      || '其他';
    const resolvedExpressCode = expressCode?.trim() || expressCodeFromName(resolvedExpressName);
    const resolvedWaybillNo = waybillNo?.trim() || legacyWaybillNo;

    if (!resolvedWaybillNo) {
      const message = '请先录入并人工确认运单号。';
      setLoadError(message);
      throw new Error(message);
    }

    setIsLoading(true);
    setLoadError(null);
    try {
      await shipXianyuOrder({
        channelOrderId: Number(targetOrder.id),
        deviceId: Number(targetDevice.id),
        idempotencyKey: idempotencyKey(
          'schedule-center-ship',
          [targetOrder.id, targetDevice.id, resolvedWaybillNo]
        ),
        expressCode: resolvedExpressCode,
        expressName: resolvedExpressName,
        waybillNo: resolvedWaybillNo,
        source: 'ADMIN',
        ocrConfirmed: true,
      });
      await loadManagementData();
    } catch (error) {
      const message = error instanceof Error ? error.message : '闲管家发货失败';
      setLoadError(message);
      throw error instanceof Error ? error : new Error(message);
    } finally {
      setIsLoading(false);
    }
  };

  // Assign Devices to Order ("一键确认排期")
  const assignDevicesToOrder = async (orderId: string, allocationMap: Record<string, string[]>) => {
    const targetOrder = orders.find((o) => o.id === orderId);
    if (!targetOrder) return;
    if (!hasPermission('rental:device:assign')) {
      setLoadError('当前账号缺少 rental:device:assign，不能创建设备占用排期。');
      return;
    }
    if (!targetOrder.canAssign || !targetOrder.occupyStartDate || !targetOrder.occupyEndDateExclusive) {
      setLoadError(`订单 ${targetOrder.orderNumber} 尚未具备完整的内部租赁明细与设备占用周期，不能自动排机。`);
      return;
    }

    const commands = targetOrder.items.flatMap((item) => {
      const deviceIds = allocationMap[item.modelId] || [];
      if (!item.rentalOrderItemId) return [];
      return deviceIds.map((deviceId) => ({
        rentalOrderItemId: item.rentalOrderItemId!,
        deviceId: Number(deviceId),
        occupyStartDate: targetOrder.occupyStartDate,
        occupyEndDateExclusive: targetOrder.occupyEndDateExclusive,
        idempotencyKey: idempotencyKey('schedule-center-assign', [
          item.rentalOrderItemId,
          deviceId,
          targetOrder.occupyStartDate,
          targetOrder.occupyEndDateExclusive,
        ]),
      }));
    });

    if (commands.length === 0) {
      setLoadError('当前订单接口未返回 rentalOrderItemId，不能真实创建排期；请先转换为内部租赁订单并补充明细接口。');
      return;
    }

    setIsLoading(true);
    setLoadError(null);
    try {
      await Promise.all(commands.map(assignRentalDevice));
      await loadManagementData();
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '设备排期创建失败');
    } finally {
      setIsLoading(false);
    }
  };

  // Dispatch Order (出库)
  const dispatchOrder = async (orderId: string) => {
    const targetOrder = orders.find((o) => o.id === orderId);
    if (!targetOrder) return;
    if (!hasPermission('rental:device:assign')) {
      setLoadError('当前账号缺少 rental:device:assign，不能执行设备出库。');
      return;
    }

    const devIds = targetOrder.items.flatMap((i) => i.assignedDeviceIds);
    if (devIds.length === 0) {
      setLoadError('当前订单未关联设备分配记录，不能真实出库。');
      return;
    }

    setIsLoading(true);
    setLoadError(null);
    try {
      await Promise.all(devIds.map((deviceId) => dispatchRentalDevice({ deviceId: Number(deviceId) })));
      await loadManagementData();
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '设备出库失败');
    } finally {
      setIsLoading(false);
    }
  };

  // Return Order (归还)
  const returnOrder = async (orderId: string, createRepair = false) => {
    const targetOrder = orders.find((o) => o.id === orderId);
    if (!targetOrder) return;
    if (!hasPermission('rental:device:assign')) {
      setLoadError('当前账号缺少 rental:device:assign，不能执行设备回仓。');
      return;
    }

    const devIds = targetOrder.items.flatMap((i) => i.assignedDeviceIds);
    if (devIds.length === 0) {
      setLoadError('当前订单未关联设备分配记录，不能真实回仓。');
      return;
    }

    setIsLoading(true);
    setLoadError(null);
    try {
      await Promise.all(
        devIds.map((deviceId) =>
          returnRentalDevice({
            deviceId: Number(deviceId),
            inspectPassed: !createRepair,
            note: createRepair ? '排期中心登记：回仓检测不通过，转维修' : '排期中心登记：回仓检测通过',
          })
        )
      );
      await loadManagementData();
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '设备回仓失败');
    } finally {
      setIsLoading(false);
    }
  };

  // Update single device status
  const updateDeviceStatus = (deviceId: string, status: DeviceStatus, note?: string) => {
    setLoadError(
      `设备状态不能只在前端修改。请通过出库/回仓接口变更状态；目标设备 ${deviceId}，目标状态 ${status}${note ? `，说明：${note}` : ''}。`
    );
  };

  const resolveException = async (exceptionId: string) => {
    if (!hasPermission('rental:review:update')) {
      setLoadError('当前账号缺少 rental:review:update，不能处理人工复核。');
      return;
    }
    setIsLoading(true);
    setLoadError(null);
    try {
      await resolveManualReview({
        id: Number(exceptionId),
        resolutionNote: '排期中心标记已处理',
      });
      await loadManagementData();
    } catch (error) {
      setLoadError(error instanceof Error ? error.message : '人工复核处理失败');
    } finally {
      setIsLoading(false);
    }
  };

  // Sync from Management System
  const syncFromManagementSystem = async () => {
    await loadManagementData();
  };

  return (
    <AppContext.Provider
      value={{
        categories,
        models,
        devices,
        orders,
        blocks,
        exceptions,
        isLoading,
        loadError,
        authRequired,
        accessDenied,
        permissions,
        hasPermission,
        xianyuConfig,
        currentUser,
        isLoggedIn,
        isLoginPageVisible,
        setIsLoginPageVisible,
        login,
        logout,
        selectedModelId,
        setSelectedModelId,
        activeTab,
        setActiveTab,
        selectedOrderIdForAllocation,
        openAllocationModal,
        selectedDeviceIdForDetail,
        openDeviceDetail,
        isQuickBindingOpen,
        openQuickBindingModal,
        preselectedOrderForBinding,
        setPreselectedOrderForBinding,
        assignDevicesToOrder,
        bindDeviceWithOrderAndLogistics,
        dispatchOrder,
        returnOrder,
        updateDeviceStatus,
        resolveException,
        syncFromManagementSystem,
        lastSyncTime,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

export const useApp = () => {
  const context = useContext(AppContext);
  if (!context) throw new Error('useApp must be used within AppProvider');
  return context;
};
