package cn.iocoder.yudao.module.rental.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SellerRemarkRentalPeriodResolver {

    public static final String VERSION = "SELLER_REMARK_V7_AI";

    private final SellerRemarkRentalPeriodParser ruleParser;
    private final SellerRemarkAiFallbackService aiFallbackService;

    public SellerRemarkRentalPeriodResolver(SellerRemarkRentalPeriodParser ruleParser,
                                            SellerRemarkAiFallbackService aiFallbackService) {
        this.ruleParser = ruleParser;
        this.aiFallbackService = aiFallbackService;
    }

    public SellerRemarkResolution resolve(String sellerRemark, LocalDate referenceDate) {
        SellerRemarkRentalPeriod ruleResult = withCurrentVersion(
                ruleParser.parse(sellerRemark, referenceDate));
        if (ruleResult.isSuccess() || sellerRemark == null || sellerRemark.isBlank()
                || referenceDate == null) {
            return SellerRemarkResolution.rule(ruleResult);
        }
        return aiFallbackService.resolve(sellerRemark, referenceDate)
                .orElseGet(() -> SellerRemarkResolution.rule(ruleResult));
    }

    private SellerRemarkRentalPeriod withCurrentVersion(SellerRemarkRentalPeriod value) {
        return new SellerRemarkRentalPeriod(VERSION, value.status(),
                value.billableStartDate(), value.billableEndDate(),
                value.shipDate(), value.receiveDate(), value.returnDate(),
                value.reasonCode());
    }

}
