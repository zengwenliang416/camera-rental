<script setup lang="ts">
import { onShow } from '@dcloudio/uni-app'
import { computed, ref, watch } from 'vue'
import { closeStocktake, createStocktake, getStocktake, getStocktakes, moveStocktakeDevice, scanStocktake } from '@/api/rental/warehouse'
import type { Stocktake, StocktakeLine } from '@/api/rental/warehouse'
import { lookupRentalDevice } from '@/api/rental/device'
import { useStaffScanner } from '@/hooks/useStaffScanner'
import { useStaffPageStyle } from '@/hooks/useStaffPageStyle'
import { useAccess } from '@/hooks/useAccess'
import { operationKey, staffError } from '@/models/rental/staffOperations'

const backToPrevious = () => uni.navigateBack()
definePage({ style: { navigationStyle: 'custom' } })
const staffPageStyle = useStaffPageStyle()
const { hasAccessByCodes } = useAccess()
const warehouse = ref('')
const manual = ref('')
const rows = ref<Stocktake[]>([])
const selected = ref<Stocktake>()
const busy = ref(false)
const error = ref('')
const filter = ref('ALL')
const total = ref(0)
let page = 0
let requestKey = operationKey('stocktake')
watch(warehouse, () => {
  requestKey = operationKey('stocktake')
})
const lines = computed(() => (selected.value?.lines || []).filter(line => filter.value === 'ALL' || (filter.value === 'MISSING' ? line.expected && !line.scanned : !line.expected && line.scanned)))
const scanned = computed(() => selected.value?.lines?.filter(line => line.scanned).length || 0)
async function perform(fn: () => Promise<void>) {
  if (busy.value)
    return
  busy.value = true
  error.value = ''
  try {
    await fn()
  } catch (e) {
    error.value = staffError(e, '盘点操作失败，请刷新核对')
  } finally {
    busy.value = false
  }
}
async function list(more = false) {
  const next = more ? page + 1 : 1
  const result = await getStocktakes(next)
  rows.value = more ? [...rows.value, ...result.list] : result.list
  total.value = result.total
  page = next
}
async function open(id: number) {
  selected.value = await getStocktake(id)
}
async function create() {
  await perform(async () => {
    const id = await createStocktake(warehouse.value.trim(), requestKey)
    requestKey = operationKey('stocktake')
    await open(id)
  })
}
async function accept(raw: string) {
  const id = selected.value?.id
  if (!id || selected.value?.status !== 'OPEN' || !hasAccessByCodes(['rental:device:assign']))
    return
  await perform(async () => {
    const device = await lookupRentalDevice(raw)
    await scanStocktake(id, device.id)
    await open(id)
    manual.value = ''
  })
}
const { scan } = useStaffScanner(result => accept(result.text))
async function close() {
  const id = selected.value?.id
  if (!id)
    return
  const result = await uni.showModal({ title: '结束本次盘点', content: '保留漏盘、多出和仓位差异，结束后不能继续扫描。确认已核对？' })
  if (result.confirm) {
    await perform(async () => {
      await closeStocktake(id)
      await open(id)
    })
  }
}
async function move(line: StocktakeLine) {
  const id = selected.value?.id
  if (!id)
    return
  const result = await uni.showModal({ title: '确认实物仓位', content: `将 ${line.deviceNo} 的仓位从 ${line.originalWarehouse || '未登记'} 改为 ${selected.value?.warehouseCode}，仅更新仓位。` })
  if (result.confirm) {
    await perform(async () => {
      await moveStocktakeDevice(id, line.deviceId)
      await open(id)
    })
  }
}
onShow(() => {
  void perform(async () => {
    if (selected.value)
      await open(selected.value.id)
    else
      await list()
  })
})
</script>

<template>
  <view class="mobile-workbench" :style="staffPageStyle">
    <wd-navbar title="扫码盘点" left-arrow safe-area-inset-top placeholder @click-left="backToPrevious()" />
    <scroll-view scroll-y class="workbench-content">
      <view v-if="error" class="workbench-error">
        {{ error }}
      </view>
      <template v-if="!selected">
        <view class="workbench-card">
          <view class="workbench-note">
            按仓位盘点在库、待检测和维修设备；未回仓设备不计入应盘。扫描只登记实物，不自动调整库存。
          </view>
          <wd-input v-model="warehouse" placeholder="输入准确仓位编码" :maxlength="64" />
          <wd-button v-if="hasAccessByCodes(['rental:device:assign'])" :loading="busy" :disabled="!warehouse.trim()" @click="create">
            新建盘点单
          </wd-button>
        </view>
        <view v-for="item in rows" :key="item.id" class="workbench-card" @click="perform(() => open(item.id))">
          {{ item.warehouseCode }} · {{ item.status === 'OPEN' ? '盘点中' : '已结束' }} · #{{ item.id }}
        </view>
        <wd-button v-if="rows.length < total" variant="plain" :loading="busy" @click="perform(() => list(true))">
          加载更多
        </wd-button>
      </template>
      <template v-else>
        <view class="workbench-card">
          <view class="workbench-title">
            {{ selected.warehouseCode }} · 已扫 {{ scanned }} 台
          </view>
          <view class="workbench-action-grid">
            <wd-button size="small" variant="plain" :disabled="busy" @click="selected = undefined; perform(() => list())">
              盘点记录
            </wd-button>
            <wd-button size="small" variant="plain" :loading="busy" @click="perform(() => open(selected!.id))">
              刷新
            </wd-button>
          </view>
          <template v-if="selected.status === 'OPEN' && hasAccessByCodes(['rental:device:assign'])">
            <wd-button block :disabled="busy" @click="scan">
              扫描设备 · 可连续按侧键
            </wd-button>
            <wd-input v-model="manual" placeholder="完整设备编号" @confirm="accept(manual)" />
            <view class="workbench-action-grid">
              <wd-button size="small" :disabled="busy || !manual.trim()" @click="accept(manual)">
                登记设备
              </wd-button>
              <wd-button size="small" variant="plain" :disabled="busy" @click="close">
                结束并核对差异
              </wd-button>
            </view>
          </template>
          <view class="workbench-actions">
            <wd-button v-for="tab in [{ key: 'ALL', label: '全部' }, { key: 'MISSING', label: '漏盘' }, { key: 'EXTRA', label: '多出' }]" :key="tab.key" size="small" :variant="filter === tab.key ? 'base' : 'plain'" @click="filter = tab.key">
              {{ tab.label }}
            </wd-button>
          </view>
        </view>
        <view v-for="line in lines" :key="line.deviceId" class="workbench-card">
          <view class="workbench-title">
            {{ line.deviceNo }} · {{ line.scanned ? line.expected ? '已盘' : '多出' : '未盘' }}
          </view>
          <view class="workbench-note">
            原仓位：{{ line.originalWarehouse || '未登记' }}{{ line.adjusted ? ' · 已确认新仓位' : '' }}
          </view>
          <wd-button v-if="selected.status === 'CLOSED' && line.scanned && !line.adjusted && line.originalWarehouse !== selected.warehouseCode && hasAccessByCodes(['rental:device:update'])" size="small" variant="plain" :disabled="busy" @click="move(line)">
            确认移入当前仓位
          </wd-button>
        </view>
      </template>
    </scroll-view>
  </view>
</template>
