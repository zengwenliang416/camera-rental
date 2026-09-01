package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuOrderPageReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XianyuOrderMapperTest {

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                XianyuOrderDO.class);
    }

    @Test
    void pendingShipQueryAcceptsNullAndHistoricalBlankWaybillValues() {
        String sql = XianyuOrderMapper.pendingShipQuery(null, null, Set.of("12"))
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("waybill_no is null"));
        assertTrue(sql.contains("trim(waybill_no) ="));
        assertTrue(sql.contains("consign_time is null"));
        assertTrue(sql.contains("cancel_time is null"));
    }

    @Test
    void pendingShipQuerySearchesAllOperatorFacingOrderFields() {
        String sql = XianyuOrderMapper.pendingShipQuery(null, "19900000000", Set.of("12"))
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("external_order_id like"));
        assertTrue(sql.contains("receiver_name like"));
        assertTrue(sql.contains("receiver_mobile like"));
        assertTrue(sql.contains("buyer_nick like"));
        assertTrue(sql.contains("goods_title like"));
    }

    @Test
    void receiverMobileLast4QueryOnlyReturnsConvertedCandidates() {
        String sql = XianyuOrderMapper.receiverMobileLast4Query("8000")
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("rental_order_id is not null"));
        assertTrue(sql.contains("receiver_mobile like"));
        assertTrue(sql.contains("limit 3"));
    }

    @Test
    void adminPageQueryFiltersExactOrderStatus() {
        XianyuOrderPageReqVO request = new XianyuOrderPageReqVO();
        request.setOrderStatus("21");

        String sql = XianyuOrderMapper.adminPageQuery(request)
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("order_status ="));
    }

    @Test
    void adminPageQueryUsesOnlyExplicitProductAndSkuIdentifiers() {
        XianyuOrderPageReqVO request = new XianyuOrderPageReqVO();
        request.setXgjProductId("xgj-product-1");
        request.setXianyuItemId("xianyu-item-1");
        request.setXgjSkuId("xgj-sku-1");
        request.setXianyuSkuId("xianyu-sku-1");

        String sql = XianyuOrderMapper.adminPageQuery(request)
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("xgj_product_id ="));
        assertTrue(sql.contains("xianyu_item_id ="));
        assertTrue(sql.contains("xgj_sku_id ="));
        assertTrue(sql.contains("xianyu_sku_id ="));
        assertFalse(sql.contains("external_product_id"));
        assertFalse(sql.contains("external_sku_id"));
    }

    @Test
    void adminPageQueryFiltersExactParsedShipDate() {
        XianyuOrderPageReqVO request = new XianyuOrderPageReqVO();
        request.setShipDate(LocalDate.of(2026, 8, 6));

        String sql = XianyuOrderMapper.adminPageQuery(request)
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("ship_date ="));
    }

    @Test
    void adminPageQueryUsesInclusiveRentalPeriodOverlapWithoutJoiningOrderItems() {
        XianyuOrderPageReqVO request = new XianyuOrderPageReqVO();
        request.setRentalStartDate(LocalDate.of(2026, 8, 8));
        request.setRentalEndDate(LocalDate.of(2026, 8, 17));

        String sql = XianyuOrderMapper.adminPageQuery(request)
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("ro.billable_start_date <="));
        assertTrue(sql.contains("ro.billable_end_date >="));
        assertTrue(sql.contains("billable_start_date <="));
        assertTrue(sql.contains("billable_end_date >="));
        assertTrue(sql.contains("exists"));
        assertFalse(sql.contains("rental_order_item"));
    }

    @Test
    void adminPageQueryFallsBackToChannelPeriodOnlyWhenConvertedPeriodIsUnavailable() {
        XianyuOrderPageReqVO request = new XianyuOrderPageReqVO();
        request.setRentalStartDate(LocalDate.of(2026, 8, 8));
        request.setRentalEndDate(LocalDate.of(2026, 8, 17));

        String sql = XianyuOrderMapper.adminPageQuery(request)
                .getCustomSqlSegment()
                .toLowerCase();

        assertTrue(sql.contains("not exists"));
        assertTrue(sql.contains("ro_period.billable_start_date is not null"));
        assertTrue(sql.contains("ro_period.billable_end_date is not null"));
        assertTrue(sql.contains("ro.tenant_id = xianyu_order.tenant_id"));
    }

    @Test
    void rentalDateRangeRequiresBothInclusiveBoundaries() {
        XianyuOrderPageReqVO request = new XianyuOrderPageReqVO();
        assertTrue(request.isRentalDateRangeComplete());
        assertTrue(request.isRentalDateRangeValid());

        request.setRentalStartDate(LocalDate.of(2026, 8, 8));
        assertFalse(request.isRentalDateRangeComplete());

        request.setRentalEndDate(LocalDate.of(2026, 8, 8));
        assertTrue(request.isRentalDateRangeComplete());
        assertTrue(request.isRentalDateRangeValid());

        request.setRentalEndDate(LocalDate.of(2026, 8, 7));
        assertFalse(request.isRentalDateRangeValid());
    }

}
