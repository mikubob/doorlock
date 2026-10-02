package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.UserRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
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
    @Select("select ur.role_id from sys_role ro,sys_user_role ur where ro.role_id = ur.role_id and ur.user_id = #{userId} and ro.weight != 1")
    List<Integer> getRoles(@Param("userId") Integer userId);


    /**
     * 查询用户权重最高的角色
     *
     * @param userId 用户ID
     * @return 匹配的角色信息
     */
    @Select("select * from sys_role where role_status = 1 and role_id in (select role_id from sys_user_role where user_id = #{userId}) order by weight desc limit 1")
    Role getRoleWeight(@Param("userId") Integer userId);

    /**
     * 查询用户关联的角色名称
     *
     * @param userId 用户ID
     * @return 查询结果列表
     */
    @Select("select role_name from sys_role where role_status = 1 and  role_id in (select role_id from sys_user_role where user_id = #{userId}) order by weight desc limit 1")
    List<String> selectRoleByUserId(@Param("userId") Integer userId);

    /**
     * 查询角色关联的用户ID
     *
     * @param roleId 角色ID
     * @return 查询结果列表
     */
    @Select("select distinct us.user_id from sys_user us,sys_user_role ur where ur.role_id = #{roleId} and us.user_id = ur.user_id")
    List<Integer> selectUserIdByRoleId(@Param("roleId") Integer roleId);

}
