<script setup lang="ts">
import { onBackPress, onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { getDeviceScheduleDetail } from '@/api/rental/device'
import { getInspectionHistory, getInspectionTemplate, getPhoto, saveInspectionTemplate, submitInspection } from '@/api/rental/warehouse'
import type { InspectionCheck, InspectionRecord } from '@/api/rental/warehouse'
import { uploadInspectionPhoto } from '@/services/staffPhoto'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useAccess } from '@/hooks/useAccess'
import { operationKey, staffError } from '@/models/rental/staffOperations'

const openPage = (options: UniApp.NavigateToOptions) => uni.navigateTo(options)
const backToPrevious = () => uni.navigateBack()
const showPhotos = (options: UniApp.PreviewImageOptions) => uni.previewImage(options)
definePage({ style: { navigationStyle: 'custom' } })
const staffPageStyle = useStaffPageStyle()
const { hasAccessByCodes } = useAccess()
const deviceId = ref(0)
const assignmentId = ref(0)
const deviceNo = ref('')
const model = ref('')
const checks = ref<Array<InspectionCheck & {
  confirmed: boolean
}>>([])
const note = ref('')
const photos = ref<Array<{
  id: number
  url: string
}>>([])
const history = ref<InspectionRecord[]>([])
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const saved = ref(false)
const configuring = ref(false)
const templateDraft = ref<InspectionCheck[]>([])
let key = operationKey('inspection')
const complete = computed(() => checks.value.length > 0 && checks.value.every(c => c.confirmed && (c.expected == null || Number.isInteger(c.actual))))
const passed = computed(() => checks.value.every(c => c.result === 'PASS' && (c.expected == null || c.actual === c.expected)))
async function run(fn: () => Promise<void>) {
  if (busy.value)
    return
  busy.value = true
  error.value = ''
  try {
    await fn()
  } catch (e) {
    error.value = staffError(e, '操作失败，请刷新核对')
  } finally {
    busy.value = false
  }
}
async function load() {
  checks.value = []
  loading.value = true
  error.value = ''
  try {
    const detail = await getDeviceScheduleDetail(deviceId.value)
    deviceNo.value = detail.deviceNo
    model.value = detail.equipmentModelCode
    if (!assignmentId.value)
      assignmentId.value = detail.latestAssignment?.id || 0
    if (!assignmentId.value || detail.latestAssignment?.id !== assignmentId.value)
      throw new Error('设备租赁轮次已变化，请重新打开检测')
    if (!detail.latestAssignment?.returnedAt)
      throw new Error('请先登记实物收货，再进行检测')
    checks.value = (await getInspectionTemplate(model.value)).map(c => ({ ...c, actual: undefined, confirmed: false }))
    history.value = await getInspectionHistory(deviceId.value)
  } catch (e) {
    error.value = staffError(e, '检测数据加载失败')
  } finally {
    loading.value = false
  }
}
async function addPhoto() {
  if (photos.value.length >= 6)
    return
  await run(async () => {
    photos.value.push(await uploadInspectionPhoto(deviceId.value, assignmentId.value))
  })
}
async function submit() {
  if (busy.value || !complete.value)
    return
  const result = await uni.showModal({ title: passed.value ? '确认检测通过' : '确认转维修', content: passed.value ? '实物和配件已核对，通过后恢复可租。' : '检测存在问题，提交后设备保持维修隔离。' })
  if (!result.confirm)
    return
  await run(async () => {
    await submitInspection({ deviceId: deviceId.value, assignmentId: assignmentId.value, idempotencyKey: key, checks: checks.value.map(({ confirmed: _, ...c }) => c), photoIds: photos.value.map(p => p.id), note: note.value.trim() || undefined })
    saved.value = true
    uni.showToast({ title: passed.value ? '检测通过，可租' : '已转维修', icon: 'success' })
    uni.navigateBack()
  })
}
async function preview(record: InspectionRecord) {
  await run(async () => {
    const ids: number[] = JSON.parse(record.photoIdsJson || '[]')
    if (!ids.length) {
      uni.showToast({ title: '本次未附照片', icon: 'none' })
      return
    }
    const result = await Promise.all(ids.map(getPhoto))
    uni.previewImage({ urls: result.map(p => p.url) })
  })
}
function editTemplate() {
  templateDraft.value = checks.value.map(c => ({ label: c.label, result: 'PASS', expected: c.expected }))
  configuring.value = true
}
async function saveTemplate() {
  await run(async () => {
    await saveInspectionTemplate(model.value, templateDraft.value)
    configuring.value = false
    key = operationKey('inspection')
    await load()
  })
}
onBackPress(() => {
  if (busy.value)
    return true
  if (!saved.value && (checks.value.some(c => c.confirmed) || note.value || photos.value.length)) {
    uni.showModal({ title: '检测尚未提交', content: '返回将放弃本页未提交的检测结果，确认返回？', success: (result) => {
      if (result.confirm) {
        saved.value = true
        uni.navigateBack()
      }
    } })
    return true
  }
  return false
})
onLoad((q) => {
  deviceId.value = Number(q?.deviceId) || 0
  assignmentId.value = Number(q?.assignmentId) || 0
  void load()
})
</script>

<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="设备检测 / 维修复检" left-arrow safe-area-inset-top placeholder @click-left="backToPrevious()" />
    <scroll-view scroll-y class="workbench-content">
      <view class="workbench-card">
        <view class="workbench-title">
          {{ deviceNo }} · {{ model }}
        </view><view class="workbench-note">
          按型号清单检测。实收配件数量不符会判定不通过，通过后才恢复可租。
        </view>
      </view>
      <view v-if="error" class="workbench-error">
        {{ error }}<wd-button size="small" variant="plain" :disabled="busy" @click="load">
          重新加载
        </wd-button>
      </view>
      <view v-if="loading" class="workbench-note">
        正在加载…
      </view>
      <template v-else-if="checks.length && !configuring">
        <view v-for="check in checks" :key="check.label" class="workbench-card">
          <view class="workbench-title">
            {{ check.label }}
          </view>
          <view v-if="check.expected != null" class="workbench-note">
            应有 {{ check.expected }} 件 · 实收 <wd-input-number v-model="check.actual" :min="0" :max="999" :precision="0" :disabled="busy" />
          </view>
          <view class="workbench-action-grid">
            <wd-button size="small" :variant="check.confirmed && check.result === 'PASS' ? 'base' : 'plain'" :disabled="busy" @click="check.result = 'PASS'; check.confirmed = true">
              正常 / 齐全
            </wd-button>
            <wd-button size="small" :variant="check.confirmed && check.result === 'FAIL' ? 'base' : 'plain'" :disabled="busy" @click="check.result = 'FAIL'; check.confirmed = true">
              有问题 / 缺件
            </wd-button>
          </view>
        </view>
        <view class="workbench-card">
          <wd-textarea v-model="note" placeholder="检测、维修或缺件说明" :maxlength="400" :disabled="busy" />
          <view class="workbench-action-grid">
            <wd-button v-if="hasAccessByCodes(['rental:device:assign'])" size="small" variant="plain" :loading="busy" :disabled="photos.length >= 6" @click="addPhoto">
              拍照 / 相册（{{ photos.length }}/6）
            </wd-button>
            <wd-button size="small" variant="plain" @click="openPage({ url: `/pages-rental/issues/index?deviceId=${deviceId}` })">
              登记维修异常
            </wd-button>
          </view>
          <view v-if="photos.length" class="workbench-action-grid">
            <view v-for="photo in photos" :key="photo.id">
              <wd-img :src="photo.url" width="140rpx" height="140rpx" @click="showPhotos({ urls: photos.map(p => p.url), current: photo.url })" /><wd-button size="small" variant="plain" :disabled="busy" @click="photos = photos.filter(p => p.id !== photo.id)">
                移除
              </wd-button>
            </view>
          </view>
          <view class="workbench-primary-action">
            <wd-button v-if="hasAccessByCodes(['rental:device:assign'])" block :loading="busy" :disabled="!complete || loading" @click="submit">
              {{ !complete ? '请逐项完成检测' : passed ? '提交通过 · 恢复可租' : '提交不通过 · 转维修' }}
            </wd-button>
          </view>
          <view v-if="hasAccessByCodes(['rental:device:update'])" class="workbench-secondary-action">
            <wd-button size="small" variant="plain" :disabled="busy" @click="editTemplate">
              配置本型号清单
            </wd-button>
          </view>
        </view>
      </template>
      <view v-if="configuring" class="workbench-card">
        <view class="workbench-note">
          修改 {{ model }} 共用清单，后续该型号检测均使用此配置。配件数量留空表示只检查状态。
        </view>
        <view v-for="(check, index) in templateDraft" :key="index" class="workbench-template-item">
          <wd-input v-model="check.label" placeholder="检查项目 / 配件名" :maxlength="60" />
          <view class="workbench-row workbench-note">
            <text>配件应有数量</text>
            <wd-input-number v-model="check.expected" :min="0" :max="999" :precision="0" />
          </view>
          <view class="workbench-action-grid">
            <wd-button size="small" variant="plain" @click="check.expected = undefined">
              只检查状态
            </wd-button>
            <wd-button size="small" variant="plain" @click="templateDraft.splice(index, 1)">
              删除项目
            </wd-button>
          </view>
        </view>
        <view class="workbench-action-grid">
          <wd-button size="small" variant="plain" :disabled="templateDraft.length >= 30" @click="templateDraft.push({ label: '', result: 'PASS' })">
            增加项目
          </wd-button>
          <wd-button size="small" variant="plain" @click="configuring = false">
            取消
          </wd-button>
        </view>
        <wd-button block :loading="busy" @click="saveTemplate">
          保存清单
        </wd-button>
      </view>
      <view v-if="history.length" class="workbench-card">
        <view class="workbench-title">
          最近检测记录
        </view>
        <view v-for="record in history" :key="record.id" class="workbench-card">
          <view>{{ record.passed ? '检测通过' : '检测不通过' }} · {{ record.createTime }}</view><view class="workbench-note">
            {{ record.note }}
          </view><view v-for="(item, index) in JSON.parse(record.checklistJson || '[]')" :key="index" class="workbench-note">
            {{ item.label }}：{{ item.result === 'PASS' ? '正常' : '有问题' }}{{ item.expected != null ? ` · 应有 ${item.expected} / 实收 ${item.actual}` : '' }}
          </view><wd-button size="small" variant="plain" :disabled="busy" @click="preview(record)">
            查看照片
          </wd-button>
        </view>
      </view>
    </scroll-view>
  </view>
</template>
