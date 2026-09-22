package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceQuantityUpdateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalChannelOrderEligibilityPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceAssignmentDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceShipmentDO;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class RentalOrderQuantityServiceTest {
    final XianyuOrderMapper channels = mock(XianyuOrderMapper.class);
    final RentalOrderMapper orders = mock(RentalOrderMapper.class);
    final RentalOrderItemMapper items = mock(RentalOrderItemMapper.class);
    final RentalDeviceAssignmentMapper assignments = mock(RentalDeviceAssignmentMapper.class);
    final RentalDeviceShipmentMapper shipments = mock(RentalDeviceShipmentMapper.class);
    final RentalOrderQuantityService service = new RentalOrderQuantityService(channels, orders, items,
            assignments, shipments, new RentalChannelOrderEligibilityPolicy());
    RentalOrderDO order;
    RentalOrderItemDO item;
    XianyuOrderDO channel;

    @BeforeEach void setup() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), ""),
                RentalDeviceAssignmentDO.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), ""),
                RentalDeviceShipmentDO.class);
        TenantContextHolder.setTenantId(1L);
        channel = new XianyuOrderDO();
        channel.setId(30L); channel.setTenantId(1L); channel.setRentalOrderId(10L);
        channel.setOrderStatus("12"); channel.setPayAmount(2800L); channel.setGoodsQuantity(28);
        order = new RentalOrderDO();
        order.setId(10L); order.setTenantId(1L); order.setChannelOrderId(30L);
        order.setSourceType("XIANYU"); order.setStatus("PENDING_ALLOCATION");
        item = new RentalOrderItemDO();
        item.setId(20L); item.setTenantId(1L); item.setRentalOrderId(10L);
        item.setQuantity(28); item.setEquipmentModelCode("TEST-CAMERA");
        when(items.selectById(20L)).thenReturn(item);
        when(orders.selectById(10L)).thenReturn(order);
        when(channels.selectByIdForUpdate(30L)).thenReturn(channel);
        when(orders.selectByIdForUpdate(10L)).thenReturn(order);
        when(items.selectByIdForUpdate(20L)).thenReturn(item);
        when(assignments.selectList(any())).thenReturn(List.of());
        when(shipments.selectList(any())).thenReturn(List.of());
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    RentalDeviceQuantityUpdateReqVO request(int quantity, int expected) {
        var req = new RentalDeviceQuantityUpdateReqVO();
        req.setQuantity(quantity); req.setExpectedQuantity(expected); return req;
    }
    void blocked(int code) {
        assertEquals(code, assertThrows(ServiceException.class,
                () -> service.update(20L, request(1, 28))).getCode());
        verify(items, never()).updateById(any(RentalOrderItemDO.class));
    }
    @Test void amountPaddingBecomesOneWithoutChangingChannelOrModel() {
        assertEquals(1, service.update(20L, request(1, 28)));
        assertEquals(28, channel.getGoodsQuantity()); assertEquals(2800L, channel.getPayAmount());
        assertEquals("TEST-CAMERA", item.getEquipmentModelCode());
        var locks = inOrder(channels, orders, items);
        locks.verify(channels).selectByIdForUpdate(30L);
        locks.verify(orders).selectByIdForUpdate(10L);
        locks.verify(items).selectByIdForUpdate(20L);
        locks.verify(items).updateById(item);
    }
    @Test void transactionAndPermissionsProtectWriteBoundary() throws Exception {
        var method = RentalOrderQuantityService.class.getMethod("update", Long.class, RentalDeviceQuantityUpdateReqVO.class);
        assertNotNull(method.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        var endpoint = cn.iocoder.yudao.module.rental.controller.admin.rental.RentalOrderQuantityController.class
                .getMethod("update", Long.class, RentalDeviceQuantityUpdateReqVO.class);
        String policy = endpoint.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
        assertEquals("@ss.hasAnyPermissions('rental:device:assign', 'rental:xianyu:ship')", policy);
    }
    @Test void historyChecksUseCurrentReadsRatherThanOldTransactionSnapshot() {
        when(assignments.selectList(any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.core.conditions.Wrapper<?> query = invocation.getArgument(0);
            assertTrue(query.getSqlSegment().endsWith("LIMIT 1 FOR UPDATE"));
            return List.of(new RentalDeviceAssignmentDO());
        });
        blocked(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE.getCode());
    }
    @Test void explicitMultipleDevicesAreAllowed() {
        assertEquals(3, service.update(20L, request(3, 28)));
    }
    @Test void concurrentEditIsRejected() {
        item.setQuantity(2); blocked(RENTAL_DEVICE_QUANTITY_CHANGED.getCode());
    }
    @Test void crossTenantIsRejectedBeforeLocks() {
        item.setTenantId(2L); blocked(RENTAL_DEVICE_QUANTITY_NOT_FOUND.getCode());
        verifyNoInteractions(channels);
    }
    @Test void associationChangedIsRejected() {
        channel.setRentalOrderId(99L); blocked(RENTAL_DEVICE_QUANTITY_NOT_FOUND.getCode());
    }
    @Test void anyAssignmentHistoryBlocksEdit() {
        when(assignments.selectList(any())).thenReturn(List.of(new RentalDeviceAssignmentDO()));
        blocked(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE.getCode());
    }
    @Test void shipmentBlocksEdit() {
        when(shipments.selectList(any())).thenReturn(List.of(new RentalDeviceShipmentDO()));
        blocked(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE.getCode());
    }
    @Test void shippedChannelBlocksEdit() {
        channel.setConsignTime(LocalDateTime.now()); blocked(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE.getCode());
    }
    @Test void refundedChannelBlocksEdit() {
        channel.setRefundStatus(5); blocked(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE.getCode());
    }
    @Test void closedChannelBlocksEdit() {
        channel.setOrderStatus("24"); blocked(RENTAL_DEVICE_QUANTITY_NOT_EDITABLE.getCode());
    }
    @Test void invalidNumbersCannotReachDatabase() {
        for (int count : new int[]{0, -1, 1000}) {
            assertEquals(RENTAL_DEVICE_QUANTITY_INVALID.getCode(), assertThrows(ServiceException.class,
                    () -> service.update(20L, request(count, 28))).getCode());
        }
        verifyNoInteractions(channels, orders, items);
    }
}
