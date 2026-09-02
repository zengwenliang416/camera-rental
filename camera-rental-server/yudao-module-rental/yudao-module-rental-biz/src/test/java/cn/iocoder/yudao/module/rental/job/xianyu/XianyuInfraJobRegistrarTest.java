package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.framework.quartz.core.scheduler.SchedulerManager;
import cn.iocoder.yudao.module.infra.dal.dataobject.job.JobDO;
import cn.iocoder.yudao.module.infra.enums.job.JobStatusEnum;
import cn.iocoder.yudao.module.infra.service.job.JobService;
import cn.iocoder.yudao.module.infra.service.job.dto.JobCreateReqDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.ObjectProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XianyuInfraJobRegistrarTest {

    @Mock
    private JobService jobService;
    @Mock
    private SchedulerManager schedulerManager;
    @Mock
    private Scheduler scheduler;
    @Mock
    private ObjectProvider<Scheduler> schedulerProvider;

    private final Map<Long, JobDO> jobsById = new HashMap<>();
    private final AtomicLong nextId = new AtomicLong(100);
    private Integer existingStatus = JobStatusEnum.INIT.getStatus();

    private XianyuInfraJobRegistrar registrar;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(schedulerProvider.getIfAvailable()).thenReturn(scheduler);
        lenient().when(jobService.createJobIfAbsentByHandler(any(JobCreateReqDTO.class))).thenAnswer(invocation -> {
            JobCreateReqDTO dto = invocation.getArgument(0);
            JobDO job = new JobDO();
            job.setId(nextId.incrementAndGet());
            job.setHandlerName(dto.getHandlerName());
            job.setHandlerParam(dto.getHandlerParam());
            job.setCronExpression(dto.getCronExpression());
            job.setRetryCount(dto.getRetryCount());
            job.setRetryInterval(dto.getRetryInterval());
            job.setStatus(existingStatus);
            jobsById.put(job.getId(), job);
            return job.getId();
        });
        lenient().when(jobService.getJob(anyLong())).thenAnswer(invocation -> jobsById.get(invocation.getArgument(0)));
        registrar = new XianyuInfraJobRegistrar(jobService, schedulerManager, schedulerProvider);
    }

    @Test
    void shouldNotRescheduleWhenQuartzTriggerExists() throws Exception {
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(true);

        registrar.run(null);

        verify(schedulerManager, never()).addJob(anyLong(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    void shouldRescheduleWhenQuartzTriggerIsMissing() throws Exception {
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(false);

        registrar.run(null);

        verify(schedulerManager).addJob(anyLong(), eq("xianyuProductSyncJob"), eq(""),
                eq("0 0/10 * * * ?"), eq(1), eq(5000));
        verify(schedulerManager).addJob(anyLong(), eq("xianyuAfterSaleSyncJob"), eq(""),
                eq("0 0/10 * * * ?"), eq(1), eq(5000));
        verify(scheduler, never()).deleteJob(any(JobKey.class));
    }

    @Test
    void shouldDeleteStaleJobDetailBeforeRescheduling() throws Exception {
        when(scheduler.checkExists(any(TriggerKey.class))).thenReturn(false);
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);

        registrar.run(null);

        verify(scheduler).deleteJob(new JobKey("xianyuProductSyncJob"));
        verify(schedulerManager).addJob(anyLong(), eq("xianyuProductSyncJob"), eq(""),
                eq("0 0/10 * * * ?"), eq(1), eq(5000));
    }

    @Test
    void shouldSkipStoppedJobWithoutScheduling() throws Exception {
        existingStatus = JobStatusEnum.STOP.getStatus();

        registrar.run(null);

        verify(scheduler, never()).checkExists(any(TriggerKey.class));
        verify(schedulerManager, never()).addJob(anyLong(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    void shouldSkipSchedulingWhenQuartzIsDisabled() throws Exception {
        when(schedulerProvider.getIfAvailable()).thenReturn(null);
        registrar = new XianyuInfraJobRegistrar(jobService, schedulerManager, schedulerProvider);

        registrar.run(null);

        verify(schedulerManager, never()).addJob(anyLong(), anyString(), any(), anyString(), any(), any());
    }

}
