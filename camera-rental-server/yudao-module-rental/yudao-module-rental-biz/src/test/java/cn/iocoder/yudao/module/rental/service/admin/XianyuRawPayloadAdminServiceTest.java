package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuRawPayloadRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuRawPayloadMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuRawPayloadAdminServiceTest {

    @BeforeEach
    void setTenantContext() {
        TenantContextHolder.setTenantId(9L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void getRawPayloadPageShouldReturnMetadataOnly() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        XianyuRawPayloadDO payload = payload();
        XianyuRawPayloadPageReqVO reqVO = new XianyuRawPayloadPageReqVO();
        when(rawPayloadMapper.selectPage(any(XianyuRawPayloadPageReqVO.class), any(LambdaQueryWrapperX.class)))
                .thenReturn(new PageResult<>(List.of(payload), 1L));
        XianyuRawPayloadAdminService service = new XianyuRawPayloadAdminService(
                rawPayloadMapper, new ObjectMapper());

        PageResult<XianyuRawPayloadRespVO> page = service.getRawPayloadPage(reqVO);
        String json = JsonUtils.toJsonString(page.getList().get(0));

        assertEquals(1L, page.getTotal());
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("杭州市 secret 路"));
        assertFalse(json.contains("3364202298717566229"));
        assertFalse(json.contains("AppSecret"));
    }

    @Test
    void getRawPayloadShouldReturnMaskedPayloadOnly() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        when(rawPayloadMapper.selectByTenantIdAndId(9L, 7L)).thenReturn(payload());
        XianyuRawPayloadAdminService service = new XianyuRawPayloadAdminService(
                rawPayloadMapper, new ObjectMapper());

        XianyuRawPayloadRespVO vo = service.getRawPayload(7L);
        String json = JsonUtils.toJsonString(vo);

        assertEquals("336***229", vo.getSourceIdentifier());
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("杭州市 secret 路"));
        assertFalse(json.contains("张三"));
        assertFalse(json.contains("AppSecret"));
        verify(rawPayloadMapper).selectByTenantIdAndId(9L, 7L);
    }

    @Test
    void getRawPayloadShouldRejectCrossTenantId() {
        XianyuRawPayloadMapper rawPayloadMapper = mock(XianyuRawPayloadMapper.class);
        XianyuRawPayloadAdminService service = new XianyuRawPayloadAdminService(
                rawPayloadMapper, new ObjectMapper());

        assertThrows(ServiceException.class, () -> service.getRawPayload(8L));

        verify(rawPayloadMapper).selectByTenantIdAndId(9L, 8L);
    }

    private XianyuRawPayloadDO payload() {
        return XianyuRawPayloadDO.builder()
                .id(7L)
                .sourceType("ORDER_DETAIL")
                .sourceIdentifier("3364202298717566229")
                .payloadHash("f".repeat(64))
                .schemaVersion("v1")
                .redactionVersion("v1")
                .payload("""
                        {
                          "order_no": "3364202298717566229",
                          "receiver_mobile": "13800138000",
                          "receiver_name": "张三",
                          "address": "杭州市 secret 路",
                          "seller_remark": "收件人：张三，手机 13800138000，收货地址：杭州市 secret 路",
                          "AppSecret": "plain-secret"
                        }
                        """)
                .receivedAt(LocalDateTime.of(2026, 7, 25, 10, 0))
                .build();
    }

}
