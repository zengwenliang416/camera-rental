import React from 'react';
import { useApp } from '../context/AppContext';
import {
  CalendarDays,
  ShoppingBag,
  ArrowRight,
  AlertTriangle,
  Clock,
  CheckCircle2,
  Send,
  RotateCcw,
  Sparkles,
  Zap,
  Wrench,
  ShieldCheck,
  Cpu,
} from 'lucide-react';

export const DashboardView: React.FC = () => {
  const {
    orders,
    devices,
    models,
    exceptions,
    setActiveTab,
    openAllocationModal,
    returnOrder,
    setPreselectedOrderForBinding,
    setSelectedModelId,
    hasPermission,
  } = useApp();

  const unassignedOrders = orders.filter((o) => o.status === 'UNASSIGNED');
  const pendingDispatchOrders = orders.filter((o) => o.status === 'PENDING_DISPATCH');
  const activeRentalOrders = orders.filter((o) => o.status === 'PENDING_RETURN' || o.status === 'RENTING');
  const overdueOrders = orders.filter((o) => o.status === 'EXCEPTION');
  const repairDevices = devices.filter((d) => d.status === 'REPAIR' || d.status === 'LOCKED');
  const firstModel = models[0];

  return (
    <div className="space-y-6 select-none">
      {/* Top Banner */}
      <div className="bg-zinc-900 rounded-3xl p-6 sm:p-8 text-white border border-zinc-800 shadow-lg relative overflow-hidden">
        <div className="absolute -top-24 -right-24 w-96 h-96 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 relative z-10">
          <div>
            <div className="flex items-center gap-2 text-blue-400 text-xs font-bold tracking-wider uppercase mb-2">
              <Sparkles className="w-4 h-4" />
              <span>智能设备排期中心 · 今日运营概览</span>
            </div>
            <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
              设备状态排期与独立 SN 实例控制台
            </h2>
            <p className="text-zinc-400 text-xs sm:text-sm mt-2 max-w-2xl leading-relaxed">
              全池共有 <span className="text-white font-semibold">{devices.length} 台设备</span>，支持针对独立 SN 实例进行精细化租赁排期、锁机防重排与维保日志履历追踪。
            </p>
          </div>

          <button
            onClick={() => {
              if (firstModel) setSelectedModelId(firstModel.id);
              setActiveTab('schedule');
            }}
            className="flex items-center gap-2 px-5 py-3 bg-white hover:bg-zinc-100 text-zinc-900 rounded-2xl text-xs font-extrabold shadow-md transition-all active:scale-95 shrink-0"
          >
            <CalendarDays className="w-4 h-4 text-blue-600" />
            <span>进入{firstModel ? ` ${firstModel.name} ` : ' '}甘特图排期视图</span>
          </button>
        </div>
      </div>

      {/* Main Grid Section */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Cols: Priority Tasks & Repair Monitor */}
        <div className="lg:col-span-2 space-y-6">
          {/* Unassigned Orders Section */}
          <div className="bg-white rounded-2xl p-6 border border-zinc-200/80 shadow-2xs">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-4 mb-4">
              <div className="flex items-center gap-2">
                <div className="w-2.5 h-2.5 rounded-full bg-amber-500 animate-pulse" />
                <h3 className="font-bold text-zinc-900 text-base">待排期闲鱼与线下订单</h3>
                <span className="px-2.5 py-0.5 rounded-full text-xs bg-amber-50 text-amber-800 font-bold border border-amber-200/60">
                  {unassignedOrders.length} 单需指定设备号
                </span>
              </div>
              <button
                onClick={() => setActiveTab('orders')}
                className="text-xs text-blue-600 hover:text-blue-700 font-semibold flex items-center gap-1"
              >
                <span>全量订单</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>

            {unassignedOrders.length === 0 ? (
              <div className="text-center py-8 text-zinc-400 text-xs">
                <CheckCircle2 className="w-8 h-8 mx-auto text-emerald-500 mb-2" />
                当前无待排期订单，所有租赁订单均已安全排机！
              </div>
            ) : (
              <div className="space-y-3">
                {unassignedOrders.map((order) => (
                  <div
                    key={order.id}
                    className="p-4 rounded-xl border border-amber-200/80 bg-amber-50/30 hover:border-amber-300 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                  >
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="px-1.5 py-0.2 rounded text-[10px] font-extrabold bg-amber-500 text-white uppercase">
                          {order.channel}
                        </span>
                        <span className="font-mono text-xs font-bold text-zinc-900">
                          {order.orderNumber}
                        </span>
                        <span className="text-xs text-zinc-500">· 客户: {order.customerName}</span>
                      </div>
                      <div className="text-xs font-semibold text-zinc-800 flex items-center gap-2 mt-1">
                        <span>需求规格:</span>
                        {order.items.map((it, idx) => (
                          <span key={idx} className="px-2 py-0.5 bg-white rounded border border-amber-200 text-amber-900 font-mono text-xs">
                            {it.modelName} × {it.quantity}
                          </span>
                        ))}
                      </div>
                      <div className="text-xs text-zinc-500 mt-1">
                        备注解析租期: <span className="font-mono font-medium text-zinc-700">{order.rentalPeriodLabel}</span>
                      </div>
                    </div>

                    <button
                      onClick={() => openAllocationModal(order.id)}
                      disabled={!hasPermission('rental:device:assign') || !order.rentalPeriodReady}
                      className="px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-white font-bold text-xs rounded-xl shadow-xs flex items-center justify-center gap-1.5 whitespace-nowrap active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Zap className="w-3.5 h-3.5 text-amber-400 fill-current" />
                      <span>{order.rentalPeriodReady ? '智能计算一键排期' : '租期待复核'}</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Equipment Repair & Maintenance Monitor Card */}
          <div className="bg-white rounded-2xl p-6 border border-zinc-200/80 shadow-2xs">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-4 mb-4">
              <div className="flex items-center gap-2">
                <Wrench className="w-4 h-4 text-rose-500" />
                <h3 className="font-bold text-zinc-900 text-base">设备维保与检修监控</h3>
                <span className="px-2.5 py-0.5 rounded-full text-xs bg-rose-50 text-rose-800 font-bold border border-rose-200/60">
                  {repairDevices.length} 台维保/锁定中
                </span>
              </div>
              <button
                onClick={() => setActiveTab('devices')}
                className="text-xs text-zinc-500 hover:text-zinc-900 font-semibold"
              >
                设备台账 →
              </button>
            </div>

            {repairDevices.length === 0 ? (
              <div className="text-center py-6 text-zinc-400 text-xs">
                <ShieldCheck className="w-8 h-8 mx-auto text-emerald-500 mb-2" />
                全池设备均健康可租，暂无报修或检修锁定项目。
              </div>
            ) : (
              <div className="space-y-3">
                {repairDevices.map((dev) => (
                  <div
                    key={dev.id}
                    className="p-4 rounded-xl border border-rose-200/80 bg-rose-50/30 flex items-center justify-between gap-4"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-zinc-900 text-sm">{dev.modelName} - {dev.unitCode}</span>
                        <span className="font-mono text-[11px] text-zinc-500 bg-white px-2 py-0.5 rounded border border-rose-200">
                          {dev.sn}
                        </span>
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800">
                          {dev.status === 'REPAIR' ? '维保/检测' : '人工锁定'}
                        </span>
                      </div>
                      <p className="text-xs text-zinc-600 mt-1">
                        检修说明: <span className="font-medium text-zinc-800">{dev.note || '检修保养排查'}</span>
                      </p>
                    </div>

                    <span className="px-3 py-1.5 bg-zinc-100 text-zinc-500 font-bold text-xs rounded-xl whitespace-nowrap">
                      状态变更需走设备作业流程
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Today Dispatch & Return Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-white rounded-2xl p-5 border border-zinc-200/80 shadow-2xs">
              <h4 className="font-bold text-zinc-900 text-sm flex items-center gap-2 mb-3">
                <Send className="w-4 h-4 text-blue-600" />
                <span>待发货订单 ({pendingDispatchOrders.length})</span>
              </h4>
              {pendingDispatchOrders.length === 0 ? (
                <p className="text-xs text-zinc-400 py-4 text-center">暂无待发货订单</p>
              ) : (
                <div className="space-y-2">
                  {pendingDispatchOrders.map((ord) => (
                    <div key={ord.id} className="p-3 bg-zinc-50/80 rounded-xl border border-zinc-200/60 text-xs flex items-center justify-between">
                      <div>
                        <div className="font-mono font-bold text-zinc-800">{ord.orderNumber}</div>
                        <div className="text-zinc-500">{ord.items.map(i => `${i.modelName} x${i.quantity}`).join(', ')}</div>
                      </div>
                      <button
                        onClick={() => {
                          setPreselectedOrderForBinding(ord.id);
                          setActiveTab('binding');
                        }}
                        disabled={!ord.canShip}
                        className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white text-xs rounded-lg font-bold transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {ord.canShip ? '选择设备发货' : '待转换完善'}
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="bg-white rounded-2xl p-5 border border-zinc-200/80 shadow-2xs">
              <h4 className="font-bold text-zinc-900 text-sm flex items-center gap-2 mb-3">
                <RotateCcw className="w-4 h-4 text-emerald-600" />
                <span>履约中订单 ({activeRentalOrders.length})</span>
              </h4>
              {activeRentalOrders.length === 0 ? (
                <p className="text-xs text-zinc-400 py-4 text-center">暂无履约中的租赁订单</p>
              ) : (
                <div className="space-y-2">
                  {activeRentalOrders.slice(0, 3).map((ord) => (
                    <div key={ord.id} className="p-3 bg-zinc-50/80 rounded-xl border border-zinc-200/60 text-xs flex items-center justify-between">
                      <div>
                        <div className="font-mono font-bold text-zinc-800">{ord.orderNumber}</div>
                        <div className="text-zinc-500">客户: {ord.customerName}</div>
                      </div>
                      <button
                        onClick={() => void returnOrder(ord.id, false)}
                        disabled={!hasPermission('rental:device:assign') || !ord.canReturn}
                        className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs rounded-lg font-bold transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {ord.canReturn ? '验机归位' : '等待设备关联'}
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Right 1 Col: Exceptions & System Capabilities */}
        <div className="space-y-6">
          {/* Active Exceptions */}
          <div className="bg-white rounded-2xl p-6 border border-zinc-200/80 shadow-2xs">
            <div className="flex items-center justify-between mb-4 border-b border-zinc-100 pb-3">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-500" />
                <h3 className="font-bold text-zinc-900 text-base">待处置冲突与告警</h3>
              </div>
              <button
                onClick={() => setActiveTab('exceptions')}
                className="text-xs text-zinc-500 hover:text-zinc-900 font-semibold"
              >
                异常中心 →
              </button>
            </div>

            <div className="space-y-3">
              {exceptions
                .filter((e) => !e.resolved)
                .slice(0, 3)
                .map((exp) => (
                  <div key={exp.id} className="p-3.5 rounded-xl bg-rose-50/60 border border-rose-200/80 text-xs space-y-1">
                    <div className="font-bold text-rose-900 flex items-center justify-between">
                      <span>{exp.title}</span>
                      <span className="text-[10px] uppercase font-bold px-1.5 py-0.2 rounded bg-rose-200 text-rose-800">
                        {exp.severity}
                      </span>
                    </div>
                    <p className="text-zinc-700 leading-relaxed">{exp.description}</p>
                  </div>
                ))}
            </div>
          </div>

          {/* SaaS Architecture Card */}
          <div className="bg-zinc-900 text-zinc-300 rounded-2xl p-6 border border-zinc-800 shadow-2xs text-xs space-y-3">
            <div className="font-bold text-white text-sm flex items-center gap-2">
              <Cpu className="w-4 h-4 text-blue-400" />
              <span>智能排期与独立 SN 控制面板</span>
            </div>
            <p className="leading-relaxed text-zinc-400">
              精准管理管理端同步的真实设备实例，实现闲鱼订单到实际 SN 码的秒级分配与全流程锁机保护。
            </p>
            <ul className="space-y-1.5 list-disc list-inside text-zinc-300">
              <li>自动检测无碰撞交叠时段</li>
              <li>转报修状态自动从可用排期库剔除</li>
              <li>独立的检测保养记录与历史轨迹</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
