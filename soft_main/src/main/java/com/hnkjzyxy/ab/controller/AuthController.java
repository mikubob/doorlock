package com.hnkjzyxy.ab.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hnkjzyxy.ab.constant.HnkjxyConstants;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.model.UserRole;
import com.hnkjzyxy.ab.params.StringParam;
import com.hnkjzyxy.ab.params.UserPasswordParam;
import com.hnkjzyxy.ab.result.ApiResult;
import com.hnkjzyxy.ab.service.RoleService;
import com.hnkjzyxy.ab.service.UserRoleService;
import com.hnkjzyxy.ab.service.UserService;
import com.hnkjzyxy.ab.utils.RedisUtils;
import com.hnkjzyxy.ab.utils.UploadUtils;
import com.hnkjzyxy.ab.vo.UserQueryVo;
import com.hnkjzyxy.ab.vo.UserSave;
import com.hnkjzyxy.ab.vo.UserVo;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.*;

/**
 * 认证授权与用户管理
 * 提供登录验证码、用户信息、用户管理、头像与电子签名等接口
 *
 * @author Shinelon
 * @version 1.0
 * @time 2022/7/11 12:32
 */
@RestController
public class AuthController {
    /**
     * @PreAuthorize("denyAll()") // 全部拒绝访问
     * @PreAuthorize("permitAll()") //全部允许访问
     * @PreAuthorize("hasRole('admin')") //必须具有admin权限
     * @PreAuthorize("hasAuthority('sys:user:list')") //必须具有sys:user:list权限
     * .....还有很多
     */
    @Value("${absolute.jwt.suffix}")
    private String suffix;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private PasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private UploadUtils uploadUtils;

    @Value("${image.width}")
    private Integer width;
    @Value("${image.height}")
    private Integer height;


    /**
     * 获取图形验证码
     * 生成 120 秒有效期的数字验证码（白名单接口，无需登录）
     *
     * @return 验证码 token 及 Base64 图片
     */
    @GetMapping("/captcha")
    public ApiResult captcha() {
        String key = UUID.randomUUID().toString();
        SpecCaptcha captcha = new SpecCaptcha(width, height);
        captcha.setCharType(Captcha.TYPE_ONLY_NUMBER);
        String base64Image = captcha.toBase64();
        String code = captcha.text();
        redisUtils.set(key, code, 120);
        HashMap<String, Object> map = new HashMap<>();
        map.put("token", key);
        map.put("image", base64Image);
        map.put("code", 200);
        return ApiResult.ok("data", map);
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 当前登录用户信息（含角色岗位）
     */
    @GetMapping("/userInfo")
    public ApiResult getUserInfo(Authentication authentication) {
        UserVo userInfo = userService.getUserInfo(authentication.getName());
        userInfo.setJobTitle(userRoleService.selectRoleByUserId(userInfo.getUserId()));
        return ApiResult.ok("data", userInfo);
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 指定用户的详细信息
     */
    @GetMapping("/userInfoById")
    public ApiResult getUserInfo(@RequestParam("userid") Integer userId) {
        UserVo infoById = userService.getUserInfoById(userId);
        return ApiResult.ok("data", infoById);
    }

    /**
     * 修改当前登录用户信息
     *
     * @param userVo 待修改的用户信息（邮箱、手机号）
     * @return 操作结果
     */
    @PostMapping("/edit/user")
    @CacheEvict(value = {"user"}, key = "#userVo.userName")
    public ApiResult editUserInfo(@Validated @RequestBody UserVo userVo, Authentication authentication) {
        User userInfo = userService.getUserByName(authentication.getName());
        userInfo.setEmail(userVo.getEmail());
        userInfo.setPhone(userVo.getPhone());
        userService.updateById(userInfo);
        return ApiResult.ok("修改成功！");
    }

    /**
     * 分页查询用户列表
     *
     * @param queryPage 分页及用户名称查询条件
     * @return 用户列表（含角色信息）及分页数据
     */
    @PreAuthorize("hasRole('admin')")//必须拥有admin角色
    @GetMapping("/user/list")
    public ApiResult pass(@Validated UserQueryVo queryPage) {
        Page<User> page = new Page<>(queryPage.getPage(), queryPage.getLimit());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryPage.getName())) {
            wrapper.like(User::getUserName, queryPage.getName());
        }
        userService.page(page, wrapper);
        for (User user : page.getRecords()) {
            List<Role> roles = roleService.getRoleByUserID(user.getUserId());
            user.setRoles(roles);
        }
        return ApiResult.ok("data", page);
    }

    /**
     * 批量删除用户
     *
     * @param ids 用户ID数组（路径参数，多个以逗号分隔）
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/user/delete/{ids}")
    public ApiResult delete(@PathVariable String[] ids) {
        if (ObjectUtil.isNull(ids)) {
            throw new RuntimeException("删除用户不能为空！");
        }
        userService.removeByIds(Arrays.asList(ids));
        return ApiResult.ok("data", "删除成功");
    }

    /**
     * 修改用户
     *
     * @param user 待修改的用户信息
     * @return 修改后的用户信息
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/user/update")
    public ApiResult update(@RequestBody @Validated User user) {
        userService.updateById(user);
        return ApiResult.ok("data", user);
    }

    /**
     * 重置用户密码
     * 将用户密码重置为用户名（工号）
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/user/reset/{id}")
    public ApiResult reset(@PathVariable Integer id) {
        if (ObjectUtil.isNull(id)) {
            throw new RuntimeException("用户id不能为空！");
        }
        User user = userService.getById(id);
        if (ObjectUtil.isNull(user)) {
            throw new RuntimeException("重置用户不能为空！");
        }
        user.setPassword(bCryptPasswordEncoder.encode(user.getUserName()));
        userService.updateById(user);
        return ApiResult.ok();
    }

    /**
     * 新增用户
     *
     * @param userSave 新增用户信息
     * @return 操作结果
     */
    @PreAuthorize("hasRole('admin')")
    @PostMapping("/user/save")
    @CacheEvict(value = {HnkjxyConstants.USERS_LIST, HnkjxyConstants.APPROVE_USERS})
    public ApiResult save(@RequestBody @Validated UserSave userSave) {
        //User oldUser = userService.getUserByName(userSave.getUserName());
        //判断工号是否存在
        User oldUser = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, userSave.getUserName()));
        if (ObjectUtil.isNotNull(oldUser)) {
            return ApiResult.error(422, "当前工号已存在");
        }

        //判断用户名是否存在
        User oldUser2 = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getNickName, userSave.getNickName()));
        if (ObjectUtil.isNotNull(oldUser2)) {
            return ApiResult.error(422, "当前用户名已存在");
        }

        //判断是否电话号码是否存在
        User oldUser3 = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, userSave.getPhone()));
        if (ObjectUtil.isNotNull(oldUser3)) {
            return ApiResult.error(422, "当前电话号码已存在");
        }


        //判断邮箱是否存在
        User oldUser4 = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, userSave.getEmail()));
        if (ObjectUtil.isNotNull(oldUser4)) {
            return ApiResult.error(422, "当前邮箱已存在");
        }


        User user = new User();
        BeanUtils.copyProperties(userSave, user);
        user.setPassword(bCryptPasswordEncoder.encode(user.getUserName()));
        userService.save(user);
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getUserId());
        userRole.setRoleId(1);
        userRoleService.save(userRole);
        return ApiResult.ok();
    }

    /**
     * 分配用户角色权限
     *
     * @param id  用户ID
     * @param ids 角色ID集合（以逗号分隔的字符串）
     * @return 操作结果
     */
    @PostMapping("/user/auth/{id}")
    @PreAuthorize("hasRole('admin')")
    @CacheEvict(value = "authority", allEntries = true)
    public ApiResult commitAuth(@PathVariable Integer id, @RequestBody String ids) {
        if (ObjectUtil.isNull(id)) {
            throw new RuntimeException("分配权限用户不能为空！");
        }
        userRoleService.remove(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        //userService.updateById();
        User byId = userService.getById(id);
        userService.updateById(byId);
        redisUtils.del(suffix.concat("-").concat(String.valueOf(id)));
        String[] auths = ids.split(",");
        ArrayList<UserRole> userRoles = new ArrayList<>();
        for (String auth : auths) {
            UserRole userRole = new UserRole();
            userRole.setUserId(id);
            userRole.setRoleId(Integer.parseInt(auth));
            userRoles.add(userRole);
        }
        userRoleService.saveBatch(userRoles);
        return ApiResult.ok("分配权限成功");
    }

    /**
     * 修改当前登录用户密码
     *
     * @param param 原密码、新密码及确认密码
     * @return 操作结果
     */
    @PostMapping("/change/password")
    public ApiResult changePassword(@Validated @RequestBody UserPasswordParam param, Authentication authentication) {
        if (!param.getNewPassword().equals(param.getConfirmPassword())) {
            throw new RuntimeException("两次密码不一致！");
        }
        if (!bCryptPasswordEncoder.matches(param.getOldPassword(), authentication.getCredentials().toString())) {
            throw new RuntimeException("原密码不正确！");
        }
        User user = userService.getUserByName(authentication.getName());
        user.setPassword(bCryptPasswordEncoder.encode(param.getNewPassword()));
        userService.updateById(user);
        return ApiResult.ok("密码修改成功！");
    }

    /**
     * 上传用户头像
     *
     * @param file 头像图片文件
     * @param flag 上传标识（1=仅上传不更新用户头像，其他=上传并更新）
     * @return 头像访问地址
     */
    @PostMapping("/uploadImg")
    public ApiResult uploadImg(@RequestParam("file") MultipartFile file, Authentication authentication, @RequestParam("flag") String flag) {
        User user = userService.getOne(new QueryWrapper<User>().eq("user_name", authentication.getName()));
        String avatars = user.getAvatar();
        String userName = user.getUserName();
        Optional<String> s = uploadUtils.uploadImg(file, avatars, userName, flag);
        if (!"1".equals(flag) && s.isPresent()) {
            user.setAvatar(s.get());
            userService.updateById(user);
        }
        return ApiResult.ok("data", s.orElse(""));
    }

    /**
     * 获取电子签名图片列表
     *
     * @return 当前用户的电子签名图片地址列表
     */
    @GetMapping("/getSignImg")
    public ApiResult getSignImg(Authentication authentication) {
        User user = userService.getUserByName(authentication.getName());
        String dir = user.getUserName();
        List<String> signImg = uploadUtils.getSignImg(dir + "/sign/");
        return ApiResult.ok("data", signImg);
    }

    /**
     * 删除电子签名
     *
     * @param param 待删除的电子签名地址
     * @return 操作结果
     */
    @PostMapping("/delSignImg")
    public ApiResult delSignImg(@Valid @RequestBody StringParam param) {
        if (!StringUtils.hasText(param.getUrl())) {
            throw new RuntimeException("删除电子签名不能为空！");
        }
        uploadUtils.delSignImg(param.getUrl());
        return ApiResult.ok();
    }


}