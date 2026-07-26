package cn.iocoder.yudao.module.rental.service.admin;

import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceGenerateFromPurchaseReqVO;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalDeviceGenerateFromPurchaseRespVO;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalDeviceDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalDeviceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.RENTAL_DEVICE_INBOUND_FAILED;

/**
 * Bridge: ERP approved purchase-in (qty) → rental device instances (one row per unit).
 */
@Service
public class RentalDeviceInboundService {

    public static final String SOURCE_ERP_PURCHASE_IN = "ERP_PURCHASE_IN";
    private static final int MAX_UNITS_PER_ITEM = 200;
    private static final Pattern TRAILING_SEQ = Pattern.compile("(\\d+)$");

    private final ErpPurchaseInService purchaseInService;
    private final ErpProductService productService;
    private final ErpWarehouseService warehouseService;
    private final RentalDeviceMapper deviceMapper;

    public RentalDeviceInboundService(ErpPurchaseInService purchaseInService,
                                      ErpProductService productService,
                                      ErpWarehouseService warehouseService,
                                      RentalDeviceMapper deviceMapper) {
        this.purchaseInService = purchaseInService;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.deviceMapper = deviceMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public RentalDeviceGenerateFromPurchaseRespVO generateFromPurchaseIn(
            RentalDeviceGenerateFromPurchaseReqVO reqVO) {
        ErpPurchaseInDO purchaseIn = purchaseInService.getPurchaseIn(reqVO.getPurchaseInId());
        if (purchaseIn == null) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "采购入库单不存在");
        }
        if (!ErpAuditStatus.APPROVE.getStatus().equals(purchaseIn.getStatus())) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "仅已审批的入库单可生成设备");
        }

        List<ErpPurchaseInItemDO> items = purchaseInService.getPurchaseInItemListByInId(purchaseIn.getId());
        if (items == null || items.isEmpty()) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "入库单无明细");
        }
        if (reqVO.getPurchaseInItemId() != null) {
            items = items.stream()
                    .filter(i -> reqVO.getPurchaseInItemId().equals(i.getId()))
                    .toList();
            if (items.isEmpty()) {
                throw exception(RENTAL_DEVICE_INBOUND_FAILED, "指定入库明细不存在");
            }
        }

        RentalDeviceGenerateFromPurchaseRespVO resp = new RentalDeviceGenerateFromPurchaseRespVO();
        resp.setPurchaseInId(purchaseIn.getId());
        resp.setPurchaseInNo(purchaseIn.getNo());

        for (ErpPurchaseInItemDO item : items) {
            generateForItem(purchaseIn, item, reqVO, resp);
        }
        return resp;
    }

    private void generateForItem(ErpPurchaseInDO purchaseIn, ErpPurchaseInItemDO item,
                                 RentalDeviceGenerateFromPurchaseReqVO reqVO,
                                 RentalDeviceGenerateFromPurchaseRespVO resp) {
        int need = toUnitCount(item.getCount());
        resp.setRequestedCount(resp.getRequestedCount() + need);

        long existed = deviceMapper.countBySourceItem(
                SOURCE_ERP_PURCHASE_IN, purchaseIn.getId(), item.getId());
        resp.setAlreadyExistedCount(resp.getAlreadyExistedCount() + (int) Math.min(existed, need));
        int toCreate = need - (int) existed;
        if (toCreate <= 0) {
            return;
        }

        ErpProductDO product = productService.getProduct(item.getProductId());
        if (product == null) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "产品不存在 productId=" + item.getProductId());
        }

        String modelCode = firstNonBlank(reqVO.getEquipmentModelCode(), product.getBarCode(), product.getName());
        if (!StringUtils.hasText(modelCode)) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "无法推断设备型号，请填写 equipmentModelCode 或产品条码");
        }
        modelCode = sanitizeToken(modelCode);

        String warehouseCode = firstNonBlank(reqVO.getWarehouseCode(), resolveWarehouseName(item.getWarehouseId()));
        String prefix = firstNonBlank(reqVO.getDeviceNoPrefix(), product.getBarCode(), modelCode);
        prefix = sanitizeToken(prefix);
        if (!StringUtils.hasText(prefix)) {
            prefix = "DEV";
        }

        int nextSeq = nextSequence(prefix);
        Integer purchaseAmountFen = toFen(item.getProductPrice());

        for (int i = 0; i < toCreate; i++) {
            String deviceNo = prefix + "-" + String.format(Locale.ROOT, "%04d", nextSeq++);
            RentalDeviceDO device = RentalDeviceDO.builder()
                    .deviceNo(deviceNo)
                    .equipmentModelCode(modelCode)
                    .status("AVAILABLE")
                    .warehouseCode(warehouseCode)
                    .purchaseAmount(purchaseAmountFen)
                    .enabled(true)
                    .sourceType(SOURCE_ERP_PURCHASE_IN)
                    .sourceBizId(purchaseIn.getId())
                    .sourceItemId(item.getId())
                    .build();
            try {
                deviceMapper.insert(device);
            } catch (Exception ex) {
                // device_no unique conflict: bump seq once more
                deviceNo = prefix + "-" + String.format(Locale.ROOT, "%04d", nextSeq++);
                device.setDeviceNo(deviceNo);
                deviceMapper.insert(device);
            }
            resp.setCreatedCount(resp.getCreatedCount() + 1);
            resp.getCreatedDeviceIds().add(device.getId());
            resp.getCreatedDeviceNos().add(device.getDeviceNo());
        }
    }

    private int nextSequence(String prefix) {
        String fullPrefix = prefix + "-";
        RentalDeviceDO latest = deviceMapper.selectLatestByDeviceNoPrefix(fullPrefix);
        if (latest == null || !StringUtils.hasText(latest.getDeviceNo())) {
            return 1;
        }
        Matcher m = TRAILING_SEQ.matcher(latest.getDeviceNo());
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1)) + 1;
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        return 1;
    }

    private String resolveWarehouseName(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        ErpWarehouseDO wh = warehouseService.getWarehouse(warehouseId);
        return wh != null ? wh.getName() : null;
    }

    private static int toUnitCount(BigDecimal count) {
        if (count == null || count.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "入库数量无效");
        }
        // Rental instances are whole units only.
        int units = count.setScale(0, RoundingMode.DOWN).intValueExact();
        if (units <= 0) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "入库数量必须至少 1");
        }
        if (units > MAX_UNITS_PER_ITEM) {
            throw exception(RENTAL_DEVICE_INBOUND_FAILED, "单明细生成上限 " + MAX_UNITS_PER_ITEM + " 台");
        }
        return units;
    }

    private static Integer toFen(BigDecimal yuan) {
        if (yuan == null) {
            return null;
        }
        return yuan.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (StringUtils.hasText(v)) {
                return v.trim();
            }
        }
        return null;
    }

    private static String sanitizeToken(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        // deviceNo / model must not contain '|' (QR) or spaces that break numbering
        return raw.trim().replace('|', '-').replaceAll("\\s+", "-");
    }

}
