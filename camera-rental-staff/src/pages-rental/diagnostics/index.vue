<script setup lang="ts">
import { computed, ref } from 'vue'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import { scannerConnected } from '@/services/scanner'
import { lookupRentalDevice } from '@/api/rental/device'
import { STAFF_VERSION } from '@/config/staffVersion'
import { sanitizeStaffMessage, staffError } from '@/models/rental/staffOperations'

const backToPrevious = () => uni.navigateBack()
definePage({ style: { navigationStyle: 'custom' } })
const staffPageStyle = useStaffPageStyle()
const info = uni.getSystemInfoSync()
const reading = ref('尚未收到扫码数据')
const query = ref('尚未查询')
const busy = ref(false)
const at = ref('')
const diagnosticId = ref('')
const { scan } = useStaffScanner(async (result) => {
  if (busy.value)
    return
  at.value = new Date().toISOString()
  diagnosticId.value = `scan-${Date.now()}`
  reading.value = `已读取 · ${result.source === 'vendor-broadcast' ? '扫码头' : '摄像头'} · ${result.text.startsWith('CRD1|') ? '设备永久码' : '其他条码'}`
  query.value = '正在核验设备…'
  busy.value = true
  try {
    await lookupRentalDevice(result.text)
    query.value = '服务器查询成功，设备已识别'
  } catch (e) {
    query.value = sanitizeStaffMessage(staffError(e, '查询失败'))
  } finally {
    busy.value = false
  }
})
const report = computed(() => [
  `捷租达 ${STAFF_VERSION}`,
  `设备 ${info.brand || ''} ${info.model || ''}`,
  `系统 ${info.system || ''}`,
  `扫码头 ${scannerConnected.value ? '已连接' : '不可用'}`,
  reading.value,
  query.value,
  `时间 ${at.value || '未扫描'}`,
  `本机诊断编号 ${diagnosticId.value || '-'}`,
].join('\n'))
function copy() {
  uni.setClipboardData({ data: report.value })
}
</script>

<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="扫码诊断" left-arrow safe-area-inset-top placeholder @click-left="backToPrevious()" />
    <view class="workbench-card">
      <view class="workbench-title">
        1 · 扫码头连接
      </view>
      <view class="workbench-note">
        {{ scannerConnected ? '已连接，可按侧键测试' : '扫码头不可用，可测试摄像头识别' }}
      </view>
      <view class="workbench-title">
        2 · 读取条码
      </view><view class="workbench-note">
        {{ reading }}
      </view>
      <view class="workbench-title">
        3 · 服务器核验
      </view><view class="workbench-note">
        {{ query }}
      </view>
      <wd-button block :loading="busy" @click="scan">
        扫描设备永久码进行诊断
      </wd-button>
      <view class="workbench-note">
        只查询设备，不执行发货或回仓。诊断报告不包含原始二维码、签名或客户资料。
      </view>
    </view>
    <view class="workbench-card">
      <view class="workbench-note" style="white-space: pre-wrap">
        {{ report }}
      </view>
      <wd-button variant="plain" @click="copy">
        复制诊断信息
      </wd-button>
    </view>
  </view>
</template>
