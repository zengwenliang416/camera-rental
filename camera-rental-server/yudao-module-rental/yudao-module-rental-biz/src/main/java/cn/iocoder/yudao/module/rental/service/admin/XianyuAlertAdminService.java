package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAlertPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAlertRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuAlertDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuAlertMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ALERT_NOT_EXISTS;

@Service
public class XianyuAlertAdminService {

    public static final String TYPE_SHOP_AUTH_INVALID = "SHOP_AUTH_INVALID";
    public static final String TYPE_SYNC_FAILED = "SYNC_FAILED";
    public static final String TYPE_AFTER_SALE_TIMEOUT = "AFTER_SALE_TIMEOUT";
    public static final String TYPE_GUARANTEE_HEALTH = "GUARANTEE_HEALTH";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RESOLVED = "RESOLVED";

    private final XianyuAlertMapper alertMapper;

    public XianyuAlertAdminService(XianyuAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    public PageResult<XianyuAlertRespVO> getAlertPage(XianyuAlertPageReqVO reqVO) {
        PageResult<XianyuAlertDO> page = alertMapper.selectPage(reqVO, new LambdaQueryWrapperX<XianyuAlertDO>()
                .eqIfPresent(XianyuAlertDO::getShopId, reqVO.getShopId())
                .eqIfPresent(XianyuAlertDO::getAlertType, reqVO.getAlertType())
                .eqIfPresent(XianyuAlertDO::getStatus, reqVO.getStatus())
                .eqIfPresent(XianyuAlertDO::getSeverity, reqVO.getSeverity())
                .orderByDesc(XianyuAlertDO::getLastSeenAt)
                .orderByDesc(XianyuAlertDO::getId));
        List<XianyuAlertRespVO> list = page.getList().stream().map(this::toVo).toList();
        return new PageResult<>(list, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordShopAuthorizationInvalid(Long shopId, String authorizeId, String reason) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now();
        String dedupeKey = shopId == null
                ? TYPE_SHOP_AUTH_INVALID + ":authorize:" + authorizeId
                : TYPE_SHOP_AUTH_INVALID + ":" + shopId;
        XianyuAlertDO alert = XianyuAlertDO.builder()
                .shopId(shopId)
                .alertType(TYPE_SHOP_AUTH_INVALID)
                .dedupeKey(dedupeKey)
                .severity(SEVERITY_WARNING)
                .status(STATUS_OPEN)
                .sourceIdentifier(authorizeId)
                .message(reason)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .build();
        alert.setCreator("system");
        alert.setUpdater("system");
        alertMapper.insertOrRefresh(tenantId, alert);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordSyncFailed(Long shopId, String resourceType, String safeErrorCode) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now();
        String normalizedResourceType = resourceType == null || resourceType.isBlank() ? "UNKNOWN" : resourceType;
        String normalizedCode = safeErrorCode == null || safeErrorCode.isBlank() ? "UNKNOWN" : safeErrorCode;
        XianyuAlertDO alert = XianyuAlertDO.builder()
                .shopId(shopId)
                .alertType(TYPE_SYNC_FAILED)
                .dedupeKey(TYPE_SYNC_FAILED + ":" + normalizedResourceType + ":" + shopId + ":" + normalizedCode)
                .severity(SEVERITY_WARNING)
                .status(STATUS_OPEN)
                .sourceIdentifier(normalizedResourceType)
                .message(normalizedResourceType + " synchronization failed: " + normalizedCode)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .build();
        alert.setCreator("system");
        alert.setUpdater("system");
        alertMapper.insertOrRefresh(tenantId, alert);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordGuaranteeHealth(Long shopId, String authorizeId, String guaranteeStatus) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now();
        String normalizedStatus = guaranteeStatus == null || guaranteeStatus.isBlank() ? "UNKNOWN" : guaranteeStatus;
        String dedupeKey = shopId == null
                ? TYPE_GUARANTEE_HEALTH + ":authorize:" + authorizeId
                : TYPE_GUARANTEE_HEALTH + ":" + shopId;
        XianyuAlertDO alert = XianyuAlertDO.builder()
                .shopId(shopId)
                .alertType(TYPE_GUARANTEE_HEALTH)
                .dedupeKey(dedupeKey)
                .severity(SEVERITY_WARNING)
                .status(STATUS_OPEN)
                .sourceIdentifier(authorizeId)
                .message("Shop guarantee health requires attention: " + normalizedStatus)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .build();
        alert.setCreator("system");
        alert.setUpdater("system");
        alertMapper.insertOrRefresh(tenantId, alert);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordAfterSaleTimeout(Long shopId, String externalAfterSaleId, LocalDateTime timeoutAt) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime now = LocalDateTime.now();
        XianyuAlertDO alert = XianyuAlertDO.builder()
                .shopId(shopId)
                .alertType(TYPE_AFTER_SALE_TIMEOUT)
                .dedupeKey(TYPE_AFTER_SALE_TIMEOUT + ":" + shopId + ":" + externalAfterSaleId)
                .severity(SEVERITY_WARNING)
                .status(STATUS_OPEN)
                .sourceIdentifier(externalAfterSaleId)
                .message("After-sale timeout reached at " + timeoutAt)
                .firstSeenAt(now)
                .lastSeenAt(now)
                .build();
        alert.setCreator("system");
        alert.setUpdater("system");
        alertMapper.insertOrRefresh(tenantId, alert);
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolveAlert(Long id, Long operatorId) {
        XianyuAlertDO alert = alertMapper.selectByTenantIdAndId(TenantContextHolder.getRequiredTenantId(), id);
        if (alert == null) {
            throw exception(XIANYU_ALERT_NOT_EXISTS);
        }
        alert.setStatus(STATUS_RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setUpdater(operatorId == null ? "system" : String.valueOf(operatorId));
        alertMapper.updateById(alert);
    }

    private XianyuAlertRespVO toVo(XianyuAlertDO alert) {
        XianyuAlertRespVO vo = new XianyuAlertRespVO();
        vo.setId(alert.getId());
        vo.setShopId(alert.getShopId());
        vo.setAlertType(alert.getAlertType());
        vo.setSeverity(alert.getSeverity());
        vo.setStatus(alert.getStatus());
        vo.setSourceIdentifier(XianyuAdminPrivacyMasker.maskIdentifier(alert.getSourceIdentifier()));
        vo.setMessage(XianyuAdminPrivacyMasker.maskFreeText(alert.getMessage()));
        vo.setFirstSeenAt(alert.getFirstSeenAt());
        vo.setLastSeenAt(alert.getLastSeenAt());
        vo.setResolvedAt(alert.getResolvedAt());
        return vo;
    }

}
