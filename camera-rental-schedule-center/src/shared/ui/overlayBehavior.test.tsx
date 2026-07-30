import assert from 'node:assert/strict';
import test, { after } from 'node:test';
import { useState } from 'react';
import { Window } from 'happy-dom';

import type { WorkspaceTab } from '../../app/navigation';
import { useQuickBindingWorkspace } from '../../features/workspace/useQuickBindingWorkspace';
import { ConfirmDialogShell } from './ConfirmDialogShell';
import { DetailDrawerShell } from './DetailDrawerShell';

const browser = new Window();
Object.assign(globalThis, {
  window: browser,
  document: browser.document,
  HTMLElement: browser.HTMLElement,
  Event: browser.Event,
  KeyboardEvent: browser.KeyboardEvent,
  MouseEvent: browser.MouseEvent,
});
Object.defineProperty(globalThis, 'navigator', {
  configurable: true,
  value: browser.navigator,
});
const { createRoot } = await import('react-dom/client');
const { flushSync } = await import('react-dom');

after(async () => {
  await browser.happyDOM.close();
});

function keydown(key: string, shiftKey = false) {
  flushSync(() => {
    document.dispatchEvent(
      new KeyboardEvent('keydown', { bubbles: true, key, shiftKey })
    );
  });
}

function QuickBindingHarness() {
  const [activeTab, setActiveTab] = useState<WorkspaceTab>('orders');
  const quickBinding = useQuickBindingWorkspace(setActiveTab);

  return (
    <>
      <output data-testid="route">{activeTab}</output>
      <output data-testid="order">
        {quickBinding.preselectedOrderForBinding || 'none'}
      </output>
      <button
        id="order-shipping-trigger"
        type="button"
        onClick={() => quickBinding.openQuickBinding('order-1')}
      >
        Open order shipping
      </button>
      <button
        id="shipping-page-trigger"
        type="button"
        onClick={() => quickBinding.openQuickBinding(null)}
      >
        Open shipping page
      </button>
      {quickBinding.isQuickBindingOpen && (
        <ConfirmDialogShell
          ariaLabel="Shipping"
          closeLabel="Close shipping"
          title="Shipping"
          onClose={quickBinding.closeQuickBinding}
        >
          <button id="shipping-content-action" type="button">
            Confirm
          </button>
        </ConfirmDialogShell>
      )}
    </>
  );
}

function DrawerHarness() {
  const [open, setOpen] = useState(false);
  return (
    <>
      <button id="drawer-trigger" type="button" onClick={() => setOpen(true)}>
        Open drawer
      </button>
      {open && (
        <DetailDrawerShell
          title="Device"
          closeLabel="Close drawer"
          onClose={() => setOpen(false)}
        >
          <button id="drawer-content-action" type="button">
            Inspect
          </button>
        </DetailDrawerShell>
      )}
    </>
  );
}

test('quick-binding modal preserves route, traps focus, and restores its trigger', () => {
  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(<QuickBindingHarness />));

  const trigger = document.querySelector<HTMLButtonElement>(
    '#order-shipping-trigger'
  )!;
  let focusReturnCount = 0;
  trigger.addEventListener('focus', () => {
    focusReturnCount += 1;
  });
  trigger.focus();
  const escapeFocusBaseline = focusReturnCount;
  flushSync(() => trigger.click());

  assert.equal(
    document.querySelector('[data-testid="route"]')?.textContent,
    'orders'
  );
  assert.equal(
    document.querySelector('[data-testid="order"]')?.textContent,
    'order-1'
  );
  assert.equal(
    document.activeElement?.getAttribute('aria-label'),
    'Close shipping'
  );

  const content = document.querySelector<HTMLButtonElement>(
    '#shipping-content-action'
  )!;
  content.focus();
  keydown('Tab');
  assert.equal(
    document.activeElement?.getAttribute('aria-label'),
    'Close shipping'
  );
  keydown('Tab', true);
  assert.ok(document.activeElement === content);

  keydown('Escape');
  assert.ok(document.querySelector('[role="dialog"]') === null);
  assert.equal(focusReturnCount, escapeFocusBaseline + 1);
  assert.equal(
    document.querySelector('[data-testid="route"]')?.textContent,
    'orders'
  );
  assert.equal(
    document.querySelector('[data-testid="order"]')?.textContent,
    'none'
  );

  trigger.focus();
  const closeFocusBaseline = focusReturnCount;
  flushSync(() => trigger.click());
  const close = document.querySelector<HTMLButtonElement>(
    '[aria-label="Close shipping"]'
  )!;
  flushSync(() => close.click());
  assert.ok(document.querySelector('[role="dialog"]') === null);
  assert.equal(focusReturnCount, closeFocusBaseline + 1);

  const pageTrigger = document.querySelector<HTMLButtonElement>(
    '#shipping-page-trigger'
  )!;
  flushSync(() => pageTrigger.click());
  assert.equal(
    document.querySelector('[data-testid="route"]')?.textContent,
    'binding'
  );
  assert.ok(document.querySelector('[role="dialog"]') === null);

  flushSync(() => root.unmount());
});

test('detail drawer traps focus, dismisses on Escape, and restores its trigger', () => {
  document.body.innerHTML = '<div id="root"></div>';
  const root = createRoot(document.querySelector('#root')!);
  flushSync(() => root.render(<DrawerHarness />));

  const trigger = document.querySelector<HTMLButtonElement>('#drawer-trigger')!;
  let focusReturnCount = 0;
  trigger.addEventListener('focus', () => {
    focusReturnCount += 1;
  });
  trigger.focus();
  const focusBaseline = focusReturnCount;
  flushSync(() => trigger.click());
  assert.equal(
    document.activeElement?.getAttribute('aria-label'),
    'Close drawer'
  );

  const content = document.querySelector<HTMLButtonElement>(
    '#drawer-content-action'
  )!;
  content.focus();
  keydown('Tab');
  assert.equal(
    document.activeElement?.getAttribute('aria-label'),
    'Close drawer'
  );
  keydown('Tab', true);
  assert.ok(document.activeElement === content);

  keydown('Escape');
  assert.ok(document.querySelector('[role="dialog"]') === null);
  assert.equal(focusReturnCount, focusBaseline + 1);

  flushSync(() => root.unmount());
});
