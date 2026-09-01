package cn.iocoder.yudao.module.rental.service.device;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCategoryCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceCategoryRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceModelCreateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalConfigurationCatalogRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalDeviceCatalogStatusReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalDeviceCategoryUpdateReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.configuration.vo.RentalDeviceModelUpdateReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceCategoryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceModelDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceCategoryMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceModelMapper;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService.DeviceNumberReservation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CATALOG_CODE_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CATEGORY_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_CODE_INVALID;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_MODEL_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_PREFIX_DUPLICATE;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_SEQUENCE_EXHAUSTED;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_CONFIGURATION_VERSION_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RentalDeviceCatalogServiceTest {

    private RentalDeviceCategoryMapper categoryMapper;
    private RentalDeviceModelMapper modelMapper;
    private RentalDeviceCatalogService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        categoryMapper = mock(RentalDeviceCategoryMapper.class);
        modelMapper = mock(RentalDeviceModelMapper.class);
        service = new RentalDeviceCatalogService(categoryMapper, modelMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createsCategoryWithNormalizedCodeAndCurrentTenant() {
        when(categoryMapper.insert(any(RentalDeviceCategoryDO.class))).thenAnswer(invocation -> {
            RentalDeviceCategoryDO category = invocation.getArgument(0);
            category.setId(11L);
            return 1;
        });

        RentalDeviceCategoryCreateReqVO reqVO = new RentalDeviceCategoryCreateReqVO();
        reqVO.setCategoryCode(" action-camera ");
        reqVO.setCategoryName(" 运动相机 ");
        assertEquals(11L, service.createCategory(reqVO));

        ArgumentCaptor<RentalDeviceCategoryDO> captor =
                ArgumentCaptor.forClass(RentalDeviceCategoryDO.class);
        verify(categoryMapper).insert(captor.capture());
        assertEquals(9L, captor.getValue().getTenantId());
        assertEquals("ACTION-CAMERA", captor.getValue().getCategoryCode());
        assertEquals("运动相机", captor.getValue().getCategoryName());
        assertEquals(100, captor.getValue().getSortOrder());
    }

    @Test
    void rejectsDuplicateCategory() {
        when(categoryMapper.selectByCode("DJI")).thenReturn(category(1L, "DJI", "大疆"));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createCategory(categoryRequest("dji", "大疆")));
        assertEquals(RENTAL_DEVICE_CATEGORY_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void rejectsChineseModelTokensExceptStand() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "STAND", "支架"));

        ServiceException invalidModel = assertThrows(ServiceException.class,
                () -> service.createModel(modelRequest(1L, "相机", "相机", "CAMERA")));
        assertEquals(RENTAL_DEVICE_CATALOG_CODE_INVALID.getCode(), invalidModel.getCode());

        ServiceException invalidPrefix = assertThrows(ServiceException.class,
                () -> service.createModel(modelRequest(1L, "CAMERA", "相机", "相机")));
        assertEquals(RENTAL_DEVICE_CATALOG_CODE_INVALID.getCode(), invalidPrefix.getCode());
    }

    @Test
    void allowsExplicitStandToken() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "STAND", "支架"));
        when(modelMapper.insert(any(RentalDeviceModelDO.class))).thenAnswer(invocation -> {
            RentalDeviceModelDO model = invocation.getArgument(0);
            model.setId(12L);
            return 1;
        });

        assertEquals(12L, service.createModel(modelRequest(1L, "支架", "支架", "支架")));

        ArgumentCaptor<RentalDeviceModelDO> captor =
                ArgumentCaptor.forClass(RentalDeviceModelDO.class);
        verify(modelMapper).insert(captor.capture());
        assertEquals("支架", captor.getValue().getModelCode());
        assertEquals("支架", captor.getValue().getDeviceNoPrefix());
        assertEquals(9L, captor.getValue().getTenantId());
    }

    @Test
    void rejectsMissingOrOtherTenantCategory() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createModel(modelRequest(99L, "P4P", "P4P", "P4P")));
        assertEquals(RENTAL_DEVICE_CATEGORY_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void rejectsDuplicateModelAndPrefix() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "DJI", "大疆"));
        when(modelMapper.selectByCode("P4P")).thenReturn(model(2L, 1L, "P4P", "P4P", 1));
        ServiceException duplicateModel = assertThrows(ServiceException.class,
                () -> service.createModel(modelRequest(1L, "P4P", "P4P", "P4P")));
        assertEquals(RENTAL_DEVICE_MODEL_DUPLICATE.getCode(), duplicateModel.getCode());

        when(modelMapper.selectByCode("P4P")).thenReturn(null);
        when(modelMapper.selectByPrefix("P4P")).thenReturn(model(3L, 1L, "OTHER", "P4P", 1));
        ServiceException duplicatePrefix = assertThrows(ServiceException.class,
                () -> service.createModel(modelRequest(1L, "P4P", "P4P", "P4P")));
        assertEquals(RENTAL_DEVICE_PREFIX_DUPLICATE.getCode(), duplicatePrefix.getCode());
    }

    @Test
    void classifiesConcurrentPrefixCollision() {
        when(categoryMapper.selectById(1L)).thenReturn(category(1L, "DJI", "大疆"));
        when(modelMapper.selectByPrefix("P4P"))
                .thenReturn(null)
                .thenReturn(model(3L, 1L, "OTHER", "P4P", 1));
        when(modelMapper.insert(any(RentalDeviceModelDO.class)))
                .thenThrow(new DuplicateKeyException("prefix collision"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.createModel(modelRequest(1L, "P4P", "P4P", "P4P")));
        assertEquals(RENTAL_DEVICE_PREFIX_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void composesAdministratorSelectedNumberWithModelPrefix() {
        RentalDeviceCategoryDO category = category(1L, "DJI", "大疆");
        RentalDeviceModelDO model = model(2L, 1L, "P4P", "P4P", 1);
        when(categoryMapper.selectByCode("DJI")).thenReturn(category);
        when(modelMapper.selectByCategoryAndCode(1L, "P4P")).thenReturn(model);

        var selection = service.composeDeviceNumber(" dji ", " p4p ", "2");

        assertEquals("P4P-02", selection.deviceNo());
        assertEquals("DJI", selection.model().categoryCode());
        assertEquals("P4P", selection.model().modelCode());
    }

    @Test
    void rejectsInvalidAdministratorSelectedNumber() {
        RentalDeviceCategoryDO category = category(1L, "STAND", "支架");
        RentalDeviceModelDO model = model(2L, 1L, "支架", "支架", 1);
        when(categoryMapper.selectByCode("STAND")).thenReturn(category);
        when(modelMapper.selectByCategoryAndCode(1L, "支架")).thenReturn(model);

        ServiceException zero = assertThrows(ServiceException.class,
                () -> service.composeDeviceNumber("STAND", "支架", "0"));
        ServiceException tooLarge = assertThrows(ServiceException.class,
                () -> service.composeDeviceNumber("STAND", "支架", "1000"));

        assertEquals(RENTAL_DEVICE_CODE_INVALID.getCode(), zero.getCode());
        assertEquals(RENTAL_DEVICE_CODE_INVALID.getCode(), tooLarge.getCode());
    }

    @Test
    void composesThreeDigitAdministratorSelectedNumber() {
        RentalDeviceCategoryDO category = category(1L, "DJI", "大疆");
        RentalDeviceModelDO model = model(2L, 1L, "P4P", "P4P", 1);
        when(categoryMapper.selectByCode("DJI")).thenReturn(category);
        when(modelMapper.selectByCategoryAndCode(1L, "P4P")).thenReturn(model);

        var selection = service.composeDeviceNumber("DJI", "P4P", "999");

        assertEquals("P4P-999", selection.deviceNo());
    }

    @Test
    void reservesConsecutiveNumbersUnderModelRowLock() {
        RentalDeviceCategoryDO category = category(1L, "DJI", "大疆");
        RentalDeviceModelDO model = model(2L, 1L, "P4P", "P4P", 1);
        when(categoryMapper.selectByCode("DJI")).thenReturn(category);
        when(modelMapper.selectByCategoryAndCodeForUpdate(1L, "P4P")).thenReturn(model);

        DeviceNumberReservation reservation = service.reserveDeviceNumbers(" dji ", " p4p ", 3);

        assertEquals(List.of("P4P-01", "P4P-02", "P4P-03"), reservation.deviceNos());
        assertEquals(4, model.getNextSequence());
        verify(modelMapper).selectByCategoryAndCodeForUpdate(1L, "P4P");
        verify(modelMapper).updateById(model);
    }

    @Test
    void reservesAcrossThreeDigitBoundaryAndRejectsOverflow() {
        RentalDeviceCategoryDO category = category(1L, "DJI", "大疆");
        when(categoryMapper.selectByCode("DJI")).thenReturn(category);
        when(modelMapper.selectByCategoryAndCodeForUpdate(1L, "P4P"))
                .thenReturn(model(2L, 1L, "P4P", "P4P", 99))
                .thenReturn(model(2L, 1L, "P4P", "P4P", 999))
                .thenReturn(model(2L, 1L, "P4P", "P4P", 999));

        DeviceNumberReservation reservation = service.reserveDeviceNumbers("DJI", "P4P", 2);
        assertEquals(List.of("P4P-99", "P4P-100"), reservation.deviceNos());

        ServiceException exhausted = assertThrows(ServiceException.class,
                () -> service.reserveDeviceNumbers("DJI", "P4P", 2));
        assertEquals(RENTAL_DEVICE_SEQUENCE_EXHAUSTED.getCode(), exhausted.getCode());

        ServiceException overflow = assertThrows(ServiceException.class,
                () -> service.reserveDeviceNumbers("DJI", "P4P", Integer.MAX_VALUE));
        assertEquals(RENTAL_DEVICE_SEQUENCE_EXHAUSTED.getCode(), overflow.getCode());
    }

    @Test
    void returnsBackendNamesModelsAndPrefixes() {
        when(categoryMapper.selectEnabledList()).thenReturn(List.of(
                category(1L, "DJI", "大疆"),
                category(3L, "EMPTY", "暂无型号")));
        when(modelMapper.selectEnabledList()).thenReturn(List.of(
                model(2L, 1L, "P4P", "P4P", 1)));

        List<RentalDeviceCategoryRespVO> catalog = service.getCatalog();

        assertEquals(2, catalog.size());
        assertEquals("大疆", catalog.get(0).getCategoryName());
        assertEquals("P4P", catalog.get(0).getModels().get(0).getModelCode());
        assertEquals("P4P", catalog.get(0).getModels().get(0).getDeviceNoPrefix());
        assertTrue(catalog.get(1).getModels().isEmpty());
    }

    @Test
    void configurationCatalogIncludesDisabledEntriesAndVersions() {
        RentalDeviceCategoryDO disabledCategory = category(3L, "EMPTY", "暂无型号");
        disabledCategory.setEnabled(false);
        disabledCategory.setSortOrder(30);
        disabledCategory.setLockVersion(4);
        RentalDeviceModelDO disabledModel = model(2L, 1L, "P4P", "P4P", 1);
        disabledModel.setEnabled(false);
        disabledModel.setSortOrder(20);
        disabledModel.setLockVersion(7);
        when(categoryMapper.selectConfigurationList()).thenReturn(List.of(
                category(1L, "DJI", "大疆"), disabledCategory));
        when(modelMapper.selectConfigurationList()).thenReturn(List.of(disabledModel));

        RentalConfigurationCatalogRespVO catalog = service.getConfigurationCatalog();

        assertEquals(2, catalog.getCategories().size());
        assertEquals(false, catalog.getCategories().get(1).getEnabled());
        assertEquals(4, catalog.getCategories().get(1).getLockVersion());
        assertEquals(false, catalog.getCategories().get(0).getModels().get(0).getEnabled());
        assertEquals(7, catalog.getCategories().get(0).getModels().get(0).getLockVersion());
    }

    @Test
    void updatesCategoryWithTenantScopedOptimisticVersion() {
        when(categoryMapper.updateByIdAndVersion(any(RentalDeviceCategoryDO.class), eq(9L), eq(2)))
                .thenReturn(1);

        RentalDeviceCategoryUpdateReqVO reqVO = new RentalDeviceCategoryUpdateReqVO();
        reqVO.setId(1L);
        reqVO.setCategoryCode(" camera ");
        reqVO.setCategoryName(" 相机 ");
        reqVO.setSortOrder(10);
        reqVO.setLockVersion(2);

        assertEquals(3, service.updateCategory(reqVO));
        ArgumentCaptor<RentalDeviceCategoryDO> captor =
                ArgumentCaptor.forClass(RentalDeviceCategoryDO.class);
        verify(categoryMapper).updateByIdAndVersion(captor.capture(), eq(9L), eq(2));
        assertEquals(1L, captor.getValue().getId());
        assertEquals("CAMERA", captor.getValue().getCategoryCode());
        assertEquals("相机", captor.getValue().getCategoryName());
        assertEquals(10, captor.getValue().getSortOrder());
        assertEquals(3, captor.getValue().getLockVersion());
    }

    @Test
    void rejectsStaleCatalogVersionWithoutMutation() {
        when(categoryMapper.updateByIdAndVersion(any(RentalDeviceCategoryDO.class), eq(9L), eq(2)))
                .thenReturn(0);
        RentalDeviceCategoryUpdateReqVO reqVO = new RentalDeviceCategoryUpdateReqVO();
        reqVO.setId(1L);
        reqVO.setCategoryCode("DJI");
        reqVO.setCategoryName("大疆");
        reqVO.setLockVersion(2);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateCategory(reqVO));

        assertEquals(RENTAL_CONFIGURATION_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(categoryMapper).updateByIdAndVersion(any(RentalDeviceCategoryDO.class), eq(9L), eq(2));
        verify(categoryMapper, never()).updateById(any(RentalDeviceCategoryDO.class));
    }

    @Test
    void enablingModelRequiresEnabledCategory() {
        RentalDeviceModelDO current = model(2L, 1L, "P4P", "P4P", 1);
        current.setEnabled(false);
        current.setLockVersion(1);
        when(modelMapper.selectByIdForUpdate(2L)).thenReturn(current);
        RentalDeviceCategoryDO disabled = category(1L, "DJI", "大疆");
        disabled.setEnabled(false);
        when(categoryMapper.selectById(1L)).thenReturn(disabled);
        RentalDeviceCatalogStatusReqVO reqVO = new RentalDeviceCatalogStatusReqVO();
        reqVO.setId(2L);
        reqVO.setEnabled(true);
        reqVO.setLockVersion(1);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateModelStatus(reqVO));

        assertEquals(RENTAL_DEVICE_CATEGORY_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void rejectsDuplicatePrefixWhenUpdatingModel() {
        RentalDeviceCategoryDO category = category(1L, "DJI", "大疆");
        RentalDeviceModelDO current = model(2L, 1L, "P4P", "P4P", 1);
        current.setLockVersion(1);
        when(modelMapper.selectByIdForUpdate(2L)).thenReturn(current);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(modelMapper.selectByPrefix("M3")).thenReturn(model(3L, 1L, "M3", "M3", 1));
        RentalDeviceModelUpdateReqVO reqVO = new RentalDeviceModelUpdateReqVO();
        reqVO.setId(2L);
        reqVO.setCategoryId(1L);
        reqVO.setModelCode("P4P");
        reqVO.setModelName("P4P");
        reqVO.setDeviceNoPrefix("M3");
        reqVO.setLockVersion(1);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateModel(reqVO));

        assertEquals(RENTAL_DEVICE_PREFIX_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    void classifiesConcurrentModelCodeCollisionWhenPrefixIsUnchanged() {
        RentalDeviceCategoryDO category = category(1L, "DJI", "大疆");
        RentalDeviceModelDO current = model(2L, 1L, "P4P", "P4P", 1);
        current.setLockVersion(1);
        when(modelMapper.selectByIdForUpdate(2L)).thenReturn(current);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(modelMapper.selectByCode("P4P-PRO")).thenReturn(null);
        when(modelMapper.selectByPrefix("P4P")).thenReturn(current);
        when(modelMapper.updateByIdAndVersion(current, 9L, 1))
                .thenThrow(new DuplicateKeyException("model code collision"));
        RentalDeviceModelUpdateReqVO reqVO = new RentalDeviceModelUpdateReqVO();
        reqVO.setId(2L);
        reqVO.setCategoryId(1L);
        reqVO.setModelCode("P4P-PRO");
        reqVO.setModelName("P4P Pro");
        reqVO.setDeviceNoPrefix("P4P");
        reqVO.setLockVersion(1);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateModel(reqVO));

        assertEquals(RENTAL_DEVICE_MODEL_DUPLICATE.getCode(), ex.getCode());
    }

    private static RentalDeviceCategoryCreateReqVO categoryRequest(String code, String name) {
        RentalDeviceCategoryCreateReqVO reqVO = new RentalDeviceCategoryCreateReqVO();
        reqVO.setCategoryCode(code);
        reqVO.setCategoryName(name);
        return reqVO;
    }

    private static RentalDeviceModelCreateReqVO modelRequest(Long categoryId, String modelCode,
                                                              String modelName, String prefix) {
        RentalDeviceModelCreateReqVO reqVO = new RentalDeviceModelCreateReqVO();
        reqVO.setCategoryId(categoryId);
        reqVO.setModelCode(modelCode);
        reqVO.setModelName(modelName);
        reqVO.setDeviceNoPrefix(prefix);
        return reqVO;
    }

    private static RentalDeviceCategoryDO category(Long id, String code, String name) {
        return RentalDeviceCategoryDO.builder()
                .id(id)
                .categoryCode(code)
                .categoryName(name)
                .sortOrder(100)
                .enabled(true)
                .lockVersion(0)
                .build();
    }

    private static RentalDeviceModelDO model(Long id, Long categoryId, String code,
                                              String prefix, int nextSequence) {
        return RentalDeviceModelDO.builder()
                .id(id)
                .categoryId(categoryId)
                .modelCode(code)
                .modelName(code)
                .deviceNoPrefix(prefix)
                .nextSequence(nextSequence)
                .sortOrder(100)
                .enabled(true)
                .lockVersion(0)
                .build();
    }

}
