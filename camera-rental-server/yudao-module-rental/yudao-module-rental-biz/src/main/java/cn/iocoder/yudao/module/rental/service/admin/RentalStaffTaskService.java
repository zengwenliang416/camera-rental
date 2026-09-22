package cn.iocoder.yudao.module.rental.service.admin;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.rental.dal.mysql.rental.RentalStaffTaskMapper;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffTaskRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
@Service @RequiredArgsConstructor
public class RentalStaffTaskService {
    private final RentalStaffTaskMapper mapper;
    public PageResult<RentalStaffTaskRespVO> page(PageParam page, String queue) {
        Long tenant = TenantContextHolder.getRequiredTenantId();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        return new PageResult<>(mapper.page(tenant, queue, today, (long)(page.getPageNo()-1)*page.getPageSize(), page.getPageSize()), mapper.count(tenant,queue,today));
    }
}
