<template>
  <view class="page" :style="staffPageStyle">
    <view class="nav">
      <wd-button size="small" variant="plain" @click="goBack">
        返回
      </wd-button><text class="title">订单发货</text>
    </view>
    <scroll-view scroll-y class="content">
      <view class="card">
        <view class="section-head">
          <text>当前订单</text><wd-button v-if="!lockedOrder" variant="plain" size="small" @click="showOrders = !showOrders">
            {{ showOrders ? '收起' : selectedOrder ? '更换订单' : '选择订单' }}
          </wd-button>
        </view>
        <view v-if="orderLoading" class="muted">
          正在获取待发货订单…
        </view>
        <view v-if="selectedOrder" class="strong">
          {{ maskOrderId(selectedOrder.externalOrderId) }}
        </view>
        <view v-if="selectedOrder" class="muted">
          {{ selectedOrder.goodsTitle || '租赁设备' }}
        </view>
        <view v-if="orderDetail" class="recipient">
          <view>{{ orderDetail.receiverName || '收件人未提供' }} · {{ orderDetail.receiverMobile || '电话未提供' }}</view>
          <view>{{ orderDetail.receiverAddress || '地址未提供，请先核对订单收货信息' }}</view>
          <view class="muted">
            本单设备 {{ requiredCount }} 台 · 本次已核验 {{ scannedDevices.length }} 台
          </view>
        </view>
        <template v-if="orderDetail">
          <StaffDeviceQuantity v-for="item in orderDetail.items || []" :key="item.id" :item="item" :source-type="orderDetail.sourceType" :status="orderDetail.status" @updated="quantityUpdated" />
        </template>
        <view v-if="orderError" class="error">
          {{ orderError }}<wd-button size="small" variant="plain" @click="reloadOrder">
            重试
          </wd-button>
        </view>
        <view v-if="!selectedOrder && !orderLoading && !orderError" class="muted">
          请先选择本次发货订单
        </view>
        <view v-if="showOrders && !lockedOrder">
          <view class="search">
            <wd-input v-model="keyword" placeholder="订单号 / 商品关键词" clearable @confirm="searchOrders()" /><wd-button size="small" :loading="orderLoading" @click="searchOrders()">
              搜索
            </wd-button>
          </view>
          <view v-for="item in orderList" :key="item.id" class="order-option" :class="{ selected: selectedOrder?.id === item.id }" @click="selectOrder(item)">
            <view class="strong">
              {{ maskOrderId(item.externalOrderId) }}
            </view><view class="muted">
              {{ item.goodsTitle || '租赁设备' }} · 渠道购买数量 {{ item.goodsQuantity ?? 1 }}（计价用）
            </view>
          </view>
          <view v-if="!orderLoading && !orderList.length" class="muted">
            没有匹配的待发货订单
          </view>
          <wd-button v-if="orderList.length < orderTotal" variant="plain" block :loading="orderLoading" @click="searchOrders(false)">
            加载更多
          </wd-button>
        </view>
      </view>

      <view v-if="selectedOrder" class="scan-hint">
        {{ resolving ? '正在核验设备…' : `侧键扫描目标：${scanTarget === 'device' ? '设备永久码' : '物流运单码'}` }}
      </view>
      <view class="card">
        <view class="section-head">
          <text>1 · 核验设备</text><text v-if="resolvedDevice" class="ok">已识别</text>
        </view>
        <view v-if="resolvedDevice" class="strong">
          {{ resolvedDevice.deviceNo }} · {{ resolvedDevice.equipmentModelCode }}
        </view>
        <view v-if="resolvedDevice" class="muted">
          {{ deviceStatusLabel(resolvedDevice.status) }}
        </view>
        <view v-for="device in scannedDevices" :key="device.id" class="carrier">
          <text>{{ device.deviceNo }} · {{ device.equipmentModelCode }}</text>
          <wd-button size="small" variant="plain" @click="removeDevice(device.id)">
            移除
          </wd-button>
        </view>
        <view class="muted">
          本单全部设备使用同一张运单，扫齐后再提交。
        </view>
        <view v-if="deviceError" class="error">
          {{ deviceError }}
        </view>
        <wd-button variant="plain" block :disabled="!selectedOrder || resolving" @click="scanDevice">
          {{ scannedDevices.length ? '继续扫描设备' : '扫描设备永久码' }}
        </wd-button>
        <view class="manual-toggle" @click="manualDeviceVisible = !manualDeviceVisible">
          {{ manualDeviceVisible ? '收起手工输入' : '无法扫码？手工输入设备码' }}
        </view>
        <view v-if="manualDeviceVisible" class="manual">
          <wd-input v-model="manualDevice" placeholder="完整设备码" clearable @confirm="acceptDeviceScan(manualDevice)" /><wd-button size="small" :disabled="!selectedOrder" :loading="resolving" @click="acceptDeviceScan(manualDevice)">
            核验设备
          </wd-button>
        </view>
      </view>
      <view class="card">
        <view class="section-head">
          <text>2 · 物流运单</text><text v-if="waybillNo" class="ok">已录入</text>
        </view>
        <view v-if="waybillNo" class="strong">
          {{ maskWaybill(waybillNo) }}
        </view>
        <wd-button variant="plain" block :disabled="!selectedOrder" @click="scanWaybill">
          {{ waybillNo ? '重扫运单' : '扫描物流运单码' }}
        </wd-button>
        <view class="manual-toggle" @click="manualWaybillVisible = !manualWaybillVisible">
          {{ manualWaybillVisible ? '收起手工输入' : '无法扫码？手工输入运单号' }}
        </view>
        <view v-if="manualWaybillVisible" class="manual">
          <wd-input v-model="manualWaybill" placeholder="完整运单号" clearable @confirm="confirmManualWaybill" /><wd-button size="small" @click="confirmManualWaybill">
            确认运单
          </wd-button>
        </view>
        <view class="carrier">
          <text>快递公司</text><wd-button variant="plain" size="small" @click="carrierVisible = true">
            {{ expressName || '请选择' }} · 修改
          </wd-button>
        </view>
        <view v-if="waybillNo && !expressCode" class="muted">
          未能确定快递公司，请手动选择
        </view>
        <view v-if="expressError" class="error">
          {{ expressError }}<wd-button variant="plain" size="small" @click="loadExpress">
            重试
          </wd-button>
        </view>
        <wd-select-picker v-model="expressCode" v-model:visible="carrierVisible" type="radio" :columns="expressList" label-key="expressName" value-key="code" filterable title="物流公司" />
      </view>
    </scroll-view>
    <view class="footer">
      <view class="muted">
        {{ blocker || '信息已齐全，下一步核对收件信息并确认发货' }}
      </view><wd-button type="primary" block :disabled="!!blocker" @click="goConfirm">
        核对并发货
      </wd-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import StaffDeviceQuantity from '@/components/rental/staff-device-quantity.vue'
import type { XianyuExpressCompany, XianyuPendingShipOrder } from '@/api/rental/xianyu'
import type { RentalDevice } from '@/api/rental/device'
import type { RentalOrderScheduleDetail } from '@/api/rental/order'
import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { getOrderScheduleDetail } from '@/api/rental/order'
import { lookupRentalDevice } from '@/api/rental/device'
import { getXianyuExpressCompanyList, getXianyuPendingShipOrderPage } from '@/api/rental/xianyu'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import { useAccess } from '@/hooks/useAccess'
import { deviceStatusLabel, staffError } from '@/models/rental/staffOperations'
import { findPendingShipment, singleShipmentBlocker } from '@/models/rental/staffWorkflow'
import { useShipDraftStore } from '@/store/shipDraft'
import { extractDeviceNo, extractWaybillNo, maskOrderId, maskWaybill } from '@/utils/staffScan'

definePage({ style: { navigationStyle: 'custom' } })
const staffPageStyle = useStaffPageStyle()
const toast = useToast()
const draftStore = useShipDraftStore()
const { hasAccessByCodes } = useAccess()
const selectedOrder = ref<XianyuPendingShipOrder>()
const orderDetail = ref<RentalOrderScheduleDetail>()
const lockedOrder = ref(false)
const showOrders = ref(false)
const orderLoading = ref(false)
const orderError = ref('')
const orderList = ref<XianyuPendingShipOrder[]>([])
const keyword = ref('')
const orderTotal = ref(0)
let orderPage = 1
let searchVersion = 0
let selectionVersion = 0
let target: {
  rentalOrderId?: number
  channelOrderId?: number
} = {}
const scannedDevices = ref<RentalDevice[]>([])
const resolvedDevice = computed(() => scannedDevices.value[scannedDevices.value.length - 1])
const deviceError = ref('')
const resolving = ref(false)
const manualDevice = ref('')
const manualDeviceVisible = ref(false)
const manualWaybillVisible = ref(false)
const manualWaybill = ref('')
const waybillNo = ref('')
const expressList = ref<XianyuExpressCompany[]>([])
const expressCode = ref('')
const expressName = computed(() => expressList.value.find(item => item.code === expressCode.value)?.expressName || '')
const expressError = ref('')
const carrierVisible = ref(false)
const scanTarget = ref<'device' | 'waybill'>('device')
let awaitingConfirmation = false
const requiredCount = computed(() => orderDetail.value?.items?.reduce((n, item) => n + (item.requiredQuantity || 0), 0) || orderDetail.value?.requiredQuantity || 0)
const blocker = computed(() => {
  if (!hasAccessByCodes(['rental:xianyu:ship']))
    return '当前账号没有发货权限'
  if (orderLoading.value)
    return '正在加载订单'
  if (orderError.value)
    return orderError.value
  if (!selectedOrder.value)
    return '请先选择发货订单'
  const unsupported = singleShipmentBlocker(orderDetail.value)
  if (unsupported)
    return unsupported
  if (resolving.value)
    return '正在核验设备'
  if (scannedDevices.value.length !== requiredCount.value)
    return `应发 ${requiredCount.value} 台，已扫 ${scannedDevices.value.length} 台，请继续核验`
  if (!waybillNo.value)
    return '请扫描或输入物流运单号'
  if (!expressName.value)
    return '请选择快递公司'
  return ''
})
function removeDevice(id: number) {
  scannedDevices.value = scannedDevices.value.filter(device => device.id !== id)
  deviceError.value = ''
  scanTarget.value = 'device'
}
function goBack() {
  uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages-rental/orders/index' }) })
}
function clearScans() {
  scannedDevices.value = []
  deviceError.value = ''
  manualDevice.value = ''
  waybillNo.value = ''
  manualWaybill.value = ''
  expressCode.value = ''
  scanTarget.value = 'device'
}
async function selectOrder(item: XianyuPendingShipOrder) {
  if (selectedOrder.value?.id !== item.id && (resolvedDevice.value || waybillNo.value)) {
    const result = await new Promise<boolean>(resolve => uni.showModal({ title: '更换发货订单', content: '更换后需要重新扫描设备和运单，是否继续？', success: r => resolve(r.confirm), fail: () => resolve(false) }))
    if (!result)
      return
  }
  const version = ++selectionVersion
  if (selectedOrder.value?.id !== item.id)
    clearScans()
  selectedOrder.value = item
  orderDetail.value = undefined
  showOrders.value = false
  orderError.value = ''
  orderLoading.value = true
  try {
    if (!item.rentalOrderId)
      throw new Error('订单尚未关联租赁订单，请先在后台完成订单准备')
    const detail = await getOrderScheduleDetail(item.rentalOrderId)
    if (version !== selectionVersion)
      return
    if (detail.id !== item.rentalOrderId)
      throw new Error('订单关联不一致，请返回重新核对')
    orderDetail.value = detail
  } catch (error) {
    if (version === selectionVersion)
      orderError.value = staffError(error, '订单详情加载失败')
  } finally {
    if (version === selectionVersion)
      orderLoading.value = false
  }
}
async function quantityUpdated() {
  scannedDevices.value = []
  deviceError.value = ''
  await reloadOrder()
}
async function reloadOrder() {
  if (!lockedOrder.value) {
    if (selectedOrder.value)
      await selectOrder(selectedOrder.value)
    else
      await searchOrders()
    return
  }
  const version = ++selectionVersion
  orderLoading.value = true
  orderError.value = ''
  try {
    // 先获取已选租赁订单，使用真实渠道订单号收窄查询；仍以 ID 精确匹配。
    const detail = target.rentalOrderId ? await getOrderScheduleDetail(target.rentalOrderId) : undefined
    const item = await findPendingShipment(pageNo => getXianyuPendingShipOrderPage({ pageNo, pageSize: 100, keyword: detail?.externalOrderNo || undefined }), target)
    if (version !== selectionVersion)
      return
    await selectOrder(item)
  } catch (error) {
    if (version !== selectionVersion)
      return
    orderError.value = staffError(error, '指定订单加载失败')
    orderLoading.value = false
  }
}
async function searchOrders(reset = true) {
  if (orderLoading.value)
    return
  const version = ++searchVersion
  const page = reset ? 1 : orderPage + 1
  orderLoading.value = true
  orderError.value = ''
  try {
    const result = await getXianyuPendingShipOrderPage({ pageNo: page, pageSize: 20, keyword: keyword.value.trim() || undefined })
    if (version !== searchVersion)
      return
    orderList.value = reset ? result.list || [] : [...orderList.value, ...(result.list || [])]
    orderTotal.value = result.total
    orderPage = page
  } catch (error) {
    if (version === searchVersion)
      orderError.value = staffError(error, '待发货订单加载失败')
  } finally {
    if (version === searchVersion)
      orderLoading.value = false
  }
}
async function acceptDeviceScan(raw: string) {
  if (!selectedOrder.value) {
    toast.warning('请先选择发货订单')
    return
  }
  if (resolving.value)
    return
  const version = selectionVersion
  resolving.value = true
  deviceError.value = ''
  try {
    const payload = raw.trim()
    const device = await lookupRentalDevice(payload.startsWith('CRD1|') ? payload : extractDeviceNo(payload))
    if (version !== selectionVersion)
      return
    const unsupported = singleShipmentBlocker(orderDetail.value)
    if (unsupported)
      throw new Error(unsupported)
    if (scannedDevices.value.some(row => row.id === device.id))
      throw new Error('这台设备已经扫描，请勿重复添加')
    const matching = orderDetail.value?.items?.filter(item => item.equipmentModelCode === device.equipmentModelCode) || []
    const capacity = matching.reduce((sum, item) => sum + (item.requiredQuantity || 0), 0)
    if (scannedDevices.value.filter(row => row.equipmentModelCode === device.equipmentModelCode).length >= capacity)
      throw new Error('此型号台数已齐或型号与订单不符')
    if (device.status !== 'AVAILABLE' || !device.enabled)
      throw new Error('设备当前不可发货，请核对状态')
    scannedDevices.value = [...scannedDevices.value, device]
    manualDevice.value = ''
    scanTarget.value = scannedDevices.value.length === requiredCount.value ? 'waybill' : 'device'
  } catch (error) {
    if (version === selectionVersion)
      deviceError.value = staffError(error, '设备核验失败')
  } finally {
    resolving.value = false
  }
}
function applyWaybill(raw: string) {
  if (!selectedOrder.value)
    throw new Error('请先选择发货订单')
  if (raw.startsWith('CRD1|'))
    throw new Error('当前等待物流运单，请点击核验设备后扫描设备码')
  const value = extractWaybillNo(raw)
  if (!/^\w{10,}$/.test(value))
    throw new Error('运单号格式不正确，请重新扫描或输入')
  waybillNo.value = value
  manualWaybill.value = value
  expressCode.value = value.startsWith('SF') ? expressList.value.find(item => /顺丰/.test(item.expressName))?.code || '' : ''
}
function confirmManualWaybill() {
  try {
    applyWaybill(manualWaybill.value)
  } catch (error) {
    toast.warning(staffError(error, '运单识别失败'))
  }
}
const { scan } = useStaffScanner(async (result) => {
  if (scanTarget.value === 'device')
    await acceptDeviceScan(result.text)
  else
    applyWaybill(result.text)
})
async function scanDevice() {
  scanTarget.value = 'device'
  await scan()
}
async function scanWaybill() {
  scanTarget.value = 'waybill'
  await scan()
}
async function loadExpress() {
  expressError.value = ''
  try {
    expressList.value = await getXianyuExpressCompanyList()
    if (waybillNo.value.startsWith('SF') && !expressCode.value)
      expressCode.value = expressList.value.find(item => /顺丰/.test(item.expressName))?.code || ''
  } catch (error) {
    expressError.value = staffError(error, '快递公司获取失败，请重试')
  }
}
function goConfirm() {
  if (blocker.value || !selectedOrder.value || !resolvedDevice.value)
    return
  draftStore.setDraft({
    channelOrderId: selectedOrder.value.id,
    rentalOrderId: selectedOrder.value.rentalOrderId,
    orderNo: selectedOrder.value.externalOrderId,
    goodsTitle: selectedOrder.value.goodsTitle,
    receiverName: orderDetail.value?.receiverName,
    receiverMobile: orderDetail.value?.receiverMobile,
    receiverAddress: orderDetail.value?.receiverAddress,
    devices: scannedDevices.value.map(device => ({ id: device.id, deviceNo: device.deviceNo })),
    deviceNo: resolvedDevice.value.deviceNo,
    deviceStatus: resolvedDevice.value.status,
    expressCode: expressCode.value,
    expressName: expressName.value,
    waybillNo: waybillNo.value,
    ocrConfirmed: false,
  })
  awaitingConfirmation = true
  uni.navigateTo({ url: '/pages-rental/xianyu-ship/confirm' })
}
onLoad((query) => {
  target = { rentalOrderId: Number(query?.rentalOrderId) || undefined, channelOrderId: Number(query?.channelOrderId) || undefined }
  lockedOrder.value = !!(target.rentalOrderId || target.channelOrderId)
  showOrders.value = !lockedOrder.value
  void loadExpress()
  void reloadOrder()
})
onShow(() => {
  if (awaitingConfirmation && !draftStore.draft) {
    clearScans()
    selectedOrder.value = undefined
    orderDetail.value = undefined
    if (lockedOrder.value) {
      orderError.value = '发货已完成，请返回订单查看最新状态'
      orderLoading.value = false
    } else {
      showOrders.value = true
      void searchOrders()
    }
  }
  awaitingConfirmation = false
})
onUnload(() => {
  selectionVersion++
  searchVersion++
})
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--staff-bg);
}
.nav {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 24rpx 16rpx;
  background: var(--staff-surface);
}
.title {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--staff-ink);
}
.content {
  flex: 1;
  height: 0;
  min-height: 0;
  padding: 20rpx 24rpx;
  box-sizing: border-box;
}
.card {
  padding: 24rpx;
  margin-bottom: 20rpx;
  border: 1rpx solid var(--staff-border);
  border-radius: 20rpx;
  background: var(--staff-surface);
}
.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12rpx;
  font-size: 28rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.strong {
  font-size: 30rpx;
  font-weight: 650;
  margin: 12rpx 0;
  overflow-wrap: anywhere;
}
.muted {
  color: var(--staff-muted);
  font-size: 24rpx;
  line-height: 1.6;
}
.recipient {
  font-size: 26rpx;
  line-height: 1.7;
  margin-top: 16rpx;
}
.scan-hint {
  color: var(--staff-info);
  background: var(--staff-info-soft);
  border-radius: 12rpx;
  padding: 18rpx;
  margin-bottom: 20rpx;
  font-size: 26rpx;
}
.ok {
  color: var(--staff-success);
  font-size: 24rpx;
}
.error {
  color: var(--staff-danger);
  background: var(--staff-danger-soft);
  padding: 14rpx;
  margin: 12rpx 0;
  font-size: 24rpx;
  line-height: 1.6;
}
.manual-toggle {
  color: var(--staff-muted);
  font-size: 24rpx;
  padding: 20rpx 0 8rpx;
}
.manual {
  margin-top: 12rpx;
}
.carrier {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20rpx;
  font-size: 26rpx;
}
.search {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin: 16rpx 0;
}
.search :deep(.wd-input) {
  flex: 1;
}
.order-option {
  border: 1rpx solid var(--staff-border);
  padding: 16rpx;
  border-radius: 12rpx;
  margin: 12rpx 0;
}
.order-option.selected {
  border-color: var(--staff-accent-text);
  background: var(--staff-accent-soft, #fff1f0);
}
.footer {
  background: var(--staff-surface);
  padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid var(--staff-border);
}
.footer .muted {
  margin-bottom: 12rpx;
}
</style>
