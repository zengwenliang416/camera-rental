package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.*;
import cn.iocoder.yudao.module.rental.service.admin.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/rental/device")
@Validated
@PreAuthorize("@ss.hasPermission('rental:device:query')")
public class RentalDeviceImportController {
    private final RentalDeviceImportService imports;
    private final RentalDeviceLabelService labels;
    public RentalDeviceImportController(RentalDeviceImportService imports, RentalDeviceLabelService labels) {
        this.imports = imports; this.labels = labels;
    }
    @PostMapping("/import/preview")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<RentalDeviceImportRespVO> preview(@Valid @RequestBody RentalDeviceImportReqVO request) {
        return success(imports.preview(request, SecurityFrameworkUtils.getLoginUserId()));
    }
    @PostMapping("/import/commit")
    @PreAuthorize("@ss.hasPermission('rental:device:query') and @ss.hasPermission('rental:device:create')")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<List<Long>> commit(@RequestParam @Pattern(regexp = "[a-f0-9-]{36}") String batchId) {
        return success(imports.commit(batchId, SecurityFrameworkUtils.getLoginUserId()));
    }
    @PostMapping("/labels/preview")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public CommonResult<RentalDeviceLabelPreviewRespVO> previewLabels(@Valid @RequestBody RentalDeviceLabelReqVO request) {
        return success(labels.preview(request));
    }
    @PostMapping("/labels/download")
    @ApiAccessLog(requestEnable = false, responseEnable = false)
    public ResponseEntity<byte[]> download(@Valid @RequestBody RentalDeviceLabelReqVO request) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=device-labels.zip")
                .body(labels.download(request));
    }
}
