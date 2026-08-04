<template>
  <ContentWrap>
    <div class="logistics-toolbar">
      <div class="logistics-toolbar__info">
        <div class="logistics-toolbar__title">{{ t('rental.logistics.pageTitle') }}</div>
        <div class="logistics-toolbar__description">
          {{ t('rental.logistics.pageDescription') }}
        </div>
      </div>
      <div class="logistics-toolbar__actions">
        <el-tag :type="logisticsStatusTagType(config?.configStatus)" effect="plain">
          {{ statusLabel }}
        </el-tag>
        <el-button :loading="loading" @click="loadConfig">
          <Icon icon="ep:refresh" class="mr-5px" />
          {{ t('common.refresh') }}
        </el-button>
      </div>
    </div>

    <el-alert
      v-if="loadError"
      class="mb-16px"
      type="error"
      :closable="false"
      :title="t('rental.logistics.loadError')"
    >
      <el-button link type="primary" @click="loadConfig">
        {{ t('rental.common.retry') }}
      </el-button>
    </el-alert>

    <el-alert
      class="mb-16px"
      type="info"
      :closable="false"
      :title="t('rental.logistics.secretHint')"
    />

    <el-alert
      v-if="config"
      class="mb-16px"
      :type="configStatusAlertType"
      :closable="false"
      show-icon
      :title="configStatusSummary"
    >
      <div class="config-checklist">
        <div
          v-for="item in configChecklist"
          :key="item.key"
          class="config-checklist__item"
          :class="{ 'is-complete': item.complete }"
        >
          <Icon
            :icon="item.complete ? 'ep:circle-check-filled' : 'ep:warning-filled'"
            class="config-checklist__icon"
          />
          <span>{{ item.label }}</span>
        </div>
      </div>
      <div class="mt-8px text-12px">
        {{ t('rental.logistics.configurationOrderHint') }}
      </div>
    </el-alert>

    <LogisticsProviderCard
      :config="config"
      :loading="loading"
      @updated="applyConfig"
      @refresh="loadConfig"
    />
    <LogisticsBackfillCard />
    <LogisticsCredentialsCard :config="config" @refresh="loadConfig" />
  </ContentWrap>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { getRentalLogisticsProviderConfig } from '@/api/rental/logistics'
import type { RentalLogisticsProviderConfigVO } from '@/api/rental/logistics'
import { useI18n } from '@/hooks/web/useI18n'
import LogisticsBackfillCard from './components/LogisticsBackfillCard.vue'
import LogisticsCredentialsCard from './components/LogisticsCredentialsCard.vue'
import LogisticsProviderCard from './components/LogisticsProviderCard.vue'
import { logisticsStatusKey, logisticsStatusTagType } from './logisticsConfigModel'

defineOptions({ name: 'RentalLogisticsConfig' })

const { t } = useI18n()
const config = ref<RentalLogisticsProviderConfigVO>()
const loading = ref(false)
const loadError = ref(false)

const statusLabel = computed(() =>
  config.value?.configStatus
    ? t(logisticsStatusKey(config.value.configStatus))
    : t('rental.logistics.statusUnknown')
)

const configChecklist = computed(() => [
  {
    key: 'credential',
    label: t('rental.logistics.checkEnabledCredential'),
    complete: Boolean(
      config.value?.credentials.some(
        (credential) =>
          credential.enabled && credential.customerCodeConfigured && credential.apiKeyConfigured
      )
    )
  },
  {
    key: 'callbackBaseUrl',
    label: config.value?.subscribeEnabled
      ? t('rental.logistics.checkCallbackBaseUrl')
      : t('rental.logistics.checkCallbackBaseUrlOptional'),
    complete: !config.value?.subscribeEnabled || Boolean(config.value?.callbackBaseUrl)
  }
])

const configStatusSummary = computed(() => {
  if (config.value?.configStatus === 'LOCALLY_VERIFIED') {
    return t('rental.logistics.configurationVerifiedSummary')
  }
  if (config.value?.configStatus === 'READY_UNVERIFIED') {
    return t('rental.logistics.configurationReadySummary')
  }
  const missing = configChecklist.value
    .filter((item) => !item.complete)
    .map((item) => item.label)
    .join(t('rental.logistics.listSeparator'))
  return t('rental.logistics.configurationIncompleteSummary', {
    items: missing || t('rental.logistics.configurationUnknownMissing')
  })
})

const configStatusAlertType = computed<'success' | 'warning' | 'error'>(() => {
  if (config.value?.configStatus === 'LOCALLY_VERIFIED') return 'success'
  if (config.value?.configStatus === 'READY_UNVERIFIED') return 'warning'
  return 'error'
})

const applyConfig = (value: RentalLogisticsProviderConfigVO) => {
  config.value = value
}

const loadConfig = async () => {
  loading.value = true
  loadError.value = false
  try {
    applyConfig(await getRentalLogisticsProviderConfig())
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.logistics-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.logistics-toolbar__info {
  min-width: 0;
}

.logistics-toolbar__title {
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
  color: var(--el-text-color-primary);
}

.logistics-toolbar__description {
  margin-top: 2px;
  font-size: 13px;
  line-height: 20px;
  color: var(--el-text-color-secondary);
}

.logistics-toolbar__actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 8px;
}

.config-checklist {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 10px;
}

.config-checklist__item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--el-color-danger);
}

.config-checklist__item.is-complete {
  color: var(--el-color-success);
}

.config-checklist__icon {
  flex: none;
}

@media (width <= 720px) {
  .logistics-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .logistics-toolbar__actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
