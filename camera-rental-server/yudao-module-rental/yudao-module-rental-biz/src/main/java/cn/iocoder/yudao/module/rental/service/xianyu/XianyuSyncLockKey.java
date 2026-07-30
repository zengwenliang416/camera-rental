package cn.iocoder.yudao.module.rental.service.xianyu;

/**
 * Centralizes tenant-scoped synchronization lock names shared by jobs and admin operations.
 */
public final class XianyuSyncLockKey {

    public static final String ORDER_RESOURCE = "order";

    private static final String PREFIX = "camera-rental:xianyu:sync:";

    private XianyuSyncLockKey() {
    }

    public static String forResource(Long tenantId, String resource) {
        return PREFIX + tenantId + ":" + resource;
    }

}
