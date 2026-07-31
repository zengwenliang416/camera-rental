package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.framework.mybatis.core.type.EncryptTypeHandler;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryCallbackInboxDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalDeliveryDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderConfigDO;
import cn.iocoder.yudao.module.rental.dal.dataobject.logistics.RentalLogisticsProviderCredentialDO;
import com.baomidou.mybatisplus.annotation.TableField;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RentalLogisticsSensitiveFieldTest {

    @Test
    void sensitivePersistenceFieldsUseEncryptionAndAreExcludedFromToString() throws Exception {
        assertEncrypted(RentalDeliveryDO.class, "trackingPhone");
        assertEncrypted(RentalDeliveryDO.class, "callbackToken");
        assertEncrypted(RentalDeliveryDO.class, "callbackSalt");
        assertEncrypted(RentalDeliveryCallbackInboxDO.class, "callbackParams");
        assertEncrypted(RentalLogisticsProviderCredentialDO.class, "customerCode");
        assertEncrypted(RentalLogisticsProviderCredentialDO.class, "apiKey");
        assertEncrypted(RentalLogisticsProviderConfigDO.class, "callbackSecret");

        String secret = "sensitive-test-value";
        assertFalse(RentalDeliveryDO.builder().trackingPhone(secret).callbackToken(secret).callbackSalt(secret)
                .build().toString().contains(secret));
        assertFalse(RentalDeliveryCallbackInboxDO.builder().callbackParams(secret).build()
                .toString().contains(secret));
        assertFalse(RentalLogisticsProviderCredentialDO.builder().customerCode(secret).apiKey(secret)
                .build().toString().contains(secret));
        assertFalse(RentalLogisticsProviderConfigDO.builder().callbackSecret(secret)
                .build().toString().contains(secret));
    }

    private void assertEncrypted(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        TableField tableField = field.getAnnotation(TableField.class);
        assertNotNull(tableField);
        assertEquals(EncryptTypeHandler.class, tableField.typeHandler());
    }
}
