<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="今日作业" left-arrow safe-area-inset-top placeholder @click-left="back" />
    <scroll-view scroll-y class="workbench-content">
      <view class="workbench-card">
        <view class="workbench-title">
          {{ today }} · 北京时间
        </view>
        <view class="workbench-actions">
          <wd-button v-for="item in tabs" :key="item.key" size="small" :variant="current === item.key ? 'base' : 'plain'" @click="changeTab(item.key)">
            {{ item.label }}
          </wd-button>
        </view>
        <view class="workbench-note">
          {{ current === 'SHIP' ? '今天及此前计划发出、仍未全部发货的订单。' : current === 'INSPECT' ? '物流已标记回仓，但尚未完成检测的设备。' : '以设备占用结束日（含回仓检测）作为计划日期，仅列出仍有出库设备的订单；不代表客户承诺的寄回日。' }}
        </view>
        <wd-button variant="plain" size="small" :loading="loading" @click="load(true)">
          刷新
        </wd-button>
      </view>
      <view v-if="loading" class="workbench-note">
        {{ progress }}
      </view>
      <view v-if="error" class="workbench-error">
        {{ error }}
      </view>
      <view v-if="!loading && !error" class="workbench-note">
        已加载 {{ tasks.length }} {{ current === 'INSPECT' ? '台' : '单' }} · {{ updatedAt }} 更新
      </view>
      <view v-if="!loading && !error && !tasks.length" class="workbench-card">
        {{ hasMore ? '已核对的订单中暂无匹配作业，可继续加载下一批' : '当前没有匹配的作业' }}
      </view>
      <view v-for="item in tasks" :key="item.key" class="workbench-card">
        <view class="workbench-title">
          {{ item.title }}
        </view>
        <view class="workbench-note">
          {{ item.subtitle }}
        </view>
        <view class="workbench-actions">
          <wd-button v-if="item.orderId" variant="plain" size="small" @click="openOrder(item.orderId)">
            查看订单
          </wd-button><wd-button v-if="item.deviceNo" variant="plain" size="small" @click="openDevice(item.deviceNo)">
            查询设备
          </wd-button>
        </view>
      </view>
      <view v-if="!loading" class="workbench-note">
        {{ progress }}
      </view>
      <wd-button v-if="hasMore" variant="plain" block :loading="loading" @click="load(false)">
        继续加载下一批
      </wd-button>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import {
  onHide,
  onShow,
} from '@dcloudio/uni-app'
import { ref } from 'vue'
import {
  getOrderScheduleDetail,
  getStaffOrders,
} from '@/api/rental/order'
import { getWorkbench } from '@/api/rental/workbench'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useAccess } from '@/hooks/useAccess'
import {
  businessToday,
  matchesDailyPlan,
  shiftDate,
} from '@/models/rental/mobileWorkbench'
import {
  displayOrderNo,
  formatApiDate,
  goodsLine,
} from '@/models/rental/orderDisplay'
import { staffError } from '@/models/rental/staffOperations'
import { maskOrderId } from '@/utils/staffScan'
import { setTabParams } from '@/utils/url'
import type { PendingAllocationOrder } from '@/api/rental/order'

definePage({
  style: {
    navigationStyle: 'custom',
  },
})
type Queue = 'SHIP' | 'RETURN' | 'OVERDUE' | 'INSPECT'
interface Task {
  key: string
  title: string
  subtitle: string
  orderId?: number
  deviceNo?: string
}
const staffPageStyle = useStaffPageStyle()
const {
  hasAccessByCodes,
} = useAccess()
const tabs: Array<{
  key: Queue
  label: string
}> = [{
  key: 'SHIP',
  label: '今日应发',
}, {
  key: 'RETURN',
  label: '今日回仓计划',
}, {
  key: 'OVERDUE',
  label: '占用超期核对',
}, {
  key: 'INSPECT',
  label: '待检测',
}]
const current = ref<Queue>('SHIP')
const today = ref(businessToday())
const loading = ref(false)
const error = ref('')
const progress = ref('正在加载…')
const tasks = ref<Task[]>([])
const updatedAt = ref('')
const hasMore = ref(false)
let loadedPage = 0
let loadedDate = ''
let generation = 0
function changeTab(value: Queue) {
  current.value = value
  void load(true)
}
async function load(reset = true) {
  if (!reset && loading.value)
    return
  const version = ++generation
  today.value = businessToday()
  const date = today.value
  const queue = current.value
  reset = reset || loadedDate !== date
  if (reset) {
    tasks.value = []
    loadedPage = 0
    hasMore.value = false
  }
  const page = reset ? 1 : loadedPage + 1
  const pageSize = 20
  error.value = ''
  if (!hasAccessByCodes(['rental:schedule:query'])) {
    loading.value = false
    error.value = '当前账号没有作业查询权限'
    return
  }
  loading.value = true
  progress.value = '正在加载任务，请稍候…'
  try {
    const found = new Map<string, Task>(tasks.value.map(task => [task.key, task]))
    let total = 0
    if (queue === 'INSPECT') {
      const data = await getWorkbench({
        pageNo: page,
        pageSize,
        fromDate: date,
        toDateExclusive: shiftDate(date, 14),
        viewMode: '14D',
        logisticsStatus: 'RETURNED_PENDING_INSPECTION',
      })
      if (version !== generation)
        return
      for (const device of data.devicePage.list || []) {
        found.set(`device-${device.deviceId}`, {
          key: `device-${device.deviceId}`,
          title: device.deviceNo,
          subtitle: `${device.equipmentModelCode} · 已回仓待检测`,
          deviceNo: device.deviceNo,
        })
      }
      total = data.devicePage.total
      if (!data.devicePage.list?.length && (page - 1) * pageSize < total)
        throw new Error('队列发生变化，请刷新后重试')
    } else {
      const data = await getStaffOrders({
        pageNo: page,
        pageSize,
        queue: 'ALL',
      })
      if (version !== generation)
        return
      const candidates = (data.list || []).filter(order => matchesDailyPlan(order, queue, date))
      for (let offset = 0;
        offset < candidates.length;
        offset += 4) {
        if (version !== generation)
          return
        const verified = await Promise.all(candidates.slice(offset, offset + 4).map(async (order) => {
          if (queue === 'SHIP')
            return order
          // 分配返回记录是实际回仓依据，不能仅凭订单日期显示“未回”。
          const detail = await getOrderScheduleDetail(order.id)
          return detail.items?.some(item => item.assignments?.some(a => ['DISPATCHED', 'DISPATCHED_PENDING_PLAN'].includes(a.status))) ? order : undefined
        }))
        if (version !== generation)
          return
        for (const order of verified.filter((o): o is PendingAllocationOrder => !!o)) {
          const due = queue === 'SHIP' ? formatApiDate(order.occupyStartDate) : shiftDate(formatApiDate(order.occupyEndDateExclusive), -1)
          found.set(`order-${order.id}`, {
            key: `order-${order.id}`,
            title: maskOrderId(displayOrderNo(order)),
            subtitle: `${goodsLine(order)} · ${queue === 'SHIP' ? '计划发出' : '计划完成回仓'} ${due}`,
            orderId: order.id,
          })
        }
      }
      total = data.total
      if (!data.list?.length && (page - 1) * pageSize < total)
        throw new Error('订单队列发生变化，请刷新后重试')
    }
    if (version !== generation)
      return
    loadedPage = page
    loadedDate = date
    hasMore.value = page * pageSize < total
    progress.value = `已核对 ${Math.min(page * pageSize, total)} / ${total} ${queue === 'INSPECT' ? '台设备' : '单订单'}${hasMore.value ? '，当前不是全部结果，请继续加载' : '，已全部核对'}`
    tasks.value = Array.from(found.values())
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', {
      timeZone: 'Asia/Shanghai',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch (e) {
    if (version === generation) {
      error.value = staffError(e, '今日作业加载失败，请重试')
    }
  } finally {
    if (version === generation)
      loading.value = false
  }
}
function back() {
  uni.navigateBack({
    fail: () => uni.switchTab({
      url: '/pages/index/index',
    }),
  })
}
function openOrder(id: number) {
  uni.navigateTo({
    url: `/pages-rental/orders/detail?id=${id}`,
  })
}
function openDevice(deviceNo: string) {
  setTabParams({
    deviceNo,
  })
  uni.switchTab({
    url: '/pages-rental/device-scan/index',
  })
}
onShow(() => {
  void load(true)
})
onHide(() => {
  generation++
  loading.value = false
})
</script>
