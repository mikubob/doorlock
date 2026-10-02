package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.LockPassword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 开锁密码数据访问接口
 */
@Mapper
public interface LockPasswordMapper extends BaseMapper<LockPassword> {

    /**
     * 根据状态查询开锁密码
     *
     * @param status 状态
     * @return 匹配的开锁密码，不存在时返回 null
     */
    default LockPassword selectByStatus(@Param("status") Integer status) {
        return selectOne(new LambdaQueryWrapper<LockPassword>().eq(LockPassword::getStatus, status));
    }
}
