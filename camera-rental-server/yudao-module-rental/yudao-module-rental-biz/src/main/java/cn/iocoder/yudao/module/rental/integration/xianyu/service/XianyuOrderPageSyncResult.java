package cn.iocoder.yudao.module.rental.integration.xianyu.service;

/**
 * Redacted result for one local order-page synchronization run.
 */
public record XianyuOrderPageSyncResult(Long syncRunId, int receivedCount, int succeededCount,
                                        boolean cursorAdvanced) {
}
