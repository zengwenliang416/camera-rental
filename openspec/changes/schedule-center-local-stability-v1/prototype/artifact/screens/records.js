import { deviceImportFields, devices, exceptions, orders, productionSnapshot } from '../fixtures.js';
import { domainText, pick } from '../state.js';
import { badge, filterBar, pageTitle, snapshotNote } from '../ui.js';

const metricCards = (items) => items.map(([label, value, note, tone = '']) => `
  <article class="data-metric" data-tone="${tone}"><span>${pick(label[0], label[1])}</span><strong>${value}</strong><small>${pick(note[0], note[1])}</small></article>`).join('');

export function renderOrders() {
  const rows = orders.map(([id, channel, customer, product, billable, occupied, status]) => `
    <button class="record-row order-row" data-open-detail="${id}">
      <span><strong class="data-code">${id}</strong><small>${pick(channel, 'Xianyu')}</small></span>
      <span><strong>${domainText(customer)}</strong><small>${product}</small></span>
      <span><small>${pick('计租周期', 'Billable')}</small><strong>${domainText(billable)}</strong></span>
      <span><small>${pick('占用周期', 'Occupied')}</small><strong>${domainText(occupied)}</strong></span>
      ${badge(status)}<em>→</em>
    </button>`).join('');
  const statusRows = productionSnapshot.channelStatus.map(([code, label, count]) => `
    <div class="distribution-row"><code>${code}</code><span>${pick(label, {
      11: 'Awaiting payment', 12: 'Awaiting shipment', 21: 'Shipped',
      22: 'Completed', 23: 'Refunded', 24: 'Closed',
    }[code])}</span><b>${count}</b><i style="--ratio:${Math.round(count / productionSnapshot.channelOrders * 100)}%"></i></div>`).join('');
  return `
    <section class="screen screen-records" data-specnav-screen="rental-orders">
      ${snapshotNote()}
      ${pageTitle('CHANNEL 868 · INTERNAL 2', '渠道订单与内部租赁单', 'Channel and internal rental orders')}
      <div class="data-metric-grid">${metricCards([
        [['渠道订单', 'Channel orders'], 868, ['闲鱼同步记录', 'Xianyu sync records'], 'blue'],
        [['内部租赁单', 'Internal orders'], 2, ['已转换 2 单', '2 converted orders'], 'green'],
        [['需人工复核', 'Review required'], 718, ['转换状态', 'Conversion status'], 'amber'],
        [['待转换', 'Pending conversion'], 148, ['转换状态', 'Conversion status']],
      ])}</div>
      <div class="record-insight-grid">
        <section class="distribution-panel">
          <header><div><p>CHANNEL STATUS</p><h2>${pick('渠道订单状态分布', 'Channel order status')}</h2></div><small>${pick('管理端权威映射', 'Admin-authoritative mapping')}</small></header>
          ${statusRows}
        </section>
        <section class="distribution-panel">
          <header><div><p>DATA COMPLETENESS</p><h2>${pick('履约字段完整度', 'Fulfillment completeness')}</h2></div><small>${pick('仅显示数量，不展示隐私字段', 'Counts only; no private values')}</small></header>
          ${productionSnapshot.receiverCompleteness.map(([label, count]) => `
            <div class="distribution-row"><span>${pick(label, {
              运单号: 'Waybill', 发货时间: 'Consign time', 收货人姓名: 'Receiver name', 收货电话: 'Receiver mobile',
            }[label])}</span><b>${count} / 868</b><i style="--ratio:${Math.round(count / 868 * 100)}%"></i></div>`).join('')}
        </section>
      </div>
      ${filterBar({
        search: ['搜索内部订单号或型号', 'Search internal order or model'],
        filters: [['内部租赁单', 'Internal orders'], ['状态待校准', 'Status needs reconciliation']],
        summary: ['rental_order 当前仅 2 条；顶层占用字段为空，周期来自订单明细', 'Only 2 rental_order rows; occupied ranges come from order items'],
      })}
      <section class="records-panel">
        <header class="record-columns order-columns"><span>${pick('订单', 'Order')}</span><span>${pick('客户 / 型号', 'Customer / model')}</span><span>${pick('计租周期', 'Billable')}</span><span>${pick('设备占用周期', 'Occupied')}</span><span>${pick('状态', 'Status')}</span><span></span></header>
        <div>${rows}</div>
        <footer class="pagination"><button disabled>←</button><span>1 / 1</span><button disabled>→</button></footer>
      </section>
    </section>`;
}

export function renderDevices() {
  const cards = devices.map(([unit, sn, model, location, status, enabled, next]) => `
    <article class="device-card">
      <header><span class="device-code">${unit}</span>${badge(status)}</header>
      <div class="device-illustration"><span></span><b>${model}</b></div>
      <h3>${model}</h3><code>${domainText(sn)}</code>
      <dl>
        <div><dt>${pick('库位', 'Location')}</dt><dd>${domainText(location)}</dd></div>
        <div><dt>${pick('启用状态', 'Enabled')}</dt><dd>${domainText(enabled)}</dd></div>
        <div><dt>${pick('下次可用', 'Next available')}</dt><dd>${domainText(next)}</dd></div>
      </dl>
      <footer><button data-open-detail="${unit}">${pick('查看履历', 'View history')}</button><button data-prototype-action="qr">${pick('设备二维码', 'Device QR')}</button></footer>
    </article>`).join('');
  const fields = deviceImportFields.map(([label, field, rule, purpose]) => `
    <div class="field-model-row"><span><strong>${pick(...label)}</strong><code>${field}</code></span><span>${pick(...rule)}</span><span>${pick(...purpose)}</span></div>`).join('');
  return `
    <section class="screen screen-records" data-specnav-screen="device-ledger">
      ${snapshotNote()}
      ${pageTitle('REGISTERED ASSETS · IMPORT READY', '设备台账与批量入库字段', 'Device ledger and import fields', `
        <button class="secondary-button" data-prototype-action="download-template">${pick('下载字段模板', 'Download field template')}</button>
        <button class="primary-button" data-prototype-action="import">${pick('批量入库', 'Bulk import')}</button>
      `)}
      <div class="data-metric-grid">${metricCards([
        [['系统已建档', 'Registered'], 4, ['不是实际总库存', 'Not actual inventory'], 'blue'],
        [['可用设备', 'Available'], 2, ['可参与分配', 'Assignment candidates'], 'green'],
        [['在租设备', 'Rented'], 2, ['均有有效排期', 'Both have schedules'], 'amber'],
        [['待补数据', 'Data gaps'], 4, ['库位 4、SN 1', '4 locations, 1 SN'], 'red'],
      ])}</div>
      <section class="import-boundary">
        <div><p>IMPORT POLICY</p><h2>${pick('几百台设备导入前先统一字段', 'Normalize fields before importing hundreds of devices')}</h2><span>${pick('设备编号和 SN 在租户内唯一；型号代码决定分组与订单映射；未建档设备不参与排期。', 'Device ID and SN are tenant-unique. Model code drives grouping and order mapping. Unregistered assets are excluded from scheduling.')}</span></div>
        <code>device_no, serial_number, equipment_model_code, warehouse_code, status, purchase_amount_fen, enabled</code>
      </section>
      <section class="field-model-panel" data-specnav-component="DeviceImportFieldModel">
        <header><span>${pick('字段', 'Field')}</span><span>${pick('校验规则', 'Validation')}</span><span>${pick('业务用途', 'Purpose')}</span></header>
        ${fields}
      </section>
      ${filterBar({
        search: ['搜索已建档设备编号、型号或 SN', 'Search registered device, model, or SN'],
        filters: [['全部库位', 'All locations'], ['全部状态', 'All states'], ['4 个已建档型号', '4 registered models']],
        summary: ['当前卡片只展示线上已建档的 4 台设备', 'Cards show only the 4 currently registered devices'],
      })}
      <div class="device-card-grid">${cards}</div>
    </section>`;
}

export function renderExceptions() {
  const rows = exceptions.map(([id, type, title, target, level, age]) => `
    <button class="record-row exception-row" data-open-detail="${id}">
      <span class="exception-level" data-tone="${level === '阻塞' ? 'red' : level === '待复核' ? 'amber' : 'blue'}"></span>
      <span><strong>${domainText(type)}</strong><small class="data-code">${id}</small></span>
      <span><strong>${domainText(title)}</strong><small>${domainText(target)}</small></span>
      ${badge(level)}<span>${domainText(age)}</span><em>→</em>
    </button>`).join('');
  return `
    <section class="screen screen-records" data-specnav-screen="exceptions">
      ${snapshotNote()}
      ${pageTitle('OPEN REVIEWS 755 · OPEN ALERTS 71', '异常、人工复核与数据质量', 'Exceptions, reviews, and data quality', `<button class="secondary-button" data-prototype-action="refresh">${pick('刷新状态', 'Refresh')}</button>`)}
      <div class="data-metric-grid">${metricCards([
        [['开放人工复核', 'Open reviews'], 755, ['4 类原因', '4 reason groups'], 'amber'],
        [['开放告警', 'Open alerts'], 71, ['售后、同步、授权', 'After-sale, sync, auth'], 'red'],
        [['有效店铺', 'Valid shops'], 3, ['共 6 个店铺', '6 shops total'], 'green'],
        [['失效店铺', 'Invalid shops'], 3, ['写操作必须阻断', 'Writes must be blocked'], 'red'],
      ])}</div>
      ${filterBar({
        filters: [['全部严重级别', 'All severity'], ['聚合异常类型', 'Aggregate types'], ['未解决', 'Unresolved']],
        summary: ['脱敏生产聚合；自动转换 creator 缺失来自近期服务端日志', 'Masked aggregates; missing creator is from a recent server log'],
      })}
      <section class="records-panel">
        <header class="record-columns exception-columns"><span></span><span>${pick('类型', 'Type')}</span><span>${pick('异常说明', 'Description')}</span><span>${pick('级别', 'Severity')}</span><span>${pick('时间', 'Time')}</span><span></span></header>
        <div>${rows}</div>
      </section>
      <aside class="safe-policy"><span>SERVER AUTHORITY</span><strong>${pick('设备导入、状态校准和异常处置都必须由服务端确认', 'Imports, status reconciliation, and resolutions require server acceptance')}</strong><p>${pick('原型不写生产数据库；重复设备编号或 SN 必须进入人工复核，不能覆盖已有资产。', 'The prototype never writes production data. Duplicate device IDs or SNs require review and never overwrite existing assets.')}</p></aside>
    </section>`;
}
