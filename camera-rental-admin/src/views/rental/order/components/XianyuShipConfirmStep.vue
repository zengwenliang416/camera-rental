<template>
  <el-descriptions :column="2" border>
    <el-descriptions-item :label="t('rental.xianyu.shipConfirmOrder')">
      {{ order?.externalOrderId || '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.order.goodsTitle')">
      {{ order?.goodsTitle || '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.order.receiverName')">
      {{ order?.receiverName || '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.order.receiverMobile')">
      {{ order?.receiverMobile || '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.order.receiverAddress')" :span="2">
      {{ order?.receiverAddress || '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.order.sellerRemark')" :span="2">
      {{ order?.sellerRemark || '-' }}
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.xianyu.shipConfirmDevice')">
      {{ form.deviceNo || '-' }}
      <span v-if="device?.serialNumber"> / {{ device.serialNumber }} </span>
    </el-descriptions-item>
    <el-descriptions-item :label="t('rental.xianyu.shipConfirmWaybill')">
      {{ form.expressName || '-' }} {{ form.waybillNo || '-' }}
    </el-descriptions-item>
  </el-descriptions>
  <el-alert
    v-if="requiresPendingPlanConfirmation"
    class="mt-12px"
    type="warning"
    :closable="false"
    :title="t('rental.xianyu.shipPendingPlanWarning')"
  />
  <el-alert
    v-if="requiresProductRuleBinding"
    class="mt-12px"
    :type="canBindProductRule ? 'warning' : 'error'"
    :closable="false"
    :title="
      canBindProductRule
        ? t('rental.xianyu.shipProductRuleBindWarning', {
            itemId: order?.xianyuItemId || '-',
            modelCode: device?.equipmentModelCode || '-'
          })
        : t('rental.xianyu.shipProductRuleBindPermissionRequired')
    "
  />
  <el-alert
    class="mt-12px"
    type="warning"
    :closable="false"
    :title="t('rental.xianyu.shipSubmitBackendHint')"
  />
  <el-alert
    v-if="result"
    class="mt-12px"
    type="success"
    :closable="false"
    :title="
      t('rental.xianyu.shipSuccess', {
        shipmentId: result.shipmentId,
        deviceNo: result.deviceNo,
        waybillNo: result.maskedWaybillNo
      })
    "
  />
</template>

<script lang="ts" setup>
import type { RentalDeviceVO } from '@/api/rental/device'
import type { XianyuOrderShipRespVO, XianyuPendingShipOrderVO } from '@/api/rental/xianyu'
import { useI18n } from '@/hooks/web/useI18n'
import type { XianyuShipmentForm } from './xianyuShipWorkbenchTypes'

defineOptions({ name: 'XianyuShipConfirmStep' })

defineProps<{
  form: XianyuShipmentForm
  order?: XianyuPendingShipOrderVO
  device?: RentalDeviceVO
  result?: XianyuOrderShipRespVO
  requiresProductRuleBinding?: boolean
  canBindProductRule?: boolean
  requiresPendingPlanConfirmation?: boolean
}>()

const { t } = useI18n()
</script>
