<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content" :scroll-into-view="timelineAnchor">
      <view class="head">
        <view>
          <view class="title">
            设备查询
          </view>
          <view class="muted">
            快速查询设备状态与排期
          </view>
        </view>
        <view class="scan-btn" @click="scanDevice">
          <view class="i-carbon-scan" />
          <text>{{ scannerConnected ? '扫码头扫码' : '摄像头扫码' }}</text>
        </view>
      </view>

      <view class="search">
        <wd-input v-model="manualCode" placeholder="输入完整设备码或签名二维码" clearable />
        <wd-button type="primary" :disabled="!manualCode.trim() || loading" @click="resolve(manualCode)">
          查询
        </wd-button>
      </view>

      <view v-if="error" class="error">
        {{ error }}
      </view>

      <view v-if="loading" class="muted">
        正在查询设备…
      </view>
      <view v-if="scheduleError" class="error">
        {{ scheduleError }}<wd-button plain size="small" @click="retrySchedule">
          重试排期查询
        </wd-button>
      </view>
      <template v-if="device">
        <view class="hero">
          <view>
            <view class="device-no">
              {{ device.deviceNo }}
            </view>
            <view class="muted">
              {{ device.equipmentModelCode }}
            </view>
          </view>
          <view class="status">
            {{ statusLabel(device.status) }}
          </view>
        </view>

        <view class="metrics">
          <view v-if="device.warehouseCode">
            <view class="muted">
              仓位编码
            </view>
            <view class="strong">
              {{ device.warehouseCode }}
            </view>
          </view>
          <view>
            <view class="muted">
              当前订单
            </view>
            <view class="strong">
              {{ assignmentOrder || '-' }}
            </view>
          </view>
          <view>
            <view class="muted">
              占用时间
            </view>
            <view class="strong">
              {{ occupyText }}
            </view>
          </view>
        </view>

        <view class="section-head">
          设备状态
        </view>
        <view class="flags">
          <view class="flag">
            <view class="dot" :class="device.enabled ? 'on' : 'off'" />
            {{ device.enabled ? '已启用' : '已停用' }}
          </view>
          <view class="flag">
            <view class="dot" :class="scheduleLoaded && !hasLock ? 'on' : 'off'" />
            {{ !scheduleLoaded ? '锁定状态未获取' : hasLock ? '存在有效锁定' : '无有效锁定' }}
          </view>
          <view class="flag">
            <view class="dot" :class="device.inspectionState === 'PASSED' ? 'on' : 'off'" />
            {{ inspectionStateLabel(device.inspectionState) }}
          </view>
        </view>

        <view id="device-timeline" class="section-head">
          完整设备排期
        </view>
        <view v-if="scheduleLoaded && !timeline.length" class="muted">
          当前没有排期记录
        </view>
        <view v-for="(row, index) in timeline" :key="index" class="tl">
          <view class="tl-dot" />
          <view class="flex-1">
            <view class="strong">
              {{ row.title }}
            </view>
            <view class="muted">
              {{ row.sub }}
            </view>
          </view>
          <view class="muted">
            {{ row.when }}
          </view>
        </view>
      </template>
    </scroll-view>
    <view v-if="device" class="footer">
      <wd-button v-if="device.currentAssignment?.rentalOrderId" plain @click="goOrders">
        查看订单
      </wd-button>
      <wd-button type="primary" @click="toastTimeline">
        定位完整排期
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { onShow } from '@dcloudio/uni-app'
import { getAndClearTabParams } from '@/utils/url'
import { deviceStatusLabel, inspectionStateLabel } from '@/models/rental/staffOperations'
import { computed, ref } from 'vue'
import { getDeviceScheduleDetail, lookupRentalDevice } from '@/api/rental/device'
import type { RentalDeviceScheduleDetail } from '@/api/rental/device'
import { formatMonthDay, occupyRangeLabel } from '@/models/rental/orderDisplay'
import { useStaffExceptionStore } from '@/store/staffException'
import { useStaffScanner } from '@/hooks/useStaffScanner'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const scheduleLoaded = ref(false)
const scheduleError = ref('')
const timelineAnchor = ref('')
const loading = ref(false)
const manualCode = ref('')
const error = ref('')
const device = ref<RentalDeviceScheduleDetail>()
const exceptions = useStaffExceptionStore()

const hasLock = computed(() => Boolean(device.value?.activeLocks?.length))
const assignmentOrder = computed(() => {
  const id = device.value?.currentAssignment?.rentalOrderId
  return id ? `订单 #${id}` : ''
})
const occupyText = computed(() => occupyRangeLabel({
  occupyStartDate: device.value?.currentAssignment?.occupyStartDate,
  occupyEndDateExclusive: device.value?.currentAssignment?.occupyEndDateExclusive,
}))
const timeline = computed(() => {
  const rows = (device.value?.schedules || []).map(item => ({
    title: item.label || item.status || '排期',
    sub: occupyRangeLabel(item),
    when: formatMonthDay(item.occupyStartDate),
  }))
  if (device.value?.currentAssignment) {
    rows.unshift({
      title: '当前分配',
      sub: assignmentOrder.value || device.value.currentAssignment.status || '',
      when: formatMonthDay(device.value.currentAssignment.occupyStartDate),
    })
  }
  return rows
})

const statusLabel = deviceStatusLabel

async function retrySchedule() {
  if (!device.value)
    return
  const requestedDeviceId = device.value.id
  scheduleLoaded.value = false
  scheduleError.value = ''
  try {
    const detail = await getDeviceScheduleDetail(requestedDeviceId)
    if (device.value?.id !== requestedDeviceId)
      return
    device.value = detail
    scheduleLoaded.value = true
  } catch {
    if (device.value?.id !== requestedDeviceId)
      return
    scheduleError.value = '设备基本信息已获取，排期、锁定和检测状态暂未获取，不能据此判断可用。'
  }
}

async function resolve(payload: string) {
  const value = payload.trim()
  if (!value || loading.value)
    return
  loading.value = true
  error.value = ''
  device.value = undefined
  scheduleLoaded.value = false
  scheduleError.value = ''
  try {
    const basic = await lookupRentalDevice(value)
    device.value = { ...basic }
    await retrySchedule()
    manualCode.value = device.value.deviceNo
  } catch (err) {
    device.value = undefined
    error.value = err instanceof Error ? err.message : '设备查询失败，请重试'
    exceptions.record({
      kind: 'scan',
      title: '设备查询失败',
      detail: error.value,
      source: '扫码入库',
    })
  } finally {
    loading.value = false
  }
}

const { scan: scanDevice, scannerConnected } = useStaffScanner(result => resolve(result.text))

function goOrders() {
  const id = device.value?.currentAssignment?.rentalOrderId
  if (id)
    uni.navigateTo({ url: `/pages-rental/orders/detail?id=${id}` })
}

function toastTimeline() {
  timelineAnchor.value = ''
  setTimeout(() => {
    timelineAnchor.value = 'device-timeline'
  }, 0)
}
onShow(() => {
  const params = getAndClearTabParams()
  if (params?.deviceNo)
    void resolve(params.deviceNo)
})
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.content {
  flex: 1;
  min-height: 0;
  height: 0;
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 28rpx 24rpx;
  box-sizing: border-box;
}
.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.title {
  font-size: 48rpx;
  font-weight: 800;
}
.scan-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 22rpx;
  font-weight: 700;
}
.search {
  display: flex;
  gap: 12rpx;
  align-items: center;
  margin: 20rpx 0;
}
.search :deep(.wd-input) {
  flex: 1;
  border: 2rpx solid #111;
  padding: 8rpx 12rpx;
}
.hero {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 2rpx solid #eee;
}
.device-no {
  font-size: 48rpx;
  font-weight: 800;
}
.status {
  color: var(--staff-accent, #e10600);
  font-size: 28rpx;
  font-weight: 800;
}
.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12rpx;
  padding: 20rpx 0;
}
.muted {
  color: #6b6b6b;
  font-size: 22rpx;
}
.strong {
  margin-top: 6rpx;
  font-size: 26rpx;
  font-weight: 800;
}
.section-head {
  margin: 20rpx 0 12rpx;
  font-weight: 800;
}
.flags {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 12rpx;
}
.flag {
  padding: 16rpx;
  border: 2rpx solid #eee;
  font-size: 22rpx;
}
.dot {
  display: inline-block;
  width: 12rpx;
  height: 12rpx;
  margin-right: 8rpx;
  border-radius: 50%;
  background: #d1d5db;
}
.dot.on {
  background: #16a34a;
}
.dot.off {
  background: var(--staff-accent, #e10600);
}
.tl {
  display: flex;
  gap: 12rpx;
  padding: 12rpx 0;
}
.tl-dot {
  width: 16rpx;
  height: 16rpx;
  margin-top: 10rpx;
  border-radius: 50%;
  background: var(--staff-accent, #e10600);
}
.error {
  padding: 16rpx;
  color: var(--staff-accent, #e10600);
  background: var(--staff-accent-soft, #fff1f0);
}
.footer {
  margin-bottom: 110rpx;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  padding: 12rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
}
</style>
