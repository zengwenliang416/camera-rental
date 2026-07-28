import React, { useState } from 'react';
import { useApp } from '../context/AppContext';
import {
  AlertCircle,
  ArrowRight,
  Building2,
  CalendarDays,
  Cpu,
  Eye,
  EyeOff,
  KeyRound,
  Layers,
  Lock,
  QrCode,
  ShieldCheck,
  Smartphone,
  Truck,
  User,
  X,
} from 'lucide-react';

interface LoginPageProps {
  isModal?: boolean;
}

export const LoginPage: React.FC<LoginPageProps> = ({ isModal = false }) => {
  const { login, setIsLoginPageVisible, isLoading } = useApp();
  const tenantEnabled = import.meta.env.VITE_APP_TENANT_ENABLE !== 'false';
  const defaultTenantName = import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '捷租达';
  const [activeTab, setActiveTab] = useState<'password' | 'sms' | 'qr'>('password');
  const [tenantName, setTenantName] = useState(tenantEnabled ? defaultTenantName : '');
  const [username, setUsername] = useState(import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '');
  const [password, setPassword] = useState(import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg('');
    if (!username.trim()) {
      setErrorMsg('请输入管理端账号');
      return;
    }
    if (!password) {
      setErrorMsg('请输入登录密码');
      return;
    }

    try {
      await login({
        tenantName: tenantName.trim() || undefined,
        username: username.trim(),
        password,
        rememberMe,
      });
    } catch (error) {
      setErrorMsg(error instanceof Error ? error.message : '登录失败，请检查账号、密码和租户');
    }
  };

  const disabledLoginTip = (type: 'sms' | 'qr') => {
    setActiveTab(type);
    setErrorMsg(type === 'sms' ? '短信登录暂未在排期中心独立应用接入，请使用管理端账号密码登录。' : '扫码登录暂未在排期中心独立应用接入，请使用管理端账号密码登录。');
  };

  return (
    <div className={`min-h-screen w-full bg-zinc-900 flex items-center justify-center p-4 sm:p-6 relative overflow-hidden ${isModal ? 'bg-black/60 backdrop-blur-md fixed inset-0 z-50' : ''}`}>
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-blue-600/20 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl pointer-events-none" />

      {isModal && (
        <button
          onClick={() => setIsLoginPageVisible(false)}
          className="absolute top-6 right-6 p-2 rounded-full bg-white/10 hover:bg-white/20 text-white transition-all z-20"
        >
          <X className="w-5 h-5" />
        </button>
      )}

      <div className="w-full max-w-5xl bg-white rounded-3xl shadow-2xl overflow-hidden grid grid-cols-1 lg:grid-cols-12 border border-zinc-200/80 z-10 my-auto">
        <div className="lg:col-span-5 bg-gradient-to-br from-zinc-900 via-zinc-900 to-zinc-950 p-8 sm:p-10 text-white flex flex-col justify-between relative overflow-hidden">
          <div className="absolute inset-0 bg-[linear-gradient(to_right,#ffffff0a_1px,transparent_1px),linear-gradient(to_bottom,#ffffff0a_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none" />
          <div className="relative">
            <div className="flex items-center gap-3 mb-8">
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-blue-600 to-emerald-500 text-white flex items-center justify-center shadow-lg shadow-blue-500/30">
                <Layers className="w-6 h-6" />
              </div>
              <div>
                <h1 className="text-xl font-extrabold tracking-tight text-white flex items-center gap-2">
                  设备排期中心
                  <span className="px-2 py-0.5 rounded-md text-[10px] font-black uppercase bg-blue-500/20 text-blue-300 border border-blue-400/30">
                    SSO
                  </span>
                </h1>
                <p className="text-xs text-zinc-400 font-medium">与相机租赁管理后台统一认证</p>
              </div>
            </div>

            <div className="space-y-4 my-8">
              <div className="p-4 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xs">
                <div className="flex items-center gap-3 mb-1.5">
                  <div className="p-1.5 rounded-lg bg-blue-500/20 text-blue-400">
                    <CalendarDays className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-bold text-zinc-100">排期、订单、设备同源</h3>
                </div>
                <p className="text-xs text-zinc-400 leading-relaxed pl-8">
                  独立网页应用只展示和操作管理端授权数据，最终校验仍由后端权限与业务接口执行。
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xs">
                <div className="flex items-center gap-3 mb-1.5">
                  <div className="p-1.5 rounded-lg bg-emerald-500/20 text-emerald-400">
                    <Cpu className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-bold text-zinc-100">单台设备 SN 追踪</h3>
                </div>
                <p className="text-xs text-zinc-400 leading-relaxed pl-8">
                  设备状态、占用周期、扫码出库和回仓流转全部对接租赁模块真实接口。
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-white/5 border border-white/10 backdrop-blur-xs">
                <div className="flex items-center gap-3 mb-1.5">
                  <div className="p-1.5 rounded-lg bg-amber-500/20 text-amber-400">
                    <Truck className="w-4 h-4" />
                  </div>
                  <h3 className="text-sm font-bold text-zinc-100">扫码发货与运单 OCR</h3>
                </div>
                <p className="text-xs text-zinc-400 leading-relaxed pl-8">
                  OCR 只做识别辅助，订单绑定和闲管家发货必须由后端二次校验。
                </p>
              </div>
            </div>
          </div>

          <div className="relative pt-6 border-t border-white/10 flex items-center justify-between text-[11px] text-zinc-400">
            <span className="flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>管理端 SSO · 角色权限同步</span>
            </span>
            <span className="text-zinc-500">v2.0</span>
          </div>
        </div>

        <div className="lg:col-span-7 p-8 sm:p-12 flex flex-col justify-between bg-white">
          <div>
            <div className="mb-6">
              <h2 className="text-2xl font-extrabold text-zinc-900 tracking-tight">登录设备排期中心</h2>
              <p className="text-xs text-zinc-500 mt-1">使用相机租赁管理后台账号登录，登录态和权限完全互通。</p>
            </div>

            <div className="flex items-center gap-2 p-1 bg-zinc-100 rounded-xl mb-6">
              <button
                type="button"
                onClick={() => {
                  setActiveTab('password');
                  setErrorMsg('');
                }}
                className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 ${
                  activeTab === 'password'
                    ? 'bg-white text-zinc-900 shadow-2xs border border-zinc-200'
                    : 'text-zinc-500 hover:text-zinc-900'
                }`}
              >
                <KeyRound className="w-3.5 h-3.5" />
                <span>管理端账号</span>
              </button>
              <button
                type="button"
                onClick={() => disabledLoginTip('sms')}
                className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 ${
                  activeTab === 'sms'
                    ? 'bg-white text-zinc-900 shadow-2xs border border-zinc-200'
                    : 'text-zinc-500 hover:text-zinc-900'
                }`}
              >
                <Smartphone className="w-3.5 h-3.5" />
                <span>手机验证码</span>
              </button>
              <button
                type="button"
                onClick={() => disabledLoginTip('qr')}
                className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 ${
                  activeTab === 'qr'
                    ? 'bg-white text-zinc-900 shadow-2xs border border-zinc-200'
                    : 'text-zinc-500 hover:text-zinc-900'
                }`}
              >
                <QrCode className="w-3.5 h-3.5" />
                <span>扫码</span>
              </button>
            </div>

            {errorMsg && (
              <div className="mb-4 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-bold flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{errorMsg}</span>
              </div>
            )}

            {activeTab === 'password' && (
              <form onSubmit={handleLoginSubmit} className="space-y-4">
                {tenantEnabled && (
                  <div>
                    <label className="block text-xs font-bold text-zinc-700 mb-1.5">租户名称</label>
                    <div className="relative">
                      <Building2 className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        value={tenantName}
                        onChange={(e) => setTenantName(e.target.value)}
                        placeholder="请输入管理端租户名称"
                        className="w-full pl-10 pr-4 py-2.5 bg-zinc-50 border border-zinc-200 rounded-xl text-xs font-semibold text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                      />
                    </div>
                  </div>
                )}

                <div>
                  <label className="block text-xs font-bold text-zinc-700 mb-1.5">账号</label>
                  <div className="relative">
                    <User className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      placeholder="请输入管理端用户名"
                      className="w-full pl-10 pr-4 py-2.5 bg-zinc-50 border border-zinc-200 rounded-xl text-xs font-semibold text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-zinc-700 mb-1.5">密码</label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-zinc-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="请输入管理端密码"
                      className="w-full pl-10 pr-10 py-2.5 bg-zinc-50 border border-zinc-200 rounded-xl text-xs font-semibold text-zinc-900 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600"
                    >
                      {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>

                <div className="flex items-center justify-between pt-1">
                  <label className="flex items-center gap-2 cursor-pointer select-none">
                    <input
                      type="checkbox"
                      checked={rememberMe}
                      onChange={(e) => setRememberMe(e.target.checked)}
                      className="w-4 h-4 rounded border-zinc-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="text-xs text-zinc-600 font-medium">保持登录态</span>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-xl text-xs shadow-md shadow-blue-500/20 transition-all flex items-center justify-center gap-2 active:scale-98 disabled:opacity-60"
                >
                  {isLoading ? (
                    <span>正在登录...</span>
                  ) : (
                    <>
                      <span>使用管理端账号登录</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </form>
            )}

            {activeTab !== 'password' && (
              <div className="py-8 flex flex-col items-center justify-center text-center space-y-4 border border-dashed border-zinc-200 rounded-2xl bg-zinc-50/50">
                <div className="p-3 bg-white border border-zinc-200 rounded-2xl shadow-xs">
                  {activeTab === 'sms' ? <Smartphone className="w-20 h-20 text-zinc-400" /> : <QrCode className="w-20 h-20 text-zinc-400" />}
                </div>
                <div>
                  <p className="text-xs font-bold text-zinc-800">该登录方式暂未接入独立排期中心</p>
                  <p className="text-[11px] text-zinc-400 mt-0.5">请先使用管理端账号密码登录；后续可复用管理端短信/扫码能力。</p>
                </div>
              </div>
            )}
          </div>

          <div className="mt-8 text-center text-[11px] text-zinc-400">
            © 2026 捷租达 · 设备排期中心
          </div>
        </div>
      </div>
    </div>
  );
};
