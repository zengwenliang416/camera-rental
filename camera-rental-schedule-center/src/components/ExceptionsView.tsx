import React from 'react';
import { useApp } from '../context/AppContext';
import {
  AlertTriangle,
  CheckCircle2,
  Zap,
  Wrench,
  ShieldAlert,
} from 'lucide-react';

export const ExceptionsView: React.FC = () => {
  const {
    exceptions,
    resolveException,
    openAllocationModal,
    openDeviceDetail,
    hasPermission,
  } = useApp();

  const activeExceptions = exceptions.filter((e) => !e.resolved);
  const resolvedExceptions = exceptions.filter((e) => e.resolved);

  return (
    <div className="space-y-6 select-none">
      {/* Header */}
      <div className="bg-white rounded-2xl p-6 border border-zinc-200/80 shadow-2xs">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-rose-50 border border-rose-200/80 flex items-center justify-center text-rose-600 shrink-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-extrabold text-zinc-900">排期与维保异常风控中心</h2>
            <p className="text-xs text-zinc-500 mt-0.5">
              实时监控归还逾期、订单未按时交收、故障转维保等风险点，提供快速闭环处置手段。
            </p>
          </div>
        </div>
      </div>

      {/* Active Exceptions */}
      <div className="space-y-3">
        <h3 className="font-extrabold text-zinc-900 text-sm flex items-center gap-2">
          <ShieldAlert className="w-4 h-4 text-rose-500" />
          <span>待处置异常 ({activeExceptions.length})</span>
        </h3>

        {activeExceptions.length === 0 ? (
          <div className="bg-white rounded-2xl p-12 text-center text-zinc-400 border border-zinc-200/80 text-xs font-semibold">
            <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
            当前无待处理运营异常，排期交收平稳运行！
          </div>
        ) : (
          activeExceptions.map((exp) => (
            <div
              key={exp.id}
              className="bg-white rounded-2xl p-5 border border-rose-200/80 shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4"
            >
              <div className="space-y-1.5 flex-1">
                <div className="flex items-center gap-2">
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-extrabold uppercase ${
                      exp.severity === 'high'
                        ? 'bg-rose-600 text-white'
                        : exp.severity === 'medium'
                        ? 'bg-amber-500 text-zinc-950'
                        : 'bg-zinc-200 text-zinc-800'
                    }`}
                  >
                    {exp.severity} 告警
                  </span>
                  <h4 className="font-extrabold text-zinc-900 text-sm">{exp.title}</h4>
                  <span className="text-xs text-zinc-400 font-mono">· {exp.createdTime}</span>
                </div>

                <p className="text-xs text-zinc-600 leading-relaxed">{exp.description}</p>
              </div>

              <div className="flex items-center gap-2 shrink-0">
                {exp.relatedOrderId && (
                  <button
                    onClick={() => openAllocationModal(exp.relatedOrderId!)}
                    className="px-3.5 py-1.5 bg-zinc-900 hover:bg-zinc-800 text-white font-extrabold text-xs rounded-xl flex items-center gap-1.5 transition-all shadow-2xs active:scale-95"
                  >
                    <Zap className="w-3.5 h-3.5 text-amber-400 fill-current" />
                    <span>处理分配</span>
                  </button>
                )}

                {exp.relatedDeviceId && (
                  <button
                    onClick={() => openDeviceDetail(exp.relatedDeviceId!)}
                    className="px-3.5 py-1.5 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 font-bold text-xs rounded-xl flex items-center gap-1.5 transition-all"
                  >
                    <Wrench className="w-3.5 h-3.5 text-zinc-600" />
                    <span>查看设备履历</span>
                  </button>
                )}

                <button
                  onClick={() => void resolveException(exp.id)}
                  disabled={!hasPermission('rental:review:update')}
                  className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs rounded-xl flex items-center gap-1.5 transition-all shadow-2xs active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>处理完成闭环</span>
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Resolved History */}
      {resolvedExceptions.length > 0 && (
        <div className="space-y-3 pt-4 border-t border-zinc-200/80">
          <h3 className="font-bold text-zinc-500 text-xs">已闭环历史记录 ({resolvedExceptions.length})</h3>
          <div className="space-y-2">
            {resolvedExceptions.map((exp) => (
              <div key={exp.id} className="p-3 bg-zinc-50/80 rounded-xl border border-zinc-200/60 text-xs text-zinc-500 flex justify-between items-center">
                <span>✓ {exp.title}</span>
                <span className="text-[10px] text-zinc-400 font-bold">已销单</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
