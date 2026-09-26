package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.CourseSchedule;

import java.util.List;

/**
 * 课程安排Service接口
 */
public interface CourseScheduleService extends IService<CourseSchedule> {

    /**
     * 条件查询课程列表
     */
    List<CourseSchedule> getList(CourseSchedule courseSchedule);

    /**
     * 清空表
     */
    void truncateTable();

    /**
     * 新增课程
     */
    boolean saveCourseSchedule(CourseSchedule courseSchedule);

    /**
     * 更新课程
     */
    boolean updateCourseSchedule(CourseSchedule courseSchedule);

    /**
     * 删除课程
     */
    boolean deleteById(Integer id);

    /**
     * 批量删除
     */
    boolean deleteBatch(List<Integer> ids);

    boolean refresh();
}
