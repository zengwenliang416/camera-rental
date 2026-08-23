'use strict';

const shell = document.querySelector('.app-shell');
const stateButtons = Array.from(document.querySelectorAll('[data-set-state]'));
const themeButton = document.querySelector('[data-specnav-theme-control]');
const localeButton = document.querySelector('[data-specnav-locale-control]');

const copy = {
  'zh-CN': {
    appTitle: '相机租赁管理后台', home: '首页', authorNews: '作者动态', system: '系统管理',
    infrastructure: '基础设施', payment: '支付管理', reports: '报表管理', workflow: '工作流程',
    rentalOps: '租赁运营', xianyuIntegration: '闲管家集成', channelOrders: '渠道订单',
    rentalDevices: '租赁设备', manualReview: '人工复核', deviceSchedule: '设备排期',
    selectTenant: '请选择租户', tenantName: '捷租达', reviewState: '原型状态',
    reviewNote: '仅切换界面证据，不调用真实接口', permissionNote: '当前账号无发货权限，补录入口不可见',
    externalOrderId: '外部订单号', externalOrderPlaceholder: '完整或部分订单号', shop: '店铺',
    selectShop: '选择本地店铺', status: '订单状态', pleaseSelect: '请选择',
    conversionStatus: '转换状态', orderDate: '订单日期', rentalRange: '租期范围',
    shipDate: '发货日期', startDate: '开始日期', endDate: '结束日期', selectShipDate: '选择发货日期',
    query: '⌕ 查询', reset: '重置', moreFilters: '更多筛选⌄', syncAction: '↻ 补同步',
    reparseAction: '✣ 重解析备注', columnSettings: '⚙ 列设置',
    tableNote: '列表读本地库并展示完整运营信息；同步后按明确租期、物流日期和确定性位置规则解析备注。无法唯一判断时进入复核。',
    shopName: '店铺名称', paidAmount: '实付', express: '快递', goodsTitle: '商品标题',
    actions: '操作', shipped: '已发货', pendingShipment: '待发货', details: '详情',
    backfillAction: '补录出库设备', totalRows: '共 2 条', dialogTitle: '已发货订单补录出库设备',
    warningBody: '仅补齐本地设备、排期和物流记录，不会再次调用闲管家发货接口。',
    deviceNo: '实际设备编号', devicePlaceholder: '输入实际寄出的设备编号',
    deviceError: '请输入实际寄出的设备编号', waybill: '运单号',
    waybillError: '快递单号至少 10 位字母或数字', carrierCode: '快递公司编码',
    carrierName: '快递公司名称', consignTime: '实际发货时间', reason: '补录原因',
    reasonError: '请输入补录原因', conflictBody: '同一运单已绑定其他设备，未修改任何本地记录。',
    successBody: '设备 A7M4-0007 已关联订单并补录出库。', cancel: '取消', submit: '确认补录并出库'
  },
  en: {
    appTitle: 'Camera Rental Admin', home: 'Home', authorNews: 'Author news', system: 'System',
    infrastructure: 'Infrastructure', payment: 'Payment', reports: 'Reports', workflow: 'Workflow',
    rentalOps: 'Rental operations', xianyuIntegration: 'XianGuanJia', channelOrders: 'Channel orders',
    rentalDevices: 'Rental devices', manualReview: 'Manual review', deviceSchedule: 'Device schedule',
    selectTenant: 'Select tenant', tenantName: 'Jiezuda', reviewState: 'Prototype state',
    reviewNote: 'UI evidence only. No real API is called.',
    permissionNote: 'The account lacks shipping permission, so the backfill action is hidden.',
    externalOrderId: 'External order no.', externalOrderPlaceholder: 'Full or partial order number',
    shop: 'Shop', selectShop: 'Select a local shop', status: 'Order status', pleaseSelect: 'Select',
    conversionStatus: 'Conversion status', orderDate: 'Order date', rentalRange: 'Rental range',
    shipDate: 'Ship date', startDate: 'Start date', endDate: 'End date',
    selectShipDate: 'Select ship date', query: '⌕ Search', reset: 'Reset', moreFilters: 'More filters⌄',
    syncAction: '↻ Backfill sync', reparseAction: '✣ Reparse remarks', columnSettings: '⚙ Columns',
    tableNote: 'The list reads the local database and shows operational details. Remark parsing uses explicit rental dates, logistics dates, and deterministic location rules; ambiguous cases go to review.',
    shopName: 'Shop name', paidAmount: 'Paid', express: 'Express', goodsTitle: 'Product title',
    actions: 'Actions', shipped: 'Shipped', pendingShipment: 'Pending shipment', details: 'Details',
    backfillAction: 'Backfill device', totalRows: '2 records', dialogTitle: 'Backfill dispatched device',
    warningBody: 'This only restores local device, schedule, and logistics records. It will not call the remote shipment API again.',
    deviceNo: 'Actual device no.', devicePlaceholder: 'Enter the device that was actually shipped',
    deviceError: 'Enter the device that was actually shipped', waybill: 'Waybill',
    waybillError: 'The waybill must contain at least 10 letters or digits', carrierCode: 'Carrier code',
    carrierName: 'Carrier name', consignTime: 'Actual ship time', reason: 'Backfill reason',
    reasonError: 'Enter a backfill reason',
    conflictBody: 'This waybill is already bound to another device. No local records were changed.',
    successBody: 'Device A7M4-0007 is now linked and dispatched.', cancel: 'Cancel',
    submit: 'Confirm backfill'
  }
};

function setState(state) {
  shell.dataset.reviewState = state;
  stateButtons.forEach((button) => {
    button.setAttribute('aria-pressed', String(button.dataset.setState === state));
  });
}

function applyLocale(locale) {
  shell.dataset.locale = locale;
  document.documentElement.lang = locale;
  document.querySelectorAll('[data-i18n]').forEach((element) => {
    const value = copy[locale][element.dataset.i18n];
    if (value) element.textContent = value;
  });
  document.querySelectorAll('[data-i18n-placeholder]').forEach((element) => {
    const value = copy[locale][element.dataset.i18nPlaceholder];
    if (value) element.placeholder = value;
  });
  document.querySelector('[data-localized-value="carrier"]').value =
    locale === 'zh-CN' ? '顺丰速运' : 'SF Express';
  document.querySelector('[data-localized-value="reason"]').value =
    locale === 'zh-CN'
      ? '订单已在闲鱼后台发货，补录实际出库设备'
      : 'The order was shipped externally; restore the actual device dispatch';
}

stateButtons.forEach((button) => {
  button.addEventListener('click', () => setState(button.dataset.setState));
});

themeButton.addEventListener('click', () => {
  const theme = shell.dataset.theme === 'light' ? 'dark' : 'light';
  shell.dataset.theme = theme;
  themeButton.querySelector('.theme-icon').textContent = theme === 'light' ? '◐' : '◑';
});

localeButton.addEventListener('click', () => {
  applyLocale(shell.dataset.locale === 'zh-CN' ? 'en' : 'zh-CN');
});

document.querySelector('.submit').addEventListener('click', () => {
  setState('loading');
  window.setTimeout(() => setState('success'), 700);
});

const params = new URLSearchParams(window.location.search);
const requestedTheme = params.get('theme');
const requestedLocale = params.get('locale');
const requestedState = params.get('state');

if (requestedTheme === 'dark' || requestedTheme === 'light') {
  shell.dataset.theme = requestedTheme;
  themeButton.querySelector('.theme-icon').textContent = requestedTheme === 'light' ? '◐' : '◑';
}
applyLocale(requestedLocale === 'en' ? 'en' : 'zh-CN');
if (requestedState && stateButtons.some((button) => button.dataset.setState === requestedState)) {
  setState(requestedState);
}
