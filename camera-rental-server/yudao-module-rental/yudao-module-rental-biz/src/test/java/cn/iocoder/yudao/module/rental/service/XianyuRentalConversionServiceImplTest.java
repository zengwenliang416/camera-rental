package cn.iocoder.yudao.module.rental.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalManualReviewDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalOrderItemDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuProductMappingDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalManualReviewMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderItemMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuProductMappingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuRentalConversionServiceImplTest {

    @Mock
    private XianyuOrderMapper xianyuOrderMapper;
    @Mock
    private XianyuProductMappingMapper productMappingMapper;
    @Mock
    private RentalOrderMapper rentalOrderMapper;
    @Mock
    private RentalOrderItemMapper rentalOrderItemMapper;
    @Mock
    private RentalManualReviewMapper manualReviewMapper;

    private XianyuRentalConversionService service;

    @BeforeEach
    void setUp() {
        service = new XianyuRentalConversionServiceImpl(xianyuOrderMapper, productMappingMapper, rentalOrderMapper,
                rentalOrderItemMapper, manualReviewMapper, new SellerRemarkRentalPeriodParser());
    }

    @Test
    void shouldConvertMappedOrderOnceWithIntegerCentAmounts() {
        XianyuOrderDO source = sourceOrder();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(productMappingMapper.selectByShopProductSkuForUpdate(7L, "product-1", "sku-1"))
                .thenReturn(XianyuProductMappingDO.builder().equipmentModelCode("A7M4")
                        .mappingStatus("MAPPED").build());
        doAnswer(invocation -> {
            invocation.getArgument(0, RentalOrderDO.class).setId(31L);
            return 1;
        }).when(rentalOrderMapper).insert(any(RentalOrderDO.class));

        RentalConversionResult result = service.convert(10L);

        assertEquals("CONVERTED", result.status());
        assertEquals(31L, result.rentalOrderId());
        ArgumentCaptor<RentalOrderDO> orderCaptor = ArgumentCaptor.forClass(RentalOrderDO.class);
        verify(rentalOrderMapper).insert(orderCaptor.capture());
        assertEquals(4_294_967_296L, orderCaptor.getValue().getRentAmount());
        assertEquals("7:order-1", orderCaptor.getValue().getSourceOrderId());
        assertEquals(LocalDateTime.of(2026, 7, 25, 0, 0).toLocalDate(),
                orderCaptor.getValue().getBillableStartDate());
        ArgumentCaptor<RentalOrderItemDO> itemCaptor = ArgumentCaptor.forClass(RentalOrderItemDO.class);
        verify(rentalOrderItemMapper).insert(itemCaptor.capture());
        assertEquals(4_294_967_296L, itemCaptor.getValue().getRentAmount());
        assertEquals("CONVERTED", source.getConversionStatus());
    }

    @Test
    void shouldReuseExistingConversionWithoutCreatingAnotherOrder() {
        XianyuOrderDO source = sourceOrder();
        source.setRentalOrderId(31L);
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);

        RentalConversionResult result = service.convert(10L);

        assertEquals("CONVERTED", result.status());
        assertEquals(31L, result.rentalOrderId());
        verify(productMappingMapper, never()).selectByShopProductSkuForUpdate(any(), any(), any());
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).insert(any(RentalOrderItemDO.class));
    }

    @Test
    void shouldCreateOneReviewWhenExplicitProductMappingIsMissing() {
        XianyuOrderDO source = sourceOrder();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(productMappingMapper.selectByShopProductSkuForUpdate(7L, "product-1", "sku-1")).thenReturn(null);
        doAnswer(invocation -> {
            invocation.getArgument(0, RentalManualReviewDO.class).setId(41L);
            return 1;
        }).when(manualReviewMapper).insert(any(RentalManualReviewDO.class));

        RentalConversionResult result = service.convert(10L);

        assertEquals("REVIEW_REQUIRED", result.status());
        assertEquals(41L, result.reviewId());
        assertEquals("PRODUCT_MAPPING_REQUIRED", result.reasonCode());
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).insert(any(RentalOrderItemDO.class));
        assertEquals("REVIEW_REQUIRED", source.getConversionStatus());
    }

    @Test
    void shouldReuseTheExistingReviewOnReplay() {
        XianyuOrderDO source = sourceOrder();
        RentalManualReviewDO existingReview = RentalManualReviewDO.builder()
                .id(41L)
                .reasonCode("PRODUCT_MAPPING_REQUIRED")
                .build();
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(source);
        when(productMappingMapper.selectByShopProductSkuForUpdate(7L, "product-1", "sku-1")).thenReturn(null);
        when(manualReviewMapper.selectBySourceAndReviewTypeForUpdate("XIANYU_ORDER", "10", "ORDER_CONVERSION"))
                .thenReturn(existingReview);

        RentalConversionResult result = service.convert(10L);

        assertEquals("REVIEW_REQUIRED", result.status());
        assertEquals(41L, result.reviewId());
        verify(manualReviewMapper, never()).insert(any(RentalManualReviewDO.class));
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
    }

    @Test
    void shouldNotCreateReviewForAnUnknownChannelOrder() {
        when(xianyuOrderMapper.selectByIdForUpdate(10L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.convert(10L));
        assertEquals(XIANYU_ORDER_NOT_EXISTS.getCode(), ex.getCode());
        assertEquals(XIANYU_ORDER_NOT_EXISTS.getMsg(), ex.getMessage());

        verify(manualReviewMapper, never()).insert(any(RentalManualReviewDO.class));
        verify(rentalOrderMapper, never()).insert(any(RentalOrderDO.class));
        verify(rentalOrderItemMapper, never()).insert(any(RentalOrderItemDO.class));
    }

    private XianyuOrderDO sourceOrder() {
        return XianyuOrderDO.builder()
                .id(10L)
                .shopId(7L)
                .externalOrderId("order-1")
                .externalProductId("product-1")
                .externalSkuId("sku-1")
                .payAmount(4_294_967_296L)
                .sellerRemark("#租期7.25-7.27#")
                .sourceCreatedAt(LocalDateTime.of(2026, 7, 20, 10, 0))
                .build();
    }

}
