package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalLogisticsBackfillMapperContractTest {

    @Test
    void backfillSqlIsTenantScopedBoundedAndIdempotent() throws Exception {
        Method select = RentalLogisticsOperationsMapper.class.getMethod(
                "selectBackfillCandidates", Long.class, int.class);
        String selectSql = String.join("\n", select.getAnnotation(Select.class).value());
        assertTrue(selectSql.contains("s.tenant_id = #{tenantId}"));
        assertTrue(selectSql.contains("s.delivery_id IS NULL"));
        assertTrue(selectSql.contains("LIMIT #{limit}"));
        assertFalse(selectSql.toUpperCase().contains("INSERT INTO RENTAL_DELIVERY"));

        Method bind = RentalLogisticsOperationsMapper.class.getMethod(
                "bindShipmentDelivery", Long.class, Long.class, Long.class);
        String bindSql = String.join("\n", bind.getAnnotation(Update.class).value());
        assertTrue(bindSql.contains("tenant_id = #{tenantId}"));
        assertTrue(bindSql.contains("delivery_id IS NULL"));
    }
}
