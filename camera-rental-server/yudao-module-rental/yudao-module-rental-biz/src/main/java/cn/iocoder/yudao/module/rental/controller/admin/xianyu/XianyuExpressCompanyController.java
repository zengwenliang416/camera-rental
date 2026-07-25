package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuExpressCompanyRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuExpressCompanyAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲管家快递公司")
@RestController
@RequestMapping("/rental/xianyu/express-company")
public class XianyuExpressCompanyController {

    private final XianyuExpressCompanyAdminService expressCompanyAdminService;

    public XianyuExpressCompanyController(XianyuExpressCompanyAdminService expressCompanyAdminService) {
        this.expressCompanyAdminService = expressCompanyAdminService;
    }

    @GetMapping("/list")
    @Operation(summary = "查询闲管家快递公司")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<List<XianyuExpressCompanyRespVO>> getList() {
        return success(expressCompanyAdminService.getExpressCompanies());
    }

}
