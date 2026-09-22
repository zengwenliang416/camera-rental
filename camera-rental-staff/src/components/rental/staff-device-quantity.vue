<script setup lang="ts">
import type { RentalOrderItem } from '@/api/rental/order'
import { computed, ref, watch } from 'vue'
import { updateDeviceQuantity } from '@/api/rental/order'
import { useAccess } from '@/hooks/useAccess'

const props = defineProps<{ item: RentalOrderItem, sourceType?: string, status?: string }>()
const emit = defineEmits<{ updated: [] }>()
const { hasAccessByCodes } = useAccess()
const editing = ref(false)
const saving = ref(false)
const quantity = ref(1)
const expected = ref(1)
const error = ref('')
const editable = computed(() => props.sourceType === 'XIANYU'
  && props.status === 'PENDING_ALLOCATION'
  && !(props.item.assignments?.length || props.item.assignedQuantity)
  && hasAccessByCodes(['rental:device:assign', 'rental:xianyu:ship']))
watch(() => props.item, () => {
  editing.value = false
  error.value = ''
})
function edit() {
  expected.value = props.item.requiredQuantity || 1
  quantity.value = expected.value
  error.value = ''
  editing.value = true
}
async function save() {
  if (saving.value)
    return
  if (!Number.isInteger(quantity.value) || quantity.value < 1 || quantity.value > 999) {
    error.value = '请输入 1–999 的整数台数'
    return
  }
  saving.value = true
  error.value = ''
  try {
    await updateDeviceQuantity(props.item.id, quantity.value, expected.value)
    editing.value = false
    emit('updated')
    uni.showToast({ title: '实际台数已保存', icon: 'success' })
  } catch (err) {
    error.value = err instanceof Error ? err.message : '保存失败，请刷新订单后重试'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <view v-if="editable" class="quantity-editor" @click.stop>
    <wd-button v-if="!editing" size="small" variant="plain" @click="edit">
      修改实际台数
    </wd-button>
    <template v-else>
      <view class="hint">
        实际设备台数，渠道购买数量仅用于计价；修改不改变租金。
      </view>
      <view class="controls">
        <wd-input-number v-model="quantity" :min="1" :max="999" :precision="0" :disabled="saving" />
        <wd-button size="small" :loading="saving" @click="save">
          保存台数
        </wd-button>
        <wd-button size="small" variant="plain" :disabled="saving" @click="editing = false">
          取消
        </wd-button>
      </view>
    </template>
    <view v-if="error" class="error">
      {{ error }}
    </view>
  </view>
</template>

<style scoped>
.quantity-editor {
  margin-top: 16rpx;
}
.hint {
  color: var(--staff-muted);
  font-size: 24rpx;
  margin-bottom: 12rpx;
}
.controls {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}
.error {
  color: var(--staff-danger);
  font-size: 24rpx;
  margin-top: 12rpx;
}
</style>
