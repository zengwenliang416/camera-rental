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

}
