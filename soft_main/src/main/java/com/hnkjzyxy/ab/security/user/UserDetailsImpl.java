package com.hnkjzyxy.ab.security.user;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 在security中用户、密码都是明文存在配置文件中的（单一，不可靠，所以需要从数据库中查询）
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/12 13:24
 */
@Service
public class UserDetailsImpl implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println(username);
        User user = userService.getOne(new QueryWrapper<User>().eq("user_name", username));
        if (ObjectUtil.isNull(user)) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        user.setLastLogin(new Date());
        userService.updateById(user);
        //(用户ID，用户名，密码，权限信息)
        return new AccountUser(user.getUserId(), user.getUserName(), user.getPassword(), getUserAuthority(user.getUserId()));
    }

    /**
     * 获取用户权限信息（角色、菜单权限）
     *
     * @param userId 用户Id
     * @return
     */
    public List<GrantedAuthority> getUserAuthority(Integer userId) {
        String authority = userService.getUserAuthority(userId);
        return AuthorityUtils.commaSeparatedStringToAuthorityList(authority);
    }
}
