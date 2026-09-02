package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReturnRegistrationAdminServiceTest {

    private final RentalReturnRegistrationMapper registrationMapper =
            mock(RentalReturnRegistrationMapper.class);
    private final RentalReturnRegistrationDeviceMapper registrationDeviceMapper =
            mock(RentalReturnRegistrationDeviceMapper.class);
    private final XianyuOrderMapper xianyuOrderMapper = mock(XianyuOrderMapper.class);
    private final ReturnRegistrationAttachmentService attachmentService =
            mock(ReturnRegistrationAttachmentService.class);
    private final ReturnRegistrationSubmissionService submissionService =
            mock(ReturnRegistrationSubmissionService.class);
    private final ReturnRegistrationAdminService service =
            new ReturnRegistrationAdminService(
                    registrationMapper, registrationDeviceMapper, xianyuOrderMapper,
                    attachmentService, submissionService);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void pageMapsPersistedRowsAndPreservesTotal() {
        PageParam page = new PageParam().setPageNo(2).setPageSize(10);
        RentalReturnRegistrationDO row = new RentalReturnRegistrationDO()
                .setId(11L)
                .setFormNo("RR202608010001")
                .setRentalOrderId(30L)
                .setExternalOrderNo("ORDER-001")
                .setStatus("DRAFT");
        when(registrationMapper.selectPage(
                page, "DRAFT", 30L, "ORDER", "A6", null, null))
                .thenReturn(new PageResult<>(List.of(row), 21L));

        PageResult<ReturnRegistrationModels.AdminRow> result =
                service.page(page, "DRAFT", 30L, "ORDER", "A6", null, null);

        assertEquals(21L, result.getTotal());
        assertEquals("RR202608010001", result.getList().get(0).formNo());
    }

    @Test
    void detailReturnsFullAuthorizedCustomerAndAttachmentData() {
        TenantContextHolder.setTenantId(9L);
        RentalReturnRegistrationDO registration = new RentalReturnRegistrationDO()
                .setId(11L)
                .setFormNo("RR202608010001")
                .setRentalOrderId(30L)
                .setChannelOrderId(40L)
                .setExternalOrderNo("ORDER-001")
                .setSenderMobile("13900139000")
                .setStatus("REVIEW_REQUIRED");
        registration.setTenantId(9L);
        when(registrationMapper.selectById(11L)).thenReturn(registration);
        when(registrationDeviceMapper.selectListByRegistrationId(11L)).thenReturn(List.of());
        when(xianyuOrderMapper.selectById(40L)).thenReturn(XianyuOrderDO.builder()
                .id(40L)
                .receiverName("测试客户")
                .receiverMobile("13800138000")
                .receiverAddress("测试地址 1 号")
                .build());
        when(attachmentService.listForAdmin(11L)).thenReturn(List.of());

        ReturnRegistrationModels.AdminDetail detail = service.get(11L);

        assertEquals("测试客户", detail.customer().name());
        assertEquals("13800138000", detail.customer().mobile());
        assertEquals("测试地址 1 号", detail.customer().address());
        assertEquals("13900139000", detail.senderMobile());
    }

    @Test
    void revokeAndRejectReviewAreSingleStateTransitions() {
        RentalReturnRegistrationDO draft =
                new RentalReturnRegistrationDO().setId(11L).setStatus("DRAFT");
        when(registrationMapper.selectByIdForUpdate(11L)).thenReturn(draft);
        service.revoke(11L);
        assertEquals("REVOKED", draft.getStatus());

        RentalReturnRegistrationDO review =
                new RentalReturnRegistrationDO().setId(12L).setStatus("REVIEW_REQUIRED");
        when(registrationMapper.selectByIdForUpdate(12L)).thenReturn(review);
        service.review(12L, false, "序列号待核对", 99L);
        assertEquals("REJECTED", review.getStatus());
        assertEquals(99L, review.getReviewerId());
    }
}
