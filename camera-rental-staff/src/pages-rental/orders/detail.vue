<template>
  <view class="page" :style="staffPageStyle">
    <view class="nav">
      <view class="back" @click="goBack">
        ‹
      </view>
      <view class="nav-title">
        订单详情
      </view>
    </view>
    <scroll-view scroll-y class="content" :scroll-into-view="detailAnchor">
      <view v-if="loading" class="empty">
        正在加载订单...
      </view>
      <view v-else-if="error" class="error">
        {{ error }}
      </view>
      <template v-else>
        <view class="status-row">
          <view>
            <view class="status-title">
              {{ orderStatusLabel(detail?.status) }}
            </view>
            <view class="status-sub">
              请在占用开始前完成分配与发出
            </view>
          </view>
          <view class="sla">
            <view class="sla-kicker">
              占用开始
            </view>
            <view class="sla-time">
              {{ formatMonthDay(detail?.occupyStartDate) || '待确认' }}
            </view>
            <view class="sla-kicker">
              前发出
            </view>
          </view>
        </view>

        <view class="ids">
          <view>
            <view class="id-label">
              订单编号
            </view>
            <view class="id-value">
              {{ displayOrderNo(detail || {}) }}
            </view>
          </view>
          <view>
            <view class="id-label">
              订单来源
            </view>
            <view class="id-value">
              {{ sourceTypeLabel(detail?.sourceType) || '-' }}
            </view>
          </view>
        </view>
        <view v-if="detail?.orderNo && detail.orderNo !== displayOrderNo(detail)" class="ext">
          内部单号 {{ detail.orderNo }}
        </view>

        <view class="block">
          <view class="block-title">
            客户信息
          </view>
          <view v-if="!hasCustomer" class="muted">
            该单没有可展示的收货人信息
          </view>
          <view v-else class="split">
            <view>
              <view class="muted">
                收货人
              </view>
              <view class="strong">
                {{ displayReceiverName(detail || {}) || '-' }}
              </view>
            </view>
            <view>
              <view class="muted">
                手机
              </view>
              <view class="strong">
                {{ detail?.receiverMobile || '-' }}
              </view>
            </view>
          </view>
          <view v-if="detail?.buyerNick && detail.buyerNick !== detail.receiverName" class="line">
            <view class="flex-1">
              <view class="muted">
                闲鱼买家
              </view>
              <view class="strong">
                {{ detail.buyerNick }}
              </view>
            </view>
          </view>
          <view v-if="detail?.receiverAddress" class="line">
            <view class="flex-1">
              <view class="muted">
                收货地址
              </view>
              <view class="strong">
                {{ detail.receiverAddress }}
              </view>
            </view>
          </view>
        </view>

        <view class="workbench-actions">
          <wd-button v-if="detail?.receiverMobile" variant="plain" size="small" @click="callReceiver(detail.receiverMobile)">
            拨打电话
          </wd-button>
          <wd-button v-if="detail?.receiverMobile" variant="plain" size="small" @click="copyStaffField(detail.receiverMobile)">
            复制电话
          </wd-button>
          <wd-button v-if="detail?.receiverAddress" variant="plain" size="small" @click="copyStaffField(detail.receiverAddress)">
            复制地址
          </wd-button>
          <wd-button v-if="detail?.externalOrderNo || detail?.orderNo" variant="plain" size="small" @click="copyStaffField(detail.externalOrderNo || detail.orderNo)">
            复制订单号
          </wd-button>
        </view>
        <view id="order-devices" />
        <view
          v-for="item in items"
          :key="String(item.id)"
          class="goods"
          @click="(item.remainingQuantity ?? 0) > 0 && openCandidates(item.id)"
        >
          <view class="thumb">
            <view class="i-carbon-camera" />
          </view>
          <view class="flex-1">
            <view class="goods-title">
              {{ detail?.goodsTitle || text(item.equipmentModelCode) }}
              × {{ item.requiredQuantity ?? 0 }}
            </view>
            <view class="muted">
              {{ itemQuantityLabel(item) }}
            </view>
            <StaffDeviceQuantity :item="item" :source-type="detail?.sourceType" :status="detail?.status" @updated="load(orderId)" />
            <view v-for="assignment in item.assignments || []" :key="assignment.id" class="muted">
              {{ assignment.deviceNo }} · {{ assignment.status === 'DISPATCHED' ? '已出库' : assignment.status === 'ASSIGNED' ? '已分配' : assignment.status === 'RETURNED' ? '已回仓' : assignment.status === 'CANCELED' ? '已取消' : '状态待核对' }}
            </view>
          </view>
          <text v-if="(item.remainingQuantity ?? 0) > 0" class="arrow">
            ›
          </text>
        </view>

        <view class="block">
          <view class="line">
            <view class="i-carbon-calendar ico" />
            <view class="flex-1">
              <view class="muted">
                计租日期
              </view>
              <view class="strong">
                {{ formatMonthDay(detail?.billableStartDate) || '-' }} – {{ formatMonthDay(detail?.billableEndDate) || '-' }}
              </view>
            </view>
            <view class="muted">
              共 {{ daysInclusive(detail?.billableStartDate, detail?.billableEndDate) ?? '-' }} 天
            </view>
          </view>
          <view class="line">
            <view class="i-carbon-calendar ico" />
            <view class="flex-1">
              <view class="muted">
                占用日期
              </view>
              <view class="strong">
                {{ occupyRangeLabel(detail || {}) }}
              </view>
            </view>
          </view>
        </view>

        <view class="block">
          <view class="block-title">
            设备信息
          </view>
          <view class="split">
            <view>
              <view class="muted">
                待配数量
              </view>
              <view class="strong">
                {{ remainingDeviceLabel(detail || {}) }}
              </view>
            </view>
            <view>
              <view class="muted">
                设备状态
              </view>
              <view class="accent">
                {{ orderStatusLabel(detail?.status) }}
              </view>
            </view>
          </view>
        </view>

        <view class="block">
          <view class="block-title">
            物流信息
          </view>
          <template v-if="detail?.deliveries?.length">
            <view v-for="delivery in detail.deliveries" :key="`${delivery.direction}-${delivery.sourceCarrierName}-${delivery.trackingStatus}`" class="line">
              <view class="flex-1">
                <view class="strong">
                  {{ delivery.direction === 'RETURN' ? '回寄' : '寄出' }} · {{ delivery.sourceCarrierName || '物流待确认' }}
                </view>
                <view class="muted">
                  {{ delivery.trackingStatus || '状态待同步' }}{{ delivery.stale ? ' · 信息可能滞后' : '' }}
                </view>
              </view>
            </view>
          </template>
          <view v-else class="muted">
            暂无物流快照
          </view>
        </view>

        <view class="block">
          <view class="block-title">
            风险提示
          </view>
          <view class="muted">
            {{ (detail?.riskCodes && detail.riskCodes.length) ? detail.riskCodes.join('、') : '暂无阻塞，按现有排期处理' }}
          </view>
        </view>
      </template>
    </scroll-view>
    <view v-if="!loading && !error" class="footer">
      <wd-button variant="plain" @click="openFirstItem">
        查看设备
      </wd-button>
      <wd-button v-if="hasAccessByCodes(['rental:xianyu:ship']) && detail?.sourceType === 'XIANYU'" type="primary" @click="openShipping">
        开始发货
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import StaffDeviceQuantity from '@/components/rental/staff-device-quantity.vue'
import { callReceiver, copyStaffField } from '@/utils/staffContact'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useAccess } from '@/hooks/useAccess'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { getOrderScheduleDetail } from '@/api/rental/order'
import type { RentalOrderItem, RentalOrderScheduleDetail } from '@/api/rental/order'
import {
  daysInclusive,
  displayOrderNo,
  displayReceiverName,
  formatMonthDay,
  itemQuantityLabel,
  occupyRangeLabel,
  orderStatusLabel,
  remainingDeviceLabel,
  sourceTypeLabel,
} from '@/models/rental/orderDisplay'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const { hasAccessByCodes } = useAccess()
const orderId = ref(0)
const detailAnchor = ref('')
const loading = ref(true)
const error = ref('')
const detail = ref<RentalOrderScheduleDetail>()
const text = (value: unknown) => value == null || value === '' ? '-' : String(value)
const items = computed<RentalOrderItem[]>(() => detail.value?.items || [])
const hasCustomer = computed(() => Boolean(
  detail.value?.receiverName
  || detail.value?.buyerNick
  || detail.value?.receiverMobile
  || detail.value?.receiverAddress,
))
async function load(id: number) {
  loading.value = true
  error.value = ''
  try {
    detail.value = await getOrderScheduleDetail(id)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '订单加载失败'
  } finally {
    loading.value = false
  }
}
function openCandidates(id: unknown) {
  uni.navigateTo({ url: `/pages-rental/orders/candidates?itemId=${String(id)}` })
}
function openFirstItem() {
  detailAnchor.value = ''
  setTimeout(() => {
    detailAnchor.value = 'order-devices'
  }, 0)
}
function goBack() {
  uni.navigateBack()
}
function openShipping() {
  uni.navigateTo({ url: `/pages-rental/xianyu-ship/index?rentalOrderId=${orderId.value}` })
}
onLoad((query) => {
  const id = Number(query?.id)
  if (id) {
    orderId.value = id
  } else {
    error.value = '缺少订单编号'
    loading.value = false
  }
})
onShow(() => {
  if (orderId.value)
    void load(orderId.value)
})
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--staff-surface);
}
.nav {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 24rpx 12rpx;
}
.back {
  width: 56rpx;
  font-size: 48rpx;
  line-height: 1;
}
.nav-title {
  font-size: 36rpx;
  font-weight: 800;
}
.content {
  flex: 1;
  min-height: 0;
  height: 0;
  padding: 8rpx 28rpx 40rpx;
  box-sizing: border-box;
}
.status-row {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 12rpx 0 24rpx;
  border-bottom: 2rpx solid var(--staff-border);
}
.status-title {
  font-size: 44rpx;
  font-weight: 800;
}
.status-sub,
.muted,
.id-label {
  color: var(--staff-muted);
  font-size: 22rpx;
}
.sla {
  text-align: right;
  color: var(--staff-accent-text);
}
.sla-kicker {
  font-size: 20rpx;
  font-weight: 700;
}
.sla-time {
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.1;
}
.ids {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  padding: 24rpx 0;
}
.id-value,
.strong {
  margin-top: 6rpx;
  font-size: 30rpx;
  font-weight: 800;
}
.ext {
  margin-bottom: 16rpx;
  color: var(--staff-muted);
  font-size: 22rpx;
}
.goods {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 0;
  border-top: 2rpx solid var(--staff-border);
  border-bottom: 2rpx solid var(--staff-border);
}
.thumb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  background: var(--staff-soft);
  font-size: 40rpx;
}
.goods-title {
  font-size: 32rpx;
  font-weight: 800;
}
.arrow {
  color: var(--staff-muted);
  font-size: 40rpx;
}
.block {
  padding: 28rpx 0;
  border-bottom: 2rpx solid var(--staff-border);
}
.block-title {
  margin-bottom: 16rpx;
  font-size: 28rpx;
  font-weight: 800;
}
.line {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 16rpx;
}
.ico {
  font-size: 36rpx;
}
.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
}
.accent {
  margin-top: 6rpx;
  color: var(--staff-accent-text);
  font-size: 30rpx;
  font-weight: 800;
}
.footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  padding: 16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
  background: var(--staff-surface);
  border-top: 2rpx solid var(--staff-border);
}
.empty,
.error {
  padding: 60rpx 20rpx;
  text-align: center;
  color: var(--staff-muted);
}
.error {
  color: var(--staff-danger);
}
</style>
