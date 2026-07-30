import { productionSnapshot, workbenchLanes } from '../fixtures.js';
import { domainText, pick } from '../state.js';
import { badge, compactStat, pageTitle, snapshotNote } from '../ui.js';

const renderLane = (lane) => {
  const rows = lane.rows.map((row) => `
    <button class="lane-row ${row[5] === '状态待校准' ? 'is-overdue' : ''}" data-open-detail="${row[1]}">
      <span class="lane-time">${domainText(row[0])}</span>
      <span class="lane-order">${domainText(row[1])}</span>
      <span class="lane-item">${domainText(row[2])}</span>
      <span class="lane-owner">${domainText(row[3])} · ${domainText(row[4])}</span>
      ${badge(row[5])}
    </button>`).join('');
  const step = { ship: '1', assign: '2', return: '3', review: '4' }[lane.id];
  return `
    <section class="operation-lane" data-tone="${lane.tone}">
      <header><div><span>${step}.</span><strong>${pick(...lane.title)}</strong></div><em>${lane.count}</em></header>
      <div class="lane-columns"><span>${pick('时间 / 范围', 'Time / scope')}</span><span>${pick('订单 / 数据', 'Order / data')}</span><span>${pick('状态', 'Status')}</span></div>
      <div class="lane-scroll">${rows}</div>
    </section>`;
};

export function renderWorkbench() {
  return `
    <section class="screen screen-workbench" data-specnav-screen="workbench">
      <div class="workbench-main">
        ${snapshotNote()}
        ${pageTitle('PRODUCTION SNAPSHOT · 2026-07-29', '今日运营与建档边界', 'Today’s operations and registration boundary', `
          ${compactStat(productionSnapshot.shippingToday, '今日计划发货', 'Ship today', 'blue')}
          ${compactStat(productionSnapshot.returnsToday, '今日计划归还', 'Return today', 'green')}
          ${compactStat(productionSnapshot.registeredDevices, '系统已建档', 'Registered', 'amber')}
          ${compactStat(productionSnapshot.openReviews, '开放人工复核', 'Open reviews', 'red')}
        `)}
        <div class="operation-board">${workbenchLanes.map(renderLane).join('')}</div>
        <section class="lifecycle-panel" data-specnav-component="DeviceLifecycleStrip">
          <div class="current-device">
            <div class="camera-glyph"><span></span><i></i></div>
            <div>
              <small>${pick('真实已建档设备', 'REGISTERED DEVICE')}</small>
              <strong>A6-09 · A6</strong>
              <code>SN: 9KRXNAC00B-405D</code>
              <button data-open-detail="A6-09-9KRXNAC00B-405D">${pick('查看设备与排期', 'View device and schedule')} ↗</button>
            </div>
          </div>
          <div class="lifecycle-track">
            <header>
              <strong>${pick('设备履约阶段', 'Device fulfillment lifecycle')}</strong>
              <span>${pick('占用周期 07-29 至 08-07 · 数据库结束日 08-08 为 exclusive', 'Occupied Jul 29 to Aug 7 · database exclusive end Aug 8')}</span>
            </header>
            <div class="phase-strip">
              <span class="active">${pick('已发货出库', 'Dispatched')}</span>
              <span class="rental">${pick('租用中', 'In rental')}</span>
              <span class="return">${pick('待客户发回', 'Awaiting return')}</span>
              <span>${pick('待回仓检测', 'Awaiting inspection')}</span>
            </div>
            <div class="milestones">
              <span><i></i><b>07-29 07:31</b><small>${pick('设备出库记录', 'Dispatch record')}</small></span>
              <span><i></i><b>07-29</b><small>${pick('占用开始', 'Occupied from')}</small></span>
              <span><i></i><b>07-31</b><small>${pick('计租开始', 'Billable from')}</small></span>
              <span><i></i><b>08-07</b><small>${pick('计租与占用结束', 'Rental and occupied end')}</small></span>
              <span><i></i><b>08-08</b><small>${pick('预计重新可用', 'Expected available')}</small></span>
            </div>
          </div>
        </section>
      </div>
      <aside class="verification-pane" data-specnav-component="DataQualityPanel">
        <header><div><p>DATA REGISTRATION</p><h2>${pick('资产建档状态', 'Asset registration')}</h2></div><button data-prototype-action="close-pane">×</button></header>
        <section class="order-summary">
          <small>${pick('数据边界', 'DATA BOUNDARY')}</small>
          <strong>${pick('实际设备总量待导入', 'Actual inventory pending import')}</strong>
          <dl>
            <div><dt>${pick('系统已建档', 'Registered')}</dt><dd>4 ${pick('台', 'units')}</dd></div>
            <div><dt>${pick('可用 / 在租', 'Available / rented')}</dt><dd>2 / 2</dd></div>
            <div><dt>${pick('SN 完整', 'SN complete')}</dt><dd>3 / 4</dd></div>
            <div><dt>${pick('库位完整', 'Location complete')}</dt><dd>0 / 4</dd></div>
          </dl>
        </section>
        <ol class="verification-steps">
          <li class="is-done"><b>1</b><div><strong>${pick('统一型号代码', 'Normalize model codes')}</strong><span>P3 / P4 / P4P / A6</span><small>${pick('导入前先建立标准字典', 'Create canonical dictionary first')}</small></div></li>
          <li><b>2</b><div><strong>${pick('整理设备编号与 SN', 'Prepare device IDs and SNs')}</strong><span>${pick('租户内唯一，不覆盖现有记录', 'Unique per tenant; never overwrite')}</span></div></li>
          <li><b>3</b><div><strong>${pick('批量导入并校验', 'Import and validate')}</strong><span>${pick('重复项进入人工复核', 'Duplicates go to manual review')}</span></div></li>
        </ol>
        <section class="range-check">
          <small>${pick('当前可用于排期的数据', 'CURRENT SCHEDULING DATA')}</small>
          <div><span>${pick('有效排期', 'Effective schedules')}</span><strong>2</strong></div>
          <div><span>${pick('已出库分配', 'Dispatched assignments')}</span><strong>2</strong></div>
          <div><span>${pick('未建档资产', 'Unregistered assets')}</span><strong>${pick('数量未知', 'Unknown count')}</strong></div>
        </section>
        <button class="primary-button" data-screen-target="devices">${pick('查看入库字段', 'Review import fields')}</button>
        <p class="server-note">▣ ${pick('未建档设备不会参与可用性和冲突计算', 'Unregistered devices are excluded from availability and conflict checks')}</p>
      </aside>
    </section>`;
}
