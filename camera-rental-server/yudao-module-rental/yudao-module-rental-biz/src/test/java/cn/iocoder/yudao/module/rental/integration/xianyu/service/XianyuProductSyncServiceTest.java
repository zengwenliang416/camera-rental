package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncCursorMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncRunMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAlertAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuProductSyncServiceTest {

    @Mock
    private XianyuReadClient readClient;
    @Mock
    private XianyuProductPersistenceService productPersistenceService;
    @Mock
    private XianyuProductSkuPersistenceService skuPersistenceService;
    @Mock
    private XianyuProductMapper productMapper;
    @Mock
    private XianyuRawPayloadMapper rawPayloadMapper;
    @Mock
    private XianyuSyncCursorMapper cursorMapper;
    @Mock
    private XianyuSyncRunMapper syncRunMapper;
    @Mock
    private XianyuAlertAdminService alertAdminService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private XianyuProductSyncService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        service = new XianyuProductSyncService(
                readClient,
                new XianyuProductListPageParser(),
                productPersistenceService,
                skuPersistenceService,
                productMapper,
                rawPayloadMapper,
                cursorMapper,
                new XianyuSyncCursorAdvancer(),
                syncRunMapper,
                alertAdminService,
                new XianyuPayloadHasher(),
                objectMapper,
                Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPersistProductPageThenRefreshDetailsAndMultiSpecSkus() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.PRODUCTS), any(), isNull())).thenReturn(response(productPage()));
        when(readClient.execute(eq(XianyuReadEndpoint.PRODUCT_DETAIL), any(), isNull()))
                .thenReturn(response(productDetail("448592974859525")))
                .thenReturn(response(productDetail("448592974859526")));
        when(readClient.execute(eq(XianyuReadEndpoint.PRODUCT_SKUS), any(), isNull())).thenReturn(response(skuPage()));
        when(productMapper.selectRefreshStateList(eq(77L), any())).thenReturn(List.of());
        when(skuPersistenceService.persistProductSkus(77L, skuPage())).thenReturn(1);

        XianyuProductPageSyncResult result = service.syncPage(77L,
                new XianyuProductSyncWindow(LocalDateTime.of(2026, 7, 1, 0, 0),
                        LocalDateTime.of(2026, 7, 2, 0, 0), 1, 2));

        assertEquals(2, result.receivedCount());
        assertEquals(2, result.succeededCount());
        assertEquals(1, result.skuCount());
        ArgumentCaptor<XianyuRawPayloadDO> rawCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), rawCaptor.capture());
        assertEquals("PRODUCT_PAGE", rawCaptor.getValue().getSourceType());
        verify(readClient).execute(eq(XianyuReadEndpoint.PRODUCTS), any(), isNull());
        verify(readClient).execute(eq(XianyuReadEndpoint.PRODUCT_SKUS), any(), isNull());
        verify(productPersistenceService).persistProductDetail(77L, productDetail("448592974859525"));
        verify(productPersistenceService).persistProductDetail(77L, productDetail("448592974859526"));
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("SUCCEEDED", runCaptor.getValue().getStatus());
    }

    @Test
    void shouldRecordSyncFailureForWindowOutsideRetention() {
        try {
            service.syncPage(77L, new XianyuProductSyncWindow(
                    LocalDateTime.of(2025, 1, 1, 0, 0),
                    LocalDateTime.of(2025, 1, 2, 0, 0), 1, 50));
        } catch (RuntimeException ignored) {
            // Expected.
        }

        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("FAILED", runCaptor.getValue().getStatus());
        assertEquals("WINDOW_OUTSIDE_RETENTION", runCaptor.getValue().getLastErrorCode());
        verify(alertAdminService).recordSyncFailed(77L, "PRODUCT", "WINDOW_OUTSIDE_RETENTION");
    }

    @Test
    void shouldAdvanceOnlyToAStrictlyNewerProductCursorPoint() {
        LocalDateTime updateTime = LocalDateTime.of(2026, 7, 24, 12, 0);
        LocalDateTime upperBound = LocalDateTime.of(2026, 7, 24, 13, 0);
        when(cursorMapper.selectByShopIdAndResourceTypeForUpdate(9L, "PRODUCT")).thenReturn(null);

        assertTrue(service.advanceProductCursor(9L, updateTime, "448592974859525", upperBound));

        ArgumentCaptor<XianyuSyncCursorDO> cursorCaptor = ArgumentCaptor.forClass(XianyuSyncCursorDO.class);
        verify(cursorMapper).insert(cursorCaptor.capture());
        assertEquals(updateTime, cursorCaptor.getValue().getCursorUpdatedAt());
        assertEquals("448592974859525", cursorCaptor.getValue().getCursorExternalId());
        assertEquals(upperBound, cursorCaptor.getValue().getSafeUpperBound());

        when(cursorMapper.selectByShopIdAndResourceTypeForUpdate(9L, "PRODUCT"))
                .thenReturn(XianyuSyncCursorDO.builder().id(12L).cursorUpdatedAt(updateTime)
                        .cursorExternalId("448592974859525").build());
        assertFalse(service.advanceProductCursor(9L, updateTime, "448592974859525", upperBound));
        verify(cursorMapper, never()).updateById(any(XianyuSyncCursorDO.class));
    }

    private XianyuReadResponse response(String rawBody) throws Exception {
        return new XianyuReadResponse(200, 0, objectMapper.readTree(rawBody), rawBody);
    }

    private String productPage() {
        return """
                {"code":0,"msg":"OK","data":{"list":[
                {"product_id":448592974859525,"update_time":1782864000,"spec_type":2},
                {"product_id":448592974859526,"update_time":1782864000,"spec_type":1}
                ],"count":2,"page_no":1,"page_size":2}}
                """;
    }

    private String productDetail(String productId) {
        return """
                {"code":0,"msg":"OK","data":{"product_id":%s,"product_status":22,
                "channel_cat_id":"camera","title":"Sony A7M4","update_time":1782864000}}
                """.formatted(productId);
    }

    private String skuPage() {
        return """
                {"code":0,"msg":"OK","data":{"list":[{"product_id":448592974859525,
                "sku_items":[{"sku_id":537044127563786,"price":2,"stock":1,
                "sku_text":"颜色:蓝色"}]}]}}
                """;
    }

}
