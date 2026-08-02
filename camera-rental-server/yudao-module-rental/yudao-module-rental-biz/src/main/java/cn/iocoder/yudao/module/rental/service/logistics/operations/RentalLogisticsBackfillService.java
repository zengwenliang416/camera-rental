package cn.iocoder.yudao.module.rental.service.logistics.operations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.logistics.RentalLogisticsOperationsMapper;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsException;
import cn.iocoder.yudao.module.rental.service.logistics.WaybillPrivacy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.module.rental.service.logistics.operations.RentalLogisticsOperationsModels.*;

@Service
public class RentalLogisticsBackfillService {

    private static final int MAX_LIMIT = 100;

    private final RentalLogisticsOperationsMapper operationsMapper;
    private final RentalLogisticsBackfillTransactionService transactionService;
    private final WaybillPrivacy waybillPrivacy;

    public RentalLogisticsBackfillService(RentalLogisticsOperationsMapper operationsMapper,
                                          RentalLogisticsBackfillTransactionService transactionService,
                                          WaybillPrivacy waybillPrivacy) {
        this.operationsMapper = operationsMapper;
        this.transactionService = transactionService;
        this.waybillPrivacy = waybillPrivacy;
    }

    public BackfillResult backfill(BackfillCommand command) {
        BackfillCommand bounded = normalize(command);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<RentalLogisticsOperationsMapper.BackfillCandidateRow> candidates = new ArrayList<>(
                operationsMapper.selectBackfillCandidates(
                        tenantId, bounded.consignDateStart(), bounded.consignDateEnd(), bounded.limit()));
        if (candidates.size() < bounded.limit()) {
            candidates.addAll(operationsMapper.selectChannelOrderBackfillCandidates(
                    tenantId, bounded.consignDateStart(), bounded.consignDateEnd(),
                    bounded.limit() - candidates.size()));
        }
        List<BackfillItem> items = new ArrayList<>();
        int applied = 0;
        int skipped = 0;
        for (RentalLogisticsOperationsMapper.BackfillCandidateRow candidate : candidates) {
            String invalidReason = validateCandidate(candidate);
            if (invalidReason != null) {
                skipped++;
                items.add(new BackfillItem(candidate.getShipmentId(), null,
                        maskIfPresent(candidate.getWaybillNo()), "SKIPPED", invalidReason));
                continue;
            }
            if (bounded.dryRun()) {
                items.add(new BackfillItem(candidate.getShipmentId(), null,
                        maskIfPresent(candidate.getWaybillNo()), "ELIGIBLE", "DRY_RUN"));
                continue;
            }
            try {
                RentalDeliveryResult result = transactionService.apply(
                        candidate, bounded.enqueueProviderTasks());
                applied++;
                items.add(new BackfillItem(candidate.getShipmentId(), result.deliveryId(),
                        result.maskedWaybillNo(), result.created() ? "CREATED" : "REUSED",
                        bounded.enqueueProviderTasks() ? "PROVIDER_TASKS_REQUESTED" : "LOCAL_DELIVERY_ONLY"));
            } catch (RentalLogisticsException ex) {
                skipped++;
                items.add(new BackfillItem(candidate.getShipmentId(), null,
                        maskIfPresent(candidate.getWaybillNo()), "SKIPPED", ex.getCode()));
            }
        }
        boolean providerTasksEnqueued = bounded.enqueueProviderTasks() && applied > 0;
        String providerTaskReason = providerTasksEnqueued
                ? "PROVIDER_TASKS_REQUESTED" : "PROVIDER_ENQUEUE_DISABLED";
        int distinctWaybillCount = (int) candidates.stream()
                .filter(candidate -> StringUtils.hasText(candidate.getWaybillNo())
                        && StringUtils.hasText(candidate.getExpressCode()))
                .map(candidate -> candidate.getExpressCode().trim().toUpperCase()
                        + ":" + candidate.getWaybillNo().trim().toUpperCase())
                .distinct()
                .count();
        return new BackfillResult(bounded.dryRun(), bounded.limit(), candidates.size(),
                distinctWaybillCount, applied, skipped, providerTasksEnqueued, providerTaskReason, items);
    }

    private BackfillCommand normalize(BackfillCommand command) {
        if (command == null) {
            return new BackfillCommand(true, 20, false);
        }
        if (command.limit() < 1 || command.limit() > MAX_LIMIT) {
            throw new RentalLogisticsException("BACKFILL_LIMIT_OUT_OF_RANGE");
        }
        if (command.consignDateStart() != null && command.consignDateEnd() != null
                && command.consignDateStart().isAfter(command.consignDateEnd())) {
            throw new RentalLogisticsException("BACKFILL_DATE_RANGE_INVALID");
        }
        return command;
    }

    private String validateCandidate(RentalLogisticsOperationsMapper.BackfillCandidateRow candidate) {
        if (candidate.getChannelOrderId() == null) {
            return "BACKFILL_CHANNEL_ORDER_MISSING";
        }
        if (candidate.getShipmentId() != null && (candidate.getRentalOrderId() == null
                || candidate.getRentalOrderItemId() == null || candidate.getAssignmentId() == null
                || candidate.getDeviceId() == null)) {
            return "BACKFILL_RELATION_INCOMPLETE";
        }
        if (!StringUtils.hasText(candidate.getWaybillNo())) {
            return "BACKFILL_WAYBILL_MISSING";
        }
        if (!StringUtils.hasText(candidate.getExpressCode())) {
            return "BACKFILL_CARRIER_MISSING";
        }
        return null;
    }

    private String maskIfPresent(String waybillNo) {
        if (!StringUtils.hasText(waybillNo)) {
            return null;
        }
        try {
            return waybillPrivacy.mask(waybillNo);
        } catch (RentalLogisticsException ex) {
            return null;
        }
    }
}
