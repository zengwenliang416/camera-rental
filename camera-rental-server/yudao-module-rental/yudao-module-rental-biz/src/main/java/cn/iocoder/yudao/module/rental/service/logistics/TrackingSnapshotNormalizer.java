package cn.iocoder.yudao.module.rental.service.logistics;

import cn.iocoder.yudao.module.rental.enums.logistics.RentalTrackingStatusEnum;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingEvent;
import cn.iocoder.yudao.module.rental.integration.logistics.LogisticsTrackingSnapshot;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class TrackingSnapshotNormalizer {

    private static final DateTimeFormatter CANONICAL_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final LogisticsHashing hashing;

    public TrackingSnapshotNormalizer(LogisticsHashing hashing) {
        this.hashing = hashing;
    }

    public NormalizedTrackingSnapshot normalize(LogisticsTrackingSnapshot source) {
        if (source == null || source.events().isEmpty()) {
            return new NormalizedTrackingSnapshot(List.of(), null,
                    source == null ? null : source.estimatedDeliveryAt(),
                    source == null ? null : source.synchronizedAt());
        }
        List<LogisticsTrackingEvent> sorted = source.events().stream()
                .filter(event -> event != null && event.trackingStatus() != null)
                .sorted(Comparator
                        .comparing(LogisticsTrackingEvent::businessTime,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(event -> normalizeText(event.rawTime()))
                        .thenComparing(event -> event.trackingStatus().name())
                        .thenComparing(event -> normalizeCode(event.providerStatus()))
                        .thenComparing(event -> normalizeText(event.traceText()))
                        .thenComparing(event -> normalizeText(event.location()))
                        .thenComparing(event -> normalizeCode(event.source())))
                .toList();
        List<NormalizedTrackingEvent> events = new ArrayList<>(sorted.size());
        StringBuilder snapshotIdentity = new StringBuilder();
        for (int index = 0; index < sorted.size(); index++) {
            LogisticsTrackingEvent event = sorted.get(index);
            String traceText = normalizeText(event.traceText());
            String location = normalizeText(event.location());
            String providerStatus = normalizeCode(event.providerStatus());
            String sourceName = normalizeCode(event.source());
            String identity = canonicalTime(event.businessTime()) + "|" + normalizeText(event.rawTime()) + "|"
                    + event.trackingStatus().name() + "|" + providerStatus + "|" + traceText + "|" + location
                    + "|" + sourceName;
            String fingerprint = hashing.sha256(identity);
            events.add(new NormalizedTrackingEvent(index + 1, fingerprint, event.businessTime(),
                    normalizeText(event.rawTime()), event.trackingStatus(), providerStatus, traceText, location,
                    sourceName, event.inboxId()));
            snapshotIdentity.append(fingerprint).append('\n');
        }
        return new NormalizedTrackingSnapshot(List.copyOf(events), hashing.sha256(snapshotIdentity.toString()),
                source.estimatedDeliveryAt(), source.synchronizedAt());
    }

    public RentalTrackingStatusEnum protectTerminal(RentalTrackingStatusEnum current,
                                                     RentalTrackingStatusEnum candidate) {
        if (current != null && current.isTerminal() && (candidate == null || !candidate.isTerminal())) {
            return current;
        }
        return candidate == null ? RentalTrackingStatusEnum.UNKNOWN : candidate;
    }

    private String canonicalTime(LocalDateTime value) {
        return value == null ? "" : CANONICAL_TIME.format(value);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeCode(String value) {
        return normalizeText(value).toUpperCase(Locale.ROOT);
    }
}
