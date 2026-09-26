package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.UserRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Select("select ur.role_id from sys_role ro,sys_user_role ur where ro.role_id = ur.role_id and ur.user_id = #{userId} and ro.weight != 1")
    List<Integer> getRoles(@Param("userId") Integer userId);


    @Select("select * from sys_role where role_status = 1 and role_id in (select role_id from sys_user_role where user_id = #{userId}) order by weight desc limit 1")
    Role getRoleWeight(@Param("userId") Integer userId);

    @Select("select role_name from sys_role where role_status = 1 and  role_id in (select role_id from sys_user_role where user_id = #{userId}) order by weight desc limit 1")
    List<String> selectRoleByUserId(@Param("userId") Integer userId);

    @Select("select distinct us.user_id from sys_user us,sys_user_role ur where ur.role_id = #{roleId} and us.user_id = ur.user_id")
    List<Integer> selectUserIdByRoleId(@Param("roleId") Integer roleId);

}
