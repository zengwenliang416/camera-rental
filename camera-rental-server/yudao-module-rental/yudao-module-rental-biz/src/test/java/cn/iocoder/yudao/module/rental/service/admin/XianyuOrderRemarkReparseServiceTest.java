package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ORDER_REPARSE_BUSY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuOrderRemarkReparseServiceTest {

    private XianyuOrderPersistenceService persistenceService;
    private RedissonClient redissonClient;
    private RLock lock;
    private XianyuOrderRemarkReparseService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        persistenceService = mock(XianyuOrderPersistenceService.class);
        redissonClient = mock(RedissonClient.class);
        lock = mock(RLock.class);
        when(redissonClient.getLock("camera-rental:xianyu:sync:9:order")).thenReturn(lock);
        service = new XianyuOrderRemarkReparseService(persistenceService, redissonClient);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void reparseProcessesOutdatedRowsInBoundedBatchesUnderOrderSyncLock() {
        when(lock.tryLock()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(persistenceService.backfillMissingRentalPeriods(500)).thenReturn(500, 200);

        int processed = service.reparse(5_000);

        assertEquals(700, processed);
        verify(persistenceService, times(2)).backfillMissingRentalPeriods(500);
        verify(lock).unlock();
    }

    @Test
    void reparseRejectsConcurrentOrderSynchronization() {
        when(lock.tryLock()).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class, () -> service.reparse(5_000));

        assertEquals(XIANYU_ORDER_REPARSE_BUSY.getCode(), exception.getCode());
        verify(persistenceService, never()).backfillMissingRentalPeriods(500);
        verify(lock, never()).unlock();
    }

}
