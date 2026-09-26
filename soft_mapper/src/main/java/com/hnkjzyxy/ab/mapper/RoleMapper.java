package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Role;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


//@CacheNamespace(implementation = RedisCacheConfig.class)
public interface RoleMapper extends BaseMapper<Role> {
    @Delete("DELETE from sys_role_menu where role_id in (#{ids})")
    void roleMenu(String ids);

    @Delete("DELETE from sys_user_role WHERE role_id in (#{ids})")
    void userRole(String ids);

    @Select("select * from sys_role where weight = #{weight}")
    List<Role> getRoleWeight(@Param("weight") int i);

    @Select("select weight from sys_role where role_id = #{role}")
    Integer getWeightByRole(Integer role);
}
