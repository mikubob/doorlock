package com.hnkjzyxy.ab.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.RoleMenu;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.RoleMenuService;
import com.hnkjzyxy.ab.service.RoleService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.params.UserQueryParam;
import com.hnkjzyxy.ab.vo.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限管理
 * 提供角色的增删改查、菜单权限分配及接收/审批角色查询
 */
@RestController
public class RoleController {
    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMenuService roleMenuService;
    @Autowired
    private UserService userService;

    /**
     * 获取角色已分配的菜单权限
     *
     * @param id 角色ID
     * @return 该角色拥有的菜单ID列表
     */
    @GetMapping("/role/info/{id}")
    @PreAuthorize("hasRole('admin')")
    public ApiResult roleInfo(@PathVariable Long id) {
        List<RoleMenu> list = roleMenuService.list(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        List<Integer> collect = list.stream().map(item -> item.getMenuId()).collect(Collectors.toList());
        return ApiResult.ok("menuIds", collect);
    }

    /**
     * 分页查询角色列表
     *
     * @param userQueryVo 分页及角色名称查询条件
     * @return 角色列表及分页数据
     */
    @GetMapping("/role/list")
    @PreAuthorize("hasRole('admin')")
    public ApiResult roleList(@Validated UserQueryParam userQueryVo) {
        Page<Role> rolePage = new Page<>(userQueryVo.getPage(), userQueryVo.getLimit());
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(userQueryVo.getName()), Role::getRoleName, userQueryVo.getName());
        roleService.page(rolePage, wrapper);
        return ApiResult.ok("data", rolePage);
    }

    /**
     * 新增或修改角色
     *
     * @param role 角色信息（含 roleId 时为修改）
     * @return 操作结果
     */
    @PostMapping("/role/save")
    @PreAuthorize("hasRole('admin')")
    @CacheEvict(value = {"authority", HnkjxyConstants.ROLES_LIST, HnkjxyConstants.RECEPTION_ROLES}, allEntries = true)
    public ApiResult roleSave(@Validated @RequestBody Role role) {
        Role oldRole = roleService.getOne(new QueryWrapper<Role>().eq("role_code", role.getRoleCode()));
        if (ObjectUtil.isNotNull(oldRole)) {
            return ApiResult.error(422, "当前编码不唯一");
        }
        roleService.saveOrUpdate(role);
        return ApiResult.ok("data", "操作成功");
    }

    /**
     * 批量删除角色
     *
     * @param ids 角色ID集合（多个以逗号分隔）
     * @return 操作结果
     */
    @PostMapping("/role/delete/{ids}")
    @PreAuthorize("hasRole('admin')")
    @CacheEvict(value = "authority", allEntries = true)
    public ApiResult delete(@PathVariable String ids) {
        roleService.deleteRole(ids);
        return ApiResult.ok("删除成功");
    }

    /**
     * 为角色分配菜单权限
     *
     * @param id      角色ID
     * @param menuIds 菜单ID集合（以逗号分隔的字符串）
     * @return 操作结果
     */
    @PostMapping("/role/commit/auth/{id}")
    @PreAuthorize("hasRole('admin')")
    @CacheEvict(value = {"authority"}, allEntries = true)
    @Transactional
    public ApiResult commitAuth(@PathVariable Integer id, @RequestBody String menuIds) {
        //删除所有关系
        roleMenuService.remove(new LambdaQueryWrapper<RoleMenu>().eq(RoleMenu::getRoleId, id));
        //根据menuIds添加关系
        String[] ids = menuIds.split(",");
        ArrayList<RoleMenu> roleMenus = new ArrayList<>();
        for (String s : ids) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setMenuId(Integer.parseInt(s));
            roleMenu.setRoleId(id);
            roleMenus.add(roleMenu);
        }
        roleMenuService.saveBatch(roleMenus);
        return ApiResult.ok("操作成功");
    }

    /**
     * 获取接收角色列表
     * 查询权重不小于 2 且不等于 10 的角色
     *
     * @return 可接收项目的角色列表
     */
    @GetMapping("/reception/roles")
    //@Cacheable(value = {HnkjxyConstants.RECEPTION_ROLES},key = "'ReceptionRoles'")
    public ApiResult getReceptionRoles() {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        //不等于10
        wrapper.ge("weight", 2);
        wrapper.ne("weight", 10);
        List<Role> roleList = roleService.list(wrapper);
        return ApiResult.ok("data", roleList);
    }

    /**
     * 获取接收用户列表
     *
     * @return 可接收项目的用户列表
     */
    @GetMapping("/users/list")
    //@Cacheable(value = {HnkjxyConstants.USERS_LIST},key = "'UsersList'")
    public ApiResult getUserList() {
        List<UserVo> userList = userService.getUserVoList();
        return ApiResult.ok("data", userList);
    }

    /**
     * 获取审批角色列表
     * 查询权重不小于 5 且不等于 10 的角色
     *
     * @return 具备审批权限的角色列表
     */
    @GetMapping("/roles/list")
    //@Cacheable(value = {HnkjxyConstants.ROLES_LIST},key = "'RolesList'")
    public ApiResult getRoleList() {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        //大于等于5
        wrapper.ge("weight", 5);
        //不等于10
        wrapper.ne("weight", 10);
        List<Role> roleList = roleService.list(wrapper);
        return ApiResult.ok("data", roleList);
    }

    /**
     * 获取审批用户列表
     *
     * @return 具备审批权限的用户列表
     */
    @GetMapping("/approve/users")
    //@Cacheable(value = {HnkjxyConstants.APPROVE_USERS},key = "'ApproveUsers'")
    public ApiResult getApproveUsers() {
        List<UserVo> userVos = userService.getApproveUsers();
        return ApiResult.ok("data", userVos);
    }

}
