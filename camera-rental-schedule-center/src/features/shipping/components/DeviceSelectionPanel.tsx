import { useRef } from 'react';
import { Camera, Check, Cpu, MapPin, QrCode, Search, ScanLine } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
import { Button } from '../../../shared/ui/Button';
import { EmptyState } from '../../../shared/ui/EmptyState';
import { StatusBadge } from '../../../shared/ui/StatusBadge';
import { PanelHeader } from './PanelHeader';

export function DeviceSelectionPanel({
  controller,
}: {
  controller: ShippingWorkbenchController;
}) {
  const fileInput = useRef<HTMLInputElement>(null);
  const { text } = useShippingMessages();
  const {
    availableDevices,
    filteredDevices,
    selectedDevice,
    selectedDeviceId,
    setSelectedDeviceId,
    deviceSearch,
    setDeviceSearch,
    scanDeviceImage,
    isScanningDevice,
    deviceScanStatus,
    openDeviceDetail,
  } = controller;

  return (
    <section className="sc-workspace-card overflow-hidden rounded-2xl">
      <PanelHeader
        step="02"
        eyebrow="ASSET RESOLUTION"
        title={text('device.title')}
        badge={<StatusBadge tone="green">{text('device.available', { count: availableDevices.length })}</StatusBadge>}
      />
      <div className="space-y-4 p-4">
        <input
          ref={fileInput}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={(event) => {
            const file = event.target.files?.[0];
            if (file) void scanDeviceImage(file);
            event.target.value = '';
          }}
        />

        <Button
          type="button"
          onClick={() => fileInput.current?.click()}
          variant="glass"
          size="sm"
          className="!flex !justify-between !gap-3 !whitespace-normal min-h-12 w-full items-center rounded-xl border border-dashed border-[var(--sc-border-strong)] px-3 text-left"
        >
          <span className="flex min-w-0 items-center gap-3">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-[var(--sc-brand)] text-[var(--sc-surface)]">
              {isScanningDevice
                ? <ScanLine className="h-4 w-4 animate-pulse" />
                : <Camera className="h-4 w-4" />}
            </span>
            <span className="min-w-0">
              <strong className="block truncate text-xs text-[var(--sc-ink)]">
                {isScanningDevice ? text('device.scanning') : text('device.upload')}
              </strong>
              <small className="text-[10px] text-[var(--sc-ink-muted)]">{text('device.manualFallback')}</small>
            </span>
          </span>
          <QrCode className="h-4 w-4 shrink-0 text-[var(--sc-ink-muted)]" />
        </Button>

        {deviceScanStatus && (
          <p className="rounded-lg border border-[color-mix(in_srgb,var(--sc-blue)_24%,var(--sc-border))] bg-[var(--sc-blue-soft)] px-3 py-2 text-[11px] leading-5 text-[var(--sc-blue)]">
            {deviceScanStatus}
          </p>
        )}

        <label className="block">
          <span className="mb-2 block text-[11px] font-bold text-[var(--sc-ink-muted)]">
            {text('device.search')}
          </span>
          <span className="relative block">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)]" />
            <input
              value={deviceSearch}
              onChange={(event) => setDeviceSearch(event.target.value)}
              placeholder={text('device.placeholder')}
              autoComplete="off"
              className="sc-form-control min-h-11 w-full rounded-lg border py-2 pl-10 pr-3 text-xs text-[var(--sc-ink)] outline-none focus:border-[var(--sc-blue)] focus:bg-[var(--sc-surface)]"
            />
          </span>
        </label>

        <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
          {filteredDevices.length === 0 ? (
            <EmptyState
              icon={<Cpu className="h-5 w-5 text-[var(--sc-ink-muted)]" />}
              title={text('device.noMatches')}
              description={text('device.noMatchesDetail')}
            />
          ) : (
            filteredDevices.map((device) => {
              const selected = selectedDeviceId === device.id;
              return (
                <Button
                  key={device.id}
                  type="button"
                  onClick={() => setSelectedDeviceId(device.id)}
                  variant="glass"
                  size="sm"
                  className={`!flex !justify-start !gap-3 !whitespace-normal min-h-16 w-full items-center rounded-xl border p-3 text-left ${
                    selected
                      ? '!border-[color-mix(in_srgb,var(--sc-blue)_42%,var(--sc-border))] !bg-[var(--sc-blue-soft)]'
                      : '!border-[var(--sc-border)] !bg-[var(--sc-surface)] hover:!border-[var(--sc-border-strong)] hover:!bg-[var(--sc-surface-soft)]'
                  }`}
                >
                  <span className={`grid h-9 w-9 shrink-0 place-items-center rounded-lg ${
                    selected ? 'bg-[var(--sc-blue)] text-[var(--sc-on-accent)]' : 'bg-[var(--sc-surface-soft)] text-[var(--sc-ink-soft)]'
                  }`}>
                    {selected ? <Check className="h-4 w-4" /> : <Cpu className="h-4 w-4" />}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="flex items-center justify-between gap-2">
                      <strong className="truncate text-xs text-[var(--sc-ink)]">{device.unitCode}</strong>
                      <span className="shrink-0 text-[10px] font-bold text-[var(--sc-green)]">AVAILABLE</span>
                    </span>
                    <span className="mt-1 block truncate font-mono text-[10px] text-[var(--sc-ink-muted)]">
                      SN {device.sn}
                    </span>
                    <span className="mt-1 flex items-center gap-1 truncate text-[10px] text-[var(--sc-ink-muted)]">
                      <MapPin className="h-3 w-3 shrink-0" />
                      {device.modelName}{device.note ? ` · ${device.note}` : ''}
                    </span>
                  </span>
                </Button>
              );
            })
          )}
        </div>

        {selectedDevice && (
          <Button
            type="button"
            onClick={() => openDeviceDetail(selectedDevice.id)}
            variant="outline"
            size="sm"
            className="min-h-11 w-full rounded-lg px-3 text-xs"
          >
            {text('device.viewSelected')}
          </Button>
        )}
      </div>
    </section>
  );
}
