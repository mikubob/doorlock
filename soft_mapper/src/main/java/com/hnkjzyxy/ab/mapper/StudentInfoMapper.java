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
    @Select("select * from sys_student_info where student_id = #{studentId} and del_flag = 1")
    StudentInfo getStudentInfoByStudentId(String studentId);

    @Select("select distinct class_name from sys_student_info")
    List<String> selectClassNameList();
}
