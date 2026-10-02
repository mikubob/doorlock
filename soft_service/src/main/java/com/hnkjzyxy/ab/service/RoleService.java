package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Role;

import java.util.List;


/**
 * 角色Service接口
 */
public interface RoleService extends IService<Role> {
    /**
     * 删除角色及其用户和菜单关联
     *
     * @param ids 角色ID集合
     */
    public void deleteRole(String ids);

    /**
     * 查询用户关联的角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<Role> getRoleByUserID(Integer userId);
}
