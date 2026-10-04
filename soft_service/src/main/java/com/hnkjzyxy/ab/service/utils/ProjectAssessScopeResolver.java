package com.hnkjzyxy.ab.service.utils;

import com.hnkjzyxy.ab.dto.ProjectAssessScope;
import com.hnkjzyxy.ab.enums.HnkjzyEncode;
import com.hnkjzyxy.ab.exception.AuthPermissionException;
import com.hnkjzyxy.ab.mapper.UserRoleMapper;
import com.hnkjzyxy.ab.model.Role;
import com.hnkjzyxy.ab.model.User;
import org.springframework.stereotype.Component;

/**
 * 项目考核列表的原有角色范围解析器
 * <p>
 * 每次仅查询一次最高有效角色；权重10等未覆盖分支保持空结果。
 * 共享角色的实际约束交由XML表达，不扩展为新的学院授权规则。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-04
 */
@Component
public class ProjectAssessScopeResolver {
    /**
     * 最高有效角色数据访问接口
     */
    private final UserRoleMapper userRoleMapper;

    /**
     * 构造本接口的范围解析器
     *
     * @param userRoleMapper 用户角色数据访问接口
     */
    public ProjectAssessScopeResolver(UserRoleMapper userRoleMapper) {
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 根据认证用户及最高有效角色生成本次查询范围
     *
     * @param operator 认证链路取得的操作人
     * @return 本接口现有分支对应的查询范围
     * @throws AuthPermissionException 无认证身份时返回401，无有效角色或权重时返回403
     */
    public ProjectAssessScope resolve(User operator) {
        if (operator == null || operator.getUserId() == null) {
            throw new AuthPermissionException(401, "请先登录");
        }
        Role role = userRoleMapper.getRoleWeight(operator.getUserId());
        if (role == null || role.getWeight() == null) {
            throw new AuthPermissionException(403, "当前用户没有有效的项目考核查询角色");
        }
        int weight = role.getWeight();
        ProjectAssessScope.Type type = ProjectAssessScope.Type.NONE;
        if (weight >= HnkjzyEncode.LEADER.getCode()) {
            type = ProjectAssessScope.Type.ALL_RECIPIENTS;
        } else if (weight == HnkjzyEncode.DEAN.getCode()) {
            type = ProjectAssessScope.Type.SHARED_ROLE_RECIPIENTS;
        } else if (weight == HnkjzyEncode.DIRECTOR.getCode()) {
            type = ProjectAssessScope.Type.SHARED_DEPARTMENT_RECIPIENTS;
        } else if (weight < HnkjzyEncode.DIRECTOR.getCode()) {
            type = ProjectAssessScope.Type.SELF_RESULTS;
        }
        return new ProjectAssessScope(operator.getUserId(), type);
    }
}
