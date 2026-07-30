import { useEffect, useMemo, useState } from 'react';
import jsQR from 'jsqr';

import {
  fetchXianyuExpressCompanies,
  recognizeXianyuShipmentImage,
  resolveRentalDeviceQr,
} from '../../api/rental';
import { useApp } from '../../context/AppContext';
import {
  DEFAULT_EXPRESS_COMPANIES,
  type ExpressCompany,
  expressCodeFromName,
} from '../../lib/expressCompanies';
import {
  buildShippingReadiness,
  buildPendingOrderCandidates,
  filterAvailableDevices,
  filterPendingOrders,
  getPendingShipmentOrders,
  type ShippingOrderCandidate,
} from './shippingModel';
import { safeShippingError } from './shippingErrors';
import { searchPendingShipmentOrders } from './shippingApi';
import { shippingMessage } from './shippingMessages';
import { usePreferences } from '../preferences/PreferenceContext';

function readFileAsDataUrl(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('图片读取失败'));
    reader.onload = () => resolve(String(reader.result || ''));
    reader.readAsDataURL(file);
  });
}

function loadImage(source: string) {
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = new Image();
    image.onerror = () => reject(new Error('图片解析失败'));
    image.onload = () => resolve(image);
    image.src = source;
  });
}

async function decodeQrPayload(file: File) {
  const image = await loadImage(await readFileAsDataUrl(file));
  const canvas = document.createElement('canvas');
  canvas.width = image.naturalWidth || image.width;
  canvas.height = image.naturalHeight || image.height;
  const context = canvas.getContext('2d');
  if (!context) return null;
  context.drawImage(image, 0, 0);
  const pixels = context.getImageData(0, 0, canvas.width, canvas.height);
  return jsQR(pixels.data, pixels.width, pixels.height)?.data || null;
}

export function useShippingWorkbench() {
  const app = useApp();
  const { locale } = usePreferences();
  const {
    devices,
    orders,
    xianyuConfig,
    preselectedOrderForBinding,
    setPreselectedOrderForBinding,
    bindDeviceWithOrderAndLogistics,
    hasPermission,
  } = app;
  const [selectedDeviceId, setSelectedDeviceId] = useState('');
  const [selectedOrderId, setSelectedOrderId] = useState('');
  const [deviceSearch, setDeviceSearch] = useState('');
  const [orderSearch, setOrderSearch] = useState('');
  const [waybillNo, setWaybillNo] = useState('');
  const [expressCompanies, setExpressCompanies] = useState<ExpressCompany[]>(
    DEFAULT_EXPRESS_COMPANIES
  );
  const [expressCode, setExpressCode] = useState('shunfeng');
  const [deviceScanStatus, setDeviceScanStatus] = useState<string | null>(null);
  const [waybillDraftStatus, setWaybillDraftStatus] = useState<string | null>(null);
  const [isScanningDevice, setIsScanningDevice] = useState(false);
  const [isScanningWaybill, setIsScanningWaybill] = useState(false);
  const [isSearchingOrders, setIsSearchingOrders] = useState(false);
  const [orderSearchError, setOrderSearchError] = useState<string | null>(null);
  const [authorizedOrderCandidates, setAuthorizedOrderCandidates] = useState<
    ShippingOrderCandidate[]
  >([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const canViewPrivateDetails = hasPermission('rental:xianyu:ship');
  const pendingOrders = useMemo(() => getPendingShipmentOrders(orders), [orders]);
  const availableDevices = useMemo(
    () => devices.filter((device) => device.status === 'IDLE'),
    [devices]
  );
  const filteredDevices = useMemo(
    () => filterAvailableDevices(availableDevices, deviceSearch),
    [availableDevices, deviceSearch]
  );
  const filteredOrders = canViewPrivateDetails
    ? authorizedOrderCandidates
    : filterPendingOrders(pendingOrders, orderSearch);
  const selectedDevice = availableDevices.find((device) => device.id === selectedDeviceId);
  const selectedOrder = pendingOrders.find((order) => order.id === selectedOrderId);
  const selectedExpressCompany = expressCompanies.find((item) => item.code === expressCode)
    || DEFAULT_EXPRESS_COMPANIES.find((item) => item.code === expressCode)
    || {
      code: expressCode || 'other',
      expressName: shippingMessage(locale, 'runtime.otherCarrier'),
    };

  const integrationBlockReason = !xianyuConfig
    ? shippingMessage(locale, 'runtime.configUnavailable')
    : xianyuConfig.enabled === false || xianyuConfig.status === 'DISABLED'
      ? shippingMessage(locale, 'runtime.integrationDisabled')
      : xianyuConfig.status === 'MISSING_CREDENTIALS'
        ? shippingMessage(locale, 'runtime.credentialsMissing')
        : xianyuConfig.writeEnabled === false
          ? shippingMessage(locale, 'runtime.writeDisabled')
          : null;
  const readiness = buildShippingReadiness({
    waybillNo,
    carrier: selectedExpressCompany.expressName,
    device: selectedDevice,
    order: selectedOrder,
    permissionAllowed: canViewPrivateDetails,
    integrationBlockReason,
    isSubmitting,
    locale,
  });
  const boundDevices = devices.filter((device) => device.currentOrderId || device.logisticsNumber);

  useEffect(() => {
    if (!hasPermission('rental:xianyu:query')) return;
    void fetchXianyuExpressCompanies()
      .then((companies) => {
        const valid = companies.filter((item) => item.code?.trim() && item.expressName?.trim());
        if (!valid.length) return;
        setExpressCompanies(valid);
        setExpressCode((current) => (
          valid.some((item) => item.code === current)
            ? current
            : valid.find((item) => item.hot)?.code || valid[0].code
        ));
      })
      .catch(() => setExpressCompanies(DEFAULT_EXPRESS_COMPANIES));
  }, [hasPermission]);

  useEffect(() => {
    if (!preselectedOrderForBinding) return;
    const order = pendingOrders.find((item) => item.id === preselectedOrderForBinding);
    if (order) {
      setSelectedOrderId(order.id);
      setOrderSearch(order.orderNumber);
    }
    setPreselectedOrderForBinding(null);
  }, [pendingOrders, preselectedOrderForBinding, setPreselectedOrderForBinding]);

  useEffect(() => {
    if (!canViewPrivateDetails || !orderSearch.trim()) {
      setAuthorizedOrderCandidates([]);
      setOrderSearchError(null);
      setIsSearchingOrders(false);
      return;
    }

    let active = true;
    setAuthorizedOrderCandidates([]);
    setOrderSearchError(null);
    setIsSearchingOrders(true);
    const timer = window.setTimeout(() => {
      void searchPendingShipmentOrders(orderSearch)
        .then((page) => {
          if (!active) return;
          setAuthorizedOrderCandidates(buildPendingOrderCandidates(page.list || [], orders));
        })
        .catch((error) => {
          if (!active) return;
          setAuthorizedOrderCandidates([]);
          setOrderSearchError(
            safeShippingError(
              error,
              shippingMessage(locale, 'runtime.orderSearchFailed'),
              locale
            )
          );
        })
        .finally(() => {
          if (active) setIsSearchingOrders(false);
        });
    }, 300);

    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [canViewPrivateDetails, locale, orderSearch, orders]);

  useEffect(() => {
    if (selectedDeviceId && !availableDevices.some((item) => item.id === selectedDeviceId)) {
      setSelectedDeviceId('');
    }
  }, [availableDevices, selectedDeviceId]);

  useEffect(() => {
    if (selectedOrderId && !pendingOrders.some((item) => item.id === selectedOrderId)) {
      setSelectedOrderId('');
    }
  }, [pendingOrders, selectedOrderId]);

  const scanDeviceImage = async (file: File) => {
    setIsScanningDevice(true);
    setDeviceScanStatus(null);
    try {
      const payload = await decodeQrPayload(file);
      let device = undefined;
      if (payload) {
        try {
          const resolved = await resolveRentalDeviceQr(payload);
          device = availableDevices.find((item) => item.id === String(resolved.id));
        } catch {
          device = availableDevices.find((item) => (
            payload.includes(item.sn) || payload.includes(item.unitCode)
          ));
        }
      }
      if (!device) {
        device = availableDevices.find((item) => (
          file.name.includes(item.unitCode) || file.name.toUpperCase().includes(item.sn.toUpperCase())
        ));
      }
      if (!device) {
        setDeviceScanStatus(shippingMessage(locale, 'runtime.deviceNotFound'));
        return;
      }
      setSelectedDeviceId(device.id);
      setDeviceSearch(device.unitCode);
      setDeviceScanStatus(
        shippingMessage(locale, 'runtime.deviceSelected', {
          unit: device.unitCode,
          sn: device.sn,
        })
      );
    } catch (error) {
      setDeviceScanStatus(
        safeShippingError(
          error,
          shippingMessage(locale, 'runtime.deviceScanFailed'),
          locale
        )
      );
    } finally {
      setIsScanningDevice(false);
    }
  };

  const scanWaybillImage = async (file: File) => {
    setIsScanningWaybill(true);
    setWaybillDraftStatus(null);
    if (!hasPermission('rental:xianyu:ship:ocr')) {
      setWaybillDraftStatus(shippingMessage(locale, 'runtime.ocrPermissionMissing'));
      setIsScanningWaybill(false);
      return;
    }
    try {
      const result = await recognizeXianyuShipmentImage(file);
      const otherCarrier = shippingMessage(locale, 'runtime.otherCarrier');
      const nextCode = result.expressCode || expressCodeFromName(result.expressName || otherCarrier);
      if (result.expressName && !expressCompanies.some((item) => item.code === nextCode)) {
        setExpressCompanies((current) => [
          ...current,
          { code: nextCode, expressName: result.expressName || otherCarrier },
        ]);
      }
      setExpressCode(nextCode);
      setWaybillNo(result.waybillNo || '');
      setWaybillDraftStatus(
        result.waybillNo
          ? shippingMessage(locale, 'runtime.ocrDraft', {
            carrier: result.expressName || nextCode,
            waybill: result.waybillNo,
          })
          : shippingMessage(locale, 'runtime.ocrNoWaybill')
      );
    } catch (error) {
      setWaybillDraftStatus(
        safeShippingError(error, shippingMessage(locale, 'runtime.ocrFailed'), locale)
      );
    } finally {
      setIsScanningWaybill(false);
    }
  };

  const submitShipment = async () => {
    if (!readiness.canSubmit || !selectedDevice || !selectedOrder) {
      setNotice(
        readiness.primaryBlockReason || shippingMessage(locale, 'runtime.conditionsMissing')
      );
      return;
    }
    setIsSubmitting(true);
    try {
      await bindDeviceWithOrderAndLogistics({
        deviceId: selectedDevice.id,
        orderId: selectedOrder.id,
        expressCode: selectedExpressCompany.code,
        expressName: selectedExpressCompany.expressName,
        waybillNo: waybillNo.trim(),
      });
      setNotice(
        shippingMessage(locale, 'runtime.shipSuccess', {
          order: selectedOrder.orderNumber,
          device: selectedDevice.unitCode,
        })
      );
      setSelectedDeviceId('');
      setSelectedOrderId('');
      setDeviceSearch('');
      setOrderSearch('');
      setWaybillNo('');
      setDeviceScanStatus(null);
      setWaybillDraftStatus(null);
    } catch (error) {
      setNotice(
        safeShippingError(error, shippingMessage(locale, 'runtime.shipFailed'), locale)
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return {
    ...app,
    pendingOrders,
    availableDevices,
    eligibleOrderCount: pendingOrders.filter((order) => order.canShip).length,
    filteredDevices,
    filteredOrders,
    selectedDevice,
    selectedOrder,
    selectedDeviceId,
    selectedOrderId,
    setSelectedDeviceId,
    setSelectedOrderId,
    deviceSearch,
    setDeviceSearch,
    orderSearch,
    setOrderSearch,
    waybillNo,
    setWaybillNo,
    expressCompanies,
    expressCode,
    setExpressCode,
    selectedExpressCompany,
    deviceScanStatus,
    waybillDraftStatus,
    isScanningDevice,
    isScanningWaybill,
    isSearchingOrders,
    orderSearchError,
    isSubmitting,
    notice,
    setNotice,
    canViewPrivateDetails,
    integrationBlockReason,
    readiness,
    boundDevices,
    scanDeviceImage,
    scanWaybillImage,
    submitShipment,
  };
}

export type ShippingWorkbenchController = ReturnType<typeof useShippingWorkbench>;
