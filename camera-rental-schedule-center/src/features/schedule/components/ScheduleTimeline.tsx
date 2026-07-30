import type { ScheduleDay } from '../scheduleModel';

export function ScheduleTimeline({ days }: { days: ScheduleDay[] }) {
  return (
    <>
      {days.map((day) => (
        <th
          key={day.dateStr}
          scope="col"
          className={`min-w-[76px] border-r border-[var(--sc-border)] px-2 py-3 text-center ${
            day.isToday ? 'bg-[var(--sc-blue-soft)] text-[var(--sc-blue)]' : ''
          }`}
        >
          <span className="sc-data block text-[11px] font-black">{day.displayDay}</span>
          <span className="mt-0.5 block text-[9px] font-semibold text-[var(--sc-ink-muted)]">
            {day.weekday}
          </span>
        </th>
      ))}
    </>
  );
}
