import React, { useState, useEffect, useRef } from 'react';
import { useApp } from '../context/AppContext';
import { QRCodeSVG } from 'qrcode.react';
import jsQR from 'jsqr';
import { recognizeXianyuShipmentImage, resolveRentalDeviceQr } from '../api/rental';
import {
  QrCode,
  Truck,
  FileText,
  Calendar,
  CheckCircle2,
  Search,
  Zap,
  ArrowRight,
  ShieldCheck,
  Camera,
  Upload,
  Sparkles,
  ScanLine,
  PackageCheck,
  Copy,
  Clock,
  Cpu,
  RefreshCw,
  ShoppingBag,
  ExternalLink,
} from 'lucide-react';

function toLocalDateString(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addDays(date: Date, days: number) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

const today = toLocalDateString(new Date());
const defaultEndDate = toLocalDateString(addDays(new Date(), 6));

export const QuickBindingView: React.FC = () => {
  const {
    devices,
    orders,
    preselectedOrderForBinding,
    setPreselectedOrderForBinding,
    bindDeviceWithOrderAndLogistics,
    openDeviceDetail,
    hasPermission,
  } = useApp();

  const [selectedDeviceId, setSelectedDeviceId] = useState<string>('');
  const [selectedOrderId, setSelectedOrderId] = useState<string>('');
  const [logisticsNumber, setLogisticsNumber] = useState<string>('');
  const [carrier, setCarrier] = useState<string>('顺丰速运');
  const [startDate, setStartDate] = useState<string>(today);
  const [endDate, setEndDate] = useState<string>(defaultEndDate);
  const [notes, setNotes] = useState<string>('');

  const [deviceSearch, setDeviceSearch] = useState<string>('');
  const [orderSearch, setOrderSearch] = useState<string>('');
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Image Recognition States
  const [isScanningDevice, setIsScanningDevice] = useState<boolean>(false);
  const [deviceScanResult, setDeviceScanResult] = useState<string | null>(null);
  const [deviceImagePreview, setDeviceImagePreview] = useState<string | null>(null);

  const [isScanningLogistics, setIsScanningLogistics] = useState<boolean>(false);
  const [logisticsScanResult, setLogisticsScanResult] = useState<string | null>(null);
  const [logisticsImagePreview, setLogisticsImagePreview] = useState<string | null>(null);

  const deviceFileInputRef = useRef<HTMLInputElement>(null);
  const logisticsFileInputRef = useRef<HTMLInputElement>(null);

  // Auto initialize or select order
  useEffect(() => {
    if (preselectedOrderForBinding) {
      setSelectedOrderId(preselectedOrderForBinding);
      const matchedOrder = orders.find((o) => o.id === preselectedOrderForBinding);
      if (matchedOrder) {
        setStartDate(matchedOrder.startDate);
        setEndDate(matchedOrder.endDate);
        if (matchedOrder.logisticsNumber) {
          setLogisticsNumber(matchedOrder.logisticsNumber);
        }
      }
    } else if (orders.length > 0 && !selectedOrderId) {
      setSelectedOrderId(orders[0].id);
      setStartDate(orders[0].startDate);
      setEndDate(orders[0].endDate);
    }
  }, [preselectedOrderForBinding, orders]);

  // Handle Order Select change
  const handleOrderChange = (oId: string) => {
    setSelectedOrderId(oId);
    const matched = orders.find((o) => o.id === oId);
    if (matched) {
      setStartDate(matched.startDate);
      setEndDate(matched.endDate);
      if (matched.logisticsNumber) {
        setLogisticsNumber(matched.logisticsNumber);
      }
    }
  };

  const targetDevice = devices.find((d) => d.id === selectedDeviceId);
  const targetOrder = orders.find((o) => o.id === selectedOrderId);

  // Filtered Lists
  const filteredDevices = devices.filter(
    (d) =>
      d.unitCode.toLowerCase().includes(deviceSearch.toLowerCase()) ||
      d.sn.toLowerCase().includes(deviceSearch.toLowerCase()) ||
      d.modelName.toLowerCase().includes(deviceSearch.toLowerCase())
  );

  const filteredOrders = orders.filter(
    (o) =>
      o.orderNumber.toLowerCase().includes(orderSearch.toLowerCase()) ||
      o.customerName.toLowerCase().includes(orderSearch.toLowerCase())
  );

  // 1. Process Device QR Code / SN Plate Image
  const processDeviceImage = (file: File) => {
    setIsScanningDevice(true);
    setDeviceScanResult(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      const imgUrl = e.target?.result as string;
      setDeviceImagePreview(imgUrl);

      const img = new Image();
      img.onload = async () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');

        let foundSN: string | null = null;
        let foundUnitCode: string | null = null;

        if (ctx) {
          ctx.drawImage(img, 0, 0);
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
          const code = jsQR(imageData.data, imageData.width, imageData.height);

          if (code && code.data) {
            const qrText = code.data;
            try {
              const resolved = await resolveRentalDeviceQr(qrText);
              foundSN = resolved.serialNumber || resolved.deviceNo;
              foundUnitCode = devices.find((device) => device.id === String(resolved.id))?.unitCode || null;
            } catch {
              // Fall through to local matching for legacy unsigned QR payloads.
            }
            const snMatch = qrText.match(/SN:([A-Z0-9-]+)/i) || qrText.match(/ANH[A-Z0-9-]+/i);
            const unitMatch = qrText.match(/(\d{1,2}号)/);

            if (!foundSN && snMatch) foundSN = snMatch[1] || snMatch[0];
            if (!foundUnitCode && unitMatch) foundUnitCode = unitMatch[1];
          }
        }

        if (!foundSN && !foundUnitCode) {
          const fileName = file.name;
          const matchedDev = devices.find(
            (d) =>
              fileName.includes(d.unitCode) ||
              fileName.toUpperCase().includes(d.sn.toUpperCase().slice(0, 8))
          );
          if (matchedDev) {
            foundSN = matchedDev.sn;
            foundUnitCode = matchedDev.unitCode;
          }
        }

        setTimeout(() => {
          setIsScanningDevice(false);
          const matched = devices.find(
            (d) =>
              (foundSN && d.sn.toLowerCase() === foundSN.toLowerCase()) ||
              (foundUnitCode && d.unitCode === foundUnitCode) ||
              (foundSN && d.sn.includes(foundSN))
          );

          if (matched) {
            setSelectedDeviceId(matched.id);
            setDeviceSearch(matched.unitCode);
            setDeviceScanResult(`已识图匹配成功！自动锁定：${matched.unitCode} (SN: ${matched.sn})`);
          } else {
            setDeviceScanResult(`识别得到识别码: ${foundSN || '未检测到已有SN'}`);
          }
        }, 500);
      };
      img.src = imgUrl;
    };
    reader.readAsDataURL(file);
  };

  const handleDeviceImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) processDeviceImage(file);
  };

  // 2. Process Logistics Waybill Image
  const processLogisticsImage = async (file: File) => {
    setIsScanningLogistics(true);
    setLogisticsScanResult(null);
    if (!hasPermission('rental:xianyu:ship:ocr')) {
      setIsScanningLogistics(false);
      setLogisticsScanResult('当前账号缺少 rental:xianyu:ship:ocr，不能识别运单图片。');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const imgUrl = e.target?.result as string;
      setLogisticsImagePreview(imgUrl);
    };
    reader.readAsDataURL(file);

    try {
      const result = await recognizeXianyuShipmentImage(file);
      setCarrier(result.expressName || result.expressCode || '其他');
      setLogisticsNumber(result.waybillNo || '');
      setLogisticsScanResult(
        result.waybillNo
          ? `后端 OCR 识别成功：${result.expressName || result.expressCode || '未知快递'} ${result.waybillNo}`
          : '后端 OCR 未识别到运单号，请人工录入。'
      );
    } catch (error) {
      setLogisticsScanResult(error instanceof Error ? error.message : '运单 OCR 识别失败');
    } finally {
      setIsScanningLogistics(false);
    }
  };

  const handleLogisticsImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) void processLogisticsImage(file);
  };

  // Confirm Binding submit
  const handleConfirmBinding = async () => {
    if (!selectedDeviceId || !selectedOrderId) return;
    if (!hasPermission('rental:xianyu:ship')) {
      setToastMessage('当前账号缺少 rental:xianyu:ship，不能执行真实发货绑定。');
      setTimeout(() => setToastMessage(null), 4000);
      return;
    }

    if (!logisticsNumber.trim()) {
      setToastMessage('请先识别或手工录入真实运单号，再提交后端发货。');
      setTimeout(() => setToastMessage(null), 4000);
      return;
    }

    const fullLogistics = `${carrier}: ${logisticsNumber.trim()}`;

    setIsSubmitting(true);
    try {
      await bindDeviceWithOrderAndLogistics({
        deviceId: selectedDeviceId,
        orderId: selectedOrderId,
        logisticsNumber: fullLogistics,
        startDate,
        endDate,
        note: notes,
      });
      setToastMessage(`设备 ${targetDevice?.unitCode} 与订单 ${targetOrder?.orderNumber} 已提交后端处理。`);
      setTimeout(() => setToastMessage(null), 4000);
    } finally {
      setIsSubmitting(false);
    }
  };

  // Bound devices list for historical table
  const boundDevices = devices.filter((d) => d.currentOrderId || d.logisticsNumber);

  return (
    <div className="space-y-6 pb-12 select-none">
      {/* Toast Banner */}
      {toastMessage && (
        <div className="fixed top-20 right-8 z-50 bg-zinc-900 text-white px-5 py-3.5 rounded-2xl shadow-2xl border border-zinc-700 flex items-center gap-3 animate-bounce">
          <CheckCircle2 className="w-5 h-5 text-emerald-400" />
          <span className="font-extrabold text-xs">{toastMessage}</span>
        </div>
      )}

      {/* Page Header Banner */}
      <div className="bg-zinc-900 text-white p-6 sm:p-8 rounded-3xl border border-zinc-800 shadow-xl relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center gap-2.5">
              <div className="w-10 h-10 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400">
                <QrCode className="w-5 h-5" />
              </div>
              <h1 className="text-2xl font-black text-white tracking-tight">
                扫码运单三合一绑定中心
              </h1>
              <span className="px-3 py-1 rounded-full text-[11px] font-extrabold bg-blue-600 text-white flex items-center gap-1 shadow-sm">
                <Sparkles className="w-3.5 h-3.5 text-amber-300" />
                支持图片与条码识别
              </span>
            </div>
            <p className="text-zinc-400 text-xs sm:text-sm max-w-2xl leading-relaxed">
              上传或拍摄设备二维码/SN铭牌、快递出库面单图片，系统自动识别条码解析物流单号并绑定租赁订单，同步写入甘特图排期锁机。
            </p>
          </div>

          <div className="flex items-center gap-3 text-xs shrink-0">
            <div className="px-4 py-3 bg-zinc-800/80 rounded-2xl border border-zinc-700/60 text-center">
              <span className="text-zinc-400 text-[10px] block">在库空闲设备</span>
              <strong className="text-emerald-400 font-extrabold text-lg">
                {devices.filter((d) => d.status === 'IDLE').length} 台
              </strong>
            </div>
            <div className="px-4 py-3 bg-zinc-800/80 rounded-2xl border border-zinc-700/60 text-center">
              <span className="text-zinc-400 text-[10px] block">待匹配订单</span>
              <strong className="text-amber-400 font-extrabold text-lg">
                {orders.filter((o) => o.status === 'UNASSIGNED').length} 单
              </strong>
            </div>
          </div>
        </div>

        <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-blue-600/10 rounded-full blur-3xl pointer-events-none" />
      </div>

      {/* Main Grid Workspace */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Device Recognition & Logistics Photo Scan (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          {/* Step 1: Device QR / SN Image Recognition */}
          <div className="bg-white p-5 rounded-3xl border border-zinc-200/80 shadow-2xs space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
              <div className="flex items-center gap-2">
                <span className="w-6 h-6 rounded-full bg-blue-600 text-white font-extrabold flex items-center justify-center text-xs">
                  1
                </span>
                <h3 className="font-extrabold text-zinc-900 text-sm">
                  识别/选择具体设备 (SN码与铭牌)
                </h3>
              </div>
              <span className="text-xs font-bold text-zinc-500">
                {targetDevice ? `已选: ${targetDevice.unitCode}` : '未选择'}
              </span>
            </div>

            {/* Photo Recognition Card */}
            <div className="p-4 bg-zinc-50 rounded-2xl border border-blue-200/80 space-y-3">
              <input
                type="file"
                accept="image/*"
                ref={deviceFileInputRef}
                onChange={handleDeviceImageUpload}
                className="hidden"
              />

              <div className="flex items-center gap-3">
                <button
                  onClick={() => deviceFileInputRef.current?.click()}
                  className="flex-1 py-3 px-4 bg-blue-600 hover:bg-blue-500 text-white rounded-2xl font-extrabold text-xs flex items-center justify-center gap-2 shadow-md transition-all active:scale-95"
                >
                  <Camera className="w-4 h-4 text-blue-100" />
                  <span>拍照/上传设备二维码/铭牌图片</span>
                </button>
              </div>

              {isScanningDevice ? (
                <div className="p-3 bg-blue-100/80 text-blue-800 rounded-xl text-xs font-bold flex items-center justify-center gap-2 animate-pulse">
                  <ScanLine className="w-4 h-4 animate-spin" />
                  <span>正在精准解析图像中的条形码与 SN 标牌...</span>
                </div>
              ) : deviceScanResult ? (
                <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-extrabold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{deviceScanResult}</span>
                </div>
              ) : (
                <div className="flex items-center justify-between text-[11px] text-zinc-500 font-medium">
                  <span>支持 JPG/PNG 或机身照片</span>
                  <span>也可在下方手工搜索管理端真实设备</span>
                </div>
              )}
            </div>

            {/* Manual Device Search & List */}
            <div className="space-y-2">
              <div className="relative">
                <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-400" />
                <input
                  type="text"
                  placeholder="检索 SN 码 / 编号 (如 01号 / ANH...)"
                  value={deviceSearch}
                  onChange={(e) => setDeviceSearch(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 bg-zinc-50 border border-zinc-200 rounded-xl text-xs font-medium focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-56 overflow-y-auto pr-1">
                {filteredDevices.slice(0, 20).map((dev) => {
                  const isSelected = dev.id === selectedDeviceId;
                  return (
                    <div
                      key={dev.id}
                      onClick={() => setSelectedDeviceId(dev.id)}
                      className={`p-3 rounded-2xl border cursor-pointer transition-all flex items-center justify-between ${
                        isSelected
                          ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-md'
                          : 'bg-white border-zinc-200/80 text-zinc-800 hover:border-zinc-300'
                      }`}
                    >
                      <div>
                        <div className="font-extrabold text-xs flex items-center gap-1.5">
                          <span>{dev.unitCode}</span>
                          <span className={isSelected ? 'text-blue-100 font-normal' : 'text-zinc-500 font-normal'}>
                            ({dev.modelName})
                          </span>
                        </div>
                        <div className={`text-[10px] font-mono mt-0.5 ${isSelected ? 'text-blue-100' : 'text-zinc-500'}`}>
                          SN: {dev.sn}
                        </div>
                      </div>

                      <span
                        className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                          dev.status === 'IDLE'
                            ? isSelected ? 'bg-white/20 text-white' : 'bg-emerald-100 text-emerald-800'
                            : isSelected ? 'bg-white/20 text-white' : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {dev.status === 'IDLE' ? '空闲' : '占用中'}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Step 2: Logistics Waybill Photo Recognition */}
          <div className="bg-white p-5 rounded-3xl border border-zinc-200/80 shadow-2xs space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
              <div className="flex items-center gap-2">
                <span className="w-6 h-6 rounded-full bg-blue-600 text-white font-extrabold flex items-center justify-center text-xs">
                  2
                </span>
                <h3 className="font-extrabold text-zinc-900 text-sm flex items-center gap-1.5">
                  <Truck className="w-4 h-4 text-blue-600" />
                  录入/拍照识别物流快递运单
                </h3>
              </div>
              <span className="text-xs text-zinc-400 font-normal">支持顺丰/京东面单</span>
            </div>

            {/* Waybill Photo Upload */}
            <div className="p-4 bg-zinc-50 rounded-2xl border border-zinc-200/80 space-y-3">
              <input
                type="file"
                accept="image/*"
                ref={logisticsFileInputRef}
                onChange={handleLogisticsImageUpload}
                className="hidden"
              />

              <div className="flex items-center gap-3">
                <button
                  onClick={() => logisticsFileInputRef.current?.click()}
                  className="flex-1 py-3 px-4 bg-zinc-900 hover:bg-zinc-800 text-white rounded-2xl font-extrabold text-xs flex items-center justify-center gap-2 transition-all active:scale-95 shadow-md"
                >
                  <Camera className="w-4 h-4 text-amber-400" />
                  <span>拍照/上传识别快递热敏面单照片</span>
                </button>
              </div>

              {isScanningLogistics ? (
                <div className="p-3 bg-amber-100/80 text-amber-900 rounded-xl text-xs font-bold flex items-center justify-center gap-2 animate-pulse">
                  <ScanLine className="w-4 h-4 animate-spin" />
                  <span>正在识别面单上的条形码与快递承运商...</span>
                </div>
              ) : logisticsScanResult ? (
                <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-extrabold flex items-center gap-2">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>{logisticsScanResult}</span>
                </div>
              ) : (
                <div className="flex items-center justify-between text-[11px] text-zinc-500 font-medium">
                  <span>自动提取单号与快递公司</span>
                  <span>也可手工录入已确认运单号</span>
                </div>
              )}
            </div>

            {/* Carrier Dropdown & Waybill Input */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="sm:col-span-1">
                <label className="text-[10px] font-bold text-zinc-500 block mb-1">快递承运商</label>
                <select
                  value={carrier}
                  onChange={(e) => setCarrier(e.target.value)}
                  className="w-full px-3 py-2.5 bg-zinc-50 border border-zinc-200 rounded-xl font-bold text-xs focus:outline-none"
                >
                  <option value="顺丰速运">顺丰速运 (SF)</option>
                  <option value="京东快递">京东快递 (JD)</option>
                  <option value="德邦物流">德邦物流</option>
                  <option value="极兔速递">极兔速递</option>
                  <option value="同城闪送/自提">同城闪送/自提</option>
                </select>
              </div>

              <div className="sm:col-span-2">
                <label className="text-[10px] font-bold text-zinc-500 block mb-1">运单跟踪号</label>
                <input
                  type="text"
                  placeholder="输入或粘贴运单号 (例如 SF18293049104)"
                  value={logisticsNumber}
                  onChange={(e) => setLogisticsNumber(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-zinc-50 border border-zinc-200 rounded-xl font-mono text-xs font-bold focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Order Selection, Period & Live Digital Preview Card (5 cols) */}
        <div className="lg:col-span-5 space-y-6">
          {/* Step 3: Order Selection */}
          <div className="bg-white p-5 rounded-3xl border border-zinc-200/80 shadow-2xs space-y-4">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
              <div className="flex items-center gap-2">
                <span className="w-6 h-6 rounded-full bg-blue-600 text-white font-extrabold flex items-center justify-center text-xs">
                  3
                </span>
                <h3 className="font-extrabold text-zinc-900 text-sm flex items-center gap-1.5">
                  <ShoppingBag className="w-4 h-4 text-blue-600" />
                  关联租赁订单
                </h3>
              </div>
              <span className="text-xs font-bold text-zinc-500">
                {targetOrder ? `已选: ${targetOrder.orderNumber}` : '未选择'}
              </span>
            </div>

            <div className="relative">
              <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-zinc-400" />
              <input
                type="text"
                placeholder="搜索订单号 / 客户名字..."
                value={orderSearch}
                onChange={(e) => setOrderSearch(e.target.value)}
                className="w-full pl-10 pr-4 py-2 bg-zinc-50 border border-zinc-200 rounded-xl text-xs font-medium focus:outline-none"
              />
            </div>

            <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
              {filteredOrders.map((ord) => {
                const isSelected = ord.id === selectedOrderId;
                return (
                  <div
                    key={ord.id}
                    onClick={() => handleOrderChange(ord.id)}
                    className={`p-3 rounded-2xl border cursor-pointer transition-all flex items-center justify-between ${
                      isSelected
                        ? 'bg-zinc-900 text-white border-zinc-900 font-bold shadow-md'
                        : 'bg-white border-zinc-200/80 text-zinc-800 hover:border-zinc-300'
                    }`}
                  >
                    <div>
                      <div className="font-extrabold text-xs flex items-center gap-1.5">
                        <span className="font-mono">{ord.orderNumber}</span>
                        <span className={isSelected ? 'text-zinc-300 font-normal' : 'text-zinc-500 font-normal'}>
                          · {ord.customerName}
                        </span>
                      </div>
                      <div className={`text-[10px] mt-0.5 ${isSelected ? 'text-zinc-400' : 'text-zinc-500'}`}>
                        租期: {ord.startDate} 至 {ord.endDate}
                      </div>
                    </div>

                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                        ord.status === 'UNASSIGNED'
                          ? isSelected ? 'bg-amber-400 text-zinc-950' : 'bg-amber-100 text-amber-800'
                          : isSelected ? 'bg-zinc-800 text-white' : 'bg-emerald-100 text-emerald-800'
                      }`}
                    >
                      {ord.status === 'UNASSIGNED' ? '待排机' : '已就绪'}
                    </span>
                  </div>
                );
              })}
            </div>

            {/* Rental Period Inputs */}
            <div className="grid grid-cols-2 gap-3 pt-2 border-t border-zinc-100">
              <div>
                <label className="text-[10px] font-bold text-zinc-500 block mb-1">起租日期</label>
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-50 border border-zinc-200 rounded-xl font-mono text-xs font-bold focus:outline-none"
                />
              </div>

              <div>
                <label className="text-[10px] font-bold text-zinc-500 block mb-1">截至归还</label>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-full px-3 py-2 bg-zinc-50 border border-zinc-200 rounded-xl font-mono text-xs font-bold focus:outline-none"
                />
              </div>
            </div>
          </div>

          {/* Step 4: Digital Badge Live Preview & Submit */}
          {targetDevice && targetOrder ? (
            <div className="bg-zinc-900 text-white p-6 rounded-3xl border border-zinc-800 shadow-xl space-y-5">
              <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
                <span className="text-xs font-extrabold text-blue-400 flex items-center gap-1.5">
                  <ShieldCheck className="w-4 h-4" />
                  三合一关联实时预览
                </span>
                <span className="text-[10px] text-zinc-400">扫码码牌实时同步</span>
              </div>

              <div className="flex items-center gap-4 bg-zinc-800/80 p-4 rounded-2xl border border-zinc-700/60">
                <div className="bg-white p-2.5 rounded-xl shrink-0 shadow-2xs">
                  <QRCodeSVG
                    value={`DJI-${targetDevice.sn}|ORD:${targetOrder.orderNumber}|LOGI:${logisticsNumber || 'NONE'}`}
                    size={80}
                    level="H"
                  />
                </div>
                <div className="space-y-1 text-xs">
                  <div>
                    <span className="text-zinc-400 text-[10px] block">绑定设备</span>
                    <strong className="text-white font-extrabold text-sm">{targetDevice.unitCode}</strong>
                    <span className="text-zinc-400 font-mono text-[10px] ml-1">({targetDevice.sn})</span>
                  </div>
                  <div>
                    <span className="text-zinc-400 text-[10px] block">关联订单与运单</span>
                    <strong className="text-amber-400 font-mono">{targetOrder.orderNumber}</strong>
                    <span className="text-blue-300 font-mono text-[11px] ml-1.5">
                      {logisticsNumber ? `${carrier}: ${logisticsNumber}` : carrier}
                    </span>
                  </div>
                  <div className="text-[10px] text-emerald-400 font-mono pt-0.5">
                    锁定租期: {startDate} ~ {endDate}
                  </div>
                </div>
              </div>

              <button
                onClick={() => void handleConfirmBinding()}
                disabled={isSubmitting || !hasPermission('rental:xianyu:ship')}
                className="w-full py-3.5 bg-blue-600 hover:bg-blue-500 text-white font-extrabold text-xs rounded-2xl shadow-lg transition-all active:scale-95 flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Zap className="w-4 h-4 fill-current text-amber-300" />
                <span>{isSubmitting ? '正在提交后端发货绑定...' : '确认三合一关联绑定 (真实调用后端)'}</span>
              </button>
            </div>
          ) : (
            <div className="p-8 text-center bg-zinc-50 border border-zinc-200/80 rounded-3xl space-y-2">
              <PackageCheck className="w-8 h-8 text-zinc-300 mx-auto" />
              <div className="text-xs font-bold text-zinc-700">请选择左侧设备与订单</div>
              <p className="text-[11px] text-zinc-400 max-w-xs mx-auto">
                完成后即可开启一键三合一关联绑定，生成专属可打印二维码并写入履历。
              </p>
            </div>
          )}
        </div>
      </div>

      {/* History Log Table: Recent Bindings */}
      <div className="bg-white p-6 rounded-3xl border border-zinc-200/80 shadow-2xs space-y-4">
        <div className="flex items-center justify-between border-b border-zinc-100 pb-3">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-emerald-600" />
            <h3 className="font-extrabold text-zinc-900 text-sm">已绑定的设备运单与租期履历</h3>
          </div>
          <span className="text-xs font-bold text-zinc-500">
            共 {boundDevices.length} 台设备已录入运单
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-zinc-100 text-zinc-400 font-bold text-[11px]">
                <th className="py-2.5 px-3">设备编号/型号</th>
                <th className="py-2.5 px-3">SN 序列码</th>
                <th className="py-2.5 px-3">关联订单与客户</th>
                <th className="py-2.5 px-3">快递运单号</th>
                <th className="py-2.5 px-3">锁定租期</th>
                <th className="py-2.5 px-3">排期状态</th>
                <th className="py-2.5 px-3 text-right">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-100 font-medium">
              {boundDevices.map((dev) => (
                <tr key={dev.id} className="hover:bg-zinc-50/80 transition-colors">
                  <td className="py-3 px-3">
                    <strong className="text-zinc-900 font-extrabold">{dev.unitCode}</strong>
                    <span className="text-zinc-500 text-[11px] ml-1">({dev.modelName})</span>
                  </td>
                  <td className="py-3 px-3 font-mono text-zinc-600 text-[11px]">{dev.sn}</td>
                  <td className="py-3 px-3">
                    <div className="font-mono text-zinc-900 font-bold">{dev.currentOrderId || '未指定'}</div>
                    <div className="text-zinc-500 text-[11px]">{dev.currentCustomer || '-'}</div>
                  </td>
                  <td className="py-3 px-3">
                    <span className="px-2 py-1 rounded bg-blue-50 text-blue-800 border border-blue-200/60 font-mono text-[11px] font-bold inline-flex items-center gap-1">
                      <Truck className="w-3 h-3 text-blue-600" />
                      {dev.logisticsNumber || '已安排发送'}
                    </span>
                  </td>
                  <td className="py-3 px-3 font-mono text-zinc-600 text-[11px]">
                    {dev.currentPeriod ? `${dev.currentPeriod.startDate} ~ ${dev.currentPeriod.endDate}` : '-'}
                  </td>
                  <td className="py-3 px-3">
                    <span className="px-2 py-0.5 rounded text-[10px] font-extrabold bg-emerald-100 text-emerald-800">
                      锁机就绪
                    </span>
                  </td>
                  <td className="py-3 px-3 text-right">
                    <button
                      onClick={() => openDeviceDetail(dev.id)}
                      className="px-2.5 py-1 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded-lg text-[11px] font-bold transition-all"
                    >
                      详情履历
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
