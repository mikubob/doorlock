package com.hnkjzyxy.ab.constant;

/**
 * 成绩访问策略使用的稳定角色编码
 * <p>
 * 对应现有角色表的 {@code role_code}，采用区分大小写的完整字符串匹配。
 * 显示名称及排序权重不参与身份判定，不提供名称或权重回退规则。
 * </p>
 *
 * @version 1.0
 * @date 2026-10-03
 */
public final class RoleCodes {
    /**
     * 管理员角色编码；可查看全部学院成绩，本编码本身不授予评分权限
     */
    public static final String ADMIN = "admin";
    /**
     * 院长角色编码；可查看及修改当前所属学院的成绩
     */
    public static final String DEAN = "dean";

    /**
     * 常量类私有构造方法，禁止实例化
     */
    private RoleCodes() { }
}
