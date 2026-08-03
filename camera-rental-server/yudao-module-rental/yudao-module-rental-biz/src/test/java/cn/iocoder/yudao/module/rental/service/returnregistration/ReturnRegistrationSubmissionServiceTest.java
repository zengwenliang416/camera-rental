package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationDeviceMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReturnRegistrationSubmissionServiceTest {

    private final ReturnRegistrationResolver resolver = mock(ReturnRegistrationResolver.class);
    private final RentalReturnRegistrationMapper registrationMapper =
            mock(RentalReturnRegistrationMapper.class);
    private final RentalReturnRegistrationDeviceMapper registrationDeviceMapper =
            mock(RentalReturnRegistrationDeviceMapper.class);
    private final ReturnRegistrationAttachmentService attachmentService =
            mock(ReturnRegistrationAttachmentService.class);
    private final RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
    private final RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
    private final RentalDeliveryService deliveryService = mock(RentalDeliveryService.class);
    private final ReturnRegistrationSubmissionService service =
            new ReturnRegistrationSubmissionService(
                    resolver, new ReturnSerialNormalizer(), attachmentService,
                    registrationMapper, registrationDeviceMapper, assignmentMapper,
                    deviceMapper, deliveryService);
    private RentalReturnRegistrationDO registration;

    @BeforeEach
    void setUp() throws Exception {
        registration = new RentalReturnRegistrationDO()
                .setId(11L)
                .setFormNo("RR202608010001")
                .setRentalOrderId(30L)
                .setChannelOrderId(40L)
                .setExternalOrderNo("ORDER-001")
                .setStatus(ReturnRegistrationStatusEnum.DRAFT.name())
                .setExpiresAt(LocalDateTime.now().plusDays(1));
        registration.setTenantId(9L);
        when(resolver.require("token")).thenReturn(registration);
        when(resolver.publicStatus(registration)).thenReturn(ReturnRegistrationStatusEnum.DRAFT.name());
        when(resolver.execute(any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(1);
            return callable.call();
        });
        when(registrationMapper.selectByIdForUpdate(11L)).thenReturn(registration);
        when(attachmentService.listConfirmedIds(11L)).thenReturn(List.of(101L, 102L));
    }

    @Test
    void dispatchedDeviceCanRegisterReturnBeforeWarehouseCheckIn() {
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(50L).rentalOrderId(30L).rentalOrderItemId(60L).deviceId(70L)
                .status("DISPATCHED").build();
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(70L).deviceNo("P4-01").legacyDeviceNo("A6-08-4L5H")
                .serialNumber("SN-A6-08-4L5H").build();
        when(assignmentMapper.selectActiveListByRentalOrderId(30L)).thenReturn(List.of(assignment));
        when(deviceMapper.selectById(70L)).thenReturn(device);
        when(deliveryService.createOrReuseLocalOnly(any())).thenReturn(
                new RentalDeliveryResult(80L, true, "READY", "DISABLED",
                        "READY", "SF1****0001", null, List.of()));

        ReturnRegistrationModels.Receipt receipt = service.submit("token", validSubmission());

        assertEquals(ReturnRegistrationStatusEnum.ACCEPTED.name(), receipt.status());
        assertEquals(80L, receipt.deliveryId());
        verify(deliveryService).createOrReuseLocalOnly(any());
        verify(registrationMapper).updateById(registration);
        verify(registrationMapper).selectByIdForUpdate(11L);
        verify(assignmentMapper, never()).updateById(any(RentalDeviceAssignmentDO.class));
        verify(deviceMapper, never()).updateById(any(RentalDeviceDO.class));
    }

    @Test
    void unknownSerialPersistsReviewStateWithoutDelivery() {
        when(assignmentMapper.selectActiveListByRentalOrderId(30L)).thenReturn(List.of());

        ReturnRegistrationModels.Receipt receipt = service.submit("token", validSubmission());

        assertEquals(ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name(), receipt.status());
        verify(deliveryService, never()).createOrReuseLocalOnly(any());
    }

    @Test
    void unconvertedOrderPersistsSimpleSubmissionForReview() {
        registration.setRentalOrderId(null);

        ReturnRegistrationModels.Receipt receipt =
                service.submitSimple("token", "P4-01", "SF1000000001", List.of());

        assertEquals(ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name(), receipt.status());
        verify(assignmentMapper, never()).selectActiveListByRentalOrderId(any());
        verify(deliveryService, never()).createOrReuseLocalOnly(any());
    }

    @Test
    void unregisteredMachineCodePersistsWithoutCreatingInventoryDevice() {
        registration.setRentalOrderId(null)
                .setChannelOrderId(null)
                .setExternalOrderNo("");

        ReturnRegistrationModels.Receipt receipt =
                service.submitSimple("token", "P4-18", "1234567567", List.of());

        assertEquals(ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name(), receipt.status());
        ArgumentCaptor<RentalReturnRegistrationDeviceDO> detailCaptor =
                ArgumentCaptor.forClass(RentalReturnRegistrationDeviceDO.class);
        verify(registrationDeviceMapper).insert(detailCaptor.capture());
        assertEquals("P4-18", detailCaptor.getValue().getSubmittedSerial());
        assertEquals("P4-18", detailCaptor.getValue().getNormalizedSerial());
        assertNull(detailCaptor.getValue().getDeviceId());
        assertNull(detailCaptor.getValue().getAssignmentId());
        verifyNoInteractions(deviceMapper, assignmentMapper);
        verify(deliveryService, never()).createOrReuseLocalOnly(any());
    }

    @Test
    void repeatSubmissionReturnsOriginalReceiptWithoutSideEffects() {
        registration.setStatus(ReturnRegistrationStatusEnum.ACCEPTED.name())
                .setDeliveryId(80L)
                .setSubmittedAt(LocalDateTime.now());

        ReturnRegistrationModels.Receipt receipt = service.submit("token", validSubmission());

        assertEquals(80L, receipt.deliveryId());
        verify(attachmentService, never()).validateForSubmission(any(), any());
        verify(deliveryService, never()).createOrReuseLocalOnly(any());
        verify(registrationMapper, never()).updateById(any(RentalReturnRegistrationDO.class));
    }

    @Test
    void simpleSubmissionDoesNotRequirePhotos() {
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(50L).rentalOrderId(30L).rentalOrderItemId(60L).deviceId(70L).build();
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(70L).deviceNo("P4-01").legacyDeviceNo("A6-08-4L5H")
                .serialNumber("SN-A6-08-4L5H").build();
        when(assignmentMapper.selectActiveListByRentalOrderId(30L)).thenReturn(List.of(assignment));
        when(deviceMapper.selectById(70L)).thenReturn(device);
        when(deliveryService.createOrReuseLocalOnly(any())).thenReturn(
                new RentalDeliveryResult(80L, true, "MAPPING_REQUIRED", "DISABLED",
                        "READY", "SF1****0001", null, List.of()));

        ReturnRegistrationModels.Receipt receipt =
                service.submitSimple("token", "p4 － 01", "SF1000000001", List.of());

        assertEquals(ReturnRegistrationStatusEnum.ACCEPTED.name(), receipt.status());
        assertEquals(80L, receipt.deliveryId());
        verify(attachmentService, never()).validateForSubmission(any(), any());
        verify(attachmentService).validateOptionalForSubmission(registration, List.of());
    }

    @Test
    void simpleSubmissionValidatesSelectedPhotosBeforeWriting() {
        registration.setRentalOrderId(null);

        ReturnRegistrationModels.Receipt receipt =
                service.submitSimple("token", "P4-01", "SF1000000001",
                        List.of(101L, 102L));

        assertEquals(ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name(), receipt.status());
        verify(attachmentService).validateOptionalForSubmission(
                registration, List.of(101L, 102L));
    }

    @Test
    void invalidOptionalPhotosBlockSimpleSubmission() {
        org.mockito.Mockito.doThrow(new RuntimeException("附件无效"))
                .when(attachmentService)
                .validateOptionalForSubmission(registration, List.of(101L));

        assertThrows(RuntimeException.class, () ->
                service.submitSimple("token", "P4-01", "SF1000000001",
                        List.of(101L)));

        verify(registrationDeviceMapper, never()).deleteByRegistrationId(any());
        verify(registrationMapper, never()).updateById(any(RentalReturnRegistrationDO.class));
        verify(deliveryService, never()).createOrReuseLocalOnly(any());
    }

    @Test
    void acceptingReviewRevalidatesAndCreatesOnlyOneDelivery() {
        registration.setStatus(ReturnRegistrationStatusEnum.REVIEW_REQUIRED.name())
                .setCarrierCode("SHUNFENG")
                .setCarrierName("顺丰速运")
                .setWaybillNo("SF1000000001")
                .setSubmittedAt(LocalDateTime.now());
        when(registrationDeviceMapper.selectListByRegistrationId(11L))
                .thenReturn(List.of(new cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration
                        .RentalReturnRegistrationDeviceDO().setSubmittedSerial("P4-01")));
        RentalDeviceAssignmentDO assignment = RentalDeviceAssignmentDO.builder()
                .id(50L).rentalOrderId(30L).rentalOrderItemId(60L).deviceId(70L).build();
        RentalDeviceDO device = RentalDeviceDO.builder()
                .id(70L).deviceNo("P4-01").legacyDeviceNo("A6-08-4L5H")
                .serialNumber("SN-A6-08-4L5H").build();
        when(assignmentMapper.selectActiveListByRentalOrderId(30L)).thenReturn(List.of(assignment));
        when(deviceMapper.selectById(70L)).thenReturn(device);
        when(deliveryService.createOrReuseLocalOnly(any())).thenReturn(
                new RentalDeliveryResult(80L, true, "READY", "DISABLED",
                        "READY", "SF1****0001", null, List.of()));

        ReturnRegistrationModels.Receipt receipt = service.acceptReview(11L);

        assertEquals(ReturnRegistrationStatusEnum.ACCEPTED.name(), receipt.status());
        assertEquals(80L, receipt.deliveryId());
        verify(attachmentService, never()).validateForSubmission(any(), any());
        verify(deliveryService, times(1)).createOrReuseLocalOnly(any());
        verify(assignmentMapper, never()).updateById(any(RentalDeviceAssignmentDO.class));
        verify(deviceMapper, never()).updateById(any(RentalDeviceDO.class));
    }

    @Test
    void repeatedReviewCannotCreateAnotherDelivery() {
        registration.setStatus(ReturnRegistrationStatusEnum.ACCEPTED.name())
                .setDeliveryId(80L);

        assertThrows(RuntimeException.class, () -> service.acceptReview(11L));

        verify(deliveryService, never()).createOrReuseLocalOnly(any());
        verify(registrationDeviceMapper, never()).deleteByRegistrationId(any());
    }

    private ReturnRegistrationModels.Submission validSubmission() {
        return new ReturnRegistrationModels.Submission(
                "ORDER-001", "SHUNFENG", "顺丰速运", "SF1000000001",
                LocalDate.of(2026, 8, 1), List.of("P4-01"),
                List.of(), null, "submit-key-1");
    }

}
