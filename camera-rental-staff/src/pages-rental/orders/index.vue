<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content" refresher-enabled :refresher-triggered="refreshing" @refresherrefresh="refresh" @scrolltolower="loadMore">
      <staff-header title="订单" @scanner="goScan" />

      <view class="search">
        <view class="i-carbon-search search-ico" />
        <wd-input v-model="keyword" placeholder="订单号 / 姓名 / 电话 / 地址 / 商品" no-border clearable @confirm="load()" />
        <wd-button size="small" type="primary" @click="load()">
          搜索
        </wd-button>
      </view>

      <view class="queues">
        <view v-for="tab in queues" :key="tab.value" class="queue" :class="{ active: queue === tab.value }" @click="changeQueue(tab.value)">
          <view class="queue-label">
            {{ tab.label }}
          </view>
        </view>
      </view>
      <view class="muted">
        {{ loading ? '正在查询…' : `共 ${pendingTotal} 单` }}
      </view>

      <view v-if="error" class="banner error">
        {{ error }}
      </view>
      <view v-else-if="!loading && !orders.length" class="banner">
        没有符合筛选条件的订单
      </view>

      <view class="list">
        <order-task-card
          v-for="order in visibleOrders"
          :key="order.id"
          tone="info"
          :kicker="(order.remainingQuantity ?? 0) > 0 ? `待分配 ${order.remainingQuantity} / ${order.requiredQuantity ?? 0}` : '设备已分配'"
          :tag="shippingLabel(order.shippingStatus)"
          :order-no="displayOrderNo(order)"
          :title="goodsLine(order)"
          :receiver-name="order.receiverName"
          :receiver-mobile="order.receiverMobile"
          :receiver-address="order.receiverAddress"
          show-recipient
          :subtitle="`${orderMetaLine(order)} · ${billableRangeLabel(order)}`"
          @click="openDetail(order.id)"
        />
      </view>

      <wd-button v-if="orders.length < pendingTotal" plain block :loading="loading" @click="loadMore">
        加载更多订单
      </wd-button>
      <scan-banner title="扫描设备" subtitle="查询设备状态与排期" class="cta" @click="goScan" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { getAndClearTabParams } from '@/utils/url'
import { onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getStaffOrders } from '@/api/rental/order'
import type { PendingAllocationOrder, StaffOrderQueue } from '@/api/rental/order'
import { billableRangeLabel, displayOrderNo, goodsLine, orderMetaLine, shippingLabel } from '@/models/rental/orderDisplay'
import { staffError } from '@/models/rental/staffOperations'
import OrderTaskCard from '@/components/rental/order-task-card.vue'
import ScanBanner from '@/components/rental/scan-banner.vue'
import StaffHeader from '@/components/rental/staff-header.vue'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const loading = ref(false)
const keyword = ref('')
const error = ref('')
const orders = ref<PendingAllocationOrder[]>([])
const pendingTotal = ref(0)
const visibleOrders = orders
let initialized = false

const refreshing = ref(false)
const pageNo = ref(1)
const queue = ref<StaffOrderQueue>('ALL')
const queues: Array<{ value: StaffOrderQueue, label: string }> = [
  { value: 'ALL', label: '全部' },
  { value: 'PENDING_ALLOCATION', label: '待分配' },
  { value: 'UNSHIPPED', label: '未发货' },
  { value: 'PARTIAL', label: '部分发货' },
  { value: 'SHIPPED', label: '已发货' },
]
let requestVersion = 0
async function load(reset = true) {
  if (!reset && loading.value)
    return
  const version = ++requestVersion
  const page = reset ? 1 : pageNo.value + 1
  loading.value = true
  error.value = ''
  if (reset) {
    orders.value = []
    pendingTotal.value = 0
  }
  try {
    const result = await getStaffOrders({ keyword: keyword.value.trim() || undefined, pageSize: 20, pageNo: page, queue: queue.value })
    if (version !== requestVersion)
      return
    orders.value = reset ? result.list || [] : [...orders.value, ...(result.list || [])]
    pendingTotal.value = result.total ?? orders.value.length
    pageNo.value = page
    initialized = true
  } catch (err) {
    if (version === requestVersion)
      error.value = staffError(err, '订单查询失败，请重试')
  } finally {
    if (version === requestVersion) {
      loading.value = false
      refreshing.value = false
    }
  }
}
function changeQueue(value: StaffOrderQueue) {
  queue.value = value
  void load()
}
function loadMore() {
  if (orders.value.length < pendingTotal.value)
    void load(false)
}
function refresh() {
  refreshing.value = true
  void load()
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages-rental/orders/detail?id=${id}` })
}
function goScan() {
  uni.switchTab({ url: '/pages-rental/device-scan/index' })
}
onShow(() => {
  const params = getAndClearTabParams()
  if (params?.queue && queues.some(item => item.value === params.queue)) {
    queue.value = params.queue as StaffOrderQueue
    void load()
  } else if (!initialized) {
    void load()
  } else {
    // 保留分页与滚动位置，仅更新已展示的对象。
    void refreshVisible()
  }
})
async function refreshVisible() {
  const version = ++requestVersion
  const pages = pageNo.value
  try {
    const fresh: PendingAllocationOrder[] = []
    for (let page = 1; page <= pages; page++) {
      const data = await getStaffOrders({ pageNo: page, pageSize: 20, keyword: keyword.value.trim() || undefined, queue: queue.value })
      if (version !== requestVersion)
        return
      fresh.push(...(data.list || []))
      pendingTotal.value = data.total
    }
    orders.value = fresh
    error.value = ''
  } catch (err) {
    if (version === requestVersion)
      error.value = `保留上次列表，刷新失败：${staffError(err, '请下拉重试')}`
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
}
.content {
  height: 100vh;
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 28rpx 180rpx;
  box-sizing: border-box;
}
.search {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 20rpx;
  margin: 20rpx 0 8rpx;
  border: 2rpx solid #111;
}
.search-ico {
  font-size: 32rpx;
}
.search :deep(.wd-input) {
  flex: 1;
}
.queues {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  margin: 8rpx 0 16rpx;
  border-bottom: 4rpx solid #111;
}
.queue {
  padding: 16rpx 8rpx 18rpx;
  color: #6b6b6b;
}
.queue.active {
  color: var(--staff-accent, #e10600);
  box-shadow: inset 0 -6rpx 0 var(--staff-accent, #e10600);
}
.queue-label {
  font-size: 26rpx;
  font-weight: 700;
}
.queue-count {
  margin-top: 4rpx;
  font-size: 44rpx;
  font-weight: 800;
}
.banner {
  padding: 28rpx;
  margin-bottom: 20rpx;
  color: #6b6b6b;
  background: #f5f5f5;
}
.banner.error {
  color: #b42318;
  background: #fff4f2;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.cta {
  margin-top: 24rpx;
}
</style>
