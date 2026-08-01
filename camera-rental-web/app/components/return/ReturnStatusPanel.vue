<script setup lang="ts">
import type { RegistrationStatus, ReturnReceipt } from '~/types/return-registration'

const props = defineProps<{ status: RegistrationStatus | 'INVALID'; receipt?: ReturnReceipt }>()
const { t } = useReturnPreferences()
const content = computed(() => {
  const map = {
    EXPIRED: [t('statusExpiredTitle'), t('statusExpiredBody')],
    REVOKED: [t('statusRevokedTitle'), t('statusRevokedBody')],
    REJECTED: [t('statusRejectedTitle'), t('statusRejectedBody')],
    REVIEW_REQUIRED: [t('statusReviewTitle'), t('statusReviewBody')],
    ACCEPTED: [t('statusAcceptedTitle'), t('statusAcceptedBody')],
    INVALID: [t('statusInvalidTitle'), t('statusInvalidBody')],
    DRAFT: ['', '']
  }
  return map[props.status]
})
</script>

<template>
  <section class="status-panel">
    <span class="status-mark">{{ status === 'ACCEPTED' ? '✓' : 'i' }}</span>
    <p>RETURN REGISTRATION</p>
    <h1>{{ content[0] }}</h1>
    <div>{{ content[1] }}</div>
    <dl v-if="receipt">
      <div><dt>{{ t('registrationNo') }}</dt><dd>{{ receipt.formNo }}</dd></div>
      <div><dt>{{ t('waybill') }}</dt><dd>{{ receipt.waybillNo || '—' }}</dd></div>
      <div><dt>{{ t('currentStatus') }}</dt><dd>{{ receipt.status }}</dd></div>
    </dl>
  </section>
</template>
