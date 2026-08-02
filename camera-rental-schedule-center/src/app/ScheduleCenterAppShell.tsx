import {
  AlertTriangle,
  CalendarDays,
  Cpu,
  LayoutDashboard,
  Layers3,
  RefreshCw,
  Send,
  ShoppingBag,
} from 'lucide-react';
import type { ReactNode } from 'react';
import { useApp } from '../context/AppContext';
import { usePreferences } from '../features/preferences/PreferenceContext';
import { useDeliveryTracking } from '../features/tracking/TrackingContext';
import { EmptyState } from '../shared/ui/EmptyState';
import { StatusBadge } from '../shared/ui/StatusBadge';
import { SyncHealthBanner } from '../shared/ui/SyncHealthBanner';
import { AccountAndPreferenceMenu } from './AccountAndPreferenceMenu';
import { canAccessTab } from './accessModel';
import { IntegrationReadinessBadge } from './IntegrationReadinessBadge';
import { integrationReadiness } from './integrationModel';
import { ResponsiveWorkspaceNavigation } from './ResponsiveWorkspaceNavigation';
import type { WorkspaceNavItem } from './navigation';

export function ScheduleCenterHeader() {
  const {
    activeTab,
    setActiveTab,
    exceptions,
    orders,
    devices,
    syncFromManagementSystem,
    currentUser,
    isLoading,
    hasPermission,
    xianyuConfig,
    xianyuConfigUnavailable,
    permissions,
    logout,
    setIsLoginPageVisible,
  } = useApp();
  const { visibleTrackingSummaries, canReadTracking } = useDeliveryTracking();
  const { t } = usePreferences();
  const trackingRiskCount = canReadTracking
    ? visibleTrackingSummaries.reduce((total, summary) => total + summary.risks.length, 0)
    : 0;

  const allItems: WorkspaceNavItem[] = [
    { id: 'dashboard', label: t('nav.dashboard'), icon: LayoutDashboard },
    { id: 'schedule', label: t('nav.schedule'), icon: CalendarDays, permission: 'rental:schedule:query' },
    {
      id: 'orders',
      label: t('nav.orders'),
      icon: ShoppingBag,
      permission: 'rental:xianyu:query',
      badge: orders.filter((order) => order.status === 'UNASSIGNED').length || undefined,
    },
    {
      id: 'devices',
      label: t('nav.devices'),
      icon: Cpu,
      permission: 'rental:device:query',
      badge: devices.filter((device) => device.status === 'REPAIR' || device.status === 'LOCKED').length || undefined,
    },
    { id: 'binding', label: t('nav.shipping'), icon: Send, permission: 'rental:xianyu:ship' },
    {
      id: 'exceptions',
      label: t('nav.exceptions'),
      icon: AlertTriangle,
      permission: 'rental:review:query',
      badge:
        exceptions.filter((item) => !item.resolved).length + trackingRiskCount || undefined,
      danger: true,
    },
  ];
  const items = allItems.filter((item) => canAccessTab(permissions, item.id));
  const integrationState = integrationReadiness(
    xianyuConfig,
    isLoading,
    xianyuConfigUnavailable
  );

  return (
    <header className="sticky top-0 z-40 border-b border-[var(--sc-border)] bg-[color-mix(in_srgb,var(--sc-surface)_94%,transparent)] backdrop-blur-xl">
      <div className="relative mx-auto flex min-h-[60px] max-w-[1600px] items-center gap-3 px-3 sm:px-5">
        <button
          type="button"
          onClick={() => setActiveTab('dashboard')}
          aria-label={t('nav.dashboard')}
          className="flex min-w-0 shrink-0 items-center gap-2.5 text-left"
        >
          <span className="grid h-9 w-9 shrink-0 place-items-center rounded-md bg-[var(--sc-brand)] text-[var(--sc-surface)]">
            <Layers3 className="h-4.5 w-4.5" />
          </span>
          <span className="hidden min-w-0 min-[430px]:block">
            <strong className="block truncate text-sm font-black tracking-[-0.03em] text-[var(--sc-ink)]">{t('app.title')}</strong>
            <small className="sc-data mt-0.5 hidden text-[8px] tracking-[0.12em] text-[var(--sc-ink-muted)] sm:block">{t('app.subtitle')}</small>
          </span>
        </button>

        <ResponsiveWorkspaceNavigation items={items} activeTab={activeTab} onSelect={setActiveTab} />

        <div className="ml-auto flex shrink-0 items-center gap-2">
          {hasPermission('rental:xianyu:query') && (
            <IntegrationReadinessBadge state={integrationState} />
          )}
          <button
            type="button"
            onClick={() => void syncFromManagementSystem()}
            disabled={isLoading}
            aria-label={t('action.sync')}
            className="grid h-11 w-11 place-items-center rounded-md border border-[var(--sc-border)] bg-[var(--sc-surface)] text-[var(--sc-ink-soft)] disabled:opacity-60"
          >
            <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          <AccountAndPreferenceMenu
            user={currentUser}
            onRelogin={() => setIsLoginPageVisible(true)}
            onLogout={() => void logout()}
          />
        </div>
      </div>
    </header>
  );
}

export function ScheduleCenterAppShell({ children }: { children: ReactNode }) {
  const {
    isLoading,
    loadError,
    authRequired,
    accessDenied,
    syncFromManagementSystem,
    setIsLoginPageVisible,
  } = useApp();
  const { t } = usePreferences();

  return (
    <div className="flex min-h-screen w-full min-w-0 flex-col overflow-x-hidden text-[var(--sc-ink)]">
      <ScheduleCenterHeader />
      <main className="mx-auto w-full min-w-0 max-w-[1600px] flex-1 px-2 py-3 sm:px-4 sm:py-4 lg:px-6">
        <SyncHealthBanner
          isLoading={isLoading}
          error={loadError}
          authRequired={authRequired}
          accessDenied={accessDenied}
          onRetry={() => void syncFromManagementSystem()}
          onLogin={() => setIsLoginPageVisible(true)}
        />
        {accessDenied ? (
          <div className="sc-surface grid min-h-[55vh] place-items-center rounded-xl p-6">
            <EmptyState
              icon={<AlertTriangle className="h-4 w-4" />}
              title={t('sync.permissionTitle')}
              description={t('state.noAccess')}
            />
          </div>
        ) : children}
      </main>
      <footer className="border-t border-[var(--sc-border)] bg-[var(--sc-surface)] px-4 py-4 text-center text-[10px] text-[var(--sc-ink-muted)]">
        {t('app.footer')} · v2.0
      </footer>
    </div>
  );
}
