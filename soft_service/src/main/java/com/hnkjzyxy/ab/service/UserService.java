package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.vo.UserVo;

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
}
