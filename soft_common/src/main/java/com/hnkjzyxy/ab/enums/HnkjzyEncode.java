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

    NORMAL(1, "普通用户"),
    ADMIN(100, "超级用户"), //超级管理员修改为100
    DIRECTOR(5, "教研室主任"), //教研室
    DEAN(9, "院长"),  //二级学院
    DEPARTMENT(2, "教研室"),
    CLERK(9, "书记"),
    COUNSELLOR(2, "辅导员"),
    LEADER(20, "教务处");  //校级

    private Integer code;
    private String name;

}
