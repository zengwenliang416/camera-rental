package cn.iocoder.yudao.module.rental.service;

/**
 * Assigns one concrete device and creates its occupied schedule atomically.
 */
public interface RentalDeviceAssignmentService {

    RentalDeviceAssignmentResult assign(RentalDeviceAssignmentCommand command);

    /**
     * Creates an assignment for an already identified device without creating an
     * occupied schedule. This is reserved for explicitly confirmed early
     * dispatches whose rental dates are still incomplete.
     */
    RentalDeviceAssignmentResult assignPendingPlan(Long rentalOrderItemId, Long deviceId,
                                                   String idempotencyKey);

}
