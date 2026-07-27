import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import { QRCodeSVG } from 'qrcode.react';
import {
  X,
  Cpu,
  Calendar,
  Clock,
  Wrench,
  QrCode,
  Truck,
  Zap,
} from 'lucide-react';

export const DeviceDetailDrawer: React.FC = () => {
  const {
    devices,
    blocks,
    selectedDeviceIdForDetail,
    openDeviceDetail,
    updateDeviceStatus,
    setActiveTab,
    hasPermission,
  } = useApp();

  const device = devices.find((d) => d.id === selectedDeviceIdForDetail);
  const [noteInput, setNoteInput] = useState('');

  if (!device || !selectedDeviceIdForDetail) return null;

  // Find all schedule blocks for this device
  const deviceBlocks = blocks.filter((b) => b.deviceId === device.id);

  // Future and past schedule logs
  const today = '2026-07-27';
  const futureBlocks = deviceBlocks.filter((b) => b.endDate >= today);
  const pastBlocks = deviceBlocks.filter((b) => b.endDate < today);

  const handleSetRepair = () => {
    updateDeviceStatus(device.id, 'REPAIR', noteInput || '设备转入维保检测');
    setNoteInput('');
  };

  const handleSetIdle = () => {
    updateDeviceStatus(device.id, 'IDLE');
  };

  const qrString = device.qrCode || `DJI-${device.modelId.toUpperCase()}|UNIT:${device.unitCode}|SN:${device.sn}`;

  return (
    <div className="fixed inset-0 z-50 bg-zinc-950/70 backdrop-blur-xs flex justify-end p-0 sm:p-4 select-none">
      <div className="bg-white max-w-xl w-full h-full sm:rounded-2xl border border-zinc-200/90 shadow-2xl overflow-hidden flex flex-col">
        {/* Header */}
        <div className="bg-zinc-900 text-white p-5 flex items-center justify-between border-b border-zinc-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 shrink-0">
              <Cpu className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-extrabold text-lg text-white">{device.modelName} - {device.unitCode}</h3>
                <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-zinc-800 text-zinc-300 font-mono">
                  {device.sn}
                </span>
              </div>
              <p className="text-zinc-400 text-xs mt-0.5">设备履历与时空轨迹中心 ("这台设备去哪了？")</p>
            </div>
          </div>

          <button
            onClick={() => openDeviceDetail(null)}
            className="p-2 text-zinc-400 hover:text-white hover:bg-zinc-800 rounded-xl transition-all"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-5 flex-1 text-xs">
          {/* QR Code & Digital Asset Card */}
          <div className="p-4 rounded-2xl border border-zinc-200/80 bg-zinc-50/80 flex items-center justify-between gap-4">
            <div className="space-y-1 flex-1">
              <span className="text-[10px] font-extrabold uppercase tracking-wider text-blue-600 flex items-center gap-1">
                <QrCode className="w-3.5 h-3.5" />
                设备二维码防伪标签
              </span>
              <div className="font-extrabold text-zinc-900 text-sm">{device.unitCode} 唯一数字铭牌</div>
              <p className="text-[11px] text-zinc-500 leading-relaxed font-mono">
                {device.sn}
              </p>
              <button
                onClick={() => {
                  openDeviceDetail(null);
                  setActiveTab('binding');
                }}
                className="mt-2 px-3 py-1.5 bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl font-bold text-[11px] inline-flex items-center gap-1.5 transition-all shadow-2xs active:scale-95"
              >
                <Zap className="w-3 h-3 text-amber-400 fill-current" />
                <span>扫码/绑定订单与物流</span>
              </button>
            </div>

            <div className="bg-white p-2.5 rounded-xl border border-zinc-200/90 shadow-2xs shrink-0">
              <QRCodeSVG value={qrString} size={84} level="M" />
            </div>
          </div>

          {/* Current Status Card */}
          <div className="p-4 rounded-2xl border border-zinc-200/80 bg-white shadow-2xs space-y-3">
            <div className="flex items-center justify-between">
              <span className="font-extrabold text-zinc-900 text-sm">当前运营状态</span>
              <span
                className={`px-2.5 py-1 rounded font-extrabold text-xs ${
                  device.status === 'IDLE'
                    ? 'bg-emerald-100 text-emerald-800'
                    : device.status === 'RENTING'
                    ? 'bg-blue-100 text-blue-800'
                    : device.status === 'RESERVED'
                    ? 'bg-purple-100 text-purple-800'
                    : 'bg-rose-100 text-rose-800'
                }`}
              >
                {device.status === 'IDLE'
                  ? '空闲在库'
                  : device.status === 'RENTING'
                  ? '租赁中'
                  : device.status === 'RESERVED'
                  ? '已预留排期'
                  : device.status === 'PENDING_RETURN'
                  ? '待归还'
                  : '维保/检测'}
              </span>
            </div>

            {device.currentOrderId && (
              <div className="space-y-1.5 text-zinc-700 pt-2 border-t border-zinc-100">
                <div>当前客户: <strong className="text-zinc-900">{device.currentCustomer}</strong></div>
                <div>关联订单: <span className="font-mono font-bold text-zinc-900">{device.currentOrderId}</span></div>
                {device.logisticsNumber && (
                  <div className="flex items-center gap-1 text-blue-700 font-bold">
                    <Truck className="w-3.5 h-3.5" />
                    <span>快递运单: <strong className="font-mono">{device.logisticsNumber}</strong></span>
                  </div>
                )}
                {device.currentPeriod && (
                  <div>租期记录: <span className="font-mono text-zinc-900 font-bold">{device.currentPeriod.startDate} 至 {device.currentPeriod.endDate}</span></div>
                )}
              </div>
            )}

            <div className="text-zinc-600 pt-1 font-medium">
              预计可用时间: <span className="font-extrabold text-zinc-900">{device.expectedAvailableDate || '立即可用'}</span>
            </div>
          </div>

          {/* Future Schedule Blocks */}
          <div className="space-y-3">
            <h4 className="font-extrabold text-zinc-900 text-sm flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-blue-600" />
              <span>未来排期与预留</span>
            </h4>

            {futureBlocks.length === 0 ? (
              <p className="text-zinc-400 py-3 text-center bg-zinc-50 rounded-xl border border-zinc-200/60 font-semibold">
                未来暂无预留排期，全时段可排
              </p>
            ) : (
              <div className="space-y-2">
                {futureBlocks.map((blk) => (
                  <div key={blk.id} className="p-3 bg-blue-50/60 border border-blue-200/80 rounded-xl space-y-1">
                    <div className="flex items-center justify-between font-extrabold text-blue-900">
                      <span>{blk.orderNumber || '维保计划'}</span>
                      <span className="font-mono text-[11px] text-blue-700">{blk.startDate} ~ {blk.endDate}</span>
                    </div>
                    {blk.customerName && <div className="text-zinc-600 font-medium">客户: {blk.customerName}</div>}
                    {blk.logisticsNumber && (
                      <div className="text-[10px] text-blue-800 font-mono font-bold flex items-center gap-1">
                        <Truck className="w-3 h-3 text-blue-600" />
                        运单号: {blk.logisticsNumber}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Historical Rental Logs */}
          <div className="space-y-3">
            <h4 className="font-extrabold text-zinc-900 text-sm flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-emerald-600" />
              <span>历史租赁与出库履历</span>
            </h4>

            {pastBlocks.length === 0 ? (
              <div className="space-y-2">
                <div className="p-3 bg-zinc-50 border border-zinc-200/80 rounded-xl space-y-1">
                  <div className="flex items-center justify-between font-bold text-zinc-800">
                    <span>XY20260715002 (历史示例)</span>
                    <span className="font-mono text-[11px] text-zinc-500">2026-07-15 ~ 2026-07-20</span>
                  </div>
                  <div className="text-zinc-500">客户: 成都摄影工作室 · 运单: SF1920384210 · 正常归还</div>
                </div>
              </div>
            ) : (
              <div className="space-y-2">
                {pastBlocks.map((blk) => (
                  <div key={blk.id} className="p-3 bg-zinc-50 border border-zinc-200/80 rounded-xl space-y-1">
                    <div className="flex items-center justify-between font-bold text-zinc-800">
                      <span>{blk.orderNumber}</span>
                      <span className="font-mono text-[11px] text-zinc-500">{blk.startDate} ~ {blk.endDate}</span>
                    </div>
                    <div className="text-zinc-500">客户: {blk.customerName}</div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Quick Operational Status Override */}
          <div className="p-4 rounded-2xl border border-zinc-200/80 bg-zinc-900 text-white space-y-3 shadow-md">
            <h4 className="font-extrabold text-xs flex items-center gap-1.5 text-blue-400">
              <Wrench className="w-4 h-4" />
              <span>人工调整设备状态</span>
            </h4>

            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="维保说明 (如：云台电机校准/电池耗损检测)"
                value={noteInput}
                onChange={(e) => setNoteInput(e.target.value)}
                className="flex-1 px-3 py-1.5 text-xs bg-zinc-800 border border-zinc-700 rounded-xl text-white placeholder-zinc-500 focus:outline-none"
              />
              <button
                onClick={handleSetRepair}
                disabled={!hasPermission('rental:device:assign')}
                className="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-xl font-extrabold text-xs transition-all shadow-2xs active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                标为维保
              </button>
            </div>

            {device.status !== 'IDLE' && (
              <button
                onClick={handleSetIdle}
                disabled={!hasPermission('rental:device:assign')}
                className="w-full py-2 bg-emerald-600 hover:bg-emerald-500 text-white font-extrabold text-xs rounded-xl transition-all shadow-2xs active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                强制重置为空闲可租用
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
