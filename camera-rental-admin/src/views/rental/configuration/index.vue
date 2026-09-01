<template>
  <Error v-if="!canQuery" type="403" />
  <ContentWrap v-else class="rental-configuration-page">
    <div class="page-heading">
      <div>
        <span>RENTAL CONFIGURATION</span>
        <h1>{{ t('rental.configuration.pageTitle') }}</h1>
        <p>{{ t('rental.configuration.pageDescription') }}</p>
      </div>
      <el-button :loading="pageLoading" @click="loadPage">
        <Icon icon="ep:refresh" class="mr-5px" />
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-alert
      v-if="pageError"
      class="mb-16px"
      type="error"
      :closable="false"
      show-icon
      :title="t('rental.configuration.errorTitle')"
    >
      <el-button link type="primary" @click="loadPage">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>

    <el-skeleton v-if="pageLoading" :rows="9" animated />

    <el-tabs v-else-if="!pageError" v-model="activeTab" class="configuration-tabs">
      <el-tab-pane name="catalog" :label="t('rental.configuration.tabCatalog')">
        <DeviceCatalogPanel
          :catalog="catalog"
          :loading="pageLoading"
          :saving="catalogSaving"
          :can-update="canUpdate"
          @save-category="saveCategory"
          @save-model="saveModel"
          @change-category-status="changeCategoryStatus"
          @change-model-status="changeModelStatus"
        />
      </el-tab-pane>

      <el-tab-pane name="rules" :label="t('rental.configuration.tabRules')">
        <el-alert
          v-if="shopsError"
          class="mb-16px"
          type="warning"
          :closable="false"
          show-icon
          :title="t('rental.configuration.shopLoadError')"
        >
          <el-button link type="primary" @click="loadShops">
            {{ t('rental.common.retry') }}
          </el-button>
        </el-alert>
        <el-alert
          v-if="rulesError"
          class="mb-16px"
          type="error"
          :closable="false"
          show-icon
          :title="t('rental.configuration.rulesLoadError')"
        >
          <el-button link type="primary" @click="loadRules">
            {{ t('rental.common.retry') }}
          </el-button>
        </el-alert>
        <el-alert
          v-if="reconciliationActive"
          class="mb-16px"
          type="info"
          :closable="false"
          show-icon
          :title="t('rental.configuration.reconciliationMutationBlocked')"
        >
          <el-button link type="primary" @click="reconciliationVisible = true">
            {{ t('rental.configuration.viewReconciliation') }}
          </el-button>
        </el-alert>
        <el-form class="rule-filters" :model="ruleQuery" inline>
          <el-form-item :label="t('rental.configuration.shop')">
            <el-select
              v-model="ruleQuery.shopId"
              class="!w-220px"
              clearable
              filterable
              :placeholder="t('rental.configuration.allShops')"
            >
              <el-option
                v-for="shop in shops"
                :key="shop.id"
                :label="`${shop.shopName} (#${shop.id})`"
                :value="shop.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('rental.configuration.keyword')">
            <el-input
              v-model="ruleQuery.keyword"
              class="!w-220px"
              clearable
              :placeholder="t('rental.configuration.keywordPlaceholder')"
              @keyup.enter="queryRules"
            />
          </el-form-item>
          <el-form-item :label="t('rental.configuration.handlingPolicy')">
            <el-select v-model="ruleQuery.handlingPolicy" class="!w-170px" clearable>
              <el-option label="CREATE_RENTAL" value="CREATE_RENTAL" />
              <el-option label="CONFIG_SKIPPED" value="CONFIG_SKIPPED" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('rental.configuration.status')">
            <el-select v-model="ruleQuery.enabled" class="!w-130px" clearable>
              <el-option :label="t('rental.configuration.enabled')" :value="true" />
              <el-option :label="t('rental.configuration.disabled')" :value="false" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="queryRules">{{ t('common.query') }}</el-button>
            <el-button @click="resetRuleQuery">{{ t('common.reset') }}</el-button>
          </el-form-item>
        </el-form>

        <ChannelProductRuleTable
          v-if="!rulesError"
          :rules="rules"
          :shops="shops"
          :models="models"
          :sku-details="skuDetails"
          :sku-loading-ids="skuLoadingIds"
          :expanded-rule-ids="expandedRuleIds"
          :loading="rulesLoading"
          :can-update="canUpdateRules"
          :can-create="canUpdateRules && shops.length > 0"
          @create="openCreateRule"
          @edit="openEditRule"
          @change-status="previewRuleStatusChange"
          @expand="handleRuleExpand"
        />
        <Pagination
          v-if="!rulesError && ruleTotal > 0"
          :total="ruleTotal"
          v-model:page="ruleQuery.pageNo"
          v-model:limit="ruleQuery.pageSize"
          @pagination="loadRules"
        />
      </el-tab-pane>

      <el-tab-pane name="remarks" :label="t('rental.configuration.tabRemarks')">
        <RemarkConventionPanel />
      </el-tab-pane>
    </el-tabs>

    <ChannelProductRuleDrawer
      v-model="drawerVisible"
      :rule="editingRule"
      :shops="drawerShops"
      :models="models"
      :synced-skus="drawerSkus"
      :sku-loading="drawerSkuLoading"
      :saving="ruleSaving"
      @load-skus="loadDrawerSkus"
      @scope-change="handleDrawerScopeChange"
      @draft-change="handleDrawerDraftChange"
      @preview="previewRuleSave"
    />

    <RuleImpactPreviewDialog
      v-model="impactVisible"
      :impact="impact"
      :confirming="ruleSaving"
      :confirm-text="impactConfirmText"
      @confirm="confirmPendingMutation"
    />

    <RuleReconciliationResultDialog
      v-model="reconciliationVisible"
      :run="reconciliationRun"
      :refreshing="reconciliationRefreshing"
      :load-error="reconciliationLoadError"
      @refresh="refreshReconciliation"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { hasPermission } from '@/directives/permission/hasPermi'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'
import {
  createRentalChannelProductRule,
  createRentalConfigurationCategory,
  createRentalConfigurationModel,
  getRentalChannelProductRule,
  getRentalChannelProductRulePage,
  getRentalChannelProductRuleReconciliation,
  getRentalChannelProductSkus,
  getRentalConfigurationCatalog,
  getRentalConfigurationShops,
  previewRentalChannelProductRuleImpact,
  updateRentalChannelProductRule,
  updateRentalChannelProductRuleStatus,
  updateRentalConfigurationCategory,
  updateRentalConfigurationCategoryStatus,
  updateRentalConfigurationModel,
  updateRentalConfigurationModelStatus,
  type RentalChannelProductRuleImpactVO,
  type RentalChannelProductRulePageReqVO,
  type RentalChannelProductRuleSaveReqVO,
  type RentalChannelProductRuleVO,
  type RentalChannelReconciliationRunVO,
  type RentalChannelProductSkuVO,
  type RentalConfigurationShopVO,
  type RentalDeviceCategoryCreateReqVO,
  type RentalDeviceCategoryUpdateReqVO,
  type RentalDeviceModelCreateReqVO,
  type RentalDeviceModelUpdateReqVO
} from '@/api/rental/configuration'
import type { RentalDeviceCategoryVO, RentalDeviceModelVO } from '@/api/rental/catalog'
import {
  buildImpactPreviewKey,
  buildRuleScopeKey,
  buildProductRuleSaveRequest,
  isConfigurationVersionConflict,
  isTerminalReconciliationStatus,
  recoverConfigurationVersionConflict,
  type RentalChannelProductRuleDraft
} from './configurationModel'
import ChannelProductRuleDrawer from './components/ChannelProductRuleDrawer.vue'
import ChannelProductRuleTable from './components/ChannelProductRuleTable.vue'
import DeviceCatalogPanel from './components/DeviceCatalogPanel.vue'
import RemarkConventionPanel from './components/RemarkConventionPanel.vue'
import RuleImpactPreviewDialog from './components/RuleImpactPreviewDialog.vue'
import RuleReconciliationResultDialog from './components/RuleReconciliationResultDialog.vue'

defineOptions({ name: 'RentalConfiguration' })

type PendingMutation =
  | {
      kind: 'save'
      draft: RentalChannelProductRuleDraft
      request: RentalChannelProductRuleSaveReqVO
      previewKey: string
    }
  | {
      kind: 'status'
      rule: RentalChannelProductRuleVO
      enabled: boolean
    }

const { t } = useI18n()
const message = useMessage()
const canQuery = computed(() => hasPermission(['rental:configuration:query']))
const canUpdate = computed(() => hasPermission(['rental:configuration:update']))
const activeTab = ref('rules')
const pageLoading = ref(false)
const pageError = ref(false)
const shopsError = ref(false)
const rulesError = ref(false)
const catalogSaving = ref(false)
const ruleSaving = ref(false)
const catalog = ref<RentalDeviceCategoryVO[]>([])
const shops = ref<RentalConfigurationShopVO[]>([])
const rules = ref<RentalChannelProductRuleVO[]>([])
const ruleTotal = ref(0)
const rulesLoading = ref(false)

const ruleQuery = reactive<RentalChannelProductRulePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  shopId: undefined,
  keyword: '',
  handlingPolicy: undefined,
  enabled: undefined
})

const models = computed(() => catalog.value.flatMap((category) => category.models))
const drawerVisible = ref(false)
const editingRule = ref<RentalChannelProductRuleVO>()
const drawerSkus = ref<RentalChannelProductSkuVO[]>([])
const drawerSkuLoading = ref(false)
const skuDetails = reactive<Record<string, RentalChannelProductSkuVO[] | undefined>>({})
const skuLoadingIds = ref<number[]>([])
const expandedRuleIds = ref<string[]>([])
const impactVisible = ref(false)
const impact = ref<RentalChannelProductRuleImpactVO>()
const pendingMutation = ref<PendingMutation>()
const drawerDraftKey = ref('')
const drawerScopeKey = ref('')
let drawerSkuRequestId = 0
const reconciliationVisible = ref(false)
const reconciliationRefreshing = ref(false)
const reconciliationLoadError = ref(false)
const reconciliationRun = ref<RentalChannelReconciliationRunVO>()
let reconciliationTimer: ReturnType<typeof setTimeout> | undefined

const reconciliationActive = computed(
  () =>
    reconciliationRun.value !== undefined &&
    !isTerminalReconciliationStatus(reconciliationRun.value.status)
)
const canUpdateRules = computed(() => canUpdate.value && !reconciliationActive.value)
const drawerShops = computed(() => {
  const shopId = editingRule.value?.shopId
  if (shopId === undefined || shops.value.some((shop) => shop.id === shopId)) return shops.value
  return [
    ...shops.value,
    {
      id: shopId,
      shopName: `#${shopId}`,
      authorizationStatus: 'UNKNOWN'
    }
  ]
})

const impactConfirmText = computed(() =>
  pendingMutation.value?.kind === 'status'
    ? t('rental.configuration.confirmStatusChange')
    : t('rental.configuration.confirmSave')
)

const loadCatalog = async () => {
  const response = await getRentalConfigurationCatalog()
  catalog.value = response.categories
}

const loadShops = async () => {
  shopsError.value = false
  try {
    shops.value = await getRentalConfigurationShops()
  } catch {
    shops.value = []
    shopsError.value = true
  }
}

const loadRules = async () => {
  rulesLoading.value = true
  rulesError.value = false
  try {
    const response = await getRentalChannelProductRulePage({
      ...ruleQuery,
      keyword: ruleQuery.keyword?.trim() || undefined
    })
    rules.value = response.list
    ruleTotal.value = response.total
  } catch {
    rules.value = []
    ruleTotal.value = 0
    rulesError.value = true
  } finally {
    rulesLoading.value = false
  }
}

const loadPage = async () => {
  pageLoading.value = true
  pageError.value = false
  try {
    await Promise.all([loadCatalog(), loadShops(), loadRules()])
  } catch {
    pageError.value = true
  } finally {
    pageLoading.value = false
  }
}

const queryRules = async () => {
  ruleQuery.pageNo = 1
  await loadRules()
}

const resetRuleQuery = async () => {
  ruleQuery.pageNo = 1
  ruleQuery.shopId = undefined
  ruleQuery.keyword = ''
  ruleQuery.handlingPolicy = undefined
  ruleQuery.enabled = undefined
  await loadRules()
}

const saveCategory = async (
  value: RentalDeviceCategoryCreateReqVO | RentalDeviceCategoryUpdateReqVO,
  done: () => void
) => {
  catalogSaving.value = true
  try {
    if ('id' in value) {
      await updateRentalConfigurationCategory(value)
    } else {
      await createRentalConfigurationCategory(value)
    }
    await loadCatalog()
    done()
    message.success(t('rental.configuration.catalogSaved'))
  } catch (error) {
    if (await recoverConfigurationVersionConflict(error, done, loadCatalog)) {
      message.warning(t('rental.configuration.versionConflictReloaded'))
    }
  } finally {
    catalogSaving.value = false
  }
}

const saveModel = async (
  value: RentalDeviceModelCreateReqVO | RentalDeviceModelUpdateReqVO,
  done: () => void
) => {
  catalogSaving.value = true
  try {
    if ('id' in value) {
      await updateRentalConfigurationModel(value)
    } else {
      await createRentalConfigurationModel(value)
    }
    await loadCatalog()
    done()
    message.success(t('rental.configuration.catalogSaved'))
  } catch (error) {
    if (await recoverConfigurationVersionConflict(error, done, loadCatalog)) {
      message.warning(t('rental.configuration.versionConflictReloaded'))
    }
  } finally {
    catalogSaving.value = false
  }
}

const changeCategoryStatus = async (category: RentalDeviceCategoryVO) => {
  try {
    await message.confirm(
      t('rental.configuration.catalogStatusConfirm', {
        name: category.categoryName,
        action:
          category.enabled === false
            ? t('rental.configuration.enable')
            : t('rental.configuration.disable')
      })
    )
  } catch {
    return
  }
  catalogSaving.value = true
  try {
    await updateRentalConfigurationCategoryStatus({
      id: category.id,
      enabled: category.enabled === false,
      lockVersion: category.lockVersion
    })
    await loadCatalog()
    message.success(t('rental.configuration.catalogStatusChanged'))
  } catch (error) {
    if (isConfigurationVersionConflict(error)) {
      await loadCatalog()
      message.warning(t('rental.configuration.versionConflictReloaded'))
    }
  } finally {
    catalogSaving.value = false
  }
}

const changeModelStatus = async (model: RentalDeviceModelVO) => {
  try {
    await message.confirm(
      t('rental.configuration.catalogStatusConfirm', {
        name: model.modelName,
        action:
          model.enabled === false
            ? t('rental.configuration.enable')
            : t('rental.configuration.disable')
      })
    )
  } catch {
    return
  }
  catalogSaving.value = true
  try {
    await updateRentalConfigurationModelStatus({
      id: model.id,
      enabled: model.enabled === false,
      lockVersion: model.lockVersion
    })
    await loadCatalog()
    message.success(t('rental.configuration.catalogStatusChanged'))
  } catch (error) {
    if (isConfigurationVersionConflict(error)) {
      await loadCatalog()
      message.warning(t('rental.configuration.versionConflictReloaded'))
    }
  } finally {
    catalogSaving.value = false
  }
}

const openCreateRule = () => {
  if (!canUpdateRules.value || shops.value.length === 0) return
  editingRule.value = undefined
  drawerSkus.value = []
  drawerDraftKey.value = ''
  drawerScopeKey.value = ''
  drawerVisible.value = true
}

const openEditRule = async (row: RentalChannelProductRuleVO) => {
  ruleSaving.value = true
  try {
    const detail = await getRentalChannelProductRule(row.id)
    editingRule.value = detail
    drawerSkus.value =
      detail.mappingMode === 'MULTI'
        ? await getRentalChannelProductSkus(detail.shopId, detail.xianyuItemId)
        : []
    drawerScopeKey.value = buildRuleScopeKey(detail.shopId, detail.xianyuItemId)
    drawerVisible.value = true
  } finally {
    ruleSaving.value = false
  }
}

const loadDrawerSkus = async (shopId: number, xianyuItemId: string) => {
  const requestId = ++drawerSkuRequestId
  const scopeKey = buildRuleScopeKey(shopId, xianyuItemId)
  drawerScopeKey.value = scopeKey
  drawerSkuLoading.value = true
  try {
    const response = await getRentalChannelProductSkus(shopId, xianyuItemId)
    if (requestId === drawerSkuRequestId && drawerScopeKey.value === scopeKey) {
      drawerSkus.value = response
    }
  } finally {
    if (requestId === drawerSkuRequestId) {
      drawerSkuLoading.value = false
    }
  }
}

const handleDrawerScopeChange = (shopId: number | undefined, xianyuItemId: string) => {
  drawerSkuRequestId++
  drawerScopeKey.value = buildRuleScopeKey(shopId, xianyuItemId)
  drawerSkus.value = []
  drawerSkuLoading.value = false
}

const handleDrawerDraftChange = (draft: RentalChannelProductRuleDraft) => {
  try {
    drawerDraftKey.value = buildImpactPreviewKey(draft)
  } catch {
    drawerDraftKey.value = ''
  }
}

const previewRuleSave = async (draft: RentalChannelProductRuleDraft) => {
  let request: RentalChannelProductRuleSaveReqVO
  try {
    request = buildProductRuleSaveRequest(draft)
  } catch {
    message.error(t('rental.configuration.ruleVersionMissing'))
    await loadRules()
    return
  }
  ruleSaving.value = true
  try {
    impact.value = await previewRentalChannelProductRuleImpact(request.shopId, request.xianyuItemId)
    if (drawerDraftKey.value && drawerDraftKey.value !== buildImpactPreviewKey(draft)) {
      message.warning(t('rental.configuration.previewExpired'))
      return
    }
    pendingMutation.value = {
      kind: 'save',
      draft,
      request,
      previewKey: buildImpactPreviewKey(draft)
    }
    impactVisible.value = true
  } finally {
    ruleSaving.value = false
  }
}

const previewRuleStatusChange = async (rule: RentalChannelProductRuleVO) => {
  ruleSaving.value = true
  try {
    impact.value = await previewRentalChannelProductRuleImpact(rule.shopId, rule.xianyuItemId)
    pendingMutation.value = {
      kind: 'status',
      rule,
      enabled: !rule.enabled
    }
    impactVisible.value = true
  } finally {
    ruleSaving.value = false
  }
}

const confirmPendingMutation = async () => {
  const pending = pendingMutation.value
  if (!pending) return
  ruleSaving.value = true
  try {
    if (pending.kind === 'save') {
      if (
        buildImpactPreviewKey(pending.draft) !== pending.previewKey ||
        drawerDraftKey.value !== pending.previewKey
      ) {
        impactVisible.value = false
        message.warning(t('rental.configuration.previewExpired'))
        return
      }
      const result =
        pending.request.id !== undefined
          ? await updateRentalChannelProductRule(pending.request)
          : await createRentalChannelProductRule(pending.request)
      drawerVisible.value = false
      message.success(t('rental.configuration.ruleSaved'))
      await startReconciliation(result.reconciliationRunId)
    } else {
      const result = await updateRentalChannelProductRuleStatus({
        id: pending.rule.id,
        enabled: pending.enabled,
        lockVersion: pending.rule.lockVersion
      })
      message.success(t('rental.configuration.ruleStatusChanged'))
      await startReconciliation(result.reconciliationRunId)
    }
    impactVisible.value = false
    pendingMutation.value = undefined
    expandedRuleIds.value = []
    Object.keys(skuDetails).forEach((key) => delete skuDetails[key])
    await loadRules()
  } catch (error) {
    impactVisible.value = false
    pendingMutation.value = undefined
    const closeRuleEditor = () => {
      drawerVisible.value = false
      editingRule.value = undefined
      drawerSkus.value = []
      drawerDraftKey.value = ''
      drawerScopeKey.value = ''
      drawerSkuRequestId++
    }
    if (await recoverConfigurationVersionConflict(error, closeRuleEditor, loadRules)) {
      message.warning(t('rental.configuration.versionConflictReloaded'))
    }
  } finally {
    ruleSaving.value = false
  }
}

const startReconciliation = async (runId: number) => {
  reconciliationLoadError.value = false
  reconciliationRun.value = {
    runId,
    productRuleId: 0,
    shopId: 0,
    xianyuItemId: '',
    triggerType: 'RULE_CHANGE',
    status: 'PENDING',
    scannedCount: 0,
    skippedCount: 0,
    createdCount: 0,
    updatedCount: 0,
    unchangedCount: 0,
    conflictCount: 0,
    failedCount: 0,
    reviewRequiredCount: 0
  }
  reconciliationVisible.value = true
  await refreshReconciliation()
}

const refreshReconciliation = async () => {
  const runId = reconciliationRun.value?.runId
  if (runId === undefined) return
  reconciliationRefreshing.value = true
  if (reconciliationTimer) clearTimeout(reconciliationTimer)
  try {
    reconciliationRun.value = await getRentalChannelProductRuleReconciliation(runId)
    reconciliationLoadError.value = false
  } catch {
    reconciliationLoadError.value = true
  } finally {
    reconciliationRefreshing.value = false
  }
  if (!isTerminalReconciliationStatus(reconciliationRun.value?.status)) {
    reconciliationTimer = setTimeout(
      refreshReconciliation,
      reconciliationLoadError.value ? 3000 : 1500
    )
  }
}

const handleRuleExpand = async (rule: RentalChannelProductRuleVO, expanded: boolean) => {
  if (expanded) {
    const ruleKey = String(rule.id)
    if (!expandedRuleIds.value.includes(ruleKey)) {
      expandedRuleIds.value = [...expandedRuleIds.value, ruleKey]
    }
    if (rule.mappingMode !== 'MULTI' || skuDetails[String(rule.id)]) return
    skuLoadingIds.value = [...skuLoadingIds.value, rule.id]
    try {
      skuDetails[String(rule.id)] = await getRentalChannelProductSkus(
        rule.shopId,
        rule.xianyuItemId
      )
    } finally {
      skuLoadingIds.value = skuLoadingIds.value.filter((id) => id !== rule.id)
    }
    return
  }
  expandedRuleIds.value = expandedRuleIds.value.filter((id) => id !== String(rule.id))
}

onMounted(() => {
  loadPage()
})

onBeforeUnmount(() => {
  if (reconciliationTimer) clearTimeout(reconciliationTimer)
})
</script>

<style scoped>
.rental-configuration-page {
  min-width: 0;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 18px;
}

.page-heading span {
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--el-color-primary);
}

.page-heading h1 {
  margin: 5px 0 0;
  font-size: 24px;
  color: var(--el-text-color-primary);
}

.page-heading p {
  max-width: 780px;
  margin: 7px 0 0;
  font-size: 13px;
  line-height: 20px;
  color: var(--el-text-color-secondary);
}

.configuration-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.rule-filters {
  padding: 14px 14px 0;
  margin-bottom: 16px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

@media (width <= 720px) {
  .page-heading {
    flex-direction: column;
  }

  .page-heading .el-button {
    width: 100%;
  }

  .rule-filters {
    display: grid;
  }

  .rule-filters :deep(.el-form-item),
  .rule-filters :deep(.el-select),
  .rule-filters :deep(.el-input) {
    width: 100% !important;
    margin-right: 0;
  }

  .rule-filters :deep(.el-form-item__content) {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .rule-filters :deep(.el-form-item__content .el-button) {
    width: 100%;
    margin: 0;
  }
}
</style>
