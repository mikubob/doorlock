package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 课程数据访问接口
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {


    /**
     * 重置课程状态
     */
    @Update("update sys_course set state = 0")
    void updateAllState();

    /**
     * 查询学院名称
     *
     * @return 查询结果列表
     */
    @Select("select DISTINCT college from sys_course")
    List<String> getColege();

    /**
     * 按学院查询班级名称
     *
     * @param college 学院名称
     * @return 查询结果列表
     */
    @Select("select DISTINCT classes from sys_course where college = #{college}")
    List<String> getClassByCollege(String college);
}
