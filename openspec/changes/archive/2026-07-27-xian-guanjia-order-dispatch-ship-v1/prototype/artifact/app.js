'use strict';

/* specnav prototype harness — admin/staff screen switch, theme toggle, dialog open, state machines */
const body = document.body;

/* ---- screen switch (admin / staff) ---- */
const screenBtns = Array.from(document.querySelectorAll('.screen-switch button'));
screenBtns.forEach((b) => b.addEventListener('click', () => {
  const s = b.getAttribute('data-screen');
  body.setAttribute('data-screen', s);
  screenBtns.forEach((x) => x.classList.toggle('active', x === b));
}));

/* ---- theme toggle (both buttons) ---- */
function setTheme(t){ body.setAttribute('data-theme', t); }
document.getElementById('themeToggle').addEventListener('click', () =>
  setTheme(body.getAttribute('data-theme') === 'light' ? 'dark' : 'light'));
document.getElementById('themeToggle2').addEventListener('click', () =>
  setTheme(body.getAttribute('data-theme') === 'light' ? 'dark' : 'light'));

/* ---- admin dialog open/close ---- */
const dialog = document.querySelector('.dialog');
function openDialog(){ body.setAttribute('data-modal-open', 'true'); setState('populated'); }
function closeDialog(){ body.setAttribute('data-modal-open', 'false'); }
document.getElementById('openShip').addEventListener('click', openDialog);
document.getElementById('closeDialog').addEventListener('click', closeDialog);
document.getElementById('cancelDialog').addEventListener('click', closeDialog);
document.querySelector('.mask').addEventListener('click', closeDialog);

/* ---- admin state machine ---- */
const stateBtns = Array.from(document.querySelectorAll('.state-toolbar button[data-state]'));
const confirmChk = document.getElementById('confirmChk');
const submitBtn = document.getElementById('submitShip');
function setState(st){ dialog.setAttribute('data-state', st); stateBtns.forEach((x) => x.classList.toggle('on', x.getAttribute('data-state') === st)); refreshSubmit(); }
stateBtns.forEach((b) => b.addEventListener('click', () => setState(b.getAttribute('data-state'))));

function refreshSubmit(){
  const st = dialog.getAttribute('data-state');
  submitBtn.disabled = !(st === 'populated' && confirmChk && confirmChk.checked);
  if (st === 'ship-success') submitBtn.disabled = true;
}
confirmChk.addEventListener('change', refreshSubmit);
submitBtn.addEventListener('click', () => { setState('ship-success'); });

/* ---- staff state machine ---- */
const staffBtns = Array.from(document.querySelectorAll('.staff-state-toolbar button[data-staff-state]'));
const phone = document.querySelector('.phone-frame');
const staffConfirm = document.getElementById('staffConfirm');
const staffSubmit = document.getElementById('staffSubmit');
function setStaffState(st){
  phone.setAttribute('data-state', st);
  staffBtns.forEach((x) => x.classList.toggle('on', x.getAttribute('data-staff-state') === st));
  refreshStaffSubmit();
}
staffBtns.forEach((b) => b.addEventListener('click', () => setStaffState(b.getAttribute('data-staff-state'))));
function refreshStaffSubmit(){
  const st = phone.getAttribute('data-state');
  staffSubmit.disabled = !(st === 'populated' && staffConfirm && staffConfirm.checked);
  if (st === 'ship-success') staffSubmit.disabled = true;
}
staffConfirm.addEventListener('change', refreshStaffSubmit);
staffSubmit.addEventListener('click', () => setStaffState('ship-success'));

/* ---- default open admin on first view ---- */
setState('populated');
refreshStaffSubmit();