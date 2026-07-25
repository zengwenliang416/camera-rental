package cn.iocoder.yudao.module.rental.integration.xianyu.service;

public record XianyuProductPageSyncResult(
        Long syncRunId,
        int receivedCount,
        int succeededCount,
        int deduplicatedCount,
        int skuCount
) {
}
