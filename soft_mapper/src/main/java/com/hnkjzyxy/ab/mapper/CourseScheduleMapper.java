package com.hnkjzyxy.ab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnkjzyxy.ab.model.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.HashMap;
import java.util.List;

/**
 * 课程安排Mapper接口
 */
@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {


    /**
     * 动态条件查询课程列表
     */
    List<CourseSchedule> selectListByCondition(CourseSchedule courseSchedule);

    /**
     * 清空表（使用 TRUNCATE TABLE）
     */
    @Update("TRUNCATE TABLE sys_course_schedule")
    void truncateTable();
}
