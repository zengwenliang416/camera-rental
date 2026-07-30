import React, { lazy, Suspense } from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { DashboardView } from './components/DashboardView';
import { LoginPage } from './components/LoginPage';
import { ScheduleCenterAppShell } from './app/ScheduleCenterAppShell';
import { PreferenceProvider } from './features/preferences/PreferenceContext';
import { RouteLoadingState } from './shared/ui/RouteLoadingState';

const QuickBindingView = lazy(() => import('./components/QuickBindingView'));
const GanttScheduleView = lazy(() =>
  import('./components/GanttScheduleView').then((module) => ({ default: module.GanttScheduleView }))
);
const OrdersView = lazy(() =>
  import('./components/OrdersView').then((module) => ({ default: module.OrdersView }))
);
const DevicesView = lazy(() =>
  import('./components/DevicesView').then((module) => ({ default: module.DevicesView }))
);
const ExceptionsView = lazy(() =>
  import('./components/ExceptionsView').then((module) => ({ default: module.ExceptionsView }))
);
const OrderAllocationModal = lazy(() =>
  import('./components/OrderAllocationModal').then((module) => ({ default: module.OrderAllocationModal }))
);
const DeviceDetailDrawer = lazy(() =>
  import('./components/DeviceDetailDrawer').then((module) => ({ default: module.DeviceDetailDrawer }))
);
const QuickBindingModal = lazy(() =>
  import('./components/QuickBindingModal').then((module) => ({ default: module.QuickBindingModal }))
);

function AppContent() {
  const {
    activeTab,
    accessDenied,
    isLoggedIn,
    isLoginPageVisible,
    isQuickBindingOpen,
  } = useApp();

  if (!isLoggedIn) {
    return <LoginPage />;
  }

  return (
    <ScheduleCenterAppShell>
      {!accessDenied && (
        <Suspense fallback={<RouteLoadingState />}>
          {activeTab === 'dashboard' && <DashboardView />}
          {activeTab === 'schedule' && <GanttScheduleView />}
          {activeTab === 'orders' && <OrdersView />}
          {activeTab === 'devices' && <DevicesView />}
          {activeTab === 'binding' && !isQuickBindingOpen && <QuickBindingView />}
          {activeTab === 'exceptions' && <ExceptionsView />}
        </Suspense>
      )}
      {!accessDenied && (
        <Suspense fallback={null}>
          <OrderAllocationModal />
          <DeviceDetailDrawer />
          <QuickBindingModal />
        </Suspense>
      )}
      {isLoginPageVisible && <LoginPage isModal />}
    </ScheduleCenterAppShell>
  );
}

export default function App() {
  return (
    <PreferenceProvider>
      <AppProvider>
        <AppContent />
      </AppProvider>
    </PreferenceProvider>
  );
}
