import type { PendingAllocationOrder, StaffChannelOrder } from '@/api/rental/order'

const reasons: Record<string, string> = {
  LOGISTICS_DATE_BEFORE_ORDER: '物流日期早于下单日期，请核对备注中的月份和日期',
  INVALID_LOGISTICS_RANGE: '发货、收货、发回日期顺序冲突，请核对备注',
  INVALID_RENTAL_DATE: '租期日期无效，请核对备注',
  INVALID_RENTAL_RANGE: '租期结束日期早于开始日期，请核对备注',
  MISSING_RECEIVE_DATE: '缺少收货日期，请核对实际约定后补充备注',
  MISSING_RETURN_DATE: '缺少发回日期，请核对实际约定后补充备注',
  MISSING_SHIP_DATE: '缺少发货日期，请核对实际约定后补充备注',
  MISSING_ORDER_DATE: '缺少下单日期，请在管理后台核对渠道数据',
  MISSING_REMARK: '卖家备注为空，请补充实际约定的租赁信息',
  RENTAL_PERIOD_NOT_FOUND: '未识别出有效租期，请核对备注',
  RENTAL_PERIOD_NOT_READY: '租期信息待补全，请在管理后台核对',
  OCCUPIED_PERIOD_NOT_READY: '设备占用日期待补全，请在管理后台核对',
  PRODUCT_RULE_NOT_CONFIGURED: '商品尚未配置设备型号，请在管理后台处理',
  SINGLE_MODEL_NOT_CONFIGURED: '商品尚未配置设备型号，请在管理后台处理',
  SKU_MODEL_NOT_CONFIGURED: '规格尚未配置设备型号，请在管理后台处理',
  MODEL_NOT_CONFIGURED: '设备型号待确认，请在管理后台处理',
  RENTAL_ORDER_LINK_CONFLICT: '订单关联异常，请在管理后台核对',
  ORDER_NOT_PAID: '订单尚未付款',
  ORDER_REFUNDED: '渠道订单已退款',
  ORDER_CLOSED: '渠道订单已关闭',
}
export function preparationHint(order: PendingAllocationOrder | StaffChannelOrder, channelOnly = false) {
  if (order.preparationReasonCode && reasons[order.preparationReasonCode])
    return reasons[order.preparationReasonCode]
  if (order.conversionStatus === 'CONFIG_SKIPPED')
    return '该商品配置为不生成租赁订单，请核对商品规则'
  if (order.conversionStatus === 'CLOSED' || order.shippingStatus === 'CANCELED')
    return '渠道订单已关闭或退款，请核对订单状态'
  if (order.shippingStatus === 'UNPAID')
    return '订单尚未付款'
  if (order.preparationStatus && order.preparationStatus !== 'READY')
    return '订单资料待核对，请在管理后台查看原因'
  return channelOnly ? '尚未生成租赁订单，请在管理后台核对转换状态' : ''
}
