package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAlertPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAlertResolveReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAlertRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAlertAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.web.core.util.WebFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - 闲鱼运营告警")
@RestController
@RequestMapping("/rental/xianyu/alert")
@Validated
public class XianyuAlertController {

    private final XianyuAlertAdminService alertAdminService;

    public XianyuAlertController(XianyuAlertAdminService alertAdminService) {
        this.alertAdminService = alertAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询闲鱼运营告警（脱敏）")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<PageResult<XianyuAlertRespVO>> getAlertPage(@Valid XianyuAlertPageReqVO reqVO) {
        return success(alertAdminService.getAlertPage(reqVO));
    }

    @PutMapping("/resolve")
    @Operation(summary = "解决闲鱼运营告警")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:sync')")
    public CommonResult<Boolean> resolveAlert(@Valid @RequestBody XianyuAlertResolveReqVO reqVO) {
        alertAdminService.resolveAlert(reqVO.getId(), getLoginUserId());
        return success(true);
    }

}
