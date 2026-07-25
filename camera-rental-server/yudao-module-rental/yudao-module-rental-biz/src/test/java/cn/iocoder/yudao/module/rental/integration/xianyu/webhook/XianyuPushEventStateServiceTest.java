package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XianyuPushEventStateServiceTest {

    private final XianyuPushEventMapper eventMapper = mock(XianyuPushEventMapper.class);
    private final XianyuPushEventStateService service = new XianyuPushEventStateService(
            eventMapper, Clock.fixed(Instant.parse("2026-07-24T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void shouldClaimReceivedEventWithAttemptToken() {
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(40L)
                .processingStatus("RECEIVED")
                .build();
        when(eventMapper.selectByIdForUpdate(40L)).thenReturn(event);

        String processingToken = service.claim(40L);

        assertNotNull(processingToken);
        assertEquals("PROCESSING", event.getProcessingStatus());
        assertEquals(processingToken, event.getProcessingToken());
        verify(eventMapper).updateById(event);
    }

    @Test
    void shouldNeverPrepareSuccessfulEventForRetry() {
        when(eventMapper.selectByIdForUpdate(41L)).thenReturn(XianyuPushEventDO.builder()
                .id(41L)
                .processingStatus("SUCCEEDED")
                .build());

        assertFalse(service.prepareRetry(41L, LocalDateTime.of(2026, 7, 24, 11, 58)));
        verify(eventMapper, never()).updateById(
                org.mockito.ArgumentMatchers.any(XianyuPushEventDO.class));
    }

    @Test
    void shouldRecoverStaleProcessingEventByInvalidatingItsClaimToken() {
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(41L)
                .processingStatus("PROCESSING")
                .processingToken("stale-token")
                .build();
        event.setUpdateTime(LocalDateTime.of(2026, 7, 24, 11, 50));
        when(eventMapper.selectByIdForUpdate(41L)).thenReturn(event);

        assertTrue(service.prepareRetry(41L, LocalDateTime.of(2026, 7, 24, 11, 58)));
        verify(eventMapper).updateById(event);
        assertEquals("RECEIVED", event.getProcessingStatus());
        assertNull(event.getProcessingToken());
    }

    @Test
    void shouldRecoverStaleFailedEvent() {
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(42L)
                .processingStatus("FAILED")
                .build();
        event.setUpdateTime(LocalDateTime.of(2026, 7, 24, 11, 50));
        when(eventMapper.selectByIdForUpdate(42L)).thenReturn(event);

        assertTrue(service.prepareRetry(42L, LocalDateTime.of(2026, 7, 24, 11, 58)));
        verify(eventMapper).updateById(event);
        assertEquals("RECEIVED", event.getProcessingStatus());
    }

    @Test
    void shouldPrepareFailedEventForManualReplay() {
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(45L)
                .processingStatus("FAILED")
                .processingToken("old-token")
                .lastErrorCode("REMOTE_ERROR")
                .lastErrorMessage("old error")
                .processedAt(LocalDateTime.of(2026, 7, 24, 11, 0))
                .build();
        when(eventMapper.selectByIdForUpdate(45L)).thenReturn(event);

        assertTrue(service.prepareManualReplay(45L, "9"));

        verify(eventMapper).updateById(event);
        assertEquals("RECEIVED", event.getProcessingStatus());
        assertNull(event.getProcessingToken());
        assertNull(event.getLastErrorCode());
        assertNull(event.getLastErrorMessage());
        assertNull(event.getProcessedAt());
        assertEquals("9", event.getUpdater());
    }

    @Test
    void shouldNotPrepareSuccessfulEventForManualReplay() {
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(46L)
                .processingStatus("SUCCEEDED")
                .build();
        when(eventMapper.selectByIdForUpdate(46L)).thenReturn(event);

        assertFalse(service.prepareManualReplay(46L, "9"));

        verify(eventMapper, never()).updateById(
                org.mockito.ArgumentMatchers.any(XianyuPushEventDO.class));
    }

    @Test
    void shouldIgnoreCompletionFromSupersededAttempt() {
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(43L)
                .processingStatus("PROCESSING")
                .processingToken("current-token")
                .build();
        when(eventMapper.selectByIdForUpdate(43L)).thenReturn(event);

        service.markSucceeded(43L, "stale-token");

        verify(eventMapper, never()).updateById(
                org.mockito.ArgumentMatchers.any(XianyuPushEventDO.class));
    }

    @Test
    void shouldIgnoreFailureFromSupersededAttempt() {
        XianyuPushEventDO event = XianyuPushEventDO.builder()
                .id(44L)
                .processingStatus("PROCESSING")
                .processingToken("current-token")
                .build();
        when(eventMapper.selectByIdForUpdate(44L)).thenReturn(event);

        service.markFailed(44L, "stale-token", "REMOTE_ERROR");

        verify(eventMapper, never()).updateById(
                org.mockito.ArgumentMatchers.any(XianyuPushEventDO.class));
    }

}
