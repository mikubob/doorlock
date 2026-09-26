package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.LockPassword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 开锁密码Mapper接口
 */
@Mapper
public interface LockPasswordMapper extends BaseMapper<LockPassword> {
    
    /**
     * 根据状态查询启用的开锁密码
     * @param status 状态
     * @return 开锁密码列表
     */
    @Select("SELECT * FROM sys_lock_password WHERE status = #{status}")
    LockPassword selectByStatus(Integer status);
}
