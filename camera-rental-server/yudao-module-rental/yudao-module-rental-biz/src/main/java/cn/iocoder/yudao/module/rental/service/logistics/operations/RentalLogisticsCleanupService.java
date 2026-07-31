package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.*;

@Service
public class RentalLogisticsCleanupService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MIN_RETENTION_DAYS = 30;
    private static final int MAX_RETENTION_DAYS = 3650;
    private static final int MAX_LIMIT = 1000;

    private final RentalLogisticsOperationsMapper operationsMapper;

    public RentalLogisticsCleanupService(RentalLogisticsOperationsMapper operationsMapper) {
        this.operationsMapper = operationsMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public CleanupResult cleanup(CleanupCommand command) {
        CleanupCommand bounded = normalize(command);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime cutoff = LocalDateTime.now(BUSINESS_ZONE).minusDays(bounded.retentionDays());
        if (bounded.dryRun()) {
            int traceCount = operationsMapper.countCleanupTraces(tenantId, cutoff, bounded.limit());
            int remaining = Math.max(0, bounded.limit() - traceCount);
            int inboxCount = remaining == 0 ? 0
                    : operationsMapper.countCleanupInbox(tenantId, cutoff, remaining);
            remaining = Math.max(0, remaining - inboxCount);
            int outboxCount = remaining == 0 ? 0
                    : operationsMapper.countCleanupOutbox(tenantId, cutoff, remaining);
            return new CleanupResult(true, bounded.retentionDays(), bounded.limit(),
                    traceCount, inboxCount, outboxCount);
        }
        int traceCount = operationsMapper.deleteCleanupTraces(tenantId, cutoff, bounded.limit());
        int remaining = Math.max(0, bounded.limit() - traceCount);
        int inboxCount = remaining == 0 ? 0 : operationsMapper.deleteCleanupInbox(tenantId, cutoff, remaining);
        remaining = Math.max(0, remaining - inboxCount);
        int outboxCount = remaining == 0 ? 0 : operationsMapper.deleteCleanupOutbox(tenantId, cutoff, remaining);
        return new CleanupResult(false, bounded.retentionDays(), bounded.limit(),
                traceCount, inboxCount, outboxCount);
    }

    private CleanupCommand normalize(CleanupCommand command) {
        if (command == null) {
            return new CleanupCommand(true, 90, 500);
        }
        if (command.retentionDays() < MIN_RETENTION_DAYS
                || command.retentionDays() > MAX_RETENTION_DAYS) {
            throw new RentalLogisticsException("CLEANUP_RETENTION_OUT_OF_RANGE");
        }
        if (command.limit() < 1 || command.limit() > MAX_LIMIT) {
            throw new RentalLogisticsException("CLEANUP_LIMIT_OUT_OF_RANGE");
        }
        return command;
    }
}
