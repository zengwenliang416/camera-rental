package cn.iocoder.yudao.module.infra.api.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.infra.api.file.dto.FileConfirmedUploadRespDTO;
import cn.iocoder.yudao.module.infra.api.file.dto.FilePresignedUploadRespDTO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 文件 API 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Override
    public String createFile(byte[] content, String name, String directory, String type) {
        return fileService.createFile(content, name, directory, type);
    }

    @Override
    public String presignGetUrl(String url, Integer expirationSeconds) {
        return fileService.presignGetUrl(url, expirationSeconds);
    }

    @Override
    public FilePresignedUploadRespDTO presignPutUrl(String name, String directory) {
        return presignPutUrl(name, directory, null);
    }

    @Override
    public FilePresignedUploadRespDTO presignPutUrl(String name, String directory,
                                                    Integer expirationSeconds) {
        FilePresignedUrlRespVO value =
                fileService.presignPutUrl(name, directory, expirationSeconds);
        return new FilePresignedUploadRespDTO(
                value.getConfigId(), value.getPath(), value.getUploadUrl(), value.getUrl());
    }

    @Override
    public FileConfirmedUploadRespDTO confirmPresignedUpload(Long configId, String path, String name,
                                                             String expectedContentType, long maxSize) {
        FileDO existing = fileService.getFileByConfigIdAndPath(configId, path);
        if (existing != null) {
            byte[] content = read(configId, path);
            validateUploadedObject(content, name, expectedContentType, maxSize);
            return response(existing, content);
        }
        byte[] content = read(configId, path);
        String actualType = validateUploadedObject(content, name, expectedContentType, maxSize);
        String url = fileService.presignGetUrl(path, 300);
        FileCreateReqVO create = new FileCreateReqVO();
        create.setConfigId(configId);
        create.setPath(path);
        create.setName(name);
        create.setUrl(url);
        create.setType(actualType);
        create.setSize((long) content.length);
        Long fileId = fileService.createFile(create);
        return new FileConfirmedUploadRespDTO(
                fileId, (long) content.length, actualType, DigestUtil.sha256Hex(content), url);
    }

    private String validateUploadedObject(byte[] content, String name,
                                          String expectedContentType, long maxSize) {
        if (content.length == 0 || content.length > maxSize) {
            throw new IllegalArgumentException("Uploaded object size is invalid");
        }
        String actualType = FileTypeUtils.getMineType(content, name);
        if (expectedContentType != null && !expectedContentType.equalsIgnoreCase(actualType)) {
            throw new IllegalArgumentException("Uploaded object content type does not match");
        }
        return actualType;
    }

    private byte[] read(Long configId, String path) {
        try {
            return fileService.getFileContent(configId, path);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Uploaded object is unavailable", ex);
        }
    }

    private FileConfirmedUploadRespDTO response(FileDO file, byte[] content) {
        return new FileConfirmedUploadRespDTO(file.getId(), file.getSize(), file.getType(),
                DigestUtil.sha256Hex(content), fileService.presignGetUrl(file.getUrl(), 300));
    }

    @Override
    public String presignGetUrlById(Long fileId, Integer expirationSeconds) {
        return fileService.presignGetUrl(fileService.getFile(fileId).getUrl(), expirationSeconds);
    }

    @Override
    public void deleteFile(Long fileId) {
        try {
            fileService.deleteFile(fileId);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to delete file", ex);
        }
    }

    @Override
    public void deleteFile(Long configId, String path) {
        try {
            fileService.deleteFile(configId, path);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to delete uploaded object", ex);
        }
    }

}
