import { AlertTriangle, LoaderCircle, LockKeyhole } from 'lucide-react';
import type { ReactNode } from 'react';

import { EmptyState } from '../../../shared/ui/EmptyState';
import type { OperationsResourceState } from '../operationsState';
import {
  operationsCopy,
  operationsErrorCopy,
} from '../operationsCopy';
import type { LocalePreference } from '../../preferences/preferenceModel';

export function OperationsPanel({
  title,
  description,
  actions,
  children,
  className = '',
}: {
  title: string;
  description: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`sc-surface min-w-0 overflow-hidden rounded-xl ${className}`}>
      <header className="flex flex-col gap-3 border-b border-[var(--sc-border)] px-4 py-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <h2 className="text-sm font-black text-[var(--sc-ink)]">{title}</h2>
          <p className="mt-1 text-[10px] leading-5 text-[var(--sc-ink-muted)]">{description}</p>
        </div>
        {actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}
      </header>
      <div className="min-w-0 p-4">{children}</div>
    </section>
  );
}

export function PanelQueryBoundary<T>({
  allowed,
  state,
  locale,
  onRetry,
  isEmpty,
  emptyTitle,
  emptyDetail,
  children,
}: {
  allowed: boolean;
  state: OperationsResourceState<T>;
  locale: LocalePreference;
  onRetry: () => void;
  isEmpty?: boolean;
  emptyTitle?: string;
  emptyDetail?: string;
  children: ReactNode;
}) {
  if (!allowed) {
    return (
      <EmptyState
        icon={<LockKeyhole className="h-4 w-4" />}
        title={operationsCopy(locale, 'common.permissionTitle')}
        description={operationsCopy(locale, 'common.permissionDetail')}
      />
    );
  }
  if (state.status === 'loading' && state.data === null) {
    return (
      <div className="grid min-h-32 place-items-center text-center">
        <LoaderCircle className="h-5 w-5 animate-spin text-[var(--sc-blue)]" />
        <p className="mt-2 text-[11px] font-bold text-[var(--sc-ink-soft)]">
          {operationsCopy(locale, 'common.loading')}
        </p>
      </div>
    );
  }
  if (state.status === 'error' && state.data === null) {
    return (
      <div role="alert" className="rounded-lg border border-[var(--sc-red)] bg-[var(--sc-red-soft)] p-4">
        <div className="flex items-start gap-2">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-[var(--sc-red)]" />
          <div>
            <p className="text-xs font-black text-[var(--sc-ink)]">
              {operationsCopy(locale, 'common.errorTitle')}
            </p>
            <p className="mt-1 text-[11px] leading-5 text-[var(--sc-ink-soft)]">
              {operationsErrorCopy(locale, state.error)}
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 min-h-10 rounded-lg border border-[var(--sc-border-strong)] px-3 text-xs font-bold"
        >
          {operationsCopy(locale, 'common.retry')}
        </button>
      </div>
    );
  }
  if (isEmpty) {
    return (
      <EmptyState
        icon={<AlertTriangle className="h-4 w-4" />}
        title={emptyTitle || operationsCopy(locale, 'common.errorTitle')}
        description={emptyDetail}
      />
    );
  }
  return (
    <>
      {state.status === 'error' && state.data !== null && (
        <div role="alert" className="mb-3 rounded-lg border border-[var(--sc-amber)] bg-[var(--sc-amber-soft)] px-3 py-2 text-[11px] text-[var(--sc-ink-soft)]">
          {operationsErrorCopy(locale, state.error)}
        </div>
      )}
      {children}
    </>
  );
}

export const fieldClassName =
  'min-h-11 w-full rounded-lg border border-[var(--sc-border)] bg-[var(--sc-surface-soft)] px-3 text-xs text-[var(--sc-ink)] placeholder:text-[var(--sc-ink-muted)] disabled:cursor-not-allowed disabled:opacity-55';

export const secondaryButtonClassName =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg border border-[var(--sc-border-strong)] px-3 text-xs font-bold text-[var(--sc-ink)] disabled:cursor-not-allowed disabled:opacity-45';

export const primaryButtonClassName =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg bg-[var(--sc-ink)] px-4 text-xs font-bold text-[var(--sc-surface)] disabled:cursor-not-allowed disabled:opacity-45';
