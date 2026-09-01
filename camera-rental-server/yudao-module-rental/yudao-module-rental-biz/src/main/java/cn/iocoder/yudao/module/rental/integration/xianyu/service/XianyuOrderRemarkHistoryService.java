package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuOrderRemarkHistoryDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuOrderRemarkHistoryMapper;
import cn.iocoder.yudao.module.rental.service.SellerRemarkRentalPeriod;
import cn.iocoder.yudao.module.rental.service.reconciliation.RentalRemarkPlanChangeType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class XianyuOrderRemarkHistoryService {

    private final XianyuOrderRemarkHistoryMapper historyMapper;

    public XianyuOrderRemarkHistoryService(XianyuOrderRemarkHistoryMapper historyMapper) {
        this.historyMapper = historyMapper;
    }

    public XianyuOrderRemarkHistoryDO record(Long xianyuOrderId, Long rawPayloadId,
                                             String sellerRemark, LocalDateTime sourceUpdatedAt,
                                             SellerRemarkRentalPeriod parsedPeriod,
                                             boolean effectivePlan,
                                             RentalRemarkPlanChangeType changeType) {
        Objects.requireNonNull(xianyuOrderId, "xianyuOrderId");
        Objects.requireNonNull(parsedPeriod, "parsedPeriod");
        Objects.requireNonNull(changeType, "changeType");
        boolean acceptedAsEffective = effectivePlan && parsedPeriod.isSuccess()
                && changeType != RentalRemarkPlanChangeType.INVALID
                && changeType != RentalRemarkPlanChangeType.AMBIGUOUS;
        XianyuOrderRemarkHistoryDO history = XianyuOrderRemarkHistoryDO.builder()
                .xianyuOrderId(xianyuOrderId)
                .rawPayloadId(rawPayloadId)
                .sellerRemark(sellerRemark)
                .parseVersion(parsedPeriod.version())
                .parseStatus(parsedPeriod.status())
                .parseReasonCode(parsedPeriod.reasonCode())
                .shipDate(parsedPeriod.shipDate())
                .receiveDate(parsedPeriod.receiveDate())
                .billableStartDate(parsedPeriod.billableStartDate())
                .billableEndDate(parsedPeriod.billableEndDate())
                .sendBackDate(parsedPeriod.returnDate())
                .effectivePlan(acceptedAsEffective)
                .changeType(changeType.name())
                .sourceUpdatedAt(sourceUpdatedAt)
                .build();
        history.setCreator("system");
        history.setUpdater("system");
        historyMapper.insert(history);
        return history;
    }

    public void markEffective(XianyuOrderRemarkHistoryDO history) {
        if (history == null || !"SUCCESS".equals(history.getParseStatus())
                || RentalRemarkPlanChangeType.INVALID.name().equals(history.getChangeType())
                || RentalRemarkPlanChangeType.AMBIGUOUS.name().equals(history.getChangeType())) {
            return;
        }
        history.setEffectivePlan(true);
        history.setUpdater("system");
        historyMapper.updateById(history);
    }

}
