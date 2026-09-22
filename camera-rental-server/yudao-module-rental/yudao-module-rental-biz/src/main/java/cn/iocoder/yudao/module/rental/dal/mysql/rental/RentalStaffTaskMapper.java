package cn.iocoder.yudao.module.rental.dal.mysql.rental;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.util.List;
import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffTaskRespVO;
@Mapper
public interface RentalStaffTaskMapper {
    List<RentalStaffTaskRespVO> page(@Param("tenantId") Long tenantId, @Param("queue") String queue,
            @Param("today") LocalDate today, @Param("offset") long offset, @Param("limit") int limit);
    long count(@Param("tenantId") Long tenantId, @Param("queue") String queue, @Param("today") LocalDate today);
}
