package com.hnkjzyxy.ab.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hnkjzyxy.ab.model.Menu;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.MenuService;
import com.hnkjzyxy.ab.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 菜单管理
 * 提供导航菜单、菜单树维护及权限标识查询
 */
@RestController
public class MenuController {
    /**
     * 用户业务服务
     */
    @Autowired
    private UserService userService;

    /**
     * 菜单业务服务
     */
    @Autowired
    private MenuService menuService;

    /**
     * 获取当前用户导航菜单及权限标识
     *
     * @param authentication 当前登录认证信息
     * @return 菜单树列表与权限标识列表
     */
    @GetMapping("/menu/nav")
    public ApiResult getNav(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        if (Objects.isNull(user)) {
            throw new RuntimeException("用户不能为空！");
        }
        List<Long> navMenu = userService.getNavMenu(user.getUserId());
        Collection<Menu> menus = menuService.listByIds(navMenu);
        List<Menu> collect = menuService.treeMenu(menus);
        String userAuthority = userService.getUserAuthority(user.getUserId());
        String[] auths = userAuthority.split(",");
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("menuList", collect);
        hashMap.put("authList", auths);
        //返回权限和菜单列表
        return ApiResult.ok("data", hashMap);
    }

    /**
     * 查询完整菜单列表
     *
     * @return 统一接口响应
     */
    @GetMapping("/menu/list")
    @PreAuthorize("hasRole('admin')")
    public ApiResult menuList() {
        Collection<Menu> menus = menuService.list(new QueryWrapper<Menu>().orderByAsc("sort"));
        List<Menu> treeMenu = menuService.treeMenu(menus);
        return ApiResult.ok("data", treeMenu);
    }

    /**
     * 批量删除菜单
     * 存在子菜单的菜单不允许删除
     *
     * @param ids 菜单ID数组（多个以逗号分隔）
     * @return 操作结果
     */
    /**
     * 批量删除菜单
     * 存在子菜单的菜单不允许删除
     *
     * @param ids 菜单ID数组（多个以逗号分隔）
     * @return 操作结果
     */
    @PostMapping("/menu/delete/{ids}")
    @PreAuthorize("hasRole('admin')")
    @CacheEvict(value = {"authority"}, allEntries = true)
    public ApiResult delete(@PathVariable String[] ids) {
        List<Menu> list = menuService.list();
        List<String> strings = Arrays.asList(ids);//待删除的ids
        long count = list.stream().filter(item -> strings.contains(item.getParentId().toString())).count();
        if (count != 0) {
            return ApiResult.error(400, "当前元素存在子菜单！！");
        }
        menuService.removeByIds(Arrays.asList(ids));
        return ApiResult.ok("删除成功");
    }

    /**
     * 新增或修改菜单
     *
     * @param menu 菜单信息（含 menuId 时为修改）
     * @return 操作结果
     */
    @PostMapping("/menu/save")
    @PreAuthorize("hasRole('admin')")
    @CacheEvict(value = {"authority"}, allEntries = true)
    public ApiResult save(@RequestBody @Validated Menu menu) {
        if (menu.getType() == 1 && Objects.isNull(menu.getMenuId())) {
            String path = menu.getPath();
            String component = menu.getComponent();
            QueryWrapper<Menu> wrapper = new QueryWrapper<>();
            wrapper.eq("path", path).or().eq("component", component);
            Menu one = menuService.getOne(wrapper);
            if (Objects.nonNull(one)) {
                return ApiResult.error(400, "菜单路径不能相同!!");
            }
        }
        menuService.saveOrUpdate(menu);
        return ApiResult.ok("data", "操作成功");
    }

}
