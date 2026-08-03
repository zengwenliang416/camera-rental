package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

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

}
