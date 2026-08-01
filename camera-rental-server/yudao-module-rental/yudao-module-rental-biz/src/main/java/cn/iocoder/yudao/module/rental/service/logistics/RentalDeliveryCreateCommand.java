package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalDeliveryDirectionEnum;

import java.util.List;

public record RentalDeliveryCreateCommand(
        Long rentalOrderId,
        Long channelOrderId,
        RentalDeliveryDirectionEnum direction,
        String sourceType,
        String sourceIdentifier,
        String sourceCarrierCode,
        String sourceCarrierName,
        String waybillNo,
        String trackingPhone,
        List<RentalDeliveryDeviceCommand> devices
) {

    public RentalDeliveryCreateCommand {
        devices = devices == null ? List.of() : List.copyOf(devices);
    }

    public RentalDeliveryCreateCommand(
            Long rentalOrderId,
            RentalDeliveryDirectionEnum direction,
            String sourceType,
            String sourceIdentifier,
            String sourceCarrierCode,
            String sourceCarrierName,
            String waybillNo,
            String trackingPhone,
            List<RentalDeliveryDeviceCommand> devices
    ) {
        this(rentalOrderId, null, direction, sourceType, sourceIdentifier, sourceCarrierCode,
                sourceCarrierName, waybillNo, trackingPhone, devices);
    }
}
