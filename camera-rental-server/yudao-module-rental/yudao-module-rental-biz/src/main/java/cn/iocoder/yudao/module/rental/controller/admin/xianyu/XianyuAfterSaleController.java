package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSalePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuAfterSaleAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲鱼售后")
@RestController
@RequestMapping("/rental/xianyu/after-sale")
@Validated
public class XianyuAfterSaleController {

    private final XianyuAfterSaleAdminService afterSaleAdminService;

    public XianyuAfterSaleController(XianyuAfterSaleAdminService afterSaleAdminService) {
        this.afterSaleAdminService = afterSaleAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询渠道售后")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<PageResult<XianyuAfterSaleRespVO>> getPage(@Valid XianyuAfterSalePageReqVO pageReqVO) {
        return success(afterSaleAdminService.getPage(pageReqVO));
    }

    @PostMapping("/sync-page")
    @Operation(summary = "有界只读售后同步一页")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:sync')")
    public CommonResult<XianyuAfterSaleSyncRespVO> syncPage(@Valid @RequestBody XianyuAfterSaleSyncReqVO reqVO) {
        return success(afterSaleAdminService.syncPage(reqVO));
    }

}
