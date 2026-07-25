package cn.iocoder.yudao.module.rental.controller.admin.rental.vo;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RentalDeviceAssignReqVOTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptHalfOpenOccupiedPeriod() {
        RentalDeviceAssignReqVO reqVO = validReqVO();

        assertTrue(validator.validate(reqVO).isEmpty());
    }

    @Test
    void shouldRejectEndDateThatIsNotAfterStartDate() {
        RentalDeviceAssignReqVO reqVO = validReqVO();
        reqVO.setOccupyEndDateExclusive(LocalDate.of(2026, 7, 22));

        Set<String> properties = validateProperties(reqVO);

        assertEquals(Set.of("occupyPeriodValid"), properties);
    }

    @Test
    void shouldRejectTooLongIdempotencyKeyBeforeServiceCall() {
        RentalDeviceAssignReqVO reqVO = validReqVO();
        reqVO.setIdempotencyKey("x".repeat(129));

        Set<String> properties = validateProperties(reqVO);

        assertEquals(Set.of("idempotencyKey"), properties);
    }

    private Set<String> validateProperties(RentalDeviceAssignReqVO reqVO) {
        return validator.validate(reqVO).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private RentalDeviceAssignReqVO validReqVO() {
        RentalDeviceAssignReqVO reqVO = new RentalDeviceAssignReqVO();
        reqVO.setRentalOrderItemId(21L);
        reqVO.setDeviceId(31L);
        reqVO.setOccupyStartDate(LocalDate.of(2026, 7, 22));
        reqVO.setOccupyEndDateExclusive(LocalDate.of(2026, 7, 23));
        reqVO.setIdempotencyKey("assign-1");
        return reqVO;
    }

}
