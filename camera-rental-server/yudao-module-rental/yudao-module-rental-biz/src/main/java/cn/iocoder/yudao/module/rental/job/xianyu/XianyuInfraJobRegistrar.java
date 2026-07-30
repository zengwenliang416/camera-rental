package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.infra.service.job.dto.JobCreateReqDTO;
import cn.iocoder.yudao.module.rental.integration.xianyu.security.XianyuSafeErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.quartz.SchedulerException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Registers XianGuanJia sync handlers into infra Job + Quartz when the scheduler is available.
 * Idempotent: creates missing handlers and reports existing DB configuration drift.
 */
@Component
@Order(200)
@Slf4j
public class XianyuInfraJobRegistrar implements ApplicationRunner {

    private final JobService jobService;

    public XianyuInfraJobRegistrar(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureJob("闲管家授权店铺同步", "xianyuShopSyncJob", "0 0/30 * * * ?");
        ensureJob("闲管家订单增量同步", "xianyuOrderSyncJob", "0 * * * * ?");
        ensureJob("闲管家商品增量同步", "xianyuProductSyncJob", "0 0/10 * * * ?");
        ensureJob("闲管家售后增量同步", "xianyuAfterSaleSyncJob", "0 0/10 * * * ?");
        ensureJob("闲管家推送失败重试", "xianyuPushRetryJob", "0 0/5 * * * ?");
    }

    private void ensureJob(String name, String handlerName, String cron) {
        JobCreateReqDTO req = new JobCreateReqDTO();
        // name max 32 chars
        req.setName(name.length() > 32 ? name.substring(0, 32) : name);
        req.setHandlerName(handlerName);
        req.setHandlerParam("");
        req.setCronExpression(cron == null || cron.isBlank() ? "0 * * * * ?" : cron);
        req.setRetryCount(1);
        req.setRetryInterval(5000);
        req.setMonitorTimeout(0);
        try {
            Long id = jobService.createJobIfAbsentByHandler(req);
            JobDO registered = jobService.getJob(id);
            if (registered == null) {
                log.warn("[xianyu][job-register] persisted job unavailable id={} handler={}", id, handlerName);
                return;
            }
            if (!Objects.equals(registered.getCronExpression(), req.getCronExpression())) {
                log.warn("[xianyu][job-register] existing cron differs handler={} configured={} actual={} status={}",
                        handlerName, req.getCronExpression(), registered.getCronExpression(), registered.getStatus());
                return;
            }
            if (Objects.equals(registered.getStatus(), JobStatusEnum.STOP.getStatus())) {
                log.info("[xianyu][job-register] existing infra job remains stopped id={} handler={} cron={}",
                        id, handlerName, registered.getCronExpression());
                return;
            }
            log.info("[xianyu][job-register] infra job ready id={} handler={} cron={} status={}",
                    id, handlerName, registered.getCronExpression(), registered.getStatus());
        } catch (SchedulerException ex) {
            log.warn("[xianyu][job-register] Quartz unavailable handler={} code={}",
                    handlerName, XianyuSafeErrorCode.from(ex));
        } catch (Exception ex) {
            log.warn("[xianyu][job-register] failed handler={} code={}",
                    handlerName, XianyuSafeErrorCode.from(ex));
        }
    }

}
