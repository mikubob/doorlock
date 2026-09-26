package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 角色菜单关联
 * 维护角色与菜单之间的多对多授权关系
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RoleMenu implements Serializable {

    /**
     * 关联记录主键ID
     */
    @TableId
    private Integer id;

    /**
     * 角色ID
     */
    private Integer roleId;

    /**
     * 菜单ID
     */
    private Integer menuId;
}
