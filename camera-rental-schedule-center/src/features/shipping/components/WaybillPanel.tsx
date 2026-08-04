import { useRef } from 'react';
import { Camera, CircleAlert, ScanLine, Truck } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
import { Button } from '../../../shared/ui/Button';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { PanelHeader } from './PanelHeader';

export function WaybillPanel({ controller }: { controller: ShippingWorkbenchController }) {
  const fileInput = useRef<HTMLInputElement>(null);
  const { text } = useShippingMessages();
  const {
    expressCompanies,
    expressCode,
    setExpressCode,
    waybillNo,
    setWaybillNo,
    scanWaybillImage,
    isScanningWaybill,
    waybillDraftStatus,
  } = controller;

  return (
    <section className="sc-workspace-card overflow-hidden rounded-2xl">
      <PanelHeader
        step="01"
        eyebrow="WAYBILL DESK"
        title={text('waybill.title')}
        badge={<StatusBadge tone="neutral">{text('waybill.draftOnly')}</StatusBadge>}
      />
      <div className="space-y-4 p-4">
        <input
          ref={fileInput}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) void scanWaybillImage(file);
            event.target.value = '';
          }}
        />
        <Button
          type="button"
          onClick={() => fileInput.current?.click()}
          variant="glass"
          size="lg"
          className="!grid !gap-2 !whitespace-normal min-h-36 w-full place-items-center content-center rounded-xl border border-dashed border-[color-mix(in_srgb,var(--sc-blue)_34%,var(--sc-border))] bg-[linear-gradient(135deg,var(--sc-blue-soft),var(--sc-surface))] px-4 text-[var(--sc-ink)] text-left hover:border-[var(--sc-blue)]"
        >
          <span className="grid h-12 w-12 place-items-center rounded-xl bg-[var(--sc-brand)] text-[var(--sc-surface)] shadow-lg">
            {isScanningWaybill ? <ScanLine className="h-5 w-5 animate-pulse" /> : <Camera className="h-5 w-5" />}
          </span>
          <strong className="text-sm">{isScanningWaybill ? text('waybill.scanning') : text('waybill.upload')}</strong>
          <small className="max-w-sm text-center text-[11px] leading-5 text-[var(--sc-ink-muted)]">
            {text('waybill.uploadDetail')}
          </small>
        </Button>

        {waybillDraftStatus && (
          <div className="flex gap-2 rounded-xl border border-[color-mix(in_srgb,var(--sc-amber)_28%,var(--sc-border))] bg-[var(--sc-amber-soft)] p-3 text-[11px] leading-5 text-[var(--sc-amber)]">
            <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" />
            {waybillDraftStatus}
          </div>
        )}

        <div>
          <label className="mb-2 block text-[11px] font-bold text-[var(--sc-ink-muted)]">{text('waybill.carrier')}</label>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
            {expressCompanies.slice(0, 6).map((company) => (
              <Button
                key={company.code}
                type="button"
                onClick={() => setExpressCode(company.code)}
                variant={expressCode === company.code ? 'primary' : 'glass'}
                size="sm"
                className="min-h-11 rounded-lg px-3"
              >
                {company.expressAlias || company.expressName}
              </Button>
            ))}
          </div>
        </div>

        <label className="block">
          <span className="mb-2 block text-[11px] font-bold text-[var(--sc-ink-muted)]">{text('waybill.number')}</span>
          <span className="relative block">
            <Truck className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)]" />
            <input
              value={waybillNo}
              onChange={(event) => setWaybillNo(event.target.value)}
              placeholder={text('waybill.placeholder')}
              autoComplete="off"
              className="sc-form-control min-h-11 w-full rounded-lg border py-2 pl-10 pr-3 font-mono text-xs text-[var(--sc-ink)] outline-none focus:border-[var(--sc-blue)] focus:bg-[var(--sc-surface)]"
            />
          </span>
        </label>

        <p className="rounded-lg bg-[var(--sc-surface-soft)] px-3 py-2 text-[10px] leading-5 text-[var(--sc-ink-muted)]">
          {text('waybill.preservation')}
        </p>
      </div>
    </section>
  );
}
