package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.*;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.*;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RentalDeviceImportServiceTest {
    RentalDeviceMapper devices = mock(RentalDeviceMapper.class);
    RentalDeviceImportBatchMapper batches = mock(RentalDeviceImportBatchMapper.class);
    RentalDeviceCatalogService catalog = mock(RentalDeviceCatalogService.class);
    RentalDeviceAdminService admin = mock(RentalDeviceAdminService.class);
    RentalDeviceImportService service = new RentalDeviceImportService(devices, batches, catalog, admin);
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(9L);
        when(catalog.findEnabledModel("A5")).thenReturn(Optional.of(
                new RentalDeviceCatalogService.CatalogModel(1L,"DJI","大疆",2L,"A5","A5","A5")));
        when(devices.selectImportCandidates(eq(9L), anyCollection(), anyCollection())).thenReturn(List.of());
    }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }
    RentalDeviceImportReqVO request(String mode, String... pairs) {
        var result = new RentalDeviceImportReqVO(); result.setMode(mode);
        var rows = new ArrayList<RentalDeviceImportReqVO.Row>();
        for (int i = 0; i < pairs.length; i += 2) {
            var row = new RentalDeviceImportReqVO.Row(); row.setFileName("sample.txt"); row.setLineNumber(i / 2 + 1);
            row.setCategoryCode("DJI"); row.setEquipmentModelCode("A5"); row.setDeviceNo(pairs[i]); row.setSerialNumber(pairs[i+1]); rows.add(row);
        }
        result.setRows(rows); return result;
    }
    RentalDeviceDO existing() {
        var device = RentalDeviceDO.builder().id(10L).deviceNo("A5-01").serialNumber("00001234").equipmentModelCode("A5").enabled(true).build();
        device.setDeleted(false); return device;
    }
    @Test void matchesSerialOnlyAndDeduplicatesAcrossFilesWithoutChangingOriginalSerial() {
        when(devices.selectImportCandidates(eq(9L), anyCollection(), anyCollection())).thenReturn(List.of(existing()));
        var result = service.inspect(request("REPRINT", "", "00001234", "", "00001234"));
        assertEquals(List.of("MATCHED","DUPLICATE"), result.getRows().stream().map(RentalDeviceImportRespVO.Row::getStatus).toList());
        assertEquals("00001234", result.getRows().get(0).getSerialNumber()); assertTrue(result.isCanSubmit());
    }
    @Test void blocksIdentityMismatchDeletedDevicesAndWrongModelPrefix() {
        when(devices.selectImportCandidates(eq(9L), anyCollection(), anyCollection())).thenReturn(List.of(existing()));
        assertFalse(service.inspect(request("CREATE","A5-01","OTHER1234")).isCanSubmit());
        var deleted = existing(); deleted.setDeleted(true);
        when(devices.selectImportCandidates(eq(9L), anyCollection(), anyCollection())).thenReturn(List.of(deleted));
        assertEquals("DEVICE_DISABLED", service.inspect(request("CREATE","A5-01","")).getRows().get(0).getReason());
        assertFalse(service.inspect(request("CREATE","P3-01","DEMO1234")).isCanSubmit());
        assertEquals("NUMBER_INVALID",service.inspect(request("CREATE","A5-OTHER-01","DEMO1234")).getRows().get(0).getReason());
    }
    @Test void reprintNeverTreatsMissingAsNewAndConflictingCrossFileRowsBlockAll() {
        assertEquals("MISSING", service.inspect(request("REPRINT","A5-01","")).getRows().get(0).getStatus());
        assertFalse(service.inspect(request("CREATE","A5-01","DEMO1111","A5-02","DEMO1111")).isCanSubmit());
        assertEquals("SERIAL_INVALID", service.inspect(request("CREATE","","123")).getRows().get(0).getReason());
    }
    @Test void commitReusesReceiptAndRejectsAnotherOwner() {
        var batch = new RentalDeviceImportBatchDO(); batch.setResultJson("{\"deviceIds\":[10,11]}");
        when(batches.lockOwned("batch", 8L)).thenReturn(batch);
        assertEquals(List.of(10L,11L), service.commit("batch",8L)); verifyNoInteractions(admin);
        assertThrows(ServiceException.class, () -> service.commit("batch",99L));
    }
    @Test void commitRechecksPreviewAndRejectsExpiredOrChangedSnapshot() {
        var input = request("CREATE","A5-01","DEMO1234");
        var batch = new RentalDeviceImportBatchDO(); batch.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
        batch.setRequestJson(JsonUtils.toJsonString(input)); batch.setPreviewJson(JsonUtils.toJsonString(service.inspect(input)));
        when(batches.lockOwned("batch",8L)).thenReturn(batch);
        when(devices.selectImportCandidates(eq(9L), anyCollection(), anyCollection())).thenReturn(List.of(existing()));
        assertThrows(ServiceException.class, () -> service.commit("batch",8L));
        batch.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        assertThrows(ServiceException.class, () -> service.commit("batch",8L)); verifyNoInteractions(admin);
    }
}
