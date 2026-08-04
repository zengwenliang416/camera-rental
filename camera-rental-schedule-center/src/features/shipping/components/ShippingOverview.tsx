import { Check, CircleAlert, History, PackageCheck, ShieldCheck, Truck } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages, type ShippingMessageKey } from '../shippingMessages';
import { StatusBadge } from '../../../shared/ui/StatusBadge';

const steps = [
  { id: 'waybill', label: 'step.waybill', icon: Truck },
  { id: 'device', label: 'step.device', icon: PackageCheck },
  { id: 'order', label: 'step.order', icon: CircleAlert },
  { id: 'permission', label: 'step.permission', icon: ShieldCheck },
] as const;

export function ShippingOverview({ controller }: { controller: ShippingWorkbenchController }) {
  const { pendingOrders, availableDevices, eligibleOrderCount, boundDevices, readiness } = controller;
  const { text } = useShippingMessages();
  const metrics: Array<[ShippingMessageKey, number, ShippingMessageKey]> = [
    ['overview.pendingOrders', pendingOrders.length, 'overview.pendingOrdersNote'],
    ['overview.availableDevices', availableDevices.length, 'overview.availableDevicesNote'],
    ['overview.eligibleOrders', eligibleOrderCount, 'overview.eligibleOrdersNote'],
    ['overview.boundRecords', boundDevices.length, 'overview.boundRecordsNote'],
  ];

  return (
    <>
      <header className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="flex items-center gap-2 font-mono text-[10px] font-black tracking-[0.14em] text-[var(--sc-brand)]">
            <span className="h-2 w-2 rounded-full bg-[var(--sc-green)] ring-4 ring-[color-mix(in_srgb,var(--sc-green)_18%,transparent)]" />
            {text('overview.eyebrow')}
          </div>
          <h1 className="mt-2 text-3xl font-black tracking-tight text-[var(--sc-ink)]">
            {text('overview.title')}
          </h1>
          <p className="mt-1.5 max-w-3xl text-sm leading-6 text-[var(--sc-ink-soft)]">
            {text('overview.description')}
          </p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <div className="flex min-h-12 items-center gap-2 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] px-4">
            <span className="h-2 w-2 rounded-full bg-[var(--sc-green)]" />
            <div>
              <strong className="block text-[11px] text-[var(--sc-ink)]">{text('overview.liveData')}</strong>
              <small className="font-mono text-[10px] text-[var(--sc-ink-muted)]">{text('overview.noPrototype')}</small>
            </div>
          </div>
          <div className="flex min-h-12 items-center gap-2 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] px-4 text-xs font-bold text-[var(--sc-ink-soft)]">
            <History className="h-4 w-4 text-[var(--sc-blue)]" />
            {text('overview.history')} <StatusBadge tone="blue">{boundDevices.length}</StatusBadge>
          </div>
        </div>
      </header>

      <section className="grid grid-cols-2 gap-2 lg:grid-cols-4">
        {metrics.map(([label, value, note], index) => (
          <article
            key={label}
            className={`sc-workspace-card relative min-h-24 overflow-hidden rounded-2xl px-4 py-3 before:absolute before:inset-y-0 before:left-0 before:w-1 ${
              index === 0
                ? 'before:bg-[var(--sc-blue)]'
                : index === 1
                  ? 'before:bg-[var(--sc-green)]'
                  : index === 2
                    ? 'before:bg-[var(--sc-amber)]'
                    : 'before:bg-[var(--sc-ink)]'
            }`}
          >
            <span className="text-[11px] font-medium text-[var(--sc-ink-muted)]">{text(label)}</span>
            <strong className="mt-1 block font-mono text-2xl font-black text-[var(--sc-ink)]">{value}</strong>
            <small className="text-[10px] text-[var(--sc-ink-muted)]">{text(note)}</small>
          </article>
        ))}
      </section>

      <nav className="grid overflow-x-auto rounded-2xl border border-[var(--sc-border)] bg-[var(--sc-surface)] sm:grid-cols-4">
        {steps.map((step, index) => {
          const gate = readiness.gates.find((item) => item.id === step.id);
          const ready = gate?.state === 'ready';
          const blocked = gate?.state === 'blocked';
          const Icon = step.icon;
          return (
            <div
              key={step.id}
              className={`sc-panel-header flex min-h-16 min-w-40 items-center gap-3 border-b border-[var(--sc-border)] px-4 last:border-0 sm:min-w-0 sm:border-b-0 sm:border-r ${
                blocked ? 'bg-[var(--sc-red-soft)]' : ''
              }`}
            >
              <span className={`grid h-8 w-8 shrink-0 place-items-center rounded-full border text-[10px] font-black ${
                ready
                  ? 'border-[var(--sc-green)] bg-[var(--sc-green-soft)] text-[var(--sc-green)]'
                  : blocked
                    ? 'border-[var(--sc-red)] bg-[var(--sc-red)] text-[var(--sc-on-accent)]'
                    : 'border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] text-[var(--sc-ink-muted)]'
              }`}>
                {ready ? <Check className="h-4 w-4" /> : <Icon className="h-4 w-4" />}
              </span>
              <div>
                <strong className="block text-xs text-[var(--sc-ink)]">{index + 1}. {text(step.label)}</strong>
                <small className="text-[10px] text-[var(--sc-ink-muted)]">
                  {ready ? text('step.ready') : blocked ? text('step.blocked') : text('step.pending')}
                </small>
              </div>
            </div>
          );
        })}
      </nav>
    </>
  );
}
