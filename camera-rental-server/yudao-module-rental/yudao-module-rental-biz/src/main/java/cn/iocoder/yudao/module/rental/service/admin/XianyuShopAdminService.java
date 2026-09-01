package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuShopRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuClientException;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuAuthorizedShop;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuAuthorizedShopListParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_CREDENTIALS_MISSING;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_INTEGRATION_DISABLED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_REMOTE_ERROR;
import static cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuAuthorizedShopListParser.GUARANTEE_STATUS_DEPOSIT_INSUFFICIENT;

@Service
public class XianyuShopAdminService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuRuntimeConfigService runtimeConfigService;
    private final XianyuReadClient readClient;
    private final XianyuShopMapper shopMapper;
    private final XianyuAlertAdminService alertAdminService;
    private final XianyuAuthorizedShopListParser listParser = new XianyuAuthorizedShopListParser();
    private final ObjectMapper objectMapper;

    public XianyuShopAdminService(XianyuRuntimeConfigService runtimeConfigService, XianyuReadClient readClient,
                                  XianyuShopMapper shopMapper,
                                  XianyuAlertAdminService alertAdminService, ObjectMapper objectMapper) {
        this.runtimeConfigService = runtimeConfigService;
        this.readClient = readClient;
        this.shopMapper = shopMapper;
        this.alertAdminService = alertAdminService;
        this.objectMapper = objectMapper;
    }

    public PageResult<XianyuShopRespVO> getShopPage(PageParam pageParam) {
        PageResult<XianyuShopDO> page = shopMapper.selectPage(pageParam,
                new LambdaQueryWrapperX<XianyuShopDO>().orderByDesc(XianyuShopDO::getId));
        List<XianyuShopRespVO> list = page.getList().stream().map(this::toVo).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public int syncAuthorizedShops() {
        XianyuProperties properties = runtimeConfigService.getCurrent();
        assertReady(properties);
        XianyuApplicationDO application = runtimeConfigService.getCurrentApplication();
        if (application == null) {
            throw exception(XIANYU_CREDENTIALS_MISSING);
        }
        ObjectNode body = objectMapper.createObjectNode();
        final XianyuReadResponse response;
        try {
            response = readClient.execute(XianyuReadEndpoint.AUTHORIZED_SHOPS, body);
        } catch (XianyuClientException exception) {
            throw exception(XIANYU_REMOTE_ERROR);
        }
        List<XianyuAuthorizedShop> remoteShops = listParser.parse(response.payload());
        LocalDateTime synchronizedAt = LocalDateTime.now(BUSINESS_ZONE);
        List<XianyuShopDO> existingShops = shopMapper.selectListByApplicationId(application.getId());
        Set<String> remoteAuthorizeIds = remoteShops.stream()
                .map(XianyuAuthorizedShop::authorizeId)
                .collect(Collectors.toCollection(HashSet::new));
        int upserted = 0;
        for (XianyuAuthorizedShop remote : remoteShops) {
            // Upsert by authorize_id: many brands share one seller_id under multi-shop authorize.
            XianyuShopDO existing = shopMapper.selectByApplicationAndAuthorizeId(
                    application.getId(), remote.authorizeId());
            XianyuShopDO shop = XianyuShopDO.builder()
                    .id(existing == null ? null : existing.getId())
                    .applicationId(application.getId())
                    .externalShopId(remote.externalShopId())
                    .xianyuUserName(remote.xianyuUserName())
                    .authorizeId(remote.authorizeId())
                    .shopName(remote.shopName())
                    .authorizationStatus(remote.valid() ? "VALID" : "INVALID")
                    .authorizationExpiresAt(toDateTime(remote.validEndTimeEpochSeconds()))
                    .guaranteeStatus(remote.guaranteeStatus())
                    .sourceUpdatedAt(synchronizedAt)
                    .build();
            // Job / unauthenticated context has no login user; columns are NOT NULL.
            if (existing == null) {
                shop.setCreator("system");
                shop.setUpdater("system");
                shopMapper.insert(shop);
            } else {
                shop.setUpdater("system");
                shopMapper.updateById(shop);
            }
            if (!remote.valid()) {
                alertAdminService.recordShopAuthorizationInvalid(
                        shop.getId(), remote.authorizeId(), "Shop authorization returned invalid in latest snapshot");
            }
            if (GUARANTEE_STATUS_DEPOSIT_INSUFFICIENT.equals(remote.guaranteeStatus())) {
                alertAdminService.recordGuaranteeHealth(shop.getId(), remote.authorizeId(), remote.guaranteeStatus());
            }
            upserted++;
        }
        for (XianyuShopDO existing : existingShops) {
            if (remoteAuthorizeIds.contains(existing.getAuthorizeId())
                    || "INVALID".equals(existing.getAuthorizationStatus())) {
                continue;
            }
            existing.setAuthorizationStatus("INVALID");
            existing.setSourceUpdatedAt(synchronizedAt);
            existing.setUpdater("system");
            shopMapper.updateById(existing);
            alertAdminService.recordShopAuthorizationInvalid(
                    existing.getId(), existing.getAuthorizeId(), "Shop authorization disappeared from latest snapshot");
            upserted++;
        }
        return upserted;
    }

    private void assertReady(XianyuProperties properties) {
        switch (properties.getIntegrationStatus()) {
            case DISABLED -> throw exception(XIANYU_INTEGRATION_DISABLED);
            case MISSING_CREDENTIALS -> throw exception(XIANYU_CREDENTIALS_MISSING);
            case READY -> {
            }
        }
    }

    private XianyuShopRespVO toVo(XianyuShopDO shop) {
        XianyuShopRespVO vo = new XianyuShopRespVO();
        vo.setId(shop.getId());
        vo.setApplicationId(shop.getApplicationId());
        vo.setExternalShopId(XianyuAdminPrivacyMasker.maskIdentifier(shop.getExternalShopId()));
        vo.setAuthorizeId(XianyuAdminPrivacyMasker.maskIdentifier(shop.getAuthorizeId()));
        vo.setShopName(shop.getShopName());
        vo.setAuthorizationStatus(shop.getAuthorizationStatus());
        vo.setAuthorizationExpiresAt(shop.getAuthorizationExpiresAt());
        vo.setGuaranteeStatus(shop.getGuaranteeStatus());
        return vo;
    }

    private static LocalDateTime toDateTime(Long epochSeconds) {
        if (epochSeconds == null || epochSeconds <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), BUSINESS_ZONE);
    }

}
