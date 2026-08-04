import { useShippingWorkbench } from '../useShippingWorkbench';
import { DeviceSelectionPanel } from './DeviceSelectionPanel';
import { OrderSelectionPanel } from './OrderSelectionPanel';
import { ShipmentDocket } from './ShipmentDocket';
import { ShipmentHistory } from './ShipmentHistory';
import { ShippingOverview } from './ShippingOverview';
import { WaybillPanel } from './WaybillPanel';

export function ShippingWorkbench({ embedded = false }: { embedded?: boolean }) {
  const controller = useShippingWorkbench();

  return (
    <section
      className={`relative isolate overflow-hidden ${
        embedded
          ? 'sc-soft-panel rounded-2xl p-3 sm:p-5'
          : 'sc-workspace-card rounded-[1.75rem] p-3 sm:p-5 lg:p-6'
      }`}
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-64 bg-[radial-gradient(circle_at_top_left,color-mix(in_srgb,var(--sc-blue)_12%,transparent),transparent_48%),linear-gradient(to_bottom,var(--sc-surface),transparent)]"
      />
      <div className="mx-auto max-w-[1480px] space-y-4">
        {!embedded && <ShippingOverview controller={controller} />}

        <div className="grid items-start gap-4 xl:grid-cols-[minmax(18rem,0.8fr)_minmax(0,1.4fr)]">
          <div className="space-y-4">
            <WaybillPanel controller={controller} />
            <DeviceSelectionPanel controller={controller} />
          </div>
          <div className="space-y-4">
            <OrderSelectionPanel controller={controller} />
            <ShipmentDocket controller={controller} />
          </div>
        </div>

        <ShipmentHistory controller={controller} />
      </div>
    </section>
  );
}
