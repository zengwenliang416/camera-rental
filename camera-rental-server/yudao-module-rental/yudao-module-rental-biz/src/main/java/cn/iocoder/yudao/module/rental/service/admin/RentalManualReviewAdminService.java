package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualReviewRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.service.RentalConversionResult;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_REVIEW_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_REVIEW_PREREQUISITES_UNRESOLVED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_REVIEW_STATUS_INVALID;
import static cn.iocoder.yudao.module.rental.enums.rental.RentalManualReviewStatusEnum.CLOSED;
import static cn.iocoder.yudao.module.rental.enums.rental.RentalManualReviewStatusEnum.OPEN;
import static cn.iocoder.yudao.module.rental.enums.rental.RentalManualReviewStatusEnum.RESOLVED;

@Service
public class RentalManualReviewAdminService {

    private static final String ORDER_CONVERSION_REVIEW_TYPE = "ORDER_CONVERSION";
    private static final String XIANYU_ORDER_SOURCE_TYPE = "XIANYU_ORDER";
    private static final String CONVERSION_STATUS_CONVERTED = "CONVERTED";
    private static final String CONVERSION_STATUS_CLOSED = "CLOSED";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final RentalManualReviewMapper reviewMapper;
    private final AdminUserApi adminUserApi;
    private final XianyuRentalConversionService conversionService;
    private final XianyuOrderMapper xianyuOrderMapper;
    private final Clock clock;

    @Autowired
    public RentalManualReviewAdminService(RentalManualReviewMapper reviewMapper, AdminUserApi adminUserApi,
                                          XianyuRentalConversionService conversionService,
                                          XianyuOrderMapper xianyuOrderMapper) {
        this(reviewMapper, adminUserApi, conversionService, xianyuOrderMapper, Clock.system(BUSINESS_ZONE));
    }

    RentalManualReviewAdminService(RentalManualReviewMapper reviewMapper, AdminUserApi adminUserApi,
                                   XianyuRentalConversionService conversionService,
                                   XianyuOrderMapper xianyuOrderMapper, Clock clock) {
        this.reviewMapper = reviewMapper;
        this.adminUserApi = adminUserApi;
        this.conversionService = conversionService;
        this.xianyuOrderMapper = xianyuOrderMapper;
        this.clock = clock;
    }

    public PageResult<RentalManualReviewRespVO> getReviewPage(String status, PageParam pageParam) {
        PageResult<RentalManualReviewDO> page = reviewMapper.selectPage(pageParam,
                new LambdaQueryWrapperX<RentalManualReviewDO>()
                        .eqIfPresent(RentalManualReviewDO::getStatus, status)
                        .orderByDesc(RentalManualReviewDO::getId));
        Set<Long> resolvedByIds = page.getList().stream()
                .map(RentalManualReviewDO::getResolvedBy)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = resolvedByIds.isEmpty()
                ? Map.of() : adminUserApi.getUserMap(resolvedByIds);
        List<RentalManualReviewRespVO> list = page.getList().stream()
                .map(review -> toVo(review, userMap))
                .collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    @Transactional(rollbackFor = Exception.class)
    public void resolveReview(Long id, String resolutionNote, Long userId) {
        RentalManualReviewDO review = getOpenReview(id);
        if (isXianyuOrderConversion(review)) {
            RentalConversionResult result = conversionService.convert(parseSourceId(review));
            if (!CONVERSION_STATUS_CONVERTED.equals(result.status())) {
                throw exception(RENTAL_MANUAL_REVIEW_PREREQUISITES_UNRESOLVED, result.reasonCode());
            }
        }
        updateReview(review.getId(), resolutionNote, userId, RESOLVED.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public void closeReview(Long id, String resolutionNote, Long userId) {
        RentalManualReviewDO review = getOpenReview(id);
        if (isXianyuOrderConversion(review)) {
            XianyuOrderDO source = xianyuOrderMapper.selectByIdForUpdate(parseSourceId(review));
            if (source != null && source.getRentalOrderId() == null) {
                source.setConversionStatus(CONVERSION_STATUS_CLOSED);
                xianyuOrderMapper.updateById(source);
            }
        }
        updateReview(review.getId(), resolutionNote, userId, CLOSED.getStatus());
    }

    private RentalManualReviewDO getOpenReview(Long id) {
        RentalManualReviewDO review = reviewMapper.selectByIdForUpdate(id);
        if (review == null) {
            throw exception(RENTAL_MANUAL_REVIEW_NOT_EXISTS);
        }
        if (!OPEN.getStatus().equals(review.getStatus())) {
            throw exception(RENTAL_MANUAL_REVIEW_STATUS_INVALID, review.getStatus());
        }
        return review;
    }

    private void updateReview(Long id, String resolutionNote, Long userId, String targetStatus) {
        reviewMapper.updateById(RentalManualReviewDO.builder()
                .id(id)
                .status(targetStatus)
                .resolutionNote(resolutionNote)
                .resolvedBy(userId)
                .resolvedAt(LocalDateTime.now(clock))
                .build());
    }

    private boolean isXianyuOrderConversion(RentalManualReviewDO review) {
        return ORDER_CONVERSION_REVIEW_TYPE.equals(review.getReviewType())
                && XIANYU_ORDER_SOURCE_TYPE.equals(review.getSourceType());
    }

    private Long parseSourceId(RentalManualReviewDO review) {
        try {
            return Long.valueOf(review.getSourceIdentifier());
        } catch (NumberFormatException exception) {
            throw exception(RENTAL_MANUAL_REVIEW_PREREQUISITES_UNRESOLVED, "INVALID_SOURCE_IDENTIFIER");
        }
    }

    private RentalManualReviewRespVO toVo(RentalManualReviewDO review, Map<Long, AdminUserRespDTO> userMap) {
        RentalManualReviewRespVO vo = new RentalManualReviewRespVO();
        vo.setId(review.getId());
        vo.setReviewType(review.getReviewType());
        vo.setSourceType(review.getSourceType());
        vo.setSourceIdentifier(review.getSourceIdentifier());
        vo.setStatus(review.getStatus());
        vo.setReasonCode(review.getReasonCode());
        vo.setReasonMessage(review.getReasonMessage());
        vo.setResolutionNote(review.getResolutionNote());
        vo.setResolvedBy(review.getResolvedBy());
        AdminUserRespDTO resolvedBy = review.getResolvedBy() == null ? null : userMap.get(review.getResolvedBy());
        vo.setResolvedByName(resolvedBy != null ? resolvedBy.getNickname() : null);
        vo.setResolvedAt(review.getResolvedAt());
        return vo;
    }

}
