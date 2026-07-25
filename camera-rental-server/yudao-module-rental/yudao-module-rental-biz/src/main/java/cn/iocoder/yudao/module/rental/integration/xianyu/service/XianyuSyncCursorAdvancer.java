package cn.iocoder.yudao.module.rental.integration.xianyu.service;

import cn.iocoder.yudao.module.rental.dal.dataobject.xianyu.XianyuSyncCursorDO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Compares the documented stable order-sync tuple: update time then external order ID.
 */
@Component
public class XianyuSyncCursorAdvancer {

    public boolean isStrictlyNewer(XianyuSyncCursorDO current, LocalDateTime candidateUpdatedAt,
                                   String candidateExternalId) {
        Objects.requireNonNull(candidateUpdatedAt, "candidateUpdatedAt");
        Objects.requireNonNull(candidateExternalId, "candidateExternalId");
        if (current == null || current.getCursorUpdatedAt() == null) {
            return true;
        }
        int timestampComparison = candidateUpdatedAt.compareTo(current.getCursorUpdatedAt());
        if (timestampComparison != 0) {
            return timestampComparison > 0;
        }
        return current.getCursorExternalId() == null
                || candidateExternalId.compareTo(current.getCursorExternalId()) > 0;
    }

}
