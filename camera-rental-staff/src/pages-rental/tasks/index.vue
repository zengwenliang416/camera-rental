<script setup lang="ts">
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { preparationHint } from '@/models/rental/orderSearch'
import { businessToday } from '@/models/rental/mobileWorkbench'
import { getTasks } from '@/api/rental/warehouse'
import type { StaffTask, TaskQueue } from '@/api/rental/warehouse'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { formatApiDate } from '@/models/rental/orderDisplay'
import { staffError } from '@/models/rental/staffOperations'
import { maskOrderId } from '@/utils/staffScan'
import { useAccess } from '@/hooks/useAccess'

const openPage = (options: UniApp.NavigateToOptions) => uni.navigateTo(options)
definePage({ style: { navigationStyle: 'custom' } })
const staffPageStyle = useStaffPageStyle()
const { hasAccessByCodes } = useAccess()
const tabs: Array<{
  key: TaskQueue
  label: string
  shortLabel: string
}> = [
  { key: 'SHIP', label: '今日应发', shortLabel: '应发' },
  { key: 'RETURN', label: '今日回仓计划', shortLabel: '回仓' },
  { key: 'OVERDUE', label: '超期核对', shortLabel: '超期' },
  { key: 'INSPECT', label: '待检测', shortLabel: '检测' },
  { key: 'REPAIR', label: '维修复检', shortLabel: '维修' },
]
const current = ref<TaskQueue>('SHIP')
const shipScope = ref(0)
const shipScopes = ['今日应发', '逾期未发', '全部到期未发']
const requestQueue = computed<TaskQueue>(() => current.value === 'SHIP'
  ? (['SHIP_TODAY', 'SHIP_OVERDUE', 'SHIP_ALL'] as const)[shipScope.value]
  : current.value)
function changeScope(event: { detail: { value: number | string } }) {
  shipScope.value = Number(event.detail.value)
  void load()
}
const rows = ref<StaffTask[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
let page = 0
let generation = 0
async function load(reset = true) {
  if (!reset && loading.value)
    return
  const version = ++generation
  const next = reset ? 1 : page + 1
  loading.value = true
  error.value = ''
  if (reset) {
    rows.value = []
    total.value = 0
  }
  try {
    const result = await getTasks(requestQueue.value, next)
    if (version !== generation)
      return
    rows.value = reset ? result.list : [...rows.value, ...result.list]
    total.value = result.total
    page = next
  } catch (e) {
    if (version === generation)
      error.value = staffError(e, '作业加载失败')
  } finally {
    if (version === generation)
      loading.value = false
  }
}
function change(queue: TaskQueue) {
  current.value = queue
  void load()
}
function open(task: StaffTask) {
  if (current.value === 'SHIP' && task.sourceType === 'XIANYU')
    uni.navigateTo({ url: `/pages-rental/xianyu-ship/index?${task.orderId ? `rentalOrderId=${task.orderId}` : `channelOrderId=${task.channelOrderId}`}` })
  else if (current.value === 'INSPECT' || current.value === 'REPAIR')
    uni.navigateTo({ url: `/pages-rental/inspection/index?deviceId=${task.deviceId}&assignmentId=${task.assignmentId}&deviceNo=${encodeURIComponent(task.deviceNo || '')}` })
  else
    uni.navigateTo({ url: `/pages-rental/orders/detail?id=${task.orderId}` })
}
onLoad((params) => {
  if (tabs.some(tab => tab.key === params?.queue))
    current.value = params?.queue as TaskQueue
})
onShow(() => {
  void load()
})
onHide(() => {
  generation++
  loading.value = false
})
const back = () => uni.navigateBack()
</script>

<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="今日作业" left-arrow safe-area-inset-top placeholder @click-left="back" />
    <scroll-view scroll-y class="workbench-content">
      <view class="workbench-card">
        <view class="workbench-task-filters">
          <view v-for="tab in tabs" :key="tab.key">
            <wd-button size="small" :variant="current === tab.key ? 'base' : 'plain'" @click="change(tab.key)">
              {{ tab.shortLabel }}
            </wd-button>
          </view>
        </view>
        <picker v-if="current === 'SHIP'" :range="shipScopes" :value="shipScope" @change="changeScope">
          <view class="workbench-title">
            {{ shipScopes[shipScope] }} ▾
          </view>
        </picker>
        <view v-if="current === 'SHIP'" class="workbench-note">
          日期未确认的订单请到订单页查询；待核对订单需补齐资料后发货。
        </view>
        <view class="workbench-note">
          {{ businessToday() }} · 按北京时间更新
        </view>
        <view class="workbench-note">
          {{ current === 'SHIP' ? shipScopes[shipScope] : tabs.find(tab => tab.key === current)?.label }} · 共 {{ total }} {{ current === 'SHIP' ? '单' : '台' }}
          <view v-if="current === 'RETURN' || current === 'OVERDUE'">
            回仓计划含运输与检测，不代表客户承诺寄回日。
          </view>
        </view>
        <wd-button size="small" variant="plain" :loading="loading" @click="load()">
          刷新
        </wd-button>
      </view>
      <view v-if="error" class="workbench-error">
        {{ error }}
      </view>
      <view v-if="!loading && !error && !rows.length" class="workbench-card">
        {{ current === 'SHIP' && shipScope === 0 ? '今天没有计划发货订单，可切换查看逾期未发。' : '当前没有待办' }}
      </view>
      <view v-for="task in rows" :key="task.assignmentId ? `assignment-${task.assignmentId}` : task.orderId ? `order-${task.orderId}` : `channel-${task.channelOrderId}`" class="workbench-card">
        <view class="workbench-title">
          {{ task.deviceNo || maskOrderId(task.orderNo) }}
        </view>
        <view class="workbench-note">
          {{ task.goodsTitle || task.equipmentModelCode || '设备信息待核对' }} · 计划 {{ formatApiDate(task.dueDate) || '日期待确认' }}
        </view>
        <view v-if="current === 'SHIP'" class="workbench-note">
          {{ task.requiredQuantity == null ? '实际设备台数待确认' : `${task.quantityNeedsReview ? '历史台数待核对：' : '实际设备'} ${task.requiredQuantity} 台` }}
        </view>
        <view v-if="current === 'SHIP' && preparationHint(task, !task.orderId)" class="workbench-note">
          {{ preparationHint(task, !task.orderId) }}
        </view>
        <view class="workbench-action-grid">
          <wd-button v-if="current === 'SHIP' ? hasAccessByCodes(['rental:xianyu:ship']) : hasAccessByCodes(['rental:device:assign'])" size="small" @click="open(task)">
            {{ current === 'SHIP' ? task.sourceType === 'XIANYU' && task.orderId && task.preparationStatus === 'READY' ? '开始发货' : '核对订单' : current === 'INSPECT' ? '开始检测' : current === 'REPAIR' ? '维修复检' : '核对订单' }}
          </wd-button>
          <wd-button v-if="current === 'REPAIR'" size="small" variant="plain" @click="openPage({ url: `/pages-rental/issues/index?deviceId=${task.deviceId}&orderId=${task.orderId}` })">
            记录维修进度
          </wd-button>
        </view>
      </view>
      <wd-button v-if="rows.length < total" block variant="plain" :loading="loading" @click="load(false)">
        加载更多（{{ rows.length }}/{{ total }}）
      </wd-button>
    </scroll-view>
  </view>
</template>
