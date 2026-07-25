package cn.iocoder.yudao.module.rental.service;

/**
 * Assigns one concrete device and creates its occupied schedule atomically.
 */
public interface RentalDeviceAssignmentService {

    RentalDeviceAssignmentResult assign(RentalDeviceAssignmentCommand command);

}
