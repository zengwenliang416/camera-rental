package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.*;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.*;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import cn.iocoder.yudao.module.rental.service.device.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.*;

@Service
public class RentalDeviceImportService {
    private final RentalDeviceMapper devices;
    private final RentalDeviceImportBatchMapper batches;
    private final RentalDeviceCatalogService catalog;
    private final RentalDeviceAdminService admin;

    public RentalDeviceImportService(RentalDeviceMapper devices, RentalDeviceImportBatchMapper batches,
                                     RentalDeviceCatalogService catalog, RentalDeviceAdminService admin) {
        this.devices = devices; this.batches = batches; this.catalog = catalog; this.admin = admin;
    }

    public RentalDeviceImportRespVO preview(RentalDeviceImportReqVO request, Long userId) {
        RentalDeviceImportRespVO response = inspect(request);
        RentalDeviceImportBatchDO batch = new RentalDeviceImportBatchDO();
        batch.setId(UUID.randomUUID().toString());
        batch.setUserId(userId);
        batch.setTenantId(TenantContextHolder.getRequiredTenantId());
        batch.setRequestJson(JsonUtils.toJsonString(request));
        batch.setPreviewJson(JsonUtils.toJsonString(response));
        batch.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24));
        batches.insert(batch);
        response.setBatchId(batch.getId());
        return response;
    }

    /** The batch row lock serializes retries. Device unique keys arbitrate competing batches. */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> commit(String batchId, Long userId) {
        RentalDeviceImportBatchDO batch = batches.lockOwned(batchId, userId);
        if (batch == null) throw exception(RENTAL_DEVICE_IMPORT_INVALID);
        if (batch.getResultJson() != null) return read(batch.getResultJson(), Result.class).deviceIds();
        if (batch.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC)))
            throw exception(RENTAL_DEVICE_IMPORT_EXPIRED);
        RentalDeviceImportReqVO request = read(batch.getRequestJson(), RentalDeviceImportReqVO.class);
        if (!"CREATE".equals(request.getMode())) throw exception(RENTAL_DEVICE_IMPORT_INVALID);
        RentalDeviceImportRespVO current = inspect(request);
        if (!current.isCanSubmit() || !JsonUtils.toJsonString(current).equals(batch.getPreviewJson()))
            throw exception(RENTAL_DEVICE_IMPORT_CHANGED);
        List<Long> ids = new ArrayList<>();
        // Stable model order avoids opposite lock order for concurrent multi-model imports.
        List<RentalDeviceImportRespVO.Row> rows = new ArrayList<>(current.getRows());
        rows.sort(Comparator.comparing(RentalDeviceImportRespVO.Row::getEquipmentModelCode)
                .thenComparing(row -> row.getDeviceNo().isEmpty() ? "~" : row.getDeviceNo()));
        try {
            for (var row : rows) {
                if ("MATCHED".equals(row.getStatus())) { ids.add(row.getDeviceId()); continue; }
                if (!"NEW".equals(row.getStatus())) continue;
                String number = row.getDeviceNo();
                if (number.isEmpty()) {
                    // Existing manual entry may be ahead of next_sequence: skip reserved identifiers.
                    do {
                        number = catalog.reserveDeviceNumbers(row.getCategoryCode(), row.getEquipmentModelCode(), 1)
                                .deviceNos().get(0);
                    } while (!devices.selectImportCandidates(TenantContextHolder.getRequiredTenantId(),
                            List.of(number), List.of()).isEmpty());
                }
                RentalDeviceCreateReqVO create = new RentalDeviceCreateReqVO();
                create.setCategoryCode(row.getCategoryCode());
                create.setEquipmentModelCode(row.getEquipmentModelCode());
                create.setDeviceNoSuffix(number.substring(number.lastIndexOf('-') + 1));
                create.setSerialNumber(row.getSerialNumber().isEmpty() ? null : row.getSerialNumber());
                ids.add(admin.createDevice(create));
            }
        } catch (DuplicateKeyException e) {
            throw exception(RENTAL_DEVICE_IMPORT_CHANGED);
        }
        batch.setResultJson(JsonUtils.toJsonString(new Result(ids)));
        batches.updateById(batch);
        return ids;
    }

    RentalDeviceImportRespVO inspect(RentalDeviceImportReqVO request) {
        Map<String, RentalDeviceCatalogService.CatalogModel> models = new HashMap<>();
        Set<String> numbers = new HashSet<>(), serials = new HashSet<>();
        for (var row : request.getRows()) {
            if (!clean(row.getDeviceNo()).isEmpty()) numbers.add(RentalDeviceCode.normalize(row.getDeviceNo()));
            if (!clean(row.getSerialNumber()).isEmpty()) serials.add(clean(row.getSerialNumber()));
            models.computeIfAbsent(row.getEquipmentModelCode(), key -> catalog.findEnabledModel(key).orElse(null));
        }
        List<RentalDeviceDO> existing = devices.selectImportCandidates(
                TenantContextHolder.getRequiredTenantId(), numbers, serials);
        Map<String, RentalDeviceDO> byNumber = index(existing, RentalDeviceDO::getDeviceNo);
        Map<String, RentalDeviceDO> bySerial = index(existing, RentalDeviceDO::getSerialNumber);
        Map<String, String> seenNo = new HashMap<>(), seenSerial = new HashMap<>();
        Set<Long> seenIds = new HashSet<>();
        List<RentalDeviceImportRespVO.Row> results = new ArrayList<>();
        for (var input : request.getRows()) {
            var row = new RentalDeviceImportRespVO.Row();
            row.setFileName(input.getFileName()); row.setLineNumber(input.getLineNumber());
            row.setDeviceNo(RentalDeviceCode.normalize(input.getDeviceNo()));
            row.setSerialNumber(clean(input.getSerialNumber()));
            row.setCategoryCode(RentalDeviceCatalogService.normalizeCategoryCode(input.getCategoryCode()));
            row.setEquipmentModelCode(RentalDeviceCode.normalize(input.getEquipmentModelCode()));
            row.setStatus("CONFLICT"); row.setReason("IDENTITY_CONFLICT");
            results.add(row);
            var model = models.get(input.getEquipmentModelCode());
            if (model == null || !model.categoryCode().equals(row.getCategoryCode())) {
                row.setReason("MODEL_INVALID"); continue;
            }
            if (row.getDeviceNo().isEmpty() && row.getSerialNumber().isEmpty()) {
                row.setReason("IDENTIFIER_REQUIRED"); continue;
            }
            if (!row.getDeviceNo().isEmpty() && (!RentalDeviceCode.isValid(row.getDeviceNo())
                    || !row.getDeviceNo().substring(0, row.getDeviceNo().lastIndexOf('-')).equals(model.deviceNoPrefix()))) {
                row.setReason("NUMBER_INVALID"); continue;
            }
            if ((!row.getSerialNumber().isEmpty() && row.getSerialNumber().codePoints().filter(Character::isLetterOrDigit).count() < 4)
                    || row.getSerialNumber().chars().anyMatch(c -> Character.isISOControl(c) || c == '|')) {
                row.setReason("SERIAL_INVALID"); continue;
            }
            String identity = key(row.getDeviceNo()) + "|" + key(row.getSerialNumber()) + "|" + row.getEquipmentModelCode();
            var sameNo = row.getDeviceNo().isEmpty() ? null : seenNo.putIfAbsent(key(row.getDeviceNo()), identity);
            var sameSerial = row.getSerialNumber().isEmpty() ? null : seenSerial.putIfAbsent(key(row.getSerialNumber()), identity);
            var prior = sameNo != null ? sameNo : sameSerial;
            if (prior != null) {
                if (prior.equals(identity)) {
                    row.setStatus("DUPLICATE"); row.setReason("DUPLICATE_ROW");
                }
                continue;
            }
            var numberMatch = byNumber.get(key(row.getDeviceNo()));
            var serialMatch = bySerial.get(key(row.getSerialNumber()));
            if (numberMatch != null && serialMatch != null && !numberMatch.getId().equals(serialMatch.getId())) continue;
            var found = numberMatch != null ? numberMatch : serialMatch;
            if (found != null) {
                if (Boolean.TRUE.equals(found.getDeleted()) || !Boolean.TRUE.equals(found.getEnabled())) {
                    row.setReason("DEVICE_DISABLED"); continue;
                }
                if (!row.getEquipmentModelCode().equals(found.getEquipmentModelCode())
                        || (!row.getDeviceNo().isEmpty() && !key(row.getDeviceNo()).equals(key(found.getDeviceNo())))
                        || (!row.getSerialNumber().isEmpty() && !key(row.getSerialNumber()).equals(key(found.getSerialNumber())))) continue;
                row.setDeviceId(found.getId()); row.setDeviceNo(found.getDeviceNo());
                row.setSerialNumber(clean(found.getSerialNumber()));
                row.setStatus(seenIds.add(found.getId()) ? "MATCHED" : "DUPLICATE");
                row.setReason("DUPLICATE".equals(row.getStatus()) ? "DUPLICATE_ROW" : "MATCHED");
            } else {
                row.setStatus("CREATE".equals(request.getMode()) ? "NEW" : "MISSING");
                row.setReason(row.getStatus());
            }
        }
        var result = new RentalDeviceImportRespVO(); result.setRows(results);
        result.setCanSubmit(results.stream().noneMatch(r -> Set.of("CONFLICT", "MISSING").contains(r.getStatus()))
                && results.stream().anyMatch(r -> Set.of("NEW", "MATCHED").contains(r.getStatus())));
        return result;
    }
    private static Map<String, RentalDeviceDO> index(List<RentalDeviceDO> values, Function<RentalDeviceDO, String> field) {
        return values.stream().filter(d -> !clean(field.apply(d)).isEmpty())
                .collect(Collectors.toMap(d -> key(field.apply(d)), Function.identity(), (a, b) -> a));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String key(String value) { return clean(value).toUpperCase(Locale.ROOT); }
    // Avoid JsonUtils.parseObject error logging the stored input/serial numbers.
    private static <T> T read(String json, Class<T> type) {
        try { return JsonUtils.getObjectMapper().readValue(json, type); }
        catch (Exception e) { throw exception(RENTAL_DEVICE_IMPORT_INVALID); }
    }
    public record Result(List<Long> deviceIds) { }
}
