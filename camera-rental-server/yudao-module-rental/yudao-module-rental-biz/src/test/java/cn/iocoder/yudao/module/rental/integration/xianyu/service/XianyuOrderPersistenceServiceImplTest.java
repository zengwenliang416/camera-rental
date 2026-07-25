package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncCursorMapper;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        XianyuOrderDO result = service.persistOrderDetail(7L, rawPayload);

        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        ArgumentCaptor<XianyuOrderDO> orderCaptor = ArgumentCaptor.forClass(XianyuOrderDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        verify(orderMapper).insert(orderCaptor.capture());
        InOrder writeOrder = inOrder(rawPayloadMapper, orderMapper);
        writeOrder.verify(rawPayloadMapper).insertOrReuse(eq(9L), any(XianyuRawPayloadDO.class));
        writeOrder.verify(rawPayloadMapper)
                .selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any());
        writeOrder.verify(orderMapper).insert(any(XianyuOrderDO.class));
        assertEquals(rawPayload, rawCaptor.getValue().getPayload());
        assertEquals("order:7:order-100", rawCaptor.getValue().getSourceIdentifier());
        assertEquals(64, rawCaptor.getValue().getPayloadHash().length());
        assertEquals(99L, orderCaptor.getValue().getRawPayloadId());
        assertEquals(3_000_000_000L, orderCaptor.getValue().getPayAmount());
        assertEquals("PENDING", result.getConversionStatus());
    }

    @Test
    void shouldUpdateExistingOrderWithoutDuplicatingAnIdenticalRawPayload() {
        String rawPayload = """
                {"code":0,"data":{"order_no":"order-200","order_status":22,"pay_amount":100,"seller_remark":"keep",
                "goods":{"product_id":"product-1","sku_id":"0"}}}""";
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), any(), any(), any()))
                .thenReturn(XianyuRawPayloadDO.builder().id(5L).build());
        when(orderMapper.selectByShopIdAndExternalOrderIdForUpdate(8L, "order-200"))
                .thenReturn(XianyuOrderDO.builder().id(6L).sellerRemark("keep")
                        .remarkParseVersion("remark-v1").remarkParseStatus("RESOLVED")
                        .conversionStatus("REVIEW_REQUIRED")
                        .rentalOrderId(7L).build());

        XianyuOrderDO result = service.persistOrderDetail(8L, rawPayload);

        verify(rawPayloadMapper).insertOrReuse(eq(9L), any(XianyuRawPayloadDO.class));
        verify(orderMapper).updateById(result);
        assertEquals(6L, result.getId());
        assertEquals(5L, result.getRawPayloadId());
        assertEquals("REVIEW_REQUIRED", result.getConversionStatus());
        assertEquals(7L, result.getRentalOrderId());
        assertEquals("0", result.getExternalSkuId());
        assertEquals("remark-v1", result.getRemarkParseVersion());
        assertEquals("RESOLVED", result.getRemarkParseStatus());
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
