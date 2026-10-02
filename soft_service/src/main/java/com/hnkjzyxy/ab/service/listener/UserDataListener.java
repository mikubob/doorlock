package com.hnkjzyxy.ab.service.listener;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.util.ListUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hnkjzyxy.ab.dto.excel.UserModel;
import com.hnkjzyxy.ab.model.User;
import com.hnkjzyxy.ab.model.UserRole;
import com.hnkjzyxy.ab.service.UserRoleService;
import com.hnkjzyxy.ab.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @version 1.0
 * @email: 1670203784@qq.com
 * @author: Spell a
 * @date: 2024-01-04 21:16
 */
public class UserDataListener extends AnalysisEventListener<UserModel> {

    private static final int BATCH_COUNT = 100;
    Map<String, Integer> roleNames = new HashMap<String, Integer>() {{
        put("行政", 4);
        put("机房", 5);
        put("电子信息工程", 6);
        put("院长", 8);
        put("书记", 17);
        put("辅导员", 18);
        put("云计算（前端开发方向）", 10);
        put("软件技术教研室", 11);
        put("动漫制作技术", 15);
        put("区块链技术应用", 16);
        put("游戏教研室", 19);
        put("云计算（运维技术方向）", 20);
    }};
    /**
     * 记录解析的数据总数
     */
    int count = 0;
    private UserService userService;
    private PasswordEncoder bCryptPasswordEncoder;
    private UserRoleService userRoleService;

    /**
     * 用于接收解析的所有数据
     */
    private List<UserModel> data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

    public UserDataListener(UserService userService, PasswordEncoder bCryptPasswordEncoder, UserRoleService userRoleService) {
        this.userService = userService;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userRoleService = userRoleService;
    }

    /**
     * 提供一个对外访问的方法
     *
     * @return 已解析的数据列表
     */
    public List<UserModel> getData() {
        return data;
    }

    /**
     * 每解析一行调用一次
     *
     * @param objects         当前行解析结果
     * @param analysisContext 解析上下文
     */
    @Override
    public void invoke(UserModel objects, AnalysisContext analysisContext) {
        //log.info("解析到一条数据:{}", JSON.toJSONString(data));
        data.add(objects);
        count++;
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (data.size() >= BATCH_COUNT) {
            saveData();
        }
    }

    /**
     * 所有数据解析完毕后执行的操作
     *
     * @param analysisContext 解析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        saveData();
        System.out.println("解析完毕，共" + (count) + "条数据");
    }

    public void saveData() {
        System.out.println("开始初始化用户数据");
        for (UserModel item : data) {
            User user = new User();
            user.setNickName(item.getNickName());
            user.setMajor(item.getMajor());
            user.setUserName(item.getUserName());
            user.setPhone(item.getPhone());
            User oldUser = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUserName, user.getUserName()));
            if (ObjectUtil.isNotNull(oldUser)) {
                System.out.println("用户已存在！");
                continue;
            }
            user.setPassword(bCryptPasswordEncoder.encode(user.getUserName()));
            userService.save(user);
            if (ObjectUtil.isNull(user.getUserId())) {
                System.out.println("用户不存在！");
                continue;
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getUserId());
            userRole.setRoleId(1);
            userRoleService.save(userRole);
            Integer roleId = roleNames.get(item.getRoleName());
            if (ObjectUtil.isNotNull(roleId)) {
                userRole = new UserRole();
                userRole.setUserId(user.getUserId());
                userRole.setRoleId(roleId);
                userRoleService.save(userRole);
            }
            if (item.getIsDirector().equals("1")) {
                userRole = new UserRole();
                userRole.setUserId(user.getUserId());
                userRole.setRoleId(3);
                userRoleService.save(userRole);
            }
        }
        ;
        System.out.println("初始化用户数据完成，共计：" + (count));
        // 存储完成清理 list
        data = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
    }
}
