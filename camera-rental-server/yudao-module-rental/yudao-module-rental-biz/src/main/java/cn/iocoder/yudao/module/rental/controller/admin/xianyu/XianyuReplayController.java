package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPushReplayReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuPushReplayRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadReplayReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadReplayRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuReplayAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 闲管家安全重放")
@RestController
@RequestMapping("/rental/xianyu/replay")
@Validated
public class XianyuReplayController {

    private final XianyuReplayAdminService replayAdminService;

    public XianyuReplayController(XianyuReplayAdminService replayAdminService) {
        this.replayAdminService = replayAdminService;
    }

    @PostMapping("/push-event")
    @Operation(summary = "安全重放失败或未完成的订单推送事件")
    @ApiAccessLog(responseEnable = false, operateType = OperateTypeEnum.UPDATE)
    @PreAuthorize("@ss.hasPermission('rental:xianyu:replay')")
    public CommonResult<XianyuPushReplayRespVO> replayPushEvent(
            @Valid @RequestBody XianyuPushReplayReqVO reqVO) {
        return success(replayAdminService.replayPushEvent(reqVO.getEventId(), getLoginUserId()));
    }

    @PostMapping("/raw-payload")
    @Operation(summary = "安全重放本地订单详情原始载荷")
    @ApiAccessLog(responseEnable = false, operateType = OperateTypeEnum.UPDATE)
    @PreAuthorize("@ss.hasPermission('rental:xianyu:replay')")
    public CommonResult<XianyuRawPayloadReplayRespVO> replayRawPayload(
            @Valid @RequestBody XianyuRawPayloadReplayReqVO reqVO) {
        return success(replayAdminService.replayRawPayload(reqVO.getRawPayloadId(), getLoginUserId()));
    }

}
