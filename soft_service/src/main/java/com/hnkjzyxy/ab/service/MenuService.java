package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Menu;

import java.util.Collection;
import java.util.List;

/**
 * 菜单Service接口
 */
public interface MenuService extends IService<Menu> {
    /**
     * 将菜单集合整理为树形结构
     *
     * @param menus 待整理的菜单集合
     * @return 菜单列表
     */
    List<Menu> treeMenu(Collection<Menu> menus);
}
