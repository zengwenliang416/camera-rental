const state = {
  screen: 'schedule',
  theme: 'light',
  locale: 'zh-CN',
  review: 'populated',
  drawerOrder: null,
  activePackage: 0,
};

const copy = {
  'zh-CN': {
    brand: '设备排期中心',
    search: '搜索订单、SN、客户或运单号',
    sync: '本地物流摘要 15:42',
    navSchedule: '设备排期',
    navShipment: '发货结果',
    navOperations: '物流运营',
    navRisks: '异常中心',
    architectureNote: '页面每 60 秒读取本地摘要，不直接查询快递100',
    loadingTitle: '正在读取物流摘要',
    loadingBody: '排期和筛选保持不变，完整轨迹仍按需加载。',
    emptyTitle: '当前窗口没有物流包裹',
    emptyBody: '尚未发货的订单不会创建物流追踪。',
    errorTitle: '本地物流摘要暂时不可用',
    errorBody: '不会直接回退到快递100查询，请保留页面状态后重试。',
    permissionTitle: '当前账号没有物流查看权限',
    permissionBody: '权限由管理端角色和租户数据范围共同控制。',
    providerDisabledTitle: '快递100追踪未启用',
    providerDisabledBody: '闲鱼发货仍然成功，本地包裹保留为待启用状态。',
    mappingTitle: '承运商编码待映射',
    mappingBody: '包裹已创建，但不会调用快递100。',
    throttledTitle: '主动查询受限频保护',
    throttledBody: '当前本地快照仍可查看，下一次允许查询时间为 16:08。',
    retry: '重试',
    review: '原型状态',
    statePopulated: '有数据',
    stateLoading: '加载',
    stateEmpty: '空数据',
    stateError: '错误',
    statePermission: '无权限',
    stateProvider: 'Provider 关闭',
    stateMapping: '待映射',
    stateThrottle: '限频',
    mobileSchedule: '排期',
    mobileShipment: '发货',
    mobileOperations: '运营',
    mobileRisks: '异常',
  },
  en: {
    brand: 'Equipment Schedule Center',
    search: 'Search order, SN, customer, or waybill',
    sync: 'Local tracking summary 15:42',
    navSchedule: 'Schedule',
    navShipment: 'Shipment result',
    navOperations: 'Logistics ops',
    navRisks: 'Exceptions',
    architectureNote: 'The page polls local summaries every 60 seconds and never queries Kuaidi100 directly',
    loadingTitle: 'Loading tracking summaries',
    loadingBody: 'Schedule and filters remain visible. Full traces load on demand.',
    emptyTitle: 'No tracked packages in this window',
    emptyBody: 'Orders without shipment do not create tracking records.',
    errorTitle: 'Local tracking summary is unavailable',
    errorBody: 'The UI never falls back to direct Kuaidi100 access. Keep state and retry.',
    permissionTitle: 'This account cannot view logistics',
    permissionBody: 'The admin role and tenant data scope remain authoritative.',
    providerDisabledTitle: 'Kuaidi100 tracking is disabled',
    providerDisabledBody: 'Xianyu shipment still succeeds and the local package remains disabled.',
    mappingTitle: 'Carrier mapping is required',
    mappingBody: 'The package exists locally but no Kuaidi100 request is sent.',
    throttledTitle: 'Provider query is throttled',
    throttledBody: 'The local snapshot remains available. Next query is allowed at 16:08.',
    retry: 'Retry',
    review: 'Prototype state',
    statePopulated: 'Populated',
    stateLoading: 'Loading',
    stateEmpty: 'Empty',
    stateError: 'Error',
    statePermission: 'Permission',
    stateProvider: 'Provider off',
    stateMapping: 'Mapping',
    stateThrottle: 'Throttled',
    mobileSchedule: 'Schedule',
    mobileShipment: 'Ship',
    mobileOperations: 'Ops',
    mobileRisks: 'Alerts',
  },
};

const words = {
  schedule: ['设备排期与物流', 'Schedule and tracking'],
  scheduleDescription: ['在占用区间内查看真实包裹状态；完整轨迹按需加载。', 'Review physical package status inside occupied ranges. Full traces load on demand.'],
  shipment: ['闲鱼发货后的物流追踪', 'Tracking after Xianyu shipment'],
  shipmentDescription: ['发货成功与快递100追踪解耦，失败不会回滚设备出库。', 'Shipment success is decoupled from provider tracking failure.'],
  operations: ['物流配置与失败任务', 'Logistics configuration and failed tasks'],
  operationsDescription: ['租户配置、承运商映射、异步任务和历史回填集中管理。', 'Manage tenant config, carrier mappings, async tasks, and backfill.'],
  risks: ['物流风险与异常', 'Logistics risks and exceptions'],
  risksDescription: ['物流只影响风险提示，不自动释放设备或完成回仓。', 'Tracking changes risk signals, never device availability or return completion.'],
};

const pick = (pair) => pair[state.locale === 'en' ? 1 : 0];
const badge = (label, tone) => `<span class="badge" data-tone="${tone}">${label}</span>`;

const deliveries = {
  'XY-3313****4097': {
    customer: '客户信息已脱敏',
    devices: ['P4P-01', 'LENS-400-09'],
    packages: [
      {
        direction: '寄出',
        carrier: '顺丰速运',
        waybill: 'SF****4432',
        status: '运输中',
        tone: 'blue',
        updated: '07-31 15:38',
        eta: '08-01',
        lastSync: '07-31 15:42',
        nextAllowed: '07-31 16:08',
        timeline: [
          ['07-31 15:38', '运输中', '快件已发往深圳转运中心'],
          ['07-31 10:21', '运输中', '快件到达广州转运中心'],
          ['07-30 20:15', '已揽收', '顺丰已揽收'],
          ['07-30 18:02', '电子信息', '快递公司已收到运单信息'],
        ],
      },
      {
        direction: '寄出 · 第 2 包',
        carrier: '京东物流',
        waybill: 'JD****8048',
        status: '已签收',
        tone: 'green',
        updated: '07-31 13:06',
        eta: null,
        lastSync: '07-31 13:08',
        nextAllowed: '已关闭主动查询',
        timeline: [
          ['07-31 13:06', '已签收', '包裹已由收件人签收'],
          ['07-31 09:10', '派送中', '配送员正在派送'],
          ['07-30 22:40', '运输中', '包裹到达长沙分拣中心'],
        ],
      },
    ],
  },
  'XY-5125****4703': {
    customer: '客户信息已脱敏',
    devices: ['A6-09'],
    packages: [
      {
        direction: '寄出',
        carrier: '顺丰速运',
        waybill: 'SF****3291',
        status: '待揽收',
        tone: 'amber',
        updated: '07-31 14:12',
        eta: null,
        lastSync: '07-31 15:12',
        nextAllowed: '07-31 15:42',
        timeline: [['07-31 14:12', '电子信息', '快递公司已收到运单信息']],
      },
    ],
  },
  'XY-5124****5839': {
    customer: '客户信息已脱敏',
    devices: ['P3-05'],
    packages: [
      {
        direction: '退回',
        carrier: '极兔速递',
        waybill: 'JT****9017',
        status: '物流异常',
        tone: 'red',
        updated: '07-31 12:22',
        eta: null,
        lastSync: '07-31 15:31',
        nextAllowed: '07-31 16:01',
        timeline: [
          ['07-31 12:22', '物流异常', '收件地址需要补充确认'],
          ['07-31 08:36', '运输中', '包裹离开贵阳转运中心'],
          ['07-30 19:04', '已揽收', '极兔已揽收'],
        ],
      },
    ],
  },
};

function pageHead(key, action = '') {
  return `<header class="page-head">
    <div><p>LOGISTICS · CAMERA RENTAL</p><h1>${pick(words[key])}</h1><small>${pick(words[`${key}Description`])}</small></div>
    <div class="head-actions">${action}</div>
  </header>`;
}

function metric(labelZh, labelEn, value, detailZh, detailEn, tone = '') {
  return `<article class="metric" data-tone="${tone}"><span>${pick([labelZh, labelEn])}</span><strong>${value}</strong><small>${pick([detailZh, detailEn])}</small></article>`;
}

function renderSchedule() {
  const days = ['07-31', '08-01', '08-02', '08-03', '08-04', '08-05', '08-06', '08-07', '08-08', '08-09', '08-10'];
  const rows = [
    ['P4P-01', 'ANHXP5L0022JCW', 'XY-3313****4097', 0, 7, '2 个包裹 · 1 运输中 / 1 已签收', 'blue'],
    ['LENS-400-09', 'LNS400-09-2001TK', 'XY-3313****4097', 0, 7, '关联同一包裹 · 运输中', 'blue'],
    ['A6-09', '9KRXNAC00B-405D', 'XY-5125****4703', 0, 6, '顺丰 SF****3291 · 待揽收', 'amber'],
    ['P3-05', '5WTCN7F002B088', 'XY-5124****5839', 1, 8, '退回 · 极兔 JT****9017 · 异常', 'red'],
    ['P4-105', 'ANGZNB8002TP18', null, -1, -1, '', 'green'],
  ];
  const header = days.map((day, index) => `<th class="${index === 0 ? 'today' : ''}"><b class="data">${day}</b><small>${pick([['周五','周六','周日','周一','周二','周三','周四','周五','周六','周日','周一'][index], ['Fri','Sat','Sun','Mon','Tue','Wed','Thu','Fri','Sat','Sun','Mon'][index]])}</small></th>`).join('');
  const body = rows.map(([device, sn, order, start, end, logistics, tone]) => `
    <tr>
      <th><span class="device-name"><strong>${device}</strong>${badge(order ? pick(['占用', 'Occupied']) : pick(['可用', 'Available']), order ? 'blue' : 'green')}</span><span class="device-meta">${sn}</span></th>
      ${days.map((day, index) => {
        if (!order || index < start || index > end) return `<td class="day-cell ${index === 0 ? 'today' : ''}"><span class="free-cell">${pick(['空闲', 'Free'])}</span></td>`;
        return `<td class="day-cell ${index === 0 ? 'today' : ''}"><button class="rental-segment" data-open-tracking="${order}" data-risk="${tone === 'red' ? 'high' : tone === 'amber' ? 'medium' : ''}"><strong>${index === start ? order : '•'}</strong><small>${index === start ? logistics : pick(['占用中', 'Occupied'])}</small></button></td>`;
      }).join('')}
    </tr>`).join('');

  return `${pageHead('schedule', `<button class="secondary-button" data-action="local-sync">${pick(['刷新本地摘要', 'Refresh local summaries'])}</button>`)}
    <section class="metrics">
      ${metric('当前包裹', 'Packages', '7', '覆盖 5 个订单', 'Across 5 orders')}
      ${metric('运输中', 'In transit', '3', '最近更新 15:38', 'Last update 15:38')}
      ${metric('已签收', 'Delivered', '2', '仍需回仓检测', 'Return inspection still required', 'green')}
      ${metric('物流风险', 'Risks', '2', '1 高风险 / 1 关注', '1 high / 1 attention', 'red')}
      ${metric('待映射', 'Mapping required', '1', '不调用快递100', 'No provider call', 'amber')}
    </section>
    <div class="toolbar">
      <button class="filter-chip">${pick(['全部型号', 'All models'])}⌄</button>
      <button class="filter-chip">${pick(['全部物流状态', 'All tracking states'])}⌄</button>
      <button class="filter-chip">${pick(['未来 14 天', 'Next 14 days'])}⌄</button>
      <span class="toolbar-note">${pick(['只读取本地读模型 · 页面可见时每 60 秒刷新', 'Local read model only · refreshes every 60s while visible'])}</span>
    </div>
    <div class="schedule-layout">
      <section class="schedule-frame" data-specnav-component="ScheduleDeviceTable">
        <table class="schedule-table"><thead><tr><th>${pick(['设备实例', 'Physical device'])}</th>${header}</tr></thead><tbody>${body}</tbody></table>
      </section>
      <aside class="side-stack">
        <section class="panel" data-specnav-component="DeliveryTrackingSummary">
          <header class="panel-head"><strong>${pick(['当前窗口物流', 'Tracking in window'])}</strong><small>15:42</small></header>
          <div class="delivery-list">
            <button class="delivery-row" data-open-tracking="XY-3313****4097"><span class="row-top"><strong>XY-3313****4097</strong>${badge(pick(['2 个包裹', '2 packages']), 'purple')}</span><span class="row-copy">${pick(['1 个运输中 · 1 个已签收', '1 in transit · 1 delivered'])}</span><span class="row-meta">SF****4432 · JD****8048</span></button>
            <button class="delivery-row" data-open-tracking="XY-5125****4703"><span class="row-top"><strong>XY-5125****4703</strong>${badge(pick(['待揽收', 'Waiting pickup']), 'amber')}</span><span class="row-copy">${pick(['快递公司已收到电子信息', 'Carrier received electronic info'])}</span><span class="row-meta">SF****3291 · 14:12</span></button>
            <button class="delivery-row" data-open-tracking="XY-5124****5839"><span class="row-top"><strong>XY-5124****5839</strong>${badge(pick(['物流异常', 'Exception']), 'red')}</span><span class="row-copy">${pick(['退回包裹地址需要确认', 'Return address needs confirmation'])}</span><span class="row-meta">JT****9017 · 12:22</span></button>
          </div>
        </section>
        <section class="panel">
          <header class="panel-head"><strong>${pick(['追踪边界', 'Tracking boundary'])}</strong></header>
          <div class="card-body">
            <div class="definition-list">
              <div><dt>${pick(['页面轮询', 'UI polling'])}</dt><dd>${pick(['每 60 秒读本地摘要', 'Local summary every 60s'])}</dd></div>
              <div><dt>${pick(['供应商查询', 'Provider query'])}</dt><dd>${pick(['后端至少间隔 30 分钟', 'Backend minimum 30 min'])}</dd></div>
              <div><dt>${pick(['签收状态', 'Delivered'])}</dt><dd>${pick(['不自动释放设备', 'Never releases device'])}</dd></div>
            </div>
          </div>
        </section>
      </aside>
    </div>`;
}

function renderShipment() {
  return `${pageHead('shipment', `<button class="secondary-button" data-action="prototype">${pick(['返回发货工作台', 'Back to shipping workbench'])}</button>`)}
    <section class="result-hero" data-specnav-component="DeliveryTrackingCreationResult">
      <span class="result-icon">✓</span>
      <div><h2>${pick(['闲鱼发货成功，物流追踪已创建', 'Xianyu shipped and tracking created'])}</h2><p>${pick(['设备出库已经提交；快递100订阅与首次查询将在事务提交后异步执行。', 'Device dispatch is committed. Subscription and initial query run asynchronously after commit.'])}</p></div>
      <code class="result-code">DLV-20260731-0182</code>
    </section>
    <section class="flow-steps">
      ${[
        ['01', '闲鱼发货', 'Xianyu ship'],
        ['02', '设备出库', 'Device dispatch'],
        ['03', '创建 Delivery', 'Create Delivery'],
        ['04', '写入 Outbox', 'Write Outbox'],
        ['05', '异步订阅', 'Async subscribe'],
        ['06', '首次查询', 'Initial query'],
      ].map(([n, zh, en]) => `<article class="flow-step"><small>${n}</small><strong>${pick([zh, en])}</strong></article>`).join('')}
    </section>
    <section class="section-grid" style="margin-top:12px">
      <article class="card span-7"><header class="card-header"><div><h2>${pick(['本次包裹', 'Created package'])}</h2><p>${pick(['真实包裹与发货审计分离', 'Physical package remains separate from shipment audit'])}</p></div>${badge(pick(['追踪待启动', 'Tracking pending']), 'blue')}</header><div class="card-body"><dl class="definition-list"><div><dt>${pick(['订单', 'Order'])}</dt><dd class="data">XY-3313****4097</dd></div><div><dt>${pick(['方向', 'Direction'])}</dt><dd>${pick(['寄出 · 第 1 包', 'Outbound · package 1'])}</dd></div><div><dt>${pick(['设备', 'Devices'])}</dt><dd>P4P-01 · LENS-400-09</dd></div><div><dt>${pick(['承运商 / 运单', 'Carrier / waybill'])}</dt><dd>${pick(['顺丰速运', 'SF Express'])} · <span class="data">SF****4432</span></dd></div><div><dt>Outbox</dt><dd>SUBSCRIBE · INITIAL_QUERY</dd></div></dl></div></article>
      <article class="card span-5"><header class="card-header"><div><h2>${pick(['降级不是发货失败', 'Degradation is not shipment failure'])}</h2></div></header><div class="card-body"><div class="risk-list"><div class="risk-row"><span class="row-top"><strong>${pick(['承运商未映射', 'Carrier unmapped'])}</strong>${badge('MAPPING_REQUIRED', 'amber')}</span><span class="row-copy">${pick(['保留本地包裹，不调用快递100', 'Keep local package; no provider call'])}</span></div><div class="risk-row"><span class="row-top"><strong>${pick(['Provider 未启用', 'Provider disabled'])}</strong>${badge('DISABLED', 'neutral')}</span><span class="row-copy">${pick(['闲鱼发货和设备出库仍然成功', 'Xianyu ship and dispatch still succeed'])}</span></div><div class="risk-row"><span class="row-top"><strong>${pick(['订阅临时失败', 'Subscribe retryable'])}</strong>${badge('FAILED_RETRYABLE', 'red')}</span><span class="row-copy">${pick(['后台重试，不回滚主业务', 'Background retry; no business rollback'])}</span></div></div></div></article>
    </section>`;
}

function renderOperations() {
  return `${pageHead('operations', `<button class="primary-button" data-action="prototype">${pick(['新建承运商映射', 'New carrier mapping'])}</button>`)}
    <section class="section-grid">
      <article class="card span-5" data-specnav-component="ProviderConfigPanel">
        <header class="card-header"><div><h2>${pick(['快递100租户配置', 'Kuaidi100 tenant config'])}</h2><p>${pick(['凭据只支持替换，不返回明文', 'Secrets can be replaced but never returned'])}</p></div>${badge(pick(['查询开启 · 订阅开启', 'Query on · subscribe on']), 'green')}</header>
        <div class="card-body">
          <dl class="definition-list"><div><dt>${pick(['配置状态', 'Config status'])}</dt><dd>READY</dd></div><div><dt>${pick(['查询最小间隔', 'Minimum query interval'])}</dt><dd>1800s</dd></div><div><dt>${pick(['结果版本', 'Result version'])}</dt><dd>4</dd></div><div><dt>${pick(['回调地址', 'Callback base'])}</dt><dd class="data">https://api.••••/rental/webhooks/...</dd></div></dl>
          <div class="secret-field"><code>API KEY · sk_••••••••••4T8P</code><button class="secondary-button" data-action="prototype">${pick(['替换', 'Replace'])}</button></div>
        </div>
      </article>
      <article class="card span-7" data-specnav-component="CarrierMappingPanel">
        <header class="card-header"><div><h2>${pick(['承运商编码映射', 'Carrier mappings'])}</h2><p>${pick(['闲鱼编码 → 平台编码 → 快递100编码', 'Xianyu → canonical → Kuaidi100'])}</p></div>${badge(pick(['1 条待配置', '1 requires mapping']), 'amber')}</header>
        <div class="table-scroll"><table class="mapping-table"><thead><tr><th>${pick(['来源', 'Source'])}</th><th>${pick(['来源编码', 'Source code'])}</th><th>${pick(['平台编码', 'Canonical'])}</th><th>${pick(['快递100', 'Kuaidi100'])}</th><th>${pick(['手机号', 'Phone'])}</th><th>${pick(['状态', 'State'])}</th></tr></thead><tbody><tr><td>XIANYU</td><td>SF</td><td>SF_EXPRESS</td><td>shunfeng</td><td>REQUIRED</td><td>${badge('MAPPED', 'green')}</td></tr><tr><td>XIANYU</td><td>JD</td><td>JD_LOGISTICS</td><td>jd</td><td>NONE</td><td>${badge('MAPPED', 'green')}</td></tr><tr><td>XIANYU</td><td>CITY_RUNNER</td><td>LOCAL_RUNNER</td><td>—</td><td>NONE</td><td>${badge('MAPPING_REQUIRED', 'amber')}</td></tr></tbody></table></div>
      </article>
      <article class="card span-12" data-specnav-component="LogisticsTaskQueuePanel">
        <header class="card-header"><div><h2>${pick(['异步任务与回调队列', 'Async task and callback queue'])}</h2><p>${pick(['短事务领取，网络调用在事务外执行', 'Short leases; provider network calls run outside transactions'])}</p></div><button class="secondary-button" data-action="prototype">${pick(['运行 Reconcile', 'Run reconcile'])}</button></header>
        <div class="table-scroll"><table class="task-table"><thead><tr><th>ID</th><th>${pick(['类型', 'Type'])}</th><th>Delivery</th><th>${pick(['状态', 'Status'])}</th><th>${pick(['尝试', 'Attempts'])}</th><th>${pick(['下次执行', 'Next run'])}</th><th>${pick(['操作', 'Action'])}</th></tr></thead><tbody><tr><td class="data">OTB-0182</td><td>SUBSCRIBE</td><td class="data">DLV-0182</td><td>${badge('PENDING', 'blue')}</td><td>0</td><td>15:43</td><td><button class="secondary-button" data-action="prototype">${pick(['查看', 'View'])}</button></td></tr><tr><td class="data">OTB-0174</td><td>REFRESH_QUERY</td><td class="data">DLV-0174</td><td>${badge('THROTTLED', 'amber')}</td><td>1</td><td>16:08</td><td><button class="secondary-button" data-action="prototype">${pick(['查看', 'View'])}</button></td></tr><tr><td class="data">INB-0911</td><td>CALLBACK</td><td class="data">DLV-0168</td><td>${badge('FAILED_RETRYABLE', 'red')}</td><td>2</td><td>15:48</td><td><button class="secondary-button" data-action="prototype">${pick(['安全重试', 'Safe retry'])}</button></td></tr></tbody></table></div>
      </article>
    </section>`;
}

function renderRisks() {
  return `${pageHead('risks', `<button class="secondary-button" data-action="prototype">${pick(['重新计算风险', 'Recalculate risks'])}</button>`)}
    <section class="metrics">
      ${metric('高风险', 'High risk', '1', '可能影响下一单', 'May affect next order', 'red')}
      ${metric('需要关注', 'Attention', '3', '待揽收 / stale', 'Pickup / stale', 'amber')}
      ${metric('已恢复', 'Recovered', '1', '轨迹已重新更新', 'Tracking resumed', 'green')}
      ${metric('映射问题', 'Mapping issue', '1', 'Provider 未调用', 'Provider not called', 'amber')}
      ${metric('设备自动释放', 'Auto release', '0', '必须回仓检测', 'Return inspection required', 'green')}
    </section>
    <section class="card" data-specnav-component="LogisticsRiskDetail">
      <header class="card-header"><div><h2>${pick(['物流风险列表', 'Logistics risk list'])}</h2><p>${pick(['后端结合租期、占用区间和下一单排期计算', 'Server-derived from rental dates, occupied range, and next booking'])}</p></div></header>
      <div class="table-scroll"><table class="risk-table"><thead><tr><th>${pick(['严重度', 'Severity'])}</th><th>${pick(['风险码', 'Risk code'])}</th><th>${pick(['订单 / 设备', 'Order / device'])}</th><th>${pick(['安全说明', 'Safe message'])}</th><th>${pick(['下一步', 'Next action'])}</th></tr></thead><tbody><tr><td>${badge(pick(['高风险', 'High']), 'red')}</td><td class="data">RETURN_DELIVERY_DELAY</td><td>XY-5124****5839 · P3-05</td><td>${pick(['退回包裹出现地址异常，可能影响 08-10 下一单', 'Return address exception may affect the Aug 10 booking'])}</td><td><button class="secondary-button" data-open-tracking="XY-5124****5839">${pick(['查看轨迹', 'View trace'])}</button></td></tr><tr><td>${badge(pick(['关注', 'Attention']), 'amber')}</td><td class="data">OUTBOUND_NOT_PICKED_UP</td><td>XY-5125****4703 · A6-09</td><td>${pick(['发货后仍只有电子信息，尚未揽收', 'Shipment still has electronic info only'])}</td><td><button class="secondary-button" data-open-tracking="XY-5125****4703">${pick(['查看轨迹', 'View trace'])}</button></td></tr><tr><td>${badge(pick(['关注', 'Attention']), 'amber')}</td><td class="data">MAPPING_REQUIRED</td><td>XY-3314****3272</td><td>${pick(['同城跑腿没有快递100编码映射', 'Local runner has no Kuaidi100 mapping'])}</td><td><button class="secondary-button" data-screen-target="operations">${pick(['配置映射', 'Configure mapping'])}</button></td></tr><tr><td>${badge(pick(['已恢复', 'Recovered']), 'green')}</td><td class="data">TRACKING_STALE</td><td>XY-3313****4097 · P4P-01</td><td>${pick(['15:38 收到新轨迹，stale 风险已解除', 'New trace at 15:38 resolved stale risk'])}</td><td><button class="secondary-button" data-open-tracking="XY-3313****4097">${pick(['查看', 'View'])}</button></td></tr></tbody></table></div>
    </section>
    <section class="card" style="margin-top:12px"><div class="card-body"><div class="result-hero"><span class="result-icon">✓</span><div><h2>${pick(['物流签收不等于设备可用', 'Delivered does not mean available'])}</h2><p>${pick(['包裹签收后，设备仍保持占用或待检测，直到员工完成回仓、验收和检测。', 'After delivery, the device stays occupied or awaiting inspection until staff completes return and inspection.'])}</p></div><code class="result-code">DELIVERED ≠ AVAILABLE</code></div></div></section>`;
}

const renderers = { schedule: renderSchedule, shipment: renderShipment, operations: renderOperations, risks: renderRisks };
const screenAnchors = { schedule: 'schedule-tracking', shipment: 'shipment-result', operations: 'logistics-operations', risks: 'logistics-risks' };
const app = document.querySelector('.app-shell');
const root = document.querySelector('#screen-root');
const drawer = document.querySelector('[data-tracking-drawer]');
const backdrop = document.querySelector('[data-drawer-backdrop]');
const toast = document.querySelector('.toast');
let toastTimer;

function translateShell() {
  document.documentElement.lang = state.locale;
  document.querySelectorAll('[data-copy]').forEach((node) => {
    const value = copy[state.locale][node.dataset.copy];
    if (value) node.textContent = value;
  });
  document.querySelector('[data-locale-toggle]').textContent = state.locale === 'en' ? '中' : 'EN';
}

function render() {
  root.innerHTML = renderers[state.screen]();
  root.dataset.specnavScreen = screenAnchors[state.screen];
  document.querySelectorAll('[data-screen-target]').forEach((button) => button.classList.toggle('is-active', button.dataset.screenTarget === state.screen));
}

function showToast(message) {
  clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add('is-visible');
  toastTimer = setTimeout(() => toast.classList.remove('is-visible'), 2300);
}

function openTracking(orderId) {
  const delivery = deliveries[orderId];
  if (!delivery) return;
  state.drawerOrder = orderId;
  state.activePackage = Math.min(state.activePackage, delivery.packages.length - 1);
  const pkg = delivery.packages[state.activePackage];
  drawer.innerHTML = `
    <header class="drawer-header"><div><p>DELIVERY TRACKING · LOCAL READ MODEL</p><h2>${orderId}</h2><small>${delivery.customer} · ${delivery.devices.join(' · ')}</small></div><button class="drawer-close" data-close-drawer aria-label="Close">×</button></header>
    <section class="drawer-section"><h3>${pick(['包裹', 'Packages'])}</h3><div class="package-tabs">${delivery.packages.map((item, index) => `<button class="package-tab ${index === state.activePackage ? 'is-active' : ''}" data-package-index="${index}"><strong>${item.direction} · ${item.carrier}</strong><small>${item.waybill} · ${item.status}</small></button>`).join('')}</div></section>
    <section class="drawer-section"><div class="tracking-summary"><div class="summary-item"><span>${pick(['平台状态', 'Platform status'])}</span><strong>${badge(pkg.status, pkg.tone)}</strong></div><div class="summary-item"><span>${pick(['最近轨迹', 'Latest event'])}</span><strong>${pkg.updated}</strong></div><div class="summary-item"><span>${pick(['最近同步', 'Last provider sync'])}</span><strong>${pkg.lastSync}</strong></div><div class="summary-item"><span>${pick(['预计到达', 'Estimated arrival'])}</span><strong>${pkg.eta || '—'}</strong></div></div></section>
    <section class="drawer-section" data-specnav-component="DeliveryTrackingTimeline"><h3>${pick(['完整物流轨迹', 'Full tracking timeline'])}</h3><ol class="timeline">${pkg.timeline.map(([time, status, context]) => `<li><time>${time}</time><div><strong>${status}</strong><p>${context}</p></div></li>`).join('')}</ol></section>
    <section class="drawer-section"><div class="definition-list"><div><dt>${pick(['刷新策略', 'Refresh policy'])}</dt><dd>${pick(['只提交异步查询任务', 'Enqueues async query only'])}</dd></div><div><dt>${pick(['下一次允许', 'Next allowed'])}</dt><dd>${pkg.nextAllowed}</dd></div><div><dt>${pick(['设备状态', 'Device state'])}</dt><dd>${pick(['签收后仍需回仓检测', 'Return inspection still required'])}</dd></div></div></section>
    <footer class="drawer-actions"><button class="secondary-button" data-close-drawer>${pick(['关闭', 'Close'])}</button><button class="primary-button" data-action="refresh-provider">${pick(['刷新物流', 'Refresh tracking'])}</button></footer>`;
  drawer.classList.add('is-open');
  backdrop.classList.add('is-open');
  drawer.setAttribute('aria-hidden', 'false');
}

function closeDrawer() {
  drawer.classList.remove('is-open');
  backdrop.classList.remove('is-open');
  drawer.setAttribute('aria-hidden', 'true');
  state.drawerOrder = null;
  state.activePackage = 0;
}

document.addEventListener('click', (event) => {
  const screenButton = event.target.closest('[data-screen-target]');
  if (screenButton) {
    state.screen = screenButton.dataset.screenTarget;
    render();
    window.scrollTo({ top: 0, behavior: 'smooth' });
    return;
  }
  if (event.target.closest('[data-theme-toggle]')) {
    state.theme = state.theme === 'light' ? 'dark' : 'light';
    document.documentElement.dataset.theme = state.theme;
    showToast(pick([`已切换为${state.theme === 'dark' ? '深色' : '浅色'}主题`, `${state.theme === 'dark' ? 'Dark' : 'Light'} theme enabled`]));
    return;
  }
  if (event.target.closest('[data-locale-toggle]')) {
    const shouldRerenderDrawer = drawer.classList.contains('is-open') && state.drawerOrder;
    state.locale = state.locale === 'zh-CN' ? 'en' : 'zh-CN';
    translateShell();
    render();
    if (shouldRerenderDrawer) openTracking(state.drawerOrder);
    return;
  }
  const reviewButton = event.target.closest('[data-review-state]');
  if (reviewButton) {
    state.review = reviewButton.dataset.reviewState;
    app.dataset.specnavState = state.review;
    document.querySelectorAll('[data-review-state]').forEach((button) => button.classList.toggle('is-active', button === reviewButton));
    return;
  }
  if (event.target.closest('[data-review-toggle]')) {
    document.querySelector('.review-panel').classList.toggle('is-collapsed');
    return;
  }
  const trackingButton = event.target.closest('[data-open-tracking]');
  if (trackingButton) {
    openTracking(trackingButton.dataset.openTracking);
    return;
  }
  const packageButton = event.target.closest('[data-package-index]');
  if (packageButton) {
    state.activePackage = Number(packageButton.dataset.packageIndex);
    openTracking(state.drawerOrder);
    return;
  }
  if (event.target.closest('[data-close-drawer]') || event.target === backdrop) {
    closeDrawer();
    return;
  }
  const action = event.target.closest('[data-action]');
  if (action) {
    if (action.dataset.action === 'refresh-provider') {
      showToast(pick(['刷新任务已排队；不会同步等待快递100', 'Refresh queued; the UI does not wait for Kuaidi100']));
    } else if (action.dataset.action === 'local-sync') {
      showToast(pick(['已刷新本地物流摘要', 'Local tracking summaries refreshed']));
    } else {
      showToast(pick(['原型操作已记录，不会提交真实业务数据', 'Prototype intent recorded; no real data submitted']));
    }
  }
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') closeDrawer();
});

translateShell();
render();
