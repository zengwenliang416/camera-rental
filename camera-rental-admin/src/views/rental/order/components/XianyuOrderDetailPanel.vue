<template>
  <div class="order-detail-panel">
    <el-descriptions :column="3" border>
      <el-descriptions-item :label="t('rental.order.externalOrderId')">
        {{ value(order.externalOrderId) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.orderStatus')">
        {{ rentalLabel('channelOrder', order.orderStatus) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.conversionStatus')">
        {{ rentalLabel('conversion', order.conversionStatus) }}
      </el-descriptions-item>

      <el-descriptions-item :label="t('rental.xianyu.buyerNick')">
        {{ value(order.buyerNick) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.receiverName')">
        {{ value(order.receiverName) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.receiverMobile')">
        {{ value(order.receiverMobile) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.receiverAddress')" :span="3">
        {{ value(order.receiverAddress) }}
      </el-descriptions-item>

      <el-descriptions-item :label="t('rental.order.goodsTitle')" :span="2">
        {{ value(order.goodsTitle) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.goodsQuantity')">
        {{ order.goodsQuantity ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.externalProductId')">
        {{ value(order.externalProductId) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.externalSkuId')">
        {{ value(order.externalSkuId) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.equipmentModelCode')">
        {{ value(order.equipmentModelCode) }}
      </el-descriptions-item>

      <el-descriptions-item :label="t('rental.order.payAmountFen')">
        {{ amount(order.payAmount) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.totalAmount')">
        {{ amount(order.totalAmount) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.refundAmount')">
        {{ amount(order.refundAmount) }}
      </el-descriptions-item>

      <el-descriptions-item :label="t('rental.order.sellerRemark')" :span="3">
        <span class="whitespace-pre-wrap">{{ value(order.sellerRemark) }}</span>
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.remarkParseStatus')">
        {{ remarkStatus }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.remarkParseReason')">
        {{ reasonLabel }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.remarkParseVersion')">
        {{ value(order.remarkParseVersion) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.billablePeriod')">
        {{ dateRange(order.billableStartDate, order.billableEndDate) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.occupiedPeriod')">
        {{ dateRange(order.occupyStartDate, order.occupyEndDateExclusive) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.assignedDevices')">
        {{ order.assignedDeviceIds?.join(', ') || '-' }}
      </el-descriptions-item>

      <el-descriptions-item :label="t('rental.order.expressName')">
        {{ value(order.expressName) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.xianyu.waybillNo')">
        {{ value(order.waybillNo) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.consignTime')">
        {{ dateTime(order.consignTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.orderTime')">
        {{ dateTime(order.orderTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.xianyu.sourceUpdatedAt')">
        {{ dateTime(order.sourceUpdatedAt) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('rental.order.rentalOrderId')">
        {{ order.rentalOrderId ?? '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { fenToYuan } from '@/utils'
import { formatNullableDate } from '@/utils/formatTime'
import { getRentalLabelKey, type RentalLabelGroup } from '@/utils/rentalLabels'
import type { XianyuOrderVO } from '@/api/rental/xianyu'

const props = defineProps<{ order: XianyuOrderVO }>()
const { t } = useI18n()

const value = (input?: string | null) => input?.trim() || '-'
const amount = (input?: number | null) =>
  input == null ? '-' : t('rental.common.yuanAmount', { amount: fenToYuan(input) })
const dateTime = (input?: string | null) => formatNullableDate(input)
const dateRange = (start?: string | null, end?: string | null) =>
  start && end ? `${start} → ${end}` : start || end || '-'
const rentalLabel = (group: RentalLabelGroup, input?: string | number | null) =>
  t(getRentalLabelKey(group, input), { code: input ?? '' })

const remarkStatus = computed(() => {
  const status = props.order.remarkParseStatus
  return status && ['PENDING', 'SUCCESS', 'FAILED'].includes(status)
    ? t(`rental.order.remarkParse.${status}`)
    : t('rental.order.remarkParse.UNKNOWN')
})

const reasonLabel = computed(() => {
  const reason = props.order.rentalPeriodReasonCode
  if (!reason) return '-'
  const key = `rental.order.remarkReason.${reason}`
  const translated = t(key, { code: reason })
  return translated === key ? reason : translated
})
</script>

<style scoped>
.order-detail-panel {
  padding: 12px 16px;
  background: var(--el-fill-color-extra-light);
}
</style>
