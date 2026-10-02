package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Message;
import org.apache.ibatis.annotations.Param;

/**
 * 用户消息数据访问接口
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/4/18 20:11
 */
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 将用户通知消息标记为已读
     *
     * @param userId 用户ID
     */
    default void readNotice(@Param("userId") Integer userId) {
        update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getUserId, userId).set(Message::getStatus, 1));
    }
}
