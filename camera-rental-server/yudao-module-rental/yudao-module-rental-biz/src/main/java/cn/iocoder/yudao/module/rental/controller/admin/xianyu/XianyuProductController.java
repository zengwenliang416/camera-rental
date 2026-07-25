package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuProductSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuProductSyncRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuProductAdminService;
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

@Tag(name = "管理后台 - 闲鱼商品")
@RestController
@RequestMapping("/rental/xianyu/product")
@Validated
public class XianyuProductController {

    private final XianyuProductAdminService productAdminService;

    public XianyuProductController(XianyuProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @PostMapping("/sync-page")
    @Operation(summary = "有界只读商品同步一页")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:sync')")
    public CommonResult<XianyuProductSyncRespVO> syncPage(@Valid @RequestBody XianyuProductSyncReqVO reqVO) {
        return success(productAdminService.syncPage(reqVO));
    }

}
