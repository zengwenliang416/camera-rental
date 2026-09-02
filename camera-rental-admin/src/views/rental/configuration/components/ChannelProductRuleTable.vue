<template>
  <section class="rule-panel">
    <div class="panel-heading">
      <div>
        <h2>{{ t('rental.configuration.rulesTitle') }}</h2>
        <p>{{ t('rental.configuration.rulesHint') }}</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="emit('create')">
        <Icon icon="ep:plus" class="mr-5px" />
        {{ t('rental.configuration.newRule') }}
      </el-button>
    </div>

    <el-alert
      class="mb-16px"
      type="info"
      :closable="false"
      show-icon
      :title="t('rental.configuration.noFallbackNotice')"
    />

    <div class="desktop-rules">
      <el-table
        v-loading="loading"
        :data="rules"
        :row-key="ruleRowKey"
        :expand-row-keys="expandedRuleIds"
        @expand-change="handleExpand"
      >
        <el-table-column type="expand" width="44">
          <template #default="{ row }">
            <div v-if="row.mappingMode === 'MULTI'" class="expanded-skus">
              <header>
                <div>
                  <strong>{{ t('rental.configuration.syncedSku') }}</strong>
                  <span>{{ t('rental.configuration.syncedSkuHint') }}</span>
                </div>
              </header>
              <ChannelSkuMappingTable
                :model-value="skuDetails[String(row.id)] ?? []"
                :models="models"
                :loading="skuLoadingIds.includes(row.id)"
              />
            </div>
            <el-empty
              v-else
              :image-size="54"
              :description="t('rental.configuration.noSkuExpansion')"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.configuration.shopProduct')" min-width="210">
          <template #default="{ row }">
            <strong class="rule-title">
              {{ row.productTitleSnapshot || t('rental.configuration.untitledProduct') }}
            </strong>
            <small>{{ shopLabel(row.shopId) }}</small>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.configuration.identifiers')" min-width="220">
          <template #default="{ row }">
            <ChannelIdentifierSummary
              scope="product"
              :xgj-product-id="row.xgjProductId"
              :xianyu-item-id="row.xianyuItemId"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.configuration.handlingPolicy')" width="155">
          <template #default="{ row }">
            <el-tag
              :type="row.handlingPolicy === 'CREATE_RENTAL' ? 'success' : 'info'"
              effect="plain"
            >
              {{
                row.handlingPolicy === 'CREATE_RENTAL'
                  ? t('rental.configuration.handlingPolicyCreateRental')
                  : t('rental.configuration.handlingPolicyConfigSkipped')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.configuration.modelMode')" width="125">
          <template #default="{ row }">
            <span v-if="row.handlingPolicy === 'CONFIG_SKIPPED'">—</span>
            <el-tag v-else :type="row.mappingMode === 'MULTI' ? 'primary' : 'info'" effect="plain">
              {{
                row.mappingMode === 'MULTI'
                  ? t('rental.configuration.bySku')
                  : t('rental.configuration.singleModel')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.configuration.mappingResult')" min-width="170">
          <template #default="{ row }">
            <span v-if="row.handlingPolicy === 'CONFIG_SKIPPED'" class="muted">
              {{ t('rental.configuration.noParsing') }}
            </span>
            <span v-else-if="row.mappingMode === 'SINGLE'">
              {{ modelLabel(row.singleDeviceModelId) }}
            </span>
            <div v-else>
              <strong>{{ skuSummary(row) }}</strong>
              <small>{{ t('rental.configuration.expandForExactStatus') }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('rental.configuration.status')" width="105">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain">
              {{
                row.enabled ? t('rental.configuration.enabled') : t('rental.configuration.disabled')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="canUpdate" :label="t('table.action')" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="emit('edit', row)">
              {{
                row.mappingMode === 'MULTI'
                  ? t('rental.configuration.editMapping')
                  : t('action.edit')
              }}
            </el-button>
            <el-button
              link
              :type="row.enabled ? 'warning' : 'success'"
              @click="emit('change-status', row)"
            >
              {{
                row.enabled ? t('rental.configuration.disable') : t('rental.configuration.enable')
              }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="rule-empty">
            <Icon icon="ep:connection" />
            <strong>{{ t('rental.configuration.emptyTitle') }}</strong>
            <span>{{ t('rental.configuration.emptyCopy') }}</span>
          </div>
        </template>
      </el-table>
    </div>

    <div v-loading="loading" class="mobile-rules">
      <article v-for="rule in rules" :key="rule.id" class="mobile-rule-card">
        <header>
          <div>
            <strong>{{
              rule.productTitleSnapshot || t('rental.configuration.untitledProduct')
            }}</strong>
            <small>{{ shopLabel(rule.shopId) }}</small>
          </div>
          <el-tag :type="rule.enabled ? 'success' : 'info'" effect="plain">
            {{
              rule.enabled ? t('rental.configuration.enabled') : t('rental.configuration.disabled')
            }}
          </el-tag>
        </header>
        <ChannelIdentifierSummary
          scope="product"
          :xgj-product-id="rule.xgjProductId"
          :xianyu-item-id="rule.xianyuItemId"
        />
        <dl>
          <div>
            <dt>{{ t('rental.configuration.handlingPolicy') }}</dt>
            <dd>{{ rule.handlingPolicy }}</dd>
          </div>
          <div>
            <dt>{{ t('rental.configuration.modelMode') }}</dt>
            <dd>
              {{
                rule.handlingPolicy === 'CONFIG_SKIPPED'
                  ? '—'
                  : rule.mappingMode === 'MULTI'
                    ? t('rental.configuration.bySku')
                    : t('rental.configuration.singleModel')
              }}
            </dd>
          </div>
          <div class="mobile-rule-card__mapping">
            <dt>{{ t('rental.configuration.mappingResult') }}</dt>
            <dd>
              <span v-if="rule.handlingPolicy === 'CONFIG_SKIPPED'">
                {{ t('rental.configuration.noParsing') }}
              </span>
              <span v-else-if="rule.mappingMode === 'SINGLE'">
                {{ modelLabel(rule.singleDeviceModelId) }}
              </span>
              <span v-else>{{ skuSummary(rule) }}</span>
            </dd>
          </div>
        </dl>
        <div v-if="rule.mappingMode === 'MULTI'" class="mobile-rule-card__skus">
          <el-button link type="primary" @click="toggleMobileExpand(rule)">
            {{
              isExpanded(rule)
                ? t('rental.configuration.collapseSkuDetails')
                : t('rental.configuration.expandForExactStatus')
            }}
          </el-button>
          <ChannelSkuMappingTable
            v-if="isExpanded(rule)"
            :model-value="skuDetails[String(rule.id)] ?? []"
            :models="models"
            :loading="skuLoadingIds.includes(rule.id)"
          />
        </div>
        <div v-if="canUpdate" class="mobile-rule-card__actions">
          <el-button @click="emit('edit', rule)">
            {{
              rule.mappingMode === 'MULTI'
                ? t('rental.configuration.editMapping')
                : t('action.edit')
            }}
          </el-button>
          <el-button
            :type="rule.enabled ? 'warning' : 'success'"
            plain
            @click="emit('change-status', rule)"
          >
            {{
              rule.enabled ? t('rental.configuration.disable') : t('rental.configuration.enable')
            }}
          </el-button>
        </div>
      </article>
      <el-empty
        v-if="!loading && rules.length === 0"
        :description="t('rental.configuration.emptyCopy')"
      />
    </div>
  </section>
</template>

<script lang="ts" setup>
import type {
  RentalConfigurationShopVO,
  RentalChannelProductRuleVO,
  RentalChannelProductSkuVO
} from '@/api/rental/configuration'
import type { RentalDeviceModelVO } from '@/api/rental/catalog'
import { useI18n } from '@/hooks/web/useI18n'
import { formatDeviceModelLabel } from '../../device/deviceCatalogModel'
import { countConfiguredSkus, hasMissingSkuMappings } from '../configurationModel'
import ChannelIdentifierSummary from './ChannelIdentifierSummary.vue'
import ChannelSkuMappingTable from './ChannelSkuMappingTable.vue'

const props = withDefaults(
  defineProps<{
    rules: RentalChannelProductRuleVO[]
    shops: RentalConfigurationShopVO[]
    models: RentalDeviceModelVO[]
    skuDetails: Record<string, RentalChannelProductSkuVO[] | undefined>
    skuLoadingIds: number[]
    expandedRuleIds: string[]
    loading?: boolean
    canUpdate?: boolean
    canCreate?: boolean
  }>(),
  {
    loading: false,
    canUpdate: false,
    canCreate: false
  }
)

const emit = defineEmits<{
  create: []
  edit: [rule: RentalChannelProductRuleVO]
  'change-status': [rule: RentalChannelProductRuleVO]
  expand: [rule: RentalChannelProductRuleVO, expanded: boolean]
}>()

const { t } = useI18n()

const shopLabel = (shopId: number) => {
  const shop = props.shops.find((item) => item.id === shopId)
  return shop ? `${shop.shopName} (#${shop.id})` : `#${shopId}`
}

const modelLabel = (modelId?: number) => {
  const model = props.models.find((item) => item.id === modelId)
  return model ? formatDeviceModelLabel(model) : t('rental.configuration.waitingMapping')
}

const skuSummary = (rule: RentalChannelProductRuleVO) => {
  const skus = props.skuDetails[String(rule.id)]
  if (!skus) {
    return t('rental.configuration.configuredSkuCount', { count: rule.skuMappings.length })
  }
  const count = countConfiguredSkus(skus)
  return hasMissingSkuMappings(skus)
    ? t('rental.configuration.skuProgress', { count, total: skus.length })
    : t('rental.configuration.allSkuConfigured', { count })
}

const ruleRowKey = (rule: RentalChannelProductRuleVO) => String(rule.id)
const isExpanded = (rule: RentalChannelProductRuleVO) =>
  props.expandedRuleIds.includes(String(rule.id))

const toggleMobileExpand = (rule: RentalChannelProductRuleVO) => {
  emit('expand', rule, !isExpanded(rule))
}

const handleExpand = (
  rule: RentalChannelProductRuleVO,
  expandedRows: RentalChannelProductRuleVO[]
) => {
  emit(
    'expand',
    rule,
    expandedRows.some((expanded) => expanded.id === rule.id)
  )
}
</script>

<style scoped>
.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-heading h2 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.panel-heading p {
  margin: 5px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.rule-title,
.rule-title + small,
.expanded-skus header strong,
.expanded-skus header span {
  display: block;
}

.rule-title + small,
.expanded-skus header span,
.muted {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.expanded-skus {
  padding: 8px 18px 18px;
}

.expanded-skus > header {
  margin-bottom: 12px;
}

.rule-empty {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 6px;
  padding: 24px;
  color: var(--el-text-color-secondary);
}

.rule-empty .iconify {
  font-size: 28px;
  color: var(--el-color-primary);
}

.mobile-rules {
  display: none;
}

@media (width <= 720px) {
  .panel-heading {
    flex-direction: column;
  }

  .panel-heading .el-button {
    width: 100%;
  }

  .desktop-rules {
    display: none;
  }

  .mobile-rules {
    display: grid;
    gap: 12px;
    min-height: 120px;
  }

  .mobile-rule-card {
    padding: 15px;
    background: var(--el-fill-color-blank);
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 10px;
  }

  .mobile-rule-card > header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 12px;
  }

  .mobile-rule-card header strong,
  .mobile-rule-card header small {
    display: block;
  }

  .mobile-rule-card header small {
    margin-top: 4px;
    color: var(--el-text-color-secondary);
  }

  .mobile-rule-card dl {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
    margin: 14px 0 0;
  }

  .mobile-rule-card__mapping {
    grid-column: 1 / -1;
  }

  .mobile-rule-card__skus {
    margin-top: 12px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .mobile-rule-card__skus > .el-button {
    padding: 12px 0 8px;
  }

  .mobile-rule-card dt {
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }

  .mobile-rule-card dd {
    margin: 3px 0 0;
    overflow-wrap: anywhere;
  }

  .mobile-rule-card__actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin-top: 14px;
  }

  .mobile-rule-card__actions .el-button {
    width: 100%;
    margin: 0;
  }
}
</style>
