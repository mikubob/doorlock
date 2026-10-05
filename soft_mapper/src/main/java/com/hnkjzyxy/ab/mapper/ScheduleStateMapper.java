package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.ScheduleState;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排程状态数据访问接口。
 */
@Mapper
public interface ScheduleStateMapper extends BaseMapper<ScheduleState> {
    /**
     * 在共同会话锁取得后锁定版本行，避免可重复读快照沿用旧排程。
     * @return 当前排程状态
     */
    ScheduleState lockState();
}
