import type { ReactNode } from 'react';
import type { StatusTone } from './StatusBadge';
import { OperationalMetricCard } from './OperationalMetricCard';

export interface OperationalMetric {
  id: string;
  label: string;
  value: number | string;
  unit: string;
  detail?: string;
  icon: ReactNode;
  tone: StatusTone;
  onSelect?: () => void;
}

export function OperationalMetricGrid({ metrics }: { metrics: OperationalMetric[] }) {
  return (
    <section className="grid grid-cols-2 gap-2 lg:grid-cols-4 2xl:grid-cols-6" aria-label="Operational metrics">
      {metrics.map((metric) => <OperationalMetricCard key={metric.id} metric={metric} />)}
    </section>
  );
}
