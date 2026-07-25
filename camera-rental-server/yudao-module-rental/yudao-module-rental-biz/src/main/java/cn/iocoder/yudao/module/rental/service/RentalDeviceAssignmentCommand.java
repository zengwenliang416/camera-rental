package cn.iocoder.yudao.module.rental.service;

import java.time.LocalDate;

/**
 * Local command consumed by the future authorized operations boundary.
 */
public record RentalDeviceAssignmentCommand(Long rentalOrderItemId, Long deviceId, LocalDate occupyStartDate,
                                            LocalDate occupyEndDateExclusive, String idempotencyKey) {
}
