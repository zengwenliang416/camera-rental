package cn.iocoder.yudao.module.rental.service.logistics;

public record RentalDeliveryDeviceCommand(
        Long rentalOrderItemId,
        Long assignmentId,
        Long deviceId
) {
}
