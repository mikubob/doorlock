package com.hnkjzyxy.ab.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户角色关联
 * 维护用户与角色之间的多对多授权关系
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserRole implements Serializable {

    /**
     * 关联记录主键ID
     */
    private Integer id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 角色ID
     */
    private Integer roleId;
}
