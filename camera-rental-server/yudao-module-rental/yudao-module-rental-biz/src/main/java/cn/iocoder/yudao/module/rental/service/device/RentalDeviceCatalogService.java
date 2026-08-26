package cn.iocoder.yudao.module.rental.service.device;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCategoryCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCategoryRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceModelCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceModelRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceCategoryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceCategoryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CATALOG_CODE_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CATEGORY_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CODE_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_MODEL_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_MODEL_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_PREFIX_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_SEQUENCE_EXHAUSTED;

@Service
public class RentalDeviceCatalogService {

    private static final Pattern CATEGORY_CODE = Pattern.compile("^[A-Z0-9]+(?:[_-][A-Z0-9]+)*$");
    private static final int DEFAULT_SORT_ORDER = 100;

    private final RentalDeviceCategoryMapper categoryMapper;
    private final RentalDeviceModelMapper modelMapper;

    public RentalDeviceCatalogService(RentalDeviceCategoryMapper categoryMapper,
                                      RentalDeviceModelMapper modelMapper) {
        this.categoryMapper = categoryMapper;
        this.modelMapper = modelMapper;
    }

    public List<RentalDeviceCategoryRespVO> getCatalog() {
        List<RentalDeviceCategoryDO> categories = categoryMapper.selectEnabledList();
        Map<Long, List<RentalDeviceModelRespVO>> modelsByCategory = new LinkedHashMap<>();
        for (RentalDeviceModelDO model : modelMapper.selectEnabledList()) {
            modelsByCategory.computeIfAbsent(model.getCategoryId(), key -> new ArrayList<>())
                    .add(toModelResp(model));
        }
        return categories.stream()
                .map(category -> new RentalDeviceCategoryRespVO(
                        category.getId(),
                        category.getCategoryCode(),
                        category.getCategoryName(),
                        modelsByCategory.getOrDefault(category.getId(), List.of())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(RentalDeviceCategoryCreateReqVO reqVO) {
        String categoryCode = normalizeCategoryCode(reqVO.getCategoryCode());
        if (!CATEGORY_CODE.matcher(categoryCode).matches()) {
            throw exception(RENTAL_DEVICE_CATALOG_CODE_INVALID, "大类编码仅支持大写字母、数字、下划线和连字符");
        }
        if (categoryMapper.selectByCode(categoryCode) != null) {
            throw exception(RENTAL_DEVICE_CATEGORY_DUPLICATE);
        }
        RentalDeviceCategoryDO category = RentalDeviceCategoryDO.builder()
                .categoryCode(categoryCode)
                .categoryName(normalizeName(reqVO.getCategoryName(), "大类名称"))
                .sortOrder(defaultSort(reqVO.getSortOrder()))
                .enabled(true)
                .build();
        category.setTenantId(TenantContextHolder.getRequiredTenantId());
        try {
            categoryMapper.insert(category);
        } catch (DuplicateKeyException ex) {
            throw exception(RENTAL_DEVICE_CATEGORY_DUPLICATE);
        }
        return category.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createModel(RentalDeviceModelCreateReqVO reqVO) {
        RentalDeviceCategoryDO category = requireEnabledCategory(reqVO.getCategoryId());
        String modelCode = normalizeCatalogToken(reqVO.getModelCode(), "型号编码");
        String deviceNoPrefix = normalizeCatalogToken(reqVO.getDeviceNoPrefix(), "编号前缀");
        if (modelMapper.selectByCode(modelCode) != null) {
            throw exception(RENTAL_DEVICE_MODEL_DUPLICATE);
        }
        if (modelMapper.selectByPrefix(deviceNoPrefix) != null) {
            throw exception(RENTAL_DEVICE_PREFIX_DUPLICATE);
        }
        RentalDeviceModelDO model = RentalDeviceModelDO.builder()
                .categoryId(category.getId())
                .modelCode(modelCode)
                .modelName(normalizeName(reqVO.getModelName(), "型号名称"))
                .deviceNoPrefix(deviceNoPrefix)
                .nextSequence(1)
                .sortOrder(defaultSort(reqVO.getSortOrder()))
                .enabled(true)
                .build();
        model.setTenantId(TenantContextHolder.getRequiredTenantId());
        try {
            modelMapper.insert(model);
        } catch (DuplicateKeyException ex) {
            if (modelMapper.selectByPrefix(deviceNoPrefix) != null) {
                throw exception(RENTAL_DEVICE_PREFIX_DUPLICATE);
            }
            throw exception(RENTAL_DEVICE_MODEL_DUPLICATE);
        }
        return model.getId();
    }

    public Optional<CatalogModel> findEnabledModel(String modelCode) {
        String normalizedModelCode = RentalDeviceCode.normalizePrefix(modelCode);
        if (!StringUtils.hasText(normalizedModelCode)
                || !normalizedModelCode.equals(RentalDeviceCode.normalize(modelCode))) {
            return Optional.empty();
        }
        RentalDeviceModelDO model = modelMapper.selectByCode(normalizedModelCode);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            return Optional.empty();
        }
        RentalDeviceCategoryDO category = categoryMapper.selectById(model.getCategoryId());
        if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
            return Optional.empty();
        }
        return Optional.of(toCatalogModel(category, model));
    }

    public DeviceNumberSelection composeDeviceNumber(String categoryCode, String modelCode,
                                                     String deviceNoSuffix) {
        String normalizedCategoryCode = normalizeCategoryCode(categoryCode);
        String normalizedModelCode = normalizeCatalogToken(modelCode, "型号编码");
        RentalDeviceCategoryDO category = categoryMapper.selectByCode(normalizedCategoryCode);
        if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
            throw exception(RENTAL_DEVICE_CATEGORY_NOT_EXISTS);
        }
        RentalDeviceModelDO model =
                modelMapper.selectByCategoryAndCode(category.getId(), normalizedModelCode);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw exception(RENTAL_DEVICE_MODEL_NOT_EXISTS);
        }
        int sequence = parseDeviceNoSuffix(deviceNoSuffix);
        return new DeviceNumberSelection(
                toCatalogModel(category, model),
                RentalDeviceCode.format(model.getDeviceNoPrefix(), sequence));
    }

    @Transactional(rollbackFor = Exception.class)
    public DeviceNumberReservation reserveDeviceNumbers(String categoryCode, String modelCode, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("设备编号预留数量必须大于 0");
        }
        String normalizedCategoryCode = normalizeCategoryCode(categoryCode);
        String normalizedModelCode = normalizeCatalogToken(modelCode, "型号编码");
        RentalDeviceCategoryDO category = categoryMapper.selectByCode(normalizedCategoryCode);
        if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
            throw exception(RENTAL_DEVICE_CATEGORY_NOT_EXISTS);
        }
        RentalDeviceModelDO model =
                modelMapper.selectByCategoryAndCodeForUpdate(category.getId(), normalizedModelCode);
        if (model == null || !Boolean.TRUE.equals(model.getEnabled())) {
            throw exception(RENTAL_DEVICE_MODEL_NOT_EXISTS);
        }
        int firstSequence = model.getNextSequence() == null ? 1 : model.getNextSequence();
        long lastSequenceValue = (long) firstSequence + count - 1;
        if (firstSequence < 1 || lastSequenceValue > 999) {
            throw exception(RENTAL_DEVICE_SEQUENCE_EXHAUSTED, model.getModelCode());
        }
        int lastSequence = (int) lastSequenceValue;
        List<String> deviceNos = new ArrayList<>(count);
        for (int sequence = firstSequence; sequence <= lastSequence; sequence++) {
            deviceNos.add(RentalDeviceCode.format(model.getDeviceNoPrefix(), sequence));
        }
        model.setNextSequence(lastSequence + 1);
        modelMapper.updateById(model);
        return new DeviceNumberReservation(toCatalogModel(category, model), List.copyOf(deviceNos));
    }

    private RentalDeviceCategoryDO requireEnabledCategory(Long categoryId) {
        RentalDeviceCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null || !Boolean.TRUE.equals(category.getEnabled())) {
            throw exception(RENTAL_DEVICE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private static RentalDeviceModelRespVO toModelResp(RentalDeviceModelDO model) {
        return new RentalDeviceModelRespVO(
                model.getId(), model.getModelCode(), model.getModelName(), model.getDeviceNoPrefix());
    }

    private static CatalogModel toCatalogModel(RentalDeviceCategoryDO category, RentalDeviceModelDO model) {
        return new CatalogModel(
                category.getId(),
                category.getCategoryCode(),
                category.getCategoryName(),
                model.getId(),
                model.getModelCode(),
                model.getModelName(),
                model.getDeviceNoPrefix());
    }

    public static String normalizeCategoryCode(String categoryCode) {
        return categoryCode == null ? "" : categoryCode.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizeCatalogToken(String value, String fieldName) {
        String normalized = RentalDeviceCode.normalizePrefix(value);
        String inputNormalized = RentalDeviceCode.normalize(value);
        if (!StringUtils.hasText(normalized) || !normalized.equals(inputNormalized)) {
            throw exception(RENTAL_DEVICE_CATALOG_CODE_INVALID, fieldName);
        }
        return normalized;
    }

    private static String normalizeName(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw exception(RENTAL_DEVICE_CATALOG_CODE_INVALID, fieldName);
        }
        return value.trim();
    }

    private static int defaultSort(Integer sortOrder) {
        return sortOrder == null ? DEFAULT_SORT_ORDER : sortOrder;
    }

    private static int parseDeviceNoSuffix(String deviceNoSuffix) {
        String normalized = deviceNoSuffix == null ? "" : deviceNoSuffix.trim();
        if (!normalized.matches("^(?:0?[1-9]|[1-9][0-9]{1,2})$")) {
            throw exception(RENTAL_DEVICE_CODE_INVALID);
        }
        return Integer.parseInt(normalized);
    }

    public record CatalogModel(Long categoryId,
                               String categoryCode,
                               String categoryName,
                               Long modelId,
                               String modelCode,
                               String modelName,
                               String deviceNoPrefix) {
    }

    public record DeviceNumberReservation(CatalogModel model, List<String> deviceNos) {
    }

    public record DeviceNumberSelection(CatalogModel model, String deviceNo) {
    }

}
