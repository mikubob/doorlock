package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.UserRole;

import java.util.List;

public interface UserRoleService extends IService<UserRole> {
    List<String> selectRoleByUserId(Integer userId);
}
