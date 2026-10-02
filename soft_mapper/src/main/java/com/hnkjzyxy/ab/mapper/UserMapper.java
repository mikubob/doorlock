package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.vo.UserVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

//@CacheNamespace(implementation = MybatisRedisCache.class)
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
    @Select("SELECT t2.menu_id from sys_user_role t1\n" +
            "INNER JOIN sys_role_menu t2 on t1.role_id = t2.role_id\n" +
            "WHERE t1.user_id = ${userId} \n" +
            "GROUP BY t2.menu_id")
    public List<Long> getNavMenu(Integer userId);

    /**
     * 按用户名查询用户展示信息
     *
     * @param username 用户名
     * @return 用户展示信息
     */
    @Select("select user_id userId,avatar,nick_name nickName,user_name userName,email,phone,sex,major " +
            "from sys_user where user_name = #{username} and status = 1")
    public UserVo getUserInfo(@Param("username") String username);


    /**
     * 查询用户展示信息列表
     *
     * @return 用户展示信息列表
     */
    @Select("select distinct A.user_id userId,A.nick_name nickName,A.user_name userName " +
            "FROM `sys_user` A join sys_user_role B on A.user_id = B.user_id " +
            "join sys_role C on B.role_id = C.role_id where C.weight != 10 and C.role_status = 1 and A.status = 1")
    List<UserVo> getUserVoList();

    /**
     * 查询可参与审批的用户展示信息
     *
     * @return 用户展示信息列表
     */
    @Select("select distinct A.user_id userId,A.nick_name nickName,A.user_name userName " +
            "FROM `sys_user` A join sys_user_role B on A.user_id = B.user_id " +
            "join sys_role C on B.role_id = C.role_id where C.weight >= 5 and C.weight != 10 and C.role_status = 1 and A.status = 1")
    List<UserVo> getApproveUsers();

    /**
     * 按ID查询用户展示信息
     *
     * @param userId 用户ID
     * @return 用户展示信息
     */
    @Select("select user_id userId,avatar,nick_name nickName,user_name userName,email,phone,sex,major " +
            "from sys_user where user_id = #{userId} and status = 1")
    public UserVo getUserInfoById(@Param("userId") Integer userId);
    /**
     * 按学院查询用户
     *
     * @param college 学院名称
     * @return 用户列表
     */
    @Select("select * from sys_user where college =#{college} ")
    public List<User> getUserByCollege(@Param("college") String college);

}
