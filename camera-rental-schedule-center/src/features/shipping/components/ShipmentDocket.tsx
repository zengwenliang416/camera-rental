import { AlertTriangle, Check, LoaderCircle, Send, ShieldCheck, X } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
import { Button } from '../../../shared/ui/Button';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { PanelHeader } from './PanelHeader';

const gateTone = {
  ready: 'border-[color-mix(in_srgb,var(--sc-green)_28%,var(--sc-border))] bg-[var(--sc-green-soft)] text-[var(--sc-green)]',
  blocked: 'border-[color-mix(in_srgb,var(--sc-red)_28%,var(--sc-border))] bg-[var(--sc-red-soft)] text-[var(--sc-red)]',
  warning: 'border-[color-mix(in_srgb,var(--sc-amber)_28%,var(--sc-border))] bg-[var(--sc-amber-soft)] text-[var(--sc-amber)]',
  pending: 'border-[var(--sc-border)] bg-[var(--sc-surface-soft)] text-[var(--sc-ink-soft)]',
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
    <section className="sc-workspace-card overflow-hidden rounded-2xl">
      <PanelHeader
        step="04"
        eyebrow="SERVER-AUTHORITATIVE COMMAND"
        title={text('docket.title')}
        tone={readiness.canSubmit ? 'blue' : 'red'}
        badge={(
          <StatusBadge tone={readiness.canSubmit ? 'green' : 'red'}>
            {readiness.canSubmit ? text('docket.canSubmit') : text('docket.blocked')}
          </StatusBadge>
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
          <div className="flex gap-2 rounded-xl border border-[color-mix(in_srgb,var(--sc-red)_28%,var(--sc-border))] bg-[var(--sc-red-soft)] p-3 text-[11px] leading-5 text-[var(--sc-red)]">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
            <div>
              <strong className="block">{text('docket.cannotShip')}</strong>
              <span>{readiness.primaryBlockReason}</span>
            </div>
          </div>
        )}

        {notice && (
          <div className="flex items-start justify-between gap-3 rounded-xl border border-[color-mix(in_srgb,var(--sc-blue)_28%,var(--sc-border))] bg-[var(--sc-blue-soft)] p-3 text-[11px] leading-5 text-[var(--sc-blue)]">
            <span>{notice}</span>
            <Button
              type="button"
              onClick={() => setNotice(null)}
              aria-label={text('docket.closeNotice')}
              variant="ghost"
              size="icon"
              className="h-7 w-7 shrink-0 rounded-md text-[var(--sc-blue)] hover:bg-[var(--sc-blue-soft)]"
              icon={<X className="h-3.5 w-3.5" />}
            />
          </div>
        )}

        <Button
          type="button"
          onClick={() => void submitShipment()}
          disabled={!readiness.canSubmit || isSubmitting}
          variant={readiness.canSubmit ? 'primary' : 'secondary'}
          size="lg"
          className="w-full rounded-xl px-4 text-sm font-black"
          icon={isSubmitting
            ? <LoaderCircle className="h-4 w-4 animate-spin" />
            : readiness.canSubmit
              ? <Send className="h-4 w-4" />
              : <ShieldCheck className="h-4 w-4" />}
        >
          {isSubmitting ? text('docket.submitting') : text('docket.submit')}
        </Button>

        <p className="text-center text-[10px] leading-5 text-[var(--sc-ink-muted)]">
          {text('docket.serverAuthority')}
        </p>
      </div>
    </section>
  );
}
