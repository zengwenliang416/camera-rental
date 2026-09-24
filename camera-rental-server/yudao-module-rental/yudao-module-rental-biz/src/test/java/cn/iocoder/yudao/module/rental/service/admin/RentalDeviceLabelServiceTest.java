package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.rental.config.RentalDeviceProperties;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceLabelReqVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import cn.iocoder.yudao.module.rental.service.device.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadataNode;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RentalDeviceLabelServiceTest {
    @Test void packageMatchesSkillDimensionsDpiGridAndQrPayloads() throws Exception {
        var mapper = mock(RentalDeviceMapper.class);
        var catalog = mock(RentalDeviceCatalogService.class);
        var props = new RentalDeviceProperties(); props.setQrSecret("unit-test-only-not-a-production-secret");
        var codec = new RentalDeviceQrCodec(props);
        var service = new RentalDeviceLabelService(mapper,catalog,codec,props);
        var first = RentalDeviceDO.builder().id(1L).deviceNo("A5-01").serialNumber("DEMO0001").equipmentModelCode("A5").enabled(true).build();
        var second = RentalDeviceDO.builder().id(2L).deviceNo("A5-02").equipmentModelCode("A5").enabled(true).build();
        when(mapper.selectBatchIds(anyCollection())).thenReturn(new ArrayList<>(List.of(first,second)));
        when(catalog.findEnabledModel("A5")).thenReturn(Optional.of(new RentalDeviceCatalogService.CatalogModel(1L,"DJI","DJI",2L,"A5","A5","A5")));
        var request = new RentalDeviceLabelReqVO(); request.setDeviceIds(List.of(1L,2L)); request.setShopName("Camera Rental"); request.setPhone("000-0000"); request.setLocale("zh-CN");
        byte[] zip = service.download(request);
        Map<String,byte[]> entries = new HashMap<>();
        try (var input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry e; (e = input.getNextEntry()) != null;) entries.put(e.getName(),input.readAllBytes());
        }
        assertEquals(8, entries.size());
        for (var device : List.of(first,second)) {
            String filename = "DJI_DJI/A5/PNG-40x20mm-600DPI/" + RentalDeviceLabelService.labelName(device) + ".png";
            byte[] bytes = entries.get(filename); assertNotNull(bytes);
            var image = ImageIO.read(new ByteArrayInputStream(bytes)); assertEquals(945,image.getWidth()); assertEquals(472,image.getHeight());
            var decoded = new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))));
            assertEquals(codec.encode(device.getDeviceNo(),"A5"),decoded.getText());
            var reader = ImageIO.getImageReadersByFormatName("png").next();
            try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                reader.setInput(stream);
                var root = (IIOMetadataNode) reader.getImageMetadata(0).getAsTree("javax_imageio_png_1.0");
                var density = (IIOMetadataNode) root.getElementsByTagName("pHYs").item(0);
                assertEquals("23622",density.getAttribute("pixelsPerUnitXAxis"));
                assertEquals("meter",density.getAttribute("unitSpecifier"));
            } finally { reader.dispose(); }
        }
        try (var pdf = Loader.loadPDF(entries.get("全部设备预览.pdf"))) {
            assertEquals(1,pdf.getNumberOfPages());
            assertEquals(595.2,pdf.getPage(0).getMediaBox().getWidth(),0.01);
            var page = new PDFRenderer(pdf).renderImageWithDPI(0,300);
            assertEquals(2480,page.getWidth(),1); assertEquals(3508,page.getHeight(),1);
            var label = page.getSubimage(118,31,472,236);
            assertNotNull(new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(label)))));
        }
        assertTrue(new String(entries.get("verification.json")).contains("\"physical_scan_tested\":false"));
        assertEquals("A5-02", RentalDeviceLabelService.labelName(second));
        second.setSerialNumber("12"); assertThrows(IllegalArgumentException.class, () -> RentalDeviceLabelService.labelName(second));
        // Optional local visual evidence, synthetic data only; not part of the repository.
        String output = System.getProperty("device.labels.testOutput");
        if (output != null) { Files.createDirectories(Path.of(output)); Files.write(Path.of(output,"sample.zip"),zip);
            Files.write(Path.of(output,"sample.png"),entries.get("DJI_DJI/A5/PNG-40x20mm-600DPI/A5-01-0001.png")); }
    }
    @Test void fiftyThreeLabelsCreateTwoA4PagesAndChineseTextUsesInstalledFont() throws Exception {
        var mapper = mock(RentalDeviceMapper.class);
        var catalog = mock(RentalDeviceCatalogService.class);
        var props = new RentalDeviceProperties(); props.setQrSecret("test-only-pagination");
        var codec = new RentalDeviceQrCodec(props);
        var service = new RentalDeviceLabelService(mapper,catalog,codec,props);
        var rows = new ArrayList<RentalDeviceDO>();
        for (int i=1;i<=53;i++) rows.add(RentalDeviceDO.builder().id((long)i)
                .deviceNo("A5-" + String.format("%02d",i)).equipmentModelCode("A5").enabled(true).build());
        when(mapper.selectBatchIds(anyCollection())).thenReturn(rows);
        when(catalog.findEnabledModel("A5")).thenReturn(Optional.of(new RentalDeviceCatalogService.CatalogModel(1L,"DJI","DJI",2L,"A5","A5","A5")));
        var req = new RentalDeviceLabelReqVO(); req.setDeviceIds(rows.stream().map(RentalDeviceDO::getId).toList());
        req.setShopName("Camera Rental"); req.setPhone("000-0000"); req.setLocale("en");
        for (var device : rows) assertDoesNotThrow(() -> service.render(device,req), device.getDeviceNo());
        byte[] archive = service.download(req);
        try (var input = new ZipInputStream(new ByteArrayInputStream(archive))) {
            int pngCount=0, pdfCount=0;
            for (ZipEntry entry; (entry=input.getNextEntry()) != null;) {
                byte[] data=input.readAllBytes();
                if(entry.getName().contains("PNG-40x20mm-600DPI")) pngCount++;
                if(entry.getName().endsWith(".pdf")) { pdfCount++; try(var document=Loader.loadPDF(data)) {
                    assertEquals(2,document.getNumberOfPages());
                    var last = new PDFRenderer(document).renderImageWithDPI(1,300).getSubimage(118,31,472,236);
                    String decoded=new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(last.getSubimage(260,12,212,212)))), Map.of(DecodeHintType.PURE_BARCODE,true)).getText();
                    assertEquals(codec.encode("A5-53","A5"),decoded);
                } }
            }
            assertEquals(53,pngCount); assertEquals(2,pdfCount);
        }
        // Font availability is a runtime prerequisite: fail closed when Chinese is unsupported.
        req.setShopName("长沙捷租达");
        if (new java.awt.Font(props.getLabelFontFamily(),java.awt.Font.BOLD,72).canDisplayUpTo(req.getShopName()) == -1) {
            var chinese = service.render(rows.get(0),req);
            String output=System.getProperty("device.labels.testOutput");
            if(output!=null) { Files.createDirectories(Path.of(output)); Files.write(Path.of(output,"sample-zh.png"),RentalDeviceLabelService.png(chinese)); }
        } else assertThrows(IllegalStateException.class, () -> service.render(rows.get(0),req));
    }
    @Test void refusesUnsignedLabelsAndUnauthorizedMissingIds() {
        var mapper = mock(RentalDeviceMapper.class); var props = new RentalDeviceProperties();
        var service = new RentalDeviceLabelService(mapper,mock(RentalDeviceCatalogService.class),new RentalDeviceQrCodec(props),props);
        var req = new RentalDeviceLabelReqVO(); req.setDeviceIds(List.of(1L));
        assertThrows(RuntimeException.class, () -> service.preview(req)); verifyNoInteractions(mapper);
        props.setQrSecret("test-only"); when(mapper.selectBatchIds(anyCollection())).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> service.preview(req));
    }
}
