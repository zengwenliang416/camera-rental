package cn.iocoder.yudao.module.rental.service;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductMappingDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMappingMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.rental.RentalManualReviewStatusEnum.OPEN;

/**
 * Converts one durable channel order after all local conversion prerequisites are explicitly satisfied.
 * Hermes-aligned default path is automatic conversion after order-detail persistence.
 */
@Service
@Slf4j
public class XianyuRentalConversionServiceImpl implements XianyuRentalConversionService {

    static final String CHANNEL_SOURCE_TYPE = "XIANYU";
    static final String REVIEW_SOURCE_TYPE = "XIANYU_ORDER";
    static final String REVIEW_TYPE = "ORDER_CONVERSION";
    static final String MAPPING_STATUS_MAPPED = "MAPPED";
    static final String CONVERSION_STATUS_CONVERTED = "CONVERTED";
    static final String CONVERSION_STATUS_REVIEW_REQUIRED = "REVIEW_REQUIRED";
    static final String CONVERSION_STATUS_CLOSED = "CLOSED";
    static final String RENTAL_STATUS_PENDING_ALLOCATION = "PENDING_ALLOCATION";
    static final String SYSTEM_OPERATOR = "system";

    private final XianyuOrderMapper xianyuOrderMapper;
    private final XianyuProductMappingMapper productMappingMapper;
    private final RentalOrderMapper rentalOrderMapper;
    private final RentalOrderItemMapper rentalOrderItemMapper;
    private final RentalManualReviewMapper manualReviewMapper;
    private final SellerRemarkRentalPeriodParser periodParser;

    public XianyuRentalConversionServiceImpl(XianyuOrderMapper xianyuOrderMapper,
                                             XianyuProductMappingMapper productMappingMapper,
                                             RentalOrderMapper rentalOrderMapper,
                                             RentalOrderItemMapper rentalOrderItemMapper,
                                             RentalManualReviewMapper manualReviewMapper,
                                             SellerRemarkRentalPeriodParser periodParser) {
        this.xianyuOrderMapper = xianyuOrderMapper;
        this.productMappingMapper = productMappingMapper;
        this.rentalOrderMapper = rentalOrderMapper;
        this.rentalOrderItemMapper = rentalOrderItemMapper;
        this.manualReviewMapper = manualReviewMapper;
        this.periodParser = periodParser;
    }

    @Override
    public void autoConvertAfterPersist(Long channelOrderId) {
        if (channelOrderId == null) {
            return;
        }
        try {
            RentalConversionResult result = convert(channelOrderId);
            log.info("[xianyu][auto-convert] channelOrderId={} status={} rentalOrderId={} reviewId={} reason={}",
                    channelOrderId, result.status(), result.rentalOrderId(), result.reviewId(), result.reasonCode());
        } catch (RuntimeException exception) {
            // Never break channel sync / webhook ingestion on conversion failures.
            log.warn("[xianyu][auto-convert] channelOrderId={} failed: {}",
                    channelOrderId, exception.toString());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RentalConversionResult convert(Long channelOrderId) {
        Objects.requireNonNull(channelOrderId, "channelOrderId");
        XianyuOrderDO source = xianyuOrderMapper.selectByIdForUpdate(channelOrderId);
        if (source == null) {
            // Missing or cross-tenant id: do not leak existence; return business not-found (not 500).
            throw exception(XIANYU_ORDER_NOT_EXISTS);
        }
        if (CONVERSION_STATUS_CLOSED.equals(source.getConversionStatus())) {
            // Closed channel rows stay closed; Hermes does not re-open them automatically.
            SellerRemarkRentalPeriod closedPeriod = periodParser.parse(source.getSellerRemark(), sourceDate(source));
            source.setRemarkParseVersion(closedPeriod.version());
            source.setRemarkParseStatus(closedPeriod.status());
            xianyuOrderMapper.updateById(source);
            return RentalConversionResult.reviewRequired(null, "CLOSED");
        }
        if (source.getRentalOrderId() != null) {
            // Already linked: still refresh remark parse metadata (Hermes re-reads remarks on updates).
            SellerRemarkRentalPeriod linkedPeriod = periodParser.parse(source.getSellerRemark(), sourceDate(source));
            source.setRemarkParseVersion(linkedPeriod.version());
            source.setRemarkParseStatus(linkedPeriod.status());
            if (!CONVERSION_STATUS_CONVERTED.equals(source.getConversionStatus())) {
                source.setConversionStatus(CONVERSION_STATUS_CONVERTED);
            }
            xianyuOrderMapper.updateById(source);
            return RentalConversionResult.converted(source.getRentalOrderId());
        }

        SellerRemarkRentalPeriod period = periodParser.parse(source.getSellerRemark(), sourceDate(source));
        source.setRemarkParseVersion(period.version());
        source.setRemarkParseStatus(period.status());
        if (source.getPayAmount() == null || source.getPayAmount() < 0) {
            return requireReview(source, "INVALID_PAY_AMOUNT");
        }
        if (!period.isSuccess()) {
            return requireReview(source, period.reasonCode());
        }
        XianyuProductMappingDO mapping = productMappingMapper.selectByShopProductSkuForUpdate(source.getShopId(),
                source.getExternalProductId(), externalSkuId(source));
        if (mapping == null || !MAPPING_STATUS_MAPPED.equals(mapping.getMappingStatus())
                || !StringUtils.hasText(mapping.getEquipmentModelCode())) {
            return requireReview(source, "PRODUCT_MAPPING_REQUIRED");
        }

        RentalOrderDO existing = rentalOrderMapper.selectBySourceForUpdate(CHANNEL_SOURCE_TYPE, sourceIdentity(source));
        if (existing != null) {
            source.setRentalOrderId(existing.getId());
            source.setConversionStatus(CONVERSION_STATUS_CONVERTED);
            xianyuOrderMapper.updateById(source);
            return RentalConversionResult.converted(existing.getId());
        }

        RentalOrderDO rentalOrder = RentalOrderDO.builder()
                .orderNo("XY-" + String.format("%019d", source.getId()))
                .sourceType(CHANNEL_SOURCE_TYPE)
                .sourceOrderId(sourceIdentity(source))
                .channelOrderId(source.getId())
                .status(RENTAL_STATUS_PENDING_ALLOCATION)
                .rentAmount(source.getPayAmount())
                .refundAmount(0L)
                .billableStartDate(period.billableStartDate())
                .billableEndDate(period.billableEndDate())
                .conversionVersion(period.version())
                .build();
        rentalOrderMapper.insert(rentalOrder);
        rentalOrderItemMapper.insert(RentalOrderItemDO.builder()
                .rentalOrderId(rentalOrder.getId())
                .equipmentModelCode(mapping.getEquipmentModelCode())
                .sourceProductId(source.getExternalProductId())
                .sourceSkuId(externalSkuId(source))
                .quantity(1)
                .rentAmount(source.getPayAmount())
                .billableStartDate(period.billableStartDate())
                .billableEndDate(period.billableEndDate())
                .build());
        source.setRentalOrderId(rentalOrder.getId());
        source.setConversionStatus(CONVERSION_STATUS_CONVERTED);
        xianyuOrderMapper.updateById(source);
        return RentalConversionResult.converted(rentalOrder.getId());
    }

    private RentalConversionResult requireReview(XianyuOrderDO source, String reasonCode) {
        source.setConversionStatus(CONVERSION_STATUS_REVIEW_REQUIRED);
        xianyuOrderMapper.updateById(source);
        RentalManualReviewDO review = manualReviewMapper.selectBySourceAndReviewTypeForUpdate(
                REVIEW_SOURCE_TYPE, source.getId().toString(), REVIEW_TYPE);
        if (review == null) {
            review = RentalManualReviewDO.builder()
                    .reviewType(REVIEW_TYPE)
                    .sourceType(REVIEW_SOURCE_TYPE)
                    .sourceIdentifier(source.getId().toString())
                    .status(OPEN.getStatus())
                    .reasonCode(reasonCode)
                    .reasonMessage("Channel order conversion requires operator review")
                    .build();
            review.setCreator(SYSTEM_OPERATOR);
            review.setUpdater(SYSTEM_OPERATOR);
            manualReviewMapper.insert(review);
        } else if (!Objects.equals(review.getReasonCode(), reasonCode)
                || !OPEN.getStatus().equals(review.getStatus())) {
            review.setStatus(OPEN.getStatus());
            review.setReasonCode(reasonCode);
            review.setReasonMessage("Channel order conversion requires operator review");
            review.setResolutionNote(null);
            review.setResolvedBy(null);
            review.setResolvedAt(null);
            review.setUpdater(SYSTEM_OPERATOR);
            manualReviewMapper.updateById(review);
        }
        return RentalConversionResult.reviewRequired(review.getId(), reasonCode);
    }

    private LocalDate sourceDate(XianyuOrderDO source) {
        return source.getSourceCreatedAt() == null ? null : source.getSourceCreatedAt().toLocalDate();
    }

    private String externalSkuId(XianyuOrderDO source) {
        return source.getExternalSkuId() == null ? "" : source.getExternalSkuId();
    }

    private String sourceIdentity(XianyuOrderDO source) {
        return source.getShopId() + ":" + source.getExternalOrderId();
    }

}
