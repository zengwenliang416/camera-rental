package cn.iocoder.yudao.module.rental.service.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
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
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderListPage;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderListPageParser;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncWindow;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductListPage;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductListPageParser;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuProductSyncWindow;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuSyncCursorAdvancer;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAfterSaleAdminService;
import cn.iocoder.yudao.module.rental.service.admin.XianyuShopAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Scheduled (and reusable) channel sync orchestration: shops, orders, and products.
 */
@Service
@Slf4j
public class XianyuChannelSyncService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String ORDER_RESOURCE = "ORDER";
    private static final String PRODUCT_RESOURCE = "PRODUCT";
    private static final String AFTER_SALE_RESOURCE = "AFTER_SALE";
    private final XianyuRuntimeConfigService runtimeConfigService;
    private final XianyuShopAdminService shopAdminService;
    private final XianyuAfterSaleAdminService afterSaleAdminService;
    private final XianyuShopMapper shopMapper;
    private final XianyuAfterSaleMapper afterSaleMapper;
    private final XianyuOrderMapper orderMapper;
    private final XianyuProductMapper productMapper;
    private final XianyuSyncCursorMapper cursorMapper;
    private final XianyuSyncCursorAdvancer cursorAdvancer;
    private final XianyuOrderSyncService orderSyncService;
    private final XianyuOrderPersistenceService orderPersistenceService;
    private final XianyuProductSyncService productSyncService;
    private final XianyuReadClient readClient;
    private final XianyuOrderListPageParser listPageParser;
    private final XianyuProductListPageParser productListPageParser;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final RedissonClient redissonClient;

    public XianyuChannelSyncService(XianyuRuntimeConfigService runtimeConfigService,
                                    XianyuShopAdminService shopAdminService,
                                    XianyuAfterSaleAdminService afterSaleAdminService,
                                    XianyuShopMapper shopMapper,
                                    XianyuAfterSaleMapper afterSaleMapper,
                                    XianyuOrderMapper orderMapper,
                                    XianyuProductMapper productMapper,
                                    XianyuSyncCursorMapper cursorMapper,
                                    XianyuSyncCursorAdvancer cursorAdvancer,
                                    XianyuOrderSyncService orderSyncService,
                                    XianyuOrderPersistenceService orderPersistenceService,
                                    XianyuProductSyncService productSyncService,
                                    XianyuReadClient readClient,
                                    XianyuOrderListPageParser listPageParser,
                                    XianyuProductListPageParser productListPageParser,
                                    ObjectMapper objectMapper,
                                    @org.springframework.beans.factory.annotation.Qualifier("xianyuClock") Clock clock,
                                    RedissonClient redissonClient) {
        this.runtimeConfigService = runtimeConfigService;
        this.shopAdminService = shopAdminService;
        this.afterSaleAdminService = afterSaleAdminService;
        this.shopMapper = shopMapper;
        this.afterSaleMapper = afterSaleMapper;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.cursorMapper = cursorMapper;
        this.cursorAdvancer = cursorAdvancer;
        this.orderSyncService = orderSyncService;
        this.orderPersistenceService = orderPersistenceService;
        this.productSyncService = productSyncService;
        this.readClient = readClient;
        this.listPageParser = listPageParser;
        this.productListPageParser = productListPageParser;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.redissonClient = redissonClient;
    }

    /**
     * @return human-readable summary for JobHandler / logs
     */
    public String syncAuthorizedShops() {
        XianyuProperties properties = runtimeConfigService.getCurrent();
        if (properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return "skip shop sync: integration not READY (" + properties.getIntegrationStatus() + ")";
        }
        return withTenantLock(properties.requireTenantId(), "shop", () -> {
            int count = shopAdminService.syncAuthorizedShops();
            log.info("[xianyu][job] authorized shops upserted={}", count);
            return "shops upserted=" + count;
        });
    }

    /**
     * Incremental order sync for all VALID shops with authorizeId.
     */
    public String syncOrdersIncremental() {
        XianyuProperties properties = runtimeConfigService.getCurrent();
        if (properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return "skip order sync: integration not READY (" + properties.getIntegrationStatus() + ")";
        }
        return withTenantLock(properties.requireTenantId(), XianyuSyncLockKey.ORDER_RESOURCE,
                () -> doSyncOrdersIncremental(properties.getJob()));
    }

    /**
     * Incremental product sync for all VALID shops. Uses read-only product list/detail/SKU APIs only.
     */
    public String syncProductsIncremental() {
        XianyuProperties properties = runtimeConfigService.getCurrent();
        if (properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return "skip product sync: integration not READY (" + properties.getIntegrationStatus() + ")";
        }
        return withTenantLock(properties.requireTenantId(), "product",
                () -> doSyncProductsIncremental(properties.getJob()));
    }

    /**
     * Incremental after-sale sync for all VALID shops with authorizeId. Uses read-only list/detail APIs only.
     */
    public String syncAfterSalesIncremental() {
        XianyuProperties properties = runtimeConfigService.getCurrent();
        if (properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return "skip after-sale sync: integration not READY (" + properties.getIntegrationStatus() + ")";
        }
        return withTenantLock(properties.requireTenantId(), "after-sale",
                () -> doSyncAfterSalesIncremental(properties.getJob()));
    }

    private String doSyncOrdersIncremental(XianyuProperties.Job job) {
        int rentalPeriodBackfilled = orderPersistenceService.backfillMissingRentalPeriods(
                job.getPageSize() * Math.max(1, job.getMaxPagesPerShop()));
        List<XianyuShopDO> shops = shopMapper.selectList(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getAuthorizationStatus, "VALID")
                .isNotNull(XianyuShopDO::getAuthorizeId));
        if (shops.isEmpty()) {
            return "skip order sync: no VALID shops (run shop sync first), rentalPeriodBackfill="
                    + rentalPeriodBackfilled;
        }

        int totalReceived = 0;
        int totalSucceeded = 0;
        int shopsOk = 0;
        int shopsFailed = 0;
        int shopsSkipped = 0;
        List<String> errors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock.withZone(BUSINESS_ZONE));

        for (XianyuShopDO shop : shops) {
            if (!StringUtils.hasText(shop.getAuthorizeId())) {
                continue;
            }
            if (shop.getAuthorizationExpiresAt() != null
                    && !shop.getAuthorizationExpiresAt().isAfter(now)) {
                shopsSkipped++;
                log.warn("[xianyu][job] skip expired authorization shopId={}", shop.getId());
                continue;
            }
            try {
                ShopOrderSyncResult result = syncShopOrders(shop, job);
                totalReceived += result.received();
                totalSucceeded += result.succeeded();
                shopsOk++;
                log.info("[xianyu][job] shopId={} pages={} received={} succeeded={}",
                        shop.getId(), result.pages(), result.received(), result.succeeded());
            } catch (Exception ex) {
                shopsFailed++;
                String errorCode = XianyuSafeErrorCode.from(ex);
                String msg = "shopId=" + shop.getId() + " code=" + errorCode;
                errors.add(msg);
                log.warn("[xianyu][job] order sync failed for shopId={} code={}", shop.getId(), errorCode);
            }
        }

        int backfilled = backfillMissingDetailJson(job.getPageSize() * Math.max(1, job.getMaxPagesPerShop()));

        StringBuilder sb = new StringBuilder();
        sb.append("shopsOk=").append(shopsOk)
                .append(" shopsFailed=").append(shopsFailed)
                .append(" shopsSkipped=").append(shopsSkipped)
                .append(" received=").append(totalReceived)
                .append(" succeeded=").append(totalSucceeded)
                .append(" detailBackfill=").append(backfilled)
                .append(" rentalPeriodBackfill=").append(rentalPeriodBackfilled);
        if (!errors.isEmpty()) {
            sb.append(" errors=").append(String.join("; ", errors.subList(0, Math.min(3, errors.size()))));
        }
        String summary = sb.toString();
        if (shopsFailed > 0) {
            throw new IllegalStateException("XianGuanJia order sync partially failed: " + summary);
        }
        return summary;
    }

    private String doSyncProductsIncremental(XianyuProperties.Job job) {
        List<XianyuShopDO> shops = shopMapper.selectList(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getAuthorizationStatus, "VALID"));
        if (shops.isEmpty()) {
            return "skip product sync: no VALID shops (run shop sync first)";
        }

        int totalReceived = 0;
        int totalSucceeded = 0;
        int totalDeduplicated = 0;
        int totalSkus = 0;
        int shopsOk = 0;
        int shopsFailed = 0;
        int shopsSkipped = 0;
        List<String> errors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock.withZone(BUSINESS_ZONE));

        for (XianyuShopDO shop : shops) {
            if (shop.getAuthorizationExpiresAt() != null
                    && !shop.getAuthorizationExpiresAt().isAfter(now)) {
                shopsSkipped++;
                log.warn("[xianyu][job] skip expired authorization shopId={}", shop.getId());
                continue;
            }
            try {
                ShopProductSyncResult result = syncShopProducts(shop, job);
                totalReceived += result.received();
                totalSucceeded += result.succeeded();
                totalDeduplicated += result.deduplicated();
                totalSkus += result.skus();
                shopsOk++;
                log.info("[xianyu][job] product shopId={} pages={} received={} succeeded={} skus={}",
                        shop.getId(), result.pages(), result.received(), result.succeeded(), result.skus());
            } catch (Exception ex) {
                shopsFailed++;
                String errorCode = XianyuSafeErrorCode.from(ex);
                String msg = "shopId=" + shop.getId() + " code=" + errorCode;
                errors.add(msg);
                log.warn("[xianyu][job] product sync failed for shopId={} code={}", shop.getId(), errorCode);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("shopsOk=").append(shopsOk)
                .append(" shopsFailed=").append(shopsFailed)
                .append(" shopsSkipped=").append(shopsSkipped)
                .append(" received=").append(totalReceived)
                .append(" succeeded=").append(totalSucceeded)
                .append(" deduplicated=").append(totalDeduplicated)
                .append(" skus=").append(totalSkus);
        if (!errors.isEmpty()) {
            sb.append(" errors=").append(String.join("; ", errors.subList(0, Math.min(3, errors.size()))));
        }
        String summary = sb.toString();
        if (shopsFailed > 0) {
            throw new IllegalStateException("XianGuanJia product sync partially failed: " + summary);
        }
        return summary;
    }

    private String doSyncAfterSalesIncremental(XianyuProperties.Job job) {
        List<XianyuShopDO> shops = shopMapper.selectList(new LambdaQueryWrapperX<XianyuShopDO>()
                .eq(XianyuShopDO::getAuthorizationStatus, "VALID")
                .isNotNull(XianyuShopDO::getAuthorizeId));
        if (shops.isEmpty()) {
            return "skip after-sale sync: no VALID shops (run shop sync first)";
        }

        int totalReceived = 0;
        int totalSucceeded = 0;
        int shopsOk = 0;
        int shopsFailed = 0;
        int shopsSkipped = 0;
        List<String> errors = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock.withZone(BUSINESS_ZONE));

        for (XianyuShopDO shop : shops) {
            if (!StringUtils.hasText(shop.getAuthorizeId())) {
                continue;
            }
            if (shop.getAuthorizationExpiresAt() != null
                    && !shop.getAuthorizationExpiresAt().isAfter(now)) {
                shopsSkipped++;
                log.warn("[xianyu][job] skip expired authorization shopId={}", shop.getId());
                continue;
            }
            try {
                ShopAfterSaleSyncResult result = syncShopAfterSales(shop, job);
                totalReceived += result.received();
                totalSucceeded += result.succeeded();
                shopsOk++;
                log.info("[xianyu][job] after-sale shopId={} pages={} received={} succeeded={}",
                        shop.getId(), result.pages(), result.received(), result.succeeded());
            } catch (Exception ex) {
                shopsFailed++;
                String errorCode = XianyuSafeErrorCode.from(ex);
                String msg = "shopId=" + shop.getId() + " code=" + errorCode;
                errors.add(msg);
                log.warn("[xianyu][job] after-sale sync failed for shopId={} code={}", shop.getId(), errorCode);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("shopsOk=").append(shopsOk)
                .append(" shopsFailed=").append(shopsFailed)
                .append(" shopsSkipped=").append(shopsSkipped)
                .append(" received=").append(totalReceived)
                .append(" succeeded=").append(totalSucceeded);
        if (!errors.isEmpty()) {
            sb.append(" errors=").append(String.join("; ", errors.subList(0, Math.min(3, errors.size()))));
        }
        String summary = sb.toString();
        if (shopsFailed > 0) {
            throw new IllegalStateException("XianGuanJia after-sale sync partially failed: " + summary);
        }
        return summary;
    }

    /**
     * Re-fetch order-detail for rows missing full {@code detail_json} (legacy / partial imports).
     */
    int backfillMissingDetailJson(int limit) {
        int max = Math.max(1, Math.min(500, limit));
        List<XianyuOrderDO> missing = orderMapper.selectMissingDetailRefs(max);
        int ok = 0;
        for (XianyuOrderDO order : missing) {
            if (!StringUtils.hasText(order.getExternalOrderId())) {
                log.warn("[xianyu][job] skip detail backfill because external order id is blank orderId={} shopId={}",
                        order.getId(), order.getShopId());
                continue;
            }
            try {
                String raw = readClient.execute(XianyuReadEndpoint.ORDER_DETAIL,
                        objectMapper.createObjectNode().put("order_no", order.getExternalOrderId())).rawBody();
                orderPersistenceService.persistOrderDetail(order.getShopId(), raw);
                ok++;
            } catch (Exception ex) {
                log.warn("[xianyu][job] detail backfill failed orderId={} shopId={} code={}",
                        order.getId(), order.getShopId(), XianyuSafeErrorCode.from(ex));
            }
        }
        if (ok > 0) {
            log.info("[xianyu][job] detail_json backfilled={}", ok);
        }
        return ok;
    }

    private String withTenantLock(Long tenantId, String summaryResource, Supplier<String> action) {
        String lockKey = XianyuSyncLockKey.forResource(tenantId, summaryResource);
        RLock lock = redissonClient.getLock(lockKey);
        if (!lock.tryLock()) {
            log.info("[xianyu][job] skip {} sync because another execution holds lock={}",
                    summaryResource, lockKey);
            return "skip " + summaryResource + " sync: already running";
        }
        try {
            return action.get();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    ShopOrderSyncResult syncShopOrders(XianyuShopDO shop, XianyuProperties.Job job) {
        LocalDateTime end = LocalDateTime.now(clock.withZone(BUSINESS_ZONE));
        LocalDateTime start = resolveWindowStart(shop.getId(), end, job);
        if (!start.isBefore(end)) {
            return new ShopOrderSyncResult(0, 0, 0);
        }

        long authorizeId = Long.parseLong(shop.getAuthorizeId().trim());
        int pageSize = Math.max(1, Math.min(100, job.getPageSize()));
        int maxPages = Math.max(1, Math.min(100, job.getMaxPagesPerShop()));
        return syncWindow(shop.getId(), authorizeId, start, end, pageSize, maxPages);
    }

    ShopProductSyncResult syncShopProducts(XianyuShopDO shop, XianyuProperties.Job job) {
        LocalDateTime end = LocalDateTime.now(clock.withZone(BUSINESS_ZONE));
        LocalDateTime start = resolveWindowStart(shop.getId(), end, job, PRODUCT_RESOURCE);
        if (!start.isBefore(end)) {
            return new ShopProductSyncResult(0, 0, 0, 0, 0);
        }

        int pageSize = Math.max(1, Math.min(100, job.getPageSize()));
        int maxPages = Math.max(1, Math.min(100, job.getMaxPagesPerShop()));
        return syncProductWindow(shop, start, end, pageSize, maxPages);
    }

    ShopAfterSaleSyncResult syncShopAfterSales(XianyuShopDO shop, XianyuProperties.Job job) {
        LocalDateTime end = LocalDateTime.now(clock.withZone(BUSINESS_ZONE));
        LocalDateTime start = resolveWindowStart(shop.getId(), end, job, AFTER_SALE_RESOURCE);
        if (!start.isBefore(end)) {
            return new ShopAfterSaleSyncResult(0, 0, 0);
        }

        int pageSize = Math.max(1, Math.min(100, job.getPageSize()));
        int maxPages = Math.max(1, Math.min(100, job.getMaxPagesPerShop()));
        ShopAfterSaleSyncResult applyResult = syncAfterSaleWindow(shop, start, end, pageSize, maxPages, true);
        ShopAfterSaleSyncResult refundResult = syncAfterSaleWindow(shop, start, end, pageSize, maxPages, false);
        ShopAfterSaleSyncResult result = applyResult.plus(refundResult);
        XianyuAfterSaleDO newest = afterSaleMapper.selectNewestCursorCandidate(shop.getId(), start, end);
        if (newest != null && newest.getSourceUpdatedAt() != null
                && StringUtils.hasText(newest.getExternalAfterSaleId())) {
            advanceAfterSaleCursor(shop.getId(), newest.getSourceUpdatedAt(), newest.getExternalAfterSaleId(), end);
        }
        return result;
    }

    private ShopOrderSyncResult syncWindow(Long shopId, long authorizeId, LocalDateTime start,
                                           LocalDateTime end, int pageSize, int maxPages) {
        int count = countWindow(authorizeId, start, end);
        if (count == 0) {
            return new ShopOrderSyncResult(0, 0, 0);
        }
        long capacity = Math.min(10_000L, (long) pageSize * maxPages);
        if (count > capacity) {
            LocalDateTime midpoint = splitMidpoint(start, end);
            ShopOrderSyncResult left = syncWindow(shopId, authorizeId, start, midpoint, pageSize, maxPages);
            ShopOrderSyncResult right = syncWindow(shopId, authorizeId, midpoint, end, pageSize, maxPages);
            return left.plus(right);
        }

        int expectedPages = (count + pageSize - 1) / pageSize;
        int pages = 0;
        int received = 0;
        int succeeded = 0;

        for (int pageNo = 1; pageNo <= expectedPages; pageNo++) {
            XianyuOrderSyncWindow window = new XianyuOrderSyncWindow(start, end, pageNo, pageSize);
            XianyuOrderPageSyncResult pageResult = orderSyncService.syncPage(
                    shopId, authorizeId, window, XianyuOrderSyncService.TRIGGER_SCHEDULED);
            int expectedRows = Math.min(pageSize, count - (pageNo - 1) * pageSize);
            if (pageResult.receivedCount() != expectedRows || pageResult.succeededCount() != expectedRows) {
                throw new IllegalStateException("XianGuanJia order-list count changed during fixed window");
            }
            pages++;
            received += pageResult.receivedCount();
            succeeded += pageResult.succeededCount();
        }
        XianyuOrderDO newest = orderMapper.selectNewestCursorCandidate(shopId, start, end);
        if (newest != null && newest.getSourceUpdatedAt() != null
                && StringUtils.hasText(newest.getExternalOrderId())) {
            orderPersistenceService.advanceOrderCursor(shopId, newest.getSourceUpdatedAt(),
                    newest.getExternalOrderId(), end);
        }
        return new ShopOrderSyncResult(pages, received, succeeded);
    }

    private ShopProductSyncResult syncProductWindow(XianyuShopDO shop, LocalDateTime start,
                                                    LocalDateTime end, int pageSize, int maxPages) {
        int count = countProductWindow(start, end);
        if (count == 0) {
            return new ShopProductSyncResult(0, 0, 0, 0, 0);
        }
        long capacity = Math.min(10_000L, (long) pageSize * maxPages);
        if (count > capacity) {
            LocalDateTime midpoint = splitMidpoint(start, end);
            ShopProductSyncResult left = syncProductWindow(shop, start, midpoint, pageSize, maxPages);
            ShopProductSyncResult right = syncProductWindow(shop, midpoint, end, pageSize, maxPages);
            return left.plus(right);
        }

        int expectedPages = (count + pageSize - 1) / pageSize;
        int pages = 0;
        int received = 0;
        int succeeded = 0;
        int deduplicated = 0;
        int skus = 0;

        for (int pageNo = 1; pageNo <= expectedPages; pageNo++) {
            XianyuProductSyncWindow window = new XianyuProductSyncWindow(start, end, pageNo, pageSize);
            XianyuProductPageSyncResult pageResult = productSyncService.syncPage(
                    shop.getId(), null, window, XianyuProductSyncService.TRIGGER_SCHEDULED);
            int expectedRows = Math.min(pageSize, count - (pageNo - 1) * pageSize);
            if (pageResult.receivedCount() != expectedRows || pageResult.succeededCount() != expectedRows) {
                throw new IllegalStateException("XianGuanJia product-list count changed during fixed window");
            }
            pages++;
            received += pageResult.receivedCount();
            succeeded += pageResult.succeededCount();
            deduplicated += pageResult.deduplicatedCount();
            skus += pageResult.skuCount();
        }
        XianyuProductDO newest = productMapper.selectNewestCursorCandidate(shop.getId(), start, end);
        if (newest != null && newest.getSourceUpdatedAt() != null
                && StringUtils.hasText(newest.getExternalProductId())) {
            productSyncService.advanceProductCursor(shop.getId(), newest.getSourceUpdatedAt(),
                    newest.getExternalProductId(), end);
        }
        return new ShopProductSyncResult(pages, received, succeeded, deduplicated, skus);
    }

    private ShopAfterSaleSyncResult syncAfterSaleWindow(XianyuShopDO shop, LocalDateTime start, LocalDateTime end,
                                                        int pageSize, int maxPages, boolean applyTimeWindow) {
        int pages = 0;
        int received = 0;
        int succeeded = 0;
        boolean hasNext = false;
        for (int pageNo = 1; pageNo <= maxPages; pageNo++) {
            XianyuAfterSaleSyncReqVO reqVO = buildAfterSaleSyncRequest(
                    shop.getId(), start, end, pageNo, pageSize, applyTimeWindow);
            XianyuAfterSaleSyncRespVO pageResult = afterSaleAdminService.syncPage(
                    reqVO, XianyuAfterSaleAdminService.TRIGGER_SCHEDULED);
            pages++;
            received += pageResult.getReceivedCount();
            succeeded += pageResult.getSucceededCount();
            hasNext = Boolean.TRUE.equals(pageResult.getHasNextPage());
            if (!hasNext) {
                break;
            }
        }
        if (hasNext) {
            throw new IllegalStateException("XianGuanJia after-sale list exceeds configured page capacity");
        }
        return new ShopAfterSaleSyncResult(pages, received, succeeded);
    }

    private XianyuAfterSaleSyncReqVO buildAfterSaleSyncRequest(Long shopId, LocalDateTime start, LocalDateTime end,
                                                               int pageNo, int pageSize, boolean applyTimeWindow) {
        XianyuAfterSaleSyncReqVO reqVO = new XianyuAfterSaleSyncReqVO();
        reqVO.setShopId(shopId);
        if (applyTimeWindow) {
            reqVO.setApplyStart(start);
            reqVO.setApplyEnd(end);
        } else {
            reqVO.setRefundStart(start);
            reqVO.setRefundEnd(end);
        }
        reqVO.setPageNo(pageNo);
        reqVO.setPageSize(pageSize);
        return reqVO;
    }

    private int countWindow(long authorizeId, LocalDateTime start, LocalDateTime end) {
        XianyuOrderSyncWindow probe = new XianyuOrderSyncWindow(start, end, 1, 1);
        XianyuOrderListPage page = listPageParser.parse(readClient.execute(
                XianyuReadEndpoint.ORDERS, probe.toRequestBody(objectMapper, authorizeId)));
        return page.count();
    }

    private int countProductWindow(LocalDateTime start, LocalDateTime end) {
        XianyuProductSyncWindow probe = new XianyuProductSyncWindow(start, end, 1, 1);
        XianyuProductListPage page = productListPageParser.parse(readClient.execute(
                XianyuReadEndpoint.PRODUCTS, probe.toRequestBody(objectMapper)));
        return page.count();
    }

    private boolean advanceAfterSaleCursor(Long shopId, LocalDateTime sourceUpdatedAt, String externalAfterSaleId,
                                           LocalDateTime safeUpperBound) {
        XianyuSyncCursorDO current = cursorMapper.selectByShopIdAndResourceTypeForUpdate(shopId, AFTER_SALE_RESOURCE);
        if (!cursorAdvancer.isStrictlyNewer(current, sourceUpdatedAt, externalAfterSaleId)) {
            return false;
        }
        XianyuSyncCursorDO cursor = XianyuSyncCursorDO.builder()
                .id(current == null ? null : current.getId())
                .shopId(shopId)
                .resourceType(AFTER_SALE_RESOURCE)
                .cursorUpdatedAt(sourceUpdatedAt)
                .cursorExternalId(externalAfterSaleId)
                .safeUpperBound(safeUpperBound)
                .build();
        if (current == null) {
            cursor.setCreator("system");
            cursor.setUpdater("system");
            cursorMapper.insert(cursor);
        } else {
            cursor.setUpdater("system");
            cursorMapper.updateById(cursor);
        }
        return true;
    }

    private LocalDateTime splitMidpoint(LocalDateTime start, LocalDateTime end) {
        long seconds = Duration.between(start, end).getSeconds();
        if (seconds <= 1) {
            throw new IllegalStateException("XianGuanJia order-list exceeds capacity within one second");
        }
        return start.plusSeconds(seconds / 2);
    }

    LocalDateTime resolveWindowStart(Long shopId, LocalDateTime end, XianyuProperties.Job job) {
        return resolveWindowStart(shopId, end, job, ORDER_RESOURCE);
    }

    private LocalDateTime resolveWindowStart(Long shopId, LocalDateTime end, XianyuProperties.Job job,
                                             String resourceType) {
        XianyuSyncCursorDO cursor = cursorMapper.selectByShopIdAndResourceType(shopId, resourceType);
        if (cursor != null && cursor.getCursorUpdatedAt() != null) {
            LocalDateTime fromCursor = cursor.getCursorUpdatedAt().minusMinutes(Math.max(0, job.getOverlapMinutes()));
            LocalDateTime minStart = end.minusMonths(6).plusMinutes(1);
            return fromCursor.isBefore(minStart) ? minStart : fromCursor;
        }
        int lookbackDays = Math.max(1, Math.min(180, job.getLookbackDays()));
        return end.minusDays(lookbackDays);
    }

    record ShopOrderSyncResult(int pages, int received, int succeeded) {

        private ShopOrderSyncResult plus(ShopOrderSyncResult other) {
            return new ShopOrderSyncResult(pages + other.pages, received + other.received,
                    succeeded + other.succeeded);
        }

    }

    record ShopProductSyncResult(int pages, int received, int succeeded, int deduplicated, int skus) {

        private ShopProductSyncResult plus(ShopProductSyncResult other) {
            return new ShopProductSyncResult(pages + other.pages, received + other.received,
                    succeeded + other.succeeded, deduplicated + other.deduplicated, skus + other.skus);
        }

    }

    record ShopAfterSaleSyncResult(int pages, int received, int succeeded) {

        private ShopAfterSaleSyncResult plus(ShopAfterSaleSyncResult other) {
            return new ShopAfterSaleSyncResult(pages + other.pages, received + other.received,
                    succeeded + other.succeeded);
        }

    }

}
