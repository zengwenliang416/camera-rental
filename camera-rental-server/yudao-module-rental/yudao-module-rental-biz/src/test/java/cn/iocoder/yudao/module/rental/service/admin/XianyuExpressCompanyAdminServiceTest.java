package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100AutoNumberCandidate;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100AutoNumberClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuPayloadHasher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuExpressCompanyAdminServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private XianyuReadClient readClient;
    private XianyuRawPayloadMapper rawPayloadMapper;
    private Kuaidi100AutoNumberClient autoNumberClient;
    private XianyuExpressCompanyAdminService service;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(9L);
        readClient = mock(XianyuReadClient.class);
        rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        autoNumberClient = mock(Kuaidi100AutoNumberClient.class);
        service = new XianyuExpressCompanyAdminService(readClient, rawPayloadMapper, new XianyuPayloadHasher(),
                autoNumberClient, Clock.fixed(Instant.parse("2026-07-25T04:00:00Z"), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldPersistRestrictedRawPayloadBeforeReturningCompanies() throws Exception {
        String raw = """
                {"code":0,"msg":"OK","data":{"list":[
                {"code":"shentong","express_name":"申通快递","express_alias":"申通","is_hot":true},
                {"code":"other","express_name":"其他","express_alias":"","is_hot":false}
                ]}}
                """;
        when(readClient.execute(eq(XianyuReadEndpoint.EXPRESS_COMPANIES), any())).thenReturn(response(raw));

        var companies = service.getExpressCompanies();

        assertEquals(2, companies.size());
        assertEquals("shentong", companies.get(0).getCode());
        assertEquals("申通快递", companies.get(0).getExpressName());
        ArgumentCaptor<XianyuRawPayloadDO> payloadCaptor = ArgumentCaptor.forClass(XianyuRawPayloadDO.class);
        verify(rawPayloadMapper).insertOrReuse(eq(9L), payloadCaptor.capture());
        assertEquals("EXPRESS_COMPANIES", payloadCaptor.getValue().getSourceType());
        assertEquals("express-companies", payloadCaptor.getValue().getSourceIdentifier());
        assertEquals("XIAN_GUAN_JIA_EXPRESS_COMPANIES_V1", payloadCaptor.getValue().getSchemaVersion());
        assertEquals("RESTRICTED_UNREDACTED_V1", payloadCaptor.getValue().getRedactionVersion());
        assertEquals(raw, payloadCaptor.getValue().getPayload());
        assertEquals(64, payloadCaptor.getValue().getPayloadHash().length());
    }

    @Test
    void shouldMapRecognizedCandidates() throws Exception {
        when(autoNumberClient.recognize("SF5119694772350")).thenReturn(List.of(
                new Kuaidi100AutoNumberCandidate("shunfeng", "顺丰速运")));

        var candidates = service.recognizeWaybill(" SF5119694772350 ");

        assertEquals(1, candidates.size());
        assertEquals("shunfeng", candidates.get(0).getCode());
        assertEquals("顺丰速运", candidates.get(0).getName());
    }

    @Test
    void shouldReturnEmptyCandidatesWhenRecognitionFails() throws Exception {
        when(autoNumberClient.recognize(any())).thenThrow(new IOException("timeout"));

        assertEquals(List.of(), service.recognizeWaybill("SF5119694772350"));
    }

    private XianyuReadResponse response(String raw) throws Exception {
        JsonNode payload = objectMapper.readTree(raw);
        return new XianyuReadResponse(200, 0, payload, raw);
    }

}
