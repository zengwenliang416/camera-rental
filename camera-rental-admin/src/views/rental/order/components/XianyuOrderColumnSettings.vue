<template>
  <el-popover placement="bottom-end" :width="460" trigger="click">
    <template #reference>
      <el-button>
        <Icon class="mr-5px" icon="ep:setting" />
        列设置
      </el-button>
    </template>

    <div class="xianyu-order-column-settings">
      <div class="flex items-center justify-between">
        <el-checkbox
          :model-value="allColumnsSelected"
          :indeterminate="isIndeterminate"
          @change="toggleSelectAll"
        >
          全选
        </el-checkbox>
        <el-button link type="primary" @click="resetToDefault">恢复默认</el-button>
      </div>

      <el-divider class="!my-8px" />

      <div class="grid grid-cols-2 gap-x-16px gap-y-12px">
        <section v-for="group in groupedColumns" :key="group.key">
          <div class="mb-6px text-12px font-600 text-[var(--el-text-color-secondary)]">
            {{ group.label }}
          </div>
          <el-checkbox-group v-model="selectedKeys" class="flex flex-col gap-4px">
            <el-checkbox
              v-for="column in group.columns"
              :key="column.key"
              :disabled="column.locked"
              :label="column.key"
            >
              {{ columnLabel(column) }}
            </el-checkbox>
          </el-checkbox-group>
        </section>
      </div>
    </div>
  </el-popover>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import {
  XIANYU_ORDER_COLUMNS,
  XIANYU_ORDER_COLUMN_GROUPS,
  resetXianyuOrderColumnKeys,
  sanitizePersistedXianyuOrderColumnKeys,
  selectAllXianyuOrderColumnKeys,
  type XianyuOrderColumnDefinition,
  type XianyuOrderColumnKey
} from './xianyuOrderColumns'

defineOptions({ name: 'XianyuOrderColumnSettings' })

const props = defineProps<{
  modelValue: XianyuOrderColumnKey[]
}>()

const emit = defineEmits<{
  (event: 'update:modelValue', value: XianyuOrderColumnKey[]): void
}>()

const { t } = useI18n()

const selectedKeys = computed<XianyuOrderColumnKey[]>({
  get: () => sanitizePersistedXianyuOrderColumnKeys(props.modelValue),
  set: (value) => emit('update:modelValue', sanitizePersistedXianyuOrderColumnKeys(value))
})

const allColumnKeys = selectAllXianyuOrderColumnKeys()
const lockedColumnKeys = new Set(
  XIANYU_ORDER_COLUMNS.filter((column) => column.locked).map((column) => column.key)
)

const groupedColumns = computed(() =>
  XIANYU_ORDER_COLUMN_GROUPS.map((group) => ({
    ...group,
    columns: XIANYU_ORDER_COLUMNS.filter((column) => column.group === group.key)
  }))
)

const selectableColumnCount = allColumnKeys.filter((key) => !lockedColumnKeys.has(key)).length

const selectedOptionalColumnCount = computed(
  () => selectedKeys.value.filter((key) => !lockedColumnKeys.has(key)).length
)

const allColumnsSelected = computed(
  () => selectedOptionalColumnCount.value === selectableColumnCount
)

const isIndeterminate = computed(
  () =>
    selectedOptionalColumnCount.value > 0 &&
    selectedOptionalColumnCount.value < selectableColumnCount
)

const columnLabel = (column: XianyuOrderColumnDefinition) => {
  if (!column.labelKey) return column.label

  const translated = t(column.labelKey)
  return translated === column.labelKey ? column.label : translated
}

const toggleSelectAll = (checked: boolean) => {
  emit(
    'update:modelValue',
    checked ? selectAllXianyuOrderColumnKeys() : sanitizePersistedXianyuOrderColumnKeys([])
  )
}

const resetToDefault = () => {
  emit('update:modelValue', resetXianyuOrderColumnKeys())
}
</script>

<style scoped>
.xianyu-order-column-settings {
  max-height: 520px;
  overflow-y: auto;
}
</style>
