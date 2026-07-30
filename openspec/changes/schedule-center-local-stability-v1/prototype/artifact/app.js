import { renderAvailability } from './screens/availability.js';
import { renderDevices, renderExceptions, renderOrders } from './screens/records.js';
import { renderSchedule } from './screens/schedule.js';
import { renderShipping } from './screens/shipping.js';
import { renderWorkbench } from './screens/workbench.js';
import { renderLogin } from './screens/login.js';
import { translations, uiState, pick } from './state.js';

const app = document.querySelector('.app-shell');
const screenRoot = document.querySelector('#screen-root');
const drawer = document.querySelector('[data-detail-drawer]');
const backdrop = document.querySelector('[data-drawer-backdrop]');
const toast = document.querySelector('.prototype-toast');
let toastTimer;

const renderers = {
  workbench: renderWorkbench,
  schedule: renderSchedule,
  availability: renderAvailability,
  orders: renderOrders,
  devices: renderDevices,
  shipping: renderShipping,
  exceptions: renderExceptions,
  login: renderLogin,
};

const requestedScreen = new URLSearchParams(window.location.search).get('screen');
if (requestedScreen && renderers[requestedScreen]) uiState.screen = requestedScreen;

const screenAnchors = {
  workbench: 'workbench',
  schedule: 'gantt-schedule',
  availability: 'availability',
  orders: 'rental-orders',
  devices: 'device-ledger',
  shipping: 'shipping-workbench',
  exceptions: 'exceptions',
  login: 'login',
};

function applyTranslations() {
  document.documentElement.lang = uiState.locale;
  document.querySelectorAll('[data-i18n]').forEach((element) => {
    const value = translations[uiState.locale][element.dataset.i18n];
    if (value) element.textContent = value;
  });
  document.querySelectorAll('[data-locale-toggle]').forEach((button) => {
    button.textContent = uiState.locale === 'en' ? '中' : 'EN';
  });
}

function renderScreen() {
  screenRoot.innerHTML = renderers[uiState.screen]();
  screenRoot.dataset.specnavScreen = screenAnchors[uiState.screen];
  app.classList.toggle('is-login-preview', uiState.screen === 'login');
  document.querySelectorAll('[data-screen-target]').forEach((button) => button.classList.toggle('is-active', button.dataset.screenTarget === uiState.screen));
  const url = new URL(window.location.href);
  if (uiState.screen === 'workbench') url.searchParams.delete('screen');
  else url.searchParams.set('screen', uiState.screen);
  window.history.replaceState({}, '', url);
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.add('is-visible');
  toastTimer = window.setTimeout(() => toast.classList.remove('is-visible'), 2200);
}

function openDrawer(identifier) {
  drawer.innerHTML = `
    <header><div><p>READ-ONLY CONTEXT</p><h2>${identifier}</h2></div><button data-drawer-close aria-label="${pick('关闭详情', 'Close detail')}">×</button></header>
    <section><h3>${pick('关键标识', 'Key identifiers')}</h3><dl><div><dt>${pick('记录', 'Record')}</dt><dd class="data-code">${identifier}</dd></div><div><dt>${pick('数据来源', 'Source')}</dt><dd>${pick('脱敏生产快照', 'Masked production snapshot')}</dd></div><div><dt>${pick('快照时间', 'Captured at')}</dt><dd>2026-07-29 17:12</dd></div><div><dt>${pick('客户信息', 'Customer data')}</dt><dd>${pick('不复制真实姓名、电话或地址', 'No real name, phone, or address copied')}</dd></div></dl></section>
    <section><h3>${pick('操作边界', 'Action policy')}</h3><p>${pick('分配、发货、回仓和异常处置必须等待服务端响应后刷新状态。当前原型不会提交真实业务数据。', 'Assignment, shipment, return, and resolution wait for server acceptance. This prototype submits no real data.')}</p></section>
    <section class="drawer-timeline"><h3>${pick('数据边界', 'Data boundary')}</h3><ol><li><b>17:12</b><span>${pick('从生产数据库读取脱敏快照', 'Masked snapshot read from production database')}</span></li><li><b>READ ONLY</b><span>${pick('原型未写入生产数据库', 'Prototype did not write production data')}</span></li><li><b>SERVER</b><span>${pick('业务状态仍以服务端为准', 'Business state remains server-authoritative')}</span></li></ol></section>
    <button class="primary-button" data-drawer-close>${pick('返回工作台', 'Back to workspace')}</button>`;
  drawer.classList.add('is-open');
  backdrop.classList.add('is-open');
  drawer.setAttribute('aria-hidden', 'false');
  drawer.querySelector('[data-drawer-close]').focus();
}

function closeDrawer() {
  drawer.classList.remove('is-open');
  backdrop.classList.remove('is-open');
  drawer.setAttribute('aria-hidden', 'true');
}

function setReviewState(nextState, source) {
  uiState.reviewState = nextState;
  app.dataset.specnavState = nextState;
  document.querySelectorAll('[data-specnav-set-state]').forEach((button) => button.classList.toggle('is-active', button === source));
}

function selectShippingDevice(card) {
  const list = card.closest('.choice-list');
  if (list) list.querySelectorAll('.choice-row').forEach((row) => row.classList.toggle('is-selected', row === card));
  const unit = card.dataset.deviceUnit;
  const shortUnit = unit?.split('-').slice(0, 2).join('-') || unit;
  const unitTarget = document.querySelector('[data-selected-device-unit]');
  const snTarget = document.querySelector('[data-selected-device-sn]');
  const modelTarget = document.querySelector('[data-selected-device-model]');
  if (unitTarget) unitTarget.textContent = unit;
  if (snTarget) snTarget.textContent = card.dataset.deviceSn;
  if (modelTarget) modelTarget.textContent = `${card.dataset.deviceModel} · AVAILABLE`;
  document.querySelectorAll('.dispatch-readiness [data-state="ready"] strong').forEach((target) => {
    target.textContent = pick(`已选择 ${shortUnit}`, `${shortUnit} selected`);
  });
}

function filterShippingDevices(input) {
  const query = input.value.trim().toLowerCase();
  const cards = [...document.querySelectorAll('[data-device-search-text]')];
  let visibleCount = 0;
  cards.forEach((card) => {
    const visible = !query || card.dataset.deviceSearchText.includes(query);
    card.hidden = !visible;
    if (visible) visibleCount += 1;
  });
  const selectedCard = cards.find((card) => card.classList.contains('is-selected') && !card.hidden);
  if (!selectedCard && visibleCount > 0) selectShippingDevice(cards.find((card) => !card.hidden));
  const emptyState = document.querySelector('[data-device-search-empty]');
  if (emptyState) emptyState.hidden = visibleCount > 0;
}

function updateOrderSearchState(input) {
  const query = input.value.trim();
  const status = document.querySelector('[data-order-search-status]');
  const resultPanel = document.querySelector('[data-order-results]');
  if (!status || !resultPanel) return;
  resultPanel.dataset.queryState = query ? 'searched' : 'idle';
  status.textContent = query
    ? pick('当前脱敏快照无可确认结果，生产查询仍会返回未脱敏详情和明确阻塞原因', 'No eligible result exists in this masked snapshot; production search still returns unmasked details and explicit blockers')
    : pick('等待输入检索条件', 'Waiting for a search query');
}

document.addEventListener('click', (event) => {
  const screenButton = event.target.closest('[data-screen-target]');
  if (screenButton) {
    uiState.screen = screenButton.dataset.screenTarget;
    renderScreen();
    window.scrollTo({ top: 0, behavior: 'smooth' });
    return;
  }
  if (event.target.closest('[data-theme-toggle]')) {
    uiState.theme = uiState.theme === 'dark' ? 'light' : 'dark';
    document.documentElement.dataset.theme = uiState.theme;
    showToast(pick(`已切换为${uiState.theme === 'dark' ? '深色' : '浅色'}主题`, `${uiState.theme === 'dark' ? 'Dark' : 'Light'} theme enabled`));
    return;
  }
  if (event.target.closest('[data-locale-toggle]')) {
    uiState.locale = uiState.locale === 'en' ? 'zh-CN' : 'en';
    applyTranslations();
    renderScreen();
    return;
  }
  const stateButton = event.target.closest('[data-specnav-set-state]');
  if (stateButton) {
    setReviewState(stateButton.dataset.specnavSetState, stateButton);
    return;
  }
  if (event.target.closest('[data-review-toggle]')) {
    document.querySelector('.review-panel').classList.toggle('is-collapsed');
    return;
  }
  const detailButton = event.target.closest('[data-open-detail]');
  if (detailButton) {
    openDrawer(detailButton.dataset.openDetail);
    return;
  }
  if (event.target.closest('[data-drawer-close]') || event.target === backdrop) {
    closeDrawer();
    return;
  }
  const action = event.target.closest('[data-prototype-action]');
  if (action) {
    if (action.dataset.prototypeAction === 'select-device') {
      selectShippingDevice(action);
    } else if (action.dataset.prototypeAction === 'select-order') {
      action.closest('.choice-list').querySelectorAll('.choice-row').forEach((row) => row.classList.toggle('is-selected', row === action));
    }
    showToast(pick('已记录原型操作，不会提交真实业务数据', 'Prototype intent recorded; no real data submitted'));
  }
});

document.addEventListener('input', (event) => {
  if (event.target.matches('[data-device-search]')) filterShippingDevices(event.target);
  if (event.target.matches('[data-order-search]')) updateOrderSearchState(event.target);
});

document.addEventListener('keydown', (event) => {
  if (event.key === 'Escape') closeDrawer();
  if (event.target.matches('input, select, textarea')) return;
  const shortcuts = { '1': 'workbench', '2': 'schedule', '3': 'availability', '4': 'orders', '5': 'devices', '6': 'shipping', '7': 'exceptions' };
  if (shortcuts[event.key]) {
    uiState.screen = shortcuts[event.key];
    renderScreen();
  }
});

applyTranslations();
renderScreen();
