package cn.iocoder.yudao.module.rental.service.returnregistration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReturnRegistrationModels {
    private ReturnRegistrationModels() {
    }

    public record PublicContext(
            String status, String formNo, String orderNo, String sourceType,
            LocalDate rentalStart, LocalDate rentalEnd, int assignedDeviceCount,
            LocalDateTime expiresAt, Receipt receipt
    ) {
    }

    public record UploadAuthorization(
            Long attachmentId, Long configId, String path, String uploadUrl,
            String category, String contentType, long maxSize, int expiresInSeconds
    ) {
    }

    public record AttachmentView(
            Long attachmentId, Long fileId, String category, String name,
            Long size, String previewUrl
    ) {
    }

    public record Submission(
            String orderNo, String carrierCode, String carrierName, String waybillNo,
            LocalDate shippedDate, List<String> serials, List<Long> attachmentIds,
            String issueDescription, String idempotencyKey
    ) {
    }

    public record Receipt(
            String formNo, String status, String waybillNo, Long deliveryId,
            LocalDateTime submittedAt
    ) {
    }

    public record AdminCreateResult(Long id, String formNo, String token, String sharePath,
                                    LocalDateTime expiresAt) {
    }

    public record AdminRow(
            Long id, String formNo, Long rentalOrderId, String orderNo, String status,
            String carrierName, String waybillNo, LocalDateTime expiresAt,
            LocalDateTime submittedAt, LocalDateTime createTime
    ) {
    }

    public record AdminDeviceView(String submittedSerial, String normalizedSerial,
                                  String matchStatus, String matchMessage, Long deviceId) {
    }

    public record AdminCustomerView(String name, String mobile, String address) {
    }

    public record AdminDetail(
            Long id, String formNo, Long rentalOrderId, String orderNo, String status,
            String carrierCode, String carrierName, String waybillNo, LocalDate shippedDate,
            String issueDescription, Long deliveryId, LocalDateTime expiresAt,
            LocalDateTime submittedAt, LocalDateTime reviewedAt, Long reviewerId,
            String reviewNote, AdminCustomerView customer,
            List<AdminDeviceView> devices, List<AttachmentView> attachments
    ) {
    }
}
