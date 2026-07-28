package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSalePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuAfterSaleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuAfterSaleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncRunMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuAfterSalePage;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuAfterSalePageParser;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuAfterSaleSnapshot;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuPayloadHasher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_AFTER_SALE_SYNC_FAILED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZE_MISSING;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;

@Service
public class XianyuAfterSaleAdminService {

    private static final String RESOURCE_TYPE = "AFTER_SALE";
    private static final String TRIGGER_MANUAL = "MANUAL";
    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    private static final String PAGE_SOURCE_TYPE = "AFTER_SALE_PAGE";
    private static final String LIST_ITEM_SOURCE_TYPE = "AFTER_SALE_LIST_ITEM";
    private static final String DETAIL_SOURCE_TYPE = "AFTER_SALE_DETAIL";
    private static final String PAGE_SCHEMA_VERSION = "XIAN_GUAN_JIA_AFTER_SALE_PAGE_V1";
    private static final String LIST_ITEM_SCHEMA_VERSION = "XIAN_GUAN_JIA_AFTER_SALE_LIST_V1";
    private static final String DETAIL_SCHEMA_VERSION = "XIAN_GUAN_JIA_AFTER_SALE_DETAIL_V1";
    private static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuAfterSaleMapper afterSaleMapper;
    private final XianyuShopMapper shopMapper;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuSyncRunMapper syncRunMapper;
    private final XianyuAlertAdminService alertAdminService;
    private final XianyuReadClient readClient;
    private final XianyuAfterSalePageParser pageParser;
    private final XianyuPayloadHasher payloadHasher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public XianyuAfterSaleAdminService(XianyuAfterSaleMapper afterSaleMapper, XianyuShopMapper shopMapper,
                                       XianyuRawPayloadMapper rawPayloadMapper, XianyuSyncRunMapper syncRunMapper,
                                       XianyuReadClient readClient, XianyuAfterSalePageParser pageParser,
                                       XianyuPayloadHasher payloadHasher, ObjectMapper objectMapper,
                                       XianyuAlertAdminService alertAdminService,
                                       @Qualifier("xianyuClock") Clock clock) {
        this.afterSaleMapper = afterSaleMapper;
        this.shopMapper = shopMapper;
        this.rawPayloadMapper = rawPayloadMapper;
        this.syncRunMapper = syncRunMapper;
        this.alertAdminService = alertAdminService;
        this.readClient = readClient;
        this.pageParser = pageParser;
        this.payloadHasher = payloadHasher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public PageResult<XianyuAfterSaleRespVO> getPage(XianyuAfterSalePageReqVO pageReqVO) {
        PageResult<XianyuAfterSaleDO> page = afterSaleMapper.selectAdminPage(
                pageReqVO, pageReqVO.getShopId(), pageReqVO.getAfterSaleStatus());
        List<XianyuAfterSaleRespVO> list = page.getList().stream()
                .map(this::toVo)
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public XianyuAfterSaleSyncRespVO syncPage(XianyuAfterSaleSyncReqVO reqVO) {
        return syncPage(reqVO, TRIGGER_MANUAL);
    }

    @Transactional(rollbackFor = Exception.class)
    public XianyuAfterSaleSyncRespVO syncPage(XianyuAfterSaleSyncReqVO reqVO, String triggerType) {
        validateTimeRange(reqVO);
        XianyuShopDO shop = validateShop(reqVO.getShopId());
        XianyuSyncRunDO run = startRun(shop.getId(), reqVO,
                StringUtils.hasText(triggerType) ? triggerType : TRIGGER_MANUAL);
        int received = 0;
        int succeeded = 0;
        try {
            XianyuReadResponse listResponse = readClient.execute(
                    XianyuReadEndpoint.AFTER_SALES, buildRequest(reqVO, Long.valueOf(shop.getAuthorizeId())));
            persistRawPayload(shop.getId(), PAGE_SOURCE_TYPE,
                    "after-sale-page:" + shop.getId() + ":" + reqVO.getPageNo(), listResponse.rawBody(),
                    PAGE_SCHEMA_VERSION);
            XianyuAfterSalePage page = pageParser.parse(listResponse);
            received = page.entries().size();
            for (XianyuAfterSaleSnapshot snapshot : page.entries()) {
                persistListItemRawPayload(shop.getId(), snapshot);
                XianyuReadResponse detailResponse = readClient.execute(XianyuReadEndpoint.AFTER_SALE_DETAIL,
                        objectMapper.createObjectNode().put("order_no", snapshot.externalOrderId()));
                Long detailRawPayloadId = persistRawPayload(shop.getId(), DETAIL_SOURCE_TYPE,
                        "after-sale-detail:" + shop.getId() + ":" + snapshot.externalOrderId(),
                        detailResponse.rawBody(), DETAIL_SCHEMA_VERSION);
                persistSnapshot(shop.getId(), pageParser.parseDetail(detailResponse), detailRawPayloadId);
                succeeded++;
            }
            finishSucceeded(run, received, succeeded);
            XianyuAfterSaleSyncRespVO resp = new XianyuAfterSaleSyncRespVO();
            resp.setSyncRunId(run.getId());
            resp.setReceivedCount(received);
            resp.setSucceededCount(succeeded);
            resp.setHasNextPage(page.hasNextPage());
            return resp;
        } catch (ServiceException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            finishFailed(run, ex, received, succeeded);
            throw exception(XIANYU_AFTER_SALE_SYNC_FAILED, XianyuSafeErrorCode.from(ex));
        }
    }

    private void validateTimeRange(XianyuAfterSaleSyncReqVO reqVO) {
        boolean hasApplyStart = reqVO.getApplyStart() != null;
        boolean hasApplyEnd = reqVO.getApplyEnd() != null;
        boolean hasRefundStart = reqVO.getRefundStart() != null;
        boolean hasRefundEnd = reqVO.getRefundEnd() != null;
        if (hasApplyStart != hasApplyEnd || hasRefundStart != hasRefundEnd
                || !hasApplyStart && !hasRefundStart) {
            throw exception(XIANYU_AFTER_SALE_SYNC_FAILED, "INVALID_TIME_RANGE");
        }
        if (hasApplyStart && !reqVO.getApplyStart().isBefore(reqVO.getApplyEnd())
                || hasRefundStart && !reqVO.getRefundStart().isBefore(reqVO.getRefundEnd())) {
            throw exception(XIANYU_AFTER_SALE_SYNC_FAILED, "INVALID_TIME_RANGE");
        }
    }

    private XianyuShopDO validateShop(Long shopId) {
        XianyuShopDO shop = shopMapper.selectByTenantIdAndId(TenantContextHolder.getRequiredTenantId(), shopId);
        if (shop == null) {
            throw exception(XIANYU_SHOP_NOT_EXISTS);
        }
        if (!StringUtils.hasText(shop.getAuthorizeId())) {
            throw exception(XIANYU_SHOP_AUTHORIZE_MISSING);
        }
        if (!"VALID".equals(shop.getAuthorizationStatus())
                || shop.getAuthorizationExpiresAt() != null
                && !shop.getAuthorizationExpiresAt().isAfter(LocalDateTime.now(clock.withZone(BUSINESS_ZONE)))) {
            throw exception(XIANYU_SHOP_AUTHORIZATION_INVALID);
        }
        return shop;
    }

    private ObjectNode buildRequest(XianyuAfterSaleSyncReqVO reqVO, Long authorizeId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("authorize_id", authorizeId);
        putRange(body, "apply_time", reqVO.getApplyStart(), reqVO.getApplyEnd());
        putRange(body, "refund_time", reqVO.getRefundStart(), reqVO.getRefundEnd());
        body.put("page_no", reqVO.getPageNo());
        body.put("page_size", reqVO.getPageSize());
        return body;
    }

    private void putRange(ObjectNode body, String fieldName, LocalDateTime start, LocalDateTime end) {
        if (start == null && end == null) {
            return;
        }
        ArrayNode range = body.putArray(fieldName);
        range.add(toEpochSecond(start));
        range.add(toEpochSecond(end));
    }

    private long toEpochSecond(LocalDateTime value) {
        return value.atZone(BUSINESS_ZONE).toEpochSecond();
    }

    private XianyuSyncRunDO startRun(Long shopId, XianyuAfterSaleSyncReqVO reqVO, String triggerType) {
        XianyuSyncRunDO run = XianyuSyncRunDO.builder()
                .shopId(shopId)
                .resourceType(RESOURCE_TYPE)
                .triggerType(triggerType)
                .status("RUNNING")
                .windowStart(reqVO.getApplyStart() != null ? reqVO.getApplyStart() : reqVO.getRefundStart())
                .windowEnd(reqVO.getApplyEnd() != null ? reqVO.getApplyEnd() : reqVO.getRefundEnd())
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

    private void finishSucceeded(XianyuSyncRunDO run, int received, int succeeded) {
        run.setStatus("SUCCEEDED");
        run.setReceivedCount(received);
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
        run.setLastErrorCode(XianyuSafeErrorCode.from(exception));
        run.setLastErrorMessage("After-sale synchronization page failed");
        run.setFinishedAt(LocalDateTime.now(clock));
        run.setUpdater("system");
        syncRunMapper.updateById(run);
        alertAdminService.recordSyncFailed(run.getShopId(), RESOURCE_TYPE, run.getLastErrorCode());
    }

    private void persistSnapshot(Long shopId, XianyuAfterSaleSnapshot snapshot, Long rawPayloadId) {
        XianyuAfterSaleDO existing = afterSaleMapper.selectByShopIdAndExternalAfterSaleIdForUpdate(
                shopId, snapshot.externalAfterSaleId());
        XianyuAfterSaleDO afterSale = XianyuAfterSaleDO.builder()
                .id(existing == null ? null : existing.getId())
                .shopId(shopId)
                .externalAfterSaleId(snapshot.externalAfterSaleId())
                .externalOrderId(snapshot.externalOrderId())
                .afterSaleStatus(snapshot.afterSaleStatus())
                .refundAmount(snapshot.refundAmount())
                .amountUnitStatus("UNCONFIRMED")
                .timeoutAt(snapshot.timeoutAt())
                .sourceUpdatedAt(snapshot.sourceUpdatedAt())
                .rawPayloadId(rawPayloadId)
                .build();
        if (existing == null) {
            afterSale.setCreator("system");
            afterSale.setUpdater("system");
            afterSaleMapper.insert(afterSale);
        } else {
            afterSale.setUpdater("system");
            afterSaleMapper.updateById(afterSale);
        }
        if (snapshot.timeoutAt() != null && !snapshot.timeoutAt().isAfter(LocalDateTime.now(clock))) {
            alertAdminService.recordAfterSaleTimeout(shopId, snapshot.externalAfterSaleId(), snapshot.timeoutAt());
        }
    }

    private void persistListItemRawPayload(Long shopId, XianyuAfterSaleSnapshot snapshot) {
        persistRawPayload(shopId, LIST_ITEM_SOURCE_TYPE,
                "after-sale:" + shopId + ":" + snapshot.externalAfterSaleId(), snapshot.payloadJson(),
                LIST_ITEM_SCHEMA_VERSION);
    }

    private Long persistRawPayload(Long shopId, String sourceType, String sourceIdentifier, String payloadJson,
                                   String schemaVersion) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String payloadHash = payloadHasher.sha256(payloadJson);
        XianyuRawPayloadDO payload = XianyuRawPayloadDO.builder()
                .sourceType(sourceType)
                .sourceIdentifier(sourceIdentifier)
                .payloadHash(payloadHash)
                .schemaVersion(schemaVersion)
                .redactionVersion(RESTRICTED_PAYLOAD_POLICY)
                .payload(payloadJson)
                .receivedAt(LocalDateTime.now(clock))
                .build();
        payload.setCreator("system");
        payload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, payload);
        XianyuRawPayloadDO existing = rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(
                tenantId, sourceType, sourceIdentifier, payloadHash);
        if (existing == null) {
            throw new IllegalStateException("After-sale payload disappeared after insert");
        }
        return existing.getId();
    }

    private XianyuAfterSaleRespVO toVo(XianyuAfterSaleDO afterSale) {
        XianyuAfterSaleRespVO vo = new XianyuAfterSaleRespVO();
        vo.setId(afterSale.getId());
        vo.setShopId(afterSale.getShopId());
        vo.setExternalAfterSaleId(XianyuAdminPrivacyMasker.maskIdentifier(afterSale.getExternalAfterSaleId()));
        vo.setExternalOrderId(XianyuAdminPrivacyMasker.maskIdentifier(afterSale.getExternalOrderId()));
        vo.setAfterSaleStatus(afterSale.getAfterSaleStatus());
        vo.setRefundAmount(afterSale.getRefundAmount());
        vo.setAmountUnitStatus(afterSale.getAmountUnitStatus());
        vo.setTimeoutAt(afterSale.getTimeoutAt());
        vo.setSourceUpdatedAt(afterSale.getSourceUpdatedAt());
        return vo;
    }

}
