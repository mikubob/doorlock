package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hnkjzyxy.ab.mapper.LockPasswordMapper;
import com.hnkjzyxy.ab.model.LockPassword;
import com.hnkjzyxy.ab.service.LockPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 开锁密码Service实现类
 */
@Service
public class LockPasswordServiceImpl implements LockPasswordService {
    
    @Autowired
    private LockPasswordMapper lockPasswordMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public boolean verifyPassword(String password) {
        // 查询启用的密码记录
        LockPassword lockPassword = getEnabledPassword();
        if (lockPassword == null) {
            return false;
        }
        
        // 验证密码
        return passwordEncoder.matches(password, lockPassword.getPassword());
    }
    
    @Override
    public boolean verifyOldPassword(String oldPassword) {
        // 查询启用的密码记录
        LockPassword lockPassword = getEnabledPassword();
        if (lockPassword == null) {
            // 如果没有设置过密码，则不需要验证旧密码
            return true;
        }
        
        // 验证旧密码
        return passwordEncoder.matches(oldPassword, lockPassword.getPassword());
    }
    
    @Override
    public LockPassword getEnabledPassword() {
        QueryWrapper<LockPassword> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        wrapper.last("LIMIT 1");
        return lockPasswordMapper.selectOne(wrapper);
    }
    
    @Override
    public boolean setPassword(String password, String description) {
        // 先禁用所有旧密码
        QueryWrapper<LockPassword> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        LockPassword oldPassword = lockPasswordMapper.selectOne(wrapper);
        if (oldPassword != null) {
            oldPassword.setStatus(0);
            lockPasswordMapper.updateById(oldPassword);
        }
        
        // 创建新密码
        LockPassword newPassword = new LockPassword();
        newPassword.setPassword(passwordEncoder.encode(password));
        newPassword.setDescription(description);
        newPassword.setStatus(1);
        
        return lockPasswordMapper.insert(newPassword) > 0;
    }
}
