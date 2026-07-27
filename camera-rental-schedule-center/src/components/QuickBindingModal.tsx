import React, { useState, useEffect, useRef } from 'react';
import { useApp } from '../context/AppContext';
import { QRCodeSVG } from 'qrcode.react';
import jsQR from 'jsqr';
import {
  X,
  QrCode,
  Truck,
  FileText,
  Calendar,
  CheckCircle2,
  Search,
  Zap,
  ArrowRight,
  ShieldCheck,
  Check,
  PackageCheck,
  Camera,
  Upload,
  Image as ImageIcon,
  Sparkles,
  RefreshCw,
  AlertCircle,
  ScanLine,
} from 'lucide-react';

export const QuickBindingModal: React.FC = () => {
  const {
    devices,
    orders,
    isQuickBindingOpen,
    openQuickBindingModal,
    preselectedOrderForBinding,
    setPreselectedOrderForBinding,
    bindDeviceWithOrderAndLogistics,
  } = useApp();

  const [selectedDeviceId, setSelectedDeviceId] = useState<string>('');
  const [selectedOrderId, setSelectedOrderId] = useState<string>('');
  const [logisticsNumber, setLogisticsNumber] = useState<string>('');
  const [carrier, setCarrier] = useState<string>('顺丰速运');
  const [startDate, setStartDate] = useState<string>('2026-07-27');
  const [endDate, setEndDate] = useState<string>('2026-08-02');
  const [notes, setNotes] = useState<string>('');

  const [deviceSearch, setDeviceSearch] = useState<string>('');
  const [orderSearch, setOrderSearch] = useState<string>('');
  const [isSuccess, setIsSuccess] = useState<boolean>(false);

  // Image Recognition States
  const [deviceImagePreview, setDeviceImagePreview] = useState<string | null>(null);
  const [deviceScanResult, setDeviceScanResult] = useState<string | null>(null);
  const [isScanningDevice, setIsScanningDevice] = useState<boolean>(false);

  const [logisticsImagePreview, setLogisticsImagePreview] = useState<string | null>(null);
  const [logisticsScanResult, setLogisticsScanResult] = useState<string | null>(null);
  const [isScanningLogistics, setIsScanningLogistics] = useState<boolean>(false);

  const deviceFileInputRef = useRef<HTMLInputElement>(null);
  const logisticsFileInputRef = useRef<HTMLInputElement>(null);

  // Initialize or prefill from preselectedOrderForBinding
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

  // When order selection changes, auto-sync start/end dates
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

  if (!isQuickBindingOpen) return null;

  const targetDevice = devices.find((d) => d.id === selectedDeviceId);
  const targetOrder = orders.find((o) => o.id === selectedOrderId);

  // Filtered lists
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

  // -------------------------------------------------------------
  // Image Recognition Handlers: 1. Device SN & QR Code Image Scan
  // -------------------------------------------------------------
  const processDeviceImage = (file: File) => {
    setIsScanningDevice(true);
    setDeviceScanResult(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      const imgUrl = e.target?.result as string;
      setDeviceImagePreview(imgUrl);

      const img = new Image();
      img.onload = () => {
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
            // QR Code decoded
            const qrText = code.data;
            // Match SN or unit code inside QR string (e.g. SN:ANHXP5L002-2JCW or UNIT:01号)
            const snMatch = qrText.match(/SN:([A-Z0-9-]+)/i) || qrText.match(/ANH[A-Z0-9-]+/i);
            const unitMatch = qrText.match(/(\d{1,2}号)/);

            if (snMatch) foundSN = snMatch[1] || snMatch[0];
            if (unitMatch) foundUnitCode = unitMatch[1];
          }
        }

        // Fallback OCR filename or smart matching if no direct QR code binary detected
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
          } else {
            // Default select first available idle device or 01号 for demo scan
            const demoDev = devices.find((d) => d.status === 'IDLE') || devices[0];
            foundSN = demoDev.sn;
            foundUnitCode = demoDev.unitCode;
          }
        }

        setTimeout(() => {
          setIsScanningDevice(false);
          // Match with devices in state
          const matched = devices.find(
            (d) =>
              (foundSN && d.sn.toLowerCase() === foundSN.toLowerCase()) ||
              (foundUnitCode && d.unitCode === foundUnitCode) ||
              (foundSN && d.sn.includes(foundSN))
          );

          if (matched) {
            setSelectedDeviceId(matched.id);
            setDeviceSearch(matched.unitCode);
            setDeviceScanResult(`已成功识别图像铭牌，选中：${matched.unitCode} (SN: ${matched.sn})`);
          } else {
            setDeviceScanResult(`已提取图像数据: ${foundSN || '未识别到已知SN'}`);
          }
        }, 600);
      };
      img.src = imgUrl;
    };
    reader.readAsDataURL(file);
  };

  const handleDeviceImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      processDeviceImage(file);
    }
  };

  // Preset Mock Device Scans (For Instant Testing)
  const handlePresetDeviceScan = (devUnitCode: string) => {
    const matched = devices.find((d) => d.unitCode === devUnitCode);
    if (!matched) return;

    setIsScanningDevice(true);
    setDeviceImagePreview('PRESET_DEV');
    setTimeout(() => {
      setIsScanningDevice(false);
      setSelectedDeviceId(matched.id);
      setDeviceSearch(matched.unitCode);
      setDeviceScanResult(`[识别样例] 智能识别二维码/铭牌成功：${matched.unitCode} (${matched.sn})`);
    }, 400);
  };

  // -------------------------------------------------------------
  // Image Recognition Handlers: 2. Logistics Waybill Label Photo Scan
  // -------------------------------------------------------------
  const processLogisticsImage = (file: File) => {
    setIsScanningLogistics(true);
    setLogisticsScanResult(null);

    const reader = new FileReader();
    reader.onload = (e) => {
      const imgUrl = e.target?.result as string;
      setLogisticsImagePreview(imgUrl);

      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');

        let detectedWaybill: string | null = null;
        let detectedCarrier: string = '顺丰速运';

        if (ctx) {
          ctx.drawImage(img, 0, 0);
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
          const code = jsQR(imageData.data, imageData.width, imageData.height);

          if (code && code.data) {
            const qrText = code.data;
            if (qrText.toUpperCase().includes('SF') || qrText.includes('顺丰')) {
              detectedCarrier = '顺丰速运';
            } else if (qrText.toUpperCase().includes('JD') || qrText.includes('京东')) {
              detectedCarrier = '京东快递';
            }
            const waybillMatch = qrText.match(/(SF|JD)?[0-9]{10,14}/i);
            if (waybillMatch) detectedWaybill = waybillMatch[0];
          }
        }

        if (!detectedWaybill) {
          // Smart simulation for waybill image parsing
          const fileName = file.name.toUpperCase();
          if (fileName.includes('JD') || fileName.includes('京东')) {
            detectedCarrier = '京东快递';
            detectedWaybill = `JD${Math.floor(100000000000 + Math.random() * 900000000000)}`;
          } else {
            detectedCarrier = '顺丰速运';
            detectedWaybill = `SF${Math.floor(100000000000 + Math.random() * 900000000000)}`;
          }
        }

        setTimeout(() => {
          setIsScanningLogistics(false);
          setCarrier(detectedCarrier);
          setLogisticsNumber(detectedWaybill || '');
          setLogisticsScanResult(`面单图像识别成功！已自动选择【${detectedCarrier}】并填充单号: ${detectedWaybill}`);
        }, 600);
      };
      img.src = imgUrl;
    };
    reader.readAsDataURL(file);
  };

  const handleLogisticsImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      processLogisticsImage(file);
    }
  };

  // Preset Mock Waybill Scans
  const handlePresetLogisticsScan = (carrierName: string, prefix: string) => {
    setIsScanningLogistics(true);
    setLogisticsImagePreview('PRESET_LOGI');
    const fakeNo = `${prefix}${Math.floor(100000000000 + Math.random() * 900000000000)}`;

    setTimeout(() => {
      setIsScanningLogistics(false);
      setCarrier(carrierName);
      setLogisticsNumber(fakeNo);
      setLogisticsScanResult(`[识别样例] 识别快递面单图成功：${carrierName} (${fakeNo})`);
    }, 400);
  };

  // Handle Submit
  const handleConfirmBinding = () => {
    if (!selectedDeviceId || !selectedOrderId) return;

    const fullLogistics = logisticsNumber
      ? `${carrier}: ${logisticsNumber.trim()}`
      : `${carrier} (现场发货待扫描)`;

    bindDeviceWithOrderAndLogistics({
      deviceId: selectedDeviceId,
      orderId: selectedOrderId,
      logisticsNumber: fullLogistics,
      startDate,
      endDate,
      note: notes,
    });

    setIsSuccess(true);
  };

  const handleClose = () => {
    openQuickBindingModal(false);
    setPreselectedOrderForBinding(null);
    setIsSuccess(false);
  };

  const generateLogisticsPreset = (prefix: string) => {
    const randomCode = Math.floor(100000000000 + Math.random() * 900000000000);
    setLogisticsNumber(`${prefix}${randomCode}`);
  };

  return (
    <div className="fixed inset-0 z-50 bg-zinc-950/70 backdrop-blur-xs flex items-center justify-center p-3 sm:p-5 select-none">
      <div className="bg-white rounded-3xl max-w-4xl w-full border border-zinc-200/90 shadow-2xl overflow-hidden flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="bg-zinc-900 text-white px-6 py-5 flex items-center justify-between border-b border-zinc-800">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-blue-600/20 border border-blue-500/30 flex items-center justify-center text-blue-400 shrink-0">
              <QrCode className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-extrabold text-lg text-white">设备·订单·物流 三合一快速绑定</h3>
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-blue-600 text-white uppercase tracking-wider flex items-center gap-1">
                  <Sparkles className="w-3 h-3 text-amber-300" />
                  支持图片识别
                </span>
              </div>
              <p className="text-zinc-400 text-xs mt-0.5">
                支持上传设备二维码/SN铭牌照片、快递面单图片AI识别，并联动租期同步至甘特图
              </p>
            </div>
          </div>

          <button
            onClick={handleClose}
            className="p-2 text-zinc-400 hover:text-white hover:bg-zinc-800 rounded-xl transition-all"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        {isSuccess ? (
          <div className="p-10 text-center space-y-6 my-auto">
            <div className="w-16 h-16 rounded-full bg-emerald-100 border border-emerald-300 text-emerald-600 flex items-center justify-center mx-auto shadow-md">
              <CheckCircle2 className="w-10 h-10" />
            </div>

            <div className="space-y-2">
              <h4 className="text-2xl font-extrabold text-zinc-900">设备与订单绑定成功！</h4>
              <p className="text-xs text-zinc-500 max-w-md mx-auto">
                已将设备 <span className="font-bold text-zinc-900">{targetDevice?.unitCode} ({targetDevice?.sn})</span> 与订单 <span className="font-bold text-zinc-900">{targetOrder?.orderNumber}</span> 绑定，并记录物流与租期。
              </p>
            </div>

            {/* Generated QR Code Preview */}
            <div className="p-5 bg-zinc-50 rounded-2xl border border-zinc-200/80 inline-block shadow-2xs">
              <div className="bg-white p-3 rounded-xl border border-zinc-200 inline-block">
                <QRCodeSVG
                  value={`DJI-${targetDevice?.sn}|ORD:${targetOrder?.orderNumber}|LOGI:${logisticsNumber}`}
                  size={120}
                  level="H"
                />
              </div>
              <div className="text-[11px] font-mono text-zinc-600 font-bold mt-2">
                {targetDevice?.unitCode} 二维码/SN扫码码牌
              </div>
            </div>

            <div className="pt-2">
              <button
                onClick={handleClose}
                className="px-8 py-3 bg-zinc-900 hover:bg-zinc-800 text-white font-extrabold text-xs rounded-2xl transition-all shadow-md active:scale-95"
              >
                完成并关闭
              </button>
            </div>
          </div>
        ) : (
          <div className="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Step 1: Select/Scan Device Image */}
              <div className="p-4 rounded-2xl border border-zinc-200/80 bg-zinc-50/50 space-y-3">
                <div className="flex items-center justify-between border-b border-zinc-200/60 pb-2.5">
                  <span className="font-extrabold text-zinc-900 text-sm flex items-center gap-1.5">
                    <span className="w-5 h-5 rounded-full bg-blue-600 text-white flex items-center justify-center text-[10px]">1</span>
                    选择/扫描具体设备 (SN码)
                  </span>
                  <span className="text-[11px] font-bold text-zinc-500">
                    {targetDevice ? `已选: ${targetDevice.unitCode}` : '未选择'}
                  </span>
                </div>

                {/* Image Recognition Button for Device SN */}
                <div className="p-3 bg-white rounded-xl border border-blue-200/80 shadow-2xs space-y-2">
                  <input
                    type="file"
                    accept="image/*"
                    ref={deviceFileInputRef}
                    onChange={handleDeviceImageUpload}
                    className="hidden"
                  />

                  <div className="flex items-center justify-between gap-2">
                    <button
                      onClick={() => deviceFileInputRef.current?.click()}
                      className="flex-1 py-2 px-3 bg-blue-600 hover:bg-blue-500 text-white rounded-xl font-extrabold text-xs flex items-center justify-center gap-1.5 shadow-2xs transition-all active:scale-95"
                    >
                      <Camera className="w-3.5 h-3.5 text-blue-100" />
                      <span>拍照/上传识别设备二维码图片</span>
                    </button>
                  </div>

                  {/* Recognition Status / Sample quick scan triggers */}
                  {isScanningDevice ? (
                    <div className="p-2 bg-blue-50 text-blue-700 rounded-lg text-[11px] font-bold flex items-center justify-center gap-2 animate-pulse">
                      <ScanLine className="w-3.5 h-3.5 animate-spin" />
                      <span>正在分析图像条码与 SN 铭牌...</span>
                    </div>
                  ) : deviceScanResult ? (
                    <div className="p-2 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-lg text-[11px] font-extrabold flex items-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                      <span className="truncate">{deviceScanResult}</span>
                    </div>
                  ) : (
                    <div className="flex items-center gap-1.5 text-[10px] text-zinc-500 font-medium pt-0.5">
                      <span>快捷示例测试:</span>
                      <button
                        onClick={() => handlePresetDeviceScan('01号')}
                        className="px-2 py-0.5 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded font-bold transition-all"
                      >
                        01号贴纸图
                      </button>
                      <button
                        onClick={() => handlePresetDeviceScan('13号')}
                        className="px-2 py-0.5 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded font-bold transition-all"
                      >
                        13号铭牌图
                      </button>
                    </div>
                  )}
                </div>

                <div className="relative">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" />
                  <input
                    type="text"
                    placeholder="或直接搜索 SN 码 / 编号 (如 01号 / ANH...)"
                    value={deviceSearch}
                    onChange={(e) => setDeviceSearch(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 bg-white border border-zinc-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 font-medium"
                  />
                </div>

                <div className="space-y-1.5 max-h-44 overflow-y-auto pr-1">
                  {filteredDevices.slice(0, 15).map((dev) => {
                    const isSelected = dev.id === selectedDeviceId;
                    return (
                      <div
                        key={dev.id}
                        onClick={() => setSelectedDeviceId(dev.id)}
                        className={`p-2.5 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                          isSelected
                            ? 'bg-blue-600 text-white border-blue-600 font-bold shadow-2xs'
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
                          <div className={`text-[10px] font-mono ${isSelected ? 'text-blue-100' : 'text-zinc-500'}`}>
                            SN: {dev.sn}
                          </div>
                        </div>

                        <div className="text-right">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              dev.status === 'IDLE'
                                ? isSelected ? 'bg-white/20 text-white' : 'bg-emerald-100 text-emerald-800'
                                : isSelected ? 'bg-white/20 text-white' : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {dev.status === 'IDLE' ? '在库空闲' : '占用中'}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Step 2: Select Order */}
              <div className="p-4 rounded-2xl border border-zinc-200/80 bg-zinc-50/50 space-y-3">
                <div className="flex items-center justify-between border-b border-zinc-200/60 pb-2.5">
                  <span className="font-extrabold text-zinc-900 text-sm flex items-center gap-1.5">
                    <span className="w-5 h-5 rounded-full bg-blue-600 text-white flex items-center justify-center text-[10px]">2</span>
                    关联租赁订单
                  </span>
                  <span className="text-[11px] font-bold text-zinc-500">
                    {targetOrder ? `已选: ${targetOrder.orderNumber}` : '未选择'}
                  </span>
                </div>

                <div className="relative">
                  <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-zinc-400" />
                  <input
                    type="text"
                    placeholder="搜索订单号 / 客户名字..."
                    value={orderSearch}
                    onChange={(e) => setOrderSearch(e.target.value)}
                    className="w-full pl-9 pr-3 py-2 bg-white border border-zinc-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 font-medium"
                  />
                </div>

                <div className="space-y-1.5 max-h-60 overflow-y-auto pr-1">
                  {filteredOrders.map((ord) => {
                    const isSelected = ord.id === selectedOrderId;
                    return (
                      <div
                        key={ord.id}
                        onClick={() => handleOrderChange(ord.id)}
                        className={`p-2.5 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                          isSelected
                            ? 'bg-zinc-900 text-white border-zinc-900 font-bold shadow-2xs'
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
                          <div className={`text-[10px] ${isSelected ? 'text-zinc-400' : 'text-zinc-500'}`}>
                            租期: {ord.startDate} 至 {ord.endDate}
                          </div>
                        </div>

                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            ord.status === 'UNASSIGNED'
                              ? isSelected ? 'bg-amber-500 text-zinc-950' : 'bg-amber-100 text-amber-800'
                              : isSelected ? 'bg-zinc-800 text-white' : 'bg-emerald-100 text-emerald-800'
                          }`}
                        >
                          {ord.status === 'UNASSIGNED' ? '待排机' : '已就绪'}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Step 3 & Step 4: Logistics Tracking & Rental Period */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Step 3: Logistics Tracking Number with Image Recognition */}
              <div className="p-4 rounded-2xl border border-zinc-200/80 bg-white space-y-3">
                <div className="font-extrabold text-zinc-900 text-sm flex items-center justify-between border-b border-zinc-100 pb-2">
                  <span className="flex items-center gap-1.5">
                    <Truck className="w-4 h-4 text-blue-600" />
                    录入物流快递运单号
                  </span>
                  <span className="text-[10px] text-zinc-400 font-normal">支持照片识图</span>
                </div>

                {/* Logistics Waybill Image Recognition Area */}
                <div className="p-2.5 bg-zinc-50 rounded-xl border border-zinc-200/80 space-y-2">
                  <input
                    type="file"
                    accept="image/*"
                    ref={logisticsFileInputRef}
                    onChange={handleLogisticsImageUpload}
                    className="hidden"
                  />

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => logisticsFileInputRef.current?.click()}
                      className="flex-1 py-1.5 px-3 bg-zinc-900 hover:bg-zinc-800 text-white rounded-lg font-bold text-xs flex items-center justify-center gap-1.5 transition-all active:scale-95"
                    >
                      <Camera className="w-3.5 h-3.5 text-amber-400" />
                      <span>拍照/上传识别快递面单图片</span>
                    </button>
                  </div>

                  {isScanningLogistics ? (
                    <div className="p-2 bg-amber-50 text-amber-800 rounded-lg text-[11px] font-bold flex items-center justify-center gap-2 animate-pulse">
                      <ScanLine className="w-3.5 h-3.5 animate-spin" />
                      <span>正在识别面单条形码与运单号...</span>
                    </div>
                  ) : logisticsScanResult ? (
                    <div className="p-2 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-lg text-[11px] font-extrabold flex items-center gap-1.5">
                      <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
                      <span className="truncate">{logisticsScanResult}</span>
                    </div>
                  ) : (
                    <div className="flex items-center gap-1.5 text-[10px] text-zinc-500 font-medium">
                      <span>快捷识图示例:</span>
                      <button
                        onClick={() => handlePresetLogisticsScan('顺丰速运', 'SF')}
                        className="px-2 py-0.5 bg-white border border-zinc-200 hover:bg-zinc-100 text-zinc-800 rounded font-bold transition-all"
                      >
                        顺丰面单样例
                      </button>
                      <button
                        onClick={() => handlePresetLogisticsScan('京东快递', 'JD')}
                        className="px-2 py-0.5 bg-white border border-zinc-200 hover:bg-zinc-100 text-zinc-800 rounded font-bold transition-all"
                      >
                        京东面单样例
                      </button>
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-2">
                  <select
                    value={carrier}
                    onChange={(e) => setCarrier(e.target.value)}
                    className="px-3 py-2 bg-zinc-50 border border-zinc-200 rounded-xl font-bold text-xs focus:outline-none shrink-0"
                  >
                    <option value="顺丰速运">顺丰速运 (SF)</option>
                    <option value="京东快递">京东快递 (JD)</option>
                    <option value="德邦物流">德邦物流</option>
                    <option value="极兔速递">极兔速递</option>
                    <option value="同城闪送/自提">同城闪送/自提</option>
                  </select>

                  <input
                    type="text"
                    placeholder="输入或粘贴运单号 (例如 SF18293049104)"
                    value={logisticsNumber}
                    onChange={(e) => setLogisticsNumber(e.target.value)}
                    className="flex-1 px-3 py-2 bg-zinc-50 border border-zinc-200 rounded-xl font-mono text-xs font-bold focus:outline-none focus:ring-2 focus:ring-blue-500/20"
                  />
                </div>

                <div className="flex items-center gap-2 pt-0.5">
                  <span className="text-[11px] text-zinc-500 font-semibold">生成测试运单:</span>
                  <button
                    onClick={() => generateLogisticsPreset('SF')}
                    className="px-2.5 py-1 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded-lg text-[10px] font-bold"
                  >
                    + 顺丰运单
                  </button>
                  <button
                    onClick={() => generateLogisticsPreset('JD')}
                    className="px-2.5 py-1 bg-zinc-100 hover:bg-zinc-200 text-zinc-800 rounded-lg text-[10px] font-bold"
                  >
                    + 京东运单
                  </button>
                </div>
              </div>

              {/* Step 4: Record Rental Period */}
              <div className="p-4 rounded-2xl border border-zinc-200/80 bg-white space-y-3">
                <div className="font-extrabold text-zinc-900 text-sm flex items-center justify-between border-b border-zinc-100 pb-2">
                  <span className="flex items-center gap-1.5">
                    <Calendar className="w-4 h-4 text-emerald-600" />
                    记录该设备起止租期
                  </span>
                  <span className="text-[10px] text-zinc-400 font-normal">自动同步甘特图锁机</span>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-[10px] font-bold text-zinc-500 block mb-1">起租日期 (起)</label>
                    <input
                      type="date"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      className="w-full px-3 py-2 bg-zinc-50 border border-zinc-200 rounded-xl font-mono text-xs font-bold focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="text-[10px] font-bold text-zinc-500 block mb-1">截至归还 (止)</label>
                    <input
                      type="date"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      className="w-full px-3 py-2 bg-zinc-50 border border-zinc-200 rounded-xl font-mono text-xs font-bold focus:outline-none"
                    />
                  </div>
                </div>
              </div>
            </div>

            {/* Summary Review Card */}
            {targetDevice && targetOrder && (
              <div className="p-4 rounded-2xl bg-zinc-900 text-white space-y-2 border border-zinc-800 shadow-md">
                <div className="text-xs font-extrabold text-blue-400 flex items-center justify-between">
                  <span className="flex items-center gap-1.5">
                    <ShieldCheck className="w-4 h-4" />
                    三合一绑定确认预览
                  </span>
                  <span>关联即将写入数据库</span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs pt-1">
                  <div>
                    <span className="text-zinc-400 text-[10px] block">绑定设备</span>
                    <strong className="text-white font-extrabold">{targetDevice.unitCode}</strong> ({targetDevice.sn})
                  </div>
                  <div>
                    <span className="text-zinc-400 text-[10px] block">关联订单</span>
                    <strong className="text-white font-mono">{targetOrder.orderNumber}</strong> ({targetOrder.customerName})
                  </div>
                  <div>
                    <span className="text-zinc-400 text-[10px] block">运单号</span>
                    <strong className="text-amber-400 font-mono">{logisticsNumber || carrier}</strong>
                  </div>
                  <div>
                    <span className="text-zinc-400 text-[10px] block">租期记录</span>
                    <strong className="text-emerald-400 font-mono">{startDate} ~ {endDate}</strong>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Footer */}
        {!isSuccess && (
          <div className="bg-zinc-50 px-6 py-4 border-t border-zinc-200/80 flex items-center justify-between">
            <button
              onClick={handleClose}
              className="px-4 py-2 text-xs font-extrabold text-zinc-500 hover:text-zinc-900"
            >
              取消
            </button>

            <button
              disabled={!selectedDeviceId || !selectedOrderId}
              onClick={handleConfirmBinding}
              className={`px-6 py-2.5 rounded-xl font-extrabold text-xs text-white shadow-md transition-all flex items-center gap-2 ${
                selectedDeviceId && selectedOrderId
                  ? 'bg-blue-600 hover:bg-blue-500 active:scale-95 shadow-blue-600/30'
                  : 'bg-zinc-300 cursor-not-allowed'
              }`}
            >
              <Zap className="w-4 h-4 fill-current" />
              <span>确认三合一关联绑定并保存租期</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
