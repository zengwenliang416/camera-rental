import { AlertTriangle, CheckCircle2, LoaderCircle } from 'lucide-react';

export function OperationResultPanel({
  state,
  message,
}: {
  state: 'pending' | 'success' | 'error';
  message: string;
}) {
  const Icon = state === 'pending' ? LoaderCircle : state === 'success' ? CheckCircle2 : AlertTriangle;
  return (
    <div role={state === 'error' ? 'alert' : 'status'} className="sc-soft-panel flex items-start gap-2 rounded-xl p-3 text-[11px] leading-5 text-[var(--sc-ink-soft)]">
      <Icon className={`mt-0.5 h-4 w-4 shrink-0 ${state === 'pending' ? 'animate-spin text-[var(--sc-blue)]' : state === 'success' ? 'text-[var(--sc-green)]' : 'text-[var(--sc-red)]'}`} />
      {message}
    </div>
  );
}
