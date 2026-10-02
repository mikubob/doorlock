package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.StudentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
     * @return 有效学生录取信息，不存在时返回 null
     */
    default StudentInfo getStudentInfoByStudentId(@Param("studentId") String studentId) {
        return selectOne(new LambdaQueryWrapper<StudentInfo>().eq(StudentInfo::getStudentId, studentId));
    }

    /**
     * 查询学生信息中的班级名称
     *
     * @return 学生信息中的班级名称列表，已去重
     */
    List<String> selectClassNameList();
}
