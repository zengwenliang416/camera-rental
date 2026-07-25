package cn.iocoder.yudao.module.rental.integration.xianyu.webhook;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuPushEventDO;
import cn.iocoder.yudao.module.rental.dal.mysql.xianyu.XianyuPushEventMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class XianyuPushEventStateService {

    private final XianyuPushEventMapper eventMapper;
    private final Clock clock;

    public XianyuPushEventStateService(XianyuPushEventMapper eventMapper,
                                       @Qualifier("xianyuClock") Clock clock) {
        this.eventMapper = eventMapper;
        this.clock = clock;
    }

    @Transactional(rollbackFor = Exception.class)
    public String claim(Long eventId) {
        XianyuPushEventDO event = eventMapper.selectByIdForUpdate(eventId);
        if (event == null || "SUCCEEDED".equals(event.getProcessingStatus())
                || "PROCESSING".equals(event.getProcessingStatus())
                || !("RECEIVED".equals(event.getProcessingStatus()) || "FAILED".equals(event.getProcessingStatus()))) {
            return null;
        }
        String processingToken = UUID.randomUUID().toString();
        event.setProcessingStatus("PROCESSING");
        event.setProcessingToken(processingToken);
        event.setLastErrorCode(null);
        event.setLastErrorMessage(null);
        event.setUpdater("system");
        eventMapper.updateById(event);
        return processingToken;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean prepareRetry(Long eventId, LocalDateTime staleBefore) {
        XianyuPushEventDO event = eventMapper.selectByIdForUpdate(eventId);
        if (event == null || "SUCCEEDED".equals(event.getProcessingStatus())
                || event.getUpdateTime() == null || event.getUpdateTime().isAfter(staleBefore)) {
            return false;
        }
        String status = event.getProcessingStatus();
        if (!("RECEIVED".equals(status) || "FAILED".equals(status) || "PROCESSING".equals(status))) {
            return false;
        }
        event.setProcessingStatus("RECEIVED");
        event.setProcessingToken(null);
        event.setLastErrorCode(null);
        event.setLastErrorMessage(null);
        event.setProcessedAt(null);
        event.setUpdater("system");
        eventMapper.updateById(event);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean prepareManualReplay(Long eventId, String updater) {
        XianyuPushEventDO event = eventMapper.selectByIdForUpdate(eventId);
        if (event == null || "SUCCEEDED".equals(event.getProcessingStatus())
                || "PROCESSING".equals(event.getProcessingStatus())) {
            return false;
        }
        String status = event.getProcessingStatus();
        if (!("RECEIVED".equals(status) || "FAILED".equals(status))) {
            return false;
        }
        event.setProcessingStatus("RECEIVED");
        event.setProcessingToken(null);
        event.setLastErrorCode(null);
        event.setLastErrorMessage(null);
        event.setProcessedAt(null);
        event.setUpdater(StringUtils.hasText(updater) ? updater : "system");
        eventMapper.updateById(event);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markSucceeded(Long eventId, String processingToken) {
        XianyuPushEventDO event = eventMapper.selectByIdForUpdate(eventId);
        if (!isCurrentAttempt(event, processingToken)) {
            return;
        }
        event.setProcessingStatus("SUCCEEDED");
        event.setProcessingToken(null);
        event.setLastErrorCode(null);
        event.setLastErrorMessage(null);
        event.setProcessedAt(LocalDateTime.now(clock));
        event.setUpdater("system");
        eventMapper.updateById(event);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long eventId, String processingToken, String safeErrorCode) {
        XianyuPushEventDO event = eventMapper.selectByIdForUpdate(eventId);
        if (!isCurrentAttempt(event, processingToken)) {
            return;
        }
        applyFailure(event, safeErrorCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRetryPreparationFailed(Long eventId, String safeErrorCode) {
        XianyuPushEventDO event = eventMapper.selectByIdForUpdate(eventId);
        if (event == null || !"RECEIVED".equals(event.getProcessingStatus())) {
            return;
        }
        applyFailure(event, safeErrorCode);
    }

    private void applyFailure(XianyuPushEventDO event, String safeErrorCode) {
        event.setProcessingStatus("FAILED");
        event.setProcessingToken(null);
        event.setLastErrorCode(safeErrorCode);
        event.setLastErrorMessage("Push event processing failed");
        event.setProcessedAt(LocalDateTime.now(clock));
        event.setUpdater("system");
        eventMapper.updateById(event);
    }

    private boolean isCurrentAttempt(XianyuPushEventDO event, String processingToken) {
        return event != null
                && "PROCESSING".equals(event.getProcessingStatus())
                && StringUtils.hasText(processingToken)
                && Objects.equals(event.getProcessingToken(), processingToken);
    }

}
