import { Grid3X3, List, Search } from 'lucide-react';

import type { EquipmentModel } from '../../../types';
import type { ScheduleStatusFilter, ScheduleViewMode } from '../scheduleModel';

export function ScheduleFilters({
  models,
  selectedModelId,
  search,
  status,
  viewMode,
  labels,
  onModelChange,
  onSearchChange,
  onStatusChange,
  onViewModeChange,
}: {
  models: EquipmentModel[];
  selectedModelId: string;
  search: string;
  status: ScheduleStatusFilter;
  viewMode: ScheduleViewMode;
  labels: Record<
    | 'model'
    | 'search'
    | 'statusAll'
    | 'statusIdle'
    | 'statusRenting'
    | 'statusReserved'
    | 'statusRepair'
    | 'statusLocked'
    | 'viewGantt'
    | 'viewTable',
    string
  >;
  onModelChange: (value: string) => void;
  onSearchChange: (value: string) => void;
  onStatusChange: (value: ScheduleStatusFilter) => void;
  onViewModeChange: (value: ScheduleViewMode) => void;
}) {
  return (
    <section className="grid gap-3 rounded-xl border border-[var(--sc-border)] bg-[var(--sc-surface)] p-3 lg:grid-cols-[minmax(180px,0.8fr)_minmax(240px,1.4fr)_minmax(160px,0.7fr)_auto]">
      <label className="grid gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
        {labels.model}
        <select
          value={selectedModelId}
          onChange={(event) => onModelChange(event.target.value)}
          className="min-h-11 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold text-[var(--sc-ink)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
        >
          {models.map((model) => (
            <option key={model.id} value={model.id}>
              {model.name} · {model.totalUnits}
            </option>
          ))}
        </select>
      </label>

      <label className="grid gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
        {labels.search}
        <span className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--sc-ink-muted)]" />
          <input
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder={labels.search}
            className="min-h-11 w-full rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] pl-10 pr-3 text-xs font-semibold text-[var(--sc-ink)] placeholder:text-[var(--sc-ink-muted)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
          />
        </span>
      </label>

      <label className="grid gap-1 text-[10px] font-bold uppercase tracking-[0.1em] text-[var(--sc-ink-muted)]">
        {labels.statusAll}
        <select
          value={status}
          onChange={(event) => onStatusChange(event.target.value as ScheduleStatusFilter)}
          className="min-h-11 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] px-3 text-xs font-bold text-[var(--sc-ink)] focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)]"
        >
          <option value="ALL">{labels.statusAll}</option>
          <option value="IDLE">{labels.statusIdle}</option>
          <option value="RENTING">{labels.statusRenting}</option>
          <option value="RESERVED">{labels.statusReserved}</option>
          <option value="REPAIR">{labels.statusRepair}</option>
          <option value="LOCKED">{labels.statusLocked}</option>
        </select>
      </label>

      <div className="flex items-end">
        <div className="grid min-h-11 w-full grid-cols-2 rounded-lg border border-[var(--sc-border-strong)] bg-[var(--sc-surface-soft)] p-1 lg:w-auto">
          {[
            { id: 'gantt' as const, label: labels.viewGantt, icon: Grid3X3 },
            { id: 'table' as const, label: labels.viewTable, icon: List },
          ].map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onViewModeChange(item.id)}
                aria-pressed={viewMode === item.id}
                className={`inline-flex min-h-11 items-center justify-center gap-1.5 rounded-md px-3 text-[11px] font-bold focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-[var(--sc-focus)] ${
                  viewMode === item.id
                    ? 'bg-[var(--sc-ink)] text-[var(--sc-surface)]'
                    : 'text-[var(--sc-ink-soft)]'
                }`}
              >
                <Icon className="h-3.5 w-3.5" />
                {item.label}
              </button>
            );
          })}
        </div>
      </div>
    </section>
  );
}
