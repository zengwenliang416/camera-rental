package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCode;
import org.springframework.stereotype.Service;

@Service
public class ReturnSerialNormalizer {

    public String normalize(String value) {
        return RentalDeviceCode.normalize(value);
    }

    public boolean isValid(String value) {
        return RentalDeviceCode.isValid(value);
    }
}
