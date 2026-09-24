<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content">
      <staff-header title="捷租达" brand @scanner="goScan" />

      <view class="home-shortcuts">
        <wd-button v-if="hasAccessByCodes(['rental:schedule:query'])" variant="plain" size="small" @click="openWorkbench('schedule')">
          设备排期
        </wd-button>
        <wd-button v-if="hasAccessByCodes(['rental:schedule:query'])" variant="plain" size="small" @click="openWorkbench('tasks')">
          今日作业
        </wd-button>
        <wd-button v-if="hasAccessByCodes(['rental:device:query'])" variant="plain" size="small" @click="openWorkbench('devices')">
          设备列表
        </wd-button>

        <wd-button v-if="hasAccessByCodes(['rental:schedule:query'])" variant="plain" size="small" @click="openWorkbench('issues')">
          异常与维修
        </wd-button>
        <wd-button v-if="hasAccessByCodes(['rental:device:query'])" variant="plain" size="small" @click="openWorkbench('stocktake')">
          扫码盘点
        </wd-button>
        <wd-button variant="plain" size="small" @click="openWorkbench('diagnostics')">
          扫码诊断
        </wd-button>
      </view>
      <view class="hero">
        <view class="hero-label">
          待分配设备
        </view>
        <view class="hero-count">
          <text class="hero-num">{{ allocationError ? '—' : pendingCount }}</text>
          <text class="hero-unit">台</text>
        </view>
      </view>

      <view v-if="allocationError || shipError" class="empty error">
        {{ allocationError || shipError }}<wd-button variant="plain" size="small" @click="load">
          重试加载
        </wd-button>
      </view>
      <view class="stats">
        <view class="stat" @click="goOrders">
          <view class="i-carbon-box stat-ico" />
          <view class="stat-label">
            待分配
          </view>
          <view class="stat-value">
            {{ allocationError ? '—' : pendingCount }}<text class="unit">台</text><text class="chev">›</text>
          </view>
        </view>
        <view class="stat" @click="goShip">
          <view class="i-carbon-delivery stat-ico" />
          <view class="stat-label">
            待发货
          </view>
          <view class="stat-value">
            {{ shipError ? '—' : shipCount }}<text class="unit">单</text><text class="chev">›</text>
          </view>
        </view>
        <view class="stat" @click="goReturn">
          <view class="i-carbon-inventory-management stat-ico" />
          <view class="stat-label">
            回仓
          </view>
          <view class="stat-value">
            <text class="unit">扫码</text><text class="chev">›</text>
          </view>
        </view>
        <view class="stat alert" @click="goExceptions">
          <view class="i-carbon-warning stat-ico" />
          <view class="stat-label">
            作业失败
          </view>
          <view class="stat-value">
            {{ errorCount }}<text class="unit">条</text><text class="chev">›</text>
          </view>
        </view>
      </view>

      <scan-banner class="cta" @click="goScan" />

      <view class="section-head">
        <text>待分配</text>
        <text class="more" @click="goOrders">
          查看待分配 ›
        </text>
      </view>
      <view v-if="urgent.length" class="task-list">
        <order-task-card
          v-for="item in urgent"
          :key="item.id"
          tone="accent"
          :kicker="dispatchByLabel(item)"
          tag="待分配"
          :order-no="displayOrderNo(item)"
          :title="goodsLine(item)"
          :subtitle="orderMetaLine(item)"
          action="去处理"
          @click="openOrder(item.id)"
          @action="openOrder(item.id)"
        />
      </view>
      <view v-else class="empty">
        暂无待分配订单
      </view>

      <view class="section-head">
        <text>待处理任务</text>
        <text class="more" @click="goShip">
          {{ followUps.length }}项 ›
        </text>
      </view>
      <view v-if="error" class="empty error">
        {{ error }}
      </view>
      <view v-else-if="followUps.length" class="task-list">
        <order-task-card
          v-for="item in followUps"
          :key="item.key"
          :tone="item.tone"
          :kicker="item.kicker"
          :tag="item.tag"
          :order-no="item.orderNo"
          :title="item.title"
          :subtitle="item.subtitle"
          :route-label="item.routeLabel"
          @click="item.open"
        />
      </view>
      <view v-else class="empty">
        暂无待处理任务
      </view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import { useAccess } from '@/hooks/useAccess'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { setTabParams } from '@/utils/url'
import { onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { getPendingAllocationOrders } from '@/api/rental/order'
import type { PendingAllocationOrder } from '@/api/rental/order'
import { getXianyuPendingShipOrderPage } from '@/api/rental/xianyu'
import type { XianyuPendingShipOrder } from '@/api/rental/xianyu'
import OrderTaskCard from '@/components/rental/order-task-card.vue'
import ScanBanner from '@/components/rental/scan-banner.vue'
import StaffHeader from '@/components/rental/staff-header.vue'
import { dispatchByLabel, displayOrderNo, goodsLine, orderMetaLine } from '@/models/rental/orderDisplay'
import { useStaffExceptionStore } from '@/store/staffException'

defineOptions({
  name: 'Home',
})
const staffPageStyle = useStaffPageStyle()
const { hasAccessByCodes } = useAccess()
function openWorkbench(page: 'schedule' | 'tasks' | 'devices' | 'issues' | 'stocktake' | 'diagnostics') {
  uni.navigateTo({ url: `/pages-rental/${page}/index` })
}
definePage({
  type: 'home',
  style: {
    navigationStyle: 'custom',
  },
})
const orders = ref<PendingAllocationOrder[]>([])
const shipOrders = ref<XianyuPendingShipOrder[]>([])
const error = ref('')
const allocationError = ref('')
const shipError = ref('')
let loadVersion = 0
const pendingCount = ref(0)
const shipCount = ref(0)
const exceptions = useStaffExceptionStore()
const errorCount = computed(() => exceptions.openItems.length)
const urgent = computed(() => orders.value.slice(0, 1))
const followUps = computed(() => {
  const rest = orders.value.slice(1, 4).map(order => ({
    key: `alloc-${order.id}`,
    tone: 'info' as const,
    kicker: `待分配 ${order.remainingQuantity ?? 0}/${order.requiredQuantity ?? 0}`,
    tag: '待分配',
    orderNo: displayOrderNo(order),
    title: goodsLine(order),
    subtitle: orderMetaLine(order),
    routeLabel: '',
    open: () => openOrder(order.id),
  }))
  const ships = shipOrders.value.slice(0, 2).map(order => ({
    key: `ship-${order.id}`,
    tone: 'accent' as const,
    kicker: '待发货',
    tag: '待发货',
    orderNo: order.externalOrderId || String(order.id),
    title: order.goodsTitle || '闲鱼待发货',
    subtitle: '渠道计价数量不代表设备台数，实际台数见订单',
    routeLabel: '',
    open: () => goShip(order.id),
  }))
  return [...rest, ...ships].slice(0, 4)
})
function queueErrorMessage(err: unknown) {
  if (err instanceof Error && err.message)
    return err.message
  if (err && typeof err === 'object' && 'msg' in err && typeof err.msg === 'string' && err.msg)
    return err.msg
  return '任务加载失败'
}
function sumRemaining(list: PendingAllocationOrder[]) {
  return list.reduce((sum, order) => sum + (order.remainingQuantity ?? 0), 0)
}
async function loadAllocationQueue() {
  const first = await getPendingAllocationOrders({ pageNo: 1, pageSize: 100 })
  const list = [...(first.list || [])]
  for (let page = 2; list.length < first.total; page++) {
    if (page > 100)
      throw new Error('待分配任务较多，请到订单列表查看')
    const next = await getPendingAllocationOrders({ pageNo: page, pageSize: 100 })
    if (!next.list?.length)
      throw new Error('任务队列发生变化，请刷新统计')
    list.push(...next.list)
  }
  return { list, total: first.total }
}
async function load() {
  const version = ++loadVersion
  error.value = ''
  allocationError.value = ''
  shipError.value = ''
  const [allocationResult, shipResult] = await Promise.allSettled([
    loadAllocationQueue(),
    getXianyuPendingShipOrderPage({ pageNo: 1, pageSize: 20 }),
  ])
  if (version !== loadVersion)
    return
  if (allocationResult.status === 'fulfilled') {
    orders.value = allocationResult.value.list || []
    pendingCount.value = sumRemaining(orders.value)
  } else {
    orders.value = []
    pendingCount.value = 0
    allocationError.value = queueErrorMessage(allocationResult.reason)
    error.value = allocationError.value
  }
  if (shipResult.status === 'fulfilled') {
    shipOrders.value = shipResult.value.list || []
    shipCount.value = shipResult.value.total ?? shipOrders.value.length
  } else {
    shipOrders.value = []
    shipCount.value = 0
    shipError.value = queueErrorMessage(shipResult.reason)
  }
}
function goOrders() {
  setTabParams({ queue: 'PENDING_ALLOCATION' })
  uni.switchTab({ url: '/pages-rental/orders/index' })
}
function goScan() {
  uni.switchTab({ url: '/pages-rental/device-scan/index' })
}
function goReturn() {
  uni.switchTab({ url: '/pages-rental/device-return/index' })
}
function goShip(channelOrderId?: number) {
  uni.navigateTo({ url: `/pages-rental/xianyu-ship/index${typeof channelOrderId === 'number' ? `?channelOrderId=${channelOrderId}` : ''}` })
}
function goExceptions() {
  uni.navigateTo({ url: '/pages-rental/exceptions/index' })
}
function openOrder(id: number) {
  uni.navigateTo({ url: `/pages-rental/orders/detail?id=${id}` })
}
onShow(() => {
  void load()
})
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
.hero {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 28rpx 0 8rpx;
}
.hero-label {
  color: var(--staff-muted);
  font-size: 24rpx;
}
.hero-count {
  display: flex;
  align-items: baseline;
  gap: 12rpx;
  margin-top: 8rpx;
}
.hero-num {
  color: var(--staff-accent-text);
  font-size: 88rpx;
  font-weight: 800;
  line-height: 1;
}
.hero-unit {
  font-size: 32rpx;
  font-weight: 800;
}
.motto {
  display: flex;
  gap: 16rpx;
  padding-top: 12rpx;
  color: var(--staff-muted);
  font-size: 20rpx;
  line-height: 1.55;
}
.motto-col {
  display: flex;
  flex-direction: column;
}
.motto-col.right {
  color: var(--staff-ink);
  font-weight: 600;
}
.motto-rule {
  width: 2rpx;
  background: var(--staff-border);
}
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin: 8rpx 0 24rpx;
  border-top: 2rpx solid var(--staff-border);
  border-bottom: 2rpx solid var(--staff-border);
}
.stat {
  padding: 20rpx 8rpx 18rpx;
  text-align: left;
}
.stat-ico {
  margin-bottom: 8rpx;
  font-size: 32rpx;
}
.stat-label {
  color: var(--staff-muted);
  font-size: 22rpx;
}
.stat-value {
  margin-top: 6rpx;
  font-size: 40rpx;
  font-weight: 800;
}
.unit {
  margin-left: 4rpx;
  font-size: 22rpx;
  font-weight: 600;
}
.chev {
  margin-left: 4rpx;
  color: var(--staff-muted);
  font-size: 28rpx;
  font-weight: 400;
}
.stat.alert,
.stat.alert .stat-label,
.stat.alert .stat-value {
  color: var(--staff-accent-text);
}
.cta {
  margin: 8rpx 0 8rpx;
}
.section-head {
  display: flex;
  justify-content: space-between;
  margin: 36rpx 0 16rpx;
  font-size: 28rpx;
  font-weight: 800;
}
.more {
  color: var(--staff-muted);
  font-size: 24rpx;
  font-weight: 500;
}
.task-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.empty {
  padding: 28rpx 8rpx;
  color: var(--staff-muted);
  font-size: 24rpx;
}
.empty.error {
  color: var(--staff-accent-text);
}
</style>
