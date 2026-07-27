import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { OrderStatus, OrderChannel } from '../types';
import {
  ShoppingBag,
  Search,
  Zap,
  User,
  Phone,
  Calendar,
  Truck,
  QrCode,
} from 'lucide-react';

export const OrdersView: React.FC = () => {
  const {
    orders,
    devices,
    openAllocationModal,
    dispatchOrder,
    returnOrder,
    setActiveTab,
    setPreselectedOrderForBinding,
    hasPermission,
  } = useApp();

  const [activeStatusTab, setActiveStatusTab] = useState<string>('ALL');
  const [activeChannel, setActiveChannel] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');

  const filteredOrders = orders.filter((o) => {
    if (activeStatusTab !== 'ALL' && o.status !== activeStatusTab) return false;
    if (activeChannel !== 'ALL' && o.channel !== activeChannel) return false;
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      return (
        o.orderNumber.toLowerCase().includes(term) ||
        o.customerName.toLowerCase().includes(term) ||
        o.items.some((i) => i.modelName.toLowerCase().includes(term))
      );
    }
    return true;
  });

  const getStatusBadge = (status: OrderStatus) => {
    switch (status) {
      case 'UNASSIGNED':
        return <span className="px-2 py-0.5 rounded text-xs font-bold bg-amber-100 text-amber-800">待排期 (未指派)</span>;
      case 'ASSIGNED':
        return <span className="px-2 py-0.5 rounded text-xs font-bold bg-blue-100 text-blue-800">已排期</span>;
      case 'PENDING_DISPATCH':
        return <span className="px-2 py-0.5 rounded text-xs font-bold bg-indigo-100 text-indigo-800">待出库</span>;
      case 'RENTING':
        return <span className="px-2 py-0.5 rounded text-xs font-bold bg-emerald-100 text-emerald-800">租赁中</span>;
      case 'PENDING_RETURN':
        return <span className="px-2 py-0.5 rounded text-xs font-bold bg-orange-100 text-orange-800">待归还</span>;
      case 'COMPLETED':
        return <span className="px-2 py-0.5 rounded text-xs font-bold bg-zinc-100 text-zinc-700">已完成</span>;
      case 'EXCEPTION':
        return <span className="px-2 py-0.5 rounded text-xs font-bold bg-rose-100 text-rose-800">异常 (逾期)</span>;
    }
  };

  const getChannelBadge = (channel: OrderChannel) => {
    switch (channel) {
      case 'XIANYU':
        return <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-amber-500 text-zinc-950 uppercase">闲鱼</span>;
      case 'OFFLINE':
        return <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-zinc-800 text-zinc-100 uppercase">线下</span>;
      case 'WEB':
        return <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-blue-600 text-white uppercase">官网</span>;
      case 'TAOBAO':
        return <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-orange-600 text-white uppercase">淘宝</span>;
    }
  };

  return (
    <div className="space-y-6 select-none">
      {/* Top Header & Search Controls */}
      <div className="bg-white rounded-2xl p-6 border border-zinc-200/80 shadow-2xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-100 pb-4">
          <div>
            <h2 className="text-lg font-extrabold text-zinc-900 flex items-center gap-2">
              <ShoppingBag className="w-5 h-5 text-blue-600" />
              <span>租赁订单管理</span>
            </h2>
            <p className="text-xs text-zinc-500 mt-0.5">
              管理已集成的闲鱼与线下渠道订单。核心职能为匹配设备 SN 实例、完成出归仓闭环。
            </p>
          </div>

          <div className="flex items-center gap-2">
            <select
              value={activeChannel}
              onChange={(e) => setActiveChannel(e.target.value)}
              className="px-3 py-1.5 text-xs font-bold bg-zinc-50 border border-zinc-200/80 rounded-xl focus:outline-none"
            >
              <option value="ALL">全部渠道</option>
              <option value="XIANYU">闲鱼渠道</option>
              <option value="OFFLINE">线下门店</option>
              <option value="WEB">官网</option>
              <option value="TAOBAO">淘宝</option>
            </select>

            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" />
              <input
                type="text"
                placeholder="搜索订单号 / 客户 / 设备..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9 pr-3 py-1.5 text-xs bg-zinc-50 border border-zinc-200/80 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 w-52 font-medium"
              />
            </div>
          </div>
        </div>

        {/* Status Filter Tabs */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
          {[
            { id: 'ALL', label: '全部订单' },
            { id: 'UNASSIGNED', label: '待排期', badge: orders.filter((o) => o.status === 'UNASSIGNED').length },
            { id: 'ASSIGNED', label: '已排期' },
            { id: 'PENDING_DISPATCH', label: '待出库', badge: orders.filter((o) => o.status === 'PENDING_DISPATCH').length },
            { id: 'RENTING', label: '租赁中' },
            { id: 'PENDING_RETURN', label: '待归还' },
            { id: 'COMPLETED', label: '已完成' },
            { id: 'EXCEPTION', label: '异常', badge: orders.filter((o) => o.status === 'EXCEPTION').length },
          ].map((tab) => {
            const isActive = activeStatusTab === tab.id;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveStatusTab(tab.id)}
                className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl font-extrabold transition-all whitespace-nowrap ${
                  isActive
                    ? 'bg-zinc-900 text-white shadow-xs'
                    : 'text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900'
                }`}
              >
                <span>{tab.label}</span>
                {tab.badge !== undefined && tab.badge > 0 && (
                  <span className="px-1.5 py-0.2 text-[10px] rounded-full bg-amber-500 text-zinc-950 font-black">
                    {tab.badge}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* Orders List Cards */}
      <div className="space-y-3">
        {filteredOrders.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center text-zinc-400 border border-zinc-200/80 text-xs font-semibold">
            未查询到符合条件的租赁订单
          </div>
        ) : (
          filteredOrders.map((order) => {
            const isUnassigned = order.status === 'UNASSIGNED';

            return (
              <div
                key={order.id}
                className="bg-white rounded-2xl p-5 border border-zinc-200/80 shadow-2xs hover:border-zinc-300 transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                {/* Left Info */}
                <div className="space-y-2 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    {getChannelBadge(order.channel)}
                    <span className="font-mono font-extrabold text-zinc-900 text-sm">
                      {order.orderNumber}
                    </span>
                    {getStatusBadge(order.status)}
                    <span className="text-xs text-zinc-400 ml-2">下单时间: {order.createdTime}</span>
                  </div>

                  {/* Customer & Period */}
                  <div className="flex flex-wrap items-center gap-4 text-xs text-zinc-600">
                    <span className="flex items-center gap-1">
                      <User className="w-3.5 h-3.5 text-zinc-400" />
                      <strong className="text-zinc-800">{order.customerName}</strong>
                    </span>
                    <span className="flex items-center gap-1 font-mono">
                      <Phone className="w-3.5 h-3.5 text-zinc-400" />
                      <span>{order.customerPhone}</span>
                    </span>
                    <span className="flex items-center gap-1 font-mono text-zinc-700">
                      <Calendar className="w-3.5 h-3.5 text-blue-500" />
                      <span>{order.startDate} 至 {order.endDate}</span>
                    </span>
                  </div>

                  {/* Items and assigned device badges */}
                  <div className="bg-zinc-50/80 rounded-xl p-3 border border-zinc-200/60 space-y-1.5 text-xs">
                    <div className="font-bold text-zinc-700">需求设备及关联 SN:</div>
                    <div className="flex flex-wrap items-center gap-2">
                      {order.items.map((item, idx) => {
                        const assignedUnits = item.assignedDeviceIds.map((devId) => {
                          const dev = devices.find((d) => d.id === devId);
                          return dev ? `${dev.unitCode} (${dev.sn})` : devId;
                        });

                        return (
                          <div
                            key={idx}
                            className="bg-white px-3 py-1.5 rounded-lg border border-zinc-200/80 shadow-2xs space-y-0.5"
                          >
                            <div className="font-bold text-zinc-900">
                              {item.modelName} <span className="text-blue-600">×{item.quantity}</span>
                            </div>
                            <div className="text-[11px] font-mono text-zinc-500">
                              锁定设备: {assignedUnits.length > 0 ? assignedUnits.join(', ') : '未指派'}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {order.note && (
                    <div className="text-xs text-amber-900 bg-amber-50 p-2.5 rounded-xl border border-amber-200/60">
                      备注说明: {order.note}
                    </div>
                  )}
                </div>

                {/* Right Action Buttons */}
                <div className="flex flex-col sm:flex-row md:flex-col items-end justify-center gap-2 shrink-0 border-t md:border-t-0 md:border-l border-zinc-100 pt-3 md:pt-0 md:pl-4">
                  <div className="text-right mb-1">
                    <div className="text-[11px] text-zinc-400 font-bold uppercase">预估总费用</div>
                    <div className="text-base font-extrabold text-zinc-900 font-mono">¥{order.totalPrice}</div>
                  </div>

                  {isUnassigned ? (
                    <div className="flex flex-col gap-2 w-full">
                      <button
                        onClick={() => openAllocationModal(order.id)}
                        disabled={!hasPermission('rental:device:assign')}
                        className="w-full sm:w-auto px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-white font-extrabold text-xs rounded-xl shadow-2xs flex items-center justify-center gap-1.5 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <Zap className="w-3.5 h-3.5 text-amber-400 fill-current" />
                        <span>自动计算并匹配设备</span>
                      </button>

                      <button
                        onClick={() => {
                          setPreselectedOrderForBinding(order.id);
                          setActiveTab('binding');
                        }}
                        disabled={!hasPermission('rental:xianyu:ship')}
                        className="w-full sm:w-auto px-3 py-1.5 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200/80 font-extrabold text-xs rounded-xl flex items-center justify-center gap-1.5 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <QrCode className="w-3.5 h-3.5" />
                        <span>扫码/运单快速绑定</span>
                      </button>
                    </div>
                  ) : (
                    <div className="flex flex-wrap gap-2 justify-end">
                      <button
                        onClick={() => {
                          setPreselectedOrderForBinding(order.id);
                          setActiveTab('binding');
                        }}
                        disabled={!hasPermission('rental:xianyu:ship')}
                        className="px-3 py-1.5 bg-blue-50 hover:bg-blue-100 text-blue-700 border border-blue-200/80 text-xs font-bold rounded-lg transition-all flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <QrCode className="w-3.5 h-3.5" />
                        <span>运单与SN绑定</span>
                      </button>

                      <button
                        onClick={() => openAllocationModal(order.id)}
                        disabled={!hasPermission('rental:device:assign')}
                        className="px-3 py-1.5 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 text-xs font-bold rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        调整锁定 SN
                      </button>

                      {order.status === 'PENDING_DISPATCH' && (
                        <button
                          onClick={() => void dispatchOrder(order.id)}
                          disabled={!hasPermission('rental:device:assign')}
                          className="px-3 py-1.5 bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          确认出仓
                        </button>
                      )}

                      {(order.status === 'RENTING' || order.status === 'PENDING_RETURN' || order.status === 'EXCEPTION') && (
                        <button
                          onClick={() => void returnOrder(order.id, false)}
                          disabled={!hasPermission('rental:device:assign')}
                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                          验机无误归位
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
