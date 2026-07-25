package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncRunMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAlertAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Coordinates one fixed order-list page. It never calls a third-party write endpoint.
 */
@Service
public class XianyuOrderSyncService {

    private static final String RESOURCE_TYPE = "ORDER";
    public static final String ORDER_PAGE_SOURCE_TYPE = "ORDER_PAGE";
    private static final String ORDER_PAGE_SCHEMA_VERSION = "XIAN_GUAN_JIA_ORDER_PAGE_V1";
    private static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";
    public static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_REPLAY = "REPLAY";
    private static final int MAX_WINDOW_ROWS = 10_000;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuReadClient readClient;
    private final XianyuOrderListPageParser listPageParser;
    private final XianyuOrderPersistenceService persistenceService;
    private final XianyuOrderMapper orderMapper;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuSyncRunMapper syncRunMapper;
    private final XianyuAlertAdminService alertAdminService;
    private final XianyuPayloadHasher payloadHasher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public XianyuOrderSyncService(XianyuReadClient readClient, XianyuOrderListPageParser listPageParser,
                                  XianyuOrderPersistenceService persistenceService,
                                  XianyuOrderMapper orderMapper, XianyuRawPayloadMapper rawPayloadMapper,
                                  XianyuSyncRunMapper syncRunMapper, XianyuAlertAdminService alertAdminService,
                                  XianyuPayloadHasher payloadHasher, ObjectMapper objectMapper,
                                  @Qualifier("xianyuClock") Clock clock) {
        this.readClient = readClient;
        this.listPageParser = listPageParser;
        this.persistenceService = persistenceService;
        this.orderMapper = orderMapper;
        this.rawPayloadMapper = rawPayloadMapper;
        this.syncRunMapper = syncRunMapper;
        this.alertAdminService = alertAdminService;
        this.payloadHasher = payloadHasher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public XianyuOrderPageSyncResult syncPage(Long shopId, Long authorizeId, XianyuOrderSyncWindow window) {
        return syncPage(shopId, authorizeId, window, TRIGGER_MANUAL);
    }

    public XianyuOrderPageSyncResult syncPage(Long shopId, Long authorizeId, XianyuOrderSyncWindow window,
                                              String triggerType) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(authorizeId, "authorizeId");
        Objects.requireNonNull(window, "window");
        String resolvedTrigger = triggerType == null || triggerType.isBlank() ? TRIGGER_MANUAL : triggerType;
        XianyuSyncRunDO run = startRun(shopId, window, resolvedTrigger);
        int received = 0;
        int succeeded = 0;
        int deduplicated = 0;
        try {
            validateRetention(window);
            XianyuReadResponse listResponse = readClient.execute(XianyuReadEndpoint.ORDERS,
                    window.toRequestBody(objectMapper, authorizeId));
            persistOrderPageRawPayload(shopId, listResponse.rawBody());
            XianyuOrderListPage page = listPageParser.parse(listResponse);
            received = page.entries().size();
            if (page.count() > MAX_WINDOW_ROWS) {
                throw new XianyuOrderSyncException("WINDOW_TOO_LARGE",
                        "XianGuanJia order-list window exceeds 10000 rows");
            }
            // Some shops return page_no=0 for a genuinely empty result.
            boolean legacyEmptyPage = page.count() == 0 && page.entries().isEmpty() && page.pageNo() == 0;
            if (!legacyEmptyPage && page.pageNo() != window.pageNo()) {
                throw new XianyuOrderSyncException("PAGE_METADATA_MISMATCH",
                        "XianGuanJia order-list page_no mismatch request=" + window.pageNo()
                                + " response=" + page.pageNo());
            }
            if (page.pageSize() != window.pageSize()) {
                throw new XianyuOrderSyncException("PAGE_METADATA_MISMATCH",
                        "XianGuanJia order-list page_size mismatch");
            }
            int expectedRows = expectedRows(page, window, legacyEmptyPage);
            if (page.entries().size() != expectedRows) {
                throw new XianyuOrderSyncException("PAGE_METADATA_MISMATCH",
                        "XianGuanJia order-list row count conflicts with pagination metadata");
            }
            DetailRefreshCounts counts = refreshDetails(shopId, page);
            succeeded = counts.succeeded();
            deduplicated = counts.deduplicated();
            finishSucceeded(run, received, succeeded, deduplicated);
            return new XianyuOrderPageSyncResult(run.getId(), received, succeeded, false);
        } catch (DetailRefreshException exception) {
            finishFailed(run, exception.cause(), received, exception.succeeded());
            throw exception.cause();
        } catch (RuntimeException exception) {
            finishFailed(run, exception, received, succeeded);
            throw exception;
        }
    }

    public XianyuOrderPageSyncResult replayPersistedPage(Long shopId, String rawPayload) {
        Objects.requireNonNull(shopId, "shopId");
        Objects.requireNonNull(rawPayload, "rawPayload");
        XianyuSyncRunDO run = startRun(shopId, null, TRIGGER_REPLAY);
        int received = 0;
        int succeeded = 0;
        try {
            XianyuOrderListPage page = listPageParser.parse(new XianyuReadResponse(
                    200, 0, objectMapper.readTree(rawPayload), rawPayload));
            received = page.entries().size();
            DetailRefreshCounts counts = refreshDetails(shopId, page);
            succeeded = counts.succeeded();
            finishSucceeded(run, received, succeeded, counts.deduplicated());
            return new XianyuOrderPageSyncResult(run.getId(), received, succeeded, false);
        } catch (DetailRefreshException exception) {
            finishFailed(run, exception.cause(), received, exception.succeeded());
            throw exception.cause();
        } catch (Exception exception) {
            RuntimeException runtimeException = exception instanceof RuntimeException runtime
                    ? runtime : new IllegalStateException("Order page replay payload could not be parsed", exception);
            finishFailed(run, runtimeException, received, succeeded);
            throw runtimeException;
        }
    }

    private XianyuSyncRunDO startRun(Long shopId, XianyuOrderSyncWindow window, String triggerType) {
        XianyuSyncRunDO run = XianyuSyncRunDO.builder()
                .shopId(shopId)
                .resourceType(RESOURCE_TYPE)
                .triggerType(triggerType)
                .status("RUNNING")
                .windowStart(window == null ? null : window.start())
                .windowEnd(window == null ? null : window.end())
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
        run.setFinishedAt(LocalDateTime.now(clock));
        run.setUpdater("system");
        syncRunMapper.updateById(run);
    }

    private void finishFailed(XianyuSyncRunDO run, RuntimeException exception, int received, int succeeded) {
        run.setStatus("FAILED");
        run.setReceivedCount(received);
        run.setSucceededCount(succeeded);
        run.setFailedCount(1);
        run.setLastErrorCode(exception instanceof XianyuOrderSyncException syncException
                ? syncException.errorCode() : XianyuSafeErrorCode.from(exception));
        run.setLastErrorMessage("Order synchronization page failed");
        run.setFinishedAt(LocalDateTime.now(clock));
        run.setUpdater("system");
        syncRunMapper.updateById(run);
        alertAdminService.recordSyncFailed(run.getShopId(), RESOURCE_TYPE, run.getLastErrorCode());
    }

    private int expectedRows(XianyuOrderListPage page, XianyuOrderSyncWindow window, boolean legacyEmptyPage) {
        if (legacyEmptyPage) {
            return 0;
        }
        int offset = (window.pageNo() - 1) * window.pageSize();
        return Math.min(window.pageSize(), Math.max(0, page.count() - offset));
    }

    private Map<String, XianyuOrderDO> loadRefreshState(Long shopId, XianyuOrderListPage page) {
        if (page.entries().isEmpty()) {
            return Map.of();
        }
        return orderMapper.selectRefreshStateList(shopId, page.entries().stream()
                        .map(XianyuOrderListEntry::externalOrderId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(XianyuOrderDO::getExternalOrderId, Function.identity()));
    }

    private boolean isDetailCurrent(XianyuOrderDO existing, XianyuOrderListEntry entry) {
        return existing != null && existing.getRawPayloadId() != null && existing.getSourceUpdatedAt() != null
                && !existing.getSourceUpdatedAt().isBefore(entry.sourceUpdatedAt());
    }

    private DetailRefreshCounts refreshDetails(Long shopId, XianyuOrderListPage page) {
        int succeeded = 0;
        int deduplicated = 0;
        Map<String, XianyuOrderDO> existingOrders = loadRefreshState(shopId, page);
        try {
            for (XianyuOrderListEntry entry : page.entries()) {
                if (isDetailCurrent(existingOrders.get(entry.externalOrderId()), entry)) {
                    succeeded++;
                    deduplicated++;
                    continue;
                }
                persistenceService.persistOrderDetail(shopId,
                        readClient.execute(XianyuReadEndpoint.ORDER_DETAIL,
                                objectMapper.createObjectNode().put("order_no", entry.externalOrderId())).rawBody());
                succeeded++;
            }
        } catch (RuntimeException exception) {
            throw new DetailRefreshException(exception, succeeded);
        }
        return new DetailRefreshCounts(succeeded, deduplicated);
    }

    private void persistOrderPageRawPayload(Long shopId, String rawPayload) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String sourceIdentifier = "order-page:" + shopId;
        XianyuRawPayloadDO payload = XianyuRawPayloadDO.builder()
                .sourceType(ORDER_PAGE_SOURCE_TYPE)
                .sourceIdentifier(sourceIdentifier)
                .payloadHash(payloadHasher.sha256(rawPayload))
                .schemaVersion(ORDER_PAGE_SCHEMA_VERSION)
                .redactionVersion(RESTRICTED_PAYLOAD_POLICY)
                .payload(rawPayload)
                .receivedAt(LocalDateTime.now(clock))
                .build();
        payload.setCreator("system");
        payload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, payload);
    }

    private void validateRetention(XianyuOrderSyncWindow window) {
        LocalDateTime oldestAllowed = LocalDateTime.now(clock.withZone(BUSINESS_ZONE)).minusMonths(6);
        if (window.start().isBefore(oldestAllowed)) {
            throw new XianyuOrderSyncException("WINDOW_OUTSIDE_RETENTION",
                    "XianGuanJia order-list window starts before the six-month retention boundary");
        }
    }

    private static final class XianyuOrderSyncException extends RuntimeException {

        private final String errorCode;

        private XianyuOrderSyncException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        private String errorCode() {
            return errorCode;
        }

    }

    private record DetailRefreshCounts(int succeeded, int deduplicated) {
    }

    private static final class DetailRefreshException extends RuntimeException {

        private final RuntimeException cause;
        private final int succeeded;

        private DetailRefreshException(RuntimeException cause, int succeeded) {
            super(cause);
            this.cause = cause;
            this.succeeded = succeeded;
        }

        private RuntimeException cause() {
            return cause;
        }

        private int succeeded() {
            return succeeded;
        }

    }

}
