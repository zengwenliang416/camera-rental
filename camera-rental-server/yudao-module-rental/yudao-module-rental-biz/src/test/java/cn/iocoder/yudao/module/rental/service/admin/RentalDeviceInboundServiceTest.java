package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceGenerateFromPurchaseReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceGenerateFromPurchaseRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_INBOUND_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeviceInboundServiceTest {

    @Test
    void generatesMissingUnitsIdempotently() {
        ErpPurchaseInService purchaseInService = mock(ErpPurchaseInService.class);
        ErpProductService productService = mock(ErpProductService.class);
        ErpWarehouseService warehouseService = mock(ErpWarehouseService.class);
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceInboundService service = new RentalDeviceInboundService(
                purchaseInService, productService, warehouseService, deviceMapper);

        when(purchaseInService.getPurchaseIn(10L)).thenReturn(ErpPurchaseInDO.builder()
                .id(10L).no("PI2026").status(ErpAuditStatus.APPROVE.getStatus()).build());
        when(purchaseInService.getPurchaseInItemListByInId(10L)).thenReturn(List.of(
                ErpPurchaseInItemDO.builder().id(100L).productId(7L).count(new BigDecimal("3"))
                        .productPrice(new BigDecimal("1999.50")).build()));
        when(productService.getProduct(7L)).thenReturn(ErpProductDO.builder()
                .id(7L).barCode("A7M4").name("索尼A7M4").build());
        when(deviceMapper.countBySourceItem("ERP_PURCHASE_IN", 10L, 100L)).thenReturn(1L);
        when(deviceMapper.selectLatestByDeviceNoPrefix("A7M4-")).thenReturn(
                RentalDeviceDO.builder().deviceNo("A7M4-01").build());
        when(deviceMapper.insert(any(RentalDeviceDO.class))).thenAnswer(inv -> {
            RentalDeviceDO d = inv.getArgument(0);
            d.setId(System.nanoTime());
            return 1;
        });

        RentalDeviceGenerateFromPurchaseReqVO req = new RentalDeviceGenerateFromPurchaseReqVO();
        req.setPurchaseInId(10L);
        RentalDeviceGenerateFromPurchaseRespVO resp = service.generateFromPurchaseIn(req);

        assertEquals(3, resp.getRequestedCount());
        assertEquals(1, resp.getAlreadyExistedCount());
        assertEquals(2, resp.getCreatedCount());
        verify(deviceMapper, times(2)).insert(any(RentalDeviceDO.class));

        ArgumentCaptor<RentalDeviceDO> cap = ArgumentCaptor.forClass(RentalDeviceDO.class);
        verify(deviceMapper, times(2)).insert(cap.capture());
        assertEquals("A7M4-02", cap.getAllValues().get(0).getDeviceNo());
        assertEquals("A7M4", cap.getAllValues().get(0).getEquipmentModelCode());
        assertEquals(199950, cap.getAllValues().get(0).getPurchaseAmount());
    }

    @Test
    void rejectsUnapprovedPurchaseIn() {
        ErpPurchaseInService purchaseInService = mock(ErpPurchaseInService.class);
        RentalDeviceInboundService service = new RentalDeviceInboundService(
                purchaseInService, mock(ErpProductService.class), mock(ErpWarehouseService.class),
                mock(RentalDeviceMapper.class));
        when(purchaseInService.getPurchaseIn(1L)).thenReturn(ErpPurchaseInDO.builder()
                .id(1L).status(ErpAuditStatus.PROCESS.getStatus()).build());

        RentalDeviceGenerateFromPurchaseReqVO req = new RentalDeviceGenerateFromPurchaseReqVO();
        req.setPurchaseInId(1L);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.generateFromPurchaseIn(req));
        assertEquals(RENTAL_DEVICE_INBOUND_FAILED.getCode(), ex.getCode());
    }

}
