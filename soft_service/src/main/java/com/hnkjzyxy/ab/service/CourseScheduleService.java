package com.hnkjzyxy.ab.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hnkjzyxy.ab.dto.CourseSourceRebindDto;
import com.hnkjzyxy.ab.model.CourseSchedule;
import com.hnkjzyxy.ab.vo.CourseScheduleSyncResult;

import java.util.List;

/**
 * 课程安排Service接口
 */
public interface CourseScheduleService extends IService<CourseSchedule> {

    /**
     * 条件查询课程列表
     *
     * @param courseSchedule 课表信息
     * @return 课表列表
     */
    List<CourseSchedule> getList(CourseSchedule courseSchedule);

    /**
     * 新增课程
     *
     * @param courseSchedule 课表信息
     * @return 操作或条件校验结果
     */
    boolean saveCourseSchedule(CourseSchedule courseSchedule);

    /**
     * 更新课程
     *
     * @param courseSchedule 课表信息
     * @return 操作或条件校验结果
     */
    boolean updateCourseSchedule(CourseSchedule courseSchedule);

    /**
     * 明确复核来源改变的独立调整，保留调整及原稳定课程键。
     *
     * @param request 双方稳定身份、最新版本和确认理由
     * @return 复核成功返回 true
     */
    boolean rebindSource(CourseSourceRebindDto request);

    /**
     * 删除课程
     *
     * @param id 课表ID
     * @return 操作或条件校验结果
     */
    boolean deleteById(Integer id);

    /**
     * 批量删除
     *
     * @param ids 课表ID集合
     * @return 操作或条件校验结果
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
