package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuSyncRunPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuSyncRunRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncRunMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuSyncRunAdminServiceTest {

    @Test
    void getSyncRunPageShouldApplyFiltersAndOrderByIdDesc() {
        XianyuSyncRunMapper syncRunMapper = mock(XianyuSyncRunMapper.class);
        XianyuSyncRunDO syncRun = XianyuSyncRunDO.builder()
                .id(99L)
                .shopId(7L)
                .resourceType("ORDER")
                .triggerType("MANUAL")
                .status("SUCCEEDED")
                .build();
        when(syncRunMapper.selectPage(any(XianyuSyncRunPageReqVO.class), any(LambdaQueryWrapperX.class)))
                .thenReturn(new PageResult<>(List.of(syncRun), 1L));
        XianyuSyncRunAdminService service = new XianyuSyncRunAdminService(syncRunMapper);
        XianyuSyncRunPageReqVO reqVO = new XianyuSyncRunPageReqVO();
        reqVO.setShopId(7L);
        reqVO.setResourceType("ORDER");
        reqVO.setStatus("SUCCEEDED");
        reqVO.setTriggerType("MANUAL");

        PageResult<XianyuSyncRunRespVO> page = service.getSyncRunPage(reqVO);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals(99L, page.getList().get(0).getId());
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                XianyuSyncRunDO.class);
        ArgumentCaptor<LambdaQueryWrapperX<XianyuSyncRunDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapperX.class);
        verify(syncRunMapper).selectPage(any(XianyuSyncRunPageReqVO.class), captor.capture());
        String sqlSegment = captor.getValue().getSqlSegment();
        assertTrue(sqlSegment.contains("shop_id"));
        assertTrue(sqlSegment.contains("resource_type"));
        assertTrue(sqlSegment.contains("status"));
        assertTrue(sqlSegment.contains("trigger_type"));
        assertTrue(sqlSegment.toLowerCase().contains("order by"));
        assertTrue(sqlSegment.toLowerCase().contains("id desc"));
    }

    @Test
    void getSyncRunPageShouldOnlyExposeRedactedOperationalFields() throws Exception {
        XianyuSyncRunMapper syncRunMapper = mock(XianyuSyncRunMapper.class);
        XianyuSyncRunDO syncRun = XianyuSyncRunDO.builder()
                .id(1L)
                .shopId(2L)
                .resourceType("ORDER")
                .triggerType("SCHEDULED")
                .status("FAILED")
                .windowStart(LocalDateTime.of(2026, 7, 1, 0, 0))
                .windowEnd(LocalDateTime.of(2026, 7, 1, 6, 0))
                .receivedCount(10)
                .deduplicatedCount(3)
                .succeededCount(6)
                .reviewRequiredCount(1)
                .failedCount(1)
                .lastErrorCode("TIMEOUT")
                .lastErrorMessage("gateway timeout")
                .startedAt(LocalDateTime.of(2026, 7, 1, 6, 1))
                .finishedAt(LocalDateTime.of(2026, 7, 1, 6, 3))
                .build();
        syncRun.setTenantId(9L);
        syncRun.setCreator("system");
        syncRun.setUpdater("system");
        when(syncRunMapper.selectPage(any(XianyuSyncRunPageReqVO.class), any(LambdaQueryWrapperX.class)))
                .thenReturn(new PageResult<>(List.of(syncRun), 1L));
        XianyuSyncRunAdminService service = new XianyuSyncRunAdminService(syncRunMapper);

        PageResult<XianyuSyncRunRespVO> page = service.getSyncRunPage(new XianyuSyncRunPageReqVO());
        String json = JsonUtils.toJsonString(page.getList().get(0));

        assertTrue(json.contains("Order synchronization page failed"));
        assertFalse(json.contains("gateway timeout"));
        assertFalse(json.contains("tenantId"));
        assertFalse(json.contains("creator"));
        assertFalse(json.contains("updater"));
        assertFalse(json.contains("createTime"));
        assertFalse(json.contains("updateTime"));
        assertFalse(json.contains("deleted"));
    }

}
