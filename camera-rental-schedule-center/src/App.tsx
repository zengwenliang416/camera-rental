/**
 * Equipment Schedule & Rental Operations Workbench ("设备排期中心")
 */

import React from 'react';
import { AppProvider, useApp } from './context/AppContext';
import { Header } from './components/Header';
import { DashboardView } from './components/DashboardView';
import { GanttScheduleView } from './components/GanttScheduleView';
import { OrdersView } from './components/OrdersView';
import { DevicesView } from './components/DevicesView';
import { ExceptionsView } from './components/ExceptionsView';
import { QuickBindingView } from './components/QuickBindingView';
import { OrderAllocationModal } from './components/OrderAllocationModal';
import { DeviceDetailDrawer } from './components/DeviceDetailDrawer';
import { redirectToAdminLogin } from './api/client';
import { LoginPage } from './components/LoginPage';

function AppContent() {
  const {
    activeTab,
    setActiveTab,
    orders,
    devices,
    isLoading,
    loadError,
    authRequired,
    accessDenied,
    isLoggedIn,
    isLoginPageVisible,
    syncFromManagementSystem,
  } = useApp();

  if (!isLoggedIn) {
    return <LoginPage />;
  }

  const unassignedOrdersCount = orders.filter((o) => o.status === 'UNASSIGNED').length;
  const pendingDispatchCount = orders.filter((o) => o.status === 'PENDING_DISPATCH').length;
  const activeRentalCount = orders.filter((o) => o.status === 'PENDING_RETURN' || o.status === 'RENTING').length;
  const repairDevicesCount = devices.filter((d) => d.status === 'REPAIR' || d.status === 'LOCKED').length;
  const totalDeviceCount = devices.length;
  const rentingDeviceCount = devices.filter((d) => d.status === 'RENTING').length;
  const utilizationPercent =
    totalDeviceCount > 0 ? Math.round((rentingDeviceCount / totalDeviceCount) * 100) : 0;

  return (
    <div className="min-h-screen w-full bg-zinc-50/70 text-zinc-900 font-sans flex flex-col antialiased">
      {/* Top Header Navigation */}
      <Header />

      {/* Main Container */}
      <main className="flex-1 max-w-[1600px] w-full mx-auto p-4 sm:p-6 space-y-6">
        {(isLoading || loadError) && (
          <div
            className={`rounded-2xl border px-4 py-3 text-sm shadow-2xs flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between ${
              loadError
                ? 'border-amber-200 bg-amber-50 text-amber-900'
                : 'border-blue-200 bg-blue-50 text-blue-900'
            }`}
          >
            <div>
              <div className="font-extrabold">
                {isLoading ? '正在同步管理端数据' : accessDenied ? '无权访问设备排期中心' : '管理端数据同步失败'}
              </div>
              <div className="text-xs opacity-80 mt-0.5">
                {isLoading
                  ? '正在读取权限、设备、排期、渠道订单和人工复核数据。'
                  : loadError}
              </div>
            </div>
            {!isLoading && (
              <div className="flex items-center gap-2">
                {authRequired && (
                  <button
                    onClick={redirectToAdminLogin}
                    className="px-3 py-1.5 rounded-lg bg-zinc-900 text-white text-xs font-bold"
                  >
                    去管理后台登录
                  </button>
                )}
                <button
                  onClick={() => void syncFromManagementSystem()}
                  className="px-3 py-1.5 rounded-lg bg-white border border-amber-200 text-xs font-bold"
                >
                  重试同步
                </button>
              </div>
            )}
          </div>
        )}

        {!accessDenied && (
          <>
            {/* Top Metric Cards Header */}
            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
          <div
            onClick={() => setActiveTab('orders')}
            className="p-4 bg-white rounded-2xl border border-zinc-200/80 shadow-2xs hover:border-zinc-300 transition-all cursor-pointer group"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-semibold text-zinc-500">待排期订单</span>
              <span className="w-2 h-2 rounded-full bg-amber-500"></span>
            </div>
            <div className="text-2xl font-extrabold text-zinc-900 tracking-tight group-hover:text-blue-600 transition-colors">
              {unassignedOrdersCount} <span className="text-xs font-normal text-zinc-400">单</span>
            </div>
          </div>

          <div
            onClick={() => setActiveTab('orders')}
            className="p-4 bg-white rounded-2xl border border-zinc-200/80 shadow-2xs hover:border-zinc-300 transition-all cursor-pointer group"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-semibold text-zinc-500">待发货订单</span>
              <span className="w-2 h-2 rounded-full bg-blue-500"></span>
            </div>
            <div className="text-2xl font-extrabold text-blue-600 tracking-tight">
              {pendingDispatchCount} <span className="text-xs font-normal text-zinc-400">单</span>
            </div>
          </div>

          <div
            onClick={() => setActiveTab('orders')}
            className="p-4 bg-white rounded-2xl border border-zinc-200/80 shadow-2xs hover:border-zinc-300 transition-all cursor-pointer group"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-semibold text-zinc-500">履约中订单</span>
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            </div>
            <div className="text-2xl font-extrabold text-emerald-600 tracking-tight">
              {activeRentalCount} <span className="text-xs font-normal text-zinc-400">单</span>
            </div>
          </div>

          <div
            onClick={() => setActiveTab('schedule')}
            className="p-4 bg-white rounded-2xl border border-zinc-200/80 shadow-2xs hover:border-zinc-300 transition-all cursor-pointer group"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-semibold text-zinc-500">设备出租利用率</span>
              <span className="w-2 h-2 rounded-full bg-purple-500"></span>
            </div>
            <div className="text-2xl font-extrabold text-zinc-900 tracking-tight">
              {utilizationPercent}% <span className="text-xs font-normal text-zinc-400">({rentingDeviceCount}/{totalDeviceCount}台出租)</span>
            </div>
          </div>

          <div
            onClick={() => setActiveTab('devices')}
            className="p-4 bg-white rounded-2xl border border-zinc-200/80 shadow-2xs hover:border-zinc-300 transition-all cursor-pointer group"
          >
            <div className="flex items-center justify-between mb-1">
              <span className="text-xs font-semibold text-zinc-500">维修/检修/锁定</span>
              <span className="w-2 h-2 rounded-full bg-rose-500"></span>
            </div>
            <div className="text-2xl font-extrabold text-rose-600 tracking-tight">
              {repairDevicesCount} <span className="text-xs font-normal text-zinc-400">台维保中</span>
            </div>
          </div>
            </div>

            {/* View Main Body */}
            <div className="transition-all duration-200">
              {activeTab === 'dashboard' && <DashboardView />}
              {activeTab === 'schedule' && <GanttScheduleView />}
              {activeTab === 'orders' && <OrdersView />}
              {activeTab === 'devices' && <DevicesView />}
              {activeTab === 'binding' && <QuickBindingView />}
              {activeTab === 'exceptions' && <ExceptionsView />}
            </div>
          </>
        )}
      </main>

      {/* Global Modals & Drawers */}
      {!accessDenied && (
        <>
        <OrderAllocationModal />
        <DeviceDetailDrawer />
        {isLoginPageVisible && <LoginPage isModal />}
        </>
      )}

      {/* Footer */}
      <footer className="mt-auto border-t border-zinc-200/80 bg-white py-4 text-center text-xs text-zinc-400">
        设备排期与租赁运营中心 · 智能算法与独立SN维保履历引擎 v2.0
      </footer>
    </div>
  );
}

export default function App() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  );
}
