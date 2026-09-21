<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content">
      <staff-header title="扫码发货" @scanner="scanWaybill" />
      <view class="hero">
        <view class="muted">
          四步完成发货
        </view>
      </view>

      <view class="step" :class="{ active: step === 1 }" @click="scanWaybill">
        <view class="badge">
          1
        </view>
        <view class="flex-1">
          <view class="step-kicker">
            运单
          </view>
          <view class="step-title">
            扫描运单码
          </view>
          <view class="step-value">
            {{ waybillNo ? maskWaybill(waybillNo) : '扫描物流运单码' }}
          </view>
          <view v-if="expressName" class="muted">
            {{ expressName }}
          </view>
        </view>
        <text class="arrow">
          ›
        </text>
      </view>

      <view class="step" :class="{ active: step === 2 }" @click="scanDevice">
        <view class="badge">
          2
        </view>
        <view class="flex-1">
          <view class="step-kicker">
            设备
          </view>
          <view class="step-title">
            扫设备永久码
          </view>
          <view class="step-value">
            {{ deviceNo || '扫描设备永久码' }}
          </view>
          <view v-if="resolvedDevice" class="ok">
            {{ deviceStatusLabel(resolvedDevice.status) }}
          </view>
        </view>
        <text class="arrow">
          ›
        </text>
      </view>

      <view class="step" :class="{ active: step === 3 }" @click="pickOrder">
        <view class="badge">
          3
        </view>
        <view class="flex-1">
          <view class="step-kicker">
            订单
          </view>
          <view class="step-title">
            选择发货订单
          </view>
          <view class="step-value">
            {{ selectedOrder ? maskOrderId(selectedOrder.externalOrderId) : '匹配发货订单' }}
          </view>
          <view v-if="selectedOrder" class="muted">
            {{ selectedOrder.goodsTitle || '闲鱼订单' }}
          </view>
        </view>
        <text class="arrow">
          ›
        </text>
      </view>

      <view class="step">
        <view class="badge">
          4
        </view>
        <view class="flex-1">
          <view class="step-kicker">
            复核
          </view>
          <view class="step-title">
            核对以下信息
          </view>
          <view class="review">
            <view>订单号 {{ maskOrderId(selectedOrder?.externalOrderId) || '-' }}</view>
            <view>设备 {{ deviceNo || '-' }}</view>
            <view>快递 {{ expressName || '-' }}</view>
            <view>运单 {{ maskWaybill(waybillNo) }}</view>
          </view>
        </view>
      </view>

      <view class="manual-inputs">
        <view class="muted">
          当前侧键扫描目标：{{ scanTarget === 'waybill' ? '运单' : '设备' }}
        </view>
        <wd-input v-model="manualWaybill" label="运单号" placeholder="无法扫码时输入运单号" clearable @confirm="confirmManualWaybill" />
        <wd-button plain size="small" @click="confirmManualWaybill">
          确认运单
        </wd-button>
        <wd-input v-model="manualDevice" label="设备编号" placeholder="无法扫码时输入设备编号" clearable @confirm="acceptDeviceScan(manualDevice)" />
        <wd-button plain size="small" :loading="resolving" @click="acceptDeviceScan(manualDevice)">
          查询设备
        </wd-button>
        <wd-button plain block @click="carrierVisible = true">
          {{ expressName || '请选择物流公司' }}
        </wd-button>
        <wd-select-picker v-model="expressCode" v-model:visible="carrierVisible" type="radio" :columns="expressList" label-key="expressName" value-key="code" filterable title="物流公司" />
        <view v-if="expressError" class="muted">
          {{ expressError }}
        </view>
        <wd-button v-if="expressError" plain size="small" @click="loadExpress">
          重新加载物流公司
        </wd-button>
      </view>
      <view id="shipping-orders" class="search">
        <wd-input v-model="keyword" placeholder="订单号/商品关键词" clearable />
        <wd-button size="small" type="primary" :loading="orderLoading" @click="searchOrders()">
          搜索
        </wd-button>
      </view>
      <view
        v-for="item in orderList"
        :key="item.id"
        class="order"
        :class="{ on: selectedOrder?.id === item.id }"
        @click="selectedOrder = item"
      >
        <view class="strong">
          {{ maskOrderId(item.externalOrderId) }}
        </view>
        <view class="muted">
          {{ item.goodsTitle || '-' }} · ×{{ item.goodsQuantity ?? 1 }}
        </view>
      </view>
      <view v-if="orderError" class="muted">
        {{ orderError }}
      </view>
      <view v-else-if="!orderLoading && !orderList.length" class="muted">
        没有匹配的待发货订单
      </view>
      <wd-button v-if="orderList.length < orderTotal" plain block :loading="orderLoading" @click="searchOrders(false)">
        加载更多订单
      </wd-button>
    </scroll-view>
    <view class="footer">
      <view class="row">
        <wd-button plain @click="resetScan">
          重新扫描
        </wd-button>
        <wd-button plain @click="pickOrder">
          更换订单
        </wd-button>
      </view>
      <wd-button type="primary" block :disabled="!canShip" @click="goConfirm">
        进入发货确认
      </wd-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import type { XianyuExpressCompany, XianyuPendingShipOrder } from '@/api/rental/xianyu'
import type { RentalDevice } from '@/api/rental/device'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { computed, ref } from 'vue'
import { resolveRentalDeviceQr } from '@/api/rental/device'
import {
  getXianyuExpressCompanyList,
  getXianyuPendingShipOrderPage,
} from '@/api/rental/xianyu'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { deviceStatusLabel, staffError } from '@/models/rental/staffOperations'
import { useAccess } from '@/hooks/useAccess'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import StaffHeader from '@/components/rental/staff-header.vue'
import { useShipDraftStore } from '@/store/shipDraft'
import { useStaffExceptionStore } from '@/store/staffException'
import { extractDeviceNo, extractWaybillNo, maskOrderId, maskWaybill, normalizeCode } from '@/utils/staffScan'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const draftStore = useShipDraftStore()
const exceptions = useStaffExceptionStore()

const waybillNo = ref('')
const expressCode = ref('')
const deviceNo = ref('')
const resolvedDevice = ref<RentalDevice>()
const keyword = ref('')
const orderList = ref<XianyuPendingShipOrder[]>([])
const selectedOrder = ref<XianyuPendingShipOrder>()
const expressList = ref<XianyuExpressCompany[]>([])
const expressName = computed(() => expressList.value.find(item => item.code === expressCode.value)?.expressName || '')
const ocrConfirmed = ref(false)
const orderLoading = ref(false)
const orderError = ref('')
const expressError = ref('')
const carrierVisible = ref(false)
const manualDevice = ref('')
const manualWaybill = ref('')
const resolving = ref(false)
const orderTotal = ref(0)
const orderPage = ref(1)
let searchVersion = 0
let awaitingConfirmation = false
let targetRentalOrderId = 0
const { hasAccessByCodes } = useAccess()

const step = computed(() => {
  if (!waybillNo.value)
    return 1
  if (!deviceNo.value)
    return 2
  if (!selectedOrder.value)
    return 3
  return 4
})
const canShip = computed(() => Boolean(
  selectedOrder.value
  && resolvedDevice.value
  && !resolving.value
  && !orderLoading.value
  && !orderError.value
  && hasAccessByCodes(['rental:xianyu:ship'])
  && normalizeCode(deviceNo.value)
  && /^\w{10,}$/.test(normalizeCode(waybillNo.value))
  && expressCode.value.trim()
  && expressName.value.trim(),
))

function applyWaybill(raw: string) {
  if (raw.startsWith('CRD1|'))
    throw new Error('请扫描物流运单码')
  const value = extractWaybillNo(raw)
  if (!/^\w{10,}$/.test(value))
    throw new Error('运单号格式不正确，请重新扫描或输入')
  waybillNo.value = value
  manualWaybill.value = value
  // 只匹配快递接口返回的真实代码，不把空字符串当作任意匹配。
  expressCode.value = value.startsWith('SF')
    ? expressList.value.find(item => /顺丰/.test(item.expressName))?.code || ''
    : ''
  ocrConfirmed.value = false
}

const scanTarget = ref<'waybill' | 'device'>('waybill')

function confirmManualWaybill() {
  try {
    applyWaybill(manualWaybill.value)
    scanTarget.value = 'device'
  } catch (error) {
    toast.warning(staffError(error, '运单识别失败'))
  }
}

const { scan } = useStaffScanner(async (result) => {
  if (scanTarget.value === 'waybill') {
    if (result.text.startsWith('CRD1|'))
      throw new Error('当前等待运单码，请先扫描运单或点击设备步骤')
    applyWaybill(result.text)
    scanTarget.value = 'device'
  } else {
    await acceptDeviceScan(result.text)
  }
})

async function scanWaybill() {
  scanTarget.value = 'waybill'
  await scan()
}

async function scanDevice() {
  scanTarget.value = 'device'
  await scan()
}

async function acceptDeviceScan(raw: string) {
  if (resolving.value)
    return
  resolving.value = true
  resolvedDevice.value = undefined
  deviceNo.value = ''
  try {
    const payload = raw.trim()
    const device = await resolveRentalDeviceQr(payload.startsWith('CRD1|') ? payload : extractDeviceNo(payload))
    resolvedDevice.value = device
    deviceNo.value = device.deviceNo
    manualDevice.value = device.deviceNo
  } catch (error) {
    toast.warning(staffError(error, '设备识别失败'))
  } finally {
    resolving.value = false
  }
}

function pickOrder() {
  selectedOrder.value = undefined
  void searchOrders()
}

function resetScan() {
  waybillNo.value = ''
  deviceNo.value = ''
  resolvedDevice.value = undefined
  ocrConfirmed.value = false
  expressCode.value = ''
  selectedOrder.value = undefined
  manualDevice.value = ''
  manualWaybill.value = ''
  scanTarget.value = 'waybill'
  draftStore.clear()
}

function goConfirm() {
  if (!canShip.value || !selectedOrder.value) {
    toast.warning('请先补齐运单、设备和订单')
    return
  }
  draftStore.setDraft({
    channelOrderId: selectedOrder.value.id,
    orderNo: selectedOrder.value.externalOrderId,
    goodsTitle: selectedOrder.value.goodsTitle,
    deviceNo: normalizeCode(deviceNo.value),
    deviceStatus: resolvedDevice.value?.status,
    expressCode: expressCode.value.trim(),
    expressName: expressName.value.trim(),
    waybillNo: normalizeCode(waybillNo.value),
    ocrConfirmed: ocrConfirmed.value,
  })
  awaitingConfirmation = true
  uni.navigateTo({ url: '/pages-rental/xianyu-ship/confirm' })
}

async function searchOrders(reset = true) {
  if (!reset && orderLoading.value)
    return
  const version = ++searchVersion
  const page = reset ? 1 : orderPage.value + 1
  orderLoading.value = true
  orderError.value = ''
  if (reset) {
    orderList.value = []
    selectedOrder.value = undefined
  }
  try {
    const data = await getXianyuPendingShipOrderPage({ pageNo: page, pageSize: 20, keyword: keyword.value.trim() || undefined })
    if (version !== searchVersion)
      return
    orderList.value = reset ? data.list || [] : [...orderList.value, ...(data.list || [])]
    orderTotal.value = data.total || 0
    orderPage.value = page
    if (targetRentalOrderId) {
      selectedOrder.value = orderList.value.find(item => item.rentalOrderId === targetRentalOrderId)
      if (selectedOrder.value)
        targetRentalOrderId = 0
    }
  } catch (error) {
    if (version !== searchVersion)
      return
    orderError.value = staffError(error, '待发货订单加载失败')
    exceptions.record({ kind: 'network', title: '订单加载失败', detail: orderError.value, source: '发货提交' })
  } finally {
    if (version === searchVersion)
      orderLoading.value = false
  }
}

async function loadExpress() {
  expressError.value = ''
  try {
    expressList.value = await getXianyuExpressCompanyList()
  } catch (error) {
    expressError.value = staffError(error, '快递公司加载失败，请重试')
  }
}

onLoad((query) => {
  targetRentalOrderId = Number(query?.rentalOrderId) || 0
  keyword.value = query?.keyword ? decodeURIComponent(String(query.keyword)) : ''
  void loadExpress()
  void searchOrders()
})
onShow(() => {
  if (awaitingConfirmation && !draftStore.draft) {
    resetScan()
    void searchOrders()
  }
  awaitingConfirmation = false
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
}
.content {
  height: calc(100vh - 220rpx);
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 28rpx 24rpx;
  box-sizing: border-box;
}
.hero {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0 20rpx;
}
.muted {
  color: #6b6b6b;
  font-size: 22rpx;
}
.motto {
  font-size: 22rpx;
  font-weight: 700;
}
.step {
  display: flex;
  gap: 16rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  border: 2rpx solid #eee;
}
.step.active {
  border-color: var(--staff-accent, #e10600);
  background: var(--staff-accent-soft, #fff1f0);
}
.badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  color: #fff;
  background: #111;
  border-radius: 50%;
  font-weight: 800;
}
.step.active .badge {
  background: var(--staff-accent, #e10600);
}
.step-kicker {
  color: #6b6b6b;
  font-size: 20rpx;
}
.step-title {
  margin-top: 4rpx;
  font-size: 28rpx;
  font-weight: 800;
}
.step-value {
  margin-top: 8rpx;
  font-size: 30rpx;
  font-weight: 700;
}
.ok {
  margin-top: 6rpx;
  color: #15803d;
  font-size: 22rpx;
}
.review {
  margin-top: 8rpx;
  color: #6b6b6b;
  font-size: 22rpx;
  line-height: 1.6;
}
.arrow {
  color: #999;
  font-size: 40rpx;
}
.search {
  display: flex;
  gap: 12rpx;
  align-items: center;
  margin: 12rpx 0;
}
.search :deep(.wd-input) {
  flex: 1;
}
.order {
  padding: 16rpx;
  margin-bottom: 12rpx;
  border: 2rpx solid #eee;
}
.order.on {
  border-color: #2563eb;
  background: #eff6ff;
}
.strong {
  font-weight: 800;
}
.footer {
  padding: 12rpx 28rpx calc(12rpx + env(safe-area-inset-bottom));
  border-top: 2rpx solid #eee;
}
.row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  margin-bottom: 12rpx;
}
</style>
