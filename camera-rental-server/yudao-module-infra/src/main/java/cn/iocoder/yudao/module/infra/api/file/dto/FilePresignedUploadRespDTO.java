package cn.iocoder.yudao.module.infra.api.file.dto;

public record FilePresignedUploadRespDTO(
        Long configId,
        String path,
        String uploadUrl,
        String visitUrl
) {
}
