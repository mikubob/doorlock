package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Role;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


//@CacheNamespace(implementation = MybatisRedisCache.class)
/**
 * 角色数据访问接口
 */
public interface RoleMapper extends BaseMapper<Role> {
    /**
     * 删除指定角色的菜单关联
     *
     * @param ids 角色ID集合
     */
    @Delete("DELETE from sys_role_menu where role_id in (#{ids})")
    void roleMenu(String ids);

    /**
     * 删除指定角色的用户关联
     *
     * @param ids 角色ID集合
     */
    @Delete("DELETE from sys_user_role WHERE role_id in (#{ids})")
    void userRole(String ids);

    /**
     * 查询指定权重的角色
     *
     * @param i 待查询的角色权重
     * @return 指定权重的角色列表
     */
    @Select("select * from sys_role where weight = #{weight}")
    List<Role> getRoleWeight(@Param("weight") int i);

    /**
     * 查询角色权重
     *
     * @param role 角色ID
     * @return 角色权重
     */
    @Select("select weight from sys_role where role_id = #{role}")
    Integer getWeightByRole(Integer role);
}
