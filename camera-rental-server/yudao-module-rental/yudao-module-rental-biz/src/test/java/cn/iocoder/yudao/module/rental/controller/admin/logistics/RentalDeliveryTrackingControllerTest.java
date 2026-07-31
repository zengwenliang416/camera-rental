package cn.iocoder.yudao.module.rental.controller.admin.logistics;

import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryRefreshRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingDetailRespVO;
import cn.iocoder.yudao.module.rental.controller.admin.logistics.vo.RentalDeliveryTrackingOrderSummaryRespVO;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryRefreshResult;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryTrackingQueryService;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryTrackingRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RentalDeliveryTrackingControllerTest {

    private final RentalDeliveryTrackingQueryService queryService = mock(RentalDeliveryTrackingQueryService.class);
    private final RentalDeliveryTrackingRefreshService refreshService =
            mock(RentalDeliveryTrackingRefreshService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new RentalDeliveryTrackingController(queryService, refreshService))
            .build();

    @Test
    void exposesBatchDetailAndRefreshContracts() throws Exception {
        RentalDeliveryTrackingOrderSummaryRespVO summary = new RentalDeliveryTrackingOrderSummaryRespVO();
        summary.setOrderId(10L);
        summary.setPackageCount(0);
        when(queryService.getSummaries(eq(java.util.List.of(10L)))).thenReturn(Map.of(10L, summary));

        RentalDeliveryTrackingDetailRespVO detail = new RentalDeliveryTrackingDetailRespVO();
        detail.setDeliveryId(20L);
        detail.setMaskedWaybillNo("SF1****7890");
        when(queryService.getDetail(20L)).thenReturn(detail);

        when(refreshService.refresh(20L)).thenReturn(new RentalDeliveryRefreshResult(
                true, "REFRESH_QUEUED", LocalDateTime.of(2026, 7, 31, 12, 30)));

        mockMvc.perform(post("/rental/delivery/tracking-summary/batch")
                        .contentType("application/json")
                        .content("{\"orderIds\":[10]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.10.orderId").value(10))
                .andExpect(jsonPath("$.data.10.packageCount").value(0));

        mockMvc.perform(get("/rental/delivery/20/tracking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryId").value(20))
                .andExpect(jsonPath("$.data.maskedWaybillNo").value("SF1****7890"));

        mockMvc.perform(post("/rental/delivery/20/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.reason").value("REFRESH_QUEUED"))
                .andExpect(jsonPath("$.data.nextAllowedAt").value("2026-07-31T12:30:00"));

        verify(queryService).getSummaries(java.util.List.of(10L));
        verify(queryService).getDetail(20L);
        verify(refreshService).refresh(20L);
    }

    @Test
    void usesOneExplicitPermissionForAllTrackingEndpoints() throws Exception {
        for (String methodName : java.util.List.of("getTrackingSummaries", "getTracking", "refresh")) {
            Method method = java.util.Arrays.stream(RentalDeliveryTrackingController.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow();
            assertEquals("@ss.hasPermission('rental:delivery:tracking')",
                    method.getAnnotation(PreAuthorize.class).value());
        }
    }
}
