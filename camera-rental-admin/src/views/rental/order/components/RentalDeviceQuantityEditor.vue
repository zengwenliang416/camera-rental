<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import {
  getRentalOrderDetail,
  updateDeviceQuantity,
  type RentalScheduleOrderDetailVO
} from '@/api/rental/schedule'
import { checkPermi } from '@/utils/permission'

const props = defineProps<{ orderId: number }>()
const detail = ref<RentalScheduleOrderDetailVO>()
const loading = ref(false)
const saving = ref<number>()
const error = ref('')
const quantities = ref<Record<number, number>>({})
let generation = 0
const editable = computed(
  () =>
    detail.value?.sourceType === 'XIANYU' &&
    detail.value.status === 'PENDING_ALLOCATION' &&
    !detail.value.items.some((item) => item.assignments?.length || item.assignedQuantity) &&
    checkPermi(['rental:device:assign', 'rental:xianyu:ship'])
)
async function load() {
  const current = ++generation
  loading.value = true
  detail.value = undefined
  error.value = ''
  try {
    const result = await getRentalOrderDetail(props.orderId)
    if (current !== generation) return
    detail.value = result
    quantities.value = Object.fromEntries(
      result.items.map((item) => [item.id, item.requiredQuantity])
    )
  } catch {
    if (current === generation) error.value = '实际设备台数加载失败，请重试'
  } finally {
    if (current === generation) loading.value = false
  }
}
async function save(itemId: number, expected: number) {
  if (saving.value) return
  const quantity = quantities.value[itemId]
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > 999) {
    error.value = '请输入 1–999 的整数台数'
    return
  }
  const current = generation
  saving.value = itemId
  error.value = ''
  try {
    await updateDeviceQuantity(itemId, quantity, expected)
    if (current === generation) await load()
  } catch {
    if (current === generation) error.value = '保存失败；若订单已被他人修改或开始履约，请刷新后核对'
  } finally {
    saving.value = undefined
  }
}
watch(() => props.orderId, load, { immediate: true })
</script>

<template>
  <section v-loading="loading" class="device-quantity-editor">
    <p>实际设备台数（渠道购买数量仅用于计价，修改台数不改变租金）</p>
    <div v-for="item in detail?.items || []" :key="item.id" class="quantity-row">
      <span>{{ item.equipmentModelCode || '设备' }} · {{ item.requiredQuantity }} 台</span>
      <template v-if="editable">
        <el-input-number
          v-model="quantities[item.id]"
          :min="1"
          :max="999"
          :precision="0"
          :disabled="!!saving"
        />
        <el-button
          type="primary"
          :loading="saving === item.id"
          :disabled="!!saving || quantities[item.id] === item.requiredQuantity"
          @click="save(item.id, item.requiredQuantity)"
          >保存台数</el-button
        >
      </template>
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-button v-if="error" :disabled="!!saving" @click="load">刷新台数</el-button>
  </section>
</template>

<style scoped>
.device-quantity-editor {
  margin-top: 16px;
}
.quantity-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}
</style>
