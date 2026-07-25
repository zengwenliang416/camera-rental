package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuAlertDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface XianyuAlertMapper extends BaseMapperX<XianyuAlertDO> {

    @Insert("""
            INSERT INTO xianyu_alert (
                tenant_id, shop_id, alert_type, dedupe_key, severity, status,
                source_identifier, message, first_seen_at, last_seen_at, resolved_at,
                creator, create_time, updater, update_time, deleted
            ) VALUES (
                #{tenantId}, #{alert.shopId}, #{alert.alertType}, #{alert.dedupeKey},
                #{alert.severity}, #{alert.status}, #{alert.sourceIdentifier}, #{alert.message},
                #{alert.firstSeenAt}, #{alert.lastSeenAt}, #{alert.resolvedAt},
                #{alert.creator}, NOW(), #{alert.updater}, NOW(), b'0'
            )
            ON DUPLICATE KEY UPDATE
                shop_id = VALUES(shop_id),
                severity = VALUES(severity),
                status = 'OPEN',
                source_identifier = VALUES(source_identifier),
                message = VALUES(message),
                last_seen_at = VALUES(last_seen_at),
                resolved_at = NULL,
                updater = VALUES(updater),
                update_time = NOW(),
                deleted = b'0'
            """)
    void insertOrRefresh(@Param("tenantId") Long tenantId, @Param("alert") XianyuAlertDO alert);

    default XianyuAlertDO selectByTenantIdAndDedupeKey(Long tenantId, String dedupeKey) {
        return selectOne(new LambdaQueryWrapper<XianyuAlertDO>()
                .eq(XianyuAlertDO::getTenantId, tenantId)
                .eq(XianyuAlertDO::getDedupeKey, dedupeKey));
    }

    default XianyuAlertDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<XianyuAlertDO>()
                .eq(XianyuAlertDO::getTenantId, tenantId)
                .eq(XianyuAlertDO::getId, id));
    }

}
