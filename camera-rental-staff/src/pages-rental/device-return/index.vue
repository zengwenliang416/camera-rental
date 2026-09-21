<template>
  <view class="page" :style="staffPageStyle">
    <scroll-view scroll-y class="content">
      <staff-header title="回仓入库" @scanner="scanDevice" />
      <scan-banner
        title="扫描设备永久码"
        subtitle="扫描设备机身永久码，完成回仓登记"
        @click="scanDevice"
      />

      <view v-if="resolvedDevice" class="card">
        <view class="card-head">
          <text>已识别设备</text>
          <text class="tag">
            识别成功
          </text>
        </view>
        <view class="device-no">
          {{ resolvedDevice.deviceNo }}
        </view>
        <view class="muted">
          {{ resolvedDevice.equipmentModelCode }} · {{ deviceStatusLabel(resolvedDevice.status) }}
        </view>
        <view v-if="resolvedDevice.warehouseCode" class="muted">
          仓位 {{ resolvedDevice.warehouseCode }}
        </view>
      </view>

      <view class="hint">
        先识别设备并完成检测，提交检测结果后才登记回仓
      </view>

      <view class="card">
        <view class="muted">
          备注（选填）
        </view>
        <wd-textarea v-model="note" :maxlength="200" show-word-limit placeholder="包装、配件或异常备注..." />
      </view>

      <wd-button plain block @click="manualFocus = true">
        无法识别，人工输入
      </wd-button>
      <view v-if="manualFocus" class="manual">
        <wd-input v-model="manualCode" placeholder="输入设备编号" clearable @confirm="resolveScannedDevice(manualCode)" />
        <wd-button plain :loading="resolving" @click="resolveScannedDevice(manualCode)">
          查询设备
        </wd-button>
      </view>
    </scroll-view>
    <view class="footer">
      <wd-button type="primary" block :disabled="!canSubmit" @click="goInspect">
        核对设备并进入检测
      </wd-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import type { RentalDevice } from '@/api/rental/device'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { computed, ref } from 'vue'
import { resolveRentalDeviceQr } from '@/api/rental/device'
import ScanBanner from '@/components/rental/scan-banner.vue'
import StaffHeader from '@/components/rental/staff-header.vue'
import { useStaffExceptionStore } from '@/store/staffException'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import { extractDeviceNo, normalizeCode } from '@/utils/staffScan'
import { deviceStatusLabel, staffError } from '@/models/rental/staffOperations'
import { useAccess } from '@/hooks/useAccess'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const exceptions = useStaffExceptionStore()
const deviceNo = ref('')
const resolvedDevice = ref<RentalDevice>()
const note = ref('')
const manualFocus = ref(false)
const manualCode = ref('')
const resolving = ref(false)
const { hasAccessByCodes } = useAccess()

const canSubmit = computed(() => Boolean(resolvedDevice.value) && !resolving.value && hasAccessByCodes(['rental:device:assign']))

async function resolveScannedDevice(raw: string) {
  const payload = String(raw || '').trim()
  if (!payload || resolving.value)
    return
  resolving.value = true
  resolvedDevice.value = undefined
  deviceNo.value = ''
  try {
    const device = await resolveRentalDeviceQr(payload.startsWith('CRD1|') ? payload : extractDeviceNo(payload))
    resolvedDevice.value = device
    deviceNo.value = device.deviceNo
    manualCode.value = device.deviceNo
  } catch (error) {
    const message = staffError(error, '设备识别失败')
    exceptions.record({ kind: 'scan', title: '设备识别失败', detail: message, source: '扫码入库' })
    toast.warning(message)
  } finally {
    resolving.value = false
  }
}

const { scan: scanDevice } = useStaffScanner(result => resolveScannedDevice(result.text))

function goInspect() {
  const target = normalizeCode(resolvedDevice.value?.deviceNo || deviceNo.value)
  if (!target || !canSubmit.value) {
    toast.warning('请先识别设备')
    return
  }
  const query = [
    `deviceNo=${encodeURIComponent(target)}`,
    resolvedDevice.value?.id ? `deviceId=${resolvedDevice.value.id}` : '',
    resolvedDevice.value?.equipmentModelCode ? `model=${encodeURIComponent(resolvedDevice.value.equipmentModelCode)}` : '',
    note.value.trim() ? `note=${encodeURIComponent(note.value.trim())}` : '',
  ].filter(Boolean).join('&')
  uni.navigateTo({
    url: `/pages-rental/inspection/index?${query}`,
    events: {
      returned: () => {
        resolvedDevice.value = undefined
        deviceNo.value = ''
        manualCode.value = ''
        note.value = ''
      },
    },
  })
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
}
.content {
  height: calc(100vh - 140rpx);
  padding: calc(var(--staff-status-bar-height, 0px) + 12rpx) 28rpx 24rpx;
  box-sizing: border-box;
}
.card {
  padding: 24rpx 0;
  border-bottom: 2rpx solid #eee;
}
.card-head {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
}
.tag {
  padding: 4rpx 12rpx;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 20rpx;
}
.device-no {
  margin-top: 12rpx;
  font-size: 44rpx;
  font-weight: 800;
}
.muted {
  margin-top: 8rpx;
  color: #6b6b6b;
  font-size: 24rpx;
}
.strong {
  margin-top: 6rpx;
  font-size: 32rpx;
  font-weight: 800;
}
.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24rpx;
  margin-top: 20rpx;
}
.hint {
  margin: 16rpx 0;
  padding: 20rpx;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 24rpx;
}
.manual {
  margin-top: 12rpx;
}
.footer {
  padding: 16rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
}
</style>
