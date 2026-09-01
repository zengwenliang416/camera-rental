package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuShopRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuApplicationDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuShopDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuShopMapper;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadClient;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadEndpoint;
import cn.iocoder.yudao.module.rental.integration.xianyu.client.XianyuReadResponse;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuShopAdminServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shopPageMustMaskExternalAuthorizationIdentifiers() throws Exception {
        XianyuProperties properties = readyProperties();
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        PageParam pageParam = new PageParam();
        when(shopMapper.selectPage(eq(pageParam), any())).thenReturn(new PageResult<>(List.of(XianyuShopDO.builder()
                .id(1L)
                .applicationId(3L)
                .externalShopId("user-identity-1")
                .xianyuUserName("shop-user-1")
                .authorizeId("922158952480837")
                .shopName("demo")
                .authorizationStatus("VALID")
                .build()), 1L));
        XianyuShopAdminService service = new XianyuShopAdminService(
                runtimeConfig(properties, null), mock(XianyuReadClient.class), shopMapper,
                mock(XianyuAlertAdminService.class), objectMapper);

        PageResult<XianyuShopRespVO> page = service.getShopPage(pageParam);
        String json = objectMapper.writeValueAsString(page.getList().get(0));

        assertFalse(json.contains("user-identity-1"));
        assertFalse(json.contains("shop-user-1"));
        assertFalse(json.contains("922158952480837"));
    }

    @Test
    void shouldInvalidatePreviouslyAuthorizedShopMissingFromSuccessfulSnapshot() throws Exception {
        XianyuProperties properties = readyProperties();
        XianyuReadClient readClient = mock(XianyuReadClient.class);
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        XianyuAlertAdminService alertAdminService = mock(XianyuAlertAdminService.class);
        XianyuApplicationDO application = XianyuApplicationDO.builder().id(3L).build();
        XianyuShopDO stale = XianyuShopDO.builder()
                .id(9L)
                .applicationId(3L)
                .authorizeId("88")
                .authorizationStatus("VALID")
                .build();
        when(readClient.execute(eq(XianyuReadEndpoint.AUTHORIZED_SHOPS), any()))
                .thenReturn(response("{\"code\":0,\"data\":{\"list\":[]}}"));
        when(shopMapper.selectListByApplicationId(3L)).thenReturn(List.of(stale));

        XianyuShopAdminService service = new XianyuShopAdminService(
                runtimeConfig(properties, application), readClient, shopMapper, alertAdminService, objectMapper);

        assertEquals(1, service.syncAuthorizedShops());
        ArgumentCaptor<XianyuShopDO> captor = ArgumentCaptor.forClass(XianyuShopDO.class);
        verify(shopMapper).updateById(captor.capture());
        assertEquals("INVALID", captor.getValue().getAuthorizationStatus());
        verify(alertAdminService).recordShopAuthorizationInvalid(
                9L, "88", "Shop authorization disappeared from latest snapshot");
    }

    @Test
    void shouldPersistGuaranteeStatusAndAlertWhenDepositIsInsufficient() throws Exception {
        XianyuProperties properties = readyProperties();
        XianyuReadClient readClient = mock(XianyuReadClient.class);
        XianyuShopMapper shopMapper = mock(XianyuShopMapper.class);
        XianyuAlertAdminService alertAdminService = mock(XianyuAlertAdminService.class);
        XianyuApplicationDO application = XianyuApplicationDO.builder().id(3L).build();
        XianyuShopDO existing = XianyuShopDO.builder()
                .id(9L)
                .applicationId(3L)
                .authorizeId("922158952480837")
                .authorizationStatus("VALID")
                .build();
        when(readClient.execute(eq(XianyuReadEndpoint.AUTHORIZED_SHOPS), any()))
                .thenReturn(response("""
                        {"code":0,"data":{"list":[
                          {"authorize_id":922158952480837,"user_identity":"user-identity-1",
                           "user_name":"shop-user-1","shop_name":"demo",
                           "is_valid":true,"valid_end_time":1785081599,
                           "is_deposit_enough":false}
                        ]}}
                        """));
        when(shopMapper.selectListByApplicationId(3L)).thenReturn(List.of(existing));
        when(shopMapper.selectByApplicationAndAuthorizeId(3L, "922158952480837")).thenReturn(existing);

        XianyuShopAdminService service = new XianyuShopAdminService(
                runtimeConfig(properties, application), readClient, shopMapper, alertAdminService, objectMapper);

        assertEquals(1, service.syncAuthorizedShops());
        ArgumentCaptor<XianyuShopDO> captor = ArgumentCaptor.forClass(XianyuShopDO.class);
        verify(shopMapper).updateById(captor.capture());
        assertEquals("user-identity-1", captor.getValue().getExternalShopId());
        assertEquals("shop-user-1", captor.getValue().getXianyuUserName());
        assertEquals("DEPOSIT_INSUFFICIENT", captor.getValue().getGuaranteeStatus());
        verify(alertAdminService).recordGuaranteeHealth(
                9L, "922158952480837", "DEPOSIT_INSUFFICIENT");
    }

    private XianyuProperties readyProperties() {
        XianyuProperties properties = new XianyuProperties();
        properties.setEnabled(true);
        properties.setAppKey("test-app");
        properties.setAppSecret("test-secret");
        return properties;
    }

    private XianyuRuntimeConfigService runtimeConfig(XianyuProperties properties,
                                                     XianyuApplicationDO application) {
        XianyuRuntimeConfigService service = mock(XianyuRuntimeConfigService.class);
        when(service.getCurrent()).thenReturn(properties);
        when(service.getCurrentApplication()).thenReturn(application);
        return service;
    }

    private XianyuReadResponse response(String json) throws Exception {
        JsonNode payload = objectMapper.readTree(json);
        return new XianyuReadResponse(200, 0, payload, json);
    }

}
