package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.XIANYU_RAW_PAYLOAD_NOT_EXISTS;

@Service
public class XianyuRawPayloadAdminService {

    private static final List<String> SENSITIVE_KEY_PARTS = List.of(
            "secret", "sign", "token", "mobile", "phone", "tel", "address", "addr",
            "receiver", "recipient", "contact", "name", "buyer", "seller", "idcard", "identity");

    private final XianyuRawPayloadMapper rawPayloadMapper;
    private final ObjectMapper objectMapper;

    public XianyuRawPayloadAdminService(XianyuRawPayloadMapper rawPayloadMapper, ObjectMapper objectMapper) {
        this.rawPayloadMapper = rawPayloadMapper;
        this.objectMapper = objectMapper;
    }

    public PageResult<XianyuRawPayloadRespVO> getRawPayloadPage(XianyuRawPayloadPageReqVO reqVO) {
        PageResult<XianyuRawPayloadDO> page = rawPayloadMapper.selectPage(reqVO,
                new LambdaQueryWrapperX<XianyuRawPayloadDO>()
                        .eqIfPresent(XianyuRawPayloadDO::getSourceType, reqVO.getSourceType())
                        .eqIfPresent(XianyuRawPayloadDO::getSourceIdentifier, reqVO.getSourceIdentifier())
                        .orderByDesc(XianyuRawPayloadDO::getReceivedAt)
                        .orderByDesc(XianyuRawPayloadDO::getId));
        List<XianyuRawPayloadRespVO> list = page.getList().stream()
                .map(payload -> toVo(payload, false))
                .toList();
        return new PageResult<>(list, page.getTotal());
    }

    public XianyuRawPayloadRespVO getRawPayload(Long id) {
        XianyuRawPayloadDO payload = rawPayloadMapper.selectByTenantIdAndId(
                TenantContextHolder.getRequiredTenantId(), id);
        if (payload == null) {
            throw exception(XIANYU_RAW_PAYLOAD_NOT_EXISTS);
        }
        return toVo(payload, true);
    }

    private XianyuRawPayloadRespVO toVo(XianyuRawPayloadDO payload, boolean includeMaskedPayload) {
        XianyuRawPayloadRespVO vo = new XianyuRawPayloadRespVO();
        vo.setId(payload.getId());
        vo.setSourceType(payload.getSourceType());
        vo.setSourceIdentifier(XianyuAdminPrivacyMasker.maskIdentifier(payload.getSourceIdentifier()));
        vo.setPayloadHash(payload.getPayloadHash());
        vo.setSchemaVersion(payload.getSchemaVersion());
        vo.setRedactionVersion(payload.getRedactionVersion());
        vo.setReceivedAt(payload.getReceivedAt());
        if (includeMaskedPayload) {
            vo.setMaskedPayload(maskPayload(payload.getPayload()));
        }
        return vo;
    }

    private String maskPayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return payload;
        }
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode masked = maskJsonNode(node);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(masked);
        } catch (Exception ignored) {
            return XianyuAdminPrivacyMasker.maskFreeText(payload);
        }
    }

    private JsonNode maskJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode masked = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                boolean sensitiveKey = isSensitiveKey(field.getKey());
                String key = sensitiveKey ? "redacted_" + masked.size() : field.getKey();
                masked.set(key, sensitiveKey ? TextNode.valueOf("***") : maskJsonNode(field.getValue()));
            }
            return masked;
        }
        if (node.isArray()) {
            ArrayNode masked = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                masked.add(maskJsonNode(item));
            }
            return masked;
        }
        if (node.isTextual()) {
            return TextNode.valueOf(XianyuAdminPrivacyMasker.maskFreeText(node.asText()));
        }
        return node;
    }

    private boolean isSensitiveKey(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
    }

}
