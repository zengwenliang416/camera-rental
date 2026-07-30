/**
 * Types for Equipment Scheduling & Rental Operations System
 */

export type DeviceStatus =
  | 'IDLE'        // 空闲
  | 'RESERVED'    // 已预留
  | 'RENTING'     // 租赁中
  | 'PENDING_RETURN' // 待归还
  | 'REPAIR'      // 维修/检测
  | 'LOCKED';     // 人工锁定

export type OrderStatus =
  | 'UNASSIGNED'     // 待排期 (未分配设备)
  | 'ASSIGNED'       // 已排期 (已分配设备)
  | 'PENDING_DISPATCH' // 待出库
  | 'RENTING'        // 租赁中
  | 'PENDING_RETURN' // 待归还
  | 'COMPLETED'      // 已完成
  | 'EXCEPTION';     // 异常

export type OrderChannel = 'XIANYU' | 'OFFLINE' | 'WEB' | 'TAOBAO';

export interface ModelCategory {
  id: string;
  name: string;
  icon?: string;
}

export interface EquipmentModel {
  id: string;
  name: string;
  categoryId: string;
  totalUnits: number;
  imageUrl?: string;
}

export interface DeviceInstance {
  id: string;
  unitCode: string; // e.g. "01号"
  sn: string;       // e.g. "ANHXP5L002-2JCW"
  modelId: string;
  modelName: string;
  status: DeviceStatus;
  currentOrderId?: string;
  currentCustomer?: string;
  logisticsNumber?: string; // 物流运单号 e.g. "SF1893029104"
  qrCode?: string;          // 设备二维码内容
  currentPeriod?: {
    startDate: string; // YYYY-MM-DD
    endDate: string;
  };
  expectedAvailableDate?: string;
  note?: string;
}

export interface OrderItemNeed {
  rentalOrderItemId?: number;
  modelId: string;
  modelName: string;
  quantity: number;
  assignedDeviceIds: string[]; // DeviceInstance IDs allocated for this item
}

export interface RentalOrder {
  id: string;
  rentalOrderId?: number;
  shopId?: number;
  orderNumber: string;
  orderStatus?: string;
  conversionStatus?: string;
  channel: OrderChannel;
  customerName: string;
  customerPhone: string;
  receiverName?: string;
  receiverPhone?: string;
  receiverAddress?: string;
  startDate: string; // YYYY-MM-DD, only populated from backend parsed rental period
  endDate: string;   // YYYY-MM-DD, only populated from backend parsed rental period
  occupyStartDate: string;
  occupyEndDateExclusive: string;
  rentalPeriodLabel: string;
  rentalPeriodReady: boolean;
  rentalPeriodStatus?: string;
  rentalPeriodReasonCode?: string;
  status: OrderStatus;
  items: OrderItemNeed[];
  totalPrice: number;
  deposit: number;
  createdTime: string;
  logisticsNumber?: string; // 物流运单号 e.g. "SF1893029104"
  expressCode?: string;
  expressName?: string;
  canAssign: boolean;
  canShip: boolean;
  canReturn: boolean;
  boundTime?: string;       // 绑单完成时间
  note?: string;
}

export interface ScheduleBlock {
  id: string;
  deviceId: string;
  orderId?: string;
  orderNumber?: string;
  type: 'RENTAL' | 'RESERVE' | 'REPAIR' | 'LOCK';
  startDate: string; // YYYY-MM-DD
  endDate: string;   // YYYY-MM-DD
  customerName?: string;
  logisticsNumber?: string; // 物流运单号
  statusText?: string;
}

export type ExceptionType = 'OVERDUE' | 'UNASSIGNED_ALERT' | 'INSPECTION_NEEDED' | 'CONFLICT';

export interface ExceptionItem {
  id: string;
  type: ExceptionType;
  title: string;
  description: string;
  relatedOrderId?: string;
  relatedDeviceId?: string;
  severity: 'high' | 'medium' | 'low';
  createdTime: string;
  resolved: boolean;
}
