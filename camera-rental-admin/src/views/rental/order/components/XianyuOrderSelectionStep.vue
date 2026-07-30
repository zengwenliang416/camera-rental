<template>
  <el-form label-position="top" :model="form">
    <el-row :gutter="16">
      <el-col :xs="24" :md="10">
        <el-form-item :label="t('rental.order.filterShop')">
          <el-select
            v-model="form.shopId"
            class="!w-1/1"
            filterable
            clearable
            :placeholder="t('rental.order.shopPlaceholder')"
          >
            <el-option
              v-for="shop in shops"
              :key="shop.id"
              :label="shop.shopName || String(shop.id)"
              :value="shop.id"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-form-item :label="t('rental.xianyu.pendingKeyword')">
          <el-input
            v-model.trim="form.keyword"
            clearable
            :placeholder="t('rental.xianyu.pendingKeywordPlaceholder')"
            @keyup.enter="emit('search', true)"
          />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :md="4">
        <el-form-item label=" ">
          <el-button
            v-hasPermi="['rental:xianyu:ship']"
            class="!w-1/1"
            type="primary"
            :loading="loading"
            @click="emit('search', true)"
          >
            {{ t('rental.xianyu.searchPendingShip') }}
          </el-button>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
  <el-table v-loading="loading" :data="orders" highlight-current-row @current-change="handleSelect">
    <el-table-column width="52">
      <template #default="{ row }">
        <el-radio :model-value="selectedId" :value="row.id" @change="handleSelect(row)" />
      </template>
    </el-table-column>
    <el-table-column :label="t('rental.order.externalOrderId')" min-width="180">
      <template #default="{ row }">{{ row.externalOrderId || '-' }}</template>
    </el-table-column>
    <el-table-column prop="goodsTitle" :label="t('rental.order.goodsTitle')" min-width="220" />
    <el-table-column :label="t('rental.xianyu.buyerNick')" min-width="120">
      <template #default="{ row }">{{ row.buyerNick || '-' }}</template>
    </el-table-column>
    <el-table-column prop="receiverName" :label="t('rental.order.receiverName')" min-width="110" />
    <el-table-column :label="t('rental.order.receiverMobile')" min-width="140">
      <template #default="{ row }">{{ row.receiverMobile || '-' }}</template>
    </el-table-column>
    <el-table-column
      prop="receiverAddress"
      :label="t('rental.order.receiverAddress')"
      min-width="220"
      show-overflow-tooltip
    />
    <el-table-column
      prop="sellerRemark"
      :label="t('rental.order.sellerRemark')"
      min-width="180"
      show-overflow-tooltip
    />
    <el-table-column :label="t('rental.order.payAmountFen')" width="120">
      <template #default="{ row }">{{ formatFen(row.payAmount) }}</template>
    </el-table-column>
    <el-table-column
      prop="conversionStatus"
      :label="t('rental.order.conversionStatus')"
      width="140"
    />
    <el-table-column :label="t('rental.xianyu.sourceUpdatedAt')" width="180">
      <template #default="{ row }">{{ formatNullableDate(row.sourceUpdatedAt) }}</template>
    </el-table-column>
    <template #empty>
      <div class="py-24px text-[var(--el-text-color-secondary)]">
        {{ t('rental.xianyu.pendingShipEmptyHint') }}
      </div>
    </template>
  </el-table>
  <Pagination
    :total="total"
    v-model:page="form.pageNo"
    v-model:limit="form.pageSize"
    @pagination="emit('search', false)"
  />
</template>

<script lang="ts" setup>
import type { XianyuPendingShipOrderVO, XianyuShopVO } from '@/api/rental/xianyu'
import { useI18n } from '@/hooks/web/useI18n'
import { formatNullableDate } from '@/utils/formatTime'
import type { XianyuShipmentForm } from './xianyuShipWorkbenchTypes'

defineOptions({ name: 'XianyuOrderSelectionStep' })

defineProps<{
  form: XianyuShipmentForm
  shops: XianyuShopVO[]
  orders: XianyuPendingShipOrderVO[]
  total: number
  selectedId?: number
  loading: boolean
}>()

const emit = defineEmits<{
  search: [resetPage: boolean]
  select: [order?: XianyuPendingShipOrderVO]
}>()
const { t } = useI18n()

const handleSelect = (order?: XianyuPendingShipOrderVO) => {
  emit('select', order)
}

const formatFen = (value?: number | null) => {
  return value === undefined || value === null ? '-' : `¥${(value / 100).toFixed(2)}`
}
</script>
