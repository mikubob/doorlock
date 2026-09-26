package com.hnkjzyxy.ab.model;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 项目信息采集项
 * 保存项目创建时由管理员自定义收集的信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Info implements Serializable {

    /**
     * 采集项主键ID
     */
    @TableId
    private Integer id;

    /**
     * 所属项目ID
     */
    @NotNull(message = "项目id不能为空！")
    private Integer pId;

    /**
     * 采集项的键名
     */
    private String infoKey;

    /**
     * 采集项的值（非数据库字段）
     */
    @TableField(exist = false)
    private List<Map<String, String>> infoValue;

}
