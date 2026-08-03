package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncCursorMapper;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriodParser;
import cn.iocoder.yudao.module.rental.service.SellerRemarkAiFallbackService;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriodResolver;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
import cn.iocoder.yudao.module.rental.service.logistics.XianyuOrderDeliverySyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuOrderPersistenceServiceImplTest {

    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuOrderMapper orderMapper;
    @Mock
    private XianyuSyncCursorMapper cursorMapper;
    @Mock
    private XianyuRentalConversionService conversionService;
    @Mock
    private XianyuOrderDeliverySyncService deliverySyncService;
    @Mock
    private SellerRemarkAiFallbackService aiFallbackService;

    private XianyuOrderPersistenceService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        service = new XianyuOrderPersistenceServiceImpl(
                new XianyuOrderPayloadParser(new ObjectMapper()),
                new XianyuPayloadHasher(),
                rawPayloadMapper,
                orderMapper,
                cursorMapper,
                new XianyuSyncCursorAdvancer(),
                conversionService,
                deliverySyncService,
                new SellerRemarkRentalPeriodResolver(
                        new SellerRemarkRentalPeriodParser(), aiFallbackService),
                Clock.fixed(Instant.parse("2026-07-23T10:11:12Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPersistRestrictedRawPayloadBeforeNormalizedOrder() {
        String rawPayload = """
                {"code":0,"data":{"order_no":"order-100","order_status":21,"pay_amount":3000000000,
                "seller_remark":"","create_time":1704067200,"update_time":1704153600,
                "goods":{"product_id":"product-1","sku_id":"sku-1"}}}""";
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(99L).build());
        when(orderMapper.selectByShopIdAndExternalOrderIdForUpdate(7L, "order-100")).thenReturn(null);
        doAnswer(invocation -> {
            invocation.getArgument(0, XianyuOrderDO.class).setId(1001L);
            return 1;
        }).when(orderMapper).insert(any(XianyuOrderDO.class));

        XianyuOrderDO result = service.persistOrderDetail(7L, rawPayload);

        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        ArgumentCaptor<XianyuOrderDO> orderCaptor = ArgumentCaptor.forClass(XianyuOrderDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        verify(orderMapper).insert(orderCaptor.capture());
        InOrder writeOrder = inOrder(rawPayloadMapper, orderMapper, conversionService);
        writeOrder.verify(rawPayloadMapper).insertOrReuse(eq(9L), any(XianyuRawPayloadDO.class));
        writeOrder.verify(rawPayloadMapper)
                .selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any());
        writeOrder.verify(orderMapper).insert(any(XianyuOrderDO.class));
        writeOrder.verify(conversionService).autoConvertAfterPersist(1001L);
        assertEquals(rawPayload, rawCaptor.getValue().getPayload());
        assertEquals("order:7:order-100", rawCaptor.getValue().getSourceIdentifier());
        assertEquals(64, rawCaptor.getValue().getPayloadHash().length());
        assertEquals(99L, orderCaptor.getValue().getRawPayloadId());
        assertEquals(3_000_000_000L, orderCaptor.getValue().getPayAmount());
        assertEquals("PENDING", orderCaptor.getValue().getRentalPeriodStatus());
        assertEquals("MISSING_REMARK", orderCaptor.getValue().getRentalPeriodReasonCode());
        assertNull(orderCaptor.getValue().getBillableStartDate());
        assertNull(orderCaptor.getValue().getBillableEndDate());
        assertEquals("PENDING", result.getConversionStatus());
    }

    @Test
    void shouldUpdateExistingOrderWithoutDuplicatingAnIdenticalRawPayload() {
        String rawPayload = """
                {"code":0,"data":{"order_no":"order-200","order_status":22,"pay_amount":100,
                "seller_remark":"发货7.25/收货7.26/发回8.02","order_time":1784952000,
                "goods":{"product_id":"product-1","sku_id":"0"}}}""";
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(5L).build());
        when(orderMapper.selectByShopIdAndExternalOrderIdForUpdate(8L, "order-200"))
                .thenReturn(XianyuOrderDO.builder().id(6L)
                        .sellerRemark("发货7.25/收货7.26/发回8.02")
                        .remarkParseVersion("remark-v1").remarkParseStatus("RESOLVED")
                        .conversionStatus("REVIEW_REQUIRED")
                        .rentalOrderId(7L).build());

        XianyuOrderDO result = service.persistOrderDetail(8L, rawPayload);

        verify(rawPayloadMapper).insertOrReuse(eq(9L), any(XianyuRawPayloadDO.class));
        verify(orderMapper).updateById(result);
        verify(conversionService).autoConvertAfterPersist(6L);
        assertEquals(6L, result.getId());
        assertEquals(5L, result.getRawPayloadId());
        assertEquals("REVIEW_REQUIRED", result.getConversionStatus());
        assertEquals(7L, result.getRentalOrderId());
        assertEquals("0", result.getExternalSkuId());
        assertEquals(SellerRemarkRentalPeriodResolver.VERSION, result.getRemarkParseVersion());
        assertEquals("SUCCESS", result.getRemarkParseStatus());
        assertEquals(LocalDate.of(2026, 7, 27), result.getBillableStartDate());
        assertEquals(LocalDate.of(2026, 8, 2), result.getBillableEndDate());
        assertEquals("SUCCESS", result.getRentalPeriodStatus());
    }

    @Test
    void shouldPreserveReceiverSnapshotWhenShippedDetailOmitsContact() {
        String rawPayload = """
                {"code":0,"data":{"order_no":"order-shipped","order_status":21,"pay_amount":100,
                "consign_time":1785200400,"update_time":1785200400,
                "goods":{"product_id":"product-1","sku_id":"0"}}}""";
        XianyuOrderDO existing = XianyuOrderDO.builder()
                .id(1004L)
                .shopId(8L)
                .externalOrderId("order-shipped")
                .receiverName("张三")
                .receiverMobile("13800138000")
                .receiverAddress("湖南省长沙市测试地址")
                .conversionStatus("REVIEW_REQUIRED")
                .sourceUpdatedAt(LocalDateTime.of(2026, 7, 27, 0, 0))
                .build();
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(8L).build());
        when(orderMapper.selectByShopIdAndExternalOrderIdForUpdate(8L, "order-shipped"))
                .thenReturn(existing);

        XianyuOrderDO result = service.persistOrderDetail(8L, rawPayload);

        assertEquals("张三", result.getReceiverName());
        assertEquals("13800138000", result.getReceiverMobile());
        assertEquals("湖南省长沙市测试地址", result.getReceiverAddress());
        verify(orderMapper).updateById(result);
    }

    @Test
    void shouldPersistTargetOrderRentalPeriodFromSellerRemark() {
        String rawPayload = """
                {"code":0,"data":{"order_no":"test-order-6425","order_status":12,"pay_amount":100,
                "seller_remark":"发货7.28上午/收货7.28下午/发回8.05","order_time":1785196800,
                "goods":{"product_id":"product-1","sku_id":"0"}}}""";
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(6L).build());
        when(orderMapper.selectByShopIdAndExternalOrderIdForUpdate(8L, "test-order-6425"))
                .thenReturn(null);
        doAnswer(invocation -> {
            invocation.getArgument(0, XianyuOrderDO.class).setId(1002L);
            return 1;
        }).when(orderMapper).insert(any(XianyuOrderDO.class));

        XianyuOrderDO result = service.persistOrderDetail(8L, rawPayload);

        assertEquals(LocalDate.of(2026, 7, 29), result.getBillableStartDate());
        assertEquals(LocalDate.of(2026, 8, 5), result.getBillableEndDate());
        assertEquals("SUCCESS", result.getRentalPeriodStatus());
        assertNull(result.getRentalPeriodReasonCode());
    }

    @Test
    void shouldReplacePendingRentalPeriodWhenLaterDetailContainsCompleteRemark() {
        String rawPayload = """
                {"code":0,"data":{"order_no":"order-later-remark","order_status":12,"pay_amount":100,
                "seller_remark":"发货7.28/收货7.28/发回8.05","order_time":1785196800,
                "update_time":1785283200,
                "goods":{"product_id":"product-1","sku_id":"0"}}}""";
        XianyuOrderDO existing = XianyuOrderDO.builder()
                .id(1003L)
                .shopId(8L)
                .externalOrderId("order-later-remark")
                .conversionStatus("PENDING")
                .rentalPeriodStatus("PENDING")
                .rentalPeriodReasonCode("MISSING_REMARK")
                .sourceUpdatedAt(LocalDateTime.of(2026, 7, 27, 0, 0))
                .build();
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(7L).build());
        when(orderMapper.selectByShopIdAndExternalOrderIdForUpdate(8L, "order-later-remark"))
                .thenReturn(existing);

        XianyuOrderDO result = service.persistOrderDetail(8L, rawPayload);

        assertEquals("SUCCESS", result.getRentalPeriodStatus());
        assertEquals(LocalDate.of(2026, 7, 29), result.getBillableStartDate());
        assertEquals(LocalDate.of(2026, 8, 5), result.getBillableEndDate());
        assertNull(result.getRentalPeriodReasonCode());
        verify(orderMapper).updateById(result);
        verify(conversionService).autoConvertAfterPersist(1003L);
    }

    @Test
    void shouldBackfillHistoricalOrderRentalPeriodWithoutRemoteAccess() {
        XianyuOrderDO historical = XianyuOrderDO.builder()
                .id(77L)
                .sellerRemark("发货7.27/收货7.28/发回8.05")
                .orderTime(LocalDateTime.of(2026, 7, 27, 18, 0))
                .build();
        when(orderMapper.selectMissingRentalPeriodRefs(SellerRemarkRentalPeriodResolver.VERSION, 500))
                .thenReturn(List.of(historical));

        int count = service.backfillMissingRentalPeriods(999);

        assertEquals(1, count);
        assertEquals(LocalDate.of(2026, 7, 29), historical.getBillableStartDate());
        assertEquals(LocalDate.of(2026, 8, 5), historical.getBillableEndDate());
        assertEquals("SUCCESS", historical.getRentalPeriodStatus());
        verify(orderMapper).updateById(historical);
        verify(conversionService).autoConvertAfterPersist(77L);
    }

    @Test
    void shouldPreserveNewerNormalizedOrderWhenAnOlderDetailArrivesLate() {
        String rawPayload = """
                {"code":0,"data":{"order_no":"order-older","order_status":21,"pay_amount":100,
                "update_time":1704153600,"goods":{"product_id":"product-1"}}}""";
        XianyuOrderDO existing = XianyuOrderDO.builder()
                .id(7L)
                .externalOrderId("order-older")
                .orderStatus("22")
                .payAmount(200L)
                .sourceUpdatedAt(LocalDateTime.of(2024, 1, 3, 0, 0))
                .build();
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(100L).build());
        when(orderMapper.selectByShopIdAndExternalOrderIdForUpdate(8L, "order-older")).thenReturn(existing);

        XianyuOrderDO result = service.persistOrderDetail(8L, rawPayload);

        assertEquals(existing, result);
        assertEquals("22", result.getOrderStatus());
        assertEquals(200L, result.getPayAmount());
        verify(rawPayloadMapper).insertOrReuse(eq(9L), any(XianyuRawPayloadDO.class));
        verify(orderMapper, never()).updateById(any(XianyuOrderDO.class));
        verify(conversionService, never()).autoConvertAfterPersist(any());
    }

    @Test
    void shouldAdvanceOnlyToAStrictlyNewerCursorPoint() {
        LocalDateTime updateTime = LocalDateTime.of(2026, 7, 23, 12, 0);
        LocalDateTime upperBound = LocalDateTime.of(2026, 7, 23, 13, 0);
        when(cursorMapper.selectByShopIdAndResourceTypeForUpdate(9L, "ORDER")).thenReturn(null);

        assertTrue(service.advanceOrderCursor(9L, updateTime, "order-300", upperBound));

        ArgumentCaptor<XianyuSyncCursorDO> cursorCaptor = ArgumentCaptor.forClass(XianyuSyncCursorDO.class);
        verify(cursorMapper).insert(cursorCaptor.capture());
        assertEquals(updateTime, cursorCaptor.getValue().getCursorUpdatedAt());
        assertEquals("order-300", cursorCaptor.getValue().getCursorExternalId());
        assertEquals(upperBound, cursorCaptor.getValue().getSafeUpperBound());

        when(cursorMapper.selectByShopIdAndResourceTypeForUpdate(9L, "ORDER"))
                .thenReturn(XianyuSyncCursorDO.builder().id(12L).cursorUpdatedAt(updateTime)
                        .cursorExternalId("order-300").build());
        assertFalse(service.advanceOrderCursor(9L, updateTime, "order-300", upperBound));
        verify(cursorMapper, never()).updateById(any(XianyuSyncCursorDO.class));
    }

}
