package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.UserRole;

import java.util.List;

/**
 * 用户角色关联Service接口
 */
public interface UserRoleService extends IService<UserRole> {
    /**
     * 查询用户关联的角色名称
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    List<String> selectRoleByUserId(Integer userId);
}
