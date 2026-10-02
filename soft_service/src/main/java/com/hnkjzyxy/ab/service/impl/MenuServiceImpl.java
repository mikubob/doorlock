package com.hnkjzyxy.ab.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hnkjzyxy.ab.mapper.MenuMapper;
import com.hnkjzyxy.ab.model.Menu;
import com.hnkjzyxy.ab.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单Service实现类
 *
 * @author 16702
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Menu> treeMenu(Collection<Menu> menus) {
        List<Menu> collect = menus.stream().filter(item -> item.getParentId() == 0).map(item -> {
            item.setChildren(deepMenu(menus, item));
            return item;
        }).collect(Collectors.toList());
        return collect;
    }

    /**
     * 递归查找并补充菜单的子菜单
     *
     * @param data 待处理的数据集合
     * @param menu 当前菜单节点
     * @return 菜单列表
     */
    public List<Menu> deepMenu(Collection<Menu> data, Menu menu) {
        return data.stream().filter(item -> item.getParentId().equals(menu.getMenuId())).map(item -> {
            item.setChildren(deepMenu(data, item));
            return item;
        }).collect(Collectors.toList());
    }

}
