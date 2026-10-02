package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.UserRole;
import com.hnkjzyxy.ab.service.UserRoleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户角色关联Service实现类
 *
 * @author 16702
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    /**
     * 用户角色关联数据访问接口
     */
    @Resource
    private UserRoleMapper userRoleMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> selectRoleByUserId(Integer userId) {
        //return userRoleMapper.selectRoleByUserId(userId);
        String roleName = userRoleMapper.getRoleWeight(userId).getRoleName();
        ArrayList<String> list = new ArrayList<>();
        list.add(roleName);
        return list;
    }
}
