package cn.iocoder.yudao.module.rental.service.configuration;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSaveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSkuReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuApplicationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductSkuMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_PRODUCT_NOT_SYNCHRONIZED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_PRODUCT_RULE_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_SKU_OWNERSHIP_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_MODEL_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_NOT_EXISTS;

@Component
public class RentalChannelProductRuleValidator {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final XianyuShopMapper shopMapper;
    private final XianyuApplicationMapper applicationMapper;
    private final XianyuProductMapper productMapper;
    private final XianyuProductSkuMapper productSkuMapper;
    private final RentalDeviceModelMapper modelMapper;

    public RentalChannelProductRuleValidator(XianyuShopMapper shopMapper,
                                             XianyuApplicationMapper applicationMapper,
                                             XianyuProductMapper productMapper,
                                             XianyuProductSkuMapper productSkuMapper,
                                             RentalDeviceModelMapper modelMapper) {
        this.shopMapper = shopMapper;
        this.applicationMapper = applicationMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.modelMapper = modelMapper;
    }

    public ValidatedRule validate(RentalChannelProductRuleSaveReqVO reqVO) {
        XianyuProductDO product = requireExactProduct(reqVO.getShopId(), reqVO.getXianyuItemId());
        String policy = normalizePolicy(reqVO.getHandlingPolicy());
        if ("CONFIG_SKIPPED".equals(policy)) {
            return new ValidatedRule(product, policy, "NONE", null, List.of());
        }
        String mappingMode = normalizeMappingMode(reqVO.getMappingMode());
        if ("SINGLE".equals(mappingMode)) {
            RentalDeviceModelDO model = requireEnabledModel(reqVO.getSingleDeviceModelId());
            return new ValidatedRule(product, policy, mappingMode, model.getId(), List.of());
        }
        List<RentalChannelProductRuleSkuReqVO> requested =
                reqVO.getSkuMappings() == null ? List.of() : reqVO.getSkuMappings();
        if (requested.isEmpty()) {
            throw exception(RENTAL_CHANNEL_PRODUCT_RULE_INVALID, "多型号商品至少需要一条 SKU 映射");
        }
        Map<Long, XianyuProductSkuDO> synchronizedSkus = new HashMap<>();
        for (XianyuProductSkuDO sku : productSkuMapper.selectListByProductId(product.getId())) {
            synchronizedSkus.put(sku.getId(), sku);
        }
        Set<Long> seenSkuIds = new HashSet<>();
        List<ValidatedSku> validatedSkus = requested.stream().map(mapping -> {
            if (mapping.getProductSkuId() == null || !seenSkuIds.add(mapping.getProductSkuId())) {
                throw exception(RENTAL_CHANNEL_SKU_OWNERSHIP_INVALID);
            }
            XianyuProductSkuDO sku = synchronizedSkus.get(mapping.getProductSkuId());
            if (sku == null || !StringUtils.hasText(sku.getXgjSkuId())) {
                throw exception(RENTAL_CHANNEL_SKU_OWNERSHIP_INVALID);
            }
            RentalDeviceModelDO model = requireEnabledModel(mapping.getDeviceModelId());
            return new ValidatedSku(sku, model.getId(), Boolean.TRUE.equals(mapping.getEnabled()));
        }).toList();
        return new ValidatedRule(product, policy, mappingMode, null, validatedSkus);
    }

    public XianyuProductDO requireExactProduct(Long shopId, String xianyuItemId) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        XianyuShopDO shop = shopMapper.selectByTenantIdAndId(tenantId, shopId);
        if (shop == null) {
            throw exception(XIANYU_SHOP_NOT_EXISTS);
        }
        XianyuApplicationDO application = shop.getApplicationId() == null ? null
                : applicationMapper.selectByTenantIdAndId(tenantId, shop.getApplicationId());
        if (application == null) {
            throw exception(XIANYU_SHOP_AUTHORIZATION_INVALID);
        }
        if (!"VALID".equals(shop.getAuthorizationStatus())
                || shop.getAuthorizationExpiresAt() != null
                && !shop.getAuthorizationExpiresAt().isAfter(LocalDateTime.now(BUSINESS_ZONE))) {
            throw exception(XIANYU_SHOP_AUTHORIZATION_INVALID);
        }
        String normalizedItemId = xianyuItemId == null ? "" : xianyuItemId.trim();
        XianyuProductDO product = productMapper.selectByShopIdAndXianyuItemId(shopId, normalizedItemId);
        if (product == null) {
            throw exception(RENTAL_CHANNEL_PRODUCT_NOT_SYNCHRONIZED);
        }
        return product;
    }

    public List<XianyuProductSkuDO> getSynchronizedSkus(Long shopId, String xianyuItemId) {
        XianyuProductDO product = requireExactProduct(shopId, xianyuItemId);
        return productSkuMapper.selectListByProductId(product.getId());
    }

    private RentalDeviceModelDO requireEnabledModel(Long modelId) {
        RentalDeviceModelDO model = modelId == null ? null : modelMapper.selectById(modelId);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw exception(RENTAL_DEVICE_MODEL_NOT_EXISTS);
        }
        return model;
    }

    private static String normalizePolicy(String policy) {
        String normalized = policy == null ? "" : policy.trim().toUpperCase();
        if (!Set.of("CREATE_RENTAL", "CONFIG_SKIPPED").contains(normalized)) {
            throw exception(RENTAL_CHANNEL_PRODUCT_RULE_INVALID, "处理策略仅支持 CREATE_RENTAL 或 CONFIG_SKIPPED");
        }
        return normalized;
    }

    private static String normalizeMappingMode(String mappingMode) {
        String normalized = mappingMode == null ? "" : mappingMode.trim().toUpperCase();
        if (!Set.of("SINGLE", "MULTI").contains(normalized)) {
            throw exception(RENTAL_CHANNEL_PRODUCT_RULE_INVALID, "型号模式仅支持 SINGLE 或 MULTI");
        }
        return normalized;
    }

    public record ValidatedRule(XianyuProductDO product,
                                String handlingPolicy,
                                String mappingMode,
                                Long singleDeviceModelId,
                                List<ValidatedSku> skuMappings) {
    }

    public record ValidatedSku(XianyuProductSkuDO sku,
                               Long deviceModelId,
                               Boolean enabled) {
    }
}
