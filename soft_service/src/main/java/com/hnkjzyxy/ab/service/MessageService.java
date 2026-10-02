package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Message;

/**
 * 用户消息Service接口
 *
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/4/18 20:10
 */
public interface MessageService extends IService<Message> {
    /**
     * 将用户通知消息标记为已读
     *
     * @param userId 用户ID
     */
    void readNotice(Integer userId);
}
