package com.hnkjzyxy.ab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生信息查询参数
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class AdmissionDto {

    /**
     * 班级名
     */
    private String className;

    /**
     * 排序字段：按专业成绩排序
     */
    private String score;

    /**
     * 排序字段：按专业成绩汇总排序
     */
    private String total;

    /**
     * 志愿方向过滤
     */
    private String majorFilter;

    /**
     * 录取状态过滤（0=未录取，1=已录取）
     */
    private Integer status;

    /**
     * 录取方向
     */
    private String result;

    /**
     * 姓名
     */
    private String name;
}
