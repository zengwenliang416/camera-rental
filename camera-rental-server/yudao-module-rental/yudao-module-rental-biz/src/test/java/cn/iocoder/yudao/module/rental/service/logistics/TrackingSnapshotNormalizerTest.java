package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingEvent;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingSnapshot;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TrackingSnapshotNormalizerTest {

    private final TrackingSnapshotNormalizer normalizer = new TrackingSnapshotNormalizer(new LogisticsHashing());

    @Test
    void createsStableHashAcrossProviderOrderingAndWhitespace() {
        LogisticsTrackingEvent first = event("2026-07-31T08:00:00", " picked  up ",
                RentalTrackingStatusEnum.PICKED_UP);
        LogisticsTrackingEvent second = event("2026-07-31T10:00:00", "in transit",
                RentalTrackingStatusEnum.IN_TRANSIT);

        NormalizedTrackingSnapshot left = normalizer.normalize(
                new LogisticsTrackingSnapshot(List.of(second, first), null, null));
        NormalizedTrackingSnapshot right = normalizer.normalize(
                new LogisticsTrackingSnapshot(List.of(
                        event("2026-07-31T08:00:00", "picked up", RentalTrackingStatusEnum.PICKED_UP),
                        second), null, null));

        assertEquals(left.snapshotHash(), right.snapshotHash());
        assertEquals(1, left.events().get(0).sequence());
        assertEquals("picked up", left.events().get(0).traceText());
    }

    @Test
    void changesHashWhenCompleteSnapshotChanges() {
        NormalizedTrackingSnapshot left = normalizer.normalize(new LogisticsTrackingSnapshot(
                List.of(event("2026-07-31T08:00:00", "picked up", RentalTrackingStatusEnum.PICKED_UP)),
                null, null));
        NormalizedTrackingSnapshot right = normalizer.normalize(new LogisticsTrackingSnapshot(
                List.of(event("2026-07-31T08:00:00", "in transit", RentalTrackingStatusEnum.IN_TRANSIT)),
                null, null));

        assertNotEquals(left.snapshotHash(), right.snapshotHash());
    }

    @Test
    void createsStableHashWhenEventsShareTheSameTimeAndTraceText() {
        LocalDateTime time = LocalDateTime.parse("2026-07-31T08:00:00");
        LogisticsTrackingEvent changsha = new LogisticsTrackingEvent(time, "2026-07-31 08:00:00",
                RentalTrackingStatusEnum.IN_TRANSIT, "5", "in transit", "Changsha", "QUERY", null);
        LogisticsTrackingEvent shanghai = new LogisticsTrackingEvent(time, "2026-07-31 08:00:00",
                RentalTrackingStatusEnum.IN_TRANSIT, "5", "in transit", "Shanghai", "CALLBACK", null);

        NormalizedTrackingSnapshot left = normalizer.normalize(
                new LogisticsTrackingSnapshot(List.of(changsha, shanghai), null, null));
        NormalizedTrackingSnapshot right = normalizer.normalize(
                new LogisticsTrackingSnapshot(List.of(shanghai, changsha), null, null));

        assertEquals(left.snapshotHash(), right.snapshotHash());
        assertEquals(left.events(), right.events());
    }

    @Test
    void terminalStatusCannotRegress() {
        assertEquals(RentalTrackingStatusEnum.DELIVERED,
                normalizer.protectTerminal(RentalTrackingStatusEnum.DELIVERED,
                        RentalTrackingStatusEnum.IN_TRANSIT));
        assertEquals(RentalTrackingStatusEnum.RETURNED,
                normalizer.protectTerminal(RentalTrackingStatusEnum.RETURNED,
                        RentalTrackingStatusEnum.EXCEPTION));
    }

    private LogisticsTrackingEvent event(String time, String text, RentalTrackingStatusEnum status) {
        return new LogisticsTrackingEvent(LocalDateTime.parse(time), time, status, status.name(), text,
                "Changsha", "QUERY", null);
    }
}
