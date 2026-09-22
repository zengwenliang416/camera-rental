package cn.iocoder.yudao.module.rental.service.admin;
import cn.iocoder.yudao.module.rental.dal.dataobject.rental.RentalShipmentAttemptDO;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalShipmentAttemptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.rental.enums.ErrorCodeConstants.STAFF_WORKFLOW_INVALID;

@Service @RequiredArgsConstructor
public class RentalShipmentAttemptService {
    private final RentalShipmentAttemptMapper mapper;
    /** Commits before the remote write, independently of the surrounding fulfillment transaction. */
    @Transactional(propagation=Propagation.REQUIRES_NEW, rollbackFor=Exception.class)
    public void begin(Long channelId, String key, String hash) {
        var row = mapper.selectOneForUpdate(new LambdaQueryWrapper<RentalShipmentAttemptDO>().eq(RentalShipmentAttemptDO::getChannelOrderId, channelId));
        if (row != null && !"REJECTED".equals(row.getStatus()))
            throw exception(STAFF_WORKFLOW_INVALID, "此订单已有发货请求，结果未核对前不能再次发货");
        if (row == null) {
            row = new RentalShipmentAttemptDO(); row.setChannelOrderId(channelId); row.setIdempotencyKey(key);
            row.setRequestHash(hash); row.setStatus("SENT"); mapper.insert(row);
        } else {
            row.setIdempotencyKey(key); row.setRequestHash(hash); row.setStatus("SENT"); mapper.updateById(row);
        }
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW, rollbackFor=Exception.class)
    public void rejected(Long channelId, String key) {
        var row = mapper.selectOneForUpdate(new LambdaQueryWrapper<RentalShipmentAttemptDO>()
                .eq(RentalShipmentAttemptDO::getChannelOrderId, channelId).eq(RentalShipmentAttemptDO::getIdempotencyKey, key));
        if (row != null) { row.setStatus("REJECTED"); mapper.updateById(row); }
    }
    public String status(Long channelId, String key) {
        var row = mapper.selectOne(new LambdaQueryWrapper<RentalShipmentAttemptDO>()
                .eq(RentalShipmentAttemptDO::getChannelOrderId, channelId).eq(RentalShipmentAttemptDO::getIdempotencyKey, key));
        return row != null && "REJECTED".equals(row.getStatus()) ? "REJECTED" : "UNKNOWN";
    }
}
