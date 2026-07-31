package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import org.apache.ibatis.annotations.Delete;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalLogisticsCleanupMapperContractTest {

    @Test
    void cleanupSqlTargetsOnlyEligibleTechnicalRows() throws Exception {
        String traceSql = deleteSql("deleteCleanupTraces");
        String inboxSql = deleteSql("deleteCleanupInbox");
        String outboxSql = deleteSql("deleteCleanupOutbox");

        assertTrue(traceSql.contains("t.snapshot_version < d.tracking_version"));
        assertTrue(traceSql.contains("t.create_time < #{cutoff}"));
        assertTrue(inboxSql.contains("processing_status = 'SUCCEEDED'"));
        assertTrue(outboxSql.contains("processing_status = 'SUCCEEDED'"));
        assertTrue(inboxSql.contains("LIMIT #{limit}"));
        assertTrue(outboxSql.contains("LIMIT #{limit}"));
        for (String sql : List.of(traceSql, inboxSql, outboxSql)) {
            assertFalse(sql.matches("(?s).*DELETE\\s+FROM\\s+rental_delivery(?:\\s|$).*"));
            assertFalse(sql.contains("DELETE FROM rental_device_shipment"));
            assertFalse(sql.contains("DELETE FROM rental_order"));
        }
    }

    private String deleteSql(String methodName) throws Exception {
        Method method = RentalLogisticsOperationsMapper.class.getMethod(
                methodName, Long.class, LocalDateTime.class, int.class);
        return String.join("\n", method.getAnnotation(Delete.class).value());
    }
}
