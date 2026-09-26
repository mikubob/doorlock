package com.hnkjzyxy.ab.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统公告
 * 存储公告标题、内容、创建人及置顶、浏览量等属性
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Notice implements Serializable {

    /**
     * 公告ID
     */
    @TableId
    private Integer id;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容
     */
    private String content;

    /**
     * 创建人
     */
    private String createName;

    /**
     * 创建时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 浏览量
     */
    private Integer readCount;

    /**
     * 状态（逻辑删除：0=正常，1=已删除）
     */
    @TableLogic
    private Integer status;

    /**
     * 是否置顶（0=否，1=是）
     */
    private Integer isTop;

}
