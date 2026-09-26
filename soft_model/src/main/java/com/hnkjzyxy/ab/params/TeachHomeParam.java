package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 教学画像统计参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeachHomeParam {

    /**
     * 年份
     */
    private String year;

    /**
     * 项目ID
     */
    @NotNull(message = "项目id不能为空!")
    private Integer projectId;

    /**
     * 教师工号
     */
    private String teacherId;

    /**
     * 学院名称
     */
    private String college;

}
