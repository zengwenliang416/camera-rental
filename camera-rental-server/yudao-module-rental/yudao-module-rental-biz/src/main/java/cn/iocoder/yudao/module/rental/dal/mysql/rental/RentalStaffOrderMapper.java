package cn.iocoder.yudao.module.rental.dal.mysql.rental;

import cn.iocoder.yudao.module.rental.controller.admin.rental.vo.RentalStaffOrderRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface RentalStaffOrderMapper {
    List<RentalStaffOrderRespVO> selectPage(@Param("tenantId") Long tenantId, @Param("queue") String queue,
            @Param("keyword") String keyword, @Param("beforeId") Long beforeId,
            @Param("offset") long offset, @Param("limit") int limit);
    long count(@Param("tenantId") Long tenantId, @Param("queue") String queue);
}
