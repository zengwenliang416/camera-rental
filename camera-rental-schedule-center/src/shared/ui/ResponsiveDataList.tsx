import type { ReactNode } from 'react';

export function ResponsiveDataList({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <section aria-label={label} className="grid gap-3">
      {children}
    </section>
  );
}
