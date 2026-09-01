<template>
  <dl class="identifier-summary">
    <div v-for="item in visibleItems" :key="item.key" class="identifier-summary__item">
      <dt>{{ t(item.labelKey) }}</dt>
      <dd>
        <code :class="{ 'is-empty': !item.value }">{{ item.value || '—' }}</code>
        <el-button v-if="item.value" link type="primary" @click="copyIdentifier(item.value)">
          <Icon icon="ep:copy-document" />
        </el-button>
      </dd>
    </div>
  </dl>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useClipboard } from '@vueuse/core'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'

const props = defineProps<{
  xgjProductId?: string
  xianyuItemId?: string
  xgjSkuId?: string
  xianyuSkuId?: string
  scope?: 'product' | 'sku' | 'all'
}>()

const { t } = useI18n()
const message = useMessage()
const { copy, isSupported } = useClipboard({ legacy: true })

const visibleItems = computed(() => {
  const items = [
    {
      key: 'xgjProductId',
      labelKey: 'rental.configuration.xgjProductId',
      value: props.xgjProductId
    },
    {
      key: 'xianyuItemId',
      labelKey: 'rental.configuration.xianyuItemId',
      value: props.xianyuItemId
    },
    {
      key: 'xgjSkuId',
      labelKey: 'rental.configuration.xgjSkuId',
      value: props.xgjSkuId
    },
    {
      key: 'xianyuSkuId',
      labelKey: 'rental.configuration.xianyuSkuId',
      value: props.xianyuSkuId
    }
  ]
  if (props.scope === 'product') return items.slice(0, 2)
  if (props.scope === 'sku') return items.slice(2)
  return items
})

const copyIdentifier = async (value: string) => {
  if (!isSupported.value) {
    message.error(t('common.copyError'))
    return
  }
  try {
    await copy(value)
    message.success(t('common.copySuccess'))
  } catch {
    message.error(t('common.copyError'))
  }
}
</script>

<style scoped>
.identifier-summary {
  display: grid;
  gap: 7px;
  margin: 0;
}

.identifier-summary__item {
  min-width: 0;
}

.identifier-summary dt {
  margin-bottom: 2px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}

.identifier-summary dd {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  margin: 0;
}

.identifier-summary code {
  overflow: hidden;
  font-family: SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
  color: var(--el-text-color-primary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.identifier-summary code.is-empty {
  color: var(--el-text-color-placeholder);
}
</style>
