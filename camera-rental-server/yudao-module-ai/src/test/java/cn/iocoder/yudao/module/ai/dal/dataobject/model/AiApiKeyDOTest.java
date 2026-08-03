package cn.iocoder.yudao.module.ai.dal.dataobject.model;

import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiApiKeyDOTest {

    @Test
    void apiKeyShouldUseEncryptedResultMapping() throws NoSuchFieldException {
        TableName tableName = AiApiKeyDO.class.getAnnotation(TableName.class);
        assertTrue(tableName.autoResultMap());

        Field apiKey = AiApiKeyDO.class.getDeclaredField("apiKey");
        TableField tableField = apiKey.getAnnotation(TableField.class);
        assertEquals(EncryptTypeHandler.class, tableField.typeHandler());
    }

}
