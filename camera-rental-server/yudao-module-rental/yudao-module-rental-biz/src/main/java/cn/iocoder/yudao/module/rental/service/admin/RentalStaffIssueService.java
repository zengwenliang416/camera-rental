package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
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
public class RentalStaffIssueService {
    private final RentalStaffIssueMapper issues;
    private final RentalDeviceMapper devices;
    private final RentalOrderMapper orders;

    public PageResult<Issue> page(PageParam page, String status) {
        var result = issues.selectPage(page, new LambdaQueryWrapper<RentalStaffIssueDO>()
                .eq(status != null && !status.isBlank(), RentalStaffIssueDO::getStatus, status)
                .orderByDesc(RentalStaffIssueDO::getId));
        return new PageResult<>(BeanUtils.toBean(result.getList(), Issue.class), result.getTotal());
    }
    public Long create(IssueSave request) {
        if (request.getRequestKey() != null) {
            var existing = issues.selectOne(new LambdaQueryWrapper<RentalStaffIssueDO>().eq(RentalStaffIssueDO::getRequestKey, request.getRequestKey()));
            if (existing != null) {
                if (!Objects.equals(existing.getRentalOrderId(), request.getRentalOrderId()) || !Objects.equals(existing.getDeviceId(), request.getDeviceId()))
                    throw exception(STAFF_WORKFLOW_INVALID, "异常请求编号已被使用");
                return existing.getId();
            }
        }
        if (request.getDeviceId() != null && devices.selectById(request.getDeviceId()) == null
                || request.getRentalOrderId() != null && orders.selectById(request.getRentalOrderId()) == null) {
            throw exception(STAFF_WORKFLOW_INVALID, "关联设备或订单不存在");
        }
        RentalStaffIssueDO row = BeanUtils.toBean(request, RentalStaffIssueDO.class);
        row.setStatus("OPEN"); row.setRevision(0); row.setNote(request.getNote() == null ? "" : request.getNote());
        issues.insert(row);
        return row.getId();
    }
    @Transactional(rollbackFor=Exception.class)
    public void action(IssueAction request) {
        RentalStaffIssueDO row = issues.selectOneForUpdate(new LambdaQueryWrapper<RentalStaffIssueDO>()
                .eq(RentalStaffIssueDO::getId, request.getId()));
        if (row == null || !Objects.equals(row.getRevision(), request.getRevision())) {
            throw exception(STAFF_WORKFLOW_INVALID, "记录已变化，请刷新后处理");
        }
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        if (actor == null) throw exception(STAFF_WORKFLOW_INVALID, "请重新登录");
        if (row.getOwnerId() != null && !actor.equals(row.getOwnerId())) {
            throw exception(STAFF_WORKFLOW_INVALID, "该异常由其他员工处理中，请由负责人更新");
        }
        switch (request.getAction()) {
            case "CLAIM" -> {
                if (!"OPEN".equals(row.getStatus())) throw exception(STAFF_WORKFLOW_INVALID, "仅待处理异常可以接单");
                row.setOwnerId(actor); row.setStatus("PROCESSING");
            }
            case "NOTE", "RESOLVE" -> {
                if (!"PROCESSING".equals(row.getStatus()) || request.getNote() == null || request.getNote().isBlank())
                    throw exception(STAFF_WORKFLOW_INVALID, "请先接单，并填写处理结果");
                String note = row.getNote() + "\n处理人 " + actor + "：" + request.getNote().trim();
                if (note.length() > 1000) throw exception(STAFF_WORKFLOW_INVALID, "处理说明过长，请缩短本次说明");
                row.setNote(note);
                if ("RESOLVE".equals(request.getAction())) row.setStatus("RESOLVED");
            }
            case "REOPEN" -> {
                if (!"RESOLVED".equals(row.getStatus())) throw exception(STAFF_WORKFLOW_INVALID, "仅已处理异常可重新打开");
                row.setStatus("PROCESSING");
            }
            default -> throw exception(STAFF_WORKFLOW_INVALID, "未知操作");
        }
        row.setRevision(row.getRevision() + 1);
        issues.updateById(row);
    }
}
