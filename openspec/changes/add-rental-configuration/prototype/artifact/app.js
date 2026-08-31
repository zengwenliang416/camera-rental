'use strict';

const shell = document.querySelector('#app-shell');
const drawer = document.querySelector('#rule-drawer');
const overlay = document.querySelector('#drawer-overlay');
const impactDialog = document.querySelector('#impact-dialog');
const stateSelect = document.querySelector('#state-select');
const themeLabel = document.querySelector('#theme-label');
const localeLabel = document.querySelector('#locale-label');
const toast = document.querySelector('#toast');

const messages = {
  'zh-CN': {
    brand: '相机租赁管理后台',
    home: '首页',
    rentalOps: '租赁运营',
    channelOrders: '渠道订单',
    rentalDevices: '租赁设备',
    rentalConfig: '租赁配置',
    schedules: '设备排期',
    manualReview: '人工复核',
    reports: '经营报表',
    prototypeData: '仅使用虚构原型数据',
    stateLabel: '原型状态',
    statePopulated: '正常数据',
    stateLoading: '加载中',
    stateEmpty: '空数据',
    stateError: '加载失败',
    statePermission: '无权限',
    stateDisabled: '禁用操作',
    admin: '管理员',
    configCenter: 'RENTAL CONFIGURATION',
    pageTitle: '租赁配置',
    pageDescription: '统一维护设备目录、闲鱼商品精确映射和卖家备注规范。配置不会直接覆盖已发生的履约事实。',
    skippedRules: '过滤规则',
    waitingMapping: '待型号配置',
    shopsSynced: '已同步店铺',
    tabCatalog: '设备目录',
    tabRules: '渠道商品规则',
    tabRemarks: '闲鱼备注规范',
    emptyTitle: '暂无配置数据',
    emptyCopy: '先同步店铺和商品，再创建第一条精确映射规则。',
    syncProducts: '同步商品',
    errorTitle: '租赁配置加载失败',
    errorCopy: '未展示为空表，请检查服务状态后安全重试。',
    retry: '重新加载',
    permissionCopy: '当前账号没有租赁配置读取权限。',
    catalogTitle: '设备目录',
    catalogHint: '大类和型号在此维护；租赁设备页只负责选择目录和创建设备。',
    newCategory: '新增大类',
    categories: '设备大类',
    camera: '相机',
    lens: '镜头',
    drone: '无人机',
    accessory: '配件',
    enabled: '启用',
    disabled: '停用',
    models: '相机型号',
    newModel: '新增型号',
    modelName: '型号名称',
    modelCode: '型号编码',
    prefix: '设备编号前缀',
    status: '状态',
    actions: '操作',
    edit: '编辑',
    rulesTitle: '渠道商品规则',
    rulesHint: '规则按当前租户内的店铺和闲鱼商品 ID 唯一匹配，所有外部标识均按字符串保存。',
    newRule: '新增规则',
    shop: '店铺',
    allShops: '全部店铺',
    xianyuItemId: '闲鱼商品 ID',
    handlingPolicy: '处理策略',
    all: '全部',
    mappingStatus: '型号状态',
    configured: '已配置',
    query: '查询',
    reset: '重置',
    noFallbackNotice: '多型号规则只按已同步闲管家 SKU 精确匹配。SKU 未同步或未配置时，订单保持“待型号配置”，不会回退商品默认型号。',
    shopProduct: '店铺 / 商品',
    identifiers: '渠道标识',
    modelMode: '型号识别',
    mappingResult: '型号映射',
    singleProduct: '口袋云台单机租赁',
    xgjProductId: '闲管家商品 ID',
    singleModel: '单型号',
    multiProduct: '微单相机多规格租赁',
    bySku: '按 SKU',
    mappedSkuCount: '已配置 SKU',
    editMapping: '配置映射',
    syncedSku: '已同步规格',
    syncedSkuHint: '只能选择这些 SKU，不能手工输入或按文本猜测',
    syncTime: '同步于 08-31 09:20',
    xgjSkuId: '闲管家 SKU ID',
    xianyuSkuId: '闲鱼 SKU ID',
    skuDisplay: '规格展示（仅辅助）',
    equipmentModel: '设备型号',
    notSelected: '未选择',
    skipProduct: '展示与押金服务商品',
    noParsing: '不解析备注 / 不建内部订单',
    remarkTitle: '闲鱼卖家备注规范',
    remarkHint: '先写完整基础日期，特殊情况只在末尾追加一个清晰关键词，避免备注越来越复杂。',
    baseTemplates: '三种基础格式',
    deliveryTemplate: '快递常用',
    pickupTemplate: '自提常用',
    explicitTemplate: '明确租期',
    copy: '复制',
    remarkPreserve: '新备注为空、不完整或解析失败时保留上一次有效计划，不会清空正确日期。',
    specialCases: '八种特殊情况',
    renew: '续租',
    renewHint: '检查后续排期，无冲突才延长',
    earlyReturn: '早退',
    earlyHint: '检测完成前不释放占用',
    reschedule: '改期',
    rescheduleHint: '重新检查设备冲突',
    swap: '换机',
    damage: '损坏',
    lost: '遗失',
    overdue: '逾期',
    delay: '物流延误',
    reviewOnly: '仅创建运营提示或复核',
    disabledCopy: '当前规则正在重评，保存和启停操作暂时禁用；读取和查看影响范围仍可用。',
    saveRule: '保存规则',
    exactMapping: 'EXACT MAPPING',
    editRule: '编辑商品规则',
    scopeTitle: '唯一匹配范围',
    perSku: '按下方规格',
    policyAndMode: '处理策略与型号识别',
    singleModeHint: '整个商品固定对应一个型号',
    skuModeHint: '每个已同步规格分别对应型号',
    singleMapping: '商品级型号',
    skuMapping: '同步 SKU 映射',
    oneMissing: '1 项未配置',
    skuSelectionRule: 'SKU 标识来自商品同步，只能选择设备型号；不允许新增或改写 SKU。',
    selectModel: '请选择设备型号',
    noFallbackValidation: '未配置时相关订单保持“待型号配置”，不会使用商品默认型号。',
    cancel: '取消',
    previewSave: '预览影响并保存',
    safeChange: 'FULFILLMENT SAFE',
    impactTitle: '确认规则变更影响',
    affectedOrders: '受影响订单',
    autoUpdate: '可自动更新',
    manualReviewCount: '进入人工复核',
    completedProtected: '已履约，仅保留历史',
    impactWarning: '已分配或已出库订单不会自动更换型号或设备；不一致项将进入人工复核。确认后由后端异步重评。',
    saveConfig: '保存配置',
    saveConfigHint: '校验店铺、商品、SKU 归属和版本',
    reconcile: '异步重评',
    reconcileHint: '只更新允许自动变化的计划字段',
    audit: '保留审计',
    auditHint: '记录配置版本、结果和冲突原因',
    backEdit: '返回修改',
    confirmSave: '确认保存并重评',
    copied: '备注格式已复制',
    saved: '配置已被原型接受，异步重评任务已创建',
  },
  en: {
    brand: 'Camera Rental Admin',
    home: 'Home',
    rentalOps: 'Rental Operations',
    channelOrders: 'Channel Orders',
    rentalDevices: 'Rental Devices',
    rentalConfig: 'Rental Configuration',
    schedules: 'Device Schedule',
    manualReview: 'Manual Review',
    reports: 'Business Reports',
    prototypeData: 'Fictional prototype data only',
    stateLabel: 'Prototype state',
    statePopulated: 'Populated',
    stateLoading: 'Loading',
    stateEmpty: 'Empty',
    stateError: 'Error',
    statePermission: 'Permission',
    stateDisabled: 'Disabled',
    admin: 'Administrator',
    configCenter: 'RENTAL CONFIGURATION',
    pageTitle: 'Rental Configuration',
    pageDescription: 'Manage the device catalog, exact Xianyu product mapping, and seller remark conventions without overwriting fulfillment facts.',
    skippedRules: 'Skip rules',
    waitingMapping: 'Awaiting model',
    shopsSynced: 'Synced shops',
    tabCatalog: 'Device Catalog',
    tabRules: 'Channel Product Rules',
    tabRemarks: 'Xianyu Remark Guide',
    emptyTitle: 'No configuration yet',
    emptyCopy: 'Sync shops and products before creating the first exact mapping rule.',
    syncProducts: 'Sync products',
    errorTitle: 'Rental configuration failed to load',
    errorCopy: 'This is not shown as an empty table. Check the service and retry safely.',
    retry: 'Reload',
    permissionCopy: 'This account cannot read rental configuration.',
    catalogTitle: 'Device Catalog',
    catalogHint: 'Maintain categories and models here. Rental Devices only consumes the catalog and creates devices.',
    newCategory: 'New category',
    categories: 'Device categories',
    camera: 'Camera',
    lens: 'Lens',
    drone: 'Drone',
    accessory: 'Accessory',
    enabled: 'Enabled',
    disabled: 'Disabled',
    models: 'Camera models',
    newModel: 'New model',
    modelName: 'Model name',
    modelCode: 'Model code',
    prefix: 'Device number prefix',
    status: 'Status',
    actions: 'Actions',
    edit: 'Edit',
    rulesTitle: 'Channel Product Rules',
    rulesHint: 'A rule is unique by tenant, shop, and Xianyu item ID. External identifiers are stored as strings.',
    newRule: 'New rule',
    shop: 'Shop',
    allShops: 'All shops',
    xianyuItemId: 'Xianyu item ID',
    handlingPolicy: 'Handling policy',
    all: 'All',
    mappingStatus: 'Model status',
    configured: 'Configured',
    query: 'Search',
    reset: 'Reset',
    noFallbackNotice: 'Multi-model rules match only synchronized XianGuanJia SKUs. Missing mappings remain “Awaiting model” and never fall back to a product default.',
    shopProduct: 'Shop / Product',
    identifiers: 'Channel identifiers',
    modelMode: 'Model resolution',
    mappingResult: 'Model mapping',
    singleProduct: 'Pocket gimbal body rental',
    xgjProductId: 'XianGuanJia product ID',
    singleModel: 'Single model',
    multiProduct: 'Multi-SKU mirrorless rental',
    bySku: 'By SKU',
    mappedSkuCount: 'mapped SKUs',
    editMapping: 'Configure mapping',
    syncedSku: 'Synchronized SKUs',
    syncedSkuHint: 'Select from these SKUs only. Manual IDs and text inference are not allowed.',
    syncTime: 'Synced Aug 31, 09:20',
    xgjSkuId: 'XianGuanJia SKU ID',
    xianyuSkuId: 'Xianyu SKU ID',
    skuDisplay: 'Display name (reference only)',
    equipmentModel: 'Equipment model',
    notSelected: 'Not selected',
    skipProduct: 'Display and deposit service item',
    noParsing: 'No remark parsing / no internal order',
    remarkTitle: 'Xianyu Seller Remark Convention',
    remarkHint: 'Write complete base dates first, then append one clear exception keyword when needed.',
    baseTemplates: 'Three base formats',
    deliveryTemplate: 'Delivery',
    pickupTemplate: 'Pickup',
    explicitTemplate: 'Explicit rental period',
    copy: 'Copy',
    remarkPreserve: 'An empty, incomplete, or invalid new remark preserves the last valid plan instead of clearing correct dates.',
    specialCases: 'Eight exception cases',
    renew: 'Extension',
    renewHint: 'Extend only when future allocation has no conflict',
    earlyReturn: 'Early return',
    earlyHint: 'Do not release occupation before inspection',
    reschedule: 'Reschedule',
    rescheduleHint: 'Recheck device conflicts',
    swap: 'Device swap',
    damage: 'Damage',
    lost: 'Loss',
    overdue: 'Overdue',
    delay: 'Logistics delay',
    reviewOnly: 'Create an operations alert or review only',
    disabledCopy: 'This rule is being reconciled. Save and enable actions are disabled, while read and impact review remain available.',
    saveRule: 'Save rule',
    exactMapping: 'EXACT MAPPING',
    editRule: 'Edit Product Rule',
    scopeTitle: 'Unique match scope',
    perSku: 'See synchronized SKUs below',
    policyAndMode: 'Handling policy and model resolution',
    singleModeHint: 'One model for the entire product',
    skuModeHint: 'One model per synchronized SKU',
    singleMapping: 'Product-level model',
    skuMapping: 'Synchronized SKU mapping',
    oneMissing: '1 missing mapping',
    skuSelectionRule: 'SKU identifiers come from synchronization. You may select a model, but cannot add or rewrite a SKU.',
    selectModel: 'Select an equipment model',
    noFallbackValidation: 'Related orders remain “Awaiting model” and never use a product default.',
    cancel: 'Cancel',
    previewSave: 'Preview impact and save',
    safeChange: 'FULFILLMENT SAFE',
    impactTitle: 'Confirm Rule Change Impact',
    affectedOrders: 'Affected orders',
    autoUpdate: 'Auto-updatable',
    manualReviewCount: 'Manual review',
    completedProtected: 'Fulfilled, history only',
    impactWarning: 'Assigned or dispatched orders will not change model or device automatically. Mismatches enter manual review, then the backend reconciles asynchronously.',
    saveConfig: 'Save configuration',
    saveConfigHint: 'Validate shop, product, SKU ownership, and version',
    reconcile: 'Async reconciliation',
    reconcileHint: 'Update only plan fields that are safe to change',
    audit: 'Preserve audit trail',
    auditHint: 'Record rule version, result, and conflict reasons',
    backEdit: 'Back to editing',
    confirmSave: 'Confirm save and reconcile',
    copied: 'Remark format copied',
    saved: 'Prototype accepted the rule and created an asynchronous reconciliation run',
  },
};

let currentLocale = 'zh-CN';
let toastTimer;

function setState(state) {
  shell.setAttribute('data-specnav-state', state);
  stateSelect.value = state;
  closeDrawer();
  closeImpact();
}

function setTab(tabName) {
  document.querySelectorAll('[data-tab]').forEach((button) => {
    const isActive = button.dataset.tab === tabName;
    button.classList.toggle('active', isActive);
    button.setAttribute('aria-selected', String(isActive));
  });
  document.querySelectorAll('[data-panel]').forEach((panel) => {
    panel.classList.toggle('active', panel.dataset.panel === tabName);
  });
}

function setTheme(theme) {
  shell.dataset.theme = theme;
  themeLabel.textContent = theme === 'dark' ? 'Dark' : 'Light';
}

function applyLocale(locale) {
  currentLocale = locale;
  shell.dataset.locale = locale;
  document.documentElement.lang = locale;
  document.querySelectorAll('[data-i18n]').forEach((element) => {
    const key = element.dataset.i18n;
    const translation = messages[locale][key];
    if (translation) {
      element.textContent = translation;
    }
  });
  localeLabel.textContent = locale === 'zh-CN' ? '简体中文' : 'English';
}

function setRuleMode(mode) {
  const isSku = mode === 'sku';
  document.querySelector('input[name="mode"][value="single"]').checked = !isSku;
  document.querySelector('input[name="mode"][value="sku"]').checked = isSku;
  document.querySelector('#single-mode-form').classList.toggle('active', !isSku);
  document.querySelector('#sku-mode-form').classList.toggle('active', isSku);
  drawer.dataset.specnavVariant = isSku ? 'multi-sku-rule-drawer' : 'single-model-drawer';
}

function openDrawer(mode = 'sku') {
  setRuleMode(mode === 'single' ? 'single' : 'sku');
  overlay.classList.add('open');
  drawer.classList.add('open');
  overlay.setAttribute('aria-hidden', 'false');
  drawer.setAttribute('aria-hidden', 'false');
}

function closeDrawer() {
  drawer.classList.remove('open');
  if (!impactDialog.classList.contains('open')) {
    overlay.classList.remove('open');
    overlay.setAttribute('aria-hidden', 'true');
  }
  drawer.setAttribute('aria-hidden', 'true');
}

function openImpact() {
  drawer.classList.remove('open');
  drawer.setAttribute('aria-hidden', 'true');
  overlay.classList.add('open');
  overlay.setAttribute('aria-hidden', 'false');
  impactDialog.classList.add('open');
  impactDialog.setAttribute('aria-hidden', 'false');
}

function closeImpact() {
  impactDialog.classList.remove('open');
  impactDialog.setAttribute('aria-hidden', 'true');
  if (!drawer.classList.contains('open')) {
    overlay.classList.remove('open');
    overlay.setAttribute('aria-hidden', 'true');
  }
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add('show');
  toastTimer = window.setTimeout(() => toast.classList.remove('show'), 2400);
}

async function copyText(text) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      textarea.remove();
    }
    showToast(messages[currentLocale].copied);
  } catch {
    showToast(text);
  }
}

document.querySelectorAll('[data-tab]').forEach((button) => {
  button.addEventListener('click', () => setTab(button.dataset.tab));
});

document.querySelector('#theme-toggle').addEventListener('click', () => {
  setTheme(shell.dataset.theme === 'light' ? 'dark' : 'light');
});

document.querySelector('#locale-toggle').addEventListener('click', () => {
  applyLocale(currentLocale === 'zh-CN' ? 'en' : 'zh-CN');
});

stateSelect.addEventListener('change', (event) => setState(event.target.value));

document.querySelectorAll('[data-action="open-rule-drawer"]').forEach((button) => {
  button.addEventListener('click', () => openDrawer(button.dataset.mode || 'sku'));
});

document.querySelectorAll('[data-action="close-drawer"]').forEach((button) => {
  button.addEventListener('click', closeDrawer);
});

document.querySelector('[data-action="open-impact"]').addEventListener('click', openImpact);

document.querySelectorAll('[data-action="close-impact"]').forEach((button) => {
  button.addEventListener('click', closeImpact);
});

document.querySelector('[data-action="confirm-save"]').addEventListener('click', () => {
  closeImpact();
  showToast(messages[currentLocale].saved);
});

document.querySelector('[data-action="toggle-sku"]').addEventListener('click', (event) => {
  event.currentTarget.classList.toggle('expanded');
  document.querySelector('.sku-detail-row').classList.toggle('hidden');
});

document.querySelectorAll('input[name="mode"]').forEach((radio) => {
  radio.addEventListener('change', () => setRuleMode(radio.value));
});

document.querySelectorAll('[data-copy]').forEach((button) => {
  button.addEventListener('click', () => copyText(button.dataset.copy));
});

overlay.addEventListener('click', () => {
  closeDrawer();
  closeImpact();
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') {
    closeDrawer();
    closeImpact();
  }
});

setTheme('light');
applyLocale('zh-CN');
setTab('rules');

const reviewParams = new URLSearchParams(window.location.search);
const requestedTheme = reviewParams.get('theme');
const requestedLocale = reviewParams.get('locale');
const requestedState = reviewParams.get('state');
const requestedTab = reviewParams.get('tab');

if (requestedTheme === 'light' || requestedTheme === 'dark') {
  setTheme(requestedTheme);
}
if (requestedLocale === 'zh-CN' || requestedLocale === 'en') {
  applyLocale(requestedLocale);
}
if (['populated', 'loading', 'empty', 'error', 'permission', 'disabled'].includes(requestedState)) {
  setState(requestedState);
}
if (['catalog', 'rules', 'remarks'].includes(requestedTab)) {
  setTab(requestedTab);
}
if (reviewParams.get('drawer') === 'single' || reviewParams.get('drawer') === 'sku') {
  openDrawer(reviewParams.get('drawer'));
}
if (reviewParams.get('dialog') === 'impact') {
  openImpact();
}
