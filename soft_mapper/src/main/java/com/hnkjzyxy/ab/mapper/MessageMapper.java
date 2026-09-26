package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/4/18 20:11
 */
//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface MessageMapper extends BaseMapper<Message> {

    @Update("update sys_message set status = 1 where user_id = #{userId}")
    void readNotice(@Param("userId") Integer userId);
}
