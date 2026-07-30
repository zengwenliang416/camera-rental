package cn.iocoder.yudao.module.rental.service.xianyu;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuAfterSaleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuAfterSaleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncCursorMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderListPageParser;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductListPageParser;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuSyncCursorAdvancer;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAfterSaleAdminService;
import cn.iocoder.yudao.module.rental.service.admin.XianyuShopAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuChannelSyncServiceTest {

    private XianyuSyncCursorMapper cursorMapper;
    private XianyuAfterSaleMapper afterSaleMapper;
    private XianyuOrderMapper orderMapper;
    private XianyuProductMapper productMapper;
    private XianyuShopMapper shopMapper;
    private XianyuAfterSaleAdminService afterSaleAdminService;
    private XianyuOrderSyncService orderSyncService;
    private XianyuOrderPersistenceService orderPersistenceService;
    private XianyuProductSyncService productSyncService;
    private XianyuReadClient readClient;
    private RedissonClient redissonClient;
    private RLock syncLock;
    private XianyuChannelSyncService service;
    private XianyuProperties properties;
    private XianyuRuntimeConfigService runtimeConfigService;
    private XianyuProperties.Job job;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setAppKey("test-app");
        properties.setAppSecret("test-secret");
        properties.setTenantId(9L);
        runtimeConfigService = mock(XianyuRuntimeConfigService.class);
        when(runtimeConfigService.getCurrent()).thenReturn(properties);
        job = properties.getJob();
        job.setLookbackDays(7);
        job.setOverlapMinutes(10);
        cursorMapper = mock(XianyuSyncCursorMapper.class);
        afterSaleMapper = mock(XianyuAfterSaleMapper.class);
        orderMapper = mock(XianyuOrderMapper.class);
        productMapper = mock(XianyuProductMapper.class);
        shopMapper = mock(XianyuShopMapper.class);
        afterSaleAdminService = mock(XianyuAfterSaleAdminService.class);
        orderSyncService = mock(XianyuOrderSyncService.class);
        orderPersistenceService = mock(XianyuOrderPersistenceService.class);
        productSyncService = mock(XianyuProductSyncService.class);
        readClient = mock(XianyuReadClient.class);
        redissonClient = mock(RedissonClient.class);
        syncLock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(syncLock);
        when(syncLock.tryLock()).thenReturn(true);
        when(syncLock.isHeldByCurrentThread()).thenReturn(true);
        Clock clock = Clock.fixed(Instant.parse("2026-07-24T06:00:00Z"), ZoneOffset.UTC);
        service = new XianyuChannelSyncService(
                runtimeConfigService,
                mock(XianyuShopAdminService.class),
                afterSaleAdminService,
                shopMapper,
                afterSaleMapper,
                orderMapper,
                productMapper,
                cursorMapper,
                new XianyuSyncCursorAdvancer(),
                orderSyncService,
                orderPersistenceService,
                productSyncService,
                readClient,
                new XianyuOrderListPageParser(),
                new XianyuProductListPageParser(),
                objectMapper,
                clock,
                redissonClient);
    }

    @Test
    void syncOrdersIncremental_skipsExpiredAuthorization() {
        XianyuShopDO expired = shop();
        expired.setAuthorizationStatus("VALID");
        expired.setAuthorizationExpiresAt(LocalDateTime.of(2026, 7, 24, 13, 59));
        when(shopMapper.selectList(any())).thenReturn(List.of(expired));
        when(orderMapper.selectList(any())).thenReturn(List.of());

        String result = service.syncOrdersIncremental();

        assertTrue(result.contains("shopsSkipped=1"));
        verify(readClient, never()).execute(any(), any());
        verify(orderSyncService, never()).syncPage(any(), any(), any(), any());
        verify(syncLock).unlock();
    }

    @Test
    void syncOrdersIncremental_skipsWhenAnotherNodeHoldsTenantLock() {
        when(syncLock.tryLock()).thenReturn(false);

        String result = service.syncOrdersIncremental();

        assertEquals("skip order sync: already running", result);
        verify(shopMapper, never()).selectList(any());
        verify(syncLock, never()).unlock();
    }

    @Test
    void syncOrdersIncremental_reportsPartialFailureToJobFramework() {
        XianyuShopDO shop = shop();
        shop.setAuthorizationStatus("VALID");
        when(shopMapper.selectList(any())).thenReturn(List.of(shop));
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any()))
                .thenThrow(new IllegalStateException("remote unavailable"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                service::syncOrdersIncremental);

        assertTrue(exception.getMessage().contains("shopsFailed=1"));
        verify(syncLock).unlock();
    }

    @Test
    void shopAndOrderSync_useSeparateTenantScopedResourceLocks() {
        when(shopMapper.selectList(any())).thenReturn(List.of());

        service.syncAuthorizedShops();
        service.syncOrdersIncremental();
        service.syncProductsIncremental();
        service.syncAfterSalesIncremental();

        verify(redissonClient).getLock("camera-rental:xianyu:sync:9:shop");
        verify(redissonClient).getLock("camera-rental:xianyu:sync:9:order");
        verify(redissonClient).getLock("camera-rental:xianyu:sync:9:product");
        verify(redissonClient).getLock("camera-rental:xianyu:sync:9:after-sale");
        verify(syncLock, times(4)).unlock();
    }

    @Test
    void backfillMissingDetailJson_usesBoundedReferenceProjection() throws Exception {
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(19L)
                .shopId(7L)
                .externalOrderId("order-19")
                .build();
        when(orderMapper.selectMissingDetailRefs(500)).thenReturn(List.of(order));
        when(readClient.execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any())).thenReturn(response(0));

        int count = service.backfillMissingDetailJson(900);

        assertEquals(1, count);
        verify(orderMapper).selectMissingDetailRefs(500);
        verify(orderPersistenceService).persistOrderDetail(eq(7L), any());
    }

    @Test
    void backfillMissingDetailJson_skipsBlankExternalOrderIdWithoutBlockingValidRows() throws Exception {
        XianyuOrderDO blank = XianyuOrderDO.builder()
                .id(18L)
                .shopId(7L)
                .externalOrderId(" ")
                .build();
        XianyuOrderDO valid = XianyuOrderDO.builder()
                .id(19L)
                .shopId(7L)
                .externalOrderId("order-19")
                .build();
        when(orderMapper.selectMissingDetailRefs(10)).thenReturn(List.of(blank, valid));
        when(readClient.execute(eq(XianyuReadEndpoint.ORDER_DETAIL), any())).thenReturn(response(0));

        int count = service.backfillMissingDetailJson(10);

        assertEquals(1, count);
        ArgumentCaptor<com.fasterxml.jackson.databind.JsonNode> bodyCaptor =
                ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
        verify(readClient).execute(eq(XianyuReadEndpoint.ORDER_DETAIL), bodyCaptor.capture());
        assertEquals("order-19", bodyCaptor.getValue().get("order_no").asText());
        verify(orderPersistenceService).persistOrderDetail(eq(7L), any());
    }

    @Test
    void resolveWindowStart_usesLookbackWhenNoCursor() {
        when(cursorMapper.selectByShopIdAndResourceType(1L, "ORDER")).thenReturn(null);
        LocalDateTime end = LocalDateTime.of(2026, 7, 24, 14, 0);
        LocalDateTime start = service.resolveWindowStart(1L, end, job);
        assertEquals(end.minusDays(7), start);
    }

    @Test
    void resolveWindowStart_usesCursorMinusOverlap() {
        LocalDateTime cursorAt = LocalDateTime.of(2026, 7, 24, 12, 0);
        when(cursorMapper.selectByShopIdAndResourceType(2L, "ORDER"))
                .thenReturn(XianyuSyncCursorDO.builder().cursorUpdatedAt(cursorAt).build());
        LocalDateTime end = LocalDateTime.of(2026, 7, 24, 14, 0);
        LocalDateTime start = service.resolveWindowStart(2L, end, job);
        assertEquals(cursorAt.minusMinutes(10), start);
    }

    @Test
    void resolveWindowStart_clampsToSixMonths() {
        LocalDateTime cursorAt = LocalDateTime.of(2025, 1, 1, 0, 0);
        when(cursorMapper.selectByShopIdAndResourceType(3L, "ORDER"))
                .thenReturn(XianyuSyncCursorDO.builder().cursorUpdatedAt(cursorAt).build());
        LocalDateTime end = LocalDateTime.of(2026, 7, 24, 14, 0);
        LocalDateTime start = service.resolveWindowStart(3L, end, job);
        assertTrue(start.isAfter(end.minusMonths(6)));
        assertEquals(end.minusMonths(6).plusMinutes(1), start);
    }

    @Test
    void syncShopOrders_splitsWindowWhenCountExceedsConfiguredPageCapacity() throws Exception {
        job.setPageSize(50);
        job.setMaxPagesPerShop(1);
        LocalDateTime cursorAt = LocalDateTime.of(2026, 7, 24, 12, 10);
        when(cursorMapper.selectByShopIdAndResourceType(7L, "ORDER"))
                .thenReturn(XianyuSyncCursorDO.builder().cursorUpdatedAt(cursorAt).build());
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any()))
                .thenReturn(response(101), response(0), response(0));

        XianyuChannelSyncService.ShopOrderSyncResult result = service.syncShopOrders(shop(), job);

        assertEquals(0, result.pages());
        verify(readClient, times(3)).execute(eq(XianyuReadEndpoint.ORDERS), any());
    }

    @Test
    void syncShopOrders_advancesCursorOnceAfterEveryPageSucceeds() throws Exception {
        job.setPageSize(1);
        job.setMaxPagesPerShop(2);
        LocalDateTime cursorAt = LocalDateTime.of(2026, 7, 24, 12, 10);
        when(cursorMapper.selectByShopIdAndResourceType(7L, "ORDER"))
                .thenReturn(XianyuSyncCursorDO.builder().cursorUpdatedAt(cursorAt).build());
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response(2));
        when(orderSyncService.syncPage(eq(7L), eq(88L), any(), eq(XianyuOrderSyncService.TRIGGER_SCHEDULED)))
                .thenReturn(new XianyuOrderPageSyncResult(1L, 1, 1, false),
                        new XianyuOrderPageSyncResult(2L, 1, 1, false));
        LocalDateTime newestAt = LocalDateTime.of(2026, 7, 24, 13, 30);
        when(orderMapper.selectNewestCursorCandidate(eq(7L), any(), any()))
                .thenReturn(XianyuOrderDO.builder()
                        .externalOrderId("order-2")
                        .sourceUpdatedAt(newestAt)
                        .build());

        XianyuChannelSyncService.ShopOrderSyncResult result = service.syncShopOrders(shop(), job);

        assertEquals(2, result.pages());
        var ordered = inOrder(orderSyncService, orderMapper, orderPersistenceService);
        ordered.verify(orderSyncService, times(2))
                .syncPage(eq(7L), eq(88L), any(), eq(XianyuOrderSyncService.TRIGGER_SCHEDULED));
        ordered.verify(orderMapper).selectNewestCursorCandidate(eq(7L), any(), any());
        ordered.verify(orderPersistenceService)
                .advanceOrderCursor(eq(7L), eq(newestAt), eq("order-2"), any());
    }

    @Test
    void syncShopOrders_doesNotAdvanceCursorWhenFixedWindowCountChanges() throws Exception {
        job.setPageSize(50);
        job.setMaxPagesPerShop(1);
        when(cursorMapper.selectByShopIdAndResourceType(7L, "ORDER"))
                .thenReturn(XianyuSyncCursorDO.builder()
                        .cursorUpdatedAt(LocalDateTime.of(2026, 7, 24, 12, 10))
                        .build());
        when(readClient.execute(eq(XianyuReadEndpoint.ORDERS), any())).thenReturn(response(2));
        when(orderSyncService.syncPage(eq(7L), eq(88L), any(), eq(XianyuOrderSyncService.TRIGGER_SCHEDULED)))
                .thenReturn(new XianyuOrderPageSyncResult(1L, 1, 1, false));

        assertThrows(IllegalStateException.class, () -> service.syncShopOrders(shop(), job));

        verify(orderMapper, never()).selectNewestCursorCandidate(any(), any(), any());
        verify(orderPersistenceService, never()).advanceOrderCursor(any(), any(), any(), any());
    }

    @Test
    void syncProductsIncremental_advancesProductCursorOnceAfterEveryPageSucceeds() throws Exception {
        job.setPageSize(1);
        job.setMaxPagesPerShop(2);
        XianyuShopDO shop = shop();
        shop.setExternalShopId("123456");
        when(shopMapper.selectList(any())).thenReturn(List.of(shop));
        when(cursorMapper.selectByShopIdAndResourceType(7L, "PRODUCT"))
                .thenReturn(XianyuSyncCursorDO.builder()
                        .cursorUpdatedAt(LocalDateTime.of(2026, 7, 24, 12, 10))
                        .build());
        when(readClient.execute(eq(XianyuReadEndpoint.PRODUCTS), any()))
                .thenReturn(response(2));
        when(productSyncService.syncPage(eq(7L), org.mockito.Mockito.isNull(), any(),
                eq(XianyuProductSyncService.TRIGGER_SCHEDULED)))
                .thenReturn(new XianyuProductPageSyncResult(1L, 1, 1, 0, 1),
                        new XianyuProductPageSyncResult(2L, 1, 1, 1, 0));
        LocalDateTime newestAt = LocalDateTime.of(2026, 7, 24, 13, 30);
        when(productMapper.selectNewestCursorCandidate(eq(7L), any(), any()))
                .thenReturn(XianyuProductDO.builder()
                        .externalProductId("448592974859526")
                        .sourceUpdatedAt(newestAt)
                        .build());

        String result = service.syncProductsIncremental();

        assertTrue(result.contains("received=2"));
        assertTrue(result.contains("skus=1"));
        var ordered = inOrder(productSyncService, productMapper);
        ordered.verify(productSyncService, times(2))
                .syncPage(eq(7L), org.mockito.Mockito.isNull(), any(), eq(XianyuProductSyncService.TRIGGER_SCHEDULED));
        ordered.verify(productMapper).selectNewestCursorCandidate(eq(7L), any(), any());
        ordered.verify(productSyncService)
                .advanceProductCursor(eq(7L), eq(newestAt), eq("448592974859526"), any());
    }

    @Test
    void syncShopProducts_doesNotAdvanceCursorWhenFixedWindowCountChanges() throws Exception {
        job.setPageSize(50);
        job.setMaxPagesPerShop(1);
        XianyuShopDO shop = shop();
        shop.setExternalShopId("123456");
        when(cursorMapper.selectByShopIdAndResourceType(7L, "PRODUCT"))
                .thenReturn(XianyuSyncCursorDO.builder()
                        .cursorUpdatedAt(LocalDateTime.of(2026, 7, 24, 12, 10))
                        .build());
        when(readClient.execute(eq(XianyuReadEndpoint.PRODUCTS), any()))
                .thenReturn(response(2));
        when(productSyncService.syncPage(eq(7L), org.mockito.Mockito.isNull(), any(),
                eq(XianyuProductSyncService.TRIGGER_SCHEDULED)))
                .thenReturn(new XianyuProductPageSyncResult(1L, 1, 1, 0, 0));

        assertThrows(IllegalStateException.class, () -> service.syncShopProducts(shop, job));

        verify(productMapper, never()).selectNewestCursorCandidate(any(), any(), any());
        verify(productSyncService, never()).advanceProductCursor(any(), any(), any(), any());
    }

    @Test
    void syncAfterSalesIncremental_runsApplyAndRefundWindowsThenAdvancesCursor() {
        job.setPageSize(10);
        job.setMaxPagesPerShop(5);
        XianyuShopDO shop = shop();
        shop.setAuthorizationStatus("VALID");
        when(shopMapper.selectList(any())).thenReturn(List.of(shop));
        when(cursorMapper.selectByShopIdAndResourceType(7L, "AFTER_SALE"))
                .thenReturn(XianyuSyncCursorDO.builder()
                        .cursorUpdatedAt(LocalDateTime.of(2026, 7, 24, 12, 10))
                        .build());
        when(afterSaleAdminService.syncPage(any(), eq(XianyuAfterSaleAdminService.TRIGGER_SCHEDULED)))
                .thenReturn(afterSaleResp(1, 1, false), afterSaleResp(2, 2, false));
        LocalDateTime newestAt = LocalDateTime.of(2026, 7, 24, 13, 20);
        when(afterSaleMapper.selectNewestCursorCandidate(eq(7L), any(), any()))
                .thenReturn(XianyuAfterSaleDO.builder()
                        .externalAfterSaleId("refund-2")
                        .sourceUpdatedAt(newestAt)
                        .build());

        String result = service.syncAfterSalesIncremental();

        assertTrue(result.contains("received=3"));
        ArgumentCaptor<XianyuAfterSaleSyncReqVO> reqCaptor =
                ArgumentCaptor.forClass(XianyuAfterSaleSyncReqVO.class);
        verify(afterSaleAdminService, times(2))
                .syncPage(reqCaptor.capture(), eq(XianyuAfterSaleAdminService.TRIGGER_SCHEDULED));
        assertEquals(LocalDateTime.of(2026, 7, 24, 12, 0), reqCaptor.getAllValues().get(0).getApplyStart());
        assertEquals(LocalDateTime.of(2026, 7, 24, 12, 0), reqCaptor.getAllValues().get(1).getRefundStart());
        verify(cursorMapper).insert(any(XianyuSyncCursorDO.class));
    }

    @Test
    void syncShopAfterSales_doesNotAdvanceCursorWhenWindowExceedsPageCapacity() {
        job.setPageSize(1);
        job.setMaxPagesPerShop(1);
        when(cursorMapper.selectByShopIdAndResourceType(7L, "AFTER_SALE"))
                .thenReturn(XianyuSyncCursorDO.builder()
                        .cursorUpdatedAt(LocalDateTime.of(2026, 7, 24, 12, 10))
                        .build());
        when(afterSaleAdminService.syncPage(any(), eq(XianyuAfterSaleAdminService.TRIGGER_SCHEDULED)))
                .thenReturn(afterSaleResp(1, 1, true));

        assertThrows(IllegalStateException.class, () -> service.syncShopAfterSales(shop(), job));

        verify(afterSaleMapper, never()).selectNewestCursorCandidate(any(), any(), any());
        verify(cursorMapper, never()).selectByShopIdAndResourceTypeForUpdate(any(), any());
    }

    private XianyuShopDO shop() {
        return XianyuShopDO.builder().id(7L).authorizeId("88").shopName("test").build();
    }

    private XianyuAfterSaleSyncRespVO afterSaleResp(int received, int succeeded, boolean hasNextPage) {
        XianyuAfterSaleSyncRespVO respVO = new XianyuAfterSaleSyncRespVO();
        respVO.setSyncRunId(99L);
        respVO.setReceivedCount(received);
        respVO.setSucceededCount(succeeded);
        respVO.setHasNextPage(hasNextPage);
        return respVO;
    }

    private XianyuReadResponse response(int count) throws Exception {
        String json = "{\"code\":0,\"data\":{\"count\":" + count
                + ",\"page_no\":1,\"page_size\":1,\"list\":[]}}";
        JsonNode payload = objectMapper.readTree(json);
        return new XianyuReadResponse(200, 0, payload, json);
    }

}
