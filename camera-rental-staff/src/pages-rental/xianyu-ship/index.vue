<template>
  <view class="yd-page-container bg-[#f5f7fb]">
    <wd-navbar
      title="扫码发货"
      left-arrow
      placeholder
      safe-area-inset-top
      fixed
      @click-left="handleBack"
    />

    <scroll-view class="min-h-0 flex-1" scroll-y>
      <view class="p-24rpx pb-180rpx">
        <view
          class="mb-24rpx rounded-20rpx bg-[#1f2937] p-28rpx text-white shadow-sm"
        >
          <view class="text-34rpx font-semibold">
            闲鱼待发货绑定
          </view>
          <view class="mt-12rpx text-26rpx text-[#d1d5db] leading-relaxed">
            扫运单、扫设备码，搜索待发货订单后确认。出库校验、设备绑定和闲管家发货由后端完成。
          </view>
        </view>

        <view class="mb-24rpx overflow-hidden rounded-16rpx bg-white shadow-sm">
          <view class="border-b border-[#edf0f5] p-24rpx">
            <view class="text-30rpx text-[#222] font-semibold">
              1. 运单信息
            </view>
            <view class="mt-8rpx text-24rpx text-[#888]">
              小标签扫不出来时，可以上传快递二维码截图识别，识别后仍需人工确认。
            </view>
          </view>
          <view class="p-24rpx">
            <view class="grid grid-cols-2 mb-20rpx gap-16rpx">
              <wd-button type="primary" block @click="scanWaybill">
                扫运单码
              </wd-button>
              <wd-button
                type="info"
                block
                :loading="ocrLoading"
                @click="chooseShipmentImage"
              >
                上传图片识别
              </wd-button>
            </view>
            <wd-cell-group border>
              <wd-cell title="运单号">
                <wd-input
                  v-model="waybillNo"
                  clearable
                  no-border
                  placeholder="请输入或扫码运单号"
                  @blur="waybillNo = normalizeCode(waybillNo)"
                />
              </wd-cell>
              <wd-cell title="快递编码">
                <wd-input
                  v-model="expressCode"
                  clearable
                  no-border
                  placeholder="如 SF"
                />
              </wd-cell>
              <wd-cell title="快递公司">
                <wd-input
                  v-model="expressName"
                  clearable
                  no-border
                  placeholder="如 顺丰速运"
                />
              </wd-cell>
            </wd-cell-group>
            <view
              v-if="ocrSummary"
              class="mt-16rpx rounded-12rpx bg-[#ecfdf3] p-20rpx text-26rpx text-[#16794c]"
            >
              {{ ocrSummary }}
            </view>
            <view
              v-if="hotExpressList.length"
              class="mt-20rpx flex flex-wrap gap-12rpx"
            >
              <view
                v-for="item in hotExpressList"
                :key="item.code"
                class="border border-[#d8dde8] rounded-full px-20rpx py-10rpx text-24rpx text-[#4b5563]"
                @click="selectExpress(item)"
              >
                {{ item.expressName }}
              </view>
            </view>
          </view>
        </view>

        <view class="mb-24rpx overflow-hidden rounded-16rpx bg-white shadow-sm">
          <view class="border-b border-[#edf0f5] p-24rpx">
            <view class="text-30rpx text-[#222] font-semibold">
              2. 设备二维码
            </view>
          </view>
          <view class="p-24rpx">
            <view class="mb-20rpx">
              <wd-button type="primary" block @click="scanDevice">
                扫机器序列号
              </wd-button>
            </view>
            <wd-cell-group border>
              <wd-cell title="设备码">
                <wd-input
                  v-model="deviceNo"
                  clearable
                  no-border
                  placeholder="请输入或扫码设备 SN/设备编号"
                  @blur="handleDeviceInputBlur"
                />
              </wd-cell>
              <wd-cell
                v-if="resolvedDevice"
                title="设备状态"
                :value="`${resolvedDevice.deviceNo} / ${resolvedDevice.status}`"
              />
              <wd-cell
                v-if="resolvedDevice?.serialNumber"
                title="设备 SN"
                :value="resolvedDevice.serialNumber"
              />
            </wd-cell-group>
          </view>
        </view>

        <view class="mb-24rpx overflow-hidden rounded-16rpx bg-white shadow-sm">
          <view class="border-b border-[#edf0f5] p-24rpx">
            <view class="text-30rpx text-[#222] font-semibold">
              3. 搜索待发货订单
            </view>
          </view>
          <view class="p-24rpx">
            <view class="mb-20rpx flex items-center gap-16rpx">
              <wd-input
                v-model="keyword"
                class="min-w-0 flex-1"
                clearable
                placeholder="订单号/商品关键词"
              />
              <wd-button
                type="primary"
                :loading="orderLoading"
                @click="searchOrders"
              >
                搜索
              </wd-button>
            </view>
            <view
              v-if="orderList.length === 0"
              class="rounded-12rpx bg-[#f7f8fa] p-28rpx text-center text-26rpx text-[#888]"
            >
              暂无待发货候选订单
            </view>
            <view
              v-for="item in orderList"
              :key="item.id"
              class="mb-16rpx border rounded-14rpx p-22rpx last:mb-0"
              :class="
                selectedOrder?.id === item.id
                  ? 'border-[#1677ff] bg-[#f0f7ff]'
                  : 'border-[#edf0f5] bg-white'
              "
              @click="selectedOrder = item"
            >
              <view
                class="mb-12rpx flex items-center justify-between gap-16rpx"
              >
                <view
                  class="min-w-0 flex-1 truncate text-28rpx text-[#222] font-semibold"
                >
                  {{ maskOrderId(item.externalOrderId) }}
                </view>
                <view
                  class="rounded-full bg-[#eef2ff] px-14rpx py-6rpx text-22rpx text-[#3854d5]"
                >
                  {{ item.orderStatus || "待发货" }}
                </view>
              </view>
              <view class="mb-10rpx text-26rpx text-[#4b5563]">
                {{ item.goodsTitle || "-" }}
              </view>
              <view
                class="flex flex-wrap gap-x-24rpx gap-y-8rpx text-24rpx text-[#777]"
              >
                <text>数量：{{ item.goodsQuantity ?? "-" }}</text>
                <text>金额：{{ formatDisplayMoney(item.payAmount) }}</text>
                <text>买家：{{ maskText(item.buyerNick) }}</text>
                <text>时间：{{ formatDateTime(item.orderTime) || "-" }}</text>
              </view>
            </view>
          </view>
        </view>

        <view class="overflow-hidden rounded-16rpx bg-white shadow-sm">
          <view class="border-b border-[#edf0f5] p-24rpx">
            <view class="text-30rpx text-[#222] font-semibold">
              4. 发货确认
            </view>
          </view>
          <wd-cell-group border>
            <wd-cell
              title="订单"
              :value="maskOrderId(selectedOrder?.externalOrderId) || '-'"
            />
            <wd-cell title="设备" :value="deviceNo || '-'" />
            <wd-cell
              title="快递"
              :value="
                expressName && expressCode
                  ? `${expressName}（${expressCode}）`
                  : '-'
              "
            />
            <wd-cell title="运单" :value="waybillNo || '-'" />
          </wd-cell-group>
        </view>
      </view>
    </scroll-view>

    <view
      class="fixed bottom-0 left-0 right-0 bg-white p-24rpx shadow-[0_-8rpx_24rpx_rgba(31,41,55,0.08)]"
    >
      <wd-button
        type="primary"
        block
        :disabled="!canShip"
        :loading="shipping"
        @click="confirmShip"
      >
        绑定设备并发货
      </wd-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type {
  XianyuExpressCompany,
  XianyuPendingShipOrder,
} from '@/api/rental/xianyu'
import type { RentalDevice } from '@/api/rental/device'
import { useDialog } from '@wot-ui/ui/components/wd-dialog'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { computed, onMounted, ref } from 'vue'
import { resolveRentalDeviceQr } from '@/api/rental/device'
import {
  getXianyuExpressCompanyList,
  getXianyuPendingShipOrderPage,
  recognizeXianyuShipmentImage,
  shipXianyuOrder,
} from '@/api/rental/xianyu'
import { formatDisplayMoney } from '@/utils/format'
import { navigateBackPlus } from '@/utils'
import { formatDateTime } from '@/utils/date'

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const dialog = useDialog()

const waybillNo = ref('')
const expressCode = ref('')
const expressName = ref('')
const deviceNo = ref('')
const resolvedDevice = ref<RentalDevice>()
const keyword = ref('')
const orderList = ref<XianyuPendingShipOrder[]>([])
const selectedOrder = ref<XianyuPendingShipOrder>()
const expressList = ref<XianyuExpressCompany[]>([])
const ocrSummary = ref('')
const ocrConfirmed = ref(false)
const ocrLoading = ref(false)
const orderLoading = ref(false)
const shipping = ref(false)

const hotExpressList = computed(() =>
  expressList.value.filter(item => item.hot).slice(0, 8),
)
const canShip = computed(() => {
  return Boolean(
    selectedOrder.value
    && normalizeCode(deviceNo.value)
    && normalizeCode(waybillNo.value).length >= 10
    && expressCode.value.trim()
    && expressName.value.trim(),
  )
})

function handleBack() {
  navigateBackPlus()
}

function normalizeCode(value?: string) {
  return String(value || '')
    .trim()
    .replace(/\s+/g, '')
    .toUpperCase()
}

function maskText(value?: string) {
  const text = String(value || '').trim()
  if (!text) {
    return '-'
  }
  if (text.length <= 2) {
    return `${text[0]}*`
  }
  return `${text.slice(0, 1)}***${text.slice(-1)}`
}

function maskOrderId(value?: string) {
  const text = String(value || '').trim()
  if (!text) {
    return ''
  }
  return text.length <= 10 ? '***' : `${text.slice(0, 6)}***${text.slice(-4)}`
}

function extractQueryValue(raw: string, keys: string[]) {
  for (const key of keys) {
    const matched = raw.match(new RegExp(`[?&]${key}=([^&#]+)`, 'i'))
    if (matched?.[1]) {
      return decodeURIComponent(matched[1])
    }
  }
  return ''
}

function extractJsonValue(raw: string, keys: string[]) {
  try {
    const json = JSON.parse(raw) as Record<string, unknown>
    for (const key of keys) {
      const value = json[key]
      if (value !== undefined && value !== null) {
        return String(value)
      }
    }
  } catch {
    return ''
  }
  return ''
}

function extractDeviceNo(raw: string) {
  const queryValue = extractQueryValue(raw, [
    'deviceNo',
    'device_no',
    'serialNo',
    'serial_no',
    'sn',
  ])
  const jsonValue = extractJsonValue(raw, [
    'deviceNo',
    'device_no',
    'serialNo',
    'serial_no',
    'sn',
  ])
  return normalizeCode(queryValue || jsonValue || raw)
}

async function resolveScannedDevice(raw: string) {
  const payload = String(raw || '').trim()
  if (!payload) {
    return
  }
  if (payload.startsWith('CRD1|')) {
    const device = await resolveRentalDeviceQr(payload)
    resolvedDevice.value = device
    deviceNo.value = device.deviceNo
    return
  }
  resolvedDevice.value = undefined
  deviceNo.value = extractDeviceNo(payload)
}

async function handleDeviceInputBlur() {
  const value = deviceNo.value
  resolvedDevice.value = undefined
  deviceNo.value = normalizeCode(value)
}

function extractWaybillNo(raw: string) {
  const text = normalizeCode(raw)
  const sfMatched = text.match(/SF[A-Z0-9]{10,}/)
  if (sfMatched?.[0]) {
    return sfMatched[0]
  }
  const candidates = text.match(/[A-Z0-9]{10,}/g) || []
  return candidates.find(item => /\d{8,}/.test(item)) || text
}

function inferExpressByWaybill(code: string) {
  if (!code && !waybillNo.value.startsWith('SF') && !expressName.value) {
    return
  }
  const matched = expressList.value.find((item) => {
    const values = [item.code, item.expressName, item.expressAlias]
      .filter(Boolean)
      .join(' ')
      .toUpperCase()
    return (
      values.includes(code.toUpperCase())
      || (waybillNo.value.startsWith('SF') && values.includes('顺丰'))
      || (!!expressName.value && values.includes(expressName.value.toUpperCase()))
    )
  })
  if (matched) {
    selectExpress(matched)
  } else if (waybillNo.value.startsWith('SF') && !expressCode.value) {
    expressCode.value = 'SF'
    expressName.value = '顺丰速运'
  } else if (expressName.value.includes('顺丰') && !expressCode.value) {
    expressCode.value = 'SF'
  }
}

function selectExpress(item: XianyuExpressCompany) {
  expressCode.value = item.code
  expressName.value = item.expressName
}

async function scanWaybill() {
  try {
    const res = await uni.scanCode({ scanType: ['barCode', 'qrCode'] })
    waybillNo.value = extractWaybillNo(res.result)
    inferExpressByWaybill('')
    ocrConfirmed.value = false
  } catch {
    toast.warning('未获取到运单码')
  }
}

async function scanDevice() {
  try {
    const res = await uni.scanCode({ scanType: ['qrCode', 'barCode'] })
    await resolveScannedDevice(res.result)
  } catch (error) {
    const message = error instanceof Error ? error.message : '未获取到设备码'
    toast.warning(message)
  }
}

async function chooseShipmentImage() {
  try {
    const chooseResult = await uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
    })
    const filePath = chooseResult.tempFilePaths[0]
    if (!filePath) {
      return
    }
    ocrLoading.value = true
    const result = await recognizeXianyuShipmentImage(filePath)
    if (result.waybillNo) {
      waybillNo.value = extractWaybillNo(result.waybillNo)
    }
    if (result.expressCode) {
      expressCode.value = normalizeCode(result.expressCode)
    }
    if (result.expressName) {
      expressName.value = result.expressName
    }
    inferExpressByWaybill(result.expressCode || result.expressName || '')
    ocrConfirmed.value = true
    const confidenceText
      = result.confidence == null ? '' : `，置信度 ${result.confidence}`
    ocrSummary.value = `已识别：${waybillNo.value || '-'} ${expressName.value || ''}${confidenceText}`
  } catch (error) {
    const message = error instanceof Error ? error.message : '发货图片识别失败'
    toast.error(message)
  } finally {
    ocrLoading.value = false
  }
}

async function searchOrders() {
  orderLoading.value = true
  selectedOrder.value = undefined
  try {
    const data = await getXianyuPendingShipOrderPage({
      pageNo: 1,
      pageSize: 20,
      keyword: keyword.value.trim() || undefined,
    })
    orderList.value = data.list || []
    if (orderList.value.length === 1) {
      selectedOrder.value = orderList.value[0]
    }
  } finally {
    orderLoading.value = false
  }
}

function createIdempotencyKey() {
  return `staff-ship-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
}

async function confirmShip() {
  if (!canShip.value || !selectedOrder.value) {
    toast.warning('请先补齐运单、设备和订单')
    return
  }
  try {
    await dialog.confirm({
      title: '确认发货',
      msg: `订单 ${maskOrderId(selectedOrder.value.externalOrderId)}\n设备 ${normalizeCode(deviceNo.value)}\n运单 ${normalizeCode(waybillNo.value)}`,
    })
  } catch {
    return
  }
  shipping.value = true
  try {
    const result = await shipXianyuOrder({
      channelOrderId: selectedOrder.value.id,
      deviceNo: normalizeCode(deviceNo.value),
      idempotencyKey: createIdempotencyKey(),
      expressCode: normalizeCode(expressCode.value),
      expressName: expressName.value.trim(),
      waybillNo: normalizeCode(waybillNo.value),
      source: 'STAFF',
      ocrConfirmed: ocrConfirmed.value,
    })
    toast.success(
      `发货成功：${result.maskedWaybillNo || normalizeCode(waybillNo.value)}`,
    )
    orderList.value = orderList.value.filter(
      item => item.id !== selectedOrder.value?.id,
    )
    selectedOrder.value = undefined
    waybillNo.value = ''
    deviceNo.value = ''
    resolvedDevice.value = undefined
    ocrSummary.value = ''
    ocrConfirmed.value = false
  } finally {
    shipping.value = false
  }
}

async function loadExpressCompanies() {
  try {
    expressList.value = await getXianyuExpressCompanyList()
    inferExpressByWaybill('')
  } catch {
    expressList.value = []
  }
}

onMounted(() => {
  loadExpressCompanies()
  searchOrders()
})
</script>
