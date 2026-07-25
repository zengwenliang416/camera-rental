package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuRawPayloadAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲管家受限原始载荷")
@RestController
@RequestMapping("/rental/xianyu/raw-payload")
@Validated
public class XianyuRawPayloadController {

    private final XianyuRawPayloadAdminService rawPayloadAdminService;

    public XianyuRawPayloadController(XianyuRawPayloadAdminService rawPayloadAdminService) {
        this.rawPayloadAdminService = rawPayloadAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询受限原始载荷元数据（脱敏）")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:raw')")
    public CommonResult<PageResult<XianyuRawPayloadRespVO>> getRawPayloadPage(
            @Valid XianyuRawPayloadPageReqVO reqVO) {
        return success(rawPayloadAdminService.getRawPayloadPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "查看受限原始载荷（仅返回二次脱敏内容，并记录访问日志）")
    @ApiAccessLog(responseEnable = false, operateType = OperateTypeEnum.GET)
    @PreAuthorize("@ss.hasPermission('rental:xianyu:raw')")
    public CommonResult<XianyuRawPayloadRespVO> getRawPayload(@RequestParam("id") Long id) {
        return success(rawPayloadAdminService.getRawPayload(id));
    }

}
