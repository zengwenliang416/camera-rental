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
              {{ draft.devices?.map(device => device.deviceNo).join('、') || draft.deviceNo }}
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
        <view class="workbench-actions">
          <wd-button variant="plain" size="small" @click="copyStaffField(draft.orderNo)">
            复制订单号
          </wd-button>
          <wd-button variant="plain" size="small" @click="copyStaffField(draft.waybillNo)">
            复制运单号
          </wd-button>
          <wd-button v-if="draft.receiverMobile" variant="plain" size="small" @click="callReceiver(draft.receiverMobile)">
            拨打电话
          </wd-button>
        </view>
        <view class="info">
          {{ resultHint || '提交后自动核对发货记录；结果不明时请勿重复发货' }}
          <wd-button v-if="resultUnknown" size="small" variant="plain" @click="confirmResult">
            重新核对结果
          </wd-button>
        </view>
      </template>
    </scroll-view>
    <view class="footer">
      <wd-button variant="plain" block :disabled="shipping" @click="goBack">
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
          {{ shipping ? '提交中...' : resultUnknown ? '结果待核对，暂停发货' : '长按确认发货' }}
        </view>
        <view class="hold-sub">
          请按住 2 秒以确认
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { callReceiver, copyStaffField } from '@/utils/staffContact'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { computed, onUnmounted, ref } from 'vue'
import { onHide, onShow } from '@dcloudio/uni-app'
import { storeToRefs } from 'pinia'
import { createIssue } from '@/api/rental/warehouse'
import { getShipmentStatus, shipXianyuOrder } from '@/api/rental/xianyu'
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
const resultUnknown = ref(false)
const resultHint = ref('')
let queryGeneration = 0
let foreground = true
async function confirmResult() {
  if (!draft.value || shipping.value)
    return
  const current = ++queryGeneration
  const submitted = { ...draft.value }
  resultHint.value = '正在核对服务器发货记录…'
  for (let attempt = 0; attempt < 3; attempt++) {
    if (!foreground || current !== queryGeneration)
      return
    try {
      const result = await getShipmentStatus(submitted.channelOrderId, submitted.idempotencyKey)
      if (!foreground || current !== queryGeneration || draft.value?.idempotencyKey !== submitted.idempotencyKey)
        return
      if (result.state === 'REJECTED') {
        resultUnknown.value = false
        resultHint.value = '渠道明确拒绝了本次发货，请修正信息后重试。'
        return
      }
      if (result.state === 'SUCCEEDED') {
        resultUnknown.value = false
        resultHint.value = '已确认发货成功'
        draftStore.clear()
        uni.showToast({ title: '已确认发货成功', icon: 'success' })
        uni.navigateBack()
        return
      }
    } catch {
      /* A failed read does not mean the original write failed. */
    }
    if (attempt < 2)
      await new Promise(resolve => setTimeout(resolve, 1500))
  }
  if (foreground && current === queryGeneration) {
    resultHint.value = '结果仍未确认，请核对订单或联系管理员处理，勿重复发货。'
    try {
      await createIssue({ requestKey: `ship-unknown:${submitted.idempotencyKey}`, rentalOrderId: submitted.rentalOrderId, title: '发货结果待核对', note: '手机未取得成功回执。请核对渠道发货及本地设备记录，勿重复发货。' })
      if (foreground && current === queryGeneration)
        resultHint.value += ' 已登记到异常处理。'
    } catch {
      /* Keep the visible unknown state if the issue service is also offline. */
    }
  }
}
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
  if (!draft.value || shipping.value || resultUnknown.value || !hasAccessByCodes(['rental:xianyu:ship']))
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
  if (!draft.value || shipping.value || resultUnknown.value || !hasAccessByCodes(['rental:xianyu:ship']))
    return
  const submittedDraft = { ...draft.value }
  shipping.value = true
  try {
    const result = await shipXianyuOrder({
      channelOrderId: submittedDraft.channelOrderId,
      deviceIds: submittedDraft.devices?.map(device => device.id),
      deviceNo: submittedDraft.devices?.length ? undefined : submittedDraft.deviceNo,
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
    resultUnknown.value = true
    const message = staffError(error, '发货结果未确认，请核对订单')
    exceptions.record({
      kind: 'order',
      title: '发货结果待核对',
      detail: message,
      source: '发货提交',
      rentalOrderId: submittedDraft.rentalOrderId,
    })
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    shipping.value = false
    if (resultUnknown.value)
      void confirmResult()
  }
}
onUnmounted(() => {
  clearTimer()
  queryGeneration++
  foreground = false
})
onHide(() => {
  endHold()
  foreground = false
  queryGeneration++
})
onShow(() => {
  foreground = true
  if (resultUnknown.value)
    void confirmResult()
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
  color: var(--staff-accent-text);
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
  border-bottom: 2rpx solid var(--staff-border);
}
.ok {
  color: var(--staff-success);
  font-size: 32rpx;
  font-weight: 800;
}
.muted {
  color: var(--staff-muted);
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
  color: var(--staff-info);
  background: var(--staff-info-soft);
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
  color: var(--staff-muted);
}
</style>
