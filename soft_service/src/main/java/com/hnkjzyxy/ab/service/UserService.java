package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.vo.UserVo;

import java.util.Date;
import java.util.List;

public interface UserService extends IService<User> {
    String getUserAuthority(Integer userId);

    public List<Long> getNavMenu(Integer userId);

    public User getUserByName(String userName);

    public UserVo getUserInfo(String username);

    List<UserVo> getUserVoList();

    List<UserVo> getApproveUsers();

    UserVo getUserInfoById(Integer userId);
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
