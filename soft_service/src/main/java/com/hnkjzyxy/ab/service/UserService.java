package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.vo.UserVo;

import java.util.Date;
import java.util.List;

/**
 * 用户Service接口
 */
public interface UserService extends IService<User> {
    /**
     * 查询用户权限标识字符串
     *
     * @param userId 用户ID
     * @return 用户权限标识字符串
     */
    String getUserAuthority(Integer userId);

    /**
     * 查询用户有权访问的菜单ID
     *
     * @param userId 用户ID
     * @return 用户有权访问的菜单ID集合
     */
    public List<Long> getNavMenu(Integer userId);

    /**
     * 按用户名查询用户实体
     *
     * @param userName 用户名（工号）
     * @return 用户信息
     */
    public User getUserByName(String userName);

    /**
     * 按用户名查询用户展示信息
     *
     * @param username 用户名
     * @return 用户展示信息
     */
    public UserVo getUserInfo(String username);

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
    UserVo getUserInfoById(Integer userId);
    /**
     * 按学院查询用户
     *
     * @param college 学院名称
     * @return 用户列表
     */
    List<User> getUserByCollege(String college);

    /**
     * 仅更新指定用户的最近登录时间
     * <p>
     * 登录流程使用精准更新而不是 {@code updateById(user)}：
     * {@code updateById} 会把整个实体（含从缓存读出的 password / nick_name / phone 等旧快照）
     * 全字段写回，并发下可能用旧值覆盖他人刚修改的资料；
     * 且 sys_user 表并没有 update_time 列，全字段更新也没有必要。
     *
     * @param userId    用户ID
     * @param lastLogin 最近登录时间
     * @return 是否更新成功
     */
    boolean updateLastLogin(Integer userId, Date lastLogin);
}
