package cn.iocoder.yudao.module.infra.api.file;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileApiImplTest {

    private final FileService fileService = mock(FileService.class);
    private final FileApiImpl fileApi = new FileApiImpl();

    FileApiImplTest() {
        ReflectionTestUtils.setField(fileApi, "fileService", fileService);
    }

    @Test
    void delegatesExplicitUploadExpirationWithoutLeakingTheVisitUrl() {
        when(fileService.presignPutUrl("photo.jpg", "return-registration", 300))
                .thenReturn(new FilePresignedUrlRespVO()
                        .setConfigId(8L)
                        .setPath("return-registration/photo.jpg")
                        .setUploadUrl("signed-put")
                        .setUrl("private-object"));

        var result = fileApi.presignPutUrl("photo.jpg", "return-registration", 300);

        assertEquals("signed-put", result.uploadUrl());
        verify(fileService).presignPutUrl("photo.jpg", "return-registration", 300);
    }

    @Test
    void rejectsOversizedObjectEvenWhenFileMetadataAlreadyExists() throws Exception {
        Long configId = 8L;
        String path = "return-registration/oversized.jpg";
        when(fileService.getFileByConfigIdAndPath(configId, path))
                .thenReturn(new FileDO().setId(99L).setConfigId(configId).setPath(path));
        when(fileService.getFileContent(configId, path)).thenReturn(new byte[11]);

        assertThrows(IllegalArgumentException.class,
                () -> fileApi.confirmPresignedUpload(configId, path, "photo.jpg", "image/jpeg", 10));

        verify(fileService, never()).presignGetUrl(path, 300);
    }
}
