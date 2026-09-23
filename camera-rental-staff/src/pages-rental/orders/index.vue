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
        {{ loading ? '正在查询租赁订单…' : `租赁订单 ${pendingTotal} 单` }}
      </view>

      <view v-if="error" class="banner error">
        {{ error }}
      </view>
      <view v-else-if="!loading && !channelLoading && !channelError && !orders.length && !channelOrders.length" class="banner">
        {{ queue === 'ALL' ? '未找到匹配订单。可用订单号或电话尾号重试，并核对号码是否输入正确。' : '当前状态下没有匹配订单，可切换“全部”查询。' }}
        <view v-if="queue === 'ALL'" class="muted">
          已查询已同步的渠道记录；尚未同步或未提供收件信息的订单可能无法按电话找到。
        </view>
      </view>

      <view class="list">
        <view v-for="order in visibleOrders" :key="order.id">
          <order-task-card
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
          <view v-if="preparationHint(order)" class="preparation-note">
            {{ preparationHint(order) }}
          </view>
        </view>
      </view>

      <wd-button v-if="orders.length < pendingTotal" variant="plain" block :loading="loading" @click="loadMore">
        加载更多订单
      </wd-button>
      <view v-if="queue === 'ALL'" class="channel-section">
        <view v-if="channelLoading || channelTotal" class="muted">
          {{ channelLoading ? '正在查询渠道记录…' : `尚未生成租赁订单 ${channelTotal} 单` }}
        </view>
        <view v-if="channelError" class="banner error">
          租赁订单查询与渠道查询结果分开显示。{{ channelError }}
          <wd-button size="small" variant="plain" @click="loadChannels()">
            重试渠道查询
          </wd-button>
        </view>
        <view v-for="order in channelOrders" :key="order.channelOrderId" class="channel-record">
          <order-task-card
            readonly tone="info" kicker="渠道订单 · 待核对"
            :order-no="order.externalOrderNo || ''" :title="order.goodsTitle || '渠道商品'"
            :receiver-name="order.receiverName" :receiver-mobile="order.receiverMobile"
            :receiver-address="order.receiverAddress" show-recipient
            :tag="shippingLabel(order.shippingStatus)" subtitle="仅供核对，尚未进入租赁履约"
          />
          <view class="preparation-note">
            {{ preparationHint(order, true) }}
          </view>
        </view>
        <wd-button v-if="channelOrders.length < channelTotal" block variant="plain" :loading="channelLoading" @click="loadChannels(false)">
          加载更多渠道记录
        </wd-button>
      </view>
      <scan-banner title="扫描设备" subtitle="查询设备状态与排期" class="cta" @click="goScan" />
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { getAndClearTabParams } from '@/utils/url'
import { onHide, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { getStaffChannelOrders, getStaffOrders } from '@/api/rental/order'
import type { PendingAllocationOrder, StaffChannelOrder, StaffOrderQueue } from '@/api/rental/order'
import { billableRangeLabel, displayOrderNo, goodsLine, orderMetaLine, shippingLabel } from '@/models/rental/orderDisplay'
import { staffError } from '@/models/rental/staffOperations'
import OrderTaskCard from '@/components/rental/order-task-card.vue'
import ScanBanner from '@/components/rental/scan-banner.vue'
import StaffHeader from '@/components/rental/staff-header.vue'
import { preparationHint } from '@/models/rental/orderSearch'

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
const queue = ref<StaffOrderQueue>('ALL')
const channelOrders = ref<StaffChannelOrder[]>([])
const channelTotal = ref(0)
const channelError = ref('')
const channelLoading = ref(false)
let channelPage = 0
let channelGeneration = 0
async function loadChannels(reset = true) {
  if (!reset && channelLoading.value)
    return
  const version = ++channelGeneration
  const page = reset ? 1 : channelPage + 1
  if (reset) {
    channelOrders.value = []
    channelTotal.value = 0
  }
  channelError.value = ''
  if (queue.value !== 'ALL') {
    channelLoading.value = false
    return
  }
  channelLoading.value = true
  try {
    const result = await getStaffChannelOrders({ keyword: keyword.value.trim() || undefined, pageNo: page, pageSize: 20 })
    if (version !== channelGeneration)
      return
    channelOrders.value = reset ? result.list : [...channelOrders.value, ...result.list]
    channelTotal.value = result.total
    channelPage = page
  } catch (err) {
    if (version === channelGeneration)
      channelError.value = staffError(err, '渠道订单查询失败，请重试')
  } finally {
    if (version === channelGeneration)
      channelLoading.value = false
  }
}
let initialized = false

const refreshing = ref(false)
const pageNo = ref(1)
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
  if (reset)
    void loadChannels()
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
onHide(() => {
  requestVersion++
  channelGeneration++
  loading.value = false
  channelLoading.value = false
  refreshing.value = false
})
async function refreshVisible() {
  void loadChannels()
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
  padding-top: var(--staff-status-bar-height, 0px);
  box-sizing: border-box;
  min-height: 100vh;
  background: var(--staff-surface);
}
.content {
  height: calc(100vh - var(--staff-status-bar-height, 0px));
  padding: 12rpx 28rpx 180rpx;
  box-sizing: border-box;
}
.search {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 12rpx 20rpx;
  margin: 20rpx 0 8rpx;
  border: 2rpx solid var(--staff-ink);
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
  border-bottom: 4rpx solid var(--staff-ink);
}
.queue {
  padding: 16rpx 8rpx 18rpx;
  color: var(--staff-muted);
}
.queue.active {
  color: var(--staff-accent-text);
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
  color: var(--staff-muted);
  background: var(--staff-bg);
}
.banner.error {
  color: var(--staff-danger);
  background: var(--staff-danger-soft);
}
.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.cta {
  margin-top: 24rpx;
}
.preparation-note {
  padding: 20rpx;
  color: var(--staff-danger);
  background: var(--staff-danger-soft);
  font-size: 26rpx;
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.channel-section {
  margin-top: 28rpx;
}
.channel-record {
  margin: 16rpx 0;
}
</style>
