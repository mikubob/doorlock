package com.hnkjzyxy.ab.dto;

import lombok.Getter;

/**
 * 服务端生成的不可变成绩访问范围
 * <p>
 * 由成绩权限策略根据当前数据库账号及有效角色构造，供 Service 和 Mapper 传递，
 * 不作为 Controller 的请求参数。全学院范围仅用于查看，评分必须使用明确的单学院范围。
 * 使用 Lombok 生成只读访问器；校验构造方法及范围工厂仍由本类显式维护。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
@Getter
public final class ResultAccessScope {
    /**
     * 已核验的当前操作人ID，不能使用查询或评分目标用户ID代替
     *
     * -- GETTER --
     * 获取已核验的操作人ID
     *
     * @return 当前操作人ID
     */
    private final Integer operatorId;
    /**
     * 是否允许查看全部学院；不表示具有全学院评分权限
     *
     * -- GETTER --
     * 判断是否允许查看全部学院
     *
     * @return 全学院查看时为 true，单学院范围时为 false
     */
    private final boolean allColleges;
    /**
     * 单学院范围的精确名称；全学院查看范围为 null
     *
     * -- GETTER --
     * 获取精确匹配的学院名称
     *
     * @return 单学院范围名称；全学院查看范围返回 null
     */
    private final String college;

    /**
     * 保存已由工厂方法确定的访问范围
     *
     * @param operatorId 已核验的操作人ID
     * @param allColleges 是否为全学院查看范围
     * @param college 单学院范围名称，全学院查看时为 null
     * @throws IllegalArgumentException 操作人ID为空时抛出
     */
    private ResultAccessScope(Integer operatorId, boolean allColleges, String college) {
        if (operatorId == null) throw new IllegalArgumentException("操作人ID不能为空");
        this.operatorId = operatorId;
        this.allColleges = allColleges;
        this.college = college;
    }

    /**
     * 构造管理员使用的全学院查看范围
     * <p>
     * 本方法只构造范围，不自行校验角色；调用方必须先通过查看权限策略。
     * </p>
     *
     * @param operatorId 已核验的操作人ID
     * @return 不限定学院的查看范围
     * @throws IllegalArgumentException 操作人ID为空时抛出
     */
    public static ResultAccessScope all(Integer operatorId) {
        return new ResultAccessScope(operatorId, true, null);
    }

    /**
     * 构造院长查看或修改成绩的单学院范围
     *
     * @param operatorId 已核验的操作人ID
     * @param college 当前数据库账号所属学院，必须非空且不含首尾空白
     * @return 保留原始学院名称的单学院范围
     * @throws IllegalArgumentException 操作人ID为空，或学院为空、全空白、含首尾空白时抛出
     */
    public static ResultAccessScope college(Integer operatorId, String college) {
        // 只校验，不自动修剪或映射学院别名，避免异常配置被隐式扩大为可访问范围。
        if (college == null || college.trim().isEmpty() || !college.equals(college.trim())) {
            throw new IllegalArgumentException("学院范围不能为空或含首尾空格");
        }
        return new ResultAccessScope(operatorId, false, college);
    }

}
