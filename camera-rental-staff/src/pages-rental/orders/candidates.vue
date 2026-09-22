<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content">
      <staff-header title="待分配拣货" @scanner="scanDevice" />

      <view class="ids">
        <view>
          <view class="muted">
            订单号
          </view>
          <view class="strong">
            {{ maskOrderId(result?.externalOrderNo || result?.orderNo) || '-' }}
          </view>
        </view>
        <view class="deadline">
          <view class="muted">
            占用开始
          </view>
          <view class="accent">
            {{ formatApiDate(result?.occupyStartDate) || '待确认' }}
          </view>
        </view>
      </view>

      <view class="goods">
        <view class="thumb">
          <view class="i-carbon-camera" />
        </view>
        <view class="flex-1">
          <view class="muted">
            需分配设备
          </view>
          <view class="goods-title">
            {{ result?.equipmentModelCode || '待确认型号' }}
          </view>
          <view class="muted">
            已扫 {{ picked.length }} / {{ result?.remainingQuantity ?? 0 }}
          </view>
        </view>
        <view class="occupy">
          <view class="muted">
            占用时间
          </view>
          <view class="strong">
            {{ occupyRangeLabel(result || {}) }}
          </view>
        </view>
      </view>

      <scan-banner
        title="扫描设备"
        :subtitle="`仅接受 ${result?.equipmentModelCode || '对应型号'} 可用设备`"
        @click="scanDevice"
      />

      <view class="section-head">
        <text>已扫描设备（{{ picked.length }} / {{ result?.remainingQuantity ?? 0 }}）</text>
        <text class="link" @click="showAll = !showAll">
          {{ showAll ? '收起候选' : '查看可用设备' }} ›
        </text>
      </view>

      <view v-if="loading" class="banner">
        正在加载候选设备…
      </view>
      <view v-if="error" class="banner error">
        {{ error }}
        <wd-button variant="plain" size="small" :disabled="submitting" @click="load">
          刷新候选
        </wd-button>
      </view>

      <view
        v-for="(item, index) in picked"
        :key="item.id"
        class="slot filled"
        @click="togglePick(item)"
      >
        <view class="idx">
          {{ index + 1 }}
        </view>
        <view class="flex-1">
          <view class="name">
            {{ item.deviceNo }}
          </view>
          <view class="muted">
            {{ item.equipmentModelCode }} · 已加入 · 点此移除
          </view>
        </view>
        <text class="arrow">
          ›
        </text>
      </view>

      <view
        v-for="slot in emptySlots"
        :key="`empty-${slot}`"
        class="slot empty-slot"
        @click="scanDevice"
      >
        <view class="idx muted">
          {{ picked.length + slot }}
        </view>
        <view class="flex-1">
          <view class="name">
            等待扫描第 {{ picked.length + slot }} 台
          </view>
          <view class="muted">
            扫描设备条码
          </view>
        </view>
      </view>

      <view v-if="showAll" class="candidates">
        <view
          v-for="item in eligible"
          :key="item.id"
          class="slot"
          :class="{ filled: picked.some(row => row.id === item.id) }"
          @click="togglePick(item)"
        >
          <view class="flex-1">
            <view class="name">
              {{ item.deviceNo }}
            </view>
            <view class="muted">
              {{ item.status || '可用' }}{{ item.eligible === false ? ' · 不可分配' : '' }}
            </view>
          </view>
        </view>
      </view>

      <view class="hint">
        设备分配后仍需核对运单并完成发货
      </view>
    </scroll-view>
    <view class="footer">
      <wd-button
        type="primary"
        block
        :disabled="!canConfirm"
        :loading="submitting"
        @click="submit"
      >
        确认分配 {{ picked.length }} / {{ result?.remainingQuantity ?? 0 }}
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { assignRentalDevice, getDeviceCandidates } from '@/api/rental/order'
import type { DeviceCandidate, DeviceCandidateResult } from '@/api/rental/order'
import { resolveRentalDeviceQr } from '@/api/rental/device'
import ScanBanner from '@/components/rental/scan-banner.vue'
import StaffHeader from '@/components/rental/staff-header.vue'
import { extractDeviceNo, maskOrderId, normalizeCode } from '@/utils/staffScan'
import { candidateReason, reconcilePicked } from '@/models/rental/staffWorkflow'
import { operationKey, staffError } from '@/models/rental/staffOperations'
import { useAccess } from '@/hooks/useAccess'
import { formatApiDate, occupyRangeLabel } from '@/models/rental/orderDisplay'
import { useStaffExceptionStore } from '@/store/staffException'
import { useStaffScanner } from '@/hooks/useStaffScanner'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const loading = ref(true)
const submitting = ref(false)
const showAll = ref(false)
const error = ref('')
const itemId = ref(0)
const result = ref<DeviceCandidateResult>()
const picked = ref<DeviceCandidate[]>([])
const exceptions = useStaffExceptionStore()
const { hasAccessByCodes } = useAccess()
const attemptKeys = new Map<string, string>()

const eligible = computed(() => (result.value?.candidates || []).filter(item => item.eligible === true))
const emptySlots = computed(() => {
  const remain = Math.max(0, (result.value?.remainingQuantity ?? 0) - picked.value.length)
  return remain > 0 ? [1] : []
})
const canConfirm = computed(() => !loading.value && !submitting.value && !error.value && picked.value.length > 0 && hasAccessByCodes(['rental:device:assign']))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const fresh = await getDeviceCandidates(itemId.value)
    const previousCount = picked.value.length
    picked.value = reconcilePicked(picked.value, fresh.candidates || [], fresh.remainingQuantity || 0)
    result.value = fresh
    if (picked.value.length !== previousCount)
      uni.showToast({ title: '已移除当前不可分配设备，请重新核对', icon: 'none' })
  } catch (err) {
    error.value = staffError(err, '候选设备加载失败')
  } finally {
    loading.value = false
  }
}

function togglePick(item: DeviceCandidate) {
  if (item.eligible !== true || loading.value || submitting.value)
    return
  if (picked.value.some(row => row.id === item.id)) {
    picked.value = picked.value.filter(row => row.id !== item.id)
    return
  }
  if (picked.value.length >= (result.value?.remainingQuantity ?? 0)) {
    uni.showToast({ title: '已达到本明细待分配数量', icon: 'none' })
    return
  }
  picked.value = [...picked.value, item]
}

const { scan: scanDevice } = useStaffScanner(result => acceptDeviceScan(result.text))

async function acceptDeviceScan(raw: string) {
  if (submitting.value) {
    uni.showToast({ title: '正在提交分配，请稍后重扫', icon: 'none' })
    return
  }
  try {
    const payload = raw.trim()
    let deviceNo = extractDeviceNo(payload)
    if (payload.startsWith('CRD1|')) {
      const device = await resolveRentalDeviceQr(payload)
      deviceNo = device.deviceNo
    }
    const matched = (result.value?.candidates || []).find(item => normalizeCode(item.deviceNo) === deviceNo)
    if (!matched || matched.eligible !== true) {
      exceptions.record({
        kind: 'scan',
        title: '设备不可分配',
        detail: candidateReason(matched),
        rentalOrderId: result.value?.rentalOrderId,
        itemId: itemId.value,
        source: '扫码拣货',
      })
      error.value = candidateReason(matched)
      return
    }
    error.value = ''
    if (!picked.value.some(row => row.id === matched.id))
      togglePick(matched)
  } catch (err) {
    if (err && typeof err === 'object' && 'errMsg' in err && String(err.errMsg).includes('cancel'))
      return
    error.value = staffError(err, '未获取到设备码')
    exceptions.record({
      kind: 'scan',
      title: '扫码失败',
      detail: staffError(err, '未获取到设备码'),
      source: '扫码拣货',
      itemId: itemId.value,
    })
  }
}

async function submit() {
  if (!canConfirm.value)
    return
  submitting.value = true
  const devices = [...picked.value]
  let completed = 0
  try {
    for (const device of devices) {
      const data = {
        rentalOrderItemId: itemId.value,
        deviceId: Number(device.id),
        occupyStartDate: formatApiDate(result.value?.occupyStartDate) || formatApiDate(device.occupyStartDate),
        occupyEndDateExclusive: formatApiDate(result.value?.occupyEndDateExclusive) || formatApiDate(device.occupyEndDateExclusive),
      }
      if (!data.occupyStartDate || !data.occupyEndDateExclusive)
        throw new Error('设备占用日期缺失，请刷新后重试')
      const fingerprint = JSON.stringify(data)
      if (!attemptKeys.has(fingerprint))
        attemptKeys.set(fingerprint, operationKey('staff-assign'))
      await assignRentalDevice({ ...data, idempotencyKey: attemptKeys.get(fingerprint)! })
      completed++
      picked.value = picked.value.filter(row => row.id !== device.id)
    }
    uni.showToast({ title: `已分配 ${completed} 台设备`, icon: 'success' })
    uni.navigateBack()
  } catch (err) {
    const failure = staffError(err, '分配结果未确认，请核对后重试')
    await load()
    error.value = `${completed ? `已成功 ${completed} 台；` : ''}${failure}`
    exceptions.record({ kind: 'order', title: '分配未全部完成', detail: error.value, source: '设备分配', itemId: itemId.value, rentalOrderId: result.value?.rentalOrderId })
  } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  itemId.value = Number(query?.itemId)
  if (itemId.value) {
    void load()
  } else {
    error.value = '缺少明细编号'
    loading.value = false
  }
})
</script>

<style scoped>
.page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--staff-surface);
}
.content {
  flex: 1;
  min-height: 0;
  height: 0;
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 28rpx 40rpx;
  box-sizing: border-box;
}
.ids,
.goods {
  display: flex;
  justify-content: space-between;
  gap: 24rpx;
  padding: 24rpx 0;
  border-bottom: 2rpx solid var(--staff-border);
}
.muted {
  color: var(--staff-muted);
  font-size: 22rpx;
}
.strong,
.goods-title {
  margin-top: 6rpx;
  font-size: 32rpx;
  font-weight: 800;
}
.accent {
  margin-top: 6rpx;
  color: var(--staff-accent-text);
  font-size: 26rpx;
  font-weight: 800;
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
.section-head {
  display: flex;
  justify-content: space-between;
  margin: 28rpx 0 16rpx;
  font-size: 26rpx;
  font-weight: 800;
}
.link {
  color: var(--staff-info);
  font-weight: 600;
}
.slot {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 22rpx;
  margin-bottom: 12rpx;
  background: var(--staff-surface);
  border: 2rpx solid var(--staff-border);
}
.slot.filled {
  border-left: 8rpx solid var(--staff-info);
}
.empty-slot {
  border-style: dashed;
}
.idx {
  width: 40rpx;
  font-size: 32rpx;
  font-weight: 800;
}
.name {
  font-size: 30rpx;
  font-weight: 800;
}
.hint {
  margin-top: 16rpx;
  padding: 20rpx;
  color: var(--staff-info);
  background: var(--staff-info-soft);
  font-size: 24rpx;
}
.banner {
  padding: 20rpx;
  margin-bottom: 16rpx;
}
.banner.error {
  color: var(--staff-danger);
  background: var(--staff-danger-soft);
}
.footer {
  padding: 16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
}
.arrow {
  color: var(--staff-muted);
  font-size: 36rpx;
}
</style>
