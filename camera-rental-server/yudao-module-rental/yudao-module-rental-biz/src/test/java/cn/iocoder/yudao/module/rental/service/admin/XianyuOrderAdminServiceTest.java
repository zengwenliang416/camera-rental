package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceAssignmentMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderSyncService;
import cn.iocoder.yudao.module.rental.service.XianyuRentalConversionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_SHOP_AUTHORIZATION_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XianyuOrderAdminServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void orderPageExposesOrderNoWithMaskedReceiverButNotRawPayloads() throws Exception {
        XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
        RentalOrderMapper rentalOrderMapper = mock(RentalOrderMapper.class);
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(1L)
                .shopId(2L)
                .externalOrderId("3364202298717566229")
                .orderStatus("22")
                .payAmount(100L)
                .currency("CNY")
                .conversionStatus("PENDING")
                .sellerRemark("发货7.25/收货7.26/发回8.02")
                .billableStartDate(LocalDate.of(2026, 7, 27))
                .billableEndDate(LocalDate.of(2026, 8, 2))
                .rentalPeriodStatus("SUCCESS")
                .orderTime(LocalDateTime.of(2026, 7, 25, 12, 0))
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
                rentalOrderMapper,
                mock(RentalOrderItemMapper.class),
                mock(RentalDeviceAssignmentMapper.class),
                mock(XianyuOrderSyncService.class),
                mock(XianyuRentalConversionService.class),
                objectMapper);

        PageResult<XianyuOrderRespVO> page = service.getOrderPage(pageParam);
        XianyuOrderRespVO vo = page.getList().get(0);
        String json = objectMapper.writeValueAsString(vo);

        // Order lookup stays available while recipient PII is masked at the API boundary.
        assertEquals("3364202298717566229", vo.getExternalOrderId());
        assertEquals("张*", vo.getReceiverName());
        assertEquals("138****8000", vo.getReceiverMobile());
        assertEquals("浙江省杭州市***", vo.getReceiverAddress());
        assertEquals(LocalDate.of(2026, 7, 27), vo.getBillableStartDate());
        assertEquals(LocalDate.of(2026, 8, 2), vo.getBillableEndDate());
        assertEquals("SUCCESS", vo.getRentalPeriodStatus());
        assertTrue(json.contains("3364202298717566229"));
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("secret 路1号"));

        // Raw blobs and non-shipping secrets stay out of list VO.
        assertFalse(json.contains("detailJson"));
        assertFalse(json.contains("goodsJson"));
        assertFalse(json.contains("202600000000000000"));
        assertFalse(json.contains("private-buyer"));
        assertEquals("SF1234567890", vo.getWaybillNo());
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
                orderMapper,
                shopMapper,
                mock(RentalOrderMapper.class),
                mock(RentalOrderItemMapper.class),
                mock(RentalDeviceAssignmentMapper.class),
                syncService,
                mock(XianyuRentalConversionService.class),
                objectMapper);
        XianyuOrderSyncReqVO reqVO = new XianyuOrderSyncReqVO();
        reqVO.setShopId(2L);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.syncPage(reqVO));

        assertEquals(XIANYU_SHOP_AUTHORIZATION_INVALID.getCode(), exception.getCode());
    }

    @Test
    void orderPagePrefersConvertedRentalOrderPeriod() {
        XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
        RentalOrderMapper rentalOrderMapper = mock(RentalOrderMapper.class);
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(1L)
                .shopId(2L)
                .externalOrderId("3364202298717566229")
                .orderStatus("22")
                .payAmount(100L)
                .currency("CNY")
                .conversionStatus("CONVERTED")
                .rentalOrderId(99L)
                .sellerRemark("bad remark")
                .orderTime(LocalDateTime.of(2026, 7, 25, 12, 0))
                .build();
        XianyuOrderPageReqVO pageParam = new XianyuOrderPageReqVO();
        when(orderMapper.selectAdminPage(pageParam)).thenReturn(new PageResult<>(List.of(order), 1L));
        when(rentalOrderMapper.selectById(99L)).thenReturn(RentalOrderDO.builder()
                .id(99L)
                .billableStartDate(LocalDate.of(2026, 8, 1))
                .billableEndDate(LocalDate.of(2026, 8, 5))
                .build());
        XianyuOrderAdminService service = new XianyuOrderAdminService(
                orderMapper,
                mock(XianyuShopMapper.class),
                rentalOrderMapper,
                mock(RentalOrderItemMapper.class),
                mock(RentalDeviceAssignmentMapper.class),
                mock(XianyuOrderSyncService.class),
                mock(XianyuRentalConversionService.class),
                objectMapper);

        XianyuOrderRespVO vo = service.getOrderPage(pageParam).getList().get(0);

        assertEquals(LocalDate.of(2026, 8, 1), vo.getBillableStartDate());
        assertEquals(LocalDate.of(2026, 8, 5), vo.getBillableEndDate());
        assertEquals("SUCCESS", vo.getRentalPeriodStatus());
    }

    @Test
    void orderPageUsesPersistedReceiverSnapshotAfterShipment() {
        XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(69L)
                .shopId(3L)
                .externalOrderId("test-order-6425")
                .orderStatus("21")
                .payAmount(14000L)
                .currency("CNY")
                .conversionStatus("REVIEW_REQUIRED")
                .receiverName("张三")
                .receiverMobile("13800138000")
                .receiverAddress("湖南省长沙市测试地址")
                .detailJson("{\"order_no\":\"test-order-6425\",\"order_status\":21}")
                .build();
        XianyuOrderPageReqVO pageParam = new XianyuOrderPageReqVO();
        when(orderMapper.selectAdminPage(pageParam)).thenReturn(new PageResult<>(List.of(order), 1L));
        XianyuOrderAdminService service = new XianyuOrderAdminService(
                orderMapper,
                mock(XianyuShopMapper.class),
                mock(RentalOrderMapper.class),
                mock(RentalOrderItemMapper.class),
                mock(RentalDeviceAssignmentMapper.class),
                mock(XianyuOrderSyncService.class),
                mock(XianyuRentalConversionService.class),
                objectMapper);

        XianyuOrderRespVO vo = service.getOrderPage(pageParam).getList().get(0);

        assertEquals("张*", vo.getReceiverName());
        assertEquals("138****8000", vo.getReceiverMobile());
        assertEquals("湖南省长沙市***", vo.getReceiverAddress());
    }

    @Test
    void orderPageIncludesRentalItemAssignmentAndWaybill() {
        XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
        RentalOrderItemMapper itemMapper = mock(RentalOrderItemMapper.class);
        RentalDeviceAssignmentMapper assignmentMapper = mock(RentalDeviceAssignmentMapper.class);
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(69L)
                .shopId(3L)
                .externalOrderId("test-order-6425")
                .orderStatus("21")
                .payAmount(14000L)
                .currency("CNY")
                .conversionStatus("CONVERTED")
                .rentalOrderId(99L)
                .waybillNo("SF1234567890")
                .build();
        RentalOrderItemDO item = RentalOrderItemDO.builder()
                .id(100L)
                .rentalOrderId(99L)
                .equipmentModelCode("POCKET4")
                .quantity(1)
                .occupyStartDate(LocalDate.of(2026, 7, 28))
                .occupyEndDateExclusive(LocalDate.of(2026, 8, 6))
                .build();
        XianyuOrderPageReqVO pageParam = new XianyuOrderPageReqVO();
        when(orderMapper.selectAdminPage(pageParam)).thenReturn(new PageResult<>(List.of(order), 1L));
        when(itemMapper.selectListByRentalOrderIds(List.of(99L))).thenReturn(List.of(item));
        when(assignmentMapper.selectActiveListByRentalOrderIds(List.of(99L))).thenReturn(List.of(
                RentalDeviceAssignmentDO.builder()
                        .id(200L)
                        .rentalOrderId(99L)
                        .rentalOrderItemId(100L)
                        .deviceId(300L)
                        .status("DISPATCHED")
                        .build()));
        XianyuOrderAdminService service = new XianyuOrderAdminService(
                orderMapper,
                mock(XianyuShopMapper.class),
                mock(RentalOrderMapper.class),
                itemMapper,
                assignmentMapper,
                mock(XianyuOrderSyncService.class),
                mock(XianyuRentalConversionService.class),
                objectMapper);

        XianyuOrderRespVO vo = service.getOrderPage(pageParam).getList().get(0);

        assertEquals("SF1234567890", vo.getWaybillNo());
        assertEquals(100L, vo.getRentalOrderItemId());
        assertEquals("POCKET4", vo.getEquipmentModelCode());
        assertEquals(1, vo.getRentalQuantity());
        assertEquals(LocalDate.of(2026, 7, 28), vo.getOccupyStartDate());
        assertEquals(LocalDate.of(2026, 8, 6), vo.getOccupyEndDateExclusive());
        assertEquals(List.of(300L), vo.getAssignedDeviceIds());
    }

    @Test
    void orderPageUsesChannelLogisticsDatesBeforeRentalConversion() {
        XianyuOrderMapper orderMapper = mock(XianyuOrderMapper.class);
        XianyuOrderDO order = XianyuOrderDO.builder()
                .id(69L)
                .shopId(3L)
                .externalOrderId("test-order-6425")
                .orderStatus("12")
                .payAmount(14000L)
                .currency("CNY")
                .conversionStatus("REVIEW_REQUIRED")
                .billableStartDate(LocalDate.of(2026, 7, 29))
                .billableEndDate(LocalDate.of(2026, 8, 5))
                .rentalPeriodStatus("SUCCESS")
                .shipDate(LocalDate.of(2026, 7, 28))
                .returnDate(LocalDate.of(2026, 8, 5))
                .build();
        XianyuOrderPageReqVO pageParam = new XianyuOrderPageReqVO();
        when(orderMapper.selectAdminPage(pageParam)).thenReturn(new PageResult<>(List.of(order), 1L));
        XianyuOrderAdminService service = new XianyuOrderAdminService(
                orderMapper,
                mock(XianyuShopMapper.class),
                mock(RentalOrderMapper.class),
                mock(RentalOrderItemMapper.class),
                mock(RentalDeviceAssignmentMapper.class),
                mock(XianyuOrderSyncService.class),
                mock(XianyuRentalConversionService.class),
                objectMapper);

        XianyuOrderRespVO vo = service.getOrderPage(pageParam).getList().get(0);

        assertEquals(LocalDate.of(2026, 7, 28), vo.getOccupyStartDate());
        assertEquals(LocalDate.of(2026, 8, 6), vo.getOccupyEndDateExclusive());
        assertNull(vo.getRentalOrderItemId());
        assertEquals(List.of(), vo.getAssignedDeviceIds());
    }

}
