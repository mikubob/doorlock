package com.hnkjzyxy.ab.service.security;

import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.mapper.UserMapper;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.User;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 当前账号及有效角色断言组件
 * <p>
 * 直接通过 Mapper 重新读取账号状态、工号及全部有效角色，不使用用户资料或权限缓存。
 * 持有任一允许的有效角色即可通过，但名称、权重及无效角色均不能作为授权依据。
 * 本组件只核验身份和角色，具体业务的数据范围由调用方权限策略确定。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
@Component
public class RoleAssert {
    /**
     * 全部有效角色码查询接口，不使用最高权重单角色查询
     */
    private final UserRoleMapper userRoleMapper;
    /**
     * 当前数据库账号查询接口，绕过用户资料缓存
     */
    private final UserMapper userMapper;

    /**
     * 构造账号及角色断言组件
     *
     * @param userRoleMapper 用户有效角色查询接口
     * @param userMapper 当前账号查询接口
     */
    public RoleAssert(UserRoleMapper userRoleMapper, UserMapper userMapper) {
        this.userRoleMapper = userRoleMapper;
        this.userMapper = userMapper;
    }

    /**
     * 核验当前账号启用状态，并要求持有至少一个允许的有效角色
     * <p>
     * 操作人必须来自认证链路，不能直接使用客户端传入的用户对象。
     * 角色码按完整字符串精确匹配，不修剪、不转换大小写；允许集合为空时默认拒绝。
     * Mapper 查询失败保留原异常，不转换成无角色或权限不足。
     * </p>
     *
     * @param operator 认证链路取得的操作人，至少包含用户ID及用户名（工号）
     * @param allowedCodes 允许的角色码集合，null 元素不参与匹配
     * @return 已核验的数据库账号及不可修改的有效角色码集合
     * @throws AuthPermissionException 身份缺失、账号不存在、禁用或工号不符时返回401；
     *                                无有效角色、无允许角色或允许集合为空时返回403
     */
    public ActiveRoles requireAnyOf(User operator, String... allowedCodes) {
        // 在解引用、查询账号或角色前拒绝缺失的认证身份。
        if (operator == null || operator.getUserId() == null || operator.getUserName() == null) {
            throw new AuthPermissionException(401, "当前登录身份无效，请重新登录");
        }
        // 重新读取数据库资料，避免缓存中的旧状态或旧学院继续参与授权。
        User current = userMapper.selectById(operator.getUserId());
        if (current == null || !Integer.valueOf(1).equals(current.getStatus())
                || !Objects.equals(current.getUserName(), operator.getUserName())) {
            throw new AuthPermissionException(401, "认证用户无效或已禁用，请重新登录");
        }
        // 查询所有有效角色；仅持有书记等高权重角色不能代替院长身份。
        List<String> codes = userRoleMapper.selectActiveRoleCodes(current.getUserId());
        if (codes == null || codes.isEmpty()) {
            throw new AuthPermissionException(403, "当前账号未分配有效角色，请联系管理员");
        }
        // 任一精确匹配即可放行；空策略默认拒绝，且不捕获数据库异常后放行。
        boolean permitted = allowedCodes != null && Arrays.stream(allowedCodes)
                .filter(Objects::nonNull).anyMatch(codes::contains);
        if (!permitted) throw new AuthPermissionException(403, "无权限执行该操作");
        return new ActiveRoles(current, codes);
    }

    /**
     * 一次账号及角色校验的结果
     * <p>
     * 保存本次读取的数据库用户引用和角色集合副本。角色集合不可修改；
     * 用户实体本身仍是可变对象，调用方应只读使用，不将本结果作为请求DTO或授权缓存。
     * </p>
     */
    public static final class ActiveRoles {
        /**
         * 本次已核验为启用状态且工号一致的数据库账号
         */
        private final User user;
        /**
         * 去重后的有效角色码集合，不允许调用方追加角色
         */
        private final Set<String> codes;

        /**
         * 保存已核验账号并复制有效角色码
         *
         * @param user 本次读取并核验的数据库账号
         * @param codes 非空的有效角色码列表
         */
        private ActiveRoles(User user, List<String> codes) {
            this.user = user;
            // 先复制再封装，防止原始列表或返回集合被修改而改变已判定的角色。
            this.codes = Collections.unmodifiableSet(new HashSet<>(codes));
        }

        /**
         * 获取本次已核验的数据库账号
         *
         * @return 数据库用户引用，调用方应只读使用
         */
        public User getUser() { return user; }

        /**
         * 获取全部有效角色码
         *
         * @return 去重且不可修改的角色码集合
         */
        public Set<String> getCodes() { return codes; }
    }
}
