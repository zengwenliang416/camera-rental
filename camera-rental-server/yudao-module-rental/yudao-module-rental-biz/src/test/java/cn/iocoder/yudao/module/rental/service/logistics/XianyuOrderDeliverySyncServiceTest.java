package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryDirectionEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuOrderDeliverySyncServiceTest {

    private final RentalDeliveryService deliveryService = mock(RentalDeliveryService.class);
    private final XianyuOrderDeliverySyncService service =
            new XianyuOrderDeliverySyncService(deliveryService);

    @Test
    void createsOutboundDeliveryForTrackableShippedRentalOrder() {
        RentalDeliveryResult expected = new RentalDeliveryResult(
                99L, true, "READY", "PROVIDER_DISABLED", "PENDING",
                "SF1****7890", null, List.of());
        when(deliveryService.createOrReuse(any())).thenReturn(expected);
        XianyuOrderDO order = trackableOrder();

        RentalDeliveryResult result = service.syncOutboundIfTrackable(order);

        assertSame(expected, result);
        ArgumentCaptor<RentalDeliveryCreateCommand> captor =
                ArgumentCaptor.forClass(RentalDeliveryCreateCommand.class);
        verify(deliveryService).createOrReuse(captor.capture());
        RentalDeliveryCreateCommand command = captor.getValue();
        assertNull(command.rentalOrderId());
        assertSame(order.getId(), command.channelOrderId());
        assertSame(RentalDeliveryDirectionEnum.OUTBOUND, command.direction());
        assertTrue(command.devices().isEmpty());
    }

    @Test
    void acceptsSupportedShippedStatusAliases() {
        XianyuOrderDO order = trackableOrder();

        order.setOrderStatus("SHIPPED");
        assertTrue(service.isTrackable(order));
        order.setOrderStatus("consigned");
        assertTrue(service.isTrackable(order));
    }

    @Test
    void rejectsLocalDeliveryGeneralTradeAndPlaceholderWaybills() {
        XianyuOrderDO order = trackableOrder();

        order.setConsignType(0);
        assertFalse(service.isTrackable(order));
        order.setConsignType(1);
        order.setExpressCode("general");
        assertFalse(service.isTrackable(order));
        order.setExpressCode("SHUNFENG");
        order.setWaybillNo("0000");
        assertFalse(service.isTrackable(order));
    }

    @Test
    void rejectsNonRentalAndUnshippedOrdersWithoutCreatingDelivery() {
        XianyuOrderDO order = trackableOrder();
        order.setRentalPeriodStatus(null);
        order.setSellerRemark(null);
        order.setGoodsTitle("二手相机出售");

        assertNull(service.syncOutboundIfTrackable(order));
        order.setGoodsTitle("免押相机租赁");
        order.setOrderStatus("12");
        assertNull(service.syncOutboundIfTrackable(order));
        verify(deliveryService, never()).createOrReuse(any());
    }

    private XianyuOrderDO trackableOrder() {
        return XianyuOrderDO.builder()
                .id(123L)
                .orderStatus("21")
                .consignType(1)
                .waybillNo("SF1234567890")
                .expressCode("SHUNFENG")
                .expressName("顺丰速运")
                .receiverMobile("13800138000")
                .rentalPeriodStatus("SUCCESS")
                .goodsTitle("免押相机租赁")
                .build();
    }
}
