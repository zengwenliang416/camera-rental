<template>
  <ContentWrap>
    <div class="logistics-hero">
      <div>
        <div class="logistics-kicker">KUAIDI100 · TENANT CONFIGURATION</div>
        <h2>{{ t('rental.logistics.pageTitle') }}</h2>
        <p>{{ t('rental.logistics.pageDescription') }}</p>
      </div>
      <div class="flex items-center gap-8px">
        <el-tag :type="logisticsStatusTagType(config?.configStatus)" effect="dark">
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
.logistics-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 22px 24px;
  margin-bottom: 16px;
  color: #f8fbff;
  background:
    linear-gradient(120deg, rgb(8 35 51 / 96%), rgb(16 74 86 / 92%)),
    radial-gradient(circle at 85% 0%, rgb(67 208 170 / 45%), transparent 42%);
  border-radius: 10px;
  box-shadow: 0 12px 28px rgb(11 47 60 / 16%);
}

.logistics-hero h2 {
  margin: 6px 0;
  font-size: 24px;
  line-height: 1.25;
}

.logistics-hero p {
  max-width: 760px;
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgb(232 246 248 / 78%);
}

.logistics-kicker {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: #6ee7c4;
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
  .logistics-hero {
    flex-direction: column;
  }
}
</style>
