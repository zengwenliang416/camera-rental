package cn.iocoder.yudao.module.rental.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Device QR runtime config. Secret only via env / config center — never commit real values.
 */
@ConfigurationProperties(prefix = "rental.device")
@Data
public class RentalDeviceProperties {

    /**
     * HMAC secret for permanent device QR payload signature (signs deviceNo + equipmentModelCode).
     */
    private String qrSecret = "";

    public boolean isQrSigned() {
        return StringUtils.hasText(qrSecret);
    }

}
