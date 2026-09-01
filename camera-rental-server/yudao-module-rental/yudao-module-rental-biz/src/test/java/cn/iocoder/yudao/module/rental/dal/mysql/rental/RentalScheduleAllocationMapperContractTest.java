package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalScheduleAllocationMapperContractTest {

    @Test
    void pendingAllocationQueriesRequireAuthoritativeReadyStatus() throws Exception {
        assertReadyGate("selectPendingAllocationOrders",
                Long.class, String.class, String.class, long.class, int.class);
        assertReadyGate("countPendingAllocationOrders",
                Long.class, String.class, String.class);
    }

    private void assertReadyGate(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = RentalScheduleAllocationMapper.class.getMethod(methodName, parameterTypes);
        String sql = String.join("\n", method.getAnnotation(Select.class).value());
        assertTrue(sql.contains("ro.status = 'PENDING_ALLOCATION'"));
        assertTrue(sql.contains("ro.preparation_status = 'READY'"));
    }

}
