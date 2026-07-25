package cn.iocoder.yudao.module.rental.enums.rental;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RentalManualReviewStatusEnum {

    OPEN("OPEN"),
    RESOLVED("RESOLVED"),
    CLOSED("CLOSED");

    private final String status;

}
