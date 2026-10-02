package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.UserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联数据访问接口
 */
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 查询用户角色ID集合
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Integer> getRoles(@Param("userId") Integer userId);

    /**
     * 查询用户权重最高的角色
     *
     * @param userId 用户ID
     * @return 匹配的角色信息
     */
    Role getRoleWeight(@Param("userId") Integer userId);

    /**
     * 查询用户关联的角色名称
     *
     * @param userId 用户ID
     * @return 用户权重最高的有效角色名称列表，最多一项
     */
    List<String> selectRoleByUserId(@Param("userId") Integer userId);

    /**
     * 查询角色关联的用户ID
     *
     * @param roleId 角色ID
     * @return 角色关联的用户ID列表，已去重
     */
    List<Integer> selectUserIdByRoleId(@Param("roleId") Integer roleId);

}
