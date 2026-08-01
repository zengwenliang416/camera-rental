package cn.iocoder.yudao.module.infra.api.file.dto;

public record FileConfirmedUploadRespDTO(
        Long fileId,
        Long size,
        String contentType,
        String sha256,
        String url
) {
}
