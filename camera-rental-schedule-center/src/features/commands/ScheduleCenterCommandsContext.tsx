import {
  createContext,
  useEffect,
  useContext,
  useRef,
  useState,
  type ReactNode,
} from 'react';

import {
  assignRentalDevice,
  dispatchRentalDevice,
  resolveManualReview,
  returnRentalDevice,
  shipXianyuOrder,
} from '../../api/rental';
import { isAuthenticationFailure } from '../../api/client';
import { expressCodeFromName } from '../../lib/expressCompanies';
import type { DeviceStatus } from '../../types';
import { useScheduleCenterData } from '../data/ScheduleCenterDataContext';
import { usePermissions } from '../permissions/PermissionContext';
import { useSession } from '../session/SessionContext';
import { canStartCommand, shipmentGuard } from './commandModel';

interface BindShipmentParams {
  deviceId: string;
  orderId: string;
  logisticsNumber?: string;
  expressCode?: string;
  expressName?: string;
  waybillNo?: string;
  note?: string;
}

interface ScheduleCenterCommandsContextValue {
  isCommandPending: boolean;
  commandError: string | null;
  clearCommandError: () => void;
  assignDevicesToOrder: (
    orderId: string,
    allocationMap: Record<string, string[]>
  ) => Promise<void>;
  bindDeviceWithOrderAndLogistics: (params: BindShipmentParams) => Promise<void>;
  dispatchOrder: (orderId: string) => Promise<void>;
  returnOrder: (orderId: string, createRepair?: boolean) => Promise<void>;
  updateDeviceStatus: (
    deviceId: string,
    status: DeviceStatus,
    note?: string
  ) => void;
  resolveException: (exceptionId: string) => Promise<void>;
}

interface ScheduleCenterCommandServices {
  assignRentalDevice: typeof assignRentalDevice;
  dispatchRentalDevice: typeof dispatchRentalDevice;
  resolveManualReview: typeof resolveManualReview;
  returnRentalDevice: typeof returnRentalDevice;
  shipXianyuOrder: typeof shipXianyuOrder;
}

const ScheduleCenterCommandsContext =
  createContext<ScheduleCenterCommandsContextValue | null>(null);

function idempotencyKey(
  prefix: string,
  parts: Array<string | number | undefined>
) {
  return `${prefix}:${parts.filter(Boolean).join(':')}:${Date.now()}`;
}

export function ScheduleCenterCommandsProvider({
  children,
  services,
}: {
  children: ReactNode;
  services?: Partial<ScheduleCenterCommandServices>;
}) {
  const { orders, devices, xianyuConfig, refreshData } = useScheduleCenterData();
  const { hasPermission, permissionRevision } = usePermissions();
  const { requireAuthentication, sessionRevision } = useSession();
  const commandScope = `${sessionRevision}:${permissionRevision}`;
  const pendingCommands = useRef(new Map<string, symbol>());
  const pendingScope = useRef(commandScope);
  const [pendingCount, setPendingCount] = useState(0);
  const [commandError, setCommandError] = useState<string | null>(null);
  const [stateScope, setStateScope] = useState(commandScope);

  if (pendingScope.current !== commandScope) {
    pendingScope.current = commandScope;
    pendingCommands.current.clear();
  }

  useEffect(() => {
    pendingCommands.current.clear();
    setPendingCount(0);
    setCommandError(null);
    setStateScope(commandScope);
  }, [commandScope]);

  const writeCommandError = (message: string | null) => {
    setStateScope(commandScope);
    setCommandError(message);
  };

  const runCommand = async (
    key: string,
    fallbackMessage: string,
    command: () => Promise<void>,
    rethrow = false
  ) => {
    if (!canStartCommand(pendingCommands.current, key)) return;
    const requestScope = commandScope;
    const commandToken = Symbol(key);
    const isCurrent = () =>
      pendingScope.current === requestScope
      && pendingCommands.current.get(key) === commandToken;
    pendingCommands.current.set(key, commandToken);
    setStateScope(commandScope);
    setPendingCount(pendingCommands.current.size);
    setCommandError(null);
    try {
      await command();
      if (!isCurrent()) return;
      await refreshData();
    } catch (error) {
      const resolved = error instanceof Error ? error : new Error(fallbackMessage);
      if (isCurrent()) {
        if (isAuthenticationFailure(resolved)) {
          writeCommandError('AUTH_REQUIRED');
          requireAuthentication();
        } else {
          writeCommandError(resolved.message || fallbackMessage);
        }
      }
      if (rethrow) throw resolved;
    } finally {
      const wasCurrent = isCurrent();
      if (pendingCommands.current.get(key) === commandToken) {
        pendingCommands.current.delete(key);
      }
      if (wasCurrent) setPendingCount(pendingCommands.current.size);
    }
  };

  const bindDeviceWithOrderAndLogistics = async ({
    deviceId,
    orderId,
    logisticsNumber,
    expressCode,
    expressName,
    waybillNo,
  }: BindShipmentParams) => {
    const targetOrder = orders.find((order) => order.id === orderId);
    const targetDevice = devices.find((device) => device.id === deviceId);
    if (!targetOrder || !targetDevice) return;

    const blocked = shipmentGuard(
      hasPermission('rental:xianyu:ship'),
      xianyuConfig,
      hasPermission('rental:xianyu:query')
    );
    if (blocked) {
      writeCommandError(blocked);
      throw new Error(blocked);
    }

    const legacyLogistics = logisticsNumber?.trim() || '';
    const [legacyExpressName, legacyWaybillNo] = legacyLogistics.includes(':')
      ? legacyLogistics.split(':', 2).map((item) => item.trim())
      : ['', legacyLogistics];
    const resolvedExpressName = expressName?.trim()
      || legacyExpressName
      || targetOrder.expressName
      || '其他';
    const resolvedExpressCode =
      expressCode?.trim() || expressCodeFromName(resolvedExpressName);
    const resolvedWaybillNo = waybillNo?.trim() || legacyWaybillNo;
    if (!resolvedWaybillNo) {
      const message = '请先录入并人工确认运单号。';
      writeCommandError(message);
      throw new Error(message);
    }

    await runCommand(
      `ship:${targetOrder.id}:${targetDevice.id}`,
      '闲管家发货失败',
      async () => {
        await (services?.shipXianyuOrder || shipXianyuOrder)({
          channelOrderId: Number(targetOrder.id),
          deviceId: Number(targetDevice.id),
          idempotencyKey: idempotencyKey('schedule-center-ship', [
            targetOrder.id,
            targetDevice.id,
            resolvedWaybillNo,
          ]),
          expressCode: resolvedExpressCode,
          expressName: resolvedExpressName,
          waybillNo: resolvedWaybillNo,
          source: 'ADMIN',
          ocrConfirmed: true,
        });
      },
      true
    );
  };

  const assignDevicesToOrder = async (
    orderId: string,
    allocationMap: Record<string, string[]>
  ) => {
    const targetOrder = orders.find((order) => order.id === orderId);
    if (!targetOrder) return;
    if (!hasPermission('rental:device:assign')) {
      writeCommandError('当前账号缺少 rental:device:assign，不能创建设备占用排期。');
      return;
    }
    if (
      !targetOrder.canAssign
      || !targetOrder.occupyStartDate
      || !targetOrder.occupyEndDateExclusive
    ) {
      writeCommandError(
        `订单 ${targetOrder.orderNumber} 尚未具备完整的内部租赁明细与设备占用周期，不能自动排机。`
      );
      return;
    }

    const commands = targetOrder.items.flatMap((item) => {
      if (!item.rentalOrderItemId) return [];
      return (allocationMap[item.modelId] || []).map((deviceId) => ({
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
    if (!commands.length) {
      writeCommandError(
        '当前订单接口未返回 rentalOrderItemId，不能真实创建排期；请先转换为内部租赁订单并补充明细接口。'
      );
      return;
    }

    await runCommand(`assign:${orderId}`, '设备排期创建失败', async () => {
      await Promise.all(
        commands.map(services?.assignRentalDevice || assignRentalDevice)
      );
    });
  };

  const dispatchOrder = async (orderId: string) => {
    const targetOrder = orders.find((order) => order.id === orderId);
    if (!targetOrder) return;
    if (!hasPermission('rental:device:assign')) {
      writeCommandError('当前账号缺少 rental:device:assign，不能执行设备出库。');
      return;
    }
    const deviceIds = targetOrder.items.flatMap((item) => item.assignedDeviceIds);
    if (!deviceIds.length) {
      writeCommandError('当前订单未关联设备分配记录，不能真实出库。');
      return;
    }
    await runCommand(`dispatch:${orderId}`, '设备出库失败', async () => {
      await Promise.all(
        deviceIds.map((deviceId) =>
          (services?.dispatchRentalDevice || dispatchRentalDevice)({
            deviceId: Number(deviceId),
          })
        )
      );
    });
  };

  const returnOrder = async (orderId: string, createRepair = false) => {
    const targetOrder = orders.find((order) => order.id === orderId);
    if (!targetOrder) return;
    if (!hasPermission('rental:device:assign')) {
      writeCommandError('当前账号缺少 rental:device:assign，不能执行设备回仓。');
      return;
    }
    const deviceIds = targetOrder.items.flatMap((item) => item.assignedDeviceIds);
    if (!deviceIds.length) {
      writeCommandError('当前订单未关联设备分配记录，不能真实回仓。');
      return;
    }
    await runCommand(`return:${orderId}`, '设备回仓失败', async () => {
      await Promise.all(
        deviceIds.map((deviceId) =>
          (services?.returnRentalDevice || returnRentalDevice)({
            deviceId: Number(deviceId),
            inspectPassed: !createRepair,
            note: createRepair
              ? '排期中心登记：回仓检测不通过，转维修'
              : '排期中心登记：回仓检测通过',
          })
        )
      );
    });
  };

  const resolveException = async (exceptionId: string) => {
    if (!hasPermission('rental:review:update')) {
      writeCommandError('当前账号缺少 rental:review:update，不能处理人工复核。');
      return;
    }
    await runCommand(`review:${exceptionId}`, '人工复核处理失败', async () => {
      await (services?.resolveManualReview || resolveManualReview)({
        id: Number(exceptionId),
        resolutionNote: '排期中心标记已处理',
      });
    });
  };

  return (
    <ScheduleCenterCommandsContext.Provider
      value={{
        isCommandPending: stateScope === commandScope && pendingCount > 0,
        commandError: stateScope === commandScope ? commandError : null,
        clearCommandError: () => writeCommandError(null),
        assignDevicesToOrder,
        bindDeviceWithOrderAndLogistics,
        dispatchOrder,
        returnOrder,
        updateDeviceStatus: (deviceId, status, note) =>
          writeCommandError(
            `设备状态不能只在前端修改。请通过出库/回仓接口变更状态；目标设备 ${deviceId}，目标状态 ${status}${note ? `，说明：${note}` : ''}。`
          ),
        resolveException,
      }}
    >
      {children}
    </ScheduleCenterCommandsContext.Provider>
  );
}

export function useScheduleCenterCommands() {
  const context = useContext(ScheduleCenterCommandsContext);
  if (!context) {
    throw new Error(
      'useScheduleCenterCommands must be used within ScheduleCenterCommandsProvider'
    );
  }
  return context;
}
