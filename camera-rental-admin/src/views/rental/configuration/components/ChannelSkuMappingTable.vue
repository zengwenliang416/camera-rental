<template>
  <div class="sku-mapping">
    <el-alert
      class="mb-12px"
      type="info"
      :closable="false"
      :title="t('rental.configuration.skuSelectionRule')"
    />
    <el-table
      v-loading="loading"
      class="desktop-sku-table"
      :data="modelValue"
      row-key="productSkuId"
    >
      <el-table-column :label="t('rental.configuration.identifiers')" min-width="230">
        <template #default="{ row }">
          <ChannelIdentifierSummary
            scope="sku"
            :xgj-sku-id="row.xgjSkuId"
            :xianyu-sku-id="row.xianyuSkuId"
          />
        </template>
      </el-table-column>
      <el-table-column prop="skuName" :label="t('rental.configuration.skuDisplay')" min-width="170">
        <template #default="{ row }">
          {{ row.skuName || t('rental.configuration.unavailable') }}
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.configuration.equipmentModel')" min-width="220">
        <template #default="{ row }">
          <el-select
            v-if="editable"
            :model-value="row.deviceModelId"
            class="!w-100%"
            clearable
            filterable
            :placeholder="t('rental.configuration.selectModel')"
            @update:model-value="updateModel(row.productSkuId, $event)"
          >
            <el-option
              v-for="model in enabledModels"
              :key="model.id"
              :label="formatDeviceModelLabel(model)"
              :value="model.id"
            />
          </el-select>
          <span v-else>
            {{ modelLabel(row.deviceModelId) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="t('rental.configuration.mappingStatus')" width="150">
        <template #default="{ row }">
          <el-tag
            :type="row.deviceModelId && row.mappingEnabled !== false ? 'success' : 'warning'"
            effect="plain"
          >
            {{
              row.deviceModelId && row.mappingEnabled !== false
                ? t('rental.configuration.configured')
                : t('rental.configuration.waitingMapping')
            }}
          </el-tag>
        </template>
      </el-table-column>
      <template #empty>
        <div class="py-20px text-[var(--el-text-color-secondary)]">
          {{ t('rental.configuration.noSyncedSkus') }}
        </div>
      </template>
    </el-table>
    <div v-loading="loading" class="mobile-sku-list">
      <article v-for="row in modelValue" :key="row.productSkuId">
        <ChannelIdentifierSummary
          scope="sku"
          :xgj-sku-id="row.xgjSkuId"
          :xianyu-sku-id="row.xianyuSkuId"
        />
        <div class="mobile-sku-list__name">
          <span>{{ t('rental.configuration.skuDisplay') }}</span>
          <strong>{{ row.skuName || t('rental.configuration.unavailable') }}</strong>
        </div>
        <el-select
          v-if="editable"
          :model-value="row.deviceModelId"
          class="!w-100%"
          clearable
          filterable
          :placeholder="t('rental.configuration.selectModel')"
          @update:model-value="updateModel(row.productSkuId, $event)"
        >
          <el-option
            v-for="model in enabledModels"
            :key="model.id"
            :label="formatDeviceModelLabel(model)"
            :value="model.id"
          />
        </el-select>
        <div v-else class="mobile-sku-list__model">
          <span>{{ t('rental.configuration.equipmentModel') }}</span>
          <strong>{{ modelLabel(row.deviceModelId) }}</strong>
        </div>
        <el-tag
          :type="row.deviceModelId && row.mappingEnabled !== false ? 'success' : 'warning'"
          effect="plain"
        >
          {{
            row.deviceModelId && row.mappingEnabled !== false
              ? t('rental.configuration.configured')
              : t('rental.configuration.waitingMapping')
          }}
        </el-tag>
      </article>
      <el-empty
        v-if="!loading && modelValue.length === 0"
        :description="t('rental.configuration.noSyncedSkus')"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import type { RentalChannelProductSkuVO } from '@/api/rental/configuration'
import type { RentalDeviceModelVO } from '@/api/rental/catalog'
import { useI18n } from '@/hooks/web/useI18n'
import { formatDeviceModelLabel } from '../../device/deviceCatalogModel'
import ChannelIdentifierSummary from './ChannelIdentifierSummary.vue'

const props = withDefaults(
  defineProps<{
    modelValue: RentalChannelProductSkuVO[]
    models: RentalDeviceModelVO[]
    editable?: boolean
    loading?: boolean
  }>(),
  {
    editable: false,
    loading: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: RentalChannelProductSkuVO[]]
}>()

const { t } = useI18n()
const enabledModels = computed(() => props.models.filter((model) => model.enabled !== false))

const updateModel = (productSkuId: number, deviceModelId?: number) => {
  emit(
    'update:modelValue',
    props.modelValue.map((sku) =>
      sku.productSkuId === productSkuId
        ? {
            ...sku,
            deviceModelId,
            mappingEnabled: deviceModelId === undefined ? undefined : true
          }
        : sku
    )
  )
}

const modelLabel = (modelId?: number) => {
  const model = props.models.find((item) => item.id === modelId)
  return model ? formatDeviceModelLabel(model) : t('rental.configuration.notSelected')
}
</script>

<style scoped>
.sku-mapping {
  min-width: 0;
}

.mobile-sku-list {
  display: none;
}

@media (width <= 720px) {
  .desktop-sku-table {
    display: none;
  }

  .mobile-sku-list {
    display: grid;
    gap: 10px;
  }

  .mobile-sku-list article {
    display: grid;
    gap: 10px;
    padding: 12px;
    background: var(--el-fill-color-light);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
  }

  .mobile-sku-list__name span,
  .mobile-sku-list__model span {
    display: block;
    margin-bottom: 3px;
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }
}
</style>
