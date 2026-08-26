package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceGenerateFromPurchaseReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.CatalogModel;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.DeviceNumberReservation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeviceInboundCategoryTest {

    @Test
    void classifiesKnownModelsAndPreservesUnknownModels() {
        assertEquals("DJI", generateOne("P4P").getCategoryCode());
        assertNull(generateOne("A7M4").getCategoryCode());
    }

    private static RentalDeviceDO generateOne(String modelCode) {
        ErpPurchaseInService purchaseInService = mock(ErpPurchaseInService.class);
        ErpProductService productService = mock(ErpProductService.class);
        RentalDeviceMapper deviceMapper = mock(RentalDeviceMapper.class);
        RentalDeviceCatalogService catalogService = mock(RentalDeviceCatalogService.class);
        RentalDeviceInboundService service = new RentalDeviceInboundService(
                purchaseInService,
                productService,
                mock(ErpWarehouseService.class),
                deviceMapper,
                catalogService);

        when(purchaseInService.getPurchaseIn(10L)).thenReturn(ErpPurchaseInDO.builder()
                .id(10L).no("PI2026").status(ErpAuditStatus.APPROVE.getStatus()).build());
        when(purchaseInService.getPurchaseInItemListByInId(10L)).thenReturn(List.of(
                ErpPurchaseInItemDO.builder()
                        .id(100L)
                        .productId(7L)
                        .count(BigDecimal.ONE)
                        .productPrice(BigDecimal.ONE)
                        .build()));
        when(productService.getProduct(7L)).thenReturn(ErpProductDO.builder()
                .id(7L).barCode(modelCode).name(modelCode).build());
        if ("P4P".equals(modelCode)) {
            CatalogModel catalogModel =
                    new CatalogModel(1L, "DJI", "大疆", 2L, "P4P", "P4P", "P4P");
            when(catalogService.findEnabledModel("P4P")).thenReturn(Optional.of(catalogModel));
            when(catalogService.reserveDeviceNumbers("DJI", "P4P", 1))
                    .thenReturn(new DeviceNumberReservation(catalogModel, List.of("P4P-01")));
        } else {
            when(catalogService.findEnabledModel(modelCode)).thenReturn(Optional.empty());
        }
        when(deviceMapper.countBySourceItem("ERP_PURCHASE_IN", 10L, 100L)).thenReturn(0L);
        when(deviceMapper.insert(any(RentalDeviceDO.class))).thenAnswer(invocation -> {
            RentalDeviceDO device = invocation.getArgument(0);
            device.setId(101L);
            return 1;
        });

        RentalDeviceGenerateFromPurchaseReqVO reqVO = new RentalDeviceGenerateFromPurchaseReqVO();
        reqVO.setPurchaseInId(10L);
        service.generateFromPurchaseIn(reqVO);

        ArgumentCaptor<RentalDeviceDO> captor = ArgumentCaptor.forClass(RentalDeviceDO.class);
        verify(deviceMapper).insert(captor.capture());
        return captor.getValue();
    }

}
