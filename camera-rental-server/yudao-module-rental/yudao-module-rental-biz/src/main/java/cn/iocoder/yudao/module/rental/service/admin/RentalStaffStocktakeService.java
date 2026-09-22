package cn.iocoder.yudao.module.rental.service.admin;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffWarehouseVO.*;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.*;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.STAFF_WORKFLOW_INVALID;

@Service
@RequiredArgsConstructor
public class RentalStaffStocktakeService {
    private final RentalStaffStocktakeMapper stocktakes;
    private final RentalStaffStocktakeLineMapper lines;
    private final RentalDeviceMapper devices;

    public PageResult<Stocktake> page(PageParam page) {
        var result = stocktakes.selectPage(page, new LambdaQueryWrapper<RentalStaffStocktakeDO>().orderByDesc(RentalStaffStocktakeDO::getId));
        return new PageResult<>(BeanUtils.toBean(result.getList(), Stocktake.class), result.getTotal());
    }
    public Stocktake get(Long id) {
        var row = stocktakes.selectById(id);
        if (row == null) throw exception(STAFF_WORKFLOW_INVALID, "盘点单不存在");
        var result = BeanUtils.toBean(row, Stocktake.class);
        result.setLines(BeanUtils.toBean(lines.selectList(new LambdaQueryWrapper<RentalStaffStocktakeLineDO>()
                .eq(RentalStaffStocktakeLineDO::getStocktakeId, id).orderByAsc(RentalStaffStocktakeLineDO::getId)), StocktakeLine.class));
        return result;
    }
    @Transactional(rollbackFor=Exception.class)
    public Long create(StocktakeCreate request) {
        var old = stocktakes.selectOne(new LambdaQueryWrapper<RentalStaffStocktakeDO>()
                .eq(RentalStaffStocktakeDO::getIdempotencyKey, request.getIdempotencyKey()));
        if (old != null) {
            if (!old.getWarehouseCode().equals(request.getWarehouseCode().trim())) throw exception(STAFF_WORKFLOW_INVALID, "请求编号已用于其他仓位");
            return old.getId();
        }
        var expected = devices.selectList(new LambdaQueryWrapper<RentalDeviceDO>()
                .eq(RentalDeviceDO::getWarehouseCode, request.getWarehouseCode().trim())
                .and(q -> q.in(RentalDeviceDO::getStatus, "AVAILABLE", "MAINTENANCE")
                    .or().apply("EXISTS (SELECT 1 FROM rental_device_assignment a WHERE a.device_id=rental_device.id AND a.tenant_id={0} AND a.deleted=0 AND a.status IN ('DISPATCHED','DISPATCHED_PENDING_PLAN') AND a.returned_at IS NOT NULL)",
                            cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId()))
                .orderByAsc(RentalDeviceDO::getId).last("LIMIT 501"));
        if (expected.size() > 500) throw exception(STAFF_WORKFLOW_INVALID, "单次最多盘点 500 台，请按仓位拆分");
        var row = new RentalStaffStocktakeDO();
        row.setWarehouseCode(request.getWarehouseCode().trim()); row.setStatus("OPEN"); row.setIdempotencyKey(request.getIdempotencyKey());
        stocktakes.insert(row);
        for (var device : expected) lines.insert(line(row.getId(), device, true, false));
        return row.getId();
    }
    @Transactional(rollbackFor=Exception.class)
    public void scan(StocktakeAction request) {
        var row = lock(request.getId());
        if (!"OPEN".equals(row.getStatus())) throw exception(STAFF_WORKFLOW_INVALID, "盘点已结束");
        var device = request.getDeviceId() == null ? null : devices.selectByIdForUpdate(request.getDeviceId());
        if (device == null) throw exception(STAFF_WORKFLOW_INVALID, "设备不存在");
        var item = lines.selectOne(new LambdaQueryWrapper<RentalStaffStocktakeLineDO>()
                .eq(RentalStaffStocktakeLineDO::getStocktakeId, row.getId()).eq(RentalStaffStocktakeLineDO::getDeviceId, device.getId()));
        if (item == null) {
            if (lines.selectCount(new LambdaQueryWrapper<RentalStaffStocktakeLineDO>().eq(RentalStaffStocktakeLineDO::getStocktakeId, row.getId())) >= 1000)
                throw exception(STAFF_WORKFLOW_INVALID, "盘点清单已达上限");
            lines.insert(line(row.getId(), device, false, true));
        } else if (!Boolean.TRUE.equals(item.getScanned())) { item.setScanned(true); lines.updateById(item); }
    }
    @Transactional(rollbackFor=Exception.class)
    public void close(Long id) { var row = lock(id); row.setStatus("CLOSED"); stocktakes.updateById(row); }

    @Transactional(rollbackFor=Exception.class)
    public void move(StocktakeAction request) {
        var row = lock(request.getId());
        if (!"CLOSED".equals(row.getStatus())) throw exception(STAFF_WORKFLOW_INVALID, "先结束盘点并核对差异，再确认仓位");
        var item = lines.selectOne(new LambdaQueryWrapper<RentalStaffStocktakeLineDO>()
                .eq(RentalStaffStocktakeLineDO::getStocktakeId, row.getId()).eq(RentalStaffStocktakeLineDO::getDeviceId, request.getDeviceId()));
        if (item == null || !Boolean.TRUE.equals(item.getScanned())) throw exception(STAFF_WORKFLOW_INVALID, "只能调整实盘设备仓位");
        if (Boolean.TRUE.equals(item.getAdjusted())) return;
        var device = devices.selectByIdForUpdate(item.getDeviceId());
        if (device == null || !Objects.equals(device.getWarehouseCode(), item.getOriginalWarehouse())
                || !java.util.Set.of("AVAILABLE", "MAINTENANCE").contains(device.getStatus()))
            throw exception(STAFF_WORKFLOW_INVALID, "设备仓位或状态已变化，请重新盘点");
        device.setWarehouseCode(row.getWarehouseCode()); devices.updateById(device);
        item.setAdjusted(true); lines.updateById(item);
    }
    private RentalStaffStocktakeDO lock(Long id) {
        var row = stocktakes.selectOneForUpdate(new LambdaQueryWrapper<RentalStaffStocktakeDO>().eq(RentalStaffStocktakeDO::getId, id));
        if (row == null) throw exception(STAFF_WORKFLOW_INVALID, "盘点单不存在");
        return row;
    }
    private RentalStaffStocktakeLineDO line(Long id, RentalDeviceDO device, boolean expected, boolean scanned) {
        var item = new RentalStaffStocktakeLineDO(); item.setStocktakeId(id); item.setDeviceId(device.getId());
        item.setDeviceNo(device.getDeviceNo()); item.setOriginalWarehouse(device.getWarehouseCode());
        item.setExpected(expected); item.setScanned(scanned); item.setAdjusted(false); return item;
    }
}
