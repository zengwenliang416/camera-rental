package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

public record XianyuPushReplayOutcome(
        Long eventId,
        String status,
        String safeErrorCode,
        String message
) {

    public static XianyuPushReplayOutcome queued(Long eventId) {
        return new XianyuPushReplayOutcome(eventId, "QUEUED", null, "Replay event queued");
    }

    public static XianyuPushReplayOutcome skipped(Long eventId, String status) {
        return new XianyuPushReplayOutcome(eventId, "SKIPPED", null,
                "Replay skipped for current status: " + status);
    }

    public static XianyuPushReplayOutcome failed(Long eventId, String safeErrorCode) {
        return new XianyuPushReplayOutcome(eventId, "FAILED", safeErrorCode,
                "Replay preparation failed: " + safeErrorCode);
    }

}
