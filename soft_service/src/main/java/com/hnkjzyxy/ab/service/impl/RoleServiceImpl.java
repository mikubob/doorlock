package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.RoleMapper;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.UserRole;
import com.hnkjzyxy.ab.service.RoleService;
import com.hnkjzyxy.ab.service.UserRoleService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色Service实现类
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {
    /**
     * 角色数据访问接口
     */
    @Resource
    private RoleMapper roleMapper;
    /**
     * 用户角色关联业务服务
     */
    @Resource
    private UserRoleService userRoleService;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    @CacheEvict(value = {"authority"})
    public void deleteRole(String ids) {
        String[] split = ids.split(",");
        roleMapper.deleteBatchIds(Arrays.asList(split));
        roleMapper.userRole(ids);
        roleMapper.roleMenu(ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Role> getRoleByUserID(Integer userId) {
        List<UserRole> list = userRoleService.list(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (list.size() != 0) {
            List<Integer> collect = list.stream().map(UserRole::getRoleId).collect(Collectors.toList());
            return (List<Role>) this.listByIds(collect);
        } else {
            return null;
        }
    }
}
