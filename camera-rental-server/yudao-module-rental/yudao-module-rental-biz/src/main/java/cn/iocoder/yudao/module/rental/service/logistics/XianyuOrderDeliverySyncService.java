package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderDO;
import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryDirectionEnum;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class XianyuOrderDeliverySyncService {

    private static final Set<String> SHIPPED_STATUSES = Set.of("21", "SHIPPED", "CONSIGNED");
    private static final Pattern RENTAL_TITLE =
            Pattern.compile("租赁|出租|免押|租机|租借");

    private final RentalDeliveryService deliveryService;

    public XianyuOrderDeliverySyncService(RentalDeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    public RentalDeliveryResult syncOutboundIfTrackable(XianyuOrderDO order) {
        if (!isTrackable(order)) {
            return null;
        }
        return deliveryService.createOrReuse(new RentalDeliveryCreateCommand(
                order.getRentalOrderId(),
                order.getId(),
                RentalDeliveryDirectionEnum.OUTBOUND,
                "XIANYU",
                "xianyu-order:" + order.getId(),
                order.getExpressCode(),
                order.getExpressName(),
                order.getWaybillNo(),
                order.getReceiverMobile(),
                List.of()));
    }

    boolean isTrackable(XianyuOrderDO order) {
        if (order == null || order.getId() == null || !Integer.valueOf(1).equals(order.getConsignType())) {
            return false;
        }
        String orderStatus = normalize(order.getOrderStatus());
        if (!SHIPPED_STATUSES.contains(orderStatus)) {
            return false;
        }
        if (!StringUtils.hasText(order.getWaybillNo()) || "0000".equals(order.getWaybillNo().trim())) {
            return false;
        }
        if (!StringUtils.hasText(order.getExpressCode())
                || "GENERAL".equals(normalize(order.getExpressCode()))) {
            return false;
        }
        return order.getRentalOrderId() != null
                || "SUCCESS".equals(normalize(order.getRentalPeriodStatus()))
                || StringUtils.hasText(order.getSellerRemark())
                || RENTAL_TITLE.matcher(String.valueOf(order.getGoodsTitle())).find();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
