package cn.iocoder.yudao.module.rental.service.configuration;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleImpactRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRulePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSaveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSaveRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleSkuReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductRuleStatusReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalChannelProductSkuRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductRuleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelProductSkuMappingDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalChannelRuleImpactDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductSkuDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelProductRuleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalChannelProductSkuMappingMapper;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationTrigger;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelReconciliationRunService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_PRODUCT_RULE_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_PRODUCT_RULE_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CHANNEL_PRODUCT_RULE_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CONFIGURATION_VERSION_CONFLICT;

@Service
@Slf4j
public class RentalChannelProductRuleService {

    private final RentalChannelProductRuleMapper ruleMapper;
    private final RentalChannelProductSkuMappingMapper mappingMapper;
    private final RentalChannelProductRuleValidator validator;
    private final RentalChannelOrderReconciliationTrigger reconciliationTrigger;
    private final RentalChannelReconciliationRunService reconciliationRunService;

    public RentalChannelProductRuleService(RentalChannelProductRuleMapper ruleMapper,
                                           RentalChannelProductSkuMappingMapper mappingMapper,
                                           RentalChannelProductRuleValidator validator,
                                           RentalChannelOrderReconciliationTrigger reconciliationTrigger,
                                           RentalChannelReconciliationRunService reconciliationRunService) {
        this.ruleMapper = ruleMapper;
        this.mappingMapper = mappingMapper;
        this.validator = validator;
        this.reconciliationTrigger = reconciliationTrigger;
        this.reconciliationRunService = reconciliationRunService;
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalChannelProductRuleSaveRespVO createRule(RentalChannelProductRuleSaveReqVO reqVO) {
        RentalChannelProductRuleValidator.ValidatedRule validated = validator.validate(reqVO);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RentalChannelProductRuleDO rule = toRuleDO(reqVO, validated, tenantId, 0);
        try {
            ruleMapper.insert(rule);
            insertMappings(rule.getId(), validated.skuMappings(), tenantId);
        } catch (DuplicateKeyException ex) {
            throw exception(RENTAL_CHANNEL_PRODUCT_RULE_DUPLICATE);
        }
        log.info("[rental-configuration][rule-created] tenantId={} ruleId={} shopId={} itemId={} policy={} mode={}",
                tenantId, rule.getId(), rule.getShopId(), rule.getXianyuItemId(),
                rule.getHandlingPolicy(), rule.getMappingMode());
        Long reconciliationRunId = reconciliationTrigger.afterRuleChange(
                rule.getId(), rule.getShopId(), rule.getXianyuItemId());
        return saveResponse(rule.getId(), 0,
                previewImpact(rule.getShopId(), rule.getXianyuItemId()),
                reconciliationRunId);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalChannelProductRuleSaveRespVO updateRule(RentalChannelProductRuleSaveReqVO reqVO) {
        if (reqVO.getId() == null || reqVO.getLockVersion() == null) {
            throw exception(RENTAL_CHANNEL_PRODUCT_RULE_INVALID, "更新规则必须提交规则 ID 和版本");
        }
        RentalChannelProductRuleDO current = requireRule(reqVO.getId());
        reconciliationRunService.assertNoActiveRuleRun(current.getId());
        RentalChannelProductRuleValidator.ValidatedRule validated = validator.validate(reqVO);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        int nextVersion = reqVO.getLockVersion() + 1;
        RentalChannelProductRuleDO rule = toRuleDO(reqVO, validated, tenantId, nextVersion);
        try {
            if (ruleMapper.updateByIdAndVersion(rule, tenantId, reqVO.getLockVersion()) == 0) {
                throw exception(RENTAL_CONFIGURATION_VERSION_CONFLICT);
            }
        } catch (DuplicateKeyException ex) {
            throw exception(RENTAL_CHANNEL_PRODUCT_RULE_DUPLICATE);
        }
        mappingMapper.deleteByProductRuleId(rule.getId());
        insertMappings(rule.getId(), validated.skuMappings(), tenantId);
        log.info("[rental-configuration][rule-updated] tenantId={} ruleId={} version={} shopId={} itemId={}",
                tenantId, rule.getId(), nextVersion, rule.getShopId(), rule.getXianyuItemId());
        if (!Objects.equals(current.getShopId(), rule.getShopId())
                || !Objects.equals(current.getXianyuItemId(), rule.getXianyuItemId())) {
            reconciliationTrigger.afterRuleChange(
                    rule.getId(), current.getShopId(), current.getXianyuItemId());
        }
        Long reconciliationRunId = reconciliationTrigger.afterRuleChange(
                rule.getId(), rule.getShopId(), rule.getXianyuItemId());
        return saveResponse(rule.getId(), nextVersion,
                previewImpact(rule.getShopId(), rule.getXianyuItemId()),
                reconciliationRunId);
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalChannelProductRuleSaveRespVO updateRuleStatus(
            RentalChannelProductRuleStatusReqVO reqVO) {
        RentalChannelProductRuleDO current = requireRule(reqVO.getId());
        reconciliationRunService.assertNoActiveRuleRun(current.getId());
        if (Boolean.TRUE.equals(reqVO.getEnabled())) {
            RentalChannelProductRuleSaveReqVO validationRequest = toValidationRequest(current);
            validator.validate(validationRequest);
        }
        int nextVersion = reqVO.getLockVersion() + 1;
        RentalChannelProductRuleDO update = new RentalChannelProductRuleDO();
        update.setId(reqVO.getId());
        update.setEnabled(reqVO.getEnabled());
        update.setLockVersion(nextVersion);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        update.setTenantId(tenantId);
        if (ruleMapper.updateByIdAndVersion(update, tenantId, reqVO.getLockVersion()) == 0) {
            throw exception(RENTAL_CONFIGURATION_VERSION_CONFLICT);
        }
        log.info("[rental-configuration][rule-status] tenantId={} ruleId={} enabled={} version={}",
                tenantId, reqVO.getId(), reqVO.getEnabled(), nextVersion);
        Long reconciliationRunId = reconciliationTrigger.afterRuleChange(
                current.getId(), current.getShopId(), current.getXianyuItemId());
        return saveResponse(current.getId(), nextVersion,
                previewImpact(current.getShopId(), current.getXianyuItemId()),
                reconciliationRunId);
    }

    public PageResult<RentalChannelProductRuleRespVO> getRulePage(
            RentalChannelProductRulePageReqVO reqVO) {
        PageResult<RentalChannelProductRuleDO> page = ruleMapper.selectPage(reqVO);
        Map<Long, List<RentalChannelProductSkuMappingDO>> mappingsByRule =
                groupMappings(page.getList());
        List<RentalChannelProductRuleRespVO> list = page.getList().stream()
                .map(rule -> toRuleResp(rule, mappingsByRule.getOrDefault(rule.getId(), List.of())))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    public RentalChannelProductRuleRespVO getRule(Long id) {
        RentalChannelProductRuleDO rule = requireRule(id);
        return toRuleResp(rule, mappingMapper.selectListByProductRuleId(id));
    }

    public List<RentalChannelProductSkuRespVO> getSynchronizedSkus(Long shopId,
                                                                   String xianyuItemId) {
        List<XianyuProductSkuDO> synchronizedSkus =
                validator.getSynchronizedSkus(shopId, xianyuItemId);
        RentalChannelProductRuleDO rule =
                ruleMapper.selectByShopIdAndItemId(shopId, xianyuItemId.trim());
        Map<Long, RentalChannelProductSkuMappingDO> mappingBySku = new HashMap<>();
        if (rule != null) {
            for (RentalChannelProductSkuMappingDO mapping :
                    mappingMapper.selectListByProductRuleId(rule.getId())) {
                mappingBySku.put(mapping.getProductSkuId(), mapping);
            }
        }
        return synchronizedSkus.stream().map(sku -> {
            RentalChannelProductSkuRespVO response = new RentalChannelProductSkuRespVO();
            response.setProductSkuId(sku.getId());
            response.setXgjSkuId(sku.getXgjSkuId());
            response.setXianyuSkuId(sku.getXianyuSkuId());
            response.setSkuName(sku.getSkuName());
            response.setStatus(sku.getStatus());
            RentalChannelProductSkuMappingDO mapping = mappingBySku.get(sku.getId());
            if (mapping != null) {
                response.setDeviceModelId(mapping.getDeviceModelId());
                response.setMappingEnabled(mapping.getEnabled());
            }
            return response;
        }).toList();
    }

    public RentalChannelProductRuleImpactRespVO previewImpact(Long shopId,
                                                              String xianyuItemId) {
        validator.requireExactProduct(shopId, xianyuItemId);
        RentalChannelRuleImpactDO impact = ruleMapper.selectImpact(shopId, xianyuItemId.trim());
        RentalChannelProductRuleImpactRespVO response = new RentalChannelProductRuleImpactRespVO();
        response.setScannedCount(value(impact == null ? null : impact.getScannedCount()));
        response.setWithoutInternalOrderCount(value(
                impact == null ? null : impact.getWithoutInternalOrderCount()));
        response.setMutableInternalOrderCount(value(
                impact == null ? null : impact.getMutableInternalOrderCount()));
        response.setProtectedOrderCount(value(
                impact == null ? null : impact.getProtectedOrderCount()));
        response.setReviewRequiredCount(value(
                impact == null ? null : impact.getReviewRequiredCount()));
        return response;
    }

    private RentalChannelProductRuleDO requireRule(Long id) {
        RentalChannelProductRuleDO rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw exception(RENTAL_CHANNEL_PRODUCT_RULE_NOT_EXISTS);
        }
        return rule;
    }

    private void insertMappings(Long ruleId,
                                List<RentalChannelProductRuleValidator.ValidatedSku> mappings,
                                Long tenantId) {
        for (RentalChannelProductRuleValidator.ValidatedSku validated : mappings) {
            RentalChannelProductSkuMappingDO mapping = RentalChannelProductSkuMappingDO.builder()
                    .productRuleId(ruleId)
                    .productSkuId(validated.sku().getId())
                    .xgjSkuId(validated.sku().getXgjSkuId())
                    .xianyuSkuId(validated.sku().getXianyuSkuId())
                    .deviceModelId(validated.deviceModelId())
                    .enabled(validated.enabled())
                    .lockVersion(0)
                    .build();
            mapping.setTenantId(tenantId);
            mappingMapper.insert(mapping);
        }
    }

    private static RentalChannelProductRuleDO toRuleDO(
            RentalChannelProductRuleSaveReqVO reqVO,
            RentalChannelProductRuleValidator.ValidatedRule validated,
            Long tenantId,
            Integer lockVersion) {
        RentalChannelProductRuleDO rule = RentalChannelProductRuleDO.builder()
                .id(reqVO.getId())
                .shopId(reqVO.getShopId())
                .xianyuItemId(validated.product().getXianyuItemId())
                .xgjProductId(validated.product().getXgjProductId())
                .productTitleSnapshot(validated.product().getTitle())
                .handlingPolicy(validated.handlingPolicy())
                .mappingMode(validated.mappingMode())
                .singleDeviceModelId(validated.singleDeviceModelId())
                .enabled(Boolean.TRUE.equals(reqVO.getEnabled()))
                .ruleNote(reqVO.getRuleNote() == null ? null : reqVO.getRuleNote().trim())
                .lockVersion(lockVersion)
                .build();
        rule.setTenantId(tenantId);
        return rule;
    }

    private RentalChannelProductRuleSaveReqVO toValidationRequest(
            RentalChannelProductRuleDO rule) {
        RentalChannelProductRuleSaveReqVO request = new RentalChannelProductRuleSaveReqVO();
        request.setShopId(rule.getShopId());
        request.setXianyuItemId(rule.getXianyuItemId());
        request.setHandlingPolicy(rule.getHandlingPolicy());
        request.setMappingMode(rule.getMappingMode());
        request.setSingleDeviceModelId(rule.getSingleDeviceModelId());
        request.setEnabled(true);
        List<RentalChannelProductRuleSkuReqVO> skuMappings = new ArrayList<>();
        for (RentalChannelProductSkuMappingDO mapping :
                mappingMapper.selectListByProductRuleId(rule.getId())) {
            RentalChannelProductRuleSkuReqVO mappingRequest =
                    new RentalChannelProductRuleSkuReqVO();
            mappingRequest.setProductSkuId(mapping.getProductSkuId());
            mappingRequest.setDeviceModelId(mapping.getDeviceModelId());
            mappingRequest.setEnabled(mapping.getEnabled());
            skuMappings.add(mappingRequest);
        }
        request.setSkuMappings(skuMappings);
        return request;
    }

    private Map<Long, List<RentalChannelProductSkuMappingDO>> groupMappings(
            List<RentalChannelProductRuleDO> rules) {
        Map<Long, List<RentalChannelProductSkuMappingDO>> mappingsByRule = new HashMap<>();
        List<Long> ruleIds = rules.stream().map(RentalChannelProductRuleDO::getId).toList();
        for (RentalChannelProductSkuMappingDO mapping :
                mappingMapper.selectListByProductRuleIds(ruleIds)) {
            mappingsByRule.computeIfAbsent(mapping.getProductRuleId(), key -> new ArrayList<>())
                    .add(mapping);
        }
        return mappingsByRule;
    }

    private static RentalChannelProductRuleRespVO toRuleResp(
            RentalChannelProductRuleDO rule,
            List<RentalChannelProductSkuMappingDO> mappings) {
        RentalChannelProductRuleRespVO response = new RentalChannelProductRuleRespVO();
        response.setId(rule.getId());
        response.setShopId(rule.getShopId());
        response.setXianyuItemId(rule.getXianyuItemId());
        response.setXgjProductId(rule.getXgjProductId());
        response.setProductTitleSnapshot(rule.getProductTitleSnapshot());
        response.setHandlingPolicy(rule.getHandlingPolicy());
        response.setMappingMode(rule.getMappingMode());
        response.setSingleDeviceModelId(rule.getSingleDeviceModelId());
        response.setEnabled(rule.getEnabled());
        response.setRuleNote(rule.getRuleNote());
        response.setLockVersion(rule.getLockVersion());
        response.setSkuMappings(mappings.stream().map(mapping -> {
            RentalChannelProductSkuRespVO sku = new RentalChannelProductSkuRespVO();
            sku.setProductSkuId(mapping.getProductSkuId());
            sku.setXgjSkuId(mapping.getXgjSkuId());
            sku.setXianyuSkuId(mapping.getXianyuSkuId());
            sku.setDeviceModelId(mapping.getDeviceModelId());
            sku.setMappingEnabled(mapping.getEnabled());
            return sku;
        }).toList());
        return response;
    }

    private static RentalChannelProductRuleSaveRespVO saveResponse(
            Long ruleId, Integer lockVersion, RentalChannelProductRuleImpactRespVO impact,
            Long reconciliationRunId) {
        RentalChannelProductRuleSaveRespVO response = new RentalChannelProductRuleSaveRespVO();
        response.setRuleId(ruleId);
        response.setLockVersion(lockVersion);
        response.setImpact(impact);
        response.setReconciliationRunId(reconciliationRunId);
        return response;
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }
}
