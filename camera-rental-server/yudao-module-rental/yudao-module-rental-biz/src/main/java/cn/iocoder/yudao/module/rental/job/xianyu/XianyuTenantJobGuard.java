package cn.iocoder.yudao.module.rental.job.xianyu;

import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuProperties;
import cn.iocoder.yudao.module.rental.integration.xianyu.config.XianyuRuntimeConfigService;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class XianyuTenantJobGuard {

    private final XianyuRuntimeConfigService runtimeConfigService;

    public XianyuTenantJobGuard(XianyuRuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }

    public String execute(Supplier<String> action) {
        XianyuProperties properties = runtimeConfigService.getCurrent();
        if (!properties.getJob().isEnabled()
                || properties.getIntegrationStatus() != XianyuProperties.IntegrationStatus.READY) {
            return "skip: status=" + properties.getIntegrationStatus()
                    + " jobEnabled=" + properties.getJob().isEnabled();
        }
        return action.get();
    }

}
