<template>
  <el-drawer
    :model-value="modelValue"
    :title="rule ? t('rental.configuration.editRule') : t('rental.configuration.newRule')"
    size="min(720px, 92vw)"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      ref="formRef"
      :model="draft"
      :rules="rules"
      label-position="top"
      :disabled="saving || skuLoading"
    >
      <section class="drawer-section">
        <header>
          <span>{{ t('rental.configuration.exactMapping') }}</span>
          <h3>{{ t('rental.configuration.scopeTitle') }}</h3>
        </header>
        <div class="form-grid">
          <el-form-item :label="t('rental.configuration.shop')" prop="shopId">
            <el-select
              v-model="draft.shopId"
              class="!w-100%"
              filterable
              :disabled="Boolean(rule)"
              @change="scopeChanged"
            >
              <el-option
                v-for="shop in shops"
                :key="shop.id"
                :label="`${shop.shopName} (#${shop.id})`"
                :value="shop.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('rental.configuration.xianyuItemId')" prop="xianyuItemId">
            <el-input
              v-model="draft.xianyuItemId"
              class="identifier-input"
              :disabled="Boolean(rule)"
              @input="scopeChanged"
            />
          </el-form-item>
        </div>
      </section>

      <section class="drawer-section">
        <header>
          <h3>{{ t('rental.configuration.policyAndMode') }}</h3>
        </header>
        <el-form-item :label="t('rental.configuration.handlingPolicy')" prop="handlingPolicy">
          <el-radio-group v-model="draft.handlingPolicy">
            <el-radio-button value="CREATE_RENTAL">
              {{ t('rental.configuration.handlingPolicyCreateRental') }}
            </el-radio-button>
            <el-radio-button value="CONFIG_SKIPPED">
              {{ t('rental.configuration.handlingPolicyConfigSkipped') }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-alert
          v-if="draft.handlingPolicy === 'CONFIG_SKIPPED'"
          type="warning"
          :closable="false"
          show-icon
          :title="t('rental.configuration.skipPolicyHint')"
        />
        <template v-else>
          <el-form-item :label="t('rental.configuration.modelMode')" prop="mappingMode">
            <el-radio-group v-model="draft.mappingMode" @change="mappingModeChanged">
              <el-radio value="SINGLE">
                {{ t('rental.configuration.singleModel') }}
              </el-radio>
              <el-radio value="MULTI">
                {{ t('rental.configuration.bySku') }}
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item
            v-if="draft.mappingMode === 'SINGLE'"
            :label="t('rental.configuration.singleMapping')"
            prop="singleDeviceModelId"
          >
            <el-select
              v-model="draft.singleDeviceModelId"
              class="!w-100%"
              filterable
              :placeholder="t('rental.configuration.selectModel')"
            >
              <el-option
                v-for="model in enabledModels"
                :key="model.id"
                :label="formatDeviceModelLabel(model)"
                :value="model.id"
              />
            </el-select>
          </el-form-item>

          <div v-else>
            <div class="sku-toolbar">
              <div>
                <strong>{{ t('rental.configuration.skuMapping') }}</strong>
                <span>{{ t('rental.configuration.skuSelectionRule') }}</span>
              </div>
              <el-button :loading="skuLoading" :disabled="!canLoadSkus" @click="emitLoadSkus">
                <Icon icon="ep:refresh" class="mr-5px" />
                {{ t('rental.configuration.loadSyncedSkus') }}
              </el-button>
            </div>
            <ChannelSkuMappingTable
              v-model="draft.skuMappings"
              :models="models"
              :loading="skuLoading"
              editable
            />
            <el-alert
              v-if="hasMissingSkuMappings(draft.skuMappings)"
              class="mt-12px"
              type="warning"
              :closable="false"
              :title="t('rental.configuration.noFallbackValidation')"
            />
          </div>
        </template>
      </section>

      <section class="drawer-section">
        <header>
          <h3>{{ t('rental.configuration.ruleSettings') }}</h3>
        </header>
        <div class="form-grid">
          <el-form-item :label="t('rental.configuration.ruleEnabled')">
            <el-switch v-model="draft.enabled" />
          </el-form-item>
          <el-form-item :label="t('rental.configuration.ruleNote')">
            <el-input
              v-model="draft.ruleNote"
              type="textarea"
              :rows="3"
              maxlength="255"
              show-word-limit
            />
          </el-form-item>
        </div>
      </section>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="emit('update:modelValue', false)">
        {{ t('common.cancel') }}
      </el-button>
      <el-button type="primary" :loading="saving" @click="submit">
        {{ t('rental.configuration.previewSave') }}
      </el-button>
    </template>
  </el-drawer>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type {
  RentalChannelProductRuleVO,
  RentalChannelProductSkuVO,
  RentalConfigurationShopVO
} from '@/api/rental/configuration'
import type { RentalDeviceModelVO } from '@/api/rental/catalog'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import { formatDeviceModelLabel } from '../../device/deviceCatalogModel'
import {
  hasMissingSkuMappings,
  normalizeExternalIdentifier,
  type RentalChannelProductRuleDraft
} from '../configurationModel'
import ChannelSkuMappingTable from './ChannelSkuMappingTable.vue'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    rule?: RentalChannelProductRuleVO
    shops: RentalConfigurationShopVO[]
    models: RentalDeviceModelVO[]
    syncedSkus: RentalChannelProductSkuVO[]
    skuLoading?: boolean
    saving?: boolean
  }>(),
  {
    skuLoading: false,
    saving: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'load-skus': [shopId: number, xianyuItemId: string]
  'scope-change': [shopId: number | undefined, xianyuItemId: string]
  'draft-change': [draft: RentalChannelProductRuleDraft]
  preview: [draft: RentalChannelProductRuleDraft]
}>()

const { t } = useI18n()
const message = useMessage()
const formRef = ref<FormInstance>()

const draft = reactive<RentalChannelProductRuleDraft>({
  xianyuItemId: '',
  handlingPolicy: 'CREATE_RENTAL',
  mappingMode: 'SINGLE',
  enabled: true,
  ruleNote: '',
  skuMappings: [],
  synchronizedProductSkuIds: []
})

const enabledModels = computed(() => props.models.filter((model) => model.enabled !== false))
const canLoadSkus = computed(
  () => draft.shopId !== undefined && normalizeExternalIdentifier(draft.xianyuItemId).length > 0
)

const rules = computed<FormRules>(() => ({
  shopId: [{ required: true, message: t('rental.configuration.shopRequired'), trigger: 'change' }],
  xianyuItemId: [
    { required: true, message: t('rental.configuration.itemIdRequired'), trigger: 'blur' }
  ],
  singleDeviceModelId: [
    {
      validator: (_rule, value, callback) => {
        if (
          draft.handlingPolicy === 'CREATE_RENTAL' &&
          draft.mappingMode === 'SINGLE' &&
          value === undefined
        ) {
          callback(new Error(t('rental.configuration.modelRequired')))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}))

const resetDraft = () => {
  const rule = props.rule
  draft.id = rule?.id
  draft.shopId = rule?.shopId
  draft.xianyuItemId = rule?.xianyuItemId ?? ''
  draft.handlingPolicy = rule?.handlingPolicy ?? 'CREATE_RENTAL'
  draft.mappingMode = rule?.mappingMode === 'MULTI' ? 'MULTI' : 'SINGLE'
  draft.singleDeviceModelId = rule?.singleDeviceModelId
  draft.enabled = rule?.enabled ?? true
  draft.ruleNote = rule?.ruleNote ?? ''
  draft.lockVersion = rule?.lockVersion
  draft.skuMappings = props.syncedSkus.map((sku) => ({ ...sku }))
  draft.synchronizedProductSkuIds = props.syncedSkus.map((sku) => sku.productSkuId)
}

watch(
  () => [props.modelValue, props.rule] as const,
  ([visible]) => {
    if (visible) resetDraft()
  },
  { immediate: true }
)

watch(
  () => props.syncedSkus,
  (skus) => {
    if (props.modelValue) {
      draft.skuMappings = skus.map((sku) => ({ ...sku }))
      draft.synchronizedProductSkuIds = skus.map((sku) => sku.productSkuId)
    }
  }
)

watch(
  draft,
  (value) => {
    if (!props.modelValue) return
    emit('draft-change', {
      ...value,
      skuMappings: value.skuMappings.map((sku) => ({ ...sku })),
      synchronizedProductSkuIds: [...value.synchronizedProductSkuIds]
    })
  },
  { deep: true }
)

const emitLoadSkus = () => {
  if (!canLoadSkus.value || draft.shopId === undefined) return
  emit('load-skus', draft.shopId, normalizeExternalIdentifier(draft.xianyuItemId))
}

const scopeChanged = () => {
  draft.skuMappings = []
  draft.synchronizedProductSkuIds = []
  emit('scope-change', draft.shopId, normalizeExternalIdentifier(draft.xianyuItemId))
}

const mappingModeChanged = () => {
  if (draft.mappingMode === 'MULTI' && draft.skuMappings.length === 0) {
    emitLoadSkus()
  }
}

const submit = async () => {
  if (!(await formRef.value?.validate())) return
  if (draft.handlingPolicy === 'CREATE_RENTAL' && draft.mappingMode === 'MULTI') {
    const selectedCount = draft.skuMappings.filter((sku) => sku.deviceModelId !== undefined).length
    if (selectedCount === 0) {
      message.warning(t('rental.configuration.atLeastOneSkuMapping'))
      return
    }
  }
  emit('preview', {
    ...draft,
    xianyuItemId: normalizeExternalIdentifier(draft.xianyuItemId),
    skuMappings: draft.skuMappings.map((sku) => ({ ...sku })),
    synchronizedProductSkuIds: [...draft.synchronizedProductSkuIds]
  })
}
</script>

<style scoped>
.drawer-section {
  padding: 2px 0 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.drawer-section:last-child {
  border-bottom: 0;
}

.drawer-section > header {
  margin-bottom: 14px;
}

.drawer-section header span,
.drawer-section header h3 {
  display: block;
  margin: 0;
}

.drawer-section header span {
  margin-bottom: 4px;
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--el-color-primary);
}

.drawer-section header h3 {
  font-size: 16px;
  color: var(--el-text-color-primary);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.identifier-input :deep(.el-input__inner) {
  font-family: SFMono-Regular, Consolas, monospace;
}

.sku-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.sku-toolbar strong,
.sku-toolbar span {
  display: block;
}

.sku-toolbar span {
  margin-top: 3px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (width <= 620px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .sku-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .sku-toolbar .el-button {
    width: 100%;
  }
}
</style>
