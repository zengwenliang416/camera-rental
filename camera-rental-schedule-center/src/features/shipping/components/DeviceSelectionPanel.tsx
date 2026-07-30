import { useRef } from 'react';
import { Camera, Check, Cpu, MapPin, QrCode, Search, ScanLine } from 'lucide-react';

import type { ShippingWorkbenchController } from '../useShippingWorkbench';
import { useShippingMessages } from '../shippingMessages';
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
    <section className="overflow-hidden rounded-2xl border border-[var(--sc-border)] bg-[var(--sc-surface)] shadow-sm">
      <PanelHeader
        step="02"
        eyebrow="ASSET RESOLUTION"
        title={text('device.title')}
        badge={(
          <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-[10px] font-bold text-emerald-700">
            {text('device.available', { count: availableDevices.length })}
          </span>
        )}
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

        <button
          type="button"
          onClick={() => fileInput.current?.click()}
          className="flex min-h-12 w-full items-center justify-between gap-3 rounded-xl border border-dashed border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-left transition hover:border-blue-400 hover:bg-blue-50 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100"
        >
          <span className="flex min-w-0 items-center gap-3">
            <span className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-zinc-900 text-white">
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
          <QrCode className="h-4 w-4 shrink-0 text-zinc-400" />
        </button>

        {deviceScanStatus && (
          <p className="rounded-lg bg-blue-50 px-3 py-2 text-[11px] leading-5 text-blue-900">
            {deviceScanStatus}
          </p>
        )}

        <label className="block">
          <span className="mb-2 block text-[11px] font-bold text-[var(--sc-ink-muted)]">
            {text('device.search')}
          </span>
          <span className="relative block">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-zinc-400" />
            <input
              value={deviceSearch}
              onChange={(event) => setDeviceSearch(event.target.value)}
              placeholder={text('device.placeholder')}
              autoComplete="off"
              className="min-h-11 w-full rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] py-2 pl-10 pr-3 text-xs text-[var(--sc-ink)] outline-none transition focus:border-blue-500 focus:bg-[var(--sc-surface)] focus:ring-4 focus:ring-blue-100"
            />
          </span>
        </label>

        <div className="max-h-72 space-y-2 overflow-y-auto pr-1">
          {filteredDevices.length === 0 ? (
            <div className="rounded-xl border border-dashed border-[var(--sc-border-strong)] px-4 py-8 text-center">
              <Cpu className="mx-auto h-5 w-5 text-[var(--sc-ink-muted)]" />
              <strong className="mt-2 block text-xs text-[var(--sc-ink-soft)]">{text('device.noMatches')}</strong>
              <small className="mt-1 block text-[10px] leading-5 text-[var(--sc-ink-muted)]">
                {text('device.noMatchesDetail')}
              </small>
            </div>
          ) : (
            filteredDevices.map((device) => {
              const selected = selectedDeviceId === device.id;
              return (
                <button
                  key={device.id}
                  type="button"
                  onClick={() => setSelectedDeviceId(device.id)}
                  className={`flex min-h-16 w-full items-center gap-3 rounded-xl border p-3 text-left transition focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100 ${
                    selected
                      ? 'border-blue-600 bg-blue-50'
                      : 'border-[var(--sc-border)] bg-[var(--sc-surface)] hover:border-[var(--sc-border-strong)] hover:bg-[var(--sc-surface-soft)]'
                  }`}
                >
                  <span className={`grid h-9 w-9 shrink-0 place-items-center rounded-lg ${
                    selected ? 'bg-blue-600 text-white' : 'bg-[var(--sc-surface-soft)] text-[var(--sc-ink-soft)]'
                  }`}>
                    {selected ? <Check className="h-4 w-4" /> : <Cpu className="h-4 w-4" />}
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="flex items-center justify-between gap-2">
                      <strong className="truncate text-xs text-[var(--sc-ink)]">{device.unitCode}</strong>
                      <span className="shrink-0 text-[10px] font-bold text-emerald-700">AVAILABLE</span>
                    </span>
                    <span className="mt-1 block truncate font-mono text-[10px] text-[var(--sc-ink-muted)]">
                      SN {device.sn}
                    </span>
                    <span className="mt-1 flex items-center gap-1 truncate text-[10px] text-[var(--sc-ink-muted)]">
                      <MapPin className="h-3 w-3 shrink-0" />
                      {device.modelName}{device.note ? ` · ${device.note}` : ''}
                    </span>
                  </span>
                </button>
              );
            })
          )}
        </div>

        {selectedDevice && (
          <button
            type="button"
            onClick={() => openDeviceDetail(selectedDevice.id)}
            className="min-h-11 w-full rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface)] px-3 text-xs font-bold text-[var(--sc-ink-soft)] transition hover:border-[var(--sc-border-strong)] hover:bg-[var(--sc-surface-soft)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-blue-100"
          >
            {text('device.viewSelected')}
          </button>
        )}
      </div>
    </section>
  );
}
