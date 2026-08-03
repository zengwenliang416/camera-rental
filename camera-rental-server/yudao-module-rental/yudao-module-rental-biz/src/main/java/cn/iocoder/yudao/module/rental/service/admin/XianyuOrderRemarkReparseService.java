package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuOrderPersistenceService;
import cn.iocoder.yudao.module.rental.service.xianyu.XianyuSyncLockKey;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_ORDER_REPARSE_BUSY;

@Service
public class XianyuOrderRemarkReparseService {

    private static final int MAX_ORDER_COUNT = 10_000;

    private final XianyuOrderPersistenceService orderPersistenceService;
    private final RedissonClient redissonClient;

    public XianyuOrderRemarkReparseService(XianyuOrderPersistenceService orderPersistenceService,
                                           RedissonClient redissonClient) {
        this.orderPersistenceService = orderPersistenceService;
        this.redissonClient = redissonClient;
    }

    public int reparse(int maxOrders) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        RLock lock = redissonClient.getLock(
                XianyuSyncLockKey.forResource(tenantId, XianyuSyncLockKey.ORDER_RESOURCE));
        if (!lock.tryLock()) {
            throw exception(XIANYU_ORDER_REPARSE_BUSY);
        }
        try {
            return reparseLocked(maxOrders);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private int reparseLocked(int maxOrders) {
        int boundedMax = Math.max(1, Math.min(MAX_ORDER_COUNT, maxOrders));
        return orderPersistenceService.reparseRentalPeriods(boundedMax);
    }

}
