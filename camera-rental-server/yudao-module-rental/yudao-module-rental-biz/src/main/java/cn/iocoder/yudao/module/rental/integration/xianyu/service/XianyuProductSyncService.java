package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
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
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAlertAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class XianyuProductSyncService {

    private static final String RESOURCE_TYPE = "PRODUCT";
    public static final String PRODUCT_PAGE_SOURCE_TYPE = "PRODUCT_PAGE";
    private static final String PRODUCT_PAGE_SCHEMA_VERSION = "XIAN_GUAN_JIA_PRODUCT_PAGE_V1";
    private static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";
    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    private static final int MAX_WINDOW_ROWS = 10_000;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuReadClient readClient;
    private final XianyuProductListPageParser listPageParser;
    private final XianyuProductPersistenceService productPersistenceService;
    private final XianyuProductSkuPersistenceService skuPersistenceService;
    private final XianyuProductMapper productMapper;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuSyncCursorMapper cursorMapper;
    private final XianyuSyncCursorAdvancer cursorAdvancer;
    private final XianyuSyncRunMapper syncRunMapper;
    private final XianyuAlertAdminService alertAdminService;
    private final XianyuPayloadHasher payloadHasher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public XianyuProductSyncService(XianyuReadClient readClient,
                                    XianyuProductListPageParser listPageParser,
                                    XianyuProductPersistenceService productPersistenceService,
                                    XianyuProductSkuPersistenceService skuPersistenceService,
                                    XianyuProductMapper productMapper,
                                    XianyuRawPayloadMapper rawPayloadMapper,
                                    XianyuSyncCursorMapper cursorMapper,
                                    XianyuSyncCursorAdvancer cursorAdvancer,
                                    XianyuSyncRunMapper syncRunMapper,
                                    XianyuAlertAdminService alertAdminService,
                                    XianyuPayloadHasher payloadHasher,
                                    ObjectMapper objectMapper,
                                    @Qualifier("xianyuClock") Clock clock) {
        this.readClient = readClient;
        this.listPageParser = listPageParser;
        this.productPersistenceService = productPersistenceService;
        this.skuPersistenceService = skuPersistenceService;
        this.productMapper = productMapper;
        this.rawPayloadMapper = rawPayloadMapper;
        this.cursorMapper = cursorMapper;
        this.cursorAdvancer = cursorAdvancer;
        this.syncRunMapper = syncRunMapper;
        this.alertAdminService = alertAdminService;
        this.payloadHasher = payloadHasher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public XianyuProductPageSyncResult syncPage(Long shopId, XianyuProductSyncWindow window) {
        return syncPage(shopId, window, TRIGGER_MANUAL);
    }

    public XianyuProductPageSyncResult syncPage(Long shopId, XianyuProductSyncWindow window, String triggerType) {
        return syncPage(shopId, null, window, triggerType);
    }

    public XianyuProductPageSyncResult syncPage(Long shopId, String sellerId, XianyuProductSyncWindow window,
                                                String triggerType) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(window, "window");
        String resolvedTrigger = triggerType == null || triggerType.isBlank() ? TRIGGER_MANUAL : triggerType;
        XianyuSyncRunDO run = startRun(shopId, window, resolvedTrigger);
        int received = 0;
        int succeeded = 0;
        int deduplicated = 0;
        int skuCount = 0;
        try {
            validateRetention(window);
            XianyuReadResponse listResponse = readClient.execute(XianyuReadEndpoint.PRODUCTS,
                    window.toRequestBody(objectMapper), sellerId);
            persistProductPageRawPayload(shopId, listResponse.rawBody());
            XianyuProductListPage page = listPageParser.parse(listResponse);
            received = page.entries().size();
            if (page.count() > MAX_WINDOW_ROWS) {
                throw new XianyuProductSyncException("WINDOW_TOO_LARGE",
                        "XianGuanJia product-list window exceeds 10000 rows");
            }
            boolean legacyEmptyPage = page.count() == 0 && page.entries().isEmpty() && page.pageNo() == 0;
            if (!legacyEmptyPage && page.pageNo() != window.pageNo()) {
                throw new XianyuProductSyncException("PAGE_METADATA_MISMATCH",
                        "XianGuanJia product-list page_no mismatch");
            }
            if (page.pageSize() != window.pageSize()) {
                throw new XianyuProductSyncException("PAGE_METADATA_MISMATCH",
                        "XianGuanJia product-list page_size mismatch");
            }
            int expectedRows = expectedRows(page, window, legacyEmptyPage);
            if (page.entries().size() != expectedRows) {
                throw new XianyuProductSyncException("PAGE_METADATA_MISMATCH",
                        "XianGuanJia product-list row count conflicts with pagination metadata");
            }
            ProductRefreshCounts counts = refreshProducts(shopId, sellerId, page);
            succeeded = counts.succeeded();
            deduplicated = counts.deduplicated();
            skuCount = refreshMultiSpecSkus(shopId, sellerId, page);
            finishSucceeded(run, received, succeeded, deduplicated);
            return new XianyuProductPageSyncResult(run.getId(), received, succeeded, deduplicated, skuCount);
        } catch (RuntimeException exception) {
            finishFailed(run, exception, received, succeeded);
            throw exception;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean advanceProductCursor(Long shopId, LocalDateTime sourceUpdatedAt, String externalProductId,
                                        LocalDateTime safeUpperBound) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(sourceUpdatedAt, "sourceUpdatedAt");
        Objects.requireNonNull(externalProductId, "externalProductId");
        Objects.requireNonNull(safeUpperBound, "safeUpperBound");
        XianyuSyncCursorDO current = cursorMapper.selectByShopIdAndResourceTypeForUpdate(shopId, RESOURCE_TYPE);
        if (!cursorAdvancer.isStrictlyNewer(current, sourceUpdatedAt, externalProductId)) {
            return false;
        }
        XianyuSyncCursorDO cursor = XianyuSyncCursorDO.builder()
                .id(current == null ? null : current.getId())
                .shopId(shopId)
                .resourceType(RESOURCE_TYPE)
                .cursorUpdatedAt(sourceUpdatedAt)
                .cursorExternalId(externalProductId)
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

    private ProductRefreshCounts refreshProducts(Long shopId, String sellerId, XianyuProductListPage page) {
        int succeeded = 0;
        int deduplicated = 0;
        Map<String, XianyuProductDO> existingProducts = loadRefreshState(shopId, page);
        for (XianyuProductListEntry entry : page.entries()) {
            if (isDetailCurrent(existingProducts.get(entry.externalProductId()), entry)) {
                succeeded++;
                deduplicated++;
                continue;
            }
            productPersistenceService.persistProductDetail(shopId,
                    readClient.execute(XianyuReadEndpoint.PRODUCT_DETAIL,
                            objectMapper.createObjectNode().put("product_id",
                                    Long.parseLong(entry.externalProductId())), sellerId).rawBody());
            succeeded++;
        }
        return new ProductRefreshCounts(succeeded, deduplicated);
    }

    private int refreshMultiSpecSkus(Long shopId, String sellerId, XianyuProductListPage page) {
        List<String> multiSpecProductIds = page.entries().stream()
                .filter(entry -> entry.specType() == 2)
                .map(XianyuProductListEntry::externalProductId)
                .distinct()
                .toList();
        int skuCount = 0;
        for (List<String> chunk : chunks(multiSpecProductIds, 100)) {
            if (chunk.isEmpty()) {
                continue;
            }
            ArrayNode productIds = objectMapper.createArrayNode();
            chunk.forEach(id -> productIds.add(Long.parseLong(id)));
            skuCount += skuPersistenceService.persistProductSkus(shopId,
                    readClient.execute(XianyuReadEndpoint.PRODUCT_SKUS,
                            objectMapper.createObjectNode().set("product_id", productIds), sellerId).rawBody());
        }
        return skuCount;
    }

    private Map<String, XianyuProductDO> loadRefreshState(Long shopId, XianyuProductListPage page) {
        if (page.entries().isEmpty()) {
            return Map.of();
        }
        return productMapper.selectRefreshStateList(shopId, page.entries().stream()
                        .map(XianyuProductListEntry::externalProductId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(XianyuProductDO::getExternalProductId, Function.identity()));
    }

    private boolean isDetailCurrent(XianyuProductDO existing, XianyuProductListEntry entry) {
        return existing != null && existing.getRawPayloadId() != null && existing.getSourceUpdatedAt() != null
                && !existing.getSourceUpdatedAt().isBefore(entry.sourceUpdatedAt());
    }

    private List<List<String>> chunks(List<String> values, int size) {
        List<List<String>> chunks = new ArrayList<>();
        for (int index = 0; index < values.size(); index += size) {
            chunks.add(values.subList(index, Math.min(values.size(), index + size)));
        }
        return chunks;
    }

    private XianyuSyncRunDO startRun(Long shopId, XianyuProductSyncWindow window, String triggerType) {
        XianyuSyncRunDO run = XianyuSyncRunDO.builder()
                .shopId(shopId)
                .resourceType(RESOURCE_TYPE)
                .triggerType(triggerType)
                .status("RUNNING")
                .windowStart(window.start())
                .windowEnd(window.end())
                .receivedCount(0)
                .deduplicatedCount(0)
                .succeededCount(0)
                .reviewRequiredCount(0)
                .failedCount(0)
                .startedAt(LocalDateTime.now(clock))
                .build();
        run.setCreator("system");
        run.setUpdater("system");
        syncRunMapper.insert(run);
        return run;
    }

    private void finishSucceeded(XianyuSyncRunDO run, int received, int succeeded, int deduplicated) {
        run.setStatus("SUCCEEDED");
        run.setReceivedCount(received);
        run.setDeduplicatedCount(deduplicated);
        run.setSucceededCount(succeeded);
        run.setReviewRequiredCount(0);
        run.setFinishedAt(LocalDateTime.now(clock));
        run.setUpdater("system");
        syncRunMapper.updateById(run);
    }

    private void finishFailed(XianyuSyncRunDO run, RuntimeException exception, int received, int succeeded) {
        run.setStatus("FAILED");
        run.setReceivedCount(received);
        run.setSucceededCount(succeeded);
        run.setFailedCount(1);
        run.setLastErrorCode(exception instanceof XianyuProductSyncException syncException
                ? syncException.errorCode() : XianyuSafeErrorCode.from(exception));
        run.setLastErrorMessage("Product synchronization page failed");
        run.setFinishedAt(LocalDateTime.now(clock));
        run.setUpdater("system");
        syncRunMapper.updateById(run);
        alertAdminService.recordSyncFailed(run.getShopId(), RESOURCE_TYPE, run.getLastErrorCode());
    }

    private int expectedRows(XianyuProductListPage page, XianyuProductSyncWindow window, boolean legacyEmptyPage) {
        if (legacyEmptyPage) {
            return 0;
        }
        int offset = (window.pageNo() - 1) * window.pageSize();
        return Math.min(window.pageSize(), Math.max(0, page.count() - offset));
    }

    private void persistProductPageRawPayload(Long shopId, String rawPayload) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String sourceIdentifier = "product-page:" + shopId;
        XianyuRawPayloadDO payload = XianyuRawPayloadDO.builder()
                .sourceType(PRODUCT_PAGE_SOURCE_TYPE)
                .sourceIdentifier(sourceIdentifier)
                .payloadHash(payloadHasher.sha256(rawPayload))
                .schemaVersion(PRODUCT_PAGE_SCHEMA_VERSION)
                .redactionVersion(RESTRICTED_PAYLOAD_POLICY)
                .payload(rawPayload)
                .receivedAt(LocalDateTime.now(clock))
                .build();
        payload.setCreator("system");
        payload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, payload);
    }

    private void validateRetention(XianyuProductSyncWindow window) {
        LocalDateTime oldestAllowed = LocalDateTime.now(clock.withZone(BUSINESS_ZONE)).minusMonths(6);
        if (window.start().isBefore(oldestAllowed)) {
            throw new XianyuProductSyncException("WINDOW_OUTSIDE_RETENTION",
                    "XianGuanJia product-list window starts before the six-month retention boundary");
        }
    }

    private record ProductRefreshCounts(int succeeded, int deduplicated) {
    }

    private static final class XianyuProductSyncException extends RuntimeException {

        private final String errorCode;

        private XianyuProductSyncException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        private String errorCode() {
            return errorCode;
        }

    }

}
