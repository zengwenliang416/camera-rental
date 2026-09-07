package cn.iocoder.yudao.module.rental.controller.admin.rental;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalAddressParseReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalAddressParseRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalCustomerSuggestRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalManualOrderCreateRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalOrderConfirmOutboundReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalCustomerDO;
import cn.iocoder.yudao.module.rental.service.rental.RentalAddressParseResult;
import cn.iocoder.yudao.module.rental.service.rental.RentalAddressParseService;
import cn.iocoder.yudao.module.rental.service.rental.RentalManualOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 线下租赁订单")
@RestController
@RequestMapping("/rental")
@Validated
public class RentalOrderManualController {

    private final RentalManualOrderService manualOrderService;
    private final RentalAddressParseService addressParseService;

    public RentalOrderManualController(RentalManualOrderService manualOrderService,
                                       RentalAddressParseService addressParseService) {
        this.manualOrderService = manualOrderService;
        this.addressParseService = addressParseService;
    }

    @PostMapping("/address/parse")
    @Operation(summary = "智能识别姓名、手机号和标准化收货地址")
    @ApiAccessLog(requestEnable = false)
    @PreAuthorize("@ss.hasPermission('rental:order:create')")
    public CommonResult<RentalAddressParseRespVO> parseAddress(
            @Valid @RequestBody RentalAddressParseReqVO reqVO) {
        RentalAddressParseResult result = addressParseService.parse(reqVO.getText());
        RentalAddressParseRespVO respVO = new RentalAddressParseRespVO();
        respVO.setName(result.name());
        respVO.setMobile(result.mobile());
        respVO.setAddress(result.address());
        respVO.setProvince(result.province());
        respVO.setCity(result.city());
        respVO.setDistrict(result.district());
        respVO.setStreet(result.street());
        respVO.setSource(result.source());
        respVO.setFallback(result.fallback());
        respVO.setWarnings(result.warnings());
        return success(respVO);
    }

    @PostMapping("/order/create-manual")
    @Operation(summary = "手动创建线下租赁订单并绑定具体设备排期")
    @PreAuthorize("@ss.hasPermission('rental:order:create') && "
            + "@ss.hasPermission('rental:device:assign')")
    public CommonResult<RentalManualOrderCreateRespVO> createManualOrder(
            @Valid @RequestBody RentalManualOrderCreateReqVO reqVO) {
        return success(manualOrderService.createManualOrder(reqVO));
    }

    @PostMapping("/order/confirm-outbound")
    @Operation(summary = "确认线下订单已送出（跑腿/自送，无运单）")
    @PreAuthorize("@ss.hasPermission('rental:order:create')")
    public CommonResult<Boolean> confirmOutbound(
            @Valid @RequestBody RentalOrderConfirmOutboundReqVO reqVO) {
        manualOrderService.confirmOutbound(reqVO.getOrderId());
        return success(true);
    }

    @GetMapping("/customer/suggest")
    @Operation(summary = "按完整手机号反查线下客户")
    @Parameter(name = "mobile", description = "完整手机号", required = true)
    @PreAuthorize("@ss.hasPermission('rental:order:create')")
    public CommonResult<RentalCustomerSuggestRespVO> suggestCustomer(
            @RequestParam("mobile") @NotBlank String mobile) {
        RentalCustomerDO customer = manualOrderService.suggestCustomer(mobile);
        if (customer == null) {
            return success(null);
        }
        RentalCustomerSuggestRespVO respVO = new RentalCustomerSuggestRespVO();
        respVO.setId(customer.getId());
        respVO.setName(customer.getName());
        respVO.setMobile(customer.getMobile());
        respVO.setWechatId(customer.getWechatId());
        return success(respVO);
    }

}
