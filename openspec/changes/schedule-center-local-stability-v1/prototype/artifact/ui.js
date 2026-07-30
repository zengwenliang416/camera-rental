import { pick } from './state.js';

const statusTone = {
  '准备中': 'neutral',
  '待发货': 'blue',
  '未分配': 'red',
  '已归还': 'green',
  '检测中': 'amber',
  '逾期': 'red',
  '在途': 'blue',
  '维修中': 'ink',
  '已完成': 'green',
  '检测': 'amber',
  '可用': 'green',
  '占用': 'blue',
  '发货中': 'amber',
  '维修锁定': 'ink',
  '缺 1 台': 'red',
  '待排期': 'amber',
  '待复核': 'red',
  '履约中': 'green',
  '已发货': 'green',
  '待归还': 'amber',
  '状态待校准': 'red',
  '已启用': 'green',
  '冲突': 'red',
  '阻塞': 'red',
  '关注': 'amber',
};

const statusEnglish = {
  '准备中': 'Preparing',
  '待发货': 'To ship',
  '未分配': 'Unassigned',
  '已归还': 'Returned',
  '检测中': 'Inspecting',
  '逾期': 'Overdue',
  '在途': 'In transit',
  '维修中': 'Maintenance',
  '已完成': 'Completed',
  '检测': 'Inspecting',
  '可用': 'Available',
  '占用': 'Occupied',
  '发货中': 'Outbound',
  '维修锁定': 'Locked',
  '缺 1 台': 'Short 1',
  '待排期': 'To schedule',
  '待复核': 'Review',
  '履约中': 'Active',
  '已发货': 'Shipped',
  '待归还': 'To return',
  '状态待校准': 'Reconcile',
  '已启用': 'Enabled',
  '冲突': 'Conflict',
  '阻塞': 'Blocking',
  '关注': 'Attention',
  '已预订': 'Reserved',
  '待确认': 'Pending',
  '维保锁定': 'Maintenance',
};

export const badge = (label) => `<span class="status-badge" data-tone="${statusTone[label] || 'neutral'}">${pick(label, statusEnglish[label] || label)}</span>`;

export function pageTitle(kicker, zh, en, meta = '') {
  return `
    <header class="page-title">
      <div><p>${kicker}</p><h1>${pick(zh, en)}</h1></div>
      ${meta ? `<div class="page-title-meta">${meta}</div>` : ''}
    </header>`;
}

export function filterBar({ search, filters = [], summary = '', actions = '' }) {
  const selects = filters.map(([zh, en]) => `<button class="filter-chip">${pick(zh, en)} <span>⌄</span></button>`).join('');
  return `
    <div class="filter-bar" data-specnav-component="FilterToolbar">
      ${search ? `<button class="filter-search" data-prototype-action="filter-search"><span>⌕</span>${pick(search[0], search[1])}</button>` : ''}
      <div class="filter-group">${selects}</div>
      ${summary ? `<span class="filter-summary">${pick(summary[0], summary[1])}</span>` : ''}
      ${actions ? `<div class="filter-actions">${actions}</div>` : ''}
    </div>`;
}

export const dateHeader = (dates) => dates.map((date) => `
  <span class="${date.today ? 'is-today' : ''}"><b>${date.short}</b><small>${pick(date.zh, date.en)}</small></span>`).join('');

export function compactStat(value, zh, en, tone = '') {
  return `<div class="compact-stat" data-tone="${tone}"><span>${pick(zh, en)}</span><strong>${value}</strong></div>`;
}

export function snapshotNote() {
  return `<div class="snapshot-note" data-specnav-component="ProductionSnapshotBoundary"><span>●</span><strong>${pick('脱敏生产快照', 'Masked production snapshot')}</strong><small>${pick('2026-07-29 17:12 · 租户 1 · 仅展示系统已建档资产，实际设备总量待导入', 'Jul 29, 2026 17:12 · tenant 1 · registered assets only; actual inventory pending import')}</small></div>`;
}

export function emptyHint(zh, en) {
  return `<div class="inline-empty"><span>○</span><strong>${pick(zh, en)}</strong></div>`;
}

export const dataCode = (value) => `<span class="data-code">${value}</span>`;
