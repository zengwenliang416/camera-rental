<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('rental.configuration.impactTitle')"
    width="min(640px, calc(100vw - 24px))"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      :title="t('rental.configuration.impactWarning')"
    />
    <div class="impact-grid">
      <article>
        <span>{{ t('rental.configuration.affectedOrders') }}</span>
        <strong>{{ impact?.scannedCount ?? 0 }}</strong>
      </article>
      <article>
        <span>{{ t('rental.configuration.withoutInternalOrder') }}</span>
        <strong>{{ impact?.withoutInternalOrderCount ?? 0 }}</strong>
      </article>
      <article>
        <span>{{ t('rental.configuration.autoUpdate') }}</span>
        <strong>{{ impact?.mutableInternalOrderCount ?? 0 }}</strong>
      </article>
      <article>
        <span>{{ t('rental.configuration.completedProtected') }}</span>
        <strong>{{ impact?.protectedOrderCount ?? 0 }}</strong>
      </article>
      <article>
        <span>{{ t('rental.configuration.manualReviewCount') }}</span>
        <strong>{{ impact?.reviewRequiredCount ?? 0 }}</strong>
      </article>
    </div>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t('rental.configuration.backEdit') }}
      </el-button>
      <el-button type="primary" :loading="confirming" @click="emit('confirm')">
        {{ confirmText || t('rental.configuration.confirmSave') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import type { RentalChannelProductRuleImpactVO } from '@/api/rental/configuration'
import { useI18n } from '@/hooks/web/useI18n'

withDefaults(
  defineProps<{
    modelValue: boolean
    impact?: RentalChannelProductRuleImpactVO
    confirming?: boolean
    confirmText?: string
  }>(),
  {
    confirming: false,
    confirmText: ''
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const { t } = useI18n()
</script>

<style scoped>
.impact-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.impact-grid article {
  padding: 14px 10px;
  text-align: center;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.impact-grid span {
  display: block;
  min-height: 34px;
  font-size: 12px;
  line-height: 17px;
  color: var(--el-text-color-secondary);
}

.impact-grid strong {
  display: block;
  margin-top: 7px;
  font-size: 22px;
  color: var(--el-text-color-primary);
}

@media (width <= 720px) {
  .impact-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
