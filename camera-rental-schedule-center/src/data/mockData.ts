import { EquipmentModel, ModelCategory, DeviceInstance, RentalOrder, ScheduleBlock, ExceptionItem } from '../types';

export const mockCategories: ModelCategory[] = [
  { id: 'cat-drone', name: '无人机' },
  { id: 'cat-camera', name: '相机主体' },
  { id: 'cat-lens', name: '专业镜头' },
  { id: 'cat-gimbal', name: '云台与稳定器' },
];

export const mockModels: EquipmentModel[] = [
  { id: 'p4p', name: '大疆 P4P', categoryId: 'cat-drone', totalUnits: 55 },
  { id: 'mavic3', name: '大疆 Mavic 3 Pro', categoryId: 'cat-drone', totalUnits: 18 },
  { id: 'air3', name: 'DJI Air 3', categoryId: 'cat-drone', totalUnits: 12 },
  { id: 'a7m4', name: 'Sony A7M4', categoryId: 'cat-camera', totalUnits: 20 },
  { id: 'a7s3', name: 'Sony A7S3', categoryId: 'cat-camera', totalUnits: 8 },
  { id: 'fx3', name: 'Sony FX3', categoryId: 'cat-camera', totalUnits: 6 },
  { id: 'lens2470', name: '24-70mm F2.8 GM II', categoryId: 'cat-lens', totalUnits: 15 },
  { id: 'lens70200', name: '70-200mm F2.8 GM II', categoryId: 'cat-lens', totalUnits: 8 },
  { id: 'rs4pro', name: 'DJI RS4 Pro 云台', categoryId: 'cat-gimbal', totalUnits: 12 },
];

// Seed generator for P4P 55 units + other units
function generateDevices(): DeviceInstance[] {
  const devices: DeviceInstance[] = [];

  // P4P - 55 units
  const p4pSns = [
    'ANHXP5L002-2JCW', 'ANHXP5H002-28RW', 'ANHXP63002-48XU', 'ANHXP6B002-58TJ', 'ANHXP5F002-2097',
    'ANHXP5J002-2C86', 'ANHXP64002-4GDV', 'ANHXP5X002-311A', 'ANHXP69002-880B', 'ANHXP5Y002-710C',
    'ANHXP6A002-339K', 'ANHXP5R002-12LK', 'ANHXP62002-90PQ', 'ANHXP5W002-44MN', 'ANHXP65002-11ZX',
    'ANHXP67002-333A', 'ANHXP68002-222B', 'ANHXP6C002-555C', 'ANHXP6D002-777D', 'ANHXP6E002-888E',
  ];

  for (let i = 1; i <= 55; i++) {
    const unitCode = `${i.toString().padStart(2, '0')}号`;
    const snIndex = (i - 1) % p4pSns.length;
    const snSuffix = i > 20 ? `-${i}` : '';
    const sn = `${p4pSns[snIndex]}${snSuffix}`;

    let status: DeviceInstance['status'] = 'IDLE';
    let currentOrderId: string | undefined;
    let currentCustomer: string | undefined;
    let currentPeriod: DeviceInstance['currentPeriod'] | undefined;
    let expectedAvailableDate: string | undefined = '立即可用';

    if (i === 2) {
      status = 'RENTING';
      currentOrderId = 'XY20260726001';
      currentCustomer = '李*华';
      currentPeriod = { startDate: '2026-07-25', endDate: '2026-07-30' };
      expectedAvailableDate = '2026-08-01';
    } else if (i === 3) {
      status = 'RESERVED';
      currentOrderId = 'XY20260727003';
      currentCustomer = '王*强';
      currentPeriod = { startDate: '2026-08-01', endDate: '2026-08-05' };
      expectedAvailableDate = '2026-08-06';
    } else if (i === 4) {
      status = 'PENDING_RETURN';
      currentOrderId = 'XY20260723008';
      currentCustomer = '陈*杰';
      currentPeriod = { startDate: '2026-07-23', endDate: '2026-07-27' };
      expectedAvailableDate = '待检查回仓';
    } else if (i === 5) {
      status = 'REPAIR';
      expectedAvailableDate = '2026-08-03 (云台校准)';
    } else if (i === 12) {
      status = 'RENTING';
      currentOrderId = 'XY20260726005';
      currentCustomer = '周*明';
      currentPeriod = { startDate: '2026-07-26', endDate: '2026-07-31' };
      expectedAvailableDate = '2026-08-01';
    } else if (i === 15) {
      status = 'LOCKED';
      expectedAvailableDate = '人工锁定 (备用机)';
    } else if (i === 18) {
      status = 'PENDING_RETURN';
      currentOrderId = 'OFF2026072402';
      currentCustomer = '成都天府影业';
      currentPeriod = { startDate: '2026-07-24', endDate: '2026-07-26' };
      expectedAvailableDate = '逾期待收回';
    }

    devices.push({
      id: `dev-p4p-${i}`,
      unitCode,
      sn,
      modelId: 'p4p',
      modelName: '大疆 P4P',
      status,
      currentOrderId,
      currentCustomer,
      currentPeriod,
      expectedAvailableDate,
    });
  }

  // Sony A7M4 - 20 units
  for (let i = 1; i <= 20; i++) {
    const unitCode = `${i.toString().padStart(2, '0')}号`;
    const sn = `S01A7M4-${(1000 + i).toString()}`;
    let status: DeviceInstance['status'] = 'IDLE';
    let currentOrderId: string | undefined;
    let currentCustomer: string | undefined;
    let currentPeriod: DeviceInstance['currentPeriod'] | undefined;

    if (i === 1 || i === 3) {
      status = 'RENTING';
      currentOrderId = 'XY20260726002';
      currentCustomer = '张*峰 (婚礼摄影)';
      currentPeriod = { startDate: '2026-07-26', endDate: '2026-07-29' };
    } else if (i === 2) {
      status = 'REPAIR';
    }

    devices.push({
      id: `dev-a7m4-${i}`,
      unitCode,
      sn,
      modelId: 'a7m4',
      modelName: 'Sony A7M4',
      status,
      currentOrderId,
      currentCustomer,
      currentPeriod,
      expectedAvailableDate: status === 'IDLE' ? '立即可用' : '2026-07-30',
    });
  }

  // 24-70 GM II - 15 units
  for (let i = 1; i <= 15; i++) {
    const unitCode = `${i.toString().padStart(2, '0')}号`;
    const sn = `L2470GM2-${(2000 + i).toString()}`;
    let status: DeviceInstance['status'] = 'IDLE';
    if (i === 1) {
      status = 'RENTING';
    }
    devices.push({
      id: `dev-2470-${i}`,
      unitCode,
      sn,
      modelId: 'lens2470',
      modelName: '24-70mm F2.8 GM II',
      status,
      expectedAvailableDate: '立即可用',
    });
  }

  // RS4 Pro - 12 units
  for (let i = 1; i <= 12; i++) {
    devices.push({
      id: `dev-rs4-${i}`,
      unitCode: `${i.toString().padStart(2, '0')}号`,
      sn: `DJIRS4P-${(3000 + i).toString()}`,
      modelId: 'rs4pro',
      modelName: 'DJI RS4 Pro 云台',
      status: i === 2 ? 'RENTING' : 'IDLE',
      expectedAvailableDate: '立即可用',
    });
  }

  return devices;
}

export const initialDevices = generateDevices();

export const initialOrders: RentalOrder[] = [
  {
    id: 'ord-001',
    orderNumber: 'XY34982103',
    channel: 'XIANYU',
    customerName: '张** (闲鱼买家)',
    customerPhone: '138****9281',
    startDate: '2026-08-01',
    endDate: '2026-08-05',
    status: 'UNASSIGNED', // 待排期
    items: [
      {
        modelId: 'p4p',
        modelName: '大疆 P4P',
        quantity: 3,
        assignedDeviceIds: [], // 需要分配 3 台 P4P
      },
    ],
    totalPrice: 1200,
    deposit: 3000,
    createdTime: '2026-07-27 01:15',
    note: '买家要求打包附送2块额外电池，送达成都市锦江区',
  },
  {
    id: 'ord-002',
    orderNumber: 'XY34982188',
    channel: 'XIANYU',
    customerName: '婚礼摄制组-赵工',
    customerPhone: '186****3321',
    startDate: '2026-08-02',
    endDate: '2026-08-04',
    status: 'UNASSIGNED', // 待排期
    items: [
      { modelId: 'a7m4', modelName: 'Sony A7M4', quantity: 2, assignedDeviceIds: [] },
      { modelId: 'lens2470', modelName: '24-70mm F2.8 GM II', quantity: 1, assignedDeviceIds: [] },
      { modelId: 'rs4pro', modelName: 'DJI RS4 Pro 云台', quantity: 1, assignedDeviceIds: [] },
    ],
    totalPrice: 1850,
    deposit: 5000,
    createdTime: '2026-07-27 02:40',
    note: '婚礼双机位套装需求',
  },
  {
    id: 'ord-003',
    orderNumber: 'XY20260726001',
    channel: 'XIANYU',
    customerName: '李*华',
    customerPhone: '177****1092',
    startDate: '2026-07-25',
    endDate: '2026-07-30',
    status: 'RENTING',
    items: [
      { modelId: 'p4p', modelName: '大疆 P4P', quantity: 1, assignedDeviceIds: ['dev-p4p-2'] },
    ],
    totalPrice: 500,
    deposit: 2000,
    createdTime: '2026-07-24 14:20',
  },
  {
    id: 'ord-004',
    orderNumber: 'XY20260726002',
    channel: 'XIANYU',
    customerName: '张*峰 (婚礼摄影)',
    customerPhone: '159****4420',
    startDate: '2026-07-26',
    endDate: '2026-07-29',
    status: 'RENTING',
    items: [
      { modelId: 'a7m4', modelName: 'Sony A7M4', quantity: 2, assignedDeviceIds: ['dev-a7m4-1', 'dev-a7m4-3'] },
      { modelId: 'lens2470', modelName: '24-70mm F2.8 GM II', quantity: 1, assignedDeviceIds: ['dev-2470-1'] },
    ],
    totalPrice: 1400,
    deposit: 4000,
    createdTime: '2026-07-25 18:30',
  },
  {
    id: 'ord-005',
    orderNumber: 'XY20260723008',
    channel: 'XIANYU',
    customerName: '陈*杰',
    customerPhone: '133****8811',
    startDate: '2026-07-23',
    endDate: '2026-07-27',
    status: 'PENDING_RETURN',
    items: [
      { modelId: 'p4p', modelName: '大疆 P4P', quantity: 1, assignedDeviceIds: ['dev-p4p-4'] },
    ],
    totalPrice: 400,
    deposit: 2000,
    createdTime: '2026-07-22 09:10',
  },
  {
    id: 'ord-006',
    orderNumber: 'OFF2026072402',
    channel: 'OFFLINE',
    customerName: '成都天府影业',
    customerPhone: '028-8549****',
    startDate: '2026-07-24',
    endDate: '2026-07-26',
    status: 'EXCEPTION', // 逾期未还
    items: [
      { modelId: 'p4p', modelName: '大疆 P4P', quantity: 1, assignedDeviceIds: ['dev-p4p-18'] },
    ],
    totalPrice: 600,
    deposit: 3000,
    createdTime: '2026-07-23 16:00',
    note: '客户电话告知因拍摄延期，尚未归还，需沟通续租',
  },
  {
    id: 'ord-007',
    orderNumber: 'XY34983301',
    channel: 'XIANYU',
    customerName: '王*亮',
    customerPhone: '181****0023',
    startDate: '2026-07-27',
    endDate: '2026-07-31',
    status: 'PENDING_DISPATCH', // 待出库
    items: [
      { modelId: 'p4p', modelName: '大疆 P4P', quantity: 2, assignedDeviceIds: ['dev-p4p-7', 'dev-p4p-8'] },
    ],
    totalPrice: 850,
    deposit: 3000,
    createdTime: '2026-07-26 21:00',
    note: '今日上午 11:00 闪送取件',
  },
];

export const initialScheduleBlocks: ScheduleBlock[] = [
  {
    id: 'blk-1',
    deviceId: 'dev-p4p-2',
    orderId: 'ord-003',
    orderNumber: 'XY20260726001',
    type: 'RENTAL',
    startDate: '2026-07-25',
    endDate: '2026-07-30',
    customerName: '李*华',
    statusText: '租赁中',
  },
  {
    id: 'blk-2',
    deviceId: 'dev-p4p-3',
    orderId: 'ord-003-res',
    orderNumber: 'XY20260727003',
    type: 'RESERVE',
    startDate: '2026-08-01',
    endDate: '2026-08-05',
    customerName: '王*强',
    statusText: '已预留',
  },
  {
    id: 'blk-3',
    deviceId: 'dev-p4p-4',
    orderId: 'ord-005',
    orderNumber: 'XY20260723008',
    type: 'RENTAL',
    startDate: '2026-07-23',
    endDate: '2026-07-27',
    customerName: '陈*杰',
    statusText: '待归还',
  },
  {
    id: 'blk-4',
    deviceId: 'dev-p4p-5',
    type: 'REPAIR',
    startDate: '2026-07-20',
    endDate: '2026-08-03',
    statusText: '维保校准',
  },
  {
    id: 'blk-5',
    deviceId: 'dev-p4p-18',
    orderId: 'ord-006',
    orderNumber: 'OFF2026072402',
    type: 'RENTAL',
    startDate: '2026-07-24',
    endDate: '2026-07-26',
    customerName: '成都天府影业',
    statusText: '逾期未归还',
  },
];

export const initialExceptions: ExceptionItem[] = [
  {
    id: 'exp-1',
    type: 'OVERDUE',
    title: '订单 OFF2026072402 归还逾期',
    description: '设备 [大疆 P4P 18号] 应于 7/26 18:00 前归还，已逾期 1 天。客户：成都天府影业',
    relatedOrderId: 'ord-006',
    relatedDeviceId: 'dev-p4p-18',
    severity: 'high',
    createdTime: '2026-07-27 00:00',
    resolved: false,
  },
  {
    id: 'exp-2',
    type: 'UNASSIGNED_ALERT',
    title: '闲鱼订单 XY34982103 尚未排机',
    description: '客户预订 3 台大疆 P4P (8/1~8/5)，需在出库前完成设备绑定。',
    relatedOrderId: 'ord-001',
    severity: 'medium',
    createdTime: '2026-07-27 01:15',
    resolved: false,
  },
  {
    id: 'exp-3',
    type: 'INSPECTION_NEEDED',
    title: '设备 05号 (ANHXP5F002-2097) 维保待检测',
    description: '云台轴向偏移，预估 8/3 完成修复。',
    relatedDeviceId: 'dev-p4p-5',
    severity: 'low',
    createdTime: '2026-07-20 10:00',
    resolved: false,
  },
];
