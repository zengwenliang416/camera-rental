import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { calculateModelStats } from '../lib/scheduleEngine';
import { Search } from 'lucide-react';

export const DevicesView: React.FC = () => {
  const {
    models,
    devices,
    blocks,
    selectedModelId,
    setSelectedModelId,
    openDeviceDetail,
  } = useApp();

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const currentModel = models.find((m) => m.id === selectedModelId) || models[0];
  const modelStats = calculateModelStats(currentModel.id, devices, blocks);

  const modelDevices = devices
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

  return (
    <div className="space-y-6 select-none">
      {/* Model Overview Cards Bar */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        {models.map((m) => {
          const stats = calculateModelStats(m.id, devices, blocks);
          const isSelected = selectedModelId === m.id;

          return (
            <button
              key={m.id}
              onClick={() => setSelectedModelId(m.id)}
              className={`p-4 rounded-2xl border text-left transition-all ${
                isSelected
                  ? 'bg-zinc-900 text-white border-zinc-900 shadow-md scale-102'
                  : 'bg-white text-zinc-800 border-zinc-200/80 hover:border-zinc-300'
              }`}
            >
              <div className="flex items-center justify-between">
                <span className={`text-[10px] font-extrabold uppercase tracking-wider px-2 py-0.5 rounded ${
                  isSelected ? 'bg-blue-600 text-white' : 'bg-zinc-100 text-zinc-600'
                }`}>
                  {stats.totalUnits} 台
                </span>
                <span className="text-[11px] font-mono text-emerald-400 font-bold">
                  {stats.utilizationRate}% 利用率
                </span>
              </div>

              <div className="mt-2 font-extrabold text-sm truncate">{m.name}</div>
              <div className={`text-xs mt-1 ${isSelected ? 'text-zinc-400' : 'text-zinc-500'}`}>
                空闲 {stats.idleCount} · 租赁 {stats.rentingCount} · 维保 {stats.repairCount}
              </div>
            </button>
          );
        })}
      </div>

      {/* Selected Model Device Detail List */}
      <div className="bg-white rounded-2xl p-6 border border-zinc-200/80 shadow-2xs space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-100 pb-4">
          <div>
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-extrabold text-zinc-900">{currentModel.name} 独立设备台账</h2>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-blue-600 text-white">
                编号 01号 ~ {modelStats.totalUnits.toString().padStart(2, '0')}号
              </span>
            </div>
            <p className="text-xs text-zinc-500 mt-0.5">
              同步管理端具体 SN 序列号，实时查询全生命周期去向轨迹与故障状态。
            </p>
          </div>

          <div className="flex items-center gap-2">
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-3 py-1.5 text-xs font-bold bg-zinc-50 border border-zinc-200/80 rounded-xl focus:outline-none"
            >
              <option value="ALL">全部状态</option>
              <option value="IDLE">空闲在库</option>
              <option value="RENTING">租赁中</option>
              <option value="RESERVED">已预留</option>
              <option value="PENDING_RETURN">待归还</option>
              <option value="REPAIR">维保/检测</option>
            </select>

            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" />
              <input
                type="text"
                placeholder="搜索编号 / SN / 客户..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9 pr-3 py-1.5 text-xs bg-zinc-50 border border-zinc-200/80 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 w-52 font-medium"
              />
            </div>
          </div>
        </div>

        {/* Device Table */}
        <div className="border border-zinc-200/80 rounded-xl overflow-hidden text-xs">
          <table className="w-full text-left border-collapse">
            <thead className="bg-zinc-50 border-b border-zinc-200/80 font-extrabold text-zinc-700">
              <tr>
                <th className="p-3">设备编号</th>
                <th className="p-3">具体 SN 码 (管理端)</th>
                <th className="p-3">当前运行状态</th>
                <th className="p-3">关联订单号</th>
                <th className="p-3">当前客户</th>
                <th className="p-3">预计可用日期</th>
                <th className="p-3 text-right">履历追踪</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100">
              {modelDevices.map((dev) => (
                <tr key={dev.id} className="hover:bg-zinc-50/80 transition-colors">
                  <td className="p-3 font-extrabold text-zinc-900 text-sm">{dev.unitCode}</td>
                  <td className="p-3 font-mono text-zinc-700 font-bold">{dev.sn}</td>
                  <td className="p-3">
                    <span
                      className={`px-2.5 py-0.5 rounded font-extrabold text-[11px] ${
                        dev.status === 'IDLE'
                          ? 'bg-emerald-100 text-emerald-800'
                          : dev.status === 'RENTING'
                          ? 'bg-blue-100 text-blue-800'
                          : dev.status === 'RESERVED'
                          ? 'bg-purple-100 text-purple-800'
                          : 'bg-rose-100 text-rose-800'
                      }`}
                    >
                      {dev.status === 'IDLE'
                        ? '空闲'
                        : dev.status === 'RENTING'
                        ? '租赁中'
                        : dev.status === 'RESERVED'
                        ? '已预留'
                        : dev.status === 'PENDING_RETURN'
                        ? '待归还'
                        : '维保/检测'}
                    </span>
                  </td>
                  <td className="p-3 font-mono font-bold text-zinc-800">
                    {dev.currentOrderId ? 'XY20260726' : '-'}
                  </td>
                  <td className="p-3 text-zinc-700 font-bold">{dev.currentCustomer || '-'}</td>
                  <td className="p-3 text-zinc-600 font-mono">{dev.expectedAvailableDate || '立即可用'}</td>
                  <td className="p-3 text-right">
                    <button
                      onClick={() => openDeviceDetail(dev.id)}
                      className="px-3 py-1.5 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded-lg font-bold transition-all"
                    >
                      查看履历
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
