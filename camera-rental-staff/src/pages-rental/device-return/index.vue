<template>
  <view class="yd-page-container bg-[#f5f7fb]">
    <wd-navbar
      title="扫码回仓"
      left-arrow
      placeholder
      safe-area-inset-top
      fixed
      @click-left="handleBack"
    />

    <scroll-view class="min-h-0 flex-1" scroll-y>
      <view class="p-24rpx pb-180rpx">
        <view
          class="mb-24rpx rounded-20rpx bg-[#1f2937] p-28rpx text-white shadow-sm"
        >
          <view class="text-34rpx font-semibold">
            设备回仓登记
          </view>
          <view class="mt-12rpx text-26rpx text-[#d1d5db] leading-relaxed">
            扫设备二维码或输入设备编号，确认检测结论后提交。回仓会同步收窄设备占用排期，检测不通过将转入维修锁定。
          </view>
        </view>

        <view class="mb-24rpx overflow-hidden rounded-16rpx bg-white shadow-sm">
          <view class="border-b border-[#edf0f5] p-24rpx">
            <view class="text-30rpx text-[#222] font-semibold">
              1. 设备识别
            </view>
            <view class="mt-8rpx text-24rpx text-[#888]">
              优先扫设备机身上的永久二维码；标签破损时可人工输入设备编号。
            </view>
          </view>
          <view class="p-24rpx">
            <view class="mb-20rpx">
              <wd-button type="primary" block @click="scanDevice">
                扫设备二维码
              </wd-button>
            </view>
            <wd-cell-group border>
              <wd-cell title="设备编号">
                <wd-input
                  v-model="deviceNo"
                  clearable
                  no-border
                  placeholder="请输入或扫码设备编号"
                  @blur="handleDeviceInputBlur"
                />
              </wd-cell>
            </wd-cell-group>
            <view
              v-if="resolvedDevice"
              class="mt-16rpx rounded-12rpx bg-[#f0f7ff] p-20rpx text-26rpx text-[#1c4f8a] leading-relaxed"
            >
              <view>设备：{{ resolvedDevice.deviceNo }}</view>
              <view>型号：{{ resolvedDevice.equipmentModelCode || '-' }}</view>
              <view>状态：{{ deviceStatusLabel(resolvedDevice.status) }}</view>
            </view>
          </view>
        </view>

        <view class="mb-24rpx overflow-hidden rounded-16rpx bg-white shadow-sm">
          <view class="border-b border-[#edf0f5] p-24rpx">
            <view class="text-30rpx text-[#222] font-semibold">
              2. 检测结论
            </view>
          </view>
          <view class="p-24rpx">
            <wd-radio-group v-model="inspectPassed" shape="button" inline>
              <wd-radio :value="true">检测通过</wd-radio>
              <wd-radio :value="false">检测不通过</wd-radio>
            </wd-radio-group>
            <view class="mt-20rpx">
              <wd-textarea
                v-model="note"
                :maxlength="512"
                show-word-limit
                placeholder="检测备注（选填）：外观、配件、异常情况等"
              />
            </view>
          </view>
        </view>

        <view class="fixed bottom-0 left-0 right-0 z-10 bg-white/95 p-24rpx pb-[calc(24rpx+env(safe-area-inset-bottom))] shadow-md">
          <wd-button
            type="primary"
            block
            :disabled="!canSubmit"
            :loading="returning"
            @click="confirmReturn"
          >
            确认回仓{{ inspectPassed === false ? '（转维修）' : '' }}
          </wd-button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script lang="ts" setup>
import type { RentalDevice } from '@/api/rental/device'
import { useDialog } from '@wot-ui/ui/components/wd-dialog'
import { useToast } from '@wot-ui/ui/components/wd-toast'
import { computed, ref } from 'vue'
import { resolveRentalDeviceQr, returnRentalDevice } from '@/api/rental/device'
import { navigateBackPlus } from '@/utils'

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const dialog = useDialog()

const deviceNo = ref('')
const resolvedDevice = ref<RentalDevice>()
const inspectPassed = ref<boolean | string>(true)
const note = ref('')
const returning = ref(false)

const canSubmit = computed(() => {
  return Boolean(resolvedDevice.value || normalizeCode(deviceNo.value))
})

function handleBack() {
  navigateBackPlus()
}

function normalizeCode(value?: string) {
  return String(value || '')
    .trim()
    .replace(/\s+/g, '')
    .toUpperCase()
}

function deviceStatusLabel(status?: string) {
  const labels: Record<string, string> = {
    AVAILABLE: '空闲',
    RENTED: '在租',
    MAINTENANCE: '维修中',
  }
  return status ? labels[status] || status : '-'
}

function extractQueryValue(raw: string, keys: string[]) {
  for (const key of keys) {
    const matched = raw.match(new RegExp(`[?&]${key}=([^&#]+)`, 'i'))
    if (matched?.[1]) {
      return decodeURIComponent(matched[1])
    }
  }
  return ''
}

function extractJsonValue(raw: string, keys: string[]) {
  try {
    const json = JSON.parse(raw) as Record<string, unknown>
    for (const key of keys) {
      const value = json[key]
      if (value !== undefined && value !== null) {
        return String(value)
      }
    }
  } catch {
    return ''
  }
  return ''
}

function extractDeviceNo(raw: string) {
  const queryValue = extractQueryValue(raw, [
    'deviceNo',
    'device_no',
    'serialNo',
    'serial_no',
    'sn',
  ])
  const jsonValue = extractJsonValue(raw, [
    'deviceNo',
    'device_no',
    'serialNo',
    'serial_no',
    'sn',
  ])
  return normalizeCode(queryValue || jsonValue || raw)
}

async function resolveScannedDevice(raw: string) {
  const payload = String(raw || '').trim()
  if (!payload) {
    return
  }
  if (payload.startsWith('CRD1|')) {
    const device = await resolveRentalDeviceQr(payload)
    resolvedDevice.value = device
    deviceNo.value = device.deviceNo
    return
  }
  resolvedDevice.value = undefined
  deviceNo.value = extractDeviceNo(payload)
}

function handleDeviceInputBlur() {
  resolvedDevice.value = undefined
  deviceNo.value = normalizeCode(deviceNo.value)
}

async function scanDevice() {
  try {
    const res = await uni.scanCode({ scanType: ['qrCode', 'barCode'] })
    await resolveScannedDevice(res.result)
  } catch (error) {
    const message = error instanceof Error ? error.message : '未获取到设备码'
    toast.warning(message)
  }
}

async function confirmReturn() {
  const target = normalizeCode(deviceNo.value)
  if (!canSubmit.value || (!resolvedDevice.value && !target)) {
    toast.warning('请先识别设备')
    return
  }
  const passed = inspectPassed.value !== false
  try {
    await dialog.confirm({
      title: '确认回仓',
      msg: `设备 ${target}\n检测${passed ? '通过（恢复可租）' : '不通过（转入维修）'}`,
    })
  } catch {
    return
  }
  returning.value = true
  try {
    await returnRentalDevice({
      deviceId: resolvedDevice.value?.id,
      deviceNo: resolvedDevice.value ? undefined : target,
      inspectPassed: passed,
      note: note.value.trim() || undefined,
    })
    toast.success(`回仓完成：${target}`)
    deviceNo.value = ''
    resolvedDevice.value = undefined
    inspectPassed.value = true
    note.value = ''
  } finally {
    returning.value = false
  }
}
</script>
