package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuExpressCandidateRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuExpressCompanyRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100AutoNumberCandidate;
import cn.iocoder.yudao.module.rental.integration.logistics.kuaidi100.Kuaidi100AutoNumberClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.service.XianyuPayloadHasher;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class XianyuExpressCompanyAdminService {

    private static final String SOURCE_TYPE = "EXPRESS_COMPANIES";
    private static final String SOURCE_IDENTIFIER = "express-companies";
    private static final String SCHEMA_VERSION = "XIAN_GUAN_JIA_EXPRESS_COMPANIES_V1";
    private static final String RESTRICTED_PAYLOAD_POLICY = "RESTRICTED_UNREDACTED_V1";
    private static final Logger log = LoggerFactory.getLogger(XianyuExpressCompanyAdminService.class);

    private final XianyuReadClient readClient;
    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final XianyuPayloadHasher payloadHasher;
    private final Kuaidi100AutoNumberClient autoNumberClient;
    private final Clock clock;

    public XianyuExpressCompanyAdminService(XianyuReadClient readClient,
                                            XianyuRawPayloadMapper rawPayloadMapper,
                                            XianyuPayloadHasher payloadHasher,
                                            Kuaidi100AutoNumberClient autoNumberClient,
                                            @Qualifier("xianyuClock") Clock clock) {
        this.readClient = readClient;
        this.rawPayloadMapper = rawPayloadMapper;
        this.payloadHasher = payloadHasher;
        this.autoNumberClient = autoNumberClient;
        this.clock = clock;
    }

    public List<XianyuExpressCompanyRespVO> getExpressCompanies() {
        XianyuReadResponse response = readClient.execute(XianyuReadEndpoint.EXPRESS_COMPANIES, null);
        persistRawPayload(response.rawBody());
        JsonNode list = response.payload().path("data").path("list");
        if (!list.isArray()) {
            return List.of();
        }
        List<XianyuExpressCompanyRespVO> result = new ArrayList<>(list.size());
        for (JsonNode item : list) {
            XianyuExpressCompanyRespVO vo = new XianyuExpressCompanyRespVO();
            vo.setCode(item.path("code").asText(""));
            vo.setExpressName(item.path("express_name").asText(""));
            vo.setExpressAlias(item.path("express_alias").asText(""));
            vo.setHot(item.path("is_hot").asBoolean(false));
            result.add(vo);
        }
        return result;
    }

    /**
     * 根据运单号识别可能的承运商（快递100 智能识别）。
     * 识别是辅助能力：第三方接口不可用时返回空列表，由运营手工选择快递公司。
     */
    public List<XianyuExpressCandidateRespVO> recognizeWaybill(String waybillNo) {
        List<Kuaidi100AutoNumberCandidate> candidates;
        try {
            candidates = autoNumberClient.recognize(waybillNo.trim());
        } catch (IOException | RuntimeException ex) {
            log.warn("[express][recognize] waybill recognition failed: {}", ex.getMessage());
            return List.of();
        }
        List<XianyuExpressCandidateRespVO> result = new ArrayList<>(candidates.size());
        for (Kuaidi100AutoNumberCandidate candidate : candidates) {
            XianyuExpressCandidateRespVO vo = new XianyuExpressCandidateRespVO();
            vo.setCode(candidate.comCode());
            vo.setName(candidate.name());
            result.add(vo);
        }
        return result;
    }

    private void persistRawPayload(String rawPayload) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        XianyuRawPayloadDO payload = XianyuRawPayloadDO.builder()
                .sourceType(SOURCE_TYPE)
                .sourceIdentifier(SOURCE_IDENTIFIER)
                .payloadHash(payloadHasher.sha256(rawPayload))
                .schemaVersion(SCHEMA_VERSION)
                .redactionVersion(RESTRICTED_PAYLOAD_POLICY)
                .payload(rawPayload)
                .receivedAt(LocalDateTime.now(clock))
                .build();
        payload.setCreator("system");
        payload.setUpdater("system");
        rawPayloadMapper.insertOrReuse(tenantId, payload);
    }

}
