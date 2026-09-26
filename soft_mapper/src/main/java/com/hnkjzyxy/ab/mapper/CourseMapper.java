package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {


    @Update("update sys_course set state = 0")
    void updateAllState();

    @Select("select DISTINCT college from sys_course")
    List<String> getColege();

    @Select("select DISTINCT classes from sys_course where college = #{college}")
    List<String> getClassByCollege(String college);
}
