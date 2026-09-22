<template>
  <view class="page" :style="staffPageStyle">
    <view class="nav">
      <view class="back" @click="goBack">
        ‹
      </view>
      <view class="nav-title">
        设备检测
      </view>
    </view>
    <scroll-view scroll-y class="content">
      <view class="hero">
        <view class="strong">
          {{ deviceNo }}{{ model ? ` · ${model}` : '' }}
        </view>
        <view class="muted">
          提交后完成本设备的回仓登记和检测
        </view>
      </view>

      <view class="section-head">
        <text>检测项目</text>
        <text class="muted">
          已检查 {{ checkedCount }} / {{ checks.length }} 项
        </text>
      </view>
      <view v-for="item in checks" :key="item.key" class="check">
        <view class="check-name">
          {{ item.no }} {{ item.label }}
        </view>
        <view class="opts">
          <view
            v-for="opt in item.options"
            :key="opt"
            class="opt"
            :class="{ on: item.value === opt }"
            @click="item.value = opt"
          >
            {{ opt }}
          </view>
        </view>
      </view>

      <view class="section-head">
        备注说明
      </view>
      <wd-textarea v-model="note" :maxlength="200" show-word-limit placeholder="损坏、缺件、清洁或维修说明（选填）" />

      <view class="warn">
        结果提交后写入检测记录，请确认检测结果准确无误
      </view>
    </scroll-view>
    <view v-if="submitHint" class="submit-hint">
      {{ submitHint }}
    </view>
    <view class="footer">
      <wd-button variant="plain" :disabled="!deviceNo || submitting || !!inspectionBlocker(checks, false) || !canReturn" :loading="submitting" @click="submit(false)">
        检测不通过 · 转维修
      </wd-button>
      <wd-button type="primary" :disabled="!deviceNo || submitting || !!inspectionBlocker(checks, true) || !canReturn" :loading="submitting" @click="submit(true)">
        检测通过 · 恢复可租
      </wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { onBackPress, onLoad } from '@dcloudio/uni-app'
import { returnRentalDevice } from '@/api/rental/device'
import { useStaffExceptionStore } from '@/store/staffException'
import { deviceStatusLabel, inspectionBlocker, staffError } from '@/models/rental/staffOperations'
import { useAccess } from '@/hooks/useAccess'

const staffPageStyle = useStaffPageStyle()

definePage({
  style: {
    navigationStyle: 'custom',
  },
})

const deviceNo = ref('')
const deviceId = ref<number>()
const model = ref('')
const note = ref('')
const submitting = ref(false)
const exceptions = useStaffExceptionStore()
const { hasAccessByCodes } = useAccess()
const canReturn = computed(() => hasAccessByCodes(['rental:device:assign']))
let saved = false
let allowBack = false
let confirmingBack = false
let initialNote = ''
const checks = reactive([
  { key: 'body', no: '01', label: '机身外观', options: ['正常', '有问题', '待确认'], value: '待确认' },
  { key: 'power', no: '02', label: '开机与功能', options: ['正常', '有问题', '待确认'], value: '待确认' },
  { key: 'mount', no: '03', label: '镜头卡口', options: ['正常', '有问题', '待确认'], value: '待确认' },
  { key: 'battery', no: '04', label: '电池与充电器', options: ['齐全', '缺件', '待确认'], value: '待确认' },
  { key: 'accessories', no: '05', label: '其他配件', options: ['齐全', '缺件', '待确认'], value: '待确认' },
])

const checkedCount = computed(() => checks.filter(item => item.value !== '待确认').length)
const submitHint = computed(() => !deviceNo.value ? '缺少设备，请返回重新扫描' : !canReturn.value ? '当前账号没有回仓权限' : inspectionBlocker(checks, checks.every(item => ['正常', '齐全'].includes(item.value))))
function goBack() {
  if (!submitting.value)
    uni.navigateBack()
}
onBackPress(() => {
  if (submitting.value)
    return true
  if (saved || allowBack || (!checkedCount.value && note.value === initialNote))
    return false
  if (!confirmingBack) {
    confirmingBack = true
    uni.showModal({ title: '检测尚未提交', content: '返回会丢弃本页检测结果，确认返回吗？', success: (result) => {
      if (result.confirm) {
        allowBack = true
        uni.navigateBack()
      }
    }, complete: () => { confirmingBack = false } })
  }
  return true
})
onBeforeUnmount(() => {
  confirmingBack = false
})

async function submit(passed: boolean) {
  if (!deviceNo.value || submitting.value || !canReturn.value)
    return
  const blocker = inspectionBlocker(checks, passed)
  if (blocker) {
    uni.showToast({ title: blocker, icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const checklist = checks.map(item => `${item.label}:${item.value}`).join('；')
    const result = await returnRentalDevice({
      deviceId: deviceId.value,
      deviceNo: deviceId.value ? undefined : deviceNo.value,
      inspectPassed: passed,
      note: [note.value.trim(), checklist].filter(Boolean).join(' | ') || undefined,
    })
    saved = true
    uni.showToast({ title: `回仓完成：${deviceStatusLabel(result.deviceStatus)}`, icon: 'success' })
    const pages = getCurrentPages() as Array<{ getOpenerEventChannel?: () => { emit: (name: string) => void } }>
    pages[pages.length - 1]?.getOpenerEventChannel?.().emit('returned')
    uni.navigateBack({ fail: () => uni.switchTab({ url: '/pages-rental/device-return/index' }) })
  } catch (error) {
    const message = staffError(error, '检测提交失败')
    exceptions.record({ kind: 'order', title: '回仓检测失败', detail: message, source: '订单校验' })
    uni.showToast({ title: message, icon: 'none' })
  } finally {
    submitting.value = false
  }
}

onLoad((query) => {
  deviceNo.value = String(query?.deviceNo || '')
  deviceId.value = query?.deviceId ? Number(query.deviceId) : undefined
  model.value = query?.model ? decodeURIComponent(String(query.model)) : ''
  note.value = query?.note ? decodeURIComponent(String(query.note)) : ''
  initialNote = note.value
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
  padding: 8rpx 28rpx 24rpx;
  box-sizing: border-box;
}
.hero {
  padding: 12rpx 0 24rpx;
  border-bottom: 6rpx solid var(--staff-ink);
}
.strong {
  font-size: 36rpx;
  font-weight: 800;
}
.muted {
  margin-top: 8rpx;
  color: var(--staff-muted);
  font-size: 22rpx;
}
.section-head {
  display: flex;
  justify-content: space-between;
  margin: 28rpx 0 12rpx;
  font-weight: 800;
}
.check {
  padding: 18rpx 0;
  border-bottom: 2rpx solid var(--staff-border);
}
.check-name {
  margin-bottom: 12rpx;
  font-weight: 700;
}
.opts {
  display: flex;
  gap: 12rpx;
}
.opt {
  flex: 1;
  padding: 12rpx 0;
  border: 2rpx solid var(--staff-border);
  font-size: 22rpx;
  text-align: center;
}
.opt.on {
  color: var(--staff-info);
  border-color: var(--staff-info);
  background: var(--staff-info-soft);
}
.photo {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 2rpx solid var(--staff-border);
}
.warn {
  margin-top: 20rpx;
  color: var(--staff-accent-text);
  font-size: 24rpx;
  font-weight: 700;
}
.submit-hint {
  padding: 12rpx 28rpx;
  color: var(--staff-muted);
  font-size: 24rpx;
}
.footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12rpx;
  padding: 12rpx 28rpx calc(16rpx + env(safe-area-inset-bottom));
}
.arrow {
  color: var(--staff-muted);
  font-size: 40rpx;
}
</style>
