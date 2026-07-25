package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuShopRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuShopAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲鱼店铺")
@RestController
@RequestMapping("/rental/xianyu/shop")
@Validated
public class XianyuShopController {

    private final XianyuShopAdminService shopAdminService;

    public XianyuShopController(XianyuShopAdminService shopAdminService) {
        this.shopAdminService = shopAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询本地店铺")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<PageResult<XianyuShopRespVO>> getShopPage(@Validated PageParam pageParam) {
        return success(shopAdminService.getShopPage(pageParam));
    }

    @PostMapping("/sync-authorized")
    @Operation(summary = "同步授权店铺（只读）")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:sync')")
    public CommonResult<Integer> syncAuthorizedShops() {
        return success(shopAdminService.syncAuthorizedShops());
    }

}
