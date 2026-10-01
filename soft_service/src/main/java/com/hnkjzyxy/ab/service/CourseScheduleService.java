package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.model.CourseScheduleSyncResult;

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

    /**
     * 从 OA 同步课表（带数据校验、分布式锁、事务整体替换）
     * <p>
     * 同步失败时正式表保持不变，不会出现「先清空、再拉取失败」导致的数据丢失。
     * </p>
     *
     * @param source 触发来源，用于日志区分（cron / startup / manual）
     * @return 同步结果明细
     */
    CourseScheduleSyncResult sync(String source);

    /**
     * 刷新课程安排数据
     *
     * @return 同步成功返回 true
     */
    boolean refresh();
}
