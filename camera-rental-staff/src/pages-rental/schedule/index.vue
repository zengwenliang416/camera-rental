<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="设备排期" left-arrow safe-area-inset-top placeholder @click-left="back" />
    <scroll-view scroll-y class="workbench-content" @scrolltolower="more">
      <view class="workbench-card">
        <wd-input v-model="keyword" placeholder="设备编号 / 型号 / 序列号" clearable @confirm="load(true)" />
        <view class="workbench-actions">
          <wd-button v-for="n in [14, 30]" :key="n" :variant="days === n ? 'base' : 'plain'" size="small" @click="changeDays(n)">
            {{ n }} 天
          </wd-button><wd-button variant="plain" size="small" @click="start = businessToday(); load(true)">
            今天
          </wd-button>
        </view>
        <picker mode="date" :value="start" @change="changeDate">
          <view class="date-picker">
            起始日期：{{ start }} · 点击修改
          </view>
        </picker>
        <wd-button block :loading="loading" @click="load(true)">
          查询排期
        </wd-button>
      </view>
      <view class="workbench-card">
        <view class="workbench-title">
          按占用日期找可用设备
        </view>
        <view class="workbench-note">
          占用开始：{{ start }}
        </view>
        <picker mode="date" :value="availableEnd" @change="availableEnd = $event.detail.value">
          <view class="date-picker">
            占用结束（含当天）：{{ availableEnd }} · 点击修改
          </view>
        </picker>
        <wd-input v-model="modelQuery" placeholder="准确型号编码，例如 P4P" />
        <wd-button size="small" :loading="searchingAvailable" :disabled="!modelQuery.trim()" @click="findAvailable">
          查找可用设备
        </wd-button>
        <view class="workbench-note">
          {{ availableHint }}
        </view>
        <view class="workbench-actions">
          <wd-button v-for="item in available" :key="item.id" size="small" variant="plain" @click="openDevice(item.deviceNo || '')">
            {{ item.deviceNo }}
          </wd-button>
        </view>
      </view>
      <view class="workbench-note">
        时间按北京时间显示。占用期包含寄送、使用及回仓检测；预计释放不代表已恢复可租。
      </view>
      <view v-if="error" class="workbench-error">
        {{ error }}<wd-button variant="plain" size="small" @click="load(true)">
          重试
        </wd-button>
      </view>
      <view v-if="!loading && !error && !rows.length" class="workbench-note">
        没有符合条件的设备
      </view>
      <view v-for="device in rows" :key="device.deviceId" class="workbench-card">
        <view class="workbench-row">
          <text class="workbench-title">{{ device.deviceNo }}</text><text>{{ deviceStatusLabel(device.deviceStatus) }}</text>
        </view>
        <view class="workbench-note">
          {{ device.equipmentModelCode }} · {{ device.warehouseCode || '仓位未登记' }}
        </view>
        <view class="timeline" aria-label="日期占用预览">
          <view v-for="date in timeline" :key="date" class="timeline-day" :class="{ occupied: occupied(device, date) }">
            {{ date.slice(-2) }}
          </view>
        </view>
        <view v-if="tightTurnaround(device)" class="workbench-error">
          相邻订单间隔不超过 1 天，请提前核对归还、检测和下一单发出。
        </view>
        <view v-if="device.expectedReleaseDate" class="workbench-note">
          预计释放：{{ formatApiDate(device.expectedReleaseDate) }}
        </view>
        <view v-if="!device.segments?.length" class="workbench-note">
          当前日期窗口无排期；仍需核对设备状态与锁定
        </view>
        <view v-for="(segment, index) in device.segments" :key="segment.scheduleId || segment.assignmentId || index" class="schedule-segment" @click="openOrder(segment.rentalOrderId)">
          <view>{{ segment.segmentType === 'PENDING_PLAN' ? '待补排期' : '设备占用' }} · {{ maskOrderId(segment.orderNo) || '未关联订单' }} {{ segment.rentalOrderId ? '›' : '' }}</view>
          <view class="workbench-note">
            {{ segment.segmentType === 'PENDING_PLAN' ? '结束日期待确认' : occupyRangeLabel(segment) }}
          </view>
          <view v-if="segment.billableStartDate" class="workbench-note">
            计租：{{ formatApiDate(segment.billableStartDate) }} 至 {{ formatApiDate(segment.billableEndDate) || '待确认' }}
          </view>
        </view>
        <wd-button variant="plain" size="small" @click="openDevice(device.deviceNo)">
          设备详情
        </wd-button>
      </view>
      <wd-button v-if="rows.length < total" variant="plain" block :loading="loading" @click="more">
        加载更多（{{ rows.length }}/{{ total }}）
      </wd-button>
      <view v-if="loading" class="workbench-note">
        正在加载排期…
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from '@dcloudio/uni-app'
import { computed, ref, watch } from 'vue'
import dayjs from 'dayjs'
import { getAvailableDevices } from '@/api/rental/warehouse'
import type { DeviceCandidate } from '@/api/rental/order'
import { getWorkbench } from '@/api/rental/workbench'
import type { DeviceLane } from '@/api/rental/workbench'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useAccess } from '@/hooks/useAccess'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import { lookupRentalDevice } from '@/api/rental/device'
import { businessToday, shiftDate } from '@/models/rental/mobileWorkbench'
import { formatApiDate, occupyRangeLabel } from '@/models/rental/orderDisplay'
import { deviceStatusLabel, staffError } from '@/models/rental/staffOperations'
import { maskOrderId } from '@/utils/staffScan'
import { setTabParams } from '@/utils/url'

definePage({
  style: {
    navigationStyle: 'custom',
  },
})
const staffPageStyle = useStaffPageStyle()
const { hasAccessByCodes } = useAccess()
const start = ref(businessToday())
const days = ref(14)
const availableEnd = ref(shiftDate(businessToday(), 6))
const modelQuery = ref('')
const available = ref<DeviceCandidate[]>([])
const availableHint = ref('')
const searchingAvailable = ref(false)
let availableGeneration = 0
watch([start, availableEnd, modelQuery], () => {
  availableGeneration++
  available.value = []
  availableHint.value = ''
})
async function findAvailable() {
  if (!modelQuery.value.trim() || searchingAvailable.value)
    return
  const version = ++availableGeneration
  searchingAvailable.value = true
  availableHint.value = ''
  try {
    const result = await getAvailableDevices(modelQuery.value.trim(), start.value, shiftDate(availableEnd.value, 1))
    if (version !== availableGeneration)
      return
    available.value = (result.candidates || []).filter(item => item.eligible)
    availableHint.value = `当前已核对 ${result.candidates?.length || 0} 台，符合条件 ${available.value.length} 台；每个型号最多核对 100 台，结果只供选机参考，实际分配时会再次检查。`
  } catch (e) {
    if (version === availableGeneration)
      availableHint.value = staffError(e, '可用设备查询失败')
  } finally {
    searchingAvailable.value = false
  }
}
const timeline = computed(() => Array.from({ length: days.value }, (_, index) => shiftDate(start.value, index)))
function occupied(device: DeviceLane, date: string) {
  return device.segments?.some(segment => segment.segmentType === 'PENDING_PLAN'
    || (date >= formatApiDate(segment.occupyStartDate) && date < formatApiDate(segment.occupyEndDateExclusive)))
}
function tightTurnaround(device: DeviceLane) {
  const ordered = (device.segments || []).filter(segment => segment.occupyStartDate && segment.occupyEndDateExclusive).slice().sort((a, b) => formatApiDate(a.occupyStartDate).localeCompare(formatApiDate(b.occupyStartDate)))
  return ordered.some((segment, index) => index > 0 && dayjs(formatApiDate(segment.occupyStartDate))
    .diff(dayjs(formatApiDate(ordered[index - 1].occupyEndDateExclusive)), 'day') <= 1)
}
const keyword = ref('')
const rows = ref<DeviceLane[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
let page = 1
let generation = 0
async function load(reset = true) {
  if (!reset && loading.value)
    return
  const version = ++generation
  if (!hasAccessByCodes(['rental:schedule:query'])) {
    loading.value = false
    rows.value = []
    error.value = '当前账号没有排期查询权限'
    return
  }
  loading.value = true
  error.value = ''
  const next = reset ? 1 : page + 1
  if (reset) {
    rows.value = []
    total.value = 0
  }
  try {
    const data = await getWorkbench({
      pageNo: next,
      pageSize: 20,
      fromDate: start.value,
      toDateExclusive: shiftDate(start.value, days.value),
      viewMode: `${days.value}D`,
      keyword: keyword.value.trim() || undefined,
    })
    if (version !== generation)
      return
    rows.value = reset ? data.devicePage.list || [] : [...rows.value, ...(data.devicePage.list || [])]
    total.value = data.devicePage.total
    page = next
  } catch (e) {
    if (version === generation)
      error.value = staffError(e, '排期获取失败')
  } finally {
    if (version === generation)
      loading.value = false
  }
}
function changeDays(n: number) {
  days.value = n
  void load(true)
}
function changeDate(e: {
  detail: {
    value: string
  }
}) {
  start.value = e.detail.value
  void load(true)
}
function more() {
  if (rows.value.length < total.value)
    void load(false)
}
function back() {
  uni.navigateBack({
    fail: () => uni.switchTab({
      url: '/pages/index/index',
    }),
  })
}
function openOrder(id?: number) {
  if (id) {
    uni.navigateTo({
      url: `/pages-rental/orders/detail?id=${id}`,
    })
  }
}
function openDevice(deviceNo: string) {
  setTabParams({
    deviceNo,
  })
  uni.switchTab({
    url: '/pages-rental/device-scan/index',
  })
}
useStaffScanner(async (scan) => {
  const device = await lookupRentalDevice(scan.text)
  keyword.value = device.deviceNo
  await load(true)
})
onLoad((query) => {
  keyword.value = String(query?.deviceNo || '')
  void load(true)
})
onUnload(() => {
  availableGeneration++
  generation++
})
</script>

<style scoped>
.date-picker {
  font-size: 26rpx;
  padding: 22rpx 0;
}
.schedule-segment {
  border-left: 5rpx solid var(--staff-accent);
  background: var(--staff-soft);
  padding: 16rpx;
  margin: 18rpx 0;
  font-size: 26rpx;
}
</style>

<style scoped>
.timeline {
  display: flex;
  overflow-x: auto;
  gap: 4rpx;
  margin: 16rpx 0;
}
.timeline-day {
  min-width: 34rpx;
  padding: 8rpx 0;
  text-align: center;
  background: var(--staff-soft);
  color: var(--staff-muted);
  font-size: 20rpx;
}
.timeline-day.occupied {
  background: var(--staff-accent-soft);
  color: var(--staff-accent-text);
}
</style>
