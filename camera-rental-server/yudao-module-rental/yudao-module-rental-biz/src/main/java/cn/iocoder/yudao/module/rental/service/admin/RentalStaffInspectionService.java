package cn.iocoder.yudao.module.rental.service.admin;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffWarehouseVO.*;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceReturnReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceOpsRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.*;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.STAFF_WORKFLOW_INVALID;

@Service
@RequiredArgsConstructor
public class RentalStaffInspectionService {
    private final RentalStaffPhotoMapper photos;
    private final RentalStaffInspectionMapper inspections;
    private final RentalDeviceMapper devices;
    private final RentalDeviceAssignmentMapper assignments;
    private final RentalDeviceOpsService operations;
    private final FileApi files;
    private final ObjectMapper json;
    private final RentalStaffInspectionTemplateMapper templates;
    private final RentalDeviceModelMapper models;

    public List<Check> template(String modelCode) {
        var template = templates.selectOne(new LambdaQueryWrapper<RentalStaffInspectionTemplateDO>()
                .eq(RentalStaffInspectionTemplateDO::getModelCode, modelCode));
        if (template != null) {
            try { return json.readValue(template.getChecklistJson(), new TypeReference<List<Check>>() {}); }
            catch (Exception ex) { throw exception(STAFF_WORKFLOW_INVALID, "型号检测模板无效，请联系管理员"); }
        }
        return List.of(check("机身外观", null), check("开机与功能", null), check("镜头卡口", null),
                check("电池与充电器", null), check("其他配件", null));
    }
    private Check check(String label, Integer expected) { var check = new Check(); check.setLabel(label); check.setExpected(expected); check.setResult("PASS"); return check; }
    @Transactional(rollbackFor=Exception.class)
    public void saveTemplate(TemplateSave request) {
        var model = models.selectOneForUpdate(new LambdaQueryWrapper<RentalDeviceModelDO>().eq(RentalDeviceModelDO::getModelCode, request.getModelCode()));
        if (model == null) throw exception(STAFF_WORKFLOW_INVALID, "型号不存在");
        validateChecks(request.getChecks());
        var row = templates.selectOneForUpdate(new LambdaQueryWrapper<RentalStaffInspectionTemplateDO>().eq(RentalStaffInspectionTemplateDO::getModelCode, request.getModelCode()));
        if (row == null) { row = new RentalStaffInspectionTemplateDO(); row.setModelCode(request.getModelCode()); row.setChecklistJson(write(request.getChecks())); templates.insert(row); }
        else { row.setChecklistJson(write(request.getChecks())); templates.updateById(row); }
    }
    @Transactional(rollbackFor=Exception.class)
    public PhotoUpload authorize(PhotoAuthorize request) {
        lockDevice(request.getDeviceId(), request.getAssignmentId());
        if (photos.selectCount(new LambdaQueryWrapper<RentalStaffPhotoDO>().eq(RentalStaffPhotoDO::getAssignmentId, request.getAssignmentId())) >= 12)
            throw exception(STAFF_WORKFLOW_INVALID, "每个租赁轮次最多上传 12 张照片");
        String suffix = "image/png".equals(request.getContentType()) ? ".png" : ".jpg";
        String directory = "rental-staff/" + TenantContextHolder.getRequiredTenantId() + "/" + request.getAssignmentId();
        var upload = files.presignPutUrl(UUID.randomUUID() + suffix, directory, 300);
        var photo = new RentalStaffPhotoDO(); photo.setDeviceId(request.getDeviceId()); photo.setAssignmentId(request.getAssignmentId());
        photo.setFileConfigId(upload.configId()); photo.setObjectPath(upload.path()); photo.setConfirmed(false); photos.insert(photo);
        return new PhotoUpload(photo.getId(), upload.uploadUrl());
    }
    @Transactional(rollbackFor=Exception.class)
    public Photo confirm(Long id) {
        var row = photos.selectOneForUpdate(new LambdaQueryWrapper<RentalStaffPhotoDO>().eq(RentalStaffPhotoDO::getId, id));
        if (row == null) throw exception(STAFF_WORKFLOW_INVALID, "照片不存在");
        if (!Boolean.TRUE.equals(row.getConfirmed())) {
            String contentType = row.getObjectPath().endsWith(".png") ? "image/png" : "image/jpeg";
            var file = files.confirmPresignedUpload(row.getFileConfigId(), row.getObjectPath(), "inspection", contentType, 5 * 1024 * 1024);
            row.setFileId(file.fileId()); row.setConfirmed(true); photos.updateById(row);
        }
        return new Photo(row.getId(), files.presignGetUrlById(row.getFileId(), 300));
    }
    public Photo photo(Long id) {
        var row = photos.selectById(id);
        if (row == null || !Boolean.TRUE.equals(row.getConfirmed()) || row.getFileId() == null)
            throw exception(STAFF_WORKFLOW_INVALID, "照片不存在或未上传完成");
        return new Photo(row.getId(), files.presignGetUrlById(row.getFileId(), 300));
    }
    public List<Inspection> history(Long deviceId) {
        if (devices.selectById(deviceId) == null) throw exception(STAFF_WORKFLOW_INVALID, "设备不存在");
        return BeanUtils.toBean(inspections.selectList(new LambdaQueryWrapper<RentalStaffInspectionDO>()
                .eq(RentalStaffInspectionDO::getDeviceId, deviceId).orderByDesc(RentalStaffInspectionDO::getId).last("LIMIT 30")), Inspection.class);
    }
    @Transactional(rollbackFor=Exception.class)
    public RentalDeviceOpsRespVO submit(InspectionSubmit request) {
        var device = lockDevice(request.getDeviceId(), request.getAssignmentId());
        validateChecks(request.getChecks());
        var stored = inspections.selectOne(new LambdaQueryWrapper<RentalStaffInspectionDO>()
                .eq(RentalStaffInspectionDO::getIdempotencyKey, request.getIdempotencyKey()));
        String checks = write(request.getChecks());
        String photoIds = write(request.getPhotoIds() == null ? List.of() : request.getPhotoIds());
        if (stored != null && (!Objects.equals(stored.getDeviceId(), request.getDeviceId())
                || !Objects.equals(stored.getAssignmentId(), request.getAssignmentId())
                || !stored.getChecklistJson().equals(checks) || !stored.getPhotoIdsJson().equals(photoIds)
                || !Objects.equals(stored.getNote(), request.getNote()))) throw exception(STAFF_WORKFLOW_INVALID, "检测请求编号已被使用");
        if (stored != null) {
            var result = new RentalDeviceOpsRespVO(); result.setDeviceId(stored.getDeviceId()); result.setDeviceNo(device.getDeviceNo());
            result.setAssignmentId(stored.getAssignmentId()); result.setAssignmentStatus("RETURNED");
            result.setDeviceStatus(Boolean.TRUE.equals(stored.getPassed()) ? "AVAILABLE" : "MAINTENANCE");
            return result;
        }
        var template = template(device.getEquipmentModelCode());
        if (request.getChecks().size() != template.size()) throw exception(STAFF_WORKFLOW_INVALID, "检测模板已变化，请刷新后重试");
        for (int i = 0; i < template.size(); i++) {
            var expected = template.get(i); var actual = request.getChecks().get(i);
            if (!Objects.equals(expected.getLabel(), actual.getLabel()) || !Objects.equals(expected.getExpected(), actual.getExpected()))
                throw exception(STAFF_WORKFLOW_INVALID, "检测项目与型号模板不一致");
            if (expected.getExpected() != null && actual.getActual() == null) throw exception(STAFF_WORKFLOW_INVALID, "请填写配件实收数量");
        }
        for (Long id : request.getPhotoIds() == null ? List.<Long>of() : request.getPhotoIds()) {
            var photo = photos.selectById(id);
            if (photo == null || !Boolean.TRUE.equals(photo.getConfirmed()) || !photo.getDeviceId().equals(device.getId())
                    || !photo.getAssignmentId().equals(request.getAssignmentId())) throw exception(STAFF_WORKFLOW_INVALID, "照片未上传完成或不属于本设备轮次");
        }
        boolean passed = request.getChecks().stream().allMatch(c -> "PASS".equals(c.getResult())
                && (c.getExpected() == null || c.getExpected().equals(c.getActual())));
        var command = new RentalDeviceReturnReqVO(); command.setDeviceId(device.getId()); command.setAssignmentId(request.getAssignmentId());
        command.setInspectPassed(passed); command.setNote(request.getNote());
        var result = operations.inspectDevice(command);
        if (stored == null) {
            var record = new RentalStaffInspectionDO(); record.setDeviceId(device.getId()); record.setAssignmentId(request.getAssignmentId());
            record.setIdempotencyKey(request.getIdempotencyKey()); record.setChecklistJson(checks); record.setPhotoIdsJson(photoIds);
            record.setPassed(passed); record.setNote(request.getNote()); inspections.insert(record);
        }
        return result;
    }
    private RentalDeviceDO lockDevice(Long deviceId, Long assignmentId) {
        var device = devices.selectByIdForUpdate(deviceId);
        var assignment = assignments.selectById(assignmentId);
        if (device == null || assignment == null || !deviceId.equals(assignment.getDeviceId())) throw exception(STAFF_WORKFLOW_INVALID, "设备或分配记录不匹配");
        return device;
    }
    private void validateChecks(List<Check> checks) {
        if (checks == null || checks.isEmpty() || checks.size() > 30
                || checks.stream().map(Check::getLabel).distinct().count() != checks.size()) throw exception(STAFF_WORKFLOW_INVALID, "检测项目为空、重复或超限");
    }
    private String write(Object value) { try { return json.writeValueAsString(value); } catch (Exception ex) { throw exception(STAFF_WORKFLOW_INVALID, "检测记录格式无效"); } }
}
