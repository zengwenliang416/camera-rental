package cn.iocoder.yudao.module.rental.job.logistics;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.service.logistics.RentalLogisticsCompensationService;
import org.springframework.stereotype.Component;

@Component("rentalLogisticsCompensationJob")
public class RentalLogisticsCompensationJob implements JobHandler {

    private final RentalLogisticsCompensationService service;

    public RentalLogisticsCompensationJob(RentalLogisticsCompensationService service) {
        this.service = service;
    }

    @Override
    @TenantJob
    public String execute(String param) {
        return "enqueued=" + service.enqueueStale(50);
    }
}
