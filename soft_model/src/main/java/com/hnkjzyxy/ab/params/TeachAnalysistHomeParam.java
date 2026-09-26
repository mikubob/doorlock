package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 教学画像分析参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeachAnalysistHomeParam {

    /**
     * 年份
     */
    private String year;

    /**
     * 项目ID列表
     */
    @NotNull(message = "项目id不能为空!")
    private List<Integer> projectId;

    /**
     * 教师工号
     */
    private String teacherId;

    /**
     * 学院名称
     */
    private String college;

    /**
     * 时间范围列表
     */
    private List<String> Times;

}
