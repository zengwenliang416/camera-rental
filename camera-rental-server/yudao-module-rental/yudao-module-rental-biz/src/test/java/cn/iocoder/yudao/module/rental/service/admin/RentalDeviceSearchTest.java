package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDevicePageReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RentalDeviceSearchTest {
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                RentalDeviceDO.class);
    }

    @Test
    void searchesDeviceNumberAcrossCategoriesAndReturnsBackfillFields() {
        RentalDevicePageReqVO request = request("  CAM-001  ");
        var result = service().getDevicePage(request);
        assertEquals(List.of(1L), result.getList().stream().map(item -> item.getId()).toList());
        assertEquals("CAMERA", result.getList().get(0).getCategoryCode());
        assertEquals("A7M4", result.getList().get(0).getEquipmentModelCode());
    }

    @Test
    void serialNumberOrBranchCannotEscapeCategoryModelAndEnabledFilters() {
        RentalDevicePageReqVO request = request("MATCH");
        request.setCategoryCode("CAMERA");
        request.setEquipmentModelCode("A7M4");
        request.setEnabled(true);
        assertEquals(List.of(1L), service().getDevicePage(request).getList().stream()
                .map(item -> item.getId()).toList());
    }

    @Test
    void blankKeywordKeepsExistingPaginationAndOptionalEnabledBehavior() {
        RentalDevicePageReqVO request = request("  ");
        request.setPageSize(2);
        request.setPageNo(2);
        var result = service().getDevicePage(request);
        assertEquals(4L, result.getTotal());
        assertEquals(List.of(2L, 1L), result.getList().stream().map(item -> item.getId()).toList());
    }

    @Test
    void noMatchesAndSqlLikeInjectionTextReturnNoDevices() {
        assertEquals(0L, service().getDevicePage(request("not-found")).getTotal());
        assertEquals(0L, service().getDevicePage(request("' OR 1=1 --")).getTotal());
    }

    @Test
    void rejectsOversizedKeywordThroughRequestValidation() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(request("x".repeat(101))).isEmpty());
        }
    }

    private RentalDevicePageReqVO request(String keyword) {
        RentalDevicePageReqVO request = new RentalDevicePageReqVO();
        request.setKeyword(keyword);
        request.setPageNo(1);
        request.setPageSize(20);
        return request;
    }

    // Execute the service's actual bound predicate against H2, including the OR grouping.
    @SuppressWarnings("unchecked")
    private RentalDeviceAdminService service() {
        RentalDeviceMapper mapper = mock(RentalDeviceMapper.class);
        when(mapper.selectPage(any(PageParam.class), any(LambdaQueryWrapperX.class)))
                .thenAnswer(invocation -> {
                    PageParam page = invocation.getArgument(0);
                    LambdaQueryWrapperX<RentalDeviceDO> query = invocation.getArgument(1);
                    var parameters = new ArrayList<Object>();
                    var matcher = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.([^}]+)}")
                            .matcher(query.getCustomSqlSegment());
                    StringBuilder sql = new StringBuilder();
                    while (matcher.find()) {
                        parameters.add(query.getParamNameValuePairs().get(matcher.group(1)));
                        matcher.appendReplacement(sql, "?");
                    }
                    matcher.appendTail(sql);
                    try (var connection = DriverManager.getConnection("jdbc:h2:mem:device_search")) {
                        connection.createStatement().execute("CREATE TABLE rental_device "
                                + "(id BIGINT, device_no VARCHAR, serial_number VARCHAR, "
                                + "category_code VARCHAR, equipment_model_code VARCHAR, enabled BOOLEAN)");
                        connection.createStatement().execute("INSERT INTO rental_device VALUES "
                                + "(1, 'CAM-001', 'SN-MATCH-1', 'CAMERA', 'A7M4', TRUE),"
                                + "(2, 'CAM-002', 'SN-MATCH-2', 'CAMERA', 'A7M4', FALSE),"
                                + "(3, 'DRONE-003', 'SN-MATCH-3', 'DRONE', 'P4P', TRUE),"
                                + "(4, 'CAM-004', 'SN-MATCH-4', 'CAMERA', 'A7M3', TRUE)");
                        try (var statement = connection.prepareStatement("SELECT * FROM rental_device " + sql)) {
                            for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
                            var rows = statement.executeQuery();
                            var devices = new ArrayList<RentalDeviceDO>();
                            while (rows.next()) {
                                devices.add(RentalDeviceDO.builder().id(rows.getLong("id"))
                                        .deviceNo(rows.getString("device_no"))
                                        .serialNumber(rows.getString("serial_number"))
                                        .categoryCode(rows.getString("category_code"))
                                        .equipmentModelCode(rows.getString("equipment_model_code"))
                                        .enabled(rows.getBoolean("enabled")).build());
                            }
                            int from = Math.min((page.getPageNo() - 1) * page.getPageSize(), devices.size());
                            int to = Math.min(from + page.getPageSize(), devices.size());
                            return new PageResult<>(devices.subList(from, to), (long) devices.size());
                        }
                    }
                });
        return new RentalDeviceAdminService(mapper, null, null, null, null,
                new RentalDeviceProperties(), null);
    }
}
