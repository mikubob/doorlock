package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.MessageMapper;
import com.hnkjzyxy.ab.model.Message;
import com.hnkjzyxy.ab.service.MessageService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2023/4/18 20:10
 */
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Resource
    private MessageMapper messageMapper;

    @Override
    public void readNotice(Integer userId) {
        messageMapper.readNotice(userId);
    }
}
