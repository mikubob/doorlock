package com.hnkjzyxy.ab.service;

import com.hnkjzyxy.ab.model.LockPassword;

/**
 * 开锁密码Service接口
 */
public interface LockPasswordService {
    
    /**
     * 验证开锁密码
     * @param password 明文密码
     * @return 是否验证通过
     */
    boolean verifyPassword(String password);
    
    /**
     * 验证旧密码是否正确
     * @param oldPassword 旧密码明文
     * @return 是否验证通过
     */
    boolean verifyOldPassword(String oldPassword);
    
    /**
     * 获取启用的开锁密码记录
     * @return 开锁密码对象
     */
    LockPassword getEnabledPassword();
    
    /**
     * 设置开锁密码(首次设置或修改)
     * @param password 明文密码
     * @param description 描述
     * @return 是否成功
     */
    boolean setPassword(String password, String description);
}
