package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XianyuOrderAdminServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void orderPageExposesOrderNoAndReceiverForOpsButNotRawPayloads() throws Exception {
        XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(1L)
                .shopId(2L)
                .externalOrderId("3364202298717566229")
                .orderStatus("22")
                .payAmount(100L)
                .currency("CNY")
                .conversionStatus("PENDING")
                .sellerRemark("发货7.25/收货7.26/发回8.02")
                .detailJson("{"
                        + "\"receiver_mobile\":\"13800138000\","
                        + "\"receiver_name\":\"张三\","
                        + "\"prov_name\":\"浙江省\","
                        + "\"city_name\":\"杭州市\","
                        + "\"area_name\":\"西湖区\","
                        + "\"town_name\":\"\","
                        + "\"address\":\"secret 路1号\","
                        + "\"pay_no\":\"202600000000000000\","
                        + "\"waybill_no\":\"SF1234567890\","
                        + "\"buyer_nick\":\"private-buyer\""
                        + "}")
                .goodsJson("{\"images\":[\"secret\"]}")
                .payNo("202600000000000000")
                .waybillNo("SF1234567890")
                .buyerNick("private-buyer")
                .build();
        XianyuOrderPageReqVO pageParam = new XianyuOrderPageReqVO();
        when(orderMapper.selectAdminPage(pageParam))
                .thenReturn(new PageResult<>(List.of(order), 1L));
        XianyuOrderAdminService service = new XianyuOrderAdminService(
                orderMapper,
                mock(XianyuShopMapper.class),
                mock(XianyuOrderSyncService.class),
                mock(XianyuRentalConversionService.class),
                objectMapper);

        PageResult<XianyuOrderRespVO> page = service.getOrderPage(pageParam);
        XianyuOrderRespVO vo = page.getList().get(0);
        String json = objectMapper.writeValueAsString(vo);

        // Ops-required fields stay full.
        assertEquals("3364202298717566229", vo.getExternalOrderId());
        assertEquals("张三", vo.getReceiverName());
        assertEquals("13800138000", vo.getReceiverMobile());
        assertEquals("浙江省杭州市西湖区secret 路1号", vo.getReceiverAddress());
        assertTrue(json.contains("3364202298717566229"));
        assertTrue(json.contains("13800138000"));

        // Raw blobs and non-shipping secrets stay out of list VO.
        assertFalse(json.contains("detailJson"));
        assertFalse(json.contains("goodsJson"));
        assertFalse(json.contains("202600000000000000"));
        assertFalse(json.contains("private-buyer"));
        assertFalse(json.contains("SF1234567890"));
    }

    @Test
    void manualSyncMustRejectExpiredShopAuthorizationBeforeRemoteAccess() {
        XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        XianyuOrderSyncService syncService = mock(XianyuOrderSyncService.class);
        when(shopMapper.selectByTenantIdAndId(9L, 2L)).thenReturn(XianyuShopDO.builder()
                .id(2L)
                .authorizeId("88")
                .authorizationStatus("VALID")
                .authorizationExpiresAt(LocalDateTime.of(2026, 7, 23, 23, 59))
                .build());
        XianyuOrderAdminService service = new XianyuOrderAdminService(
                orderMapper, shopMapper, syncService, mock(XianyuRentalConversionService.class), objectMapper);
        XianyuOrderSyncReqVO reqVO = new XianyuOrderSyncReqVO();
        reqVO.setShopId(2L);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.syncPage(reqVO));

        assertEquals(XIANYU_SHOP_AUTHORIZATION_INVALID.getCode(), exception.getCode());
    }

}
