package cn.iocoder.yudao.module.rental.job.logistics;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.rental.service.logistics.RentalDeliveryInboxWorker;
import org.springframework.stereotype.Component;

@Component("rentalDeliveryInboxJob")
public class RentalDeliveryInboxJob implements JobHandler {

    private final RentalDeliveryInboxWorker worker;

    public RentalDeliveryInboxJob(RentalDeliveryInboxWorker worker) {
        this.worker = worker;
    }

    @Override
    @TenantJob
    public String execute(String param) {
        return "processed=" + worker.processBatch(20);
    }
}
