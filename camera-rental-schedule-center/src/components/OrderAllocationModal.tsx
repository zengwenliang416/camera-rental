import React, { useState, useEffect } from 'react';
import { useApp } from '../context/AppContext';
import { recommendDevicesForOrder, checkDeviceAvailability } from '../lib/scheduleEngine';
import { DeviceInstance } from '../types';
import {
  X,
  Zap,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Cpu,
  Calendar,
  User,
  Phone,
  Tag,
  Check,
  ChevronRight,
} from 'lucide-react';

export const OrderAllocationModal: React.FC = () => {
  const {
    orders,
    devices,
    blocks,
    selectedOrderIdForAllocation,
    openAllocationModal,
    assignDevicesToOrder,
    hasPermission,
  } = useApp();

  const targetOrder = orders.find((o) => o.id === selectedOrderIdForAllocation);

  // Local allocation state: modelId -> array of deviceIds
  const [allocationMap, setAllocationMap] = useState<Record<string, string[]>>({});
  const [manualSwapModelId, setManualSwapModelId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (targetOrder) {
      const initialMap: Record<string, string[]> = {};
      targetOrder.items.forEach((item) => {
        initialMap[item.modelId] = [...item.assignedDeviceIds];
      });
      setAllocationMap(initialMap);
    }
  }, [targetOrder]);

  if (!targetOrder || !selectedOrderIdForAllocation) return null;

  // Run auto recommendation
  const handleAutoAllocate = () => {
    if (!targetOrder.rentalPeriodReady) {
      setAllocationMap({});
      return;
    }
    const recommended = recommendDevicesForOrder(targetOrder, devices, blocks);
    const newMap: Record<string, string[]> = {};
    Object.entries(recommended).forEach(([mId, devList]) => {
      newMap[mId] = devList.map((d) => d.id);
    });
    setAllocationMap(newMap);
  };

  // Calculate total required vs total assigned
  const totalRequired = targetOrder.items.reduce((sum, item) => sum + item.quantity, 0);
  const totalAssigned = Object.values(allocationMap).reduce<number>(
    (sum, arr) => sum + (Array.isArray(arr) ? arr.length : 0),
    0
  );
  const isFullyAssigned = totalAssigned >= totalRequired;
  const progressPercent = totalRequired > 0 ? Math.min(100, Math.round((totalAssigned / totalRequired) * 100)) : 0;

  // Confirm allocation
  const canSubmitRealSchedule =
    hasPermission('rental:device:assign') &&
    targetOrder.rentalPeriodReady &&
    targetOrder.items.some((item) => item.rentalOrderItemId);

  const handleConfirm = async () => {
    if (!canSubmitRealSchedule || !isFullyAssigned) return;
    setIsSubmitting(true);
    try {
      await assignDevicesToOrder(targetOrder.id, allocationMap);
      if (canSubmitRealSchedule) {
        openAllocationModal(null);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-3xl w-full border border-slate-200 shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="bg-slate-900 text-white p-5 flex items-center justify-between border-b border-slate-800">
          <div>
            <div className="flex items-center space-x-2">
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-500 text-slate-950 uppercase">
                {targetOrder.channel} 渠道订单
              </span>
              <h3 className="font-bold text-lg font-mono tracking-wide">{targetOrder.orderNumber}</h3>
            </div>
            <p className="text-slate-400 text-xs mt-1">
              备注解析租期: <span className="text-white font-medium">{targetOrder.rentalPeriodLabel}</span> · 客户: {targetOrder.customerName}
            </p>
          </div>

          <button
            onClick={() => openAllocationModal(null)}
            className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {/* Top Auto Allocation Banner */}
          <div className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <div className="font-bold text-blue-900 text-sm flex items-center space-x-1.5">
                <Zap className="w-4 h-4 text-blue-600 fill-current" />
                <span>智能自动排机引擎</span>
              </div>
              <p className="text-xs text-blue-700 mt-1">
                根据【备注解析租期无冲突 + 当前在库 + 无维保记录】推荐设备；最终排期由后端事务校验。
              </p>
              {!targetOrder.rentalPeriodReady && (
                <p className="text-xs text-amber-700 mt-1 font-bold">
                  当前订单租期待复核，不能自动推荐或创建排期。
                </p>
              )}
            </div>

            <button
              onClick={handleAutoAllocate}
              disabled={!targetOrder.rentalPeriodReady}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded-xl shadow-sm shadow-blue-500/30 flex items-center justify-center space-x-1.5 whitespace-nowrap active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Zap className="w-3.5 h-3.5 fill-current" />
              <span>一键推荐并填充</span>
            </button>
          </div>

          {/* Allocation Progress Bar */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-xs font-semibold">
              <span className="text-slate-700">设备分配进度</span>
              <span className={isFullyAssigned ? 'text-emerald-600 font-bold' : 'text-amber-600 font-bold'}>
                {totalAssigned} / {totalRequired} 台
              </span>
            </div>
            <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden border border-slate-200">
              <div
                className={`h-full transition-all duration-300 ${
                  isFullyAssigned ? 'bg-emerald-500' : 'bg-amber-500'
                }`}
                style={{ width: `${progressPercent}%` }}
              />
            </div>
          </div>

          {/* Items Requirements Breakdown */}
          <div className="space-y-4">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-400">设备需求与分配列表</h4>

            {targetOrder.items.map((item) => {
              const assignedIds = allocationMap[item.modelId] || [];
              const modelDevices = devices.filter((d) => d.modelId === item.modelId);
              const isItemComplete = assignedIds.length >= item.quantity;

              return (
                <div
                  key={item.modelId}
                  className="p-4 rounded-xl border border-slate-200 bg-white shadow-xs space-y-3"
                >
                  <div className="flex items-center justify-between border-b border-slate-100 pb-2">
                    <div className="flex items-center space-x-2">
                      <Cpu className="w-4 h-4 text-slate-600" />
                      <span className="font-bold text-slate-900 text-sm">{item.modelName}</span>
                      <span className="text-xs text-slate-500">需求: {item.quantity} 台</span>
                    </div>

                    <span
                      className={`px-2 py-0.5 rounded text-xs font-bold ${
                        isItemComplete ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      {isItemComplete ? '✓ 分配完成' : `缺少 ${item.quantity - assignedIds.length} 台`}
                    </span>
                  </div>

                  {/* Assigned Cards */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {assignedIds.map((devId, idx) => {
                      const dev = devices.find((d) => d.id === devId);
                      if (!dev) return null;

                      return (
                        <div
                          key={devId}
                          className="p-2.5 rounded-lg border border-emerald-200 bg-emerald-50/60 flex items-center justify-between text-xs"
                        >
                          <div>
                            <div className="font-bold text-slate-900 flex items-center space-x-1.5">
                              <span>{dev.unitCode}</span>
                              <span className="text-[10px] text-emerald-700 font-normal">({dev.sn})</span>
                            </div>
                            <div className="text-[10px] text-emerald-700 font-medium">在库空闲 · 无排期冲突</div>
                          </div>

                          <button
                            onClick={() => {
                              setAllocationMap((prev) => ({
                                ...prev,
                                [item.modelId]: prev[item.modelId].filter((id) => id !== devId),
                              }));
                            }}
                            className="text-slate-400 hover:text-red-500 font-bold p-1"
                          >
                            ×
                          </button>
                        </div>
                      );
                    })}

                    {/* Button to trigger Manual Swap */}
                    {assignedIds.length < item.quantity && (
                      <button
                        onClick={() => setManualSwapModelId(item.modelId)}
                        className="p-2.5 rounded-lg border border-dashed border-slate-300 hover:border-blue-500 text-slate-500 hover:text-blue-600 text-xs font-medium flex items-center justify-center space-x-1 transition-colors"
                      >
                        <span>+ 选择并指定设备</span>
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>

          {/* Manual Substitution Modal Sub-Panel if active */}
          {manualSwapModelId && (
            <div className="p-4 rounded-xl bg-slate-900 text-slate-100 space-y-3">
              <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                <span className="font-bold text-xs text-white">选择具体设备 (手动指定)</span>
                <button
                  onClick={() => setManualSwapModelId(null)}
                  className="text-xs text-slate-400 hover:text-white"
                >
                  关闭
                </button>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-48 overflow-y-auto">
                {devices
                  .filter((d) => d.modelId === manualSwapModelId)
                  .map((dev) => {
                    const check = checkDeviceAvailability(
                      dev,
                      blocks,
                      targetOrder.startDate || '9999-12-30',
                      targetOrder.endDate || '9999-12-31',
                      targetOrder.id
                    );
                    const isSelected = (allocationMap[manualSwapModelId] || []).includes(dev.id);

                    return (
                      <button
                        key={dev.id}
                        disabled={!check.available}
                        onClick={() => {
                          const currentArr = allocationMap[manualSwapModelId] || [];
                          if (isSelected) {
                            setAllocationMap((prev) => ({
                              ...prev,
                              [manualSwapModelId]: currentArr.filter((id) => id !== dev.id),
                            }));
                          } else {
                            setAllocationMap((prev) => ({
                              ...prev,
                              [manualSwapModelId]: [...currentArr, dev.id],
                            }));
                          }
                        }}
                        className={`p-2 rounded-lg text-xs text-left border transition-all ${
                          isSelected
                            ? 'bg-blue-600 text-white border-blue-500 font-bold'
                            : check.available
                            ? 'bg-slate-800 text-slate-200 border-slate-700 hover:bg-slate-700'
                            : 'bg-slate-800/40 text-slate-500 border-slate-800/60 cursor-not-allowed opacity-60'
                        }`}
                      >
                        <div className="font-bold flex items-center justify-between">
                          <span>{dev.unitCode}</span>
                          {isSelected && <Check className="w-3 h-3 text-white" />}
                        </div>
                        <div className="text-[10px] font-mono truncate">{dev.sn}</div>
                        {!check.available && (
                          <div className="text-[9px] text-rose-400 font-medium truncate mt-0.5">
                            {check.reason || '冲突/维保中'}
                          </div>
                        )}
                      </button>
                    );
                  })}
              </div>
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="bg-slate-50 p-4 border-t border-slate-200 flex items-center justify-between">
          <button
            onClick={() => openAllocationModal(null)}
            className="px-4 py-2 text-xs font-semibold text-slate-600 hover:text-slate-900"
          >
            取消
          </button>

          <button
            onClick={() => void handleConfirm()}
            disabled={isSubmitting || !canSubmitRealSchedule || !isFullyAssigned}
            className={`px-6 py-2.5 rounded-xl font-bold text-xs text-white shadow-md transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed ${
              isFullyAssigned && canSubmitRealSchedule
                ? 'bg-emerald-600 hover:bg-emerald-500 shadow-emerald-600/30'
                : 'bg-amber-600 hover:bg-amber-500 shadow-amber-600/30'
            }`}
          >
            {isSubmitting
              ? '正在提交后端排期...'
              : canSubmitRealSchedule
              ? isFullyAssigned
                ? '✓ 确认并完成设备排期'
                : '保存部分分配结果'
              : '缺少内部订单明细，无法真实排期'}
          </button>
        </div>
      </div>
    </div>
  );
};
