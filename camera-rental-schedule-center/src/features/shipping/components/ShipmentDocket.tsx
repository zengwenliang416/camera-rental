import { AlertTriangle, Check, LoaderCircle, Send, ShieldCheck, X } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
import { PanelHeader } from './PanelHeader';

const gateTone = {
  ready: 'border-emerald-200 bg-emerald-50 text-emerald-900',
  blocked: 'border-rose-200 bg-rose-50 text-rose-900',
  warning: 'border-amber-200 bg-amber-50 text-amber-900',
  pending: 'border-zinc-200 bg-zinc-50 text-zinc-600',
};

export function ShipmentDocket({
  controller,
}: {
  controller: ShippingWorkbenchController;
}) {
  const { text } = useShippingMessages();
  const {
    selectedDevice,
    selectedOrder,
    selectedExpressCompany,
    waybillNo,
    readiness,
    isSubmitting,
    submitShipment,
    notice,
    setNotice,
  } = controller;

  return (
    <section className="overflow-hidden rounded-2xl border border-[var(--sc-border)] bg-[var(--sc-surface)] shadow-sm">
      <PanelHeader
        step="04"
        eyebrow="SERVER-AUTHORITATIVE COMMAND"
        title={text('docket.title')}
        tone={readiness.canSubmit ? 'blue' : 'red'}
        badge={(
          <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${
            readiness.canSubmit
              ? 'bg-emerald-50 text-emerald-700'
              : 'bg-rose-50 text-rose-700'
          }`}>
            {readiness.canSubmit ? text('docket.canSubmit') : text('docket.blocked')}
          </span>
        )}
      />
      <div className="space-y-4 p-4">
        <div className="grid gap-2 sm:grid-cols-2">
          {readiness.gates.map((gate) => (
            <div key={gate.id} className={`rounded-xl border p-3 ${gateTone[gate.state]}`}>
              <span className="flex items-center gap-2 text-[10px] font-bold">
                {gate.state === 'ready'
                  ? <Check className="h-3.5 w-3.5" />
                  : <AlertTriangle className="h-3.5 w-3.5" />}
                {gate.label}
              </span>
              <strong className="mt-1 block truncate text-[11px]">{gate.value}</strong>
            </div>
          ))}
        </div>

        <div className="overflow-hidden rounded-xl border border-[var(--sc-border)]">
          {[
            [
              text('docket.carrierWaybill'),
              waybillNo.trim()
                ? `${selectedExpressCompany.expressName} · ${waybillNo.trim()}`
                : text('docket.notEntered'),
            ],
            [
              text('docket.device'),
              selectedDevice
                ? `${selectedDevice.unitCode} · ${selectedDevice.sn}`
                : text('docket.notSelected'),
            ],
            [text('docket.order'), selectedOrder?.orderNumber || text('docket.notSelected')],
            [text('docket.billable'), selectedOrder?.rentalPeriodLabel || text('docket.waitingOrder')],
            [
              text('docket.occupied'),
              selectedOrder?.occupyStartDate && selectedOrder.occupyEndDateExclusive
                ? text('docket.halfOpenRange', {
                  start: selectedOrder.occupyStartDate,
                  end: selectedOrder.occupyEndDateExclusive,
                })
                : text('docket.occupiedMissing'),
            ],
          ].map(([label, value]) => (
            <div
              key={label}
              className="grid gap-1 border-b border-[var(--sc-border)] px-3 py-2.5 last:border-0 sm:grid-cols-[8rem_1fr]"
            >
              <span className="text-[10px] font-bold text-[var(--sc-ink-muted)]">{label}</span>
              <strong className="break-all font-mono text-[10px] text-[var(--sc-ink-soft)]">{value}</strong>
            </div>
          ))}
        </div>

        {readiness.primaryBlockReason && (
          <div className="flex gap-2 rounded-xl border border-rose-200 bg-rose-50 p-3 text-[11px] leading-5 text-rose-900">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            <div>
              <strong className="block">{text('docket.cannotShip')}</strong>
              <span>{readiness.primaryBlockReason}</span>
            </div>
          </div>
        )}

        {notice && (
          <div className="flex items-start justify-between gap-3 rounded-xl border border-blue-200 bg-blue-50 p-3 text-[11px] leading-5 text-blue-900">
            <span>{notice}</span>
            <button
              type="button"
              onClick={() => setNotice(null)}
              aria-label={text('docket.closeNotice')}
              className="grid h-7 w-7 shrink-0 place-items-center rounded-md hover:bg-blue-100 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          </div>
        )}

        <button
          type="button"
          onClick={() => void submitShipment()}
          disabled={!readiness.canSubmit || isSubmitting}
          className="flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-zinc-950 px-4 text-sm font-black text-white transition hover:bg-blue-700 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-200 disabled:cursor-not-allowed disabled:bg-zinc-200 disabled:text-zinc-500"
        >
          {isSubmitting
            ? <LoaderCircle className="h-4 w-4 animate-spin" />
            : readiness.canSubmit
              ? <Send className="h-4 w-4" />
              : <ShieldCheck className="h-4 w-4" />}
          {isSubmitting ? text('docket.submitting') : text('docket.submit')}
        </button>

        <p className="text-center text-[10px] leading-5 text-zinc-400">
          {text('docket.serverAuthority')}
        </p>
      </div>
    </section>
  );
}
