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
    <section className="sc-metric-group grid grid-cols-1" aria-label="Operational metrics">
      {metrics.map((metric) => <OperationalMetricCard key={metric.id} metric={metric} />)}
    </section>
  );
}
