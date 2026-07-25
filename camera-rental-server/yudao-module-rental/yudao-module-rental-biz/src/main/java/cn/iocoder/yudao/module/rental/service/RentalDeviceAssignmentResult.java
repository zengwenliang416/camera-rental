package cn.iocoder.yudao.module.rental.service;

import java.time.LocalDate;

/**
 * Accepted assignment returned by a local transactional operation.
 */
public record RentalDeviceAssignmentResult(Long assignmentId, Long scheduleId, Long deviceId,
                                           LocalDate occupyStartDate, LocalDate occupyEndDateExclusive) {
}
