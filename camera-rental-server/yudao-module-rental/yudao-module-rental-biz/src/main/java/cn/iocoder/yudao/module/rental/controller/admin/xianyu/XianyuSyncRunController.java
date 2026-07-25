package cn.iocoder.yudao.module.rental.controller.admin.xianyu;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuSyncRunPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuSyncRunRespVO;
import cn.iocoder.yudao.module.rental.service.admin.XianyuSyncRunAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 闲鱼同步运行历史")
@RestController
@RequestMapping("/rental/xianyu/sync-run")
@Validated
public class XianyuSyncRunController {

    private final XianyuSyncRunAdminService syncRunAdminService;

    public XianyuSyncRunController(XianyuSyncRunAdminService syncRunAdminService) {
        this.syncRunAdminService = syncRunAdminService;
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询同步运行历史（脱敏）")
    @PreAuthorize("@ss.hasPermission('rental:xianyu:query')")
    public CommonResult<PageResult<XianyuSyncRunRespVO>> getSyncRunPage(@Valid XianyuSyncRunPageReqVO reqVO) {
        return success(syncRunAdminService.getSyncRunPage(reqVO));
    }

}
