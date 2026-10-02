package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.RoleMenuMapper;
import com.hnkjzyxy.ab.model.RoleMenu;
import com.hnkjzyxy.ab.service.RoleMenuService;
import org.springframework.stereotype.Service;

/**
 * 角色菜单关联Service实现类
 */
@Service
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu> implements RoleMenuService {
}
