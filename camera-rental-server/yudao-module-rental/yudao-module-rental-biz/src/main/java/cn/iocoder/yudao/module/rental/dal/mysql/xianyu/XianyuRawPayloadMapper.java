package cn.iocoder.yudao.module.rental.dal.mysql.xianyu;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuRawPayloadDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for restricted channel payloads.
 */
@Mapper
public interface XianyuRawPayloadMapper extends BaseMapperX<XianyuRawPayloadDO> {

    @Insert("""
            INSERT INTO xianyu_raw_payload (
                tenant_id, source_type, source_identifier, payload_hash, schema_version,
                redaction_version, payload, received_at, creator, create_time,
                updater, update_time, deleted
            ) VALUES (
                #{tenantId}, #{payload.sourceType}, #{payload.sourceIdentifier}, #{payload.payloadHash},
                #{payload.schemaVersion}, #{payload.redactionVersion}, #{payload.payload},
                #{payload.receivedAt}, #{payload.creator}, NOW(), #{payload.updater}, NOW(), b'0'
            )
            ON DUPLICATE KEY UPDATE id = id
            """)
    void insertOrReuse(@Param("tenantId") Long tenantId, @Param("payload") XianyuRawPayloadDO payload);

    default XianyuRawPayloadDO selectByTenantIdAndId(Long tenantId, Long id) {
        return selectOne(new LambdaQueryWrapper<XianyuRawPayloadDO>()
                .eq(XianyuRawPayloadDO::getTenantId, tenantId)
                .eq(XianyuRawPayloadDO::getId, id));
    }

    default XianyuRawPayloadDO selectByTenantIdAndSourceAndHashForUpdate(Long tenantId, String sourceType,
                                                                          String sourceIdentifier,
                                                                          String payloadHash) {
        return selectOneForUpdate(new LambdaQueryWrapper<XianyuRawPayloadDO>()
                .eq(XianyuRawPayloadDO::getTenantId, tenantId)
                .eq(XianyuRawPayloadDO::getSourceType, sourceType)
                .eq(XianyuRawPayloadDO::getSourceIdentifier, sourceIdentifier)
                .eq(XianyuRawPayloadDO::getPayloadHash, payloadHash));
    }

}
