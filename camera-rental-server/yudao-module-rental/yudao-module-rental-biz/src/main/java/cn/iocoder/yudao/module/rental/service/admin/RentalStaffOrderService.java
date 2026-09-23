package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.*;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.*;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RentalStaffOrderService {
    private final RentalStaffOrderMapper mapper;
    private final RentalOrderItemMapper items;
    private final RentalDeviceAssignmentMapper assignments;
    private final RentalOrderDeliveryMapper deliveries;

    public RentalStaffOrderService(RentalStaffOrderMapper mapper, RentalOrderItemMapper items,
            RentalDeviceAssignmentMapper assignments, RentalOrderDeliveryMapper deliveries) {
        this.mapper = mapper;
        this.items = items;
        this.assignments = assignments;
        this.deliveries = deliveries;
    }

    public PageResult<RentalStaffOrderRespVO> getPage(RentalStaffOrderPageReqVO req) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        String queue = req.getQueue() == null ? "ALL" : req.getQueue();
        String keyword = req.getKeyword() == null ? "" : req.getKeyword().trim().toLowerCase(Locale.ROOT);
        long offset = (long) (req.getPageNo() - 1) * req.getPageSize();
        if (keyword.isEmpty()) {
            long total = mapper.count(tenant, queue);
            List<RentalStaffOrderRespVO> rows = mapper.selectPage(tenant, queue, null, null, offset, req.getPageSize());
            hydrate(rows, tenant);
            return new PageResult<>(rows, total);
        }
        // 线下联系方式由既有 TypeHandler 解密，不能对密文 LIKE 或新增明文副本。
        // 每次只持有 200 条候选，并按主键游标扫描；分页总数在服务端过滤后计算。
        List<RentalStaffOrderRespVO> page = new ArrayList<>();
        long total = 0;
        Long before = null;
        String pattern = likePattern(keyword);
        while (true) {
            List<RentalStaffOrderRespVO> batch = mapper.selectPage(tenant, queue, pattern, before, 0, 200);
            if (batch.isEmpty()) break;
            hydrate(batch, tenant);
            for (RentalStaffOrderRespVO row : batch) {
                if (matches(row, keyword)) {
                    if (total >= offset && page.size() < req.getPageSize()) page.add(row);
                    total++;
                }
            }
            before = batch.get(batch.size() - 1).getId();
            if (batch.size() < 200) break;
        }
        return new PageResult<>(page, total);
    }

    /** Channel visibility is read-only: never convert, infer dates, or allocate while searching. */
    public PageResult<RentalStaffOrderRespVO> getChannelPage(RentalStaffOrderPageReqVO req) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        String keyword = req.getKeyword() == null ? "" : req.getKeyword().trim().toLowerCase(Locale.ROOT);
        String pattern = keyword.isEmpty() ? null : likePattern(keyword);
        long total = mapper.countChannel(tenant, pattern);
        long offset = (long) (req.getPageNo() - 1) * req.getPageSize();
        return new PageResult<>(mapper.selectChannelPage(tenant, pattern, offset, req.getPageSize()), total);
    }

    private static String likePattern(String keyword) {
        return "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    static boolean matches(RentalStaffOrderRespVO row, String keyword) {
        List<String> values = new ArrayList<>(Arrays.asList(row.getOrderNo(), row.getExternalOrderNo(),
                row.getGoodsTitle(), row.getReceiverName(), row.getReceiverMobile(), row.getReceiverAddress()));
        if (row.getItems() != null) row.getItems().forEach(item -> values.add(item.getEquipmentModelCode()));
        return values.stream().filter(Objects::nonNull).anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(keyword));
    }

    private void hydrate(List<RentalStaffOrderRespVO> rows, Long tenant) {
        if (rows.isEmpty()) return;
        List<Long> ids = rows.stream().map(RentalStaffOrderRespVO::getId).toList();
        var itemRows = items.selectListByRentalOrderIds(ids).stream()
                .filter(item -> tenant.equals(item.getTenantId())).collect(Collectors.groupingBy(RentalOrderItemDO::getRentalOrderId));
        // 已回仓的分配也占据该订单明细的履约数量，不重新显示为待分配。
        var assignmentRows = assignments.selectList(new LambdaQueryWrapper<RentalDeviceAssignmentDO>()
                .in(RentalDeviceAssignmentDO::getRentalOrderId, ids).eq(RentalDeviceAssignmentDO::getTenantId, tenant)
                .in(RentalDeviceAssignmentDO::getStatus, List.of("ASSIGNED", "DISPATCHED", "DISPATCHED_PENDING_PLAN", "RETURNED")));
        var counts = assignmentRows.stream().filter(a -> tenant.equals(a.getTenantId()))
                .collect(Collectors.groupingBy(RentalDeviceAssignmentDO::getRentalOrderItemId, Collectors.counting()));
        var manual = deliveries.selectList(new LambdaQueryWrapper<RentalOrderDeliveryDO>()
                .in(RentalOrderDeliveryDO::getRentalOrderId, ids).eq(RentalOrderDeliveryDO::getTenantId, tenant)).stream()
                .filter(d -> tenant.equals(d.getTenantId()))
                .collect(Collectors.toMap(RentalOrderDeliveryDO::getRentalOrderId, d -> d, (a, b) -> a));
        for (var row : rows) {
            var delivery = manual.get(row.getId());
            if (row.getChannelOrderId() == null && delivery != null) {
                row.setReceiverName(delivery.getReceiverName());
                row.setReceiverMobile(delivery.getReceiverMobile());
                row.setReceiverAddress(delivery.getReceiverAddress());
            }
            row.setItems(itemRows.getOrDefault(row.getId(), List.of()).stream().map(item -> {
                var result = new RentalPendingAllocationItemRespVO();
                result.setId(item.getId()); result.setRentalOrderId(item.getRentalOrderId());
                result.setEquipmentModelCode(item.getEquipmentModelCode());
                result.setRequiredQuantity(item.getQuantity() == null ? 0 : item.getQuantity());
                result.setAssignedQuantity(counts.getOrDefault(item.getId(), 0L).intValue());
                result.setRemainingQuantity(Math.max(0, result.getRequiredQuantity() - result.getAssignedQuantity()));
                result.setOccupyStartDate(item.getOccupyStartDate()); result.setOccupyEndDateExclusive(item.getOccupyEndDateExclusive());
                return result;
            }).toList());
            row.setRequiredQuantity(row.getItems().stream().mapToInt(RentalPendingAllocationItemRespVO::getRequiredQuantity).sum());
            row.setAssignedQuantity(row.getItems().stream().mapToInt(RentalPendingAllocationItemRespVO::getAssignedQuantity).sum());
            row.setRemainingQuantity(row.getItems().stream().mapToInt(RentalPendingAllocationItemRespVO::getRemainingQuantity).sum());
        }
    }
}
