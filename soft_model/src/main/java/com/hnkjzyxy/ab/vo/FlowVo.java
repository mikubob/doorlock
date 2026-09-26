package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;


/**
 * 流程信息
 * 用于流程列表及流程详情展示
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FlowVo implements Serializable {

    /**
     * 流程ID
     */
    private Integer id;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 所属项目ID
     */
    private Integer pId;

    /**
     * 流程描述
     */
    private String describe;

    /**
     * 流程标题
     */
    private String title;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

}
