package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.infra.api.file.dto.FileConfirmedUploadRespDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignedUploadRespDTO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationAttachmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.returnregistration.RentalReturnRegistrationDO;
import cn.iocoder.yudao.module.rental.dal.mysql.returnregistration.RentalReturnRegistrationAttachmentMapper;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnAttachmentCategoryEnum;
import cn.iocoder.yudao.module.rental.enums.returnregistration.ReturnRegistrationStatusEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_ATTACHMENT_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RETURN_REGISTRATION_STATUS_INVALID;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.AttachmentView;
import static cn.iocoder.yudao.module.rental.service.returnregistration.ReturnRegistrationModels.UploadAuthorization;

@Service
public class ReturnRegistrationAttachmentService {

    public static final long MAX_SIZE = 15L * 1024L * 1024L;
    public static final int MAX_TOTAL_COUNT = 20;
    public static final int MAX_CATEGORY_COUNT = 6;
    public static final int UPLOAD_URL_EXPIRATION_SECONDS = 300;
    private static final Set<String> ALLOWED_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    private final ReturnRegistrationResolver resolver;
    private final RentalReturnRegistrationAttachmentMapper attachmentMapper;
    private final FileApi fileApi;

    public ReturnRegistrationAttachmentService(ReturnRegistrationResolver resolver,
                                               RentalReturnRegistrationAttachmentMapper attachmentMapper,
                                               FileApi fileApi) {
        this.resolver = resolver;
        this.attachmentMapper = attachmentMapper;
        this.fileApi = fileApi;
    }

    @Transactional(rollbackFor = Exception.class)
    public UploadAuthorization authorize(String token, String categoryValue,
                                         String originalName, String contentType) {
        RentalReturnRegistrationDO registration = resolver.require(token);
        return resolver.execute(registration, () -> {
            RentalReturnRegistrationDO current = resolver.lock(registration);
            requireDraft(current);
            ReturnAttachmentCategoryEnum category = parseCategory(categoryValue);
            String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
            if (!ALLOWED_TYPES.contains(normalizedType)) {
                throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "仅支持 JPEG、PNG、WebP");
            }
            if (attachmentMapper.countByRegistration(current.getId()) >= MAX_TOTAL_COUNT) {
                throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "照片总数最多 20 张");
            }
            if (attachmentMapper.countByCategory(current.getId(), category.name()) >= MAX_CATEGORY_COUNT) {
                throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "每类最多 6 张");
            }
            String extension = FileUtil.extName(originalName);
            String safeName = IdUtil.fastSimpleUUID() + (extension.isBlank() ? "" : "." + extension);
            String directory = objectDirectory(current, category);
            FilePresignedUploadRespDTO upload = fileApi.presignPutUrl(
                    safeName, directory, UPLOAD_URL_EXPIRATION_SECONDS);
            RentalReturnRegistrationAttachmentDO attachment =
                    new RentalReturnRegistrationAttachmentDO()
                            .setRegistrationId(current.getId())
                            .setFileConfigId(upload.configId())
                            .setCategory(category.name())
                            .setObjectPath(upload.path())
                            .setObjectPathHash(DigestUtil.sha256Hex(upload.path()))
                            .setOriginalName(safeOriginalName(originalName))
                            .setContentType(normalizedType)
                            .setSortNo((int) attachmentMapper.countByCategory(
                                    current.getId(), category.name()))
                            .setConfirmed(false);
            attachmentMapper.insert(attachment);
            return new UploadAuthorization(attachment.getId(), upload.configId(), upload.path(),
                    upload.uploadUrl(), category.name(), normalizedType, MAX_SIZE,
                    UPLOAD_URL_EXPIRATION_SECONDS);
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public AttachmentView confirm(String token, Long attachmentId) {
        RentalReturnRegistrationDO registration = resolver.require(token);
        return resolver.execute(registration, () -> {
            RentalReturnRegistrationDO current = resolver.lock(registration);
            requireDraft(current);
            if (attachmentMapper.countByRegistration(current.getId()) > MAX_TOTAL_COUNT) {
                throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "照片总数最多 20 张");
            }
            RentalReturnRegistrationAttachmentDO attachment =
                    requireAttachment(current.getId(), attachmentId);
            validateObjectOwnership(current, attachment);
            if (Boolean.TRUE.equals(attachment.getConfirmed())) {
                return view(attachment);
            }
            FileConfirmedUploadRespDTO file = fileApi.confirmPresignedUpload(
                    attachment.getFileConfigId(), attachment.getObjectPath(),
                    attachment.getOriginalName(), attachment.getContentType(), MAX_SIZE);
            attachment.setInfraFileId(file.fileId())
                    .setFileSize(file.size())
                    .setContentType(file.contentType())
                    .setContentSha256(file.sha256())
                    .setConfirmed(true);
            attachmentMapper.updateById(attachment);
            return new AttachmentView(attachment.getId(), file.fileId(), attachment.getCategory(),
                    attachment.getOriginalName(), file.size(), file.url());
        });
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(String token, Long attachmentId) {
        RentalReturnRegistrationDO registration = resolver.require(token);
        resolver.execute(registration, () -> {
            RentalReturnRegistrationDO current = resolver.lock(registration);
            requireDraft(current);
            RentalReturnRegistrationAttachmentDO attachment =
                    requireAttachment(current.getId(), attachmentId);
            validateObjectOwnership(current, attachment);
            if (Boolean.TRUE.equals(attachment.getConfirmed()) && attachment.getInfraFileId() != null) {
                fileApi.deleteFile(attachment.getInfraFileId());
            } else {
                fileApi.deleteFile(attachment.getFileConfigId(), attachment.getObjectPath());
            }
            attachmentMapper.deleteById(attachment.getId());
            return null;
        });
    }

    public List<AttachmentView> listForAdmin(Long registrationId) {
        return attachmentMapper.selectConfirmedList(registrationId).stream().map(this::view).toList();
    }

    public List<Long> listConfirmedIds(Long registrationId) {
        return attachmentMapper.selectConfirmedList(registrationId)
                .stream().map(RentalReturnRegistrationAttachmentDO::getId).toList();
    }

    public void validateForSubmission(Long registrationId, List<Long> requestedIds) {
        List<RentalReturnRegistrationAttachmentDO> attachments =
                attachmentMapper.selectConfirmedList(registrationId);
        Set<Long> requested = new HashSet<>(requestedIds);
        if (attachments.size() != requested.size()
                || attachments.stream().anyMatch(item -> !requested.contains(item.getId()))) {
            throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID,
                    "附件不属于当前链接或尚未确认");
        }
        Set<String> categories = new HashSet<>();
        for (RentalReturnRegistrationAttachmentDO attachment : attachments) {
            categories.add(attachment.getCategory());
            FileConfirmedUploadRespDTO file = fileApi.confirmPresignedUpload(
                    attachment.getFileConfigId(), attachment.getObjectPath(),
                    attachment.getOriginalName(), attachment.getContentType(), MAX_SIZE);
            if (!Objects.equals(file.fileId(), attachment.getInfraFileId())
                    || !Objects.equals(file.size(), attachment.getFileSize())
                    || !Objects.equals(file.contentType(), attachment.getContentType())
                    || !Objects.equals(file.sha256(), attachment.getContentSha256())) {
                throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "附件内容或文件关系已变化");
            }
        }
        for (ReturnAttachmentCategoryEnum category : ReturnAttachmentCategoryEnum.values()) {
            if (category.isRequired() && !categories.contains(category.name())) {
                throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "缺少必拍照片");
            }
        }
        if (attachments.size() > MAX_TOTAL_COUNT) {
            throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "照片总数最多 20 张");
        }
    }

    private AttachmentView view(RentalReturnRegistrationAttachmentDO attachment) {
        String url = attachment.getInfraFileId() == null ? null
                : fileApi.presignGetUrlById(attachment.getInfraFileId(), 300);
        return new AttachmentView(attachment.getId(), attachment.getInfraFileId(),
                attachment.getCategory(), attachment.getOriginalName(), attachment.getFileSize(), url);
    }

    private RentalReturnRegistrationAttachmentDO requireAttachment(Long registrationId, Long attachmentId) {
        RentalReturnRegistrationAttachmentDO attachment =
                attachmentMapper.selectByRegistrationAndId(registrationId, attachmentId);
        if (attachment == null) {
            throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "附件不存在或不属于当前链接");
        }
        return attachment;
    }

    private ReturnAttachmentCategoryEnum parseCategory(String value) {
        try {
            return ReturnAttachmentCategoryEnum.valueOf(value);
        } catch (Exception ex) {
            throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "附件类别无效");
        }
    }

    private void requireDraft(RentalReturnRegistrationDO registration) {
        if (!ReturnRegistrationStatusEnum.DRAFT.name().equals(resolver.publicStatus(registration))) {
            throw exception(RETURN_REGISTRATION_STATUS_INVALID);
        }
    }

    private void validateObjectOwnership(RentalReturnRegistrationDO registration,
                                         RentalReturnRegistrationAttachmentDO attachment) {
        ReturnAttachmentCategoryEnum category = parseCategory(attachment.getCategory());
        String expectedPrefix = objectDirectory(registration, category) + "/";
        if (!attachment.getObjectPath().startsWith(expectedPrefix)
                || !DigestUtil.sha256Hex(attachment.getObjectPath())
                .equals(attachment.getObjectPathHash())) {
            throw exception(RETURN_REGISTRATION_ATTACHMENT_INVALID, "附件对象关系无效");
        }
    }

    private String objectDirectory(RentalReturnRegistrationDO registration,
                                   ReturnAttachmentCategoryEnum category) {
        return "return-registration/tenant-" + registration.getTenantId()
                + "/registration-" + registration.getId() + "/"
                + category.name().toLowerCase(Locale.ROOT);
    }

    private String safeOriginalName(String name) {
        String safe = FileUtil.getName(name == null ? "photo" : name);
        return safe.length() > 255 ? safe.substring(safe.length() - 255) : safe;
    }
}
