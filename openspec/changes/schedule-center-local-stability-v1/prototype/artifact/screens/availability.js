import { availabilityRows, dates, serialRows } from '../fixtures.js';
import { domainText, pick } from '../state.js';
import { badge, dateHeader, filterBar, pageTitle, snapshotNote } from '../ui.js';

const availabilityTone = (value) => value.startsWith('0/') ? 'shortage' : 'safe';

export function renderAvailability() {
  const matrixRows = availabilityRows.map(([model, category, physical, bookable, locked, values]) => `
    <div class="matrix-row">
      <button class="matrix-model" data-open-detail="${model}"><strong>${model}</strong><small>${domainText(category)}</small></button>
      <span class="matrix-number">${physical}</span><span class="matrix-number">${bookable}</span><span class="matrix-number">${locked}</span>
      <div class="matrix-days">${values.map((value, index) => `<button class="availability-cell" data-tone="${availabilityTone(value)}" data-open-detail="${model}-${dates[index].short}">${value}</button>`).join('')}</div>
    </div>`).join('');
  const serials = serialRows.map(([sn, status, location, start, span, label]) => `
    <div class="serial-row">
      <span class="data-code">${domainText(sn)}</span>${badge(status)}<span>${domainText(location)}</span>
      <div class="serial-track">${span ? `<button style="--start:${start};--span:${span}" data-open-detail="${sn}">${domainText(label)}</button>` : `<span>${pick('可用', 'Available')}</span>`}</div>
    </div>`).join('');
  return `
    <section class="screen screen-availability" data-specnav-screen="availability">
      ${snapshotNote()}
      ${pageTitle('REGISTERED CAPACITY · NOT ACTUAL INVENTORY', '已建档设备可用性', 'Registered-device availability', `
        <span class="alert-counter">${pick('已建档', 'Registered')} <b>4</b></span>
        <span class="alert-counter">${pick('可用', 'Available')} <b>2</b></span>
        <span class="alert-counter">${pick('待导入', 'Pending import')} <b>—</b></span>
      `)}
      ${filterBar({
        search: ['搜索已建档型号、SN 或设备编号', 'Search registered model, SN, or device'],
        filters: [['未来 10 天', 'Next 10 days'], ['4 个已建档型号', '4 registered models'], ['全部已建档设备', 'All registered devices']],
        summary: ['这里不是实际总库存；未导入设备不会进入可用性计算', 'This is not actual inventory; unregistered assets are excluded'],
      })}
      <div class="availability-layout">
        <section class="matrix-panel">
          <div class="matrix-scroll">
            <div class="matrix-canvas">
              <div class="matrix-header"><span>${pick('型号', 'Model')}</span><span>${pick('已建档', 'Registered')}</span><span>${pick('可预订', 'Bookable')}</span><span>${pick('维保锁定', 'Locked')}</span><div class="matrix-dates">${dateHeader(dates)}</div></div>
              ${matrixRows}
            </div>
          </div>
          <footer><span><i data-tone="shortage"></i>${pick('已建档设备不可用', 'Registered unit unavailable')}</span><span><i data-tone="safe"></i>${pick('已建档设备可用', 'Registered unit available')}</span></footer>
        </section>
        <aside class="shortage-resolver">
          <header><div><p>INVENTORY BOUNDARY</p><h2>${pick('真实库存尚不能计算', 'Actual capacity unavailable')}</h2></div><button data-prototype-action="close-resolver">×</button></header>
          <section class="shortage-causes">
            <h3>${pick('当前系统口径', 'Current system scope')}</h3>
            <div><span>${pick('系统已建档', 'Registered')}</span><strong>4 ${pick('台', 'units')}</strong></div>
            <div><span>${pick('已有有效排期', 'Effective schedules')}</span><strong>2 ${pick('条', 'records')}</strong></div>
            <div><span>${pick('实际设备资产', 'Actual assets')}</span><strong>${pick('待导入', 'Pending import')}</strong></div>
            <footer><span>${pick('可用', 'Available')} <b>2</b></span><span>${pick('在租', 'Rented')} <b>2</b></span><span class="danger">${pick('库存置信度', 'Inventory confidence')} <b>${pick('不完整', 'Incomplete')}</b></span></footer>
          </section>
          <section class="replacement-list">
            <h3>${pick('导入前置检查', 'Import prerequisites')}</h3>
            <article><div><strong>${pick('统一型号代码', 'Canonical model codes')}</strong><code>P3 / P4 / P4P / A6 / ...</code><small>${pick('禁止同一型号使用多个别名', 'No aliases for the same model')}</small></div><div><button data-screen-target="devices">${pick('查看字段', 'Review fields')}</button></div></article>
            <article><div><strong>${pick('设备编号与 SN 去重', 'Deduplicate IDs and SNs')}</strong><code>tenant + deviceNo / serialNumber</code><small>${pick('重复项进入人工复核', 'Duplicates require review')}</small></div><div><button data-screen-target="exceptions">${pick('查看规则', 'View policy')}</button></div></article>
            <article><div><strong>${pick('仓库与库位编码', 'Warehouse and location codes')}</strong><code>warehouseCode</code><small>${pick('不要混用名称和自由文本', 'Do not mix names and free text')}</small></div><div><button data-screen-target="devices">${pick('查看模板', 'View template')}</button></div></article>
          </section>
        </aside>
      </div>
      <section class="serial-panel">
        <header><div><strong>${pick('全部已建档设备实例', 'All registered device instances')}</strong><span>${pick('4 台已建档 · 2 台可用 · 2 台在租 · 1 台 SN 待补录', '4 registered · 2 available · 2 rented · 1 missing SN')}</span></div><div class="serial-legend"><span>${pick('占用', 'Occupied')}</span><span>${pick('可用', 'Available')}</span></div></header>
        <div class="serial-scroll"><div class="serial-canvas"><div class="serial-header"><span>SN</span><span>${pick('状态', 'Status')}</span><span>${pick('位置', 'Location')}</span><div>${dateHeader(dates)}</div></div>${serials}</div></div>
      </section>
    </section>`;
}
