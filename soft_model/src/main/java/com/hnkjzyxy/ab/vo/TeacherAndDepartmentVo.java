package com.hnkjzyxy.ab.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 项目关联的老师与教研室
 * 用于雷达图按老师 / 按教研室筛选的下拉数据
 *
 * @version 1.0
 * @author Lucas
 * @date 2024/10/8 11:43
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeacherAndDepartmentVo {

    /**
     * 和项目相关的老师姓名列表
     */
    private List<String> teacherNameList;

    /**
     * 和项目相关的教研室名称列表
     */
    private List<String> departmentNameList;
}
