package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface XianyuPushEventMapper extends BaseMapperX<XianyuPushEventDO> {

    @Insert("""
            INSERT INTO xianyu_push_event (
                tenant_id, event_type, dedupe_key, external_identifier, processing_status,
                processing_token, raw_payload_id, last_error_code, last_error_message, processed_at,
                creator, create_time, updater, update_time, deleted
            ) VALUES (
                #{tenantId}, #{event.eventType}, #{event.dedupeKey}, #{event.externalIdentifier},
                #{event.processingStatus}, #{event.processingToken}, #{event.rawPayloadId}, #{event.lastErrorCode},
                #{event.lastErrorMessage}, #{event.processedAt}, #{event.creator}, NOW(),
                #{event.updater}, NOW(), b'0'
            )
            ON DUPLICATE KEY UPDATE id = id
            """)
    void insertOrReuse(@Param("tenantId") Long tenantId, @Param("event") XianyuPushEventDO event);

    default XianyuPushEventDO selectByTenantIdAndDedupeKeyForUpdate(Long tenantId, String dedupeKey) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuPushEventDO>()
                .eq(XianyuPushEventDO::getTenantId, tenantId)
                .eq(XianyuPushEventDO::getDedupeKey, dedupeKey));
    }

    default XianyuPushEventDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<XianyuPushEventDO>()
                .eq(XianyuPushEventDO::getTenantId, tenantId)
                .eq(XianyuPushEventDO::getId, id));
    }

    default XianyuPushEventDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuPushEventDO>()
                .eq(XianyuPushEventDO::getId, id));
    }

    default List<XianyuPushEventDO> selectRetryCandidates(LocalDateTime staleBefore, int limit) {
        int boundedLimit = Math.max(1, Math.min(500, limit));
        return selectList(new LambdaQueryWrapperX<XianyuPushEventDO>()
                .in(XianyuPushEventDO::getProcessingStatus, "RECEIVED", "FAILED", "PROCESSING")
                .le(XianyuPushEventDO::getUpdateTime, staleBefore)
                .orderByAsc(XianyuPushEventDO::getUpdateTime)
                .orderByAsc(XianyuPushEventDO::getId)
                .last("LIMIT " + boundedLimit));
    }

}
