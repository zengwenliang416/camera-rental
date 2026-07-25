package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuConfigRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuConfigAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲管家配置")
@RestController
@RequestMapping("/rental/xianyu/config")
@Validated
public class XianyuConfigController {

    private final XianyuConfigAdminService configAdminService;

    public XianyuConfigController(XianyuConfigAdminService configAdminService) {
        this.configAdminService = configAdminService;
    }

    @GetMapping("/get")
    @Operation(summary = "获取闲管家集成状态（脱敏，永不返回 AppSecret）")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<XianyuConfigRespVO> getConfig() {
        return success(configAdminService.getConfig());
    }

}
