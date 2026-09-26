package com.hnkjzyxy.ab.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 专业方向信息
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@TableName("sys_major_details")
public class MajorDetails {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 方向编码
     */
    private String majorCode;

    /**
     * 方向名称
     */
    private String majorName;

    /**
     * 待录取人数
     */
    private Integer number;

    /**
     * 已录取人数
     */
    private Integer accepted;

    /**
     * 删除标记（0=未删除，1=已删除）
     */
    @TableLogic
    private Integer delFlag;

}
