<template>
  <section class="remark-panel">
    <div class="panel-heading">
      <div>
        <h2>{{ t('rental.configuration.remarkTitle') }}</h2>
        <p>{{ t('rental.configuration.remarkHint') }}</p>
      </div>
    </div>

    <div class="remark-layout">
      <section>
        <h3>{{ t('rental.configuration.baseTemplates') }}</h3>
        <article v-for="(template, index) in templates" :key="template.value" class="template-card">
          <span class="template-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
          <div>
            <strong>{{ t(template.labelKey) }}</strong>
            <code>{{ template.value }}</code>
          </div>
          <el-button @click="copyTemplate(template.value)">
            <Icon icon="ep:copy-document" class="mr-5px" />
            {{ t('common.copy') }}
          </el-button>
        </article>
        <el-alert
          class="mt-14px"
          type="warning"
          :closable="false"
          show-icon
          :title="t('rental.configuration.remarkPreserve')"
        />
      </section>

      <section>
        <h3>{{ t('rental.configuration.specialCases') }}</h3>
        <div class="case-grid">
          <article v-for="item in specialCases" :key="item.key">
            <strong>{{ t(item.labelKey) }}</strong>
            <code>{{ item.example }}</code>
            <span>{{ t(item.hintKey) }}</span>
          </article>
        </div>
      </section>
    </div>
  </section>
</template>

<script lang="ts" setup>
import { useClipboard } from '@vueuse/core'
import { useI18n } from '@/hooks/web/useI18n'
import { useMessage } from '@/hooks/web/useMessage'

const { t } = useI18n()
const message = useMessage()
const { copy, isSupported } = useClipboard({ legacy: true })

const templates = [
  {
    labelKey: 'rental.configuration.deliveryTemplate',
    value: '发货8.31/收货9.1/发回9.6'
  },
  {
    labelKey: 'rental.configuration.pickupTemplate',
    value: '自提8.31/发回9.6'
  },
  {
    labelKey: 'rental.configuration.explicitTemplate',
    value: '发货8.31/收货9.1/租期9.2-9.6/发回9.6'
  }
]

const specialCases = [
  {
    key: 'renew',
    labelKey: 'rental.configuration.renew',
    example: '…/发回9.8/续租',
    hintKey: 'rental.configuration.renewHint'
  },
  {
    key: 'earlyReturn',
    labelKey: 'rental.configuration.earlyReturn',
    example: '…/发回9.4/早退',
    hintKey: 'rental.configuration.earlyHint'
  },
  {
    key: 'reschedule',
    labelKey: 'rental.configuration.reschedule',
    example: '完整新日期/改期',
    hintKey: 'rental.configuration.rescheduleHint'
  },
  {
    key: 'swap',
    labelKey: 'rental.configuration.swap',
    example: '完整日期/换机',
    hintKey: 'rental.configuration.reviewOnly'
  },
  {
    key: 'damage',
    labelKey: 'rental.configuration.damage',
    example: '完整日期/损坏',
    hintKey: 'rental.configuration.reviewOnly'
  },
  {
    key: 'lost',
    labelKey: 'rental.configuration.lost',
    example: '完整日期/遗失',
    hintKey: 'rental.configuration.reviewOnly'
  },
  {
    key: 'overdue',
    labelKey: 'rental.configuration.overdue',
    example: '完整日期/逾期',
    hintKey: 'rental.configuration.reviewOnly'
  },
  {
    key: 'delay',
    labelKey: 'rental.configuration.delay',
    example: '完整日期/物流延误',
    hintKey: 'rental.configuration.reviewOnly'
  }
]

const copyTemplate = async (value: string) => {
  if (!isSupported.value) {
    message.error(t('common.copyError'))
    return
  }
  try {
    await copy(value)
    message.success(t('rental.configuration.copied'))
  } catch {
    message.error(t('common.copyError'))
  }
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

.panel-heading h2,
.remark-layout h3 {
  margin: 0;
  color: var(--el-text-color-primary);
}

.panel-heading h2 {
  font-size: 18px;
}

.panel-heading p {
  margin: 5px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.remark-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.remark-layout h3 {
  margin-bottom: 12px;
  font-size: 15px;
}

.template-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 10px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.template-card__index {
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  color: var(--el-color-primary);
}

.template-card strong,
.template-card code {
  display: block;
}

.template-card code,
.case-grid code {
  margin-top: 5px;
  font-family: SFMono-Regular, Consolas, monospace;
  font-size: 12px;
  color: var(--el-text-color-primary);
}

.case-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.case-grid article {
  min-width: 0;
  padding: 14px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
}

.case-grid strong,
.case-grid code,
.case-grid span {
  display: block;
}

.case-grid span {
  margin-top: 7px;
  font-size: 12px;
  line-height: 18px;
  color: var(--el-text-color-secondary);
}

@media (width <= 840px) {
  .remark-layout {
    grid-template-columns: 1fr;
  }
}

@media (width <= 560px) {
  .template-card {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .template-card .el-button {
    grid-column: 1 / -1;
    width: 100%;
  }

  .case-grid {
    grid-template-columns: 1fr;
  }
}
</style>
