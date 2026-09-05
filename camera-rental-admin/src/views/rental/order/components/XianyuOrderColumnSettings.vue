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

      <div class="mb-6px text-12px font-600 text-[var(--el-text-color-secondary)]">
        已显示（拖动排序）
      </div>
      <draggable
        :list="visibleList"
        :animation="150"
        handle=".column-drag-handle"
        :item-key="identity"
        class="flex flex-col gap-4px"
        @end="handleDragEnd"
      >
        <template #item="{ element: key }">
          <div class="flex items-center gap-6px">
            <Icon
              icon="ic:round-drag-indicator"
              class="column-drag-handle cursor-move text-[var(--el-text-color-secondary)]"
            />
            <el-checkbox
              :model-value="true"
              :disabled="lockedColumnKeys.has(key)"
              :label="columnLabel(columnByKey.get(key)!)"
              @change="(checked) => toggleColumn(key, checked === true)"
            />
            <span class="ml-auto text-12px text-[var(--el-text-color-secondary)]">
              {{ groupLabel(columnByKey.get(key)!.group) }}
            </span>
          </div>
        </template>
      </draggable>

      <template v-if="hiddenColumns.length > 0">
        <el-divider class="!my-8px" />
        <div class="mb-6px text-12px font-600 text-[var(--el-text-color-secondary)]">未显示</div>
        <div class="grid grid-cols-2 gap-x-16px gap-y-4px">
          <el-checkbox
            v-for="column in hiddenColumns"
            :key="column.key"
            :model-value="false"
            :label="columnLabel(column)"
            @change="(checked) => toggleColumn(column.key, checked === true)"
          />
        </div>
      </template>
    </div>
  </el-popover>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { useI18n } from '@/hooks/web/useI18n'
import {
  XIANYU_ORDER_COLUMNS,
  XIANYU_ORDER_COLUMN_GROUPS,
  resetXianyuOrderColumnKeys,
  sanitizePersistedXianyuOrderColumnKeys,
  selectAllXianyuOrderColumnKeys,
  type XianyuOrderColumnDefinition,
  type XianyuOrderColumnGroup,
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

const selectedKeys = computed<XianyuOrderColumnKey[]>(() =>
  sanitizePersistedXianyuOrderColumnKeys(props.modelValue)
)

const columnByKey = new Map<XianyuOrderColumnKey, XianyuOrderColumnDefinition>(
  XIANYU_ORDER_COLUMNS.map((column) => [column.key, column])
)
const groupLabelByKey = new Map<XianyuOrderColumnGroup, string>(
  XIANYU_ORDER_COLUMN_GROUPS.map((group) => [group.key, group.label])
)
const lockedColumnKeys = new Set(
  XIANYU_ORDER_COLUMNS.filter((column) => column.locked).map((column) => column.key)
)

// Local mutable copy for vuedraggable; re-synced when modelValue changes from outside.
const visibleList = ref<XianyuOrderColumnKey[]>([...selectedKeys.value])
watch(selectedKeys, (keys) => {
  if (keys.length !== visibleList.value.length || keys.some((key, i) => visibleList.value[i] !== key)) {
    visibleList.value = [...keys]
  }
})

const hiddenColumns = computed(() =>
  XIANYU_ORDER_COLUMNS.filter((column) => !selectedKeys.value.includes(column.key))
)

const selectableColumnCount = XIANYU_ORDER_COLUMNS.length - lockedColumnKeys.size

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

const identity = (key: XianyuOrderColumnKey) => key

const columnLabel = (column: XianyuOrderColumnDefinition) => {
  if (!column.labelKey) return column.label

  const translated = t(column.labelKey)
  return translated === column.labelKey ? column.label : translated
}

const groupLabel = (group: XianyuOrderColumnGroup) => groupLabelByKey.get(group) ?? group

const emitVisible = (keys: XianyuOrderColumnKey[]) => {
  emit('update:modelValue', sanitizePersistedXianyuOrderColumnKeys(keys))
}

const handleDragEnd = () => emitVisible(visibleList.value)

const toggleColumn = (key: XianyuOrderColumnKey, checked: boolean) => {
  const next = visibleList.value.filter((existing) => existing !== key)
  if (checked) next.push(key)
  emitVisible(next)
}

const toggleSelectAll = (checked: boolean | string | number) => {
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
