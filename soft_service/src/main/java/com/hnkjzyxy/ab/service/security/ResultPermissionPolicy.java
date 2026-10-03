package com.hnkjzyxy.ab.service.security;

import com.hnkjzyxy.ab.constant.RoleCodes;
import com.hnkjzyxy.ab.dto.ResultAccessScope;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.model.User;
import org.springframework.stereotype.Component;

/**
 * 子任务成绩查看及修改权限策略
 * <p>
 * 新旧查询统一允许有效院长或管理员：院长查看本学院，管理员查看全部学院。
 * 修改必须具有有效院长角色，即使同时持有管理员角色也只允许修改当前所属学院成绩。
 * 账号状态、角色及学院均使用数据库当前资料，生成的范围只在本次操作中使用。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
@Component
public class ResultPermissionPolicy {
    /**
     * 当前启用账号及全部有效角色的统一校验组件
     */
    private final RoleAssert roleAssert;

    /**
     * 构造成绩访问策略
     *
     * @param roleAssert 当前账号及有效角色断言组件
     */
    public ResultPermissionPolicy(RoleAssert roleAssert) {
        this.roleAssert = roleAssert;
    }

    /**
     * 校验成绩查看权限并生成新旧查询共用的数据范围
     *
     * @param operator 认证链路取得的操作人
     * @return 管理员的全学院查看范围，或院长的当前学院范围
     * @throws AuthPermissionException 身份无效时返回401；不持有有效院长或管理员角色，
     *                                或院长学院配置不完整时返回403
     */
    public ResultAccessScope requireView(User operator) {
        RoleAssert.ActiveRoles active = roleAssert.requireAnyOf(operator, RoleCodes.DEAN, RoleCodes.ADMIN);
        // 查看时管理员身份优先，因此管理员兼院长可查看全部学院。
        if (active.getCodes().contains(RoleCodes.ADMIN)) {
            return ResultAccessScope.all(active.getUser().getUserId());
        }
        return requireCollegeScope(active.getUser());
    }

    /**
     * 校验院长评分权限并生成当前学院的修改范围
     *
     * @param operator 认证链路取得的操作人
     * @return 当前数据库账号所属学院的修改范围，始终不是全学院范围
     * @throws AuthPermissionException 身份无效时返回401；无有效院长角色或学院配置不完整时返回403
     */
    public ResultAccessScope requireEdit(User operator) {
        // 管理员查看能力不自动授予评分职责，多角色账号也必须保留单学院修改边界。
        return requireCollegeScope(roleAssert.requireAnyOf(operator, RoleCodes.DEAN).getUser());
    }

    /**
     * 从已核验的数据库账号生成精确学院范围
     *
     * @param current 已通过账号及角色校验的数据库用户
     * @return 该用户当前所属学院的访问范围
     * @throws AuthPermissionException 学院为空、全空白或含首尾空白时返回403
     */
    private ResultAccessScope requireCollegeScope(User current) {
        String college = current.getCollege();
        // 空学院不表示全校，也不自动修剪配置后放行，应由账号管理流程订正数据。
        if (college == null || college.trim().isEmpty() || !college.equals(college.trim())) {
            throw new AuthPermissionException(403, "当前账号学院配置不完整，请联系管理员");
        }
        return ResultAccessScope.college(current.getUserId(), college);
    }
}
