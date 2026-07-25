package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPushReplayRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadReplayRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPageSyncResult;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushReplayOutcome;
import cn.iocoder.yudao.module.rental.integration.xianyu.webhook.XianyuPushRetryService;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_PUSH_EVENT_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_RAW_PAYLOAD_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_RAW_PAYLOAD_REPLAY_UNSUPPORTED;

@Service
public class XianyuReplayAdminService {

    private static final String ORDER_DETAIL_SOURCE_TYPE = "ORDER_DETAIL";
    private static final String ORDER_PAGE_SOURCE_TYPE = "ORDER_PAGE";

    private final XianyuPushEventMapper pushEventMapper;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuPushRetryService pushRetryService;
    private final XianyuOrderPersistenceService orderPersistenceService;
    private final XianyuOrderSyncService orderSyncService;

    public XianyuReplayAdminService(XianyuPushEventMapper pushEventMapper,
                                    XianyuRawPayloadMapper rawPayloadMapper,
                                    XianyuPushRetryService pushRetryService,
                                    XianyuOrderPersistenceService orderPersistenceService,
                                    XianyuOrderSyncService orderSyncService) {
        this.pushEventMapper = pushEventMapper;
        this.rawPayloadMapper = rawPayloadMapper;
        this.pushRetryService = pushRetryService;
        this.orderPersistenceService = orderPersistenceService;
        this.orderSyncService = orderSyncService;
    }

    public XianyuPushReplayRespVO replayPushEvent(Long eventId, Long operatorId) {
        if (pushEventMapper.selectByTenantIdAndId(TenantContextHolder.getRequiredTenantId(), eventId) == null) {
            throw exception(XIANYU_PUSH_EVENT_NOT_EXISTS);
        }
        return toVo(pushRetryService.replayPushEvent(eventId, operatorId));
    }

    public XianyuRawPayloadReplayRespVO replayRawPayload(Long rawPayloadId, Long operatorId) {
        XianyuRawPayloadDO rawPayload = rawPayloadMapper.selectByTenantIdAndId(
                TenantContextHolder.getRequiredTenantId(), rawPayloadId);
        if (rawPayload == null) {
            throw exception(XIANYU_RAW_PAYLOAD_NOT_EXISTS);
        }
        if (ORDER_PAGE_SOURCE_TYPE.equals(rawPayload.getSourceType())) {
            return replayOrderPage(rawPayloadId, rawPayload);
        }
        if (!ORDER_DETAIL_SOURCE_TYPE.equals(rawPayload.getSourceType())) {
            throw unsupportedRawPayload(rawPayload);
        }
        Long shopId = parseShopId(rawPayload, "order:");
        try {
            XianyuOrderDO order = orderPersistenceService.persistOrderDetail(shopId, rawPayload.getPayload());
            XianyuRawPayloadReplayRespVO vo = new XianyuRawPayloadReplayRespVO();
            vo.setRawPayloadId(rawPayloadId);
            vo.setOrderId(order.getId());
            vo.setStatus("REPLAYED");
            vo.setMessage(XianyuAdminPrivacyMasker.maskFreeText("Order detail raw payload replayed"));
            return vo;
        } catch (RuntimeException exception) {
            String safeErrorCode = XianyuSafeErrorCode.from(exception);
            XianyuRawPayloadReplayRespVO vo = new XianyuRawPayloadReplayRespVO();
            vo.setRawPayloadId(rawPayloadId);
            vo.setStatus("FAILED");
            vo.setSafeErrorCode(safeErrorCode);
            vo.setMessage("Order detail raw payload replay failed: " + safeErrorCode);
            return vo;
        }
    }

    private XianyuRawPayloadReplayRespVO replayOrderPage(Long rawPayloadId, XianyuRawPayloadDO rawPayload) {
        Long shopId = parseShopId(rawPayload, "order-page:");
        try {
            XianyuOrderPageSyncResult result = orderSyncService.replayPersistedPage(shopId, rawPayload.getPayload());
            XianyuRawPayloadReplayRespVO vo = new XianyuRawPayloadReplayRespVO();
            vo.setRawPayloadId(rawPayloadId);
            vo.setStatus("REPLAYED");
            vo.setMessage(XianyuAdminPrivacyMasker.maskFreeText("Order page raw payload replayed; received="
                    + result.receivedCount() + ", succeeded=" + result.succeededCount()));
            return vo;
        } catch (RuntimeException exception) {
            String safeErrorCode = XianyuSafeErrorCode.from(exception);
            XianyuRawPayloadReplayRespVO vo = new XianyuRawPayloadReplayRespVO();
            vo.setRawPayloadId(rawPayloadId);
            vo.setStatus("FAILED");
            vo.setSafeErrorCode(safeErrorCode);
            vo.setMessage("Order page raw payload replay failed: " + safeErrorCode);
            return vo;
        }
    }

    private XianyuPushReplayRespVO toVo(XianyuPushReplayOutcome outcome) {
        XianyuPushReplayRespVO vo = new XianyuPushReplayRespVO();
        vo.setEventId(outcome.eventId());
        vo.setStatus(outcome.status());
        vo.setSafeErrorCode(outcome.safeErrorCode());
        vo.setMessage(XianyuAdminPrivacyMasker.maskFreeText(outcome.message()));
        return vo;
    }

    private Long parseShopId(XianyuRawPayloadDO rawPayload, String prefix) {
        String identifier = rawPayload.getSourceIdentifier();
        if (identifier == null || !identifier.startsWith(prefix)) {
            throw unsupportedRawPayload(rawPayload);
        }
        String remainder = identifier.substring(prefix.length());
        int separator = remainder.indexOf(':');
        String shopId = separator >= 0 ? remainder.substring(0, separator) : remainder;
        if (shopId.isBlank()) {
            throw unsupportedRawPayload(rawPayload);
        }
        try {
            return Long.valueOf(shopId);
        } catch (NumberFormatException ex) {
            throw unsupportedRawPayload(rawPayload);
        }
    }

    private ServiceException unsupportedRawPayload(XianyuRawPayloadDO rawPayload) {
        return exception(XIANYU_RAW_PAYLOAD_REPLAY_UNSUPPORTED,
                XianyuAdminPrivacyMasker.maskFreeText(rawPayload.getSourceType()));
    }

}
