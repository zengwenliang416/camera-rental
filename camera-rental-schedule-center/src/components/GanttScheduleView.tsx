import React, { useState, useMemo } from 'react';
import { useApp } from '../context/AppContext';
import { calculateModelStats } from '../lib/scheduleEngine';
import {
  Calendar as CalendarIcon,
  Search,
  Filter,
  Layers,
  ChevronRight,
  Info,
  List,
  Grid,
  Wrench,
} from 'lucide-react';

function toLocalDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export const GanttScheduleView: React.FC = () => {
  const {
    categories,
    models,
    devices,
    blocks,
    orders,
    selectedModelId,
    setSelectedModelId,
    openAllocationModal,
    openDeviceDetail,
  } = useApp();

  const [viewMode, setViewMode] = useState<'gantt' | 'table'>('gantt');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Date range generator (14 days from the real client date)
  const days = useMemo(() => {
    const list: { dateStr: string; displayDay: string; weekday: string; isToday: boolean }[] = [];
    const base = new Date();
    base.setHours(0, 0, 0, 0);
    const weekMap = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

    for (let i = 0; i < 14; i++) {
      const d = new Date(base);
      d.setDate(base.getDate() + i);
      const m = d.getMonth() + 1;
      const dayNum = d.getDate();
      const dateStr = toLocalDateString(d);
      const displayDay = `${m}/${dayNum}`;
      const weekday = weekMap[d.getDay()];

      list.push({
        dateStr,
        displayDay,
        weekday,
        isToday: i === 0,
      });
    }
    return list;
  }, []);

  // Current selected model
  const currentModel = models.find((m) => m.id === selectedModelId) || models[0];

  // Stats for selected model
  const modelStats = useMemo(() => {
    return currentModel
      ? calculateModelStats(currentModel.id, devices, blocks)
      : {
          totalUnits: 0,
          idleCount: 0,
          rentingCount: 0,
          reservedCount: 0,
          pendingReturnCount: 0,
          repairCount: 0,
          lockedCount: 0,
          utilizationRate: 0,
        };
  }, [currentModel, devices, blocks]);

  // Devices for selected model
  const filteredDevices = useMemo(() => {
    if (!currentModel) return [];
    return devices
      .filter((d) => d.modelId === currentModel.id)
      .filter((d) => {
        if (statusFilter !== 'ALL' && d.status !== statusFilter) return false;
        if (searchTerm) {
          const term = searchTerm.toLowerCase();
          return (
            d.unitCode.toLowerCase().includes(term) ||
            d.sn.toLowerCase().includes(term) ||
            (d.currentCustomer && d.currentCustomer.toLowerCase().includes(term))
          );
        }
        return true;
      });
  }, [devices, currentModel, statusFilter, searchTerm]);

  // Block style mapping
  const getBlockStyle = (type: string) => {
    switch (type) {
      case 'RENTAL':
        return 'bg-blue-600 text-white shadow-2xs border border-blue-700 hover:bg-blue-700';
      case 'RESERVE':
        return 'bg-purple-600 text-white shadow-2xs border border-purple-700 hover:bg-purple-700';
      case 'REPAIR':
        return 'bg-rose-500 text-white border border-rose-600 shadow-2xs hover:bg-rose-600';
      case 'LOCK':
        return 'bg-zinc-800 text-zinc-100 border border-zinc-900 shadow-2xs hover:bg-zinc-900';
      default:
        return 'bg-zinc-500 text-white';
    }
  };

  if (!currentModel) {
    return (
      <div className="bg-white rounded-2xl p-10 border border-zinc-200/80 shadow-2xs text-center">
        <div className="text-sm font-extrabold text-zinc-900">暂无真实排期数据</div>
        <p className="text-xs text-zinc-500 mt-2">
          请先完成管理端数据同步，或检查当前账号是否拥有设备、排期和闲鱼订单查询权限。
        </p>
      </div>
    );
  }

  return (
    <div className="flex flex-col lg:flex-row gap-6 items-start select-none">
      {/* Left Sidebar: Categories & Models Tree */}
      <div className="w-full lg:w-72 bg-white rounded-2xl p-4 border border-zinc-200/80 shadow-2xs shrink-0 space-y-4">
        <div className="flex items-center justify-between pb-3 border-b border-zinc-100">
          <h3 className="font-bold text-zinc-900 text-sm flex items-center gap-2">
            <Layers className="w-4 h-4 text-blue-600" />
            <span>设备型号选单</span>
          </h3>
          <span className="text-[11px] font-mono text-zinc-400">共 {models.length} 款</span>
        </div>

        <div className="space-y-4 max-h-[640px] overflow-y-auto pr-1">
          {categories.map((cat) => {
            const catModels = models.filter((m) => m.categoryId === cat.id);
            return (
              <div key={cat.id} className="space-y-1">
                <div className="text-[10px] font-bold uppercase tracking-wider text-zinc-400 px-2 py-1">
                  {cat.name}
                </div>
                {catModels.map((m) => {
                  const mDevices = devices.filter((d) => d.modelId === m.id);
                  const count = mDevices.length;
                  const repairCount = mDevices.filter((d) => d.status === 'REPAIR').length;
                  const isSelected = selectedModelId === m.id;
                  return (
                    <button
                      key={m.id}
                      onClick={() => setSelectedModelId(m.id)}
                      className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-semibold transition-all ${
                        isSelected
                          ? 'bg-zinc-900 text-white shadow-xs'
                          : 'text-zinc-700 hover:bg-zinc-100 hover:text-zinc-900'
                      }`}
                    >
                      <span className="truncate">{m.name}</span>
                      <div className="flex items-center gap-1.5">
                        {repairCount > 0 && (
                          <span className="px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-rose-500 text-white">
                            维保 {repairCount}
                          </span>
                        )}
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                            isSelected ? 'bg-zinc-800 text-zinc-200' : 'bg-zinc-100 text-zinc-600'
                          }`}
                        >
                          {count} 台
                        </span>
                      </div>
                    </button>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>

      {/* Right Main Content: Selected Model Gantt Schedule */}
      <div className="flex-1 w-full bg-white rounded-2xl p-5 border border-zinc-200/80 shadow-2xs space-y-5">
        {/* Model Header & Stats Row */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-zinc-100">
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-extrabold text-zinc-900 tracking-tight">
                {currentModel.name} 甘特图排期控制台
              </h2>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-zinc-900 text-white">
                全池 {modelStats.totalUnits} 台独立SN
              </span>
            </div>
            <p className="text-xs text-zinc-500 mt-1">
              按时间轴向视图查看独立 SN 实例 (01号~{modelStats.totalUnits.toString().padStart(2, '0')}号) 的租赁排期与锁机图表
            </p>
          </div>

          {/* Model Status Metrics Badges */}
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="px-2.5 py-1 rounded-lg bg-emerald-50 text-emerald-800 font-bold border border-emerald-200/60">
              空闲在库: {modelStats.idleCount}
            </span>
            <span className="px-2.5 py-1 rounded-lg bg-blue-50 text-blue-800 font-bold border border-blue-200/60">
              租赁中: {modelStats.rentingCount}
            </span>
            <span className="px-2.5 py-1 rounded-lg bg-purple-50 text-purple-800 font-bold border border-purple-200/60">
              已预留: {modelStats.reservedCount}
            </span>
            <span className="px-2.5 py-1 rounded-lg bg-amber-50 text-amber-800 font-bold border border-amber-200/60">
              待归还: {modelStats.pendingReturnCount}
            </span>
            <span className="px-2.5 py-1 rounded-lg bg-rose-50 text-rose-800 font-bold border border-rose-200/60 flex items-center gap-1">
              <Wrench className="w-3 h-3 text-rose-600" />
              维保检测: {modelStats.repairCount}
            </span>
          </div>
        </div>

        {/* Toolbar Controls */}
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-zinc-50/80 p-2.5 rounded-xl border border-zinc-200/80">
          {/* Search & Status Filter */}
          <div className="flex items-center gap-2 w-full sm:w-auto flex-1 max-w-lg">
            <div className="relative flex-1">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" />
              <input
                type="text"
                placeholder="搜索机号 (01号) / SN 码 / 客户姓名..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 text-xs bg-white border border-zinc-200/80 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
              />
            </div>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-1.5 text-xs bg-white border border-zinc-200/80 rounded-lg font-semibold text-zinc-700 cursor-pointer"
            >
              <option value="ALL">全部状态</option>
              <option value="IDLE">仅看空闲在库</option>
              <option value="RENTING">仅看租赁中</option>
              <option value="REPAIR">仅看维保/检修</option>
              <option value="LOCKED">仅看人工锁定</option>
            </select>
          </div>

          {/* View Mode Switcher */}
          <div className="flex items-center gap-1 bg-white p-1 rounded-lg border border-zinc-200/80">
            <button
              onClick={() => setViewMode('gantt')}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition-all ${
                viewMode === 'gantt'
                  ? 'bg-zinc-900 text-white shadow-2xs'
                  : 'text-zinc-600 hover:text-zinc-900'
              }`}
            >
              <Grid className="w-3.5 h-3.5" />
              <span>甘特排期视角</span>
            </button>
            <button
              onClick={() => setViewMode('table')}
              className={`flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition-all ${
                viewMode === 'table'
                  ? 'bg-zinc-900 text-white shadow-2xs'
                  : 'text-zinc-600 hover:text-zinc-900'
              }`}
            >
              <List className="w-3.5 h-3.5" />
              <span>设备台账明细</span>
            </button>
          </div>
        </div>

        {/* View Mode 1: Gantt Chart */}
        {viewMode === 'gantt' && (
          <div className="space-y-3">
            {/* Gantt Legend Bar */}
            <div className="flex flex-wrap items-center justify-between gap-3 bg-zinc-50/80 px-4 py-2.5 rounded-xl border border-zinc-200/80 text-xs">
              <div className="flex items-center gap-1.5 font-bold text-zinc-700">
                <Grid className="w-3.5 h-3.5 text-blue-600" />
                <span>甘特图例:</span>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-zinc-600">
                  <span className="w-3 h-3 rounded bg-blue-600 inline-block" /> 租赁在租 (出库履约中)
                </span>
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-zinc-600">
                  <span className="w-3 h-3 rounded bg-amber-500 inline-block" /> 待出库/已排期
                </span>
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-zinc-600">
                  <span className="w-3 h-3 rounded bg-purple-600 inline-block" /> 预约预留
                </span>
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-zinc-600">
                  <span className="w-3 h-3 rounded bg-rose-600 inline-block" /> 维保/故障检测
                </span>
                <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-zinc-600">
                  <span className="w-3 h-3 rounded bg-zinc-100 border border-zinc-300 inline-block" /> 空闲可排期
                </span>
              </div>
            </div>

            <div className="border border-zinc-200/80 rounded-xl overflow-hidden shadow-2xs">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse min-w-[1000px]">
                {/* Header: Dates */}
                <thead>
                  <tr className="bg-zinc-100/70 border-b border-zinc-200/80 text-xs font-bold text-zinc-700">
                    <th className="sticky left-0 bg-zinc-100 z-10 w-48 p-3 border-r border-zinc-200/80">
                      设备编号 & SN 实例
                    </th>
                    {days.map((d, idx) => (
                      <th
                        key={idx}
                        className={`p-2 text-center border-r border-zinc-200/60 min-w-[72px] ${
                          d.isToday ? 'bg-blue-50/90 text-blue-700 font-extrabold' : ''
                        }`}
                      >
                        <div>{d.displayDay}</div>
                        <div className="text-[10px] text-zinc-400 font-normal">{d.weekday}</div>
                      </th>
                    ))}
                  </tr>
                </thead>

                {/* Rows: Devices */}
                <tbody className="divide-y divide-zinc-100 text-xs">
                  {filteredDevices.length === 0 ? (
                    <tr>
                      <td colSpan={15} className="p-8 text-center text-zinc-400">
                        未搜索到符合条件的设备实例
                      </td>
                    </tr>
                  ) : (
                    filteredDevices.map((dev) => {
                      const devBlocks = blocks.filter((b) => b.deviceId === dev.id);

                      return (
                        <tr key={dev.id} className="hover:bg-zinc-50/80 transition-colors">
                          {/* Device Identity Column */}
                          <td className="sticky left-0 bg-white z-10 p-3 border-r border-zinc-200/80 shadow-2xs">
                            <div className="flex items-center justify-between">
                              <button
                                onClick={() => openDeviceDetail(dev.id)}
                                className="text-left hover:text-blue-600 group"
                              >
                                <div className="font-extrabold text-zinc-900 group-hover:underline flex items-center gap-1.5">
                                  <span>{dev.unitCode}</span>
                                  <span
                                    className={`w-2 h-2 rounded-full ${
                                      dev.status === 'IDLE'
                                        ? 'bg-emerald-500'
                                        : dev.status === 'RENTING'
                                        ? 'bg-blue-500'
                                        : dev.status === 'RESERVED'
                                        ? 'bg-purple-500'
                                        : 'bg-rose-500'
                                    }`}
                                  />
                                </div>
                                <div className="text-[10px] font-mono text-zinc-400 truncate max-w-[120px]">
                                  {dev.sn}
                                </div>
                              </button>

                            </div>
                          </td>

                          {/* 14 Day Timeline Cells */}
                          {days.map((d, dIdx) => {
                            const activeBlock = devBlocks.find(
                              (b) => d.dateStr >= b.startDate && d.dateStr <= b.endDate
                            );

                            const isStart = activeBlock && (d.dateStr === activeBlock.startDate || dIdx === 0);
                            const isEnd = activeBlock && (d.dateStr === activeBlock.endDate || dIdx === days.length - 1);

                            return (
                              <td
                                key={dIdx}
                                className={`p-0.5 border-r border-zinc-100 text-center relative ${
                                  d.isToday ? 'bg-blue-50/20' : ''
                                }`}
                              >
                                {activeBlock ? (
                                  <div
                                    title={`订单/任务: ${
                                      activeBlock.orderNumber || '维修'
                                    } | 客户: ${
                                      activeBlock.customerName || '检测'
                                    } | 租期: ${activeBlock.startDate}~${activeBlock.endDate}`}
                                    onClick={() => {
                                      if (activeBlock.orderId)
                                        openAllocationModal(activeBlock.orderId);
                                      else openDeviceDetail(dev.id);
                                    }}
                                    className={`py-1.5 px-1 text-[10px] font-extrabold truncate cursor-pointer transition-transform hover:scale-105 flex items-center justify-center gap-0.5 shadow-2xs ${
                                      isStart ? 'rounded-l-lg' : ''
                                    } ${isEnd ? 'rounded-r-lg' : ''} ${getBlockStyle(
                                      activeBlock.type
                                    )}`}
                                  >
                                    {activeBlock.type === 'REPAIR' && <Wrench className="w-2.5 h-2.5 shrink-0" />}
                                    <span className="truncate">
                                      {isStart || dIdx === 0
                                        ? activeBlock.orderNumber || activeBlock.statusText || '在租'
                                        : '•'}
                                    </span>
                                  </div>
                                ) : dev.status === 'REPAIR' ? (
                                  <div
                                    onClick={() => openDeviceDetail(dev.id)}
                                    className="py-1.5 px-1 rounded-md bg-rose-50 text-rose-700 border border-rose-200/80 text-[10px] font-bold flex items-center justify-center gap-1 cursor-pointer hover:bg-rose-100/80"
                                  >
                                    <Wrench className="w-2.5 h-2.5 text-rose-600 shrink-0" />
                                    <span>维保</span>
                                  </div>
                                ) : (
                                  <div
                                    onClick={() => openDeviceDetail(dev.id)}
                                    className="h-6 rounded-md bg-zinc-50/60 hover:bg-emerald-50/70 border border-transparent hover:border-emerald-200/80 transition-colors flex items-center justify-center text-[10px] text-zinc-300 hover:text-emerald-700 font-semibold cursor-pointer"
                                  >
                                    可排
                                  </div>
                                )}
                              </td>
                            );
                          })}
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>
          </div>
        )}

        {/* View Mode 2: Table List */}
        {viewMode === 'table' && (
          <div className="border border-zinc-200/80 rounded-xl overflow-hidden shadow-2xs">
            <table className="w-full text-left text-xs">
              <thead className="bg-zinc-100/70 border-b border-zinc-200/80 font-bold text-zinc-700">
                <tr>
                  <th className="p-3">机号</th>
                  <th className="p-3">设备 SN 码</th>
                  <th className="p-3">当前状态</th>
                  <th className="p-3">关联订单</th>
                  <th className="p-3">客户</th>
                  <th className="p-3">租期/维保期限</th>
                  <th className="p-3">预计可用时间</th>
                  <th className="p-3 text-right">设备详情</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-100">
                {filteredDevices.map((dev) => (
                  <tr key={dev.id} className="hover:bg-zinc-50/80">
                    <td className="p-3 font-extrabold text-zinc-900">{dev.unitCode}</td>
                    <td className="p-3 font-mono text-zinc-600">{dev.sn}</td>
                    <td className="p-3">
                      <span
                        className={`px-2.5 py-1 rounded-md font-bold text-[11px] inline-flex items-center gap-1 ${
                          dev.status === 'IDLE'
                            ? 'bg-emerald-50 text-emerald-800 border border-emerald-200/60'
                            : dev.status === 'RENTING'
                            ? 'bg-blue-50 text-blue-800 border border-blue-200/60'
                            : dev.status === 'RESERVED'
                            ? 'bg-purple-50 text-purple-800 border border-purple-200/60'
                            : 'bg-rose-50 text-rose-800 border border-rose-200/60'
                        }`}
                      >
                        {dev.status === 'REPAIR' && <Wrench className="w-3 h-3 text-rose-600" />}
                        {dev.status === 'IDLE'
                          ? '空闲在库'
                          : dev.status === 'RENTING'
                          ? '租赁中'
                          : dev.status === 'RESERVED'
                          ? '已预留'
                          : dev.status === 'PENDING_RETURN'
                          ? '待归还'
                          : '维保/检测'}
                      </span>
                    </td>
                    <td className="p-3 font-mono text-zinc-800">
                      {dev.currentOrderId || '-'}
                    </td>
                    <td className="p-3 text-zinc-700 font-medium">{dev.currentCustomer || '-'}</td>
                    <td className="p-3 font-mono text-zinc-600">
                      {dev.currentPeriod
                        ? `${dev.currentPeriod.startDate} ~ ${dev.currentPeriod.endDate}`
                        : '-'}
                    </td>
                    <td className="p-3 font-semibold text-zinc-800">
                      {dev.expectedAvailableDate || '立即可用'}
                    </td>
                    <td className="p-3 text-right">
                      <button
                        onClick={() => openDeviceDetail(dev.id)}
                        className="px-2.5 py-1 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded-md font-bold text-[11px] transition-colors"
                      >
                        全轨迹
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
