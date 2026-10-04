package com.hnkjzyxy.ab.dto;

import lombok.Getter;

/**
 * 服务端生成的项目考核列表访问范围
 * <p>
 * 仅保留本接口原有最高有效角色权重分支，不作为请求参数或学院授权策略。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-04
 */
@Getter
public final class ProjectAssessScope {
    /**
     * 已认证的操作人ID
     */
    private final Integer operatorId;

    /**
     * 本次查询范围类型
     */
    private final Type type;

    /**
     * 构造已由服务层确定的访问范围
     *
     * @param operatorId 已认证的操作人ID
     * @param type 范围类型，为空时按无范围处理
     */
    public ProjectAssessScope(Integer operatorId, Type type) {
        this.operatorId = operatorId;
        this.type = operatorId == null || type == null ? Type.NONE : type;
    }

    /**
     * 与原有角色权重分支对应的查询范围
     */
    public enum Type {
        /**
         * 全部有效CC接收人
         */
        ALL_RECIPIENTS,
        /**
         * 与操作人共享非普通角色的有效CC接收人
         */
        SHARED_ROLE_RECIPIENTS,
        /**
         * 与操作人共享权重2角色的有效CC接收人
         */
        SHARED_DEPARTMENT_RECIPIENTS,
        /**
         * 操作人已有结果的项目
         */
        SELF_RESULTS,
        /**
         * 未命中已有分支，不允许读取数据
         */
        NONE
    }
}
