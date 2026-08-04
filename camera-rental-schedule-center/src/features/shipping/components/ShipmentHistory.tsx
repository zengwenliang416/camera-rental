import { History, PackageCheck } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
import { Button } from '../../../shared/ui/Button';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { PanelHeader } from './PanelHeader';

export function ShipmentHistory({
  controller,
}: {
  controller: ShippingWorkbenchController;
}) {
  const { boundDevices, openDeviceDetail } = controller;
  const { text } = useShippingMessages();

  return (
    <section className="sc-workspace-card overflow-hidden rounded-2xl">
      <PanelHeader
        eyebrow="MASKED OPERATION HISTORY"
        title={text('history.title')}
        tone="neutral"
        badge={<StatusBadge tone="neutral">{text('history.count', { count: boundDevices.length })}</StatusBadge>}
      />

      {boundDevices.length === 0 ? (
        <EmptyState
          icon={<History className="h-5 w-5 text-[var(--sc-ink-muted)]" />}
          title={text('history.empty')}
          description={text('history.emptyDetail')}
        />
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
                    <StatusBadge tone="blue" icon={<PackageCheck className="h-3 w-3" />}>
                      {device.status}
                    </StatusBadge>
                  </td>
                  <td className="px-4 py-3">
                    <Button
                      type="button"
                      onClick={() => openDeviceDetail(device.id)}
                      variant="outline"
                      size="sm"
                      className="min-h-9 rounded-lg px-3 text-[10px]"
                    >
                      {text('history.viewDevice')}
                    </Button>
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
