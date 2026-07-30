import { dates, scheduleGroups } from '../fixtures.js';
import { domainText, pick } from '../state.js';
import { badge, dateHeader, filterBar, pageTitle, snapshotNote } from '../ui.js';

const segment = ([type, start, span, label]) => `
  <button class="timeline-segment" data-type="${type}" style="--start:${start};--span:${span}" data-open-detail="${label}">
    <span>${domainText(label)}</span>
  </button>`;

const groupRows = (group) => `
  <div class="timeline-group">
    <div class="group-label"><strong>${domainText(group.model)}</strong><small>${domainText(group.category)}</small><em>${group.units.length} ${pick('台', 'units')}</em></div>
    <div class="group-calendar"></div>
  </div>
  ${group.units.map((unit) => `
    <div class="timeline-row">
      <button class="device-identity" data-open-detail="${unit.unit}"><strong>${unit.unit}</strong><small>${domainText(unit.sn)}</small></button>
      <div class="device-state">${badge(unit.status)}</div>
      <div class="timeline-track">${unit.segments.map(segment).join('')}</div>
    </div>`).join('')}`;

export function renderSchedule() {
  return `
    <section class="screen screen-schedule" data-specnav-screen="gantt-schedule">
      ${snapshotNote()}
      ${pageTitle('RESOURCE PLANNER · 10 DAYS', '独立设备排期', 'Physical device schedule', `
        <button class="secondary-button" data-prototype-action="export">${pick('导出排期', 'Export')}</button>
        <button class="primary-button" data-prototype-action="assign">${pick('分配设备', 'Assign device')}</button>
      `)}
      ${filterBar({
        search: ['搜索设备编号、SN 或订单号', 'Search unit, SN, or order'],
        filters: [['未来 10 天', 'Next 10 days'], ['4 台已建档设备', '4 registered devices'], ['全部状态', 'All states']],
        summary: ['仅计算已建档设备；实际资产总量待导入', 'Registered devices only; actual inventory pending import'],
      })}
      <div class="schedule-layout">
        <section class="timeline-panel">
          <div class="timeline-scroll" aria-label="${pick('设备占用时间轴，可横向滚动', 'Device occupancy timeline, horizontally scrollable')}">
            <div class="timeline-canvas">
              <div class="timeline-header">
                <div class="device-heading">${pick('设备 / SN', 'Device / SN')}</div>
                <div class="state-heading">${pick('状态', 'Status')}</div>
                <div class="timeline-dates">${dateHeader(dates)}</div>
              </div>
              ${scheduleGroups.map(groupRows).join('')}
            </div>
          </div>
          <footer class="timeline-footer">
            <div class="timeline-legend">
              <span data-type="prep">${pick('发货准备', 'Preparation')}</span>
              <span data-type="occupied">${pick('设备占用', 'Occupied')}</span>
              <span data-type="billable">${pick('计租区间', 'Billable')}</span>
              <span data-type="return">${pick('回仓运输', 'Return')}</span>
              <span data-type="inspect">${pick('检测缓冲', 'Inspection')}</span>
              <span data-type="maintenance">${pick('维修锁定', 'Maintenance')}</span>
              <span data-type="conflict">${pick('排期冲突', 'Conflict')}</span>
            </div>
            <div class="timeline-totals"><span>${pick('已建档', 'Registered')} <b>4</b></span><span>${pick('占用', 'Occupied')} <b>2</b></span><span>${pick('可用', 'Available')} <b>2</b></span><span>${pick('维保', 'Maintenance')} <b>0</b></span><span class="danger">${pick('未建档', 'Not imported')} <b>—</b></span></div>
          </footer>
        </section>
        <aside class="action-rail">
          <header><h2>${pick('数据完整性', 'Data completeness')}</h2><em>4</em></header>
          <section data-tone="amber"><div><b>●</b><strong>${pick('资产尚未完整导入', 'Inventory import incomplete')}</strong><em>—</em></div><p>${pick('系统只有 4 台已建档设备，不能代表实际几百台资产。', 'Only 4 devices are registered; this is not the actual inventory.')}</p><button data-screen-target="devices">${pick('查看字段', 'Review fields')}</button></section>
          <section data-tone="blue"><div><b>◷</b><strong>${pick('有效设备排期', 'Effective schedules')}</strong><em>2</em></div><p>XY-0****0812 · P4P<br>XY-0****0803 · A6</p><button data-screen-target="orders">${pick('查看订单', 'View orders')}</button></section>
          <section data-tone="red"><div><b>△</b><strong>${pick('内部订单状态待校准', 'Internal status mismatch')}</strong><em>2</em></div><p>${pick('订单仍为待分配，但设备分配已出库。', 'Orders remain pending allocation while assignments are dispatched.')}</p><button data-screen-target="exceptions">${pick('查看异常', 'View exceptions')}</button></section>
          <section data-tone="green"><div><b>⌘</b><strong>${pick('已建档可用设备', 'Registered available devices')}</strong><em>2</em></div><p>P3-05 · P3<br>P4-105 · P4</p><button data-screen-target="availability">${pick('查看可用性', 'View availability')}</button></section>
        </aside>
      </div>
    </section>`;
}
