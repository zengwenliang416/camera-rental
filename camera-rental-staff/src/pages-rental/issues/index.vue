<script setup lang="ts">
import { onHide, onLoad, onShow } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { actOnIssue, createIssue, getIssues } from '@/api/rental/warehouse'
import type { StaffIssue } from '@/api/rental/warehouse'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useAccess } from '@/hooks/useAccess'
import { useUserStore } from '@/store/user'
import { staffError } from '@/models/rental/staffOperations'

const openPage = (options: UniApp.NavigateToOptions) => uni.navigateTo(options)
const backToPrevious = () => uni.navigateBack()
definePage({ style: { navigationStyle: 'custom' } })
const staffPageStyle = useStaffPageStyle()
const { hasAccessByCodes } = useAccess()
const user = useUserStore()
const status = ref('OPEN')
const rows = ref<StaffIssue[]>([])
const total = ref(0)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const title = ref('')
const note = ref('')
const resolution = ref<Record<number, string>>({})
const creating = ref(false)
let orderId: number | undefined
let deviceId: number | undefined
let page = 0
let generation = 0
async function load(reset = true) {
  if (loading.value && !reset)
    return
  const version = ++generation
  loading.value = true
  error.value = ''
  const next = reset ? 1 : page + 1
  try {
    const result = await getIssues(status.value, next)
    if (generation !== version)
      return
    rows.value = reset ? result.list : [...rows.value, ...result.list]
    total.value = result.total
    page = next
  } catch (e) {
    if (version === generation)
      error.value = staffError(e, '异常记录加载失败')
  } finally {
    if (version === generation)
      loading.value = false
  }
}
async function save() {
  if (busy.value || !title.value.trim())
    return
  busy.value = true
  try {
    await createIssue({ title: title.value.trim(), note: note.value.trim(), rentalOrderId: orderId, deviceId })
    title.value = ''
    note.value = ''
    creating.value = false
    status.value = 'OPEN'
    await load()
  } catch (e) {
    error.value = staffError(e, '提交失败')
  } finally {
    busy.value = false
  }
}
async function action(item: StaffIssue, action: 'CLAIM' | 'NOTE' | 'RESOLVE' | 'REOPEN') {
  if (busy.value)
    return
  busy.value = true
  try {
    await actOnIssue({ id: item.id, revision: item.revision, action, note: resolution.value[item.id] })
    await load()
  } catch (e) {
    error.value = staffError(e, '处理失败，请刷新记录')
  } finally {
    busy.value = false
  }
}
function change(value: string) {
  status.value = value
  void load()
}
onLoad((q) => {
  orderId = Number(q?.orderId) || undefined
  deviceId = Number(q?.deviceId) || undefined
  creating.value = !!(orderId || deviceId)
})
onShow(() => {
  void load()
})
onHide(() => {
  generation++
  loading.value = false
})
</script>

<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="异常与维修处理" left-arrow safe-area-inset-top placeholder @click-left="backToPrevious()" />
    <scroll-view scroll-y class="workbench-content">
      <view class="workbench-card">
        <view class="workbench-note">
          记录同步到服务器，可交接处理。处理记录不会自动修改订单或设备状态。
        </view>
        <view class="workbench-actions">
          <wd-button v-for="tab in [{ key: 'OPEN', label: '待接单' }, { key: 'PROCESSING', label: '处理中' }, { key: 'RESOLVED', label: '已处理' }]" :key="tab.key" size="small" :variant="status === tab.key ? 'base' : 'plain'" @click="change(tab.key)">
            {{ tab.label }}
          </wd-button>
        </view>
        <view class="workbench-secondary-action">
          <wd-button v-if="hasAccessByCodes(['rental:device:assign', 'rental:xianyu:ship'])" size="small" variant="plain" @click="creating = !creating">
            登记异常
          </wd-button>
        </view>
      </view>
      <view v-if="creating" class="workbench-card">
        <view class="workbench-note">
          {{ deviceId ? `关联设备 ${deviceId}` : '' }} {{ orderId ? `关联订单 ${orderId}` : '' }}
        </view>
        <wd-input v-model="title" placeholder="异常或维修项目" :maxlength="100" />
        <wd-textarea v-model="note" placeholder="现象、原因及需要协助的事项" :maxlength="600" />
        <wd-button :loading="busy" :disabled="!title.trim()" @click="save">
          提交记录
        </wd-button>
      </view>
      <view v-if="error" class="workbench-error">
        {{ error }}<wd-button size="small" variant="plain" @click="load()">
          刷新
        </wd-button>
      </view>
      <view v-if="!loading && !rows.length" class="workbench-note">
        暂无记录
      </view>
      <view v-for="item in rows" :key="item.id" class="workbench-card">
        <view class="workbench-title">
          {{ item.title }}
        </view>
        <view class="workbench-note" style="white-space: pre-wrap">
          {{ item.note || '暂无说明' }}
        </view>
        <view class="workbench-note">
          负责人：{{ item.ownerId ? `员工 ${item.ownerId}` : '待认领' }}
        </view>
        <view class="workbench-action-grid">
          <wd-button v-if="item.rentalOrderId" size="small" variant="plain" @click="openPage({ url: `/pages-rental/orders/detail?id=${item.rentalOrderId}` })">
            查看订单
          </wd-button>
          <wd-button v-if="item.status === 'OPEN' && hasAccessByCodes(['rental:device:assign', 'rental:xianyu:ship'])" size="small" :loading="busy" @click="action(item, 'CLAIM')">
            我来处理
          </wd-button>
        </view>
        <template v-if="String(item.ownerId) === String(user.userInfo.userId)">
          <wd-textarea v-if="item.status === 'PROCESSING'" v-model="resolution[item.id]" placeholder="填写处理结果后结单" :maxlength="300" />
          <view class="workbench-action-grid">
            <wd-button v-if="item.status === 'PROCESSING'" size="small" variant="plain" :disabled="busy || !resolution[item.id]?.trim()" @click="action(item, 'NOTE')">
              保存处理进度
            </wd-button>
            <wd-button size="small" :loading="busy" :disabled="item.status === 'PROCESSING' && !resolution[item.id]?.trim()" @click="action(item, item.status === 'RESOLVED' ? 'REOPEN' : 'RESOLVE')">
              {{ item.status === 'RESOLVED' ? '重新打开' : '记录处理完成' }}
            </wd-button>
          </view>
        </template>
      </view>
      <wd-button v-if="rows.length < total" block variant="plain" :loading="loading" @click="load(false)">
        加载更多
      </wd-button>
    </scroll-view>
  </view>
</template>
