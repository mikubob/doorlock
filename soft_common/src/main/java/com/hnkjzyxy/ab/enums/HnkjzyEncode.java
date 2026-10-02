package com.hnkjzyxy.ab.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/***
 * 角色权重枚举类
 * 权重值越大，权限越大
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum HnkjzyEncode {

    /**
     * 普通用户角色权重
     */
    NORMAL(1, "普通用户"),
    /**
     * 超级用户角色权重
     */
    ADMIN(100, "超级用户"), //超级管理员修改为100
    /**
     * 教研室主任角色权重
     */
    DIRECTOR(5, "教研室主任"), //教研室
    /**
     * 院长角色权重
     */
    DEAN(9, "院长"),  //二级学院
    /**
     * 教研室角色权重
     */
    DEPARTMENT(2, "教研室"),
    /**
     * 书记角色权重
     */
    CLERK(9, "书记"),
    /**
     * 辅导员角色权重
     */
    COUNSELLOR(2, "辅导员"),
    /**
     * 教务处角色权重
     */
    LEADER(20, "教务处");  //校级

    /**
     * 角色权重
     */
    private Integer code;
    /**
     * 角色中文名称
     */
    private String name;

}
