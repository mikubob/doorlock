package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.ScheduleAudit;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排程审计数据访问接口。
 */
@Mapper
public interface ScheduleAuditMapper extends BaseMapper<ScheduleAudit> {
    /**
     * 重新核对排程前关闭上一版本问题，当前问题随后原子重新登记。
     * @return 关闭行数
     */
    int resolveOpen();
}
