import { apiClient, type PageResult } from '../../api/client';

export interface PendingShipmentSearchResult {
  id: number;
  shopId: number;
  externalOrderId: string;
  orderStatus: string;
  goodsTitle?: string;
  goodsQuantity?: number;
  payAmount?: number;
  buyerNick?: string;
  rentalOrderId?: number;
  conversionStatus: string;
  receiverName?: string;
  receiverMobile?: string;
  receiverAddress?: string;
  sellerRemark?: string;
}

export function searchPendingShipmentOrders(keyword: string) {
  return apiClient.get<PageResult<PendingShipmentSearchResult>>(
    '/rental/xianyu/order/pending-ship/page',
    {
      keyword: keyword.trim(),
      pageNo: 1,
      pageSize: 100,
    }
  );
}
