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
     * 查询用户关联的全部有效角色码
     * <p>
     * 只返回状态为1且编码非空、非全空白的真实角色；缺失角色记录及重复关联不增加权限。
     * 按编码字节去重，保留大小写及首尾空格供上层精确匹配，不按名称或最高权重取单个角色。
     * 对应查询禁用二级缓存并刷新本地缓存，不使用已有权限缓存决定本次授权。
     * </p>
     *
     * @param userId 已核验的当前用户ID
     * @return 去重的有效角色码列表，无匹配角色时返回空列表
     */
    List<String> selectActiveRoleCodes(@Param("userId") Integer userId);

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
