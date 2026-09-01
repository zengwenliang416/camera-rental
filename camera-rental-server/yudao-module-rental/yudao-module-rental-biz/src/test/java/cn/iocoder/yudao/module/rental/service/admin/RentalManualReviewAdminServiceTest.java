package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualReviewRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationResult;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderReconciliationService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_REVIEW_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_REVIEW_PREREQUISITES_UNRESOLVED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_MANUAL_REVIEW_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalManualReviewAdminServiceTest {

    private RentalManualReviewMapper reviewMapper;
    private AdminUserApi adminUserApi;
    private RentalChannelOrderReconciliationService reconciliationService;
    private XianyuOrderMapper xianyuOrderMapper;
    private RentalManualReviewAdminService service;

    @BeforeEach
    void setUp() {
        reviewMapper = mock(RentalManualReviewMapper.class);
        adminUserApi = mock(AdminUserApi.class);
        reconciliationService = mock(RentalChannelOrderReconciliationService.class);
        xianyuOrderMapper = mock(XianyuOrderMapper.class);
        service = new RentalManualReviewAdminService(
                reviewMapper, adminUserApi, reconciliationService, xianyuOrderMapper,
                Clock.fixed(Instant.parse("2026-07-24T04:00:00Z"), ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void getReviewPageShouldExposeResolvedOperatorName() {
        RentalManualReviewDO review = RentalManualReviewDO.builder()
                .id(1L)
                .status("RESOLVED")
                .resolvedBy(9L)
                .build();
        when(reviewMapper.selectPage(any(PageParam.class), any()))
                .thenReturn(new PageResult<>(List.of(review), 1L));
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(9L);
        user.setNickname("Operator");
        when(adminUserApi.getUserMap(Set.of(9L))).thenReturn(Map.of(9L, user));

        PageResult<RentalManualReviewRespVO> result = service.getReviewPage("RESOLVED", new PageParam());

        assertEquals(1, result.getList().size());
        assertEquals(9L, result.getList().get(0).getResolvedBy());
        assertEquals("Operator", result.getList().get(0).getResolvedByName());
    }

    @Test
    void getReviewPageShouldHandleOpenReviewWithoutResolvedOperator() {
        RentalManualReviewDO review = RentalManualReviewDO.builder()
                .id(1L)
                .status("OPEN")
                .build();
        when(reviewMapper.selectPage(any(PageParam.class), any()))
                .thenReturn(new PageResult<>(List.of(review), 1L));

        PageResult<RentalManualReviewRespVO> result = service.getReviewPage(null, new PageParam());

        assertEquals(1, result.getList().size());
        assertEquals("OPEN", result.getList().get(0).getStatus());
        verify(adminUserApi, never()).getUserMap(any());
    }

    @Test
    void resolveReviewShouldPersistOperatorAndResolution() {
        when(reviewMapper.selectByIdForUpdate(1L)).thenReturn(orderConversionReview());
        when(reconciliationService.reconcile(10L)).thenReturn(
                new RentalChannelOrderReconciliationResult(
                        "CONVERTED", 20L, null, null, "READY", true));

        service.resolveReview(1L, "Product mapping completed", 9L);

        RentalManualReviewDO update = captureUpdate();
        assertEquals(1L, update.getId());
        assertEquals("RESOLVED", update.getStatus());
        assertEquals("Product mapping completed", update.getResolutionNote());
        assertEquals(9L, update.getResolvedBy());
        assertEquals(LocalDateTime.of(2026, 7, 24, 12, 0), update.getResolvedAt());
    }

    @Test
    void closeReviewShouldPersistOperatorAndResolution() {
        when(reviewMapper.selectByIdForUpdate(1L)).thenReturn(orderConversionReview());
        XianyuOrderDO source = XianyuOrderDO.builder().id(10L).conversionStatus("REVIEW_REQUIRED").build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);

        service.closeReview(1L, "Duplicate exception", 9L);

        RentalManualReviewDO update = captureUpdate();
        assertEquals("CLOSED", update.getStatus());
        assertEquals("Duplicate exception", update.getResolutionNote());
        assertEquals(9L, update.getResolvedBy());
        assertEquals(LocalDateTime.of(2026, 7, 24, 12, 0), update.getResolvedAt());
        assertEquals("CLOSED", source.getConversionStatus());
        verify(xianyuOrderMapper).updateById(source);
    }

    @Test
    void resolveReviewShouldStayOpenWhenConversionPrerequisitesStillFail() {
        when(reviewMapper.selectByIdForUpdate(1L)).thenReturn(orderConversionReview());
        when(reconciliationService.reconcile(10L))
                .thenReturn(RentalChannelOrderReconciliationResult.reviewRequired(
                        1L, "PRODUCT_MAPPING_REQUIRED"));

        assertServiceException(() -> service.resolveReview(1L, "Mapping completed", 9L),
                RENTAL_MANUAL_REVIEW_PREREQUISITES_UNRESOLVED, "PRODUCT_MAPPING_REQUIRED");

        verify(reviewMapper, never()).updateById(any(RentalManualReviewDO.class));
    }

    @Test
    void resolveReviewShouldRejectMissingRecord() {
        when(reviewMapper.selectByIdForUpdate(1L)).thenReturn(null);

        assertServiceException(() -> service.resolveReview(1L, "Resolved", 9L),
                RENTAL_MANUAL_REVIEW_NOT_EXISTS);
    }

    @Test
    void resolveReviewShouldRejectAlreadyHandledRecord() {
        RentalManualReviewDO review = openReview();
        review.setStatus("RESOLVED");
        when(reviewMapper.selectByIdForUpdate(1L)).thenReturn(review);

        assertServiceException(() -> service.resolveReview(1L, "Resolved again", 9L),
                RENTAL_MANUAL_REVIEW_STATUS_INVALID, "RESOLVED");
    }

    private RentalManualReviewDO openReview() {
        return RentalManualReviewDO.builder()
                .id(1L)
                .status("OPEN")
                .build();
    }

    private RentalManualReviewDO orderConversionReview() {
        RentalManualReviewDO review = openReview();
        review.setReviewType("ORDER_CONVERSION");
        review.setSourceType("XIANYU_ORDER");
        review.setSourceIdentifier("10");
        return review;
    }

    private RentalManualReviewDO captureUpdate() {
        ArgumentCaptor<RentalManualReviewDO> captor = ArgumentCaptor.forClass(RentalManualReviewDO.class);
        verify(reviewMapper).updateById(captor.capture());
        return captor.getValue();
    }

}
