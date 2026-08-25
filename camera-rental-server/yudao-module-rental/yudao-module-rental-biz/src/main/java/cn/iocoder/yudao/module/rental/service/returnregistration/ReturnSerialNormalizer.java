package cn.iocoder.yudao.module.rental.service.returnregistration;

import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCode;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class ReturnSerialNormalizer {

    private static final Pattern RETURN_CODE =
            Pattern.compile("^(?=.{4,64}$)(?:[A-Z0-9]+(?:-[A-Z0-9]+)*|支架)-\\d{2}$");

    public String normalize(String value) {
        return RentalDeviceCode.normalize(value);
    }

    public boolean isValid(String value) {
        return RETURN_CODE.matcher(normalize(value)).matches();
    }
}
