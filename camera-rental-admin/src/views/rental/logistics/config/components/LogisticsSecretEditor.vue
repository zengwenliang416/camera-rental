<template>
  <div class="secret-editor">
    <div class="secret-editor__state">
      {{
        configured
          ? t('rental.logistics.secretConfigured', { masked: masked || '********' })
          : t('rental.logistics.secretMissing')
      }}
    </div>
    <el-radio-group :model-value="action" @update:model-value="updateAction">
      <el-radio-button v-if="allowKeep" value="KEEP">
        {{ t('rental.logistics.secretKeep') }}
      </el-radio-button>
      <el-radio-button value="REPLACE">
        {{ t('rental.logistics.secretReplace') }}
      </el-radio-button>
      <el-radio-button v-if="allowKeep" value="CLEAR">
        {{ t('rental.logistics.secretClear') }}
      </el-radio-button>
    </el-radio-group>
    <el-input
      v-if="action === 'REPLACE'"
      class="mt-12px"
      :model-value="value"
      type="password"
      show-password
      clearable
      autocomplete="new-password"
      :placeholder="t('rental.logistics.secretValuePlaceholder', { label })"
      @update:model-value="$emit('update:value', $event)"
    />
  </div>
</template>

<script lang="ts" setup>
import type { RentalLogisticsSecretAction } from '@/api/rental/logistics'
import { useI18n } from '@/hooks/web/useI18n'

defineProps<{
  action: RentalLogisticsSecretAction
  value: string
  configured: boolean
  masked?: string
  allowKeep: boolean
  label: string
}>()

const emit = defineEmits<{
  'update:action': [action: RentalLogisticsSecretAction]
  'update:value': [value: string]
}>()

const { t } = useI18n()

const updateAction = (value: string | number | boolean | undefined) => {
  emit('update:action', String(value) as RentalLogisticsSecretAction)
}
</script>

<style scoped>
.secret-editor {
  width: 100%;
  padding: 14px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.secret-editor__state {
  width: 100%;
  margin: 0 0 10px;
  font-size: 12px;
  line-height: 1.5;
  color: var(--el-text-color-secondary);
}
</style>
