import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  LayoutDashboard,
  CalendarDays,
  ShoppingBag,
  Cpu,
  AlertTriangle,
  RefreshCw,
  Layers,
  QrCode,
  ChevronDown,
  LogOut,
  User,
} from 'lucide-react';

interface NavItem {
  id: 'dashboard' | 'schedule' | 'orders' | 'devices' | 'exceptions' | 'binding';
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  permission?: string | string[];
  badge?: number;
  badgeColor?: string;
}

export const Header: React.FC = () => {
  const {
    activeTab,
    setActiveTab,
    exceptions,
    orders,
    devices,
    syncFromManagementSystem,
    lastSyncTime,
    currentUser,
    isLoading,
    hasPermission,
    logout,
    setIsLoginPageVisible,
  } = useApp();
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);

  const unassignedOrdersCount = orders.filter((o) => o.status === 'UNASSIGNED').length;
  const activeExceptionsCount = exceptions.filter((e) => !e.resolved).length;
  const repairDevicesCount = devices.filter((d) => d.status === 'REPAIR' || d.status === 'LOCKED').length;

  const allNavItems: NavItem[] = [
    { id: 'dashboard', label: '工作台', icon: LayoutDashboard },
    { id: 'schedule', label: '甘特图排期', icon: CalendarDays, permission: 'rental:schedule:query' },
    {
      id: 'orders',
      label: '租赁订单',
      icon: ShoppingBag,
      permission: 'rental:xianyu:query',
      badge: unassignedOrdersCount > 0 ? unassignedOrdersCount : undefined,
      badgeColor: 'bg-amber-500 text-white',
    },
    {
      id: 'devices',
      label: '设备台账与维保',
      icon: Cpu,
      permission: 'rental:device:query',
      badge: repairDevicesCount > 0 ? repairDevicesCount : undefined,
      badgeColor: 'bg-indigo-600 text-white',
    },
    {
      id: 'binding',
      label: '扫码运单绑定',
      icon: QrCode,
      permission: ['rental:xianyu:ship', 'rental:device:query'],
    },
    {
      id: 'exceptions',
      label: '异常与告警',
      icon: AlertTriangle,
      permission: 'rental:review:query',
      badge: activeExceptionsCount > 0 ? activeExceptionsCount : undefined,
      badgeColor: 'bg-rose-500 text-white',
    },
  ];
  const navItems = allNavItems.filter((item) => !item.permission || hasPermission(item.permission));

  return (
    <header className="sticky top-0 z-30 bg-white/90 backdrop-blur-md border-b border-zinc-200/80 px-6 py-3 shadow-2xs select-none">
      <div className="max-w-[1600px] mx-auto flex items-center justify-between gap-4">
        {/* Brand & System Logo */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-zinc-900 text-white flex items-center justify-center shadow-sm">
            <Layers className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-zinc-900 text-base tracking-tight">设备排期中心</span>
              <span className="px-2 py-0.5 rounded-full text-[10px] font-bold tracking-wider uppercase bg-blue-50 text-blue-700 border border-blue-200/60">
                PRO SaaS
              </span>
            </div>
            <p className="text-[11px] text-zinc-500 font-normal hidden sm:block">
              智能排期算法 · 独立SN实例追踪 · 维保管理
            </p>
          </div>
        </div>

        {/* Center Minimalist Navigation Tabs */}
        <nav className="flex items-center gap-1 bg-zinc-100/80 p-1 rounded-xl border border-zinc-200/60">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`relative flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                  isActive
                    ? 'bg-white text-zinc-900 shadow-2xs border border-zinc-200/80'
                    : 'text-zinc-600 hover:text-zinc-900 hover:bg-zinc-200/50'
                }`}
              >
                <Icon className={`w-3.5 h-3.5 ${isActive ? 'text-blue-600' : 'text-zinc-500'}`} />
                <span>{item.label}</span>
                {item.badge !== undefined && (
                  <span
                    className={`ml-0.5 px-1.5 py-0.2 rounded-full text-[10px] font-bold leading-tight ${item.badgeColor}`}
                  >
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        {/* Right Action & Sync Status */}
        <div className="flex items-center gap-3">
          {hasPermission(['rental:xianyu:ship', 'rental:device:query']) && (
            <button
              onClick={() => setActiveTab('binding')}
              className={`flex items-center gap-1.5 px-3 py-1.5 text-xs font-extrabold rounded-lg shadow-2xs transition-all active:scale-95 shrink-0 ${
                activeTab === 'binding'
                  ? 'bg-blue-700 text-white ring-2 ring-blue-400/50'
                  : 'bg-blue-600 hover:bg-blue-500 text-white'
              }`}
            >
              <QrCode className="w-3.5 h-3.5" />
              <span>扫码/运单绑定</span>
            </button>
          )}

          <div className="hidden lg:flex items-center gap-2 text-xs text-zinc-500 bg-zinc-50 px-3 py-1.5 rounded-lg border border-zinc-200/60">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
            <span className="font-mono text-[11px] text-zinc-600">{lastSyncTime}</span>
          </div>

          <button
            onClick={() => void syncFromManagementSystem()}
            title="同步管理端数据"
            className="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold text-zinc-700 bg-white hover:bg-zinc-50 border border-zinc-200/80 rounded-lg shadow-2xs transition-all active:scale-95 disabled:opacity-60"
            disabled={isLoading}
          >
            <RefreshCw className={`w-3.5 h-3.5 text-zinc-600 ${isLoading ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline">{isLoading ? '同步中' : '同步管理端'}</span>
          </button>

          <div className="relative">
            <button
              onClick={() => setIsUserMenuOpen((open) => !open)}
              className="flex items-center gap-2 p-1 pl-2 pr-2.5 rounded-xl border border-zinc-200/80 bg-white hover:bg-zinc-50 shadow-2xs transition-all cursor-pointer group"
            >
              {currentUser?.avatar ? (
                <img
                  src={currentUser.avatar}
                  alt={currentUser.nickname || currentUser.username || '用户'}
                  className="w-7 h-7 rounded-full object-cover ring-2 ring-blue-500/20"
                />
              ) : (
                <span className="w-7 h-7 rounded-full bg-zinc-900 text-white font-bold text-xs flex items-center justify-center">
                  {(currentUser?.nickname || currentUser?.username || 'OP').slice(0, 2).toUpperCase()}
                </span>
              )}
              <div className="text-left hidden md:block">
                <div className="text-xs font-bold text-zinc-900 leading-none">
                  {currentUser?.nickname || currentUser?.username || '已登录'}
                </div>
                <div className="text-[10px] text-zinc-400 mt-1">管理端统一账号</div>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-zinc-400 group-hover:text-zinc-600 transition-colors" />
            </button>

            {isUserMenuOpen && (
              <>
                <div className="fixed inset-0 z-40" onClick={() => setIsUserMenuOpen(false)} />
                <div className="absolute right-0 mt-2 w-64 bg-white rounded-2xl shadow-xl border border-zinc-200/90 py-2 z-50 text-xs text-zinc-700">
                  <div className="px-4 py-3 border-b border-zinc-100 bg-zinc-50/50">
                    <p className="font-extrabold text-zinc-900 text-sm">
                      {currentUser?.nickname || currentUser?.username || '管理端用户'}
                    </p>
                    <p className="text-zinc-500 text-[11px] font-mono mt-0.5">
                      ID: {currentUser?.id || '-'}
                    </p>
                  </div>

                  <button
                    onClick={() => {
                      setIsUserMenuOpen(false);
                      setIsLoginPageVisible(true);
                    }}
                    className="w-full px-4 py-2 text-left hover:bg-zinc-50 font-bold text-blue-600 flex items-center gap-2 transition-colors"
                  >
                    <User className="w-3.5 h-3.5" />
                    <span>切换/重新登录</span>
                  </button>

                  <button
                    onClick={() => {
                      setIsUserMenuOpen(false);
                      void logout();
                    }}
                    className="w-full px-4 py-2 text-left hover:bg-rose-50 text-rose-600 font-bold flex items-center gap-2 transition-colors border-t border-zinc-100 mt-1"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>退出登录</span>
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
