package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.vo.UserVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据访问接口
 */
public interface UserMapper extends BaseMapper<User> {
    /**
     * 查询用户有权访问的菜单ID
     *
     * @param userId 用户ID
     * @return 用户有权访问的菜单ID集合
     */
    List<Long> getNavMenu(@Param("userId") Integer userId);

    /**
     * 按用户名查询用户展示信息
     *
     * @param username 用户名
     * @return 用户展示信息
     */
    UserVo getUserInfo(@Param("username") String username);

    /**
     * 查询用户展示信息列表
     *
     * @return 用户展示信息列表
     */
    List<UserVo> getUserVoList();

    /**
     * 查询可参与审批的用户展示信息
     *
     * @return 用户展示信息列表
     */
    List<UserVo> getApproveUsers();

    /**
     * 按ID查询用户展示信息
     *
     * @param userId 用户ID
     * @return 用户展示信息
     */
    UserVo getUserInfoById(@Param("userId") Integer userId);

    /**
     * 按学院查询用户
     *
     * @param college 学院名称
     * @return 用户列表
     */
    default List<User> getUserByCollege(@Param("college") String college) {
        return selectList(new LambdaQueryWrapper<User>().eq(User::getCollege, college));
    }

}
