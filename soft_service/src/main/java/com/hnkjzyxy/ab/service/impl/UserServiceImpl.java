package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.Menu;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.service.MenuService;
import com.hnkjzyxy.ab.service.RoleService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.vo.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户Service实现类
 *
 * @author 16702
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 角色业务服务
     */
    @Autowired
    private RoleService roleService;
    /**
     * 菜单业务服务
     */
    @Autowired
    private MenuService menuService;
    /**
     * 用户数据访问接口
     */
    @Resource
    private UserMapper userMapper;
    /**
     * 用户角色关联数据访问接口
     */
    @Resource
    private UserRoleMapper userRoleMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = {"authority"}, key = "#userId", sync = true)
    public String getUserAuthority(Integer userId) {
        String authority = "";
        //获取角色信息
        List<Role> roleList =
                roleService.list(new QueryWrapper<Role>().inSql("role_id", "select role_id from sys_user_role where user_id = " + userId));
        if (roleList.size() > 0) {
            // 拼接成 ROLE_admin,ROLE_normal
            authority = roleList.stream().map(item -> "ROLE_" + item.getRoleCode()).collect(Collectors.joining(","));
        }
        //获取菜单权限信息
        List<Long> menuIds = this.getNavMenu(userId);
        Collection<Menu> menus = menuService.listByIds(menuIds);
        if (menus.size() > 0) {
            String perms = menus.stream().map(i -> i.getCode()).collect(Collectors.joining(","));
            authority = authority.concat(",").concat(perms);
        }
        return authority;
    }

    /**
     * 查询用户有权访问的菜单ID
     *
     * @param userId 用户ID
     * @return 用户有权访问的菜单ID集合
     */

    @Override
    public List<Long> getNavMenu(Integer userId) {
        return userMapper.getNavMenu(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Cacheable(value = {HnkjxyConstants.USERS}, key = "#userName", sync = true)
    public User getUserByName(String userName) {
        //return baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUserName,userName));
        return this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, userName));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserVo getUserInfo(String userName) {
        return userMapper.getUserInfo(userName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserVo> getUserVoList() {
        List<UserVo> userVoList = userMapper.getUserVoList();
        userVoList.forEach(item -> {
            item.setWeight(userRoleMapper.getRoleWeight(item.getUserId()).getWeight());
        });
        return userVoList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserVo> getApproveUsers() {
        List<UserVo> approveUsers = userMapper.getApproveUsers();
        approveUsers.forEach(item -> {
            item.setWeight(userRoleMapper.getRoleWeight(item.getUserId()).getWeight());
        });
        return approveUsers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserVo getUserInfoById(Integer userId) {
        return userMapper.getUserInfoById(userId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<User> getUserByCollege(String college) {
        return  userMapper.getUserByCollege(college);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean updateLastLogin(Integer userId, Date lastLogin) {
        if (userId == null || lastLogin == null) {
            return false;
        }
        // 只更新 last_login 一列，避免用缓存中的旧快照覆盖他人刚改动的资料
        return this.update(new LambdaUpdateWrapper<User>()
                .set(User::getLastLogin, lastLogin)
                .eq(User::getUserId, userId));
    }


}
