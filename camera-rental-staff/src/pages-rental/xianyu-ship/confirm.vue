<template>
  <view class="page" :style="staffPageStyle">
    <view class="nav">
      <view class="back" @click="goBack">
        ‹
      </view>
      <view class="nav-title">
        发货确认
      </view>
    </view>
    <scroll-view scroll-y class="content">
      <view class="warn">
        提交后将更新设备出库状态并发起渠道发货。请确认以下信息准确无误，提交后不可撤销。
      </view>
      <view v-if="!draft" class="empty">
        没有待确认的发货草稿，请返回扫码发货。
      </view>
      <template v-else>
        <view class="section-head">
          <text>核对信息</text>
          <text class="muted">
            请核对本次发货
          </text>
        </view>
        <view class="row">
          <text class="ok">
            ✓
          </text>
          <view>
            <view class="muted">
              订单
            </view>
            <view class="strong">
              {{ maskOrderId(draft.orderNo) }}
            </view>
          </view>
        </view>
        <view class="row">
          <text class="ok">
            ✓
          </text>
          <view>
            <view class="muted">
              设备
            </view>
            <view class="strong">
              {{ draft.deviceNo }}
            </view>
          </view>
        </view>
        <view class="row">
          <text class="ok">
            ✓
          </text>
          <view>
            <view class="muted">
              物流渠道
            </view>
            <view class="strong">
              {{ draft.expressName }} · {{ maskWaybill(draft.waybillNo) }}
            </view>
          </view>
        </view>
        <view class="row">
          <text class="ok">
            ✓
          </text>
          <view>
            <view class="muted">
              操作人
            </view>
            <view class="strong">
              {{ operator }}
            </view>
          </view>
        </view>
        <view class="row">
          <view>
            <view class="muted">
              收件信息
            </view><view class="strong">
              {{ draft.receiverName || '收件人未提供' }} · {{ draft.receiverMobile || '电话未提供' }}
            </view><view>{{ draft.receiverAddress || '地址未提供，请返回核对订单' }}</view>
          </view>
        </view>
        <view class="info">
          如提交结果不明，请先核对订单状态，再决定是否重试
        </view>
      </template>
    </scroll-view>
    <view class="footer">
      <wd-button plain block :disabled="shipping" @click="goBack">
        返回修改
      </wd-button>
      <view
        class="hold"
        :class="{ on: holding }"
        @touchstart.prevent="startHold"
        @touchend="endHold"
        @touchcancel="endHold"
        @mousedown.prevent="startHold"
        @mouseup="endHold"
        @mouseleave="endHold"
      >
        <view class="hold-title">
          {{ shipping ? '提交中...' : '长按确认发货' }}
        </view>
        <view class="hold-sub">
          请按住 2 秒以确认
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { computed, onUnmounted, ref } from 'vue'
import { onHide } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { shipXianyuOrder } from '@/api/rental/xianyu'
import { useUserStore } from '@/store/user'
import { useShipDraftStore } from '@/store/shipDraft'
import { useStaffExceptionStore } from '@/store/staffException'
import { maskOrderId, maskWaybill } from '@/utils/staffScan'
import { staffError } from '@/models/rental/staffOperations'
import { useAccess } from '@/hooks/useAccess'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const draftStore = useShipDraftStore()
const exceptions = useStaffExceptionStore()
const { draft } = storeToRefs(draftStore)
const userStore = useUserStore()
const operator = computed(() => userStore.userInfo.nickname || userStore.userInfo.username || '员工')
const shipping = ref(false)
const { hasAccessByCodes } = useAccess()
const holding = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

function goBack() {
  endHold()
  if (!shipping.value)
    uni.navigateBack()
}

function clearTimer() {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

function startHold() {
  if (!draft.value || shipping.value || !hasAccessByCodes(['rental:xianyu:ship']))
    return
  holding.value = true
  clearTimer()
  timer = setTimeout(() => {
    holding.value = false
    void submit()
  }, 2000)
}

function endHold() {
  holding.value = false
  clearTimer()
}

async function submit() {
  if (!draft.value || shipping.value || !hasAccessByCodes(['rental:xianyu:ship']))
    return
  const submittedDraft = { ...draft.value }
  shipping.value = true
  try {
    const result = await shipXianyuOrder({
      channelOrderId: submittedDraft.channelOrderId,
      deviceNo: submittedDraft.deviceNo,
      idempotencyKey: submittedDraft.idempotencyKey,
      expressCode: submittedDraft.expressCode,
      expressName: submittedDraft.expressName,
      waybillNo: submittedDraft.waybillNo,
      source: 'STAFF',
      ocrConfirmed: submittedDraft.ocrConfirmed,
    })
    if (draft.value?.idempotencyKey !== submittedDraft.idempotencyKey)
      return
    uni.showToast({ title: `发货成功：${result.maskedWaybillNo || maskWaybill(submittedDraft.waybillNo)}`, icon: 'success' })
    draftStore.clear()
    uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages-rental/orders/index' }) })
  } catch (error) {
    const message = staffError(error, '发货结果未确认，请核对订单后重试')
    exceptions.record({
      kind: 'order',
      title: '发货失败',
      detail: message,
      source: '发货提交',
      rentalOrderId: submittedDraft.rentalOrderId,
    })
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    shipping.value = false
  }
}

onUnmounted(() => {
  clearTimer()
})
onHide(endHold)
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
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
}
.nav-title {
  font-size: 36rpx;
  font-weight: 800;
}
.content {
  flex: 1;
  min-height: 0;
  height: 0;
  padding: 8rpx 28rpx;
  box-sizing: border-box;
}
.warn {
  padding: 20rpx;
  margin-bottom: 24rpx;
  color: var(--staff-accent, #e10600);
  background: var(--staff-accent-soft, #fff1f0);
  font-size: 26rpx;
  font-weight: 700;
}
.section-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
  font-weight: 800;
}
.row {
  display: flex;
  gap: 16rpx;
  padding: 22rpx 0;
  border-bottom: 2rpx solid #eee;
}
.ok {
  color: #15803d;
  font-size: 32rpx;
  font-weight: 800;
}
.muted {
  color: #6b6b6b;
  font-size: 22rpx;
}
.strong {
  margin-top: 6rpx;
  font-size: 30rpx;
  font-weight: 800;
}
.info {
  margin-top: 16rpx;
  padding: 20rpx;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 24rpx;
}
.footer {
  padding: 12rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
}
.hold {
  margin-top: 12rpx;
  padding: 28rpx;
  color: #fff;
  text-align: center;
  background: var(--staff-accent, #e10600);
}
.hold.on {
  background: var(--wot-button-primary-bg-active, #b80500);
}
.hold-title {
  font-size: 32rpx;
  font-weight: 800;
}
.hold-sub {
  margin-top: 6rpx;
  font-size: 22rpx;
  opacity: 0.9;
}
.empty {
  padding: 40rpx 0;
  color: #6b6b6b;
}
</style>
