package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSalePageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuAfterSaleSyncRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuAfterSaleDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuAfterSaleMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncRunMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuAfterSalePageParser;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuPayloadHasher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuAfterSaleAdminServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private XianyuAfterSaleMapper afterSaleMapper;
    private XianyuRawPayloadMapper rawPayloadMapper;
    private XianyuSyncRunMapper syncRunMapper;
    private XianyuReadClient readClient;
    private XianyuAlertAdminService alertAdminService;
    private XianyuAfterSaleAdminService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        afterSaleMapper = mock(XianyuAfterSaleMapper.class);
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        syncRunMapper = mock(XianyuSyncRunMapper.class);
        readClient = mock(XianyuReadClient.class);
        alertAdminService = mock(XianyuAlertAdminService.class);
        when(shopMapper.selectByTenantIdAndId(9L, 7L)).thenReturn(XianyuShopDO.builder()
                .id(7L)
                .authorizeId("88")
                .authorizationStatus("VALID")
                .authorizationExpiresAt(LocalDateTime.of(2026, 8, 1, 0, 0))
                .build());
        doAnswer(invocation -> {
            invocation.getArgument(0, XianyuSyncRunDO.class).setId(99L);
            return 1;
        }).when(syncRunMapper).insert(any(XianyuSyncRunDO.class));
        doAnswer(invocation -> {
            XianyuRawPayloadDO payload = invocation.getArgument(1, XianyuRawPayloadDO.class);
            payload.setTenantId(invocation.getArgument(0, Long.class));
            return null;
        }).when(rawPayloadMapper).insertOrReuse(any(), any());
        when(rawPayloadMapper.selectByTenantIdAndSourceAndHashForUpdate(eq(9L), anyString(),
                any(), any())).thenReturn(XianyuRawPayloadDO.builder().id(31L).build());
        service = new XianyuAfterSaleAdminService(afterSaleMapper, shopMapper, rawPayloadMapper, syncRunMapper,
                readClient, new XianyuAfterSalePageParser(objectMapper), new XianyuPayloadHasher(), objectMapper,
                alertAdminService, Clock.fixed(Instant.parse("2026-07-24T04:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void syncPageShouldPersistAfterSaleAndRunOutcome() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.AFTER_SALES), any())).thenReturn(response("""
                {"code":0,"data":{"list":[{"refund_no":"refund-1","order_no":"order-1",
                "refund_status":2,"refund_amount":99,"apply_time":1784774400,"timeout_time":1784774400}],
                "has_next_page":true}}"""));
        when(readClient.execute(eq(XianyuReadEndpoint.AFTER_SALE_DETAIL), any())).thenReturn(response("""
                {"code":0,"data":{"refund_no":"refund-1","order_no":"order-1",
                "refund_status":3,"refund_amount":99,"apply_time":1784774400,"timeout_time":1784774400}}"""));
        XianyuAfterSaleSyncReqVO reqVO = req();

        XianyuAfterSaleSyncRespVO resp = service.syncPage(reqVO);

        assertEquals(99L, resp.getSyncRunId());
        assertEquals(1, resp.getReceivedCount());
        assertEquals(1, resp.getSucceededCount());
        ArgumentCaptor<XianyuAfterSaleDO> afterSaleCaptor = ArgumentCaptor.forClass(XianyuAfterSaleDO.class);
        verify(afterSaleMapper).insert(afterSaleCaptor.capture());
        assertEquals("refund-1", afterSaleCaptor.getValue().getExternalAfterSaleId());
        assertEquals("UNCONFIRMED", afterSaleCaptor.getValue().getAmountUnitStatus());
        assertEquals(31L, afterSaleCaptor.getValue().getRawPayloadId());
        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("SUCCEEDED", runCaptor.getValue().getStatus());
        verify(alertAdminService).recordAfterSaleTimeout(
                eq(7L), eq("refund-1"), eq(LocalDateTime.of(2026, 7, 23, 10, 40)));
        verify(rawPayloadMapper, org.mockito.Mockito.times(3)).insertOrReuse(eq(9L), any());
    }

    @Test
    void afterSalePageMustMaskExternalIdentifiers() throws Exception {
        XianyuAfterSalePageReqVO reqVO = new XianyuAfterSalePageReqVO();
        when(afterSaleMapper.selectAdminPage(eq(reqVO), any(), any())).thenReturn(new PageResult<>(
                List.of(XianyuAfterSaleDO.builder()
                        .id(1L)
                        .shopId(7L)
                        .externalAfterSaleId("393611004596913055")
                        .externalOrderId("5119489332694004337")
                        .afterSaleStatus("2")
                        .build()), 1L));

        PageResult<XianyuAfterSaleRespVO> page = service.getPage(reqVO);
        String json = objectMapper.writeValueAsString(page.getList().get(0));

        assertFalse(json.contains("393611004596913055"));
        assertFalse(json.contains("5119489332694004337"));
    }

    @Test
    void syncPageFailureShouldRecordDeduplicatedAlert() throws Exception {
        when(readClient.execute(eq(XianyuReadEndpoint.AFTER_SALES), any()))
                .thenThrow(new IllegalStateException("transport secret"));

        org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.syncPage(req()));

        ArgumentCaptor<XianyuSyncRunDO> runCaptor = ArgumentCaptor.forClass(XianyuSyncRunDO.class);
        verify(syncRunMapper).updateById(runCaptor.capture());
        assertEquals("FAILED", runCaptor.getValue().getStatus());
        verify(alertAdminService).recordSyncFailed(7L, "AFTER_SALE", "IllegalStateException");
    }

    private XianyuAfterSaleSyncReqVO req() {
        XianyuAfterSaleSyncReqVO reqVO = new XianyuAfterSaleSyncReqVO();
        reqVO.setShopId(7L);
        reqVO.setApplyStart(LocalDateTime.of(2026, 7, 20, 0, 0));
        reqVO.setApplyEnd(LocalDateTime.of(2026, 7, 24, 0, 0));
        reqVO.setPageNo(1);
        reqVO.setPageSize(50);
        return reqVO;
    }

    private XianyuReadResponse response(String json) throws Exception {
        JsonNode payload = objectMapper.readTree(json);
        return new XianyuReadResponse(200, 0, payload, json);
    }

}
