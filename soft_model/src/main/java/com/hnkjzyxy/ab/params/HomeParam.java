package com.hnkjzyxy.ab.params;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 首页统计查询参数
 *
 * @version 1.0
 * @author Spell a
 * @date 2024-01-17 10:59
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HomeParam {

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
     * 教研室名称
     */
    private String department;

    /**
     * 教师姓名
     */
    private String teacher;

    /**
     * 学院名称
     */
    private String college;

    /**
     * 时间范围列表
     */
    private List<String> Times;

}
