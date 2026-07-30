import { useRef } from 'react';
import { Camera, CircleAlert, ScanLine, Truck } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
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
    <section className="overflow-hidden rounded-2xl border border-[var(--sc-border)] bg-[var(--sc-surface)] shadow-sm">
      <PanelHeader
        step="01"
        eyebrow="WAYBILL DESK"
        title={text('waybill.title')}
        badge={<span className="rounded-full bg-[var(--sc-surface-soft)] px-2.5 py-1 text-[10px] text-[var(--sc-ink-muted)]">{text('waybill.draftOnly')}</span>}
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
        <button
          type="button"
          onClick={() => fileInput.current?.click()}
          className="grid min-h-36 w-full place-items-center content-center gap-2 rounded-xl border border-dashed border-blue-300 bg-gradient-to-br from-blue-50 to-[var(--sc-surface)] px-4 text-[var(--sc-ink)] transition hover:border-blue-500 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100"
        >
          <span className="grid h-12 w-12 place-items-center rounded-xl bg-[#12364b] text-white shadow-lg">
            {isScanningWaybill ? <ScanLine className="h-5 w-5 animate-pulse" /> : <Camera className="h-5 w-5" />}
          </span>
          <strong className="text-sm">{isScanningWaybill ? text('waybill.scanning') : text('waybill.upload')}</strong>
          <small className="max-w-sm text-center text-[11px] leading-5 text-[var(--sc-ink-muted)]">
            {text('waybill.uploadDetail')}
          </small>
        </button>

        {waybillDraftStatus && (
          <div className="flex gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-[11px] leading-5 text-amber-900">
            <CircleAlert className="mt-0.5 h-4 w-4 shrink-0" />
            {waybillDraftStatus}
          </div>
        )}

        <div>
          <label className="mb-2 block text-[11px] font-bold text-[var(--sc-ink-muted)]">{text('waybill.carrier')}</label>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
            {expressCompanies.slice(0, 6).map((company) => (
              <button
                key={company.code}
                type="button"
                onClick={() => setExpressCode(company.code)}
                className={`min-h-11 rounded-lg border px-3 text-xs font-bold transition focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100 ${
                  expressCode === company.code
                    ? 'border-blue-600 bg-blue-600 text-white'
                    : 'border-[var(--sc-border)] bg-[var(--sc-surface-soft)] text-[var(--sc-ink-soft)] hover:border-[var(--sc-border-strong)]'
                }`}
              >
                {company.expressAlias || company.expressName}
              </button>
            ))}
          </div>
        </div>

        <label className="block">
          <span className="mb-2 block text-[11px] font-bold text-[var(--sc-ink-muted)]">{text('waybill.number')}</span>
          <span className="relative block">
            <Truck className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <input
              value={waybillNo}
              onChange={(event) => setWaybillNo(event.target.value)}
              placeholder={text('waybill.placeholder')}
              autoComplete="off"
              className="min-h-11 w-full rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] py-2 pl-10 pr-3 font-mono text-xs text-[var(--sc-ink)] outline-none transition focus:border-blue-500 focus:bg-[var(--sc-surface)] focus:ring-4 focus:ring-blue-100"
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
