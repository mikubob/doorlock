package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 菜单保存参数
 * 新增或编辑菜单时提交的请求体
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MenuSave implements Serializable {

    /**
     * 菜单名称
     */
    private String menuName;

    /**
     * 菜单唯一编码
     */
    @NotBlank(message = "请输入唯一编码")
    private String code;

    /**
     * 菜单图标
     */
    @NotBlank(message = "请选择图标")
    private String icon;

    /**
     * 菜单类型
     */
    @NotNull(message = "请选择类型")
    private Integer type;

    /**
     * 菜单状态
     */
    @NotNull(message = "请选择状态")
    private Integer status;

    /**
     * 排序号
     */
    @NotNull(message = "请输入排序号")
    private Integer sort;
}
