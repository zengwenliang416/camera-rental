package cn.iocoder.yudao.module.rental.service.admin;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffWarehouseVO.*;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.*;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.*;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class RentalStaffWarehouseServiceTest {
    final RentalStaffIssueMapper issues = mock(RentalStaffIssueMapper.class);
    final RentalDeviceMapper devices = mock(RentalDeviceMapper.class);
    final RentalOrderMapper orders = mock(RentalOrderMapper.class);
    final RentalStaffStocktakeMapper stocktakes = mock(RentalStaffStocktakeMapper.class);
    final RentalStaffStocktakeLineMapper lines = mock(RentalStaffStocktakeLineMapper.class);
    final RentalStaffPhotoMapper photos = mock(RentalStaffPhotoMapper.class);
    final RentalStaffInspectionMapper inspections = mock(RentalStaffInspectionMapper.class);
    final RentalDeviceAssignmentMapper assignments = mock(RentalDeviceAssignmentMapper.class);
    final RentalDeviceOpsService operations = mock(RentalDeviceOpsService.class);
    final RentalStaffInspectionTemplateMapper templates = mock(RentalStaffInspectionTemplateMapper.class);
    final RentalDeviceModelMapper models = mock(RentalDeviceModelMapper.class);
    final FileApi files = mock(FileApi.class);
    final RentalStaffStocktakeService stock = new RentalStaffStocktakeService(stocktakes,lines,devices);
    final RentalStaffIssueService issue = new RentalStaffIssueService(issues,devices,orders);
    final RentalStaffInspectionService inspection = new RentalStaffInspectionService(photos,inspections,devices,assignments,operations,files,new ObjectMapper(),templates,models);
    @BeforeEach void tenant() { TenantContextHolder.setTenantId(9L); }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void issueCannotLinkMissingOrderOrDevice() {
        var req = new IssueSave(); req.setTitle("TEST"); req.setRentalOrderId(1L);
        assertThrows(ServiceException.class, () -> issue.create(req));
        verify(issues,never()).insert(any(RentalStaffIssueDO.class));
    }
    @Test void issueConcurrentRevisionCannotOverwrite() {
        var row = new RentalStaffIssueDO(); row.setId(1L); row.setRevision(2);
        when(issues.selectOneForUpdate(any())).thenReturn(row);
        var req = new IssueAction(); req.setId(1L); req.setRevision(1); req.setAction("CLAIM");
        assertThrows(ServiceException.class, () -> issue.action(req));
        verify(issues,never()).updateById(any(RentalStaffIssueDO.class));
    }
    @Test void stocktakeRepeatedScanOnlyMarksOnceAndDoesNotMoveDevice() {
        var session = new RentalStaffStocktakeDO(); session.setId(1L); session.setStatus("OPEN"); session.setWarehouseCode("A");
        when(stocktakes.selectOneForUpdate(any())).thenReturn(session);
        var device = RentalDeviceDO.builder().id(2L).deviceNo("TEST-02").status("AVAILABLE").warehouseCode("B").build();
        when(devices.selectByIdForUpdate(2L)).thenReturn(device);
        var line = new RentalStaffStocktakeLineDO(); line.setScanned(false);
        when(lines.selectOne(any())).thenReturn(line);
        var req = new StocktakeAction(); req.setId(1L); req.setDeviceId(2L);
        stock.scan(req); stock.scan(req);
        verify(lines,times(1)).updateById(line);
        verify(devices,never()).updateById(any(RentalDeviceDO.class));
    }
    @Test void closedStocktakeCannotBeScanned() {
        var session = new RentalStaffStocktakeDO(); session.setId(1L); session.setStatus("CLOSED");
        when(stocktakes.selectOneForUpdate(any())).thenReturn(session);
        var req = new StocktakeAction(); req.setId(1L); req.setDeviceId(2L);
        assertThrows(ServiceException.class, () -> stock.scan(req));
        verifyNoInteractions(devices);
    }
    @Test void changedWarehouseCannotBeOverwrittenByOldStocktake() {
        var session = new RentalStaffStocktakeDO(); session.setId(1L); session.setStatus("CLOSED"); session.setWarehouseCode("A");
        when(stocktakes.selectOneForUpdate(any())).thenReturn(session);
        var line = new RentalStaffStocktakeLineDO(); line.setDeviceId(2L); line.setScanned(true); line.setOriginalWarehouse("B");
        when(lines.selectOne(any())).thenReturn(line);
        when(devices.selectByIdForUpdate(2L)).thenReturn(RentalDeviceDO.builder().id(2L).status("AVAILABLE").warehouseCode("C").build());
        var req = new StocktakeAction(); req.setId(1L); req.setDeviceId(2L);
        assertThrows(ServiceException.class, () -> stock.move(req));
        verify(devices,never()).updateById(any(RentalDeviceDO.class));
    }
    @Test void stocktakeOnlyMovesScannedDeviceAfterReview() {
        var session = new RentalStaffStocktakeDO(); session.setId(1L); session.setStatus("CLOSED"); session.setWarehouseCode("A");
        when(stocktakes.selectOneForUpdate(any())).thenReturn(session);
        var line = new RentalStaffStocktakeLineDO(); line.setDeviceId(2L); line.setScanned(true); line.setOriginalWarehouse("B");
        when(lines.selectOne(any())).thenReturn(line);
        var device = RentalDeviceDO.builder().id(2L).status("MAINTENANCE").warehouseCode("B").build();
        when(devices.selectByIdForUpdate(2L)).thenReturn(device);
        var req = new StocktakeAction(); req.setId(1L); req.setDeviceId(2L);
        stock.move(req); stock.move(req);
        assertEquals("A",device.getWarehouseCode()); assertEquals("MAINTENANCE",device.getStatus());
        verify(devices,times(1)).updateById(device);
    }
    InspectionSubmit inspectionRequest() {
        when(devices.selectByIdForUpdate(2L)).thenReturn(RentalDeviceDO.builder().id(2L).equipmentModelCode("TEST").build());
        when(assignments.selectById(3L)).thenReturn(RentalDeviceAssignmentDO.builder().id(3L).deviceId(2L).build());
        var req = new InspectionSubmit(); req.setDeviceId(2L); req.setAssignmentId(3L); req.setIdempotencyKey("test");
        req.setChecks(inspection.template("TEST")); req.setPhotoIds(List.of()); return req;
    }
    @Test void inspectionRejectsPhotoFromAnotherCycle() {
        var req=inspectionRequest(); req.setPhotoIds(List.of(4L));
        var photo=new RentalStaffPhotoDO(); photo.setDeviceId(2L); photo.setAssignmentId(8L); photo.setConfirmed(true);
        when(photos.selectById(4L)).thenReturn(photo);
        assertThrows(ServiceException.class, () -> inspection.submit(req)); verifyNoInteractions(operations);
    }
    @Test void missingAccessoryIsFailureEvenWhenClientSaysPass() {
        var req=inspectionRequest(); var check=new Check(); check.setLabel("电池"); check.setExpected(2); check.setActual(1); check.setResult("PASS");
        var template=new RentalStaffInspectionTemplateDO(); template.setChecklistJson("[{\"label\":\"电池\",\"result\":\"PASS\",\"expected\":2}]");
        when(templates.selectOne(any())).thenReturn(template); req.setChecks(List.of(check));
        inspection.submit(req);
        var capture=org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceReturnReqVO.class);
        verify(operations).inspectDevice(capture.capture()); assertFalse(capture.getValue().getInspectPassed());
        verify(inspections).insert(any(RentalStaffInspectionDO.class));
    }
    @Test void changedTemplateCannotSilentlySkipChecks() {
        var req=inspectionRequest(); req.setChecks(req.getChecks().subList(0,1));
        assertThrows(ServiceException.class, () -> inspection.submit(req)); verifyNoInteractions(operations);
    }
    @Test void photoReadCannotExposeUnownedId() {
        assertThrows(ServiceException.class, () -> inspection.confirm(9L)); verifyNoInteractions(files);
    }
}
