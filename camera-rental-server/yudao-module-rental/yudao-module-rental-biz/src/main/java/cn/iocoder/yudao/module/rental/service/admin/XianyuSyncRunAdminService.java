package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuSyncRunPageReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuSyncRunRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncRunDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuSyncRunMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class XianyuSyncRunAdminService {

    private static final String SAFE_ERROR_MESSAGE = "Order synchronization page failed";

    private final XianyuSyncRunMapper syncRunMapper;

    public XianyuSyncRunAdminService(XianyuSyncRunMapper syncRunMapper) {
        this.syncRunMapper = syncRunMapper;
    }

    public PageResult<XianyuSyncRunRespVO> getSyncRunPage(XianyuSyncRunPageReqVO reqVO) {
        PageResult<XianyuSyncRunDO> page = syncRunMapper.selectPage(reqVO, new LambdaQueryWrapperX<XianyuSyncRunDO>()
                .eqIfPresent(XianyuSyncRunDO::getShopId, reqVO.getShopId())
                .eqIfPresent(XianyuSyncRunDO::getResourceType, reqVO.getResourceType())
                .eqIfPresent(XianyuSyncRunDO::getStatus, reqVO.getStatus())
                .eqIfPresent(XianyuSyncRunDO::getTriggerType, reqVO.getTriggerType())
                .orderByDesc(XianyuSyncRunDO::getId));
        List<XianyuSyncRunRespVO> list = page.getList().stream().map(this::toRespVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal());
    }

    private XianyuSyncRunRespVO toRespVO(XianyuSyncRunDO syncRun) {
        XianyuSyncRunRespVO vo = new XianyuSyncRunRespVO();
        vo.setId(syncRun.getId());
        vo.setShopId(syncRun.getShopId());
        vo.setResourceType(syncRun.getResourceType());
        vo.setTriggerType(syncRun.getTriggerType());
        vo.setStatus(syncRun.getStatus());
        vo.setWindowStart(syncRun.getWindowStart());
        vo.setWindowEnd(syncRun.getWindowEnd());
        vo.setReceivedCount(syncRun.getReceivedCount());
        vo.setDeduplicatedCount(syncRun.getDeduplicatedCount());
        vo.setSucceededCount(syncRun.getSucceededCount());
        vo.setReviewRequiredCount(syncRun.getReviewRequiredCount());
        vo.setFailedCount(syncRun.getFailedCount());
        vo.setLastErrorCode(syncRun.getLastErrorCode());
        vo.setLastErrorMessage(StringUtils.hasText(syncRun.getLastErrorMessage()) ? SAFE_ERROR_MESSAGE : null);
        vo.setStartedAt(syncRun.getStartedAt());
        vo.setFinishedAt(syncRun.getFinishedAt());
        return vo;
    }

}
