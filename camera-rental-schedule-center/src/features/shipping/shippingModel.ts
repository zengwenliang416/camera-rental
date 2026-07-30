import type { DeviceInstance, RentalOrder } from '../../types';
import type { LocalePreference } from '../preferences/preferenceModel';
import type { PendingShipmentSearchResult } from './shippingApi';
import { shippingMessage } from './shippingMessages';

export interface ShipmentOrderDetails {
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  buyerNick?: string;
  goodsTitle?: string;
  goodsQuantity?: number;
  amountCents?: number;
  sellerRemark?: string;
  shopId?: number;
  rentalOrderId?: number;
  channelStatus?: string;
  conversionStatus?: string;
}

export interface ShippingOrderCandidate {
  order: RentalOrder;
  details: ShipmentOrderDetails;
}

export type ShippingGateState = 'ready' | 'warning' | 'blocked' | 'pending';

export interface ShippingGate {
  id: 'waybill' | 'device' | 'order' | 'period' | 'permission';
  label: string;
  value: string;
  state: ShippingGateState;
}

export interface ShippingReadiness {
  canSubmit: boolean;
  primaryBlockReason: string | null;
  orderBlockReasons: string[];
  gates: ShippingGate[];
}

function normalize(value?: string) {
  return value?.trim().toLocaleLowerCase('zh-CN') || '';
}

export function getPendingShipmentOrders(orders: RentalOrder[]) {
  return orders.filter((order) => order.status === 'PENDING_DISPATCH');
}

export function filterAvailableDevices(devices: DeviceInstance[], query: string) {
  const term = normalize(query);
  return devices.filter((device) => {
    if (device.status !== 'IDLE') return false;
    if (!term) return true;
    return [
      device.unitCode,
      device.sn,
      device.modelId,
      device.modelName,
      device.note,
    ].some((value) => normalize(value).includes(term));
  });
}

export function filterPendingOrders(
  orders: RentalOrder[],
  query: string
) {
  const term = normalize(query);
  if (!term) return [];

  return getPendingShipmentOrders(orders)
    .filter((order) => normalize(order.orderNumber).includes(term))
    .map((order) => ({ order, details: {} }));
}

export function buildPendingOrderCandidates(
  results: PendingShipmentSearchResult[],
  orders: RentalOrder[]
): ShippingOrderCandidate[] {
  const pendingOrders = getPendingShipmentOrders(orders);
  const byId = new Map(pendingOrders.map((order) => [order.id, order]));
  const byOrderNumber = new Map(pendingOrders.map((order) => [order.orderNumber, order]));

  return results.flatMap((result) => {
    const order = byId.get(String(result.id)) || byOrderNumber.get(result.externalOrderId);
    if (!order) return [];
    return [{
      order,
      details: {
        receiverName: result.receiverName,
        receiverPhone: result.receiverMobile,
        receiverAddress: result.receiverAddress,
        buyerNick: result.buyerNick,
        goodsTitle: result.goodsTitle,
        goodsQuantity: result.goodsQuantity,
        amountCents: result.payAmount,
        sellerRemark: result.sellerRemark,
        shopId: result.shopId,
        rentalOrderId: result.rentalOrderId,
        channelStatus: result.orderStatus,
        conversionStatus: result.conversionStatus,
      },
    }];
  });
}

export function getOrderBlockReasons(
  order: RentalOrder | undefined,
  device: DeviceInstance | undefined,
  locale: LocalePreference = 'zh-CN'
) {
  if (!order) return [shippingMessage(locale, 'model.selectOrder')];
  const reasons: string[] = [];
  if (order.status !== 'PENDING_DISPATCH') reasons.push(shippingMessage(locale, 'model.notPending'));
  if (!order.rentalPeriodReady) reasons.push(shippingMessage(locale, 'model.periodPending'));
  if (!order.occupyStartDate || !order.occupyEndDateExclusive) {
    reasons.push(shippingMessage(locale, 'model.occupiedIncomplete'));
  }
  if (order.logisticsNumber) reasons.push(shippingMessage(locale, 'model.existingWaybill'));
  const modelId = order.items[0]?.modelId;
  if (device && modelId && device.modelId !== modelId) {
    reasons.push(shippingMessage(locale, 'model.modelMismatch', { model: device.modelName }));
  }
  if (!order.canShip && reasons.length === 0) {
    reasons.push(shippingMessage(locale, 'model.serverBlocked'));
  }
  return reasons;
}

export function buildShippingReadiness(params: {
  waybillNo: string;
  carrier: string;
  device?: DeviceInstance;
  order?: RentalOrder;
  permissionAllowed: boolean;
  integrationBlockReason: string | null;
  isSubmitting: boolean;
  locale?: LocalePreference;
}): ShippingReadiness {
  const {
    waybillNo,
    carrier,
    device,
    order,
    permissionAllowed,
    integrationBlockReason,
    isSubmitting,
    locale = 'zh-CN',
  } = params;
  const orderBlockReasons = getOrderBlockReasons(order, device, locale);
  const blockReasons = [
    !permissionAllowed ? shippingMessage(locale, 'model.permissionMissing') : null,
    integrationBlockReason,
    !waybillNo.trim() ? shippingMessage(locale, 'model.enterWaybill') : null,
    !carrier.trim() ? shippingMessage(locale, 'model.selectCarrier') : null,
    !device ? shippingMessage(locale, 'model.selectDevice') : null,
    ...orderBlockReasons,
    isSubmitting ? shippingMessage(locale, 'model.submitting') : null,
  ].filter((reason): reason is string => Boolean(reason));

  return {
    canSubmit: blockReasons.length === 0,
    primaryBlockReason: blockReasons[0] || null,
    orderBlockReasons,
    gates: [
      {
        id: 'waybill',
        label: shippingMessage(locale, 'model.gateWaybill'),
        value: waybillNo.trim()
          ? `${carrier} · ${waybillNo.trim()}`
          : shippingMessage(locale, 'docket.notEntered'),
        state: waybillNo.trim() && carrier.trim() ? 'ready' : 'pending',
      },
      {
        id: 'device',
        label: shippingMessage(locale, 'model.gateDevice'),
        value: device
          ? `${device.unitCode} · ${device.sn}`
          : shippingMessage(locale, 'docket.notSelected'),
        state: device ? 'ready' : 'pending',
      },
      {
        id: 'order',
        label: shippingMessage(locale, 'model.gateOrder'),
        value: order?.orderNumber || shippingMessage(locale, 'docket.notSelected'),
        state: !order ? 'pending' : orderBlockReasons.length ? 'blocked' : 'ready',
      },
      {
        id: 'period',
        label: shippingMessage(locale, 'model.gatePeriod'),
        value: order?.rentalPeriodLabel || shippingMessage(locale, 'docket.waitingOrder'),
        state: !order ? 'pending' : order.rentalPeriodReady ? 'ready' : 'blocked',
      },
      {
        id: 'permission',
        label: shippingMessage(locale, 'model.gatePermission'),
        value: !permissionAllowed
          ? shippingMessage(locale, 'model.permissionShort')
          : integrationBlockReason || shippingMessage(locale, 'model.writeAllowed'),
        state: permissionAllowed && !integrationBlockReason ? 'ready' : 'blocked',
      },
    ],
  };
}

export function formatAmount(cents?: number) {
  if (cents == null) return '-';
  return `¥${(cents / 100).toFixed(2)}`;
}
