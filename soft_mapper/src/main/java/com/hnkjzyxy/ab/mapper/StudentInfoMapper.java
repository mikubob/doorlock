package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.StudentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * (StudentInfo)表数据库访问层
 *
 * @author Binc
 * @since 2025-04-14 17:44:20
 */
@Mapper
public interface StudentInfoMapper extends BaseMapper<StudentInfo> {
    /**
     * 按学号查询学生录取记录
     *
     * @param studentId 学生学号
     * @return 学生录取信息信息
     */
    @Select("select * from sys_student_info where student_id = #{studentId} and del_flag = 1")
    StudentInfo getStudentInfoByStudentId(String studentId);

    /**
     * 查询学生信息中的班级名称
     *
     * @return 查询结果列表
     */
    @Select("select distinct class_name from sys_student_info")
    List<String> selectClassNameList();
}
