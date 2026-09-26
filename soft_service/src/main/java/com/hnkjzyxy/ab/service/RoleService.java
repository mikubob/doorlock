package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Role;

import java.util.List;


public interface RoleService extends IService<Role> {
    public void deleteRole(String ids);

    public List<Role> getRoleByUserID(Integer userId);
}
