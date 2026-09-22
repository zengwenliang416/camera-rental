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
import {
  onLoad,
  onUnload,
} from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getWorkbench } from '@/api/rental/workbench'
import type { DeviceLane } from '@/api/rental/workbench'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useAccess } from '@/hooks/useAccess'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import { lookupRentalDevice } from '@/api/rental/device'
import {
  businessToday,
  shiftDate,
} from '@/models/rental/mobileWorkbench'
import {
  formatApiDate,
  occupyRangeLabel,
} from '@/models/rental/orderDisplay'
import {
  deviceStatusLabel,
  staffError,
} from '@/models/rental/staffOperations'
import { maskOrderId } from '@/utils/staffScan'
import { setTabParams } from '@/utils/url'

definePage({
  style: {
    navigationStyle: 'custom',
  },
})
const staffPageStyle = useStaffPageStyle()
const {
  hasAccessByCodes,
} = useAccess()
const keyword = ref('')
const start = ref(businessToday())
const days = ref(14)
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
