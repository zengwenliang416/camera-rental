package cn.iocoder.yudao.module.rental.job.logistics;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryOutboxWorker;
import org.springframework.stereotype.Component;

@Component("rentalDeliveryOutboxJob")
public class RentalDeliveryOutboxJob implements JobHandler {

    private final RentalDeliveryOutboxWorker worker;

    public RentalDeliveryOutboxJob(RentalDeliveryOutboxWorker worker) {
        this.worker = worker;
    }

    @Override
    @TenantJob
    public String execute(String param) {
        return "processed=" + worker.processBatch(20);
    }
}
