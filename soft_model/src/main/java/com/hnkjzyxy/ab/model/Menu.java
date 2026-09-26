package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 菜单信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Menu implements Serializable {

    /**
     * 菜单ID
     */
    @TableId(type = IdType.AUTO)
    private Integer menuId;

    /**
     * 父级菜单ID（0 表示顶级菜单）
     */
    private Integer parentId;

    /**
     * 菜单名称
     */
    @NotBlank(message = "请输入菜单名称")
    private String menuName;

    /**
     * 权限编码
     */
    @NotBlank(message = "请输入唯一编码")
    private String code;

    /**
     * 菜单图标
     */
    private String icon;

    /**
     * 前端路由地址
     */
    private String path;

    /**
     * 前端组件路径
     */
    private String component;

    /**
     * 菜单类型（1=菜单，其他=按钮/权限点）
     */
    @NotNull(message = "请选择类型")
    private Integer type;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 菜单状态
     */
    @TableLogic
    @NotNull(message = "请选择状态")
    private Integer status;

    /**
     * 排序号
     */
    @NotNull(message = "请输入排序")
    private Integer sort;

    /**
     * 是否在导航栏展示
     */
    private Boolean isNav;

    /**
     * 子菜单列表（非数据库字段）
     */
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<Menu> children;
}
