package cn.iocoder.yudao.module.rental.integration.ocr;

import org.springframework.web.multipart.MultipartFile;

public interface ShipmentOcrClient {

    ShipmentOcrResult extract(MultipartFile file);

}
