package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileConfirmedUploadRespDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignedUploadRespDTO;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationAttachmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationAttachmentMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReturnRegistrationAttachmentServiceTest {

    private final ReturnRegistrationResolver resolver = mock(ReturnRegistrationResolver.class);
    private final RentalReturnRegistrationAttachmentMapper attachmentMapper =
            mock(RentalReturnRegistrationAttachmentMapper.class);
    private final FileApi fileApi = mock(FileApi.class);
    private final ReturnRegistrationAttachmentService service =
            new ReturnRegistrationAttachmentService(resolver, attachmentMapper, fileApi);

    @Test
    void rejectsUnsupportedContentTypeBeforeIssuingUploadUrl() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);

        assertThrows(RuntimeException.class,
                () -> service.authorize("token", "DEVICE_EXTERIOR", "proof.pdf", "application/pdf"));

        verify(fileApi, never()).presignPutUrl(any(), any(), any());
        verify(attachmentMapper, never()).insert(any(RentalReturnRegistrationAttachmentDO.class));
    }

    @Test
    void issuesOnlyFiveMinuteUploadAuthorizationUnderRegistrationOwnedPath() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        when(fileApi.presignPutUrl(any(), any(), any()))
                .thenReturn(new FilePresignedUploadRespDTO(
                        8L,
                        "return-registration/tenant-9/registration-11/device_exterior/photo.jpg",
                        "signed-upload", "private-object"));
        org.mockito.Mockito.doAnswer(invocation -> {
            RentalReturnRegistrationAttachmentDO value = invocation.getArgument(0);
            value.setId(21L);
            return 1;
        }).when(attachmentMapper).insert(any(RentalReturnRegistrationAttachmentDO.class));

        ReturnRegistrationModels.UploadAuthorization authorization =
                service.authorize("token", "DEVICE_EXTERIOR", "photo.jpg", "image/jpeg");

        assertEquals(300, authorization.expiresInSeconds());
        verify(fileApi).presignPutUrl(
                any(), org.mockito.ArgumentMatchers.eq(
                        "return-registration/tenant-9/registration-11/device_exterior"),
                org.mockito.ArgumentMatchers.eq(300));
    }

    @Test
    void rejectsAuthorizationWhenTotalPhotoLimitIsReached() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        when(attachmentMapper.countByRegistration(11L)).thenReturn(20L);

        assertThrows(RuntimeException.class,
                () -> service.authorize(
                        "token", "DEVICE_EXTERIOR", "photo.jpg", "image/jpeg"));

        verify(fileApi, never()).presignPutUrl(any(), any(), any());
    }

    @Test
    void simpleReturnCategoryAllowsUpToTenPhotos() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        when(attachmentMapper.countByCategory(11L, "RETURN_PHOTO")).thenReturn(9L);
        when(fileApi.presignPutUrl(any(), any(), any()))
                .thenReturn(new FilePresignedUploadRespDTO(
                        8L,
                        "return-registration/tenant-9/registration-11/return_photo/photo.jpg",
                        "signed-upload", "private-object"));
        org.mockito.Mockito.doAnswer(invocation -> {
            invocation.<RentalReturnRegistrationAttachmentDO>getArgument(0).setId(21L);
            return 1;
        }).when(attachmentMapper).insert(any(RentalReturnRegistrationAttachmentDO.class));

        assertDoesNotThrow(() ->
                service.authorize("token", "RETURN_PHOTO", "photo.jpg", "image/jpeg"));
    }

    @Test
    void simpleReturnCategoryRejectsEleventhPhoto() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        when(attachmentMapper.countByCategory(11L, "RETURN_PHOTO")).thenReturn(10L);

        assertThrows(RuntimeException.class, () ->
                service.authorize("token", "RETURN_PHOTO", "photo.jpg", "image/jpeg"));

        verify(fileApi, never()).presignPutUrl(any(), any(), any());
    }

    @Test
    void crossTokenAttachmentIdIsRejectedBeforeObjectAccess() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        when(attachmentMapper.selectByRegistrationAndId(11L, 99L)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> service.confirm("token", 99L));

        verify(fileApi, never()).confirmPresignedUpload(
                any(), any(), any(), any(), anyLong());
    }

    @Test
    void forgedObjectRelationIsRejectedBeforeObjectAccess() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        RentalReturnRegistrationAttachmentDO attachment =
                new RentalReturnRegistrationAttachmentDO()
                        .setId(21L)
                        .setRegistrationId(11L)
                        .setFileConfigId(8L)
                        .setCategory("DEVICE_EXTERIOR")
                        .setObjectPath(
                                "return-registration/tenant-10/registration-11/device_exterior/photo.jpg")
                        .setObjectPathHash(DigestUtil.sha256Hex(
                                "return-registration/tenant-10/registration-11/device_exterior/photo.jpg"))
                        .setOriginalName("photo.jpg")
                        .setContentType("image/jpeg")
                        .setConfirmed(false);
        when(attachmentMapper.selectByRegistrationAndId(11L, 21L)).thenReturn(attachment);

        assertThrows(RuntimeException.class, () -> service.confirm("token", 21L));

        verify(fileApi, never()).confirmPresignedUpload(
                any(), any(), any(), any(), anyLong());
    }

    @Test
    void confirmedAttachmentRemovalDeletesOwnedFile() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        RentalReturnRegistrationAttachmentDO attachment =
                new RentalReturnRegistrationAttachmentDO()
                        .setId(21L)
                        .setRegistrationId(registration.getId())
                        .setInfraFileId(31L)
                        .setCategory("DEVICE_EXTERIOR")
                        .setObjectPath(ownedObjectPath())
                        .setObjectPathHash(DigestUtil.sha256Hex(ownedObjectPath()))
                        .setConfirmed(true);
        when(attachmentMapper.selectByRegistrationAndId(registration.getId(), 21L))
                .thenReturn(attachment);

        service.remove("token", 21L);

        verify(fileApi).deleteFile(31L);
        verify(attachmentMapper).deleteById(21L);
    }

    @Test
    void unconfirmedAttachmentRemovalDeletesUploadedObject() throws Exception {
        RentalReturnRegistrationDO registration = draftRegistration();
        stubResolver(registration);
        RentalReturnRegistrationAttachmentDO attachment =
                new RentalReturnRegistrationAttachmentDO()
                        .setId(21L)
                        .setRegistrationId(registration.getId())
                        .setFileConfigId(9L)
                        .setCategory("DEVICE_EXTERIOR")
                        .setObjectPath(ownedObjectPath())
                        .setObjectPathHash(DigestUtil.sha256Hex(ownedObjectPath()))
                        .setConfirmed(false);
        when(attachmentMapper.selectByRegistrationAndId(registration.getId(), 21L))
                .thenReturn(attachment);

        service.remove("token", 21L);

        verify(fileApi).deleteFile(9L, ownedObjectPath());
        verify(attachmentMapper).deleteById(21L);
    }

    @Test
    void submissionRevalidatesTheStoredObject() {
        RentalReturnRegistrationAttachmentDO exterior = confirmedAttachment(
                21L, 31L, "DEVICE_EXTERIOR", "outside.jpg");
        RentalReturnRegistrationAttachmentDO serial = confirmedAttachment(
                22L, 32L, "SERIAL_LABEL", "serial.jpg");
        when(attachmentMapper.selectConfirmedList(11L)).thenReturn(List.of(exterior, serial));
        when(fileApi.confirmPresignedUpload(any(), any(), any(), any(), anyLong()))
                .thenAnswer(invocation -> new FileConfirmedUploadRespDTO(
                        invocation.getArgument(0, Long.class) + 30L,
                        2048L, "image/jpeg", "sha256", "preview"));

        assertDoesNotThrow(() -> service.validateForSubmission(11L, List.of(21L, 22L)));

        verify(fileApi).confirmPresignedUpload(
                1L, "return/exterior", "outside.jpg", "image/jpeg",
                ReturnRegistrationAttachmentService.MAX_SIZE);
        verify(fileApi).confirmPresignedUpload(
                2L, "return/serial", "serial.jpg", "image/jpeg",
                ReturnRegistrationAttachmentService.MAX_SIZE);
    }

    @Test
    void submissionRejectsObjectReplacedAfterConfirmation() {
        RentalReturnRegistrationAttachmentDO exterior = confirmedAttachment(
                21L, 31L, "DEVICE_EXTERIOR", "outside.jpg");
        RentalReturnRegistrationAttachmentDO serial = confirmedAttachment(
                22L, 32L, "SERIAL_LABEL", "serial.jpg");
        when(attachmentMapper.selectConfirmedList(11L)).thenReturn(List.of(exterior, serial));
        when(fileApi.confirmPresignedUpload(any(), any(), any(), any(), anyLong()))
                .thenReturn(new FileConfirmedUploadRespDTO(
                        31L, 2048L, "image/jpeg", "changed-sha256", "preview"));

        assertThrows(RuntimeException.class,
                () -> service.validateForSubmission(11L, List.of(21L, 22L)));
    }

    @Test
    void optionalSubmissionAllowsNoPhotos() {
        assertDoesNotThrow(() ->
                service.validateOptionalForSubmission(draftRegistration(), List.of()));

        verify(attachmentMapper, never()).selectByRegistrationAndId(any(), any());
        verify(fileApi, never()).confirmPresignedUpload(
                any(), any(), any(), any(), anyLong());
    }

    @Test
    void optionalSubmissionAcceptsConfirmedOwnedPhoto() {
        RentalReturnRegistrationDO registration = draftRegistration();
        RentalReturnRegistrationAttachmentDO photo = confirmedReturnPhoto(true);
        when(attachmentMapper.selectByRegistrationAndId(11L, 21L)).thenReturn(photo);
        when(fileApi.confirmPresignedUpload(any(), any(), any(), any(), anyLong()))
                .thenReturn(new FileConfirmedUploadRespDTO(
                        31L, 2048L, "image/jpeg", "sha256", "preview"));

        assertDoesNotThrow(() ->
                service.validateOptionalForSubmission(registration, List.of(21L)));
    }

    @Test
    void optionalSubmissionRejectsMoreThanTenPhotosBeforeDatabaseAccess() {
        List<Long> ids = LongStream.rangeClosed(1, 11).boxed().toList();

        assertThrows(RuntimeException.class, () ->
                service.validateOptionalForSubmission(draftRegistration(), ids));

        verify(attachmentMapper, never()).selectByRegistrationAndId(any(), any());
    }

    @Test
    void optionalSubmissionRejectsForeignOrUnconfirmedPhoto() {
        RentalReturnRegistrationDO registration = draftRegistration();
        when(attachmentMapper.selectByRegistrationAndId(11L, 99L)).thenReturn(null);

        assertThrows(RuntimeException.class, () ->
                service.validateOptionalForSubmission(registration, List.of(99L)));

        RentalReturnRegistrationAttachmentDO unconfirmed = confirmedReturnPhoto(false);
        when(attachmentMapper.selectByRegistrationAndId(11L, 21L)).thenReturn(unconfirmed);
        assertThrows(RuntimeException.class, () ->
                service.validateOptionalForSubmission(registration, List.of(21L)));
        verify(fileApi, never()).confirmPresignedUpload(
                any(), any(), any(), any(), anyLong());
    }

    @Test
    void optionalSubmissionRejectsRustfsContentChange() {
        RentalReturnRegistrationDO registration = draftRegistration();
        RentalReturnRegistrationAttachmentDO photo = confirmedReturnPhoto(true);
        when(attachmentMapper.selectByRegistrationAndId(11L, 21L)).thenReturn(photo);
        when(fileApi.confirmPresignedUpload(any(), any(), any(), any(), anyLong()))
                .thenReturn(new FileConfirmedUploadRespDTO(
                        31L, 2048L, "image/jpeg", "changed", "preview"));

        assertThrows(RuntimeException.class, () ->
                service.validateOptionalForSubmission(registration, List.of(21L)));
    }

    private RentalReturnRegistrationDO draftRegistration() {
        RentalReturnRegistrationDO registration = new RentalReturnRegistrationDO()
                .setId(11L)
                .setStatus(ReturnRegistrationStatusEnum.DRAFT.name());
        registration.setTenantId(9L);
        return registration;
    }

    private String ownedObjectPath() {
        return "return-registration/tenant-9/registration-11/device_exterior/photo.jpg";
    }

    private RentalReturnRegistrationAttachmentDO confirmedAttachment(
            Long id, Long fileId, String category, String name) {
        return new RentalReturnRegistrationAttachmentDO()
                .setId(id)
                .setRegistrationId(11L)
                .setInfraFileId(fileId)
                .setFileConfigId(id - 20L)
                .setObjectPath("return/" + (id == 21L ? "exterior" : "serial"))
                .setOriginalName(name)
                .setContentType("image/jpeg")
                .setFileSize(2048L)
                .setContentSha256("sha256")
                .setCategory(category)
                .setConfirmed(true);
    }

    private RentalReturnRegistrationAttachmentDO confirmedReturnPhoto(boolean confirmed) {
        String path =
                "return-registration/tenant-9/registration-11/return_photo/photo.jpg";
        return new RentalReturnRegistrationAttachmentDO()
                .setId(21L)
                .setRegistrationId(11L)
                .setInfraFileId(31L)
                .setFileConfigId(1L)
                .setObjectPath(path)
                .setObjectPathHash(DigestUtil.sha256Hex(path))
                .setOriginalName("photo.jpg")
                .setContentType("image/jpeg")
                .setFileSize(2048L)
                .setContentSha256("sha256")
                .setCategory("RETURN_PHOTO")
                .setConfirmed(confirmed);
    }

    private void stubResolver(RentalReturnRegistrationDO registration) throws Exception {
        when(resolver.require("token")).thenReturn(registration);
        when(resolver.lock(registration)).thenReturn(registration);
        when(resolver.publicStatus(registration)).thenReturn(ReturnRegistrationStatusEnum.DRAFT.name());
        when(resolver.execute(any(), any())).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(1);
            return callable.call();
        });
    }
}
