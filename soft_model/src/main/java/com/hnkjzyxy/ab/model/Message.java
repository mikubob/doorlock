package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 站内消息
 * 项目提醒、打回等系统通知消息
 *
 * @version 1.0
 * @author Spell a
 * @date 2023/4/17 16:16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message implements Serializable {

    /**
     * 消息ID
     */
    @TableId
    private Integer id;

    /**
     * 接收用户ID
     */
    private Integer userId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 创建人
     */
    private String createName;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 状态（0=未读，1=已读）
     */
    private Integer status;

}
