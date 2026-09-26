package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.Menu;

import java.util.Collection;
import java.util.List;

public interface MenuService extends IService<Menu> {
    List<Menu> treeMenu(Collection<Menu> menus);
}
