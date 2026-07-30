import { devices, productionSnapshot } from '../fixtures.js';
import { domainText, pick } from '../state.js';
import { badge } from '../ui.js';

const flowSteps = [
  ['1', '运单', 'Waybill', 'draft'],
  ['2', '设备', 'Device', 'ready'],
  ['3', '订单准入', 'Order gate', 'blocked'],
  ['4', '服务端确认', 'Server confirm', 'pending'],
];

const readinessRows = [
  ['运单信息', 'Waybill', '未录入', 'Missing', 'pending'],
  ['独立设备', 'Physical device', '已选择 P4-105', 'P4-105 selected', 'ready'],
  ['租赁订单', 'Rental order', '无可确认候选', 'No eligible candidate', 'blocked'],
  ['租期与权限', 'Period and permission', '等待服务端返回', 'Awaiting server', 'pending'],
];

export function renderShipping() {
  const availableDevices = devices.filter((item) => item[4] === '可用');
  const deviceChoices = availableDevices.map(([unit, sn, model, location, status], index) => `
    <button
      class="choice-row dispatch-device-card ${index === 0 ? 'is-selected' : ''}"
      data-prototype-action="select-device"
      data-device-search-text="${[unit, sn, model, location].join(' ').toLowerCase()}"
      data-device-unit="${unit}"
      data-device-sn="${sn}"
      data-device-model="${model}"
    >
      <span class="dispatch-device-radio">${index + 1}</span>
      <span class="dispatch-device-copy">
        <small>${model} · ${domainText(location)}</small>
        <strong>${unit}</strong>
        <code>SN ${sn}</code>
      </span>
      <span class="dispatch-device-state">${badge(status)}<small>${pick('可立即分配', 'Ready now')}</small></span>
    </button>`).join('');
  return `
    <section class="screen screen-shipping dispatch-screen" data-specnav-screen="shipping-workbench">
      <header class="dispatch-hero">
        <div class="dispatch-heading">
          <div class="dispatch-kicker"><span></span> OUTBOUND CONTROL · ${pick('服务端权威', 'SERVER AUTHORITATIVE')}</div>
          <h1>${pick('发货作业台', 'Outbound operations')}</h1>
          <p>${pick('从运单识别到具体设备出库，一次完成核对；任何一步未通过，都不会调用闲管家发货接口。', 'Review the waybill, physical device, and eligible order in one flow. No XianGuanJia write occurs until every gate passes.')}</p>
        </div>
        <div class="dispatch-hero-actions">
          <div class="dispatch-snapshot"><span>●</span><div><strong>${pick('脱敏生产快照', 'Masked production snapshot')}</strong><small>2026-07-29 17:12 · tenant 1</small></div></div>
          <button class="secondary-button" data-prototype-action="history">${pick('查看发货履历', 'Shipment history')} <b>2</b></button>
        </div>
      </header>

      <section class="dispatch-overview" aria-label="${pick('发货状态总览', 'Outbound status overview')}">
        <article data-tone="blue"><span>${pick('渠道待发货', 'Channel pending')}</span><strong>151</strong><small>${pick('闲鱼状态码 12', 'Xianyu status 12')}</small></article>
        <article data-tone="green"><span>${pick('已建档可用设备', 'Registered available')}</span><strong>2</strong><small>P3 / P4</small></article>
        <article data-tone="amber"><span>${pick('已转换内部订单', 'Converted internal')}</span><strong>${productionSnapshot.internalOrders}</strong><small>${pick('均已出库，状态待校准', 'Both dispatched; status mismatch')}</small></article>
        <article data-tone="red"><span>${pick('当前可确认', 'Confirmable now')}</span><strong>0</strong><small>${pick('准入条件未满足', 'Eligibility gates failed')}</small></article>
      </section>

      <nav class="dispatch-flow" data-specnav-component="ShippingWorkflowStepper">
        ${flowSteps.map(([number, zh, en, state]) => `
          <div data-state="${state}"><b>${number}</b><span><strong>${pick(zh, en)}</strong><small>${state === 'ready' ? pick('已选择', 'Selected') : state === 'blocked' ? pick('当前阻塞', 'Blocked') : state === 'draft' ? pick('等待录入', 'Awaiting input') : pick('尚未开始', 'Not started')}</small></span></div>`).join('')}
      </nav>

      <div class="dispatch-layout">
        <div class="dispatch-primary">
          <section class="dispatch-panel dispatch-waybill" data-specnav-component="WaybillReviewPanel">
            <header><div><span>01</span><div><p>WAYBILL DESK</p><h2>${pick('录入并复核运单', 'Capture and review waybill')}</h2></div></div><em>${pick('OCR 只生成草稿', 'OCR draft only')}</em></header>
            <div class="dispatch-waybill-body">
              <button class="dispatch-dropzone" data-prototype-action="ocr">
                <span class="dispatch-scan-glyph"><i></i><i></i><b>＋</b></span>
                <strong>${pick('拖入或拍摄物流面单', 'Drop or capture a waybill')}</strong>
                <small>${pick('支持顺丰 / 京东 · JPG、PNG · 识别后仍需人工确认', 'SF / JD · JPG or PNG · operator review remains required')}</small>
              </button>
              <div class="dispatch-waybill-form">
                <label><span>${pick('快递承运商', 'Carrier')}</span><div class="dispatch-carriers"><button class="is-active" type="button">${pick('顺丰', 'SF')}</button><button type="button">${pick('京东', 'JD')}</button><button type="button">${pick('其他', 'Other')}</button></div></label>
                <label><span>${pick('运单跟踪号', 'Tracking number')}</span><div class="dispatch-input"><i>⌁</i><input value="" placeholder="${pick('手工输入或等待 OCR 草稿', 'Enter manually or wait for OCR draft')}" /><em>${pick('未识别', 'Empty')}</em></div></label>
                <p><span>i</span>${pick('选择订单时保留已录入的运单号，不使用订单字段覆盖当前草稿。', 'Selecting an order preserves the entered waybill and never overwrites the draft.')}</p>
              </div>
            </div>
          </section>

          <section class="dispatch-panel dispatch-devices" data-specnav-component="DeviceSelectionPanel">
            <header><div><span>02</span><div><p>PHYSICAL DEVICE</p><h2>${pick('选择具体设备实例', 'Select a physical device')}</h2></div></div><em>${pick('2 台可用', '2 available')}</em></header>
            <div class="dispatch-device-toolbar">
              <label class="dispatch-search-field">
                <span>⌕</span>
                <input
                  type="search"
                  data-device-search
                  autocomplete="off"
                  placeholder="${pick('搜索设备编号、SN、型号或库位', 'Search device ID, SN, model, or location')}"
                  aria-label="${pick('搜索可用设备', 'Search available devices')}"
                />
                <kbd>2</kbd>
              </label>
              <span>${pick('仅显示 AVAILABLE 且已启用', 'AVAILABLE and enabled only')}</span>
            </div>
            <div class="choice-list dispatch-device-list">${deviceChoices}</div>
            <div class="dispatch-search-empty" data-device-search-empty hidden>
              <span>Ø</span>
              <div><strong>${pick('没有匹配的可用设备', 'No matching available device')}</strong><small>${pick('可尝试设备编号、完整 SN、型号代码或库位。', 'Try a device ID, full SN, model code, or location.')}</small></div>
            </div>
            <footer><span>△</span><p><strong>${pick('资产边界', 'Asset boundary')}</strong>${pick('实际几百台设备未完成入库前，不会出现在这里，也不会参与可用性计算。', 'The larger physical fleet remains excluded until device registration is complete.')}</p><button data-screen-target="devices">${pick('查看入库字段', 'Import fields')} →</button></footer>
          </section>
        </div>

        <section class="dispatch-panel dispatch-orders" data-specnav-component="PendingOrderSelectionPanel">
          <header><div><span>03</span><div><p>ORDER ELIGIBILITY</p><h2>${pick('待发货订单准入', 'Pending-order eligibility')}</h2></div></div><em data-tone="blocked">${pick('0 可确认', '0 eligible')}</em></header>
          <div class="dispatch-order-search">
            <label class="dispatch-search-field">
              <span>⌕</span>
              <input
                type="search"
                data-order-search
                autocomplete="off"
                placeholder="${pick('输入收货人姓名、完整手机号或订单号', 'Enter receiver name, full phone, or order number')}"
                aria-label="${pick('搜索待发货订单', 'Search pending shipment orders')}"
              />
              <kbd>⌘ K</kbd>
            </label>
            <div class="dispatch-order-search-meta">
              <span>${pick('精确匹配手机号 / 模糊匹配姓名 / 完整或尾号匹配订单号', 'Exact phone / fuzzy name / full or suffix order match')}</span>
              <strong>${pick('授权员工：结果不脱敏', 'Authorized staff: unmasked results')}</strong>
            </div>
          </div>
          <section class="dispatch-order-results" data-order-results aria-live="polite">
            <header>
              <div><strong>${pick('订单搜索结果', 'Order search results')}</strong><small data-order-search-status>${pick('等待输入检索条件', 'Waiting for a search query')}</small></div>
              <span>${pick('服务端权限过滤', 'Server permission filter')}</span>
            </header>
            <div class="dispatch-order-fields">
              <span>${pick('订单号', 'Order no.')}</span><span>${pick('收货人', 'Receiver')}</span><span>${pick('手机号', 'Phone')}</span>
              <span>${pick('收货地址', 'Address')}</span><span>${pick('商品与数量', 'Items')}</span><span>${pick('订单金额', 'Amount')}</span>
              <span>${pick('卖家备注', 'Seller remark')}</span><span>${pick('解析租期', 'Rental period')}</span><span>${pick('渠道与店铺', 'Channel and shop')}</span>
              <span>${pick('渠道状态', 'Channel status')}</span><span>${pick('转换状态', 'Conversion')}</span><span>${pick('发货准入原因', 'Eligibility reason')}</span>
            </div>
            <div class="dispatch-order-result-empty">
              <span>⌕</span>
              <div>
                <strong>${pick('搜索后在这里核对完整订单信息', 'Review complete order data here after searching')}</strong>
                <p>${pick('原型不复制真实姓名、电话和地址；生产实现中，仅拥有发货权限的员工可查看未脱敏结果，查询和查看行为需记录审计日志。', 'The prototype does not copy real names, phones, or addresses. In production, only staff with shipment permission can view unmasked results, with search and view access audited.')}</p>
              </div>
            </div>
          </section>
          <div class="dispatch-funnel">
            <header><strong>${pick('候选准入漏斗', 'Eligibility funnel')}</strong><small>${pick('每一步都以后端结果为准', 'Every gate is server-authoritative')}</small></header>
            <div class="dispatch-funnel-track">
              <span style="--size:100%"><b>868</b><small>${pick('渠道订单', 'Channel orders')}</small></span>
              <span style="--size:72%"><b>151</b><small>${pick('待发货', 'Pending shipment')}</small></span>
              <span style="--size:43%"><b>2</b><small>${pick('已转换', 'Converted')}</small></span>
              <span data-tone="blocked" style="--size:22%"><b>0</b><small>${pick('可确认', 'Eligible')}</small></span>
            </div>
          </div>
          <div class="dispatch-gates">
            <div data-state="ready"><span>✓</span><p><strong>${pick('渠道状态校验', 'Channel status')}</strong><small>${pick('151 单处于待发货状态', '151 orders are pending shipment')}</small></p><em>${pick('通过', 'Passed')}</em></div>
            <div data-state="warning"><span>!</span><p><strong>${pick('内部订单转换', 'Internal conversion')}</strong><small>${pick('仅 2 单已转换，且均已出库', 'Only 2 converted; both already dispatched')}</small></p><em>${pick('需校准', 'Reconcile')}</em></div>
            <div data-state="blocked"><span>×</span><p><strong>${pick('商品映射与租期', 'Product mapping and rental period')}</strong><small>${pick('大量订单仍处于人工复核', 'Many orders still require manual review')}</small></p><em>${pick('阻塞', 'Blocked')}</em></div>
          </div>
          <div class="dispatch-empty-order">
            <span>0</span><div><strong>${pick('当前没有可安全发货的订单', 'No order is currently safe to ship')}</strong><p>${pick('不是搜索失效：订单必须完成转换、商品映射、租期解析，且不能已经出库。', 'Search is working. An order must be converted, mapped, period-ready, and not already dispatched.')}</p></div>
            <button data-screen-target="exceptions">${pick('处理人工复核', 'Resolve reviews')} →</button>
          </div>
        </section>

        <aside class="dispatch-review">
          <section class="dispatch-docket" data-specnav-component="ShipmentConfirmationPanel">
            <header><div><p>DISPATCH DOCKET</p><h2>${pick('发货清单', 'Shipment docket')}</h2></div><span>${pick('未就绪', 'Not ready')}</span></header>
            <div class="dispatch-selected-device"><small>${pick('已选择设备', 'SELECTED DEVICE')}</small><strong data-selected-device-unit>P4-105-ANGZNB8002TP18</strong><code data-selected-device-sn>ANGZNB8002TP18</code><span data-selected-device-model>P4 · AVAILABLE</span></div>
            <div class="dispatch-readiness">
              ${readinessRows.map(([zh, en, valueZh, valueEn, state]) => `
                <div data-state="${state}"><i>${state === 'ready' ? '✓' : state === 'blocked' ? '×' : '·'}</i><span>${pick(zh, en)}</span><strong>${pick(valueZh, valueEn)}</strong></div>`).join('')}
            </div>
            <button class="dispatch-submit" disabled><span>${pick('条件未满足，不能发货', 'Shipment requirements not met')}</span><b>→</b></button>
            <p class="dispatch-server-note">▣ ${pick('满足条件后仍需服务端二次校验，原型不会调用真实接口。', 'The server validates again after all gates pass. This prototype calls no real API.')}</p>
          </section>
          <section class="dispatch-history">
            <header><div><p>RECENT ACTIVITY</p><h3>${pick('今日已出库', 'Dispatched today')}</h3></div><b>2</b></header>
            <article><span>03:00</span><div><strong>P4P-01 · P4P</strong><small>3314****9073 · ${pick('顺丰', 'SF')}</small></div><em>${pick('已出库', 'Dispatched')}</em></article>
            <article><span>07:31</span><div><strong>A6-09 · A6</strong><small>3314****7893 · ${pick('顺丰', 'SF')}</small></div><em>${pick('已出库', 'Dispatched')}</em></article>
          </section>
        </aside>
      </div>
    </section>`;
}
