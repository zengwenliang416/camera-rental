import { History, PackageCheck } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
import { PanelHeader } from './PanelHeader';

export function ShipmentHistory({
  controller,
}: {
  controller: ShippingWorkbenchController;
}) {
  const { boundDevices, openDeviceDetail } = controller;
  const { text } = useShippingMessages();

  return (
    <section className="overflow-hidden rounded-2xl border border-[var(--sc-border)] bg-[var(--sc-surface)] shadow-sm">
      <PanelHeader
        eyebrow="MASKED OPERATION HISTORY"
        title={text('history.title')}
        tone="neutral"
        badge={(
          <span className="rounded-full bg-zinc-100 px-2.5 py-1 text-[10px] font-bold text-zinc-600">
            {text('history.count', { count: boundDevices.length })}
          </span>
        )}
      />

      {boundDevices.length === 0 ? (
        <div className="grid min-h-32 place-items-center px-4 py-8 text-center">
          <div>
            <History className="mx-auto h-5 w-5 text-[var(--sc-ink-muted)]" />
            <strong className="mt-2 block text-xs text-[var(--sc-ink-soft)]">{text('history.empty')}</strong>
            <small className="mt-1 block text-[10px] text-[var(--sc-ink-muted)]">
              {text('history.emptyDetail')}
            </small>
          </div>
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] border-collapse text-left">
            <thead>
              <tr className="border-b border-[var(--sc-border)] bg-[var(--sc-surface-soft)] text-[10px] text-[var(--sc-ink-muted)]">
                <th className="px-4 py-3 font-bold">{text('history.device')}</th>
                <th className="px-4 py-3 font-bold">{text('history.sn')}</th>
                <th className="px-4 py-3 font-bold">{text('history.order')}</th>
                <th className="px-4 py-3 font-bold">{text('history.waybill')}</th>
                <th className="px-4 py-3 font-bold">{text('history.status')}</th>
                <th className="px-4 py-3 font-bold">{text('history.action')}</th>
              </tr>
            </thead>
            <tbody>
              {boundDevices.map((device) => (
                <tr key={device.id} className="border-b border-[var(--sc-border)] text-[11px] last:border-0">
                  <td className="px-4 py-3 font-bold text-[var(--sc-ink)]">
                    {device.unitCode}
                    <span className="mt-0.5 block text-[10px] font-normal text-[var(--sc-ink-muted)]">
                      {device.modelName}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono text-[var(--sc-ink-soft)]">{device.sn}</td>
                  <td className="px-4 py-3 font-mono text-[var(--sc-ink-soft)]">
                    {device.currentOrderId || '-'}
                  </td>
                  <td className="px-4 py-3 font-mono text-[var(--sc-ink-soft)]">
                    {device.logisticsNumber || '-'}
                  </td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2 py-1 text-[9px] font-bold text-blue-700">
                      <PackageCheck className="h-3 w-3" />
                      {device.status}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => openDeviceDetail(device.id)}
                      className="min-h-9 rounded-lg border border-[var(--sc-border)] px-3 text-[10px] font-bold text-[var(--sc-ink-soft)] hover:bg-[var(--sc-surface-soft)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100"
                    >
                      {text('history.viewDevice')}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
