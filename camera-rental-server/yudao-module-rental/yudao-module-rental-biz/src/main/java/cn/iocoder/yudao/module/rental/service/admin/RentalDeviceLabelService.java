package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLabelReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLabelPreviewRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceCatalogService;
import cn.iocoder.yudao.module.rental.service.device.RentalDeviceQrCodec;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.stereotype.Service;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;
import java.util.zip.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.*;

@Service
public class RentalDeviceLabelService {
    private final RentalDeviceMapper devices;
    private final RentalDeviceCatalogService catalog;
    private final RentalDeviceQrCodec codec;
    private final RentalDeviceProperties properties;
    public RentalDeviceLabelService(RentalDeviceMapper devices, RentalDeviceCatalogService catalog,
                                    RentalDeviceQrCodec codec, RentalDeviceProperties properties) {
        this.devices = devices; this.catalog = catalog; this.codec = codec; this.properties = properties;
    }

    public RentalDeviceLabelPreviewRespVO preview(RentalDeviceLabelReqVO request) {
        List<RentalDeviceDO> rows = resolve(request);
        try {
            var first = rows.get(0);
            byte[] png = png(render(first, request));
            return new RentalDeviceLabelPreviewRespVO(first.getDeviceNo(), "data:image/png;base64," + Base64.getEncoder().encodeToString(png));
        } catch (Exception e) { throw exception(RENTAL_DEVICE_LABEL_UNAVAILABLE); }
    }

    public byte[] download(RentalDeviceLabelReqVO request) {
        List<RentalDeviceDO> rows = resolve(request);
        boolean en = "en".equals(request.getLocale());
        Map<String, List<RentalDeviceDO>> groups = new LinkedHashMap<>();
        var models = new HashMap<String, RentalDeviceCatalogService.CatalogModel>();
        for (var device : rows) {
            var model = models.computeIfAbsent(device.getEquipmentModelCode(),
                    key -> catalog.findEnabledModel(key).orElseThrow(() -> exception(RENTAL_DEVICE_MODEL_NOT_EXISTS)));
            String group = safe(model.categoryName()) + "_" + safe(model.categoryCode()) + "/" + safe(model.modelCode());
            groups.computeIfAbsent(group, key -> new ArrayList<>()).add(device);
        }
        try (var bytes = new ByteArrayOutputStream(); var zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8);
             var all = new PDDocument()) {
            StringBuilder csv = new StringBuilder("\ufeffdevice_no,serial_number,model,png,sha256,decoded\r\n");
            int total = 0;
            for (var group : groups.entrySet()) {
                try (var document = new PDDocument()) {
                    int index = 0;
                    int columns = Math.min(4, group.getValue().size());
                    BufferedImage sheet = new BufferedImage(72 + columns * 472 + (columns - 1) * 24,
                            72 + ((group.getValue().size() + columns - 1) / columns) * 236
                                    + ((group.getValue().size() + columns - 1) / columns - 1) * 24, BufferedImage.TYPE_INT_RGB);
                    Graphics2D sheetGraphics = sheet.createGraphics();
                    sheetGraphics.setColor(new Color(0xE7E4DD));
                    sheetGraphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
                    for (var device : group.getValue()) {
                        BufferedImage label = render(device, request);
                        byte[] png = png(label);
                        String name = group.getKey() + "/" + "PNG-40x20mm-600DPI" + "/" + safe(labelName(device)) + ".png";
                        entry(zip, name, png);
                        sheetGraphics.drawImage(thumbnail(label), 36 + (index % columns) * 496,
                                36 + (index / columns) * 260, null);
                        addLabel(document, label, index++);
                        addLabel(all, label, total++);
                        csv.append(csv(device.getDeviceNo())).append(',').append(csv(device.getSerialNumber())).append(',')
                                .append(csv(device.getEquipmentModelCode())).append(',').append(csv(name)).append(',')
                                .append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png))).append(",true\r\n");
                    }
                    sheetGraphics.dispose();
                    entry(zip, group.getKey() + (en ? "/Preview-sheet.png" : "/设备二维码-预览总表.png"), png(sheet, 150));
                    entry(zip, group.getKey() + (en ? "/A4-print-40x20mm.pdf" : "/设备二维码-A4打印版-40x20mm.pdf"), pdf(document));
                }
            }
            entry(zip, en ? "All-labels.pdf" : "全部设备预览.pdf", pdf(all));
            entry(zip, en ? "Devices.csv" : "设备清单.csv", csv.toString().getBytes(StandardCharsets.UTF_8));
            String instructions = en
                    ? "40 x 20 mm labels, 945 x 472 px, 600 DPI. Print at 100% / actual size. Disable fit-to-page. Each QR was decoded and verified. Physical scanning has NOT been tested. Print a sample and scan with the staff app before use."
                    : "标签尺寸40×20mm，945×472像素，600DPI。按100%或实际尺寸打印，关闭适应页面及缩放。二维码已逐张软件解码核对。尚未完成实体打印扫码验收，请先打印样张，用员工端或PDA扫码后再批量使用。";
            entry(zip, en ? "Printing.txt" : "打印说明.txt", instructions.getBytes(StandardCharsets.UTF_8));
            entry(zip, "verification.json", ("{\"device_count\":" + total + ",\"decoded_count\":" + total
                    + ",\"label_pixels\":[945,472],\"dpi\":600,\"a4_pixels\":[2480,3508],\"a4_dpi\":300,\"a4_columns\":4,\"a4_rows\":13,\"all_labels_pdf_pages\":" + all.getNumberOfPages() + ",\"decoder\":\"ZXing Java QR region\",\"qr_region\":[527,31,410,410],\"physical_scan_tested\":false}").getBytes(StandardCharsets.UTF_8));
            zip.finish();
            return bytes.toByteArray();
        } catch (Exception e) { throw exception(RENTAL_DEVICE_LABEL_UNAVAILABLE); }
    }

    private List<RentalDeviceDO> resolve(RentalDeviceLabelReqVO request) {
        if (!properties.isQrSigned()) throw exception(RENTAL_DEVICE_LABEL_UNAVAILABLE);
        List<Long> ids = request.getDeviceIds().stream().distinct().toList();
        List<RentalDeviceDO> rows = new ArrayList<>(devices.selectBatchIds(ids));
        if (rows.size() != ids.size() || rows.stream().anyMatch(d -> !Boolean.TRUE.equals(d.getEnabled())))
            throw exception(RENTAL_DEVICE_IMPORT_CHANGED);
        rows.sort(Comparator.comparing(RentalDeviceDO::getEquipmentModelCode).thenComparing(RentalDeviceDO::getDeviceNo));
        return rows;
    }

    BufferedImage render(RentalDeviceDO device, RentalDeviceLabelReqVO request) throws Exception {
        String payload = codec.encode(device.getDeviceNo(), device.getEquipmentModelCode());
        if (!codec.decode(payload).signed()) throw new IllegalStateException("Unsigned QR");
        BufferedImage image = new BufferedImage(945, 472, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.WHITE); g.fillRect(0, 0, 945, 472);
            g.setColor(Color.BLACK);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            text(g, request.getShopName(), 72, 42, 28, 112);
            text(g, request.getPhone(), 68, 40, 166, 104);
            text(g, labelName(device), 70, 38, 304, 112);
            var hints = Map.<EncodeHintType, Object>of(EncodeHintType.MARGIN, 4,
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.CHARACTER_SET, "UTF-8");
            var matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 410, 410, hints);
            g.drawImage(MatrixToImageWriter.toBufferedImage(matrix), 527, 31, null);
        } finally { g.dispose(); }
        // Geometry is fixed by the print contract: verify that QR region directly, avoiding
        // text glyphs being mistaken for finder patterns by Java ZXing's whole-label detector.
        var decoded = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(image.getSubimage(527, 31, 410, 410)))), Map.of(DecodeHintType.PURE_BARCODE, true,
                        DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE)));
        if (!payload.equals(decoded.getText())) throw new IllegalStateException("QR round trip failed");
        return image;
    }
    private void text(Graphics2D g, String value, int preferred, int minimum, int top, int height) {
        Font font = new Font(properties.getLabelFontFamily(), Font.BOLD, preferred);
        if (font.canDisplayUpTo(value) != -1) throw new IllegalStateException("Font cannot render label");
        for (int size = preferred; size >= minimum; size -= 2) {
            font = font.deriveFont((float) size);
            var glyph = font.createGlyphVector(g.getFontRenderContext(), value);
            var bounds = glyph.getVisualBounds();
            if (bounds.getWidth() <= 470 && bounds.getHeight() <= height - 8) {
                float x = (float) ((520 - bounds.getWidth()) / 2 - bounds.getX());
                float y = (float) (top + (height - bounds.getHeight()) / 2 - bounds.getY());
                g.drawGlyphVector(glyph, x, y);
                return;
            }
        }
        throw new IllegalStateException("Label text cannot fit without truncation");
    }
    static String labelName(RentalDeviceDO device) {
        String serial = device.getSerialNumber() == null ? "" : device.getSerialNumber();
        if (serial.isEmpty()) return device.getDeviceNo();
        String normalized = serial.codePoints().filter(Character::isLetterOrDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        int count = normalized.codePointCount(0, normalized.length());
        if (count < 4) throw new IllegalArgumentException("Serial suffix requires four alphanumeric characters");
        return device.getDeviceNo() + "-" + normalized.substring(normalized.offsetByCodePoints(0, count - 4)).toUpperCase(Locale.ROOT);
    }
    static byte[] png(BufferedImage image) throws IOException { return png(image, 600); }
    private static byte[] png(BufferedImage image, int dpi) throws IOException {
        var writer = ImageIO.getImageWritersByFormatName("png").next();
        try (var bytes = new ByteArrayOutputStream(); var output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            var metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), writer.getDefaultWriteParam());
            var root = new IIOMetadataNode("javax_imageio_png_1.0");
            var density = new IIOMetadataNode("pHYs");
            density.setAttribute("pixelsPerUnitXAxis", Integer.toString((int) Math.round(dpi / 0.0254))); density.setAttribute("pixelsPerUnitYAxis", Integer.toString((int) Math.round(dpi / 0.0254)));
            density.setAttribute("unitSpecifier", "meter"); root.appendChild(density);
            metadata.mergeTree("javax_imageio_png_1.0", root);
            writer.write(null, new IIOImage(image, null, metadata), writer.getDefaultWriteParam());
            output.flush(); return bytes.toByteArray();
        } finally { writer.dispose(); }
    }
    private static BufferedImage thumbnail(BufferedImage image) {
        BufferedImage result = new BufferedImage(472, 236, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(image, 0, 0, 472, 236, null);
        } finally { graphics.dispose(); }
        return result;
    }
    private static void addLabel(PDDocument document, BufferedImage image, int index) throws IOException {
        // Match skill's 2480x3508 canvas at 300 DPI, 118px/31px integer gaps and 472x236px labels.
        float pointPerPixel = 72f / 300;
        if (index % 52 == 0) document.addPage(new PDPage(new PDRectangle(2480 * pointPerPixel, 3508 * pointPerPixel)));
        var page = document.getPage(document.getNumberOfPages() - 1);
        int slot = index % 52;
        try (var content = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            content.drawImage(LosslessFactory.createFromImage(document, thumbnail(image)),
                    (118 + (slot % 4) * 590) * pointPerPixel,
                    (3508 - (31 + (slot / 4) * 267) - 236) * pointPerPixel,
                    472 * pointPerPixel, 236 * pointPerPixel);
        }
    }
    private static byte[] pdf(PDDocument document) throws IOException {
        var output = new ByteArrayOutputStream(); document.save(output); return output.toByteArray();
    }
    private static void entry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name)); zip.write(bytes); zip.closeEntry();
    }
    private static String safe(String value) { return value.replaceAll("[^\\p{L}\\p{N}_-]", "_"); }
    private static String csv(String value) {
        String text = value == null ? "" : value;
        if (text.matches("^[=+@\\-\\t\\r].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
