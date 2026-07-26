package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.controller.admin.xianyu.vo.XianyuShipmentOcrRespVO;
import cn.iocoder.yudao.module.rental.integration.ocr.ShipmentOcrClient;
import cn.iocoder.yudao.module.rental.integration.ocr.ShipmentOcrResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort shipment code extraction. The production adapter can be replaced
 * by the configured multimodal OCR client without changing the controller.
 */
@Service
public class ShipmentOcrService {

    private static final Pattern WAYBILL_PATTERN = Pattern.compile("\\b(?:SF|JD|JT|YT|STO|YTO|ZTO|EMS)?\\d[A-Z0-9]{9,}\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Map<DecodeHintType, Object> BARCODE_HINTS = Map.of(
            DecodeHintType.TRY_HARDER, Boolean.TRUE,
            DecodeHintType.POSSIBLE_FORMATS, List.of(
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39,
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.EAN_13,
                    BarcodeFormat.ITF));

    private final Optional<ShipmentOcrClient> shipmentOcrClient;

    public ShipmentOcrService(Optional<ShipmentOcrClient> shipmentOcrClient) {
        this.shipmentOcrClient = shipmentOcrClient;
    }

    public XianyuShipmentOcrRespVO extract(MultipartFile file) {
        XianyuShipmentOcrRespVO resp = new XianyuShipmentOcrRespVO();
        ShipmentOcrResult aiResult = shipmentOcrClient
                .map(client -> client.extract(file))
                .orElseGet(() -> ShipmentOcrResult.empty("AI_MULTIMODAL_DISABLED"));
        String aiWaybillNo = firstWaybill(aiResult.getWaybillNo());
        if (StringUtils.hasText(aiWaybillNo)) {
            resp.setWaybillNo(aiWaybillNo);
            fillExpress(resp, aiWaybillNo, aiResult.getExpressName());
            resp.setConfidence(nonNullConfidence(aiResult.getConfidence(), new BigDecimal("0.80")));
            resp.setExtractionSource(aiResult.getSource());
            return resp;
        }

        String barcode = decodeBarcode(file);
        String text = StringUtils.hasText(barcode) ? barcode : readableProbeText(file);
        String waybillNo = firstWaybill(text);
        if (waybillNo != null) {
            resp.setWaybillNo(waybillNo);
            fillExpress(resp, waybillNo, null);
            resp.setConfidence(StringUtils.hasText(barcode) ? new BigDecimal("0.95") : new BigDecimal("0.50"));
            resp.setExtractionSource(StringUtils.hasText(barcode) ? "BARCODE" : "VISIBLE_TEXT_FALLBACK");
        } else {
            resp.setConfidence(BigDecimal.ZERO);
            resp.setExtractionSource("NO_MATCH");
        }
        return resp;
    }

    private static void fillExpress(XianyuShipmentOcrRespVO resp, String waybillNo, String expressName) {
        String normalizedName = StringUtils.hasText(expressName) ? expressName.trim() : "";
        String upperWaybill = waybillNo.toUpperCase(Locale.ROOT);
        if (upperWaybill.startsWith("SF") || normalizedName.contains("顺丰")) {
            resp.setExpressCode("shunfeng");
            resp.setExpressName("顺丰速运");
        } else if (normalizedName.contains("京东")) {
            resp.setExpressCode("jd");
            resp.setExpressName("京东快递");
        } else if (normalizedName.contains("极兔")) {
            resp.setExpressCode("jtexpress");
            resp.setExpressName("极兔速递");
        } else if (normalizedName.contains("圆通")) {
            resp.setExpressCode("yuantong");
            resp.setExpressName("圆通速递");
        } else if (normalizedName.contains("中通")) {
            resp.setExpressCode("zhongtong");
            resp.setExpressName("中通快递");
        } else if (normalizedName.contains("韵达")) {
            resp.setExpressCode("yunda");
            resp.setExpressName("韵达快递");
        } else if (normalizedName.contains("申通")) {
            resp.setExpressCode("shentong");
            resp.setExpressName("申通快递");
        } else if (StringUtils.hasText(normalizedName)) {
            resp.setExpressName(normalizedName);
        }
    }

    private static BigDecimal nonNullConfidence(BigDecimal confidence, BigDecimal defaultValue) {
        return confidence != null && confidence.compareTo(BigDecimal.ZERO) > 0 ? confidence : defaultValue;
    }

    private static String decodeBarcode(MultipartFile file) {
        if (file == null) {
            return "";
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return "";
            }
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
            Result result = new MultiFormatReader().decode(bitmap, BARCODE_HINTS);
            return result.getText();
        } catch (IOException | NotFoundException ignored) {
            return "";
        }
    }

    private static String readableProbeText(MultipartFile file) {
        if (file == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try {
            byte[] bytes = file.getBytes();
            int length = Math.min(bytes.length, 4096);
            builder.append(new String(bytes, 0, length, StandardCharsets.ISO_8859_1));
        } catch (Exception ignored) {
            // OCR is best-effort; manual entry remains available.
        }
        return builder.toString();
    }

    private static String firstWaybill(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = WAYBILL_PATTERN.matcher(text.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group() : null;
    }

}
