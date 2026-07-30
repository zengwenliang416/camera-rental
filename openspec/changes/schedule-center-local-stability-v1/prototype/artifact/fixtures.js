export const productionSnapshot = {
  capturedAt: '2026-07-29 17:12',
  tenantId: 1,
  channelOrders: 868,
  internalOrders: 2,
  registeredDevices: 4,
  actualAssetCount: null,
  activeRentals: 46,
  shippingToday: 21,
  returnsToday: 5,
  openReviews: 755,
  openAlerts: 71,
  shops: { valid: 3, invalid: 3 },
  deviceStatus: { available: 2, rented: 2, maintenance: 0 },
  channelStatus: [
    ['22', '交易成功', 359],
    ['21', '已发货', 213],
    ['12', '待发货', 151],
    ['23', '已退款', 85],
    ['24', '交易关闭', 58],
    ['11', '待付款', 2],
  ],
  conversion: [
    ['需人工复核', 718],
    ['待转换', 148],
    ['已转换', 2],
  ],
  receiverCompleteness: [
    ['运单号', 289],
    ['发货时间', 575],
    ['收货人姓名', 223],
    ['收货电话', 213],
  ],
};

export const dates = [
  ['07-29', '周三', 'Wed'],
  ['07-30', '周四', 'Thu'],
  ['07-31', '周五', 'Fri'],
  ['08-01', '周六', 'Sat'],
  ['08-02', '周日', 'Sun'],
  ['08-03', '周一', 'Mon'],
  ['08-04', '周二', 'Tue'],
  ['08-05', '周三', 'Wed'],
  ['08-06', '周四', 'Thu'],
  ['08-07', '周五', 'Fri'],
].map(([short, zh, en], index) => ({ short, zh, en, today: index === 0 }));

export const workbenchLanes = [
  {
    id: 'ship',
    title: ['今日渠道发货', 'Channel shipments today'],
    count: 21,
    tone: 'blue',
    rows: [
      ['今日', '发货日期汇总', '命中 21 个渠道订单', '6 个店铺', '脱敏聚合', '待发货'],
      ['累计', '渠道状态 12', '待发货 151 单', '闲鱼', '管理端状态', '待发货'],
      ['累计', '渠道状态 21', '已发货 213 单', '闲鱼', '管理端状态', '已发货'],
    ],
  },
  {
    id: 'assign',
    title: ['内部订单与设备', 'Internal orders and devices'],
    count: 2,
    tone: 'amber',
    rows: [
      ['07-29', 'XY-0****0812', 'P4P · ¥160.00', 'P4P-01', '占用至 08-07', '状态待校准'],
      ['07-29', 'XY-0****0803', 'A6 · ¥139.93', 'A6-09', '占用至 08-07', '状态待校准'],
    ],
  },
  {
    id: 'return',
    title: ['今日归还与在租', 'Returns and active rentals'],
    count: 5,
    tone: 'green',
    rows: [
      ['今日', '归还日期汇总', '计划归还 5 单', '渠道订单', '租期字段', '待归还'],
      ['当前', '计租中汇总', '当前计租 46 单', '渠道订单', '07-29 快照', '履约中'],
      ['系统', '已建档设备', '2 台设备在租', 'P4P / A6', '实例状态', '占用'],
    ],
  },
  {
    id: 'review',
    title: ['人工复核积压', 'Manual review backlog'],
    count: 755,
    tone: 'ink',
    rows: [
      ['开放', '缺少卖家备注', '527 单', '渠道订单', '复核原因', '待复核'],
      ['开放', '商品映射缺失', '173 单', '渠道订单', '仅 2 个型号已映射', '待复核'],
      ['开放', '未找到租期', '54 单', '渠道订单', '租期解析', '待复核'],
      ['开放', '物流区间异常', '1 单', '渠道订单', '日期校验', '待复核'],
    ],
  },
];

export const scheduleGroups = [
  {
    model: 'P3',
    category: '设备型号代码',
    units: [
      { unit: 'P3-05-5WTCN7F002B088', sn: '5WTCN7F002B088', status: '可用', segments: [] },
    ],
  },
  {
    model: 'P4',
    category: '设备型号代码',
    units: [
      { unit: 'P4-105-ANGZNB8002TP18', sn: 'ANGZNB8002TP18', status: '可用', segments: [] },
    ],
  },
  {
    model: 'P4P',
    category: '设备型号代码',
    units: [
      {
        unit: 'P4P-01-ANHXP5L0022JCW',
        sn: 'SN 待补录',
        status: '占用',
        segments: [
          ['occupied', 1, 10, 'XY-0****0812 · 占用'],
          ['billable', 2, 9, '07-30 至 08-07 · 计租'],
        ],
      },
    ],
  },
  {
    model: 'A6',
    category: '设备型号代码',
    units: [
      {
        unit: 'A6-09-9KRXNAC00B-405D',
        sn: '9KRXNAC00B-405D',
        status: '占用',
        segments: [
          ['occupied', 1, 10, 'XY-0****0803 · 占用'],
          ['billable', 3, 8, '07-31 至 08-07 · 计租'],
        ],
      },
    ],
  },
];

export const availabilityRows = [
  ['P3', '已建档型号', 1, 1, 0, Array(10).fill('1/1')],
  ['P4', '已建档型号', 1, 1, 0, Array(10).fill('1/1')],
  ['P4P', '已建档型号', 1, 0, 0, Array(10).fill('0/1')],
  ['A6', '已建档型号', 1, 0, 0, Array(10).fill('0/1')],
];

export const serialRows = [
  ['5WTCN7F002B088', '可用', '库位待补录', 0, 0, '可用'],
  ['ANGZNB8002TP18', '可用', '库位待补录', 0, 0, '可用'],
  ['SN 待补录', '占用', '库位待补录', 1, 10, 'XY-0****0812'],
  ['9KRXNAC00B-405D', '占用', '库位待补录', 1, 10, 'XY-0****0803'],
];

export const orders = [
  ['XY-0****0812', '闲鱼', '客户信息已脱敏', 'P4P', '07-30 至 08-07', '07-29 至 08-07', '状态待校准'],
  ['XY-0****0803', '闲鱼', '客户信息已脱敏', 'A6', '07-31 至 08-07', '07-29 至 08-07', '状态待校准'],
];

export const devices = [
  ['P4-105-ANGZNB8002TP18', 'ANGZNB8002TP18', 'P4', '库位待补录', '可用', '已启用', '立即可用'],
  ['P3-05-5WTCN7F002B088', '5WTCN7F002B088', 'P3', '库位待补录', '可用', '已启用', '立即可用'],
  ['P4P-01-ANHXP5L0022JCW', 'SN 待补录', 'P4P', '库位待补录', '占用', '已启用', '08-08'],
  ['A6-09-9KRXNAC00B-405D', '9KRXNAC00B-405D', 'A6', '库位待补录', '占用', '已启用', '08-08'],
];

export const deviceImportFields = [
  [['设备编号', 'Device ID'], 'deviceNo', ['必填 / 租户内唯一', 'Required / tenant-unique'], ['稳定资产身份与二维码主键', 'Stable asset identity and QR key']],
  [['机身序列号', 'Serial number'], 'serialNumber', ['强烈建议 / 有值时唯一', 'Strongly recommended / unique when present'], ['厂家 SN；P4P 当前待补录', 'Manufacturer SN; missing on current P4P']],
  [['型号代码', 'Model code'], 'equipmentModelCode', ['必填', 'Required'], ['先统一 P3、P4、P4P、A6 等标准代码', 'Normalize canonical codes such as P3, P4, P4P, and A6']],
  [['设备状态', 'Device status'], 'status', ['默认 AVAILABLE', 'Defaults to AVAILABLE'], ['AVAILABLE / RENTED / MAINTENANCE', 'AVAILABLE / RENTED / MAINTENANCE']],
  [['仓库或库位', 'Warehouse or location'], 'warehouseCode', ['建议必填', 'Recommended'], ['当前后端为单字段，先统一编码规则', 'One backend field today; normalize the coding rule first']],
  [['采购金额（分）', 'Purchase amount (cents)'], 'purchaseAmount', ['选填 / 整数分', 'Optional / integer cents'], ['禁止用浮点元写入', 'Never write floating-point currency']],
  [['是否启用', 'Enabled'], 'enabled', ['默认 true', 'Defaults to true'], ['停用设备不进入分配候选', 'Disabled devices are excluded from assignment']],
  [['来源标识', 'Source identity'], 'sourceType + sourceBizId + sourceItemId', ['系统字段', 'System managed'], ['ERP 入库生成时自动记录并保证幂等', 'Recorded by ERP inbound generation for idempotency']],
];

export const exceptions = [
  ['MR-001', '缺少卖家备注', '527 个开放复核项', '渠道订单聚合', '待复核', '当前'],
  ['MR-002', '商品映射', '173 个开放复核项', '仅 A6 / P4P 已映射', '待复核', '当前'],
  ['MR-003', '租期解析', '54 个开放复核项', '未找到有效租期', '待复核', '当前'],
  ['MR-004', '物流日期', '1 个开放复核项', '物流区间异常', '待复核', '当前'],
  ['AL-001', '售后超时', '60 个开放告警', '闲鱼售后', '关注', '当前'],
  ['AL-002', '同步失败', '8 个开放告警', '同步运行历史', '关注', '当前'],
  ['AL-003', '店铺授权', '3 个开放告警', '3 有效 / 3 失效', '阻塞', '当前'],
  ['AL-004', '自动转换', '写入租赁订单时 creator 缺失', '近期服务端日志', '阻塞', '近期'],
];
